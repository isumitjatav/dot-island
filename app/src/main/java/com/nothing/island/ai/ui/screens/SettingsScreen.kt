package com.nothing.island.ai.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.nothing.island.ai.CutoutGeometry
import com.nothing.island.ai.IslandOverlayService
import com.nothing.island.ai.OverlayPermissionManager
import com.nothing.island.ai.notifications.IslandNotificationListenerService
import com.nothing.island.ai.data.IslandPreferencesRepository
import com.nothing.island.ai.ui.components.IslandCard
import com.nothing.island.ai.ui.components.SectionHeader
import com.nothing.island.ai.ui.theme.NothingDivider
import com.nothing.island.ai.ui.theme.NothingElevatedSurface
import com.nothing.island.ai.ui.theme.NothingMutedText
import com.nothing.island.ai.ui.theme.NothingPrimaryAccent
import com.nothing.island.ai.ui.theme.NothingPrimaryBackground
import com.nothing.island.ai.ui.theme.NothingPrimaryText
import com.nothing.island.ai.ui.theme.NothingSecondaryText
import com.nothing.island.ai.ui.theme.NothingSuccess
import com.nothing.island.ai.ui.theme.NothingWarning
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    geometry: CutoutGeometry,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Real Permission Queries
    val hasAccessibility = OverlayPermissionManager.isAccessibilityServiceEnabled(context)
    val hasOverlay = OverlayPermissionManager.hasOverlayPermission(context)
    val hasNotificationPermission = OverlayPermissionManager.hasNotificationPermission(context)
    val hasNotificationListener = OverlayPermissionManager.hasNotificationListenerPermission(context)
    val isNotificationListenerConnected by IslandNotificationListenerService.connectionState.collectAsState()

    val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
    val isIgnoringBatteryOptimizations = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: false
    } else true

    val hasRecordAudio = ContextCompat.checkSelfPermission(
        context,
        android.Manifest.permission.RECORD_AUDIO
    ) == android.content.pm.PackageManager.PERMISSION_GRANTED

    // Persistent Preferences from DataStore
    val preferencesRepository = remember { IslandPreferencesRepository.getInstance(context) }
    val coroutineScope = rememberCoroutineScope()
    val prefs by preferencesRepository.livePreferences.collectAsState()
    val notifPrefs by preferencesRepository.liveNotificationPreferences.collectAsState()
    val batteryPrefs by preferencesRepository.liveBatteryPreferences.collectAsState()
    val callPrefs by preferencesRepository.liveCallPreferences.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NothingPrimaryBackground)
            .verticalScroll(scrollState)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Bar with Back Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(NothingElevatedSurface)
                    .border(1.dp, NothingDivider, RoundedCornerShape(10.dp))
                    .clickable { onNavigateBack() }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "← BACK",
                    color = NothingPrimaryText,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column {
                Text(
                    text = "SETTINGS & PERMISSIONS",
                    color = NothingPrimaryText,
                    fontSize = 16.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "SYSTEM GATEWAYS & HARDWARE DIAGNOSTICS",
                    color = NothingMutedText,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // ACCESSIBILITY OVERLAY ANCHOR (R4.6)
        SectionHeader(title = "ACCESSIBILITY OVERLAY ANCHOR (R4.6)")

        Spacer(modifier = Modifier.height(6.dp))

        IslandCard {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Accessibility Overlay Host",
                            color = NothingPrimaryText,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (hasAccessibility) "ACTIVE (TYPE_ACCESSIBILITY_OVERLAY)" else "DISABLED (Using Fallback)",
                            color = if (hasAccessibility) NothingSuccess else NothingWarning,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (hasAccessibility) NothingElevatedSurface else NothingPrimaryAccent)
                            .clickable {
                                val intent = OverlayPermissionManager.createAccessibilitySettingsIntent()
                                context.startActivity(intent)
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (hasAccessibility) "SETTINGS" else "ENABLE",
                            color = if (hasAccessibility) NothingPrimaryText else Color.White,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Text(
                    text = "Enables hardware-level touch interaction directly across the camera punch-hole (AOSP Layer 24000). Sits above the status bar without touch swallowing.",
                    color = NothingMutedText,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 15.sp
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(NothingElevatedSurface)
                        .padding(10.dp)
                ) {
                    Text(
                        text = "ZERO-DATA PRIVACY CONTRACT:\n• canRetrieveWindowContent = false (No screen reading)\n• canPerformGestures = false (No touch injection)\n• canRequestFilterKeyEvents = false (No keylogging)\n• Zero accessibility data is collected or transmitted.",
                        color = NothingSecondaryText,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 14.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // PERMISSIONS REGISTRY
        SectionHeader(title = "SYSTEM PERMISSIONS REGISTRY")

        Spacer(modifier = Modifier.height(6.dp))

        IslandCard {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                PermissionStatusRow(
                    title = "Display Over Other Apps",
                    subtitle = "Required to anchor the floating pill over applications",
                    isGranted = hasOverlay,
                    onGrantClick = {
                        val intent = OverlayPermissionManager.createOverlayPermissionIntent(context)
                        context.startActivity(intent)
                    }
                )

                PermissionStatusRow(
                    title = "Notification Posting",
                    subtitle = "Maintains persistent foreground service on Android 13+",
                    isGranted = hasNotificationPermission,
                    onGrantClick = {
                        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    }
                )

                PermissionStatusRow(
                    title = "Notification Listener Access",
                    subtitle = if (isNotificationListenerConnected) "Service active & connected (Milestone R6)" else "Allows reading incoming alerts & media metadata (R6)",
                    isGranted = hasNotificationListener,
                    onGrantClick = {
                        val intent = OverlayPermissionManager.createNotificationListenerSettingsIntent()
                        context.startActivity(intent)
                    }
                )

                PermissionStatusRow(
                    title = "Battery Optimization Exemption",
                    subtitle = "Prevents system background termination during deep sleep",
                    isGranted = isIgnoringBatteryOptimizations,
                    onGrantClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                data = Uri.parse("package:${context.packageName}")
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            try { context.startActivity(intent) } catch (_: Exception) {}
                        }
                    }
                )

                PermissionStatusRow(
                    title = "Microphone Access (RECORD_AUDIO)",
                    subtitle = "Required for audio waveform visualizer (M5)",
                    isGranted = hasRecordAudio,
                    onGrantClick = {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.parse("package:${context.packageName}")
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // QUICK CONTROLS — live customizable preview
        SectionHeader(title = "QUICK CONTROLS — CUSTOMIZE", badgeText = "LIVE", badgeColor = NothingPrimaryAccent)
        Spacer(modifier = Modifier.height(6.dp))
        com.nothing.island.ai.ui.components.QuickControlsPreview()
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Tapping a tile adds/removes it from the island's quick controls drawer.",
            color = NothingSecondaryText,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace
        )

        Spacer(modifier = Modifier.height(18.dp))

        // GENERAL PREFERENCES
        SectionHeader(title = "GENERAL PREFERENCES")

        Spacer(modifier = Modifier.height(6.dp))

        IslandCard {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                PreferenceToggleRow(
                    title = "Hide During Games",
                    subtitle = "Automatically hides the island during gaming or landscape fullscreen to avoid covering controls.",
                    checked = prefs.autoHideInGames,
                    onCheckedChange = { enabled ->
                        coroutineScope.launch {
                            preferencesRepository.updateAutoHideInGames(enabled)
                        }
                    }
                )

                PreferenceToggleRow(
                    title = "Camera Cutout Notch",
                    subtitle = "Wrap expanded cards around the front camera lens (reference style). When disabled, cards use classic smooth rounded corners.",
                    checked = prefs.isCameraCutoutEnabled,
                    onCheckedChange = { enabled ->
                        coroutineScope.launch {
                            preferencesRepository.updateCameraCutoutEnabled(enabled)
                        }
                    }
                )

                PreferenceToggleRow(
                    title = "Haptic Vibration Feedback",
                    subtitle = "Subtle tactile click when expanding or collapsing",
                    checked = prefs.hapticFeedbackEnabled,
                    onCheckedChange = { enabled ->
                        coroutineScope.launch {
                            preferencesRepository.updateHapticFeedback(enabled)
                        }
                    }
                )

                PreferenceToggleRow(
                    title = "Reduced Motion Animations",
                    subtitle = "Simplifies transition dynamics to conserve battery",
                    checked = prefs.reducedMotionEnabled,
                    onCheckedChange = { enabled ->
                        coroutineScope.launch {
                            preferencesRepository.updateReducedMotion(enabled)
                        }
                    }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "Visual Theme", color = NothingPrimaryText, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Text(text = "Active interface color scheme", color = NothingMutedText, fontSize = 11.sp)
                    }
                    Text(
                        text = "NOTHING MONOCHROME",
                        color = NothingPrimaryAccent,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // NOTIFICATION BEHAVIOUR & SYNC (R6 / R7.1)
        SectionHeader(title = "NOTIFICATION BEHAVIOUR & SYNC")

        Spacer(modifier = Modifier.height(6.dp))

        IslandCard {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                PreferenceToggleRow(
                    title = "Automatically expand for notifications",
                    subtitle = "Open Dot Island when a new eligible notification arrives.",
                    checked = notifPrefs.autoExpandOnNotification,
                    onCheckedChange = { enabled ->
                        coroutineScope.launch {
                            preferencesRepository.updateNotificationAutoExpand(enabled)
                        }
                    }
                )

                PreferenceToggleRow(
                    title = "Sync Dismissals with Android",
                    subtitle = "Dismissing or clearing alerts in Dot Island also cancels them from the Android notification shade. When disabled, only local island history is cleared.",
                    checked = notifPrefs.syncDismissWithAndroid,
                    onCheckedChange = { enabled ->
                        coroutineScope.launch {
                            preferencesRepository.updateSyncDismissWithAndroid(enabled)
                        }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // BATTERY & CHARGING INTELLIGENCE (Milestone R8)
        SectionHeader(title = "BATTERY & CHARGING INTELLIGENCE")

        Spacer(modifier = Modifier.height(6.dp))

        IslandCard {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                PreferenceToggleRow(
                    title = "Battery & Charging Intelligence",
                    subtitle = "Dynamic battery status, charging telemetry, and low-power alerts.",
                    checked = batteryPrefs.enabled,
                    onCheckedChange = { enabled ->
                        coroutineScope.launch {
                            preferencesRepository.updateBatteryEnabled(enabled)
                        }
                    }
                )

                PreferenceToggleRow(
                    title = "Automatically expand when plugged in",
                    subtitle = "Open Dot Island charging intelligence card when charger is connected.",
                    checked = batteryPrefs.autoExpandOnPlugged,
                    onCheckedChange = { enabled ->
                        coroutineScope.launch {
                            preferencesRepository.updateBatteryAutoExpandOnPlugged(enabled)
                        }
                    }
                )

                PreferenceToggleRow(
                    title = "Temperature Safety Warning",
                    subtitle = "Show urgent safety alert if battery exceeds safety limits (45°C).",
                    checked = batteryPrefs.temperatureWarningEnabled,
                    onCheckedChange = { enabled ->
                        coroutineScope.launch {
                            preferencesRepository.updateBatteryTempWarningEnabled(enabled)
                        }
                    }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "Low Battery Alert Threshold", color = NothingPrimaryText, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Text(text = "Threshold to trigger safety alert", color = NothingMutedText, fontSize = 11.sp)
                    }
                    Text(
                        text = "${batteryPrefs.lowBatteryThreshold}%",
                        color = NothingPrimaryAccent,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // TELEPHONY & LIVE CALLS (Milestone R9)
        SectionHeader(title = "TELEPHONY & LIVE CALLS")

        Spacer(modifier = Modifier.height(6.dp))

        IslandCard {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                PreferenceToggleRow(
                    title = "Live Call Island",
                    subtitle = "Show active calls and incoming ringing in Dot Island.",
                    checked = callPrefs.systemCallEnabled,
                    onCheckedChange = { enabled ->
                        coroutineScope.launch {
                            preferencesRepository.updateCallPreferences(callPrefs.copy(systemCallEnabled = enabled))
                        }
                    }
                )

                PreferenceToggleRow(
                    title = "Auto-Expand Call Card",
                    subtitle = "When disabled, calls appear as a sleek compact capsule around the camera. Enable to automatically expand into a full card on connect.",
                    checked = callPrefs.autoExpandCall,
                    onCheckedChange = { enabled ->
                        coroutineScope.launch {
                            preferencesRepository.updateCallPreferences(callPrefs.copy(autoExpandCall = enabled))
                        }
                    }
                )

                PreferenceToggleRow(
                    title = "Double Incoming Call Actions",
                    subtitle = "Show the red end-call button on the compact pill.",
                    checked = callPrefs.doubleIncomingCallActions,
                    onCheckedChange = { enabled ->
                        coroutineScope.launch {
                            preferencesRepository.updateCallPreferences(callPrefs.copy(doubleIncomingCallActions = enabled))
                        }
                    }
                )

                PreferenceToggleRow(
                    title = "VoIP & Other Apps Calls",
                    subtitle = "Display calls from third-party communication apps (WhatsApp, Telegram, etc.).",
                    checked = callPrefs.otherAppsCalls,
                    onCheckedChange = { enabled ->
                        coroutineScope.launch {
                            preferencesRepository.updateCallPreferences(callPrefs.copy(otherAppsCalls = enabled))
                        }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // HARDWARE & RUNTIME DIAGNOSTICS
        SectionHeader(title = "HARDWARE & RUNTIME DIAGNOSTICS")

        Spacer(modifier = Modifier.height(6.dp))

        IslandCard {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DiagItem("Hardware Model", "${Build.MODEL} (${Build.DEVICE})")
                DiagItem("Android Version", "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
                DiagItem("Target SDK", "36 (Android 14+ FGS Compliant)")
                DiagItem("Display Resolution", "1224 x 2720 px @ 480 dpi")
                DiagItem("Density Scale Factor", "${geometry.density}x")
                DiagItem("Punch-Hole Center", "X: ${geometry.centerXDp.toInt()}dp, Y: ${geometry.centerYDp.toInt()}dp")
                DiagItem("Cutout Safe Inset Top", "${geometry.safeInsetTopDp.toInt()} dp (${geometry.safeInsetTopPx} px)")
                DiagItem("Overlay Service State", if (IslandOverlayService.isRunning) "RUNNING" else "STOPPED")
                DiagItem("Application Package", "com.nothing.island.ai")
                DiagItem("Milestone Stage", "M4.1 (Design System)")
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // ABOUT CARD
        SectionHeader(title = "ABOUT DOT ISLAND")

        Spacer(modifier = Modifier.height(6.dp))

        IslandCard {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Dot Island v1.0.2",
                    color = NothingPrimaryText,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Native Android companion for Nothing Phone (4a). Designed with pure AMOLED minimalism, precision camera cutout alignment, and zero bloat.",
                    color = NothingSecondaryText,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.SansSerif,
                    lineHeight = 16.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "PRIVACY: All notification parsing, audio visualizer processing, and cutout calibration run 100% on-device. Zero advertising telemetry.",
                    color = NothingMutedText,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun PermissionStatusRow(
    title: String,
    subtitle: String,
    isGranted: Boolean,
    onGrantClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = NothingPrimaryText,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.SansSerif
            )
            Text(
                text = subtitle,
                color = NothingMutedText,
                fontSize = 11.sp,
                fontFamily = FontFamily.SansSerif
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        if (isGranted) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(NothingElevatedSurface)
                    .border(1.dp, NothingSuccess.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    text = "ACTIVE",
                    color = NothingSuccess,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(NothingPrimaryAccent)
                    .clickable { onGrantClick() }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "GRANT",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun PreferenceToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = NothingPrimaryText,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.SansSerif
            )
            Text(
                text = subtitle,
                color = NothingMutedText,
                fontSize = 11.sp,
                fontFamily = FontFamily.SansSerif
            )
        }

        com.nothing.island.ai.ui.components.NothingSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
fun DiagItem(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = NothingMutedText, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        Text(text = value, color = NothingPrimaryText, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
    }
}
