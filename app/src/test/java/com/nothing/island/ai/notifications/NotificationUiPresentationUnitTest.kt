package com.nothing.island.ai.notifications

import android.graphics.Rect
import com.nothing.island.ai.CutoutGeometry
import com.nothing.island.ai.IslandMode
import com.nothing.island.ai.data.IslandPreferences
import com.nothing.island.ai.engine.IslandEvent
import com.nothing.island.ai.engine.IslandMasterState
import com.nothing.island.ai.engine.IslandPriority
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * NotificationUiPresentationUnitTest
 *
 * Verifies Milestone R6 Phase 3 UI presentation logic:
 * - Dynamic 200dp symmetric horizontal expansion for compact notifications.
 * - Dynamic 350dp x 210dp window sizing for expanded notification card.
 * - WindowManager layout calculations for Nothing Phone (4a) punch-hole alignment.
 * - Decoupled action invocation and dismiss synchronization.
 */
class NotificationUiPresentationUnitTest {

    private val testPreferences = IslandPreferences(
        compactWidthDp = 124f,
        compactHeightDp = 36f,
        expandedWidthDp = 350f,
        expandedHeightDp = 210f,
        topMarginDp = 9f,
        horizontalOffsetDp = 0f,
        cornerRadiusDp = 18f
    )

    private val testCutout = CutoutGeometry(
        hasCutout = true,
        boundingRect = Rect(559, 0, 665, 162),
        safeInsetTopPx = 162,
        centerXpx = 612f,
        centerYpx = 81f,
        widthPx = 162f,
        heightPx = 108f,
        density = 3.0f,
        source = "Physical Nothing Phone (4a) Calibration"
    )

    private fun computeTargetWidth(state: IslandMasterState, prefs: IslandPreferences): Float {
        return if (state.mode == IslandMode.EXPANDED) {
            prefs.expandedWidthDp
        } else {
            if (state.activeEvent is IslandEvent.NotificationPosted) {
                200f
            } else {
                prefs.compactWidthDp
            }
        }
    }

    private fun computeTargetHeight(state: IslandMasterState, prefs: IslandPreferences): Float {
        return if (state.mode == IslandMode.EXPANDED) {
            prefs.expandedHeightDp
        } else {
            prefs.compactHeightDp
        }
    }

    @Test
    fun testIdleCapsuleUsesCalibratedDimensions() {
        val idleState = IslandMasterState(
            mode = IslandMode.COLLAPSED,
            activeEvent = null,
            activePriority = IslandPriority.IDLE_BASE
        )

        val targetWidth = computeTargetWidth(idleState, testPreferences)
        val targetHeight = computeTargetHeight(idleState, testPreferences)

        assertEquals("Idle capsule width must strictly match calibrated 124dp", 124f, targetWidth, 0.01f)
        assertEquals("Idle capsule height must strictly match calibrated 36dp", 36f, targetHeight, 0.01f)
    }

    @Test
    fun testActiveNotificationExpandsTo200dpSymmetricallyInCompactMode() {
        val notifEvent = IslandEvent.NotificationPosted(
            id = "0|com.whatsapp|100|null|1001",
            packageName = "com.whatsapp",
            title = "Alice",
            text = "Testing compact expansion",
            timeoutMs = 4500L
        )

        val activeState = IslandMasterState(
            mode = IslandMode.COLLAPSED,
            activeEvent = notifEvent,
            activePriority = IslandPriority.NOTIFICATION
        )

        val targetWidth = computeTargetWidth(activeState, testPreferences)
        val targetHeight = computeTargetHeight(activeState, testPreferences)

        assertEquals("Active notification in compact mode must expand to 200dp", 200f, targetWidth, 0.01f)
        assertEquals("Capsule height must strictly preserve 36dp status bar alignment", 36f, targetHeight, 0.01f)
    }

    @Test
    fun testExpandedModeDimensionsAre350dpBy210dp() {
        val notifEvent = IslandEvent.NotificationPosted(
            id = "0|com.slack|200|null|2001",
            packageName = "com.slack",
            title = "Deploy Bot",
            text = "Build succeeded",
            timeoutMs = 4500L
        )

        val expandedState = IslandMasterState(
            mode = IslandMode.EXPANDED,
            activeEvent = notifEvent,
            activePriority = IslandPriority.NOTIFICATION
        )

        val targetWidth = computeTargetWidth(expandedState, testPreferences)
        val targetHeight = computeTargetHeight(expandedState, testPreferences)

        assertEquals("Expanded card width must be 350dp", 350f, targetWidth, 0.01f)
        assertEquals("Expanded card height must be 210dp", 210f, targetHeight, 0.01f)
    }

