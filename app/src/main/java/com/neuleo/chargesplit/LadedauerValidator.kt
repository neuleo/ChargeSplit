package com.neuleo.chargesplit

object LadedauerValidator {
    fun validateSocRange(start: Float, target: Float): Boolean {
        if (start !in 0f..100f || target !in 0f..100f) return false
        return target >= start
    }

    fun shouldShowCustomCharger(chargerType: String): Boolean {
        return chargerType == "Benutzerdefiniert"
    }
}
