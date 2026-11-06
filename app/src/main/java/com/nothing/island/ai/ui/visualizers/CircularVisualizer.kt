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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import kotlin.math.cos
import kotlin.math.sin

/**
 * Visualizer Style 3: Circular Visualizer
 *
 * Renders radial equalizer spikes emanating outward around a circular center.
 * Ideal for framing circular album artwork or as a standalone orbital visualizer.
 */
@Composable
fun CircularVisualizer(
    isPlaying: Boolean,
    accentColor: Color,
    speed: Float = 1.0f,
    intensity: Float = 1.0f,
    glowIntensity: Float = 0.5f,
    reducedMotion: Boolean = false,
    spikeCount: Int = 24,
    modifier: Modifier = Modifier
) {
    val effectiveSpeed = if (reducedMotion) 0.5f else speed
    val effectiveIntensity = if (reducedMotion) 0.4f else intensity

    val infiniteTransition = rememberInfiniteTransition(label = "CircularAnim")
    val durationMs = (2000 / effectiveSpeed.coerceAtLeast(0.1f)).toInt()

    val rotationPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (isPlaying) 6.28318f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMs, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "CircularRotation"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        val maxRadius = (size.width.coerceAtMost(size.height) / 2f)

        val innerRadius = maxRadius * 0.55f
        val spikeRange = (maxRadius - innerRadius) * effectiveIntensity.coerceIn(0.2f, 1.5f)

        for (i in 0 until spikeCount) {
            val angle = (i.toFloat() / spikeCount.toFloat()) * 6.28318f + rotationPhase
            val wave = if (isPlaying) {
                sin(angle * 3f + rotationPhase * 2f) * 0.5f + 0.5f
            } else {
                0.15f
            }

            val spikeLength = (spikeRange * wave).coerceAtLeast(2f)
            val outerRadius = innerRadius + spikeLength

            val startX = centerX + cos(angle) * innerRadius
            val startY = centerY + sin(angle) * innerRadius

            val endX = centerX + cos(angle) * outerRadius
            val endY = centerY + sin(angle) * outerRadius

            // Ambient glow behind spike
            if (glowIntensity > 0f) {
                drawLine(
                    color = accentColor.copy(alpha = 0.25f * glowIntensity),
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = 4.5f,
                    cap = StrokeCap.Round
                )
            }

            // Main sharp spike
            drawLine(
                color = accentColor,
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = 2.0f,
                cap = StrokeCap.Round
            )
        }
    }
}
