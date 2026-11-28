package com.nothing.island.ai.ui.screens

import android.content.Context
import android.os.Build
import android.os.PowerManager
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nothing.island.ai.CutoutGeometry
import com.nothing.island.ai.OverlayPermissionManager
import com.nothing.island.ai.data.IslandPreferencesRepository
import com.nothing.island.ai.notifications.IslandNotificationListenerService
import com.nothing.island.ai.ui.components.IslandCard
import com.nothing.island.ai.ui.components.NothingSwitch
import com.nothing.island.ai.ui.components.ProminentDisclosureDialog
import com.nothing.island.ai.ui.components.ProminentDisclosureType
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

data class InstalledAppItem(
    val categoryName: String,
    val packageName: String,
    val appLabel: String
)

/**
 * Clean Settings Screen
 *
 * Implements:
 * 1. App Permissions (Display Over Other Apps, Notification Access, Battery Optimization)
 * 2. Display & Touch Behaviour (Auto-Hide in Games, Haptic Feedback, Reduced Motion)
 * 3. Battery & Charging Alerts (Battery Intelligence, Auto-Expand on Plugged, Temp Warning)
 * 4. Replay Welcome Guide
 * 5. Version
 */
@Composable
fun SettingsScreen(
    geometry: CutoutGeometry? = null,
    onNavigateBack: () -> Unit = {},
    onReopenOnboarding: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val preferencesRepository = remember { IslandPreferencesRepository.getInstance(context) }
    val prefs by preferencesRepository.livePreferences.collectAsState()
    val batteryPrefs by preferencesRepository.liveBatteryPreferences.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    var activeDisclosure by remember { mutableStateOf<ProminentDisclosureType?>(null) }

    activeDisclosure?.let { type ->
        ProminentDisclosureDialog(
            type = type,
            onAcceptAndContinue = {
                activeDisclosure = null
                when (type) {
                    ProminentDisclosureType.NOTIFICATION_LISTENER -> {
                        context.startActivity(
                            com.nothing.island.ai.notifications.NotificationPermissionManager
                                .createNotificationAccessIntent(context)
                        )
                    }
                    ProminentDisclosureType.ACCESSIBILITY -> {
                        context.startActivity(OverlayPermissionManager.createAccessibilitySettingsIntent())
                    }
                }
            },
            onDecline = { activeDisclosure = null }
        )
    }

    // Live permission states
    val hasOverlay = OverlayPermissionManager.hasOverlayPermission(context)
    val hasNotificationListener = OverlayPermissionManager.hasNotificationListenerPermission(context)
    val isNotificationListenerConnected by IslandNotificationListenerService.connectionState.collectAsState()

    val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
    val isIgnoringBatteryOptimizations = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: false
    } else true

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NothingPrimaryBackground)
            .verticalScroll(scrollState)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
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
                        text = "SETTINGS",
                        color = NothingPrimaryText,
                        fontSize = 18.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    )
                }
                Text(
                    text = "PREFERENCES & PERMISSIONS",
                    color = NothingMutedText,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(NothingElevatedSurface)
                    .border(1.dp, NothingDivider, RoundedCornerShape(10.dp))
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onNavigateBack()
                    }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "DONE",
                    color = NothingPrimaryText,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 1. APP PERMISSIONS
        SectionHeader(
            title = "APP PERMISSIONS",
            badgeText = "REQUIRED",
            badgeColor = NothingPrimaryAccent
        )
        Spacer(modifier = Modifier.height(6.dp))

        IslandCard {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // Overlay Permission
                PermissionRow(
                    title = "Display Over Other Apps",
                    subtitle = "Required to anchor the floating pill over applications",
                    isGranted = hasOverlay,
                    onGrant = {
                        val intent = OverlayPermissionManager.createOverlayPermissionIntent(context)
                        context.startActivity(intent)
                    }
                )

                // Notification & Music Access
                PermissionRow(
                    title = "Notification & Music Access",
                    subtitle = if (isNotificationListenerConnected) "Connected — alerts and media active" else "Allows reading incoming alerts and music metadata",
                    isGranted = hasNotificationListener,
                    onGrant = {
                        activeDisclosure = ProminentDisclosureType.NOTIFICATION_LISTENER
                    }
                )

                // Battery Optimization
                PermissionRow(
                    title = "Battery Optimization Exemption",
                    subtitle = "Prevents system background termination during sleep",
                    isGranted = isIgnoringBatteryOptimizations,
                    onGrant = {
                        OverlayPermissionManager.launchBatteryOptimizationSettings(context)
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // 2. DISPLAY & TOUCH BEHAVIOUR
        SectionHeader(
            title = "DISPLAY & TOUCH",
            badgeText = "EXPERIENCE",
            badgeColor = NothingPrimaryAccent
        )
        Spacer(modifier = Modifier.height(6.dp))

        IslandCard {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Auto-Hide in Games
                SettingToggleRow(
                    title = "Auto-Hide in Fullscreen Games",
                    subtitle = "Temporarily hides Dot Island when gaming or viewing fullscreen media to avoid obstructing controls.",
                    checked = prefs.autoHideInGames,
                    onCheckedChange = { isChecked ->
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        coroutineScope.launch {
                            preferencesRepository.updateAutoHideInGames(isChecked)
                        }
                    }
                )

                // Haptic Feedback
                SettingToggleRow(
                    title = "Haptic Vibration Feedback",
                    subtitle = "Subtle tactile vibration feedback when interacting with island pills and controls.",
                    checked = prefs.hapticFeedbackEnabled,
                    onCheckedChange = { isChecked ->
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        coroutineScope.launch {
                            preferencesRepository.updateHapticFeedback(isChecked)
                        }
                    }
                )

                // Reduced Motion
                SettingToggleRow(
                    title = "Reduced Motion Animations",
                    subtitle = "Simplifies transition dynamics from spring physics to subtle fades to conserve battery.",
                    checked = prefs.reducedMotionEnabled,
                    onCheckedChange = { isChecked ->
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        coroutineScope.launch {
                            preferencesRepository.updateReducedMotion(isChecked)
                        }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // 3. BATTERY & CHARGING ALERTS
        SectionHeader(
            title = "BATTERY & CHARGING ALERTS",
            badgeText = "SMART",
            badgeColor = NothingPrimaryAccent
        )
        Spacer(modifier = Modifier.height(6.dp))

        IslandCard {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SettingToggleRow(
                    title = "Battery & Charging Intelligence",
                    subtitle = "Dynamic battery status, charging animations, and low-power notifications.",
                    checked = batteryPrefs.enabled,
                    onCheckedChange = { isChecked ->
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        coroutineScope.launch {
                            preferencesRepository.updateBatteryEnabled(isChecked)
                        }
                    }
                )

                SettingToggleRow(
                    title = "Auto-Expand on Plugged In",
                    subtitle = "Open Dot Island charging intelligence card automatically when charger is connected.",
                    checked = batteryPrefs.autoExpandOnPlugged,
                    onCheckedChange = { isChecked ->
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        coroutineScope.launch {
                            preferencesRepository.updateBatteryAutoExpandOnPlugged(isChecked)
                        }
                    }
                )

                SettingToggleRow(
                    title = "Temperature Safety Warning",
                    subtitle = "Show urgent safety alert on the island if battery temperature exceeds 45°C.",
                    checked = batteryPrefs.temperatureWarningEnabled,
                    onCheckedChange = { isChecked ->
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        coroutineScope.launch {
                            preferencesRepository.updateBatteryTempWarningEnabled(isChecked)
                        }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // 4. REPLAY WELCOME GUIDE
        SectionHeader(
            title = "ONBOARDING & SETUP",
            badgeText = "GUIDE",
            badgeColor = NothingPrimaryAccent
        )
        Spacer(modifier = Modifier.height(6.dp))

        IslandCard {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Replay Welcome Guide",
                        color = NothingPrimaryText,
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Step through the 15-second setup guide and permission checker again.",
                        color = NothingSecondaryText,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(NothingElevatedSurface)
                        .border(1.dp, NothingDivider, RoundedCornerShape(8.dp))
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onReopenOnboarding()
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "REPLAY",
                        color = NothingPrimaryText,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // 5. VERSION & PRIVACY
        SectionHeader(title = "ABOUT DOT ISLAND")
        Spacer(modifier = Modifier.height(6.dp))

        IslandCard {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Dot Island v1.2.0",
                        color = NothingPrimaryText,
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(NothingElevatedSurface)
                            .border(1.dp, NothingDivider, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "OFFLINE ONLY",
                            color = NothingSuccess,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Text(
                    text = "Smart dynamic camera punch-hole island for Android. Designed with Apple-grade spring physics, Nothing industrial dot aesthetics, and zero internet permissions.",
                    color = NothingSecondaryText,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "100% On-Device • Zero Analytics",
                        color = NothingMutedText,
                        fontSize = 10.5.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))
    }
}

@Composable
private fun SettingToggleRow(
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
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
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

@Composable
private fun PermissionRow(
    title: String,
    subtitle: String,
    isGranted: Boolean,
    onGrant: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    color = NothingPrimaryText,
                    fontSize = 13.5.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                if (isGranted) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "✓",
                        color = NothingSuccess,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = NothingSecondaryText,
                fontSize = 11.5.sp,
                lineHeight = 15.sp
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(if (isGranted) NothingElevatedSurface else NothingPrimaryAccent)
                .border(
                    1.dp,
                    if (isGranted) NothingSuccess.copy(alpha = 0.5f) else NothingPrimaryAccent,
                    RoundedCornerShape(8.dp)
                )
                .clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    if (!isGranted) {
                        onGrant()
                    }
                }
                .padding(horizontal = 10.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isGranted) "ALLOWED" else "GRANT",
                color = if (isGranted) NothingSuccess else Color.White,
                fontSize = 10.5.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
