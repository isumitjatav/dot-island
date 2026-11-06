package com.nothing.island.ai.notifications

import com.nothing.island.ai.ExpandedViewType
import com.nothing.island.ai.IslandMode
import com.nothing.island.ai.engine.IslandEvent

/**
 * NotificationAutoExpansionPolicy
 *
 * Pure, deterministic decision layer for R7.1 notification-triggered auto-expansion.
 * Contains ZERO Android framework dependencies so every branch is JVM unit testable.
 *
 * Two independent decisions are modelled:
 *
 *  1. [evaluateExpansion]  — should an arriving notification pop the Island open?
 *  2. [evaluateCollapse]   — is it still safe to collapse for the notification that opened it?
 *
 * The controller owns the timer and the actual view mutation; this class owns only the rules.
 * That separation is deliberate: it keeps the accepted R7 overlay architecture untouched and
 * makes every rule here provable without a device or a binder.
 */
object NotificationAutoExpansionPolicy {

    /**
     * Duration clamp bounds reused from [NotificationPreferences] so the notification
     * auto-collapse window can never drift outside the supported 2s–10s range.
     */
    private val MIN_DURATION_MS = NotificationPreferences.MIN_DURATION_MS
    private val MAX_DURATION_MS = NotificationPreferences.MAX_DURATION_MS

    /**
     * Result of an expansion evaluation.
     */
    sealed interface ExpansionOutcome {
        /**
         * Expand the Island now and schedule a collapse after [durationMs].
         */
        data class Expand(val durationMs: Long) : ExpansionOutcome

        /**
         * Do not expand. [reason] is a stable machine-readable tag used by Logcat and tests.
         */
        data class Skip(val reason: String) : ExpansionOutcome
    }

    /**
     * Reason tags. Stable strings so they can be asserted in unit tests and grepped in Logcat.
     */
    object Reasons {
        const val DISABLED_BY_PREFERENCE = "AUTO_EXPAND_DISABLED"
        const val NOTIFICATIONS_DISABLED = "NOTIFICATIONS_DISABLED"
        const val PREVIEW_MODE_HIDDEN = "PREVIEW_MODE_HIDDEN"
        const val NOT_A_NOTIFICATION = "NOT_A_NOTIFICATION"
        const val ALREADY_HANDLED = "ALREADY_HANDLED_PRESENTATION"
        const val ALREADY_EXPANDED = "ISLAND_ALREADY_EXPANDED"
        const val USER_INTERACTING = "USER_INTERACTING"
    }

    /**
     * Whether an arriving notification presentation is eligible to open the Island.
     *
     * IMPORTANT — precedence of higher-priority events is enforced upstream by
     * [com.nothing.island.ai.engine.IslandPriorityManager], not here. This policy is only ever
     * consulted after the priority manager has already resolved the notification as the winning
     * presentation. That means an incoming call (P100) or an active safety alert (P70) will have
     * rejected the P55 notification before this function is reached, and the Island correctly
     * stays on the higher-priority interface.
     *
     * @param event the currently resolved presentation, or null for idle.
     * @param presentationId identity of the resolved presentation. The priority manager keeps this
     *   value STABLE across in-place updates to the same notification, which is precisely how
     *   duplicate/progress updates are suppressed here.
     * @param lastHandledPresentationId the presentation identity this policy last acted on.
     * @param mode current Island display mode.
     * @param expandedViewType current expanded view type (only meaningful when mode == EXPANDED).
     * @param preferences live notification preferences snapshot.
     * @param isUserInteracting true when the user is actively engaged (typing a reply, manual expand).
     */
    fun evaluateExpansion(
        event: IslandEvent?,
        presentationId: String,
        lastHandledPresentationId: String?,
        mode: IslandMode,
        expandedViewType: ExpandedViewType,
        preferences: NotificationPreferences,
        isUserInteracting: Boolean
    ): ExpansionOutcome {
        // --- Preference gates -------------------------------------------------
        if (!preferences.enabled) {
            return ExpansionOutcome.Skip(Reasons.NOTIFICATIONS_DISABLED)
        }
        if (!preferences.autoExpandOnNotification) {
            return ExpansionOutcome.Skip(Reasons.DISABLED_BY_PREFERENCE)
        }
        if (preferences.previewMode == NotificationPreviewMode.HIDDEN) {
            return ExpansionOutcome.Skip(Reasons.PREVIEW_MODE_HIDDEN)
        }

        if (event !is IslandEvent.NotificationPosted) {
            return ExpansionOutcome.Skip(Reasons.NOT_A_NOTIFICATION)
        }

        // --- Deduplication ----------------------------------------------------
        // In-place updates (download progress ticks, notification refreshes) preserve the
        // presentation identity. Same identity means we have already expanded for this
        // notification, so we must not re-expand and must not restart the collapse timer.
        if (presentationId == lastHandledPresentationId) {
            return ExpansionOutcome.Skip(Reasons.ALREADY_HANDLED)
        }

        // --- Respect an already-open Island -----------------------------------
        // If the user (or the music engine) already has the Island open, never yank them
        // into the notification view. This keeps R7 music auto-expansion behaviour intact.
        if (mode == IslandMode.EXPANDED) {
            return ExpansionOutcome.Skip(Reasons.ALREADY_EXPANDED)
        }

        if (isUserInteracting) {
            return ExpansionOutcome.Skip(Reasons.USER_INTERACTING)
        }

        // --- Duration ---------------------------------------------------------
        // The collapse window is the configured notification display duration, clamped.
        // We deliberately do NOT use event.timeoutMs here: persistent progress events carry
        // a 0ms timeout (they live in the priority manager indefinitely), but the Island must
        // still collapse on schedule and fall back to the compact pill.
        val durationMs = preferences.displayDurationMs
            .coerceIn(MIN_DURATION_MS, MAX_DURATION_MS)

        return ExpansionOutcome.Expand(durationMs)
    }

    /**
     * Whether the post-expansion collapse timer may actually collapse the Island.
     *
     * The timer is deliberately conservative: it only collapses if the Island is still showing
     * exactly the notification that opened it, the user is not mid-interaction, and no other
     * interface has taken over in the meantime.
     *
     * @param currentPresentationId the presentation identity active at timer fire time.
     * @param expectedPresentationId the presentation identity that triggered the expansion.
     * @param mode current display mode.
     * @param expandedViewType current expanded view type.
     * @param isUserInteracting true when the user is engaged (reply keyboard open, gesture in flight).
     */
    fun evaluateCollapse(
        currentPresentationId: String,
        expectedPresentationId: String?,
        mode: IslandMode,
        expandedViewType: ExpandedViewType,
        isUserInteracting: Boolean,
        hasActiveNotification: Boolean = true
    ): Boolean {
        if (mode != IslandMode.EXPANDED) return false
        if (expandedViewType != ExpandedViewType.NOTIFICATION_FOCUSED) return false
        if (isUserInteracting) return false
        if (expectedPresentationId == null) return false

        // The priority manager schedules its own timeout on the same duration. Whichever fires
        // first wins the race: if the notification has already expired, activeEvent is null and
        // the presentation identity has moved to an idle token. A blank expanded card must never
        // be left behind, so absence of an active notification is itself sufficient to collapse.
        if (!hasActiveNotification) return true

        return currentPresentationId == expectedPresentationId
    }
}
