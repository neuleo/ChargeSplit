package com.neuleo.chargesplit

import com.neuleo.chargesplit.model.VehiclePreset
import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsDialogTest {
    @Test
    fun testSettingsDialogMockFlow() {
        val fakePrefs = PreferencesManagerTest.FakeSharedPreferences()
        val manager = PreferencesManager(fakePrefs)

        manager.selectedPreset = VehiclePreset.TESLA_MODEL_S_90D
        manager.setBatteryCapacityForPreset(VehiclePreset.TESLA_MODEL_S_90D, 88.0f)
        manager.setAcEfficiencyForPreset(VehiclePreset.TESLA_MODEL_S_90D.id, 0.92f)
        manager.setDcEfficiencyForPreset(VehiclePreset.TESLA_MODEL_S_90D.id, 0.94f)

        assertEquals(VehiclePreset.TESLA_MODEL_S_90D, manager.selectedPreset)
        assertEquals(88.0f, manager.getBatteryCapacityForPreset(VehiclePreset.TESLA_MODEL_S_90D))
        assertEquals(0.92f, manager.getAcEfficiencyForPreset(VehiclePreset.TESLA_MODEL_S_90D.id))
        assertEquals(0.94f, manager.getDcEfficiencyForPreset(VehiclePreset.TESLA_MODEL_S_90D.id))
    }
}
