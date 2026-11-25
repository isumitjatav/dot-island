package com.nothing.island.ai

import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nothing.island.ai.gestures.AccessibilityScreenshotHandler
import com.nothing.island.ai.gestures.AccessibilityScreenshotServiceBridge
import com.nothing.island.ai.gestures.GestureAction
import com.nothing.island.ai.gestures.IslandGestureActionExecutor
import com.nothing.island.ai.gestures.ScreenshotActionResult
import com.nothing.island.ai.gestures.ScreenshotFailureReason
import com.nothing.island.ai.gestures.ScreenshotServiceBridge
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumentation validation for AccessibilityService screenshot routing and Settings intents.
 */
@RunWith(AndroidJUnit4::class)
class AccessibilityAndScreenshotInstrumentationTest {

    @Test
    fun accessibilityBridge_reportsRuntimeSdkSafely() {
        assertEquals(Build.VERSION.SDK_INT, AccessibilityScreenshotServiceBridge.sdkInt())
        // Should not crash even when AccessibilityService is unbound in test process
        val available = AccessibilityScreenshotServiceBridge.isServiceAvailable()
        if (!available) {
            val result = AccessibilityScreenshotHandler(AccessibilityScreenshotServiceBridge).triggerScreenshot()
            assertTrue(result is ScreenshotActionResult.Failure)
            assertEquals(
                ScreenshotFailureReason.SERVICE_UNAVAILABLE,
                (result as ScreenshotActionResult.Failure).reason
            )
        }
    }

    @Test
    fun screenshotExecutor_routesToBridgeAndPropagatesResult() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val fakeBridge = object : ScreenshotServiceBridge {
            override fun isServiceAvailable(): Boolean = true
            override fun sdkInt(): Int = Build.VERSION.SDK_INT
            override fun performGlobalScreenshot(): Boolean = true
        }
        var received: ScreenshotActionResult? = null
        val executor = IslandGestureActionExecutor(
            context = context,
            onToggleExpand = {},
            onClearAllNotifications = {},
            screenshotHandler = AccessibilityScreenshotHandler(fakeBridge),
            onScreenshotResult = { received = it }
        )

        executor.execute(GestureAction.SCREENSHOT)
        assertEquals(ScreenshotActionResult.Success, received)
        assertEquals(ScreenshotActionResult.Success, executor.lastScreenshotResult.value)
    }

    @Test
    fun accessibilitySettingsIntent_resolvesOnDevicePackageManager() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val intent = OverlayPermissionManager.createAccessibilitySettingsIntent()
        val resolved = context.packageManager.resolveActivity(intent, 0)
        assertNotNull("Device must resolve ACTION_ACCESSIBILITY_SETTINGS", resolved)
    }
}
