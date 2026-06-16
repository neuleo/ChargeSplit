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

            override fun putString(key: String, value: String?): SharedPreferences.Editor {
                if (value != null) tempMap[key] = value else tempMap.remove(key)
                return this
            }
            override fun putStringSet(key: String, values: MutableSet<String>?): SharedPreferences.Editor {
                if (values != null) tempMap[key] = values else tempMap.remove(key)
                return this
            }
            override fun putInt(key: String, value: Int): SharedPreferences.Editor {
                tempMap[key] = value
                return this
            }
            override fun putLong(key: String, value: Long): SharedPreferences.Editor {
                tempMap[key] = value
                return this
            }
            override fun putFloat(key: String, value: Float): SharedPreferences.Editor {
                tempMap[key] = value
                return this
            }
            override fun putBoolean(key: String, value: Boolean): SharedPreferences.Editor {
                tempMap[key] = value
                return this
            }
            override fun remove(key: String): SharedPreferences.Editor {
                tempMap.remove(key) // In simple fake we don't handle removal markers, but this is enough
                return this
            }
            override fun clear(): SharedPreferences.Editor {
                tempMap.clear()
                return this
            }
            override fun commit(): Boolean {
                fakePrefs.map.putAll(tempMap)
                return true
            }
            override fun apply() {
                fakePrefs.map.putAll(tempMap)
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
}
