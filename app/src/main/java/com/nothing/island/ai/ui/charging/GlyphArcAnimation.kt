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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/**
 * Style 1: Glyph Arc Animation (Nothing Signature)
 *
 * Concentric segmented Glyph lighting ring with orbiting white arc.
 * Inspired by the physical Nothing Phone Glyph interface.
 */
@Composable
fun GlyphArcAnimation(
    percentage: Int,
    isCharging: Boolean,
    accentColor: Color,
    speed: Float = 1.0f,
    intensity: Float = 1.0f,
    glowIntensity: Float = 0.6f,
    reducedMotion: Boolean = false,
    modifier: Modifier = Modifier
) {
    val durationMs = if (reducedMotion) 6000 else (2200 / speed.coerceIn(0.5f, 2.0f)).toInt()
    val transition = rememberInfiniteTransition(label = "GlyphArcTransition")

    val orbitAngle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMs, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "glyphOrbitAngle"
    )

    val pulseAlpha by transition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glyphPulseAlpha"
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

            val strokeWidth = (3.dp.toPx() * intensity).coerceIn(2f, 16f)
            val trackColor = Color.White.copy(alpha = 0.12f)

            // 1. Static Segmented Background Tracks (3 sectors with 15 deg gaps)
            val segments = 3
            val gapAngle = 18f
            val segmentAngle = (360f - (segments * gapAngle)) / segments

            for (i in 0 until segments) {
                val start = i * (segmentAngle + gapAngle)
                drawArc(
                    color = trackColor,
                    startAngle = start,
                    sweepAngle = segmentAngle,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2f, radius * 2f),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }

            // 2. Proportional Battery Level Fill Arc
            val levelSweep = (percentage.coerceIn(0, 100) / 100f) * 360f
            if (levelSweep > 0f) {
                drawArc(
                    color = Color.White.copy(alpha = 0.35f),
                    startAngle = -90f,
                    sweepAngle = levelSweep,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2f, radius * 2f),
                    style = Stroke(width = strokeWidth * 0.8f, cap = StrokeCap.Round)
                )
            }

            // 3. Orbiting White Glyph Pulse Arc (when charging and not reduced motion)
            if (isCharging && !reducedMotion) {
                val arcSweep = 75f
                val headAngle = (orbitAngle - 90f) % 360f

                drawArc(
                    brush = Brush.sweepGradient(
                        0.0f to Color.Transparent,
                        0.7f to Color.White.copy(alpha = pulseAlpha),
                        1.0f to Color.White,
                        center = center
                    ),
                    startAngle = headAngle,
                    sweepAngle = arcSweep,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2f, radius * 2f),
                    style = Stroke(width = strokeWidth * 1.25f, cap = StrokeCap.Round)
                )

                // Leading glow dot at the tip of the arc
                val rad = Math.toRadians((headAngle + arcSweep).toDouble())
                val dotX = center.x + (radius * Math.cos(rad)).toFloat()
                val dotY = center.y + (radius * Math.sin(rad)).toFloat()
                drawCircle(
                    color = Color.White,
                    radius = strokeWidth * 1.1f,
                    center = Offset(dotX, dotY)
                )
                if (glowIntensity > 0f) {
                    drawCircle(
                        color = accentColor.copy(alpha = (0.5f * glowIntensity).coerceIn(0.1f, 0.8f)),
                        radius = strokeWidth * 2.2f,
                        center = Offset(dotX, dotY)
                    )
                }
            } else if (isCharging) {
                // Reduced motion fallback: steady accent arc
                drawArc(
                    color = accentColor,
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
