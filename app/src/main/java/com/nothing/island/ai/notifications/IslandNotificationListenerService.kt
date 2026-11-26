package com.nothing.island.ai.notifications

import android.Manifest
import android.annotation.SuppressLint
import android.app.KeyguardManager
import android.app.Notification
import android.content.pm.PackageManager
import com.nothing.island.ai.telephony.CallActionRegistry
import com.nothing.island.ai.telephony.CallNotificationParser
import com.nothing.island.ai.telephony.CallPhase
import com.nothing.island.ai.telephony.CallSignal
import com.nothing.island.ai.telephony.CallerPhotoResolver
import com.nothing.island.ai.telephony.DialerHeadsUpPolicy
import com.nothing.island.ai.telephony.TelephonyCallTransition
import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.content.ContextCompat
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

        fun hasActiveDialerCallNotification(): Boolean {
            val service = instance ?: return false
            return runCatching {
                service.activeNotifications?.any { sbn ->
                    sbn != null &&
                        sbn.notification.category == Notification.CATEGORY_CALL &&
                        (sbn.key == service.activeCallKey ||
                            sbn.packageName.lowercase() in DialerHeadsUpPolicy.DIALER_PACKAGES)
                } == true
            }.getOrDefault(false)
        }

        fun clearActiveCallKeyIfMatches(callId: String) {
            val service = instance ?: return
            if (service.activeCallKey == callId) {
                service.activeCallKey = null
                service.activeCallPhase = null
                service.activeCallCaller = null
                service.activeCallPhoto = null
            }
        }

        /**
         * Safely attempts to request rebinding if the system has unbound the listener
         * under memory pressure while user permission remains granted.
         */
        fun requestRebindIfNecessary(context: Context) {
            if (isConnected) return
            if (!com.nothing.island.ai.OverlayPermissionManager.hasNotificationListenerPermission(context)) {
                return
            }
            try {
                requestRebind(ComponentName(context, IslandNotificationListenerService::class.java))
                Log.i(TAG, "Requested rebind for IslandNotificationListenerService")
            } catch (t: Throwable) {
                Log.w(TAG, "Failed to request rebind", t)
            }
        }

        internal fun extractNotificationText(extras: android.os.Bundle?): String {
            if (extras == null) return ""
            return extractNotificationText(
                primaryText = extras.getCharSequence(Notification.EXTRA_TEXT),
                bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
            )
        }

        internal fun extractNotificationText(primaryText: CharSequence?, bigText: CharSequence?): String {
            val primary = primaryText?.toString()?.trim().orEmpty()
            if (primary.isNotEmpty()) return primary
            return bigText?.toString()?.trim().orEmpty()
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

    /** R9: notification key and phase of the call currently presented by the island. */
    @Volatile
    private var activeCallKey: String? = null

    @Volatile
    private var activeCallPhase: CallPhase? = null

    @Volatile
    private var activeCallCaller: String? = null

    @Volatile
    private var activeCallPhoto: Bitmap? = null

    @Volatile
    private var currentTelephonyCallState: Int = TelephonyManager.CALL_STATE_IDLE

    private var telephonyCallback: Any? = null
    @Suppress("DEPRECATION")
    private var legacyPhoneStateListener: android.telephony.PhoneStateListener? = null

    /** Policy & dedupe guard for suppressing redundant Android heads-up notifications without loops. */
    internal val systemSuppressor = SystemNotificationSuppressor()

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
        registerTelephonyCallStateMonitor()

        try {
            IslandOverlayController.getInstance(applicationContext)
                .startMediaTracking(ComponentName(this, IslandNotificationListenerService::class.java))
        } catch (e: Exception) {
            Log.w(TAG, "Failed to start media tracking on listener connected", e)
        }
    }

    @SuppressLint("MissingPermission")
    private fun registerTelephonyCallStateMonitor() {
        if (ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.READ_PHONE_STATE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val tm = applicationContext.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager ?: return
        runCatching {
            @Suppress("DEPRECATION")
            currentTelephonyCallState = tm.callState
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (telephonyCallback != null) return
            val callback = object : TelephonyCallback(), TelephonyCallback.CallStateListener {
                override fun onCallStateChanged(state: Int) {
                    handleTelephonyStateChanged(state)
                }
            }
            runCatching {
                tm.registerTelephonyCallback(applicationContext.mainExecutor, callback)
                telephonyCallback = callback
                Log.d(TAG, "Registered TelephonyCallback.CallStateListener")
            }.onFailure { e -> Log.w(TAG, "Failed to register TelephonyCallback", e) }
        } else {
            if (legacyPhoneStateListener != null) return
            @Suppress("DEPRECATION")
            val listener = object : android.telephony.PhoneStateListener() {
                @Deprecated("Deprecated in Java")
                override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                    handleTelephonyStateChanged(state)
                }
            }
            @Suppress("DEPRECATION")
            runCatching {
                tm.listen(listener, android.telephony.PhoneStateListener.LISTEN_CALL_STATE)
                legacyPhoneStateListener = listener
            }
        }
    }

    private fun unregisterTelephonyCallStateMonitor() {
        val ctx = runCatching { applicationContext }.getOrNull() ?: return
        val tm = runCatching { ctx.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager }.getOrNull() ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (telephonyCallback as? TelephonyCallback)?.let { cb ->
                runCatching { tm.unregisterTelephonyCallback(cb) }
            }
            telephonyCallback = null
        } else {
            @Suppress("DEPRECATION")
            legacyPhoneStateListener?.let { l ->
                runCatching { tm.listen(l, android.telephony.PhoneStateListener.LISTEN_NONE) }
            }
            legacyPhoneStateListener = null
        }
    }

    @SuppressLint("MissingPermission")
    private fun isTelephonyCallCurrentlyActive(): Boolean {
        if (currentTelephonyCallState == TelephonyManager.CALL_STATE_RINGING ||
            currentTelephonyCallState == TelephonyManager.CALL_STATE_OFFHOOK
        ) {
            return true
        }
        if (ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.READ_PHONE_STATE
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val tm = applicationContext.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            val live = runCatching {
                @Suppress("DEPRECATION")
                tm?.callState
            }.getOrNull()
            if (live != null) {
                currentTelephonyCallState = live
                return live == TelephonyManager.CALL_STATE_RINGING ||
                    live == TelephonyManager.CALL_STATE_OFFHOOK
            }
        }
        return false
    }

    private fun handleTelephonyStateChanged(newState: Int) {
        val prev = currentTelephonyCallState
        currentTelephonyCallState = newState
        val key = activeCallKey ?: return
        when (DialerHeadsUpPolicy.resolveTelephonyStateTransition(prev, newState, hasActiveCall = true)) {
            TelephonyCallTransition.END_CALL -> {
                ingestionScope.launch {
                    if (activeCallKey == key) {
                        clearActiveCallAndEmitEnd(key, reason = "TelephonyManager CALL_STATE_IDLE")
                    }
                }
            }
            TelephonyCallTransition.TRANSITION_TO_OFFHOOK -> {
                ingestionScope.launch {
                    if (activeCallKey == key && activeCallPhase == CallPhase.INCOMING) {
                        val caller = activeCallCaller ?: "Call"
                        val photo = activeCallPhoto
                        val nowMs = System.currentTimeMillis()
                        activeCallPhase = CallPhase.ONGOING
                        val controller = IslandOverlayController.getInstance(applicationContext)
                        controller.eventEngine.emit(
                            IslandEvent.CallConnected(
                                id = "call_connected_$key",
                                callId = key,
                                callerDisplayName = caller,
                                durationSeconds = 0L,
                                isDialing = false,
                                connectedAtMs = nowMs,
                                contactPhoto = photo,
                            )
                        )
                        controller.startCallTicker(
                            callId = key,
                            caller = caller,
                            contactPhoto = photo,
                            isDialing = false,
                            connectedAtMs = nowMs,
                        )
                    }
                }
            }
            TelephonyCallTransition.NO_CHANGE -> Unit
        }
    }

    private suspend fun clearActiveCallAndEmitEnd(key: String, reason: String) {
        activeCallKey = null
        activeCallPhase = null
        activeCallCaller = null
        activeCallPhoto = null
        CallActionRegistry.clear(key)
        com.nothing.island.ai.telephony.CallController.resetCallStates()
        val controller = IslandOverlayController.getInstance(applicationContext)
        controller.stopCallTicker()
        controller.eventEngine.emit(
            IslandEvent.CallEnded(id = "call_end_$key", callId = key)
        )
        controller.collapse()
        Log.i(TAG, "R9 call ended for key=$key ($reason)")
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
            val extras = sbn.notification.extras
            val callPersonObj: android.app.Person? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    extras?.getParcelable(Notification.EXTRA_CALL_PERSON, android.app.Person::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    extras?.getParcelable(Notification.EXTRA_CALL_PERSON)
                }
            } else {
                null
            }
            val callPersonName: String? = callPersonObj?.name?.toString()
            val callPersonUri: String? = callPersonObj?.uri
                ?: extras?.getString("android.telecom.extra.INCOMING_CALL_ADDRESS")
                ?: extras?.getCharSequence(Notification.EXTRA_TEXT)?.toString()
            val callTypeExtra: Int? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && extras?.containsKey(Notification.EXTRA_CALL_TYPE) == true) {
                extras.getInt(Notification.EXTRA_CALL_TYPE)
            } else {
                null
            }
            val callActions = sbn.notification.actions.orEmpty()
            val rawCallActions = callActions.map { action ->
                val semantic = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) action.semanticAction else 0
                val iconHint = runCatching {
                    val icon = action.getIcon()
                    if (icon != null && icon.type == android.graphics.drawable.Icon.TYPE_RESOURCE) {
                        val pkg = icon.resPackage ?: sbn.packageName
                        applicationContext.packageManager
                            .getResourcesForApplication(pkg)
                            .getResourceEntryName(icon.resId)
                    } else {
                        ""
                    }
                }.getOrDefault("")
                com.nothing.island.ai.telephony.RawCallAction(
                    title = action.title?.toString().orEmpty(),
                    intent = action.actionIntent,
                    semanticAction = semantic,
                    iconHint = iconHint,
                )
            }
            val callTextExtra = extras?.getCharSequence(Notification.EXTRA_TEXT)?.toString()
            val callSubTextExtra = extras?.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()
            val matchedActions = CallNotificationParser.matchCallActions(
                actions = rawCallActions,
                callText = callTextExtra,
                callSubText = callSubTextExtra,
            )

            val callStyleAnswer: android.app.PendingIntent? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                extras?.getParcelable(Notification.EXTRA_ANSWER_INTENT) ?: matchedActions.answer
            } else {
                matchedActions.answer
            }

            val callStyleDecline: android.app.PendingIntent? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                extras?.getParcelable(Notification.EXTRA_DECLINE_INTENT) ?: matchedActions.decline
            } else {
                matchedActions.decline
            }

            val callStyleHangUp: android.app.PendingIntent? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                extras?.getParcelable(Notification.EXTRA_HANG_UP_INTENT) ?: matchedActions.hangUp
            } else {
                matchedActions.hangUp
            }

            val usesChronometer = (extras?.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER, false) == true) ||
                (extras?.getBoolean("android.usesChronometer", false) == true)
            val notificationWhenMs = sbn.notification.`when`
            val callInfo = CallNotificationParser.parse(
                CallSignal(
                    category = sbn.notification.category,
                    title = extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: callPersonName,
                    text = callTextExtra,
                    actionTitles = callActions.mapNotNull { it.title?.toString() },
                    showChronometer = usesChronometer,
                    subText = callSubTextExtra,
                    callType = callTypeExtra,
                    hasAnswerIntent = callStyleAnswer != null,
                    notificationWhenMs = notificationWhenMs,
                )
            )
            if (callInfo != null) {
                val controller = IslandOverlayController.getInstance(applicationContext)
                val callPrefs = controller.preferencesRepository.liveCallPreferences.value

                val callPhoto: Bitmap? = CallerPhotoResolver.resolve(
                    context = applicationContext,
                    callerDisplayName = callInfo.callerDisplayName,
                    phoneNumberOrUri = callPersonUri,
                    notificationExtras = extras,
                )
                activeCallPhoto = callPhoto

                val registeredActions = CallActionRegistry.CallActions(
                    key = key,
                    answer = if (callInfo.phase == CallPhase.INCOMING) callStyleAnswer else null,
                    decline = callStyleDecline,
                    hangUp = callStyleHangUp ?: callStyleDecline,
                    packageName = sbn.packageName,
                    speaker = matchedActions.speaker,
                    mute = matchedActions.mute,
                    hold = matchedActions.hold,
                    record = matchedActions.record,
                    contentIntent = sbn.notification.contentIntent,
                )
                CallActionRegistry.register(registeredActions)
                com.nothing.island.ai.telephony.CallController.syncFromNotification(
                    context = applicationContext,
                    speakerStateHint = matchedActions.speakerStateHint,
                    muteStateHint = matchedActions.muteStateHint,
                    holdStateHint = matchedActions.holdStateHint,
                    recordingStateHint = matchedActions.recordStateHint,
                )

                when (callInfo.phase) {
                    CallPhase.INCOMING -> {
                        val isDuplicateRinging = activeCallKey == key &&
                            activeCallPhase == CallPhase.INCOMING &&
                            activeCallCaller == callInfo.callerDisplayName
                        activeCallKey = key
                        activeCallPhase = CallPhase.INCOMING
                        activeCallCaller = callInfo.callerDisplayName
                        if (!isDuplicateRinging) {
                            controller.apply {
                                eventEngine.emit(
                                    IslandEvent.IncomingCallRinging(
                                        id = key,
                                        callId = key,
                                        callerDisplayName = callInfo.callerDisplayName,
                                        isVideoCall = callInfo.isVideoCall,
                                        contactPhoto = callPhoto,
                                    )
                                )
                                // Auto-expand incoming call capsule once per incoming call
                                expandCall()
                            }
                            Log.i(TAG, "R9 incoming call routed to island: ${callInfo.callerDisplayName}")
                        }
                    }

                    CallPhase.DIALING -> {
                        val isDuplicateDialing = activeCallKey == key &&
                            activeCallPhase == CallPhase.DIALING &&
                            activeCallCaller == callInfo.callerDisplayName
                        activeCallKey = key
                        activeCallPhase = CallPhase.DIALING
                        activeCallCaller = callInfo.callerDisplayName
                        if (!isDuplicateDialing) {
                            controller.eventEngine.emit(
                                IslandEvent.CallConnected(
                                    id = "call_dialing_$key",
                                    callId = key,
                                    callerDisplayName = callInfo.callerDisplayName,
                                    durationSeconds = 0L,
                                    isDialing = true,
                                    connectedAtMs = 0L,
                                    contactPhoto = callPhoto,
                                )
                            )
                            controller.startCallTicker(
                                callId = key,
                                caller = callInfo.callerDisplayName,
                                contactPhoto = callPhoto,
                                isDialing = true,
                                connectedAtMs = 0L,
                            )
                            if (callPrefs.autoExpandCall) {
                                controller.expandCall()
                            }
                            Log.i(TAG, "R9 call dialing/outgoing (usesChronometer=$usesChronometer): ${callInfo.callerDisplayName}")
                        }
                    }

                    CallPhase.ONGOING -> {
                        activeCallKey = key
                        activeCallPhase = CallPhase.ONGOING
                        activeCallCaller = callInfo.callerDisplayName
                        val active = controller.priorityManager?.activeEvent as? IslandEvent.CallConnected
                        val wasDialing = active?.isDialing == true
                        val connectedAtChanged = active != null && callInfo.connectedAtMs > 0L && active.connectedAtMs != callInfo.connectedAtMs
                        val isAlreadyConnected = active != null && !wasDialing && !connectedAtChanged
                        if (!isAlreadyConnected) {
                            val initialDurationSeconds = CallNotificationParser.computeCallDurationSeconds(
                                isDialing = false,
                                connectedAtMs = callInfo.connectedAtMs,
                                nowMs = System.currentTimeMillis(),
                            )
                            controller.eventEngine.emit(
                                IslandEvent.CallConnected(
                                    id = "call_connected_$key",
                                    callId = key,
                                    callerDisplayName = callInfo.callerDisplayName,
                                    durationSeconds = initialDurationSeconds,
                                    isDialing = false,
                                    connectedAtMs = callInfo.connectedAtMs,
                                    contactPhoto = callPhoto,
                                )
                            )
                            if (callPrefs.autoExpandCall) {
                                controller.expandCall()
                            }
                            controller.startCallTicker(
                                callId = key,
                                caller = callInfo.callerDisplayName,
                                contactPhoto = callPhoto,
                                isDialing = false,
                                connectedAtMs = callInfo.connectedAtMs,
                            )
                            Log.i(TAG, "R9 call connected (when=${callInfo.connectedAtMs}, initialSec=$initialDurationSeconds): ${callInfo.callerDisplayName}")
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

                // 4-5. Register actions & commit to history repository
                val actionGeneration = commitNotificationToHistory(
                    sbn = sbn,
                    key = key,
                    recordedAtMs = System.currentTimeMillis(),
                    sourcePostTimeMs = postTime,
                    ingestSequence = seq,
                    historyRepo = historyRepo,
                    prefs = prefs,
                    event = event
                ) ?: return@launch

                // 6. Emit presentation event ONLY after successful commit (and only if not historical)
                if (!isHistorical) {
                    // Island always presents the alert (slide-down popup).
                    val accepted = controller.eventEngine.emit(event)
                    Log.i(TAG, "Notification accepted=$accepted & emitted: id=${event.id}, pkg=${event.packageName}")

                    // "Hide System (Double) Notification": evaluate explicit suppression rules
                    // (excludes calls, media, non-clearable, ongoing, group summaries, and loops).
                    val notification = sbn.notification
                    val isGroupSummary = (notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0
                    val hasMediaStyle = notification.extras?.containsKey(Notification.EXTRA_MEDIA_SESSION) == true ||
                        notification.category == Notification.CATEGORY_TRANSPORT
                    val suppressionDecision = systemSuppressor.evaluate(
                        candidate = SuppressionCandidate(
                            key = key,
                            postTime = postTime,
                            category = notification.category,
                            isClearable = sbn.isClearable,
                            isOngoing = sbn.isOngoing ||
                                (notification.flags and Notification.FLAG_ONGOING_EVENT) != 0 ||
                                (notification.flags and Notification.FLAG_FOREGROUND_SERVICE) != 0,
                            isGroupSummary = isGroupSummary,
                            hasMediaStyle = hasMediaStyle,
                            acceptedByIsland = accepted
                        ),
                        preferences = prefs
                    )
                    if (suppressionDecision == SuppressionDecision.CANCEL) {
                        try {
                            systemSuppressor.recordSelfCancellation(key = key, postTime = postTime)
                            cancelNotification(key)
                            Log.i(TAG, "Hide System Notification: heads-up cancelled key=$key")
                        } catch (e: Exception) {
                            systemSuppressor.consumeSelfCancelledRemoval(key)
                            Log.w(TAG, "Failed to cancel redundant notification key=$key", e)
                        }
                    } else if (prefs.hideSystemNotification) {
                        Log.d(TAG, "Hide System Notification skipped ($suppressionDecision) for key=$key")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing incoming notification for key=$key", e)
            }
        }
    }

    override fun onNotificationRemoved(
        sbn: StatusBarNotification?,
        rankingMap: RankingMap?,
        reason: Int,
    ) {
        handleNotificationRemoved(sbn, reason)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        handleNotificationRemoved(sbn, removalReason = null)
    }

    private fun handleNotificationRemoved(sbn: StatusBarNotification?, removalReason: Int?) {
        if (sbn == null) return

        ingestionScope.launch {
            val seq = ingressSequence.incrementAndGet()
            val key = sbn.key ?: return@launch
            val postTime = sbn.postTime

            if (systemSuppressor.consumeSelfCancelledRemoval(key)) {
                Log.d(TAG, "Ignoring self-inflicted removal callback for key=$key (Hide System Notification)")
                return@launch
            }

            if (activeCallKey == key) {
                val shouldEnd = DialerHeadsUpPolicy.shouldEndCallOnNotificationRemoval(
                    removedKey = key,
                    activeCallKey = activeCallKey,
                    removalReason = removalReason,
                )
                if (shouldEnd) {
                    clearActiveCallAndEmitEnd(key, reason = "notification removed reason=$removalReason")
                }
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

                    commitNotificationToHistory(
                        sbn = sbn,
                        key = sbn.key,
                        recordedAtMs = sbn.postTime,
                        sourcePostTimeMs = sbn.postTime,
                        ingestSequence = ingressSequence.incrementAndGet(),
                        historyRepo = historyRepo,
                        prefs = prefs,
                        event = event
                    )
                }
            }
            Log.i(TAG, "Silently reconciled ${historicalNotificationKeys.size} historical active notifications.")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to query active notifications during silent reconciliation", e)
        }
    }

    private fun commitNotificationToHistory(
        sbn: StatusBarNotification,
        key: String,
        recordedAtMs: Long,
        sourcePostTimeMs: Long,
        ingestSequence: Long,
        historyRepo: NotificationHistoryRepository,
        prefs: NotificationPreferences,
        event: com.nothing.island.ai.engine.IslandEvent.NotificationPosted
    ): Long? {
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
            token = sourcePostTimeMs
        )

        val notification = sbn.notification
        val extras = notification.extras
        val rawTitle = extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim().orEmpty()
        val rawText = extractNotificationText(extras)
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
            postTime = recordedAtMs,
            sourcePostTime = sourcePostTimeMs,
            ingestSequence = ingestSequence,
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
            Log.w(TAG, "History commit rejected for key=$key. Rolling back action generation $actionGeneration")
            NotificationActionBridge.unregisterIfGenerationMatches(key, actionGeneration)
            return null
        }
        return actionGeneration
    }

    internal fun cleanupListenerState() {
        isConnected = false
        if (instance === this) {
            instance = null
        }
        _connectionState.value = false
        unregisterTelephonyCallStateMonitor()
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
