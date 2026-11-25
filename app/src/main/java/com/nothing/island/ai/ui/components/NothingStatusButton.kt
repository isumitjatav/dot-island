package com.nothing.island.ai.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nothing.island.ai.ui.theme.NothingPrimaryAccent
import com.nothing.island.ai.ui.theme.NothingPrimaryText
import com.nothing.island.ai.ui.theme.NothingSecondaryText
import com.nothing.island.ai.ui.theme.NothingTokens
import kotlinx.coroutines.launch

/**
 * Animata-inspired fluid Status Button with state morphing, spring squeeze,
 * pulsing status indicators, and shockwave particle release.
 */
@Composable
fun NothingStatusButton(
    isActive: Boolean,
    enabledText: String = "STOP DOT ISLAND",
    disabledText: String = "START DOT ISLAND",
    enabledSubtext: String = "OVERLAY SERVICE RUNNING",
    disabledSubtext: String = "TAP TO ACTIVATE OVERLAY",
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    accentColor: Color = NothingPrimaryAccent,
    disabledColor: Color = NothingTokens.NothingSurfaceMedium
) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val pressScale = remember { Animatable(1f) }
    val shockwave = remember { Animatable(0f) }

    val infiniteTransition = rememberInfiniteTransition(label = "status_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    val targetBg = when {
        !enabled -> NothingTokens.NothingSurfaceLowest
        isActive -> NothingTokens.NothingSurfaceLow
        else -> NothingTokens.NothingSurfaceMedium
    }

    val animatedBg by animateColorAsState(
        targetValue = targetBg,
        animationSpec = tween(300),
        label = "status_button_bg"
    )

    val targetBorder = when {
        !enabled -> NothingTokens.NothingBorderSubtle
        isActive -> accentColor
        else -> NothingTokens.NothingBorderSubtle
    }

    val animatedBorder by animateColorAsState(
        targetValue = targetBorder,
        animationSpec = tween(300),
        label = "status_button_border"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(58.dp)
            .graphicsLayer {
                scaleX = pressScale.value
                scaleY = pressScale.value
            }
            .clip(RoundedCornerShape(14.dp))
            .background(animatedBg)
            .border(
                width = if (isActive) 1.5.dp else 1.dp,
                color = animatedBorder,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                scope.launch {
                    pressScale.animateTo(0.92f, tween(50))
                    pressScale.animateTo(
                        1.0f,
                        spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    )
                }
                scope.launch {
                    shockwave.snapTo(0f)
                    shockwave.animateTo(1f, tween(400, easing = LinearOutSlowInEasing))
                }
                onClick()
            }
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        // Shockwave ring on tap
        if (shockwave.value > 0f && shockwave.value < 1f) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val waveAlpha = (1f - shockwave.value) * 0.8f
                val maxRadius = size.width * 0.55f
                val currentRadius = shockwave.value * maxRadius
                drawCircle(
                    color = (if (isActive) accentColor else Color.White).copy(alpha = waveAlpha),
                    radius = currentRadius,
                    center = center,
                    style = Stroke(width = (2f * (1f - shockwave.value)).dp.toPx())
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Status Icon + Text Info
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Pulsing dot indicator
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(
                            if (!enabled) NothingTokens.NothingBorderSubtle
                            else if (isActive) accentColor.copy(alpha = pulseAlpha)
                            else NothingTokens.NothingBorderProminent
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isActive && enabled) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                        )
                    }
                }

                // Action Labels
                androidx.compose.foundation.layout.Column {
                    Text(
                        text = if (!enabled) "PERMISSION REQUIRED"
                        else if (isActive) enabledText
                        else disabledText,
                        color = if (!enabled) NothingSecondaryText else NothingPrimaryText,
                        fontSize = 12.5.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = if (!enabled) "Grant overlay permission in settings"
                        else if (isActive) enabledSubtext
                        else disabledSubtext,
                        color = if (isActive) accentColor else NothingSecondaryText,
                        fontSize = 8.5.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Right Action Status Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        if (!enabled) NothingTokens.NothingSurfaceLowest
                        else if (isActive) accentColor.copy(alpha = 0.2f)
                        else NothingTokens.NothingSurfaceMedium
                    )
                    .border(
                        1.dp,
                        if (isActive) accentColor else NothingTokens.NothingBorderSubtle,
                        RoundedCornerShape(6.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = if (!enabled) "LOCKED" else if (isActive) "ACTIVE" else "OFF",
                    color = if (!enabled) NothingSecondaryText else if (isActive) accentColor else NothingSecondaryText,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Sakuga VFX / Animata-style Fluid Toggle Switch with animated liquid knob smear,
 * velocity overshoot, and Nothing OS dot-matrix design.
 */
@Composable
fun NothingFluidToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    activeColor: Color = NothingPrimaryAccent,
    width: Dp = 50.dp,
    height: Dp = 28.dp
) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val pressSquash = remember { Animatable(1f) }

    // Liquid knob transition: knob slides and stretches during motion
    val knobPosition by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "knob_pos"
    )

    // Dynamic track color
    val trackColor by animateColorAsState(
        targetValue = when {
            !enabled -> NothingTokens.NothingSurfaceLowest
            checked -> activeColor
            else -> NothingTokens.NothingSurfaceMedium
        },
        animationSpec = tween(220),
        label = "track_color"
    )

    val knobSize = height - 6.dp
    val maxTravel = width - height

    Box(
        modifier = modifier
            .size(width, height)
            .graphicsLayer {
                scaleX = pressSquash.value
                scaleY = pressSquash.value
            }
            .clip(RoundedCornerShape(height / 2))
            .background(trackColor)
            .border(
                1.dp,
                if (checked) activeColor else NothingTokens.NothingBorderSubtle,
                RoundedCornerShape(height / 2)
            )
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                scope.launch {
                    pressSquash.animateTo(0.85f, tween(40))
                    pressSquash.animateTo(1.0f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow))
                }
                onCheckedChange(!checked)
            }
            .padding(3.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        // Knob with dynamic elastic stretch during slide
        val offsetDp = maxTravel * knobPosition
        val stretchFactor = 1f + kotlin.math.abs(knobPosition - (if (checked) 1f else 0f)) * 0.35f

        Box(
            modifier = Modifier
                .offset { androidx.compose.ui.unit.IntOffset(offsetDp.roundToPx(), 0) }
                .size(knobSize)
                .graphicsLayer {
                    scaleX = stretchFactor.coerceIn(1f, 1.35f)
                }
                .clip(CircleShape)
                .background(if (checked) Color.White else NothingTokens.NothingTextSecondary)
        )
    }
}
