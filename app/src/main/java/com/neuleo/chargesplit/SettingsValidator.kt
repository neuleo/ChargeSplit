package com.neuleo.chargesplit

object SettingsValidator {
    fun validateBatteryCapacity(input: String): Float? {
        val value = input.toFloatOrNull()
        return if (value != null && value > 0f) value else null
    }

    fun validateCostPerKWh(input: String): Float? {
        val value = input.toFloatOrNull()
        return if (value != null && value >= 0f) value else null
    }

    fun validateEfficiency(input: String): Float? {
        val value = input.toFloatOrNull()
        return if (value != null && value > 0f && value <= 100f) value / 100f else null
    }

    fun validateWearCost(input: String): Float? {
        val value = input.toFloatOrNull()
        return if (value != null && value >= 0f) value else null
    }
}
