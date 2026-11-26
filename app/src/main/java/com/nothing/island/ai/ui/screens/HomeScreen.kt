package com.nothing.island.ai.ui.screens

import android.os.Build
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nothing.island.ai.CutoutGeometry
import com.nothing.island.ai.data.IslandPreferencesRepository
import com.nothing.island.ai.navigation.IslandDestination
import com.nothing.island.ai.ui.components.DotMatrixGlyph
import com.nothing.island.ai.ui.components.DotMatrixPatterns
import com.nothing.island.ai.ui.components.IslandCard
import com.nothing.island.ai.ui.components.IslandPreview
import com.nothing.island.ai.ui.components.NothingStatusButton
import com.nothing.island.ai.ui.components.SectionHeader
import com.nothing.island.ai.ui.components.StatusIndicator
import com.nothing.island.ai.ui.theme.NothingDivider
import com.nothing.island.ai.ui.theme.NothingElevatedSurface
import com.nothing.island.ai.ui.theme.NothingMutedText
import com.nothing.island.ai.ui.theme.NothingPrimaryAccent
import com.nothing.island.ai.ui.theme.NothingPrimaryBackground
import com.nothing.island.ai.ui.theme.NothingPrimaryText
import com.nothing.island.ai.ui.theme.NothingSecondaryText
import com.nothing.island.ai.ui.theme.NothingSuccess
import com.nothing.island.ai.ui.theme.NothingTokens
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    hasOverlayPermission: Boolean,
    isServiceRunning: Boolean,
    geometry: CutoutGeometry,
    hasNotificationPermission: Boolean = true,
    onRequestPermission: () -> Unit,
    onRequestNotificationPermission: () -> Unit = {},
    onToggleService: () -> Unit,
    onNavigateToDestination: (IslandDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val preferencesRepository = remember { IslandPreferencesRepository.getInstance(context) }
    val prefs by preferencesRepository.livePreferences.collectAsState()
    val scrollState = rememberScrollState()
    val isPreviewExpanded = remember { mutableStateOf(false) }
    var showNotifDisclosure by remember { mutableStateOf(false) }

    if (showNotifDisclosure) {
        com.nothing.island.ai.ui.components.ProminentDisclosureDialog(
            type = com.nothing.island.ai.ui.components.ProminentDisclosureType.NOTIFICATION_LISTENER,
            onAcceptAndContinue = {
                showNotifDisclosure = false
                onRequestNotificationPermission()
            },
            onDecline = { showNotifDisclosure = false }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NothingPrimaryBackground)
            .verticalScroll(scrollState)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // App Top Bar
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
                        text = "DOT ISLAND",
                        color = NothingPrimaryText,
                        fontSize = 18.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    )
                }
                Text(
                    text = "PUNCH-HOLE OVERLAY ENGINE",
                    color = NothingMutedText,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
            }

            // Settings gear button
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(NothingElevatedSurface)
                    .border(1.dp, NothingDivider, CircleShape)
                    .clickable { onNavigateToDestination(IslandDestination.Settings) },
                contentAlignment = Alignment.Center
            ) {
                DotMatrixGlyph(
                    pattern = DotMatrixPatterns.Gear,
                    dotSize = 3.dp,
                    spacing = 2.dp,
                    activeColor = NothingPrimaryText
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Live UI Preview Card
        IslandPreview(
            isExpanded = isPreviewExpanded.value,
            onToggleExpanded = { isPreviewExpanded.value = !isPreviewExpanded.value },
            pillWidthDp = prefs.compactWidthDp,
            pillHeightDp = prefs.compactHeightDp,
            cornerRadiusDp = prefs.cornerRadiusDp,
            topOffsetDp = prefs.topMarginDp,
            horizontalOffsetDp = prefs.horizontalOffsetDp,
            accentColor = Color(prefs.accentColorHex),
            isCameraCutoutEnabled = prefs.isCameraCutoutEnabled,
            statusText = "${Build.MODEL} (${Build.DEVICE})"
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Service & Permission Controls Card
        SectionHeader(title = "CORE ENGINE STATUS", badgeText = if (isServiceRunning) "ACTIVE" else "STANDBY", badgeColor = if (isServiceRunning) NothingSuccess else NothingMutedText)

        Spacer(modifier = Modifier.height(6.dp))

        IslandCard {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // Permission Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Display over other apps",
                            color = NothingPrimaryText,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = if (hasOverlayPermission) "Permission Granted" else "Required for punch-hole overlay",
                            color = if (hasOverlayPermission) NothingSuccess else NothingPrimaryAccent,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    if (!hasOverlayPermission) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(NothingPrimaryAccent)
                                .clickable { onRequestPermission() }
                                .padding(horizontal = 14.dp, vertical = 7.dp)
                        ) {
                            Text(
                                text = "GRANT",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(NothingElevatedSurface)
                                .border(1.dp, NothingSuccess.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = "ALLOWED",
                                color = NothingSuccess,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Notification access — required for notifications + music pills
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Notification access",
                            color = NothingPrimaryText,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = if (hasNotificationPermission) "Connected — alerts & media enabled"
                                   else "REQUIRED for notifications & music",
                            color = if (hasNotificationPermission) NothingSuccess else NothingPrimaryAccent,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    if (!hasNotificationPermission) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(NothingPrimaryAccent)
                                .clickable { showNotifDisclosure = true }
                                .padding(horizontal = 12.dp, vertical = 7.dp)
                        ) {
                            Text(
                                text = "GRANT",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(NothingElevatedSurface)
                                .border(1.dp, NothingSuccess.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = "ALLOWED",
                                color = NothingSuccess,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Service Toggle Button with Animata-style Status Button dynamics
                NothingStatusButton(
                    isActive = isServiceRunning,
                    enabled = hasOverlayPermission,
                    enabledText = "STOP DOT ISLAND",
                    disabledText = "START DOT ISLAND",
                    enabledSubtext = "FLOATING OVERLAY ACTIVE",
                    disabledSubtext = "TAP TO RUN ISLAND OVERLAY",
                    onClick = onToggleService,
                    accentColor = Color(prefs.accentColorHex),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Quick Shortcuts Row
        SectionHeader(title = "QUICK SHORTCUTS")

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ShortcutCard(
                label = "APPEARANCE",
                subLabel = "Calibrate & Style",
                modifier = Modifier.weight(1f),
                onClick = { onNavigateToDestination(IslandDestination.Appearance) }
            )
            ShortcutCard(
                label = "FEATURES",
                subLabel = "Modules Registry",
                modifier = Modifier.weight(1f),
                onClick = { onNavigateToDestination(IslandDestination.Features) }
            )
            ShortcutCard(
                label = "GEMINI AI",
                subLabel = "AI Assistant",
                modifier = Modifier.weight(1f),
                onClick = { onNavigateToDestination(IslandDestination.Gemini) }
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Activity Overview (Honest Module States)
        SectionHeader(title = "MODULE ACTIVITY OVERVIEW")

        Spacer(modifier = Modifier.height(6.dp))

        IslandCard {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ModuleStatusRow(
                    "System Notifications",
                    if (hasNotificationPermission) "Connected" else "Permission required",
                    if (hasNotificationPermission) NothingSuccess else NothingPrimaryAccent
                )
                ModuleStatusRow(
                    "Media Playback",
                    if (hasNotificationPermission) "Connected" else "Permission required",
                    if (hasNotificationPermission) NothingSuccess else NothingPrimaryAccent
                )
                val mascotSprite = com.nothing.island.ai.ui.components.mascotByName(prefs.mascotSpriteName)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToDestination(IslandDestination.MascotStudio) },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Pixel Companion", color = NothingSecondaryText, fontSize = 12.sp, fontFamily = FontFamily.SansSerif)
                    Text(
                        text = if (prefs.mascotEnabled) "${mascotSprite.displayName} • Active" else "Standby • Tap to enable",
                        color = if (prefs.mascotEnabled) NothingSuccess else NothingMutedText,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
                ModuleStatusRow("Charging & Battery", "UI Integration Pending (M4.4)", NothingMutedText)
                ModuleStatusRow("Gemini AI Engine", "Not configured (M5)", NothingMutedText)
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Hardware Cutout Geometry (Preserved Diagnostics)
        SectionHeader(title = "HARDWARE TELEMETRY")

        Spacer(modifier = Modifier.height(6.dp))

        IslandCard {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TelemetryRow("Device Model", "${Build.MODEL} (${Build.DEVICE})")
                TelemetryRow("Android Version", "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
                TelemetryRow("Target SDK", "36 (Android 14+ FGS Compliant)")
                TelemetryRow("Cutout Center", "X: ${geometry.centerXDp.toInt()}dp, Y: ${geometry.centerYDp.toInt()}dp")
                TelemetryRow("Cutout Insets", "Top: ${geometry.safeInsetTopDp.toInt()}dp (${geometry.safeInsetTopPx}px)")
                TelemetryRow("Engine Architecture", "Headless Compose + WindowManager")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun ShortcutCard(
    label: String,
    subLabel: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(NothingElevatedSurface)
            .border(1.dp, NothingDivider, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Column {
            Text(
                text = label,
                color = NothingPrimaryText,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subLabel,
                color = NothingMutedText,
                fontSize = 10.sp,
                fontFamily = FontFamily.SansSerif
            )
        }
    }
}

@Composable
fun ModuleStatusRow(name: String, status: String, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = name, color = NothingSecondaryText, fontSize = 12.sp, fontFamily = FontFamily.SansSerif)
        Text(text = status, color = color, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
    }
}

@Composable
fun TelemetryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = NothingMutedText, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        Text(text = value, color = NothingPrimaryText, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
    }
}
