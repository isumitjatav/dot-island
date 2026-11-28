package com.nothing.island.ai

import android.accessibilityservice.AccessibilityService
import android.app.KeyguardManager
import android.app.Notification
import android.content.BroadcastReceiver
import android.content.ComponentCallbacks2
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.nothing.island.ai.battery.BatteryAutoExpansionPolicy
import com.nothing.island.ai.battery.BatteryMonitor
import com.nothing.island.ai.battery.BatteryPreferences
import com.nothing.island.ai.battery.BatteryState
import com.nothing.island.ai.data.IslandPreferences
import com.nothing.island.ai.data.IslandPreferencesRepository
import com.nothing.island.ai.engine.IslandEvent
import com.nothing.island.ai.engine.IslandEventEngine
import com.nothing.island.ai.engine.IslandMasterState
import com.nothing.island.ai.engine.IslandPriorityManager
import com.nothing.island.ai.engine.IslandStateRepository
import com.nothing.island.ai.media.AudioOutputDeviceTracker
import com.nothing.island.ai.media.MediaSessionTracker
import com.nothing.island.ai.media.MediaTrackState
import com.nothing.island.ai.media.MusicPreferences
import com.nothing.island.ai.notifications.IslandNotificationListenerService
import com.nothing.island.ai.telephony.CallActionRegistry
import com.nothing.island.ai.notifications.NotificationActionBridge
import com.nothing.island.ai.notifications.NotificationAutoExpansionPolicy
import com.nothing.island.ai.notifications.ReplyResult
import com.nothing.island.ai.notifications.NotificationHistoryRepository
import com.nothing.island.ai.notifications.NotificationHistoryRepositoryImpl
import com.nothing.island.ai.notifications.NotificationPreviewMode
import com.nothing.island.ai.notifications.PrivacySafeNotificationPresentation
import com.nothing.island.ai.switcher.IslandActivitySwitcher
import com.nothing.island.ai.switcher.IslandInterface
import com.nothing.island.ai.switcher.SwipeDirection
import com.nothing.island.ai.hardware.TorchCallbackHandler
import com.nothing.island.ai.hardware.NetworkCallbackHandler
import com.nothing.island.ai.gestures.RingerState
import com.nothing.island.ai.receiver.SystemEventsReceiver
import com.nothing.island.ai.telephony.CallController
import com.nothing.island.ai.gestures.GesturePreferences
import com.nothing.island.ai.gamemode.GameModeDetector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * IslandOverlayController
 *
 * Centralized singleton controller managing the floating WindowManager overlay lifecycle,
 * ComposeView mounting, touch flags, dynamic sizing between collapsed and expanded modes,
 * and single-owner arbitration between AccessibilityService and legacy Service.
 */
class IslandOverlayController private constructor(private val appContext: Context) {

    companion object {
        private const val TAG = "IslandOverlayController"

        @Volatile
        private var instance: IslandOverlayController? = null

        fun getInstance(context: Context): IslandOverlayController {
            return instance ?: synchronized(this) {
                instance ?: IslandOverlayController(context.applicationContext).also { instance = it }
            }
        }

        @Volatile
        var isOverlayActive: Boolean = false
            private set

        /**
         * Returns true while the incoming-call card is actively ringing so no automatic collapse
         * timer can hide it over the heads-up phone banner underneath.
         */
        fun shouldBlockAutoCollapse(state: IslandMasterState): Boolean =
            state.mode == IslandMode.EXPANDED &&
                state.expandedViewType == ExpandedViewType.CALL &&
                state.activeEvent is IslandEvent.IncomingCallRinging
    }

    /**
     * Sprint 2: gates momentary system-hardware capsules against persisted preferences.
     * Unknown event domains are allowed through unchanged.
     */
    private fun shouldEmitSystemEvent(event: IslandEvent): Boolean {
        val prefs = preferencesRepository.liveSystemEventsPreferences.value
        if (!prefs.systemEventsEnabled) return false
        return when (event) {
            is IslandEvent.RingerModeChanged -> prefs.ringerCapsuleEnabled && when (event.ringerMode) {
                RingerState.SILENT, RingerState.VIBRATE -> prefs.silentModeCapsuleEnabled
                RingerState.NORMAL -> prefs.ringModeCapsuleEnabled
            }
            is IslandEvent.DeviceUnlocked -> prefs.deviceUnlockCapsuleEnabled
            is IslandEvent.TorchStateChanged -> prefs.flashlightCapsuleEnabled
            is IslandEvent.WifiStateChanged -> prefs.wifiCapsuleEnabled
            is IslandEvent.DndStateChanged -> prefs.zenModeCapsuleEnabled
            is IslandEvent.BatteryThresholdWarning -> prefs.batteryWarningCapsuleEnabled
            is IslandEvent.BluetoothDeviceConnected,
            is IslandEvent.BluetoothDeviceDisconnected -> prefs.bluetoothCapsuleEnabled
            else -> true
        }
    }

    private val mainHandler = Handler(Looper.getMainLooper())

    private var activeWindowManager: WindowManager? = null
    private var activeContext: Context? = null
    private var composeView: ComposeView? = null
    private var lifecycleOwner: OverlayLifecycleOwner? = null
    private var layoutParams: WindowManager.LayoutParams? = null

    val pillBoundsRect = android.graphics.Rect()
    val mascotBoundsRect = android.graphics.Rect()

    fun updatePillBounds(left: Int, top: Int, right: Int, bottom: Int) {
        if (pillBoundsRect.left != left || pillBoundsRect.top != top ||
            pillBoundsRect.right != right || pillBoundsRect.bottom != bottom) {
            pillBoundsRect.set(left, top, right, bottom)
            composeView?.let { v ->
                if (v.isAttachedToWindow) {
                    try {
                        v.viewTreeObserver.dispatchOnGlobalLayout()
                    } catch (_: Exception) {}
                }
            }
        }
    }

    fun updateMascotBounds(left: Int, top: Int, right: Int, bottom: Int) {
        if (mascotBoundsRect.left != left || mascotBoundsRect.top != top ||
            mascotBoundsRect.right != right || mascotBoundsRect.bottom != bottom) {
            mascotBoundsRect.set(left, top, right, bottom)
            composeView?.let { v ->
                if (v.isAttachedToWindow) {
                    try {
                        v.viewTreeObserver.dispatchOnGlobalLayout()
                    } catch (_: Exception) {}
                }
            }
        }
    }

    private var insetsListenerProxy: Any? = null
    private var removeInsetsListenerMethod: java.lang.reflect.Method? = null

