package com.nothing.island.ai.telephony

import android.app.PendingIntent
import android.util.Log

/**
 * R9 — holds the Answer/Decline PendingIntents from the active call notification
 * so the island's own call card can invoke the dialer's real actions. Using the
 * notification actions avoids restricted ANSWER_PHONE_CALLS permissions.
 */
object CallActionRegistry {
    private const val TAG = "CallActionRegistry"

    data class CallActions(
        val key: String,
        val answer: PendingIntent?,
        val decline: PendingIntent?,
        val hangUp: PendingIntent? = null,
    )

    @Volatile
    private var current: CallActions? = null

    fun register(actions: CallActions) {
        current = actions
        Log.d(TAG, "Registered call actions for ${actions.key}")
    }

    fun currentActions(): CallActions? = current

    fun clear(key: String) {
        if (current?.key == key) current = null
    }

    fun answer(): Boolean = send(current?.answer, "answer")

    fun decline(): Boolean = send(current?.decline, "decline")

    fun hangUp(): Boolean = send(current?.hangUp ?: current?.decline, "hangUp")

    private fun send(intent: PendingIntent?, label: String): Boolean {
        if (intent == null) {
            Log.w(TAG, "No $label action available for active call")
            return false
        }
        return try {
            intent.send()
            Log.d(TAG, "Sent $label action")
            true
        } catch (e: PendingIntent.CanceledException) {
            Log.w(TAG, "$label action was cancelled", e)
            false
        }
    }
}
