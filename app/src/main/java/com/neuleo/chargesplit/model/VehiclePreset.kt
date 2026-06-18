package com.neuleo.chargesplit.model

data class VehiclePreset(
    val id: String,
    val name: String,
    val nominalKwh: Float,
    val usableKwh: Float,
    val maxAcKw: Float,
    val maxDcKw: Float,
    val acEfficiency: Float,
    val dcEfficiency: Float,
    val isSinglePhaseAc: Boolean = false
) {
    companion object {
        val TESLA_S_P85 = VehiclePreset(
            id = "tesla_s_p85",
            name = "Tesla Model S P85",
            nominalKwh = 85.0f,
            usableKwh = 80.8f,
            maxAcKw = 22.0f,
            maxDcKw = 120.0f,
            acEfficiency = 0.91f,
            dcEfficiency = 0.93f
        )

        val LEAPMOTOR_T03 = VehiclePreset(
            id = "leapmotor_t03",
            name = "Leapmotor T03",
            nominalKwh = 37.3f,
            usableKwh = 36.0f,
            maxAcKw = 6.6f,
            maxDcKw = 48.0f,
            acEfficiency = 0.90f,
            dcEfficiency = 0.92f,
            isSinglePhaseAc = true
        )

        val CUSTOM = VehiclePreset(
            id = "custom",
            name = "Custom",
            nominalKwh = 70.0f,
            usableKwh = 70.0f,
            maxAcKw = 11.0f,
            maxDcKw = 50.0f,
            acEfficiency = 0.90f,
            dcEfficiency = 0.90f
        )

        val ALL_PRESETS = listOf(TESLA_S_P85, LEAPMOTOR_T03, CUSTOM)

        fun fromId(id: String?): VehiclePreset {
            return ALL_PRESETS.find { it.id == id } ?: CUSTOM
        }
    }
}
