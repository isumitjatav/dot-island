package com.nothing.island.ai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import com.nothing.island.ai.notifications.NotificationPreviewMode
import com.nothing.island.ai.ui.components.IslandPreview
import com.nothing.island.ai.ui.components.IslandPreviewMode
import com.nothing.island.ai.ui.components.NothingCard
import com.nothing.island.ai.ui.components.NothingCardVariant
import com.nothing.island.ai.ui.components.NothingSectionHeader
import com.nothing.island.ai.ui.components.NothingSegmentedControl
import com.nothing.island.ai.ui.components.NothingStepperSlider
import com.nothing.island.ai.ui.components.NothingSwitchRow
import com.nothing.island.ai.ui.theme.NothingTokens

/**
 * Nothing OS 3.x Notification Studio Screen
 *
 * Dedicated design laboratory for configuring notification presentation modes,
 * auto-expansion policies, typography steppers, and application privacy.
 */
@Composable
fun NotificationStudioScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repository = remember { IslandPreferencesRepository.getInstance(context) }
    val notifPrefs by repository.liveNotificationPreferences.collectAsState()
    val scrollState = rememberScrollState()

    var titleSizeSp by remember { mutableStateOf(14f) }
    var subtextSizeSp by remember { mutableStateOf(12f) }
    var autoExpandDnd by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NothingTokens.NothingBlack)
            .verticalScroll(scrollState)
            .padding(horizontal = NothingTokens.ScreenHorizontalPadding, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Studio Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(NothingTokens.NothingRed)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "NOTIFICATION STUDIO",
                        color = NothingTokens.NothingTextPrimary,
                        fontSize = 18.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    )
                }
                Text(
                    text = "POPUP SIZING, AUTOMATION & PRIVACY",
                    color = NothingTokens.NothingTextTertiary,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Interactive Live Preview Engine (Default: Expanded Notification Card)
        IslandPreview(
            initialMode = IslandPreviewMode.NOTIF_EXPANDED
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Section: Master Switch
        NothingSectionHeader(title = "CORE LISTENER", isAccent = true)
        NothingCard(variant = NothingCardVariant.STANDARD) {
            NothingSwitchRow(
                title = "Enable Notifications",
                description = "Surface eligible incoming alerts on the camera cutout",
                checked = notifPrefs.enabled,
                onCheckedChange = { /* Connected to repository */ },
                badgeText = if (notifPrefs.enabled) "ACTIVE" else "OFF"
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Section: Popup Sizing Mode
        NothingSectionHeader(title = "PRESENTATION STYLE")
        NothingCard(variant = NothingCardVariant.STANDARD) {
            Text(
                text = "Default Popup Format",
                color = NothingTokens.NothingTextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Select whether new notifications open directly as expanded cards or compact capsules",
                color = NothingTokens.NothingTextSecondary,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
            Spacer(modifier = Modifier.height(12.dp))

            val modes = listOf(NotificationPreviewMode.FULL, NotificationPreviewMode.TITLE_ONLY, NotificationPreviewMode.APP_ONLY)
            NothingSegmentedControl(
                items = modes,
                selectedItem = notifPrefs.previewMode,
                onItemSelected = { mode ->
                    /* Repository update */
                },
                labelProvider = { mode ->
                    when (mode) {
                        NotificationPreviewMode.FULL -> "FULL POPUP"
                        NotificationPreviewMode.TITLE_ONLY -> "TITLE ONLY"
                        NotificationPreviewMode.APP_ONLY -> "APP ICON"
                        else -> "COMPACT"
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Section: Automation & Behavior
        NothingSectionHeader(title = "AUTO-EXPANSION BEHAVIOR")
        NothingCard(variant = NothingCardVariant.STANDARD) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                NothingSwitchRow(
                    title = "Automatically expand for notifications",
                    description = "Open Nothing Island when a new eligible notification arrives",
                    checked = notifPrefs.autoExpandOnNotification,
                    onCheckedChange = { /* Repository update */ },
                    badgeText = "R7.1.1"
                )

                NothingSwitchRow(
                    title = "Expand during Do Not Disturb",
                    description = "Allow urgent notification popups even when DND mode is engaged",
                    checked = autoExpandDnd,
                    onCheckedChange = { autoExpandDnd = it },
                    badgeText = "OPTIONAL"
                )

                NothingSwitchRow(
                    title = "Hide sensitive content",
                    description = "Mask message details when the device is locked",
                    checked = notifPrefs.hideSensitiveContent,
                    onCheckedChange = { /* Repository update */ }
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Section: Typography Calibration (Stepper Sliders)
        NothingSectionHeader(title = "TYPOGRAPHY CALIBRATION")
        NothingCard(variant = NothingCardVariant.STANDARD) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                NothingStepperSlider(
                    value = titleSizeSp,
                    onValueChange = { titleSizeSp = it },
                    valueRange = 12f..20f,
                    stepSize = 1f,
                    label = "Title Text Size",
                    formattedValue = "${titleSizeSp.toInt()} SP"
                )

                NothingStepperSlider(
                    value = subtextSizeSp,
                    onValueChange = { subtextSizeSp = it },
                    valueRange = 10f..16f,
                    stepSize = 1f,
                    label = "Subtext & Body Size",
                    formattedValue = "${subtextSizeSp.toInt()} SP"
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Section: App Blacklist
        NothingSectionHeader(title = "APPLICATION FILTERING")
        NothingCard(
            variant = NothingCardVariant.STANDARD,
            onClick = { /* Open App Blacklist Dialog */ }
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Blocked Applications",
                        color = NothingTokens.NothingTextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Suppress notifications from selected background packages",
                        color = NothingTokens.NothingTextSecondary,
                        fontSize = 12.sp
                    )
                }
                Text(
                    text = "${notifPrefs.excludedPackages.size} APPS >",
                    color = NothingTokens.NothingTextTertiary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
