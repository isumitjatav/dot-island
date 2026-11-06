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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/**
 * Style 6: Electric Pulse Animation (Fluid & Elemental)
 *
 * High-frequency electrical discharges with surging lightning glyphs and dynamic discharge arcs.
 * Adapts high-voltage discharge from Reference A #11, #22 & #23.
 */
@Composable
fun ElectricPulseAnimation(
    percentage: Int,
    isCharging: Boolean,
    accentColor: Color = Color(0xFF00E5FF),
    speed: Float = 1.0f,
    intensity: Float = 1.0f,
    glowIntensity: Float = 0.6f,
    reducedMotion: Boolean = false,
    modifier: Modifier = Modifier
) {
    val durationMs = if (reducedMotion) 4000 else (1200 / speed.coerceIn(0.5f, 2.0f)).toInt()
    val transition = rememberInfiniteTransition(label = "ElectricPulseTransition")

    val pulsePhase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMs, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulsePhase"
    )

    val surgeIntensity by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = (durationMs / 2).coerceAtLeast(200), easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "surgeIntensity"
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

            val strokeWidth = (2.dp.toPx() * intensity).coerceIn(1.5f, 10f)

            // 1. Ambient Glow Ring
            val effectiveGlow = (surgeIntensity * glowIntensity * intensity).coerceIn(0.1f, 1f)
            drawCircle(
                color = accentColor.copy(alpha = 0.12f * effectiveGlow),
                radius = radius + (3.dp.toPx() * surgeIntensity),
                center = center
            )
            drawCircle(
                color = accentColor.copy(alpha = 0.25f * effectiveGlow),
                radius = radius,
                center = center,
                style = Stroke(width = strokeWidth * 0.8f)
            )

            // 2. High-Frequency Discharge Arcs (Dynamic jagged branching lines)
            if (isCharging && !reducedMotion) {
                val arcCount = 4
                for (i in 0 until arcCount) {
                    val baseAngle = (i * 90f) + (pulsePhase * 360f)
                    val angleRad = Math.toRadians(baseAngle.toDouble())
                    val midAngleRad = Math.toRadians((baseAngle + (if (i % 2 == 0) 12f else -12f)).toDouble())

                    val startR = radius * 0.45f
                    val midR = radius * 0.75f
                    val endR = radius * 0.98f

                    val p1 = Offset(
                        center.x + (startR * cos(angleRad)).toFloat(),
                        center.y + (startR * sin(angleRad)).toFloat()
                    )
                    val p2 = Offset(
                        center.x + (midR * cos(midAngleRad)).toFloat(),
                        center.y + (midR * sin(midAngleRad)).toFloat()
                    )
                    val p3 = Offset(
                        center.x + (endR * cos(angleRad)).toFloat(),
                        center.y + (endR * sin(angleRad)).toFloat()
                    )

                    val lightningPath = Path().apply {
                        moveTo(p1.x, p1.y)
                        lineTo(p2.x, p2.y)
                        lineTo(p3.x, p3.y)
                    }

                    // Electric cyan branch
                    drawPath(
                        path = lightningPath,
                        color = accentColor.copy(alpha = 0.85f * surgeIntensity),
                        style = Stroke(
                            width = strokeWidth,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Miter
                        )
                    )

                    // Intense white core for lightning
                    drawPath(
                        path = lightningPath,
                        color = Color.White.copy(alpha = 0.9f * surgeIntensity),
                        style = Stroke(
                            width = strokeWidth * 0.5f,
                            cap = StrokeCap.Round
                        )
                    )
                }
            } else if (isCharging) {
                // Reduced motion: steady electric outline
                drawCircle(
                    color = accentColor,
                    radius = radius,
                    center = center,
                    style = Stroke(width = strokeWidth)
                )
            }
        }
    }
}
