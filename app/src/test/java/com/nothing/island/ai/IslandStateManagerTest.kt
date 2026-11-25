package com.nothing.island.ai

import com.nothing.island.ai.engine.IslandEvent
import com.nothing.island.ai.media.MediaTrackState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit test suite for [IslandStateManager] smart time-decayed tap priority routing.
 *
 * Verifies:
 * 1. Tapping < 90s after a notification opens Notifications (ExpandedViewType.NOTIFICATION_FOCUSED).
 * 2. Tapping >= 90s after a notification while music is playing opens the Music Player (ExpandedViewType.MUSIC).
 * 3. Tapping < 90s after collapsing/acknowledging the notification while music is playing opens the Music Player (ExpandedViewType.MUSIC).
 * 4. Active Phone Call always takes top precedence (ExpandedViewType.CALL).
 * 5. Tapping >= 90s when no media is playing falls back to Dashboard (ExpandedViewType.DASHBOARD).
 * 6. Arrival of a subsequent notification resets acknowledgment and starts a fresh 90s window.
 */
class IslandStateManagerTest {

    private var simulatedTimeMs: Long = 100_000L
    private lateinit var stateManager: IslandStateManager

    @Before
    fun setUp() {
        simulatedTimeMs = 100_000L
        stateManager = IslandStateManager.createForTesting { simulatedTimeMs }
    }

    // =========================================================================
    // 1. FRESH NOTIFICATION (< 90s) OPENS NOTIFICATIONS
    // =========================================================================

    @Test
    fun testTapWithin90SecondsAfterNotification_opensNotifications() {
        // Given: Active media playback
        val isMediaPlaying = true

        // When: A notification arrives at t = 100_000 ms
        val notificationTime = simulatedTimeMs
        stateManager.onNotificationArrived(notificationTime)

        assertFalse("New notification must be unacknowledged", stateManager.isLatestNotificationAcknowledged)
        assertEquals(notificationTime, stateManager.lastNotificationTimestampMs)

        // And: User taps 30 seconds later (t = 130_000 ms, < 90s elapsed)
        simulatedTimeMs = notificationTime + 30_000L
        val destination = stateManager.resolveTapDestination(
            isCallActive = false,
            isMediaPlaying = isMediaPlaying
        )

        // Then: Must prioritize fresh notification over active media
        assertEquals(
            "Tapping < 90s after notification must open Notification-Focused view",
            ExpandedViewType.NOTIFICATION_FOCUSED,
            destination
        )
    }

    @Test
    fun testTapAt89SecondsAfterNotification_stillOpensNotifications() {
        stateManager.onNotificationArrived(simulatedTimeMs)

        // 89.9 seconds later (just before 90s window closes)
        simulatedTimeMs += 89_900L
        val destination = stateManager.resolveTapDestination(
            isCallActive = false,
            isMediaPlaying = true
        )

        assertEquals(ExpandedViewType.NOTIFICATION_FOCUSED, destination)
    }

    // =========================================================================
    // 2. EXPIRED NOTIFICATION (>= 90s) YIELDS TO ACTIVE MUSIC
    // =========================================================================

    @Test
    fun testTapAtOrAfter90Seconds_whileMusicPlaying_opensMusicPlayer() {
        // Given: Active media playback
        val isMediaPlaying = true

        // When: A notification arrives at t = 100_000 ms and is never acknowledged
        val notificationTime = simulatedTimeMs
        stateManager.onNotificationArrived(notificationTime)

        // And: Exactly 90 seconds pass (t = 190_000 ms)
        simulatedTimeMs = notificationTime + 90_000L
        val destinationAt90s = stateManager.resolveTapDestination(
            isCallActive = false,
            isMediaPlaying = isMediaPlaying
        )

        // Then: Freshness window has expired, must yield back to Music Player
        assertEquals(
            "Tapping at 90s while music is playing must yield to Music Player",
            ExpandedViewType.MUSIC,
            destinationAt90s
        )

        // And: Well past 90 seconds (t = 220_000 ms, 120s elapsed)
        simulatedTimeMs = notificationTime + 120_000L
        val destinationAt120s = stateManager.resolveTapDestination(
            isCallActive = false,
            isMediaPlaying = isMediaPlaying
        )

        assertEquals(
            "Tapping > 90s while music is playing must yield to Music Player",
            ExpandedViewType.MUSIC,
            destinationAt120s
        )
    }

    @Test
    fun testTapAtOrAfter90Seconds_whenNoMusicPlaying_opensDashboard() {
        stateManager.onNotificationArrived(simulatedTimeMs)

        // 95 seconds later, no media playing
        simulatedTimeMs += 95_000L
        val destination = stateManager.resolveTapDestination(
            isCallActive = false,
            isMediaPlaying = false
        )

        // Falls back to default Dashboard
        assertEquals(ExpandedViewType.DASHBOARD, destination)
    }

