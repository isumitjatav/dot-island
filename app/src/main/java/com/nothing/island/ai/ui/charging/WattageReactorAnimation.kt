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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.sin

/**
 * Style 10: Wattage Reactor Animation (Technical & Minimalist)
 *
 * Kinetic reactor power bars scaling with charging speed.
 * Adapts power bolt indicator from Reference A #36 & Reference B ap2.
 */
@Composable
fun WattageReactorAnimation(
    percentage: Int,
    isCharging: Boolean,
    accentColor: Color,
    speed: Float = 1.0f,
    intensity: Float = 1.0f,
    glowIntensity: Float = 0.6f,
    reducedMotion: Boolean = false,
    modifier: Modifier = Modifier
) {
    val barCount = 5
    val durationMs = if (reducedMotion) 4000 else (1000 / speed.coerceIn(0.5f, 2.0f)).toInt()
    val transition = rememberInfiniteTransition(label = "WattageReactorTransition")

    val pulsePhase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28318f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMs, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wattagePulse"
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

            // Circular subtle boundary track
            drawCircle(
                color = Color.White.copy(alpha = 0.08f),
                radius = radius,
                center = center
            )

            // Draw 5 vertical reactor bars centered
            val totalWidth = radius * 1.5f
            val barWidth = (totalWidth / (barCount * 1.6f)).coerceIn(4f, 20f)
            val barSpacing = barWidth * 0.6f
            val startX = center.x - (totalWidth / 2f)

            val maxBarHeight = radius * 1.3f
            val activeBars = ((percentage.coerceIn(0, 100) / 100f) * barCount).toInt().coerceAtLeast(1)

            for (i in 0 until barCount) {
                val x = startX + (i * (barWidth + barSpacing))
                val isLit = i < activeBars

                val dynamicHeightFrac = if (isCharging && !reducedMotion) {
                    val phaseOffset = i * 0.7f
                    0.4f + (0.55f * abs(sin(pulsePhase + phaseOffset)))
                } else {
                    0.25f + (0.75f * ((i + 1).toFloat() / barCount))
                }

                val currentBarHeight = maxBarHeight * dynamicHeightFrac
                val topY = center.y - (currentBarHeight / 2f)

                val barColor = when {
                    isLit && isCharging -> accentColor
                    isLit -> Color.White
                    else -> Color.White.copy(alpha = 0.15f)
                }

                drawRoundRect(
                    color = barColor,
                    topLeft = Offset(x, topY),
                    size = Size(barWidth, currentBarHeight),
                    cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                )
            }
        }
    }
}
