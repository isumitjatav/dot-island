package com.nothing.island.ai.telephony

import android.Manifest
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

    fun canControlCalls(context: Context): Boolean =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ANSWER_PHONE_CALLS,
        ) == PackageManager.PERMISSION_GRANTED

    fun answer(context: Context): Boolean {
        if (canControlCalls(context)) {
            val telecom = context.getSystemService(TelecomManager::class.java)
            try {
                telecom?.acceptRingingCall()
                Log.d(TAG, "Answered through TelecomManager")
                return true
            } catch (e: Exception) {
                Log.w(TAG, "TelecomManager answer failed, falling back", e)
            }
        }
        return CallActionRegistry.answer()
    }

    fun decline(context: Context): Boolean {
        if (canControlCalls(context)) {
            val telecom = context.getSystemService(TelecomManager::class.java)
            try {
                telecom?.endCall()
                Log.d(TAG, "Declined through TelecomManager")
                return true
            } catch (e: Exception) {
                Log.w(TAG, "TelecomManager decline failed, falling back", e)
            }
        }
        return CallActionRegistry.hangUp()
    }

    fun isSpeakerOn(context: Context): Boolean {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return false
        val active = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            am.communicationDevice?.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
        } else {
            @Suppress("DEPRECATION")
            am.isSpeakerphoneOn
        }
        _isSpeakerOn.value = active
        return active
    }

    fun toggleSpeaker(context: Context): Boolean {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return false
        val currentlyOn = isSpeakerOn(context)
        val turningOn = !currentlyOn
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                am.mode = AudioManager.MODE_IN_COMMUNICATION
                if (turningOn) {
                    val speaker = am.availableCommunicationDevices.firstOrNull {
                        it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
                    }
                    if (speaker != null) {
                        val success = am.setCommunicationDevice(speaker)
                        Log.d(TAG, "setCommunicationDevice(speaker) result: $success")
                    } else {
                        @Suppress("DEPRECATION")
                        am.isSpeakerphoneOn = true
                    }
                } else {
                    am.clearCommunicationDevice()
                    @Suppress("DEPRECATION")
                    am.isSpeakerphoneOn = false
                }
            } else {
                @Suppress("DEPRECATION")
                am.mode = AudioManager.MODE_IN_COMMUNICATION
                @Suppress("DEPRECATION")
                am.isSpeakerphoneOn = turningOn
            }
            _isSpeakerOn.value = turningOn
            Log.d(TAG, "Speaker toggled: now $turningOn")
            return turningOn
        } catch (e: Exception) {
            Log.w(TAG, "Failed to toggle speaker", e)
            _isSpeakerOn.value = currentlyOn
            return currentlyOn
        }
    }
}
