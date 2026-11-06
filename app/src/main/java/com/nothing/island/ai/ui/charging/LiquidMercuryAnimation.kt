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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Style 5: Liquid Mercury Animation (Fluid & Elemental)
 *
 * Monochrome fluid reservoir with buoyant meniscus physics.
 * Adapts fluid reservoir mechanics from Reference A #10 & Reference A Wave #1.
 */
@Composable
fun LiquidMercuryAnimation(
    percentage: Int,
    isCharging: Boolean,
    accentColor: Color,
    speed: Float = 1.0f,
    intensity: Float = 1.0f,
    glowIntensity: Float = 0.6f,
    reducedMotion: Boolean = false,
    modifier: Modifier = Modifier
) {
    val durationMs = if (reducedMotion) 6000 else (2000 / speed.coerceIn(0.5f, 2.0f)).toInt()
    val transition = rememberInfiniteTransition(label = "LiquidMercuryTransition")

    val wavePhase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMs, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "mercuryWavePhase"
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

            // Circular boundary path for clipping the liquid reservoir
            val circleClip = Path().apply {
                addOval(androidx.compose.ui.geometry.Rect(center, radius))
            }

            // 1. Outer Container Circle Ring
            drawCircle(
                color = Color.White.copy(alpha = 0.2f),
                radius = radius,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )

            // Fill height proportional to battery percentage (from bottom to top)
            val normLevel = percentage.coerceIn(0, 100) / 100f
            val baseWaterY = (center.y + radius) - (2f * radius * normLevel)

            clipPath(circleClip) {
                // 2. Liquid Reservoir Body
                val wavePath = Path()
                val meniscusPath = Path()

                val waveAmp1 = if (reducedMotion || !isCharging) 0f else 3.5.dp.toPx() * intensity
                val waveAmp2 = if (reducedMotion || !isCharging) 0f else 1.8.dp.toPx() * intensity
                val waveLen = radius * 1.5f

                val startX = center.x - radius - 10f
                val endX = center.x + radius + 10f
                val steps = 30
                val stepX = (endX - startX) / steps

                var first = true
                for (i in 0..steps) {
                    val x = startX + (i * stepX)
                    val y = baseWaterY +
                            (waveAmp1 * sin((2f * PI.toFloat() * (x - startX) / waveLen) + wavePhase)) +
                            (waveAmp2 * cos((2f * PI.toFloat() * (x - startX) / (waveLen * 0.7f)) - wavePhase))

                    if (first) {
                        wavePath.moveTo(x, y)
                        meniscusPath.moveTo(x, y)
                        first = false
                    } else {
                        wavePath.lineTo(x, y)
                        meniscusPath.lineTo(x, y)
                    }
                }

                // Close the fluid polygon to the bottom of the circle
                wavePath.lineTo(endX, center.y + radius + 10f)
                wavePath.lineTo(startX, center.y + radius + 10f)
                wavePath.close()

                // Draw translucent silvery fluid
                drawPath(
                    path = wavePath,
                    color = Color.White.copy(alpha = 0.25f)
                )

                // Optional subtle accent tint at base
                drawPath(
                    path = wavePath,
                    color = accentColor.copy(alpha = 0.12f)
                )

                // Draw bright white liquid meniscus surface line
                drawPath(
                    path = meniscusPath,
                    color = Color.White,
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }
    }
}
