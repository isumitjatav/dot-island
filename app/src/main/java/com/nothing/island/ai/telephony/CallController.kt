package com.nothing.island.ai.telephony

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.telecom.TelecomManager
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * R9 call controls. Prefers TelecomManager (the reliable, documented API) and
 * falls back to the dialer notification's own actions when the optional
 * ANSWER_PHONE_CALLS permission has not been granted.
 */
object CallController {
    private const val TAG = "CallController"

    private val _isSpeakerOn = MutableStateFlow(false)
    val isSpeakerOn: StateFlow<Boolean> = _isSpeakerOn.asStateFlow()

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    private val _isOnHold = MutableStateFlow(false)
    val isOnHold: StateFlow<Boolean> = _isOnHold.asStateFlow()

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    @Volatile
    private var audioDeviceListenerRegistered = false

    fun ensureAudioDeviceListener(context: Context) {
        if (audioDeviceListenerRegistered || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        val appContext = context.applicationContext ?: context
        val am = appContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        try {
            am.addOnCommunicationDeviceChangedListener(appContext.mainExecutor) { device ->
                val speakerActive = device?.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
                _isSpeakerOn.value = speakerActive
                Log.d(TAG, "Communication device changed: type=${device?.type}, speakerActive=$speakerActive")
            }
            audioDeviceListenerRegistered = true
        } catch (e: Exception) {
            Log.w(TAG, "Unable to register OnCommunicationDeviceChangedListener", e)
        }
    }

    fun syncFromNotification(
        context: Context,
        speakerStateHint: Boolean?,
        muteStateHint: Boolean?,
        holdStateHint: Boolean?,
        recordingStateHint: Boolean?,
    ) {
        ensureAudioDeviceListener(context)
        if (speakerStateHint != null) {
            _isSpeakerOn.value = speakerStateHint
        } else {
            isSpeakerOn(context)
        }
        if (muteStateHint != null) {
            _isMuted.value = muteStateHint
        }
        if (holdStateHint != null) {
            _isOnHold.value = holdStateHint
        }
        if (recordingStateHint != null) {
            _isRecording.value = recordingStateHint
        }
    }

    fun resetCallStates() {
        _isSpeakerOn.value = false
        _isMuted.value = false
        _isOnHold.value = false
        _isRecording.value = false
    }

    fun canControlCalls(context: Context): Boolean =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ANSWER_PHONE_CALLS,
        ) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    fun answer(context: Context): Boolean {
        if (CallActionRegistry.answer(context)) {
            Log.d(TAG, "Answered through CallActionRegistry PendingIntent")
            return true
        }
        if (canControlCalls(context)) {
            val telecom = context.getSystemService(TelecomManager::class.java)
            try {
                telecom?.acceptRingingCall()
                Log.d(TAG, "Answered through TelecomManager fallback")
                return true
            } catch (se: SecurityException) {
                Log.w(TAG, "ANSWER_PHONE_CALLS rejected during answer", se)
            } catch (e: Exception) {
                Log.w(TAG, "TelecomManager answer failed", e)
            }
        }
        return false
    }

    @SuppressLint("MissingPermission")
    fun decline(context: Context): Boolean {
        val appCtx = context.applicationContext ?: context
        if (CallActionRegistry.decline(appCtx) || CallActionRegistry.hangUp(appCtx)) {
            Log.d(TAG, "Declined/hung up through CallActionRegistry PendingIntent")
            return true
        }
        if (canControlCalls(appCtx)) {
            val telecom = appCtx.getSystemService(TelecomManager::class.java)
            try {
                telecom?.endCall()
                Log.d(TAG, "Declined through TelecomManager fallback")
                return true
            } catch (se: SecurityException) {
                Log.w(TAG, "ANSWER_PHONE_CALLS rejected during decline", se)
            } catch (e: Exception) {
                Log.w(TAG, "TelecomManager decline failed", e)
            }
        }
        return false
    }

