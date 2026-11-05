package com.neuleo.chargesplit

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LadedauerInputTest {

    @Test
    fun testValidateSocRange() {
        // Valid cases
        assertTrue(LadedauerValidator.validateSocRange(10f, 80f))
        assertTrue(LadedauerValidator.validateSocRange(0f, 100f))
        assertTrue(LadedauerValidator.validateSocRange(50f, 50f))

        // Invalid cases: target < start
        assertFalse(LadedauerValidator.validateSocRange(80f, 10f))

        // Invalid cases: out of bounds
        assertFalse(LadedauerValidator.validateSocRange(-10f, 80f))
        assertFalse(LadedauerValidator.validateSocRange(10f, 101f))
    }

    @Test
    fun testShouldShowCustomCharger() {
        assertTrue(LadedauerValidator.shouldShowCustomCharger("Benutzerdefiniert"))
        assertTrue(LadedauerValidator.shouldShowCustomCharger("Manuell"))
        assertFalse(LadedauerValidator.shouldShowCustomCharger("Schuko (10A)"))
        assertFalse(LadedauerValidator.shouldShowCustomCharger("16A / 11 kW"))
        assertFalse(LadedauerValidator.shouldShowCustomCharger("32A / 22 kW"))
    }
}
