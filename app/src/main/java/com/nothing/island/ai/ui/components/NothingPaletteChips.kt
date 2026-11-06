package com.nothing.island.ai.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
 * Nothing OS 3.x Inspired Palette Chips
 *
 * Implements removable color chips with (x) badges and an optional "+ Add Color" chip,
 * matching the reference controls observed in flashy border animation style.
 */
@Composable
fun NothingPaletteChips(
    colors: List<Color>,
    selectedColor: Color?,
    onColorSelected: (Color) -> Unit,
    onRemoveColor: ((Color) -> Unit)? = null,
    onAddColorClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val scrollState = rememberScrollState()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        colors.forEach { color ->
            val isSelected = color == selectedColor

            val borderColor by animateColorAsState(
                targetValue = if (isSelected) NothingTokens.NothingTextPrimary else NothingTokens.NothingBorderSubtle,
                animationSpec = tween(durationMillis = 180),
                label = "ChipBorder"
            )

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(NothingTokens.ControlRadius))
                    .background(NothingTokens.NothingSurfaceLow)
                    .border(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = borderColor,
                        shape = RoundedCornerShape(NothingTokens.ControlRadius)
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onColorSelected(color)
                    }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Color Circle Dot
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(1.dp, NothingTokens.NothingBorderSubtle, CircleShape)
                    )

                    if (onRemoveColor != null && colors.size > 1) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(NothingTokens.NothingSurfaceMedium)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onRemoveColor(color)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "✕",
                                color = NothingTokens.NothingTextSecondary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Add Color Button
        if (onAddColorClick != null) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(NothingTokens.ControlRadius))
                    .background(NothingTokens.NothingSurfaceMedium)
                    .border(1.dp, NothingTokens.NothingBorderSubtle, RoundedCornerShape(NothingTokens.ControlRadius))
                    .semantics { role = Role.Button }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onAddColorClick()
                    }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "+ ADD",
                    color = NothingTokens.NothingTextPrimary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
