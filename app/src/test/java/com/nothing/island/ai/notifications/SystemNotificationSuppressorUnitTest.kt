package com.nothing.island.ai.notifications

import android.app.Notification
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SystemNotificationSuppressorUnitTest {

    private val enabledPrefs = NotificationPreferences.DEFAULTS.copy(hideSystemNotification = true)
    private val disabledPrefs = NotificationPreferences.DEFAULTS.copy(hideSystemNotification = false)

    private fun candidate(
        key: String = "0|com.chat.app|1|null|1000",
        postTime: Long = 1000L,
        category: String? = Notification.CATEGORY_MESSAGE,
        isClearable: Boolean = true,
        isOngoing: Boolean = false,
        isGroupSummary: Boolean = false,
        hasMediaStyle: Boolean = false,
        acceptedByIsland: Boolean = true
    ) = SuppressionCandidate(
        key = key,
        postTime = postTime,
        category = category,
        isClearable = isClearable,
        isOngoing = isOngoing,
        isGroupSummary = isGroupSummary,
        hasMediaStyle = hasMediaStyle,
        acceptedByIsland = acceptedByIsland
    )

    @Test
    fun clearableAcceptedNotification_isCancelledWhenPreferenceEnabled() {
        val suppressor = SystemNotificationSuppressor()
        assertEquals(SuppressionDecision.CANCEL, suppressor.evaluate(candidate(), enabledPrefs))
        assertEquals(SuppressionDecision.SKIP_PREFERENCE_DISABLED, suppressor.evaluate(candidate(), disabledPrefs))
    }

    @Test
    fun callAndMediaNotifications_areIntentionallyExcluded() {
        val suppressor = SystemNotificationSuppressor()
        assertEquals(
            SuppressionDecision.SKIP_CALL_NOTIFICATION,
            suppressor.evaluate(candidate(category = Notification.CATEGORY_CALL), enabledPrefs)
        )
        assertEquals(
            SuppressionDecision.SKIP_MEDIA_NOTIFICATION,
            suppressor.evaluate(candidate(category = Notification.CATEGORY_TRANSPORT), enabledPrefs)
        )
        assertEquals(
            SuppressionDecision.SKIP_MEDIA_NOTIFICATION,
            suppressor.evaluate(candidate(hasMediaStyle = true), enabledPrefs)
        )
    }

    @Test
    fun ongoingNonClearableAndGroupSummaryNotifications_areExcluded() {
        val suppressor = SystemNotificationSuppressor()
        assertEquals(
            SuppressionDecision.SKIP_ONGOING_NOTIFICATION,
            suppressor.evaluate(candidate(isOngoing = true), enabledPrefs)
        )
        assertEquals(
            SuppressionDecision.SKIP_NOT_CLEARABLE,
            suppressor.evaluate(candidate(isClearable = false), enabledPrefs)
        )
        assertEquals(
            SuppressionDecision.SKIP_GROUP_SUMMARY,
            suppressor.evaluate(candidate(isGroupSummary = true), enabledPrefs)
        )
    }

    @Test
    fun repeatedCancellation_preventsLoopAndConsumesSelfRemovalOnce() {
        var now = 5000L
        val suppressor = SystemNotificationSuppressor(dedupeWindowMs = 3000L, timeProvider = { now })
        val item = candidate(key = "k1", postTime = 42L)

        assertEquals(SuppressionDecision.CANCEL, suppressor.evaluate(item, enabledPrefs))
        suppressor.recordSelfCancellation("k1", postTime = 42L)

        // Rapid re-post within window or same postTime must not loop-cancel
        now += 500L
        assertEquals(
            SuppressionDecision.SKIP_ALREADY_CANCELLED_LOOP,
            suppressor.evaluate(item.copy(postTime = 43L), enabledPrefs)
        )

        assertTrue(suppressor.consumeSelfCancelledRemoval("k1"))
        assertFalse(suppressor.consumeSelfCancelledRemoval("k1"))
    }
}
