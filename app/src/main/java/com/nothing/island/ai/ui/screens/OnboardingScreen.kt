package com.nothing.island.ai.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nothing.island.ai.ui.components.IslandCard
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

/** Returns a human-readable onboarding step title. Pure and unit-testable. */
internal fun onboardingStepTitle(stepIndex: Int): String = when (stepIndex.coerceIn(0, 1)) {
    0 -> "FLOATING PILL DISPLAY"
    else -> "NOTIFICATION & MUSIC ACCESS"
}

/**
 * 15-Second 2-Step Onboarding Flow:
 * - Step 1: Clean dark welcome screen with an animated pill preview and a single Get Started button.
 * - Step 2: Only 2 plain-English permission cards (Floating Pill Display and Notification & Music Access)
 *   with live status checkmarks that auto-advance to Home as soon as both are granted.
 */
@Composable
fun OnboardingScreen(
    hasOverlayPermission: Boolean,
    hasNotificationListenerPermission: Boolean,
    hasAccessibilityPermission: Boolean = false,
    isIgnoringBatteryOptimizations: Boolean = false,
    onRequestOverlayPermission: () -> Unit,
    onRequestNotificationAccess: () -> Unit,
    onRequestAccessibilityAccess: () -> Unit = {},
    onRequestBatteryOptimization: () -> Unit = {},
    onSkip: () -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 0 = Step 1 (Welcome Screen), 1 = Step 2 (Permissions)
    var currentStep by remember { mutableIntStateOf(0) }
    var activeDisclosure by remember { mutableStateOf<ProminentDisclosureType?>(null) }
    val scrollState = rememberScrollState()
    val haptic = LocalHapticFeedback.current

    // Auto-advance to Home as soon as both permissions are granted when on Step 2
    LaunchedEffect(hasOverlayPermission, hasNotificationListenerPermission, currentStep) {
        if (currentStep == 1 && hasOverlayPermission && hasNotificationListenerPermission) {
            onFinish()
        }
    }

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

    AnimatedContent(
        targetState = currentStep,
        transitionSpec = {
            fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(180))
        },
        label = "OnboardingStageTransition",
        modifier = modifier
            .fillMaxSize()
            .background(NothingPrimaryBackground)
    ) { step ->
        if (step == 0) {
            // STEP 1: CLEAN DARK WELCOME SCREEN WITH ANIMATED PILL PREVIEW
            WelcomeStep(
                onGetStarted = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    currentStep = 1
                },
                onSkip = onSkip
            )
        } else {
            // STEP 2: ONLY 2 PLAIN-ENGLISH PERMISSION CARDS
            PermissionsStep(
                hasOverlayPermission = hasOverlayPermission,
                hasNotificationListenerPermission = hasNotificationListenerPermission,
                onRequestOverlay = onRequestOverlayPermission,
                onRequestNotification = {
                    activeDisclosure = ProminentDisclosureType.NOTIFICATION_LISTENER
                },
                onFinish = onFinish,
                onBack = { currentStep = 0 },
                scrollState = scrollState
            )
        }
    }
}

/**
 * Step 1: Clean dark welcome screen with an animated pill preview and a single Get Started button.
 */
@Composable
private fun WelcomeStep(
    onGetStarted: () -> Unit,
    onSkip: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "welcomePill")
    val animatedWidth by infiniteTransition.animateFloat(
        initialValue = 130f,
        targetValue = 190f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pillWidth"
    )
    val waveOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "waveOffset"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Bar with Skip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onSkip() }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "SKIP",
                    color = NothingMutedText,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }

        // Center: Animated Pill Showcase & Hero Content
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Simulated Camera Cutout with expanding interactive pill
            Box(
                modifier = Modifier
                    .size(width = 240.dp, height = 110.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF0F0F0F))
                    .border(1.dp, NothingDivider, RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                // Punch-hole camera dot
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF050505))
                        .border(1.dp, Color(0x33FFFFFF), CircleShape)
                )

                // Animated Fluid Pill
                Box(
                    modifier = Modifier
                        .size(width = animatedWidth.dp, height = 36.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFF000000))
                        .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(18.dp))
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left active indicator
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(NothingPrimaryAccent)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "DOT ISLAND",
                                color = Color.White,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }

                        // Right animated equalizer bars
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val barHeights = listOf(10.dp, 16.dp, 8.dp, 14.dp)
                            barHeights.forEachIndexed { idx, baseHeight ->
                                val factor = if ((idx + (waveOffset * 4).toInt()) % 2 == 0) 1f else 0.5f
                                Box(
                                    modifier = Modifier
                                        .width(2.5.dp)
                                        .height(baseHeight * factor)
                                        .clip(CircleShape)
                                        .background(NothingPrimaryAccent)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Hero Brand Title & Subtitle
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(NothingPrimaryAccent)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "DOT ISLAND",
                    color = NothingPrimaryText,
                    fontSize = 24.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Smart Camera Cutout for Android",
                color = NothingSecondaryText,
                fontSize = 14.sp,
                fontFamily = FontFamily.SansSerif,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Value Props
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                FeatureChip("100% Offline • Zero Analytics")
                FeatureChip("Apple-Grade Spring Physics")
                FeatureChip("Custom Themes & Pixel Mascots")
            }
        }

        // Bottom: Single Get Started Button
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(NothingPrimaryAccent)
                    .clickable { onGetStarted() }
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "GET STARTED →",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Setup takes ~15 seconds",
                color = NothingMutedText,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