    // =========================================================================
    // 3. COLLAPSED / ACKNOWLEDGED NOTIFICATION (< 90s) YIELDS TO MUSIC
    // =========================================================================

    @Test
    fun testTapWithin90Seconds_afterCollapsingOrAcknowledging_whileMusicPlaying_opensMusicPlayer() {
        // Given: Active media playback
        val isMediaPlaying = true

        // When: A notification arrives at t = 100_000 ms
        val notificationTime = simulatedTimeMs
        stateManager.onNotificationArrived(notificationTime)

        // User taps to view the notification at t = 115_000 ms (15s elapsed)
        simulatedTimeMs = notificationTime + 15_000L
        stateManager.onNotificationViewExpanded()

        // User finishes viewing and collapses the notification view at t = 125_000 ms (25s elapsed)
        simulatedTimeMs = notificationTime + 25_000L
        stateManager.onNotificationViewCollapsed()

        assertTrue("Collapsing notification view must mark it acknowledged", stateManager.isLatestNotificationAcknowledged)

        // When: User taps the compact pill again at t = 135_000 ms (35s elapsed, still < 90s)
        simulatedTimeMs = notificationTime + 35_000L
        val destination = stateManager.resolveTapDestination(
            isCallActive = false,
            isMediaPlaying = isMediaPlaying
        )

        // Then: Because the notification was already acknowledged, must yield directly to Music Player
        assertEquals(
            "Tapping < 90s after acknowledging/collapsing notification must open Music Player",
            ExpandedViewType.MUSIC,
            destination
        )
    }

    @Test
    fun testExplicitAcknowledge_yieldsToMusicImmediately() {
        stateManager.onNotificationArrived(simulatedTimeMs)

        // 10s later, user dismisses/acknowledges via notification action
        simulatedTimeMs += 10_000L
        stateManager.acknowledgeLatestNotification()
        assertTrue(stateManager.isLatestNotificationAcknowledged)

        // Tap at 15s with music playing
        simulatedTimeMs += 5_000L
        val destination = stateManager.resolveTapDestination(
            isCallActive = false,
            isMediaPlaying = true
        )

        assertEquals(ExpandedViewType.MUSIC, destination)
    }

    // =========================================================================
    // 4. PHONE CALL PRECEDENCE & MULTI-NOTIFICATION CYCLES
    // =========================================================================

    @Test
    fun testActivePhoneCall_alwaysOpensCallExpandedView() {
        // Notification arrived 5 seconds ago, media is playing, call is active
        stateManager.onNotificationArrived(simulatedTimeMs)
        simulatedTimeMs += 5_000L

        val destination = stateManager.resolveTapDestination(
            isCallActive = true,
            isMediaPlaying = true
        )

        assertEquals(
            "Active phone call must override fresh notifications and media",
            ExpandedViewType.CALL,
            destination
        )
    }

    @Test
    fun testNewNotificationArrival_resetsAcknowledgmentAndTimer() {
        // Notification 1
        stateManager.onNotificationArrived(simulatedTimeMs)
        stateManager.onNotificationViewExpanded()
        stateManager.onNotificationViewCollapsed()
        assertTrue(stateManager.isLatestNotificationAcknowledged)

        // Notification 2 arrives at t = 150_000 ms
        simulatedTimeMs = 150_000L
        stateManager.onNotificationArrived(simulatedTimeMs)

        assertFalse("New notification must reset acknowledgment", stateManager.isLatestNotificationAcknowledged)
        assertEquals(150_000L, stateManager.lastNotificationTimestampMs)

        // Tap 10s later while music is playing
        simulatedTimeMs = 160_000L
        val destination = stateManager.resolveTapDestination(
            isCallActive = false,
            isMediaPlaying = true
        )

        assertEquals(
            "New unacknowledged notification must take priority again",
            ExpandedViewType.NOTIFICATION_FOCUSED,
            destination
        )
    }

    @Test
    fun testOverloadWithNullableTrackAndEvent() {
        val track = MediaTrackState(trackTitle = "Track", isPlaying = true)
        val notifEvent = IslandEvent.NotificationPosted(
            id = "notif_1",
            packageName = "com.whatsapp",
            title = "Test",
            text = "Hello"
        )

        stateManager.onNotificationArrived(simulatedTimeMs)
        simulatedTimeMs += 10_000L

        // Fresh notif -> NOTIFICATION_FOCUSED
        assertEquals(
            ExpandedViewType.NOTIFICATION_FOCUSED,
            stateManager.resolveTapDestination(notifEvent, track, simulatedTimeMs)
        )

        // Acknowledge -> MUSIC
        stateManager.acknowledgeLatestNotification()
        assertEquals(
            ExpandedViewType.MUSIC,
            stateManager.resolveTapDestination(notifEvent, track, simulatedTimeMs)
        )
    }
}