    private fun setupTouchPassthrough(view: View) {
        view.setOnTouchListener { _, ev ->
            val isExpanded = repository.state.value.mode == IslandMode.EXPANDED
            if (isExpanded) {
                if (ev.actionMasked == MotionEvent.ACTION_DOWN) {
                    val x = ev.x.toInt()
                    val y = ev.y.toInt()
                    if (!pillBoundsRect.contains(x, y)) {
                        Log.d("IslandTouch", "Touch outside pill bounds ($x, $y) while expanded -> collapsing")
                        collapse()
                        return@setOnTouchListener true
                    }
                } else if (ev.actionMasked == MotionEvent.ACTION_OUTSIDE) {
                    Log.d("IslandTouch", "ACTION_OUTSIDE detected while expanded -> collapsing")
                    collapse()
                    return@setOnTouchListener true
                }
                false
            } else {
                false
            }
        }

        try {
            val listenerClass = Class.forName("android.view.ViewTreeObserver\$OnComputeInternalInsetsListener")
            val insetsInfoClass = Class.forName("android.view.ViewTreeObserver\$InternalInsetsInfo")
            val setTouchableInsetsMethod = insetsInfoClass.getMethod("setTouchableInsets", Int::class.javaPrimitiveType)
            val touchableRegionField = insetsInfoClass.getField("touchableRegion")
            val TOUCHABLE_INSETS_REGION = 3
            val TOUCHABLE_INSETS_FRAME = 0

            val vtoClass = android.view.ViewTreeObserver::class.java
            val addListenerMethod = vtoClass.getMethod("addOnComputeInternalInsetsListener", listenerClass)
            removeInsetsListenerMethod = vtoClass.getMethod("removeOnComputeInternalInsetsListener", listenerClass)

            val invocationHandler = java.lang.reflect.InvocationHandler { _, method, args ->
                if (method.name == "onComputeInternalInsets" && args != null && args.isNotEmpty()) {
                    val insetsInfo = args[0]
                    try {
                        val isExpanded = repository.state.value.mode == IslandMode.EXPANDED
                        if (isExpanded) {
                            setTouchableInsetsMethod.invoke(insetsInfo, TOUCHABLE_INSETS_FRAME)
                        } else {
                            setTouchableInsetsMethod.invoke(insetsInfo, TOUCHABLE_INSETS_REGION)
                            val region = touchableRegionField.get(insetsInfo) as? android.graphics.Region
                            if (region != null) {
                                if (pillBoundsRect.isEmpty && mascotBoundsRect.isEmpty) {
                                    region.setEmpty()
                                } else {
                                    region.set(pillBoundsRect)
                                    if (!mascotBoundsRect.isEmpty) {
                                        region.op(mascotBoundsRect, android.graphics.Region.Op.UNION)
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Error in onComputeInternalInsets", e)
                    }
                }
                null
            }

            val proxy = java.lang.reflect.Proxy.newProxyInstance(
                listenerClass.classLoader,
                arrayOf(listenerClass),
                invocationHandler
            )
            insetsListenerProxy = proxy
            addListenerMethod.invoke(view.viewTreeObserver, proxy)
            Log.d(TAG, "OnComputeInternalInsetsListener installed successfully")
        } catch (e: Throwable) {
            Log.w(TAG, "OnComputeInternalInsetsListener reflection setup: ${e.message}")
        }
    }

    val geometry = CutoutDetector.detectCutout(appContext)
    val preferencesRepository = IslandPreferencesRepository.getInstance(appContext)

    val historyRepository: NotificationHistoryRepository = NotificationHistoryRepositoryImpl()

    private val keyguardManager = appContext.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
    val isKeyguardLockedFlow = MutableStateFlow(keyguardManager?.isKeyguardLocked ?: false)

    private val keyguardReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val locked = keyguardManager?.isKeyguardLocked ?: (intent?.action == Intent.ACTION_SCREEN_OFF)
            Log.d(TAG, "Keyguard receiver: action=${intent?.action}, locked=$locked")
            isKeyguardLockedFlow.value = locked
            if (locked) {
                handleKeyguardLocked()
            }
        }
    }
    private var isKeyguardReceiverRegistered = false

    private val controllerLifecycleScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /**
     * Privacy-safe notification history projection reactively updating upon keyguard lock transitions
     * or preference changes. Never exposes raw unmasked data to Compose UI.
     */
    val projectedHistory: StateFlow<List<PrivacySafeNotificationPresentation>> = combine(
        historyRepository.history,
        isKeyguardLockedFlow,
        preferencesRepository.liveNotificationPreferences
    ) { rawHistory, isLocked, notifPrefs ->
        rawHistory.mapNotNull { entry ->
            entry.toPrivacySafePresentation(isKeyguardLocked = isLocked, preferences = notifPrefs)
        }
    }.stateIn(controllerLifecycleScope, SharingStarted.Eagerly, emptyList())

    val repository = IslandStateRepository(
        IslandMasterState(
            statusText = "${Build.MODEL} (${Build.DEVICE})"
        )
    )

    val eventEngine = IslandEventEngine()

    /** R9 live call duration: ticks once per second while a call is active. */
    private var callTickerJob: Job? = null

    fun startCallTicker(
        callId: String,
        caller: String,
        contactPhoto: android.graphics.Bitmap? = null,
        isDialing: Boolean = false,
        connectedAtMs: Long = 0L,
    ) {
        callTickerJob?.cancel()
        val nowWallMs = System.currentTimeMillis()
        val initialAgeMs = if (!isDialing && connectedAtMs > 0L && nowWallMs >= connectedAtMs) {
            nowWallMs - connectedAtMs
        } else {
            0L
        }
        val startedAt = SystemClock.elapsedRealtime() - initialAgeMs
        callTickerJob = controllerLifecycleScope.launch {
            var tickCount = 0L
            while (isActive) {
                delay(1000L)
                tickCount++
                val seconds = if (isDialing) {
                    0L
                } else {
                    ((SystemClock.elapsedRealtime() - startedAt) / 1000L).coerceAtLeast(0L)
                }
                // Every 2 seconds, verify via AudioManager.mode or active dialer notifications
                // that a call is still active so an orphaned call timer can never get stuck.
                if (tickCount >= 2L && tickCount % 2L == 0L) {
                    val audioMode = runCatching {
                        (appContext.getSystemService(Context.AUDIO_SERVICE) as? android.media.AudioManager)?.mode
                    }.getOrNull() ?: android.media.AudioManager.MODE_NORMAL
                    val hasDialerNotif = com.nothing.island.ai.notifications.IslandNotificationListenerService
                        .hasActiveDialerCallNotification()
                    val isSyntheticDebugCall = callId.startsWith("debug_")
                    if (com.nothing.island.ai.telephony.DialerHeadsUpPolicy.shouldAutoEndStaleCall(
                            audioMode = audioMode,
                            hasActiveDialerNotification = hasDialerNotif,
                            isDebugSyntheticCall = isSyntheticDebugCall,
                        )
                    ) {
                        Log.w(TAG, "Stale call detected by watchdog (audioMode=$audioMode, hasDialerNotif=$hasDialerNotif, callId=$callId) -> ending call")
                        com.nothing.island.ai.notifications.IslandNotificationListenerService
                            .clearActiveCallKeyIfMatches(callId)
                        CallActionRegistry.clear(callId)
                        CallController.resetCallStates()
                        eventEngine.emit(IslandEvent.CallEnded(id = "call_watchdog_end_$callId", callId = callId))
                        collapse()
                        callTickerJob = null
                        return@launch
                    }
                }
                val latestPhoto = contactPhoto ?: when (val active = priorityManager?.activeEvent) {
                    is IslandEvent.CallConnected -> active.contactPhoto
                    is IslandEvent.IncomingCallRinging -> active.contactPhoto
                    else -> null
                }
                eventEngine.emit(
                    IslandEvent.CallConnected(
                        id = if (isDialing) "call_dialing_$callId" else "call_tick_$callId",
                        callId = callId,
                        callerDisplayName = caller,
                        durationSeconds = seconds,
                        isDialing = isDialing,
                        connectedAtMs = if (isDialing) 0L else connectedAtMs,
                        contactPhoto = latestPhoto,
                    )
                )
            }
        }
    }

    fun stopCallTicker() {
        callTickerJob?.cancel()
        callTickerJob = null
    }

    /** Adjusts media volume from the quick controls drawer. */
    fun adjustQuickVolume(delta: Int) {
        try {
            val audio = appContext.getSystemService(Context.AUDIO_SERVICE) as? android.media.AudioManager
            audio?.adjustStreamVolume(
                android.media.AudioManager.STREAM_MUSIC,
                if (delta > 0) android.media.AudioManager.ADJUST_RAISE else android.media.AudioManager.ADJUST_LOWER,
                0,
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed to adjust volume", e)
        }
    }

    /** Gets media volume percentage (0.0f to 1.0f). */
    fun getMediaVolumePercent(): Float {
        return try {
            val audio = appContext.getSystemService(Context.AUDIO_SERVICE) as? android.media.AudioManager ?: return 0.5f
            val max = audio.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC).toFloat()
            val current = audio.getStreamVolume(android.media.AudioManager.STREAM_MUSIC).toFloat()
            if (max > 0f) (current / max).coerceIn(0f, 1f) else 0.5f
        } catch (e: Exception) {
            0.5f
        }
    }

    /** Sets media volume percentage (0.0f to 1.0f). */
    fun setMediaVolumePercent(percent: Float) {
        try {
            val audio = appContext.getSystemService(Context.AUDIO_SERVICE) as? android.media.AudioManager ?: return
            val max = audio.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC)
            val target = kotlin.math.round(percent * max).toInt().coerceIn(0, max)
            audio.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, target, 0)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to set media volume", e)
        }
    }

    /** Gets screen brightness percentage (0.0f to 1.0f). */
    fun getScreenBrightnessPercent(): Float {
        return try {
            val value = android.provider.Settings.System.getInt(
                appContext.contentResolver,
                android.provider.Settings.System.SCREEN_BRIGHTNESS
            )
            (value / 255f).coerceIn(0f, 1f)
        } catch (e: Exception) {
            0.5f
        }
    }

