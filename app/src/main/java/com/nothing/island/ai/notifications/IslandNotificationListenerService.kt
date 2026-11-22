package com.nothing.island.ai.notifications

import android.app.KeyguardManager
import android.app.Notification
import com.nothing.island.ai.telephony.CallActionRegistry
import com.nothing.island.ai.telephony.CallNotificationParser
import com.nothing.island.ai.telephony.CallPhase
import com.nothing.island.ai.telephony.CallSignal
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.nothing.island.ai.IslandOverlayController
import com.nothing.island.ai.data.IslandPreferencesRepository
import com.nothing.island.ai.engine.IslandEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * IslandNotificationListenerService
 *
 * Headless AOSP NotificationListenerService for Nothing Island (Milestone R6).
 * Manages notification listener connection lifecycle, multi-stage filtering,
 * silent initial reconciliation, serialized callback ingestion, action intent registration,
 * and decoupled in-memory history management.
 *
 * INVARIANT: Operates strictly headless in background. Does NOT touch WindowManager
 * or claim overlay ownership (IslandAccessibilityService is the sole overlay owner).
 */
class IslandNotificationListenerService : NotificationListenerService() {

    companion object {
        private const val TAG = "IslandNotifListener"

        @Volatile
        var isConnected: Boolean = false
            private set

        @Volatile
        var instance: IslandNotificationListenerService? = null
            private set

        private val _connectionState = MutableStateFlow(false)
        val connectionState: StateFlow<Boolean> = _connectionState.asStateFlow()

        val isServiceBound: Boolean
            get() = isConnected && instance != null

        /**
         * Safely attempts to request rebinding if the system has unbound the listener
         * under memory pressure while user permission remains granted.
         */
        fun requestRebindIfNecessary(context: Context) {
            if (!isConnected && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                try {
                    requestRebind(ComponentName(context, IslandNotificationListenerService::class.java))
                    Log.i(TAG, "Requested rebind for IslandNotificationListenerService")
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to request rebind", e)
                }
            }
        }

        /**
         * Cancels an active system notification from the notification shade.
         */
        fun cancelSystemNotification(key: String): Boolean {
            val service = instance ?: return false
            return try {
                service.cancelNotification(key)
                true
            } catch (e: Exception) {
                Log.w(TAG, "Failed to cancel notification $key", e)
                false
            }
        }

        /**
         * REQ-29: Selectively cancels the specified clearable notifications from the Android notification shade.
         * Strictly scoped to notifications represented by Nothing Island to prevent dismissing unrelated apps.
         */
        fun cancelSystemNotifications(keys: Collection<String>): Int {
            val service = instance ?: return 0
            var count = 0
            for (k in keys) {
                try {
                    service.cancelNotification(k)
                    count++
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to cancel notification $k", e)
                }
            }
            Log.i(TAG, "Selective system notification cancellation: $count of ${keys.size} notifications cleared")
            return count
        }
    }

    // Serialized ingestion dispatcher enforcing FIFO callback processing
    private val ingestionScope = CoroutineScope(SupervisorJob() + Dispatchers.Default.limitedParallelism(1))

    // Monotonic sequence counter assigned at callback arrival
    private val ingressSequence = AtomicLong(0L)

    /** R9: notification key of the call currently presented by the island. */
    @Volatile
    private var activeCallKey: String? = null

    /** Keys we cancelled ourselves via Hide System Notification; their removal callbacks are ignored. */
    private val selfCancelledKeys = java.util.Collections.newSetFromMap(java.util.concurrent.ConcurrentHashMap<String, Boolean>())

    // Set of notification keys present at connection time (historical)
    private val historicalNotificationKeys = ConcurrentHashMap.newKeySet<String>()

    // Dedicated filtering and arbitration pipeline
    internal var notificationFilter: NotificationFilter = NotificationFilter()

    /**
     * Exposes whether a notification key was reconciled as historical at connection time.
     */
    fun isHistorical(key: String): Boolean = historicalNotificationKeys.contains(key)

