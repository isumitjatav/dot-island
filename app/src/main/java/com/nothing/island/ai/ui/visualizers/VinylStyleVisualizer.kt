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
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.cos
import kotlin.math.sin

/**
 * Visualizer Style 4: Vinyl Style
 *
 * Renders an authentic vinyl record with concentric micro-grooves, dynamic light glints,
 * and spinning strobe indicators that rotate smoothly during playback.
 * Freezes instantly in place when paused.
 */
@Composable
fun VinylStyleVisualizer(
    isPlaying: Boolean,
    accentColor: Color,
    speed: Float = 1.0f,
    intensity: Float = 1.0f,
    glowIntensity: Float = 0.5f,
    reducedMotion: Boolean = false,
    modifier: Modifier = Modifier
) {
    val effectiveSpeed = if (reducedMotion) 0.4f else speed

    val infiniteTransition = rememberInfiniteTransition(label = "VinylAnim")
    val durationMs = (3000 / effectiveSpeed.coerceAtLeast(0.1f)).toInt()

    val spinAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (isPlaying) 360f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMs, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "VinylSpin"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        val radius = (size.width.coerceAtMost(size.height) / 2f)

        // Vinyl disk outer rim
        drawCircle(
            color = Color(0xFF141414),
            radius = radius,
            center = Offset(centerX, centerY)
        )

        // Concentric groove rings (micro-grooves)
        val ringCount = 5
        for (i in 1..ringCount) {
            val r = radius * (0.35f + (i * 0.12f))
            drawCircle(
                color = Color(0xFF262626),
                radius = r,
                center = Offset(centerX, centerY),
                style = Stroke(width = 1f)
            )
        }

        // Light reflection sheen across record grooves
        val radAngle = Math.toRadians(spinAngle.toDouble()).toFloat()
        val sheenAngle1 = radAngle
        val sheenAngle2 = radAngle + 3.14159f

        val glintLength = radius * 0.85f
        val glintInner = radius * 0.35f

        // Strobe accent notch dots along the outer rim
        val notchCount = 12
        for (n in 0 until notchCount) {
            val notchAngle = Math.toRadians((spinAngle + (n * (360f / notchCount))).toDouble()).toFloat()
            val nx = centerX + cos(notchAngle) * (radius * 0.92f)
            val ny = centerY + sin(notchAngle) * (radius * 0.92f)

            drawCircle(
                color = if (n % 3 == 0) accentColor else Color(0xFF444444),
                radius = 1.5f,
                center = Offset(nx, ny)
            )
        }

        // Light glint lines
        drawLine(
            color = accentColor.copy(alpha = 0.35f * intensity),
            start = Offset(centerX + cos(sheenAngle1) * glintInner, centerY + sin(sheenAngle1) * glintInner),
            end = Offset(centerX + cos(sheenAngle1) * glintLength, centerY + sin(sheenAngle1) * glintLength),
            strokeWidth = 2.5f,
            cap = StrokeCap.Round
        )

        drawLine(
            color = accentColor.copy(alpha = 0.35f * intensity),
            start = Offset(centerX + cos(sheenAngle2) * glintInner, centerY + sin(sheenAngle2) * glintInner),
            end = Offset(centerX + cos(sheenAngle2) * glintLength, centerY + sin(sheenAngle2) * glintLength),
            strokeWidth = 2.5f,
            cap = StrokeCap.Round
        )

        // Outer glow border
        if (glowIntensity > 0f) {
            drawCircle(
                color = accentColor.copy(alpha = 0.20f * glowIntensity),
                radius = radius,
                center = Offset(centerX, centerY),
                style = Stroke(width = 2f)
            )
        }
    }
}
