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
    fun testOnboardingStepTitlesMapAllThreeSteps() {
        assertEquals("OVERLAY PERMISSION", onboardingStepTitle(0))
        assertEquals("DISCLOSURES & ACCESS", onboardingStepTitle(1))
        assertEquals("CALIBRATION & BATTERY", onboardingStepTitle(2))
        assertEquals("OVERLAY PERMISSION", onboardingStepTitle(-1))
        assertEquals("CALIBRATION & BATTERY", onboardingStepTitle(9))
    }
}
