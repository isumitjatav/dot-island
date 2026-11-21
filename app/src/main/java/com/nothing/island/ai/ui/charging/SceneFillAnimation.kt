package com.nothing.island.ai.ui.charging

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope

/**
 * Original scene-fill charging animation: a colored scene fills the pill from
 * the left up to the battery percentage, with a small character at the fill
 * boundary. Inspired by the reference apps' *behavior*, drawn fresh here.
 */
@Composable
fun SceneFillAnimation(
    percentage: Int,
    isCharging: Boolean,
    accentColor: Color,
    scene: SceneKind,
    speed: Float = 1f,
    intensity: Float = 1f,
    glowIntensity: Float = 0.6f,
    reducedMotion: Boolean,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "sceneFill")
    val bob by transition.animateFloat(
        initialValue = 0f,
        targetValue = if (reducedMotion) 0f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((900 / speed.coerceAtLeast(0.1f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "sceneBob",
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val fill = (percentage.coerceIn(0, 100) / 100f) * w
        val colors = scene.colors(accentColor)
        val unit = h * 0.12f

        // Full-width dark track
        drawRoundRect(
            color = Color(0xFF141414),
            topLeft = Offset.Zero,
            size = Size(w, h),
            cornerRadius = CornerRadius(h * 0.3f, h * 0.3f),
        )
        // Filled scene up to percentage
        if (fill > 0f) {
            drawRoundRect(
                color = colors.first.copy(alpha = 0.85f * intensity.coerceIn(0.2f, 1f)),
                topLeft = Offset.Zero,
                size = Size(fill, h),
                cornerRadius = CornerRadius(h * 0.3f, h * 0.3f),
            )
        }
        // Boundary marker
        if (fill > unit && fill < w) {
            drawRect(
                color = colors.second,
                topLeft = Offset(fill - unit * 0.25f, 0f),
                size = Size(unit * 0.25f, h),
            )
        }
        // Character at the boundary
        val cx = (fill - unit * 1.6f).coerceIn(unit, w - unit)
        val cy = h * 0.5f + bob * unit * 0.4f
        drawCharacter(this, cx, cy, unit, colors.second, scene)
    }
}

enum class SceneKind(val label: String) {
    PUMPKIN("Pumpkin Patch"),
    ROBOT("Robot Buddy"),
    SNOWMAN("Snowman"),
    DRAGONFLY("Dragonfly");

    fun colors(accent: Color): Pair<Color, Color> = when (this) {
        PUMPKIN -> Color(0xFF5E239D) to Color(0xFFFF8A3D)
        ROBOT -> Color(0xFF1A4A3A) to Color(0xFF00E5FF)
        SNOWMAN -> Color(0xFF2C4A7A) to Color(0xFFFFFFFF)
        DRAGONFLY -> Color(0xFF2E6E8E) to Color(0xFFFF5FA2)
    }
}

private fun drawCharacter(
    scope: DrawScope,
    cx: Float,
    cy: Float,
    unit: Float,
    color: Color,
    scene: SceneKind,
) {
    val r = unit * 0.9f
    when (scene) {
        SceneKind.PUMPKIN -> {
            scope.drawCircle(color, radius = r, center = Offset(cx, cy))
            scope.drawLine(color, Offset(cx, cy - r), Offset(cx, cy - r - unit * 0.6f), unit * 0.3f)
        }
        SceneKind.ROBOT -> {
            scope.drawRoundRect(
                color,
                topLeft = Offset(cx - r, cy - r),
                size = Size(r * 2, r * 2),
                cornerRadius = CornerRadius(unit * 0.3f, unit * 0.3f),
            )
            scope.drawCircle(Color(0xFF0A0A0A), radius = unit * 0.16f, center = Offset(cx - r * 0.4f, cy))
            scope.drawCircle(Color(0xFF0A0A0A), radius = unit * 0.16f, center = Offset(cx + r * 0.4f, cy))
        }
        SceneKind.SNOWMAN -> {
            scope.drawCircle(color, radius = r, center = Offset(cx, cy + r * 0.5f))
            scope.drawCircle(color, radius = r * 0.7f, center = Offset(cx, cy - r * 0.7f))
        }
        SceneKind.DRAGONFLY -> {
            scope.drawCircle(color, radius = unit * 0.35f, center = Offset(cx, cy))
            scope.drawLine(color, Offset(cx - r, cy - r * 0.6f), Offset(cx, cy), unit * 0.18f)
            scope.drawLine(color, Offset(cx + r, cy - r * 0.6f), Offset(cx, cy), unit * 0.18f)
        }
    }
}
