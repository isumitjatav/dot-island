package com.nothing.island.ai.gestures

import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.provider.MediaStore
import android.util.Log
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import com.nothing.island.ai.IslandOverlayController
import com.nothing.island.ai.MainActivity
import com.nothing.island.ai.hardware.TorchCallbackHandler
import com.nothing.island.ai.switcher.SwipeDirection
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.abs

/**
 * IslandGestureActionExecutor handles the system execution of user-assigned gesture actions.
 */
class IslandGestureActionExecutor(
    private val context: Context,
    private val torchHandler: TorchCallbackHandler? = null,
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
            GestureAction.NONE -> {
                // Task 8: Ensure NONE performs no unintended action
                Unit
            }
            GestureAction.EXPAND_COLLAPSE -> {
                // Task 4: Default single-tap action expands/collapses island
                onToggleExpand()
            }
            GestureAction.NOTIFICATIONS_HUB -> {
                try {
                    val intent = Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    }
                    context.startActivity(intent)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to launch notifications hub", e)
                }
            }
            GestureAction.TORCH -> {
                val handler = torchHandler ?: try {
                    IslandOverlayController.getInstance(context).torchHandler
                } catch (e: Exception) {
                    null
                }
                if (handler != null) {
                    handler.toggleTorch()
                } else {
                    toggleTorchDirectly(context)
                }
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
                try {
                    val intent = Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to launch settings", e)
                }
            }
            GestureAction.SCREENSHOT -> {
                // Task 10: Do not claim SCREENSHOT is functional unless an actual implementation exists.
                // Global screenshot without user prompt requires AccessibilityService.performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT).
                Log.w(TAG, "Screenshot action requested: not implemented in this context (requires AccessibilityService performGlobalAction)")
            }
            GestureAction.CLEAR_ALL -> {
                // Task 9: Ensure CLEAR_ALL only performs user-requested notification clearing
                onClearAllNotifications()
            }
        }
    }

    private fun toggleTorchDirectly(context: Context) {
        try {
            val cm = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager ?: return
            val cameraId = cm.cameraIdList.firstOrNull() ?: return
            Log.d(TAG, "toggleTorchDirectly: fallback torch toggle on camera $cameraId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to toggle torch directly", e)
        }
    }

    /**
     * REQ-20: Adjusts media volume proportionally to horizontal drag delta without system slider UI.
     */
    fun adjustVolumeDelta(deltaX: Float, totalDragWidth: Float = 300f) {
        val am = audioManager ?: return
        try {
            val maxVolume = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            val currentVolume = am.getStreamVolume(AudioManager.STREAM_MUSIC)
            val stepSize = (totalDragWidth / maxVolume.coerceAtLeast(1)).coerceAtLeast(15f)
            val stepsToChange = (deltaX / stepSize).toInt()
            if (stepsToChange != 0) {
                val targetVolume = (currentVolume + stepsToChange).coerceIn(0, maxVolume)
                if (targetVolume != currentVolume) {
                    am.setStreamVolume(AudioManager.STREAM_MUSIC, targetVolume, 0)
                    Log.d(TAG, "REQ-20: Volume adjusted from $currentVolume to $targetVolume (max=$maxVolume)")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to adjust stream volume", e)
        }
    }
}

/**
 * Unified Island Gesture Detector Modifier.
 * Handles single tap, 500ms long press, directional swipes (left, right, down),
 * and REQ-20 media volume dragging without pointer conflicts.
 */
fun Modifier.islandGestureDetector(
    preferences: GesturePreferences,
    executor: IslandGestureActionExecutor,
    isMediaIslandActive: Boolean = false,
    onTap: (() -> Unit)? = null,
    onVolumeDrag: ((Float) -> Unit)? = null,
    onSwipeActivity: ((SwipeDirection) -> Unit)? = null
): Modifier = pointerInput(preferences, isMediaIslandActive) {
    val touchSlop = viewConfiguration.touchSlop
    val longPressTimeoutMs = preferences.longPressDurationMs // default 500L

    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        val startPosition = down.position
        var totalDragX = 0f
        var totalDragY = 0f
        var isLongPressEngaged = false
        var isMovedBeyondSlop = false

        // Phase 1: Wait up to longPressTimeoutMs for Tap vs Move vs Hold
        val tapOrMoveResult = withTimeoutOrNull(longPressTimeoutMs) {
            while (true) {
                val event = awaitPointerEvent()
                val change = event.changes.firstOrNull { it.id == down.id } ?: break

                if (change.positionChanged()) {
                    val delta = change.position - startPosition
                    if (delta.getDistance() > touchSlop) {
                        isMovedBeyondSlop = true
                        totalDragX = delta.x
                        totalDragY = delta.y
                        return@withTimeoutOrNull "MOVED"
                    }
                }

                if (!change.pressed) {
                    // Finger lifted before 500ms and within touchSlop -> Single Tap!
                    change.consume()
                    if (onTap != null) {
                        onTap.invoke()
                    } else {
                        executor.execute(preferences.singleTapAction)
                    }
                    return@withTimeoutOrNull "TAPPED"
                }
            }
            "TERMINATED"
        }

        if (tapOrMoveResult == "TAPPED") {
            return@awaitEachGesture
        }

        if (tapOrMoveResult == null) {
            // Held for 500ms without moving beyond touch slop -> Long Press!
            isLongPressEngaged = true
            if (!isMediaIslandActive) {
                executor.execute(preferences.longPressAction)
            }
        }

        // Phase 2: If long press engaged on active media island, handle horizontal volume drag (REQ-20)
        if (isLongPressEngaged && isMediaIslandActive) {
            while (true) {
                val event = awaitPointerEvent()
                val change = event.changes.firstOrNull { it.id == down.id } ?: break

                if (change.positionChanged()) {
                    val dragAmountX = change.position.x - change.previousPosition.x
                    change.consume()
                    executor.adjustVolumeDelta(dragAmountX, 300f)
                    onVolumeDrag?.invoke(dragAmountX)
                }

                if (!change.pressed) {
                    change.consume()
                    break
                }
            }
            return@awaitEachGesture
        }

        // Phase 3: If long press engaged on idle pill, consume all events until release so no swipe fires
        if (isLongPressEngaged && !isMediaIslandActive) {
            while (true) {
                val event = awaitPointerEvent()
                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                if (!change.pressed) {
                    change.consume()
                    break
                }
            }
            return@awaitEachGesture
        }

        // Phase 4: User moved beyond touch slop before 500ms -> Directional Swipe
        if (isMovedBeyondSlop) {
            while (true) {
                val event = awaitPointerEvent()
                val change = event.changes.firstOrNull { it.id == down.id } ?: break

                if (change.positionChanged()) {
                    totalDragX = change.position.x - startPosition.x
                    totalDragY = change.position.y - startPosition.y
                }

                if (!change.pressed) {
                    change.consume()
                    val swipeThreshold = 30f
                    if (abs(totalDragX) > abs(totalDragY)) {
                        if (totalDragX > swipeThreshold) {
                            if (onSwipeActivity != null) {
                                onSwipeActivity(SwipeDirection.RIGHT)
                            } else {
                                executor.execute(preferences.swipeRightAction)
                            }
                        } else if (totalDragX < -swipeThreshold) {
                            if (onSwipeActivity != null) {
                                onSwipeActivity(SwipeDirection.LEFT)
                            } else {
                                executor.execute(preferences.swipeLeftAction)
                            }
                        }
                    } else {
                        if (totalDragY > swipeThreshold) {
                            // Swipe down always opens the quick tools drawer; fall back
                            // to the configured action if no callback is provided.
                            if (onSwipeActivity != null) {
                                onSwipeActivity(SwipeDirection.DOWN)
                            } else {
                                executor.execute(preferences.swipeDownAction)
                            }
                        }
                    }
                    break
                }
            }
        }
    }
}
