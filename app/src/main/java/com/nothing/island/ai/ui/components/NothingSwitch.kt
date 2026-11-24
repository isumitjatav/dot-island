package com.nothing.island.ai.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.graphicsLayer
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
 * Nothing OS 3.x Custom Tactile Switch
 *
 * Implements a high-contrast, minimalist toggle with tactile haptic confirmation,
 * smooth sliding thumb animation, and proper accessibility semantics.
 */
@Composable
fun NothingSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    activeTrackColor: Color = NothingTokens.NothingRed,
    inactiveTrackColor: Color = NothingTokens.NothingSurfaceHighest
) {
    val haptic = LocalHapticFeedback.current

    val trackColor by animateColorAsState(
        targetValue = if (!enabled) {
            NothingTokens.NothingSurfaceMedium
        } else if (checked) {
            activeTrackColor
        } else {
            inactiveTrackColor
        },
        animationSpec = tween(durationMillis = 200),
        label = "SwitchTrackColor"
    )

    // Sakuga VFX fluid knob smear with bouncy spring
    val knobPosition by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
            stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow
        ),
        label = "SwitchThumbPosition"
    )

    val thumbColor by animateColorAsState(
        targetValue = if (!enabled) NothingTokens.NothingTextDisabled else NothingTokens.NothingTextPrimary,
        animationSpec = tween(durationMillis = 200),
        label = "SwitchThumbColor"
    )

    val maxTravel = 20.dp
    val safeKnobPosition = if (knobPosition.isNaN() || knobPosition.isInfinite()) (if (checked) 1f else 0f) else knobPosition
    val offsetDp = maxTravel * safeKnobPosition + 2.dp
    val rawStretch = 1f + kotlin.math.abs(safeKnobPosition - (if (checked) 1f else 0f)) * 0.32f
    val safeStretch = if (rawStretch.isNaN() || rawStretch.isInfinite()) 1f else rawStretch.coerceIn(1f, 1.35f)

    Box(
        modifier = modifier
            .width(44.dp)
            .height(24.dp)
            .clip(RoundedCornerShape(NothingTokens.PillRadius))
            .background(trackColor)
            .border(
                width = 1.dp,
                color = if (checked && enabled) activeTrackColor else NothingTokens.NothingBorderSubtle,
                shape = RoundedCornerShape(NothingTokens.PillRadius)
            )
            .semantics { role = Role.Switch }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = enabled
            ) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onCheckedChange(!checked)
            },
        contentAlignment = Alignment.CenterStart
    ) {
        // Sliding Circular Thumb with fluid elongation
        Box(
            modifier = Modifier
                .offset(x = offsetDp)
                .size(20.dp)
                .graphicsLayer {
                    scaleX = safeStretch
                }
                .clip(CircleShape)
                .background(thumbColor)
        )
    }
}

/**
 * Full-width Settings Switch Row with title, description, and optional status badge
 */
@Composable
fun NothingSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
    enabled: Boolean = true,
    badgeText: String? = null,
    isDestructive: Boolean = false
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = enabled
            ) {
                onCheckedChange(!checked)
            }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    color = if (!enabled) NothingTokens.NothingTextDisabled else NothingTokens.NothingTextPrimary,
                    fontSize = 15.sp,
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Medium
                )

                if (badgeText != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(NothingTokens.NothingSurfaceMedium)
                            .border(1.dp, NothingTokens.NothingBorderSubtle, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = badgeText.uppercase(),
                            color = NothingTokens.NothingTextTertiary,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (description != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    color = if (!enabled) NothingTokens.NothingTextDisabled else NothingTokens.NothingTextSecondary,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.SansSerif,
                    lineHeight = 16.sp
                )
            }
        }

        NothingSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            activeTrackColor = if (isDestructive) NothingTokens.NothingAmber else NothingTokens.NothingRed
        )
    }
}
