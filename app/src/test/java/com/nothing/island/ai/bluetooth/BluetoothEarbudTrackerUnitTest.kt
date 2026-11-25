package com.nothing.island.ai.bluetooth

import android.content.BroadcastReceiver
import com.nothing.island.ai.engine.IslandEvent
import com.nothing.island.ai.ui.components.formatEarbudBatteryDetailText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BluetoothEarbudTrackerUnitTest {

    private class FakeBluetoothBridge(
        var permissionGranted: Boolean = true,
        var adapterEnabled: Boolean = true,
        var queriedDevices: List<BluetoothDeviceSnapshot> = emptyList()
    ) : BluetoothPlatformBridge {
        var registered = false
        override fun hasConnectPermission(): Boolean = permissionGranted
        override fun isAdapterEnabled(): Boolean = adapterEnabled
        override fun queryConnectedAudioDevices(): List<BluetoothDeviceSnapshot> = queriedDevices
        override fun registerReceiver(receiver: BroadcastReceiver) { registered = true }
        override fun unregisterReceiver(receiver: BroadcastReceiver) { registered = false }
    }

    @Test
    fun connectedDevice_emitsConnectedEventWithUnknownBatterySafely() {
        val events = mutableListOf<IslandEvent>()
        val bridge = FakeBluetoothBridge()
        val tracker = BluetoothEarbudTracker(bridge, { events.add(it) }, timeProvider = { 1000L })

        tracker.onDeviceConnected(
            BluetoothDeviceSnapshot(
                name = "Nothing Ear (a)",
                address = "AA:BB:CC:00:00:01",
                battery = EarbudBatterySnapshot.UNKNOWN,
                isAudioActive = true
            )
        )

        assertEquals(1, events.size)
        val evt = events[0] as IslandEvent.BluetoothDeviceConnected
        assertEquals("Nothing Ear (a)", evt.deviceName)
        assertNull(evt.batteryLevelPercentage)
        assertNull(evt.leftBatteryPercentage)
        assertNull(evt.rightBatteryPercentage)
        assertNull(evt.caseBatteryPercentage)
    }

    @Test
    fun partialBatteryData_preservesOnlyValidPercentagesAndFormatsDetails() {
        val sanitized = EarbudBatteryParser.sanitizeSnapshot(
            primary = -1,
            left = 80,
            right = 150, // invalid -> null
            caseLevel = 65
        )
        assertNull(sanitized.primaryPercent)
        assertEquals(80, sanitized.leftPercent)
        assertNull(sanitized.rightPercent)
        assertEquals(65, sanitized.casePercent)
        assertEquals(80, sanitized.representativePercent())

        val event = IslandEvent.BluetoothDeviceConnected(
            id = "bt_1",
            deviceName = "Nothing Ear",
            deviceAddress = "AA:BB:CC:00:00:02",
            batteryLevelPercentage = sanitized.representativePercent(),
            leftBatteryPercentage = sanitized.leftPercent,
            rightBatteryPercentage = sanitized.rightPercent,
            caseBatteryPercentage = sanitized.casePercent
        )
        assertEquals("L 80% · C 65%", formatEarbudBatteryDetailText(event))
    }

    @Test
    fun disconnectedDevice_emitsDisconnectedEventAndPromotesNextDevice() {
        val events = mutableListOf<IslandEvent>()
        var clock = 1000L
        val bridge = FakeBluetoothBridge()
        val tracker = BluetoothEarbudTracker(bridge, { events.add(it) }, timeProvider = { clock })

        val devA = BluetoothDeviceSnapshot("Buds A", "00:00:00:00:00:0A", EarbudBatterySnapshot(primaryPercent = 90), isAudioActive = true)
        val devB = BluetoothDeviceSnapshot("Buds B", "00:00:00:00:00:0B", EarbudBatterySnapshot(primaryPercent = 50), isAudioActive = false)

        tracker.onDeviceConnected(devA)
        clock = 2000L
        tracker.onDeviceConnected(devB)

        // devA remains active because it has active audio routing
        assertEquals("00:00:00:00:00:0A", tracker.activeDevice.value?.address)

        clock = 3000L
        tracker.onDeviceDisconnected("00:00:00:00:00:0A")

        val disc = events.first { it is IslandEvent.BluetoothDeviceDisconnected } as IslandEvent.BluetoothDeviceDisconnected
        assertEquals("Buds A", disc.deviceName)
        assertEquals("00:00:00:00:00:0B", tracker.activeDevice.value?.address)
    }

    @Test
    fun permissionDenied_doesNotCrashAndFlagsMissingPermission() {
        val events = mutableListOf<IslandEvent>()
        val bridge = FakeBluetoothBridge(permissionGranted = false)
        val tracker = BluetoothEarbudTracker(bridge, { events.add(it) })

        tracker.start()
        tracker.refreshConnectedDevices(emitConnectedEvent = true)

        assertTrue(tracker.isPermissionMissing.value)
        assertTrue(tracker.connectedDevices.value.isEmpty())
        assertTrue(events.isEmpty())
        tracker.stop()
        assertFalse(bridge.registered)
    }

    @Test
    fun adapterDisabled_clearsTrackedDevicesAndNotifiesDisconnect() {
        val events = mutableListOf<IslandEvent>()
        val bridge = FakeBluetoothBridge(permissionGranted = true, adapterEnabled = true)
        val tracker = BluetoothEarbudTracker(bridge, { events.add(it) })

        tracker.onDeviceConnected(BluetoothDeviceSnapshot("AirPods Pro", "11:22:33:44:55:66", isAudioActive = true))
        bridge.adapterEnabled = false
        tracker.handleAdapterDisabled()

        assertNull(tracker.activeDevice.value)
        assertTrue(events.any { it is IslandEvent.BluetoothDeviceDisconnected && it.deviceName == "AirPods Pro" })
    }
}
