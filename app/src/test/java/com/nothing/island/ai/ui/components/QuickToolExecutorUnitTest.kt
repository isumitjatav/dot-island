package com.nothing.island.ai.ui.components

import android.provider.Settings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuickToolExecutorUnitTest {

    private class FakeQuickToolBridge(
        var torchSupported: Boolean = true,
        var settingsAvailable: Boolean = true
    ) : QuickToolPlatformBridge {
        var torchState = false
        val launchedSettings = mutableListOf<String>()
        var mainLaunched = false

        override fun toggleTorch(): Boolean {
            if (!torchSupported) return false
            torchState = !torchState
            return true
        }

        override fun isTorchEnabled(): Boolean = torchState

        override fun launchSettings(primaryAction: String, fallbackAction: String?): String? {
            if (!settingsAvailable) return null
            launchedSettings.add(primaryAction)
            return primaryAction
        }

        override fun launchAppMain(): Boolean {
            if (!settingsAvailable) return false
            mainLaunched = true
            return true
        }
    }

    @Test
    fun torchToggle_executesDirectlyWithoutCollapsingOverlay() {
        val bridge = FakeQuickToolBridge(torchSupported = true)
        var collapsed = false
        val executor = QuickToolExecutor(bridge = bridge, onCollapse = { collapsed = true })

        val result = executor.execute(QuickTools.TORCH)

        assertTrue(result is QuickToolExecutionResult.DirectToggleSuccess)
        assertTrue((result as QuickToolExecutionResult.DirectToggleSuccess).isEnabled == true)
        assertFalse(collapsed)
    }

    @Test
    fun protectedToggles_openSettingsFallbackAndCollapse() {
        val bridge = FakeQuickToolBridge(settingsAvailable = true)
        var now = 1000L
        var collapseCount = 0
        val executor = QuickToolExecutor(
            bridge = bridge,
            onCollapse = { collapseCount++ },
            timeProvider = { now }
        )

        val wifiRes = executor.execute(QuickTools.WIFI)
        assertEquals(QuickToolExecutionResult.SettingsOpened(QuickTools.WIFI, Settings.ACTION_WIFI_SETTINGS), wifiRes)

        now += 1000L
        val dataRes = executor.execute(QuickTools.DATA)
        assertEquals(QuickToolExecutionResult.SettingsOpened(QuickTools.DATA, Settings.ACTION_DATA_ROAMING_SETTINGS), dataRes)

        now += 1000L
        val btRes = executor.execute(QuickTools.BLUETOOTH)
        assertEquals(QuickToolExecutionResult.SettingsOpened(QuickTools.BLUETOOTH, Settings.ACTION_BLUETOOTH_SETTINGS), btRes)

        now += 1000L
        val locRes = executor.execute(QuickTools.LOCATION)
        assertEquals(QuickToolExecutionResult.SettingsOpened(QuickTools.LOCATION, Settings.ACTION_LOCATION_SOURCE_SETTINGS), locRes)

        assertEquals(4, collapseCount)
        assertEquals(4, bridge.launchedSettings.size)
    }

    @Test
    fun repeatedTapsWithinCooldown_areDebouncedWithoutDuplicateLaunches() {
        val bridge = FakeQuickToolBridge(settingsAvailable = true)
        var now = 10_000L
        val executor = QuickToolExecutor(bridge = bridge, debounceWindowMs = 450L, timeProvider = { now })

        val first = executor.execute(QuickTools.WIFI)
        now += 100L
        val second = executor.execute(QuickTools.WIFI)

        assertTrue(first is QuickToolExecutionResult.SettingsOpened)
        assertEquals(QuickToolExecutionResult.Debounced(QuickTools.WIFI), second)
        assertEquals(1, bridge.launchedSettings.size)
    }

    @Test
    fun unavailableSettingsActivity_returnsUnavailableWithoutCrashing() {
        val bridge = FakeQuickToolBridge(settingsAvailable = false)
        var collapsed = false
        val executor = QuickToolExecutor(bridge = bridge, onCollapse = { collapsed = true })

        val res = executor.execute(QuickTools.LOCATION)

        assertTrue(res is QuickToolExecutionResult.Unavailable)
        assertFalse(collapsed)
    }
}
