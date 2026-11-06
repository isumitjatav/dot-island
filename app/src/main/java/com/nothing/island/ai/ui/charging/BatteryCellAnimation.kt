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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/**
 * Style 12: Battery Cell Animation (Technical & Minimalist)
 *
 * Technical battery cell enclosure with 5 discrete segmented blocks.
 * The active filling block pulses during charging.
 * Adapts battery enclosure from Reference A #34 & #35.
 */
@Composable
fun BatteryCellAnimation(
    percentage: Int,
    isCharging: Boolean,
    accentColor: Color,
    speed: Float = 1.0f,
    intensity: Float = 1.0f,
    glowIntensity: Float = 0.6f,
    reducedMotion: Boolean = false,
    modifier: Modifier = Modifier
) {
    val durationMs = if (reducedMotion) 4000 else (900 / speed.coerceIn(0.5f, 2.0f)).toInt()
    val transition = rememberInfiniteTransition(label = "BatteryCellTransition")

    val pulseAlpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMs, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cellPulseAlpha"
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

            // Circular boundary background
            drawCircle(
                color = Color.White.copy(alpha = 0.08f),
                radius = radius,
                center = center
            )

            // Battery enclosure dimensions
            val bodyW = radius * 1.45f
            val bodyH = radius * 0.72f
            val capW = bodyW * 0.08f
            val capH = bodyH * 0.45f

            val bodyX = center.x - (bodyW / 2f) - (capW / 2f)
            val bodyY = center.y - (bodyH / 2f)
            val capX = bodyX + bodyW + 1.dp.toPx()
            val capY = center.y - (capH / 2f)

            val strokeWidth = 1.8.dp.toPx() * intensity

            // 1. Outer Battery Body Box
            drawRoundRect(
                color = Color.White.copy(alpha = 0.5f),
                topLeft = Offset(bodyX, bodyY),
                size = Size(bodyW, bodyH),
                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                style = Stroke(width = strokeWidth)
            )

            // 2. Positive Terminal Nipple
            drawRoundRect(
                color = Color.White.copy(alpha = 0.5f),
                topLeft = Offset(capX, capY),
                size = Size(capW, capH),
                cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
            )

            // 3. 5 Discrete Internal Battery Segments
            val segmentCount = 5
            val innerPadding = 3.dp.toPx()
            val innerW = bodyW - (innerPadding * 2f)
            val innerH = bodyH - (innerPadding * 2f)
            val segmentSpacing = 2.dp.toPx()
            val segW = (innerW - ((segmentCount - 1) * segmentSpacing)) / segmentCount

            val activeSegments = ((percentage.coerceIn(0, 100) / 100f) * segmentCount).toInt()

            for (i in 0 until segmentCount) {
                val segX = bodyX + innerPadding + (i * (segW + segmentSpacing))
                val segY = bodyY + innerPadding

                val isLit = i < activeSegments
                val isFilling = i == activeSegments && isCharging

                val segColor = when {
                    isFilling && !reducedMotion -> accentColor.copy(alpha = pulseAlpha)
                    isLit -> if (isCharging) accentColor else Color.White
                    else -> Color.Transparent
                }

                if (segColor != Color.Transparent) {
                    drawRoundRect(
                        color = segColor,
                        topLeft = Offset(segX, segY),
                        size = Size(segW, innerH),
                        cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                    )
                }
            }
        }
    }
}