    fun isSpeakerOn(context: Context): Boolean {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return false
        @Suppress("DEPRECATION")
        val legacySpeaker = am.isSpeakerphoneOn
        val active = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            am.communicationDevice?.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER || legacySpeaker
        } else {
            legacySpeaker
        }
        _isSpeakerOn.value = active
        return active
    }

    internal fun computeNextSpeakerState(currentlyOn: Boolean): Boolean = !currentlyOn

    internal fun setSpeakerStateForTesting(on: Boolean) {
        _isSpeakerOn.value = on
    }

    fun toggleSpeaker(context: Context): Boolean {
        ensureAudioDeviceListener(context)
        val currentlyOn = isSpeakerOn(context)
        val turningOn = computeNextSpeakerState(currentlyOn)

        // 1. Prefer Dialer Notification.Action PendingIntent first so Telecom / Google Dialer
        // switches real cellular audio routing natively without overlay restrictions.
        if (CallActionRegistry.toggleSpeaker(context)) {
            _isSpeakerOn.value = turningOn
            Log.d(TAG, "Speaker toggled via Dialer PendingIntent -> optimistic $turningOn")
            return turningOn
        }

        // 2. Fallback to AudioManager when no Dialer Speaker PendingIntent is exposed (e.g. VoIP apps).
        val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return currentlyOn
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (turningOn) {
                    val speaker = am.availableCommunicationDevices.firstOrNull {
                        it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
                    }
                    val commSuccess = speaker != null && am.setCommunicationDevice(speaker)
                    Log.d(TAG, "setCommunicationDevice(speaker) result: $commSuccess (mode=${am.mode})")
                    if (!commSuccess) {
                        if (am.mode == AudioManager.MODE_NORMAL) {
                            am.mode = AudioManager.MODE_IN_COMMUNICATION
                        }
                        @Suppress("DEPRECATION")
                        am.isSpeakerphoneOn = true
                    }
                } else {
                    am.clearCommunicationDevice()
                    @Suppress("DEPRECATION")
                    am.isSpeakerphoneOn = false
                }
            } else {
                if (am.mode == AudioManager.MODE_NORMAL) {
                    @Suppress("DEPRECATION")
                    am.mode = AudioManager.MODE_IN_COMMUNICATION
                }
                @Suppress("DEPRECATION")
                am.isSpeakerphoneOn = turningOn
            }
            _isSpeakerOn.value = turningOn
            Log.d(TAG, "Speaker toggled via AudioManager fallback: now $turningOn (mode=${am.mode})")
            return turningOn
        } catch (e: Exception) {
            Log.w(TAG, "Failed to toggle speaker", e)
            _isSpeakerOn.value = currentlyOn
            return currentlyOn
        }
    }

    fun toggleMute(context: Context): Boolean {
        val nextMuted = !_isMuted.value
        if (CallActionRegistry.toggleMute(context)) {
            _isMuted.value = nextMuted
            Log.d(TAG, "Mute toggled via Dialer PendingIntent -> optimistic $nextMuted")
            return nextMuted
        }
        val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        if (am != null) {
            try {
                am.isMicrophoneMute = nextMuted
                _isMuted.value = nextMuted
                Log.d(TAG, "Mute toggled via AudioManager fallback -> $nextMuted")
                return nextMuted
            } catch (e: Exception) {
                Log.w(TAG, "AudioManager.isMicrophoneMute fallback failed", e)
            }
        }
        return _isMuted.value
    }

    fun toggleHold(context: Context): Boolean {
        val nextHold = !_isOnHold.value
        if (CallActionRegistry.toggleHold(context)) {
            _isOnHold.value = nextHold
            Log.d(TAG, "Hold toggled via Dialer PendingIntent -> optimistic $nextHold")
            return nextHold
        }
        Log.w(TAG, "No Dialer Hold PendingIntent available; opening active call screen")
        openInCallScreen(context)
        return _isOnHold.value
    }

    fun toggleRecord(context: Context): Boolean {
        val nextRecording = !_isRecording.value
        if (CallActionRegistry.toggleRecord(context)) {
            _isRecording.value = nextRecording
            Log.d(TAG, "Record toggled via Dialer PendingIntent -> optimistic $nextRecording")
            return nextRecording
        }
        Log.d(TAG, "No Dialer Record PendingIntent exposed; bringing call screen forward")
        openInCallScreen(context)
        return _isRecording.value
    }

    @SuppressLint("MissingPermission")
    fun openInCallScreen(context: Context): Boolean {
        if (CallActionRegistry.openCallScreen(context)) {
            return true
        }
        return try {
            val telecom = context.getSystemService(TelecomManager::class.java)
            telecom?.showInCallScreen(false)
            true
        } catch (e: Exception) {
            Log.w(TAG, "Unable to show in-call screen", e)
            false
        }
    }
}
