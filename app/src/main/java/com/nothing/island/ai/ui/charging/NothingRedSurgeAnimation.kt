package com.nothing.island.ai.ui.charging

import androidx.compose.animation.core.CubicBezierEasing
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/**
 * Style 4: Nothing Red Surge Animation (Nothing Signature)
 *
 * Surgical Nothing Red energy surge tracing the contour, leaving a ruby particle trail.
 * Adapts directional energy surge from Reference A Wave #8.
 */
@Composable
fun NothingRedSurgeAnimation(
    percentage: Int,
    isCharging: Boolean,
    accentColor: Color = Color(0xFFD71920),
    speed: Float = 1.0f,
    intensity: Float = 1.0f,
    glowIntensity: Float = 0.6f,
    reducedMotion: Boolean = false,
    modifier: Modifier = Modifier
) {
    val durationMs = if (reducedMotion) 4000 else (1600 / speed.coerceIn(0.5f, 2.0f)).toInt()
    val transition = rememberInfiniteTransition(label = "RedSurgeTransition")

    val surgeAngle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMs, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "surgeAngle"
    )

    val breathingAlpha by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "surgeBreathing"
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val minDim = size.minDimension
            val radius = (minDim / 2f) - 6.dp.toPx()
            if (radius <= 0f) return@Canvas

            val strokeWidth = (3.dp.toPx() * intensity).coerceIn(2f, 14f)
            val nothingRed = Color(0xFFD71920)

            // 1. Subtle Background Track in Muted Nothing Red
            drawCircle(
                color = nothingRed.copy(alpha = 0.15f),
                radius = radius,
                center = center,
                style = Stroke(width = strokeWidth)
            )

            // 2. Battery Percentage Gauge (Muted Red progress)
            val levelSweep = (percentage.coerceIn(0, 100) / 100f) * 360f
            if (levelSweep > 0f) {
                drawArc(
                    color = nothingRed.copy(alpha = 0.45f),
                    startAngle = -90f,
                    sweepAngle = levelSweep,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2f, radius * 2f),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }

            // 3. Dynamic Red Surge Head & Radiant Trail (Active when charging)
            if (isCharging && !reducedMotion) {
                val tailLength = 80f
                val headAngle = (surgeAngle - 90f) % 360f

                drawArc(
                    brush = Brush.sweepGradient(
                        0.0f to Color.Transparent,
                        0.7f to nothingRed.copy(alpha = breathingAlpha),
                        0.95f to Color.White,
                        1.0f to Color.White,
                        center = center
                    ),
                    startAngle = headAngle,
                    sweepAngle = tailLength,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2f, radius * 2f),
                    style = Stroke(width = strokeWidth * 1.3f, cap = StrokeCap.Round)
                )

                // White-hot core spark at surge tip
                val tipRad = Math.toRadians((headAngle + tailLength).toDouble())
                val tipX = center.x + (radius * cos(tipRad)).toFloat()
                val tipY = center.y + (radius * sin(tipRad)).toFloat()

                drawCircle(
                    color = Color.White,
                    radius = strokeWidth * 1.0f,
                    center = Offset(tipX, tipY)
                )

                // Surrounding ruby red glow
                if (glowIntensity > 0f) {
                    drawCircle(
                        color = nothingRed.copy(alpha = 0.5f * glowIntensity),
                        radius = strokeWidth * 2.5f,
                        center = Offset(tipX, tipY)
                    )
                }
            } else if (isCharging) {
                // Reduced motion fallback: steady solid red level arc
                drawArc(
                    color = nothingRed,
                    startAngle = -90f,
                    sweepAngle = levelSweep,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2f, radius * 2f),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
        }
    }
}
