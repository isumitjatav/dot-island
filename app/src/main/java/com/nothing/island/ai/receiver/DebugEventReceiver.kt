package com.nothing.island.ai.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.nothing.island.ai.IslandOverlayController
import com.nothing.island.ai.data.IslandPreferencesRepository
import com.nothing.island.ai.engine.IslandEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Debug-only receiver for previewing system pills without real hardware.
 * ADB usage:
 *   adb shell am broadcast -a com.nothing.island.ai.DEBUG_EARBUD --ei battery 75 --es name "AirPods Pro"
 *   adb shell am broadcast -a com.nothing.island.ai.DEBUG_EARBUD_DISCONNECT --es name "AirPods Pro"
 */
class DebugEventReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val controller = IslandOverlayController.getInstance(context)
        when (intent.action) {
            ACTION_EARBUD -> {
                val name = intent.getStringExtra(EXTRA_NAME) ?: "AirPods Pro"
                val battery = if (intent.hasExtra(EXTRA_BATTERY)) {
                    intent.getIntExtra(EXTRA_BATTERY, 75)
                } else null
                controller.eventEngine.emit(
                    IslandEvent.BluetoothDeviceConnected(
                        id = "debug_earbud_${System.currentTimeMillis()}",
                        deviceName = name,
                        deviceAddress = "00:00:00:00:00:00",
                        batteryLevelPercentage = battery,
                    )
                )
            }
            ACTION_DUAL_PILL -> {
                IslandOverlayController.getInstance(context).repository.setDualPill(
                    intent.getBooleanExtra(EXTRA_ENABLED, true)
                )
            }
            ACTION_FACE_UNLOCK -> {
                IslandOverlayController.getInstance(context).eventEngine.emit(
                    IslandEvent.DeviceUnlocked(
                        id = "debug_face_unlock_${System.currentTimeMillis()}",
                        unlockMethod = com.nothing.island.ai.engine.UnlockMethod.FACE,
                    )
                )
            }
            ACTION_QUICK_TOOLS -> {
                IslandOverlayController.getInstance(context).repository.expandQuickTools()
            }
            ACTION_MASCOT -> {
                val enabled = intent.getBooleanExtra(EXTRA_ENABLED, true)
                val name = intent.getStringExtra(EXTRA_NAME) ?: "CAT"
                CoroutineScope(Dispatchers.IO).launch {
                    IslandPreferencesRepository.getInstance(context).updateMascot(enabled, name)
                }
            }
            ACTION_EARBUD_DISCONNECT -> {
                val name = intent.getStringExtra(EXTRA_NAME) ?: "AirPods Pro"
                controller.eventEngine.emit(
                    IslandEvent.BluetoothDeviceDisconnected(
                        id = "debug_earbud_off_${System.currentTimeMillis()}",
                        deviceName = name,
                        deviceAddress = "00:00:00:00:00:00",
                    )
                )
            }
        }
    }

    companion object {
        const val ACTION_EARBUD = "com.nothing.island.ai.DEBUG_EARBUD"
        const val ACTION_EARBUD_DISCONNECT = "com.nothing.island.ai.DEBUG_EARBUD_DISCONNECT"
        const val EXTRA_NAME = "name"
        const val EXTRA_BATTERY = "battery"
        const val ACTION_DUAL_PILL = "com.nothing.island.ai.DEBUG_DUAL_PILL"
        const val ACTION_FACE_UNLOCK = "com.nothing.island.ai.DEBUG_FACE_UNLOCK"
        const val ACTION_QUICK_TOOLS = "com.nothing.island.ai.DEBUG_QUICK_TOOLS"
        const val ACTION_MASCOT = "com.nothing.island.ai.DEBUG_MASCOT"
        const val EXTRA_ENABLED = "enabled"
    }
}
