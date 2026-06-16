package com.neuleo.chargesplit

import org.junit.Test
import org.junit.Assert.*

class SettingsValidatorTest {

    @Test
    fun testValidateBatteryCapacity() {
        assertEquals(70f, SettingsValidator.validateBatteryCapacity("70") ?: 0f, 0.01f)
        assertEquals(70.5f, SettingsValidator.validateBatteryCapacity("70.5") ?: 0f, 0.01f)
        assertNull(SettingsValidator.validateBatteryCapacity(""))
        assertNull(SettingsValidator.validateBatteryCapacity("abc"))
        assertNull(SettingsValidator.validateBatteryCapacity("-10"))
        assertNull(SettingsValidator.validateBatteryCapacity("0"))
    }

    @Test
    fun testValidateCostPerKWh() {
        assertEquals(0.35f, SettingsValidator.validateCostPerKWh("0.35") ?: 0f, 0.01f)
        assertEquals(0f, SettingsValidator.validateCostPerKWh("0") ?: -1f, 0.01f)
        assertNull(SettingsValidator.validateCostPerKWh("-0.5"))
        assertNull(SettingsValidator.validateCostPerKWh("abc"))
    }

    @Test
    fun testValidateEfficiency() {
        assertEquals(0.90f, SettingsValidator.validateEfficiency("90") ?: 0f, 0.01f)
        assertEquals(1.00f, SettingsValidator.validateEfficiency("100") ?: 0f, 0.01f)
        assertEquals(0.50f, SettingsValidator.validateEfficiency("50") ?: 0f, 0.01f)
        assertNull(SettingsValidator.validateEfficiency("0"))
        assertNull(SettingsValidator.validateEfficiency("101"))
        assertNull(SettingsValidator.validateEfficiency("-10"))
    }

    @Test
    fun testValidateWearCost() {
        assertEquals(60f, SettingsValidator.validateWearCost("60") ?: 0f, 0.01f)
        assertEquals(0f, SettingsValidator.validateWearCost("0") ?: -1f, 0.01f)
        assertNull(SettingsValidator.validateWearCost("-10"))
        assertNull(SettingsValidator.validateWearCost("abc"))
    }
}
