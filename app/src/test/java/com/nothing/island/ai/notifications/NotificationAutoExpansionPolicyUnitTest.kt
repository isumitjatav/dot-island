package com.nothing.island.ai.notifications

import com.nothing.island.ai.ExpandedViewType
import com.nothing.island.ai.IslandMode
import com.nothing.island.ai.engine.IslandEvent
import com.nothing.island.ai.engine.IslandPriority
import com.nothing.island.ai.engine.DownloadProgressState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * NotificationAutoExpansionPolicyUnitTest
 *
 * R7.1 acceptance coverage for the notification-triggered auto-expansion decision layer.
 * Pure JVM — no Android framework, no binder, no device required.
 */
class NotificationAutoExpansionPolicyUnitTest {

    private fun notification(
        id: String = "0|com.whatsapp|1|null|10123",
        title: String = "Alice",
        text: String = "Hey there",
        timeoutMs: Long = 4500L,
        progressState: DownloadProgressState = DownloadProgressState.NONE,
        hasProgress: Boolean = false,
        progress: Int = 0,
        maxProgress: Int = 0
    ) = IslandEvent.NotificationPosted(
        id = id,
        timeoutMs = timeoutMs,
        packageName = "com.whatsapp",
        title = title,
        text = text,
        hasProgress = hasProgress,
        progress = progress,
        maxProgress = maxProgress,
        progressState = progressState
    )

    private fun evaluate(
        event: IslandEvent? = notification(),
        presentationId: String = "presentation_1",
        lastHandledPresentationId: String? = null,
        mode: IslandMode = IslandMode.COLLAPSED,
        expandedViewType: ExpandedViewType = ExpandedViewType.DASHBOARD,
        preferences: NotificationPreferences = NotificationPreferences.DEFAULTS,
        isUserInteracting: Boolean = false
    ) = NotificationAutoExpansionPolicy.evaluateExpansion(
        event = event,
        presentationId = presentationId,
        lastHandledPresentationId = lastHandledPresentationId,
        mode = mode,
        expandedViewType = expandedViewType,
        preferences = preferences,
        isUserInteracting = isUserInteracting
    )

    // =========================================================================
    // 1. Eligible new notification expands the Island
    // =========================================================================
    @Test
    fun eligibleNewNotificationExpands() {
        val outcome = evaluate()
        assertTrue("Expected Expand, got $outcome", outcome is NotificationAutoExpansionPolicy.ExpansionOutcome.Expand)
        assertEquals(4500L, (outcome as NotificationAutoExpansionPolicy.ExpansionOutcome.Expand).durationMs)
    }

    @Test
    fun defaultPreferenceIsEnabled() {
        assertTrue(
            "autoExpandOnNotification must default to ON",
            NotificationPreferences.DEFAULTS.autoExpandOnNotification
        )
    }

    @Test
    fun defaultDurationIsFourPointFiveSeconds() {
        assertEquals(4500L, NotificationPreferences.DEFAULTS.displayDurationMs)
    }

    // =========================================================================
    // 2. Disabled auto-expansion does not expand
    // =========================================================================
    @Test
    fun disabledPreferenceSkipsExpansion() {
        val outcome = evaluate(
            preferences = NotificationPreferences.DEFAULTS.copy(autoExpandOnNotification = false)
        )
        assertEquals(
            NotificationAutoExpansionPolicy.ExpansionOutcome.Skip(
                NotificationAutoExpansionPolicy.Reasons.DISABLED_BY_PREFERENCE
            ),
            outcome
        )
    }

    @Test
    fun globallyDisabledNotificationsSkipExpansion() {
        val outcome = evaluate(
            preferences = NotificationPreferences.DEFAULTS.copy(enabled = false)
        )
        assertEquals(
            NotificationAutoExpansionPolicy.ExpansionOutcome.Skip(
                NotificationAutoExpansionPolicy.Reasons.NOTIFICATIONS_DISABLED
            ),
            outcome
        )
    }

