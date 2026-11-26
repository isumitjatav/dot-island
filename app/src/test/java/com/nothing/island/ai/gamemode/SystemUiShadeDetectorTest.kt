package com.nothing.island.ai.gamemode

import android.view.accessibility.AccessibilityEvent
import org.junit.Assert.assertEquals
import org.junit.Test

class SystemUiShadeDetectorTest {

    private val ownPkg = "com.nothing.island.ai"

    @Test
    fun shadeWindow_opensAndHidesOverlay_whileVolumeAndHeadsUpStayIgnored() {
        assertEquals(
            SystemShadeTransition.SHADE_OPENED,
            SystemUiShadeDetector.detectShadeTransition(
                eventType = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
                eventPackage = "com.android.systemui",
                eventClassName = "com.android.systemui.shade.NotificationShadeWindowView",
                eventText = "Notification shade.",
                ownPackageName = ownPkg,
                isShadeCurrentlyOpen = false,
            )
        )

        // Volume dialog and heads-up banners inside SystemUI must NOT trigger shade-hide
        assertEquals(
            SystemShadeTransition.IGNORE,
            SystemUiShadeDetector.detectShadeTransition(
                eventType = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
                eventPackage = "com.android.systemui",
                eventClassName = "com.android.systemui.volume.VolumeDialogImpl",
                eventText = null,
                ownPackageName = ownPkg,
                isShadeCurrentlyOpen = false,
            )
        )

        assertEquals(
            SystemShadeTransition.IGNORE,
            SystemUiShadeDetector.detectShadeTransition(
                eventType = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
                eventPackage = "com.android.systemui",
                eventClassName = "com.android.systemui.statusbar.notification.HeadsUpManager",
                eventText = null,
                ownPackageName = ownPkg,
                isShadeCurrentlyOpen = false,
            )
        )
    }

    @Test
    fun returningToForegroundAppOrLauncher_closesShadeAndRestoresOverlay() {
        assertEquals(
            SystemShadeTransition.SHADE_CLOSED,
            SystemUiShadeDetector.detectShadeTransition(
                eventType = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
                eventPackage = "com.nothing.launcher",
                eventClassName = "com.nothing.launcher.Launcher",
                eventText = null,
                ownPackageName = ownPkg,
                isShadeCurrentlyOpen = true,
            )
        )
        assertEquals(
            SystemShadeTransition.SHADE_CLOSED,
            SystemUiShadeDetector.detectShadeTransition(
                eventType = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
                eventPackage = ownPkg,
                eventClassName = "com.nothing.island.ai.MainActivity",
                eventText = null,
                ownPackageName = ownPkg,
                isShadeCurrentlyOpen = true,
            )
        )

        // Trailing SystemUI Notification shade teardown event 300ms after closing must be ignored
        assertEquals(
            SystemShadeTransition.IGNORE,
            SystemUiShadeDetector.detectShadeTransition(
                eventType = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
                eventPackage = "com.android.systemui",
                eventClassName = "android.view.View",
                eventText = "Notification shade.",
                ownPackageName = ownPkg,
                isShadeCurrentlyOpen = false,
                nowMs = 1_300L,
                lastShadeClosedAtMs = 1_000L,
            )
        )

        // Subsequent pull-down after settle window opens cleanly again
        assertEquals(
            SystemShadeTransition.SHADE_OPENED,
            SystemUiShadeDetector.detectShadeTransition(
                eventType = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
                eventPackage = "com.android.systemui",
                eventClassName = "android.view.View",
                eventText = "Notification shade.",
                ownPackageName = ownPkg,
                isShadeCurrentlyOpen = false,
                nowMs = 2_000L,
                lastShadeClosedAtMs = 1_000L,
            )
        )
    }
}
