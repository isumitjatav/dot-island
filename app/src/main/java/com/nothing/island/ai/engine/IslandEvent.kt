package com.nothing.island.ai.engine

import com.nothing.island.ai.gestures.RingerState

/**
 * Priority classification levels for Nothing Island event arbitration.
 * Higher integer values represent strictly higher precedence.
 */
object IslandPriority {
    const val CRITICAL_TELEPHONY = 100 // Incoming Phone Call, Emergency / Security Alerts
    const val ACTIVE_TELEPHONY = 85    // Ongoing Connected Call
    const val SAFETY_ALERT = 70        // Critical Low Battery (<15%), Overheat Warning
    const val NOTIFICATION = 55        // Incoming Notification (WhatsApp, Messages, etc.)
    const val MEDIA_PLAYBACK = 40      // Ongoing Media Playback (Spotify, YouTube Music)
    const val LIVE_ACTIVITY = 25       // Active Countdown Timer, Stopwatch
    const val RECORDING_INDICATOR = 20 // Microphone / Camera / Screen Recording Active
    const val SYSTEM_HARDWARE = 15     // Device Unlock, Bluetooth Connect, Charging Plugged
    const val USER_INTERACTION = 10    // User Tap, Toggle Expand, Toggle Pulse
    const val IDLE_BASE = 0            // Base Idle State
}

/**
 * Method utilized for biometric or secure device unlock.
 */
enum class UnlockMethod {
    FINGERPRINT,
    FACE,
    PIN_PATTERN,
    SMART_LOCK
}

/**
 * Type of active recording session.
 */
enum class RecordingType {
    AUDIO,
    CAMERA,
    SCREEN
}

/**
 * State of download or ongoing progress activity.
 */
enum class DownloadProgressState {
    NONE,
    DOWNLOADING,
    INDETERMINATE,
    PAUSED,
    COMPLETED,
    FAILED
}

/**
 * Strongly typed sealed hierarchy representing all system, hardware, and user events
 * processed by the Nothing Island engine.
 */
sealed interface IslandEvent {
    val id: String
    val timestamp: Long
    val source: String
    val priority: Int
    val timeoutMs: Long get() = 0L // 0L indicates persistent presentation until explicitly dismissed or ended

