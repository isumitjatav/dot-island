package com.nothing.island.ai.ui

import android.media.ToneGenerator
import com.nothing.island.ai.ui.components.CallControlsUiState
import com.nothing.island.ai.ui.components.dtmfToneForChar
import com.nothing.island.ai.ui.components.formatCallDuration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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

    @Test
    fun numpadToggle_switchesExpandedStateAndControls() {
        val initial = CallControlsUiState(controlsExpanded = false, numpadExpanded = false)
        val opened = initial.toggleNumpad()
        assertTrue(opened.controlsExpanded)
        assertTrue(opened.numpadExpanded)

        val closed = opened.toggleNumpad()
        assertTrue(closed.controlsExpanded)
        assertFalse(closed.numpadExpanded)

        val collapsedAll = opened.toggleControls()
        assertFalse(collapsedAll.controlsExpanded)
        assertFalse(collapsedAll.numpadExpanded)
    }

    @Test
    fun dtmfBuffer_appendsDigitsAndHandlesBackspace() {
        var state = CallControlsUiState()
        "123*0#".forEach { ch ->
            state = state.appendDtmfDigit(ch)
        }
        assertEquals("123*0#", state.dtmfDigits)

        state = state.backspaceDtmf()
        assertEquals("123*0", state.dtmfDigits)

        state = state.clearDtmf()
        assertEquals("", state.dtmfDigits)
    }

    @Test
    fun dtmfBuffer_ignoresInvalidCharsAndMapsTones() {
        val state = CallControlsUiState().appendDtmfDigit('A').appendDtmfDigit('9')
        assertEquals("9", state.dtmfDigits)
        assertEquals(ToneGenerator.TONE_DTMF_9, dtmfToneForChar('9'))
        assertEquals(ToneGenerator.TONE_DTMF_S, dtmfToneForChar('*'))
        assertEquals(ToneGenerator.TONE_DTMF_P, dtmfToneForChar('#'))
    }
}
