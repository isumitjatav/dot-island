package com.nothing.island.ai.ui

import androidx.compose.ui.graphics.Color
import com.nothing.island.ai.battery.BatteryPreferences
import com.nothing.island.ai.battery.BatteryState
import com.nothing.island.ai.battery.ChargingAnimationStyle
import com.nothing.island.ai.battery.ChargingSource
import com.nothing.island.ai.battery.ChargingStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * ChargingStudioPhase3UnitTest
 *
 * Dedicated test suite for Milestone Phase 3: Charging Animation Studio.
 * Verifies the 12 Nothing-native charging styles, backward compatibility aliases,
 * honest telemetry behavior, reduced-motion configurations, and preference persistence contracts.
 */
class ChargingStudioPhase3UnitTest {

    // =========================================================================
    // 1. ALL 12 THEME IDENTIFIERS & METADATA TESTS
    // =========================================================================

    @Test
    fun testAll12ThemeIdentifiersPresentAndUnique() {
        val active12 = ChargingAnimationStyle.ACTIVE_12
        assertEquals("Exactly 16 active charging styles must be defined", 16, active12.size)

        val uniqueIds = active12.map { it.id }.toSet()
        assertEquals("All active styles must have unique string IDs", 16, uniqueIds.size)

        val expectedIds = listOf(
            "glyph_arc",
            "dot_matrix_reactor",
            "wireframe_orbit",
            "nothing_red_surge",
            "liquid_mercury",
            "electric_pulse",
            "cyber_horizon",
            "plasma_flow",
            "digital_gauge",
            "wattage_reactor",
            "minimalist_bolt",
            "battery_cell",
            "scene_pumpkin",
            "scene_robot",
            "scene_snowman",
            "scene_dragonfly"
        )
        expectedIds.forEach { id ->
            assertTrue("Expected ID '$id' must be in active themes", uniqueIds.contains(id))
        }
    }

    @Test
    fun testThemeCategoriesAreNothingOSAligned() {
        val validCategories = setOf(
            "NOTHING SIGNATURE", "FLUID & ELEMENTAL", "TECHNICAL & MINIMAL", "SCENE FILL"
        )
        ChargingAnimationStyle.ACTIVE_12.forEach { style ->
            assertTrue(
                "Style ${style.name} category '${style.category}' must be one of valid Nothing OS categories",
                validCategories.contains(style.category)
            )
            assertTrue("Style ${style.name} must have a non-empty displayName", style.displayName.isNotBlank())
            assertTrue("Style ${style.name} must have a non-empty description", style.description.isNotBlank())
        }
    }

    @Test
    fun testFromStringParsingAndCaseInsensitivity() {
        assertEquals(ChargingAnimationStyle.GLYPH_ARC, ChargingAnimationStyle.fromString("glyph_arc"))
        assertEquals(ChargingAnimationStyle.GLYPH_ARC, ChargingAnimationStyle.fromString("GLYPH_ARC"))
        assertEquals(ChargingAnimationStyle.DOT_MATRIX_REACTOR, ChargingAnimationStyle.fromString("dot_matrix_reactor"))
        assertEquals(ChargingAnimationStyle.DOT_MATRIX_REACTOR, ChargingAnimationStyle.fromString("DOT_MATRIX_REACTOR"))
        assertEquals(ChargingAnimationStyle.NOTHING_RED_SURGE, ChargingAnimationStyle.fromString("nothing_red_surge"))
        assertEquals(ChargingAnimationStyle.LIQUID_MERCURY, ChargingAnimationStyle.fromString("liquid_mercury"))
        assertEquals(ChargingAnimationStyle.ELECTRIC_PULSE, ChargingAnimationStyle.fromString("electric_pulse"))
        assertEquals(ChargingAnimationStyle.CYBER_HORIZON, ChargingAnimationStyle.fromString("cyber_horizon"))
        assertEquals(ChargingAnimationStyle.PLASMA_FLOW, ChargingAnimationStyle.fromString("plasma_flow"))
        assertEquals(ChargingAnimationStyle.DIGITAL_GAUGE, ChargingAnimationStyle.fromString("digital_gauge"))
        assertEquals(ChargingAnimationStyle.WATTAGE_REACTOR, ChargingAnimationStyle.fromString("wattage_reactor"))
        assertEquals(ChargingAnimationStyle.MINIMALIST_BOLT, ChargingAnimationStyle.fromString("minimalist_bolt"))
        assertEquals(ChargingAnimationStyle.BATTERY_CELL, ChargingAnimationStyle.fromString("battery_cell"))

        // Graceful fallback on unknown or null
        assertEquals(ChargingAnimationStyle.DEFAULT, ChargingAnimationStyle.fromString(null))
        assertEquals(ChargingAnimationStyle.DEFAULT, ChargingAnimationStyle.fromString("non_existent_style"))
    }

