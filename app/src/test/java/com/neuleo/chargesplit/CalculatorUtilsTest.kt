package com.neuleo.chargesplit

import org.junit.Test
import org.junit.Assert.*

class CalculatorUtilsTest {

    @Test
    fun testCalculateChargingCost() {
        val result = CalculatorUtils.calculateChargingCost(
            startSOC = 60f,
            endSOC = 40f,
            batteryCapacity = 70f,
            efficiency = 0.90f,
            costPerKWh = 0.35f,
            passengers = 0
        )
        // Expected energy consumed: (20 / 100 * 70) / 0.9 = 15.5555...
        // Total cost: 15.5555... * 0.35 = 5.4444...
        assertEquals(15.56f, result.requiredKWh, 0.01f)
        assertEquals(5.44f, result.totalCost, 0.01f)
        assertEquals(5.44f, result.costPerPerson, 0.01f)
    }

    @Test
    fun testCalculateWearCost() {
        val result = CalculatorUtils.calculateWearCost(
            kilometers = 160f,
            wearCostPer1600Km = 60f,
            baseMileage = 1600f,
            passengers = 1
        )
        assertEquals(6.0f, result.totalWearCost, 0.01f)
        assertEquals(3.0f, result.wearCostPerPerson, 0.01f)
    }
}
