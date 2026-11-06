package com.nothing.island.ai.ui.charging

import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/**
 * Style 4: Charging Aura Animation
 *
 * Multi-layered breathing ambient halo with Nothing chromatic glow and rhythmic pulse.
 */
@Composable
fun ChargingAuraAnimation(
    percentage: Int,
    isCharging: Boolean,
    accentColor: Color,
    speed: Float = 1.0f,
    intensity: Float = 1.0f,
    glowIntensity: Float = 0.6f,
    reducedMotion: Boolean = false,
    modifier: Modifier = Modifier
) {
    val durationMs = if (reducedMotion) 5000 else (2000 / speed.coerceIn(0.5f, 2.0f)).toInt()
    val transition = rememberInfiniteTransition(label = "ChargingAuraTransition")

    val breathingScale by transition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMs, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathingScale"
    )

    val auraAlpha by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMs, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "auraAlpha"
    )

    Box(
        modifier = modifier.size(110.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val baseRadius = (size.minDimension / 2f) - 10.dp.toPx()
            val effectiveRadius = baseRadius * (if (reducedMotion) 1.0f else breathingScale)
            val effectiveGlow = (auraAlpha * glowIntensity * intensity).coerceIn(0.1f, 1.0f)

            // 1. Broad Ambient Radial Glow (outer halo)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        accentColor.copy(alpha = 0.4f * effectiveGlow),
                        accentColor.copy(alpha = 0.15f * effectiveGlow),
                        Color.Transparent
                    ),
                    center = center,
                    radius = effectiveRadius * 1.35f
                ),
                radius = effectiveRadius * 1.35f,
                center = center
            )

            // 2. Mid Aura Ring
            drawCircle(
                color = accentColor.copy(alpha = 0.35f * effectiveGlow),
                radius = effectiveRadius,
                center = center,
                style = Stroke(width = 4.dp.toPx())
            )

            // 3. Inner Crisp Boundary Ring
            drawCircle(
                color = accentColor.copy(alpha = 0.85f),
                radius = baseRadius * 0.9f,
                center = center,
                style = Stroke(width = 1.5.dp.toPx())
            )

            // 4. Subtle Dotted Aura Halo
            val dotCount = 16
            for (i in 0 until dotCount) {
                val angle = Math.toRadians((i * (360f / dotCount)).toDouble())
                val dotRadius = effectiveRadius * 1.15f
                val dotX = center.x + (dotRadius * kotlin.math.cos(angle)).toFloat()
                val dotY = center.y + (dotRadius * kotlin.math.sin(angle)).toFloat()

                drawCircle(
                    color = Color.White.copy(alpha = 0.3f * effectiveGlow),
                    radius = 1.5.dp.toPx(),
                    center = Offset(dotX, dotY)
                )
            }
        }
    }
}
