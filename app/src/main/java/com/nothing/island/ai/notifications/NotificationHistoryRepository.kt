package com.nothing.island.ai.notifications

import android.util.Log
import com.nothing.island.ai.engine.DownloadProgressState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/**
 * Snapshot of a pending undo state exposed to UI observers.
 */
data class UndoNotificationState(
    val id: String,                 // Single notification key or batch operation token
    val title: String,              // Human-readable title or summary (e.g. "Alice" or "3 alerts cleared")
    val timestamp: Long = System.currentTimeMillis(),
    val timeoutMs: Long = 3500L,
    val expirationTimeMs: Long = timestamp + timeoutMs,
    val isBatch: Boolean = false,
    val count: Int = 1
)

/**
 * Internal tracking record for a dismissed notification entry (or batch) awaiting undo timeout.
 */
internal data class PendingUndoRecord(
    val id: String,
    val isBatch: Boolean,
    val items: List<Pair<Int, NotificationHistoryEntry>>, // Pair of original list index to historical entry
    val dismissedAt: Long = System.currentTimeMillis(),
    val undoTimeoutMs: Long = 3500L,
    val timeoutJob: Job? = null
)

/**
 * Public interface for Nothing Island's in-memory Notification History Repository.
 * Enforces a strict 30-entry capacity, newest-first ordering, stable AOSP key identity,
 * 3.5s undoable dismissals, and thread-safe lock-free external unregistration callbacks.
 */
interface NotificationHistoryRepository {

    /**
     * Read-only StateFlow stream of the active notification history list (max 30 items, newest first).
     */
    val history: StateFlow<List<NotificationHistoryEntry>>

    /**
     * Read-only StateFlow stream of the current active undo operation (if any within 3.5s).
     */
    val pendingUndoFlow: StateFlow<UndoNotificationState?>

    /**
     * Total number of retained active history entries (0..30).
     */
    val size: Int

    /**
     * Ingests a validated notification event into history.
     * Evaluates event freshness against any existing entry for this key.
     * If key is new: prepends to index 0, evicting the oldest entry if capacity is exceeded.
     * If key exists: updates entry in-place, preserving firstPostTime.
     *
     * Returns true if accepted and committed; false if rejected as stale or ineligible.
     */
    fun recordNotification(
        key: String,
        packageName: String,
        appDisplayName: String,
        title: String,
        text: String,
        subText: String? = null,
        category: String? = null,
        postTime: Long = System.currentTimeMillis(),
        sourcePostTime: Long = postTime,
        ingestSequence: Long = 0L,
        isOngoing: Boolean = false,
        isClearable: Boolean = true,
        visibility: Int = 0,
        previewMode: NotificationPreviewMode = NotificationPreviewMode.FULL,
        actionGeneration: Long = 0L,
        progress: Int = 0,
        maxProgress: Int = 0,
        isIndeterminate: Boolean = false,
        hasProgress: Boolean = false,
        progressState: DownloadProgressState = DownloadProgressState.NONE
    ): Boolean

    /**
     * Marks an entry as removed from the Android system shade without deleting it from history.
     */
    fun markSystemNotificationRemoved(
        key: String,
        removalPostTime: Long = System.currentTimeMillis(),
        removalSequence: Long = 0L
    )

    /**
     * Dismisses an individual notification entry from Nothing Island history with a 3.5s undo window.
     * Actions are retained until undo window expires. Does NOT dismiss system notifications.
     */
    fun dismissEntry(key: String, undoTimeoutMs: Long = 3500L): NotificationHistoryEntry?

    /**
     * Restores a dismissed entry (or the latest dismissed entry if key is null) to its original
     * chronological list index, cancelling its pending eviction.
     */
    fun undoDismiss(key: String? = null): Boolean

    /**
     * Clears all entries from Nothing Island local history with a 3.5s batch undo window.
     * Does NOT dismiss Android system notifications in the shade.
     */
    fun clearAllLocalHistory(undoTimeoutMs: Long = 3500L): Int

    /**
     * Restores all entries cleared by the latest clearAllLocalHistory batch operation.
     */
    fun undoClearAll(): Boolean

    /**
     * Checks if a notification key is currently in the pending undo buffer.
     */
    fun isPendingUndo(key: String): Boolean

    /**
     * Total number of entries currently awaiting undo expiration.
     */
    val pendingUndoCount: Int

    /**
     * Explicitly removes a single entry immediately from history and releases its actions (no undo).
     */
    fun removeEntry(key: String): Boolean

    /**
     * Clears all entries immediately from history and undo buffers without undo opportunity.
     */
    fun clearAll()

