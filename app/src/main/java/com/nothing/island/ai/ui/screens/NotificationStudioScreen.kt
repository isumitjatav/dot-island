package com.nothing.island.ai.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import com.nothing.island.ai.NothingDarkGray
import com.nothing.island.ai.NothingRed
import com.nothing.island.ai.NothingWhite
import com.nothing.island.ai.data.IslandPreferencesRepository
import com.nothing.island.ai.notifications.NotificationPreferences
import com.nothing.island.ai.notifications.NotificationPreviewMode
import com.nothing.island.ai.ui.components.AppBlacklistDialog
import com.nothing.island.ai.ui.components.IslandPreview
import com.nothing.island.ai.ui.components.IslandPreviewMode
import com.nothing.island.ai.ui.components.NothingCard
import com.nothing.island.ai.ui.components.NothingCardVariant
import com.nothing.island.ai.ui.components.NothingSectionHeader
import com.nothing.island.ai.ui.components.NothingSegmentedControl
import com.nothing.island.ai.ui.components.NothingStepperSlider
import com.nothing.island.ai.ui.components.NothingSwitchRow
import com.nothing.island.ai.ui.theme.NothingTokens
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Nothing OS 3.x Notification Studio Screen
 *
 * Dedicated design laboratory for configuring notification presentation modes,
 * auto-expansion policies, typography steppers, swipe-to-dismiss, and application privacy.
 */
@Composable
fun NotificationStudioScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repository = remember { IslandPreferencesRepository.getInstance(context) }
    val notifPrefs by repository.liveNotificationPreferences.collectAsState()
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    var titleSizeSp by remember { mutableFloatStateOf(14f) }
    var subtextSizeSp by remember { mutableFloatStateOf(12f) }
    var autoExpandDnd by remember { mutableStateOf(notifPrefs.allowSilentNotifications) }
    var showBlacklistDialog by remember { mutableStateOf(false) }

    if (showBlacklistDialog) {
        AppBlacklistDialog(
            title = "Blocked Notifications",
            subtitle = "Suppress island alerts from selected apps",
            excludedPackages = notifPrefs.excludedPackages,
            onTogglePackage = { pkg ->
                coroutineScope.launch {
                    repository.toggleNotificationPackageExclusion(pkg)
                }
            },
            onDismiss = { showBlacklistDialog = false }
        )
    }

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
                    Box(
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
                onCheckedChange = { isChecked ->
                    coroutineScope.launch { repository.updateNotificationEnabled(isChecked) }
                },
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
                    coroutineScope.launch { repository.updateNotificationPreviewMode(mode) }
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
        NothingSectionHeader(title = "AUTO-EXPANSION & GESTURES")
        NothingCard(variant = NothingCardVariant.STANDARD) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                NothingSwitchRow(
                    title = "Automatically expand for notifications",
                    description = "Open Nothing Island when a new eligible notification arrives",
                    checked = notifPrefs.autoExpandOnNotification,
                    onCheckedChange = { isChecked ->
                        coroutineScope.launch { repository.updateNotificationAutoExpand(isChecked) }
                    },
                    badgeText = "R7.1.1"
                )

                NothingSwitchRow(
                    title = "Direct Swipe-to-Dismiss",
                    description = "Swipe left or right across the notification pill or card to dismiss immediately",
                    checked = notifPrefs.swipeToDismissEnabled,
                    onCheckedChange = { isChecked ->
                        coroutineScope.launch { repository.updateNotificationSwipeDismiss(isChecked) }
                    },
                    badgeText = "REQ-49"
                )

                NothingSwitchRow(
                    title = "Sync Shade Dismissal",
                    description = "Dismissing an alert on Nothing Island also clears it from the Android shade",
                    checked = notifPrefs.syncDismissWithAndroid,
                    onCheckedChange = { isChecked ->
                        coroutineScope.launch { repository.updateSyncDismissWithAndroid(isChecked) }
                    },
                    badgeText = "SHADE"
                )

                NothingSwitchRow(
                    title = "Hide System (Double) Notification",
                    description = "Remove the duplicate system heads-up once the island shows the alert. Calls and ongoing media are never hidden.",
                    checked = notifPrefs.hideSystemNotification,
                    onCheckedChange = { isChecked ->
                        coroutineScope.launch { repository.updateHideSystemNotification(isChecked) }
                    },
                    badgeText = "NEW"
                )

                NothingSwitchRow(
                    title = "Expand during Do Not Disturb",
                    description = "Allow urgent notification popups even when DND mode is engaged",
                    checked = autoExpandDnd,
                    onCheckedChange = { isChecked ->
                        autoExpandDnd = isChecked
                        coroutineScope.launch { repository.updateNotificationAllowSilent(isChecked) }
                    },
                    badgeText = "OPTIONAL"
                )

                NothingSwitchRow(
                    title = "Hide sensitive content",
                    description = "Mask message details when the device is locked",
                    checked = notifPrefs.hideSensitiveContent,
                    onCheckedChange = { isChecked ->
                        coroutineScope.launch { repository.updateNotificationHideSensitive(isChecked) }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Section: Display Duration Stepper
        NothingSectionHeader(title = "POPUP TIMING")
        NothingCard(variant = NothingCardVariant.STANDARD) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                NothingStepperSlider(
                    value = (notifPrefs.displayDurationMs / 1000f),
                    onValueChange = { sec ->
                        val durationMs = (sec * 1000L).toLong()
                        coroutineScope.launch { repository.updateNotificationDuration(durationMs) }
                    },
                    valueRange = 2f..10f,
                    stepSize = 0.5f,
                    label = "Popup Visibility Duration",
                    formattedValue = String.format(Locale.US, "%.1f SEC", notifPrefs.displayDurationMs / 1000f)
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
            onClick = { showBlacklistDialog = true }
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

        Spacer(modifier = Modifier.height(14.dp))

        // Section: Reset to Factory Defaults
        NothingSectionHeader(title = "HARDWARE RESTORATION")
        NothingCard(
            variant = NothingCardVariant.STANDARD,
            onClick = {
                coroutineScope.launch {
                    repository.updateNotificationEnabled(true)
                    repository.updateNotificationDuration(4500L)
                    repository.updateNotificationPreviewMode(NotificationPreviewMode.FULL)
                    repository.updateNotificationAutoExpand(true)
                    repository.updateNotificationSwipeDismiss(true)
                    repository.updateNotificationHideSensitive(true)
                    repository.updateNotificationAllowSilent(false)
                    repository.updateSyncDismissWithAndroid(false)
                    repository.updateNotificationExcludedPackages(emptySet())
                }
            }
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Reset Notification Settings",
                        color = NothingRed,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Restore all notification parameters to Nothing Phone (4a) baseline (4.5s)",
                        color = NothingTokens.NothingTextSecondary,
                        fontSize = 12.sp
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(NothingDarkGray)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "RESET",
                        color = NothingWhite,
                        fontSize = 10.5.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
