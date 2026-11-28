package com.nothing.island.ai.ui.screens

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
import androidx.compose.foundation.horizontalScroll
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
import com.nothing.island.ai.ui.components.IslandCard
import com.nothing.island.ai.ui.components.MascotPillPreview
import com.nothing.island.ai.ui.components.MascotReactionState
import com.nothing.island.ai.ui.components.MascotSprites
import com.nothing.island.ai.ui.components.NothingStepperSlider
import com.nothing.island.ai.ui.components.NothingSwitch
import com.nothing.island.ai.ui.components.SectionHeader
import com.nothing.island.ai.ui.theme.IslandSignatureTheme
import com.nothing.island.ai.ui.theme.NothingDivider
import com.nothing.island.ai.ui.theme.NothingElevatedSurface
import com.nothing.island.ai.ui.theme.NothingMutedText
import com.nothing.island.ai.ui.theme.NothingPrimaryAccent
import com.nothing.island.ai.ui.theme.NothingPrimaryBackground
import com.nothing.island.ai.ui.theme.NothingPrimaryText
import com.nothing.island.ai.ui.theme.NothingSecondaryText
import com.nothing.island.ai.ui.theme.NothingSuccess
import kotlinx.coroutines.launch

/**
 * Visual Style Screen
 *
 * Implements:
 * 1. 2-column visual grid of live theme preview cards (Frosted Glass, AMOLED Pitch, Nothing Dot, etc.)
 * 2. 2-column tap-to-equip Pixel Mascot cards showing each mascot perched on a mini pill
 * 3. Clean accent color swatches
 */
