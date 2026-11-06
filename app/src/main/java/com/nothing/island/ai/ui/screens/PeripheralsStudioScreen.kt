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
import com.nothing.island.ai.ui.components.IslandPreviewMode
import com.nothing.island.ai.ui.components.NothingCard
import com.nothing.island.ai.ui.components.NothingCardVariant
import com.nothing.island.ai.ui.components.NothingSectionHeader
import com.nothing.island.ai.ui.components.NothingSwitchRow
import com.nothing.island.ai.ui.theme.NothingTokens

/**
 * Nothing OS 3.x Peripherals & System Studio Screen
 *
 * Implements settings for Bluetooth accessory connection alerts,
 * device unlock feedback, and sound profile / ringer mode toasts.
 */
@Composable
fun PeripheralsStudioScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    var showBluetoothConnection by remember { mutableStateOf(true) }
    var showDeviceUnlock by remember { mutableStateOf(true) }
    var showSilentMode by remember { mutableStateOf(true) }
    var showRingMode by remember { mutableStateOf(true) }

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
                    text = "PERIPHERALS & SYSTEM",
                    color = NothingTokens.NothingTextPrimary,
                    fontSize = 18.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "BLUETOOTH, UNLOCK & RINGER ALERTS",
                    color = NothingTokens.NothingTextTertiary,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Interactive Live Preview (Default: Bluetooth Connected Pill)
        IslandPreview(
            initialMode = IslandPreviewMode.BLUETOOTH_COMPACT
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Section: Bluetooth Audio
        NothingSectionHeader(title = "BLUETOOTH CONNECTIVITY", isAccent = true)
        NothingCard(variant = NothingCardVariant.STANDARD) {
            NothingSwitchRow(
                title = "Show Connection",
                description = "Show connection popup and battery gauge in small notch",
                checked = showBluetoothConnection,
                onCheckedChange = { showBluetoothConnection = it },
                badgeText = "AUDIO"
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Section: System Events & Unlocking
        NothingSectionHeader(title = "SYSTEM FEEDBACK")
        NothingCard(variant = NothingCardVariant.STANDARD) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                NothingSwitchRow(
                    title = "Show Device Unlock",
                    description = "Feedback displayed when device keyguard is unlocked",
                    checked = showDeviceUnlock,
                    onCheckedChange = { showDeviceUnlock = it }
                )

                NothingSwitchRow(
                    title = "Show Silent Mode",
                    description = "Momentary red toast when ringer switches to silent/vibrate",
                    checked = showSilentMode,
                    onCheckedChange = { showSilentMode = it }
                )

                NothingSwitchRow(
                    title = "Show Ring Mode",
                    description = "Momentary white toast when ringer switches to sound on",
                    checked = showRingMode,
                    onCheckedChange = { showRingMode = it }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
