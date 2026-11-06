package com.nothing.island.ai.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
 * Nothing OS 3.x Segmented Capsule Selector
 *
 * Implements a high-contrast segmented control with smooth animated indicator,
 * monospace option labels, and tactile haptic feedback.
 */
@Composable
fun <T> NothingSegmentedControl(
    items: List<T>,
    selectedItem: T,
    onItemSelected: (T) -> Unit,
    labelProvider: (T) -> String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    activeColor: Color = NothingTokens.NothingSurfaceHigh,
    activeBorderColor: Color = NothingTokens.NothingBorderProminent
) {
    val haptic = LocalHapticFeedback.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(NothingTokens.ControlRadius))
            .background(NothingTokens.NothingSurfaceLowest)
            .border(
                width = 1.dp,
                color = NothingTokens.NothingBorderSubtle,
                shape = RoundedCornerShape(NothingTokens.ControlRadius)
            )
            .padding(3.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                val isSelected = item == selectedItem

                val itemBgColor by animateColorAsState(
                    targetValue = if (isSelected) activeColor else Color.Transparent,
                    animationSpec = tween(durationMillis = 180),
                    label = "SegmentBg"
                )

                val itemBorderColor by animateColorAsState(
                    targetValue = if (isSelected) activeBorderColor else Color.Transparent,
                    animationSpec = tween(durationMillis = 180),
                    label = "SegmentBorder"
                )

                val textColor by animateColorAsState(
                    targetValue = if (isSelected) {
                        NothingTokens.NothingTextPrimary
                    } else if (enabled) {
                        NothingTokens.NothingTextSecondary
                    } else {
                        NothingTokens.NothingTextDisabled
                    },
                    animationSpec = tween(durationMillis = 180),
                    label = "SegmentText"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(NothingTokens.BadgeRadius + 2.dp))
                        .background(itemBgColor)
                        .border(
                            width = 1.dp,
                            color = itemBorderColor,
                            shape = RoundedCornerShape(NothingTokens.BadgeRadius + 2.dp)
                        )
                        .semantics { role = Role.RadioButton }
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            enabled = enabled
                        ) {
                            if (!isSelected) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onItemSelected(item)
                            }
                        }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = labelProvider(item).uppercase(),
                        color = textColor,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        letterSpacing = 0.8.sp,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