    /**
     * Compares the semantic business payload against another event, explicitly
     * ignoring non-semantic fields such as transient generation timestamps.
     */
    fun hasIdenticalBusinessPayload(other: IslandEvent): Boolean {
        if (this.javaClass != other.javaClass) return false
        if (this.id != other.id) return false
        if (this.source != other.source) return false
        if (this.priority != other.priority) return false
        if (this.timeoutMs != other.timeoutMs) return false

        return when (this) {
            is OrderTrackingUpdated -> {
                val o = other as OrderTrackingUpdated
                merchant == o.merchant && status == o.status && etaMinutes == o.etaMinutes
            }
            is NotificationPosted -> {
                val o = other as NotificationPosted
                packageName == o.packageName &&
                        title == o.title &&
                        text == o.text &&
                        subText == o.subText &&
                        category == o.category &&
                        isClearable == o.isClearable &&
                        progress == o.progress &&
                        maxProgress == o.maxProgress &&
                        isIndeterminate == o.isIndeterminate &&
                        hasProgress == o.hasProgress &&
                        progressState == o.progressState
            }
            is NotificationDismissed -> {
                val o = other as NotificationDismissed
                targetNotificationId == o.targetNotificationId &&
                        packageName == o.packageName
            }
            is MediaPlaybackChanged -> {
                val o = other as MediaPlaybackChanged
                packageName == o.packageName &&
                        trackTitle == o.trackTitle &&
                        artistName == o.artistName &&
                        isPlaying == o.isPlaying &&
                        durationMs == o.durationMs &&
                        currentPositionMs == o.currentPositionMs
            }
            is MediaPlaybackStopped -> {
                val o = other as MediaPlaybackStopped
                packageName == o.packageName
            }
            is BatteryStateChanged -> {
                val o = other as BatteryStateChanged
                batteryLevel == o.batteryLevel &&
                        isCharging == o.isCharging &&
                        isLowBattery == o.isLowBattery
            }
            is ChargingConnected -> {
                val o = other as ChargingConnected
                batteryLevel == o.batteryLevel &&
                        chargeType == o.chargeType &&
                        isFastCharging == o.isFastCharging
            }
            is ChargingDisconnected -> {
                val o = other as ChargingDisconnected
                batteryLevel == o.batteryLevel
            }
            is BatteryWarning -> {
                val o = other as BatteryWarning
                batteryLevel == o.batteryLevel &&
                        temperatureC == o.temperatureC &&
                        isOverheat == o.isOverheat
            }
            is EmergencyAlert -> {
                val o = other as EmergencyAlert
                alertTitle == o.alertTitle && alertMessage == o.alertMessage
            }
            is IncomingCallRinging -> {
                val o = other as IncomingCallRinging
                callId == o.callId &&
                        callerDisplayName == o.callerDisplayName &&
                        isVideoCall == o.isVideoCall
            }
            is CallConnected -> {
                val o = other as CallConnected
                callId == o.callId &&
                        callerDisplayName == o.callerDisplayName &&
                        durationSeconds == o.durationSeconds
            }
            is CallEnded -> {
                val o = other as CallEnded
                callId == o.callId
            }
            is DeviceUnlocked -> {
                val o = other as DeviceUnlocked
                unlockMethod == o.unlockMethod
            }
            is BluetoothDeviceConnected -> {
                val o = other as BluetoothDeviceConnected
                deviceName == o.deviceName &&
                        deviceAddress == o.deviceAddress &&
                        batteryLevelPercentage == o.batteryLevelPercentage
            }
            is BluetoothDeviceDisconnected -> {
                val o = other as BluetoothDeviceDisconnected
                deviceName == o.deviceName &&
                        deviceAddress == o.deviceAddress
            }
            is TimerStarted -> {
                val o = other as TimerStarted
                timerId == o.timerId &&
                        label == o.label &&
                        totalDurationSeconds == o.totalDurationSeconds &&
                        remainingSeconds == o.remainingSeconds
            }
            is TimerTick -> {
                val o = other as TimerTick
                timerId == o.timerId &&
                        remainingSeconds == o.remainingSeconds
            }
            is TimerFinished -> {
                val o = other as TimerFinished
                timerId == o.timerId &&
                        label == o.label
            }
            is RecordingStateChanged -> {
                val o = other as RecordingStateChanged
                isRecording == o.isRecording &&
                        recordingType == o.recordingType
            }
            is UserToggleExpand, is UserTogglePulse, is UserDismissCurrent -> true
            is OverlayAttached -> {
                val o = other as OverlayAttached
                isAccessibilityMode == o.isAccessibilityMode
            }
            is OverlayDetached -> true
            is ScreenStateChanged -> {
                val o = other as ScreenStateChanged
                isScreenOn == o.isScreenOn
            }
            is RingerModeChanged -> {
                val o = other as RingerModeChanged
                ringerMode == o.ringerMode
            }
            is TorchStateChanged -> {
                val o = other as TorchStateChanged
                isEnabled == o.isEnabled
            }
            is WifiStateChanged -> {
                val o = other as WifiStateChanged
                isConnected == o.isConnected && ssid == o.ssid
            }
            is DndStateChanged -> {
                val o = other as DndStateChanged
                isEnabled == o.isEnabled
            }
            is BatteryThresholdWarning -> {
                val o = other as BatteryThresholdWarning
                batteryLevel == o.batteryLevel && isCritical == o.isCritical
            }
        }
    }

    // ==========================================
    // 1. NOTIFICATIONS
    // ==========================================
    data class NotificationPosted(
        override val id: String,
        override val timestamp: Long = System.currentTimeMillis(),
        override val source: String = "NotificationEngine",
        override val priority: Int = IslandPriority.NOTIFICATION,
        override val timeoutMs: Long = 4500L,
        val packageName: String,
        val title: String,
        val text: String,
        val subText: String? = null,
        val category: String? = null,
        val isClearable: Boolean = true,
        val progress: Int = 0,
        val maxProgress: Int = 0,
        val isIndeterminate: Boolean = false,
        val hasProgress: Boolean = false,
        val progressState: DownloadProgressState = DownloadProgressState.NONE
    ) : IslandEvent