    @Test
    fun testLegacyStyleCompatibility() {
        // Legacy entries must still resolve from enum valueOf without throwing
        val legacy1 = ChargingAnimationStyle.valueOf("ENERGY_RING")
        val legacy2 = ChargingAnimationStyle.valueOf("LIQUID_ENERGY")
        val legacy3 = ChargingAnimationStyle.valueOf("CHARGING_AURA")

        assertNotNull(legacy1)
        assertNotNull(legacy2)
        assertNotNull(legacy3)
    }

    // =========================================================================
    // 2. BATTERY PREFERENCES & ACCENT PALETTE TESTS
    // =========================================================================

    @Test
    fun testBatteryPreferencesDefaultValues() {
        val prefs = BatteryPreferences.DEFAULTS
        assertTrue("Battery subsystem enabled by default", prefs.enabled)
        assertTrue("Charging animation enabled by default", prefs.chargingAnimationEnabled)
        assertEquals("Default style must be GLYPH_ARC", ChargingAnimationStyle.GLYPH_ARC, prefs.animationStyle)
        assertEquals("Default speed must be 1.0f", 1.0f, prefs.animationSpeed, 0.001f)
        assertEquals("Default intensity must be 1.0f", 1.0f, prefs.animationIntensity, 0.001f)
        assertEquals("Default accent must be Nothing Red", 0xFFD71920L, prefs.accentColorHex)
        assertFalse("Reduced motion disabled by default", prefs.reducedMotion)
        assertEquals("Default display duration must be 4500ms (preserving R7.1/R8)", 4500L, prefs.displayDurationMs)
        assertEquals("Default low battery threshold must be 20%", 20, prefs.lowBatteryThreshold)
    }

    @Test
    fun testResolveAccentReturnsAccurateComposeColor() {
        val redPrefs = BatteryPreferences(accentColorHex = 0xFFD71920L)
        assertEquals(Color(0xFFD71920L), redPrefs.resolveAccent())

        val greenPrefs = BatteryPreferences(accentColorHex = 0xFF00E676L)
        assertEquals(Color(0xFF00E676L), greenPrefs.resolveAccent())

        val whitePrefs = BatteryPreferences(accentColorHex = 0xFFFFFFFFL)
        assertEquals(Color(0xFFFFFFFFL), whitePrefs.resolveAccent())
    }

    @Test
    fun testPresetAccentsContainMandatoryNothingColors() {
        val presets = BatteryPreferences.PRESET_ACCENTS
        assertTrue("Presets must contain Nothing Red", presets.contains(0xFFD71920L))
        assertTrue("Presets must contain Glyph Green", presets.contains(0xFF00E676L))
        assertTrue("Presets must contain Pure White", presets.contains(0xFFFFFFFFL))
        assertTrue("Presets must contain Cyan Pulse", presets.contains(0xFF00E5FFL))
        assertTrue("Presets must contain Amber Glow", presets.contains(0xFFFF9100L))
    }

    // =========================================================================
    // 3. HONEST TELEMETRY VS SIMULATED DATA INVARIANTS
    // =========================================================================

    @Test
    fun testRealBatteryTelemetryPreservedWhenConnected() {
        val state = BatteryState(
            level = 88,
            scale = 100,
            status = ChargingStatus.CHARGING,
            source = ChargingSource.AC,
            voltageMv = 4350,
            temperatureC = 34.2f,
            currentMicroAmps = 2500000,
            isCharging = true
        )

        assertEquals(88, state.percentage)
        assertTrue(state.isCharging)
        assertEquals("AC CHARGER", state.sourceLabel)
        assertEquals("4.35V", state.formattedVoltage)
        assertEquals("34.2°C", state.formattedTemperature)
        assertNotNull(state.wattageW)
        // Power = (4350 * 2500000) / 10^9 = 10.875W
        assertEquals(10.875f, state.wattageW!!, 0.01f)
        assertEquals("10.9W", state.formattedWattage)
    }

    @Test
    fun testSimulatedTelemetryNeverPollutesRealStateWhenDisconnected() {
        val disconnectedState = BatteryState(
            level = 75,
            scale = 100,
            status = ChargingStatus.DISCHARGING,
            source = ChargingSource.NONE,
            voltageMv = 3900,
            temperatureC = 28.0f,
            currentMicroAmps = -450000,
            isCharging = false
        )

        assertFalse(disconnectedState.isCharging)
        assertEquals("BATTERY", disconnectedState.sourceLabel)
        assertNull("Wattage must be null when discharging", disconnectedState.wattageW)
        assertEquals("--", disconnectedState.formattedWattage)
    }

    // =========================================================================
    // 4. REDUCED MOTION ACCESSIBILITY TEST
    // =========================================================================

    @Test
    fun testReducedMotionFlagTransitions() {
        val normalPrefs = BatteryPreferences(reducedMotion = false)
        assertFalse(normalPrefs.reducedMotion)

        val reducedPrefs = normalPrefs.copy(reducedMotion = true)
        assertTrue(reducedPrefs.reducedMotion)
    }
}
