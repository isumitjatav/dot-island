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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/**
 * Style 5: Plasma Flow Animation
 *
 * Quad-orb vortex rotating along the pill contour with luminous phosphor trailing tails.
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
    val durationMs = if (reducedMotion) 6000 else (2200 / speed.coerceIn(0.5f, 2.0f)).toInt()
    val transition = rememberInfiniteTransition(label = "PlasmaFlowTransition")

    val orbitRotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMs, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbitRotation"
    )

    Box(
        modifier = modifier.size(110.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val orbitRadius = (size.minDimension / 2f) - 8.dp.toPx()

            // 1. Orbital Ring Guide Track
            drawCircle(
                color = Color.White.copy(alpha = 0.08f),
                radius = orbitRadius,
                center = center,
                style = Stroke(width = 1.dp.toPx())
            )

            // 2. 4 Orbiting Plasma Nodes with Phosphor Trailing Tails
            val nodeCount = 4
            val tailSteps = if (reducedMotion) 2 else 5

            for (node in 0 until nodeCount) {
                val baseAngle = (node * 90f) + (if (!reducedMotion) orbitRotation else 0f)

                // Trailing tail particles
                for (t in tailSteps downTo 1) {
                    val tailAngle = baseAngle - (t * 7f)
                    val tailRad = Math.toRadians(tailAngle.toDouble())
                    val tailPos = Offset(
                        (center.x + orbitRadius * cos(tailRad)).toFloat(),
                        (center.y + orbitRadius * sin(tailRad)).toFloat()
                    )
                    val tailAlpha = ((1f - (t.toFloat() / (tailSteps + 1))) * 0.4f * glowIntensity).coerceIn(0.05f, 0.6f)
                    val tailSize = (3.dp.toPx() * (1f - (t.toFloat() / (tailSteps + 2)))).coerceAtLeast(1.dp.toPx())

                    drawCircle(
                        color = accentColor.copy(alpha = tailAlpha),
                        radius = tailSize,
                        center = tailPos
                    )
                }

                // Lead Plasma Node
                val leadRad = Math.toRadians(baseAngle.toDouble())
                val leadPos = Offset(
                    (center.x + orbitRadius * cos(leadRad)).toFloat(),
                    (center.y + orbitRadius * sin(leadRad)).toFloat()
                )

                // Node outer glow
                drawCircle(
                    color = accentColor.copy(alpha = (0.35f * glowIntensity).coerceIn(0.1f, 0.7f)),
                    radius = 5.dp.toPx() * intensity,
                    center = leadPos
                )

                // Node dense core
                drawCircle(
                    color = Color.White,
                    radius = 2.5.dp.toPx(),
                    center = leadPos
                )
            }
        }
    }
}
