package com.nothing.island.ai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nothing.island.ai.ui.components.IslandPreview
import com.nothing.island.ai.ui.components.NothingCard
import com.nothing.island.ai.ui.components.NothingCardVariant
import com.nothing.island.ai.ui.components.NothingSectionHeader
import com.nothing.island.ai.ui.components.NothingSwitchRow
import com.nothing.island.ai.ui.theme.NothingTokens

/**
 * Nothing OS 3.x Gesture Studio Screen
 *
 * Implements gesture configuration for bi-directional activity switching,
 * long-press volume dragging, and visual gesture guidance.
 */
@Composable
fun GestureStudioScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    var activitySwitcherEnabled by remember { mutableStateOf(true) }
    var volumeControlGesture by remember { mutableStateOf(true) }
    var gamingProtection by remember { mutableStateOf(true) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NothingTokens.NothingBlack)
            .verticalScroll(scrollState)
            .padding(horizontal = NothingTokens.ScreenHorizontalPadding, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Back Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(NothingTokens.NothingSurfaceMedium)
                    .border(1.dp, NothingTokens.NothingBorderSubtle, CircleShape)
                    .clickable { onNavigateBack() },
                contentAlignment = Alignment.Center
            ) {
                Text(text = "<", color = NothingTokens.NothingTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "GESTURE STUDIO",
                    color = NothingTokens.NothingTextPrimary,
                    fontSize = 18.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "TOUCH CONTROLS & ACTIVITY NAVIGATION",
                    color = NothingTokens.NothingTextTertiary,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        IslandPreview()

        Spacer(modifier = Modifier.height(16.dp))

        NothingSectionHeader(title = "CORE GESTURE ACTIONS", isAccent = true)
        NothingCard(variant = NothingCardVariant.STANDARD) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                NothingSwitchRow(
                    title = "Bi-Directional Activity Switcher",
                    description = "Swipe left or right across the island to cycle through active modules",
                    checked = activitySwitcherEnabled,
                    onCheckedChange = { activitySwitcherEnabled = it },
                    badgeText = "R8 CORE"
                )

                NothingSwitchRow(
                    title = "Long Press : Volume Control",
                    description = "Press & hold on cutout, then swipe left/right to adjust media volume",
                    checked = volumeControlGesture,
                    onCheckedChange = { volumeControlGesture = it },
                    badgeText = "AUDIO"
                )

                NothingSwitchRow(
                    title = "Gaming Touch Protection",
                    description = "Temporarily disable island touch targets during full-screen games",
                    checked = gamingProtection,
                    onCheckedChange = { gamingProtection = it }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
