package com.nothing.island.ai.receiver

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.BatteryManager
import android.util.Log
import com.nothing.island.ai.engine.IslandEvent
import com.nothing.island.ai.engine.IslandPriority
import com.nothing.island.ai.engine.UnlockMethod
import com.nothing.island.ai.gestures.RingerState

/**
 * SystemEventsReceiver handles core system hardware events, alerts, and keyguard signals
 * on Android 17 / API 37:
 * - REQ-36 / REQ-37: Ringer mode transitions (Silent, Vibrate, Normal)
 * - REQ-62: Do Not Disturb / Interruption filter changes
 * - REQ-75: Keyguard unlock (ACTION_USER_PRESENT, zero biometric telemetry)
 * - REQ-64: Low battery threshold warnings at 20% and 10%
 */
class SystemEventsReceiver(
    private val onEvent: (IslandEvent) -> Unit
) : BroadcastReceiver() {

    companion object {
        private const val TAG = "SystemEventsReceiver"

        fun createIntentFilter(): IntentFilter {
            return IntentFilter().apply {
                addAction(AudioManager.RINGER_MODE_CHANGED_ACTION)
                addAction(NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED)
                addAction(Intent.ACTION_USER_PRESENT)
                addAction(Intent.ACTION_BATTERY_CHANGED)
            }
        }
    }

    private var lastRingerMode: Int? = null
    private var lastDndState: Boolean? = null
    private var lastBatteryLevel: Int? = null

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return

        when (action) {
            AudioManager.RINGER_MODE_CHANGED_ACTION -> {
                val ringerMode = intent.getIntExtra(AudioManager.EXTRA_RINGER_MODE, -1)
                handleRingerModeChanged(ringerMode)
            }

            NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED -> {
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                val currentFilter = notificationManager?.currentInterruptionFilter ?: NotificationManager.INTERRUPTION_FILTER_ALL
                val isDndActive = currentFilter != NotificationManager.INTERRUPTION_FILTER_ALL &&
                        currentFilter != NotificationManager.INTERRUPTION_FILTER_UNKNOWN
                handleDndStateChanged(isDndActive)
            }

            Intent.ACTION_USER_PRESENT -> {
                handleUserPresent()
            }

            Intent.ACTION_BATTERY_CHANGED -> {
                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == BatteryManager.BATTERY_STATUS_FULL
                handleBatteryChanged(level, scale, isCharging)
            }
        }
    }

    internal fun handleRingerModeChanged(ringerMode: Int) {
        if (ringerMode != -1 && ringerMode != lastRingerMode) {
            lastRingerMode = ringerMode
            val state = when (ringerMode) {
                AudioManager.RINGER_MODE_SILENT -> RingerState.SILENT
                AudioManager.RINGER_MODE_VIBRATE -> RingerState.VIBRATE
                AudioManager.RINGER_MODE_NORMAL -> RingerState.NORMAL
                else -> RingerState.NORMAL
            }
            Log.d(TAG, "Ringer mode changed: $state")
            onEvent(IslandEvent.RingerModeChanged(ringerMode = state))
        }
    }

    internal fun handleDndStateChanged(isDndActive: Boolean) {
        if (lastDndState != isDndActive) {
            lastDndState = isDndActive
            Log.d(TAG, "DND state changed: isDndActive=$isDndActive")
            onEvent(IslandEvent.DndStateChanged(isEnabled = isDndActive))
        }
    }

    internal fun handleUserPresent() {
        Log.d(TAG, "Device unlocked (ACTION_USER_PRESENT)")
        onEvent(
            IslandEvent.DeviceUnlocked(
                id = "user_present_${System.currentTimeMillis()}",
                source = "SystemEventsReceiver",
                priority = IslandPriority.SYSTEM_HARDWARE,
                timeoutMs = 2500L,
                unlockMethod = UnlockMethod.PIN_PATTERN
            )
        )
    }

    internal fun handleBatteryChanged(level: Int, scale: Int, isCharging: Boolean) {
        if (level >= 0 && scale > 0 && !isCharging) {
            val percentage = (level * 100) / scale
            val prevLevel = lastBatteryLevel
            lastBatteryLevel = percentage

            if (prevLevel != null && prevLevel > 20 && percentage <= 20 && percentage > 10) {
                Log.d(TAG, "Low battery warning: 20%")
                onEvent(IslandEvent.BatteryThresholdWarning(batteryLevel = 20, isCritical = false))
            } else if (prevLevel != null && prevLevel > 10 && percentage <= 10) {
                Log.d(TAG, "Critical battery warning: 10%")
                onEvent(IslandEvent.BatteryThresholdWarning(batteryLevel = 10, isCritical = true))
            }
        }
    }
}
