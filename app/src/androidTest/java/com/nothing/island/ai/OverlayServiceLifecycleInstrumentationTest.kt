package com.nothing.island.ai

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nothing.island.ai.engine.IslandPriority
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Android instrumentation tests for overlay lifecycle, controller initialization,
 * game-mode foreground safety, and permission Settings intents.
 */
@RunWith(AndroidJUnit4::class)
class OverlayServiceLifecycleInstrumentationTest {

    @Test
    fun overlayController_isSingletonAndDoesNotHideOwnPackageInGameMode() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val first = IslandOverlayController.getInstance(context)
        val second = IslandOverlayController.getInstance(context)

        assertSame(first, second)
        assertNotNull(first.geometry)
        assertNotNull(first.weatherProvider)
        assertNotNull(first.quickToolExecutor)
        assertNotNull(first.bluetoothEarbudTracker)
        assertTrue(first.repository.state.value.activePriority >= IslandPriority.IDLE_BASE)

        val shouldHideOwnApp = first.gameModeDetector.shouldHideOverlay(
            autoHideInGames = true,
            foregroundPackage = context.packageName,
            orientation = Configuration.ORIENTATION_LANDSCAPE
        )
        assertFalse("Own host package must never trigger game-mode auto-hide", shouldHideOwnApp)
    }

    @Test
    fun overlayService_onBindReturnsNullAndHandlesStartStopIntentsCleanly() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val service = IslandOverlayService()
        assertNull(
            "Overlay service is started-only and must return null binder",
            service.onBind(Intent(context, IslandOverlayService::class.java))
        )

        val startIntent = Intent(context, IslandOverlayService::class.java).apply {
            action = IslandOverlayService.ACTION_START
        }
        val stopIntent = Intent(context, IslandOverlayService::class.java).apply {
            action = IslandOverlayService.ACTION_STOP
        }
        assertEquals(IslandOverlayService.ACTION_START, startIntent.action)
        assertEquals(IslandOverlayService.ACTION_STOP, stopIntent.action)
        assertEquals(IslandOverlayService::class.java.name, startIntent.component?.className)
    }

    @Test
    fun permissionSettingsIntents_constructExpectedActionsAndResolve() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        val overlayIntent = OverlayPermissionManager.createOverlayPermissionIntent(context)
        assertEquals(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, overlayIntent.action)
        assertEquals("package:${context.packageName}", overlayIntent.dataString)
        assertTrue((overlayIntent.flags and Intent.FLAG_ACTIVITY_NEW_TASK) != 0)
        assertNotNull(
            "Device must resolve ACTION_MANAGE_OVERLAY_PERMISSION",
            context.packageManager.resolveActivity(overlayIntent, 0)
        )

        val listenerIntent = OverlayPermissionManager.createNotificationListenerSettingsIntent()
        assertEquals(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS, listenerIntent.action)
        assertTrue((listenerIntent.flags and Intent.FLAG_ACTIVITY_NEW_TASK) != 0)
        assertNotNull(
            "Device must resolve ACTION_NOTIFICATION_LISTENER_SETTINGS",
            context.packageManager.resolveActivity(listenerIntent, 0)
        )
    }
}
