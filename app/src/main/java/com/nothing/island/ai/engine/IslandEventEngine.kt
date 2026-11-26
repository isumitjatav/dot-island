package com.nothing.island.ai.engine

import android.util.Log
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.merge
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

/**
 * IslandEventEngine
 *
 * Centralized, priority-aware, bounded event bus for Nothing Island.
 * Employs separate buffering for critical events (Priority >= 70, e.g. Telephony, Safety Alerts)
 * and standard events to guarantee critical events are never evicted by low-priority traffic.
 * Enforces semantic deduplication, coalesces non-transitional high-frequency updates,
 * and caches critical events across zero-subscriber intervals.
 */
class IslandEventEngine(
    private val bufferCapacity: Int = 64,
    private val deduplicationWindowMs: Long = 250L,
    private val coalescingWindowMs: Long = 250L
) {
    companion object {
        private const val TAG = "IslandEventEngine"
        private const val CRITICAL_BUFFER_CAPACITY = 32
        private const val MAX_RETAINED_CRITICAL = 16
    }

    // Critical events buffer (Priority >= 70)
    private val _criticalEvents = MutableSharedFlow<IslandEvent>(
        replay = 0,
        extraBufferCapacity = CRITICAL_BUFFER_CAPACITY,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    // Standard events buffer (Priority < 70)
    private val _standardEvents = MutableSharedFlow<IslandEvent>(
        replay = 0,
        extraBufferCapacity = bufferCapacity,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    private val retainedLock = Any()
    private val retainedCriticalEvents = mutableListOf<IslandEvent>()

    /**
     * Read-only event stream merging critical and standard event flows.
     * Drains any retained critical events upon new collector attachment.
     */
    val events: Flow<IslandEvent> = flow {
        val pending = synchronized(retainedLock) {
            val list = ArrayList(retainedCriticalEvents)
            retainedCriticalEvents.clear()
            list
        }
        for (event in pending) {
            emit(event)
        }
        emitAll(merge(_criticalEvents.asSharedFlow(), _standardEvents.asSharedFlow()))
    }

    /**
     * Exposes current subscriber count for observability and test verification.
     */
    val subscriptionCount: StateFlow<Int> = _standardEvents.subscriptionCount

    private val lastEventRef = AtomicReference<IslandEvent?>(null)
    private val lastEmissionTime = AtomicLong(0L)
    private val coalescedTimestamps = ConcurrentHashMap<String, Long>()

    /**
     * Submits an event to the bus synchronously.
     * Returns true if accepted, false if dropped via deduplication or coalescing.
     */
    fun emit(event: IslandEvent): Boolean {
        val now = System.currentTimeMillis()
        val isCritical = event.priority >= IslandPriority.SAFETY_ALERT // >= 70 (Call, Emergency, Low Battery)

        // 1. Non-critical events pass through coalescing and semantic deduplication
        if (!isCritical) {
            // 2. High-frequency media position update coalescing
            if (shouldCoalesce(event, now)) {
                Log.d(TAG, "Event coalesced (throttled): id=${event.id}, source=${event.source}, type=${event.javaClass.simpleName}")
                return false
            }

            // 3. Semantic deduplication: ignore identical consecutive events within deduplicationWindowMs
            val last = lastEventRef.get()
            if (last != null && isDuplicate(last, event, now)) {
                Log.d(TAG, "Event deduplicated: id=${event.id}, source=${event.source}, type=${event.javaClass.simpleName}")
                return false
            }
        }

        lastEventRef.set(event)
        lastEmissionTime.set(now)

        return if (isCritical) {
            if (subscriptionCount.value == 0) {
                synchronized(retainedLock) {
                    if (retainedCriticalEvents.size >= MAX_RETAINED_CRITICAL) {
                        retainedCriticalEvents.removeAt(0)
                    }
                    retainedCriticalEvents.add(event)
                }
                Log.i(TAG, "Critical event retained while subscriptionCount == 0: id=${event.id}, priority=${event.priority}")
                true
            } else {
                val success = _criticalEvents.tryEmit(event)
                if (success) {
                    Log.d(TAG, "Critical event emitted: id=${event.id}, priority=${event.priority}")
                } else {
                    Log.w(TAG, "Critical event buffer saturated: id=${event.id}")
                }
                success
            }
        } else {
            val success = _standardEvents.tryEmit(event)
            if (success) {
                Log.d(TAG, "Standard event emitted: id=${event.id}, priority=${event.priority}")
            } else {
                Log.w(TAG, "Standard event buffer saturated: id=${event.id}")
            }
            success
        }
    }

    /**
     * Suspending submission for structured coroutine callers.
     */
    suspend fun emitSuspend(event: IslandEvent): Boolean {
        return emit(event)
    }

    private fun isDuplicate(previous: IslandEvent, current: IslandEvent, now: Long): Boolean {
        if (now - lastEmissionTime.get() > deduplicationWindowMs) return false
        if (previous.source != current.source) return false
        return previous.hasIdenticalBusinessPayload(current)
    }

    private fun shouldCoalesce(event: IslandEvent, now: Long): Boolean {
        val coalesceKey = when (event) {
            is IslandEvent.MediaPlaybackChanged -> {
                val last = lastEventRef.get() as? IslandEvent.MediaPlaybackChanged
                if (last != null && (last.isPlaying != event.isPlaying ||
                                     last.trackTitle != event.trackTitle ||
                                     last.artistName != event.artistName)) {
                    // Playback state or track metadata changed: NEVER suppress!
                    return false
                }
                "MediaTick_${event.packageName}"
            }
            else -> return false
        }

        val lastTime = coalescedTimestamps[coalesceKey] ?: 0L
        if (now - lastTime < coalescingWindowMs) {
            return true
        }
        coalescedTimestamps[coalesceKey] = now
        return false
    }

    /**
     * Resets the event engine internal caches and tracking.
     */
    fun reset() {
        lastEventRef.set(null)
        lastEmissionTime.set(0L)
        coalescedTimestamps.clear()
        synchronized(retainedLock) {
            retainedCriticalEvents.clear()
        }
        Log.i(TAG, "IslandEventEngine reset completed.")
    }
}
