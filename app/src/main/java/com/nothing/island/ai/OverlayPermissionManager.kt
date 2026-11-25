package com.nothing.island.ai

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.text.TextUtils
import androidx.core.content.ContextCompat

/**
 * OverlayPermissionManager
 *
 * Verifies and requests SYSTEM_ALERT_WINDOW, BIND_ACCESSIBILITY_SERVICE, and
 * POST_NOTIFICATIONS permissions required for the Nothing Island overlay.
 */
object OverlayPermissionManager {

    /**
     * Checks if the application has been granted permission to draw overlays
     * on top of other applications.
     */
    fun hasOverlayPermission(context: Context): Boolean {
        return Settings.canDrawOverlays(context)
    }

    /**
     * Creates an Intent to navigate directly to the System Alert Window settings page
     * for this application.
     */
    fun createOverlayPermissionIntent(context: Context): Intent {
        return Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    /**
     * Checks if Nothing Island's IslandAccessibilityService is currently enabled
     * by the user in Android Accessibility Settings.
     */
    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val expectedComponentName = ComponentName(context, IslandAccessibilityService::class.java)
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        val colonSplitter = TextUtils.SimpleStringSplitter(':')
        colonSplitter.setString(enabledServices)
        while (colonSplitter.hasNext()) {
            val componentNameString = colonSplitter.next()
            val enabledComponent = ComponentName.unflattenFromString(componentNameString)
            if (enabledComponent != null && enabledComponent == expectedComponentName) {
                return true
            }
        }
        return false
    }

    /**
     * Creates an Intent to navigate the user directly to Android Accessibility Settings.
     */
    fun createAccessibilitySettingsIntent(): Intent {
        return Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    /**
     * Checks if notification posting is permitted (required on Android 13+ / API 33+ for
     * foreground service persistence).
     */
    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    /**
     * Checks if Nothing Island has been granted Notification Listener access
     * in Android Notification Access Settings (Milestone R6).
     */
    fun hasNotificationListenerPermission(context: Context): Boolean {
        val enabledPackages = androidx.core.app.NotificationManagerCompat.getEnabledListenerPackages(context)
        return enabledPackages.contains(context.packageName)
    }

    /**
     * Creates an Intent navigating the user directly to Notification Listener Settings.
     */
    fun createNotificationListenerSettingsIntent(): Intent {
        return Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    /**
     * Checks whether the app is exempted from OEM battery optimizations so the floating
     * capsule service is not killed in the background.
     */
    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? android.os.PowerManager
        return powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: false
    }

    /**
     * Creates an Intent to request unrestricted background battery status for Dot Island.
     */
    fun createBatteryOptimizationIntent(context: Context): Intent {
        return Intent(
            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
            Uri.parse("package:${context.packageName}")
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    /**
     * Launches battery optimization exemption settings, falling back gracefully to the
     * general Battery Optimization list or App Info page on strict OEM ROMs.
     */
    fun launchBatteryOptimizationSettings(context: Context) {
        val candidates = listOf(
            createBatteryOptimizationIntent(context),
            Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:${context.packageName}")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
        for (intent in candidates) {
            try {
                context.startActivity(intent)
                return
            } catch (_: Exception) {
            }
        }
    }
}
