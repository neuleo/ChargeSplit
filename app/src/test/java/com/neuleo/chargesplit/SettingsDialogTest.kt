package com.neuleo.chargesplit

import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsDialogTest {
    @Test
    fun testSettingsDialogMockFlow() {
        val fakePrefs = PreferencesManagerTest.FakeSharedPreferences()
        val manager = PreferencesManager(fakePrefs)

        // Mocking what the dialog does when saving overrides for Tesla Model S P85
        manager.vehiclePresetId = "tesla_s_p85"
        manager.batteryNominalKwh = 88.0f
        manager.acEfficiency = 0.92f
        manager.dcEfficiency = 0.94f

        assertEquals("tesla_s_p85", manager.vehiclePresetId)
        assertEquals(88.0f, manager.batteryNominalKwh)
        assertEquals(0.92f, manager.acEfficiency)
        assertEquals(0.94f, manager.dcEfficiency)
    }
}
