package com.neuleo.chargesplit.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VehiclePresetTest {

    @Test
    fun testTeslaModelS90DPreset() {
        val preset = VehiclePreset.TESLA_MODEL_S_90D
        assertEquals("Tesla Model S 90D", preset.displayName)
        assertEquals("Model S 90D", preset.shortName)
        assertEquals(85.5f, preset.defaultCapacityKwh)
        assertEquals(85.5f, preset.nominalKwh)
        assertEquals(3, preset.maxPhases)
        assertEquals(16, preset.maxAmps)
        assertEquals(11.0f, preset.maxAcKw)
        assertEquals(120.0f, preset.maxDcKw)
        assertEquals(0.91f, preset.acEfficiency)
        assertEquals(0.93f, preset.dcEfficiency)
        assertFalse(preset.isSinglePhaseAc)
    }

    @Test
    fun testLeapmotorT03Preset() {
        val preset = VehiclePreset.LEAPMOTOR_T03
        assertEquals("Leapmotor T03", preset.displayName)
        assertEquals("Leapmotor T03", preset.shortName)
        assertEquals(37.3f, preset.defaultCapacityKwh)
        assertEquals(37.3f, preset.nominalKwh)
        assertEquals(1, preset.maxPhases)
        assertEquals(32, preset.maxAmps)
        assertEquals(6.6f, preset.maxAcKw)
        assertEquals(48.0f, preset.maxDcKw)
        assertEquals(0.90f, preset.acEfficiency)
        assertEquals(0.92f, preset.dcEfficiency)
        assertTrue(preset.isSinglePhaseAc)
    }

    @Test
    fun testCustomPreset() {
        val preset = VehiclePreset.CUSTOM
        assertEquals("Benutzerdefiniert", preset.displayName)
        assertEquals("Custom", preset.shortName)
        assertEquals(70.0f, preset.defaultCapacityKwh)
        assertEquals(3, preset.maxPhases)
        assertEquals(32, preset.maxAmps)
        assertEquals(22.0f, preset.maxAcKw)
    }

    @Test
    fun testMigrationFromName() {
        assertEquals(VehiclePreset.TESLA_MODEL_S_90D, VehiclePreset.fromName("TESLA_MODEL_S_P85"))
        assertEquals(VehiclePreset.TESLA_MODEL_S_90D, VehiclePreset.fromName("tesla_s_p85"))
        assertEquals(VehiclePreset.TESLA_MODEL_S_90D, VehiclePreset.fromName("TESLA_S_P85"))
        assertEquals(VehiclePreset.TESLA_MODEL_S_90D, VehiclePreset.fromName(null))
        assertEquals(VehiclePreset.LEAPMOTOR_T03, VehiclePreset.fromName("LEAPMOTOR_T03"))
        assertEquals(VehiclePreset.LEAPMOTOR_T03, VehiclePreset.fromName("leapmotor_t03"))
        assertEquals(VehiclePreset.CUSTOM, VehiclePreset.fromName("CUSTOM"))
        assertEquals(VehiclePreset.CUSTOM, VehiclePreset.fromName("custom"))
    }
}