/**
 * Step 2: Only 2 plain-English permission cards with live status checkmarks that auto-advance to Home.
 */
@Composable
private fun PermissionsStep(
    hasOverlayPermission: Boolean,
    hasNotificationListenerPermission: Boolean,
    onRequestOverlay: () -> Unit,
    onRequestNotification: () -> Unit,
    onFinish: () -> Unit,
    onBack: () -> Unit,
    scrollState: androidx.compose.foundation.ScrollState
) {
    val haptic = LocalHapticFeedback.current
    val bothGranted = hasOverlayPermission && hasNotificationListenerPermission

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(NothingElevatedSurface)
                        .border(1.dp, NothingDivider, RoundedCornerShape(8.dp))
                        .clickable { onBack() }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "← BACK",
                        color = NothingPrimaryText,
                        fontSize = 10.5.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "PERMISSIONS SETUP",
                    color = NothingPrimaryText,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(NothingElevatedSurface)
                        .border(1.dp, NothingDivider, RoundedCornerShape(8.dp))
                        .clickable { onFinish() }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "SKIP",
                        color = NothingMutedText,
                        fontSize = 10.5.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Two essential permissions for your dynamic island:",
                color = NothingSecondaryText,
                fontSize = 13.sp,
                fontFamily = FontFamily.SansSerif
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Card 1: Floating Pill Display
            SectionHeader(
                title = "1 • FLOATING PILL DISPLAY",
                badgeText = if (hasOverlayPermission) "GRANTED ✓" else "REQUIRED",
                badgeColor = if (hasOverlayPermission) NothingSuccess else NothingPrimaryAccent
            )
            Spacer(modifier = Modifier.height(6.dp))

            IslandCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Floating Pill Display",
                            color = NothingPrimaryText,
                            fontSize = 15.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        if (hasOverlayPermission) {
                            Text(
                                text = "✓ GRANTED",
                                color = NothingSuccess,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Text(
                        text = "Anchors the interactive pill around your front camera cutout so music, incoming calls, earbuds, and alerts stay accessible over any app.",
                        color = NothingSecondaryText,
                        fontSize = 12.5.sp,
                        lineHeight = 18.sp
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (hasOverlayPermission) NothingElevatedSurface else NothingPrimaryAccent)
                            .border(
                                1.dp,
                                if (hasOverlayPermission) NothingSuccess.copy(alpha = 0.5f) else NothingPrimaryAccent,
                                RoundedCornerShape(10.dp)
                            )
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                if (!hasOverlayPermission) {
                                    onRequestOverlay()
                                }
                            }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (hasOverlayPermission) "✓ DISPLAY ACCESS ALLOWED" else "GRANT DISPLAY PERMISSION",
                            color = if (hasOverlayPermission) NothingSuccess else Color.White,
                            fontSize = 11.5.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Card 2: Notification & Music Access
            SectionHeader(
                title = "2 • NOTIFICATION & MUSIC ACCESS",
                badgeText = if (hasNotificationListenerPermission) "GRANTED ✓" else "REQUIRED",
                badgeColor = if (hasNotificationListenerPermission) NothingSuccess else NothingPrimaryAccent
            )
            Spacer(modifier = Modifier.height(6.dp))

            IslandCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Notification & Music Access",
                            color = NothingPrimaryText,
                            fontSize = 15.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        if (hasNotificationListenerPermission) {
                            Text(
                                text = "✓ GRANTED",
                                color = NothingSuccess,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Text(
                        text = "Allows Dot Island to display incoming message alerts and now-playing songs directly in the floating pill. All processing is 100% on-device with zero internet transmission.",
                        color = NothingSecondaryText,
                        fontSize = 12.5.sp,
                        lineHeight = 18.sp
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
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                if (!hasNotificationListenerPermission) {
                                    onRequestNotification()
                                }
                            }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (hasNotificationListenerPermission) "✓ NOTIFICATION ACCESS ALLOWED" else "GRANT NOTIFICATION ACCESS",
                            color = if (hasNotificationListenerPermission) NothingSuccess else Color.White,
                            fontSize = 11.5.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Bottom Continue button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(if (bothGranted) NothingPrimaryAccent else NothingElevatedSurface)
                .border(
                    1.dp,
                    if (bothGranted) NothingPrimaryAccent else NothingDivider,
                    RoundedCornerShape(14.dp)
                )
                .clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onFinish()
                }
                .padding(vertical = 15.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (bothGranted) "CONTINUE TO HOME →" else "CONTINUE TO HOME",
                color = if (bothGranted) Color.White else NothingSecondaryText,
                fontSize = 12.5.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp
            )
        }
    }
}

@Composable
private fun FeatureChip(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(NothingElevatedSurface)
            .border(1.dp, NothingDivider, RoundedCornerShape(6.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            color = NothingMutedText,
            fontSize = 10.5.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}
