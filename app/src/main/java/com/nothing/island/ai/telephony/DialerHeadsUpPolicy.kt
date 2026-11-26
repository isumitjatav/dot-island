package com.nothing.island.ai.telephony

import android.media.AudioManager
import android.telephony.TelephonyManager

enum class TelephonyCallTransition {
    NO_CHANGE,
    TRANSITION_TO_OFFHOOK,
    END_CALL,
}

/**
 * Pure policy governing dialer call lifecycle updates, notification-based call teardown,
 * and stale-call safety watchdog evaluation.
 *
 * Dialer notifications are intentionally NEVER snoozed so Android continues delivering
 * live `CallStyle` action updates (Mute, Speaker, Hold, Hang Up) across RINGING -> OFFHOOK
 * and delivers `onNotificationRemoved` immediately upon hangup.
 */
object DialerHeadsUpPolicy {

    val DIALER_PACKAGES: Set<String> = setOf(
        "com.google.android.dialer",
        "com.android.dialer",
        "com.android.incallui",
        "com.android.server.telecom",
    )

    private val ACTIVE_CALL_AUDIO_MODES: Set<Int> = setOf(
        AudioManager.MODE_IN_CALL,
        AudioManager.MODE_IN_COMMUNICATION,
        AudioManager.MODE_RINGTONE,
    )

    /** Dialer notifications must stay active (never snoozed) so CallStyle actions & removal stay live. */
    @Suppress("UNUSED_PARAMETER")
    fun shouldSnoozeDialerCall(
        packageName: String?,
        hasCallInfo: Boolean,
    ): Boolean = false

    fun shouldEndCallOnNotificationRemoval(
        removedKey: String,
        activeCallKey: String?,
        @Suppress("UNUSED_PARAMETER") removalReason: Int? = null,
        @Suppress("UNUSED_PARAMETER") isCallSnoozed: Boolean = false,
        @Suppress("UNUSED_PARAMETER") isTelephonyCallActive: Boolean = false,
    ): Boolean = activeCallKey != null && removedKey == activeCallKey

    /**
     * Safety watchdog check: returns true if a non-synthetic call is still shown while
     * [AudioManager.getMode] is back in [AudioManager.MODE_NORMAL] (neither in-call,
     * VoIP communication, nor ringtone) AND no active dialer/call notification exists.
     */
    fun shouldAutoEndStaleCall(
        audioMode: Int,
        hasActiveDialerNotification: Boolean,
        isDebugSyntheticCall: Boolean = false,
    ): Boolean {
        if (isDebugSyntheticCall) return false
        return audioMode !in ACTIVE_CALL_AUDIO_MODES && !hasActiveDialerNotification
    }

    fun resolveTelephonyStateTransition(
        previousState: Int,
        newState: Int,
        hasActiveCall: Boolean,
    ): TelephonyCallTransition {
        if (!hasActiveCall) return TelephonyCallTransition.NO_CHANGE
        return when {
            newState == TelephonyManager.CALL_STATE_IDLE -> TelephonyCallTransition.END_CALL
            previousState == TelephonyManager.CALL_STATE_RINGING &&
                newState == TelephonyManager.CALL_STATE_OFFHOOK -> TelephonyCallTransition.TRANSITION_TO_OFFHOOK
            else -> TelephonyCallTransition.NO_CHANGE
        }
    }
}
