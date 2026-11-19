package com.nothing.island.ai.telephony

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.telecom.TelecomManager
import android.util.Log
import androidx.core.content.ContextCompat

/**
 * R9 call controls. Prefers TelecomManager (the reliable, documented API) and
 * falls back to the dialer notification's own actions when the optional
 * ANSWER_PHONE_CALLS permission has not been granted.
 */
object CallController {
    private const val TAG = "CallController"

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
        return CallActionRegistry.decline()
    }
}
