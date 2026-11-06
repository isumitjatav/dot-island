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
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp

/**
 * Style 8: Plasma Flow Animation (Fluid & Elemental)
 *
 * Multi-stop angular sweep gradient with continuous rotation around the punch-hole.
 * Adapts rotational gradient vortex from Reference A #2 & Reference A Wave #5.
 */
@Composable
fun PlasmaFlowAnimation(
    percentage: Int,
    isCharging: Boolean,
    accentColor: Color,
    speed: Float = 1.0f,
    intensity: Float = 1.0f,
    glowIntensity: Float = 0.6f,
    reducedMotion: Boolean = false,
    modifier: Modifier = Modifier
) {
    val durationMs = if (reducedMotion) 6000 else (1800 / speed.coerceIn(0.5f, 2.0f)).toInt()
    val transition = rememberInfiniteTransition(label = "PlasmaFlowTransition")

    val rotationAngle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMs, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "plasmaRotation"
    )

    val breathingScale by transition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = (durationMs * 0.8f).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "plasmaBreathing"
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

            val strokeWidth = (3.5.dp.toPx() * intensity).coerceIn(2f, 14f)

            // 1. Ambient Background Track
            drawCircle(
                color = Color.White.copy(alpha = 0.1f),
                radius = radius,
                center = center,
                style = Stroke(width = strokeWidth * 0.8f)
            )

            // 2. Continuous Liquid-Light Plasma Vortex Sweep
            val currentRot = if (reducedMotion || !isCharging) 0f else rotationAngle
            val currentScale = if (reducedMotion || !isCharging) 1.0f else breathingScale

            rotate(degrees = currentRot, pivot = center) {
                drawCircle(
                    brush = Brush.sweepGradient(
                        0.0f to Color.White,
                        0.25f to accentColor,
                        0.5f to Color.White.copy(alpha = 0.15f),
                        0.75f to accentColor.copy(alpha = 0.8f),
                        1.0f to Color.White,
                        center = center
                    ),
                    radius = radius * currentScale,
                    center = center,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }

            // 3. Proportional Level Indicator Dot
            val levelAngle = (percentage.coerceIn(0, 100) / 100f) * 360f - 90f
            val rad = Math.toRadians(levelAngle.toDouble())
            val dotX = center.x + (radius * Math.cos(rad)).toFloat()
            val dotY = center.y + (radius * Math.sin(rad)).toFloat()

            drawCircle(
                color = Color.White,
                radius = strokeWidth * 1.1f,
                center = Offset(dotX, dotY)
            )
            if (glowIntensity > 0f) {
                drawCircle(
                    color = accentColor.copy(alpha = 0.4f * glowIntensity),
                    radius = strokeWidth * 2.2f,
                    center = Offset(dotX, dotY)
                )
            }
        }
    }
}
