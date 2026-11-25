package com.nothing.island.ai

import com.nothing.island.ai.engine.IslandEvent
import com.nothing.island.ai.media.MediaTrackState

/**
 * IslandStateManager
 *
 * Implements smart time-decayed tap priority routing and freshness tracking for Nothing Island.
 *
 * Rules:
 * 1. Track Fresh Notification Window (90 seconds):
 *    - Records [lastNotificationTimestampMs] whenever a new notification arrives.
 *    - Tracks [isLatestNotificationAcknowledged] boolean (set to false on alert arrival,
 *      and set to true once the user expands and collapses that notification view,
 *      or explicitly acknowledges it).
 * 2. Tap-to-Expand Priority Logic:
 *    - When the user taps the compact pill:
 *      1) If an active incoming/ongoing Phone Call exists, always open Call Expanded View.
 *      2) Else if an unacknowledged notification arrived within the last 90 seconds
 *         (now - lastNotificationTimestampMs < 90_000L && !isLatestNotificationAcknowledged),
 *         expand directly into Notification / Recent Alerts.
 *      3) Else if media is active/playing (currentTrack != null && currentTrack.isPlaying),
 *         expand directly into the Music Expanded Player (MusicExpandedView), even if older
 *         notifications (>= 90s or already viewed) are still sitting in Recent Alerts.
 *      4) Otherwise, fall back to the default Dashboard / Recent Alerts view.
 * 3. Manual Swipe Switching Preserved:
 *    - Keep horizontal swipe gestures (IslandActivitySwitcher.kt) working normally so the user
 *      can manually swipe between Music, Recent Alerts, and Dashboard at any time regardless
 *      of the 90-second timer.
 */
class IslandStateManager(
    private val clock: () -> Long = { System.currentTimeMillis() }
) {
    companion object {
        const val FRESH_NOTIFICATION_WINDOW_MS = 90_000L // 90 seconds

        @Volatile
        private var instance: IslandStateManager? = null

        fun getInstance(clock: () -> Long = { System.currentTimeMillis() }): IslandStateManager {
            return instance ?: synchronized(this) {
                instance ?: IslandStateManager(clock).also { instance = it }
            }
        }

        fun createForTesting(clock: () -> Long): IslandStateManager {
            return IslandStateManager(clock)
        }
    }

    var lastNotificationTimestampMs: Long = 0L
        private set

    var isLatestNotificationAcknowledged: Boolean = true
        private set

    private var isNotificationViewCurrentlyOpen: Boolean = false

    /**
     * Called whenever a new notification arrives.
     * Records arrival timestamp and sets [isLatestNotificationAcknowledged] to false.
     */
    fun onNotificationArrived(timestampMs: Long = clock()) {
        lastNotificationTimestampMs = timestampMs
        isLatestNotificationAcknowledged = false
        isNotificationViewCurrentlyOpen = false
    }

    /**
     * Called when the notification-focused view is opened / expanded.
     */
    fun onNotificationViewExpanded() {
        isNotificationViewCurrentlyOpen = true
    }

    /**
     * Called when the notification view is collapsed.
     * Sets [isLatestNotificationAcknowledged] to true.
     */
    fun onNotificationViewCollapsed() {
        if (isNotificationViewCurrentlyOpen || !isLatestNotificationAcknowledged) {
            isLatestNotificationAcknowledged = true
        }
        isNotificationViewCurrentlyOpen = false
    }

    /**
     * Explicitly acknowledges the latest notification (e.g. action clicked or dismissed).
     */
    fun acknowledgeLatestNotification() {
        isLatestNotificationAcknowledged = true
        isNotificationViewCurrentlyOpen = false
    }

    /**
     * Resolves the target expanded view type when tapping the compact pill,
     * adhering strictly to the 4-tier priority hierarchy.
     */
    fun resolveTapDestination(
        isCallActive: Boolean,
        isMediaPlaying: Boolean,
        nowMs: Long = clock()
    ): ExpandedViewType {
        // Tier 1: Active incoming / ongoing Phone Call
        if (isCallActive) {
            return ExpandedViewType.CALL
        }

        // Tier 2: Unacknowledged notification within 90 seconds
        val elapsedSinceNotification = (nowMs - lastNotificationTimestampMs).coerceAtLeast(0L)
        if (lastNotificationTimestampMs > 0L &&
            elapsedSinceNotification < FRESH_NOTIFICATION_WINDOW_MS &&
            !isLatestNotificationAcknowledged
        ) {
            return ExpandedViewType.NOTIFICATION_FOCUSED
        }

        // Tier 3: Active media playback
        if (isMediaPlaying) {
            return ExpandedViewType.MUSIC
        }

        // Tier 4: Fallback to default Dashboard / Recent Alerts view
        return ExpandedViewType.DASHBOARD
    }

    /**
     * Convenience overload taking active event and current track snapshot.
     */
    fun resolveTapDestination(
        activeEvent: IslandEvent?,
        currentTrack: MediaTrackState?,
        nowMs: Long = clock()
    ): ExpandedViewType {
        val isCall = activeEvent is IslandEvent.IncomingCallRinging ||
                activeEvent is IslandEvent.CallConnected ||
                activeEvent is IslandEvent.CallEnded
        val isMedia = currentTrack != null && currentTrack.isPlaying
        return resolveTapDestination(
            isCallActive = isCall,
            isMediaPlaying = isMedia,
            nowMs = nowMs
        )
    }

    /**
     * Resets state back to initial defaults (useful for test isolation).
     */
    fun reset() {
        lastNotificationTimestampMs = 0L
        isLatestNotificationAcknowledged = true
        isNotificationViewCurrentlyOpen = false
    }
}
