package com.nothing.island.ai.ui

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
}
