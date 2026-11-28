package com.nothing.island.ai.ui

import com.nothing.island.ai.ui.components.ProminentDisclosureType
import com.nothing.island.ai.ui.components.prominentDisclosureSpec
import com.nothing.island.ai.ui.screens.onboardingStepTitle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verifies Google Play Prominent Disclosure contracts and 3-step onboarding metadata.
 */
class PlayComplianceAndOnboardingUnitTest {

    @Test
    fun testAccessibilityProminentDisclosureStatesLocalOnlyAndNoContentReading() {
        val spec = prominentDisclosureSpec(ProminentDisclosureType.ACCESSIBILITY)
        assertTrue(spec.title.contains("Accessibility", ignoreCase = true))
        assertTrue(spec.summary.contains("camera cutout", ignoreCase = true))
        assertTrue(spec.bulletPoints.any { it.contains("canRetrieveWindowContent = false") })
        assertTrue(spec.bulletPoints.any { it.contains("100% locally", ignoreCase = true) })
        assertEquals("ACCEPT & OPEN SETTINGS", spec.confirmLabel)
    }

    @Test
    fun testNotificationProminentDisclosureStatesOnDeviceZeroTransmission() {
        val spec = prominentDisclosureSpec(ProminentDisclosureType.NOTIFICATION_LISTENER)
        assertTrue(spec.title.contains("Notification", ignoreCase = true))
        assertTrue(spec.summary.contains("Notification Listener", ignoreCase = true))
        assertTrue(spec.bulletPoints.any { it.contains("100% on-device", ignoreCase = true) })
        assertTrue(spec.bulletPoints.any { it.contains("Zero analytics", ignoreCase = true) })
        assertEquals("ACCEPT & OPEN SETTINGS", spec.confirmLabel)
    }

    @Test
    fun testOnboardingStepTitlesMapTwoSteps() {
        assertEquals("FLOATING PILL DISPLAY", onboardingStepTitle(0))
        assertEquals("NOTIFICATION & MUSIC ACCESS", onboardingStepTitle(1))
        assertEquals("FLOATING PILL DISPLAY", onboardingStepTitle(-1))
        assertEquals("NOTIFICATION & MUSIC ACCESS", onboardingStepTitle(9))
    }

    @Test
    fun testInstalledAppItemDataModel() {
        val item = com.nothing.island.ai.ui.screens.InstalledAppItem(
            categoryName = "WhatsApp",
            packageName = "com.whatsapp",
            appLabel = "WhatsApp"
        )
        assertEquals("WhatsApp", item.categoryName)
        assertEquals("com.whatsapp", item.packageName)
        assertEquals("WhatsApp", item.appLabel)
    }

    @Test
    fun testLiquidGlassThreeTabNavigation() {
        val tabs = com.nothing.island.ai.navigation.IslandDestination.bottomNavDestinations
        assertEquals(3, tabs.size)
        assertEquals(com.nothing.island.ai.navigation.IslandDestination.Home, tabs[0])
        assertEquals(com.nothing.island.ai.navigation.IslandDestination.Style, tabs[1])
        assertEquals(com.nothing.island.ai.navigation.IslandDestination.Alerts, tabs[2])
    }

    @Test
    fun testAppleGradeSpringPhysicsDefaults() {
        val theme = com.nothing.island.ai.ui.theme.IslandSignatureTheme.NOTHING_CLASSIC
        assertEquals(0.72f, theme.springDampingRatio, 0.001f)
        assertEquals(380f, theme.springStiffness, 0.001f)
    }
}
