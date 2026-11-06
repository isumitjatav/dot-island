package com.nothing.island.ai.ui.visualizers

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import kotlin.math.sin

/**
 * Visualizer Style 1: Vertical Equalizer (Default)
 *
 * Renders dynamic vertical equalizer bars with Nothing OS dot-matrix and segmented styling.
 * Audio-reactive behavior: Harmonically oscillates in sync with playback.
 * Freeze invariant: When playback is paused, bars instantly freeze at resting heights.
 */
@Composable
fun VerticalEqualizerVisualizer(
    isPlaying: Boolean,
    accentColor: Color,
    speed: Float = 1.0f,
    intensity: Float = 1.0f,
    glowIntensity: Float = 0.5f,
    reducedMotion: Boolean = false,
    barCount: Int = 5,
    modifier: Modifier = Modifier
) {
    val effectiveSpeed = if (reducedMotion) 0.5f else speed
    val effectiveIntensity = if (reducedMotion) 0.4f else intensity

    val infiniteTransition = rememberInfiniteTransition(label = "EqualizerAnim")
    val cycleTimeMs = (1200 / effectiveSpeed.coerceAtLeast(0.1f)).toInt()

    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (isPlaying) 6.28318f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = cycleTimeMs, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "EqualizerPhase"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        val totalSpacing = width * 0.25f
        val barSpacing = totalSpacing / (barCount + 1).coerceAtLeast(1)
        val barWidth = (width - (barSpacing * (barCount + 1))) / barCount.coerceAtLeast(1)

        val minBarHeight = height * 0.15f
        val maxBarHeight = height * 0.90f
        val dynamicRange = (maxBarHeight - minBarHeight) * effectiveIntensity.coerceIn(0.2f, 1.5f)

        for (i in 0 until barCount) {
            val barPhaseOffset = i * 1.15f
            val wave = if (isPlaying) {
                (sin(phase + barPhaseOffset) * 0.5f + 0.5f)
            } else {
                0.2f // Steady resting height when paused
            }

            val currentBarHeight = (minBarHeight + wave * dynamicRange).coerceIn(minBarHeight, height)
            val x = barSpacing + i * (barWidth + barSpacing)
            val y = height - currentBarHeight

            // Draw glowing background bar reflection if glow enabled
            if (glowIntensity > 0f) {
                drawRoundRect(
                    color = accentColor.copy(alpha = 0.20f * glowIntensity),
                    topLeft = Offset(x - 1f, y - 1f),
                    size = Size(barWidth + 2f, currentBarHeight + 2f),
                    cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                )
            }

            // Draw main active bar
            drawRoundRect(
                color = accentColor,
                topLeft = Offset(x, y),
                size = Size(barWidth, currentBarHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}
