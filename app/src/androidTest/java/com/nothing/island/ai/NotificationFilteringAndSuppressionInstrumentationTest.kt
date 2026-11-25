package com.nothing.island.ai

import android.app.Notification
import android.content.Context
import android.media.session.MediaSession
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nothing.island.ai.notifications.NotificationFilter
import com.nothing.island.ai.notifications.NotificationPreferences
import com.nothing.island.ai.notifications.RawNotificationData
import com.nothing.island.ai.notifications.SuppressionCandidate
import com.nothing.island.ai.notifications.SuppressionDecision
import com.nothing.island.ai.notifications.SystemNotificationSuppressor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumentation tests exercising Android Notification framework objects alongside
 * [NotificationFilter] and [SystemNotificationSuppressor].
 */
@RunWith(AndroidJUnit4::class)
class NotificationFilteringAndSuppressionInstrumentationTest {

    @Test
    fun notificationFilter_excludesMediaStyleAndCallBuiltWithAndroidNotificationBuilder() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val filter = NotificationFilter(context)
        val prefs = NotificationPreferences.DEFAULTS

        val session = MediaSession(context, "test_media_session")
        try {
            val mediaNotif = Notification.Builder(context, "ch")
                .setContentTitle("Circles")
                .setContentText("Post Malone")
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setStyle(Notification.MediaStyle().setMediaSession(session.sessionToken))
                .setCategory(Notification.CATEGORY_TRANSPORT)
                .build()

            val hasMedia = mediaNotif.extras.containsKey(Notification.EXTRA_MEDIA_SESSION) ||
                mediaNotif.category == Notification.CATEGORY_TRANSPORT

            val droppedMedia = filter.filter(
                RawNotificationData(
                    key = "media_1",
                    packageName = "com.spotify.music",
                    title = "Circles",
                    text = "Post Malone",
                    category = mediaNotif.category,
                    hasMediaStyle = hasMedia
                ),
                preferences = prefs
            )
            assertNull("Media transport notification must be excluded from generic alerts", droppedMedia)
        } finally {
            session.release()
        }

        val droppedCall = filter.filter(
            RawNotificationData(
                key = "call_1",
                packageName = "com.google.android.dialer",
                title = "Incoming call",
                text = "Daisy",
                category = Notification.CATEGORY_CALL
            ),
            preferences = prefs
        )
        assertNull("Call notification must be excluded from generic alerts", droppedCall)

        val acceptedMessage = filter.filter(
            RawNotificationData(
                key = "msg_1",
                packageName = "com.whatsapp",
                title = "Alex",
                text = "Hello",
                category = Notification.CATEGORY_MESSAGE
            ),
            preferences = prefs
        )
        assertNotNull(acceptedMessage)
    }

    @Test
    fun systemSuppressor_respectsClearableAndOngoingRulesOnDevice() {
        val suppressor = SystemNotificationSuppressor()
        val prefs = NotificationPreferences.DEFAULTS.copy(hideSystemNotification = true)

        assertEquals(
            SuppressionDecision.CANCEL,
            suppressor.evaluate(
                SuppressionCandidate(
                    key = "k_clear",
                    postTime = 100L,
                    category = Notification.CATEGORY_MESSAGE,
                    isClearable = true,
                    isOngoing = false,
                    isGroupSummary = false,
                    hasMediaStyle = false,
                    acceptedByIsland = true
                ),
                prefs
            )
        )

        assertEquals(
            SuppressionDecision.SKIP_NOT_CLEARABLE,
            suppressor.evaluate(
                SuppressionCandidate(
                    key = "k_noclear",
                    postTime = 100L,
                    category = Notification.CATEGORY_SYSTEM,
                    isClearable = false,
                    isOngoing = false,
                    isGroupSummary = false,
                    hasMediaStyle = false,
                    acceptedByIsland = true
                ),
                prefs
            )
        )
    }
}
