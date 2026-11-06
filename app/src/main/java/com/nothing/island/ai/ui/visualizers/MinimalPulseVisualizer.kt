package com.nothing.island.ai.ui.visualizers

import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke

/**
 * Visualizer Style 5: Minimal Pulse
 *
 * Nothing OS-inspired minimalist breathing rings that pulse gently to the beat.
 * Renders concentric soft rings with dot-matrix accent accents.
 * Freezes at low resting opacity when paused.
 */
@Composable
fun MinimalPulseVisualizer(
    isPlaying: Boolean,
    accentColor: Color,
    speed: Float = 1.0f,
    intensity: Float = 1.0f,
    glowIntensity: Float = 0.5f,
    reducedMotion: Boolean = false,
    modifier: Modifier = Modifier
) {
    val effectiveSpeed = if (reducedMotion) 0.5f else speed
    val effectiveIntensity = if (reducedMotion) 0.4f else intensity

    val infiniteTransition = rememberInfiniteTransition(label = "PulseAnim")
    val durationMs = (1000 / effectiveSpeed.coerceAtLeast(0.1f)).toInt()

    val pulseFraction by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = if (isPlaying) 1.0f else 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMs, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseFraction"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        val maxRadius = (size.width.coerceAtMost(size.height) / 2f)

        // Core central dot
        drawCircle(
            color = accentColor,
            radius = 3.5f * effectiveIntensity,
            center = Offset(centerX, centerY)
        )

        // Inner breathing ring
        val r1 = maxRadius * (0.35f + pulseFraction * 0.25f * effectiveIntensity)
        drawCircle(
            color = accentColor.copy(alpha = 0.70f * pulseFraction),
            radius = r1,
            center = Offset(centerX, centerY),
            style = Stroke(width = 1.8f)
        )

        // Outer expanding harmonic ring
        val r2 = maxRadius * (0.60f + pulseFraction * 0.35f * effectiveIntensity)
        drawCircle(
            color = accentColor.copy(alpha = 0.35f * (1f - pulseFraction * 0.5f) * glowIntensity),
            radius = r2,
            center = Offset(centerX, centerY),
            style = Stroke(width = 1.2f)
        )

        // Subtle ambient glow aura
        if (glowIntensity > 0f) {
            drawCircle(
                color = accentColor.copy(alpha = 0.12f * glowIntensity * pulseFraction),
                radius = r2 + 4f,
                center = Offset(centerX, centerY)
            )
        }
    }
}
