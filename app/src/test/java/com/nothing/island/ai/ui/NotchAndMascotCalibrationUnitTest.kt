package com.nothing.island.ai.ui

import com.nothing.island.ai.data.IslandPreferences
import com.nothing.island.ai.shouldShowIdleMascot
import com.nothing.island.ai.ui.components.computeNotchCenterXPx
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for decoupled expanded U-notch calibration math & defaults
 * and idle capsule mascot visibility gating.
 */
class NotchAndMascotCalibrationUnitTest {

    @Test
    fun expandedCalibrationDefaults_areDecoupledFromCompactPill() {
        val prefs = IslandPreferences(
            horizontalOffsetDp = 12f,
            topMarginDp = 15f,
            expandedOffsetX = -8f,
            expandedOffsetY = 6f,
            expandedNotchWidth = 42f,
            expandedNotchDepth = 30f,
        )
        assertEquals(12f, prefs.horizontalOffsetDp, 0.001f)
        assertEquals(-8f, prefs.expandedOffsetX, 0.001f)
        assertEquals(6f, prefs.expandedOffsetY, 0.001f)
        assertEquals(42f, prefs.expandedNotchWidth, 0.001f)
        assertEquals(30f, prefs.expandedNotchDepth, 0.001f)
    }

    @Test
    fun computeNotchCenterX_tracksPhysicalCameraRelativeToCardLeftPlusOffset() {
        // Screen width 1224px, camera at 612px, card 1170px centered -> leftX = 27px
        val centered = computeNotchCenterXPx(
            physicalCameraCenterX = 612f,
            expandedCardLeftX = 27f,
            expandedOffsetXPx = 0f,
            cardWidthPx = 1170f,
            notchWidthPx = 114f,
            cornerRadiusPx = 84f,
        )
        assertEquals(585f, centered, 0.001f)

        val shiftedRight = computeNotchCenterXPx(
            physicalCameraCenterX = 612f,
            expandedCardLeftX = 27f,
            expandedOffsetXPx = 18f,
            cardWidthPx = 1170f,
            notchWidthPx = 114f,
            cornerRadiusPx = 84f,
        )
        assertEquals(603f, shiftedRight, 0.001f)
    }

    @Test
    fun computeNotchCenterX_clampsSafelyAwayFromCornerRadii() {
        val clampedLeft = computeNotchCenterXPx(
            physicalCameraCenterX = 612f,
            expandedCardLeftX = 27f,
            expandedOffsetXPx = -1000f,
            cardWidthPx = 1170f,
            notchWidthPx = 114f,
            cornerRadiusPx = 84f,
        )
        // cornerRadiusPx (84) + notchWidthPx/2 (57) = 141
        assertEquals(141f, clampedLeft, 0.001f)

        val clampedRight = computeNotchCenterXPx(
            physicalCameraCenterX = 612f,
            expandedCardLeftX = 27f,
            expandedOffsetXPx = 1000f,
            cardWidthPx = 1170f,
            notchWidthPx = 114f,
            cornerRadiusPx = 84f,
        )
        // cardWidthPx (1170) - 84 - 57 = 1029
        assertEquals(1029f, clampedRight, 0.001f)
    }

    @Test
    fun idleMascotShowsWheneverEnabledAndIdle_andHidesOnActivePills() {
        assertTrue(
            shouldShowIdleMascot(
                isExpanded = false,
                isDualPill = false,
                isCallActive = false,
                isEarbudEvent = false,
                isSystemAlertActive = false,
                hasActiveNotification = false,
                isMusicPlaying = false,
                isBatteryActive = false,
                isChargingPillVisible = false,
                hasDownloadProgress = false,
                mascotEnabled = true,
            )
        )

        assertFalse(
            shouldShowIdleMascot(
                isExpanded = false,
                isDualPill = false,
                isCallActive = false,
                isEarbudEvent = false,
                isSystemAlertActive = false,
                hasActiveNotification = false,
                isMusicPlaying = true,
                isBatteryActive = false,
                isChargingPillVisible = false,
                hasDownloadProgress = false,
                mascotEnabled = true,
            )
        )
    }
}
