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

    fun getSinglePhaseLimit(chargerKw: Float): Float {
        return when {
            chargerKw >= 22f -> 4.6f   // 20A limit at 22kW/32A charger
            chargerKw >= 11f -> 3.68f  // 16A limit at 11kW/16A charger
            chargerKw >= 7.4f -> 4.6f  // 20A Schieflast limit at 7.4kW charger
            else -> chargerKw
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
            if (preset.isSinglePhaseAc) {
                minOf(preset.maxAcKw, getSinglePhaseLimit(chargerKw))
            } else {
                preset.maxAcKw
            }
        } else {
            preset.maxDcKw
        }

        val effectivePowerKw = minOf(chargerKw, vehicleMaxChargerKw)
        val isCapped = chargerKw > vehicleMaxChargerKw

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
