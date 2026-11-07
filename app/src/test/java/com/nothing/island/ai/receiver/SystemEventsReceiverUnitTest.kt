package com.nothing.island.ai.receiver

import android.media.AudioManager
import com.nothing.island.ai.engine.IslandEvent
import com.nothing.island.ai.engine.IslandPriority
import com.nothing.island.ai.engine.UnlockMethod
import com.nothing.island.ai.gestures.RingerState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SystemEventsReceiverUnitTest {

    private val emittedEvents = mutableListOf<IslandEvent>()
    private lateinit var receiver: SystemEventsReceiver

    @Before
    fun setUp() {
        emittedEvents.clear()
        receiver = SystemEventsReceiver { event ->
            emittedEvents.add(event)
        }
    }

    @Test
    fun ringerModeChanged_emitsCorrectRingerStates() {
        receiver.handleRingerModeChanged(AudioManager.RINGER_MODE_SILENT)
        assertEquals(1, emittedEvents.size)
        val event1 = emittedEvents[0] as IslandEvent.RingerModeChanged
        assertEquals(RingerState.SILENT, event1.ringerMode)
        assertEquals(IslandPriority.SYSTEM_HARDWARE, event1.priority)

        // Duplicate ringer mode should be deduplicated
        receiver.handleRingerModeChanged(AudioManager.RINGER_MODE_SILENT)
        assertEquals(1, emittedEvents.size)

        // Transition to Vibrate
        receiver.handleRingerModeChanged(AudioManager.RINGER_MODE_VIBRATE)
        assertEquals(2, emittedEvents.size)
        val event2 = emittedEvents[1] as IslandEvent.RingerModeChanged
        assertEquals(RingerState.VIBRATE, event2.ringerMode)

        // Transition to Normal
        receiver.handleRingerModeChanged(AudioManager.RINGER_MODE_NORMAL)
        assertEquals(3, emittedEvents.size)
        val event3 = emittedEvents[2] as IslandEvent.RingerModeChanged
        assertEquals(RingerState.NORMAL, event3.ringerMode)
    }

    @Test
    fun dndStateChanged_emitsCorrectStates() {
        receiver.handleDndStateChanged(isDndActive = true)
        assertEquals(1, emittedEvents.size)
        val event1 = emittedEvents[0] as IslandEvent.DndStateChanged
        assertTrue(event1.isEnabled)
        assertEquals(IslandPriority.SYSTEM_HARDWARE, event1.priority)

        // Duplicate DND state should be deduplicated
        receiver.handleDndStateChanged(isDndActive = true)
        assertEquals(1, emittedEvents.size)

        // Transition to DND inactive
        receiver.handleDndStateChanged(isDndActive = false)
        assertEquals(2, emittedEvents.size)
        val event2 = emittedEvents[1] as IslandEvent.DndStateChanged
        assertFalse(event2.isEnabled)
    }

    @Test
    fun userPresent_emitsDeviceUnlockedWithZeroBiometricTelemetry() {
        receiver.handleUserPresent()
        assertEquals(1, emittedEvents.size)
        val event = emittedEvents[0] as IslandEvent.DeviceUnlocked

        assertEquals("SystemEventsReceiver", event.source)
        assertEquals(IslandPriority.SYSTEM_HARDWARE, event.priority)
        assertEquals(2500L, event.timeoutMs)
        assertEquals(UnlockMethod.PIN_PATTERN, event.unlockMethod)
    }

    @Test
    fun batteryChanged_triggersAt20And10PercentThresholdsWhenDischarging() {
        // Initial state at 25%
        receiver.handleBatteryChanged(level = 25, scale = 100, isCharging = false)
        assertEquals(0, emittedEvents.size)

        // Drop to 20% -> triggers low battery alert
        receiver.handleBatteryChanged(level = 20, scale = 100, isCharging = false)
        assertEquals(1, emittedEvents.size)
        val alert20 = emittedEvents[0] as IslandEvent.BatteryThresholdWarning
        assertEquals(20, alert20.batteryLevel)
        assertFalse(alert20.isCritical)
        assertEquals(IslandPriority.SAFETY_ALERT, alert20.priority)

        // Drop to 18% -> no spurious alert
        receiver.handleBatteryChanged(level = 18, scale = 100, isCharging = false)
        assertEquals(1, emittedEvents.size)

        // Drop to 10% -> triggers critical battery alert
        receiver.handleBatteryChanged(level = 10, scale = 100, isCharging = false)
        assertEquals(2, emittedEvents.size)
        val alert10 = emittedEvents[1] as IslandEvent.BatteryThresholdWarning
        assertEquals(10, alert10.batteryLevel)
        assertTrue(alert10.isCritical)
    }

    @Test
    fun batteryChanged_suppressesWarningWhileCharging() {
        // Drop to 20% while plugged in -> suppressed
        receiver.handleBatteryChanged(level = 20, scale = 100, isCharging = true)
        assertEquals(0, emittedEvents.size)
    }
}
