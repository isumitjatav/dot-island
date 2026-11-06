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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.sin

/**
 * Visualizer Style 2: Waveform
 *
 * Renders a continuous, fluid audio waveform curve with harmonic peaks.
 * Freezes cleanly into a subtle baseline curve when paused.
 */
@Composable
fun WaveformVisualizer(
    isPlaying: Boolean,
    accentColor: Color,
    speed: Float = 1.0f,
    intensity: Float = 1.0f,
    glowIntensity: Float = 0.5f,
    reducedMotion: Boolean = false,
    modifier: Modifier = Modifier
) {
    val effectiveSpeed = if (reducedMotion) 0.4f else speed
    val effectiveIntensity = if (reducedMotion) 0.35f else intensity

    val infiniteTransition = rememberInfiniteTransition(label = "WaveformAnim")
    val durationMs = (1500 / effectiveSpeed.coerceAtLeast(0.1f)).toInt()

    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (isPlaying) 6.28318f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMs, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "WaveformPhase"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val centerY = height / 2f

        val strokeWidth = 2.5f
        val points = 32
        val step = width / (points - 1).coerceAtLeast(1)

        val path = Path()
        val fillPath = Path()

        for (i in 0 until points) {
            val x = i * step
            val normX = i.toFloat() / points.toFloat()

            // Envelope to attenuate wave at edges
            val envelope = sin(normX * 3.14159f)

            val wave1 = if (isPlaying) sin(normX * 8f + phase) else sin(normX * 4f) * 0.2f
            val wave2 = if (isPlaying) sin(normX * 16f - phase * 1.5f) * 0.4f else 0f
            val amplitude = (wave1 + wave2) * (height * 0.35f) * effectiveIntensity * envelope

            val y = centerY + amplitude

            if (i == 0) {
                path.moveTo(x, y)
                fillPath.moveTo(x, height)
                fillPath.lineTo(x, y)
            } else {
                path.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
        }

        fillPath.lineTo(width, height)
        fillPath.close()

        // Draw soft ambient fill below curve
        if (glowIntensity > 0f) {
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        accentColor.copy(alpha = 0.25f * glowIntensity),
                        Color.Transparent
                    ),
                    startY = centerY - height * 0.3f,
                    endY = height
                )
            )
        }

        // Draw active waveform stroke
        drawPath(
            path = path,
            color = accentColor,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
    }
}
