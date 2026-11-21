package com.nothing.island.ai.engine

import android.util.Log
import com.nothing.island.ai.ExpandedViewType
import com.nothing.island.ai.IslandMode
import com.nothing.island.ai.IslandState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Immutable master state snapshot for Nothing Island.
 */
data class IslandMasterState(
    val mode: IslandMode = IslandMode.COLLAPSED,
    val expandedViewType: ExpandedViewType = ExpandedViewType.DASHBOARD,
    val activeEvent: IslandEvent? = null,
    val activePriority: Int = IslandPriority.IDLE_BASE,
    val presentationId: String = "idle_init",
    val title: String = "NOTHING ISLAND",
    val subtitle: String = "ACTIVE • CALIBRATED",
    val statusText: String = "A069 (FroggerIND)",
    val isPulsing: Boolean = true,
    val isRecordingActive: Boolean = false,
    val timeoutDeadlineMs: Long? = null,
    val selectedNotificationKey: String? = null,
    /**
     * R7.1: true when the expanded presentation was opened automatically by the notification
     * auto-expansion engine rather than by the user. Governs whether event expiration is allowed
     * to collapse the Island:
     *  - auto-expanded -> collapse when the triggering event expires (never leave a blank card)
     *  - user-expanded -> stay open; the user's deliberate choice outranks event churn
     */
    val isAutoExpanded: Boolean = false,
    /** True when two concurrent activities are shown as satellite pills. */
    val isDualPill: Boolean = false
) {
    /**
     * Bridges cleanly to existing IslandState for backward compatibility with Compose views.
     */
    fun toLegacyIslandState(): IslandState {
        return IslandState(
            mode = mode,
            expandedViewType = expandedViewType,
            title = title,
            subtitle = subtitle,
            statusText = statusText,
            isPulsing = isPulsing,
            tag = if (activeEvent != null) "EVENT P$activePriority" else "M3 ENGINE"
        )
    }
}

/**
 * IslandStateRepository
 *
 * Single source of truth for the Nothing Island visual presentation.
 * Exposes a read-only StateFlow to UI consumers and guarantees deterministic,
 * thread-safe state mutation. Completely decoupled from Android Views/Context.
 */
