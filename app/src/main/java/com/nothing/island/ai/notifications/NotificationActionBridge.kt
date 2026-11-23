package com.nothing.island.ai.notifications

import android.app.PendingIntent
import android.app.RemoteInput
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Encapsulates an interactive action button attached to a notification.
 */
data class NotificationActionItem(
    val title: String,
    val actionIntent: PendingIntent?,
    val isContextual: Boolean = false,
    val isReply: Boolean = false,
    val remoteInputs: Array<RemoteInput>? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as NotificationActionItem

        if (title != other.title) return false
        if (actionIntent != other.actionIntent) return false
        if (isContextual != other.isContextual) return false
        if (isReply != other.isReply) return false
        if (remoteInputs != null) {
            if (other.remoteInputs == null) return false
            if (!remoteInputs.contentEquals(other.remoteInputs)) return false
        } else if (other.remoteInputs != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = title.hashCode()
        result = 31 * result + (actionIntent?.hashCode() ?: 0)
        result = 31 * result + isContextual.hashCode()
        result = 31 * result + isReply.hashCode()
        result = 31 * result + (remoteInputs?.contentHashCode() ?: 0)
        return result
    }
}

/**
 * Result status of an inline reply execution.
 */
enum class ReplyResult {
    SUCCESS,
    CANCELED,
    NOT_FOUND,
    NOT_REPLY,
    ERROR
}

/**
 * Cached action container for a single notification key with registration generation token.
 */
data class NotificationActionEntry(
    val key: String,
    val packageName: String? = null,
    val contentIntent: PendingIntent? = null,
    val actions: List<NotificationActionItem> = emptyList(),
    val generation: Long = 0L,
    val token: Long = generation // Backward compatibility with legacy callers
)

typealias CachedNotificationActions = NotificationActionEntry

/**
 * NotificationActionBridge
 *
 * Manages transient in-memory references to platform-dependent PendingIntent objects
 * and action items, keeping heavy Android framework classes completely out of the
 * domain-level IslandEvent hierarchy.
 *
 * Owns the monotonic action generation counter (AtomicLong).
 */
object NotificationActionBridge {

    private const val TAG = "NotifActionBridge"

    private val generationSequence = AtomicLong(0L)
    private val entries = ConcurrentHashMap<String, NotificationActionEntry>()

    /**
     * Registers the content PendingIntent and optional action intents for a notification key.
     * Generates and returns a monotonically increasing actionGeneration token.
     * Uses atomic compute to ensure an older callback arriving out-of-order cannot clobber
     * a newer registration for the same notification key.
     */
    fun register(
        key: String,
        packageName: String? = null,
        contentIntent: PendingIntent?,
        actions: List<NotificationActionItem> = emptyList(),
        token: Long = 0L
    ): Long {
        val nextGen = generationSequence.incrementAndGet()
        val assignedToken = if (token > 0L) token else nextGen

        val newEntry = NotificationActionEntry(
            key = key,
            packageName = packageName,
            contentIntent = contentIntent,
            actions = actions,
            generation = nextGen,
            token = assignedToken
        )

        entries.compute(key) { _, current ->
            if (current == null || nextGen > current.generation) {
                newEntry
            } else {
                // Out-of-order callback detected: keep newer current entry!
                Log.w(TAG, "Preserved newer action registration for key=$key: current gen (${current.generation}) >= incoming ($nextGen)")
                current
            }
        }

        return nextGen
    }

    /**
     * Retrieves cached action metadata for a notification key.
     */
    fun get(key: String): NotificationActionEntry? = entries[key]

    /**
     * Returns total number of active cached notifications.
     */
    fun getCachedCount(): Int = entries.size

    /**
     * Safely executes the primary content intent for a notification.
     * Returns true if successfully fired; false if missing or canceled.
     * Automatically removes dead registration upon CanceledException.
     */
    fun executeContentIntent(context: Context? = null, key: String): Boolean {
        val entry = entries[key] ?: return false
        val intent = entry.contentIntent ?: return false
        return try {
            if (context != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val options = android.app.ActivityOptions.makeBasic()
                options.setPendingIntentBackgroundActivityStartMode(
                    android.app.ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
                )
                intent.send(context, 0, null, null, null, null, options.toBundle())
            } else {
                intent.send()
            }
            Log.d(TAG, "Fired content PendingIntent for key=$key")
            true
        } catch (e: PendingIntent.CanceledException) {
            Log.w(TAG, "Content PendingIntent was canceled for key=$key; cleaning dead action entry", e)
            entries.remove(key, entry)
            false
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fire PendingIntent for key=$key", e)
            false
        }
    }

    /**
     * Backward-compatible overload without explicit context.
     */
    fun executeContentIntent(key: String): Boolean = executeContentIntent(null, key)

