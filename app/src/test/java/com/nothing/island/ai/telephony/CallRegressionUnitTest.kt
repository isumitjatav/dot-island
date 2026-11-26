package com.nothing.island.ai.telephony

import android.app.Notification
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regression tests for CallStyle incoming call classification (CallPhase.INCOMING)
 * and CallController speaker state toggles.
 */
class CallRegressionUnitTest {

    @Test
    fun incomingCallStyleWithAnswerIntentStaysRingingWithoutEnglishActionLabels() {
        val signal = CallSignal(
            category = Notification.CATEGORY_CALL,
            title = "Aarav Sharma",
            text = "Mobile",
            actionTitles = listOf("उत्तर दें", "अस्वीकार करें"),
            showChronometer = false,
            subText = null,
            callType = CallNotificationParser.CALL_TYPE_INCOMING,
            hasAnswerIntent = true,
        )

        val info = CallNotificationParser.parse(signal)
        assertNotNull(info)
        assertEquals("Aarav Sharma", info!!.callerDisplayName)
        assertEquals(CallPhase.INCOMING, info.phase)
    }

    @Test
    fun incomingCallTypeWithoutAnswerTitleNeverBecomesDialingOrEnded() {
        val signal = CallSignal(
            category = Notification.CATEGORY_CALL,
            title = "WhatsApp Voice",
            text = "Ringing...",
            actionTitles = emptyList(),
            showChronometer = false,
            callType = CallNotificationParser.CALL_TYPE_INCOMING,
            hasAnswerIntent = false,
        )

        val info = CallNotificationParser.parse(signal)
        assertNotNull(info)
        assertEquals(CallPhase.INCOMING, info!!.phase)
    }

    @Test
    fun ongoingCallTypeWithChronometerClassifiesAsOngoing() {
        val signal = CallSignal(
            category = Notification.CATEGORY_CALL,
            title = "Aarav Sharma",
            text = "On call",
            actionTitles = listOf("Hang up"),
            showChronometer = true,
            callType = CallNotificationParser.CALL_TYPE_ONGOING,
            hasAnswerIntent = false,
            notificationWhenMs = 1_700_000_000_000L,
        )

        val info = CallNotificationParser.parse(signal)
        assertNotNull(info)
        assertEquals(CallPhase.ONGOING, info!!.phase)
        assertFalse(info.isDialing)
        assertEquals(1_700_000_000_000L, info.connectedAtMs)
    }

    @Test
    fun callControllerSpeakerStateToggleComputesInverseAndUpdatesFlow() {
        CallController.setSpeakerStateForTesting(false)
        assertFalse(CallController.isSpeakerOn.value)

        val nextOn = CallController.computeNextSpeakerState(CallController.isSpeakerOn.value)
        assertTrue(nextOn)
        CallController.setSpeakerStateForTesting(nextOn)
        assertTrue(CallController.isSpeakerOn.value)

        val nextOff = CallController.computeNextSpeakerState(CallController.isSpeakerOn.value)
        assertFalse(nextOff)
        CallController.setSpeakerStateForTesting(nextOff)
        assertFalse(CallController.isSpeakerOn.value)
    }

    @Test
    fun extractsSpeakerMuteHoldAndRecordFromCallStyleActions() {
        val actions = listOf(
            RawCallAction(title = "Mute", intent = "intent_mute"),
            RawCallAction(title = "Speaker", intent = "intent_speaker"),
            RawCallAction(title = "Hold", intent = "intent_hold"),
            RawCallAction(title = "Record", intent = "intent_record"),
            RawCallAction(title = "Hang up", intent = "intent_hangup"),
        )

        val matched = CallNotificationParser.matchCallActions(actions)
        assertEquals("intent_mute", matched.mute)
        assertEquals("intent_speaker", matched.speaker)
        assertEquals("intent_hold", matched.hold)
        assertEquals("intent_record", matched.record)
        assertEquals("intent_hangup", matched.hangUp)
        assertEquals(false, matched.muteStateHint)
        assertEquals(false, matched.holdStateHint)
        assertEquals(false, matched.recordStateHint)
        assertEquals(null, matched.speakerStateHint)
    }

    @Test
    fun toggledDialerLabelsAndSemanticActionsReflectActiveStates() {
        val actions = listOf(
            RawCallAction(title = "Unmute", intent = "intent_unmute", semanticAction = 9),
            RawCallAction(title = "Turn off speaker", intent = "intent_speaker_off"),
            RawCallAction(title = "Resume", intent = "intent_resume"),
            RawCallAction(title = "Stop recording", intent = "intent_rec_stop"),
        )

        val matched = CallNotificationParser.matchCallActions(
            actions = actions,
            callSubText = "On hold"
        )
        assertEquals("intent_unmute", matched.mute)
        assertEquals("intent_speaker_off", matched.speaker)
        assertEquals("intent_resume", matched.hold)
        assertEquals("intent_rec_stop", matched.record)
        assertEquals(true, matched.muteStateHint)
        assertEquals(true, matched.speakerStateHint)
        assertEquals(true, matched.holdStateHint)
        assertEquals(true, matched.recordStateHint)
    }
}
