package com.nothing.island.ai.hardware

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.nothing.island.ai.engine.IslandEvent

/**
 * Handles flashlight / torch state monitoring and toggling via CameraManager (REQ-60).
 */
class TorchCallbackHandler(
    private val context: Context,
    private val onEvent: (IslandEvent) -> Unit
) {
    companion object {
        private const val TAG = "TorchCallbackHandler"
    }

    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
    private val mainHandler = Handler(Looper.getMainLooper())
    private var primaryCameraId: String? = null
    private var isTorchOn: Boolean = false

    private val torchCallback = object : CameraManager.TorchCallback() {
        override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
            super.onTorchModeChanged(cameraId, enabled)
            if (cameraId == primaryCameraId || primaryCameraId == null) {
                primaryCameraId = cameraId
                isTorchOn = enabled
                Log.d(TAG, "Torch mode changed: cameraId=$cameraId, enabled=$enabled")
                onEvent(IslandEvent.TorchStateChanged(isEnabled = enabled))
            }
        }

        override fun onTorchModeUnavailable(cameraId: String) {
            super.onTorchModeUnavailable(cameraId)
            if (cameraId == primaryCameraId) {
                isTorchOn = false
            }
        }
    }

    fun start() {
        try {
            cameraManager?.let { cm ->
                // Identify camera ID with flash unit
                for (id in cm.cameraIdList) {
                    val characteristics = cm.getCameraCharacteristics(id)
                    val hasFlash = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) ?: false
                    val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                    if (hasFlash && facing == CameraCharacteristics.LENS_FACING_BACK) {
                        primaryCameraId = id
                        break
                    }
                }
                if (primaryCameraId == null && cm.cameraIdList.isNotEmpty()) {
                    primaryCameraId = cm.cameraIdList[0]
                }
                cm.registerTorchCallback(torchCallback, mainHandler)
                Log.d(TAG, "Registered TorchCallback on camera: $primaryCameraId")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register TorchCallback", e)
        }
    }

    fun stop() {
        try {
            cameraManager?.unregisterTorchCallback(torchCallback)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to unregister TorchCallback", e)
        }
    }

    fun toggleTorch(): Boolean {
        val id = primaryCameraId ?: return false
        return try {
            val targetState = !isTorchOn
            cameraManager?.setTorchMode(id, targetState)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set torch mode", e)
            false
        }
    }

    fun isEnabled(): Boolean = isTorchOn
}