@Composable
fun StyleScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repository = remember { IslandPreferencesRepository.getInstance(context) }
    val prefs by repository.livePreferences.collectAsState()
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    val currentPreset = prefs.presetName.ifBlank { "Nothing Classic" }
    val currentMascotName = prefs.mascotSpriteName.ifBlank { MascotSprites.DOTCAT.name }
    val currentAccentHex = prefs.accentColorHex
    var isFineTuneExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NothingPrimaryBackground)
            .verticalScroll(scrollState)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Header
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
                        text = "STYLE & THEMES",
                        color = NothingPrimaryText,
                        fontSize = 18.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    )
                }
                Text(
                    text = "VISUAL IDENTITY & MASCOTS",
                    color = NothingMutedText,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 1. ACCENT COLOR SWATCHES
        SectionHeader(
            title = "ACCENT COLOR",
            badgeText = "SURGICAL",
            badgeColor = NothingPrimaryAccent
        )
        Spacer(modifier = Modifier.height(6.dp))

        IslandCard {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Select alert highlights and active glyph color:",
                    color = NothingSecondaryText,
                    fontSize = 12.sp
                )

                val accentSwatches = listOf(
                    "Nothing Red" to 0xFFD71921L,
                    "Glyph White" to 0xFFFFFFFFL,
                    "Cyber Cyan" to 0xFF00E5FFL,
                    "Electric Amber" to 0xFFFF6B00L,
                    "Acid Green" to 0xFF30D158L,
                    "Electric Violet" to 0xFFB388FFL,
                    "Solar Yellow" to 0xFFFFD60AL
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    accentSwatches.forEach { (name, hex) ->
                        val isSelected = currentAccentHex == hex
                        val swatchColor = Color(hex)

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    coroutineScope.launch {
                                        repository.updateLiveCalibration(accentColorHex = hex)
                                        repository.persistCurrentCalibration()
                                    }
                                }
                                .padding(vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(swatchColor)
                                    .border(
                                        width = if (isSelected) 3.dp else 1.dp,
                                        color = if (isSelected) Color.White else Color(0x40FFFFFF),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(if (hex == 0xFFFFFFFFL) Color.Black else Color.White)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = name.split(" ").last(),
                                color = if (isSelected) NothingPrimaryText else NothingMutedText,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // 2. 2-COLUMN VISUAL GRID OF LIVE THEME PREVIEW CARDS
        SectionHeader(
            title = "ISLAND THEMES",
            badgeText = "LIVE PREVIEWS",
            badgeColor = NothingPrimaryAccent
        )
        Spacer(modifier = Modifier.height(6.dp))

        val themes = listOf(
            ThemePresentation(
                key = "Glassmorphic Frosted",
                displayName = "Frosted Glass",
                subtitle = "Translucent acrylic with specular rim",
                bgPreview = Color(0xD6222222),
                borderPreview = Color(0x80FFFFFF),
                accentPreview = Color.White
            ),
            ThemePresentation(
                key = "High-Contrast Inverted",
                displayName = "AMOLED Pitch",
                subtitle = "Pure pitch canvas & high-contrast punch",
                bgPreview = Color(0xFF000000),
                borderPreview = Color(0xFF2B2B30),
                accentPreview = Color(0xFFD71921)
            ),
            ThemePresentation(
                key = "Nothing Classic",
                displayName = "Nothing Dot",
                subtitle = "Monochrome dot matrix with surgical red",
                bgPreview = Color(0xFF000000),
                borderPreview = Color(0xFF333333),
                accentPreview = Color(0xFFD71921),
                isDotMatrix = true
            ),
            ThemePresentation(
                key = "Glyph Arc",
                displayName = "Glyph Arc",
                subtitle = "Dual illuminated arcs with LED meters",
                bgPreview = Color(0xFF050505),
                borderPreview = Color(0xFF444444),
                accentPreview = Color.White
            ),
            ThemePresentation(
                key = "Tape Deck",
                displayName = "Tape Deck",
                subtitle = "Charcoal finish with analog cassette vibe",
                bgPreview = Color(0xFF1E1E1E),
                borderPreview = Color(0xFF444444),
                accentPreview = Color(0xFFD71921)
            )
        )

        // 2-Column Grid of Theme Cards
        for (i in themes.indices step 2) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ThemeCard(
                    theme = themes[i],
                    isSelected = currentPreset.equals(themes[i].key, ignoreCase = true) ||
                            (themes[i].displayName == "Nothing Dot" && currentPreset == "Nothing Classic") ||
                            (themes[i].displayName == "Frosted Glass" && currentPreset.contains("Frosted", ignoreCase = true)),
                    onSelect = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        coroutineScope.launch {
                            val sigTheme = IslandSignatureTheme.resolve(themes[i].key)
                            repository.updatePillTheme(
                                backgroundHex = sigTheme.pillBackgroundColorHex,
                                accentHex = sigTheme.accentColorHex,
                                presetName = sigTheme.displayName
                            )
                        }
                    },
                    modifier = Modifier.weight(1f)
                )

                if (i + 1 < themes.size) {
                    ThemeCard(
                        theme = themes[i + 1],
                        isSelected = currentPreset.equals(themes[i + 1].key, ignoreCase = true) ||
                                (themes[i + 1].displayName == "Nothing Dot" && currentPreset == "Nothing Classic") ||
                                (themes[i + 1].displayName == "Frosted Glass" && currentPreset.contains("Frosted", ignoreCase = true)),
                        onSelect = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            coroutineScope.launch {
                                val sigTheme = IslandSignatureTheme.resolve(themes[i + 1].key)
                                repository.updatePillTheme(
                                    backgroundHex = sigTheme.pillBackgroundColorHex,
                                    accentHex = sigTheme.accentColorHex,
                                    presetName = sigTheme.displayName
                                )
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        Spacer(modifier = Modifier.height(18.dp))

        // 3. 2-COLUMN TAP-TO-EQUIP PIXEL MASCOT CARDS
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SectionHeader(
                title = "PIXEL MASCOTS",
                badgeText = if (prefs.mascotEnabled) "ON" else "OFF",
                badgeColor = if (prefs.mascotEnabled) NothingSuccess else NothingMutedText
            )
            NothingSwitch(
                checked = prefs.mascotEnabled,
                onCheckedChange = { enabled ->
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    coroutineScope.launch {
                        repository.updateMascot(enabled, currentMascotName)
                    }
                }
            )
        }

        IslandCard {
            Text(
                text = "Tap any companion to equip. Mascots perch on top of the capsule and react to music, battery charging, and alerts.",
                color = NothingSecondaryText,
                fontSize = 12.sp,
                lineHeight = 17.sp
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        val mascots = MascotSprites.ALL
        for (i in mascots.indices step 2) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MascotTile(
                    sprite = mascots[i],
                    isEquipped = prefs.mascotEnabled && currentMascotName.equals(mascots[i].name, ignoreCase = true),
                    onEquip = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        coroutineScope.launch {
                            repository.updateMascot(true, mascots[i].name)
                        }
                    },
                    modifier = Modifier.weight(1f)
                )

                if (i + 1 < mascots.size) {
                    MascotTile(
                        sprite = mascots[i + 1],
                        isEquipped = prefs.mascotEnabled && currentMascotName.equals(mascots[i + 1].name, ignoreCase = true),
                        onEquip = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            coroutineScope.launch {
                                repository.updateMascot(true, mascots[i + 1].name)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        Spacer(modifier = Modifier.height(18.dp))

        // 4. FINE-TUNE SHAPE & MASCOT EXPANDABLE CARD
        SectionHeader(
            title = "FINE-TUNE SHAPE & MOTION",
            badgeText = "CUSTOM",
            badgeColor = NothingPrimaryAccent
        )
        Spacer(modifier = Modifier.height(6.dp))

        IslandCard {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                val fineTuneRotation by animateFloatAsState(
                    targetValue = if (isFineTuneExpanded) 180f else 0f,
                    animationSpec = tween(180),
                    label = "fineTuneChevron"
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            isFineTuneExpanded = !isFineTuneExpanded
                        }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Fine-Tune Shape & Mascot",
                            color = NothingPrimaryText,
                            fontSize = 14.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Pill height, corner radius rounding, cutout avoidance & motion",
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
                            modifier = Modifier.rotate(fineTuneRotation)
                        )
                    }
                }

                AnimatedVisibility(
                    visible = isFineTuneExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        // 1. Pill Height Stepper
                        NothingStepperSlider(
                            value = prefs.compactHeightDp,
                            onValueChange = { heightVal ->
                                repository.updateLiveCalibration(compactHeightDp = heightVal)
                                coroutineScope.launch {
                                    repository.persistCurrentCalibration()
                                }
                            },
                            valueRange = 28f..48f,
                            stepSize = 1f,
                            label = "Capsule Height",
                            formattedValue = "${prefs.compactHeightDp.toInt()} DP"
                        )

                        // 2. Corner Radius Stepper
                        NothingStepperSlider(
                            value = prefs.cornerRadiusDp,
                            onValueChange = { radiusVal ->
                                repository.updateLiveCalibration(cornerRadiusDp = radiusVal)
                                coroutineScope.launch {
                                    repository.persistCurrentCalibration()
                                }
                            },
                            valueRange = 10f..26f,
                            stepSize = 1f,
                            label = "Corner Rounding Radius",
                            formattedValue = "${prefs.cornerRadiusDp.toInt()} DP"
                        )

                        // 3. Avoid Camera Cutout
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    val newCutout = !prefs.isCameraCutoutEnabled
                                    repository.updateLiveCalibration(isCameraCutoutEnabled = newCutout)
                                    coroutineScope.launch {
                                        repository.updateCameraCutoutEnabled(newCutout)
                                        repository.persistCurrentCalibration()
                                    }
                                }
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Avoid Center Camera Lens",
                                    color = NothingPrimaryText,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Keeps pixel companion patrolling on the right shoulder without covering front camera glass.",
                                    color = NothingSecondaryText,
                                    fontSize = 11.5.sp,
                                    lineHeight = 15.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            NothingSwitch(
                                checked = prefs.isCameraCutoutEnabled,
                                onCheckedChange = { isChecked ->
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    repository.updateLiveCalibration(isCameraCutoutEnabled = isChecked)
                                    coroutineScope.launch {
                                        repository.updateCameraCutoutEnabled(isChecked)
                                        repository.persistCurrentCalibration()
                                    }
                                }
                            )
                        }

                        // 4. Reduced Motion Animation Toggle
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    coroutineScope.launch {
                                        repository.updateReducedMotion(!prefs.reducedMotionEnabled)
                                    }
                                }
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Reduced Motion Animations",
                                    color = NothingPrimaryText,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Replaces spring physics with instant mechanical fades to maximize battery life.",
                                    color = NothingSecondaryText,
                                    fontSize = 11.5.sp,
                                    lineHeight = 15.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            NothingSwitch(
                                checked = prefs.reducedMotionEnabled,
                                onCheckedChange = { isChecked ->
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    coroutineScope.launch {
                                        repository.updateReducedMotion(isChecked)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))
    }
}

private data class ThemePresentation(
    val key: String,
    val displayName: String,
    val subtitle: String,
    val bgPreview: Color,
    val borderPreview: Color,
    val accentPreview: Color,
    val isDotMatrix: Boolean = false
)

@Composable
private fun ThemeCard(
    theme: ThemePresentation,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) NothingPrimaryAccent else NothingDivider,
        animationSpec = tween(180),
        label = "themeBorder"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(NothingElevatedSurface)
            .border(width = if (isSelected) 1.5.dp else 1.dp, color = borderColor, shape = RoundedCornerShape(16.dp))
            .clickable { onSelect() }
            .padding(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Live Mini-Pill Preview Stage
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF070707)),
                contentAlignment = Alignment.Center
            ) {
                // Mini Pill Capsule
                Box(
                    modifier = Modifier
                        .size(width = 100.dp, height = 28.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(theme.bgPreview)
                        .border(1.dp, theme.borderPreview, RoundedCornerShape(14.dp))
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(theme.accentPreview)
                        )
                        Text(
                            text = if (theme.isDotMatrix) ":: ::" else "● ●",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.6f))
                        )
                    }
                }
            }

            // Theme details
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = theme.displayName,
                        color = NothingPrimaryText,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(NothingPrimaryAccent)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = theme.subtitle,
                    color = NothingSecondaryText,
                    fontSize = 10.5.sp,
                    lineHeight = 14.sp
                )
            }

            // Equip Badge
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) NothingPrimaryAccent else Color.Transparent)
                    .border(1.dp, if (isSelected) NothingPrimaryAccent else NothingDivider, RoundedCornerShape(8.dp))
                    .padding(vertical = 5.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isSelected) "EQUIPPED ✓" else "EQUIP",
                    color = if (isSelected) Color.White else NothingSecondaryText,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun MascotTile(
    sprite: MascotSprites.Sprite,
    isEquipped: Boolean,
    onEquip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor by animateColorAsState(
        targetValue = if (isEquipped) NothingPrimaryAccent else NothingDivider,
        animationSpec = tween(180),
        label = "mascotBorder"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(NothingElevatedSurface)
            .border(width = if (isEquipped) 1.5.dp else 1.dp, color = borderColor, shape = RoundedCornerShape(16.dp))
            .clickable { onEquip() }
            .padding(12.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Live Mini-Pill with perched Mascot Preview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(68.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF070707)),
                contentAlignment = Alignment.Center
            ) {
                MascotPillPreview(
                    modifier = Modifier.size(width = 110.dp, height = 50.dp),
                    sprite = sprite,
                    reactionState = MascotReactionState.IDLE,
                    renderContainer = true,
                    avoidCenterCutout = false
                )
            }

            // Mascot Name & Motion
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = sprite.displayName,
                    color = NothingPrimaryText,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${sprite.motion.name.lowercase()} cycle",
                    color = NothingMutedText,
                    fontSize = 9.5.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Equip Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isEquipped) NothingPrimaryAccent else Color.Transparent)
                    .border(1.dp, if (isEquipped) NothingPrimaryAccent else NothingDivider, RoundedCornerShape(8.dp))
                    .padding(vertical = 5.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isEquipped) "EQUIPPED ✓" else "EQUIP",
                    color = if (isEquipped) Color.White else NothingSecondaryText,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
