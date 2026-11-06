package com.nothing.island.ai.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nothing.island.ai.ui.theme.NothingTokens

/**
 * Nothing OS 3.x Inspired High-Precision Slider
 *
 * Implements a high-contrast slider with a technical monospace value display,
 * tactile haptic tick feedback, and surgical Nothing Red progress fill.
 */
@Composable
fun NothingSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
    label: String? = null,
    formattedValue: String? = null,
    steps: Int = 0,
    enabled: Boolean = true,
    activeColor: Color = NothingTokens.NothingRed
) {
    val haptic = LocalHapticFeedback.current

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

        Slider(
            value = value,
            onValueChange = { newVal ->
                if (newVal != value) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onValueChange(newVal)
                }
            },
            valueRange = valueRange,
            steps = steps,
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
}