    // =========================================================================
    // 3. Excluded / hidden notifications do not expand
    // =========================================================================
    // Exclusion is enforced upstream in NotificationFilter, which returns null for an excluded
    // package. A null event means no presentation is ever resolved, so this policy is never
    // consulted with an eligible event. We assert that contract here using the real filter.
    @Test
    fun excludedPackageNeverProducesAnExpandableEvent() {
        val filter = NotificationFilter(hostPackageName = "com.nothing.island.ai")
        val raw = RawNotificationData(
            key = "0|com.spam|1|null|999",
            packageName = "com.spam",
            title = "Spam",
            text = "Buy now",
            importance = 3
        )
        val prefs = NotificationPreferences.DEFAULTS.copy(excludedPackages = setOf("com.spam"))
        val event = filter.filter(raw, prefs, isKeyguardLocked = false)

        assertEquals("Excluded app must be dropped by the filter", null, event)
        // And a null event can never expand:
        assertTrue(evaluate(event = event) is NotificationAutoExpansionPolicy.ExpansionOutcome.Skip)
    }

    @Test
    fun hiddenPreviewModeSkipsExpansion() {
        val outcome = evaluate(
            preferences = NotificationPreferences.DEFAULTS.copy(previewMode = NotificationPreviewMode.HIDDEN)
        )
        assertEquals(
            NotificationAutoExpansionPolicy.ExpansionOutcome.Skip(
                NotificationAutoExpansionPolicy.Reasons.PREVIEW_MODE_HIDDEN
            ),
            outcome
        )
    }

    @Test
    fun silentNotificationFilteredUpstreamDoesNotExpand() {
        val filter = NotificationFilter(hostPackageName = "com.nothing.island.ai")
        val raw = RawNotificationData(
            key = "0|com.slack|1|null|1",
            packageName = "com.slack",
            title = "Channel",
            text = "quiet ping",
            importance = 2 // IMPORTANCE_LOW
        )
        val event = filter.filter(raw, NotificationPreferences.DEFAULTS, isKeyguardLocked = false)
        assertEquals(null, event)
        assertTrue(evaluate(event = event) is NotificationAutoExpansionPolicy.ExpansionOutcome.Skip)
    }

    // =========================================================================
    // 4. Duplicate notification updates do not trigger repeated expansion
    // =========================================================================
    @Test
    fun duplicatePresentationIdentityDoesNotReexpand() {
        val outcome = evaluate(
            presentationId = "presentation_1",
            lastHandledPresentationId = "presentation_1"
        )
        assertEquals(
            NotificationAutoExpansionPolicy.ExpansionOutcome.Skip(
                NotificationAutoExpansionPolicy.Reasons.ALREADY_HANDLED
            ),
            outcome
        )
    }

    @Test
    fun newPresentationIdentityExpandsAgain() {
        val outcome = evaluate(
            presentationId = "presentation_2",
            lastHandledPresentationId = "presentation_1"
        )
        assertTrue(outcome is NotificationAutoExpansionPolicy.ExpansionOutcome.Expand)
    }

    // =========================================================================
    // 5. Ongoing download updates do not repeatedly expand
    // =========================================================================
    @Test
    fun ongoingDownloadUpdateDoesNotReexpand() {
        // Progress notifications carry timeoutMs == 0 and update in place, so the priority
        // manager preserves presentationId. Result: no re-expansion, no timer restart.
        val progressEvent = notification(
            timeoutMs = 0L,
            hasProgress = true,
            progress = 42,
            maxProgress = 100,
            progressState = DownloadProgressState.DOWNLOADING
        )
        val outcome = evaluate(
            event = progressEvent,
            presentationId = "download_presentation",
            lastHandledPresentationId = "download_presentation"
        )
        assertEquals(
            NotificationAutoExpansionPolicy.ExpansionOutcome.Skip(
                NotificationAutoExpansionPolicy.Reasons.ALREADY_HANDLED
            ),
            outcome
        )
    }

    @Test
    fun downloadCollapseDurationUsesConfiguredDurationNotZeroTimeout() {
        // A persistent download event has timeoutMs == 0. The Island must still collapse on
        // the configured schedule rather than staying open forever.
        val progressEvent = notification(
            timeoutMs = 0L,
            hasProgress = true,
            progressState = DownloadProgressState.DOWNLOADING
        )
        val outcome = evaluate(event = progressEvent)
        assertTrue(outcome is NotificationAutoExpansionPolicy.ExpansionOutcome.Expand)
        assertEquals(4500L, (outcome as NotificationAutoExpansionPolicy.ExpansionOutcome.Expand).durationMs)
    }

