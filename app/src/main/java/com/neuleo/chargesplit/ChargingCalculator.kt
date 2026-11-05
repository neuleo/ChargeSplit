package com.neuleo.chargesplit

import com.neuleo.chargesplit.model.VehiclePreset

object ChargingCalculator {
    data class ChargingResult(
        val durationMinutes: Float,
        val gridEnergyKwh: Float,
        val totalCostEur: Float,
        val effectivePowerKw: Float,
        val isCapped: Boolean
    )

    /**
     * Ermittelt das einphasige Limit ohne die frühere 20A-Schieflast-Drossel.
     * Bei 22 kW Säulen (32 A) können volle 32 A (7,36 kW) gezogen werden.
     */
    fun getSinglePhaseLimit(chargerKw: Float): Float {
        return when {
            chargerKw >= 22f -> 7.36f  // Volle 32A an 22kW/32A Ladesäule
            chargerKw >= 11f -> 3.68f  // 16A Limit an 11kW/16A Ladesäule
            chargerKw >= 7.3f -> 7.36f // 32A an 7.4kW Ladesäule
            else -> chargerKw
        }
    }

    /**
     * Berechnet die effektive Ladeleistung anhand der Phasen und Stromstärke
     * unter Berücksichtigung der Fahrzeuglimits (maxPhases, maxAmps, maxAcKw).
     */
    fun calculateEffectivePower(
        chargerPhases: Int,
        chargerAmps: Int,
        preset: VehiclePreset,
        isAc: Boolean = true
    ): Float {
        if (!isAc) return preset.maxDcKw
        val effectivePhases = minOf(chargerPhases, preset.maxPhases)
        val effectiveAmps = minOf(chargerAmps, preset.maxAmps)
        val grossKw = (effectivePhases * effectiveAmps * 230f) / 1000f
        return minOf(grossKw, preset.maxAcKw)
    }

    fun getEffectiveAcChargerKw(chargerKw: Float, preset: VehiclePreset): Float {
        if (preset.isSinglePhaseAc) {
            val singlePhaseKw = getSinglePhaseLimit(chargerKw)
            return minOf(singlePhaseKw, preset.maxAcKw)
        } else {
            return preset.maxAcKw
        }
    }

    fun calculateChargingDuration(
        startSoc: Float,
        targetSoc: Float,
        chargerKw: Float,
        isAc: Boolean,
        preset: VehiclePreset,
        degradation: Float,
        electricityPrice: Float,
        efficiencyOverride: Float? = null
    ): ChargingResult {
        val deltaSoc = (targetSoc - startSoc).coerceAtLeast(0f)
        val effectiveCapacity = preset.nominalKwh * (1f - degradation / 100f)
        val energyNeeded = effectiveCapacity * deltaSoc / 100f

        val vehicleMaxChargerKw = if (isAc) {
            getEffectiveAcChargerKw(chargerKw, preset)
        } else {
            preset.maxDcKw
        }

        val effectivePowerKw = minOf(chargerKw, vehicleMaxChargerKw)
        val isCapped = chargerKw > effectivePowerKw

        val efficiency = efficiencyOverride ?: (if (isAc) preset.acEfficiency else preset.dcEfficiency)
        val gridEnergyDrawn = energyNeeded / efficiency

        val durationMinutes = if (effectivePowerKw > 0f) {
            (gridEnergyDrawn / effectivePowerKw) * 60f
        } else {
            0f
        }

        val totalCost = gridEnergyDrawn * electricityPrice

        return ChargingResult(
            durationMinutes = durationMinutes,
            gridEnergyKwh = gridEnergyDrawn,
            totalCostEur = totalCost,
            effectivePowerKw = effectivePowerKw,
            isCapped = isCapped
        )
    }

    fun calculateCalibratedEfficiency(
        startSoc: Float,
        targetSoc: Float,
        actualDurationHours: Float,
        chargerPowerKw: Float,
        effectiveBatteryCapacityKwh: Float
    ): Float {
        if (actualDurationHours <= 0f || chargerPowerKw <= 0f) return 0f
        val deltaSoc = (targetSoc - startSoc).coerceAtLeast(0f)
        val energyNeededKWh = effectiveBatteryCapacityKwh * (deltaSoc / 100f)
        val calculated = energyNeededKWh / (actualDurationHours * chargerPowerKw)
        return calculated.coerceIn(0f, 1f)
    }
}
