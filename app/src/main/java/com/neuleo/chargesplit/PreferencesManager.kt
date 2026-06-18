package com.neuleo.chargesplit

import android.content.SharedPreferences
import com.neuleo.chargesplit.model.VehiclePreset

class PreferencesManager(private val prefs: SharedPreferences) {

    var vehiclePresetId: String
        get() = prefs.getString("pref_vehicle_preset", "custom") ?: "custom"
        set(value) {
            prefs.edit().putString("pref_vehicle_preset", value).apply()
            // Keep legacy keys in sync with the active preset values
            prefs.edit().apply {
                putFloat("pref_battery_nominal_kwh", getBatteryNominalKwhForPreset(value))
                putFloat("pref_ac_efficiency", getAcEfficiencyForPreset(value))
                putFloat("pref_dc_efficiency", getDcEfficiencyForPreset(value))
            }.apply()
        }

    var batteryNominalKwh: Float
        get() = getBatteryNominalKwhForPreset(vehiclePresetId)
        set(value) = setBatteryNominalKwhForPreset(vehiclePresetId, value)

    var batteryDegradation: Float
        get() = prefs.getFloat("pref_battery_degradation", 0.0f)
        set(value) = prefs.edit().putFloat("pref_battery_degradation", value).apply()

    var acEfficiency: Float
        get() = getAcEfficiencyForPreset(vehiclePresetId)
        set(value) = setAcEfficiencyForPreset(vehiclePresetId, value)

    var dcEfficiency: Float
        get() = getDcEfficiencyForPreset(vehiclePresetId)
        set(value) = setDcEfficiencyForPreset(vehiclePresetId, value)

    var costPerKWh: Float
        get() = prefs.getFloat("pref_cost_per_kwh", 0.35f)
        set(value) = prefs.edit().putFloat("pref_cost_per_kwh", value).apply()

    var wearCostPer1600: Float
        get() = getWearCostPer1600ForPreset(vehiclePresetId)
        set(value) = setWearCostPer1600ForPreset(vehiclePresetId, value)

    val effectiveCapacity: Float
        get() = batteryNominalKwh * (1.0f - batteryDegradation / 100.0f)

    fun applyPreset(preset: VehiclePreset) {
        prefs.edit().apply {
            putString("pref_vehicle_preset", preset.id)
            putFloat("pref_battery_nominal_kwh", getBatteryNominalKwhForPreset(preset.id))
            putFloat("pref_ac_efficiency", getAcEfficiencyForPreset(preset.id))
            putFloat("pref_dc_efficiency", getDcEfficiencyForPreset(preset.id))
            apply()
        }
    }

    // Preset Overrides Helper Methods
    fun getBatteryNominalKwhForPreset(presetId: String): Float {
        val preset = VehiclePreset.fromId(presetId)
        val key = "pref_${presetId}_battery_nominal_kwh"
        return if (prefs.contains(key)) {
            prefs.getFloat(key, preset.nominalKwh)
        } else {
            if (presetId == "custom") prefs.getFloat("pref_battery_nominal_kwh", 70.0f) else preset.nominalKwh
        }
    }

    fun setBatteryNominalKwhForPreset(presetId: String, value: Float) {
        val key = "pref_${presetId}_battery_nominal_kwh"
        prefs.edit().putFloat(key, value).apply()
        // Always keep legacy key in sync if this is the active preset
        if (presetId == vehiclePresetId) {
            prefs.edit().putFloat("pref_battery_nominal_kwh", value).apply()
        }
    }

    fun getAcEfficiencyForPreset(presetId: String): Float {
        val preset = VehiclePreset.fromId(presetId)
        val key = "pref_${presetId}_ac_efficiency"
        return if (prefs.contains(key)) {
            prefs.getFloat(key, preset.acEfficiency)
        } else {
            if (presetId == "custom") prefs.getFloat("pref_ac_efficiency", 0.90f) else preset.acEfficiency
        }
    }

