package com.neuleo.chargesplit.model

import org.junit.Assert.assertEquals
import org.junit.Test

class VehiclePresetTest {

    @Test
    fun testTeslaSP85Preset() {
        val preset = VehiclePreset.TESLA_S_P85
        assertEquals(85.0f, preset.nominalKwh)
        assertEquals(80.8f, preset.usableKwh)
        assertEquals(22.0f, preset.maxAcKw)
        assertEquals(120.0f, preset.maxDcKw)
        assertEquals(0.91f, preset.acEfficiency)
        assertEquals(0.93f, preset.dcEfficiency)
        org.junit.Assert.assertFalse(preset.isSinglePhaseAc)
    }

    @Test
    fun testLeapmotorT03Preset() {
        val preset = VehiclePreset.LEAPMOTOR_T03
        assertEquals(37.3f, preset.nominalKwh)
        assertEquals(36.0f, preset.usableKwh)
        assertEquals(6.6f, preset.maxAcKw)
        assertEquals(48.0f, preset.maxDcKw)
        assertEquals(0.90f, preset.acEfficiency)
        assertEquals(0.92f, preset.dcEfficiency)
        org.junit.Assert.assertTrue(preset.isSinglePhaseAc)
    }

    @Test
    fun testCustomPreset() {
        val preset = VehiclePreset.CUSTOM
        // Check fields are present, values can be default
        assertEquals(70.0f, preset.nominalKwh)
        assertEquals(11.0f, preset.maxAcKw)
    }
}
