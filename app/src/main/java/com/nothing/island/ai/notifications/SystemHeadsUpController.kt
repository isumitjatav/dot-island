package com.nothing.island.ai.notifications

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * SystemHeadsUpController
 *
 * Manages the global system heads-up notifications mute state via
 * Settings.Global.heads_up_notifications_enabled.
 *
 * Requires android.permission.WRITE_SECURE_SETTINGS (signature|privileged).
 * On standard Android builds this permission must be granted via ADB or Shizuku:
 *   adb shell pm grant com.nothing.island.ai android.permission.WRITE_SECURE_SETTINGS
 *
 * Automatically restores heads_up_notifications_enabled = 1 when Dot Island is
 * disabled or the notification listener is disconnected, ensuring users are never
 * stuck with muted system banners.
 */
class SystemHeadsUpController(
    private val context: Context,
    private val permissionChecker: (Context) -> Boolean = { ctx ->
        ContextCompat.checkSelfPermission(
            ctx,
            android.Manifest.permission.WRITE_SECURE_SETTINGS
        ) == PackageManager.PERMISSION_GRANTED
    },
    private val settingsReader: (Context) -> Int = { ctx ->
        try {
            Settings.Global.getInt(ctx.contentResolver, SETTING_KEY, 1)
        } catch (_: Exception) {
            1
        }
    },
    private val settingsWriter: (Context, Int) -> Boolean = { ctx, value ->
        try {
            Settings.Global.putInt(ctx.contentResolver, SETTING_KEY, value)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write heads_up_notifications_enabled", e)
            false
        }
    }
) {

    companion object {
        private const val TAG = "SystemHeadsUpCtrl"
        const val SETTING_KEY = "heads_up_notifications_enabled"
    }

    /**
     * Checks if the app currently holds the WRITE_SECURE_SETTINGS permission.
     * This is a signature|privileged permission that must be granted via ADB or Shizuku.
     */
    fun hasSecureSettingsPermission(): Boolean {
        return permissionChecker(context)
    }

    /**
     * Reads the current system heads-up mute state.
     * Returns true if system heads-up banners are currently muted (0).
     */
    fun isSystemHeadsUpMuted(): Boolean {
        return settingsReader(context) == 0
    }

    /**
     * Sets the system heads-up mute state.
     *
     * @param muted true to mute system heads-up banners, false to unmute
     * @return true if the write succeeded, false if permission is missing or write failed
     */
    fun setSystemHeadsUpMuted(muted: Boolean): Boolean {
        if (!hasSecureSettingsPermission()) {
            Log.w(TAG, "WRITE_SECURE_SETTINGS not granted, cannot toggle heads-up mute")
            return false
        }

        val success = settingsWriter(context, if (muted) 0 else 1)
        if (success) {
            Log.i(TAG, "System heads-up banners ${if (muted) "muted" else "restored"}")
        }
        return success
    }

    /**
     * Restores system heads-up banners to enabled state (value = 1).
     * Called automatically on listener disconnect or app disable to ensure
     * users are never stuck with muted system banners.
     */
    fun restoreSystemHeadsUp(): Boolean {
        if (!hasSecureSettingsPermission()) {
            Log.w(TAG, "WRITE_SECURE_SETTINGS not granted, cannot restore heads-up")
            return false
        }

        val success = settingsWriter(context, 1)
        if (success) {
            Log.i(TAG, "System heads-up banners restored to enabled (cleanup)")
        }
        return success
    }

    /**
     * Creates an intent to open the notification settings for the Phone app
     * so users can manually disable "Pop on screen" if WRITE_SECURE_SETTINGS
     * is not available.
     */
    fun createPhoneNotificationSettingsIntent(): android.content.Intent {
        return android.content.Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, "com.google.android.dialer")
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    /**
     * Creates an intent to open the notification settings for the Messages app
     * so users can manually disable "Pop on screen" if WRITE_SECURE_SETTINGS
     * is not available.
     */
    fun createMessagesNotificationSettingsIntent(): android.content.Intent {
        return android.content.Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, "com.google.android.apps.messaging")
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    /**
     * Creates a fallback intent to general notification settings.
     */
    fun createGeneralNotificationSettingsIntent(): android.content.Intent {
        return android.content.Intent(android.provider.Settings.ACTION_SETTINGS).apply {
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
}

/**
 * Convenience extension for CoroutineScope to run heads-up operations off the main thread.
 */
fun CoroutineScope.withSystemHeadsUp(
    controller: SystemHeadsUpController,
    block: (SystemHeadsUpController) -> Unit
) {
    launch(Dispatchers.IO) {
        block(controller)
    }
}