    data class NotificationDismissed(
        override val id: String,
        override val timestamp: Long = System.currentTimeMillis(),
        override val source: String = "NotificationEngine",
        override val priority: Int = IslandPriority.NOTIFICATION,
        override val timeoutMs: Long = 0L,
        val targetNotificationId: String,
        val packageName: String
    ) : IslandEvent

    // ==========================================
    // 2. MEDIA PLAYBACK
    // ==========================================
    data class MediaPlaybackChanged(
        override val id: String,
        override val timestamp: Long = System.currentTimeMillis(),
        override val source: String = "MediaEngine",
        override val priority: Int = IslandPriority.MEDIA_PLAYBACK,
        override val timeoutMs: Long = 0L,
        val packageName: String,
        val trackTitle: String,
        val artistName: String,
        val isPlaying: Boolean,
        val durationMs: Long = 0L,
        val currentPositionMs: Long = 0L
    ) : IslandEvent

    data class MediaPlaybackStopped(
        override val id: String,
        override val timestamp: Long = System.currentTimeMillis(),
        override val source: String = "MediaEngine",
        override val priority: Int = IslandPriority.MEDIA_PLAYBACK,
        override val timeoutMs: Long = 0L,
        val packageName: String
    ) : IslandEvent

    // ==========================================
    // 3. BATTERY & CHARGING
    // ==========================================
    data class BatteryStateChanged(
        override val id: String,
        val batteryLevel: Int,
        val isCharging: Boolean,
        val isLowBattery: Boolean = false,
        override val timestamp: Long = System.currentTimeMillis(),
        override val source: String = "BatteryEngine",
        override val priority: Int = if (isLowBattery) IslandPriority.SAFETY_ALERT else IslandPriority.SYSTEM_HARDWARE,
        override val timeoutMs: Long = if (isLowBattery) 5000L else 3000L
    ) : IslandEvent

    data class ChargingConnected(
        override val id: String,
        override val timestamp: Long = System.currentTimeMillis(),
        override val source: String = "BatteryEngine",
        override val priority: Int = IslandPriority.SYSTEM_HARDWARE,
        override val timeoutMs: Long = 3000L,
        val batteryLevel: Int,
        val chargeType: String,
        val isFastCharging: Boolean = false
    ) : IslandEvent

    data class ChargingDisconnected(
        override val id: String,
        override val timestamp: Long = System.currentTimeMillis(),
        override val source: String = "BatteryEngine",
        override val priority: Int = IslandPriority.SYSTEM_HARDWARE,
        override val timeoutMs: Long = 3000L,
        val batteryLevel: Int
    ) : IslandEvent

    data class BatteryWarning(
        override val id: String,
        override val timestamp: Long = System.currentTimeMillis(),
        override val source: String = "BatteryEngine",
        override val priority: Int = IslandPriority.SAFETY_ALERT,
        override val timeoutMs: Long = 5000L,
        val batteryLevel: Int,
        val temperatureC: Float,
        val isOverheat: Boolean
    ) : IslandEvent

    // ==========================================
    // 4. TELEPHONY & CALLS / CRITICAL SAFETY
    // ==========================================
    data class EmergencyAlert(
        override val id: String,
        override val timestamp: Long = System.currentTimeMillis(),
        override val source: String = "SafetyEngine",
        override val priority: Int = IslandPriority.CRITICAL_TELEPHONY,
        override val timeoutMs: Long = 3000L,
        val alertTitle: String,
        val alertMessage: String
    ) : IslandEvent

    data class IncomingCallRinging(
        override val id: String,
        override val timestamp: Long = System.currentTimeMillis(),
        override val source: String = "TelephonyEngine",
        override val priority: Int = IslandPriority.CRITICAL_TELEPHONY,
        override val timeoutMs: Long = 0L, // Persistent while ringing
        val callId: String,
        val callerDisplayName: String,
        val isVideoCall: Boolean = false,
        val contactPhoto: android.graphics.Bitmap? = null
    ) : IslandEvent

