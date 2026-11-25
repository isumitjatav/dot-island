package com.nothing.island.ai.gestures

import android.os.Build
import com.nothing.island.ai.IslandAccessibilityService

enum class ScreenshotFailureReason {
    SERVICE_UNAVAILABLE,
    UNSUPPORTED_API_LEVEL,
    ACTION_FAILED
}

sealed interface ScreenshotActionResult {
    data object Success : ScreenshotActionResult
    data class Failure(
        val reason: ScreenshotFailureReason,
        val message: String
    ) : ScreenshotActionResult
}

/**
 * Abstraction over AccessibilityService global screenshot capability for unit/instrumentation testing
 * without coupling gesture execution to Android framework singletons.
 */
interface ScreenshotServiceBridge {
    fun isServiceAvailable(): Boolean
    fun sdkInt(): Int
    fun performGlobalScreenshot(): Boolean
}

object AccessibilityScreenshotServiceBridge : ScreenshotServiceBridge {
    override fun isServiceAvailable(): Boolean =
        IslandAccessibilityService.isRunning && IslandAccessibilityService.instance != null

    override fun sdkInt(): Int = Build.VERSION.SDK_INT

    override fun performGlobalScreenshot(): Boolean =
        IslandAccessibilityService.instance?.performGlobalScreenshotAction() == true
}

/**
 * Routes the screenshot gesture action to the active AccessibilityService global action API.
 */
class AccessibilityScreenshotHandler(
    private val bridge: ScreenshotServiceBridge = AccessibilityScreenshotServiceBridge
) {
    fun triggerScreenshot(): ScreenshotActionResult {
        if (!bridge.isServiceAvailable()) {
            return ScreenshotActionResult.Failure(
                reason = ScreenshotFailureReason.SERVICE_UNAVAILABLE,
                message = "Dot Island Accessibility Service is not enabled or connected."
            )
        }
        val sdk = bridge.sdkInt()
        if (sdk < Build.VERSION_CODES.P) {
            return ScreenshotActionResult.Failure(
                reason = ScreenshotFailureReason.UNSUPPORTED_API_LEVEL,
                message = "Global screenshot requires Android 9 (API 28+); current API=$sdk."
            )
        }
        val performed = runCatching { bridge.performGlobalScreenshot() }.getOrDefault(false)
        return if (performed) {
            ScreenshotActionResult.Success
        } else {
            ScreenshotActionResult.Failure(
                reason = ScreenshotFailureReason.ACTION_FAILED,
                message = "AccessibilityService.performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT) returned false."
            )
        }
    }
}