    // =========================================================================
    // 6. Non-notification events never expand
    // =========================================================================
    @Test
    fun mediaEventDoesNotTriggerNotificationExpansion() {
        val mediaEvent = IslandEvent.MediaPlaybackChanged(
            id = "media_1",
            packageName = "com.spotify.music",
            trackTitle = "Track",
            artistName = "Artist",
            isPlaying = true
        )
        assertEquals(
            NotificationAutoExpansionPolicy.ExpansionOutcome.Skip(
                NotificationAutoExpansionPolicy.Reasons.NOT_A_NOTIFICATION
            ),
            evaluate(event = mediaEvent)
        )
    }

    @Test
    fun nullEventDoesNotExpand() {
        assertEquals(
            NotificationAutoExpansionPolicy.ExpansionOutcome.Skip(
                NotificationAutoExpansionPolicy.Reasons.NOT_A_NOTIFICATION
            ),
            evaluate(event = null)
        )
    }

    // =========================================================================
    // 7. Music independence
    // =========================================================================
    @Test
    fun alreadyExpandedIslandIsNotHijacked() {
        // Music (or the user) already has the Island open. Do not yank them into notification view.
        val outcome = evaluate(
            mode = IslandMode.EXPANDED,
            expandedViewType = ExpandedViewType.MUSIC
        )
        assertEquals(
            NotificationAutoExpansionPolicy.ExpansionOutcome.Skip(
                NotificationAutoExpansionPolicy.Reasons.ALREADY_EXPANDED
            ),
            outcome
        )
    }

    @Test
    fun batteryExpandedAllowsNotificationExpansion() {
        // When charging, incoming notifications must be allowed to expand so user gets messages.
        val outcome = evaluate(
            mode = IslandMode.EXPANDED,
            expandedViewType = ExpandedViewType.BATTERY
        )
        assertTrue(outcome is NotificationAutoExpansionPolicy.ExpansionOutcome.Expand)
    }

    @Test
    fun notificationPreferenceDoesNotAffectMusicPreference() {
        // Notification auto-expand is OFF; music auto-expand stays independent and ON by default.
        val notifPrefs = NotificationPreferences.DEFAULTS.copy(autoExpandOnNotification = false)
        val musicPrefs = com.nothing.island.ai.media.MusicPreferences.DEFAULTS

        assertFalse(notifPrefs.autoExpandOnNotification)
        assertTrue("Music auto-expand must remain independently enabled", musicPrefs.isAutoExpandEnabled)
        assertTrue(
            "Notification and music durations must be logically independent fields",
            notifPrefs.displayDurationMs > 0L && musicPrefs.autoExpandDurationSeconds > 0f
        )
    }

    // =========================================================================
    // 8. Timer collapses after configured duration
    // =========================================================================
    @Test
    fun collapseAllowedForMatchingNotificationPresentation() {
        assertTrue(
            NotificationAutoExpansionPolicy.evaluateCollapse(
                currentPresentationId = "presentation_1",
                expectedPresentationId = "presentation_1",
                mode = IslandMode.EXPANDED,
                expandedViewType = ExpandedViewType.NOTIFICATION_FOCUSED,
                isUserInteracting = false
            )
        )
    }

    @Test
    fun collapseBlockedWhenPresentationChanged() {
        assertFalse(
            NotificationAutoExpansionPolicy.evaluateCollapse(
                currentPresentationId = "presentation_2",
                expectedPresentationId = "presentation_1",
                mode = IslandMode.EXPANDED,
                expandedViewType = ExpandedViewType.NOTIFICATION_FOCUSED,
                isUserInteracting = false
            )
        )
    }

    @Test
    fun collapseBlockedWhenViewSwitchedToMusic() {
        assertFalse(
            NotificationAutoExpansionPolicy.evaluateCollapse(
                currentPresentationId = "presentation_1",
                expectedPresentationId = "presentation_1",
                mode = IslandMode.EXPANDED,
                expandedViewType = ExpandedViewType.MUSIC,
                isUserInteracting = false
            )
        )
    }

