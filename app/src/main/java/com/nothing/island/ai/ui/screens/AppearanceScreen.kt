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
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import com.nothing.island.ai.data.IslandPreferencesRepository
import com.nothing.island.ai.ui.components.ChargingStyleStudio
import com.nothing.island.ai.ui.components.IslandCard
import com.nothing.island.ai.ui.components.IslandPreview
import com.nothing.island.ai.ui.components.MusicStyleStudio
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
fun AppearanceScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repository = remember { IslandPreferencesRepository.getInstance(context) }
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val prefs by repository.livePreferences.collectAsState()
    val musicPrefs by repository.liveMusicPreferences.collectAsState()
    val batteryPrefs by repository.liveBatteryPreferences.collectAsState()
    val isPreviewExpanded = remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NothingPrimaryBackground)
            .verticalScroll(scrollState)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Screen Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Text(
                text = "APPEARANCE",
                color = NothingPrimaryText,
                fontSize = 20.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
            )
            Text(
                text = "GEOMETRY CALIBRATION & THEME PRESETS",
                color = NothingMutedText,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Dynamic Live Preview Component
        IslandPreview(
            isExpanded = isPreviewExpanded.value,
            onToggleExpanded = { isPreviewExpanded.value = !isPreviewExpanded.value },
            pillWidthDp = prefs.compactWidthDp,
            pillHeightDp = prefs.compactHeightDp,
            cornerRadiusDp = prefs.cornerRadiusDp,
            topOffsetDp = prefs.topMarginDp,
            horizontalOffsetDp = prefs.horizontalOffsetDp,
            accentColor = Color(prefs.accentColorHex),
            isCameraCutoutEnabled = prefs.isCameraCutoutEnabled
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Preset Styles
        SectionHeader(title = "DESIGN PRESETS", badgeText = prefs.presetName)

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PresetPill(
                label = "Nothing Minimal",
                isSelected = prefs.presetName == "Nothing Minimal",
                modifier = Modifier.weight(1f),
                onClick = {
                    coroutineScope.launch {
                        repository.applyPreset(
                            presetName = "Nothing Minimal",
                            widthDp = 124f,
                            heightDp = 36f,
                            cornerRadiusDp = 18f,
                            accentColorHex = 0xFFD71920L
                        )
                    }
                }
            )
            PresetPill(
                label = "Futuristic Red",
                isSelected = prefs.presetName == "Futuristic Red",
                modifier = Modifier.weight(1f),
                onClick = {
                    coroutineScope.launch {
                        repository.applyPreset(
                            presetName = "Futuristic Red",
                            widthDp = 142f,
                            heightDp = 38f,
                            cornerRadiusDp = 19f,
                            accentColorHex = 0xFFFF2A32L
                        )
                    }
                }
            )
            PresetPill(
                label = "Stealth Black",
                isSelected = prefs.presetName == "Stealth Black",
                modifier = Modifier.weight(1f),
                onClick = {
                    coroutineScope.launch {
                        repository.applyPreset(
                            presetName = "Stealth Black",
                            widthDp = 118f,
                            heightDp = 34f,
                            cornerRadiusDp = 17f,
                            accentColorHex = 0xFFDDDDDDL
                        )
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Geometry Controls
        SectionHeader(title = "GEOMETRY & SIZING (DP)")

        Spacer(modifier = Modifier.height(6.dp))

        IslandCard {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                CustomSlider(
                    label = "Pill Width",
                    value = prefs.compactWidthDp,
                    range = 100f..180f,
                    unit = "dp",
                    onValueChange = { repository.updateLiveCalibration(compactWidthDp = it) },
                    onValueChangeFinished = {
                        coroutineScope.launch { repository.persistCurrentCalibration() }
                    }
                )

                CustomSlider(
                    label = "Pill Height",
                    value = prefs.compactHeightDp,
                    range = 28f..48f,
                    unit = "dp",
                    onValueChange = { repository.updateLiveCalibration(compactHeightDp = it) },
                    onValueChangeFinished = {
                        coroutineScope.launch { repository.persistCurrentCalibration() }
                    }
                )

                CustomSlider(
                    label = "Corner Radius",
                    value = prefs.cornerRadiusDp,
                    range = 10f..24f,
                    unit = "dp",
                    onValueChange = { repository.updateLiveCalibration(cornerRadiusDp = it) },
                    onValueChangeFinished = {
                        coroutineScope.launch { repository.persistCurrentCalibration() }
                    }
                )

                CustomSlider(
                    label = "Compact Top Offset (Y)",
                    value = prefs.topMarginDp,
                    range = 0f..24f,
                    unit = "dp",
                    onValueChange = { repository.updateLiveCalibration(topMarginDp = it) },
                    onValueChangeFinished = {
                        coroutineScope.launch { repository.persistCurrentCalibration() }
                    }
                )

                CustomSlider(
                    label = "Expanded Top Offset (Y)",
                    value = prefs.expandedTopMarginDp,
                    range = 0f..24f,
                    unit = "dp",
                    onValueChange = { repository.updateLiveCalibration(expandedTopMarginDp = it) },
                    onValueChangeFinished = {
                        coroutineScope.launch { repository.persistCurrentCalibration() }
                    }
                )

                CustomSlider(
                    label = "Horizontal Offset (X)",
                    value = prefs.horizontalOffsetDp,
                    range = -20f..20f,
                    unit = "dp",
                    onValueChange = { repository.updateLiveCalibration(horizontalOffsetDp = it) },
                    onValueChangeFinished = {
                        coroutineScope.launch { repository.persistCurrentCalibration() }
                    }
                )

                PreferenceToggleRow(
                    title = "Camera Cutout Notch",
                    subtitle = "Wrap expanded cards around front camera lens (reference style). When disabled, cards use smooth rounded corners.",
                    checked = prefs.isCameraCutoutEnabled,
                    onCheckedChange = { enabled ->
                        coroutineScope.launch {
                            repository.updateCameraCutoutEnabled(enabled)
                        }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Accent Color Selection
        SectionHeader(title = "ACCENT COLOR INDICATOR")

        Spacer(modifier = Modifier.height(6.dp))

        IslandCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val colors = listOf(
                    "Nothing Red" to 0xFFD71920L,
                    "Crimson" to 0xFFFF2A32L,
                    "Pure White" to 0xFFFFFFFFL,
                    "Amber" to 0xFFFF9800L,
                    "Cyan AI" to 0xFF00E5FFL
                )

                colors.forEach { (name, hex) ->
                    val isSelected = prefs.accentColorHex == hex
                    val color = Color(hex)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable {
                            coroutineScope.launch {
                                repository.saveCalibration(
                                    compactWidthDp = prefs.compactWidthDp,
                                    compactHeightDp = prefs.compactHeightDp,
                                    cornerRadiusDp = prefs.cornerRadiusDp,
                                    topMarginDp = prefs.topMarginDp,
                                    horizontalOffsetDp = prefs.horizontalOffsetDp,
                                    accentColorHex = hex,
                                    presetName = prefs.presetName
                                )
                            }
                        }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    2.dp,
                                    if (isSelected) NothingPrimaryText else Color.Transparent,
                                    CircleShape
                                )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = name.split(" ").first(),
                            color = if (isSelected) NothingPrimaryText else NothingMutedText,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Music Style Studio Section (Milestone R7)
        SectionHeader(
            title = "MUSIC STYLE STUDIO",
            badgeText = musicPrefs.visualizerStyle.name.replace('_', ' ')
        )

        Spacer(modifier = Modifier.height(6.dp))

        MusicStyleStudio(
            currentPreferences = musicPrefs,
            onApplyPreferences = { updated ->
                coroutineScope.launch {
                    repository.updateMusicPreferences(updated)
                }
            },
            onRestoreDefaults = {
                coroutineScope.launch {
                    repository.resetMusicPreferencesToDefaults()
                }
            }
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Charging Style Studio Section (Milestone R8)
        SectionHeader(
            title = "CHARGING STYLE STUDIO",
            badgeText = batteryPrefs.animationStyle.name.replace('_', ' ')
        )

        Spacer(modifier = Modifier.height(6.dp))

        ChargingStyleStudio(
            currentPreferences = batteryPrefs,
            onApplyPreferences = { updated ->
                coroutineScope.launch {
                    repository.updateBatteryPreferences(updated)
                }
            },
            onRestoreDefaults = {
                coroutineScope.launch {
                    repository.resetBatteryPreferencesToDefaults()
                }
            }
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Reset to Defaults Button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(NothingElevatedSurface)
                .border(1.dp, NothingDivider, RoundedCornerShape(12.dp))
                .clickable {
                    coroutineScope.launch {
                        repository.resetToHardwareDefaults()
                    }
                }
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "RESET TO HARDWARE DEFAULTS",
                color = NothingSecondaryText,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Milestone R5 Scope Notice
        IslandCard(
            backgroundColor = Color(0xFF0D0D0D),
            borderColor = Color(0xFF1F1F1F)
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(NothingSuccess)
                        .padding(top = 4.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "ACTIVE (R5): All calibration sliders and theme presets are dynamically wired to Jetpack Preferences DataStore and the live WindowManager overlay. Adjusting values updates the physical floating island immediately.",
                    color = NothingMutedText,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.SansSerif,
                    lineHeight = 15.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun PresetPill(
    label: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) NothingElevatedSurface else Color.Transparent)
            .border(
                1.dp,
                if (isSelected) NothingPrimaryAccent else NothingDivider,
                RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isSelected) NothingPrimaryText else NothingMutedText,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
fun CustomSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    unit: String,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: (() -> Unit)? = null
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = NothingSecondaryText,
                fontSize = 12.sp,
                fontFamily = FontFamily.SansSerif
            )
            Text(
                text = "${value.toInt()} $unit",
                color = NothingPrimaryText,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }

        Slider(
            value = value,
            onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = NothingPrimaryAccent,
                activeTrackColor = NothingPrimaryAccent,
                inactiveTrackColor = NothingDivider
            )
        )
    }
}

