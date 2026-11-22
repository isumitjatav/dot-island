package com.nothing.island.ai.data

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.nothing.island.ai.CutoutDetector
import com.nothing.island.ai.gestures.GestureAction
import com.nothing.island.ai.gestures.GesturePreferences
import com.nothing.island.ai.gestures.NotchDisappearMode
import com.nothing.island.ai.battery.BatteryPreferences
import com.nothing.island.ai.battery.ChargingAnimationStyle
import com.nothing.island.ai.media.ArtworkShape
import com.nothing.island.ai.media.MusicPreferences
import com.nothing.island.ai.media.MusicVisualizerStyle
import com.nothing.island.ai.notifications.NotificationAnimationStyle
import com.nothing.island.ai.notifications.NotificationPreferences
import com.nothing.island.ai.notifications.NotificationPreviewMode
import com.nothing.island.ai.system.SystemEventsPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.io.IOException

/**
 * Immutable data class representing the complete persistent and live configuration
 * of Nothing Island.
 *
 * All dimension and offset metrics use DP as the canonical unit.
 * Conversion to physical pixels occurs strictly at the WindowManager boundary.
 */
data class IslandPreferences(
    val compactWidthDp: Float = CutoutDetector.DEFAULT_COMPACT_WIDTH_DP,
    val compactHeightDp: Float = CutoutDetector.DEFAULT_COMPACT_VISIBLE_HEIGHT_DP,
    val expandedWidthDp: Float = CutoutDetector.DEFAULT_EXPANDED_WIDTH_DP,
    val expandedHeightDp: Float = CutoutDetector.DEFAULT_EXPANDED_HEIGHT_DP,
    val horizontalOffsetDp: Float = 0f,
    val topMarginDp: Float = CutoutDetector.DEFAULT_TOP_MARGIN_DP,
    val cornerRadiusDp: Float = CutoutDetector.DEFAULT_COMPACT_CORNER_RADIUS_DP,
    val isPulsing: Boolean = true,
    val hapticFeedbackEnabled: Boolean = true,
    val reducedMotionEnabled: Boolean = false,
    val presetName: String = "Nothing Minimal",
    val accentColorHex: Long = 0xFFD71920L, // NothingPrimaryAccent (NothingRed)
    val mascotEnabled: Boolean = false,
    val mascotSpriteName: String = "CAT",
    val pillBackgroundColorHex: Long = 0xFF000000L
)

/** Context extension for single-instance Preferences DataStore */
val Context.islandDataStore: DataStore<Preferences> by preferencesDataStore(name = "island_preferences")

/**
 * IslandPreferencesRepository
 *
 * Application-scoped singleton repository managing Jetpack Preferences DataStore persistence
 * and live in-memory calibration streaming for the Nothing Island overlay.
 */
