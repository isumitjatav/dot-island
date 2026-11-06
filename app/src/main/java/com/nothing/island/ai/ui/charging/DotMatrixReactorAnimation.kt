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
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.sin

/**
 * Style 2: Dot Matrix Reactor Animation (Nothing Signature)
 *
 * LED dot-matrix ring pulsating around the camera lens.
 * Dots illuminate sequentially in a clockwise running chase with exponential decay.
 */
@Composable
fun DotMatrixReactorAnimation(
    percentage: Int,
    isCharging: Boolean,
    accentColor: Color,
    speed: Float = 1.0f,
    intensity: Float = 1.0f,
    glowIntensity: Float = 0.6f,
    reducedMotion: Boolean = false,
    modifier: Modifier = Modifier
) {
    val totalDots = 28
    val durationMs = if (reducedMotion) 5000 else (1800 / speed.coerceIn(0.5f, 2.0f)).toInt()
    val transition = rememberInfiniteTransition(label = "DotMatrixTransition")

    val pulsePhase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMs, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "dotPulsePhase"
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

            val dotRadius = (2.2.dp.toPx() * intensity).coerceIn(2f, 10f)
            val activeIndex = (pulsePhase * totalDots).toInt() % totalDots
            val activeThreshold = (percentage.coerceIn(0, 100) / 100f) * totalDots

            for (i in 0 until totalDots) {
                // Angle starting from top (-90 degrees)
                val angleDeg = (i.toFloat() / totalDots) * 360f - 90f
                val angleRad = angleDeg * (PI / 180f)
                val dotCenter = Offset(
                    center.x + (radius * cos(angleRad)).toFloat(),
                    center.y + (radius * sin(angleRad)).toFloat()
                )

                val isUnderLevel = i < activeThreshold

                if (isCharging && !reducedMotion) {
                    // Angular distance to current pulse head
                    val dist = min(
                        Math.abs(i - activeIndex),
                        totalDots - Math.abs(i - activeIndex)
                    )
                    // Gaussian decay falloff
                    val excitation = exp(-(dist * dist) / (2.0 * 2.2 * 2.2)).toFloat()

                    val dotColor = when {
                        excitation > 0.5f -> Color.White
                        excitation > 0.15f -> Color.White.copy(alpha = 0.5f + (0.5f * excitation))
                        isUnderLevel -> accentColor.copy(alpha = 0.75f)
                        else -> Color.White.copy(alpha = 0.12f)
                    }

                    val dynamicRadius = if (excitation > 0.3f) dotRadius * (1f + 0.35f * excitation) else dotRadius

                    drawCircle(
                        color = dotColor,
                        radius = dynamicRadius,
                        center = dotCenter
                    )

                    // Optional outer soft aura on the head dot
                    if (dist == 0 && glowIntensity > 0f) {
                        drawCircle(
                            color = accentColor.copy(alpha = 0.4f * glowIntensity),
                            radius = dotRadius * 2.2f,
                            center = dotCenter
                        )
                    }
                } else {
                    // Reduced motion or idle fallback
                    val dotColor = if (isUnderLevel) {
                        if (isCharging) accentColor else Color.White.copy(alpha = 0.8f)
                    } else {
                        Color.White.copy(alpha = 0.12f)
                    }

                    drawCircle(
                        color = dotColor,
                        radius = dotRadius,
                        center = dotCenter
                    )
                }
            }
        }
    }
}