    /**
     * Retrieves an entry by stable notification key.
     */
    fun getEntry(key: String): NotificationHistoryEntry?

    /**
     * Checks whether an incoming notification callback is fresh and eligible for update/resurrection
     * before modifying action registrations.
     */
    fun isFresh(key: String, sourcePostTime: Long, ingestSequence: Long): Boolean
}

/**
 * Thread-safe in-memory implementation of NotificationHistoryRepository.
 * Resides exclusively in volatile heap memory.
 */
class NotificationHistoryRepositoryImpl(
    private val actionBridge: NotificationActionBridge = NotificationActionBridge,
    private val coroutineScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) : NotificationHistoryRepository {

    companion object {
        private const val TAG = "NotifHistoryRepo"
        const val MAX_CAPACITY = 30
    }

    private val lock = Any()
    // LinkedHashMap with insertion-order preserving newest-first in the list representation
    private val entries = LinkedHashMap<String, NotificationHistoryEntry>()

    private val _history = MutableStateFlow<List<NotificationHistoryEntry>>(emptyList())
    override val history: StateFlow<List<NotificationHistoryEntry>> = _history.asStateFlow()

    private val undoLock = Any()
    private val pendingUndos = ConcurrentHashMap<String, PendingUndoRecord>()
    private val pendingUndoOrder = mutableListOf<String>()

    private val _pendingUndoFlow = MutableStateFlow<UndoNotificationState?>(null)
    override val pendingUndoFlow: StateFlow<UndoNotificationState?> = _pendingUndoFlow.asStateFlow()

    override val size: Int
        get() = synchronized(lock) { entries.size }

    override val pendingUndoCount: Int
        get() = synchronized(undoLock) { pendingUndos.size }

    override fun recordNotification(
        key: String,
        packageName: String,
        appDisplayName: String,
        title: String,
        text: String,
        subText: String?,
        category: String?,
        postTime: Long,
        sourcePostTime: Long,
        ingestSequence: Long,
        isOngoing: Boolean,
        isClearable: Boolean,
        visibility: Int,
        previewMode: NotificationPreviewMode,
        actionGeneration: Long,
        progress: Int,
        maxProgress: Int,
        isIndeterminate: Boolean,
        hasProgress: Boolean,
        progressState: DownloadProgressState
    ): Boolean {
        // Invariant: Notifications with previewMode == HIDDEN never enter history
        if (previewMode == NotificationPreviewMode.HIDDEN) {
            Log.d(TAG, "Rejecting recordNotification: previewMode is HIDDEN for key=$key")
            return false
        }

        // If this key was pending undo, cancel its pending undo and timeout job
        synchronized(undoLock) {
            val pending = pendingUndos[key]
            if (pending != null && !pending.isBatch) {
                pendingUndos.remove(key)
                pendingUndoOrder.remove(key)
                pending.timeoutJob?.cancel()
                updatePendingUndoFlowLocked()
                Log.d(TAG, "New notification for key=$key superseded pending undo")
            }
        }

        var evictedEntry: NotificationHistoryEntry? = null

        synchronized(lock) {
            val existing = entries[key]

            if (existing != null) {
                // Check event freshness against stored entry
                if (!isEventFresh(sourcePostTime, ingestSequence, existing)) {
                    Log.w(TAG, "REJECT STALE UPDATE: Incoming (postTime=$sourcePostTime, seq=$ingestSequence) is older than stored (postTime=${existing.sourcePostTime}, seq=${existing.ingestSequence}) for key=$key")
                    return false
                }

                // Conservative update/removal race resolution:
                // If notification was previously removed, only resurrect if sourcePostTime is strictly newer
                if (!existing.isSystemActive && sourcePostTime <= existing.lastRemovalPostTime) {
                    Log.w(TAG, "REJECT STALE RESURRECTION: Incoming postTime ($sourcePostTime) <= lastRemovalPostTime (${existing.lastRemovalPostTime}) for key=$key")
                    return false
                }

                // Update entry in-place; preserve original firstPostTime
                val updated = existing.copy(
                    rawTitle = title,
                    rawText = text,
                    rawSubText = subText,
                    category = category ?: existing.category,
                    lastUpdateTime = postTime,
                    sourcePostTime = sourcePostTime,
                    ingestSequence = ingestSequence,
                    isSystemActive = true,
                    isOngoing = isOngoing,
                    isClearable = isClearable,
                    visibility = visibility,
                    previewMode = previewMode,
                    actionGeneration = actionGeneration,
                    updateCount = existing.updateCount + 1,
                    progress = progress,
                    maxProgress = maxProgress,
                    isIndeterminate = isIndeterminate,
                    hasProgress = hasProgress,
                    progressState = progressState
                )

                // If this is an in-place download progress update, retain exact position in map
                // to avoid jarring list jumps during ongoing download progress streaming
                val isPureProgressUpdate = existing.hasProgress && hasProgress && (progress != existing.progress || progressState != existing.progressState)

                if (isPureProgressUpdate) {
                    entries[key] = updated
                } else {
                    // Move updated entry to the front (newest-first)
                    val newMap = LinkedHashMap<String, NotificationHistoryEntry>()
                    newMap[key] = updated
                    for ((k, v) in entries) {
                        if (k != key) {
                            newMap[k] = v
                        }
                    }
                    entries.clear()
                    entries.putAll(newMap)
                }
            } else {
                // New entry insertion
                if (entries.size >= MAX_CAPACITY) {
                    // Evict oldest entry (tail of the map)
                    val oldestKey = entries.keys.lastOrNull()
                    if (oldestKey != null) {
                        evictedEntry = entries.remove(oldestKey)
                        Log.d(TAG, "Capacity $MAX_CAPACITY reached. Evicting oldest entry: $oldestKey")
                    }
                }

                val newEntry = NotificationHistoryEntry(
                    key = key,
                    packageName = packageName,
                    appDisplayName = appDisplayName,
                    rawTitle = title,
                    rawText = text,
                    rawSubText = subText,
                    category = category,
                    firstPostTime = postTime,
                    lastUpdateTime = postTime,
                    sourcePostTime = sourcePostTime,
                    ingestSequence = ingestSequence,
                    isSystemActive = true,
                    isOngoing = isOngoing,
                    isClearable = isClearable,
                    visibility = visibility,
                    previewMode = previewMode,
                    actionGeneration = actionGeneration,
                    updateCount = 0,
                    progress = progress,
                    maxProgress = maxProgress,
                    isIndeterminate = isIndeterminate,
                    hasProgress = hasProgress,
                    progressState = progressState
                )

                // Prepend to top
                val newMap = LinkedHashMap<String, NotificationHistoryEntry>()
                newMap[key] = newEntry
                newMap.putAll(entries)
                entries.clear()
                entries.putAll(newMap)
            }

            _history.value = entries.values.toList()
        }

        // Lock-Free External Invariant: Fired strictly outside synchronized block
        evictedEntry?.let { evicted ->
            actionBridge.unregisterIfGenerationMatches(evicted.key, evicted.actionGeneration)
            Log.d(TAG, "Unregistered actions for evicted entry: key=${evicted.key}, gen=${evicted.actionGeneration}")
        }

        return true
    }

    override fun markSystemNotificationRemoved(
        key: String,
        removalPostTime: Long,
        removalSequence: Long
    ) {
        synchronized(lock) {
            val existing = entries[key]
            if (existing != null) {
                val updated = existing.copy(
                    isSystemActive = false,
                    lastRemovalPostTime = removalPostTime,
                    lastRemovalSequence = removalSequence
                )
                entries[key] = updated
                _history.value = entries.values.toList()
                Log.d(TAG, "Marked notification as system-removed: key=$key (retained in history)")
            }
        }

        // Also update pending undo record if present
        synchronized(undoLock) {
            val pending = pendingUndos[key]
            if (pending != null) {
                val updatedItems = pending.items.map { (idx, entry) ->
                    if (entry.key == key) {
                        idx to entry.copy(
                            isSystemActive = false,
                            lastRemovalPostTime = removalPostTime,
                            lastRemovalSequence = removalSequence
                        )
                    } else {
                        idx to entry
                    }
                }
                pendingUndos[key] = pending.copy(items = updatedItems)
            }
        }
    }

    override fun dismissEntry(key: String, undoTimeoutMs: Long): NotificationHistoryEntry? {
        val entryToDismiss: NotificationHistoryEntry
        val originalIndex: Int
        synchronized(lock) {
            val currentList = entries.values.toList()
            val idx = currentList.indexOfFirst { it.key == key }
            if (idx == -1) return null
            entryToDismiss = currentList[idx]
            originalIndex = idx
            entries.remove(key)
            _history.value = entries.values.toList()
        }

        val recordId = key
        val title = entryToDismiss.rawTitle.ifBlank { entryToDismiss.appDisplayName }
        val undoState = UndoNotificationState(
            id = recordId,
            title = title,
            timestamp = System.currentTimeMillis(),
            timeoutMs = undoTimeoutMs,
            isBatch = false,
            count = 1
        )

        val job = coroutineScope.launch {
            delay(undoTimeoutMs)
            finalizeDismissal(recordId)
        }

        val record = PendingUndoRecord(
            id = recordId,
            isBatch = false,
            items = listOf(originalIndex to entryToDismiss),
            dismissedAt = undoState.timestamp,
            undoTimeoutMs = undoTimeoutMs,
            timeoutJob = job
        )

        synchronized(undoLock) {
            // Cancel any existing pending undo for this key
            pendingUndos[recordId]?.timeoutJob?.cancel()
            pendingUndos[recordId] = record
            pendingUndoOrder.remove(recordId)
            pendingUndoOrder.add(recordId)
            _pendingUndoFlow.value = undoState
        }

        Log.d(TAG, "Dismissed entry key=$key (index $originalIndex). Pending undo for ${undoTimeoutMs}ms.")
        return entryToDismiss
    }

    override fun undoDismiss(key: String?): Boolean {
        val recordToUndo: PendingUndoRecord?
        synchronized(undoLock) {
            val targetId = key ?: pendingUndoOrder.lastOrNull() ?: return false
            recordToUndo = pendingUndos.remove(targetId)
            pendingUndoOrder.remove(targetId)
            updatePendingUndoFlowLocked()
        }

        if (recordToUndo == null) return false
        recordToUndo.timeoutJob?.cancel()

        val evictedEntries = mutableListOf<NotificationHistoryEntry>()
        synchronized(lock) {
            if (recordToUndo.isBatch) {
                // Reconstruct batch in original order
                val newEntries = LinkedHashMap<String, NotificationHistoryEntry>()
                val sortedItems = recordToUndo.items.sortedBy { it.first }
                for ((_, item) in sortedItems) {
                    newEntries[item.key] = item
                }
                for ((k, v) in entries) {
                    if (!newEntries.containsKey(k)) {
                        newEntries[k] = v
                    }
                }
                entries.clear()
                while (newEntries.size > MAX_CAPACITY) {
                    val oldestKey = newEntries.keys.lastOrNull() ?: break
                    val evicted = newEntries.remove(oldestKey)
                    if (evicted != null) evictedEntries.add(evicted)
                }
                entries.putAll(newEntries)
            } else {
                val (originalIndex, entry) = recordToUndo.items.first()
                val currentList = entries.values.toMutableList()
                val insertIndex = originalIndex.coerceIn(0, currentList.size)
                currentList.add(insertIndex, entry)

                entries.clear()
                while (currentList.size > MAX_CAPACITY) {
                    val evicted = currentList.removeAt(currentList.lastIndex)
                    evictedEntries.add(evicted)
                }
                for (item in currentList) {
                    entries[item.key] = item
                }
            }
            _history.value = entries.values.toList()
        }

        // Lock-Free External Invariant: unregister evicted actions outside locks
        for (evicted in evictedEntries) {
            actionBridge.unregisterIfGenerationMatches(evicted.key, evicted.actionGeneration)
            Log.d(TAG, "Unregistered actions for capacity-evicted entry on undo: key=${evicted.key}")
        }

        Log.d(TAG, "Undo successful for dismissal id=${recordToUndo.id}. Restored ${recordToUndo.items.size} entries.")
        return true
    }

    override fun clearAllLocalHistory(undoTimeoutMs: Long): Int {
        val clearedItems: List<Pair<Int, NotificationHistoryEntry>>
        synchronized(lock) {
            if (entries.isEmpty()) return 0
            clearedItems = entries.values.mapIndexed { index, entry -> index to entry }
            entries.clear()
            _history.value = emptyList()
        }

        val recordId = "batch_clear_${System.currentTimeMillis()}"
        val undoState = UndoNotificationState(
            id = recordId,
            title = "${clearedItems.size} alerts cleared",
            timestamp = System.currentTimeMillis(),
            timeoutMs = undoTimeoutMs,
            isBatch = true,
            count = clearedItems.size
        )

        val job = coroutineScope.launch {
            delay(undoTimeoutMs)
            finalizeDismissal(recordId)
        }

        val record = PendingUndoRecord(
            id = recordId,
            isBatch = true,
            items = clearedItems,
            dismissedAt = undoState.timestamp,
            undoTimeoutMs = undoTimeoutMs,
            timeoutJob = job
        )

        synchronized(undoLock) {
            pendingUndos[recordId] = record
            pendingUndoOrder.remove(recordId)
            pendingUndoOrder.add(recordId)
            _pendingUndoFlow.value = undoState
        }

        Log.d(TAG, "Cleared all local history (${clearedItems.size} items). Pending batch undo for ${undoTimeoutMs}ms.")
        return clearedItems.size
    }

    override fun undoClearAll(): Boolean {
        var batchId: String? = null
        synchronized(undoLock) {
            batchId = pendingUndoOrder.lastOrNull { id ->
                pendingUndos[id]?.isBatch == true
            }
        }
        return if (batchId != null) undoDismiss(batchId) else false
    }

    override fun isPendingUndo(key: String): Boolean {
        return synchronized(undoLock) {
            pendingUndos.containsKey(key) || pendingUndos.values.any { record ->
                record.isBatch && record.items.any { it.second.key == key }
            }
        }
    }

    private fun finalizeDismissal(id: String) {
        val record: PendingUndoRecord?
        synchronized(undoLock) {
            record = pendingUndos.remove(id)
            pendingUndoOrder.remove(id)
            updatePendingUndoFlowLocked()
        }

        if (record != null) {
            Log.d(TAG, "Undo period expired for dismissal id=$id (${record.items.size} items). Finalizing unregistration.")
            // Lock-Free External Invariant: unregister actions outside any lock
            for ((_, entry) in record.items) {
                actionBridge.unregisterIfGenerationMatches(entry.key, entry.actionGeneration)
            }
        }
    }

    private fun updatePendingUndoFlowLocked() {
        val latestId = pendingUndoOrder.lastOrNull()
        if (latestId == null) {
            _pendingUndoFlow.value = null
        } else {
            val record = pendingUndos[latestId]
            if (record != null) {
                _pendingUndoFlow.value = UndoNotificationState(
                    id = record.id,
                    title = if (record.isBatch) "${record.items.size} alerts cleared" else record.items.first().second.rawTitle.ifBlank { record.items.first().second.appDisplayName },
                    timestamp = record.dismissedAt,
                    timeoutMs = record.undoTimeoutMs,
                    isBatch = record.isBatch,
                    count = record.items.size
                )
            } else {
                _pendingUndoFlow.value = null
            }
        }
    }

    override fun removeEntry(key: String): Boolean {
        var removedEntry: NotificationHistoryEntry? = null
        synchronized(lock) {
            removedEntry = entries.remove(key)
            if (removedEntry != null) {
                _history.value = entries.values.toList()
            }
        }

        // Cancel any pending undo for this key
        synchronized(undoLock) {
            val pending = pendingUndos.remove(key)
            if (pending != null) {
                pendingUndoOrder.remove(key)
                pending.timeoutJob?.cancel()
                updatePendingUndoFlowLocked()
            }
        }

        // Lock-Free External Invariant
        removedEntry?.let { entry ->
            actionBridge.unregisterIfGenerationMatches(entry.key, entry.actionGeneration)
            return true
        }

        return false
    }

    override fun clearAll() {
        val clearedEntries: List<NotificationHistoryEntry>
        synchronized(lock) {
            clearedEntries = entries.values.toList()
            entries.clear()
            _history.value = emptyList()
        }

        // Cancel all pending undos and collect their entries
        val pendingEntries = mutableListOf<NotificationHistoryEntry>()
        synchronized(undoLock) {
            for ((_, record) in pendingUndos) {
                record.timeoutJob?.cancel()
                pendingEntries.addAll(record.items.map { it.second })
            }
            pendingUndos.clear()
            pendingUndoOrder.clear()
            _pendingUndoFlow.value = null
        }

        // Lock-Free External Invariant
        for (entry in clearedEntries) {
            actionBridge.unregisterIfGenerationMatches(entry.key, entry.actionGeneration)
        }
        for (entry in pendingEntries) {
            actionBridge.unregisterIfGenerationMatches(entry.key, entry.actionGeneration)
        }
        Log.i(TAG, "NotificationHistoryRepository cleared all entries (${clearedEntries.size} active, ${pendingEntries.size} pending undo).")
    }

    override fun getEntry(key: String): NotificationHistoryEntry? {
        return synchronized(lock) { entries[key] }
    }

    override fun isFresh(key: String, sourcePostTime: Long, ingestSequence: Long): Boolean {
        synchronized(lock) {
            val existing = entries[key] ?: return true
            if (!isEventFresh(sourcePostTime, ingestSequence, existing)) {
                return false
            }
            if (!existing.isSystemActive && sourcePostTime <= existing.lastRemovalPostTime) {
                return false
            }
            return true
        }
    }

    /**
     * Determines whether an incoming callback is fresher than the stored entry.
     */
    private fun isEventFresh(
        incomingPostTime: Long,
        incomingSequence: Long,
        stored: NotificationHistoryEntry
    ): Boolean {
        return when {
            incomingPostTime > stored.sourcePostTime -> true
            incomingPostTime < stored.sourcePostTime -> false
            else -> incomingSequence >= stored.ingestSequence
        }
    }
}
