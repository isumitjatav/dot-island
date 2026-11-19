package com.nothing.island.ai.notifications

import com.nothing.island.ai.engine.IslandEvent
import com.nothing.island.ai.engine.IslandPriority
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CallAndMessageRegressionTest {

    @Test
    fun callConnected_preemptsRinging() {
        // Root cause of the "ANSWER does nothing" bug: ACTIVE_TELEPHONY (85)
        // was lower than CRITICAL_TELEPHONY (100), so Rule 2 rejected it.
        assertTrue(IslandPriority.CRITICAL_TELEPHONY >= IslandPriority.ACTIVE_TELEPHONY)
        val ringing = IslandEvent.IncomingCallRinging(
            id = "ring", callId = "c1", callerDisplayName = "Papa"
        )
        val connected = IslandEvent.CallConnected(
            id = "c1", callId = "c1", callerDisplayName = "Papa"
        )
        assertTrue(connected.priority >= ringing.priority)
    }

    @Test
    fun timedNotification_expiresAfterDisplayDuration() {
        val event = IslandEvent.NotificationPosted(
            id = "msg-1",
            packageName = "com.whatsapp",
            title = "Alex",
            text = "Hello",
            timestamp = 0L,
        )
        assertTrue(event.timeoutMs > 0L)
    }

    @Test
    fun formatCallDuration_countsUp() {
        assertEquals("00:01", com.nothing.island.ai.ui.components.formatCallDuration(1))
        assertEquals("00:42", com.nothing.island.ai.ui.components.formatCallDuration(42))
    }
}
