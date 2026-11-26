package com.nothing.island.ai.telephony

import android.app.ActivityOptions
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import android.util.Log

/**
 * R9 — holds the Answer/Decline PendingIntents from the active call notification
 * so the island's own call card can invoke the dialer's real actions. Using the
 * notification actions avoids restricted ANSWER_PHONE_CALLS permissions and works for VoIP apps.
 */
object CallActionRegistry {
    private const val TAG = "CallActionRegistry"

    data class CallActions(
        val key: String,
        val answer: PendingIntent?,
        val decline: PendingIntent?,
        val hangUp: PendingIntent? = null,
        val packageName: String? = null,
        val speaker: PendingIntent? = null,
        val mute: PendingIntent? = null,
        val hold: PendingIntent? = null,
        val record: PendingIntent? = null,
        val contentIntent: PendingIntent? = null,
    )

    @Volatile
    private var current: CallActions? = null

    fun register(actions: CallActions) {
        current = actions
        Log.d(
            TAG,
            "Registered call actions for ${actions.key} (pkg=${actions.packageName}, speaker=${actions.speaker != null}, mute=${actions.mute != null}, hold=${actions.hold != null}, record=${actions.record != null})"
        )
    }

    fun currentActions(): CallActions? = current

    fun clear(key: String) {
        if (current?.key == key) current = null
    }

    fun answer(context: Context? = null): Boolean = send(context, current?.answer, "answer")

    fun decline(context: Context? = null): Boolean = send(context, current?.decline, "decline")

    fun hangUp(context: Context? = null): Boolean =
        send(context, current?.hangUp ?: current?.decline, "hangUp")

    fun toggleSpeaker(context: Context? = null): Boolean = send(context, current?.speaker, "speaker")

    fun toggleMute(context: Context? = null): Boolean = send(context, current?.mute, "mute")

    fun toggleHold(context: Context? = null): Boolean = send(context, current?.hold, "hold")

    fun toggleRecord(context: Context? = null): Boolean = send(context, current?.record, "record")

    fun openCallScreen(context: Context? = null): Boolean = send(context, current?.contentIntent, "contentIntent")

    private fun send(context: Context?, intent: PendingIntent?, label: String): Boolean {
        if (intent == null) {
            Log.w(TAG, "No $label action available for active call")
            return false
        }
        return try {
            val targetContext = context?.applicationContext ?: context
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val options = ActivityOptions.makeBasic()
                    .setPendingIntentBackgroundActivityStartMode(
                        ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
                    )
                    .toBundle()
                if (targetContext != null) {
                    intent.send(targetContext, 0, null, null, null, null, options)
                } else {
                    intent.send(options)
                }
            } else {
                if (targetContext != null) {
                    intent.send(targetContext, 0, null)
                } else {
                    intent.send()
                }
            }
            Log.d(TAG, "Sent $label action")
            true
        } catch (e: PendingIntent.CanceledException) {
            Log.w(TAG, "$label action was cancelled", e)
            false
        } catch (e: Exception) {
            Log.w(TAG, "Failed sending $label action", e)
            false
        }
    }
}
