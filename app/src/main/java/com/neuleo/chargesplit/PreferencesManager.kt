package com.neuleo.chargesplit

import android.content.SharedPreferences
import com.neuleo.chargesplit.model.VehiclePreset

data class VehicleChargingState(
    val currentSoc: Float = 20f,
    val targetSoc: Float = 80f,
    val chargingMode: String = "16A",
    val customAmps: Int = 16,
    val customPhases: Int = 3,
    val capacityKwh: Float = 85.5f
)

class PreferencesManager(private val prefs: SharedPreferences) {

    var selectedPreset: VehiclePreset
        get() {
            val raw = prefs.getString("selected_vehicle_preset", null)
                ?: prefs.getString("pref_vehicle_preset", null)
            return VehiclePreset.fromName(raw)
        }
        set(value) {
            prefs.edit().apply {
                putString("selected_vehicle_preset", value.name)
                putString("pref_vehicle_preset", value.id)
                putFloat("pref_battery_nominal_kwh", getBatteryCapacityForPreset(value))
                putFloat("pref_ac_efficiency", getAcEfficiencyForPreset(value.id))
                putFloat("pref_dc_efficiency", getDcEfficiencyForPreset(value.id))
                apply()
            }
        }

    // Backwards-compatible ID getter/setter
    var vehiclePresetId: String
        get() = selectedPreset.id
        set(value) {
            selectedPreset = VehiclePreset.fromName(value)
        }

    var batteryNominalKwh: Float
        get() = getBatteryCapacityForPreset(selectedPreset)
        set(value) = setBatteryCapacityForPreset(selectedPreset, value)

    var batteryDegradation: Float
        get() = prefs.getFloat("pref_battery_degradation", 0.0f)
        set(value) = prefs.edit().putFloat("pref_battery_degradation", value).apply()

    var acEfficiency: Float
        get() = getAcEfficiencyForPreset(selectedPreset.id)
        set(value) = setAcEfficiencyForPreset(selectedPreset.id, value)

    var dcEfficiency: Float
        get() = getDcEfficiencyForPreset(selectedPreset.id)
        set(value) = setDcEfficiencyForPreset(selectedPreset.id, value)

    var costPerKWh: Float
        get() = prefs.getFloat("electricity_price", prefs.getFloat("pref_cost_per_kwh", 0.35f))
        set(value) {
            prefs.edit().apply {
                putFloat("electricity_price", value)
                putFloat("pref_cost_per_kwh", value)
                apply()
            }
        }

    var wearCostPer1600: Float
        get() = getWearCostPer1600ForPreset(selectedPreset.id)
        set(value) = setWearCostPer1600ForPreset(selectedPreset.id, value)

    val effectiveCapacity: Float
        get() = batteryNominalKwh * (1.0f - batteryDegradation / 100.0f)

    fun applyPreset(preset: VehiclePreset) {
        selectedPreset = preset
    }

    // Vehicle Charging State Persistence
    fun getVehicleChargingState(preset: VehiclePreset): VehicleChargingState {
        val capacity = getBatteryCapacityForPreset(preset)
        val currentSoc = prefs.getFloat("current_soc_${preset.name}", 20f)
        val targetSoc = prefs.getFloat("target_soc_${preset.name}", 80f)
        val chargingMode = prefs.getString("charging_mode_${preset.name}", "16A") ?: "16A"
        val customAmps = prefs.getInt("custom_amps_${preset.name}", if (preset == VehiclePreset.LEAPMOTOR_T03) 32 else 16)
        val customPhases = prefs.getInt("custom_phases_${preset.name}", preset.maxPhases)

        return VehicleChargingState(
            currentSoc = currentSoc,
            targetSoc = targetSoc,
            chargingMode = chargingMode,
            customAmps = customAmps,
            customPhases = customPhases,
            capacityKwh = capacity
        )
    }

