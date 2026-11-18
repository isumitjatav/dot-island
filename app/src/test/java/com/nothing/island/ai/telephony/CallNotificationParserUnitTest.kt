package com.nothing.island.ai.telephony

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CallNotificationParserUnitTest {

    @Test
    fun nonCallCategory_isIgnored() {
        assertNull(
            CallNotificationParser.parse(
                CallSignal("msg", "Alex", "Hello", listOf("Reply"))
            )
        )
    }

    @Test
    fun incomingCall_withAnswerAction_isDetected() {
        val info = CallNotificationParser.parse(
            CallSignal(
                category = CallNotificationParser.CATEGORY_CALL,
                title = "Mom",
                text = "Incoming voice call",
                actionTitles = listOf("Answer", "Decline"),
            )
        )
        assertEquals("Mom", info?.callerDisplayName)
        assertTrue(info!!.isIncoming)
        assertFalse(info.isVideoCall)
    }

    @Test
    fun videoCallAction_isDetected() {
        val info = CallNotificationParser.parse(
            CallSignal(
                category = CallNotificationParser.CATEGORY_CALL,
                title = "Alex",
                text = null,
                actionTitles = listOf("Answer video", "Reject"),
            )
        )
        assertTrue(info!!.isVideoCall)
        assertTrue(info.isIncoming)
    }

    @Test
    fun ongoingCall_withoutAnswerAction_isNotIncoming() {
        val info = CallNotificationParser.parse(
            CallSignal(
                category = CallNotificationParser.CATEGORY_CALL,
                title = "Mom",
                text = "00:42",
                actionTitles = listOf("Speaker", "Mute"),
            )
        )
        assertFalse(info!!.isIncoming)
        assertEquals("Mom", info.callerDisplayName)
    }

    @Test
    fun blankCaller_isIgnored() {
        assertNull(
            CallNotificationParser.parse(
                CallSignal(CallNotificationParser.CATEGORY_CALL, "", "  ", listOf("Answer"))
            )
        )
    }

    @Test
    fun actionMatching_isCaseInsensitive() {
        val info = CallNotificationParser.parse(
            CallSignal(CallNotificationParser.CATEGORY_CALL, "Mom", null, listOf("ANSWER"))
        )
        assertTrue(info!!.isIncoming)
    }
}
