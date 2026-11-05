package com.neuleo.chargesplit.model

enum class VehiclePreset(
    val displayName: String,
    val shortName: String,
    val defaultCapacityKwh: Float,
    val maxPhases: Int,
    val maxAmps: Int,
    val maxAcKw: Float,
    val defaultAcEfficiency: Float = 0.90f,
    val defaultDcEfficiency: Float = 0.92f
) {
    TESLA_MODEL_S_90D(
        displayName = "Tesla Model S 90D",
        shortName = "Model S 90D",
        defaultCapacityKwh = 85.5f,
        maxPhases = 3,
        maxAmps = 16,
        maxAcKw = 11.0f,
        defaultAcEfficiency = 0.91f,
        defaultDcEfficiency = 0.93f
    ),
    LEAPMOTOR_T03(
        displayName = "Leapmotor T03",
        shortName = "Leapmotor T03",
        defaultCapacityKwh = 37.3f,
        maxPhases = 1,
        maxAmps = 32,
        maxAcKw = 6.6f,
        defaultAcEfficiency = 0.90f,
        defaultDcEfficiency = 0.92f
    ),
    CUSTOM(
        displayName = "Benutzerdefiniert",
        shortName = "Custom",
        defaultCapacityKwh = 70.0f,
        maxPhases = 3,
        maxAmps = 32,
        maxAcKw = 22.0f,
        defaultAcEfficiency = 0.90f,
        defaultDcEfficiency = 0.90f
    );

    // Compatibility getters
    val id: String get() = name.lowercase()
    val nominalKwh: Float get() = defaultCapacityKwh
    val usableKwh: Float get() = defaultCapacityKwh
    val maxDcKw: Float get() = when (this) {
        TESLA_MODEL_S_90D -> 120.0f
        LEAPMOTOR_T03 -> 48.0f
        CUSTOM -> 50.0f
    }
    val acEfficiency: Float get() = defaultAcEfficiency
    val dcEfficiency: Float get() = defaultDcEfficiency
    val isSinglePhaseAc: Boolean get() = maxPhases == 1

    companion object {
        val ALL_PRESETS: List<VehiclePreset> = entries.toList()

        fun fromName(name: String?): VehiclePreset {
            if (name.isNullOrBlank()) return TESLA_MODEL_S_90D
            if (name == "TESLA_MODEL_S_P85" || name == "tesla_s_p85" || name == "TESLA_S_P85") {
                return TESLA_MODEL_S_90D
            }
            if (name == "LEAPMOTOR_T03" || name == "leapmotor_t03") {
                return LEAPMOTOR_T03
            }
            if (name == "CUSTOM" || name == "custom") {
                return CUSTOM
            }
            return entries.find { it.name.equals(name, ignoreCase = true) } ?: TESLA_MODEL_S_90D
        }

        fun fromId(id: String?): VehiclePreset = fromName(id)
    }
}
