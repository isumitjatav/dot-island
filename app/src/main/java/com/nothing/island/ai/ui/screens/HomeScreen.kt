package com.nothing.island.ai.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nothing.island.ai.CutoutDetector
import com.nothing.island.ai.CutoutGeometry
import com.nothing.island.ai.data.IslandPreferencesRepository
import com.nothing.island.ai.navigation.IslandDestination
import com.nothing.island.ai.ui.components.DotMatrixGlyph
import com.nothing.island.ai.ui.components.DotMatrixPatterns
import com.nothing.island.ai.ui.components.IslandCard
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
import com.nothing.island.ai.ui.theme.NothingTokens
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

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
    onOpenSettings: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val preferencesRepository = remember { IslandPreferencesRepository.getInstance(context) }
    val prefs by preferencesRepository.livePreferences.collectAsState()
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current

    var showNotifDisclosure by remember { mutableStateOf(false) }
    var isFinePositionExpanded by remember { mutableStateOf(false) }

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
        // App Top Bar with Brand and Gear Button
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
                    text = "SMART CAMERA ISLAND",
                    color = NothingMutedText,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
            }

            // Gear Icon Button opening Settings Sheet
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(NothingElevatedSurface)
                    .border(1.dp, NothingDivider, CircleShape)
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        if (onOpenSettings != null) {
                            onOpenSettings()
                        } else {
                            onNavigateToDestination(IslandDestination.Settings)
                        }
                    },
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

        Spacer(modifier = Modifier.height(12.dp))

        // 1. Tactile Master Dot Island ON/OFF Hero Card
        IslandCard {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(if (isServiceRunning && hasOverlayPermission) NothingSuccess else NothingMutedText)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isServiceRunning && hasOverlayPermission) "Dot Island Active" else "Dot Island Off",
                            color = NothingPrimaryText,
                            fontSize = 16.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (!hasOverlayPermission) {
                            "Permission needed to display over apps"
                        } else if (isServiceRunning) {
                            "Floating pill active around front camera"
                        } else {
                            "Turn on to show floating pill"
                        },
                        color = if (!hasOverlayPermission) NothingPrimaryAccent else NothingSecondaryText,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                if (!hasOverlayPermission) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(NothingPrimaryAccent)
                            .clickable { onRequestPermission() }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "GRANT",
                            color = Color.White,
                            fontSize = 11.5.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    NothingSwitch(
                        checked = isServiceRunning,
                        onCheckedChange = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onToggleService()
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. Interactive Phone-Bezel Stage with Direct Touch Drag
        SectionHeader(
            title = "CAMERA CUTOUT ALIGNMENT",
            badgeText = "TOUCH & DRAG",
            badgeColor = NothingPrimaryAccent
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Phone-Bezel Stage Container
        var liveOffsetX by remember(prefs.horizontalOffsetDp) { mutableFloatStateOf(prefs.horizontalOffsetDp) }
        var liveOffsetY by remember(prefs.topMarginDp) { mutableFloatStateOf(prefs.topMarginDp) }

        val animatedOffsetX by animateFloatAsState(
            targetValue = liveOffsetX,
            animationSpec = spring(dampingRatio = 0.72f, stiffness = 380f),
            label = "bezel_offset_x"
        )
        val animatedOffsetY by animateFloatAsState(
            targetValue = liveOffsetY,
            animationSpec = spring(dampingRatio = 0.72f, stiffness = 380f),
            label = "bezel_offset_y"
        )

        val stageShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(stageShape)
                .background(Color(0xFF0D0D0D))
                .border(1.dp, NothingDivider, stageShape)
                .padding(12.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            // Simulated Camera Cutout Dot
            Box(
                modifier = Modifier
                    .padding(top = 10.dp)
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF030303))
                    .border(1.dp, Color(0x33FFFFFF), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                // Subtle camera lens glass reflection
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(Color(0x884070A0))
                )
            }

            // Interactive Draggable Pill
            val pillWidthDp = prefs.compactWidthDp.dp
            val pillHeightDp = prefs.compactHeightDp.dp
            val pillCornerRadiusDp = prefs.cornerRadiusDp.dp

            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            x = with(density) { animatedOffsetX.dp.roundToPx() },
                            y = with(density) { animatedOffsetY.dp.roundToPx() }
                        )
                    }
                    .size(width = pillWidthDp, height = pillHeightDp)
                    .clip(RoundedCornerShape(pillCornerRadiusDp))
                    .background(Color(0xFF000000))
                    .border(1.dp, Color(0x40FFFFFF), RoundedCornerShape(pillCornerRadiusDp))
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragEnd = {
                                coroutineScope.launch {
                                    preferencesRepository.updateLiveCalibration(
                                        horizontalOffsetDp = liveOffsetX,
                                        topMarginDp = liveOffsetY
                                    )
                                    preferencesRepository.persistCurrentCalibration()
                                }
                            }
                        ) { change, dragAmount ->
                            change.consume()
                            val dx = with(density) { dragAmount.x.toDp().value }
                            val dy = with(density) { dragAmount.y.toDp().value }
                            liveOffsetX = (liveOffsetX + dx).coerceIn(-65f, 65f)
                            liveOffsetY = (liveOffsetY + dy).coerceIn(0f, 36f)
                            preferencesRepository.updateLiveCalibration(
                                horizontalOffsetDp = liveOffsetX,
                                topMarginDp = liveOffsetY
                            )
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                    }
                    .padding(horizontal = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(NothingPrimaryAccent)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "DRAG TO ALIGN",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                }
            }

            // Bottom coordinates readout and instructional hint
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "X: ${if (liveOffsetX >= 0) "+${liveOffsetX.toInt()}" else "${liveOffsetX.toInt()}"} DP  •  Y: ${liveOffsetY.toInt()} DP",
                    color = NothingPrimaryAccent,
                    fontSize = 10.5.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Drag the pill with your finger to center on your punch-hole",
                    color = NothingMutedText,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 3. Compact / Standard / Wide Size Pills & One-Tap Reset Button
        IslandCard {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // Size Pills
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Pill Width",
                        color = NothingPrimaryText,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    val sizeLabel = when {
                        prefs.compactWidthDp < 114f -> "Compact (104dp)"
                        prefs.compactWidthDp > 140f -> "Wide (152dp)"
                        else -> "Standard (124dp)"
                    }
                    Text(
                        text = sizeLabel,
                        color = NothingPrimaryAccent,
                        fontSize = 11.5.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val sizes = listOf(
                        "Compact" to 104f,
                        "Standard" to 124f,
                        "Wide" to 152f
                    )
                    sizes.forEach { (label, widthVal) ->
                        val isSelected = when (label) {
                            "Compact" -> prefs.compactWidthDp < 114f
                            "Wide" -> prefs.compactWidthDp > 140f
                            else -> prefs.compactWidthDp in 114f..140f
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) NothingElevatedSurface else Color.Transparent)
                                .border(
                                    1.dp,
                                    if (isSelected) NothingPrimaryAccent else NothingDivider,
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    preferencesRepository.updateLiveCalibration(compactWidthDp = widthVal)
                                    coroutineScope.launch {
                                        preferencesRepository.persistCurrentCalibration()
                                    }
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label.uppercase(),
                                color = if (isSelected) NothingPrimaryText else NothingSecondaryText,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }

                // Collapsible Fine Position Nudge Row
                val finePosRotation by animateFloatAsState(
                    targetValue = if (isFinePositionExpanded) 180f else 0f,
                    animationSpec = tween(180),
                    label = "finePosChevron"
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            isFinePositionExpanded = !isFinePositionExpanded
                        }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Fine Position (Pixel Nudge)",
                            color = NothingPrimaryText,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "X: ${if (liveOffsetX >= 0) "+${liveOffsetX.toInt()}" else "${liveOffsetX.toInt()}"} DP  •  Y: ${liveOffsetY.toInt()} DP",
                            color = NothingPrimaryAccent,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(NothingElevatedSurface)
                            .border(1.dp, NothingDivider, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "▼",
                            color = NothingPrimaryText,
                            fontSize = 10.sp,
                            modifier = Modifier.rotate(finePosRotation)
                        )
                    }
                }

                AnimatedVisibility(
                    visible = isFinePositionExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        // Horizontal X Nudge Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(NothingElevatedSurface)
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Horizontal (X)",
                                color = NothingSecondaryText,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                NudgeButton(label = "-1") {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    liveOffsetX = (liveOffsetX - 1f).coerceIn(-65f, 65f)
                                    preferencesRepository.updateLiveCalibration(
                                        horizontalOffsetDp = liveOffsetX,
                                        topMarginDp = liveOffsetY
                                    )
                                    coroutineScope.launch { preferencesRepository.persistCurrentCalibration() }
                                }
                                Text(
                                    text = "${if (liveOffsetX >= 0) "+${liveOffsetX.toInt()}" else "${liveOffsetX.toInt()}"} dp",
                                    color = NothingPrimaryText,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.width(48.dp),
                                    textAlign = TextAlign.Center
                                )
                                NudgeButton(label = "+1") {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    liveOffsetX = (liveOffsetX + 1f).coerceIn(-65f, 65f)
                                    preferencesRepository.updateLiveCalibration(
                                        horizontalOffsetDp = liveOffsetX,
                                        topMarginDp = liveOffsetY
                                    )
                                    coroutineScope.launch { preferencesRepository.persistCurrentCalibration() }
                                }
                            }
                        }

                        // Vertical Y Nudge Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(NothingElevatedSurface)
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Vertical (Y)",
                                color = NothingSecondaryText,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                NudgeButton(label = "-1") {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    liveOffsetY = (liveOffsetY - 1f).coerceIn(0f, 36f)
                                    preferencesRepository.updateLiveCalibration(
                                        horizontalOffsetDp = liveOffsetX,
                                        topMarginDp = liveOffsetY
                                    )
                                    coroutineScope.launch { preferencesRepository.persistCurrentCalibration() }
                                }
                                Text(
                                    text = "${liveOffsetY.toInt()} dp",
                                    color = NothingPrimaryText,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.width(48.dp),
                                    textAlign = TextAlign.Center
                                )
                                NudgeButton(label = "+1") {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    liveOffsetY = (liveOffsetY + 1f).coerceIn(0f, 36f)
                                    preferencesRepository.updateLiveCalibration(
                                        horizontalOffsetDp = liveOffsetX,
                                        topMarginDp = liveOffsetY
                                    )
                                    coroutineScope.launch { preferencesRepository.persistCurrentCalibration() }
                                }
                            }
                        }
                    }
                }

                // One-tap Reset Button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(NothingElevatedSurface)
                        .border(1.dp, NothingDivider, RoundedCornerShape(10.dp))
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            liveOffsetX = 0f
                            liveOffsetY = 10f
                            coroutineScope.launch {
                                preferencesRepository.updateLiveCalibration(
                                    compactWidthDp = CutoutDetector.DEFAULT_COMPACT_WIDTH_DP,
                                    compactHeightDp = CutoutDetector.DEFAULT_COMPACT_VISIBLE_HEIGHT_DP,
                                    cornerRadiusDp = CutoutDetector.DEFAULT_COMPACT_CORNER_RADIUS_DP,
                                    topMarginDp = CutoutDetector.DEFAULT_TOP_MARGIN_DP,
                                    horizontalOffsetDp = 0f
                                )
                                preferencesRepository.persistCurrentCalibration()
                            }
                        }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "↺ RESET TO CAMERA CUTOUT",
                            color = NothingPrimaryText,
                            fontSize = 11.5.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 4. Connected Features / Overview
        SectionHeader(title = "CONNECTED FEATURES")

        Spacer(modifier = Modifier.height(6.dp))

        IslandCard {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ModuleStatusRow(
                    "Calls & Voice",
                    "Active",
                    NothingSuccess
                )
                ModuleStatusRow(
                    "Notifications & Messages",
                    if (hasNotificationPermission) "Connected" else "Tap to enable",
                    if (hasNotificationPermission) NothingSuccess else NothingPrimaryAccent
                )
                ModuleStatusRow(
                    "Music & Media",
                    if (hasNotificationPermission) "Connected" else "Tap to enable",
                    if (hasNotificationPermission) NothingSuccess else NothingPrimaryAccent
                )
                ModuleStatusRow(
                    "Battery & Charging",
                    "Active",
                    NothingSuccess
                )
                val mascotSprite = com.nothing.island.ai.ui.components.mascotByName(prefs.mascotSpriteName)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToDestination(IslandDestination.ThemeStudio) },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Pixel Companion", color = NothingSecondaryText, fontSize = 12.sp, fontFamily = FontFamily.SansSerif)
                    Text(
                        text = if (prefs.mascotEnabled) "${mascotSprite.displayName} • Active" else "Off • Customize",
                        color = if (prefs.mascotEnabled) NothingSuccess else NothingMutedText,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
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
private fun NudgeButton(
    label: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(width = 34.dp, height = 30.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(NothingPrimaryBackground)
            .border(1.dp, NothingDivider, RoundedCornerShape(6.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = NothingPrimaryText,
            fontSize = 11.5.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )
    }
}
