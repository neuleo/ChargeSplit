package com.neuleo.chargesplit

import com.neuleo.chargesplit.model.VehiclePreset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChargingCalculatorTest {

    @Test
    fun testTeslaModelS90DCalculationWithoutDegradation() {
        val result = ChargingCalculator.calculateChargingDuration(
            startSoc = 20f,
            targetSoc = 80f,
            chargerKw = 11f,
            isAc = true,
            preset = VehiclePreset.TESLA_MODEL_S_90D,
            degradation = 0f,
            electricityPrice = 0.35f
        )

        // nominal = 85.5 kWh, delta = 60%, energy needed = 51.3 kWh, grid drawn = 51.3 / 0.91 = 56.37 kWh
        // duration = (56.374 / 11) * 60 = 307.49 min (~5h 7min)
        assertEquals(307.49f, result.durationMinutes, 0.5f)
        assertEquals(56.37f, result.gridEnergyKwh, 0.1f)
        assertEquals(19.73f, result.totalCostEur, 0.1f)
        assertFalse(result.isCapped)
    }

    @Test
    fun testTeslaModelS90DCalculationWithDegradation() {
        val result = ChargingCalculator.calculateChargingDuration(
            startSoc = 20f,
            targetSoc = 80f,
            chargerKw = 11f,
            isAc = true,
            preset = VehiclePreset.TESLA_MODEL_S_90D,
            degradation = 10f,
            electricityPrice = 0.35f
        )

        // effective capacity = 85.5 * 0.90 = 76.95 kWh
        // delta = 60% -> 46.17 kWh needed
        // grid drawn = 46.17 / 0.91 = 50.736 kWh
        // duration = (50.736 / 11) * 60 = 276.74 minutes (~4h 37min)
        assertEquals(276.74f, result.durationMinutes, 0.5f)
        assertEquals(50.74f, result.gridEnergyKwh, 0.1f)
    }

    @Test
    fun testLeapmotorT03Calculation16A() {
        // At an 11 kW (3x16A) charger, single-phase AC charging is limited to 16A * 230V = 3.68 kW
        val result = ChargingCalculator.calculateChargingDuration(
            startSoc = 20f,
            targetSoc = 80f,
            chargerKw = 11f,
            isAc = true,
            preset = VehiclePreset.LEAPMOTOR_T03,
            degradation = 0f,
            electricityPrice = 0.35f
        )

        assertEquals(3.68f, result.effectivePowerKw, 0.01f)
        assertTrue(result.isCapped)
    }

    @Test
    fun testLeapmotorT03Calculation32A_No20ALimit() {
        // At a 22 kW (3x32A) charger, 20A limit is removed! Vehicle draws 32A single phase capped at 6.6 kW OBC limit
        val result = ChargingCalculator.calculateChargingDuration(
            startSoc = 20f,
            targetSoc = 80f,
            chargerKw = 22f,
            isAc = true,
            preset = VehiclePreset.LEAPMOTOR_T03,
            degradation = 0f,
            electricityPrice = 0.35f
        )

        // Must NOT be capped at 4.6 kW! Effective power is 6.6 kW
        assertEquals(6.6f, result.effectivePowerKw, 0.01f)
        assertTrue(result.isCapped)
    }

    @Test
    fun testCalculateEffectivePower() {
        // Leapmotor T03: 1 phase @ 16A -> 3.68 kW
        val t03_16A = ChargingCalculator.calculateEffectivePower(
            chargerPhases = 3,
            chargerAmps = 16,
            preset = VehiclePreset.LEAPMOTOR_T03
        )
        assertEquals(3.68f, t03_16A, 0.01f)

        // Leapmotor T03: 1 phase @ 32A -> 7.36 kW capped at 6.6 kW
        val t03_32A = ChargingCalculator.calculateEffectivePower(
            chargerPhases = 3,
            chargerAmps = 32,
            preset = VehiclePreset.LEAPMOTOR_T03
        )
        assertEquals(6.6f, t03_32A, 0.01f)

        // Tesla Model S 90D: 3 phases @ 16A -> 11.0 kW
        val tesla_16A = ChargingCalculator.calculateEffectivePower(
            chargerPhases = 3,
            chargerAmps = 16,
            preset = VehiclePreset.TESLA_MODEL_S_90D
        )
        assertEquals(11.0f, tesla_16A, 0.05f)

        // Tesla Model S 90D: 1 phase @ 10A (Schuko) -> 2.3 kW
        val tesla_schuko = ChargingCalculator.calculateEffectivePower(
            chargerPhases = 1,
            chargerAmps = 10,
            preset = VehiclePreset.TESLA_MODEL_S_90D
        )
        assertEquals(2.3f, tesla_schuko, 0.01f)
    }

    @Test
    fun testLeapmotorT03CalculationDC() {
        val result = ChargingCalculator.calculateChargingDuration(
            startSoc = 10f,
            targetSoc = 90f,
            chargerKw = 50f,
            isAc = false,
            preset = VehiclePreset.LEAPMOTOR_T03,
            degradation = 0f,
            electricityPrice = 0.35f
        )

        // nominal = 37.3 kWh, delta = 80%, energy = 29.84 kWh, grid drawn = 29.84 / 0.92 = 32.43 kWh
        // capped power = min(50, 48) = 48 kW
        // duration = (32.43 / 48) * 60 = 40.54 minutes
        assertEquals(40.54f, result.durationMinutes, 0.5f)
        assertEquals(32.43f, result.gridEnergyKwh, 0.1f)
        assertTrue(result.isCapped)
    }

    @Test
    fun testCalculateCalibratedEfficiency() {
        val efficiency = ChargingCalculator.calculateCalibratedEfficiency(
            startSoc = 20f,
            targetSoc = 80f,
            actualDurationHours = 5.0f,
            chargerPowerKw = 11f,
            effectiveBatteryCapacityKwh = 85.5f
        )
        // 85.5 * 0.6 = 51.3; 51.3 / (5 * 11) = 51.3 / 55 = 0.9327
        assertEquals(0.933f, efficiency, 0.005f)

        val zeroEfficiency = ChargingCalculator.calculateCalibratedEfficiency(
            startSoc = 20f,
            targetSoc = 80f,
            actualDurationHours = 0f,
            chargerPowerKw = 11f,
            effectiveBatteryCapacityKwh = 85.5f
        )
        assertEquals(0f, zeroEfficiency, 0.001f)
    }

    @Test
    fun testCalculateChargingDurationWithEfficiencyOverride() {
        val result = ChargingCalculator.calculateChargingDuration(
            startSoc = 20f,
            targetSoc = 80f,
            chargerKw = 11f,
            isAc = true,
            preset = VehiclePreset.TESLA_MODEL_S_90D,
            degradation = 0f,
            electricityPrice = 0.35f,
            efficiencyOverride = 0.85f
        )

        // energy needed = 51.3 kWh, grid drawn = 51.3 / 0.85 = 60.35 kWh
        // duration = (60.35 / 11) * 60 = 329.2 min
        assertEquals(329.2f, result.durationMinutes, 0.5f)
        assertEquals(60.35f, result.gridEnergyKwh, 0.1f)
    }
}