    @Test
    fun collapseBlockedWhenAlreadyCollapsed() {
        assertFalse(
            NotificationAutoExpansionPolicy.evaluateCollapse(
                currentPresentationId = "presentation_1",
                expectedPresentationId = "presentation_1",
                mode = IslandMode.COLLAPSED,
                expandedViewType = ExpandedViewType.NOTIFICATION_FOCUSED,
                isUserInteracting = false
            )
        )
    }

    @Test
    fun collapseAllowedWhenNotificationAlreadyExpired() {
        // Device-verified regression: the priority manager's own timeout fires on the same
        // duration as this timer. If it wins the race, activeEvent becomes null and the
        // presentation identity moves to an idle token. The expanded card must still close
        // rather than being left blank and stuck open.
        assertTrue(
            NotificationAutoExpansionPolicy.evaluateCollapse(
                currentPresentationId = "idle_1793936584816",
                expectedPresentationId = "presentation_1",
                mode = IslandMode.EXPANDED,
                expandedViewType = ExpandedViewType.NOTIFICATION_FOCUSED,
                isUserInteracting = false,
                hasActiveNotification = false
            )
        )
    }

    @Test
    fun collapseBlockedWhenInteractingEvenIfNotificationExpired() {
        assertFalse(
            NotificationAutoExpansionPolicy.evaluateCollapse(
                currentPresentationId = "idle_1",
                expectedPresentationId = "presentation_1",
                mode = IslandMode.EXPANDED,
                expandedViewType = ExpandedViewType.NOTIFICATION_FOCUSED,
                isUserInteracting = true,
                hasActiveNotification = false
            )
        )
    }

    // =========================================================================
    // 9. User interaction prevents premature collapse
    // =========================================================================
    @Test
    fun collapseBlockedWhileUserInteracting() {
        assertFalse(
            NotificationAutoExpansionPolicy.evaluateCollapse(
                currentPresentationId = "presentation_1",
                expectedPresentationId = "presentation_1",
                mode = IslandMode.EXPANDED,
                expandedViewType = ExpandedViewType.NOTIFICATION_FOCUSED,
                isUserInteracting = true
            )
        )
    }

    @Test
    fun expansionBlockedWhileUserInteracting() {
        assertEquals(
            NotificationAutoExpansionPolicy.ExpansionOutcome.Skip(
                NotificationAutoExpansionPolicy.Reasons.USER_INTERACTING
            ),
            evaluate(isUserInteracting = true)
        )
    }

    // =========================================================================
    // 10. Duration clamping
    // =========================================================================
    @Test
    fun durationClampedToMinimum() {
        val outcome = evaluate(
            preferences = NotificationPreferences.DEFAULTS.copy(displayDurationMs = 500L)
        )
        assertEquals(
            2000L,
            (outcome as NotificationAutoExpansionPolicy.ExpansionOutcome.Expand).durationMs
        )
    }

    @Test
    fun durationClampedToMaximum() {
        val outcome = evaluate(
            preferences = NotificationPreferences.DEFAULTS.copy(displayDurationMs = 60000L)
        )
        assertEquals(
            10000L,
            (outcome as NotificationAutoExpansionPolicy.ExpansionOutcome.Expand).durationMs
        )
    }

    // =========================================================================
    // 11. Higher-priority interruption behaviour
    // =========================================================================
    @Test
    fun higherPriorityEventIsNotAnExpansionCandidate() {
        // Priority manager rejects the P55 notification while P100 ring is active, so the
        // resolved event handed to this policy is the call — never a notification.
        val incomingCall = IslandEvent.IncomingCallRinging(
            id = "call_1",
            callId = "call_1",
            callerDisplayName = "Mom",
            isVideoCall = false
        )
        assertEquals(
            NotificationAutoExpansionPolicy.ExpansionOutcome.Skip(
                NotificationAutoExpansionPolicy.Reasons.NOT_A_NOTIFICATION
            ),
            evaluate(event = incomingCall)
        )
    }

    @Test
    fun notificationPriorityIsBelowCallAndSafety() {
        assertTrue(IslandPriority.NOTIFICATION < IslandPriority.CRITICAL_TELEPHONY)
        assertTrue(IslandPriority.NOTIFICATION < IslandPriority.SAFETY_ALERT)
        assertTrue(IslandPriority.NOTIFICATION > IslandPriority.MEDIA_PLAYBACK)
    }
}