    fun setAcEfficiencyForPreset(presetId: String, value: Float) {
        val key = "pref_${presetId}_ac_efficiency"
        prefs.edit().putFloat(key, value).apply()
        if (presetId == vehiclePresetId) {
            prefs.edit().putFloat("pref_ac_efficiency", value).apply()
        }
    }

    fun getDcEfficiencyForPreset(presetId: String): Float {
        val preset = VehiclePreset.fromId(presetId)
        val key = "pref_${presetId}_dc_efficiency"
        return if (prefs.contains(key)) {
            prefs.getFloat(key, preset.dcEfficiency)
        } else {
            if (presetId == "custom") prefs.getFloat("pref_dc_efficiency", 0.90f) else preset.dcEfficiency
        }
    }

    fun setDcEfficiencyForPreset(presetId: String, value: Float) {
        val key = "pref_${presetId}_dc_efficiency"
        prefs.edit().putFloat(key, value).apply()
        if (presetId == vehiclePresetId) {
            prefs.edit().putFloat("pref_dc_efficiency", value).apply()
        }
    }

    fun getWearCostPer1600ForPreset(presetId: String): Float {
        val key = "pref_${presetId}_wear_cost_per_1600"
        return if (prefs.contains(key)) {
            prefs.getFloat(key, 60.0f)
        } else {
            if (presetId == "custom") prefs.getFloat("pref_wear_cost_per_1600", 60.0f) else 60.0f
        }
    }

    fun setWearCostPer1600ForPreset(presetId: String, value: Float) {
        val key = "pref_${presetId}_wear_cost_per_1600"
        prefs.edit().putFloat(key, value).apply()
        if (presetId == vehiclePresetId) {
            prefs.edit().putFloat("pref_wear_cost_per_1600", value).apply()
        }
    }

    // Charger-Specific Efficiencies
    fun getChargerKey(chargerType: String): String {
        return when (chargerType) {
            "Schuko (2.3 kW)" -> "schuko"
            "Wallbox AC 11 kW" -> "wallbox_ac_11kw"
            "Wallbox AC 22 kW" -> "wallbox_ac_22kw"
            "DC Schnelllader 50 kW" -> "dc_50kw"
            "Benutzerdefiniert" -> "custom"
            else -> chargerType.lowercase(java.util.Locale.US).replace(Regex("[^a-z0-9_]"), "_")
        }
    }

    fun isChargerCalibrated(vehicleId: String, chargerType: String): Boolean {
        val key = "pref_${vehicleId}_efficiency_${getChargerKey(chargerType)}"
        return prefs.contains(key)
    }

    fun clearChargerEfficiency(vehicleId: String, chargerType: String) {
        val key = "pref_${vehicleId}_efficiency_${getChargerKey(chargerType)}"
        prefs.edit().remove(key).apply()
    }

    fun getChargerEfficiency(vehicleId: String, chargerType: String, isAc: Boolean = true): Float {
        val key = "pref_${vehicleId}_efficiency_${getChargerKey(chargerType)}"
        if (prefs.contains(key)) {
            return prefs.getFloat(key, 0.90f)
        }
        return when (chargerType) {
            "Schuko (2.3 kW)" -> 0.85f
            "Wallbox AC 11 kW", "Wallbox AC 22 kW" -> getAcEfficiencyForPreset(vehicleId)
            "DC Schnelllader 50 kW" -> getDcEfficiencyForPreset(vehicleId)
            "Benutzerdefiniert" -> if (isAc) getAcEfficiencyForPreset(vehicleId) else getDcEfficiencyForPreset(vehicleId)
            else -> if (isAc) getAcEfficiencyForPreset(vehicleId) else getDcEfficiencyForPreset(vehicleId)
        }
    }

    fun setChargerEfficiency(vehicleId: String, chargerType: String, efficiency: Float) {
        val key = "pref_${vehicleId}_efficiency_${getChargerKey(chargerType)}"
        prefs.edit().putFloat(key, efficiency).apply()
    }
}

