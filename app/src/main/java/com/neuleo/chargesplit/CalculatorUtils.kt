package com.neuleo.chargesplit

object CalculatorUtils {
    val TABS = listOf("Ladekosten", "Verschleiß", "Einstellungen")

    data class ChargingCostResult(
        val requiredKWh: Float,
        val totalCost: Float,
        val costPerPerson: Float
    )

    data class WearCostResult(
        val totalWearCost: Float,
        val wearCostPerPerson: Float
    )

    fun calculateChargingCost(
        startSOC: Float,
        endSOC: Float,
        batteryCapacity: Float,
        efficiency: Float,
        costPerKWh: Float,
        passengers: Int
    ): ChargingCostResult {
        val deltaSOC = (startSOC - endSOC).coerceAtLeast(0f)
        val requiredKWh = (deltaSOC / 100f * batteryCapacity) / efficiency
        val totalCost = requiredKWh * costPerKWh
        val numPeople = passengers + 1
        val costPerPerson = totalCost / numPeople
        return ChargingCostResult(requiredKWh, totalCost, costPerPerson)
    }

    fun calculateWearCost(
        kilometers: Float,
        wearCostPer1600Km: Float,
        baseMileage: Float,
        passengers: Int
    ): WearCostResult {
        val totalWearCost = if (kilometers > 0f) {
            (kilometers / baseMileage) * wearCostPer1600Km
        } else {
            0f
        }
        val numPeople = passengers + 1
        val wearCostPerPerson = totalWearCost / numPeople
        return WearCostResult(totalWearCost, wearCostPerPerson)
    }
}