    /** Sets screen brightness percentage (0.0f to 1.0f). Writes to Settings.System; never touches params.screenBrightness. */
    fun setScreenBrightnessPercent(percent: Float) {
        val clamped = percent.coerceIn(0.01f, 1f)
        try {
            if (android.provider.Settings.System.canWrite(appContext)) {
                android.provider.Settings.System.putInt(
                    appContext.contentResolver,
                    android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE,
                    android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
                )
                val intVal = kotlin.math.round(clamped * 255f).toInt().coerceIn(1, 255)
                android.provider.Settings.System.putInt(
                    appContext.contentResolver,
                    android.provider.Settings.System.SCREEN_BRIGHTNESS,
                    intVal
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to write system brightness", e)
        }
    }

    /** Opens the system call history from quick controls. */
    fun openQuickHistory() {
        try {
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW)
            intent.type = "vnd.android.cursor.dir/calls"
            intent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
            appContext.startActivity(intent)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to open call history", e)
        }
        collapse()
    }

    private val _lastScreenshotResult = MutableStateFlow<com.nothing.island.ai.gestures.ScreenshotActionResult?>(null)
    val lastScreenshotResult: StateFlow<com.nothing.island.ai.gestures.ScreenshotActionResult?> = _lastScreenshotResult.asStateFlow()

    fun reportScreenshotResult(result: com.nothing.island.ai.gestures.ScreenshotActionResult) {
        _lastScreenshotResult.value = result
    }

    val quickToolExecutor: com.nothing.island.ai.ui.components.QuickToolExecutor by lazy {
        com.nothing.island.ai.ui.components.QuickToolExecutor(
            bridge = com.nothing.island.ai.ui.components.AndroidQuickToolPlatformBridge(appContext, torchHandler),
            onExpandMusic = { expandMusic() },
            onCollapse = { collapse() }
        )
    }

    /** Handles a quick-tools button press via [QuickToolExecutor] with debounce and Settings fallback. */
    fun handleQuickTool(id: String): com.nothing.island.ai.ui.components.QuickToolExecutionResult {
        Log.d(TAG, "QuickTool: $id")
        return quickToolExecutor.execute(id)
    }

    val bluetoothEarbudTracker: com.nothing.island.ai.bluetooth.BluetoothEarbudTracker by lazy {
        com.nothing.island.ai.bluetooth.BluetoothEarbudTracker(
            bridge = com.nothing.island.ai.bluetooth.AndroidBluetoothPlatformBridge(appContext),
            onEvent = { event ->
                if (shouldEmitSystemEvent(event)) eventEngine.emit(event)
            }
        )
    }

    val audioOutputTracker = AudioOutputDeviceTracker(appContext)
    val mediaTracker: MediaSessionTracker = MediaSessionTracker.getInstance(appContext, eventEngine, audioOutputTracker)
    val batteryMonitor: BatteryMonitor = BatteryMonitor.getInstance(appContext, eventEngine, preferencesRepository)
    val activitySwitcher = IslandActivitySwitcher()
    val stateManager: IslandStateManager = IslandStateManager.getInstance()
    val gameModeDetector = GameModeDetector(appContext)

    // R9: InCallService for native call handling (Dialer Role)
    private var islandInCallService: com.nothing.island.ai.telephony.IslandInCallService? = null

    fun getInCallService(): com.nothing.island.ai.telephony.IslandInCallService? = islandInCallService

    fun setInCallService(service: com.nothing.island.ai.telephony.IslandInCallService) {
        islandInCallService = service
        islandInCallService?.setEventEngine(eventEngine)
        Log.d(TAG, "IslandInCallService registered with EventEngine")
    }

    fun clearInCallService() {
        islandInCallService = null
        Log.d(TAG, "IslandInCallService cleared")
    }

    var currentForegroundPackage: String? = null
        private set

    var currentOrientation: Int = appContext.resources.configuration.orientation
        private set

    var isHiddenByGameMode: Boolean = false
        private set

    @Volatile
    var isHiddenBySystemShade: Boolean = false
        private set

    private val isOverlayHidden: Boolean
        get() = isHiddenByGameMode || isHiddenBySystemShade

    private val componentCallbacks = object : ComponentCallbacks2 {
        override fun onConfigurationChanged(newConfig: Configuration) {
            this@IslandOverlayController.onConfigurationChanged(newConfig)
        }
        override fun onLowMemory() {}
        override fun onTrimMemory(level: Int) {}
    }

    init {
        try {
            appContext.registerComponentCallbacks(componentCallbacks)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to register component callbacks on appContext", e)
        }
    }

    val torchHandler = TorchCallbackHandler(appContext) { event ->
        if (shouldEmitSystemEvent(event)) eventEngine.emit(event)
    }
    val networkHandler = NetworkCallbackHandler(appContext) { event ->
        if (shouldEmitSystemEvent(event)) eventEngine.emit(event)
    }
    private var systemEventsReceiver: SystemEventsReceiver? = null

    private var autoExpandJob: Job? = null
    private var batteryAutoExpandJob: Job? = null
    private var previousOverlayMode: IslandMode? = null

    /**
     * R7.1: presentation identity of the notification that most recently triggered an
     * auto-expansion. The priority manager keeps this identity stable across in-place
     * updates to the same notification, which is how duplicate/progress-tick suppression works.
     */
    @Volatile
    private var lastAutoExpandedPresentationId: String? = null

    @Volatile
    private var lastAutoExpandedBatteryPresentationId: String? = null

    /**
     * R7.1: true while the user is actively engaged with the expanded notification view
     * (inline-reply IME open). Blocks both expansion and premature auto-collapse.
     */
    @Volatile
    private var isUserInteractingWithNotification: Boolean = false

    private var controllerScope: CoroutineScope? = null
    var priorityManager: IslandPriorityManager? = null
        private set

    val state: IslandState
        get() = repository.state.value.toLegacyIslandState()

    var isAttached: Boolean = false
        private set

    var isAccessibilityMode: Boolean = false
        private set

    private fun handleKeyguardLocked() {
        val current = repository.state.value
        if (current.mode == IslandMode.EXPANDED && current.expandedViewType == ExpandedViewType.NOTIFICATION_FOCUSED) {
            val selectedKey = current.selectedNotificationKey
            if (selectedKey != null) {
                val entry = historyRepository.getEntry(selectedKey)
                if (entry != null && entry.visibility == Notification.VISIBILITY_SECRET) {
                    Log.i(TAG, "Keyguard locked while viewing VISIBILITY_SECRET notification $selectedKey. Falling back to dashboard.")
                    mainHandler.post { repository.expandDashboard() }
                }
            } else {
                val activeEvent = current.activeEvent
                if (activeEvent is IslandEvent.NotificationPosted) {
                    val entry = historyRepository.getEntry(activeEvent.id)
                    if (entry != null && entry.visibility == Notification.VISIBILITY_SECRET) {
                        Log.i(TAG, "Keyguard locked while active event is VISIBILITY_SECRET. Falling back to dashboard.")
                        mainHandler.post { repository.expandDashboard() }
                    }
                }
            }
        }
    }

    private fun registerKeyguardReceiver() {
        if (!isKeyguardReceiverRegistered) {
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_USER_PRESENT)
            }
            try {
                appContext.registerReceiver(keyguardReceiver, filter)
                isKeyguardReceiverRegistered = true
                Log.d(TAG, "Keyguard receiver registered successfully.")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to register keyguard receiver", e)
            }
        }
    }

    private fun unregisterKeyguardReceiver() {
        if (isKeyguardReceiverRegistered) {
            try {
                appContext.unregisterReceiver(keyguardReceiver)
            } catch (_: Exception) {}
            isKeyguardReceiverRegistered = false
            Log.d(TAG, "Keyguard receiver unregistered.")
        }
    }

    /**
     * Mounts the overlay view onto the WindowManager with TYPE_ACCESSIBILITY_OVERLAY (2032).
     * Sits at AOSP Base Layer 24000, allowing direct touch reception across the camera cutout.
     */
    fun attachAccessibilityOverlay(service: AccessibilityService) {
        mainHandler.post {
            if (isAttached) {
                Log.w(TAG, "Overlay is already attached. Detaching existing window before attaching accessibility overlay.")
                detachInternal()
            }

            try {
                // Dual-pill is a transient preview state; never persist it across attaches.
                repository.setDualPill(false)
                activeContext = service
                val wm = service.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                activeWindowManager = wm
                isAccessibilityMode = true

                val currentPrefs = preferencesRepository.livePreferences.value
                val screenWidthPx = service.resources.displayMetrics.widthPixels
                // True capsule height (108px @ 36dp) — no 22dp touch bridge needed at Layer 24000
                val compactHeightPx = CutoutDetector.dpToPx(currentPrefs.compactHeightDp, service)
                val topMarginPx = CutoutDetector.dpToPx(currentPrefs.topMarginDp, service)

                // Reference flags: FLAG_NOT_FOCUSABLE (8) | FLAG_NOT_TOUCH_MODAL (32) | FLAG_LAYOUT_NO_LIMITS (512) = 552
                val flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS

                val params = WindowManager.LayoutParams(
                    screenWidthPx,
                    compactHeightPx,
                    WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                    flags,
                    PixelFormat.TRANSLUCENT
                ).apply {
                    gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                    x = 0
                    y = topMarginPx
                    screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
                        fitInsetsTypes = 0
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        @Suppress("DEPRECATION")
                        layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                    }
                }

                val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
                controllerScope = scope

                val pm = IslandPriorityManager(scope) { resolvedEvent, priority, presentationId ->
                    repository.updateFromResolvedEvent(resolvedEvent, priority, presentationId)
                }
                priorityManager = pm

                scope.launch {
                    eventEngine.events.collect { event ->
                        pm.processEvent(event)
                    }
                }

                // Reactive WindowManager resizing observing StateFlow master state, calibration preferences, and media track
                scope.launch {
                    combine(
                        repository.state,
                        preferencesRepository.livePreferences,
                        mediaTracker.currentTrack
                    ) { masterState, prefs, _ ->
                        Pair(masterState, prefs)
                    }.collect { (masterState, prefs) ->
                        mainHandler.post {
                            updateWindowLayout(masterState, prefs)
                        }
                    }
                }

                // R7.1 Notification-triggered auto-expansion observer (independent of music)
                launchNotificationAutoExpansionObserver(scope, pm)

                // R8 Battery & Charging auto-expansion observer
                launchBatteryAutoExpansionObserver(scope, pm)
                batteryMonitor.startMonitoring()

                // Sprint 2 Batch 1: Hardware callbacks & System events
                torchHandler.start()
                networkHandler.start()
                bluetoothEarbudTracker.start()
                if (systemEventsReceiver == null) {
                    systemEventsReceiver = SystemEventsReceiver { event ->
                        if (shouldEmitSystemEvent(event)) eventEngine.emit(event)
                    }
                    val filter = SystemEventsReceiver.createIntentFilter()
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        appContext.registerReceiver(systemEventsReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
                    } else {
                        appContext.registerReceiver(systemEventsReceiver, filter)
                    }
                }

                // Smart Music Auto-expansion observer
                scope.launch {
                    var previousTrackKey: String? = null
                    mediaTracker.currentTrack.collect { track ->
                        val musicPrefs = preferencesRepository.liveMusicPreferences.value
                        val trackKey = "${track.packageName}_${track.trackTitle}"
                        val isNewTrack = track.isPlaying && trackKey != previousTrackKey && track.trackTitle.isNotBlank() && track.trackTitle != "Audio Track"
                        if (track.isPlaying) {
                            previousTrackKey = trackKey
                        }
                        if (isNewTrack && musicPrefs.isAutoExpandEnabled && repository.state.value.mode == IslandMode.COLLAPSED) {
                            Log.d(TAG, "Auto-expanding music for new track: ${track.trackTitle}")
                            expandMusic() // schedules its own auto-collapse
                        }
                    }
                }

                // Synchronize pulse preference with repository state
                scope.launch {
                    preferencesRepository.livePreferences
                        .map { it.isPulsing }
                        .distinctUntilChanged()
                        .collect { isPulsing ->
                            repository.setPulse(isPulsing)
                        }
                }

                // Game mode auto-hide preference observer
                scope.launch {
                    preferencesRepository.livePreferences
                        .map { it.autoHideInGames }
                        .distinctUntilChanged()
                        .collect {
                            evaluateGameMode()
                        }
                }

                // Foreground app polling fallback (complements AccessibilityService)
                scope.launch {
                    val usm = appContext.getSystemService(Context.USAGE_STATS_SERVICE) as? android.app.usage.UsageStatsManager
                    while (isActive) {
                        delay(2000L)
                        try {
                            if (preferencesRepository.livePreferences.value.autoHideInGames && usm != null) {
                                val time = System.currentTimeMillis()
                                val events = usm.queryEvents(time - 5000L, time)
                                var lastPkg: String? = null
                                if (events != null) {
                                    val event = android.app.usage.UsageEvents.Event()
                                    while (events.hasNextEvent()) {
                                        events.getNextEvent(event)
                                        if (event.eventType == android.app.usage.UsageEvents.Event.ACTIVITY_RESUMED) {
                                            lastPkg = event.packageName
                                        }
                                    }
                                }
                                if (lastPkg != null && lastPkg != currentForegroundPackage) {
                                    onForegroundPackageChanged(lastPkg)
                                }
                            }
                        } catch (_: Exception) {}
                    }
                }

                // Immediately dismiss active notification presentation if preview mode is switched to HIDDEN
                scope.launch {
                    preferencesRepository.notificationPreferencesFlow
                        .map { it.previewMode }
                        .distinctUntilChanged()
                        .collect { mode ->
                            if (mode == NotificationPreviewMode.HIDDEN) {
                                val current = pm.activeEvent
                                if (current is IslandEvent.NotificationPosted) {
                                    Log.i(TAG, "Notification preview mode switched to HIDDEN; dismissing active presentation.")
                                    pm.processEvent(IslandEvent.UserDismissCurrent(id = "dismiss_mode_hidden"))
                                }
                                Log.i(TAG, "Notification preview mode switched to HIDDEN; clearing history repository and actions.")
                                historyRepository.clearAll()
                                NotificationActionBridge.clearAll()
                                repository.collapse()
                            }
                        }
                }

                registerKeyguardReceiver()

                val owner = OverlayLifecycleOwner()
                val view = ComposeView(service).apply {
                    setOnTouchListener { _, ev ->
                        if (ev.actionMasked == MotionEvent.ACTION_OUTSIDE) {
                            Log.d("IslandTouch", "AccessibilityOverlay: ACTION_OUTSIDE detected -> collapsing overlay to pill")
                            collapse()
                            true
                        } else {
                            false
                        }
                    }
                    setViewTreeLifecycleOwner(owner)
                    setViewTreeSavedStateRegistryOwner(owner)
                    setViewTreeViewModelStoreOwner(owner)
                    setContent {
                        val masterState by repository.state.collectAsState()
                        val prefs by preferencesRepository.livePreferences.collectAsState()
                        val notifPrefs by preferencesRepository.liveNotificationPreferences.collectAsState()
                        val musicPrefs by preferencesRepository.liveMusicPreferences.collectAsState()
                        val batteryPrefs by preferencesRepository.liveBatteryPreferences.collectAsState()
                        val gesturePrefs by preferencesRepository.liveGesturePreferences.collectAsState()
                        val batteryState by batteryMonitor.batteryState.collectAsState()
                        val currentTrack by mediaTracker.currentTrack.collectAsState()
                        val resumableMedia by mediaTracker.lastResumableMedia.collectAsState()
                        val history by projectedHistory.collectAsState()
                        val undoState by historyRepository.pendingUndoFlow.collectAsState()
                        val activeInterface by activitySwitcher.currentInterface.collectAsState()
                        val signatureTheme = remember(prefs.presetName) {
                            com.nothing.island.ai.ui.theme.IslandSignatureTheme.resolve(prefs.presetName)
                        }

                        androidx.compose.runtime.CompositionLocalProvider(
                            com.nothing.island.ai.ui.theme.LocalIslandTheme provides signatureTheme
                        ) {
                            IslandOverlayView(
                            state = masterState.toLegacyIslandState(),
                            geometry = geometry,
                            preferences = prefs,
                            notificationPreferences = notifPrefs,
                            musicPreferences = musicPrefs,
                            batteryPreferences = batteryPrefs,
                            batteryState = batteryState,
                            currentTrack = currentTrack,
                            resumableMedia = resumableMedia,
                            activeEvent = masterState.activeEvent,
                            isDualPill = masterState.isDualPill,
                            projectedHistory = history,
                            selectedNotificationKey = masterState.selectedNotificationKey,
                            activeUndo = undoState,
                            activitySwitcherInterface = activeInterface,
                            onSelectHistoricalNotification = { key ->
                                selectHistoricalNotification(key)
                            },
                            onDismissHistoryEntry = { key ->
                                dismissHistoryEntry(key)
                            },
                            onUndoDismiss = {
                                undoDismiss()
                            },
                            onClearHistory = {
                                clearLocalHistory()
                            },
                            onToggleExpand = {
                                Log.d("IslandTouch", "onToggleExpand lambda invoked")
                                toggleExpand()
                            },
                            onExpandDashboard = {
                                Log.d("IslandTouch", "onExpandDashboard lambda invoked")
                                expandDashboard()
                            },
                            onExpandNotificationFocused = {
                                Log.d("IslandTouch", "onExpandNotificationFocused lambda invoked")
                                expandNotificationFocused()
                            },
                            onExpandMusic = {
                                Log.d("IslandTouch", "onExpandMusic lambda invoked")
                                expandMusic()
                            },
                            onExpandBattery = {
                                Log.d("IslandTouch", "onExpandBattery lambda invoked")
                                expandBattery()
                            },
                            onExpandCall = {
                                Log.d("IslandTouch", "onExpandCall lambda invoked")
                                expandCall()
                            },
                            onAnswerCall = {
                                Log.d("IslandTouch", "onAnswerCall lambda invoked")
                                if (CallController.answer(appContext)) {
                                    val ringing = priorityManager?.activeEvent as? IslandEvent.IncomingCallRinging
                                    val callId = ringing?.callId ?: "active_call"
                                    val caller = ringing?.callerDisplayName ?: "Call"
                                    val photo = ringing?.contactPhoto
                                    eventEngine.emit(
                                        IslandEvent.CallConnected(
                                            id = "call_connected_$callId",
                                            callId = callId,
                                            callerDisplayName = caller,
                                            contactPhoto = photo,
                                        )
                                    )
                                    startCallTicker(callId, caller, photo)
                                }
                            },
                            onQuickTool = { tool -> handleQuickTool(tool) },
                            onQuickVolume = { delta -> adjustQuickVolume(delta) },
                            onQuickVolumePercent = { percent -> setMediaVolumePercent(percent) },
                            onQuickBrightnessPercent = { percent -> setScreenBrightnessPercent(percent) },
                            getQuickVolumePercent = { getMediaVolumePercent() },
                            getQuickBrightnessPercent = { getScreenBrightnessPercent() },
                            onQuickHistory = { openQuickHistory() },
                            onDeclineCall = {
                                Log.d("IslandTouch", "onDeclineCall lambda invoked")
                                val currentCall = priorityManager?.activeEvent
                                val callId = when (currentCall) {
                                    is IslandEvent.CallConnected -> currentCall.callId
                                    is IslandEvent.IncomingCallRinging -> currentCall.callId
                                    else -> "active_call"
                                }
                                CallController.decline(appContext)
                                CallController.resetCallStates()
                                stopCallTicker()
                                eventEngine.emit(IslandEvent.CallEnded(id = "call_end_$callId", callId = callId))
                                collapse()
                            },
                            onCollapse = {
                                Log.d("IslandTouch", "onCollapse lambda invoked")
                                collapse()
                            },
                            onTogglePulse = {
                                Log.d("IslandTouch", "onTogglePulse lambda invoked")
                                togglePulse()
                            },
                            onOpenContent = { id ->
                                openNotificationContent(id)
                            },
                            onDismissEvent = { id ->
                                dismissNotification(id)
                            },
                            onExecuteAction = { id, index ->
                                executeNotificationAction(id, index)
                            },
                            onSendReply = { key, index, text ->
                                sendNotificationReply(key, index, text)
                            },
                            onInputFocusChanged = { enabled ->
                                setInputFocusMode(enabled)
                            },
                            onPlayPause = {
                                playPauseMedia()
                            },
                            onSkipNext = {
                                skipNextMedia()
                            },
                            onSkipPrevious = {
                                skipPrevMedia()
                            },
                            onSeekTo = { pos ->
                                seekMediaTo(pos)
                            },
                            onToggleRepeat = { mode ->
                                setMediaRepeatMode(mode)
                            },
                            onOpenMediaApp = {
                                openMediaApp()
                            },
                            onResumeLastMedia = {
                                resumeLastMedia()
                            },
                            onSwipeActivity = { direction ->
                                when (direction) {
                                    com.nothing.island.ai.switcher.SwipeDirection.DOWN -> repository.expandQuickTools()
                                    com.nothing.island.ai.switcher.SwipeDirection.UP -> collapse()
                                    com.nothing.island.ai.switcher.SwipeDirection.LEFT,
                                    com.nothing.island.ai.switcher.SwipeDirection.RIGHT -> handleActivitySwipe(direction)
                                }
                            },
                            gesturePreferences = gesturePrefs,
                            stateManager = stateManager,
                            onScreenshotResult = { res -> reportScreenshotResult(res) },
                            onPillBoundsChanged = { l, t, r, b -> updatePillBounds(l, t, r, b) },
                            onMascotBoundsChanged = { l, t, r, b -> updateMascotBounds(l, t, r, b) }
                        )
                        }
                    }
                }

                setupTouchPassthrough(view)
                owner.attach()
                wm.addView(view, params)

                this.lifecycleOwner = owner
                this.composeView = view
                this.layoutParams = params
                this.isAttached = true
                isOverlayActive = true

                if (isHiddenByGameMode) {
                    view.visibility = View.GONE
                    params.flags = params.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                    try { wm.updateViewLayout(view, params) } catch (_: Exception) {}
                }

                Log.i(TAG, "Nothing Island Accessibility Overlay attached successfully at Layer 24000 (y=$topMarginPx px, 124dp x 36dp).")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to attach Nothing Island accessibility overlay window", e)
                detachInternal()
            }
        }
    }

    /**
     * Mounts the overlay view onto the WindowManager with TYPE_APPLICATION_OVERLAY (2038).
     * Retained for controlled rollback if accessibility is disabled.
     */
    fun attachApplicationOverlay(context: Context) {
        mainHandler.post {
            if (isAttached) {
                Log.w(TAG, "Overlay is already attached. Detaching existing window before attaching application overlay.")
                detachInternal()
            }

            try {
                activeContext = context
                val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                activeWindowManager = wm
                isAccessibilityMode = false

                val currentPrefs = preferencesRepository.livePreferences.value
                val screenWidthPx = context.resources.displayMetrics.widthPixels
                val compactHeightPx = CutoutDetector.dpToPx(CutoutDetector.DEFAULT_COMPACT_TOUCH_HEIGHT_DP, context)
                val topMarginPx = CutoutDetector.dpToPx(currentPrefs.topMarginDp, context)

                val params = WindowManager.LayoutParams(
                    screenWidthPx,
                    compactHeightPx,
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                    PixelFormat.TRANSLUCENT
                ).apply {
                    gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                    x = 0
                    y = topMarginPx
                    screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
                        fitInsetsTypes = 0
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        @Suppress("DEPRECATION")
                        layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                    }
                }

                val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
                controllerScope = scope

                val pm = IslandPriorityManager(scope) { resolvedEvent, priority, presentationId ->
                    repository.updateFromResolvedEvent(resolvedEvent, priority, presentationId)
                }
                priorityManager = pm

                scope.launch {
                    eventEngine.events.collect { event ->
                        pm.processEvent(event)
                    }
                }

                // Reactive WindowManager resizing observing StateFlow master state, calibration preferences, and media track
                scope.launch {
                    combine(
                        repository.state,
                        preferencesRepository.livePreferences,
                        mediaTracker.currentTrack
                    ) { masterState, prefs, _ ->
                        Pair(masterState, prefs)
                    }.collect { (masterState, prefs) ->
                        mainHandler.post {
                            updateWindowLayout(masterState, prefs)
                        }
                    }
                }

                // R7.1 Notification-triggered auto-expansion observer (independent of music)
                launchNotificationAutoExpansionObserver(scope, pm)

                // R8 Battery & Charging auto-expansion observer
                launchBatteryAutoExpansionObserver(scope, pm)
                batteryMonitor.startMonitoring()

                // Sprint 2 Batch 1: Hardware callbacks & System events
                torchHandler.start()
                networkHandler.start()
                bluetoothEarbudTracker.start()
                if (systemEventsReceiver == null) {
                    systemEventsReceiver = SystemEventsReceiver { event ->
                        if (shouldEmitSystemEvent(event)) eventEngine.emit(event)
                    }
                    val filter = SystemEventsReceiver.createIntentFilter()
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        appContext.registerReceiver(systemEventsReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
                    } else {
                        appContext.registerReceiver(systemEventsReceiver, filter)
                    }
                }

                // Smart Music Auto-expansion observer
                scope.launch {
                    var previousTrackKey: String? = null
                    mediaTracker.currentTrack.collect { track ->
                        val musicPrefs = preferencesRepository.liveMusicPreferences.value
                        val trackKey = "${track.packageName}_${track.trackTitle}"
                        val isNewTrack = track.isPlaying && trackKey != previousTrackKey && track.trackTitle.isNotBlank() && track.trackTitle != "Audio Track"
                        if (track.isPlaying) {
                            previousTrackKey = trackKey
                        }
                        if (isNewTrack && musicPrefs.isAutoExpandEnabled && repository.state.value.mode == IslandMode.COLLAPSED) {
                            Log.d(TAG, "Auto-expanding music for new track: ${track.trackTitle}")
                            expandMusic() // schedules its own auto-collapse
                        }
                    }
                }

                // Synchronize pulse preference with repository state
                scope.launch {
                    preferencesRepository.livePreferences
                        .map { it.isPulsing }
                        .distinctUntilChanged()
                        .collect { isPulsing ->
                            repository.setPulse(isPulsing)
                        }
                }

                // Game mode auto-hide preference observer
                scope.launch {
                    preferencesRepository.livePreferences
                        .map { it.autoHideInGames }
                        .distinctUntilChanged()
                        .collect {
                            evaluateGameMode()
                        }
                }

                // Foreground app polling fallback (complements AccessibilityService)
                scope.launch {
                    val usm = appContext.getSystemService(Context.USAGE_STATS_SERVICE) as? android.app.usage.UsageStatsManager
                    while (isActive) {
                        delay(2000L)
                        try {
                            if (preferencesRepository.livePreferences.value.autoHideInGames && usm != null) {
                                val time = System.currentTimeMillis()
                                val events = usm.queryEvents(time - 5000L, time)
                                var lastPkg: String? = null
                                if (events != null) {
                                    val event = android.app.usage.UsageEvents.Event()
                                    while (events.hasNextEvent()) {
                                        events.getNextEvent(event)
                                        if (event.eventType == android.app.usage.UsageEvents.Event.ACTIVITY_RESUMED) {
                                            lastPkg = event.packageName
                                        }
                                    }
                                }
                                if (lastPkg != null && lastPkg != currentForegroundPackage) {
                                    onForegroundPackageChanged(lastPkg)
                                }
                            }
                        } catch (_: Exception) {}
                    }
                }

                // Immediately dismiss active notification presentation if preview mode is switched to HIDDEN
                scope.launch {
                    preferencesRepository.notificationPreferencesFlow
                        .map { it.previewMode }
                        .distinctUntilChanged()
                        .collect { mode ->
                            if (mode == NotificationPreviewMode.HIDDEN) {
                                val current = pm.activeEvent
                                if (current is IslandEvent.NotificationPosted) {
                                    Log.i(TAG, "Notification preview mode switched to HIDDEN; dismissing active presentation.")
                                    pm.processEvent(IslandEvent.UserDismissCurrent(id = "dismiss_mode_hidden"))
                                }
                                Log.i(TAG, "Notification preview mode switched to HIDDEN; clearing history repository and actions.")
                                historyRepository.clearAll()
                                NotificationActionBridge.clearAll()
                                repository.collapse()
                            }
                        }
                }

                registerKeyguardReceiver()

                val owner = OverlayLifecycleOwner()
                val view = ComposeView(context).apply {
                    setOnTouchListener { _, ev ->
                        if (ev.actionMasked == MotionEvent.ACTION_OUTSIDE) {
                            Log.d("IslandTouch", "ApplicationOverlay: ACTION_OUTSIDE detected -> collapsing overlay to pill")
                            collapse()
                            true
                        } else {
                            false
                        }
                    }
                    setViewTreeLifecycleOwner(owner)
                    setViewTreeSavedStateRegistryOwner(owner)
                    setViewTreeViewModelStoreOwner(owner)
                    setContent {
                        val masterState by repository.state.collectAsState()
                        val prefs by preferencesRepository.livePreferences.collectAsState()
                        val notifPrefs by preferencesRepository.liveNotificationPreferences.collectAsState()
                        val musicPrefs by preferencesRepository.liveMusicPreferences.collectAsState()
                        val batteryPrefs by preferencesRepository.liveBatteryPreferences.collectAsState()
                        val gesturePrefs by preferencesRepository.liveGesturePreferences.collectAsState()
                        val batteryState by batteryMonitor.batteryState.collectAsState()
                        val currentTrack by mediaTracker.currentTrack.collectAsState()
                        val resumableMedia by mediaTracker.lastResumableMedia.collectAsState()
                        val history by projectedHistory.collectAsState()
                        val undoState by historyRepository.pendingUndoFlow.collectAsState()
                        val activeInterface by activitySwitcher.currentInterface.collectAsState()
                        val signatureTheme = remember(prefs.presetName) {
                            com.nothing.island.ai.ui.theme.IslandSignatureTheme.resolve(prefs.presetName)
                        }

                        androidx.compose.runtime.CompositionLocalProvider(
                            com.nothing.island.ai.ui.theme.LocalIslandTheme provides signatureTheme
                        ) {
                            IslandOverlayView(
                            state = masterState.toLegacyIslandState(),
                            geometry = geometry,
                            preferences = prefs,
                            notificationPreferences = notifPrefs,
                            musicPreferences = musicPrefs,
                            batteryPreferences = batteryPrefs,
                            batteryState = batteryState,
                            currentTrack = currentTrack,
                            resumableMedia = resumableMedia,
                            activeEvent = masterState.activeEvent,
                            isDualPill = masterState.isDualPill,
                            projectedHistory = history,
                            selectedNotificationKey = masterState.selectedNotificationKey,
                            activeUndo = undoState,
                            activitySwitcherInterface = activeInterface,
                            onSelectHistoricalNotification = { key ->
                                selectHistoricalNotification(key)
                            },
                            onDismissHistoryEntry = { key ->
                                dismissHistoryEntry(key)
                            },
                            onUndoDismiss = {
                                undoDismiss()
                            },
                            onClearHistory = {
                                clearLocalHistory()
                            },
                            onToggleExpand = {
                                Log.d("IslandTouch", "onToggleExpand lambda invoked")
                                toggleExpand()
                            },
                            onExpandDashboard = {
                                Log.d("IslandTouch", "onExpandDashboard lambda invoked")
                                expandDashboard()
                            },
                            onExpandNotificationFocused = {
                                Log.d("IslandTouch", "onExpandNotificationFocused lambda invoked")
                                expandNotificationFocused()
                            },
                            onExpandMusic = {
                                Log.d("IslandTouch", "onExpandMusic lambda invoked")
                                expandMusic()
                            },
                            onExpandBattery = {
                                Log.d("IslandTouch", "onExpandBattery lambda invoked")
                                expandBattery()
                            },
                            onExpandCall = {
                                Log.d("IslandTouch", "onExpandCall lambda invoked")
                                expandCall()
                            },
                            onAnswerCall = {
                                Log.d("IslandTouch", "onAnswerCall lambda invoked")
                                if (CallController.answer(appContext)) {
                                    val ringing = priorityManager?.activeEvent as? IslandEvent.IncomingCallRinging
                                    val callId = ringing?.callId ?: "active_call"
                                    val caller = ringing?.callerDisplayName ?: "Call"
                                    val photo = ringing?.contactPhoto
                                    eventEngine.emit(
                                        IslandEvent.CallConnected(
                                            id = "call_connected_$callId",
                                            callId = callId,
                                            callerDisplayName = caller,
                                            contactPhoto = photo,
                                        )
                                    )
                                    startCallTicker(callId, caller, photo)
                                }
                            },
                            onQuickTool = { tool -> handleQuickTool(tool) },
                            onQuickVolume = { delta -> adjustQuickVolume(delta) },
                            onQuickVolumePercent = { percent -> setMediaVolumePercent(percent) },
                            onQuickBrightnessPercent = { percent -> setScreenBrightnessPercent(percent) },
                            getQuickVolumePercent = { getMediaVolumePercent() },
                            getQuickBrightnessPercent = { getScreenBrightnessPercent() },
                            onQuickHistory = { openQuickHistory() },
                            onDeclineCall = {
                                Log.d("IslandTouch", "onDeclineCall lambda invoked")
                                val currentCall = priorityManager?.activeEvent
                                val callId = when (currentCall) {
                                    is IslandEvent.CallConnected -> currentCall.callId
                                    is IslandEvent.IncomingCallRinging -> currentCall.callId
                                    else -> "active_call"
                                }
                                CallController.decline(appContext)
                                CallController.resetCallStates()
                                stopCallTicker()
                                eventEngine.emit(IslandEvent.CallEnded(id = "call_end_$callId", callId = callId))
                                collapse()
                            },
                            onCollapse = {
                                Log.d("IslandTouch", "onCollapse lambda invoked")
                                collapse()
                            },
                            onTogglePulse = {
                                Log.d("IslandTouch", "onTogglePulse lambda invoked")
                                togglePulse()
                            },
                            onOpenContent = { id ->
                                openNotificationContent(id)
                            },
                            onDismissEvent = { id ->
                                dismissNotification(id)
                            },
                            onExecuteAction = { id, index ->
                                executeNotificationAction(id, index)
                            },
                            onSendReply = { key, index, text ->
                                sendNotificationReply(key, index, text)
                            },
                            onInputFocusChanged = { enabled ->
                                setInputFocusMode(enabled)
                            },
                            onPlayPause = {
                                playPauseMedia()
                            },
                            onSkipNext = {
                                skipNextMedia()
                            },
                            onSkipPrevious = {
                                skipPrevMedia()
                            },
                            onSeekTo = { pos ->
                                seekMediaTo(pos)
                            },
                            onToggleRepeat = { mode ->
                                setMediaRepeatMode(mode)
                            },
                            onOpenMediaApp = {
                                openMediaApp()
                            },
                            onResumeLastMedia = {
                                resumeLastMedia()
                            },
                            onSwipeActivity = { direction ->
                                when (direction) {
                                    com.nothing.island.ai.switcher.SwipeDirection.DOWN -> repository.expandQuickTools()
                                    com.nothing.island.ai.switcher.SwipeDirection.UP -> collapse()
                                    com.nothing.island.ai.switcher.SwipeDirection.LEFT,
                                    com.nothing.island.ai.switcher.SwipeDirection.RIGHT -> handleActivitySwipe(direction)
                                }
                            },
                            gesturePreferences = gesturePrefs,
                            stateManager = stateManager,
                            onScreenshotResult = { res -> reportScreenshotResult(res) },
                            onPillBoundsChanged = { l, t, r, b -> updatePillBounds(l, t, r, b) },
                            onMascotBoundsChanged = { l, t, r, b -> updateMascotBounds(l, t, r, b) }
                        )
                        }
                    }
                }

                setupTouchPassthrough(view)
                owner.attach()
                wm.addView(view, params)

                this.lifecycleOwner = owner
                this.composeView = view
                this.layoutParams = params
                this.isAttached = true
                isOverlayActive = true

                if (isHiddenByGameMode) {
                    view.visibility = View.GONE
                    params.flags = params.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                    try { wm.updateViewLayout(view, params) } catch (_: Exception) {}
                }

                Log.i(TAG, "Nothing Island Application Overlay attached successfully at Layer 21000.")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to attach Nothing Island application overlay window", e)
                detachInternal()
            }
        }
    }

    /**
     * Backward-compatible attach method defaulting to application overlay.
     */
    fun attach() {
        attachApplicationOverlay(activeContext ?: appContext)
    }

    /**
     * Toggles between collapsed punch-hole pill and expanded card.
     */
    fun toggleExpand() {
        mainHandler.post {
            autoExpandJob?.cancel()
            val masterState = repository.state.value
            if (masterState.mode == IslandMode.EXPANDED) {
                collapse()
            } else {
                handleCompactPillTap()
            }
        }
    }

    /**
     * Smart time-decayed tap priority handler for the compact pill.
     * Evaluates 90-second unacknowledged notification window and active media.
     */
    fun handleCompactPillTap() {
        mainHandler.post {
            autoExpandJob?.cancel()
            val masterState = repository.state.value
            val currentTrack = mediaTracker.currentTrack.value
            val isCallActive = masterState.activeEvent is IslandEvent.IncomingCallRinging ||
                    masterState.activeEvent is IslandEvent.CallConnected ||
                    masterState.activeEvent is IslandEvent.CallEnded
            val destination = stateManager.resolveTapDestination(
                isCallActive = isCallActive,
                isMediaPlaying = currentTrack.isPlaying
            )
            when (destination) {
                ExpandedViewType.CALL -> {
                    repository.expandCall()
                }
                ExpandedViewType.NOTIFICATION_FOCUSED -> {
                    expandNotificationFocused()
                }
                ExpandedViewType.MUSIC -> {
                    expandMusic()
                }
                else -> {
                    expandDashboard()
                }
            }
        }
    }

    /**
     * Expands overlay into State A: Normal Dashboard (Time, Date, Weather, Recent Previews).
     */
    fun expandDashboard() {
        mainHandler.post {
            autoExpandJob?.cancel()
            activitySwitcher.navigateTo(IslandInterface.DASHBOARD)
            repository.expandDashboard()
        }
    }

    /**
     * Expands overlay into State B: Notification-Focused View (Notification details, actions; strictly NO clock/weather).
     */
    fun expandNotificationFocused() {
        mainHandler.post {
            autoExpandJob?.cancel()
            stateManager.onNotificationViewExpanded()
            activitySwitcher.navigateTo(IslandInterface.NOTIFICATION_FOCUSED)
            repository.expandNotificationFocused()
        }
    }

    /**
     * R7.1: opens the notification-focused view on behalf of the auto-expansion engine.
     * Marks the state as auto-expanded so event expiry may collapse it again.
     */
    private fun expandNotificationFocusedAuto() {
        mainHandler.post {
            autoExpandJob?.cancel()
            stateManager.onNotificationViewExpanded()
            activitySwitcher.navigateTo(IslandInterface.NOTIFICATION_FOCUSED)
            repository.expandNotificationFocused(auto = true)
        }
    }

    /**
     * R7.1 — Notification-triggered auto-expansion observer.
     *
     * Independent of, and parallel to, the R7 music auto-expansion observer. It watches the
     * resolved master state (already arbitrated by [IslandPriorityManager]) and opens the Island
     * into the notification-focused view when an eligible notification wins presentation.
     *
     * Design notes:
     *  - Priority arbitration happens UPSTREAM. A P100 call or P70 safety alert rejects the P55
     *    notification before this observer ever sees it, so higher-priority interruptions are
     *    respected without duplicating precedence logic here.
     *  - The collapse timer reuses the shared [autoExpandJob] field rather than allocating a
     *    second timer, matching the existing R7 mechanism.
     *  - Nothing here touches the Android notification: no cancellation, no removal from the
     *    shade, no channel edits. Actions and history live in separate components and are untouched.
     */
    private fun launchNotificationAutoExpansionObserver(scope: CoroutineScope, pm: IslandPriorityManager) {
        scope.launch {
            repository.state
                .map { it.activeEvent to it.presentationId }
                .distinctUntilChanged { (prevEvent, prevId), (currEvent, currId) ->
                    prevEvent === currEvent && prevId == currId
                }
                .collect { (event, presentationId) ->
                    if (event is IslandEvent.NotificationPosted) {
                        stateManager.onNotificationArrived(event.timestamp)
                    }
                    val prefs = preferencesRepository.liveNotificationPreferences.value
                    val current = repository.state.value

                    val outcome = NotificationAutoExpansionPolicy.evaluateExpansion(
                        event = event,
                        presentationId = presentationId,
                        lastHandledPresentationId = lastAutoExpandedPresentationId,
                        mode = current.mode,
                        expandedViewType = current.expandedViewType,
                        preferences = prefs,
                        isUserInteracting = isUserInteractingWithNotification
                    )

                    when (outcome) {
                        is NotificationAutoExpansionPolicy.ExpansionOutcome.Skip -> {
                            if (event is IslandEvent.NotificationPosted) {
                                Log.d(TAG, "Notification auto-expansion skipped (${outcome.reason}) for ${event.id}")
                            }
                        }

                        is NotificationAutoExpansionPolicy.ExpansionOutcome.Expand -> {
                            if (pm.activePresentationId != presentationId) return@collect
                            Log.i(TAG, "Auto-expanding Island for notification presentation $presentationId (${outcome.durationMs}ms)")
                            lastAutoExpandedPresentationId = presentationId

                            expandNotificationFocusedAuto()
                            autoExpandJob?.cancel()
                            autoExpandJob = scope.launch {
                                kotlinx.coroutines.delay(outcome.durationMs)
                                val stillCurrent = repository.state.value
                                val canCollapse = !shouldBlockAutoCollapse(stillCurrent) &&
                                    NotificationAutoExpansionPolicy.evaluateCollapse(
                                        currentPresentationId = pm.activePresentationId,
                                        expectedPresentationId = presentationId,
                                        mode = stillCurrent.mode,
                                        expandedViewType = stillCurrent.expandedViewType,
                                        isUserInteracting = isUserInteractingWithNotification,
                                        hasActiveNotification = stillCurrent.activeEvent is IslandEvent.NotificationPosted
                                    )
                                if (canCollapse) {
                                    Log.d(TAG, "Notification auto-expand duration expired; collapsing to compact pill.")
                                    collapse()
                                } else {
                                    Log.d(TAG, "Notification auto-collapse suppressed (interaction or presentation changed).")
                                }
                            }
                        }
                    }
                }
        }
    }

    private fun launchBatteryAutoExpansionObserver(scope: CoroutineScope, pm: IslandPriorityManager) {
        scope.launch {
            repository.state
                .map { it.activeEvent to it.presentationId }
                .distinctUntilChanged { (prevEvent, prevId), (currEvent, currId) ->
                    prevEvent === currEvent && prevId == currId
                }
                .collect { (event, presentationId) ->
                    val prefs = preferencesRepository.liveBatteryPreferences.value
                    val current = repository.state.value

                    val outcome = BatteryAutoExpansionPolicy.evaluateExpansion(
                        event = event,
                        presentationId = presentationId,
                        lastHandledPresentationId = lastAutoExpandedBatteryPresentationId,
                        mode = current.mode,
                        expandedViewType = current.expandedViewType,
                        preferences = prefs,
                        isUserInteracting = isUserInteractingWithNotification
                    )

                    when (outcome) {
                        is BatteryAutoExpansionPolicy.ExpansionOutcome.Skip -> {
                            if (event is IslandEvent.ChargingConnected) {
                                Log.d(TAG, "Battery auto-expansion skipped (${outcome.reason}) for ${event.id}")
                            }
                        }

                        is BatteryAutoExpansionPolicy.ExpansionOutcome.Expand -> {
                            if (pm.activePresentationId != presentationId) return@collect
                            Log.i(TAG, "Auto-expanding Island for battery presentation $presentationId (${outcome.durationMs}ms)")
                            lastAutoExpandedBatteryPresentationId = presentationId

                            expandBattery(auto = true)
                            batteryAutoExpandJob?.cancel()
                            batteryAutoExpandJob = scope.launch {
                                kotlinx.coroutines.delay(outcome.durationMs)
                                val stillCurrent = repository.state.value
                                val canCollapse = !shouldBlockAutoCollapse(stillCurrent) &&
                                    BatteryAutoExpansionPolicy.evaluateCollapse(
                                        currentPresentationId = pm.activePresentationId,
                                        expectedPresentationId = presentationId,
                                        mode = stillCurrent.mode,
                                        expandedViewType = stillCurrent.expandedViewType,
                                        isUserInteracting = isUserInteractingWithNotification,
                                        hasActiveBatteryEvent = stillCurrent.activeEvent is IslandEvent.ChargingConnected ||
                                                stillCurrent.activeEvent is IslandEvent.BatteryStateChanged ||
                                                stillCurrent.activeEvent is IslandEvent.BatteryWarning
                                    )
                                if (canCollapse) {
                                    Log.d(TAG, "Battery auto-expand duration expired; collapsing to compact pill.")
                                    collapse()
                                } else {
                                    Log.d(TAG, "Battery auto-collapse suppressed (interaction or presentation changed).")
                                }
                            }
                        }
                    }
                }
        }
    }

    /**
     * Expands overlay into State C: Music Player (Album art, track info, visualizer, transport controls, timeline, device output).
     */
    fun expandMusic() {
        mainHandler.post {
            autoExpandJob?.cancel()
            batteryAutoExpandJob?.cancel()
            activitySwitcher.navigateTo(IslandInterface.MUSIC)
            repository.expandMusic()

            // Auto-collapse the music player a few seconds after it opens so the
            // island returns to the compact pill (reference behavior).
            val prefs = preferencesRepository.liveMusicPreferences.value
            if (prefs.isAutoExpandEnabled) {
                autoExpandJob = controllerScope?.launch {
                    kotlinx.coroutines.delay((prefs.autoExpandDurationSeconds * 1000).toLong())
                    val current = repository.state.value
                    if (!shouldBlockAutoCollapse(current) &&
                        current.mode == IslandMode.EXPANDED &&
                        current.expandedViewType == ExpandedViewType.MUSIC
                    ) {
                        Log.d(TAG, "Music auto-collapse timer expired; collapsing to compact pill.")
                        collapse()
                    }
                }
            }
        }
    }

    /**
     * Expands overlay into State E: Call Card, cancelling any competing auto-collapse timers so
     * an actively ringing call stays open until answered, declined, or manually collapsed.
     */
    fun expandCall() {
        mainHandler.post {
            autoExpandJob?.cancel()
            autoExpandJob = null
            batteryAutoExpandJob?.cancel()
            batteryAutoExpandJob = null
            repository.expandCall()
        }
    }

    /**
     * Expands overlay into State D: Battery & Charging Intelligence.
     */
    fun expandBattery(auto: Boolean = false) {
        mainHandler.post {
            autoExpandJob?.cancel()
            batteryAutoExpandJob?.cancel()
            activitySwitcher.navigateTo(IslandInterface.BATTERY)
            repository.expandBattery(auto = auto)
        }
    }

    /**
     * Collapses overlay back to the calibrated compact pill.
     */
    fun collapse() {
        autoExpandJob?.cancel()
        batteryAutoExpandJob?.cancel()
        setInputFocusMode(false)
        mainHandler.post {
            if (repository.state.value.expandedViewType == ExpandedViewType.NOTIFICATION_FOCUSED) {
                stateManager.onNotificationViewCollapsed()
            }
            repository.collapse()
        }
    }

    /**
     * Reactively synchronizes physical WindowManager overlay bounds and offsets
     * with current IslandMasterState and user calibration preferences.
     */
    fun updateWindowLayout(state: IslandMasterState, prefs: IslandPreferences) {
        val view = composeView ?: return
        val params = layoutParams ?: return
        val wm = activeWindowManager ?: return
        val ctx = activeContext ?: appContext

        if (shouldBlockAutoCollapse(state)) {
            autoExpandJob?.cancel()
            autoExpandJob = null
            batteryAutoExpandJob?.cancel()
            batteryAutoExpandJob = null
        }

        val isEarbudActive = state.activeEvent is IslandEvent.BluetoothDeviceConnected ||
                state.activeEvent is IslandEvent.BluetoothDeviceDisconnected
        val mode = if (isEarbudActive) IslandMode.COLLAPSED else state.mode
        val isModeTransitionFromExpandedToCollapsed = previousOverlayMode == IslandMode.EXPANDED && mode == IslandMode.COLLAPSED
        previousOverlayMode = mode

        val hasActiveNotification = state.activeEvent is IslandEvent.NotificationPosted
        val track = mediaTracker.currentTrack.value
        val isMediaPlaying = track.isPlaying || state.activeEvent is IslandEvent.MediaPlaybackChanged
        val isBatteryActive = state.activeEvent is IslandEvent.ChargingConnected ||
                state.activeEvent is IslandEvent.ChargingDisconnected ||
                (state.activeEvent is IslandEvent.BatteryStateChanged && state.activeEvent.isLowBattery) ||
                (state.activeEvent is IslandEvent.BatteryWarning) ||
                (state.mode == IslandMode.EXPANDED && activitySwitcher.currentInterface.value == IslandInterface.BATTERY)
        val isCallActive = state.activeEvent is IslandEvent.IncomingCallRinging ||
                state.activeEvent is IslandEvent.CallConnected ||
                state.activeEvent is IslandEvent.CallEnded

        val isChargingCompactVisible = isBatteryActive ||
            (batteryMonitor.batteryState.value.isCharging &&
                preferencesRepository.liveBatteryPreferences.value.chargingAnimationEnabled &&
                !isMediaPlaying)

        val isSystemAlertActive = state.activeEvent is IslandEvent.RingerModeChanged ||
            state.activeEvent is IslandEvent.TorchStateChanged ||
            state.activeEvent is IslandEvent.WifiStateChanged ||
            state.activeEvent is IslandEvent.DndStateChanged ||
            state.activeEvent is IslandEvent.BatteryThresholdWarning ||
            state.activeEvent is IslandEvent.DeviceUnlocked

        val isQuickToolsExpanded = mode == IslandMode.EXPANDED &&
                state.expandedViewType == com.nothing.island.ai.ExpandedViewType.QUICK_TOOLS
        val isCallExpanded = mode == IslandMode.EXPANDED &&
                state.expandedViewType == com.nothing.island.ai.ExpandedViewType.CALL

        val compactStageSpec = com.nothing.island.ai.ui.components.computeCompactStageWindowSpec(
            compactWidthDp = prefs.compactWidthDp,
            compactHeightDp = prefs.compactHeightDp,
            mascotEnabled = prefs.mascotEnabled,
            isDualPill = state.isDualPill,
            isEarbudActive = isEarbudActive,
            isSystemAlertActive = isSystemAlertActive,
            isCallActive = isCallActive,
            hasActiveNotification = hasActiveNotification,
            isMusicPlaying = isMediaPlaying,
            isChargingActive = isChargingCompactVisible,
        )

        val widthDp = if (mode == IslandMode.EXPANDED) {
            if (isCallExpanded) 344f else prefs.expandedWidthDp
        } else {
            compactStageSpec.widthDp
        }

        val isMascotStageActive = mode != IslandMode.EXPANDED &&
            !state.isDualPill &&
            prefs.mascotEnabled

        if (!isMascotStageActive) {
            mascotBoundsRect.setEmpty()
        }

        val callExpandedHeightDp = (if (prefs.isCameraCutoutEnabled) 120f else 78f) +
                if (prefs.mascotEnabled && state.activeEvent !is IslandEvent.CallEnded) 14f else 0f
        val heightDp = if (isQuickToolsExpanded) {
            360f // Room for tiles, shortcuts, contacts and the bottom control row
        } else if (isCallExpanded) {
            callExpandedHeightDp
        } else if (mode == IslandMode.EXPANDED) {
            prefs.expandedHeightDp
        } else {
            compactStageSpec.heightDp
        }

        // Tight WindowManager bounds around visible capsule (plus minimal active chibi overhang only)
        // so 100% of taps outside the pill pass natively to top-bar buttons and apps underneath.
        val screenWidthPx = ctx.resources.displayMetrics.widthPixels
        val targetWidthPx = if (mode == IslandMode.EXPANDED) {
            screenWidthPx
        } else {
            CutoutDetector.dpToPx(widthDp, ctx)
        }
        val targetXPx = 0

        val headroomDp = if (mode == IslandMode.EXPANDED) 24f else 0f
        val targetHeightPx = if (mode == IslandMode.EXPANDED) {
            ctx.resources.displayMetrics.heightPixels
        } else {
            CutoutDetector.dpToPx(heightDp + headroomDp, ctx)
        }

        // Compact mode anchors strictly at prefs.topMarginDp; expanded mode honors expandedOffsetY.
        val targetYPx = if (mode == IslandMode.EXPANDED) {
            CutoutDetector.dpToPx((prefs.topMarginDp + prefs.expandedOffsetY).coerceAtLeast(0f), ctx)
        } else {
            CutoutDetector.dpToPx(prefs.topMarginDp, ctx)
        }

        val baseFlags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                if (isAccessibilityMode) WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS else WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN

        val targetFlags = when {
            isOverlayHidden -> baseFlags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
            mode == IslandMode.EXPANDED -> baseFlags or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
            else -> baseFlags
        }

        if (isOverlayHidden && view.visibility != View.GONE) {
            view.visibility = View.GONE
        } else if (!isOverlayHidden && view.visibility != View.VISIBLE) {
            view.visibility = View.VISIBLE
        }

        var changed = false
        if (params.gravity != (Gravity.TOP or Gravity.CENTER_HORIZONTAL)) {
            params.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            changed = true
        }
        if (params.flags != targetFlags) {
            params.flags = targetFlags
            changed = true
        }
        if (params.width != targetWidthPx) {
            params.width = targetWidthPx
            changed = true
        }
        if (params.height != targetHeightPx) {
            params.height = targetHeightPx
            changed = true
        }
        if (params.x != targetXPx) {
            params.x = targetXPx
            changed = true
        }
        if (params.y != targetYPx) {
            params.y = targetYPx
            changed = true
        }

        if (changed) {
            try {
                wm.updateViewLayout(view, params)
                Log.d(TAG, "WindowManager layout updated: mode=$mode, size=${widthDp}x${heightDp}dp (${targetWidthPx}x${targetHeightPx}px), offset=($targetXPx, $targetYPx)px, flags=$targetFlags")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update overlay view layout for mode $mode", e)
            }
        }
    }

    /**
     * Backward-compatible overload for mode-only callers.
     */
    fun updateWindowLayout(mode: IslandMode, prefs: IslandPreferences) {
        updateWindowLayout(repository.state.value.copy(mode = mode), prefs)
    }

    // =========================================================================
    // GAME MODE AUTO-HIDE & FOREGROUND DETECTION
    // =========================================================================

    /**
     * Called whenever a foreground package change is detected.
     */
    fun onForegroundPackageChanged(packageName: String) {
        if (GameModeDetector.isTransientSystemWindow(packageName)) return
        if (currentForegroundPackage == packageName) return
        currentForegroundPackage = packageName
        evaluateGameMode()
    }

    /**
     * Called whenever screen orientation changes (portrait/landscape).
     */
    fun onConfigurationChanged(newConfig: Configuration) {
        if (currentOrientation == newConfig.orientation) return
        currentOrientation = newConfig.orientation
        evaluateGameMode()
    }

    /**
     * Evaluates game mode state and updates overlay visibility accordingly.
     */
    fun evaluateGameMode() {
        mainHandler.post {
            val autoHide = preferencesRepository.livePreferences.value.autoHideInGames
            val shouldHide = gameModeDetector.shouldHideOverlay(
                autoHideInGames = autoHide,
                foregroundPackage = currentForegroundPackage,
                orientation = currentOrientation
            )
            setGameModeHidden(shouldHide)
        }
    }

    /**
     * Hides or restores the overlay window for game mode.
     * When hidden, the root view is set to View.GONE and FLAG_NOT_TOUCHABLE is applied
     * so it never covers game HUD controls or registers accidental touches.
     */
    fun setGameModeHidden(hidden: Boolean) {
        mainHandler.post {
            if (isHiddenByGameMode == hidden) return@post
            isHiddenByGameMode = hidden
            applyCombinedOverlayVisibility(reason = if (hidden) "game_hide(pkg=$currentForegroundPackage)" else "game_restore")
        }
    }

    /**
     * Temporarily hides or restores the overlay when SystemUI Notification Shade / Quick Settings opens.
     */
    fun setSystemShadeHidden(hidden: Boolean) {
        mainHandler.post {
            if (isHiddenBySystemShade == hidden) return@post
            isHiddenBySystemShade = hidden
            applyCombinedOverlayVisibility(reason = if (hidden) "shade_opened" else "shade_closed")
        }
    }

    private fun applyCombinedOverlayVisibility(reason: String) {
        val view = composeView ?: return
        val params = layoutParams ?: return
        val wm = activeWindowManager ?: return
        val shouldHide = isOverlayHidden

        if (shouldHide) {
            view.visibility = View.GONE
            params.flags = params.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
            try {
                wm.updateViewLayout(view, params)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to hide overlay ($reason)", e)
            }
            Log.i(TAG, "Overlay hidden ($reason, game=$isHiddenByGameMode, shade=$isHiddenBySystemShade)")
        } else {
            view.visibility = View.VISIBLE
            params.flags = params.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()
            try {
                wm.updateViewLayout(view, params)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to restore overlay ($reason)", e)
            }
            updateWindowLayout(repository.state.value, preferencesRepository.livePreferences.value)
            Log.i(TAG, "Overlay restored ($reason)")
        }
    }

    /**
     * Launches notification primary content intent and collapses the island.
     */
    fun openNotificationContent(notificationId: String, packageName: String? = null) {
        val resolvedPackage = packageName ?: historyRepository.getEntry(notificationId)?.packageName
        var fired = NotificationActionBridge.executeContentIntent(appContext, notificationId)
        // Fallback: if the notification has no content PendingIntent or execution failed, launch the app
        // by package name so tapping the message always reliably opens the source app on Android 14+.
        if (!fired && !resolvedPackage.isNullOrBlank()) {
            try {
                val launchIntent = appContext.packageManager.getLaunchIntentForPackage(resolvedPackage)
                if (launchIntent != null) {
                    launchIntent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        val options = android.app.ActivityOptions.makeBasic()
                        options.setPendingIntentBackgroundActivityStartMode(
                            android.app.ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
                        )
                        appContext.startActivity(launchIntent, options.toBundle())
                    } else {
                        appContext.startActivity(launchIntent)
                    }
                    fired = true
                }
            } catch (e: Exception) {
                Log.w(TAG, "Fallback launch failed for $resolvedPackage", e)
            }
        }
        Log.d(TAG, "openNotificationContent: id=$notificationId, pkg=$resolvedPackage, fired=$fired")
        repository.setMode(IslandMode.COLLAPSED)
    }

    /**
     * Dismisses notification both from the Android shade and the Island overlay.
     */
    fun dismissNotification(notificationId: String) {
        Log.d(TAG, "dismissNotification: id=$notificationId")
        stateManager.acknowledgeLatestNotification()
        IslandNotificationListenerService.cancelSystemNotification(notificationId)
        controllerScope?.launch {
            eventEngine.emit(IslandEvent.UserDismissCurrent(id = "dismiss_$notificationId", source = "NotificationCard"))
        }
        repository.setMode(IslandMode.COLLAPSED)
    }

    /**
     * Safely executes an action button attached to a notification.
     */
    fun executeNotificationAction(notificationId: String, actionIndex: Int) {
        stateManager.acknowledgeLatestNotification()
        val fired = NotificationActionBridge.executeAction(notificationId, actionIndex)
        Log.d(TAG, "executeNotificationAction: id=$notificationId, index=$actionIndex, fired=$fired")
    }

    /**
     * Executes an inline reply to a notification via RemoteInput.
     */
    fun sendNotificationReply(key: String, actionIndex: Int, replyText: String): ReplyResult {
        stateManager.acknowledgeLatestNotification()
        val result = NotificationActionBridge.sendReply(appContext, key, actionIndex, replyText)
        Log.d(TAG, "sendNotificationReply: key=$key, index=$actionIndex, result=$result")
        return result
    }

    /**
     * Dynamically updates WindowManager focusable flags to allow soft keyboard IME interaction
     * during inline reply input.
     */
    fun setInputFocusMode(enabled: Boolean) {
        // R7.1: an open inline-reply IME counts as active user interaction. The Island must not
        // be yanked shut mid-sentence by the notification auto-collapse timer.
        isUserInteractingWithNotification = enabled
        mainHandler.post {
            val params = layoutParams ?: return@post
            val wm = activeWindowManager ?: return@post
            val view = composeView ?: return@post

            val oldFlags = params.flags
            val newFlags = if (enabled) {
                oldFlags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()
            } else {
                oldFlags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            }

            if (oldFlags != newFlags) {
                params.flags = newFlags
                try {
                    wm.updateViewLayout(view, params)
                    Log.d(TAG, "Input focus mode updated: enabled=$enabled, flags=$newFlags")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to update input focus mode flags", e)
                }
            }
        }
    }

    /**
     * Backward-compatible dimension updater delegating to updateWindowLayout with live preferences.
     */
    private fun updateWindowDimensions(mode: IslandMode) {
        updateWindowLayout(mode, preferencesRepository.livePreferences.value)
    }

    /**
     * Toggles accent breathing pulse and persists preference.
     */
    fun togglePulse() {
        val nextPulse = !repository.state.value.isPulsing
        repository.setPulse(nextPulse)
        controllerScope?.launch {
            preferencesRepository.updatePulseEnabled(nextPulse)
        }
    }

    /**
     * Selects a historical notification to view in State B (Notification-Focused View).
     */
    fun selectHistoricalNotification(key: String) {
        mainHandler.post {
            repository.selectHistoricalNotification(key)
        }
    }

    /**
     * Clears the currently selected historical notification.
     */
    fun clearSelectedNotification() {
        mainHandler.post {
            repository.clearSelectedNotification()
        }
    }

    /**
     * Dismisses a historical notification from Nothing Island.
     * If syncDismissWithAndroid is ON, also invokes system notification dismissal.
     * Otherwise, only affects Nothing Island local history.
     */
    fun dismissHistoryEntry(key: String) {
        Log.d(TAG, "dismissHistoryEntry: key=$key")
        stateManager.acknowledgeLatestNotification()
        val dismissed = historyRepository.dismissEntry(key) ?: return

        // Optional Android Dismissal Synchronization
        val shouldSync = preferencesRepository.liveNotificationPreferences.value.syncDismissWithAndroid
        if (shouldSync) {
            Log.i(TAG, "Sync Dismissals with Android is ON -> Requesting system shade dismissal for $key")
            IslandNotificationListenerService.cancelSystemNotification(key)
        } else {
            Log.d(TAG, "Sync Dismissals with Android is OFF -> Android notification shade untouched for $key")
        }

        // If currently viewing this entry in State B, switch back to Dashboard to surface Undo
        val current = repository.state.value
        if (current.selectedNotificationKey == key) {
            mainHandler.post { repository.expandDashboard() }
        } else if (current.activeEvent?.id == key) {
            controllerScope?.launch {
                eventEngine.emit(IslandEvent.UserDismissCurrent(id = "dismiss_$key", source = "IslandHistory"))
            }
        }
    }

    /**
     * Restores the latest dismissed entry (or specified key) back into Nothing Island history.
     */
    fun undoDismiss(key: String? = null) {
        val restored = historyRepository.undoDismiss(key)
        Log.d(TAG, "undoDismiss: key=$key, restored=$restored")
    }

    /**
     * Clears all local Nothing Island notification history with batch undo support.
     * If syncDismissWithAndroid is ON, also cancels active system notifications.
     */
    fun clearLocalHistory() {
        stateManager.acknowledgeLatestNotification()
        val shouldSync = preferencesRepository.liveNotificationPreferences.value.syncDismissWithAndroid
        if (shouldSync) {
            val activeKeys = historyRepository.history.value.filter { it.isSystemActive }.map { it.key }
            Log.i(TAG, "Sync Dismissals with Android is ON -> Requesting selective system dismissal for ${activeKeys.size} active alerts")
            val dismissedCount = IslandNotificationListenerService.cancelSystemNotifications(activeKeys)
            Log.i(TAG, "Selective system notification dismissal completed: $dismissedCount active alerts cleared from shade")
        } else {
            Log.d(TAG, "Sync Dismissals with Android is OFF -> Android notification shade untouched for Clear All")
        }

        val clearedCount = historyRepository.clearAllLocalHistory()
        mainHandler.post {
            repository.clearSelectedNotification()
        }
        Log.i(TAG, "clearLocalHistory: cleared $clearedCount items from Nothing Island history")
    }

    // =========================================================================
    // MEDIA & ACTIVITY SWITCHER CONTROLS (Milestone R7)
    // =========================================================================

    fun playPauseMedia() {
        mediaTracker.togglePlayPause()
    }

    fun resumeLastMedia(): Boolean = mediaTracker.resumeLastMedia()

    fun skipNextMedia() {
        mediaTracker.skipToNext()
    }

    fun skipPrevMedia() {
        mediaTracker.skipToPrevious()
    }

    fun seekMediaTo(positionMs: Long) {
        mediaTracker.seekTo(positionMs)
    }

    fun setMediaRepeatMode(mode: Int) {
        mediaTracker.setRepeatMode(mode)
    }

    fun openMediaApp(packageName: String? = null) {
        val targetPackage = packageName?.takeIf { it.isNotBlank() } ?: mediaTracker.currentTrack.value.packageName
        val launched = com.nothing.island.ai.media.MediaSessionTracker.launchMediaPlayer(
            context = appContext,
            packageName = targetPackage,
            sessionActivity = mediaTracker.getActiveSessionActivity(),
        )
        Log.d(TAG, "openMediaApp: pkg=$targetPackage, launched=$launched")
        collapse()
    }

    fun startMediaTracking(componentName: ComponentName) {
        audioOutputTracker.start()
        mediaTracker.startTracking(componentName)
    }

    fun stopMediaTracking() {
        audioOutputTracker.stop()
        mediaTracker.release()
    }

    /**
     * Traverses between available Island interfaces based on historical navigation stacks.
     */
    fun handleActivitySwipe(direction: SwipeDirection) {
        mainHandler.post {
            autoExpandJob?.cancel()
            batteryAutoExpandJob?.cancel()
            val track = mediaTracker.currentTrack.value
            val isMediaActive = track.isPlaying || (!track.isHistorical && track != MediaTrackState.EMPTY)
            val hasNotifications = projectedHistory.value.isNotEmpty() || repository.state.value.activeEvent is IslandEvent.NotificationPosted
            val isBatteryEligible = batteryMonitor.batteryState.value.isCharging || preferencesRepository.liveBatteryPreferences.value.enabled

            val candidates = mutableSetOf<IslandInterface>()
            candidates.add(IslandInterface.DASHBOARD)
            if (hasNotifications) {
                candidates.add(IslandInterface.NOTIFICATION_FOCUSED)
            }
            if (isMediaActive) {
                candidates.add(IslandInterface.MUSIC)
            }
            if (isBatteryEligible) {
                candidates.add(IslandInterface.BATTERY)
            }

            val currentExpandedType = repository.state.value.expandedViewType
            val currentTarget = when (currentExpandedType) {
                ExpandedViewType.DASHBOARD -> IslandInterface.DASHBOARD
                ExpandedViewType.NOTIFICATION_FOCUSED -> IslandInterface.NOTIFICATION_FOCUSED
                ExpandedViewType.MUSIC -> IslandInterface.MUSIC
                ExpandedViewType.BATTERY -> IslandInterface.BATTERY
                ExpandedViewType.CALL -> IslandInterface.DASHBOARD
                ExpandedViewType.QUICK_TOOLS -> IslandInterface.DASHBOARD
            }
            activitySwitcher.syncCurrentInterface(currentTarget)

            Log.d(TAG, "handleActivitySwipe: $direction, candidates=$candidates, current=${activitySwitcher.currentInterface.value}")
            val handled = activitySwitcher.handleSwipe(direction, candidates)
            if (handled) {
                when (activitySwitcher.currentInterface.value) {
                    IslandInterface.DASHBOARD -> repository.expandDashboard()
                    IslandInterface.NOTIFICATION_FOCUSED -> repository.expandNotificationFocused()
                    IslandInterface.MUSIC -> repository.expandMusic()
                    IslandInterface.BATTERY -> repository.expandBattery()
                }
            }
        }
    }

    /**
     * Pauses Compose lifecycle when screen turns off.
     */
    fun onScreenOff() {
        isKeyguardLockedFlow.value = true
        handleKeyguardLocked()
        lifecycleOwner?.onPause()
    }

    /**
     * Resumes Compose lifecycle when screen turns on.
     */
    fun onScreenOn() {
        val locked = keyguardManager?.isKeyguardLocked ?: true
        isKeyguardLockedFlow.value = locked
        if (locked) {
            handleKeyguardLocked()
        }
        lifecycleOwner?.onResume()
    }

    /**
     * Completely unmounts the WindowManager view and releases lifecycle resources.
     */
    fun detach() {
        mainHandler.post {
            detachInternal()
        }
    }

    private fun detachInternal() {
        if (!isAttached && composeView == null) {
            return
        }

        try {
            autoExpandJob?.cancel()
            autoExpandJob = null
            batteryAutoExpandJob?.cancel()
            batteryAutoExpandJob = null
            lastAutoExpandedPresentationId = null
            lastAutoExpandedBatteryPresentationId = null
            isUserInteractingWithNotification = false
            batteryMonitor.stopMonitoring()
            audioOutputTracker.stop()
            torchHandler.stop()
            networkHandler.stop()
            bluetoothEarbudTracker.stop()
            systemEventsReceiver?.let {
                try {
                    appContext.unregisterReceiver(it)
                } catch (e: Exception) {
                    Log.e(TAG, "Error unregistering systemEventsReceiver", e)
                }
                systemEventsReceiver = null
            }
            unregisterKeyguardReceiver()
            controllerScope?.cancel()
            controllerScope = null
            priorityManager?.reset()
            priorityManager = null
            eventEngine.reset()
            repository.resetToIdle()
            lifecycleOwner?.detach()
            if (insetsListenerProxy != null && composeView != null) {
                try {
                    removeInsetsListenerMethod?.invoke(composeView?.viewTreeObserver, insetsListenerProxy)
                } catch (_: Exception) {}
                insetsListenerProxy = null
            }
            composeView?.let { view ->
                activeWindowManager?.removeViewImmediate(view)
            }
            Log.i(TAG, "Nothing Island overlay detached, view removed, and state reset to idle.")
        } catch (e: Exception) {
            Log.e(TAG, "Error detaching Nothing Island overlay", e)
        } finally {
            composeView = null
            lifecycleOwner = null
            layoutParams = null
            activeWindowManager = null
            isAttached = false
            isOverlayActive = false
        }
    }
}
