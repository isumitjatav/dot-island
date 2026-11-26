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
                    intent.getBooleanExtra(EXTRA_ENABLED, false)
                )
            }
            ACTION_QUICK_TOOLS -> {
                IslandOverlayController.getInstance(context).repository.expandQuickTools()
            }
            ACTION_COLLAPSE -> {
                IslandOverlayController.getInstance(context).collapse()
            }
            ACTION_CALL -> {
                val isDialing = intent.getBooleanExtra("is_dialing", false)
                val isRinging = intent.getBooleanExtra("ringing", false)
                val withPhoto = intent.getBooleanExtra("photo", false)
                val duration = if (isDialing) 0L else intent.getLongExtra("duration", 8L)
                val expand = intent.getBooleanExtra("expand", false)
                val callerName = intent.getStringExtra(EXTRA_NAME) ?: "Daisy Williams"
                val demoPhoto = if (withPhoto) createDemoCallerBitmap() else null
                if (isRinging) {
                    controller.eventEngine.emit(
                        IslandEvent.IncomingCallRinging(
                            id = "debug_ringing_${System.currentTimeMillis()}",
                            callId = "debug_call_id",
                            callerDisplayName = callerName,
                            contactPhoto = demoPhoto,
                        )
                    )
                } else {
                    controller.eventEngine.emit(
                        IslandEvent.CallConnected(
                            id = "debug_call_${System.currentTimeMillis()}",
                            callId = "debug_call_id",
                            callerDisplayName = callerName,
                            durationSeconds = duration,
                            isDialing = isDialing,
                            contactPhoto = demoPhoto,
                        )
                    )
                }
                if (expand) {
                    controller.expandCall()
                }
            }
            ACTION_NOTIFICATION -> {
                val title = intent.getStringExtra("title") ?: "ALEX"
                val text = intent.getStringExtra("text") ?: "ON MY WAY"
                val pkg = intent.getStringExtra("pkg") ?: "com.google.android.apps.messaging"
                val expand = intent.getBooleanExtra("expand", false)
                controller.eventEngine.emit(
                    IslandEvent.NotificationPosted(
                        id = "debug_notif_${System.currentTimeMillis()}",
                        packageName = pkg,
                        title = title,
                        text = text,
                    )
                )
                if (expand) {
                    controller.expandNotificationFocused()
                }
            }
            ACTION_MUSIC -> {
                val playing = intent.getBooleanExtra(EXTRA_ENABLED, true)
                controller.mediaTracker.injectTrackForTesting(
                    com.nothing.island.ai.media.MediaTrackState(
                        sessionId = "debug_session",
                        packageName = "com.debug.music",
                        appDisplayName = "DEBUG MUSIC",
                        trackTitle = intent.getStringExtra(EXTRA_NAME) ?: "Circles",
                        artistName = "Post Malone",
                        isPlaying = playing,
                        durationMs = 215_000L,
                        currentPositionMs = 90_000L,
                        isHistorical = false,
                    )
                )
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

    private fun createDemoCallerBitmap(): android.graphics.Bitmap {
        val bmp = android.graphics.Bitmap.createBitmap(64, 64, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bmp)
        canvas.drawColor(android.graphics.Color.rgb(34, 48, 66))
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.rgb(215, 25, 33)
        }
        canvas.drawCircle(32f, 24f, 12f, paint)
        canvas.drawCircle(32f, 56f, 20f, paint)
        return bmp
    }

    companion object {
        const val ACTION_EARBUD = "com.nothing.island.ai.DEBUG_EARBUD"
        const val ACTION_EARBUD_DISCONNECT = "com.nothing.island.ai.DEBUG_EARBUD_DISCONNECT"
        const val EXTRA_NAME = "name"
        const val EXTRA_BATTERY = "battery"
        const val ACTION_DUAL_PILL = "com.nothing.island.ai.DEBUG_DUAL_PILL"
        const val ACTION_QUICK_TOOLS = "com.nothing.island.ai.DEBUG_QUICK_TOOLS"
        const val ACTION_MASCOT = "com.nothing.island.ai.DEBUG_MASCOT"
        const val ACTION_MUSIC = "com.nothing.island.ai.DEBUG_MUSIC"
        const val ACTION_CALL = "com.nothing.island.ai.DEBUG_CALL"
        const val ACTION_NOTIFICATION = "com.nothing.island.ai.DEBUG_NOTIFICATION"
        const val ACTION_COLLAPSE = "com.nothing.island.ai.DEBUG_COLLAPSE"
        const val EXTRA_ENABLED = "enabled"
    }
}
