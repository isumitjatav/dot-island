package com.nothing.island.ai.media

import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Tracks active audio output devices (Bluetooth headphones, wired headsets, phone speakers)
 * using standard AOSP AudioManager APIs.
 */
class AudioOutputDeviceTracker(private val context: Context) {

    companion object {
        private const val TAG = "AudioOutputTracker"
    }

    private val audioManager = context.getSystemService(Context.MEDIA_ROUTER_SERVICE.let { Context.AUDIO_SERVICE }) as AudioManager

    private val _currentOutputName = MutableStateFlow("Phone Speaker")
    val currentOutputName: StateFlow<String> = _currentOutputName.asStateFlow()

    private val deviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
            updateCurrentOutput()
        }

        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
            updateCurrentOutput()
        }
    }

    init {
        try {
            audioManager.registerAudioDeviceCallback(deviceCallback, null)
            updateCurrentOutput()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to register audio device callback", e)
        }
    }

    /**
     * Determines the currently active physical or Bluetooth output device.
     */
    fun updateCurrentOutput() {
        try {
            val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            var resolvedName = "Phone Speaker"

            // Look for Bluetooth A2DP / BLE outputs first
            val btDevice = devices.firstOrNull {
                it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && it.type == AudioDeviceInfo.TYPE_BLE_HEADSET) ||
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && it.type == AudioDeviceInfo.TYPE_BLE_SPEAKER)
            }

            if (btDevice != null) {
                val productName = btDevice.productName?.toString()?.trim()
                resolvedName = if (!productName.isNullOrBlank()) productName else "Bluetooth Audio"
            } else {
                val wiredDevice = devices.firstOrNull {
                    it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                    it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                    it.type == AudioDeviceInfo.TYPE_USB_HEADSET
                }
                if (wiredDevice != null) {
                    resolvedName = "Wired Headphones"
                }
            }

            _currentOutputName.value = resolvedName
            Log.d(TAG, "Active audio output resolved: $resolvedName")
        } catch (e: Exception) {
            Log.w(TAG, "Error resolving audio output device", e)
        }
    }

    fun start() {
        try {
            audioManager.registerAudioDeviceCallback(deviceCallback, null)
            updateCurrentOutput()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to register audio device callback on start", e)
        }
    }

    fun stop() {
        release()
    }

    /**
     * Unregisters listeners when service shuts down.
     */
    fun release() {
        try {
            audioManager.unregisterAudioDeviceCallback(deviceCallback)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to unregister audio device callback", e)
        }
    }
}
