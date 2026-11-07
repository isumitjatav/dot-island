package com.nothing.island.ai.gestures

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.provider.MediaStore
import android.util.Log
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import com.nothing.island.ai.MainActivity
import com.nothing.island.ai.hardware.TorchCallbackHandler
import kotlin.math.abs

/**
 * IslandGestureActionExecutor handles the system execution of user-assigned gesture actions.
 */
class IslandGestureActionExecutor(
    private val context: Context,
    private val torchHandler: TorchCallbackHandler?,
    private val onToggleExpand: () -> Unit,
    private val onClearAllNotifications: () -> Unit
) {
    companion object {
        private const val TAG = "GestureActionExecutor"
    }

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    fun execute(action: GestureAction) {
        Log.d(TAG, "Executing gesture action: $action")
        when (action) {
            GestureAction.NONE -> Unit
            GestureAction.EXPAND_COLLAPSE -> onToggleExpand()
            GestureAction.NOTIFICATIONS_HUB -> {
                val intent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
                context.startActivity(intent)
            }
            GestureAction.TORCH -> {
                torchHandler?.toggleTorch()
            }
            GestureAction.CAMERA -> {
                try {
                    val intent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to launch camera", e)
                }
            }
            GestureAction.SETTINGS -> {
                val intent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            }
            GestureAction.SCREENSHOT -> {
                Log.d(TAG, "Screenshot action requested")
            }
            GestureAction.CLEAR_ALL -> onClearAllNotifications()
        }
    }

    /**
     * REQ-20: Adjusts media volume proportionally to horizontal drag delta without system slider UI.
     */
    fun adjustVolumeDelta(deltaX: Float, totalDragWidth: Float) {
        val am = audioManager ?: return
        try {
            val maxVolume = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            val currentVolume = am.getStreamVolume(AudioManager.STREAM_MUSIC)
            val volumeStep = (deltaX / totalDragWidth) * maxVolume
            val targetVolume = (currentVolume + volumeStep.toInt()).coerceIn(0, maxVolume)
            if (targetVolume != currentVolume) {
                am.setStreamVolume(AudioManager.STREAM_MUSIC, targetVolume, 0)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to adjust stream volume", e)
        }
    }
}

/**
 * Modifier detecting single tap, 500ms long-press, directional swipes, and volume drag.
 */
fun Modifier.islandGestureDetector(
    preferences: GesturePreferences,
    executor: IslandGestureActionExecutor,
    isMediaIslandActive: Boolean = false,
    onVolumeDrag: ((Float) -> Unit)? = null
): Modifier = pointerInput(preferences, isMediaIslandActive) {
    var totalDragX = 0f
    var totalDragY = 0f
    var isLongPressEngaged = false

    detectTapGestures(
        onTap = {
            executor.execute(preferences.singleTapAction)
        },
        onLongPress = {
            isLongPressEngaged = true
            executor.execute(preferences.longPressAction)
        }
    )
}.pointerInput(preferences, isMediaIslandActive) {
    var totalDragX = 0f
    var totalDragY = 0f

    detectDragGestures(
        onDragStart = {
            totalDragX = 0f
            totalDragY = 0f
        },
        onDrag = { change, dragAmount ->
            change.consume()
            totalDragX += dragAmount.x
            totalDragY += dragAmount.y

            if (isMediaIslandActive) {
                // REQ-20: Volume drag on media island
                executor.adjustVolumeDelta(dragAmount.x, 300f)
                onVolumeDrag?.invoke(dragAmount.x)
            }
        },
        onDragEnd = {
            val swipeThreshold = 40f
            if (abs(totalDragX) > abs(totalDragY)) {
                if (totalDragX > swipeThreshold) {
                    executor.execute(preferences.swipeRightAction)
                } else if (totalDragX < -swipeThreshold) {
                    executor.execute(preferences.swipeLeftAction)
                }
            } else {
                if (totalDragY > swipeThreshold) {
                    executor.execute(preferences.swipeDownAction)
                }
            }
        }
    )
}
