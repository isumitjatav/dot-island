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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp

/**
 * Style 7: Cyber Horizon Animation (Fluid & Elemental)
 *
 * Dual neon laser lines sweeping laterally across the notch with rhythmic pulse expansion.
 * Adapts horizontal beam sweep from Reference A #30.
 */
@Composable
fun CyberHorizonAnimation(
    percentage: Int,
    isCharging: Boolean,
    accentColor: Color = Color(0xFF00FF66),
    speed: Float = 1.0f,
    intensity: Float = 1.0f,
    glowIntensity: Float = 0.6f,
    reducedMotion: Boolean = false,
    modifier: Modifier = Modifier
) {
    val durationMs = if (reducedMotion) 3000 else (1400 / speed.coerceIn(0.5f, 2.0f)).toInt()
    val transition = rememberInfiniteTransition(label = "CyberHorizonTransition")

    val pulseExpansion by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMs, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "horizonExpansion"
    )

    val auraAlpha by transition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMs, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "horizonAura"
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

            val strokeWidth = (2.2.dp.toPx() * intensity).coerceIn(2f, 10f)
            val laserColor = if (accentColor != Color(0xFFD71920)) accentColor else Color(0xFF00FF66)

            // 1. Center Core Aperture Ring
            drawCircle(
                color = Color.White.copy(alpha = 0.2f),
                radius = radius * 0.45f,
                center = center
            )
            drawCircle(
                color = laserColor.copy(alpha = 0.4f * auraAlpha),
                radius = radius * 0.45f,
                center = center
            )

            // 2. Dual Horizontal Laser Sweep Lines (Left and Right)
            val expansion = if (reducedMotion || !isCharging) 0.6f else pulseExpansion
            val maxLineLen = radius * 0.95f
            val curLineLen = maxLineLen * expansion

            // Left Laser
            drawLine(
                color = laserColor.copy(alpha = 0.9f),
                start = Offset(center.x - (radius * 0.5f), center.y),
                end = Offset(center.x - (radius * 0.5f) - curLineLen, center.y),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
            // Left Laser Core Glow
            drawLine(
                color = Color.White,
                start = Offset(center.x - (radius * 0.5f), center.y),
                end = Offset(center.x - (radius * 0.5f) - (curLineLen * 0.7f), center.y),
                strokeWidth = strokeWidth * 0.5f,
                cap = StrokeCap.Round
            )

            // Right Laser
            drawLine(
                color = laserColor.copy(alpha = 0.9f),
                start = Offset(center.x + (radius * 0.5f), center.y),
                end = Offset(center.x + (radius * 0.5f) + curLineLen, center.y),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
            // Right Laser Core Glow
            drawLine(
                color = Color.White,
                start = Offset(center.x + (radius * 0.5f), center.y),
                end = Offset(center.x + (radius * 0.5f) + (curLineLen * 0.7f), center.y),
                strokeWidth = strokeWidth * 0.5f,
                cap = StrokeCap.Round
            )

            // 3. Vertical Framing Ticks at Ends
            val tickH = 6.dp.toPx()
            drawLine(
                color = laserColor.copy(alpha = 0.7f),
                start = Offset(center.x - (radius * 0.5f) - curLineLen, center.y - tickH),
                end = Offset(center.x - (radius * 0.5f) - curLineLen, center.y + tickH),
                strokeWidth = strokeWidth * 0.8f,
                cap = StrokeCap.Square
            )
            drawLine(
                color = laserColor.copy(alpha = 0.7f),
                start = Offset(center.x + (radius * 0.5f) + curLineLen, center.y - tickH),
                end = Offset(center.x + (radius * 0.5f) + curLineLen, center.y + tickH),
                strokeWidth = strokeWidth * 0.8f,
                cap = StrokeCap.Square
            )
        }
    }
}
