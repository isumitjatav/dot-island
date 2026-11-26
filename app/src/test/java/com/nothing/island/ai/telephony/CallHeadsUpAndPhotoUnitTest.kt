package com.nothing.island.ai.telephony

import android.app.Notification
import android.media.AudioManager
import android.telephony.TelephonyManager
import com.nothing.island.ai.engine.IslandEvent
import com.nothing.island.ai.shouldTriggerIdleSwipeResume
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CallHeadsUpAndPhotoUnitTest {

    @Test
    fun idlePillSwipeResume_triggersOnlyWhenResumableCachedAndThresholdMet() {
        val threshold = 84f // ~28dp at 3x density

        assertTrue(
            shouldTriggerIdleSwipeResume(
                hasResumableMedia = true,
                isMusicPlaying = false,
                isExpanded = false,
                hasActivePriorityEvent = false,
                dragDistancePx = -85f,
                thresholdPx = threshold,
            )
        )
        assertTrue(
            shouldTriggerIdleSwipeResume(
                hasResumableMedia = true,
                isMusicPlaying = false,
                isExpanded = false,
                hasActivePriorityEvent = false,
                dragDistancePx = 90f,
                thresholdPx = threshold,
            )
        )

        // Sub-threshold drag must NOT trigger resume
        assertFalse(
            shouldTriggerIdleSwipeResume(
                hasResumableMedia = true,
                isMusicPlaying = false,
                isExpanded = false,
                hasActivePriorityEvent = false,
                dragDistancePx = 40f,
                thresholdPx = threshold,
            )
        )

        // Without lastResumableMedia, swipe never triggers resume
        assertFalse(
            shouldTriggerIdleSwipeResume(
                hasResumableMedia = false,
                isMusicPlaying = false,
                isExpanded = false,
                hasActivePriorityEvent = false,
                dragDistancePx = 120f,
                thresholdPx = threshold,
            )
        )

        // Expanded card or active priority event must NOT trigger idle swipe-to-resume
        assertFalse(
            shouldTriggerIdleSwipeResume(
                hasResumableMedia = true,
                isMusicPlaying = false,
                isExpanded = true,
                hasActivePriorityEvent = false,
                dragDistancePx = 120f,
                thresholdPx = threshold,
            )
        )
        assertFalse(
            shouldTriggerIdleSwipeResume(
                hasResumableMedia = true,
                isMusicPlaying = false,
                isExpanded = false,
                hasActivePriorityEvent = true,
                dragDistancePx = 120f,
                thresholdPx = threshold,
            )
        )
    }

    @Test
    fun dialerPolicy_neverSnoozes_endsImmediatelyOnRemovalAndWatchdogDetectsStaleCalls() {
        // Dialer notifications must NEVER be snoozed so CallStyle actions and removal stay live
        assertFalse(DialerHeadsUpPolicy.shouldSnoozeDialerCall("com.google.android.dialer", hasCallInfo = true))
        assertFalse(DialerHeadsUpPolicy.shouldSnoozeDialerCall("com.android.dialer", hasCallInfo = true))
        assertFalse(DialerHeadsUpPolicy.shouldSnoozeDialerCall("com.android.incallui", hasCallInfo = true))
        assertFalse(DialerHeadsUpPolicy.shouldSnoozeDialerCall("com.android.server.telecom", hasCallInfo = true))

        // onNotificationRemoved for active call ends the call immediately
        assertTrue(
            DialerHeadsUpPolicy.shouldEndCallOnNotificationRemoval(
                removedKey = "call_1",
                activeCallKey = "call_1",
            )
        )
        assertFalse(
            DialerHeadsUpPolicy.shouldEndCallOnNotificationRemoval(
                removedKey = "other_key",
                activeCallKey = "call_1",
            )
        )

        // Watchdog clears stale call timers when AudioManager.mode == MODE_NORMAL and no dialer notif exists
        assertTrue(
            DialerHeadsUpPolicy.shouldAutoEndStaleCall(
                audioMode = AudioManager.MODE_NORMAL,
                hasActiveDialerNotification = false,
                isDebugSyntheticCall = false,
            )
        )
        assertFalse(
            DialerHeadsUpPolicy.shouldAutoEndStaleCall(
                audioMode = AudioManager.MODE_IN_CALL,
                hasActiveDialerNotification = false,
            )
        )
        assertFalse(
            DialerHeadsUpPolicy.shouldAutoEndStaleCall(
                audioMode = AudioManager.MODE_IN_COMMUNICATION,
                hasActiveDialerNotification = false,
            )
        )
        assertFalse(
            DialerHeadsUpPolicy.shouldAutoEndStaleCall(
                audioMode = AudioManager.MODE_NORMAL,
                hasActiveDialerNotification = true,
            )
        )

        // Telephony transition back to IDLE immediately ends the active call
        assertEquals(
            TelephonyCallTransition.END_CALL,
            DialerHeadsUpPolicy.resolveTelephonyStateTransition(
                previousState = TelephonyManager.CALL_STATE_OFFHOOK,
                newState = TelephonyManager.CALL_STATE_IDLE,
                hasActiveCall = true,
            )
        )
    }

    @Test
    fun callerPhotoResolver_fallsBackGracefullyWhenPermissionDeniedOrMissingPhoto() {
        val notifToken = "notif_person_photo"
        val contactToken = "contacts_high_res_photo"

        // Permission denied -> skips ContactsContract lookup and uses notification Person/largeIcon fallback
        var contactsQueried = false
        val deniedResolver = GenericCallerPhotoResolver(
            hasContactsPermission = { false },
            lookupPhotoByPhone = { contactsQueried = true; contactToken },
            lookupPhotoByName = { contactsQueried = true; contactToken },
            fallbackNotificationPhoto = { notifToken },
        )
        assertEquals(notifToken, deniedResolver.resolve("Alex Rivera", "+15551234567"))
        assertFalse(contactsQueried)

        // Permission granted, contact has NO saved photo, notification has NO icon -> returns null for initials fallback
        val emptyResolver = GenericCallerPhotoResolver<String>(
            hasContactsPermission = { true },
            lookupPhotoByPhone = { null },
            lookupPhotoByName = { null },
            fallbackNotificationPhoto = { null },
        )
        assertNull(emptyResolver.resolve("Unknown Caller", null))

        // Permission granted with saved photo -> resolves via phone lookup and caches result
        var lookupCalls = 0
        val grantedResolver = GenericCallerPhotoResolver(
            hasContactsPermission = { true },
            lookupPhotoByPhone = { phone ->
                lookupCalls++
                if (phone == "+15551234567") contactToken else null
            },
            lookupPhotoByName = { null },
            fallbackNotificationPhoto = { notifToken },
        )
        assertNotNull(grantedResolver.resolve("Alex Rivera", "+1 (555) 123-4567"))
        assertEquals(contactToken, grantedResolver.resolve("Alex Rivera", "+1 (555) 123-4567"))
        assertEquals(1, lookupCalls)
    }

    @Test
    fun outgoingCall_staysDialingWithZeroTimerUntilChronometerConnects() {
        val dialingInfo = CallNotificationParser.parse(
            CallSignal(
                category = Notification.CATEGORY_CALL,
                title = "Alex Rivera",
                text = "Calling...",
                actionTitles = listOf("Speaker", "Hang up"),
                showChronometer = false,
                subText = "Dialing",
                callType = CallNotificationParser.CALL_TYPE_ONGOING,
                hasAnswerIntent = false,
                notificationWhenMs = 1_700_000_000_000L,
            )
        )
        assertNotNull(dialingInfo)
        assertEquals(CallPhase.DIALING, dialingInfo!!.phase)
        assertTrue(dialingInfo.isDialing)
        assertEquals(0L, dialingInfo.connectedAtMs)
        assertEquals(
            0L,
            CallNotificationParser.computeCallDurationSeconds(
                isDialing = dialingInfo.isDialing,
                connectedAtMs = dialingInfo.connectedAtMs,
                nowMs = 1_700_000_012_000L,
            )
        )

        // When recipient answers, Dialer flips showChronometer = true with notification.when > 0
        val connectTimeMs = 1_700_000_015_000L
        val connectedInfo = CallNotificationParser.parse(
            CallSignal(
                category = Notification.CATEGORY_CALL,
                title = "Alex Rivera",
                text = "On call",
                actionTitles = listOf("Speaker", "Mute", "Hang up"),
                showChronometer = true,
                callType = CallNotificationParser.CALL_TYPE_ONGOING,
                hasAnswerIntent = false,
                notificationWhenMs = connectTimeMs,
            )
        )
        assertNotNull(connectedInfo)
        assertEquals(CallPhase.ONGOING, connectedInfo!!.phase)
        assertFalse(connectedInfo.isDialing)
        assertEquals(connectTimeMs, connectedInfo.connectedAtMs)
        assertEquals(
            0L,
            CallNotificationParser.computeCallDurationSeconds(
                isDialing = connectedInfo.isDialing,
                connectedAtMs = connectedInfo.connectedAtMs,
                nowMs = connectTimeMs,
            )
        )
        assertEquals(
            5L,
            CallNotificationParser.computeCallDurationSeconds(
                isDialing = connectedInfo.isDialing,
                connectedAtMs = connectedInfo.connectedAtMs,
                nowMs = connectTimeMs + 5_000L,
            )
        )

        // Ensure event payload diff does not drop transition from CALLING... (0s) to 00:00 (0s)
        val dialingEvent = IslandEvent.CallConnected(
            id = "call_1",
            callId = "call_1",
            callerDisplayName = "Alex Rivera",
            durationSeconds = 0L,
            isDialing = true,
            connectedAtMs = 0L,
        )
        val connectedAtZeroEvent = IslandEvent.CallConnected(
            id = "call_1",
            callId = "call_1",
            callerDisplayName = "Alex Rivera",
            durationSeconds = 0L,
            isDialing = false,
            connectedAtMs = connectTimeMs,
        )
        assertFalse(dialingEvent.hasIdenticalBusinessPayload(connectedAtZeroEvent))
    }
}