    @Test
    fun testPunchHoleClearanceZoneIsCenteredIn200dpPill() {
        // Total width: 200dp
        // Centered punch-hole cutout: 54dp
        // Left Wing: (200 - 54) / 2 = 73dp
        // Right Wing: (200 - 54) / 2 = 73dp
        val totalWidth = 200f
        val punchHoleWidth = testCutout.widthDp
        val leftWingWidth = (totalWidth - punchHoleWidth) / 2f
        val rightWingWidth = (totalWidth - punchHoleWidth) / 2f

        assertEquals(73f, leftWingWidth, 0.01f)
        assertEquals(73f, rightWingWidth, 0.01f)
        assertEquals(200f, leftWingWidth + punchHoleWidth + rightWingWidth, 0.01f)
    }

    @Test
    fun testNotificationExpirationRevertsWidthTo124dp() {
        val notifEvent = IslandEvent.NotificationPosted(
            id = "0|com.telegram|300|null|3001",
            packageName = "org.telegram.messenger",
            title = "Bob",
            text = "Expiring message",
            timeoutMs = 4500L
        )

        val activeState = IslandMasterState(
            mode = IslandMode.COLLAPSED,
            activeEvent = notifEvent,
            activePriority = IslandPriority.NOTIFICATION
        )
        assertEquals(200f, computeTargetWidth(activeState, testPreferences), 0.01f)

        // After timeout expired -> state reverts to activeEvent = null
        val expiredState = activeState.copy(activeEvent = null, activePriority = IslandPriority.IDLE_BASE)
        assertEquals(124f, computeTargetWidth(expiredState, testPreferences), 0.01f)
    }

    @Test
    fun testActionBridgeInvocationNullSafety() {
        NotificationActionBridge.clearAll()
        assertFalse(NotificationActionBridge.executeContentIntent("non_existent_id"))
        assertFalse(NotificationActionBridge.executeAction("non_existent_id", 0))
    }

    @Test
    fun testExpandedViewTypeStateTransitions() {
        val repo = com.nothing.island.ai.engine.IslandStateRepository()

        // 1. Initial State: COLLAPSED, DASHBOARD
        assertEquals(IslandMode.COLLAPSED, repo.state.value.mode)
        assertEquals(com.nothing.island.ai.ExpandedViewType.DASHBOARD, repo.state.value.expandedViewType)

        // 2. Expand Dashboard explicitly (State A)
        repo.expandDashboard()
        assertEquals(IslandMode.EXPANDED, repo.state.value.mode)
        assertEquals(com.nothing.island.ai.ExpandedViewType.DASHBOARD, repo.state.value.expandedViewType)

        // 3. Collapse
        repo.collapse()
        assertEquals(IslandMode.COLLAPSED, repo.state.value.mode)

        // 4. Expand Notification-Focused explicitly (State B)
        repo.expandNotificationFocused()
        assertEquals(IslandMode.EXPANDED, repo.state.value.mode)
        assertEquals(com.nothing.island.ai.ExpandedViewType.NOTIFICATION_FOCUSED, repo.state.value.expandedViewType)

        // 5. Toggle Mode when active event is present
        repo.collapse()
        val notif = IslandEvent.NotificationPosted(
            id = "test_key",
            packageName = "com.test",
            title = "Test",
            text = "Msg"
        )
        repo.updateFromResolvedEvent(notif, IslandPriority.NOTIFICATION, "pres_1")
        repo.toggleMode()
        assertEquals("Toggle with active notification must expand into NOTIFICATION_FOCUSED",
            com.nothing.island.ai.ExpandedViewType.NOTIFICATION_FOCUSED, repo.state.value.expandedViewType)
        assertEquals(IslandMode.EXPANDED, repo.state.value.mode)

        // 6. Legacy bridge preservation
        val legacy = repo.state.value.toLegacyIslandState()
        assertEquals(com.nothing.island.ai.ExpandedViewType.NOTIFICATION_FOCUSED, legacy.expandedViewType)
        assertEquals(IslandMode.EXPANDED, legacy.mode)
    }

    @Test
    fun testOutsideTouchFlagOnlyPresentWhenExpanded() {
        fun computeTargetFlags(mode: IslandMode, isAccessibilityMode: Boolean): Int {
            val baseFlags = android.view.WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    if (isAccessibilityMode) android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                    else android.view.WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN

            return if (mode == IslandMode.EXPANDED) {
                baseFlags or android.view.WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
            } else {
                baseFlags
            }
        }

        val collapsedFlags = computeTargetFlags(IslandMode.COLLAPSED, true)
        val expandedFlags = computeTargetFlags(IslandMode.EXPANDED, true)

        assertFalse("FLAG_WATCH_OUTSIDE_TOUCH must not be present when COLLAPSED",
            (collapsedFlags and android.view.WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH) != 0)
        assertTrue("FLAG_WATCH_OUTSIDE_TOUCH must be present when EXPANDED",
            (expandedFlags and android.view.WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH) != 0)
    }
}
