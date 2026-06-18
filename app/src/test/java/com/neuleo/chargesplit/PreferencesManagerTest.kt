package com.neuleo.chargesplit

import android.content.SharedPreferences
import com.neuleo.chargesplit.model.VehiclePreset
import org.junit.Assert.assertEquals
import org.junit.Test

class PreferencesManagerTest {

    // Simple fake SharedPreferences for unit testing without Android runtime dependencies
    class FakeSharedPreferences : SharedPreferences {
        val map = mutableMapOf<String, Any>()

        override fun getAll(): MutableMap<String, *> = map
        override fun getString(key: String?, defValue: String?): String? = map[key] as? String ?: defValue
        override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? = map[key] as? MutableSet<String> ?: defValues
        override fun getInt(key: String?, defValue: Int): Int = map[key] as? Int ?: defValue
        override fun getLong(key: String?, defValue: Long): Long = map[key] as? Long ?: defValue
        override fun getFloat(key: String?, defValue: Float): Float = map[key] as? Float ?: defValue
        override fun getBoolean(key: String?, defValue: Boolean): Boolean = map[key] as? Boolean ?: defValue
        override fun contains(key: String?): Boolean = map.containsKey(key)
        override fun edit(): SharedPreferences.Editor = FakeEditor(this)
        override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}
        override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}

        class FakeEditor(private val fakePrefs: FakeSharedPreferences) : SharedPreferences.Editor {
            private val tempMap = mutableMapOf<String, Any>()
            private val removedKeys = mutableSetOf<String>()

            override fun putString(key: String, value: String?): SharedPreferences.Editor {
                if (value != null) {
                    tempMap[key] = value
                    removedKeys.remove(key)
                } else {
                    removedKeys.add(key)
                    tempMap.remove(key)
                }
                return this
            }
            override fun putStringSet(key: String, values: MutableSet<String>?): SharedPreferences.Editor {
                if (values != null) {
                    tempMap[key] = values
                    removedKeys.remove(key)
                } else {
                    removedKeys.add(key)
                    tempMap.remove(key)
                }
                return this
            }
            override fun putInt(key: String, value: Int): SharedPreferences.Editor {
                tempMap[key] = value
                removedKeys.remove(key)
                return this
            }
            override fun putLong(key: String, value: Long): SharedPreferences.Editor {
                tempMap[key] = value
                removedKeys.remove(key)
                return this
            }
            override fun putFloat(key: String, value: Float): SharedPreferences.Editor {
                tempMap[key] = value
                removedKeys.remove(key)
                return this
            }
            override fun putBoolean(key: String, value: Boolean): SharedPreferences.Editor {
                tempMap[key] = value
                removedKeys.remove(key)
                return this
            }
            override fun remove(key: String): SharedPreferences.Editor {
                removedKeys.add(key)
                tempMap.remove(key)
                return this
            }
            override fun clear(): SharedPreferences.Editor {
                removedKeys.addAll(fakePrefs.map.keys)
                tempMap.clear()
                return this
            }
            override fun commit(): Boolean {
                for (k in removedKeys) {
                    fakePrefs.map.remove(k)
                }
                fakePrefs.map.putAll(tempMap)
                removedKeys.clear()
                tempMap.clear()
                return true
            }
            override fun apply() {
                commit()
            }
        }
    }

    @Test
    fun testDefaultValues() {
        val fakePrefs = FakeSharedPreferences()
        val manager = PreferencesManager(fakePrefs)

        assertEquals("custom", manager.vehiclePresetId)
        assertEquals(70.0f, manager.batteryNominalKwh)
        assertEquals(0.0f, manager.batteryDegradation)
        assertEquals(0.90f, manager.acEfficiency)
        assertEquals(0.90f, manager.dcEfficiency)
        assertEquals(0.35f, manager.costPerKWh)
    }

    @Test
    fun testPersistenceRoundTrip() {
        val fakePrefs = FakeSharedPreferences()
        val manager = PreferencesManager(fakePrefs)

        manager.vehiclePresetId = "tesla_s_p85"
        manager.batteryNominalKwh = 85.0f
        manager.batteryDegradation = 5.0f
        manager.acEfficiency = 0.91f
        manager.dcEfficiency = 0.93f
        manager.costPerKWh = 0.40f

        assertEquals("tesla_s_p85", manager.vehiclePresetId)
        assertEquals(85.0f, manager.batteryNominalKwh)
        assertEquals(5.0f, manager.batteryDegradation)
        assertEquals(0.91f, manager.acEfficiency)
        assertEquals(0.93f, manager.dcEfficiency)
        assertEquals(0.40f, manager.costPerKWh)

        // Verify they are written to the expected preference keys
        assertEquals("tesla_s_p85", fakePrefs.getString("pref_vehicle_preset", null))
        assertEquals(85.0f, fakePrefs.getFloat("pref_battery_nominal_kwh", 0f))
        assertEquals(5.0f, fakePrefs.getFloat("pref_battery_degradation", 0f))
        assertEquals(0.91f, fakePrefs.getFloat("pref_ac_efficiency", 0f))
        assertEquals(0.93f, fakePrefs.getFloat("pref_dc_efficiency", 0f))
    }

    @Test
    fun testEffectiveCapacityCalculation() {
        val fakePrefs = FakeSharedPreferences()
        val manager = PreferencesManager(fakePrefs)

        manager.batteryNominalKwh = 85.0f
        manager.batteryDegradation = 5.0f
        // 85 * (1 - 0.05) = 80.75
        assertEquals(80.75f, manager.effectiveCapacity)

        manager.batteryNominalKwh = 100.0f
        manager.batteryDegradation = 10.0f
        assertEquals(90.0f, manager.effectiveCapacity)
    }

    @Test
    fun testPresetAutoFill() {
        val fakePrefs = FakeSharedPreferences()
        val manager = PreferencesManager(fakePrefs)

        // Select Tesla preset
        manager.applyPreset(VehiclePreset.TESLA_S_P85)

        assertEquals("tesla_s_p85", manager.vehiclePresetId)
        assertEquals(85.0f, manager.batteryNominalKwh)
        assertEquals(0.91f, manager.acEfficiency)
        assertEquals(0.93f, manager.dcEfficiency)
    }

    @Test
    fun testChargerSpecificEfficiencies() {
        val fakePrefs = FakeSharedPreferences()
        val manager = PreferencesManager(fakePrefs)

        // Default values for custom vehicle
        assertEquals(0.85f, manager.getChargerEfficiency("custom", "Schuko (2.3 kW)", true))
        assertEquals(0.90f, manager.getChargerEfficiency("custom", "Wallbox AC 11 kW", true))
        assertEquals(0.90f, manager.getChargerEfficiency("custom", "Benutzerdefiniert", true))

        // Default values for Tesla Model S P85
        assertEquals(0.85f, manager.getChargerEfficiency("tesla_s_p85", "Schuko (2.3 kW)", true))
        assertEquals(0.91f, manager.getChargerEfficiency("tesla_s_p85", "Wallbox AC 11 kW", true))
        assertEquals(0.93f, manager.getChargerEfficiency("tesla_s_p85", "DC Schnelllader 50 kW", false))

        // Saving and loading custom efficiencies
        manager.setChargerEfficiency("tesla_s_p85", "Wallbox AC 11 kW", 0.88f)
        assertEquals(0.88f, manager.getChargerEfficiency("tesla_s_p85", "Wallbox AC 11 kW", true))
        // SharedPreferences storage check
        assertEquals(0.88f, fakePrefs.getFloat("pref_tesla_s_p85_efficiency_wallbox_ac_11kw", 0f))
    }

    @Test
    fun testPresetOverrides() {
        val fakePrefs = FakeSharedPreferences()
        val manager = PreferencesManager(fakePrefs)

        // Select Tesla preset, verify it has default values initially
        manager.vehiclePresetId = "tesla_s_p85"
        assertEquals(85.0f, manager.batteryNominalKwh)
        assertEquals(0.91f, manager.acEfficiency)

        // Set overrides for Tesla
        manager.batteryNominalKwh = 83.0f
        manager.acEfficiency = 0.89f

        // Verify they are returned from getters
        assertEquals(83.0f, manager.batteryNominalKwh)
        assertEquals(0.89f, manager.acEfficiency)

        // Verify the keys in fakePrefs
        assertEquals(83.0f, fakePrefs.getFloat("pref_tesla_s_p85_battery_nominal_kwh", 0f))
        assertEquals(0.89f, fakePrefs.getFloat("pref_tesla_s_p85_ac_efficiency", 0f))

        // Change preset to Leapmotor, verify Leapmotor defaults are returned
        manager.vehiclePresetId = "leapmotor_t03"
        assertEquals(37.3f, manager.batteryNominalKwh)
        assertEquals(0.90f, manager.acEfficiency)

        // Switch back to Tesla, verify the overrides are still there
        manager.vehiclePresetId = "tesla_s_p85"
        assertEquals(83.0f, manager.batteryNominalKwh)
        assertEquals(0.89f, manager.acEfficiency)
    }

    @Test
    fun testCalibrationStatusAndClear() {
        val fakePrefs = FakeSharedPreferences()
        val manager = PreferencesManager(fakePrefs)

        // Initially not calibrated
        org.junit.Assert.assertFalse(manager.isChargerCalibrated("tesla_s_p85", "Wallbox AC 11 kW"))

        // Set efficiency -> should be calibrated
        manager.setChargerEfficiency("tesla_s_p85", "Wallbox AC 11 kW", 0.88f)
        org.junit.Assert.assertTrue(manager.isChargerCalibrated("tesla_s_p85", "Wallbox AC 11 kW"))

        // Clear efficiency -> should not be calibrated anymore
        manager.clearChargerEfficiency("tesla_s_p85", "Wallbox AC 11 kW")
        org.junit.Assert.assertFalse(manager.isChargerCalibrated("tesla_s_p85", "Wallbox AC 11 kW"))
    }
}