    fun saveVehicleChargingState(preset: VehiclePreset, state: VehicleChargingState) {
        prefs.edit().apply {
            putFloat("current_soc_${preset.name}", state.currentSoc)
            putFloat("target_soc_${preset.name}", state.targetSoc)
            putString("charging_mode_${preset.name}", state.chargingMode)
            putInt("custom_amps_${preset.name}", state.customAmps)
            putInt("custom_phases_${preset.name}", state.customPhases)
            putFloat("battery_capacity_${preset.name}", state.capacityKwh)
            putFloat("pref_${preset.id}_battery_nominal_kwh", state.capacityKwh)
            if (preset == selectedPreset) {
                putFloat("pref_battery_nominal_kwh", state.capacityKwh)
            }
            apply()
        }
    }

    // Preset Overrides Helper Methods
    fun getBatteryCapacityForPreset(preset: VehiclePreset): Float {
        val key = "battery_capacity_${preset.name}"
        if (prefs.contains(key)) {
            return prefs.getFloat(key, preset.defaultCapacityKwh)
        }
        val legacyKey = "pref_${preset.id}_battery_nominal_kwh"
        if (prefs.contains(legacyKey)) {
            return prefs.getFloat(legacyKey, preset.defaultCapacityKwh)
        }
        if (preset == VehiclePreset.CUSTOM && prefs.contains("pref_battery_nominal_kwh")) {
            return prefs.getFloat("pref_battery_nominal_kwh", 70.0f)
        }
        return preset.defaultCapacityKwh
    }

    fun setBatteryCapacityForPreset(preset: VehiclePreset, value: Float) {
        prefs.edit().apply {
            putFloat("battery_capacity_${preset.name}", value)
            putFloat("pref_${preset.id}_battery_nominal_kwh", value)
            if (preset == selectedPreset) {
                putFloat("pref_battery_nominal_kwh", value)
            }
            apply()
        }
    }

    fun getBatteryNominalKwhForPreset(presetId: String): Float {
        val preset = VehiclePreset.fromName(presetId)
        return getBatteryCapacityForPreset(preset)
    }

    fun setBatteryNominalKwhForPreset(presetId: String, value: Float) {
        val preset = VehiclePreset.fromName(presetId)
        setBatteryCapacityForPreset(preset, value)
    }

    fun getAcEfficiencyForPreset(presetId: String): Float {
        val preset = VehiclePreset.fromName(presetId)
        val key = "pref_${preset.id}_ac_efficiency"
        return if (prefs.contains(key)) {
            prefs.getFloat(key, preset.acEfficiency)
        } else {
            if (preset == VehiclePreset.CUSTOM) prefs.getFloat("pref_ac_efficiency", 0.90f) else preset.acEfficiency
        }
    }

    fun setAcEfficiencyForPreset(presetId: String, value: Float) {
        val preset = VehiclePreset.fromName(presetId)
        val key = "pref_${preset.id}_ac_efficiency"
        prefs.edit().putFloat(key, value).apply()
        if (preset == selectedPreset) {
            prefs.edit().putFloat("pref_ac_efficiency", value).apply()
        }
    }

    fun getDcEfficiencyForPreset(presetId: String): Float {
        val preset = VehiclePreset.fromName(presetId)
        val key = "pref_${preset.id}_dc_efficiency"
        return if (prefs.contains(key)) {
            prefs.getFloat(key, preset.dcEfficiency)
        } else {
            if (preset == VehiclePreset.CUSTOM) prefs.getFloat("pref_dc_efficiency", 0.90f) else preset.dcEfficiency
        }
    }

    fun setDcEfficiencyForPreset(presetId: String, value: Float) {
        val preset = VehiclePreset.fromName(presetId)
        val key = "pref_${preset.id}_dc_efficiency"
        prefs.edit().putFloat(key, value).apply()
        if (preset == selectedPreset) {
            prefs.edit().putFloat("pref_dc_efficiency", value).apply()
        }
    }