    data class CallConnected(
        override val id: String,
        override val timestamp: Long = System.currentTimeMillis(),
        override val source: String = "TelephonyEngine",
        // Must preempt IncomingCallRinging after the user answers.
        override val priority: Int = IslandPriority.CRITICAL_TELEPHONY,
        override val timeoutMs: Long = 0L,
        val callId: String,
        val callerDisplayName: String,
        val durationSeconds: Long = 0L,
        val isDialing: Boolean = false,
        val contactPhoto: android.graphics.Bitmap? = null
    ) : IslandEvent

    data class CallEnded(
        override val id: String,
        override val timestamp: Long = System.currentTimeMillis(),
        override val source: String = "TelephonyEngine",
        override val priority: Int = IslandPriority.ACTIVE_TELEPHONY,
        override val timeoutMs: Long = 1500L, // Brief confirmation before fallback
        val callId: String
    ) : IslandEvent

    // ==========================================
    // 5. DEVICE UNLOCK
    // ==========================================
    data class DeviceUnlocked(
        override val id: String,
        override val timestamp: Long = System.currentTimeMillis(),
        override val source: String = "UnlockEngine",
        override val priority: Int = IslandPriority.SYSTEM_HARDWARE,
        override val timeoutMs: Long = 2000L,
        val unlockMethod: UnlockMethod
    ) : IslandEvent

    // ==========================================
    // 6. BLUETOOTH
    // ==========================================
    data class BluetoothDeviceConnected(
        override val id: String,
        override val timestamp: Long = System.currentTimeMillis(),
        override val source: String = "BluetoothEngine",
        override val priority: Int = IslandPriority.SYSTEM_HARDWARE,
        override val timeoutMs: Long = 3000L,
        val deviceName: String,
        val deviceAddress: String,
        val batteryLevelPercentage: Int? = null
    ) : IslandEvent

    data class BluetoothDeviceDisconnected(
        override val id: String,
        override val timestamp: Long = System.currentTimeMillis(),
        override val source: String = "BluetoothEngine",
        override val priority: Int = IslandPriority.SYSTEM_HARDWARE,
        override val timeoutMs: Long = 2000L,
        val deviceName: String,
        val deviceAddress: String
    ) : IslandEvent

    // ==========================================
    // 7. TIMERS & LIVE ACTIVITIES
    // ==========================================
    data class TimerStarted(
        override val id: String,
        override val timestamp: Long = System.currentTimeMillis(),
        override val source: String = "TimerEngine",
        override val priority: Int = IslandPriority.LIVE_ACTIVITY,
        override val timeoutMs: Long = 0L,
        val timerId: String,
        val label: String,
        val totalDurationSeconds: Long,
        val remainingSeconds: Long
    ) : IslandEvent

    data class TimerTick(
        override val id: String,
        override val timestamp: Long = System.currentTimeMillis(),
        override val source: String = "TimerEngine",
        override val priority: Int = IslandPriority.LIVE_ACTIVITY,
        override val timeoutMs: Long = 0L,
        val timerId: String,
        val remainingSeconds: Long
    ) : IslandEvent

    data class TimerFinished(
        override val id: String,
        override val timestamp: Long = System.currentTimeMillis(),
        override val source: String = "TimerEngine",
        override val priority: Int = IslandPriority.NOTIFICATION,
        override val timeoutMs: Long = 5000L,
        val timerId: String,
        val label: String
    ) : IslandEvent

    // ==========================================
    // 8. RECORDING INDICATOR
    // ==========================================
    data class RecordingStateChanged(
        override val id: String,
        override val timestamp: Long = System.currentTimeMillis(),
        override val source: String = "RecordingIndicatorEngine",
        override val priority: Int = IslandPriority.RECORDING_INDICATOR,
        override val timeoutMs: Long = 0L,
        val isRecording: Boolean,
        val recordingType: RecordingType
    ) : IslandEvent

    // ==========================================
    // 9. USER INTERACTIONS
    // ==========================================
    data class UserToggleExpand(
        override val id: String,
        override val timestamp: Long = System.currentTimeMillis(),
        override val source: String = "UserTouch",
        override val priority: Int = IslandPriority.USER_INTERACTION,
        override val timeoutMs: Long = 0L
    ) : IslandEvent

