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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.dp
import kotlin.math.sin

/**
 * Style 3: Liquid Energy Animation
 *
 * Fluid wave fill oscillating to current percentage with harmonic surface tension ripple.
 */
@Composable
fun LiquidEnergyAnimation(
    percentage: Int,
    isCharging: Boolean,
    accentColor: Color,
    speed: Float = 1.0f,
    intensity: Float = 1.0f,
    glowIntensity: Float = 0.6f,
    reducedMotion: Boolean = false,
    modifier: Modifier = Modifier
) {
    val durationMs = if (reducedMotion) 6000 else (2400 / speed.coerceIn(0.5f, 2.0f)).toInt()
    val transition = rememberInfiniteTransition(label = "LiquidEnergyTransition")

    val wavePhase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMs, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wavePhase"
    )

    val bubbleProgress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = (durationMs * 1.5f).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "bubbleProgress"
    )

    Box(
        modifier = modifier.size(110.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = (size.minDimension / 2f) - 6.dp.toPx()

            // Outer perimeter mask path
            val clipCircle = Path().apply {
                addOval(androidx.compose.ui.geometry.Rect(center.x - radius, center.y - radius, center.x + radius, center.y + radius))
            }

            clipPath(clipCircle) {
                // Background container tint
                drawCircle(
                    color = Color.White.copy(alpha = 0.05f),
                    radius = radius,
                    center = center
                )

                // Fill level calculation (0% at bottom, 100% at top)
                val fillRatio = (percentage.coerceIn(0, 100) / 100f)
                val baseWaterHeight = (size.height / 2f) + radius - (2 * radius * fillRatio)
                val amplitude = if (reducedMotion) 2.dp.toPx() else (5.dp.toPx() * intensity).coerceIn(1.dp.toPx(), 12.dp.toPx())

                // 1. Primary Wave Path
                val wavePath = Path().apply {
                    moveTo(center.x - radius, size.height)
                    lineTo(center.x - radius, baseWaterHeight)

                    val waveWidth = radius * 2
                    val step = 4f
                    var x = 0f
                    while (x <= waveWidth) {
                        val currentX = (center.x - radius) + x
                        val angle = (x / waveWidth) * (2 * Math.PI) + wavePhase
                        val y = baseWaterHeight + (amplitude * sin(angle)).toFloat()
                        lineTo(currentX, y)
                        x += step
                    }

                    lineTo(center.x + radius, size.height)
                    close()
                }

                // 2. Secondary Harmonic Wave Path (depth fill)
                val harmonicPath = Path().apply {
                    moveTo(center.x - radius, size.height)
                    lineTo(center.x - radius, baseWaterHeight)

                    val waveWidth = radius * 2
                    val step = 4f
                    var x = 0f
                    while (x <= waveWidth) {
                        val currentX = (center.x - radius) + x
                        val angle = (x / waveWidth) * (2.4 * Math.PI) - (wavePhase * 0.8)
                        val y = baseWaterHeight + ((amplitude * 0.7f) * sin(angle)).toFloat()
                        lineTo(currentX, y)
                        x += step
                    }

                    lineTo(center.x + radius, size.height)
                    close()
                }

                // Draw secondary harmonic layer
                drawPath(
                    path = harmonicPath,
                    color = accentColor.copy(alpha = (0.25f * glowIntensity).coerceIn(0.1f, 0.6f))
                )

                // Draw primary wave layer
                drawPath(
                    path = wavePath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            accentColor.copy(alpha = (0.7f * intensity).coerceIn(0.3f, 0.95f)),
                            accentColor.copy(alpha = 0.4f)
                        ),
                        startY = baseWaterHeight - amplitude,
                        endY = size.height
                    )
                )

                // 3. Floating Micro-Bubbles
                if (isCharging && !reducedMotion) {
                    val bubbleX1 = center.x - (radius * 0.3f)
                    val bubbleY1 = size.height - (size.height * bubbleProgress)
                    if (bubbleY1 > baseWaterHeight) {
                        drawCircle(
                            color = Color.White.copy(alpha = 0.6f * (1f - bubbleProgress)),
                            radius = 2.dp.toPx(),
                            center = Offset(bubbleX1, bubbleY1)
                        )
                    }

                    val bubbleX2 = center.x + (radius * 0.25f)
                    val bubbleY2 = size.height - (size.height * ((bubbleProgress + 0.5f) % 1f))
                    if (bubbleY2 > baseWaterHeight) {
                        drawCircle(
                            color = Color.White.copy(alpha = 0.5f * (1f - ((bubbleProgress + 0.5f) % 1f))),
                            radius = 1.5.dp.toPx(),
                            center = Offset(bubbleX2, bubbleY2)
                        )
                    }
                }
            }

            // Outer Perimeter Border Ring
            drawCircle(
                color = accentColor.copy(alpha = (0.5f * glowIntensity).coerceIn(0.2f, 0.9f)),
                radius = radius,
                center = center,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx())
            )
        }
    }
}