class IslandPreferencesRepository internal constructor(
    private val dataStore: DataStore<Preferences>,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) {
    companion object {
        private const val TAG = "IslandPreferencesRepo"

        // Baseline hardware measurements for Nothing Phone (4a)
        val HARDWARE_DEFAULTS = IslandPreferences(
            compactWidthDp = CutoutDetector.DEFAULT_COMPACT_WIDTH_DP,          // 124.0f
            compactHeightDp = CutoutDetector.DEFAULT_COMPACT_VISIBLE_HEIGHT_DP, // 36.0f
            expandedWidthDp = CutoutDetector.DEFAULT_EXPANDED_WIDTH_DP,        // 350.0f
            expandedHeightDp = CutoutDetector.DEFAULT_EXPANDED_HEIGHT_DP,      // 210.0f
            horizontalOffsetDp = 0f,
            topMarginDp = CutoutDetector.DEFAULT_TOP_MARGIN_DP,                // 9.0f (27px)
            cornerRadiusDp = CutoutDetector.DEFAULT_COMPACT_CORNER_RADIUS_DP,  // 18.0f (54px)
            isPulsing = true,
            hapticFeedbackEnabled = true,
            reducedMotionEnabled = false,
            presetName = "Nothing Minimal",
            accentColorHex = 0xFFD71920L
        )

        @Volatile
        private var instance: IslandPreferencesRepository? = null

        fun getInstance(context: Context): IslandPreferencesRepository {
            return instance ?: synchronized(this) {
                instance ?: IslandPreferencesRepository(context.applicationContext.islandDataStore).also {
                    instance = it
                }
            }
        }

        fun createForTesting(
            dataStore: DataStore<Preferences>,
            testScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        ): IslandPreferencesRepository {
            return IslandPreferencesRepository(dataStore, testScope)
        }
    }

    object PreferencesKeys {
        val COMPACT_WIDTH_DP = floatPreferencesKey("compact_width_dp")
        val COMPACT_HEIGHT_DP = floatPreferencesKey("compact_height_dp")
        val EXPANDED_WIDTH_DP = floatPreferencesKey("expanded_width_dp")
        val EXPANDED_HEIGHT_DP = floatPreferencesKey("expanded_height_dp")
        val HORIZONTAL_OFFSET_DP = floatPreferencesKey("horizontal_offset_dp")
        val TOP_MARGIN_DP = floatPreferencesKey("top_margin_dp")
        val CORNER_RADIUS_DP = floatPreferencesKey("corner_radius_dp")
        val IS_PULSING = booleanPreferencesKey("is_pulsing")
        val HAPTIC_FEEDBACK_ENABLED = booleanPreferencesKey("haptic_feedback_enabled")
        val REDUCED_MOTION_ENABLED = booleanPreferencesKey("reduced_motion_enabled")
        val PRESET_NAME = stringPreferencesKey("preset_name")
        val ACCENT_COLOR_HEX = longPreferencesKey("accent_color_hex")
        val MASCOT_ENABLED = booleanPreferencesKey("mascot_enabled")
        val MASCOT_SPRITE_NAME = stringPreferencesKey("mascot_sprite_name")
        val PILL_BACKGROUND_COLOR_HEX = longPreferencesKey("pill_background_color_hex")
        val ANIM_PHYSICS_MODE = stringPreferencesKey("anim_physics_mode")
        val ANIM_BORDER_GLOW = booleanPreferencesKey("anim_border_glow")
        val ANIM_GLOW_SPEED = floatPreferencesKey("anim_glow_speed")
        val ANIM_GLOW_THICKNESS = floatPreferencesKey("anim_glow_thickness")
        val QUICK_TOOLS_ENABLED = stringSetPreferencesKey("quick_tools_enabled")
        val CALL_ENABLED = booleanPreferencesKey("call_enabled")
        val CALL_DOUBLE_ACTIONS = booleanPreferencesKey("call_double_actions")
        val CALL_OTHER_APPS = booleanPreferencesKey("call_other_apps")
    }

    object NotificationPreferencesKeys {
        val NOTIFICATION_ENABLED = booleanPreferencesKey("notif_enabled")
        val NOTIFICATION_DURATION_MS = longPreferencesKey("notif_duration_ms")
        val NOTIFICATION_PREVIEW_MODE = stringPreferencesKey("notif_preview_mode")
        val NOTIFICATION_COMPACT_EXPAND = booleanPreferencesKey("notif_compact_expand")
        val NOTIFICATION_ALLOW_SILENT = booleanPreferencesKey("notif_allow_silent")
        val NOTIFICATION_ALLOW_ONGOING = booleanPreferencesKey("notif_allow_ongoing")
        val NOTIFICATION_SHOW_ON_LOCKSCREEN = booleanPreferencesKey("notif_show_lockscreen")
        val NOTIFICATION_HIDE_SENSITIVE = booleanPreferencesKey("notif_hide_sensitive")
        val NOTIFICATION_ANIMATION_STYLE = stringPreferencesKey("notif_anim_style")
        val NOTIFICATION_EXCLUDED_PACKAGES = stringSetPreferencesKey("notif_excluded_packages")
        val NOTIFICATION_SYNC_DISMISS_ANDROID = booleanPreferencesKey("notif_sync_dismiss_android")
        val NOTIFICATION_AUTO_EXPAND = booleanPreferencesKey("notif_auto_expand")
        val NOTIFICATION_SWIPE_DISMISS = booleanPreferencesKey("notif_swipe_dismiss_enabled")
        val NOTIFICATION_HIDE_SYSTEM = booleanPreferencesKey("notif_hide_system")
    }

    object MusicPreferencesKeys {
        val VISUALIZER_STYLE = stringPreferencesKey("music_visualizer_style")
        val ACCENT_COLOR_HEX = longPreferencesKey("music_accent_color_hex")
        val USE_ADAPTIVE_ACCENT = booleanPreferencesKey("music_use_adaptive_accent")
        val ANIMATION_SPEED = floatPreferencesKey("music_animation_speed")
        val ANIMATION_INTENSITY = floatPreferencesKey("music_animation_intensity")
        val ALBUM_ROTATION_ENABLED = booleanPreferencesKey("music_album_rotation_enabled")
        val ARTWORK_SHAPE = stringPreferencesKey("music_artwork_shape")
        val GLOW_INTENSITY = floatPreferencesKey("music_glow_intensity")
        val REDUCED_MOTION = booleanPreferencesKey("music_reduced_motion")
        val AUTO_EXPAND_DURATION_SEC = floatPreferencesKey("music_auto_expand_duration_sec")
        val IS_AUTO_EXPAND_ENABLED = booleanPreferencesKey("music_is_auto_expand_enabled")
        val BACKGROUND_BLUR_ENABLED = booleanPreferencesKey("music_bg_blur_enabled")
        val FAST_FORWARD_BUTTONS_ENABLED = booleanPreferencesKey("music_fast_forward_enabled")
        val ONLY_SHOW_WHILE_PLAYING = booleanPreferencesKey("music_only_show_while_playing")
        val MUSIC_EXCLUDED_PACKAGES = stringSetPreferencesKey("music_excluded_packages")
    }

    object BatteryPreferencesKeys {
        val BATTERY_ENABLED = booleanPreferencesKey("battery_enabled")
        val BATTERY_CHARGING_ANIM_ENABLED = booleanPreferencesKey("battery_charging_anim_enabled")
        val BATTERY_ANIM_STYLE = stringPreferencesKey("battery_anim_style")
        val BATTERY_ANIM_SPEED = floatPreferencesKey("battery_anim_speed")
        val BATTERY_ANIM_INTENSITY = floatPreferencesKey("battery_anim_intensity")
        val BATTERY_ACCENT_COLOR_HEX = longPreferencesKey("battery_accent_color_hex")
        val BATTERY_GLOW_INTENSITY = floatPreferencesKey("battery_glow_intensity")
        val BATTERY_REDUCED_MOTION = booleanPreferencesKey("battery_reduced_motion")
        val BATTERY_DISPLAY_DURATION_MS = longPreferencesKey("battery_display_duration_ms")
        val BATTERY_LOW_THRESHOLD = intPreferencesKey("battery_low_threshold")
        val BATTERY_TEMP_WARNING_ENABLED = booleanPreferencesKey("battery_temp_warning_enabled")
        val BATTERY_TEMP_WARNING_THRESHOLD_C = floatPreferencesKey("battery_temp_warning_threshold_c")
        val BATTERY_AUTO_EXPAND_ON_PLUGGED = booleanPreferencesKey("battery_auto_expand_on_plugged")
        val BATTERY_SEGMENTED_GAUGE = booleanPreferencesKey("battery_segmented_gauge")
    }

    object GesturePreferencesKeys {
        val SINGLE_TAP_ACTION = stringPreferencesKey("gesture_single_tap_action")
        val LONG_PRESS_ACTION = stringPreferencesKey("gesture_long_press_action")
        val SWIPE_LEFT_ACTION = stringPreferencesKey("gesture_swipe_left_action")
        val SWIPE_RIGHT_ACTION = stringPreferencesKey("gesture_swipe_right_action")
        val SWIPE_DOWN_ACTION = stringPreferencesKey("gesture_swipe_down_action")
        val LONG_PRESS_DURATION_MS = longPreferencesKey("gesture_long_press_duration_ms")
        val DND_AUTO_EXPAND_OVERRIDE = booleanPreferencesKey("dnd_auto_expand_override")
        val HIDE_SENSITIVE_ON_KEYGUARD = booleanPreferencesKey("hide_sensitive_on_keyguard")
        val NOTCH_DISAPPEAR_MODE = stringPreferencesKey("notch_disappear_mode")
        val EXPAND_SILENT_NOTIFICATIONS = booleanPreferencesKey("expand_silent_notifications")
    }

    object SystemEventsPreferencesKeys {
        val SYSTEM_EVENTS_ENABLED = booleanPreferencesKey("system_events_enabled")
        val RINGER_CAPSULE_ENABLED = booleanPreferencesKey("ringer_capsule_enabled")
        val SILENT_MODE_CAPSULE_ENABLED = booleanPreferencesKey("silent_mode_capsule_enabled")
        val RING_MODE_CAPSULE_ENABLED = booleanPreferencesKey("ring_mode_capsule_enabled")
        val DEVICE_UNLOCK_CAPSULE_ENABLED = booleanPreferencesKey("device_unlock_capsule_enabled")
        val FLASHLIGHT_CAPSULE_ENABLED = booleanPreferencesKey("flashlight_capsule_enabled")
        val WIFI_CAPSULE_ENABLED = booleanPreferencesKey("wifi_capsule_enabled")
        val ZEN_MODE_CAPSULE_ENABLED = booleanPreferencesKey("zen_mode_capsule_enabled")
        val BATTERY_WARNING_CAPSULE_ENABLED = booleanPreferencesKey("battery_warning_capsule_enabled")
    }

    /**
     * Immutable Flow emitting preferences stored in DataStore.
     * Catches IOExceptions cleanly and emits emptyPreferences() as safe fallback.
     */
    val preferencesFlow: Flow<IslandPreferences> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                Log.e(TAG, "Error reading preferences from DataStore, falling back to safe defaults", exception)
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            mapPreferences(preferences)
        }
        .distinctUntilChanged()

    /**
     * Immutable Flow emitting notification preferences stored in DataStore.
     */
    val notificationPreferencesFlow: Flow<NotificationPreferences> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                Log.e(TAG, "Error reading notification preferences from DataStore, falling back to safe defaults", exception)
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            mapNotificationPreferences(preferences)
        }
        .distinctUntilChanged()

    /**
     * Immutable Flow emitting music visualizer preferences stored in DataStore.
     */
    val musicPreferencesFlow: Flow<MusicPreferences> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                Log.e(TAG, "Error reading music preferences from DataStore, falling back to safe defaults", exception)
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            mapMusicPreferences(preferences)
        }
        .distinctUntilChanged()

    /**
     * Immutable Flow emitting battery preferences stored in DataStore.
     */
    val batteryPreferencesFlow: Flow<BatteryPreferences> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                Log.e(TAG, "Error reading battery preferences from DataStore, falling back to safe defaults", exception)
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            mapBatteryPreferences(preferences)
        }
        .distinctUntilChanged()

    /**
     * Immutable Flow emitting gesture and behavior preferences stored in DataStore.
     */
    val gesturePreferencesFlow: Flow<GesturePreferences> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                Log.e(TAG, "Error reading gesture preferences from DataStore, falling back to safe defaults", exception)
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            mapGesturePreferences(preferences)
        }
        .distinctUntilChanged()

    /**
     * Immutable Flow emitting Sprint 2 system-event feedback preferences.
     */
    val systemEventsPreferencesFlow: Flow<SystemEventsPreferences> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                Log.e(TAG, "Error reading system event preferences from DataStore, falling back to safe defaults", exception)
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            mapSystemEventsPreferences(preferences)
        }
        .distinctUntilChanged()

    /**
     * Live calibration StateFlow.
     * Immediately updated when sliders move to provide real-time overlay feedback
     * without thrashing disk I/O on every frame.
     */
    private val _livePreferences = MutableStateFlow(HARDWARE_DEFAULTS)
    val livePreferences: StateFlow<IslandPreferences> = _livePreferences.asStateFlow()

    private val _liveNotificationPreferences = MutableStateFlow(NotificationPreferences())
    val liveNotificationPreferences: StateFlow<NotificationPreferences> = _liveNotificationPreferences.asStateFlow()

    private val _liveMusicPreferences = MutableStateFlow(MusicPreferences.DEFAULTS)
    val liveMusicPreferences: StateFlow<MusicPreferences> = _liveMusicPreferences.asStateFlow()

    private val _liveBatteryPreferences = MutableStateFlow(BatteryPreferences())
    val liveBatteryPreferences: StateFlow<BatteryPreferences> = _liveBatteryPreferences.asStateFlow()

    private val _liveGesturePreferences = MutableStateFlow(GesturePreferences())
    val liveGesturePreferences: StateFlow<GesturePreferences> = _liveGesturePreferences.asStateFlow()

    private val _liveSystemEventsPreferences = MutableStateFlow(SystemEventsPreferences.DEFAULTS)
    val liveSystemEventsPreferences: StateFlow<SystemEventsPreferences> = _liveSystemEventsPreferences.asStateFlow()

    private var syncJob: Job? = null
    private var notificationSyncJob: Job? = null
    private var musicSyncJob: Job? = null
    private var batterySyncJob: Job? = null
    private var gestureSyncJob: Job? = null
    private var systemEventsSyncJob: Job? = null

    init {
        // Synchronize live StateFlow with persisted DataStore values on launch
        syncJob = scope.launch {
            preferencesFlow.collect { persisted ->
                _livePreferences.value = persisted
            }
        }
        notificationSyncJob = scope.launch {
            notificationPreferencesFlow.collect { persisted ->
                _liveNotificationPreferences.value = persisted
            }
        }
        musicSyncJob = scope.launch {
            musicPreferencesFlow.collect { persisted ->
                _liveMusicPreferences.value = persisted
            }
        }
        batterySyncJob = scope.launch {
            batteryPreferencesFlow.collect { persisted ->
                _liveBatteryPreferences.value = persisted
            }
        }
        gestureSyncJob = scope.launch {
            gesturePreferencesFlow.collect { persisted ->
                _liveGesturePreferences.value = persisted
            }
        }
        systemEventsSyncJob = scope.launch {
            systemEventsPreferencesFlow.collect { persisted ->
                _liveSystemEventsPreferences.value = persisted
            }
        }
    }

    /**
     * Cancels background collectors and synchronizers. Used in testing or component teardown.
     */
    fun close() {
        syncJob?.cancel()
        notificationSyncJob?.cancel()
        musicSyncJob?.cancel()
        batterySyncJob?.cancel()
        gestureSyncJob?.cancel()
        systemEventsSyncJob?.cancel()
    }

    private fun mapPreferences(preferences: Preferences): IslandPreferences {
        return IslandPreferences(
            compactWidthDp = preferences[PreferencesKeys.COMPACT_WIDTH_DP] ?: HARDWARE_DEFAULTS.compactWidthDp,
            compactHeightDp = preferences[PreferencesKeys.COMPACT_HEIGHT_DP] ?: HARDWARE_DEFAULTS.compactHeightDp,
            expandedWidthDp = preferences[PreferencesKeys.EXPANDED_WIDTH_DP] ?: HARDWARE_DEFAULTS.expandedWidthDp,
            expandedHeightDp = preferences[PreferencesKeys.EXPANDED_HEIGHT_DP] ?: HARDWARE_DEFAULTS.expandedHeightDp,
            horizontalOffsetDp = preferences[PreferencesKeys.HORIZONTAL_OFFSET_DP] ?: HARDWARE_DEFAULTS.horizontalOffsetDp,
            topMarginDp = preferences[PreferencesKeys.TOP_MARGIN_DP] ?: HARDWARE_DEFAULTS.topMarginDp,
            cornerRadiusDp = preferences[PreferencesKeys.CORNER_RADIUS_DP] ?: HARDWARE_DEFAULTS.cornerRadiusDp,
            isPulsing = preferences[PreferencesKeys.IS_PULSING] ?: HARDWARE_DEFAULTS.isPulsing,
            hapticFeedbackEnabled = preferences[PreferencesKeys.HAPTIC_FEEDBACK_ENABLED] ?: HARDWARE_DEFAULTS.hapticFeedbackEnabled,
            reducedMotionEnabled = preferences[PreferencesKeys.REDUCED_MOTION_ENABLED] ?: HARDWARE_DEFAULTS.reducedMotionEnabled,
            presetName = preferences[PreferencesKeys.PRESET_NAME] ?: HARDWARE_DEFAULTS.presetName,
            accentColorHex = preferences[PreferencesKeys.ACCENT_COLOR_HEX] ?: HARDWARE_DEFAULTS.accentColorHex,
            mascotEnabled = preferences[PreferencesKeys.MASCOT_ENABLED] ?: HARDWARE_DEFAULTS.mascotEnabled,
            mascotSpriteName = preferences[PreferencesKeys.MASCOT_SPRITE_NAME] ?: HARDWARE_DEFAULTS.mascotSpriteName,
            pillBackgroundColorHex = preferences[PreferencesKeys.PILL_BACKGROUND_COLOR_HEX] ?: HARDWARE_DEFAULTS.pillBackgroundColorHex
        )
    }

    suspend fun updatePillTheme(backgroundHex: Long, accentHex: Long, presetName: String) {
        _livePreferences.value = _livePreferences.value.copy(
            pillBackgroundColorHex = backgroundHex,
            accentColorHex = accentHex,
            presetName = presetName,
        )
        dataStore.edit { prefs ->
            prefs[PreferencesKeys.PILL_BACKGROUND_COLOR_HEX] = backgroundHex
            prefs[PreferencesKeys.ACCENT_COLOR_HEX] = accentHex
            prefs[PreferencesKeys.PRESET_NAME] = presetName
        }
    }

    suspend fun updateAnimation(prefs: com.nothing.island.ai.ui.AnimationPreferences) {
        dataStore.edit { p ->
            p[PreferencesKeys.ANIM_PHYSICS_MODE] = prefs.physicsMode
            p[PreferencesKeys.ANIM_BORDER_GLOW] = prefs.borderGlowEnabled
            p[PreferencesKeys.ANIM_GLOW_SPEED] = prefs.glowSpeed
            p[PreferencesKeys.ANIM_GLOW_THICKNESS] = prefs.glowThickness
        }
    }

    fun animationPreferencesFlow(): kotlinx.coroutines.flow.Flow<com.nothing.island.ai.ui.AnimationPreferences> =
        dataStore.data.map { p ->
            com.nothing.island.ai.ui.AnimationPreferences(
                physicsMode = p[PreferencesKeys.ANIM_PHYSICS_MODE] ?: "LIQUID",
                borderGlowEnabled = p[PreferencesKeys.ANIM_BORDER_GLOW] ?: true,
                glowSpeed = p[PreferencesKeys.ANIM_GLOW_SPEED] ?: 1.2f,
                glowThickness = p[PreferencesKeys.ANIM_GLOW_THICKNESS] ?: 2.0f,
            )
        }

    /** Call studio settings. */
    fun callPreferencesFlow(): kotlinx.coroutines.flow.Flow<com.nothing.island.ai.telephony.CallPreferences> =
        dataStore.data.map { p ->
            com.nothing.island.ai.telephony.CallPreferences(
                systemCallEnabled = p[PreferencesKeys.CALL_ENABLED] ?: true,
                doubleIncomingCallActions = p[PreferencesKeys.CALL_DOUBLE_ACTIONS] ?: true,
                otherAppsCalls = p[PreferencesKeys.CALL_OTHER_APPS] ?: true,
            )
        }

    suspend fun updateCallPreferences(prefs: com.nothing.island.ai.telephony.CallPreferences) {
        dataStore.edit { p ->
            p[PreferencesKeys.CALL_ENABLED] = prefs.systemCallEnabled
            p[PreferencesKeys.CALL_DOUBLE_ACTIONS] = prefs.doubleIncomingCallActions
            p[PreferencesKeys.CALL_OTHER_APPS] = prefs.otherAppsCalls
        }
    }

    /** Enabled quick-tool ids shown in the island's quick controls drawer. */
    fun quickToolsFlow(): kotlinx.coroutines.flow.Flow<Set<String>> =
        dataStore.data.map { p ->
            p[PreferencesKeys.QUICK_TOOLS_ENABLED]
                ?: com.nothing.island.ai.ui.components.QuickTools.DEFAULT.toSet()
        }

    suspend fun updateQuickTools(enabled: Set<String>) {
        dataStore.edit { p -> p[PreferencesKeys.QUICK_TOOLS_ENABLED] = enabled }
    }

    suspend fun updateMascot(enabled: Boolean, spriteName: String) {
        _livePreferences.value = _livePreferences.value.copy(
            mascotEnabled = enabled,
            mascotSpriteName = spriteName,
        )
        dataStore.edit { prefs ->
            prefs[PreferencesKeys.MASCOT_ENABLED] = enabled
            prefs[PreferencesKeys.MASCOT_SPRITE_NAME] = spriteName
        }
    }

    /**
     * Updates in-memory live calibration immediately for zero-latency overlay rendering.
     */
    fun updateLiveCalibration(
        compactWidthDp: Float = _livePreferences.value.compactWidthDp,
        compactHeightDp: Float = _livePreferences.value.compactHeightDp,
        cornerRadiusDp: Float = _livePreferences.value.cornerRadiusDp,
        topMarginDp: Float = _livePreferences.value.topMarginDp,
        horizontalOffsetDp: Float = _livePreferences.value.horizontalOffsetDp,
        accentColorHex: Long = _livePreferences.value.accentColorHex,
        presetName: String = _livePreferences.value.presetName
    ) {
        _livePreferences.value = _livePreferences.value.copy(
            compactWidthDp = compactWidthDp,
            compactHeightDp = compactHeightDp,
            cornerRadiusDp = cornerRadiusDp,
            topMarginDp = topMarginDp,
            horizontalOffsetDp = horizontalOffsetDp,
            accentColorHex = accentColorHex,
            presetName = presetName
        )
    }

    /**
     * Persists the current in-memory live calibration values to DataStore.
     * Invoked when slider dragging gestures complete (onValueChangeFinished).
     */
    suspend fun persistCurrentCalibration() {
        val current = _livePreferences.value
        saveCalibration(
            compactWidthDp = current.compactWidthDp,
            compactHeightDp = current.compactHeightDp,
            cornerRadiusDp = current.cornerRadiusDp,
            topMarginDp = current.topMarginDp,
            horizontalOffsetDp = current.horizontalOffsetDp,
            accentColorHex = current.accentColorHex,
            presetName = current.presetName
        )
    }

    /**
     * Persists calibration geometry and styling to DataStore.
     */
    suspend fun saveCalibration(
        compactWidthDp: Float,
        compactHeightDp: Float,
        cornerRadiusDp: Float,
        topMarginDp: Float,
        horizontalOffsetDp: Float,
        accentColorHex: Long = _livePreferences.value.accentColorHex,
        presetName: String = _livePreferences.value.presetName
    ) {
        try {
            dataStore.edit { preferences ->
                preferences[PreferencesKeys.COMPACT_WIDTH_DP] = compactWidthDp
                preferences[PreferencesKeys.COMPACT_HEIGHT_DP] = compactHeightDp
                preferences[PreferencesKeys.CORNER_RADIUS_DP] = cornerRadiusDp
                preferences[PreferencesKeys.TOP_MARGIN_DP] = topMarginDp
                preferences[PreferencesKeys.HORIZONTAL_OFFSET_DP] = horizontalOffsetDp
                preferences[PreferencesKeys.ACCENT_COLOR_HEX] = accentColorHex
                preferences[PreferencesKeys.PRESET_NAME] = presetName
            }
        } catch (e: IOException) {
            Log.e(TAG, "Failed to persist calibration to DataStore", e)
        }
    }

    /**
     * Persists pulse setting to DataStore and updates live state.
     */
    suspend fun updatePulseEnabled(enabled: Boolean) {
        _livePreferences.value = _livePreferences.value.copy(isPulsing = enabled)
        try {
            dataStore.edit { preferences ->
                preferences[PreferencesKeys.IS_PULSING] = enabled
            }
        } catch (e: IOException) {
            Log.e(TAG, "Failed to persist isPulsing to DataStore", e)
        }
    }

    /**
     * Persists haptic feedback setting to DataStore.
     */
    suspend fun updateHapticFeedback(enabled: Boolean) {
        _livePreferences.value = _livePreferences.value.copy(hapticFeedbackEnabled = enabled)
        try {
            dataStore.edit { preferences ->
                preferences[PreferencesKeys.HAPTIC_FEEDBACK_ENABLED] = enabled
            }
        } catch (e: IOException) {
            Log.e(TAG, "Failed to persist hapticFeedbackEnabled to DataStore", e)
        }
    }

    /**
     * Persists reduced motion setting to DataStore.
     */
    suspend fun updateReducedMotion(enabled: Boolean) {
        _livePreferences.value = _livePreferences.value.copy(reducedMotionEnabled = enabled)
        try {
            dataStore.edit { preferences ->
                preferences[PreferencesKeys.REDUCED_MOTION_ENABLED] = enabled
            }
        } catch (e: IOException) {
            Log.e(TAG, "Failed to persist reducedMotionEnabled to DataStore", e)
        }
    }

    /**
     * Applies and persists a full design preset.
     */
    suspend fun applyPreset(
        presetName: String,
        widthDp: Float,
        heightDp: Float,
        cornerRadiusDp: Float,
        accentColorHex: Long
    ) {
        _livePreferences.value = _livePreferences.value.copy(
            presetName = presetName,
            compactWidthDp = widthDp,
            compactHeightDp = heightDp,
            cornerRadiusDp = cornerRadiusDp,
            accentColorHex = accentColorHex
        )
        try {
            dataStore.edit { preferences ->
                preferences[PreferencesKeys.PRESET_NAME] = presetName
                preferences[PreferencesKeys.COMPACT_WIDTH_DP] = widthDp
                preferences[PreferencesKeys.COMPACT_HEIGHT_DP] = heightDp
                preferences[PreferencesKeys.CORNER_RADIUS_DP] = cornerRadiusDp
                preferences[PreferencesKeys.ACCENT_COLOR_HEX] = accentColorHex
            }
        } catch (e: IOException) {
            Log.e(TAG, "Failed to persist preset to DataStore", e)
        }
    }

    /**
     * Resets all calibration and appearance settings back to the hardware-verified
     * baseline for Nothing Phone (4a) A069.
     */
    suspend fun resetToHardwareDefaults() {
        _livePreferences.value = HARDWARE_DEFAULTS
        try {
            dataStore.edit { preferences ->
                preferences.clear()
            }
        } catch (e: IOException) {
            Log.e(TAG, "Failed to reset preferences in DataStore", e)
        }
    }

    private fun mapNotificationPreferences(preferences: Preferences): NotificationPreferences {
        val previewModeStr = preferences[NotificationPreferencesKeys.NOTIFICATION_PREVIEW_MODE] ?: NotificationPreviewMode.FULL.name
        val previewMode = try {
            NotificationPreviewMode.valueOf(previewModeStr)
        } catch (_: Exception) {
            NotificationPreviewMode.FULL
        }

        val animStyleStr = preferences[NotificationPreferencesKeys.NOTIFICATION_ANIMATION_STYLE] ?: NotificationAnimationStyle.NOTHING_SNAP.name
        val animStyle = try {
            NotificationAnimationStyle.valueOf(animStyleStr)
        } catch (_: Exception) {
            NotificationAnimationStyle.NOTHING_SNAP
        }

        return NotificationPreferences(
            enabled = preferences[NotificationPreferencesKeys.NOTIFICATION_ENABLED] ?: true,
            displayDurationMs = preferences[NotificationPreferencesKeys.NOTIFICATION_DURATION_MS] ?: 4500L,
            previewMode = previewMode,
            compactExpansionEnabled = preferences[NotificationPreferencesKeys.NOTIFICATION_COMPACT_EXPAND] ?: true,
            autoExpandOnNotification = preferences[NotificationPreferencesKeys.NOTIFICATION_AUTO_EXPAND] ?: true,
            allowSilentNotifications = preferences[NotificationPreferencesKeys.NOTIFICATION_ALLOW_SILENT] ?: false,
            allowOngoingNotifications = preferences[NotificationPreferencesKeys.NOTIFICATION_ALLOW_ONGOING] ?: false,
            showOnLockscreen = preferences[NotificationPreferencesKeys.NOTIFICATION_SHOW_ON_LOCKSCREEN] ?: false,
            hideSensitiveContent = preferences[NotificationPreferencesKeys.NOTIFICATION_HIDE_SENSITIVE] ?: true,
            animationStyle = animStyle,
            excludedPackages = preferences[NotificationPreferencesKeys.NOTIFICATION_EXCLUDED_PACKAGES] ?: emptySet(),
            syncDismissWithAndroid = preferences[NotificationPreferencesKeys.NOTIFICATION_SYNC_DISMISS_ANDROID] ?: false,
            swipeToDismissEnabled = preferences[NotificationPreferencesKeys.NOTIFICATION_SWIPE_DISMISS] ?: true,
            hideSystemNotification = preferences[NotificationPreferencesKeys.NOTIFICATION_HIDE_SYSTEM] ?: false
        )
    }

    suspend fun updateNotificationSwipeDismiss(enabled: Boolean) {
        _liveNotificationPreferences.value = _liveNotificationPreferences.value.copy(swipeToDismissEnabled = enabled)
        dataStore.edit { it[NotificationPreferencesKeys.NOTIFICATION_SWIPE_DISMISS] = enabled }
    }

    suspend fun updateSyncDismissWithAndroid(enabled: Boolean) {
        _liveNotificationPreferences.value = _liveNotificationPreferences.value.copy(syncDismissWithAndroid = enabled)
        dataStore.edit { it[NotificationPreferencesKeys.NOTIFICATION_SYNC_DISMISS_ANDROID] = enabled }
    }

    suspend fun updateHideSystemNotification(enabled: Boolean) {
        _liveNotificationPreferences.value = _liveNotificationPreferences.value.copy(hideSystemNotification = enabled)
        dataStore.edit { it[NotificationPreferencesKeys.NOTIFICATION_HIDE_SYSTEM] = enabled }
    }

    suspend fun updateNotificationEnabled(enabled: Boolean) {
        dataStore.edit { it[NotificationPreferencesKeys.NOTIFICATION_ENABLED] = enabled }
    }

    suspend fun updateNotificationDuration(durationMs: Long) {
        dataStore.edit { it[NotificationPreferencesKeys.NOTIFICATION_DURATION_MS] = durationMs }
    }

    suspend fun updateNotificationPreviewMode(mode: NotificationPreviewMode) {
        dataStore.edit { it[NotificationPreferencesKeys.NOTIFICATION_PREVIEW_MODE] = mode.name }
    }

    suspend fun updateNotificationCompactExpand(enabled: Boolean) {
        dataStore.edit { it[NotificationPreferencesKeys.NOTIFICATION_COMPACT_EXPAND] = enabled }
    }

    /**
     * R7.1: Enables/disables notification-triggered Island auto-expansion.
     * Independent of the music auto-expansion preference.
     */
    suspend fun updateNotificationAutoExpand(enabled: Boolean) {
        _liveNotificationPreferences.value = _liveNotificationPreferences.value.copy(autoExpandOnNotification = enabled)
        dataStore.edit { it[NotificationPreferencesKeys.NOTIFICATION_AUTO_EXPAND] = enabled }
    }

    suspend fun updateNotificationAllowSilent(allowed: Boolean) {
        dataStore.edit { it[NotificationPreferencesKeys.NOTIFICATION_ALLOW_SILENT] = allowed }
    }

    suspend fun updateNotificationAllowOngoing(allowed: Boolean) {
        dataStore.edit { it[NotificationPreferencesKeys.NOTIFICATION_ALLOW_ONGOING] = allowed }
    }

    suspend fun updateNotificationShowOnLockscreen(allowed: Boolean) {
        dataStore.edit { it[NotificationPreferencesKeys.NOTIFICATION_SHOW_ON_LOCKSCREEN] = allowed }
    }

    suspend fun updateNotificationHideSensitive(hide: Boolean) {
        dataStore.edit { it[NotificationPreferencesKeys.NOTIFICATION_HIDE_SENSITIVE] = hide }
    }

    suspend fun updateNotificationExcludedPackages(packages: Set<String>) {
        dataStore.edit { it[NotificationPreferencesKeys.NOTIFICATION_EXCLUDED_PACKAGES] = packages }
    }

    suspend fun updateNotificationAnimationStyle(style: NotificationAnimationStyle) {
        dataStore.edit { it[NotificationPreferencesKeys.NOTIFICATION_ANIMATION_STYLE] = style.name }
    }

    suspend fun toggleNotificationPackageExclusion(packageName: String) {
        dataStore.edit { prefs ->
            val current = prefs[NotificationPreferencesKeys.NOTIFICATION_EXCLUDED_PACKAGES] ?: emptySet()
            val updated = if (current.contains(packageName)) current - packageName else current + packageName
            prefs[NotificationPreferencesKeys.NOTIFICATION_EXCLUDED_PACKAGES] = updated
        }
    }

    private fun mapMusicPreferences(preferences: Preferences): MusicPreferences {
        val styleStr = preferences[MusicPreferencesKeys.VISUALIZER_STYLE] ?: MusicVisualizerStyle.VERTICAL_EQUALIZER.name
        val style = try {
            MusicVisualizerStyle.valueOf(styleStr)
        } catch (_: Exception) {
            MusicVisualizerStyle.VERTICAL_EQUALIZER
        }

        val shapeStr = preferences[MusicPreferencesKeys.ARTWORK_SHAPE] ?: ArtworkShape.CIRCLE.name
        val shape = try {
            ArtworkShape.valueOf(shapeStr)
        } catch (_: Exception) {
            ArtworkShape.CIRCLE
        }

        return MusicPreferences(
            visualizerStyle = style,
            accentColorHex = preferences[MusicPreferencesKeys.ACCENT_COLOR_HEX] ?: MusicPreferences.DEFAULTS.accentColorHex,
            useAdaptiveAccent = preferences[MusicPreferencesKeys.USE_ADAPTIVE_ACCENT] ?: MusicPreferences.DEFAULTS.useAdaptiveAccent,
            animationSpeed = preferences[MusicPreferencesKeys.ANIMATION_SPEED] ?: MusicPreferences.DEFAULTS.animationSpeed,
            animationIntensity = preferences[MusicPreferencesKeys.ANIMATION_INTENSITY] ?: MusicPreferences.DEFAULTS.animationIntensity,
            albumRotationEnabled = preferences[MusicPreferencesKeys.ALBUM_ROTATION_ENABLED] ?: MusicPreferences.DEFAULTS.albumRotationEnabled,
            artworkShape = shape,
            glowIntensity = preferences[MusicPreferencesKeys.GLOW_INTENSITY] ?: MusicPreferences.DEFAULTS.glowIntensity,
            reducedMotion = preferences[MusicPreferencesKeys.REDUCED_MOTION] ?: MusicPreferences.DEFAULTS.reducedMotion,
            autoExpandDurationSeconds = preferences[MusicPreferencesKeys.AUTO_EXPAND_DURATION_SEC] ?: MusicPreferences.DEFAULTS.autoExpandDurationSeconds,
            isAutoExpandEnabled = preferences[MusicPreferencesKeys.IS_AUTO_EXPAND_ENABLED] ?: MusicPreferences.DEFAULTS.isAutoExpandEnabled,
            backgroundBlurEnabled = preferences[MusicPreferencesKeys.BACKGROUND_BLUR_ENABLED] ?: MusicPreferences.DEFAULTS.backgroundBlurEnabled,
            fastForwardButtonsEnabled = preferences[MusicPreferencesKeys.FAST_FORWARD_BUTTONS_ENABLED] ?: MusicPreferences.DEFAULTS.fastForwardButtonsEnabled,
            onlyShowWhilePlaying = preferences[MusicPreferencesKeys.ONLY_SHOW_WHILE_PLAYING] ?: MusicPreferences.DEFAULTS.onlyShowWhilePlaying,
            excludedPackages = preferences[MusicPreferencesKeys.MUSIC_EXCLUDED_PACKAGES] ?: emptySet()
        )
    }

    /**
     * Updates in-memory live music preferences immediately for live preview without disk thrashing.
     */
    fun updateLiveMusicPreferences(prefs: MusicPreferences) {
        _liveMusicPreferences.value = prefs
    }

    /**
     * Persists updated music preferences to DataStore.
     */
    suspend fun updateMusicPreferences(prefs: MusicPreferences) {
        _liveMusicPreferences.value = prefs
        try {
            dataStore.edit { p ->
                p[MusicPreferencesKeys.VISUALIZER_STYLE] = prefs.visualizerStyle.name
                p[MusicPreferencesKeys.ACCENT_COLOR_HEX] = prefs.accentColorHex
                p[MusicPreferencesKeys.USE_ADAPTIVE_ACCENT] = prefs.useAdaptiveAccent
                p[MusicPreferencesKeys.ANIMATION_SPEED] = prefs.animationSpeed
                p[MusicPreferencesKeys.ANIMATION_INTENSITY] = prefs.animationIntensity
                p[MusicPreferencesKeys.ALBUM_ROTATION_ENABLED] = prefs.albumRotationEnabled
                p[MusicPreferencesKeys.ARTWORK_SHAPE] = prefs.artworkShape.name
                p[MusicPreferencesKeys.GLOW_INTENSITY] = prefs.glowIntensity
                p[MusicPreferencesKeys.REDUCED_MOTION] = prefs.reducedMotion
                p[MusicPreferencesKeys.AUTO_EXPAND_DURATION_SEC] = prefs.autoExpandDurationSeconds
                p[MusicPreferencesKeys.IS_AUTO_EXPAND_ENABLED] = prefs.isAutoExpandEnabled
                p[MusicPreferencesKeys.BACKGROUND_BLUR_ENABLED] = prefs.backgroundBlurEnabled
                p[MusicPreferencesKeys.FAST_FORWARD_BUTTONS_ENABLED] = prefs.fastForwardButtonsEnabled
                p[MusicPreferencesKeys.ONLY_SHOW_WHILE_PLAYING] = prefs.onlyShowWhilePlaying
                p[MusicPreferencesKeys.MUSIC_EXCLUDED_PACKAGES] = prefs.excludedPackages
            }
        } catch (e: IOException) {
            Log.e(TAG, "Failed to persist music preferences to DataStore", e)
        }
    }

    suspend fun updateMusicBackgroundBlur(enabled: Boolean) {
        val updated = _liveMusicPreferences.value.copy(backgroundBlurEnabled = enabled)
        updateMusicPreferences(updated)
    }

    suspend fun updateMusicFastForwardButtons(enabled: Boolean) {
        val updated = _liveMusicPreferences.value.copy(fastForwardButtonsEnabled = enabled)
        updateMusicPreferences(updated)
    }

    suspend fun updateMusicOnlyShowWhilePlaying(enabled: Boolean) {
        val updated = _liveMusicPreferences.value.copy(onlyShowWhilePlaying = enabled)
        updateMusicPreferences(updated)
    }

    suspend fun updateMusicExcludedPackages(packages: Set<String>) {
        val updated = _liveMusicPreferences.value.copy(excludedPackages = packages)
        updateMusicPreferences(updated)
    }

    suspend fun toggleMusicPackageExclusion(packageName: String) {
        val current = _liveMusicPreferences.value.excludedPackages
        val updatedSet = if (current.contains(packageName)) current - packageName else current + packageName
        val updated = _liveMusicPreferences.value.copy(excludedPackages = updatedSet)
        updateMusicPreferences(updated)
    }

    /**
     * Resets music preferences back to default settings (Vertical Equalizer, Nothing Red/Purple adaptive).
     */
    suspend fun resetMusicPreferencesToDefaults() {
        val defaults = MusicPreferences.DEFAULTS
        _liveMusicPreferences.value = defaults
        updateMusicPreferences(defaults)
    }

    private fun mapBatteryPreferences(preferences: Preferences): BatteryPreferences {
        val styleStr = preferences[BatteryPreferencesKeys.BATTERY_ANIM_STYLE] ?: ChargingAnimationStyle.ENERGY_RING.name
        val style = try {
            ChargingAnimationStyle.valueOf(styleStr)
        } catch (_: Exception) {
            ChargingAnimationStyle.ENERGY_RING
        }

        return BatteryPreferences(
            enabled = preferences[BatteryPreferencesKeys.BATTERY_ENABLED] ?: true,
            chargingAnimationEnabled = preferences[BatteryPreferencesKeys.BATTERY_CHARGING_ANIM_ENABLED] ?: true,
            animationStyle = style,
            animationSpeed = preferences[BatteryPreferencesKeys.BATTERY_ANIM_SPEED] ?: 1.0f,
            animationIntensity = preferences[BatteryPreferencesKeys.BATTERY_ANIM_INTENSITY] ?: 1.0f,
            accentColorHex = preferences[BatteryPreferencesKeys.BATTERY_ACCENT_COLOR_HEX] ?: 0xFFD71920L,
            glowIntensity = preferences[BatteryPreferencesKeys.BATTERY_GLOW_INTENSITY] ?: 0.6f,
            reducedMotion = preferences[BatteryPreferencesKeys.BATTERY_REDUCED_MOTION] ?: false,
            displayDurationMs = preferences[BatteryPreferencesKeys.BATTERY_DISPLAY_DURATION_MS] ?: 4500L,
            lowBatteryThreshold = preferences[BatteryPreferencesKeys.BATTERY_LOW_THRESHOLD] ?: 20,
            temperatureWarningEnabled = preferences[BatteryPreferencesKeys.BATTERY_TEMP_WARNING_ENABLED] ?: true,
            temperatureWarningThresholdC = preferences[BatteryPreferencesKeys.BATTERY_TEMP_WARNING_THRESHOLD_C] ?: 45.0f,
            autoExpandOnPlugged = preferences[BatteryPreferencesKeys.BATTERY_AUTO_EXPAND_ON_PLUGGED] ?: true,
            showSegmentedBatteryGauge = preferences[BatteryPreferencesKeys.BATTERY_SEGMENTED_GAUGE] ?: true
        )
    }

    /**
     * Updates in-memory live battery preferences immediately for live preview without disk thrashing.
     */
    fun updateLiveBatteryPreferences(prefs: BatteryPreferences) {
        _liveBatteryPreferences.value = prefs
    }

    /**
     * Persists updated battery preferences to DataStore.
     */
    suspend fun updateBatteryPreferences(prefs: BatteryPreferences) {
        _liveBatteryPreferences.value = prefs
        try {
            dataStore.edit { p ->
                p[BatteryPreferencesKeys.BATTERY_ENABLED] = prefs.enabled
                p[BatteryPreferencesKeys.BATTERY_CHARGING_ANIM_ENABLED] = prefs.chargingAnimationEnabled
                p[BatteryPreferencesKeys.BATTERY_ANIM_STYLE] = prefs.animationStyle.name
                p[BatteryPreferencesKeys.BATTERY_ANIM_SPEED] = prefs.animationSpeed
                p[BatteryPreferencesKeys.BATTERY_ANIM_INTENSITY] = prefs.animationIntensity
                p[BatteryPreferencesKeys.BATTERY_ACCENT_COLOR_HEX] = prefs.accentColorHex
                p[BatteryPreferencesKeys.BATTERY_GLOW_INTENSITY] = prefs.glowIntensity
                p[BatteryPreferencesKeys.BATTERY_REDUCED_MOTION] = prefs.reducedMotion
                p[BatteryPreferencesKeys.BATTERY_DISPLAY_DURATION_MS] = prefs.displayDurationMs
                p[BatteryPreferencesKeys.BATTERY_LOW_THRESHOLD] = prefs.lowBatteryThreshold
                p[BatteryPreferencesKeys.BATTERY_TEMP_WARNING_ENABLED] = prefs.temperatureWarningEnabled
                p[BatteryPreferencesKeys.BATTERY_TEMP_WARNING_THRESHOLD_C] = prefs.temperatureWarningThresholdC
                p[BatteryPreferencesKeys.BATTERY_AUTO_EXPAND_ON_PLUGGED] = prefs.autoExpandOnPlugged
                p[BatteryPreferencesKeys.BATTERY_SEGMENTED_GAUGE] = prefs.showSegmentedBatteryGauge
            }
        } catch (e: IOException) {
            Log.e(TAG, "Failed to persist battery preferences to DataStore", e)
        }
    }

    suspend fun updateBatteryEnabled(enabled: Boolean) {
        dataStore.edit { it[BatteryPreferencesKeys.BATTERY_ENABLED] = enabled }
    }

    suspend fun updateBatteryAutoExpandOnPlugged(enabled: Boolean) {
        dataStore.edit { it[BatteryPreferencesKeys.BATTERY_AUTO_EXPAND_ON_PLUGGED] = enabled }
    }

    suspend fun updateBatteryTempWarningEnabled(enabled: Boolean) {
        dataStore.edit { it[BatteryPreferencesKeys.BATTERY_TEMP_WARNING_ENABLED] = enabled }
    }

    suspend fun updateBatteryLowThreshold(threshold: Int) {
        dataStore.edit { it[BatteryPreferencesKeys.BATTERY_LOW_THRESHOLD] = threshold }
    }

    /**
     * Resets battery preferences back to default settings (Energy Ring, Nothing Red, 4.5s display duration).
     */
    suspend fun resetBatteryPreferencesToDefaults() {
        val defaults = BatteryPreferences()
        _liveBatteryPreferences.value = defaults
        updateBatteryPreferences(defaults)
    }

    private fun mapGesturePreferences(preferences: Preferences): GesturePreferences {
        val singleTap = try {
            GestureAction.valueOf(preferences[GesturePreferencesKeys.SINGLE_TAP_ACTION] ?: GestureAction.EXPAND_COLLAPSE.name)
        } catch (_: Exception) {
            GestureAction.EXPAND_COLLAPSE
        }
        val longPress = try {
            GestureAction.valueOf(preferences[GesturePreferencesKeys.LONG_PRESS_ACTION] ?: GestureAction.NOTIFICATIONS_HUB.name)
        } catch (_: Exception) {
            GestureAction.NOTIFICATIONS_HUB
        }
        val swipeLeft = try {
            GestureAction.valueOf(preferences[GesturePreferencesKeys.SWIPE_LEFT_ACTION] ?: GestureAction.NONE.name)
        } catch (_: Exception) {
            GestureAction.NONE
        }
        val swipeRight = try {
            GestureAction.valueOf(preferences[GesturePreferencesKeys.SWIPE_RIGHT_ACTION] ?: GestureAction.NONE.name)
        } catch (_: Exception) {
            GestureAction.NONE
        }
        val swipeDown = try {
            GestureAction.valueOf(preferences[GesturePreferencesKeys.SWIPE_DOWN_ACTION] ?: GestureAction.NONE.name)
        } catch (_: Exception) {
            GestureAction.NONE
        }
        val disappearMode = try {
            NotchDisappearMode.valueOf(preferences[GesturePreferencesKeys.NOTCH_DISAPPEAR_MODE] ?: NotchDisappearMode.AUTO.name)
        } catch (_: Exception) {
            NotchDisappearMode.AUTO
        }

        return GesturePreferences(
            singleTapAction = singleTap,
            longPressAction = longPress,
            swipeLeftAction = swipeLeft,
            swipeRightAction = swipeRight,
            swipeDownAction = swipeDown,
            longPressDurationMs = preferences[GesturePreferencesKeys.LONG_PRESS_DURATION_MS] ?: 500L,
            dndAutoExpandOverride = preferences[GesturePreferencesKeys.DND_AUTO_EXPAND_OVERRIDE] ?: false,
            hideSensitiveOnKeyguard = preferences[GesturePreferencesKeys.HIDE_SENSITIVE_ON_KEYGUARD] ?: true,
            notchDisappearMode = disappearMode,
            expandSilentNotifications = preferences[GesturePreferencesKeys.EXPAND_SILENT_NOTIFICATIONS] ?: false
        )
    }

    /**
     * Updates in-memory live gesture preferences immediately for zero-latency feedback.
     */
    fun updateLiveGesturePreferences(prefs: GesturePreferences) {
        _liveGesturePreferences.value = prefs
    }

    /**
     * Persists updated gesture preferences to DataStore.
     */
    suspend fun updateGesturePreferences(prefs: GesturePreferences) {
        _liveGesturePreferences.value = prefs
        try {
            dataStore.edit { p ->
                p[GesturePreferencesKeys.SINGLE_TAP_ACTION] = prefs.singleTapAction.name
                p[GesturePreferencesKeys.LONG_PRESS_ACTION] = prefs.longPressAction.name
                p[GesturePreferencesKeys.SWIPE_LEFT_ACTION] = prefs.swipeLeftAction.name
                p[GesturePreferencesKeys.SWIPE_RIGHT_ACTION] = prefs.swipeRightAction.name
                p[GesturePreferencesKeys.SWIPE_DOWN_ACTION] = prefs.swipeDownAction.name
                p[GesturePreferencesKeys.LONG_PRESS_DURATION_MS] = prefs.longPressDurationMs
                p[GesturePreferencesKeys.DND_AUTO_EXPAND_OVERRIDE] = prefs.dndAutoExpandOverride
                p[GesturePreferencesKeys.HIDE_SENSITIVE_ON_KEYGUARD] = prefs.hideSensitiveOnKeyguard
                p[GesturePreferencesKeys.NOTCH_DISAPPEAR_MODE] = prefs.notchDisappearMode.name
                p[GesturePreferencesKeys.EXPAND_SILENT_NOTIFICATIONS] = prefs.expandSilentNotifications
            }
        } catch (e: IOException) {
            Log.e(TAG, "Failed to persist gesture preferences to DataStore", e)
        }
    }

    private fun mapSystemEventsPreferences(preferences: Preferences): SystemEventsPreferences {
        val defaults = SystemEventsPreferences.DEFAULTS
        return SystemEventsPreferences(
            systemEventsEnabled = preferences[SystemEventsPreferencesKeys.SYSTEM_EVENTS_ENABLED] ?: defaults.systemEventsEnabled,
            ringerCapsuleEnabled = preferences[SystemEventsPreferencesKeys.RINGER_CAPSULE_ENABLED] ?: defaults.ringerCapsuleEnabled,
            silentModeCapsuleEnabled = preferences[SystemEventsPreferencesKeys.SILENT_MODE_CAPSULE_ENABLED] ?: defaults.silentModeCapsuleEnabled,
            ringModeCapsuleEnabled = preferences[SystemEventsPreferencesKeys.RING_MODE_CAPSULE_ENABLED] ?: defaults.ringModeCapsuleEnabled,
            deviceUnlockCapsuleEnabled = preferences[SystemEventsPreferencesKeys.DEVICE_UNLOCK_CAPSULE_ENABLED] ?: defaults.deviceUnlockCapsuleEnabled,
            flashlightCapsuleEnabled = preferences[SystemEventsPreferencesKeys.FLASHLIGHT_CAPSULE_ENABLED] ?: defaults.flashlightCapsuleEnabled,
            wifiCapsuleEnabled = preferences[SystemEventsPreferencesKeys.WIFI_CAPSULE_ENABLED] ?: defaults.wifiCapsuleEnabled,
            zenModeCapsuleEnabled = preferences[SystemEventsPreferencesKeys.ZEN_MODE_CAPSULE_ENABLED] ?: defaults.zenModeCapsuleEnabled,
            batteryWarningCapsuleEnabled = preferences[SystemEventsPreferencesKeys.BATTERY_WARNING_CAPSULE_ENABLED] ?: defaults.batteryWarningCapsuleEnabled,
        )
    }

    fun updateLiveSystemEventsPreferences(prefs: SystemEventsPreferences) {
        _liveSystemEventsPreferences.value = prefs
    }

    suspend fun updateSystemEventsPreferences(prefs: SystemEventsPreferences) {
        _liveSystemEventsPreferences.value = prefs
        try {
            dataStore.edit { p ->
                p[SystemEventsPreferencesKeys.SYSTEM_EVENTS_ENABLED] = prefs.systemEventsEnabled
                p[SystemEventsPreferencesKeys.RINGER_CAPSULE_ENABLED] = prefs.ringerCapsuleEnabled
                p[SystemEventsPreferencesKeys.SILENT_MODE_CAPSULE_ENABLED] = prefs.silentModeCapsuleEnabled
                p[SystemEventsPreferencesKeys.RING_MODE_CAPSULE_ENABLED] = prefs.ringModeCapsuleEnabled
                p[SystemEventsPreferencesKeys.DEVICE_UNLOCK_CAPSULE_ENABLED] = prefs.deviceUnlockCapsuleEnabled
                p[SystemEventsPreferencesKeys.FLASHLIGHT_CAPSULE_ENABLED] = prefs.flashlightCapsuleEnabled
                p[SystemEventsPreferencesKeys.WIFI_CAPSULE_ENABLED] = prefs.wifiCapsuleEnabled
                p[SystemEventsPreferencesKeys.ZEN_MODE_CAPSULE_ENABLED] = prefs.zenModeCapsuleEnabled
                p[SystemEventsPreferencesKeys.BATTERY_WARNING_CAPSULE_ENABLED] = prefs.batteryWarningCapsuleEnabled
            }
        } catch (e: IOException) {
            Log.e(TAG, "Failed to persist system event preferences to DataStore", e)
        }
    }

    suspend fun setSingleTapAction(action: GestureAction) {
        dataStore.edit { it[GesturePreferencesKeys.SINGLE_TAP_ACTION] = action.name }
    }

    suspend fun setLongPressAction(action: GestureAction) {
        dataStore.edit { it[GesturePreferencesKeys.LONG_PRESS_ACTION] = action.name }
    }

    suspend fun setSwipeLeftAction(action: GestureAction) {
        dataStore.edit { it[GesturePreferencesKeys.SWIPE_LEFT_ACTION] = action.name }
    }

    suspend fun setSwipeRightAction(action: GestureAction) {
        dataStore.edit { it[GesturePreferencesKeys.SWIPE_RIGHT_ACTION] = action.name }
    }

    suspend fun setSwipeDownAction(action: GestureAction) {
        dataStore.edit { it[GesturePreferencesKeys.SWIPE_DOWN_ACTION] = action.name }
    }

    suspend fun setDndAutoExpandOverride(enabled: Boolean) {
        dataStore.edit { it[GesturePreferencesKeys.DND_AUTO_EXPAND_OVERRIDE] = enabled }
    }

    suspend fun setHideSensitiveOnKeyguard(enabled: Boolean) {
        dataStore.edit { it[GesturePreferencesKeys.HIDE_SENSITIVE_ON_KEYGUARD] = enabled }
    }

    suspend fun setNotchDisappearMode(mode: NotchDisappearMode) {
        dataStore.edit { it[GesturePreferencesKeys.NOTCH_DISAPPEAR_MODE] = mode.name }
    }

    suspend fun setExpandSilentNotifications(enabled: Boolean) {
        dataStore.edit { it[GesturePreferencesKeys.EXPAND_SILENT_NOTIFICATIONS] = enabled }
    }

    suspend fun resetGesturePreferencesToDefaults() {
        val defaults = GesturePreferences()
        _liveGesturePreferences.value = defaults
        updateGesturePreferences(defaults)
    }
}
