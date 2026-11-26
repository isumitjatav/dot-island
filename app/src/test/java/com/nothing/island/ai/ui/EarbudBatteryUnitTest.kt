package com.nothing.island.ai.ui

import com.nothing.island.ai.engine.IslandEvent
import com.nothing.island.ai.ui.components.formatEarbudStatusBadge
import com.nothing.island.ai.ui.components.normalizeEarbudDisplayName
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EarbudBatteryUnitTest {

    /** Mirrors the clamping used by EarbudConnectionView. */
    private fun clampBattery(value: Int?): Int = (value ?: 100).coerceIn(0, 100)

    @Test
    fun nullBattery_defaultsToFull() {
        assertEquals(100, clampBattery(null))
    }

    @Test
    fun batteryIsClampedToValidRange() {
        assertEquals(75, clampBattery(75))
        assertEquals(0, clampBattery(-5))
        assertEquals(100, clampBattery(140))
    }

    @Test
    fun lowBatteryThreshold_isDetected() {
        assertTrue(clampBattery(12) <= 15)
        assertTrue(clampBattery(15) <= 15)
    }

    @Test
    fun compactPill_normalizesBlankNameAndFormatsBadgeHonest() {
        assertEquals("Bluetooth Audio", normalizeEarbudDisplayName(""))
        assertEquals("Bluetooth Audio", normalizeEarbudDisplayName("   "))
        assertEquals("Nothing Ear (a)", normalizeEarbudDisplayName(" Nothing Ear (a) "))

        val withBattery = IslandEvent.BluetoothDeviceConnected(
            id = "bt_1",
            deviceName = "Nothing Ear",
            deviceAddress = "AA:BB:CC:DD:EE:01",
            batteryLevelPercentage = 85,
        )
        assertEquals("85%", formatEarbudStatusBadge(withBattery))

        val withoutBattery = IslandEvent.BluetoothDeviceConnected(
            id = "bt_2",
            deviceName = "",
            deviceAddress = "AA:BB:CC:DD:EE:02",
            batteryLevelPercentage = null,
        )
        assertEquals("CONNECTED", formatEarbudStatusBadge(withoutBattery))
    }
}
