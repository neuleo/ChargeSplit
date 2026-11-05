package com.neuleo.chargesplit

import android.content.SharedPreferences
import com.neuleo.chargesplit.model.VehiclePreset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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

        assertEquals(VehiclePreset.TESLA_MODEL_S_90D, manager.selectedPreset)
        assertEquals(85.5f, manager.batteryNominalKwh)
        assertEquals(0.0f, manager.batteryDegradation)
        assertEquals(0.91f, manager.acEfficiency)
        assertEquals(0.93f, manager.dcEfficiency)
        assertEquals(0.35f, manager.costPerKWh)
    }

    @Test
    fun testMigrationFromLegacyPresetKey() {
        val fakePrefs = FakeSharedPreferences()
        fakePrefs.map["pref_vehicle_preset"] = "tesla_s_p85"
        val manager = PreferencesManager(fakePrefs)

        assertEquals(VehiclePreset.TESLA_MODEL_S_90D, manager.selectedPreset)
        assertEquals(85.5f, manager.batteryNominalKwh)
    }

    @Test
    fun testPersistenceRoundTrip() {
        val fakePrefs = FakeSharedPreferences()
        val manager = PreferencesManager(fakePrefs)

        manager.selectedPreset = VehiclePreset.TESLA_MODEL_S_90D
        manager.batteryNominalKwh = 85.5f
        manager.batteryDegradation = 5.0f
        manager.acEfficiency = 0.91f
        manager.dcEfficiency = 0.93f
        manager.costPerKWh = 0.40f

        assertEquals(VehiclePreset.TESLA_MODEL_S_90D, manager.selectedPreset)
        assertEquals(85.5f, manager.batteryNominalKwh)
        assertEquals(5.0f, manager.batteryDegradation)
        assertEquals(0.91f, manager.acEfficiency)
        assertEquals(0.93f, manager.dcEfficiency)
        assertEquals(0.40f, manager.costPerKWh)

        assertEquals("TESLA_MODEL_S_90D", fakePrefs.getString("selected_vehicle_preset", null))
        assertEquals(85.5f, fakePrefs.getFloat("battery_capacity_TESLA_MODEL_S_90D", 0f))
        assertEquals(5.0f, fakePrefs.getFloat("pref_battery_degradation", 0f))
        assertEquals(0.40f, fakePrefs.getFloat("electricity_price", 0f))
    }

    @Test
    fun testPerVehicleChargingStatePersistence() {
        val fakePrefs = FakeSharedPreferences()
        val manager = PreferencesManager(fakePrefs)

        // Save Tesla State
        val teslaState = VehicleChargingState(
            currentSoc = 15f,
            targetSoc = 85f,
            chargingMode = "16A",
            customAmps = 16,
            customPhases = 3,
            capacityKwh = 85.5f
        )
        manager.saveVehicleChargingState(VehiclePreset.TESLA_MODEL_S_90D, teslaState)

        // Save Leapmotor State
        val t03State = VehicleChargingState(
            currentSoc = 30f,
            targetSoc = 90f,
            chargingMode = "32A",
            customAmps = 32,
            customPhases = 1,
            capacityKwh = 37.3f
        )
        manager.saveVehicleChargingState(VehiclePreset.LEAPMOTOR_T03, t03State)

        // Verify Tesla state loaded
        val loadedTesla = manager.getVehicleChargingState(VehiclePreset.TESLA_MODEL_S_90D)
        assertEquals(15f, loadedTesla.currentSoc)
        assertEquals(85f, loadedTesla.targetSoc)
        assertEquals("16A", loadedTesla.chargingMode)
        assertEquals(16, loadedTesla.customAmps)
        assertEquals(3, loadedTesla.customPhases)
        assertEquals(85.5f, loadedTesla.capacityKwh)

        // Verify Leapmotor state loaded
        val loadedT03 = manager.getVehicleChargingState(VehiclePreset.LEAPMOTOR_T03)
        assertEquals(30f, loadedT03.currentSoc)
        assertEquals(90f, loadedT03.targetSoc)
        assertEquals("32A", loadedT03.chargingMode)
        assertEquals(32, loadedT03.customAmps)
        assertEquals(1, loadedT03.customPhases)
        assertEquals(37.3f, loadedT03.capacityKwh)
    }

    @Test
    fun testEffectiveCapacityCalculation() {
        val fakePrefs = FakeSharedPreferences()
        val manager = PreferencesManager(fakePrefs)

        manager.batteryNominalKwh = 85.5f
        manager.batteryDegradation = 10.0f
        // 85.5 * 0.9 = 76.95
        assertEquals(76.95f, manager.effectiveCapacity, 0.01f)
    }

    @Test
    fun testPresetAutoFill() {
        val fakePrefs = FakeSharedPreferences()
        val manager = PreferencesManager(fakePrefs)

        manager.applyPreset(VehiclePreset.LEAPMOTOR_T03)

        assertEquals(VehiclePreset.LEAPMOTOR_T03, manager.selectedPreset)
        assertEquals(37.3f, manager.batteryNominalKwh)
        assertEquals(0.90f, manager.acEfficiency)
        assertEquals(0.92f, manager.dcEfficiency)
    }

    @Test
    fun testChargerSpecificEfficiencies() {
        val fakePrefs = FakeSharedPreferences()
        val manager = PreferencesManager(fakePrefs)

        assertEquals(0.85f, manager.getChargerEfficiency("custom", "Schuko (10A)", true))
        assertEquals(0.90f, manager.getChargerEfficiency("custom", "16A / 11 kW", true))

        assertEquals(0.85f, manager.getChargerEfficiency("TESLA_MODEL_S_90D", "Schuko (10A)", true))
        assertEquals(0.91f, manager.getChargerEfficiency("TESLA_MODEL_S_90D", "16A / 11 kW", true))

        manager.setChargerEfficiency("TESLA_MODEL_S_90D", "16A / 11 kW", 0.88f)
        assertEquals(0.88f, manager.getChargerEfficiency("TESLA_MODEL_S_90D", "16A / 11 kW", true))
    }

    @Test
    fun testCalibrationStatusAndClear() {
        val fakePrefs = FakeSharedPreferences()
        val manager = PreferencesManager(fakePrefs)

        assertFalse(manager.isChargerCalibrated("TESLA_MODEL_S_90D", "16A / 11 kW"))

        manager.setChargerEfficiency("TESLA_MODEL_S_90D", "16A / 11 kW", 0.88f)
        assertTrue(manager.isChargerCalibrated("TESLA_MODEL_S_90D", "16A / 11 kW"))

        manager.clearChargerEfficiency("TESLA_MODEL_S_90D", "16A / 11 kW")
        assertFalse(manager.isChargerCalibrated("TESLA_MODEL_S_90D", "16A / 11 kW"))
    }
}
