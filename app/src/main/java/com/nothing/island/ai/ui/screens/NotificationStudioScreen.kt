package com.nothing.island.ai.ui.screens

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nothing.island.ai.data.IslandPreferencesRepository
import com.nothing.island.ai.ui.components.AppNotificationIcon
import com.nothing.island.ai.ui.components.DoubleBannerPreventionCard
import com.nothing.island.ai.ui.components.IslandCard
import com.nothing.island.ai.ui.components.NothingStepperSlider
import com.nothing.island.ai.ui.components.NothingSwitch
import com.nothing.island.ai.ui.components.SectionHeader
import com.nothing.island.ai.ui.theme.NothingDivider
import com.nothing.island.ai.ui.theme.NothingElevatedSurface
import com.nothing.island.ai.ui.theme.NothingMutedText
import com.nothing.island.ai.ui.theme.NothingPrimaryAccent
import com.nothing.island.ai.ui.theme.NothingPrimaryBackground
import com.nothing.island.ai.ui.theme.NothingPrimaryText
import com.nothing.island.ai.ui.theme.NothingSecondaryText
import com.nothing.island.ai.ui.theme.NothingSuccess
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Clean Alerts Screen (NotificationStudioScreen)
 *
 * Implements:
 * 1. 2-column icon toggle grid for Calls, Messages, Music, Charging, and Timers.
 * 2. Auto-detected Prevent Double Banners card with real app icons and 1-tap settings button.
 * 3. Expandable Allowed Apps card: Searchable list of installed launcher apps with ON/OFF switches.
 * 4. Expandable Alert Behavior & Privacy card: Hide Sensitive Content, Auto-Expand Popups,
 *    Swipe to Dismiss, Sync Shade Dismissal, Silent Notifications, and Display Duration.
 */