    /**
     * Number of reconciled historical notifications for test and diagnostic queries.
     */
    val reconciledHistoricalCount: Int
        get() = historicalNotificationKeys.size

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.i(TAG, "IslandNotificationListenerService connected. Initializing listener state.")
        isConnected = true
        instance = this
        _connectionState.value = true
        notificationFilter = NotificationFilter(applicationContext)

        reconcileActiveNotificationsSilently()

        try {
            IslandOverlayController.getInstance(applicationContext)
                .startMediaTracking(ComponentName(this, IslandNotificationListenerService::class.java))
        } catch (e: Exception) {
            Log.w(TAG, "Failed to start media tracking on listener connected", e)
        }
    }

    override fun onListenerDisconnected() {
        Log.i(TAG, "IslandNotificationListenerService disconnected.")
        cleanupListenerState()
        super.onListenerDisconnected()
    }

    override fun onDestroy() {
        Log.i(TAG, "IslandNotificationListenerService onDestroy.")
        cleanupListenerState()
        ingestionScope.coroutineContext.cancelChildren()
        super.onDestroy()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return

        ingestionScope.launch {
            val seq = ingressSequence.incrementAndGet()
            val key = sbn.key ?: return@launch
            val postTime = sbn.postTime

            // R9: Route incoming-call notifications straight to the telephony engine.
            // This avoids restricted READ_PHONE_STATE / ANSWER_PHONE_CALLS permissions.
            val callInfo = CallNotificationParser.parse(
                CallSignal(
                    category = sbn.notification.category,
                    title = sbn.notification.extras.getCharSequence(Notification.EXTRA_TITLE)?.toString(),
                    text = sbn.notification.extras.getCharSequence(Notification.EXTRA_TEXT)?.toString(),
                    actionTitles = sbn.notification.actions?.mapNotNull { it.title?.toString() } ?: emptyList(),
                    showChronometer = sbn.notification.extras.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER, false),
                    subText = sbn.notification.extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString(),
                )
            )
            if (callInfo != null) {
                val controller = IslandOverlayController.getInstance(applicationContext)
                val callPrefs = controller.preferencesRepository.liveCallPreferences.value
                val callActions = sbn.notification.actions.orEmpty()
                fun actionFor(vararg keywords: String) = callActions.firstOrNull { action ->
                    val label = action.title?.toString()?.lowercase().orEmpty()
                    keywords.any { label.contains(it) }
                }?.actionIntent

                when (callInfo.phase) {
                    CallPhase.INCOMING -> {
                        activeCallKey = key
                        CallActionRegistry.register(
                            CallActionRegistry.CallActions(
                                key = key,
                                answer = actionFor("answer", "accept"),
                                decline = actionFor("decline", "reject", "hang", "end"),
                            )
                        )
                        controller.apply {
                            eventEngine.emit(
                                IslandEvent.IncomingCallRinging(
                                    id = key,
                                    callId = key,
                                    callerDisplayName = callInfo.callerDisplayName,
                                    isVideoCall = callInfo.isVideoCall,
                                )
                            )
                            if (callPrefs.autoExpandCall) {
                                repository.expandCall()
                            }
                        }
                        Log.i(TAG, "R9 incoming call routed to island: ${callInfo.callerDisplayName}")
                    }

                    CallPhase.DIALING -> {
                        activeCallKey = key
                        CallActionRegistry.register(
                            CallActionRegistry.CallActions(
                                key = key,
                                answer = null,
                                decline = actionFor("decline", "reject", "hang", "end"),
                            )
                        )
                        controller.stopCallTicker()
                        controller.eventEngine.emit(
                            IslandEvent.CallConnected(
                                id = "call_connected_$key",
                                callId = key,
                                callerDisplayName = callInfo.callerDisplayName,
                                durationSeconds = 0L,
                                isDialing = true,
                            )
                        )
                        if (callPrefs.autoExpandCall) {
                            controller.repository.expandCall()
                        }
                        Log.i(TAG, "R9 call dialing/outgoing: ${callInfo.callerDisplayName}")
                    }

                    CallPhase.ONGOING -> {
                        activeCallKey = key
                        CallActionRegistry.register(
                            CallActionRegistry.CallActions(
                                key = key,
                                answer = null,
                                decline = actionFor("decline", "reject", "hang", "end"),
                            )
                        )
                        val active = controller.priorityManager?.activeEvent as? IslandEvent.CallConnected
                        val wasDialing = active?.isDialing == true
                        val isAlreadyConnected = active != null && !wasDialing
                        if (!isAlreadyConnected) {
                            controller.eventEngine.emit(
                                IslandEvent.CallConnected(
                                    id = "call_connected_$key",
                                    callId = key,
                                    callerDisplayName = callInfo.callerDisplayName,
                                    durationSeconds = 0L,
                                    isDialing = false,
                                )
                            )
                            if (callPrefs.autoExpandCall) {
                                controller.repository.expandCall()
                            }
                            controller.startCallTicker(key, callInfo.callerDisplayName)
                            Log.i(TAG, "R9 call connected: ${callInfo.callerDisplayName}")
                        }
                    }
                }
                return@launch
            }

            // 1. Silent check: Do not emit alerts for historical notifications cataloged at connection time
            val isHistorical = isHistorical(key)

            try {
                val controller = IslandOverlayController.getInstance(applicationContext)
                val historyRepo = controller.historyRepository

                // 2. Validate event freshness and history eligibility BEFORE touching ActionBridge
                if (!historyRepo.isFresh(key, postTime, seq)) {
                    Log.w(TAG, "REJECT STALE INGRESS: Incoming event for key=$key (postTime=$postTime, seq=$seq) is stale")
                    return@launch
                }

                val prefs = try {
                    IslandPreferencesRepository.getInstance(applicationContext).liveNotificationPreferences.value
                } catch (_: Exception) {
                    NotificationPreferences.DEFAULTS
                }

                // If preview mode is HIDDEN, suppress history and presentation completely
                if (prefs.previewMode == NotificationPreviewMode.HIDDEN) {
                    Log.d(TAG, "Suppressed notification under HIDDEN preview mode for key=$key")
                    return@launch
                }

                val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
                val isKeyguardLocked = keyguardManager?.isKeyguardLocked ?: false

                // 3. Process through NotificationFilter
                val event = notificationFilter.filter(sbn, prefs, isKeyguardLocked)
                if (event == null) {
                    Log.d(TAG, "Notification dropped or suppressed by filter for key=$key")
                    return@launch
                }

                // 4. Register actions into ActionBridge -> returns monotonic generation
                val actionItems = sbn.notification.actions?.mapNotNull { action ->
                    val title = action.title?.toString()
                    if (title != null) {
                        val remoteInputs = action.remoteInputs
                        NotificationActionItem(
                            title = title,
                            actionIntent = action.actionIntent,
                            isContextual = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) action.isContextual else false,
                            isReply = !remoteInputs.isNullOrEmpty(),
                            remoteInputs = remoteInputs
                        )
                    } else null
                } ?: emptyList()

                val actionGeneration = NotificationActionBridge.register(
                    key = key,
                    packageName = sbn.packageName,
                    contentIntent = sbn.notification.contentIntent,
                    actions = actionItems,
                    token = postTime
                )

                // 5. Commit to history repository
                val notification = sbn.notification
                val extras = notification.extras
                val rawTitle = extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim() ?: ""
                val rawText = extras?.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim() ?: ""
                val rawSubText = extras?.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()?.trim()

                val appName = try {
                    val appInfo = packageManager.getApplicationInfo(sbn.packageName, 0)
                    packageManager.getApplicationLabel(appInfo).toString()
                } catch (_: Exception) {
                    sbn.packageName.substringAfterLast('.').ifBlank { "APP" }
                }

                val committed = historyRepo.recordNotification(
                    key = key,
                    packageName = sbn.packageName,
                    appDisplayName = appName,
                    title = rawTitle,
                    text = rawText,
                    subText = rawSubText,
                    category = notification.category,
                    postTime = System.currentTimeMillis(),
                    sourcePostTime = postTime,
                    ingestSequence = seq,
                    isOngoing = sbn.isOngoing,
                    isClearable = sbn.isClearable,
                    visibility = notification.visibility,
                    previewMode = prefs.previewMode,
                    actionGeneration = actionGeneration,
                    progress = event.progress,
                    maxProgress = event.maxProgress,
                    isIndeterminate = event.isIndeterminate,
                    hasProgress = event.hasProgress,
                    progressState = event.progressState
                )

                if (!committed) {
                    // Commit failed (e.g. race rejection or stale callback): rollback actions
                    Log.w(TAG, "History commit rejected for key=$key. Rolling back action generation $actionGeneration")
                    NotificationActionBridge.unregisterIfGenerationMatches(key, actionGeneration)
                    return@launch
                }

                // 6. Emit presentation event ONLY after successful commit (and only if not historical)
                if (event != null && !isHistorical) {
                    // Island always presents the alert (slide-down popup).
                    val accepted = controller.eventEngine.emit(event)
                    Log.i(TAG, "Notification accepted=$accepted & emitted: id=${event.id}, pkg=${event.packageName}")

                    // "Hide System (Double) Notification": cancel the redundant
                    // Android heads-up so the same alert does not appear twice.
                    if (accepted && prefs.hideSystemNotification && sbn.isClearable && !sbn.isOngoing) {
                        try {
                            selfCancelledKeys.add(key)
                            cancelNotification(key)
                            Log.i(TAG, "Hide System Notification: heads-up cancelled key=$key")
                        } catch (e: Exception) {
                            Log.w(TAG, "Failed to cancel redundant notification key=$key", e)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing incoming notification for key=$key", e)
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        if (sbn == null) return

        ingestionScope.launch {
            val seq = ingressSequence.incrementAndGet()
            val key = sbn.key ?: return@launch
            val postTime = sbn.postTime

            if (selfCancelledKeys.remove(key)) {
                Log.d(TAG, "Ignoring self-inflicted removal callback for key=$key (Hide System Notification)")
                return@launch
            }

            if (activeCallKey == key) {
                activeCallKey = null
                CallActionRegistry.clear(key)
                val controller = IslandOverlayController.getInstance(applicationContext)
                controller.stopCallTicker()
                controller.eventEngine.emit(
                    IslandEvent.CallEnded(id = "call_end_$key", callId = key)
                )
                Log.i(TAG, "R9 call ended for key=$key")
            }

            try {
                historicalNotificationKeys.remove(key)
                notificationFilter.onNotificationRemoved(key, sbn.groupKey)

                val controller = IslandOverlayController.getInstance(applicationContext)

                // Mark system notification removed in history, but RETAIN history entry and actions!
                controller.historyRepository.markSystemNotificationRemoved(
                    key = key,
                    removalPostTime = postTime,
                    removalSequence = seq
                )

                val dismissalEvent = IslandEvent.NotificationDismissed(
                    id = key,
                    targetNotificationId = key,
                    packageName = sbn.packageName
                )

                controller.eventEngine.emit(dismissalEvent)
                Log.d(TAG, "Notification dismissal emitted: id=$key, pkg=${sbn.packageName}")
            } catch (e: Exception) {
                Log.e(TAG, "Error processing notification removal for key=$key", e)
            }
        }
    }

    /**
     * Silently indexes currently active status bar notifications upon connection.
     * Prevents historical alerts from triggering an immediate flood of island popups
     * while silently populating the in-memory history repository.
     */
    internal fun reconcileActiveNotificationsSilently() {
        historicalNotificationKeys.clear()
        try {
            val activeList = activeNotifications ?: return
            val controller = IslandOverlayController.getInstance(applicationContext)
            val historyRepo = controller.historyRepository
            val prefs = try {
                IslandPreferencesRepository.getInstance(applicationContext).liveNotificationPreferences.value
            } catch (_: Exception) {
                NotificationPreferences.DEFAULTS
            }

            // Sort active notifications: non-group summaries first so child alerts populate
            // activeGroupChildren before group summaries are arbitrated
            val sortedActiveList = activeList.filterNotNull().sortedBy { sbn ->
                (sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0
            }

            for (sbn in sortedActiveList) {
                if (sbn.key == null) continue
                historicalNotificationKeys.add(sbn.key)

                if (prefs.previewMode != NotificationPreviewMode.HIDDEN) {
                    val event = notificationFilter.filter(sbn, prefs, isKeyguardLocked = false)
                    if (event == null) {
                        Log.d(TAG, "Reconciliation: suppressed by filter for key=${sbn.key}")
                        continue
                    }

                    val actionItems = sbn.notification.actions?.mapNotNull { action ->
                        val title = action.title?.toString()
                        if (title != null) {
                            val remoteInputs = action.remoteInputs
                            NotificationActionItem(
                                title = title,
                                actionIntent = action.actionIntent,
                                isContextual = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) action.isContextual else false,
                                isReply = !remoteInputs.isNullOrEmpty(),
                                remoteInputs = remoteInputs
                            )
                        } else null
                    } ?: emptyList()

                    val gen = NotificationActionBridge.register(
                        key = sbn.key,
                        packageName = sbn.packageName,
                        contentIntent = sbn.notification.contentIntent,
                        actions = actionItems,
                        token = sbn.postTime
                    )

                    val notification = sbn.notification
                    val extras = notification.extras
                    val rawTitle = extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim() ?: ""
                    val rawText = extras?.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim() ?: ""
                    val rawSubText = extras?.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()?.trim()

                    val appName = try {
                        val appInfo = packageManager.getApplicationInfo(sbn.packageName, 0)
                        packageManager.getApplicationLabel(appInfo).toString()
                    } catch (_: Exception) {
                        sbn.packageName.substringAfterLast('.').ifBlank { "APP" }
                    }

                    historyRepo.recordNotification(
                        key = sbn.key,
                        packageName = sbn.packageName,
                        appDisplayName = appName,
                        title = rawTitle,
                        text = rawText,
                        subText = rawSubText,
                        category = notification.category,
                        postTime = sbn.postTime,
                        sourcePostTime = sbn.postTime,
                        ingestSequence = ingressSequence.incrementAndGet(),
                        isOngoing = sbn.isOngoing,
                        isClearable = sbn.isClearable,
                        visibility = notification.visibility,
                        previewMode = prefs.previewMode,
                        actionGeneration = gen,
                        progress = event.progress,
                        maxProgress = event.maxProgress,
                        isIndeterminate = event.isIndeterminate,
                        hasProgress = event.hasProgress,
                        progressState = event.progressState
                    )
                }
            }
            Log.i(TAG, "Silently reconciled ${historicalNotificationKeys.size} historical active notifications.")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to query active notifications during silent reconciliation", e)
        }
    }

    internal fun cleanupListenerState() {
        isConnected = false
        if (instance === this) {
            instance = null
        }
        _connectionState.value = false
        historicalNotificationKeys.clear()
        notificationFilter.reset()
        // ActionBridge cleared on listener disconnect, but history repository is preserved!
        NotificationActionBridge.clearAll()
        try {
            IslandOverlayController.getInstance(applicationContext).stopMediaTracking()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to stop media tracking on cleanup", e)
        }
    }

    /**
     * Testing hooks to verify reconciliation behavior in unit tests without mocking framework binders.
     */
    internal fun addHistoricalKeyForTesting(key: String) {
        historicalNotificationKeys.add(key)
    }

    internal fun clearHistoricalKeysForTesting() {
        historicalNotificationKeys.clear()
    }

    internal fun setConnectedForTesting(connected: Boolean) {
        isConnected = connected
        _connectionState.value = connected
        if (connected) {
            instance = this
        } else if (instance === this) {
            instance = null
        }
    }
}
