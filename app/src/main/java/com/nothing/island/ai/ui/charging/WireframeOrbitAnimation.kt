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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp

/**
 * Style 3: Wireframe Orbit Animation (Nothing Signature)
 *
 * Dual counter-rotating technical wireframe ellipses.
 * Adapts technical orbit geometry from Reference A #16 & #12.
 */
@Composable
fun WireframeOrbitAnimation(
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
    val transition = rememberInfiniteTransition(label = "WireframeOrbitTransition")

    val rot1 by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMs, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbitRot1"
    )

    val rot2 by transition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = (durationMs * 1.35f).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbitRot2"
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

            val strokeWidth = (1.8.dp.toPx() * intensity).coerceIn(1.5f, 8f)

            // 1. Ambient Background Circle
            drawCircle(
                color = Color.White.copy(alpha = 0.08f),
                radius = radius,
                center = center,
                style = Stroke(width = 1.dp.toPx())
            )

            // 2. Outer Tilted Ellipse (Rotated +25 deg tilt)
            rotate(degrees = if (reducedMotion || !isCharging) 25f else 25f + rot1, pivot = center) {
                drawOval(
                    color = Color.White.copy(alpha = 0.45f),
                    topLeft = Offset(center.x - radius, center.y - (radius * 0.72f)),
                    size = Size(radius * 2f, radius * 1.44f),
                    style = Stroke(
                        width = strokeWidth,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                    )
                )

                // Leading glowing node on Outer Ellipse
                if (isCharging) {
                    drawCircle(
                        color = Color.White,
                        radius = strokeWidth * 1.3f,
                        center = Offset(center.x + radius, center.y)
                    )
                }
            }

            // 3. Inner Counter-Rotating Tilted Ellipse (Rotated -25 deg tilt)
            rotate(degrees = if (reducedMotion || !isCharging) -25f else -25f + rot2, pivot = center) {
                drawOval(
                    color = accentColor.copy(alpha = 0.65f),
                    topLeft = Offset(center.x - (radius * 0.82f), center.y - (radius * 0.58f)),
                    size = Size(radius * 1.64f, radius * 1.16f),
                    style = Stroke(
                        width = strokeWidth * 1.1f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 10f), 0f)
                    )
                )

                // Accent node on Inner Ellipse
                if (isCharging) {
                    drawCircle(
                        color = accentColor,
                        radius = strokeWidth * 1.2f,
                        center = Offset(center.x + (radius * 0.82f), center.y)
                    )
                }
            }

            // 4. Center Core Hub Dot
            drawCircle(
                color = if (isCharging) accentColor else Color.White.copy(alpha = 0.3f),
                radius = strokeWidth * 1.5f,
                center = center
            )
        }
    }
}
