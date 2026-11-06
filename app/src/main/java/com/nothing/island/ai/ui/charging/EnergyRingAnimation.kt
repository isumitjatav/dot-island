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
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/**
 * Style 1: Energy Ring Animation
 *
 * Concentric orbiting energy arcs with particle tick marks.
 * Outer arc orbits clockwise, inner arc orbits counter-clockwise.
 */
@Composable
fun EnergyRingAnimation(
    percentage: Int,
    isCharging: Boolean,
    accentColor: Color,
    speed: Float = 1.0f,
    intensity: Float = 1.0f,
    glowIntensity: Float = 0.6f,
    reducedMotion: Boolean = false,
    modifier: Modifier = Modifier
) {
    val durationMs = if (reducedMotion) 6000 else (2800 / speed.coerceIn(0.5f, 2.0f)).toInt()
    val transition = rememberInfiniteTransition(label = "EnergyRingTransition")

    val outerRotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMs, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "outerRotation"
    )

    val innerRotation by transition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = (durationMs * 1.4f).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "innerRotation"
    )

    val pulseGlow by transition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseGlow"
    )

    Box(
        modifier = modifier.size(110.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val outerRadius = (size.minDimension / 2f) - 6.dp.toPx()
            val innerRadius = outerRadius - 10.dp.toPx()

            // 1. Ambient Background Ring Track
            drawCircle(
                color = Color.White.copy(alpha = 0.08f),
                radius = outerRadius,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.05f),
                radius = innerRadius,
                center = center,
                style = Stroke(width = 1.5.dp.toPx())
            )

            // 2. Outer Progress Arc
            val sweepAngle = if (isCharging) 120f else (percentage.toFloat() / 100f) * 360f
            val startAngle = if (isCharging && !reducedMotion) outerRotation else -90f

            drawArc(
                brush = Brush.sweepGradient(
                    0.0f to accentColor.copy(alpha = 0.1f),
                    0.7f to accentColor.copy(alpha = (pulseGlow * intensity).coerceIn(0.3f, 1.0f)),
                    1.0f to accentColor,
                    center = center
                ),
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = Offset(center.x - outerRadius, center.y - outerRadius),
                size = Size(outerRadius * 2, outerRadius * 2),
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )

            // 3. Inner Counter-Rotating Arc (Charging dynamic)
            if (isCharging) {
                drawArc(
                    color = accentColor.copy(alpha = (0.5f * glowIntensity).coerceIn(0.1f, 0.9f)),
                    startAngle = if (!reducedMotion) innerRotation else 45f,
                    sweepAngle = 90f,
                    useCenter = false,
                    topLeft = Offset(center.x - innerRadius, center.y - innerRadius),
                    size = Size(innerRadius * 2, innerRadius * 2),
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // 4. Dot Matrix Cardinal Ticks (12, 3, 6, 9 o'clock)
            val tickCount = 12
            for (i in 0 until tickCount) {
                val angleDeg = (i * (360f / tickCount))
                val angleRad = Math.toRadians(angleDeg.toDouble())
                val isCardinal = i % 3 == 0
                val tickLen = if (isCardinal) 4.dp.toPx() else 2.dp.toPx()
                val rStart = outerRadius + 3.dp.toPx()
                val rEnd = rStart + tickLen

                val start = Offset(
                    (center.x + rStart * cos(angleRad)).toFloat(),
                    (center.y + rStart * sin(angleRad)).toFloat()
                )
                val end = Offset(
                    (center.x + rEnd * cos(angleRad)).toFloat(),
                    (center.y + rEnd * sin(angleRad)).toFloat()
                )

                val tickAlpha = if (isCardinal) 0.6f else 0.2f
                drawLine(
                    color = if (isCardinal) accentColor.copy(alpha = tickAlpha) else Color.White.copy(alpha = tickAlpha),
                    start = start,
                    end = end,
                    strokeWidth = 1.5.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }
    }
}