class IslandStateRepository(
    initialState: IslandMasterState = IslandMasterState()
) {
    companion object {
        private const val TAG = "IslandStateRepository"
    }

    private val _state = MutableStateFlow(initialState)

    /**
     * Read-only state stream observed by IslandOverlayController and Compose UI.
     */
    val state: StateFlow<IslandMasterState> = _state.asStateFlow()

    /**
     * Updates the presentation state from an event resolved by IslandPriorityManager.
     */
    fun updateFromResolvedEvent(event: IslandEvent?, priority: Int, presentationId: String) {
        _state.update { current ->
            if (event == null) {
                // Revert to Base Idle Presentation.
                // R7.1: if the Island was opened AUTOMATICALLY for a notification, the expiry of
                // that event must also collapse it — otherwise the window is left EXPANDED and
                // blank on screen. A user-initiated expansion is preserved untouched.
                Log.d(TAG, "State transition -> IDLE ($presentationId)")
                val shouldAutoCollapse = current.isAutoExpanded
                current.copy(
                    mode = if (shouldAutoCollapse) IslandMode.COLLAPSED else current.mode,
                    activeEvent = null,
                    activePriority = IslandPriority.IDLE_BASE,
                    presentationId = presentationId,
                    title = "NOTHING ISLAND",
                    subtitle = "ACTIVE • CALIBRATED",
                    statusText = "A069 (FroggerIND)",
                    timeoutDeadlineMs = null,
                    selectedNotificationKey = if (shouldAutoCollapse) null else current.selectedNotificationKey,
                    isAutoExpanded = false
                )
            } else {
                Log.d(TAG, "State transition -> Event ${event.id} (P$priority, $presentationId)")
                val deadline = if (event.timeoutMs > 0L) System.currentTimeMillis() + event.timeoutMs else null
                val presentation = derivePresentationDetails(event)

                current.copy(
                    activeEvent = event,
                    activePriority = priority,
                    presentationId = presentationId,
                    title = presentation.title,
                    subtitle = presentation.subtitle,
                    statusText = presentation.statusText,
                    isRecordingActive = event is IslandEvent.RecordingStateChanged && event.isRecording,
                    timeoutDeadlineMs = deadline
                )
            }
        }
    }

    /**
     * Expands overlay into State A: Normal Dashboard (Time, Date, Weather, Recent Previews).
     */
    fun expandDashboard() {
        _state.update { current ->
            Log.d(TAG, "Expanded -> State A (DASHBOARD)")
            current.copy(
                mode = IslandMode.EXPANDED,
                expandedViewType = ExpandedViewType.DASHBOARD,
                selectedNotificationKey = null,
                isAutoExpanded = false
            )
        }
    }

    /**
     * Expands overlay into State B: Notification-Focused View (Notification details, actions; NO clock/weather).
     *
     * @param auto true when opened by the R7.1 notification auto-expansion engine, which allows
     *   event expiry to collapse the Island automatically. False for user-initiated expansion.
     */
    fun expandNotificationFocused(auto: Boolean = false) {
        _state.update { current ->
            Log.d(TAG, "Expanded -> State B (NOTIFICATION_FOCUSED), auto=$auto")
            current.copy(
                mode = IslandMode.EXPANDED,
                expandedViewType = ExpandedViewType.NOTIFICATION_FOCUSED,
                isAutoExpanded = auto
            )
        }
    }

    /**
     * Expands overlay into State C: Music Player (Artwork, Controls, Scrubber, Visualizer).
     */
    fun expandMusic() {
        _state.update { current ->
            Log.d(TAG, "Expanded -> State C (MUSIC)")
            current.copy(
                mode = IslandMode.EXPANDED,
                expandedViewType = ExpandedViewType.MUSIC,
                selectedNotificationKey = null,
                isAutoExpanded = false
            )
        }
    }

    /**
     * Expands overlay into State D: Battery & Charging Intelligence.
     *
     * @param auto true when opened by the battery auto-expansion engine (cable connect or safety alert).
     */
    fun expandBattery(auto: Boolean = false) {
        _state.update { current ->
            Log.d(TAG, "Expanded -> State D (BATTERY), auto=$auto")
            current.copy(
                mode = IslandMode.EXPANDED,
                expandedViewType = ExpandedViewType.BATTERY,
                selectedNotificationKey = null,
                isAutoExpanded = auto
            )
        }
    }

    /**
     * Expands overlay into State E: Call Card (caller, video badge, duration timer,
     * answer and decline controls backed by the dialer's notification actions).
     */
    fun expandCall() {
        _state.update { current ->
            Log.d(TAG, "Expanded -> State E (CALL)")
            current.copy(
                mode = IslandMode.EXPANDED,
                expandedViewType = ExpandedViewType.CALL,
                selectedNotificationKey = null,
                isAutoExpanded = true
            )
        }
    }

    /** Enables or disables the dual-pill split layout. */
    fun setDualPill(enabled: Boolean) {
        _state.update { it.copy(isDualPill = enabled) }
    }

    /** Expands the quick tools drawer (State F). */
    fun expandQuickTools() {
        _state.update { current ->
            Log.d(TAG, "Expanded -> State F (QUICK_TOOLS)")
            current.copy(
                mode = IslandMode.EXPANDED,
                expandedViewType = ExpandedViewType.QUICK_TOOLS,
                selectedNotificationKey = null,
                isAutoExpanded = false,
            )
        }
    }

    /**
     * Selects a historical notification to view in State B (Notification-Focused View).
     */
    fun selectHistoricalNotification(key: String) {
        _state.update { current ->
            Log.d(TAG, "Selected historical notification: key=$key -> Opening State B")
            current.copy(
                mode = IslandMode.EXPANDED,
                expandedViewType = ExpandedViewType.NOTIFICATION_FOCUSED,
                selectedNotificationKey = key,
                isAutoExpanded = false
            )
        }
    }

    /**
     * Clears the currently selected historical notification.
     */
    fun clearSelectedNotification() {
        _state.update { current ->
            if (current.selectedNotificationKey != null) {
                Log.d(TAG, "Cleared selected historical notification")
                current.copy(selectedNotificationKey = null)
            } else {
                current
            }
        }
    }

    /**
     * Collapses the overlay back to the calibrated compact pill.
     */
    fun collapse() {
        _state.update { current ->
            if (current.mode != IslandMode.COLLAPSED || current.selectedNotificationKey != null) {
                Log.d(TAG, "Collapsed overlay to compact pill (clearing selected notification)")
                current.copy(
                    mode = IslandMode.COLLAPSED,
                    selectedNotificationKey = null,
                    isAutoExpanded = false
                )
            } else {
                current
            }
        }
    }

    /**
     * Toggles between collapsed capsule and expanded card mode.
     */
    fun toggleMode() {
        _state.update { current ->
            val nextMode = if (current.mode == IslandMode.COLLAPSED) {
                IslandMode.EXPANDED
            } else {
                IslandMode.COLLAPSED
            }
            val targetType = if (current.activeEvent is IslandEvent.NotificationPosted) {
                ExpandedViewType.NOTIFICATION_FOCUSED
            } else {
                ExpandedViewType.DASHBOARD
            }
            val nextKey = if (nextMode == IslandMode.COLLAPSED) null else current.selectedNotificationKey
            Log.d(TAG, "User toggled mode: ${current.mode} -> $nextMode (viewType=$targetType)")
            // R7.1: an explicit user toggle always takes ownership of the expansion state.
            current.copy(
                mode = nextMode,
                expandedViewType = targetType,
                selectedNotificationKey = nextKey,
                isAutoExpanded = false
            )
        }
    }

    /**
     * Explicitly sets the display mode (COLLAPSED vs EXPANDED).
     */
    fun setMode(mode: IslandMode) {
        _state.update { current ->
            if (current.mode != mode) {
                Log.d(TAG, "Mode updated: ${current.mode} -> $mode")
                val nextKey = if (mode == IslandMode.COLLAPSED) null else current.selectedNotificationKey
                current.copy(
                    mode = mode,
                    selectedNotificationKey = nextKey,
                    isAutoExpanded = if (mode == IslandMode.COLLAPSED) false else current.isAutoExpanded
                )
            } else {
                current
            }
        }
    }

    /**
     * Toggles the Nothing OS breathing accent pulse.
     */
    fun togglePulse() {
        _state.update { current ->
            val nextPulse = !current.isPulsing
            Log.d(TAG, "Pulse toggled: $nextPulse")
            current.copy(isPulsing = nextPulse)
        }
    }

    /**
     * Explicitly sets the Nothing OS breathing accent pulse.
     */
    fun setPulse(isPulsing: Boolean) {
        _state.update { current ->
            if (current.isPulsing != isPulsing) {
                Log.d(TAG, "Pulse updated: $isPulsing")
                current.copy(isPulsing = isPulsing)
            } else {
                current
            }
        }
    }

    /**
     * Resets repository presentation state to Base Idle while preserving user display mode.
     */
    fun resetToIdle() {
        _state.update { current ->
            IslandMasterState(
                mode = current.mode,
                isPulsing = current.isPulsing,
                selectedNotificationKey = null
            )
        }
        Log.i(TAG, "IslandStateRepository reset to idle.")
    }

    private data class PresentationDetails(
        val title: String,
        val subtitle: String,
        val statusText: String
    )

    private fun derivePresentationDetails(event: IslandEvent): PresentationDetails {
        return when (event) {
            is IslandEvent.EmergencyAlert -> PresentationDetails(
                title = event.alertTitle,
                subtitle = event.alertMessage,
                statusText = "CRITICAL ALERT"
            )
            is IslandEvent.IncomingCallRinging -> PresentationDetails(
                title = "INCOMING CALL",
                subtitle = event.callerDisplayName,
                statusText = if (event.isVideoCall) "VIDEO CALL" else "VOICE CALL"
            )
            is IslandEvent.CallConnected -> PresentationDetails(
                title = "ACTIVE CALL",
                subtitle = event.callerDisplayName,
                statusText = "${event.durationSeconds}s"
            )
            is IslandEvent.CallEnded -> PresentationDetails(
                title = "CALL ENDED",
                subtitle = "DISCONNECTED",
                statusText = "RECENT"
            )
            is IslandEvent.NotificationPosted -> PresentationDetails(
                title = event.title,
                subtitle = event.text,
                statusText = event.packageName
            )
            is IslandEvent.NotificationDismissed -> PresentationDetails(
                title = "NOTIFICATION DISMISSED",
                subtitle = event.packageName,
                statusText = "DISMISSED"
            )
            is IslandEvent.MediaPlaybackChanged -> PresentationDetails(
                title = event.trackTitle,
                subtitle = event.artistName,
                statusText = if (event.isPlaying) "PLAYING" else "PAUSED"
            )
            is IslandEvent.MediaPlaybackStopped -> PresentationDetails(
                title = "MEDIA STOPPED",
                subtitle = event.packageName,
                statusText = "STOPPED"
            )
            is IslandEvent.BatteryStateChanged -> PresentationDetails(
                title = if (event.isLowBattery) "LOW BATTERY" else "BATTERY LEVEL",
                subtitle = "${event.batteryLevel}%",
                statusText = if (event.isCharging) "CHARGING" else "DISCHARGING"
            )
            is IslandEvent.ChargingConnected -> PresentationDetails(
                title = if (event.isFastCharging) "FAST CHARGING" else "CHARGING",
                subtitle = "${event.batteryLevel}%",
                statusText = event.chargeType
            )
            is IslandEvent.ChargingDisconnected -> PresentationDetails(
                title = "CHARGER DISCONNECTED",
                subtitle = "${event.batteryLevel}%",
                statusText = "DISCHARGING"
            )
            is IslandEvent.BatteryWarning -> PresentationDetails(
                title = if (event.isOverheat) "BATTERY OVERHEAT" else "BATTERY WARNING",
                subtitle = "${event.temperatureC}°C",
                statusText = "CRITICAL SAFETY"
            )
            is IslandEvent.BluetoothDeviceConnected -> PresentationDetails(
                title = "BLUETOOTH CONNECTED",
                subtitle = event.deviceName,
                statusText = if (event.batteryLevelPercentage != null) "${event.batteryLevelPercentage}%" else "CONNECTED"
            )
            is IslandEvent.BluetoothDeviceDisconnected -> PresentationDetails(
                title = "BLUETOOTH DISCONNECTED",
                subtitle = event.deviceName,
                statusText = "DISCONNECTED"
            )
            is IslandEvent.DeviceUnlocked -> PresentationDetails(
                title = "UNLOCKED",
                subtitle = event.unlockMethod.name,
                statusText = "AUTHENTICATED"
            )
            is IslandEvent.TimerStarted -> PresentationDetails(
                title = "TIMER: ${event.remainingSeconds}s",
                subtitle = event.label,
                statusText = "STARTED"
            )
            is IslandEvent.TimerTick -> PresentationDetails(
                title = "TIMER: ${event.remainingSeconds}s",
                subtitle = "ACTIVE COUNTDOWN",
                statusText = "RUNNING"
            )
            is IslandEvent.TimerFinished -> PresentationDetails(
                title = "TIMER FINISHED",
                subtitle = event.label,
                statusText = "TIME UP"
            )
            is IslandEvent.RecordingStateChanged -> PresentationDetails(
                title = "RECORDING ACTIVE",
                subtitle = event.recordingType.name,
                statusText = "PRIVACY ANCHOR"
            )
            is IslandEvent.RingerModeChanged -> PresentationDetails(
                title = when (event.ringerMode) {
                    com.nothing.island.ai.gestures.RingerState.SILENT -> "SILENT"
                    com.nothing.island.ai.gestures.RingerState.VIBRATE -> "VIBRATE"
                    com.nothing.island.ai.gestures.RingerState.NORMAL -> "SOUND ON"
                },
                subtitle = "RINGER MODE",
                statusText = "SYSTEM"
            )
            is IslandEvent.TorchStateChanged -> PresentationDetails(
                title = if (event.isEnabled) "TORCH ON" else "TORCH OFF",
                subtitle = "FLASHLIGHT",
                statusText = "HARDWARE"
            )
            is IslandEvent.WifiStateChanged -> PresentationDetails(
                title = if (event.isConnected) (event.ssid ?: "WI-FI CONNECTED") else "WI-FI DISCONNECTED",
                subtitle = if (event.isConnected) "NETWORK ACTIVE" else "DISCONNECTED",
                statusText = "CONNECTIVITY"
            )
            is IslandEvent.DndStateChanged -> PresentationDetails(
                title = if (event.isEnabled) "DND ON" else "DND OFF",
                subtitle = "DO NOT DISTURB",
                statusText = "BEDTIME"
            )
            is IslandEvent.BatteryThresholdWarning -> PresentationDetails(
                title = if (event.isCritical) "CRITICAL BATTERY" else "LOW BATTERY",
                subtitle = "${event.batteryLevel}% REMAINING",
                statusText = if (event.isCritical) "CONNECT CHARGER" else "BATTERY SAVER"
            )
            else -> PresentationDetails(
                title = "NOTHING ISLAND",
                subtitle = "ACTIVE • CALIBRATED",
                statusText = "A069 (FroggerIND)"
            )
        }
    }
}
