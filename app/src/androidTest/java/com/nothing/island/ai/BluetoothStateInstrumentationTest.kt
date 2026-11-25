package com.nothing.island.ai

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nothing.island.ai.bluetooth.AndroidBluetoothPlatformBridge
import com.nothing.island.ai.bluetooth.BluetoothDeviceSnapshot
import com.nothing.island.ai.bluetooth.BluetoothEarbudTracker
import com.nothing.island.ai.bluetooth.BluetoothPlatformBridge
import com.nothing.island.ai.bluetooth.EarbudBatteryParser
import com.nothing.island.ai.bluetooth.EarbudBatterySnapshot
import com.nothing.island.ai.engine.IslandEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumentation validation for Android Bluetooth permission/adapter safety and intent parsing.
 */
@RunWith(AndroidJUnit4::class)
class BluetoothStateInstrumentationTest {

    @Test
    fun androidBluetoothBridge_queriesSafelyWithoutThrowingWhenPermissionMissing() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val bridge = AndroidBluetoothPlatformBridge(context)

        // Must never throw SecurityException even if BLUETOOTH_CONNECT is ungranted on device
        val hasPerm = bridge.hasConnectPermission()
        val adapterOn = bridge.isAdapterEnabled()
        val devices = bridge.queryConnectedAudioDevices()

        if (!hasPerm || !adapterOn) {
            assertTrue(devices.isEmpty())
        }
    }

    @Test
    fun earbudTracker_handlesRealAndroidBatteryIntentAndAdapterOffBroadcast() {
        val emitted = mutableListOf<IslandEvent>()
        val fakeBridge = object : BluetoothPlatformBridge {
            override fun hasConnectPermission(): Boolean = true
            override fun isAdapterEnabled(): Boolean = true
            override fun queryConnectedAudioDevices(): List<BluetoothDeviceSnapshot> = emptyList()
            override fun registerReceiver(receiver: BroadcastReceiver) {}
            override fun unregisterReceiver(receiver: BroadcastReceiver) {}
        }

        val tracker = BluetoothEarbudTracker(fakeBridge, { emitted.add(it) }, timeProvider = { 42L })
        tracker.onDeviceConnected(
            BluetoothDeviceSnapshot(
                name = "Nothing Ear (2)",
                address = "00:11:22:33:44:55",
                battery = EarbudBatterySnapshot(leftPercent = 85, rightPercent = 90),
                isAudioActive = true
            )
        )

        val conn = emitted.last() as IslandEvent.BluetoothDeviceConnected
        assertEquals("Nothing Ear (2)", conn.deviceName)
        assertEquals(85, conn.leftBatteryPercentage)
        assertEquals(90, conn.rightBatteryPercentage)
        assertNull(conn.caseBatteryPercentage)

        // Simulate Android BluetoothAdapter turning OFF intent
        val offIntent = Intent(BluetoothAdapter.ACTION_STATE_CHANGED).apply {
            putExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF)
        }
        tracker.handleIntent(offIntent)
        assertTrue(emitted.last() is IslandEvent.BluetoothDeviceDisconnected)
    }

    @Test
    fun batteryMetadataBytes_parseAcrossRealCharsetEncodingSafely() {
        assertEquals(72, EarbudBatteryParser.parseMetadataBytes("72".toByteArray(Charsets.UTF_8)))
        assertNull(EarbudBatteryParser.parseMetadataBytes("-1".toByteArray(Charsets.UTF_8)))
        assertNull(EarbudBatteryParser.parseMetadataBytes("invalid".toByteArray(Charsets.UTF_8)))
    }
}
