package com.neuleo.chargesplit

import com.neuleo.chargesplit.model.VehiclePreset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChargingCalculatorTest {

    @Test
    fun testTeslaSP85CalculationWithoutDegradation() {
        val result = ChargingCalculator.calculateChargingDuration(
            startSoc = 20f,
            targetSoc = 80f,
            chargerKw = 11f,
            isAc = true,
            preset = VehiclePreset.TESLA_S_P85,
            degradation = 0f,
            electricityPrice = 0.35f
        )

        // nominal = 85.0kWh, delta = 60%, energy needed = 51kWh, grid drawn = 51 / 0.91 = 56.044kWh
        // duration = (56.044 / 11) * 60 = 305.69 min (~5h 6min)
        assertEquals(305.69f, result.durationMinutes, 0.5f)
        assertEquals(56.04f, result.gridEnergyKwh, 0.1f)
        assertEquals(19.61f, result.totalCostEur, 0.1f)
        org.junit.Assert.assertFalse(result.isCapped)
    }

    @Test
    fun testTeslaSP85CalculationWithDegradation() {
        // Test with 20.4% degradation to check if it matches the plan's 4h 3min example (~243 minutes)
        val result = ChargingCalculator.calculateChargingDuration(
            startSoc = 20f,
            targetSoc = 80f,
            chargerKw = 11f,
            isAc = true,
            preset = VehiclePreset.TESLA_S_P85,
            degradation = 20.4f,
            electricityPrice = 0.35f
        )

        // effective capacity = 85 * 0.796 = 67.66 kWh
        // delta = 60% -> 40.596 kWh needed
        // grid drawn = 40.596 / 0.91 = 44.61 kWh
        // duration = 44.61 / 11 = 4.055 hours = 243.3 minutes (~4h 3min)
        assertEquals(243.3f, result.durationMinutes, 0.5f)
        assertEquals(44.61f, result.gridEnergyKwh, 0.1f)
    }

    @Test
    fun testLeapmotorT03CalculationDC() {
        val result = ChargingCalculator.calculateChargingDuration(
            startSoc = 10f,
            targetSoc = 90f,
            chargerKw = 50f, // exceeds 48kW max DC rate
            isAc = false,
            preset = VehiclePreset.LEAPMOTOR_T03,
            degradation = 0f,
            electricityPrice = 0.35f
        )

        // nominal = 37.3kWh, delta = 80%, energy = 29.84kWh, grid drawn = 29.84 / 0.92 = 32.43kWh
        // capped power = min(50, 48) = 48kW
        // duration = (32.43 / 48) * 60 = 40.54 minutes
        assertEquals(40.54f, result.durationMinutes, 0.5f)
        assertEquals(32.43f, result.gridEnergyKwh, 0.1f)
        assertTrue(result.isCapped)
    }

    @Test
    fun testPowerCappingAc() {
        val result = ChargingCalculator.calculateChargingDuration(
            startSoc = 20f,
            targetSoc = 80f,
            chargerKw = 22f, // exceeds Leapmotor's 6.6kW max AC rate
            isAc = true,
            preset = VehiclePreset.LEAPMOTOR_T03,
            degradation = 0f,
            electricityPrice = 0.35f
        )

        // capped power = 6.6kW
        assertEquals(6.6f, result.effectivePowerKw, 0.01f)
        assertTrue(result.isCapped)
    }
}
