package com.nothing.island.ai.ui

import com.nothing.island.ai.ui.components.formatCallDuration
import org.junit.Assert.assertEquals
import org.junit.Test

class CallCardUnitTest {

    @Test
    fun zeroSeconds_formatsToDoubleZero() {
        assertEquals("00:00", formatCallDuration(0L))
    }

    @Test
    fun secondsUnderOneMinute_formatsCorrectly() {
        assertEquals("00:42", formatCallDuration(42L))
    }

    @Test
    fun mixedMinutesAndSeconds_formatsCorrectly() {
        assertEquals("01:05", formatCallDuration(65L))
        assertEquals("61:01", formatCallDuration(3661L))
    }

    @Test
    fun negativeDuration_isClampedToZero() {
        assertEquals("00:00", formatCallDuration(-5L))
    }
}
