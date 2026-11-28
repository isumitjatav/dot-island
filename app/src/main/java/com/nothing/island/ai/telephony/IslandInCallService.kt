package com.nothing.island.ai.telephony

import android.os.Build
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.InCallService
import android.telecom.VideoProfile
import android.util.Log
import com.nothing.island.ai.IslandOverlayController
import com.nothing.island.ai.engine.IslandEvent
import com.nothing.island.ai.engine.IslandEventEngine
import java.util.concurrent.ConcurrentHashMap

/**
 * IslandInCallService
 *
 * Native InCallService implementation for handling telephony calls directly
 * within Dot Island when granted the default dialer role (RoleManager.ROLE_DIALER).
 *
 * Requires android.permission.BIND_INCALL_SERVICE and IN_CALL_SERVICE_UI = true.
 * Routes onCallAdded / onCallRemoved to IslandEventEngine and wires Answer
 * (call.answer(VideoProfile.STATE_AUDIO_ONLY)) and Decline (call.disconnect()).
 */
class IslandInCallService : InCallService() {

    companion object {
        private const val TAG = "IslandInCallService"

        @Volatile
        var instance: IslandInCallService? = null
            private set
    }

    private val callMap = ConcurrentHashMap<String, Call>()
    private var eventEngine: IslandEventEngine? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
        Log.i(TAG, "IslandInCallService created")
        try {
            val controller = IslandOverlayController.getInstance(applicationContext)
            controller.setInCallService(this)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to register with IslandOverlayController on create", e)
        }
    }

    override fun onDestroy() {
        Log.i(TAG, "IslandInCallService destroyed")
        try {
            val controller = IslandOverlayController.getInstance(applicationContext)
            controller.clearInCallService()
        } catch (_: Exception) {}
        callMap.clear()
        if (instance === this) {
            instance = null
        }
        super.onDestroy()
    }

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        val callId = System.identityHashCode(call).toString()
        callMap[callId] = call
        Log.i(TAG, "Call added: $callId, state=${call.state}")

        val details = call.details
        val callerName = details?.callerDisplayName?.takeIf { it.isNotBlank() }
            ?: details?.handle?.schemeSpecificPart
            ?: "Unknown"

        val isVideoCall = details?.videoState == VideoProfile.STATE_BIDIRECTIONAL

        // If the call is incoming, emit IncomingCallRinging
        val isIncoming = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            details?.callDirection == Call.Details.DIRECTION_INCOMING
        } else {
            call.state == Call.STATE_RINGING
        }

        if (isIncoming || call.state == Call.STATE_RINGING) {
            val event = IslandEvent.IncomingCallRinging(
                id = "call_$callId",
                callId = callId,
                callerDisplayName = callerName,
                isVideoCall = isVideoCall
            )
            eventEngine?.emit(event)
            Log.i(TAG, "Emitted IncomingCallRinging for $callId: $callerName")
        } else if (call.state == Call.STATE_ACTIVE) {
            val event = IslandEvent.CallConnected(
                id = "call_connected_$callId",
                callId = callId,
                callerDisplayName = callerName,
                durationSeconds = 0L,
                isDialing = false,
                connectedAtMs = details?.connectTimeMillis ?: System.currentTimeMillis()
            )
            eventEngine?.emit(event)
            Log.i(TAG, "Emitted CallConnected for $callId: $callerName")
        }

        // Register callback for call state transitions
        call.registerCallback(object : Call.Callback() {
            override fun onStateChanged(c: Call, state: Int) {
                super.onStateChanged(c, state)
                Log.d(TAG, "Call $callId state changed to $state")
                when (state) {
                    Call.STATE_ACTIVE -> {
                        val event = IslandEvent.CallConnected(
                            id = "call_connected_$callId",
                            callId = callId,
                            callerDisplayName = callerName,
                            durationSeconds = 0L,
                            isDialing = false,
                            connectedAtMs = c.details?.connectTimeMillis ?: System.currentTimeMillis()
                        )
                        eventEngine?.emit(event)
                    }
                    Call.STATE_DISCONNECTED -> {
                        callMap.remove(callId)
                        val event = IslandEvent.CallEnded(
                            id = "call_ended_$callId",
                            callId = callId
                        )
                        eventEngine?.emit(event)
                    }
                }
            }
        })
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        val callId = System.identityHashCode(call).toString()
        callMap.remove(callId)
        Log.i(TAG, "Call removed: $callId")

        val event = IslandEvent.CallEnded(
            id = "call_ended_$callId",
            callId = callId
        )
        eventEngine?.emit(event)
        Log.i(TAG, "Emitted CallEnded for $callId")
    }

    /**
     * Sets the event engine for emitting call events.
     * Called by IslandOverlayController after service binding.
     */
    fun setEventEngine(engine: IslandEventEngine) {
        this.eventEngine = engine
        Log.d(TAG, "EventEngine attached to IslandInCallService")
    }

    /**
     * Checks if there are currently any managed active calls.
     */
    fun hasActiveCalls(): Boolean = callMap.isNotEmpty()

    /**
     * Answers the active ringing call using audio-only video profile.
     */
    fun answerActiveCall(): Boolean {
        val ringingCall = callMap.values.firstOrNull { it.state == Call.STATE_RINGING }
            ?: callMap.values.firstOrNull()
        return if (ringingCall != null) {
            ringingCall.answer(VideoProfile.STATE_AUDIO_ONLY)
            Log.i(TAG, "Answered active call via InCallService: ${System.identityHashCode(ringingCall)}")
            true
        } else {
            Log.w(TAG, "No ringing call to answer")
            false
        }
    }

    /**
     * Declines / disconnects the active call.
     */
    fun declineActiveCall(): Boolean {
        val call = callMap.values.firstOrNull()
        return if (call != null) {
            call.disconnect()
            Log.i(TAG, "Declined/disconnected active call via InCallService: ${System.identityHashCode(call)}")
            true
        } else {
            Log.w(TAG, "No active call to decline")
            false
        }
    }

    /**
     * Answers the specified call by ID.
     */
    fun answerCall(callId: String): Boolean {
        val call = callMap[callId]
        return if (call != null) {
            call.answer(VideoProfile.STATE_AUDIO_ONLY)
            Log.i(TAG, "Answered call: $callId")
            true
        } else {
            false
        }
    }

    /**
     * Declines/disconnects the specified call by ID.
     */
    fun declineCall(callId: String): Boolean {
        val call = callMap[callId]
        return if (call != null) {
            call.disconnect()
            Log.i(TAG, "Disconnected call: $callId")
            true
        } else {
            false
        }
    }
}