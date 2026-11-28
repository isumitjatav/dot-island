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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nothing.island.ai.navigation.IslandDestination
import com.nothing.island.ai.ui.theme.NothingTokens

/**
 * Liquid Glass 3-Tab Floating Navigation Bar
 *
 * Floating translucent pill bottom bar containing 3 tabs:
 * - Home
 * - Style
 * - Alerts
 *
 * Styled with specular highlights, frosted translucent glass, and tactile feedback.
 */
@Composable
fun NothingBottomNavigation(
    currentDestination: IslandDestination,
    onDestinationSelected: (IslandDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        // Floating Translucent Glass Pill Container
        val pillShape = RoundedCornerShape(32.dp)
        Box(
            modifier = Modifier
                .clip(pillShape)
                .background(Color(0xE018181C)) // Translucent frosted dark glass
                .border(
                    width = 1.2.dp,
                    color = Color(0x38FFFFFF), // Specular liquid glass rim
                    shape = pillShape
                )
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IslandDestination.bottomNavDestinations.forEach { destination ->
                    val isSelected = when (destination) {
                        is IslandDestination.Home -> currentDestination.route == IslandDestination.Home.route
                        is IslandDestination.Style -> currentDestination.route == IslandDestination.Style.route ||
                                currentDestination.route == IslandDestination.ThemeStudio.route ||
                                currentDestination.route == IslandDestination.MascotStudio.route ||
                                currentDestination.route == IslandDestination.Studios.route
                        is IslandDestination.Alerts -> currentDestination.route == IslandDestination.Alerts.route ||
                                currentDestination.route == IslandDestination.Notifications.route ||
                                currentDestination.route == IslandDestination.Music.route ||
                                currentDestination.route == IslandDestination.Battery.route
                        else -> currentDestination.route == destination.route
                    }

                    val textColor by animateColorAsState(
                        targetValue = if (isSelected) NothingTokens.NothingTextPrimary else NothingTokens.NothingTextTertiary,
                        animationSpec = tween(durationMillis = 180),
                        label = "NavTextColor"
                    )

                    val pillBg by animateColorAsState(
                        targetValue = if (isSelected) Color(0xFF2B2B30) else Color.Transparent,
                        animationSpec = tween(durationMillis = 180),
                        label = "NavPillBg"
                    )

                    val borderColor by animateColorAsState(
                        targetValue = if (isSelected) Color(0x30FFFFFF) else Color.Transparent,
                        animationSpec = tween(durationMillis = 180),
                        label = "NavBorderColor"
                    )

                    val tabGlyph = when (destination) {
                        is IslandDestination.Home -> "●"
                        is IslandDestination.Style -> "✦"
                        is IslandDestination.Alerts -> "⚡"
                        else -> "●"
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(pillBg)
                            .border(1.dp, borderColor, RoundedCornerShape(20.dp))
                            .semantics { role = Role.Tab }
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                if (!isSelected) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onDestinationSelected(destination)
                                }
                            }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(5.dp)
                                        .clip(CircleShape)
                                        .background(NothingTokens.NothingRed)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            } else {
                                Text(
                                    text = tabGlyph,
                                    color = NothingTokens.NothingTextTertiary,
                                    fontSize = 10.sp
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                            }

                            Text(
                                text = destination.shortLabel,
                                color = textColor,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                letterSpacing = 0.6.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