    /**
     * Safely executes an action button intent by index.
     */
    fun executeAction(key: String, actionIndex: Int): Boolean {
        val entry = entries[key] ?: return false
        val list = entry.actions
        if (actionIndex < 0 || actionIndex >= list.size) return false
        val action = list[actionIndex]
        val intent = action.actionIntent ?: return false
        return try {
            intent.send()
            Log.d(TAG, "Fired action PendingIntent (index $actionIndex) for key=$key")
            true
        } catch (e: PendingIntent.CanceledException) {
            Log.w(TAG, "Action PendingIntent canceled for key=$key (index $actionIndex); marking action dead", e)
            entries.computeIfPresent(key) { _, current ->
                val updatedActions = current.actions.toMutableList()
                if (actionIndex in updatedActions.indices) {
                    updatedActions[actionIndex] = updatedActions[actionIndex].copy(actionIntent = null)
                }
                current.copy(actions = updatedActions)
            }
            false
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fire action PendingIntent for key=$key", e)
            false
        }
    }

    /**
     * Safely populates RemoteInput results and sends the reply action PendingIntent.
     * Enforces zero unnecessary persistent storage of reply text.
     */
    fun sendReply(
        context: Context? = null,
        key: String,
        actionIndex: Int,
        replyText: String
    ): ReplyResult {
        val entry = entries[key] ?: return ReplyResult.NOT_FOUND
        val list = entry.actions
        if (actionIndex < 0 || actionIndex >= list.size) return ReplyResult.NOT_FOUND
        val action = list[actionIndex]
        if (!action.isReply || action.actionIntent == null) return ReplyResult.NOT_REPLY

        val intent = Intent()
        val bundle = Bundle()
        val inputs = action.remoteInputs

        if (!inputs.isNullOrEmpty()) {
            for (input in inputs) {
                bundle.putCharSequence(input.resultKey, replyText)
            }
            try {
                RemoteInput.addResultsToIntent(inputs, intent, bundle)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    RemoteInput.setResultsSource(intent, RemoteInput.SOURCE_FREE_FORM_INPUT)
                }
            } catch (e: Throwable) {
                Log.w(TAG, "Error adding RemoteInput results to intent for key=$key", e)
            }
        } else {
            bundle.putCharSequence("extra_voice_reply", replyText)
        }

        return try {
            action.actionIntent.send(context, 0, intent)
            Log.d(TAG, "Fired RemoteInput reply PendingIntent (index $actionIndex) for key=$key")
            ReplyResult.SUCCESS
        } catch (e: PendingIntent.CanceledException) {
            Log.w(TAG, "Reply PendingIntent canceled for key=$key (index $actionIndex); marking action dead", e)
            entries.computeIfPresent(key) { _, current ->
                val updatedActions = current.actions.toMutableList()
                if (actionIndex in updatedActions.indices) {
                    updatedActions[actionIndex] = updatedActions[actionIndex].copy(actionIntent = null)
                }
                current.copy(actions = updatedActions)
            }
            ReplyResult.CANCELED
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send reply PendingIntent for key=$key", e)
            ReplyResult.ERROR
        }
    }

    /**
     * Unregisters actions for the key ONLY if the registration generation strictly matches.
     * Prevents an older update or failed attempt from removing actions belonging to a newer registration.
     */
    fun unregisterIfGenerationMatches(key: String, generation: Long): Boolean {
        val current = entries[key] ?: return false
        return if (current.generation == generation) {
            val removed = entries.remove(key, current)
            if (removed) {
                Log.d(TAG, "Unregistered actions for key=$key with matching generation=$generation")
            }
            removed
        } else {
            Log.d(TAG, "Preserved actions for key=$key: current gen (${current.generation}) != target gen ($generation)")
            false
        }
    }

    /**
     * Unregisters actions for the key ONLY if the registration token matches.
     * Kept for backward compatibility with legacy token callers.
     */
    fun unregisterIfTokenMatches(key: String, token: Long): Boolean {
        val current = entries[key] ?: return false
        return if (current.token == token || current.generation == token) {
            val removed = entries.remove(key, current)
            if (removed) {
                Log.d(TAG, "Unregistered actions for key=$key with matching token=$token")
            }
            removed
        } else {
            Log.d(TAG, "Preserved actions for key=$key: current token (${current.token}) != expired token ($token)")
            false
        }
    }

    /**
     * Unregisters cached intents for a given notification key upon dismissal.
     */
    fun unregister(key: String) {
        entries.remove(key)
    }

    /**
     * Clears cached intents for a given notification key upon dismissal.
     */
    fun clear(key: String) {
        entries.remove(key)
    }

    /**
     * Clears all cached intents upon listener disconnection.
     */
    fun clearAll() {
        entries.clear()
    }

    /**
     * Prunes entries older than the cutoff timestamp.
     */
    fun pruneOlderThan(maxAgeMs: Long, currentTimeMs: Long = System.currentTimeMillis()): Int {
        val cutoff = currentTimeMs - maxAgeMs
        var removed = 0
        for ((k, v) in entries) {
            if (v.generation < cutoff && v.token < cutoff) {
                if (entries.remove(k, v)) {
                    removed++
                }
            }
        }
        return removed
    }

    /**
     * Number of notifications with a registered content PendingIntent.
     */
    val registeredContentCount: Int
        get() = entries.count { it.value.contentIntent != null }
}
