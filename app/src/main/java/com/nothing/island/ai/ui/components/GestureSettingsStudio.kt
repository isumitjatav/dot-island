package com.nothing.island.ai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nothing.island.ai.NothingBlack
import com.nothing.island.ai.NothingBorder
import com.nothing.island.ai.NothingDarkGray
import com.nothing.island.ai.NothingMutedText
import com.nothing.island.ai.NothingRed
import com.nothing.island.ai.NothingWhite
import com.nothing.island.ai.gestures.GestureAction
import com.nothing.island.ai.gestures.GesturePreferences
import com.nothing.island.ai.gestures.NotchDisappearMode

/**
 * Dedicated Gesture & System Behavior Studio screen.
 * Configures gesture assignments (REQ-67 - REQ-71) and system behavior rules (REQ-26, REQ-28, REQ-48, REQ-51).
 */
@Composable
fun GestureSettingsStudio(
    preferences: GesturePreferences,
    onPreferencesChanged: (GesturePreferences) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Section: Gesture Bindings
        Text(
            text = "TOUCH & GESTURE BINDINGS",
            color = NothingRed,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(12.dp))

        GestureActionSelector(
            label = "Single Tap",
            description = "Custom action when tapping the collapsed island",
            selectedAction = preferences.singleTapAction,
            onActionSelected = { onPreferencesChanged(preferences.copy(singleTapAction = it)) }
        )
        Spacer(modifier = Modifier.height(8.dp))

        GestureActionSelector(
            label = "Long Press (500ms)",
            description = "Action triggered on 500ms touch and hold",
            selectedAction = preferences.longPressAction,
            onActionSelected = { onPreferencesChanged(preferences.copy(longPressAction = it)) }
        )
        Spacer(modifier = Modifier.height(8.dp))

        GestureActionSelector(
            label = "Swipe Left",
            description = "Action on left swipe when no notification is present",
            selectedAction = preferences.swipeLeftAction,
            onActionSelected = { onPreferencesChanged(preferences.copy(swipeLeftAction = it)) }
        )
        Spacer(modifier = Modifier.height(8.dp))

        GestureActionSelector(
            label = "Swipe Right",
            description = "Action on right swipe when no notification is present",
            selectedAction = preferences.swipeRightAction,
            onActionSelected = { onPreferencesChanged(preferences.copy(swipeRightAction = it)) }
        )
        Spacer(modifier = Modifier.height(8.dp))

        GestureActionSelector(
            label = "Swipe Down",
            description = "Action on downward swipe over the island",
            selectedAction = preferences.swipeDownAction,
            onActionSelected = { onPreferencesChanged(preferences.copy(swipeDownAction = it)) }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Section: Disappear Mode (REQ-48)
        Text(
            text = "NOTCH DISAPPEAR MODE",
            color = NothingRed,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Control when the momentary island collapses back into the camera notch",
            color = NothingMutedText,
            fontSize = 12.sp
        )
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            NotchDisappearMode.values().forEach { mode ->
                val isSelected = preferences.notchDisappearMode == mode
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) NothingRed else NothingDarkGray)
                        .border(1.dp, if (isSelected) NothingRed else NothingBorder, RoundedCornerShape(12.dp))
                        .clickable { onPreferencesChanged(preferences.copy(notchDisappearMode = mode)) }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = mode.name.replace("_", " "),
                        color = if (isSelected) NothingWhite else NothingMutedText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Section: Behavior Toggles (REQ-26, REQ-28, REQ-51)
        Text(
            text = "SYSTEM BEHAVIOR & PRIVACY",
            color = NothingRed,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(12.dp))

        BehaviorSwitchRow(
            label = "DND Auto-Expand Override",
            description = "Allow critical notifications to auto-expand even when Do Not Disturb is active (REQ-26)",
            checked = preferences.dndAutoExpandOverride,
            onCheckedChange = { onPreferencesChanged(preferences.copy(dndAutoExpandOverride = it)) }
        )
        Spacer(modifier = Modifier.height(8.dp))

        BehaviorSwitchRow(
            label = "Hide Sensitive on Keyguard",
            description = "Conceal notification content on the lock screen until unlocked (REQ-28)",
            checked = preferences.hideSensitiveOnKeyguard,
            onCheckedChange = { onPreferencesChanged(preferences.copy(hideSensitiveOnKeyguard = it)) }
        )
        Spacer(modifier = Modifier.height(8.dp))

        BehaviorSwitchRow(
            label = "Expand Silent Notifications",
            description = "Display island alerts for notifications configured with silent channels (REQ-51)",
            checked = preferences.expandSilentNotifications,
            onCheckedChange = { onPreferencesChanged(preferences.copy(expandSilentNotifications = it)) }
        )
    }
}

@Composable
private fun GestureActionSelector(
    label: String,
    description: String,
    selectedAction: GestureAction,
    onActionSelected: (GestureAction) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(NothingDarkGray)
            .border(1.dp, NothingBorder, RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = label,
                        color = NothingWhite,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = description,
                        color = NothingMutedText,
                        fontSize = 11.5.sp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Current action chip
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(NothingBlack)
                        .border(1.dp, NothingRed.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = selectedAction.name.replace("_", " "),
                        color = NothingRed,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action options scroll row / chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val availableActions = listOf(
                    GestureAction.NONE,
                    GestureAction.EXPAND_COLLAPSE,
                    GestureAction.NOTIFICATIONS_HUB,
                    GestureAction.TORCH,
                    GestureAction.CAMERA
                )
                availableActions.forEach { action ->
                    val isActionSelected = selectedAction == action
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isActionSelected) NothingRed.copy(alpha = 0.2f) else NothingBlack)
                            .border(
                                1.dp,
                                if (isActionSelected) NothingRed else NothingBorder.copy(alpha = 0.5f),
                                RoundedCornerShape(6.dp)
                            )
                            .clickable { onActionSelected(action) }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = action.name.replace("_", " "),
                            color = if (isActionSelected) NothingRed else NothingWhite.copy(alpha = 0.8f),
                            fontSize = 9.5.sp,
                            fontWeight = if (isActionSelected) FontWeight.Bold else FontWeight.Normal,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BehaviorSwitchRow(
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(NothingDarkGray)
            .border(1.dp, NothingBorder, RoundedCornerShape(14.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    color = NothingWhite,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    color = NothingMutedText,
                    fontSize = 11.5.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Switch Indicator
            Box(
                modifier = Modifier
                    .width(42.dp)
                    .height(24.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (checked) NothingRed else NothingBlack)
                    .border(1.dp, if (checked) NothingRed else NothingBorder, RoundedCornerShape(12.dp))
                    .padding(2.dp),
                contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart
            ) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(NothingWhite)
                )
            }
        }
    }
}
