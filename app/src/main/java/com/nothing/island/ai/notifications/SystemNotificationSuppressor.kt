package com.nothing.island.ai.notifications

import android.app.Notification
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

/**
 * Outcome of evaluating whether Nothing Island should suppress/cancel a system notification
 * when "Hide System Notification" is enabled.
 */
enum class SuppressionDecision {
    CANCEL,
    SKIP_PREFERENCE_DISABLED,
    SKIP_NOT_ACCEPTED,
    SKIP_CALL_NOTIFICATION,
    SKIP_MEDIA_NOTIFICATION,
    SKIP_NOT_CLEARABLE,
    SKIP_ONGOING_NOTIFICATION,
    SKIP_GROUP_SUMMARY,
    SKIP_ALREADY_CANCELLED_LOOP
}

/**
 * Input metadata for [SystemNotificationSuppressor].
 */
data class SuppressionCandidate(
    val key: String,
    val postTime: Long,
    val category: String?,
    val isClearable: Boolean,
    val isOngoing: Boolean,
    val isGroupSummary: Boolean,
    val hasMediaStyle: Boolean,
    val acceptedByIsland: Boolean
)

/**
 * Explicit, deterministic policy governing system heads-up notification cancellation when
 * `prefs.hideSystemNotification` is enabled.
 *
 * Android Platform Restrictions:
 * - Unprivileged NotificationListenerService apps cannot hide heads-up banners while keeping the
 *   shade entry intact for arbitrary apps; `cancelNotification(key)` removes the notification
 *   from the Android shade as well.
 * - Non-clearable and ongoing notifications (`FLAG_ONGOING_EVENT` / `FLAG_FOREGROUND_SERVICE`)
 *   cannot be user-dismissed safely and are excluded.
 * - Call (`CATEGORY_CALL`) and Media (`CATEGORY_TRANSPORT` / MediaStyle) notifications are
 *   strictly excluded so dialer and active media controls are never disrupted.
 * - Group summary notifications (`FLAG_GROUP_SUMMARY`) are excluded to prevent cancelling an
 *   entire notification bundle when a single child alert posts.
 * - Recent cancellations are tracked by `(key, postTime)` within a dedupe window to prevent
 *   cancellation loops if an app rapidly re-posts the same key.
 */
class SystemNotificationSuppressor(
    private val dedupeWindowMs: Long = 3_000L,
    private val timeProvider: () -> Long = System::currentTimeMillis
) {
    private data class CancellationRecord(
        val postTime: Long,
        val cancelledAtMs: Long
    )

    private val selfCancelledKeys =
        Collections.newSetFromMap(ConcurrentHashMap<String, Boolean>())
    private val recentCancellations = ConcurrentHashMap<String, CancellationRecord>()

    fun evaluate(
        candidate: SuppressionCandidate,
        preferences: NotificationPreferences
    ): SuppressionDecision {
        if (!preferences.hideSystemNotification) {
            return SuppressionDecision.SKIP_PREFERENCE_DISABLED
        }
        if (!candidate.acceptedByIsland) {
            return SuppressionDecision.SKIP_NOT_ACCEPTED
        }
        if (candidate.category == Notification.CATEGORY_CALL) {
            return SuppressionDecision.SKIP_CALL_NOTIFICATION
        }
        if (candidate.category == Notification.CATEGORY_TRANSPORT || candidate.hasMediaStyle) {
            return SuppressionDecision.SKIP_MEDIA_NOTIFICATION
        }
        if (!candidate.isClearable) {
            return SuppressionDecision.SKIP_NOT_CLEARABLE
        }
        if (candidate.isOngoing) {
            return SuppressionDecision.SKIP_ONGOING_NOTIFICATION
        }
        if (candidate.isGroupSummary) {
            return SuppressionDecision.SKIP_GROUP_SUMMARY
        }

        val now = timeProvider()
        val prev = recentCancellations[candidate.key]
        if (prev != null) {
            val withinWindow = (now - prev.cancelledAtMs) < dedupeWindowMs
            val samePost = candidate.postTime > 0L && prev.postTime == candidate.postTime
            if (samePost || withinWindow) {
                return SuppressionDecision.SKIP_ALREADY_CANCELLED_LOOP
            }
        }
        return SuppressionDecision.CANCEL
    }

    /**
     * Marks [key] as cancelled by Nothing Island so the incoming `onNotificationRemoved`
     * callback is ignored and does not strip the alert from Island's card or history.
     */
    fun recordSelfCancellation(key: String, postTime: Long) {
        val now = timeProvider()
        selfCancelledKeys.add(key)
        recentCancellations[key] = CancellationRecord(postTime = postTime, cancelledAtMs = now)
        pruneExpired(now)
    }

    /**
     * Returns true once if [key] removal was triggered by our own cancellation.
     */
    fun consumeSelfCancelledRemoval(key: String): Boolean {
        return selfCancelledKeys.remove(key)
    }

    fun clearAll() {
        selfCancelledKeys.clear()
        recentCancellations.clear()
    }

    private fun pruneExpired(nowMs: Long) {
        if (recentCancellations.size <= 64) return
        val threshold = nowMs - (dedupeWindowMs * 2L)
        recentCancellations.entries.removeIf { it.value.cancelledAtMs < threshold }
    }
}