@Composable
fun NotificationStudioScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repository = remember { IslandPreferencesRepository.getInstance(context) }
    val notifPrefs by repository.liveNotificationPreferences.collectAsState()
    val callPrefs by repository.liveCallPreferences.collectAsState()
    val musicPrefs by repository.liveMusicPreferences.collectAsState()
    val batteryPrefs by repository.liveBatteryPreferences.collectAsState()
    val systemPrefs by repository.liveSystemEventsPreferences.collectAsState()
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    var timersEnabled by remember { mutableStateOf(systemPrefs.systemEventsEnabled) }
    var isAllowedAppsExpanded by remember { mutableStateOf(false) }
    var isPrivacyExpanded by remember { mutableStateOf(false) }
    var appSearchQuery by remember { mutableStateOf("") }
    var showAllApps by remember { mutableStateOf(false) }

    // Query installed launcher applications cleanly once
    val installedApps = remember(context) {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = pm.queryIntentActivities(intent, 0)
        resolveInfos.map { ri ->
            val pkg = ri.activityInfo.packageName
            val label = ri.loadLabel(pm).toString()
            pkg to label
        }.distinctBy { it.first }.sortedBy { it.second.lowercase() }
    }

    val filteredApps = remember(installedApps, appSearchQuery) {
        if (appSearchQuery.isBlank()) {
            installedApps
        } else {
            val q = appSearchQuery.trim().lowercase()
            installedApps.filter { it.second.lowercase().contains(q) || it.first.lowercase().contains(q) }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NothingPrimaryBackground)
            .verticalScroll(scrollState)
            .padding(horizontal = 18.dp, vertical = 12.dp),
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
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(NothingPrimaryAccent)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ALERTS & SIGNALS",
                        color = NothingPrimaryText,
                        fontSize = 18.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    )
                }
                Text(
                    text = "ACTIVE ISLAND MODULES",
                    color = NothingMutedText,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 1. 2-COLUMN ICON TOGGLE GRID
        SectionHeader(
            title = "ACTIVE CHANNELS",
            badgeText = "SURGICAL",
            badgeColor = NothingPrimaryAccent
        )
        Spacer(modifier = Modifier.height(6.dp))

        val alertChannels = listOf(
            AlertChannelItem(
                title = "Calls",
                glyph = "☎",
                subtitle = "Incoming & active phone calls",
                isEnabled = callPrefs.systemCallEnabled,
                onToggle = { isChecked ->
                    coroutineScope.launch {
                        repository.updateCallPreferences(callPrefs.copy(systemCallEnabled = isChecked))
                    }
                }
            ),
            AlertChannelItem(
                title = "Messages",
                glyph = "✉",
                subtitle = "Incoming notifications & text alerts",
                isEnabled = notifPrefs.autoExpandOnNotification,
                onToggle = { isChecked ->
                    coroutineScope.launch {
                        repository.updateNotificationAutoExpand(isChecked)
                    }
                }
            ),
            AlertChannelItem(
                title = "Music",
                glyph = "♫",
                subtitle = "Now playing & playback controls",
                isEnabled = musicPrefs.isAutoExpandEnabled,
                onToggle = { isChecked ->
                    coroutineScope.launch {
                        repository.updateMusicPreferences(musicPrefs.copy(isAutoExpandEnabled = isChecked))
                    }
                }
            ),
            AlertChannelItem(
                title = "Charging",
                glyph = "⚡",
                subtitle = "Battery percentage & power status",
                isEnabled = batteryPrefs.enabled,
                onToggle = { isChecked ->
                    coroutineScope.launch {
                        repository.updateBatteryEnabled(isChecked)
                    }
                }
            ),
            AlertChannelItem(
                title = "Timers",
                glyph = "⏱",
                subtitle = "Active countdowns & stopwatch",
                isEnabled = timersEnabled,
                onToggle = { isChecked ->
                    timersEnabled = isChecked
                    coroutineScope.launch {
                        repository.updateSystemEventsPreferences(systemPrefs.copy(systemEventsEnabled = isChecked))
                    }
                }
            )
        )

        // Render in 2-Column Grid
        for (i in alertChannels.indices step 2) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AlertChannelCard(
                    item = alertChannels[i],
                    onToggle = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        alertChannels[i].onToggle(!alertChannels[i].isEnabled)
                    },
                    modifier = Modifier.weight(1f)
                )

                if (i + 1 < alertChannels.size) {
                    AlertChannelCard(
                        item = alertChannels[i + 1],
                        onToggle = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            alertChannels[i + 1].onToggle(!alertChannels[i + 1].isEnabled)
                        },
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. PREVENT DOUBLE BANNERS CARD
        SectionHeader(
            title = "PREVENT DOUBLE BANNERS",
            badgeText = "RECOMMENDED",
            badgeColor = NothingPrimaryAccent
        )
        Spacer(modifier = Modifier.height(6.dp))

        DoubleBannerPreventionCard()

        Spacer(modifier = Modifier.height(18.dp))

        // 3. EXPANDABLE ALLOWED APPS CARD
        SectionHeader(
            title = "APPLICATION PERMISSIONS",
            badgeText = if (notifPrefs.excludedPackages.isEmpty()) "ALL ACTIVE" else "${installedApps.size - notifPrefs.excludedPackages.size} ACTIVE",
            badgeColor = NothingPrimaryAccent
        )
        Spacer(modifier = Modifier.height(6.dp))

        IslandCard {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Header with expand toggle
                val chevronRotation by animateFloatAsState(
                    targetValue = if (isAllowedAppsExpanded) 180f else 0f,
                    animationSpec = tween(180),
                    label = "appsChevron"
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            isAllowedAppsExpanded = !isAllowedAppsExpanded
                        }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Allowed Applications",
                            color = NothingPrimaryText,
                            fontSize = 14.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (notifPrefs.excludedPackages.isEmpty()) {
                                "All installed apps display alerts on the island"
                            } else {
                                "${notifPrefs.excludedPackages.size} apps suppressed from displaying"
                            },
                            color = NothingSecondaryText,
                            fontSize = 12.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(NothingElevatedSurface)
                            .border(1.dp, NothingDivider, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "▼",
                            color = NothingPrimaryText,
                            fontSize = 11.sp,
                            modifier = Modifier.rotate(chevronRotation)
                        )
                    }
                }

                // Expandable content
                AnimatedVisibility(
                    visible = isAllowedAppsExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        // Search bar
                        OutlinedTextField(
                            value = appSearchQuery,
                            onValueChange = { appSearchQuery = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = {
                                Text(
                                    text = "Search apps...",
                                    color = NothingMutedText,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = NothingElevatedSurface,
                                unfocusedContainerColor = NothingElevatedSurface,
                                focusedBorderColor = NothingPrimaryAccent,
                                unfocusedBorderColor = NothingDivider,
                                focusedTextColor = NothingPrimaryText,
                                unfocusedTextColor = NothingPrimaryText
                            )
                        )

                        val displayList = if (showAllApps || appSearchQuery.isNotBlank()) {
                            filteredApps
                        } else {
                            filteredApps.take(8)
                        }

                        displayList.forEach { (pkg, label) ->
                            val isAllowed = !notifPrefs.excludedPackages.contains(pkg)

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(NothingElevatedSurface)
                                    .border(1.dp, NothingDivider, RoundedCornerShape(10.dp))
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        coroutineScope.launch {
                                            repository.toggleNotificationPackageExclusion(pkg)
                                        }
                                    }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    AppNotificationIcon(
                                        packageName = pkg,
                                        fallbackChar = label.firstOrNull() ?: 'A',
                                        size = 28.dp
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = label,
                                            color = NothingPrimaryText,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = pkg,
                                            color = NothingMutedText,
                                            fontSize = 10.sp,
                                            fontFamily = FontFamily.Monospace,
                                            maxLines = 1
                                        )
                                    }
                                }

                                NothingSwitch(
                                    checked = isAllowed,
                                    onCheckedChange = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        coroutineScope.launch {
                                            repository.toggleNotificationPackageExclusion(pkg)
                                        }
                                    }
                                )
                            }
                        }

                        if (!showAllApps && appSearchQuery.isBlank() && filteredApps.size > 8) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(NothingElevatedSurface)
                                    .border(1.dp, NothingDivider, RoundedCornerShape(8.dp))
                                    .clickable { showAllApps = true }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "SHOW ALL (${filteredApps.size} APPS)",
                                    color = NothingPrimaryAccent,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // 4. EXPANDABLE ALERT BEHAVIOR & PRIVACY CARD
        SectionHeader(
            title = "BEHAVIOUR & PRIVACY",
            badgeText = "ADVANCED",
            badgeColor = NothingPrimaryAccent
        )
        Spacer(modifier = Modifier.height(6.dp))

        IslandCard {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                val privacyRotation by animateFloatAsState(
                    targetValue = if (isPrivacyExpanded) 180f else 0f,
                    animationSpec = tween(180),
                    label = "privacyChevron"
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            isPrivacyExpanded = !isPrivacyExpanded
                        }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Alert Behaviour & Privacy",
                            color = NothingPrimaryText,
                            fontSize = 14.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Sensitive content, popup duration, auto-expand, and gestures",
                            color = NothingSecondaryText,
                            fontSize = 12.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(NothingElevatedSurface)
                            .border(1.dp, NothingDivider, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "▼",
                            color = NothingPrimaryText,
                            fontSize = 11.sp,
                            modifier = Modifier.rotate(privacyRotation)
                        )
                    }
                }

                // Expandable content
                AnimatedVisibility(
                    visible = isPrivacyExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        // 1. Hide Sensitive Content
                        BehaviorToggleRow(
                            title = "Hide Sensitive Content",
                            subtitle = "Mask message text snippets and sender details when your phone is locked.",
                            checked = notifPrefs.hideSensitiveContent,
                            onCheckedChange = { checked ->
                                coroutineScope.launch { repository.updateNotificationHideSensitive(checked) }
                            }
                        )

                        // 2. Auto-Expand Popups
                        BehaviorToggleRow(
                            title = "Auto-Expand Popups",
                            subtitle = "Automatically expand Dot Island into an interactive card when an alert arrives.",
                            checked = notifPrefs.autoExpandOnNotification,
                            onCheckedChange = { checked ->
                                coroutineScope.launch { repository.updateNotificationAutoExpand(checked) }
                            }
                        )

                        // 3. Swipe to Dismiss
                        BehaviorToggleRow(
                            title = "Swipe to Dismiss",
                            subtitle = "Swipe horizontally across active alerts on the island to quickly dismiss them.",
                            checked = notifPrefs.swipeToDismissEnabled,
                            onCheckedChange = { checked ->
                                coroutineScope.launch { repository.updateNotificationSwipeDismiss(checked) }
                            }
                        )

                        // 4. Sync Dismissal with System Shade
                        BehaviorToggleRow(
                            title = "Sync Shade Dismissal",
                            subtitle = "Clearing an alert in Dot Island also removes it from your phone's notification shade.",
                            checked = notifPrefs.syncDismissWithAndroid,
                            onCheckedChange = { checked ->
                                coroutineScope.launch { repository.updateSyncDismissWithAndroid(checked) }
                            }
                        )

                        // 5. Silent Notifications
                        BehaviorToggleRow(
                            title = "Silent Notifications",
                            subtitle = "Allow background or low-priority notifications to appear on the island.",
                            checked = notifPrefs.allowSilentNotifications,
                            onCheckedChange = { checked ->
                                coroutineScope.launch { repository.updateNotificationAllowSilent(checked) }
                            }
                        )

                        // 6. Display Duration
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "POPUP VISIBILITY DURATION",
                                color = NothingMutedText,
                                fontSize = 10.5.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            NothingStepperSlider(
                                value = notifPrefs.displayDurationMs / 1000f,
                                onValueChange = { sec ->
                                    val durationMs = (sec * 1000L).toLong()
                                    coroutineScope.launch { repository.updateNotificationDuration(durationMs) }
                                },
                                valueRange = 2f..10f,
                                stepSize = 0.5f,
                                label = "Display Duration",
                                formattedValue = String.format(Locale.US, "%.1f SEC", notifPrefs.displayDurationMs / 1000f)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))
    }
}

