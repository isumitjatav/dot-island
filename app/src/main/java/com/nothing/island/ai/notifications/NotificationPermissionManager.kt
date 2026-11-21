package com.nothing.island.ai.notifications

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings

/**
 * Helper for the Notification Listener permission that powers the island's
 * notification and media pills. Without this, the app receives no real alerts.
 */
object NotificationPermissionManager {

    const val LISTENER_COMPONENT =
        "com.nothing.island.ai/com.nothing.island.ai.notifications.IslandNotificationListenerService"

    /** True when our listener service is in the system's enabled listener set. */
    fun isNotificationListenerEnabled(context: Context): Boolean {
        val flat = try {
            Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners"
            )
        } catch (_: Exception) {
            null
        } ?: return false
        return flat.split(':').any { entry ->
            entry.equals(LISTENER_COMPONENT, ignoreCase = true)
        }
    }

    /** Opens the system screen where the user grants notification access. */
    fun createNotificationAccessIntent(context: Context): Intent {
        return Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(":settings:fragment_args_key", LISTENER_COMPONENT)
            putExtra(
                ":settings:show_fragment_args",
                android.os.Bundle().apply {
                    putString(":settings:fragment_args_key", LISTENER_COMPONENT)
                }
            )
        }
    }

    /** ComponentName form, useful for dumps/tests. */
    fun listenerComponentName(): ComponentName =
        ComponentName.unflattenFromString(LISTENER_COMPONENT)!!
}
