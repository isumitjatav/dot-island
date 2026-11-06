package com.nothing.island.ai.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nothing.island.ai.ui.theme.NothingTokens

/**
 * Nothing OS 3.x Stepper Slider
 *
 * Flanks the track with tactile (-) and (+) stepper buttons for exact numerical tuning,
 * matching the reference controls observed in flashy border animation and notification studios.
 */
@Composable
fun NothingStepperSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    stepSize: Float,
    modifier: Modifier = Modifier,
    label: String? = null,
    formattedValue: String? = null,
    enabled: Boolean = true,
    activeColor: Color = NothingTokens.NothingRed
) {
    val haptic = LocalHapticFeedback.current
    val canDecrement = enabled && value > valueRange.start
    val canIncrement = enabled && value < valueRange.endInclusive

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        if (label != null || formattedValue != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (label != null) {
                    Text(
                        text = label,
                        color = if (enabled) NothingTokens.NothingTextPrimary else NothingTokens.NothingTextDisabled,
                        fontSize = 14.sp,
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Medium
                    )
                }

                if (formattedValue != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(NothingTokens.NothingSurfaceMedium)
                            .border(1.dp, NothingTokens.NothingBorderSubtle, RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = formattedValue,
                            color = if (enabled) NothingTokens.NothingTextPrimary else NothingTokens.NothingTextDisabled,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Decrement Button (-)
            NothingStepperButton(
                symbol = "−",
                enabled = canDecrement,
                onClick = {
                    val nextVal = (value - stepSize).coerceAtLeast(valueRange.start)
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onValueChange(nextVal)
                }
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Center Slider Track
            Box(
                modifier = Modifier.weight(1f)
            ) {
                Slider(
                    value = value,
                    onValueChange = { newVal ->
                        if (newVal != value) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onValueChange(newVal)
                        }
                    },
                    valueRange = valueRange,
                    enabled = enabled,
                    colors = SliderDefaults.colors(
                        thumbColor = NothingTokens.NothingTextPrimary,
                        activeTrackColor = activeColor,
                        inactiveTrackColor = NothingTokens.NothingSurfaceHighest,
                        disabledThumbColor = NothingTokens.NothingTextDisabled,
                        disabledActiveTrackColor = NothingTokens.NothingSurfaceMedium,
                        disabledInactiveTrackColor = NothingTokens.NothingSurfaceLow
                    )
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Increment Button (+)
            NothingStepperButton(
                symbol = "+",
                enabled = canIncrement,
                onClick = {
                    val nextVal = (value + stepSize).coerceAtMost(valueRange.endInclusive)
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onValueChange(nextVal)
                }
            )
        }
    }
}

/**
 * Tactile Stepper Button (+ / -)
 */
@Composable
fun NothingStepperButton(
    symbol: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    val backgroundColor by animateColorAsState(
        targetValue = if (enabled) NothingTokens.NothingSurfaceMedium else NothingTokens.NothingSurfaceLow,
        animationSpec = tween(durationMillis = 150),
        label = "StepperBg"
    )

    val contentColor by animateColorAsState(
        targetValue = if (enabled) NothingTokens.NothingTextPrimary else NothingTokens.NothingTextDisabled,
        animationSpec = tween(durationMillis = 150),
        label = "StepperText"
    )

    Box(
        modifier = modifier
            .size(36.dp)
            .clip(RoundedCornerShape(NothingTokens.ControlRadius))
            .background(backgroundColor)
            .border(
                width = 1.dp,
                color = if (enabled) NothingTokens.NothingBorderSubtle else Color.Transparent,
                shape = RoundedCornerShape(NothingTokens.ControlRadius)
            )
            .semantics { role = Role.Button }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = enabled
            ) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = symbol,
            color = contentColor,
            fontSize = 18.sp,
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Bold
        )
    }
}
