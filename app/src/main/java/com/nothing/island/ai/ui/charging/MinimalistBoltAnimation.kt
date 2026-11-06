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
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.dp

/**
 * Style 11: Minimalist Bolt Animation (Technical & Minimalist)
 *
 * Subtle high-contrast lightning glyph with breathing pulse glow.
 * Adapts clean indicator from Reference A #33 & Reference B ap2.
 */
@Composable
fun MinimalistBoltAnimation(
    percentage: Int,
    isCharging: Boolean,
    accentColor: Color,
    speed: Float = 1.0f,
    intensity: Float = 1.0f,
    glowIntensity: Float = 0.6f,
    reducedMotion: Boolean = false,
    modifier: Modifier = Modifier
) {
    val durationMs = if (reducedMotion) 4000 else (800 / speed.coerceIn(0.5f, 2.0f)).toInt()
    val transition = rememberInfiniteTransition(label = "MinimalistBoltTransition")

    val pulseAlpha by transition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMs, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "boltPulseAlpha"
    )

    val pulseScale by transition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMs, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "boltPulseScale"
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

            val effectiveColor = when {
                percentage <= 20 -> Color(0xFFD71920) // Low battery red
                else -> if (isCharging) accentColor else Color.White
            }

            // 1. Subtle Outer Border Circle
            drawCircle(
                color = effectiveColor.copy(alpha = 0.15f),
                radius = radius,
                center = center,
                style = Stroke(width = 1.5.dp.toPx())
            )

            // 2. Animated Pulsing Glow Halo
            if (isCharging && !reducedMotion && glowIntensity > 0f) {
                drawCircle(
                    color = effectiveColor.copy(alpha = (0.25f * glowIntensity * pulseAlpha).coerceIn(0.05f, 0.6f)),
                    radius = radius * pulseScale,
                    center = center
                )
            }

            // 3. Precision Lightning Bolt Path (Centered)
            val boltH = radius * 1.15f
            val boltW = boltH * 0.58f

            val currentScale = if (reducedMotion || !isCharging) 1.0f else pulseScale
            val currentAlpha = if (reducedMotion || !isCharging) 1.0f else pulseAlpha

            scale(scale = currentScale, pivot = center) {
                val boltPath = Path().apply {
                    moveTo(center.x + (boltW * 0.12f), center.y - (boltH * 0.5f))
                    lineTo(center.x - (boltW * 0.45f), center.y + (boltH * 0.05f))
                    lineTo(center.x - (boltW * 0.02f), center.y + (boltH * 0.05f))
                    lineTo(center.x - (boltW * 0.15f), center.y + (boltH * 0.5f))
                    lineTo(center.x + (boltW * 0.45f), center.y - (boltH * 0.08f))
                    lineTo(center.x + (boltW * 0.02f), center.y - (boltH * 0.08f))
                    close()
                }

                drawPath(
                    path = boltPath,
                    color = effectiveColor.copy(alpha = currentAlpha)
                )
            }
        }
    }
}
