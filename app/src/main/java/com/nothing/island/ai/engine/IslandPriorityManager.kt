package com.nothing.island.ai.engine

import android.util.Log
import com.nothing.island.ai.notifications.NotificationActionBridge
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

/**
 * IslandPriorityManager
 *
 * Centralized arbiter for Nothing Island events.
 * Implements strict priority arbitration, preemption, persistent background fallbacks,
 * cancellable timeout timers, and safe state restoration upon event expiration.
 */
class IslandPriorityManager(
    private val scope: CoroutineScope,
    private val onStateResolved: (resolvedEvent: IslandEvent?, priority: Int, presentationId: String) -> Unit
) {
    companion object {
        private const val TAG = "IslandPriorityManager"
    }

    private val mutex = Mutex()

    @Volatile
    var activeEvent: IslandEvent? = null
        private set

    @Volatile
    var activePriority: Int = IslandPriority.IDLE_BASE
        private set

    @Volatile
    var activePresentationId: String = "idle_0"
        private set

    private var activeTimeoutJob: Job? = null

    // Background persistent states held while temporary higher-priority alerts are showing
    // Key: domain tag (e.g. "MEDIA", "CALL", "TIMER", "RECORDING")
    private val backgroundFallbacks = ConcurrentHashMap<String, IslandEvent>()

    /**
     * Evaluates an incoming event against the active presentation and priority hierarchy.
     */
    suspend fun processEvent(event: IslandEvent) {
        mutex.withLock {
            Log.d(TAG, "Evaluating event: id=${event.id}, source=${event.source}, priority=${event.priority}, type=${event.javaClass.simpleName}")

            // Handle domain-specific dismissals / stop events first
            when (event) {
                is IslandEvent.NotificationDismissed -> {
                    handleNotificationDismissed(event)
                    return@withLock
                }
                is IslandEvent.MediaPlaybackStopped -> {
                    handleMediaStopped(event)
                    return@withLock
                }
                is IslandEvent.CallConnected -> {
                    // Answered: replace the persistent ringing card even though both
                    // events share the CRITICAL tier (equal priority replaces).
                    backgroundFallbacks.remove("CALL")
                }
                is IslandEvent.CallEnded -> {
                    handleCallEnded(event)
                    return@withLock
                }
                is IslandEvent.TimerFinished -> {
                    // Remove completed timer from fallback storage so it cannot be resurrected
                    backgroundFallbacks.remove("TIMER")
                }
                is IslandEvent.UserDismissCurrent -> {
                    dismissActivePresentation(reason = "UserDismissCurrent")
                    return@withLock
                }
                else -> {
                    // Track fresh notification window in IslandStateManager
                    if (event is IslandEvent.NotificationPosted) {
                        IslandStateManager.getInstance().onNotificationArrived(event.timestamp)
                    }
                    // Cache persistent ongoing events into background fallbacks
                    cacheBackgroundEventIfPersistent(event)
                }
            }

            val currentPriority = activePriority
            val newPriority = event.priority

            when {
                // Rule 1: Strictly Higher Priority -> Immediate Preemption
                newPriority > currentPriority -> {
                    Log.i(TAG, "PREEMPTION: New event ${event.id} (P$newPriority) preempts current (P$currentPriority)")
                    activateEventLocked(event)
                }

                // Rule 2: Strictly Lower Priority -> Suppress/Reject from foreground presentation
                newPriority < currentPriority -> {
                    Log.d(TAG, "REJECTION: New event ${event.id} (P$newPriority) rejected; active priority P$currentPriority is higher")
                    // If the event was persistent, it was already cached in background fallbacks above
                }

                // Rule 3: Equal Priority -> Deterministic Replacement or Refresh
                else -> {
                    // Priority 0 (IDLE_BASE) lifecycle events must not replace or overwrite Base Idle presentation
                    if (newPriority == IslandPriority.IDLE_BASE) {
                        Log.d(TAG, "IDLE LIFECYCLE: Lifecycle event ${event.id} (P0) noted without overriding Base Idle presentation.")
                        return@withLock
                    }

                    // In-place update for identical notification ID: preserves active timeout countdown deadline
                    val current = activeEvent
                    if (current is IslandEvent.NotificationPosted && event is IslandEvent.NotificationPosted && current.id == event.id) {
                        Log.d(TAG, "IN-PLACE UPDATE: Updating active notification ${event.id} in-place without resetting timeout.")
                        activeEvent = event
                        onStateResolved(event, event.priority, activePresentationId)
                        return@withLock
                    }

                    // In-place update for identical active media track/playback state: avoids transition flicker
                    if (current is IslandEvent.MediaPlaybackChanged &&
                        event is IslandEvent.MediaPlaybackChanged &&
                        current.packageName == event.packageName &&
                        current.trackTitle == event.trackTitle &&
                        current.artistName == event.artistName &&
                        current.isPlaying == event.isPlaying
                    ) {
                        activeEvent = event
                        onStateResolved(event, event.priority, activePresentationId)
                        return@withLock
                    }

                    Log.d(TAG, "EQUAL PRIORITY: Replacing current presentation with new event ${event.id} (P$newPriority)")
                    activateEventLocked(event)
                }
            }
        }
    }

    private fun activateEventLocked(event: IslandEvent) {
        // Cancel any obsolete timeout job for the previously active event
        activeTimeoutJob?.cancel()
        activeTimeoutJob = null

        // In Phase 3.3, actions are retained across preemption because history entries remain accessible
        val presentationId = "${event.id}_${System.currentTimeMillis()}"
        activeEvent = event
        activePriority = event.priority
        activePresentationId = presentationId

        // Schedule timeout for temporary events
        if (event.timeoutMs > 0L) {
            val timeoutDuration = event.timeoutMs
            Log.d(TAG, "Scheduling timeout (${timeoutDuration}ms) for presentation $presentationId")
            activeTimeoutJob = scope.launch {
                delay(timeoutDuration)
                onTimeoutExpired(presentationId, event)
            }
        }

        onStateResolved(event, event.priority, presentationId)
    }

    private suspend fun onTimeoutExpired(expiredPresentationId: String, expiredEvent: IslandEvent) {
        mutex.withLock {
            // Safety Check: Verify active presentation identity to avoid cancelling a newer event
            if (activePresentationId != expiredPresentationId) {
                Log.d(TAG, "Ignoring expired timeout for $expiredPresentationId (active is $activePresentationId)")
                return@withLock
            }

            Log.i(TAG, "TIMEOUT EXPIRED: Presentation $expiredPresentationId (${expiredEvent.id}) expired.")
            // In Phase 3.3, actions are retained across timeout expiration so history retains them
            restoreHighestFallbackLocked()
        }
    }

    private fun dismissActivePresentation(reason: String) {
        Log.i(TAG, "Dismissing active presentation $activePresentationId. Reason: $reason")
        // In Phase 3.3, actions are retained in NotificationActionBridge for history access
        activeTimeoutJob?.cancel()
        activeTimeoutJob = null
        restoreHighestFallbackLocked()
    }

    private fun restoreHighestFallbackLocked() {
        // Find highest priority active background fallback
        val highestFallback = backgroundFallbacks.values.maxByOrNull { it.priority }

        if (highestFallback != null) {
            Log.i(TAG, "RESTORING FALLBACK: Activating highest fallback ${highestFallback.id} (P${highestFallback.priority})")
            val presentationId = "${highestFallback.id}_${System.currentTimeMillis()}"
            activeEvent = highestFallback
            activePriority = highestFallback.priority
            activePresentationId = presentationId
            activeTimeoutJob?.cancel()
            activeTimeoutJob = null

            onStateResolved(highestFallback, highestFallback.priority, presentationId)
        } else {
            Log.i(TAG, "RESTORING IDLE: No fallbacks remaining. Reverting to base idle state.")
            val idlePresentationId = "idle_${System.currentTimeMillis()}"
            activeEvent = null
            activePriority = IslandPriority.IDLE_BASE
            activePresentationId = idlePresentationId
            activeTimeoutJob?.cancel()
            activeTimeoutJob = null

            onStateResolved(null, IslandPriority.IDLE_BASE, idlePresentationId)
        }
    }

    private fun cacheBackgroundEventIfPersistent(event: IslandEvent) {
        // Persistent events have timeoutMs == 0
        if (event.timeoutMs == 0L) {
            when (event) {
                is IslandEvent.MediaPlaybackChanged -> {
                    if (event.isPlaying) {
                        backgroundFallbacks["MEDIA"] = event
                    } else {
                        backgroundFallbacks.remove("MEDIA")
                    }
                }
                is IslandEvent.IncomingCallRinging -> {
                    backgroundFallbacks["CALL"] = event
                }
                is IslandEvent.CallConnected -> {
                    backgroundFallbacks["CALL"] = event
                }
                is IslandEvent.TimerStarted, is IslandEvent.TimerTick -> {
                    backgroundFallbacks["TIMER"] = event
                }
                is IslandEvent.RecordingStateChanged -> {
                    if (event.isRecording) {
                        backgroundFallbacks["RECORDING"] = event
                    } else {
                        backgroundFallbacks.remove("RECORDING")
                    }
                }
                else -> {
                    // Other persistent events
                }
            }
        }
    }

    private fun handleNotificationDismissed(event: IslandEvent.NotificationDismissed) {
        // In Phase 3.3, actions remain registered in NotificationActionBridge for historical interactions
        val current = activeEvent
        if (current is IslandEvent.NotificationPosted && current.id == event.targetNotificationId) {
            Log.i(TAG, "Active notification ${current.id} dismissed via external event.")
            dismissActivePresentation(reason = "NotificationDismissed")
        }
    }

    private fun handleMediaStopped(event: IslandEvent.MediaPlaybackStopped) {
        backgroundFallbacks.remove("MEDIA")
        val current = activeEvent
        if (current is IslandEvent.MediaPlaybackChanged && current.packageName == event.packageName) {
            Log.i(TAG, "Active media ${current.id} stopped.")
            dismissActivePresentation(reason = "MediaPlaybackStopped")
        }
    }

    private fun handleCallEnded(event: IslandEvent.CallEnded) {
        backgroundFallbacks.remove("CALL")
        val current = activeEvent
        if (current is IslandEvent.IncomingCallRinging || current is IslandEvent.CallConnected) {
            Log.i(TAG, "Active call ended. Displaying brief call ended confirmation (${event.timeoutMs}ms).")
            activateEventLocked(event)
        }
    }

    /**
     * Resets manager state, clears all fallback caches, and cancels active timeouts.
     */
    fun reset() {
        activeTimeoutJob?.cancel()
        activeTimeoutJob = null
        backgroundFallbacks.clear()
        activeEvent = null
        activePriority = IslandPriority.IDLE_BASE
        activePresentationId = "idle_${System.currentTimeMillis()}"
        NotificationActionBridge.clearAll()
        onStateResolved(null, IslandPriority.IDLE_BASE, activePresentationId)
        Log.i(TAG, "IslandPriorityManager reset completed.")
    }
}
