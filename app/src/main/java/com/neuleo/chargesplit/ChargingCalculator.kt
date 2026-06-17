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

    fun calculateChargingDuration(
        startSoc: Float,
        targetSoc: Float,
        chargerKw: Float,
        isAc: Boolean,
        preset: VehiclePreset,
        degradation: Float,
        electricityPrice: Float
    ): ChargingResult {
        val deltaSoc = (targetSoc - startSoc).coerceAtLeast(0f)
        val effectiveCapacity = preset.nominalKwh * (1f - degradation / 100f)
        val energyNeeded = effectiveCapacity * deltaSoc / 100f

        val vehicleMaxChargerKw = if (isAc) preset.maxAcKw else preset.maxDcKw
        val effectivePowerKw = minOf(chargerKw, vehicleMaxChargerKw)
        val isCapped = chargerKw > vehicleMaxChargerKw

        val efficiency = if (isAc) preset.acEfficiency else preset.dcEfficiency
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
}
