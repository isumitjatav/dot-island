package com.nothing.island.ai

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat

/**
 * IslandOverlayService
 *
 * Foreground service hosting the Nothing Island overlay. Maintains lifecycle
 * persistence across applications, lock/unlock cycles, and system configurations.
 */
class IslandOverlayService : Service() {

    companion object {
        private const val TAG = "IslandOverlayService"
        private const val CHANNEL_ID = "nothing_island_overlay_channel"
        private const val NOTIFICATION_ID = 4040

        const val ACTION_START = "com.nothing.island.ai.ACTION_START"
        const val ACTION_STOP = "com.nothing.island.ai.ACTION_STOP"
        const val ACTION_TOGGLE = "com.nothing.island.ai.ACTION_TOGGLE"

        @Volatile
        var isRunning: Boolean = false
            private set

        fun startService(context: Context) {
            val intent = Intent(context, IslandOverlayService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, IslandOverlayService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    private var controller: IslandOverlayController? = null
    private var screenReceiver: BroadcastReceiver? = null

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "IslandOverlayService created.")
        createNotificationChannel()
        controller = IslandOverlayController.getInstance(this)
        registerScreenStateReceiver()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START

        when (action) {
            ACTION_STOP -> {
                Log.i(TAG, "Stopping Nothing Island service via action.")
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_TOGGLE -> {
                controller?.toggleExpand()
            }
            ACTION_START -> {
                startAsForeground()
                if (IslandAccessibilityService.isRunning || OverlayPermissionManager.isAccessibilityServiceEnabled(this)) {
                    Log.i(TAG, "Accessibility service is active as sole overlay owner. IslandOverlayService will not mount a competing window.")
                    isRunning = true
                } else if (OverlayPermissionManager.hasOverlayPermission(this)) {
                    controller?.attachApplicationOverlay(this)
                    isRunning = true
                } else {
                    Log.w(TAG, "SYSTEM_ALERT_WINDOW permission missing. Cannot mount overlay view.")
                }
            }
        }

        return START_STICKY
    }

    private fun startAsForeground() {
        val notification = buildServiceNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) { // Android 14+ / API 34+
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun buildServiceNotification(): Notification {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = Intent(this, IslandOverlayService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Nothing Island")
            .setContentText("Punch-hole overlay engine active")
            .setSmallIcon(android.R.drawable.sym_def_app_icon)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Nothing Island Overlay Engine",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Maintains persistent camera punch-hole floating overlay"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun registerScreenStateReceiver() {
        screenReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    Intent.ACTION_SCREEN_OFF -> {
                        Log.d(TAG, "Screen OFF detected: pausing overlay lifecycle.")
                        controller?.onScreenOff()
                    }
                    Intent.ACTION_SCREEN_ON -> {
                        Log.d(TAG, "Screen ON detected: resuming overlay lifecycle.")
                        controller?.onScreenOn()
                    }
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
        }
        registerReceiver(screenReceiver, filter)
    }

    override fun onDestroy() {
        Log.i(TAG, "IslandOverlayService being destroyed.")
        isRunning = false

        screenReceiver?.let {
            try {
                unregisterReceiver(it)
            } catch (e: Exception) {
                Log.e(TAG, "Error unregistering screen receiver", e)
            }
            screenReceiver = null
        }

        controller?.detach()
        controller = null

        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
