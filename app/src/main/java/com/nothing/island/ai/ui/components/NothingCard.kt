package com.nothing.island.ai.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nothing.island.ai.ui.theme.NothingTokens

/**
 * Visual variant styles for [NothingCard].
 * Allows each studio to establish a distinct visual hierarchy within the shared design system.
 */
enum class NothingCardVariant {
    STANDARD,       // Default container (NothingSurfaceLow + 10% white border)
    ELEVATED,       // Slightly raised container for primary actions (NothingSurfaceMedium)
    ACCENT_BORDER,  // Focused/active container with NothingRed or prominent border
    PREVIEW_WELL,   // Recessed AMOLED container for live preview simulations (NothingSurfaceLowest)
    TRANSPARENT     // Minimal container with subtle outline
}

/**
 * Nothing OS 3.x Inspired Card Component
 *
 * Implements 16dp rounded corners, subtle translucent borders, tactile haptics,
 * and high-contrast typography.
 */
@Composable
fun NothingCard(
    modifier: Modifier = Modifier,
    variant: NothingCardVariant = NothingCardVariant.STANDARD,
    shape: Shape = RoundedCornerShape(NothingTokens.CardRadius),
    contentPadding: Dp = NothingTokens.CardPadding,
    customBackgroundColor: Color? = null,
    customBorderColor: Color? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val haptic = LocalHapticFeedback.current

    val backgroundColor = customBackgroundColor ?: when (variant) {
        NothingCardVariant.STANDARD -> NothingTokens.NothingSurfaceLow
        NothingCardVariant.ELEVATED -> NothingTokens.NothingSurfaceMedium
        NothingCardVariant.ACCENT_BORDER -> NothingTokens.NothingSurfaceLow
        NothingCardVariant.PREVIEW_WELL -> NothingTokens.NothingSurfaceLowest
        NothingCardVariant.TRANSPARENT -> Color.Transparent
    }

    val borderColor = customBorderColor ?: when (variant) {
        NothingCardVariant.STANDARD -> NothingTokens.NothingBorderSubtle
        NothingCardVariant.ELEVATED -> NothingTokens.NothingBorderProminent
        NothingCardVariant.ACCENT_BORDER -> NothingTokens.NothingRed
        NothingCardVariant.PREVIEW_WELL -> NothingTokens.NothingBorderSubtle
        NothingCardVariant.TRANSPARENT -> NothingTokens.NothingBorderSubtle
    }

    val animatedBorderColor by animateColorAsState(
        targetValue = borderColor,
        animationSpec = tween(durationMillis = 200),
        label = "CardBorderColor"
    )

    val clickableModifier = if (onClick != null) {
        Modifier
            .semantics { role = Role.Button }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(backgroundColor)
            .border(width = 1.dp, color = animatedBorderColor, shape = shape)
            .then(clickableModifier)
            .padding(contentPadding)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            content = content
        )
    }
}

/**
 * Section Header for grouping NothingCards
 */
@Composable
fun NothingSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    badgeText: String? = null,
    isAccent: Boolean = false
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = NothingTokens.SectionHeaderSpacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isAccent) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(NothingTokens.NothingRed)
            )
            Spacer(modifier = Modifier.size(8.dp))
        }

        Text(
            text = title.uppercase(),
            color = if (isAccent) NothingTokens.NothingRed else NothingTokens.NothingTextSecondary,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.4.sp
        )

        if (badgeText != null) {
            Spacer(modifier = Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(NothingTokens.BadgeRadius))
                    .background(NothingTokens.NothingSurfaceMedium)
                    .border(1.dp, NothingTokens.NothingBorderSubtle, RoundedCornerShape(NothingTokens.BadgeRadius))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = badgeText.uppercase(),
                    color = NothingTokens.NothingTextTertiary,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
