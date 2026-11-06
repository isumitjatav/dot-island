package com.nothing.island.ai.ui.charging

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Style 9: Digital Gauge Animation (Technical & Minimalist)
 *
 * Precision 270-degree calibrated technical tick dial.
 * Ticks illuminate progressively up to the active battery percentage.
 * Adapts progressive meter from Reference A #32 & #34.
 */
@Composable
fun DigitalGaugeAnimation(
    percentage: Int,
    isCharging: Boolean,
    accentColor: Color,
    speed: Float = 1.0f,
    intensity: Float = 1.0f,
    glowIntensity: Float = 0.6f,
    reducedMotion: Boolean = false,
    modifier: Modifier = Modifier
) {
    val totalTicks = 16
    val durationMs = if (reducedMotion) 4000 else (1200 / speed.coerceIn(0.5f, 2.0f)).toInt()
    val transition = rememberInfiniteTransition(label = "DigitalGaugeTransition")

    val pulseAlpha by transition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMs, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gaugePulseAlpha"
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val minDim = size.minDimension
            val outerR = (minDim / 2f) - 6.dp.toPx()
            val innerR = outerR - (7.dp.toPx() * intensity).coerceIn(4f, 24f)
            if (outerR <= 0f || innerR <= 0f) return@Canvas

            val strokeWidth = (2.2.dp.toPx() * intensity).coerceIn(1.8f, 10f)

            // Sweep from 135 deg to 405 deg (270 deg total)
            val startAngleDeg = 135f
            val totalSweepDeg = 270f
            val activeTicksCount = ((percentage.coerceIn(0, 100) / 100f) * totalTicks).toInt()

            for (i in 0 until totalTicks) {
                val frac = i.toFloat() / (totalTicks - 1).coerceAtLeast(1)
                val angleDeg = startAngleDeg + (frac * totalSweepDeg)
                val angleRad = angleDeg * (PI / 180f)

                val isLit = i < activeTicksCount
                val isLeading = i == activeTicksCount - 1

                val tickColor = when {
                    isLeading && isCharging && !reducedMotion -> accentColor.copy(alpha = pulseAlpha)
                    isLit -> if (isCharging) accentColor else Color.White
                    else -> Color.White.copy(alpha = 0.15f)
                }

                val pStart = Offset(
                    center.x + (innerR * cos(angleRad)).toFloat(),
                    center.y + (innerR * sin(angleRad)).toFloat()
                )
                val pEnd = Offset(
                    center.x + (outerR * cos(angleRad)).toFloat(),
                    center.y + (outerR * sin(angleRad)).toFloat()
                )

                drawLine(
                    color = tickColor,
                    start = pStart,
                    end = pEnd,
                    strokeWidth = if (isLit) strokeWidth * 1.15f else strokeWidth,
                    cap = StrokeCap.Round
                )
            }

            // Central Core Accent Pip
            drawCircle(
                color = if (isCharging) accentColor.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.2f),
                radius = 3.dp.toPx(),
                center = center
            )
        }
    }
}