private data class AlertChannelItem(
    val title: String,
    val glyph: String,
    val subtitle: String,
    val isEnabled: Boolean,
    val onToggle: (Boolean) -> Unit
)

@Composable
private fun AlertChannelCard(
    item: AlertChannelItem,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor by animateColorAsState(
        targetValue = if (item.isEnabled) NothingPrimaryAccent.copy(alpha = 0.5f) else NothingDivider,
        animationSpec = tween(180),
        label = "alertBorder"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(NothingElevatedSurface)
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable { onToggle() }
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Circle glyph
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(if (item.isEnabled) NothingPrimaryAccent.copy(alpha = 0.15f) else Color(0xFF1F1F1F))
                        .border(1.dp, if (item.isEnabled) NothingPrimaryAccent else Color(0x22FFFFFF), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = item.glyph,
                        color = if (item.isEnabled) NothingPrimaryAccent else NothingMutedText,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                NothingSwitch(
                    checked = item.isEnabled,
                    onCheckedChange = { onToggle() }
                )
            }

            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.title,
                        color = NothingPrimaryText,
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (item.isEnabled) NothingSuccess else NothingMutedText)
                    )
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = item.subtitle,
                    color = NothingSecondaryText,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }
        }
    }
}

@Composable
private fun BehaviorToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val haptic = LocalHapticFeedback.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onCheckedChange(!checked)
            }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = NothingPrimaryText,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = NothingSecondaryText,
                fontSize = 11.5.sp,
                lineHeight = 15.sp
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        NothingSwitch(
            checked = checked,
            onCheckedChange = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onCheckedChange(it)
            }
        )
    }
}
