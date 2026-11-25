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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nothing.island.ai.data.IslandPreferencesRepository
import com.nothing.island.ai.system.SystemEventsPreferences
import com.nothing.island.ai.ui.components.IslandPreview
import com.nothing.island.ai.ui.components.NothingCard
import com.nothing.island.ai.ui.components.NothingCardVariant
import com.nothing.island.ai.ui.components.NothingSectionHeader
import com.nothing.island.ai.ui.components.NothingSwitchRow
import com.nothing.island.ai.ui.theme.NothingTokens
import kotlinx.coroutines.launch

/**
 * Nothing OS 3.x Peripherals & System Studio Screen
 *
 * Sprint 2: system-hardware feedback toggles are now persisted in DataStore
 * and gate the corresponding events before they reach the island engine.
 */
@Composable
fun PeripheralsStudioScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val repository = remember { IslandPreferencesRepository.getInstance(context) }
    val prefs by repository.liveSystemEventsPreferences.collectAsState()
    val scope = rememberCoroutineScope()

    fun updateSystemEvents(transform: (SystemEventsPreferences) -> SystemEventsPreferences) {
        scope.launch {
            repository.updateSystemEventsPreferences(transform(prefs))
        }
    }

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
                    text = "PERIPHERALS STUDIO",
                    color = NothingTokens.NothingTextPrimary,
                    fontSize = 18.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "SYSTEM FEEDBACK & CONNECTIVITY",
                    color = NothingTokens.NothingTextTertiary,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        IslandPreview()

        Spacer(modifier = Modifier.height(16.dp))

        // Section: Bluetooth Audio
        NothingSectionHeader(title = "BLUETOOTH CONNECTIVITY", isAccent = true)
        NothingCard(variant = NothingCardVariant.STANDARD) {
            NothingSwitchRow(
                title = "Show Connection",
                description = "Show headset/earbud connection pill and battery telemetry when BLUETOOTH_CONNECT is granted",
                checked = prefs.bluetoothCapsuleEnabled,
                onCheckedChange = { enabled -> updateSystemEvents { it.copy(bluetoothCapsuleEnabled = enabled) } },
                badgeText = "AUDIO"
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Section: System Events & Unlocking
        NothingSectionHeader(title = "SYSTEM FEEDBACK", isAccent = true)
        NothingCard(variant = NothingCardVariant.STANDARD) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                NothingSwitchRow(
                    title = "System Feedback Capsules",
                    description = "Master switch for all momentary hardware status pills",
                    checked = prefs.systemEventsEnabled,
                    onCheckedChange = { enabled -> updateSystemEvents { it.copy(systemEventsEnabled = enabled) } },
                    badgeText = "SPRINT 2"
                )

                NothingSwitchRow(
                    title = "Show Device Unlock",
                    description = "Feedback displayed when device keyguard is unlocked",
                    checked = prefs.deviceUnlockCapsuleEnabled,
                    onCheckedChange = { enabled -> updateSystemEvents { it.copy(deviceUnlockCapsuleEnabled = enabled) } }
                )

                NothingSwitchRow(
                    title = "Show Silent Mode",
                    description = "Momentary red toast when ringer switches to silent/vibrate",
                    checked = prefs.silentModeCapsuleEnabled,
                    onCheckedChange = { enabled -> updateSystemEvents { it.copy(silentModeCapsuleEnabled = enabled) } }
                )

                NothingSwitchRow(
                    title = "Show Ring Mode",
                    description = "Momentary white toast when ringer switches to sound on",
                    checked = prefs.ringModeCapsuleEnabled,
                    onCheckedChange = { enabled -> updateSystemEvents { it.copy(ringModeCapsuleEnabled = enabled) } }
                )

                NothingSwitchRow(
                    title = "Show Wi-Fi Status",
                    description = "Feedback when Wi-Fi connects or disconnects",
                    checked = prefs.wifiCapsuleEnabled,
                    onCheckedChange = { enabled -> updateSystemEvents { it.copy(wifiCapsuleEnabled = enabled) } }
                )

                NothingSwitchRow(
                    title = "Show Flashlight State",
                    description = "Feedback when the device torch is toggled",
                    checked = prefs.flashlightCapsuleEnabled,
                    onCheckedChange = { enabled -> updateSystemEvents { it.copy(flashlightCapsuleEnabled = enabled) } }
                )

                NothingSwitchRow(
                    title = "Show Do Not Disturb",
                    description = "Feedback when the interruption filter changes",
                    checked = prefs.zenModeCapsuleEnabled,
                    onCheckedChange = { enabled -> updateSystemEvents { it.copy(zenModeCapsuleEnabled = enabled) } }
                )

                NothingSwitchRow(
                    title = "Low Battery Warnings",
                    description = "Momentary warning at 20% and 10% battery",
                    checked = prefs.batteryWarningCapsuleEnabled,
                    onCheckedChange = { enabled -> updateSystemEvents { it.copy(batteryWarningCapsuleEnabled = enabled) } }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
