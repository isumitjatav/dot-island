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
 * Nothing OS 3.x Studio Bottom Navigation Bar
 *
 * Implements a 5-destination navigation bar (HOME, NOTIFS, MUSIC, BATTERY, STUDIOS)
 * with tactile haptic feedback, animated active indicator pills, and surgical Nothing Red dots.
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
            .background(NothingTokens.NothingSurfaceLow)
            .border(
                width = 1.dp,
                color = NothingTokens.NothingBorderSubtle,
                shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp)
            )
            .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
            .padding(horizontal = 6.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IslandDestination.bottomNavDestinations.forEach { destination ->
                val isSelected = currentDestination.route == destination.route ||
                        (destination is IslandDestination.Studios &&
                                IslandDestination.studioHubDestinations.any { it.route == currentDestination.route })

                val textColor by animateColorAsState(
                    targetValue = if (isSelected) NothingTokens.NothingTextPrimary else NothingTokens.NothingTextTertiary,
                    animationSpec = tween(durationMillis = 180),
                    label = "NavTextColor"
                )

                val pillBg by animateColorAsState(
                    targetValue = if (isSelected) NothingTokens.NothingSurfaceMedium else Color.Transparent,
                    animationSpec = tween(durationMillis = 180),
                    label = "NavPillBg"
                )

                val borderColor by animateColorAsState(
                    targetValue = if (isSelected) NothingTokens.NothingBorderSubtle else Color.Transparent,
                    animationSpec = tween(durationMillis = 180),
                    label = "NavBorderColor"
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(pillBg)
                        .border(1.dp, borderColor, RoundedCornerShape(12.dp))
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
                        .padding(horizontal = 10.dp, vertical = 6.dp),
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
                            Spacer(modifier = Modifier.width(5.dp))
                        }

                        Text(
                            text = destination.shortLabel,
                            color = textColor,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }
        }
    }
}
