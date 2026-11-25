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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.nothing.island.ai.data.IslandPreferencesRepository
import com.nothing.island.ai.ui.components.IslandCard
import com.nothing.island.ai.ui.components.IslandPreview
import com.nothing.island.ai.ui.components.NothingStepperSlider
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

/** Returns a human-readable onboarding step title. Pure and unit-testable. */
internal fun onboardingStepTitle(stepIndex: Int): String = when (stepIndex.coerceIn(0, 2)) {
    0 -> "OVERLAY PERMISSION"
    1 -> "DISCLOSURES & ACCESS"
    else -> "CALIBRATION & BATTERY"
}

/**
 * 3-step Google Play–compliant first-launch onboarding flow for Dot Island:
 * - Step 1: Display over other apps
 * - Step 2: Prominent Disclosures + Notification Access + Optional Accessibility Shortcuts
 * - Step 3: Camera Cutout Calibration Preview + Unrestricted Battery Guard
 */
@Composable
fun OnboardingScreen(
    hasOverlayPermission: Boolean,
    hasNotificationListenerPermission: Boolean,
    hasAccessibilityPermission: Boolean,
    isIgnoringBatteryOptimizations: Boolean,
    onRequestOverlayPermission: () -> Unit,
    onRequestNotificationAccess: () -> Unit,
    onRequestAccessibilityAccess: () -> Unit,
    onRequestBatteryOptimization: () -> Unit,
    onSkip: () -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repository = remember { IslandPreferencesRepository.getInstance(context) }
    val prefs by repository.livePreferences.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var stepIndex by remember { mutableIntStateOf(0) }
    var activeDisclosure by remember { mutableStateOf<ProminentDisclosureType?>(null) }

    activeDisclosure?.let { type ->
        ProminentDisclosureDialog(
            type = type,
            onAcceptAndContinue = {
                activeDisclosure = null
                when (type) {
                    ProminentDisclosureType.NOTIFICATION_LISTENER -> onRequestNotificationAccess()
                    ProminentDisclosureType.ACCESSIBILITY -> onRequestAccessibilityAccess()
                }
            },
            onDecline = { activeDisclosure = null }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NothingPrimaryBackground)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
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
                            text = "DOT ISLAND SETUP",
                            color = NothingPrimaryText,
                            fontSize = 16.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                    Text(
                        text = "STEP 0${stepIndex + 1} / 03 • ${onboardingStepTitle(stepIndex)}",
                        color = NothingMutedText,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(NothingElevatedSurface)
                        .border(1.dp, NothingDivider, RoundedCornerShape(10.dp))
                        .clickable { onSkip() }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "SKIP",
                        color = NothingSecondaryText,
                        fontSize = 10.5.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3-step segmented dot-matrix progress bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (i in 0..2) {
                    val isActive = i == stepIndex
                    val isPassed = i < stepIndex
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(5.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    isActive -> NothingPrimaryAccent
                                    isPassed -> NothingPrimaryText
                                    else -> NothingElevatedSurface
                                }
                            )
                            .clickable { stepIndex = i }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            when (stepIndex) {
                0 -> {
                    SectionHeader(
                        title = "STEP 1 • FLOATING CAPSULE",
                        badgeText = if (hasOverlayPermission) "GRANTED" else "REQUIRED",
                        badgeColor = if (hasOverlayPermission) NothingSuccess else NothingPrimaryAccent
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    IslandCard {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                text = "Display over other apps",
                                color = NothingPrimaryText,
                                fontSize = 16.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Dot Island anchors an interactive Dynamic Capsule around your front camera punch-hole so music, calls, earbuds, charging telemetry, and quick shortcuts stay accessible over any app.",
                                color = NothingSecondaryText,
                                fontSize = 12.5.sp,
                                lineHeight = 18.sp
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(NothingElevatedSurface)
                                    .border(1.dp, NothingDivider, RoundedCornerShape(12.dp))
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (hasOverlayPermission) "SYSTEM_ALERT_WINDOW ALLOWED" else "OVERLAY PERMISSION PENDING",
                                    color = if (hasOverlayPermission) NothingSuccess else NothingMutedText,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (hasOverlayPermission) NothingElevatedSurface else NothingPrimaryAccent)
                                    .border(
                                        1.dp,
                                        if (hasOverlayPermission) NothingSuccess.copy(alpha = 0.5f) else NothingPrimaryAccent,
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable { onRequestOverlayPermission() }
                                    .padding(vertical = 13.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (hasOverlayPermission) "OVERLAY PERMISSION GRANTED ✓" else "GRANT DISPLAY OVER OTHER APPS",
                                    color = if (hasOverlayPermission) NothingSuccess else Color.White,
                                    fontSize = 11.5.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                1 -> {
                    SectionHeader(
                        title = "STEP 2 • PROMINENT DISCLOSURES",
                        badgeText = if (hasNotificationListenerPermission) "READY" else "ACTION NEEDED",
                        badgeColor = if (hasNotificationListenerPermission) NothingSuccess else NothingPrimaryAccent
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Card A: Notification Access
                    IslandCard {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Notification Access",
                                    color = NothingPrimaryText,
                                    fontSize = 15.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (hasNotificationListenerPermission) "ALLOWED" else "REQUIRED",
                                    color = if (hasNotificationListenerPermission) NothingSuccess else NothingPrimaryAccent,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "Allows Dot Island to read incoming notifications and media sessions strictly in on-device memory to show alerts, replies, and playback pills. Zero data leaves your phone.",
                                color = NothingSecondaryText,
                                fontSize = 12.sp,
                                lineHeight = 17.sp
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (hasNotificationListenerPermission) NothingElevatedSurface else NothingPrimaryAccent)
                                    .border(
                                        1.dp,
                                        if (hasNotificationListenerPermission) NothingSuccess.copy(alpha = 0.5f) else NothingPrimaryAccent,
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable {
                                        activeDisclosure = ProminentDisclosureType.NOTIFICATION_LISTENER
                                    }
                                    .padding(vertical = 11.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (hasNotificationListenerPermission) "NOTIFICATION ACCESS ALLOWED ✓" else "REVIEW DISCLOSURE & GRANT",
                                    color = if (hasNotificationListenerPermission) NothingSuccess else Color.White,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Card B: Optional Accessibility Shortcuts
                    IslandCard {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Accessibility Shortcuts",
                                    color = NothingPrimaryText,
                                    fontSize = 15.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (hasAccessibilityPermission) "ACTIVE" else "OPTIONAL",
                                    color = if (hasAccessibilityPermission) NothingSuccess else NothingMutedText,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "Optional service for Layer 24000 punch-hole touches and local quick actions (Lock, Screenshot, Media). Runs with canRetrieveWindowContent = false and collects zero data.",
                                color = NothingSecondaryText,
                                fontSize = 12.sp,
                                lineHeight = 17.sp
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(NothingElevatedSurface)
                                    .border(
                                        1.dp,
                                        if (hasAccessibilityPermission) NothingSuccess.copy(alpha = 0.5f) else NothingDivider,
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable {
                                        activeDisclosure = ProminentDisclosureType.ACCESSIBILITY
                                    }
                                    .padding(vertical = 11.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (hasAccessibilityPermission) "ACCESSIBILITY ACTIVE ✓" else "REVIEW DISCLOSURE & ENABLE",
                                    color = if (hasAccessibilityPermission) NothingSuccess else NothingPrimaryText,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                else -> {
                    SectionHeader(
                        title = "STEP 3 • CUTOUT & BATTERY",
                        badgeText = if (isIgnoringBatteryOptimizations) "UNRESTRICTED" else "CALIBRATE",
                        badgeColor = if (isIgnoringBatteryOptimizations) NothingSuccess else NothingPrimaryAccent
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    IslandPreview(
                        pillWidthDp = prefs.compactWidthDp,
                        pillHeightDp = prefs.compactHeightDp,
                        cornerRadiusDp = prefs.cornerRadiusDp,
                        topOffsetDp = prefs.topMarginDp,
                        horizontalOffsetDp = prefs.horizontalOffsetDp,
                        isCameraCutoutEnabled = prefs.isCameraCutoutEnabled
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    IslandCard {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            NothingStepperSlider(
                                value = prefs.compactWidthDp,
                                onValueChange = { repository.updateLiveCalibration(compactWidthDp = it) },
                                valueRange = 80f..260f,
                                stepSize = 2f,
                                label = "Capsule Width",
                                formattedValue = "${prefs.compactWidthDp.toInt()} DP"
                            )
                            NothingStepperSlider(
                                value = prefs.horizontalOffsetDp,
                                onValueChange = { repository.updateLiveCalibration(horizontalOffsetDp = it) },
                                valueRange = -120f..120f,
                                stepSize = 2f,
                                label = "Horizontal Offset (X)",
                                formattedValue = "${prefs.horizontalOffsetDp.toInt()} DP"
                            )
                            NothingStepperSlider(
                                value = prefs.topMarginDp,
                                onValueChange = { repository.updateLiveCalibration(topMarginDp = it) },
                                valueRange = 0f..40f,
                                stepSize = 1f,
                                label = "Top Margin (Y)",
                                formattedValue = "${prefs.topMarginDp.toInt()} DP"
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Camera Cutout Notch",
                                        color = NothingPrimaryText,
                                        fontSize = 12.5.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Carve U-notch clearance in expanded cards",
                                        color = NothingMutedText,
                                        fontSize = 10.5.sp
                                    )
                                }
                                NothingSwitch(
                                    checked = prefs.isCameraCutoutEnabled,
                                    onCheckedChange = { enabled ->
                                        coroutineScope.launch {
                                            repository.updateCameraCutoutEnabled(enabled)
                                        }
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    IslandCard {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Unrestricted Battery",
                                    color = NothingPrimaryText,
                                    fontSize = 14.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (isIgnoringBatteryOptimizations) "UNRESTRICTED" else "RECOMMENDED",
                                    color = if (isIgnoringBatteryOptimizations) NothingSuccess else NothingPrimaryAccent,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "Exempt Dot Island from Android / OEM battery optimizations so the background overlay service stays alive reliably.",
                                color = NothingSecondaryText,
                                fontSize = 12.sp,
                                lineHeight = 17.sp
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(NothingElevatedSurface)
                                    .border(
                                        1.dp,
                                        if (isIgnoringBatteryOptimizations) NothingSuccess.copy(alpha = 0.5f) else NothingDivider,
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable { onRequestBatteryOptimization() }
                                    .padding(vertical = 11.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (isIgnoringBatteryOptimizations) "BATTERY UNRESTRICTED ✓" else "ALLOW UNRESTRICTED BATTERY",
                                    color = if (isIgnoringBatteryOptimizations) NothingSuccess else NothingPrimaryText,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Bottom Navigation Bar for Wizard Steps
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (stepIndex > 0) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(NothingElevatedSurface)
                        .border(1.dp, NothingDivider, RoundedCornerShape(14.dp))
                        .clickable { stepIndex -= 1 }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "← BACK",
                        color = NothingPrimaryText,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1.8f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(NothingPrimaryAccent)
                    .clickable {
                        if (stepIndex < 2) {
                            stepIndex += 1
                        } else {
                            coroutineScope.launch {
                                repository.persistCurrentCalibration()
                            }
                            onFinish()
                        }
                    }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (stepIndex < 2) "NEXT STEP →" else "FINISH & LAUNCH",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