    data class UserTogglePulse(
        override val id: String,
        override val timestamp: Long = System.currentTimeMillis(),
        override val source: String = "UserTouch",
        override val priority: Int = IslandPriority.USER_INTERACTION,
        override val timeoutMs: Long = 0L
    ) : IslandEvent

    data class UserDismissCurrent(
        override val id: String,
        override val timestamp: Long = System.currentTimeMillis(),
        override val source: String = "UserTouch",
        override val priority: Int = IslandPriority.USER_INTERACTION,
        override val timeoutMs: Long = 0L
    ) : IslandEvent

    // ==========================================
    // 10. SYSTEM LIFECYCLE
    // ==========================================
    data class OverlayAttached(
        override val id: String,
        override val timestamp: Long = System.currentTimeMillis(),
        override val source: String = "OverlayHost",
        override val priority: Int = IslandPriority.IDLE_BASE,
        override val timeoutMs: Long = 0L,
        val isAccessibilityMode: Boolean
    ) : IslandEvent

    data class OverlayDetached(
        override val id: String,
        override val timestamp: Long = System.currentTimeMillis(),
        override val source: String = "OverlayHost",
        override val priority: Int = IslandPriority.IDLE_BASE,
        override val timeoutMs: Long = 0L
    ) : IslandEvent

    data class ScreenStateChanged(
        override val id: String,
        override val timestamp: Long = System.currentTimeMillis(),
        override val source: String = "SystemPower",
        override val priority: Int = IslandPriority.IDLE_BASE,
        override val timeoutMs: Long = 0L,
        val isScreenOn: Boolean
    ) : IslandEvent

    // ==========================================
    // 11. HARDWARE ALERTS & SYSTEM SENSORS
    // ==========================================
    data class RingerModeChanged(
        override val id: String = "ringer_mode_changed",
        override val timestamp: Long = System.currentTimeMillis(),
        override val source: String = "SystemEventsReceiver",
        override val priority: Int = IslandPriority.SYSTEM_HARDWARE,
        override val timeoutMs: Long = 2500L,
        val ringerMode: RingerState
    ) : IslandEvent

    data class TorchStateChanged(
        override val id: String = "torch_state_changed",
        override val timestamp: Long = System.currentTimeMillis(),
        override val source: String = "TorchCallbackHandler",
        override val priority: Int = IslandPriority.SYSTEM_HARDWARE,
        override val timeoutMs: Long = 2500L,
        val isEnabled: Boolean
    ) : IslandEvent

    data class WifiStateChanged(
        override val id: String = "wifi_state_changed",
        override val timestamp: Long = System.currentTimeMillis(),
        override val source: String = "NetworkCallbackHandler",
        override val priority: Int = IslandPriority.SYSTEM_HARDWARE,
        override val timeoutMs: Long = 3000L,
        val isConnected: Boolean,
        val ssid: String? = null
    ) : IslandEvent

    data class DndStateChanged(
        override val id: String = "dnd_state_changed",
        override val timestamp: Long = System.currentTimeMillis(),
        override val source: String = "SystemEventsReceiver",
        override val priority: Int = IslandPriority.SYSTEM_HARDWARE,
        override val timeoutMs: Long = 2500L,
        val isEnabled: Boolean
    ) : IslandEvent

    data class BatteryThresholdWarning(
        override val id: String = "battery_threshold_warning",
        override val timestamp: Long = System.currentTimeMillis(),
        override val source: String = "SystemEventsReceiver",
        override val priority: Int = IslandPriority.SAFETY_ALERT,
        override val timeoutMs: Long = 4000L,
        val batteryLevel: Int,
        val isCritical: Boolean
    ) : IslandEvent

    /** Live order/delivery tracking state, typically derived from notifications. */
    data class OrderTrackingUpdated(
        override val id: String,
        override val timestamp: Long = System.currentTimeMillis(),
        override val source: String = "OrderTrackingEngine",
        override val priority: Int = IslandPriority.LIVE_ACTIVITY,
        override val timeoutMs: Long = 8000L,
        val merchant: String,
        val status: String,
        val etaMinutes: Int? = null,
    ) : IslandEvent
}
