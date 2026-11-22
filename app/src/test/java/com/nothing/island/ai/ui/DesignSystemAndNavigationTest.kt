package com.nothing.island.ai.ui

import com.nothing.island.ai.navigation.IslandDestination
import com.nothing.island.ai.ui.components.IslandPreviewMode
import com.nothing.island.ai.ui.components.NothingCardVariant
import com.nothing.island.ai.ui.theme.NothingTokens
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Phase 2 Unit Tests: Design System Tokens, Preview Engine Modes & Navigation Architecture
 */
class DesignSystemAndNavigationTest {

    @Test
    fun testAllEightPreviewModesExistAndConfigured() {
        val modes = IslandPreviewMode.values()
        assertEquals(8, modes.size)

        // Verify compact vs expanded classification
        assertFalse(IslandPreviewMode.IDLE.isExpanded)
        assertFalse(IslandPreviewMode.NOTIF_COMPACT.isExpanded)
        assertTrue(IslandPreviewMode.NOTIF_EXPANDED.isExpanded)
        assertFalse(IslandPreviewMode.MUSIC_COMPACT.isExpanded)
        assertTrue(IslandPreviewMode.MUSIC_EXPANDED.isExpanded)
        assertFalse(IslandPreviewMode.CHARGING.isExpanded)
        assertFalse(IslandPreviewMode.BLUETOOTH_COMPACT.isExpanded)
        assertTrue(IslandPreviewMode.BLUETOOTH_EXPANDED.isExpanded)

        // Verify labels
        assertEquals("IDLE", IslandPreviewMode.IDLE.label)
        assertEquals("NOTIF (C)", IslandPreviewMode.NOTIF_COMPACT.label)
        assertEquals("NOTIF (E)", IslandPreviewMode.NOTIF_EXPANDED.label)
        assertEquals("MUSIC (C)", IslandPreviewMode.MUSIC_COMPACT.label)
        assertEquals("MUSIC (E)", IslandPreviewMode.MUSIC_EXPANDED.label)
        assertEquals("CHARGING", IslandPreviewMode.CHARGING.label)
        assertEquals("BT (C)", IslandPreviewMode.BLUETOOTH_COMPACT.label)
        assertEquals("BT (E)", IslandPreviewMode.BLUETOOTH_EXPANDED.label)
    }

    @Test
    fun testPrimaryBottomNavDestinations() {
        val destinations = IslandDestination.bottomNavDestinations
        assertEquals(5, destinations.size)

        assertEquals(IslandDestination.Home, destinations[0])
        assertEquals(IslandDestination.Notifications, destinations[1])
        assertEquals(IslandDestination.Music, destinations[2])
        assertEquals(IslandDestination.Battery, destinations[3])
        assertEquals(IslandDestination.Studios, destinations[4])

        assertEquals("home", IslandDestination.Home.route)
        assertEquals("notifications", IslandDestination.Notifications.route)
        assertEquals("music", IslandDestination.Music.route)
        assertEquals("battery", IslandDestination.Battery.route)
        assertEquals("studios", IslandDestination.Studios.route)
    }

    @Test
    fun testStudioHubSubDestinations() {
        val hubDestinations = IslandDestination.studioHubDestinations
        assertEquals(8, hubDestinations.size)

        assertTrue(hubDestinations.contains(IslandDestination.ThemeStudio))
        assertTrue(hubDestinations.contains(IslandDestination.AnimationStudio))
        assertTrue(hubDestinations.contains(IslandDestination.GestureStudio))
        assertTrue(hubDestinations.contains(IslandDestination.PeripheralsStudio))
        assertTrue(hubDestinations.contains(IslandDestination.MascotStudio))
        assertTrue(hubDestinations.contains(IslandDestination.GeometryStudio))
        assertTrue(hubDestinations.contains(IslandDestination.Settings))
        assertTrue(hubDestinations.contains(IslandDestination.CallStudio))

        // Verify routes
        assertEquals("studio_theme", IslandDestination.ThemeStudio.route)
        assertEquals("studio_anim", IslandDestination.AnimationStudio.route)
        assertEquals("studio_gesture", IslandDestination.GestureStudio.route)
        assertEquals("studio_peripherals", IslandDestination.PeripheralsStudio.route)
        assertEquals("studio_geometry", IslandDestination.GeometryStudio.route)
        assertEquals("settings", IslandDestination.Settings.route)
        assertEquals("studio_call", IslandDestination.CallStudio.route)
    }

    @Test
    fun testBackwardCompatibilityLegacyDestinations() {
        // Ensure legacy destinations remain instantiated and accessible
        assertEquals("appearance", IslandDestination.Appearance.route)
        assertEquals("features", IslandDestination.Features.route)
        assertEquals("gemini", IslandDestination.Gemini.route)
    }

    @Test
    fun testNothingTokensBaselineValues() {
        assertEquals(androidx.compose.ui.graphics.Color(0xFF000000), NothingTokens.NothingBlack)
        assertEquals(androidx.compose.ui.graphics.Color(0xFFD71920), NothingTokens.NothingRed)
        assertEquals(124f, NothingTokens.CutoutWidthDp, 0.01f)
        assertEquals(36f, NothingTokens.CutoutHeightDp, 0.01f)
        assertEquals(9f, NothingTokens.CutoutTopOffsetDp, 0.01f)
        assertEquals(18f, NothingTokens.CutoutCornerRadiusDp, 0.01f)
    }

    @Test
    fun testNothingCardVariants() {
        val variants = NothingCardVariant.values()
        assertEquals(5, variants.size)
        assertTrue(variants.contains(NothingCardVariant.STANDARD))
        assertTrue(variants.contains(NothingCardVariant.ELEVATED))
        assertTrue(variants.contains(NothingCardVariant.ACCENT_BORDER))
        assertTrue(variants.contains(NothingCardVariant.PREVIEW_WELL))
        assertTrue(variants.contains(NothingCardVariant.TRANSPARENT))
    }
}