    fun getWearCostPer1600ForPreset(presetId: String): Float {
        val preset = VehiclePreset.fromName(presetId)
        val key = "pref_${preset.id}_wear_cost_per_1600"
        return if (prefs.contains(key)) {
            prefs.getFloat(key, 60.0f)
        } else {
            if (preset == VehiclePreset.CUSTOM) prefs.getFloat("pref_wear_cost_per_1600", 60.0f) else 60.0f
        }
    }

    fun setWearCostPer1600ForPreset(presetId: String, value: Float) {
        val preset = VehiclePreset.fromName(presetId)
        val key = "pref_${preset.id}_wear_cost_per_1600"
        prefs.edit().putFloat(key, value).apply()
        if (preset == selectedPreset) {
            prefs.edit().putFloat("pref_wear_cost_per_1600", value).apply()
        }
    }

    // Split / Wear calculation inputs persistence
    var splitTotalKwh: String
        get() = prefs.getString("split_total_kwh", "") ?: ""
        set(value) = prefs.edit().putString("split_total_kwh", value).apply()

    var splitSubKwh: String
        get() = prefs.getString("split_sub_kwh", "") ?: ""
        set(value) = prefs.edit().putString("split_sub_kwh", value).apply()

    // Charger-Specific Efficiencies
    fun getChargerKey(chargerType: String): String {
        return when (chargerType) {
            "Schuko (2.3 kW)", "Schuko (10A)" -> "schuko"
            "Wallbox AC 11 kW", "16A / 11 kW" -> "wallbox_ac_11kw"
            "Wallbox AC 22 kW", "32A / 22 kW" -> "wallbox_ac_22kw"
            "DC Schnelllader 50 kW" -> "dc_50kw"
            "Benutzerdefiniert", "Manuell" -> "custom"
            else -> chargerType.lowercase(java.util.Locale.US).replace(Regex("[^a-z0-9_]"), "_")
        }
    }

    fun isChargerCalibrated(vehicleId: String, chargerType: String): Boolean {
        val preset = VehiclePreset.fromName(vehicleId)
        val key = "pref_${preset.id}_efficiency_${getChargerKey(chargerType)}"
        return prefs.contains(key)
    }

    fun clearChargerEfficiency(vehicleId: String, chargerType: String) {
        val preset = VehiclePreset.fromName(vehicleId)
        val key = "pref_${preset.id}_efficiency_${getChargerKey(chargerType)}"
        prefs.edit().remove(key).apply()
    }

    fun getChargerEfficiency(vehicleId: String, chargerType: String, isAc: Boolean = true): Float {
        val preset = VehiclePreset.fromName(vehicleId)
        val key = "pref_${preset.id}_efficiency_${getChargerKey(chargerType)}"
        if (prefs.contains(key)) {
            return prefs.getFloat(key, 0.90f)
        }
        return when (chargerType) {
            "Schuko (2.3 kW)", "Schuko (10A)" -> 0.85f
            "Wallbox AC 11 kW", "16A / 11 kW", "Wallbox AC 22 kW", "32A / 22 kW" -> getAcEfficiencyForPreset(preset.id)
            "DC Schnelllader 50 kW" -> getDcEfficiencyForPreset(preset.id)
            "Benutzerdefiniert", "Manuell" -> if (isAc) getAcEfficiencyForPreset(preset.id) else getDcEfficiencyForPreset(preset.id)
            else -> if (isAc) getAcEfficiencyForPreset(preset.id) else getDcEfficiencyForPreset(preset.id)
        }
    }

    fun setChargerEfficiency(vehicleId: String, chargerType: String, efficiency: Float) {
        val preset = VehiclePreset.fromName(vehicleId)
        val key = "pref_${preset.id}_efficiency_${getChargerKey(chargerType)}"
        prefs.edit().putFloat(key, efficiency).apply()
    }
}
