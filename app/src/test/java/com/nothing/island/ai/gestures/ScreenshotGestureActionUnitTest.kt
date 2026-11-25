package com.nothing.island.ai.gestures

import android.content.ContextWrapper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenshotGestureActionUnitTest {

    private class FakeScreenshotBridge(
        var serviceAvailable: Boolean = true,
        var sdk: Int = 36,
        var performResult: Boolean = true
    ) : ScreenshotServiceBridge {
        var performCalls = 0
        override fun isServiceAvailable(): Boolean = serviceAvailable
        override fun sdkInt(): Int = sdk
        override fun performGlobalScreenshot(): Boolean {
            performCalls++
            return performResult
        }
    }

    @Test
    fun screenshotAction_succeedsWhenServiceAvailableAndSupportedApi() {
        val bridge = FakeScreenshotBridge(serviceAvailable = true, sdk = 34, performResult = true)
        val handler = AccessibilityScreenshotHandler(bridge)
        var callbackResult: ScreenshotActionResult? = null
        val executor = IslandGestureActionExecutor(
            context = object : ContextWrapper(null) {},
            onToggleExpand = {},
            onClearAllNotifications = {},
            screenshotHandler = handler,
            onScreenshotResult = { callbackResult = it }
        )

        executor.execute(GestureAction.SCREENSHOT)

        assertEquals(1, bridge.performCalls)
        assertEquals(ScreenshotActionResult.Success, callbackResult)
        assertEquals(ScreenshotActionResult.Success, executor.lastScreenshotResult.value)
    }

    @Test
    fun screenshotAction_failsGracefullyWhenServiceUnavailable() {
        val bridge = FakeScreenshotBridge(serviceAvailable = false, sdk = 34, performResult = true)
        val executor = IslandGestureActionExecutor(
            context = object : ContextWrapper(null) {},
            onToggleExpand = {},
            onClearAllNotifications = {},
            screenshotHandler = AccessibilityScreenshotHandler(bridge)
        )

        executor.execute(GestureAction.SCREENSHOT)

        assertEquals(0, bridge.performCalls)
        val result = executor.lastScreenshotResult.value as ScreenshotActionResult.Failure
        assertEquals(ScreenshotFailureReason.SERVICE_UNAVAILABLE, result.reason)
    }

    @Test
    fun screenshotAction_failsGracefullyBelowApi28() {
        val bridge = FakeScreenshotBridge(serviceAvailable = true, sdk = 27, performResult = true)
        val handler = AccessibilityScreenshotHandler(bridge)

        val result = handler.triggerScreenshot() as ScreenshotActionResult.Failure
        assertEquals(ScreenshotFailureReason.UNSUPPORTED_API_LEVEL, result.reason)
        assertEquals(0, bridge.performCalls)
    }

    @Test
    fun screenshotAction_reportsActionFailureWhenPerformReturnsFalse() {
        val bridge = FakeScreenshotBridge(serviceAvailable = true, sdk = 34, performResult = false)
        val handler = AccessibilityScreenshotHandler(bridge)

        val result = handler.triggerScreenshot() as ScreenshotActionResult.Failure
        assertEquals(ScreenshotFailureReason.ACTION_FAILED, result.reason)
        assertEquals(1, bridge.performCalls)
    }

    @Test
    fun otherGestures_stillInvokeCallbacksWithoutRegression() {
        val bridge = FakeScreenshotBridge()
        var expanded = false
        var cleared = false
        val executor = IslandGestureActionExecutor(
            context = object : ContextWrapper(null) {},
            onToggleExpand = { expanded = true },
            onClearAllNotifications = { cleared = true },
            screenshotHandler = AccessibilityScreenshotHandler(bridge)
        )

        executor.execute(GestureAction.NONE)
        assertFalse(expanded)
        assertFalse(cleared)
        assertEquals(0, bridge.performCalls)

        executor.execute(GestureAction.EXPAND_COLLAPSE)
        assertTrue(expanded)

        executor.execute(GestureAction.CLEAR_ALL)
        assertTrue(cleared)
        assertEquals(0, bridge.performCalls)
    }
}
