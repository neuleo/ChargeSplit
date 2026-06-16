package com.neuleo.chargesplit

import android.content.SharedPreferences
import com.neuleo.chargesplit.model.VehiclePreset

class PreferencesManager(private val prefs: SharedPreferences) {

    var vehiclePresetId: String
        get() = prefs.getString("pref_vehicle_preset", "custom") ?: "custom"
        set(value) = prefs.edit().putString("pref_vehicle_preset", value).apply()

    var batteryNominalKwh: Float
        get() = prefs.getFloat("pref_battery_nominal_kwh", 70.0f)
        set(value) = prefs.edit().putFloat("pref_battery_nominal_kwh", value).apply()

    var batteryDegradation: Float
        get() = prefs.getFloat("pref_battery_degradation", 0.0f)
        set(value) = prefs.edit().putFloat("pref_battery_degradation", value).apply()

    var acEfficiency: Float
        get() = prefs.getFloat("pref_ac_efficiency", 0.90f)
        set(value) = prefs.edit().putFloat("pref_ac_efficiency", value).apply()

    var dcEfficiency: Float
        get() = prefs.getFloat("pref_dc_efficiency", 0.90f)
        set(value) = prefs.edit().putFloat("pref_dc_efficiency", value).apply()

    var costPerKWh: Float
        get() = prefs.getFloat("pref_cost_per_kwh", 0.35f)
        set(value) = prefs.edit().putFloat("pref_cost_per_kwh", value).apply()

    var wearCostPer1600: Float
        get() = prefs.getFloat("pref_wear_cost_per_1600", 60.0f)
        set(value) = prefs.edit().putFloat("pref_wear_cost_per_1600", value).apply()

    val effectiveCapacity: Float
        get() = batteryNominalKwh * (1.0f - batteryDegradation / 100.0f)

    fun applyPreset(preset: VehiclePreset) {
        prefs.edit().apply {
            putString("pref_vehicle_preset", preset.id)
            if (preset != VehiclePreset.CUSTOM) {
                putFloat("pref_battery_nominal_kwh", preset.nominalKwh)
                putFloat("pref_ac_efficiency", preset.acEfficiency)
                putFloat("pref_dc_efficiency", preset.dcEfficiency)
            }
            apply()
        }
    }
}
