package com.nothing.island.ai

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent

/**
 * IslandAccessibilityService
 *
 * Hosts Nothing Island floating overlay with WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY (2032).
 * Sits at AOSP Base Layer 24000 (above SystemUI StatusBar at Layer 22000), allowing 100% direct
 * touch reception across the centered camera cutout on Nothing Phone (4a).
 *
 * Strictly operates under a Zero-Data Privacy Contract:
 * - Zero screen content retrieval (canRetrieveWindowContent = false)
 * - Zero gesture injection (canPerformGestures = false)
 * - Zero key event monitoring (canRequestFilterKeyEvents = false)
 * - Zero event processing / data harvesting.
 */
class IslandAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "IslandAccessibilityService"

        @Volatile
        var isRunning: Boolean = false
            private set

        @Volatile
        var instance: IslandAccessibilityService? = null
            private set
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.i(TAG, "IslandAccessibilityService connected. Initializing accessibility overlay host.")
        isRunning = true
        instance = this

        try {
            com.nothing.island.ai.notifications.IslandNotificationListenerService.requestRebindIfNecessary(this)
            val controller = IslandOverlayController.getInstance(this)
            controller.attachAccessibilityOverlay(this)
            Log.i(TAG, "Accessibility overlay attached successfully at Layer 24000.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to attach accessibility overlay", e)
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val pkg = event.packageName?.toString()
            if (!pkg.isNullOrBlank()) {
                val controller = IslandOverlayController.getInstance(this)
                controller.onForegroundPackageChanged(pkg)
            }
        }
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        try {
            val controller = IslandOverlayController.getInstance(this)
            controller.onConfigurationChanged(newConfig)
        } catch (_: Exception) {}
    }

    override fun onInterrupt() {
        Log.w(TAG, "IslandAccessibilityService interrupted by system.")
    }

    /**
     * Performs the Android global screenshot action (`minSdk = 29` >= API 28).
     * Returns true when handed off to the system screenshot handler, false otherwise.
     */
    fun performGlobalScreenshotAction(): Boolean {
        return try {
            performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to perform global screenshot action", e)
            false
        }
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.i(TAG, "IslandAccessibilityService unbinding. Cleaning up overlay window.")
        try {
            val controller = IslandOverlayController.getInstance(this)
            controller.detach()
        } catch (e: Exception) {
            Log.e(TAG, "Error detaching overlay during onUnbind", e)
        }
        isRunning = false
        instance = null
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        Log.i(TAG, "IslandAccessibilityService destroying.")
        try {
            val controller = IslandOverlayController.getInstance(this)
            controller.detach()
        } catch (e: Exception) {
            Log.e(TAG, "Error detaching overlay during onDestroy", e)
        }
        isRunning = false
        instance = null
        super.onDestroy()
    }
}
