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
import androidx.compose.runtime.setValue
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.nothing.island.ai.data.IslandPreferencesRepository
import com.nothing.island.ai.ui.components.IslandPreview
import com.nothing.island.ai.ui.components.NothingCard
import com.nothing.island.ai.ui.components.NothingCardVariant
import com.nothing.island.ai.ui.components.NothingSectionHeader
import com.nothing.island.ai.ui.components.NothingStepperSlider
import com.nothing.island.ai.ui.theme.NothingTokens

/**
 * Nothing OS 3.x Geometry Calibration Screen
 *
 * Dedicated calibration laboratory for micro-adjusting punch-hole dimensions,
 * corner radii, and vertical top offsets for screen protectors or display scaling.
 */
@Composable
fun GeometryStudioScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repository = remember { IslandPreferencesRepository.getInstance(context) }
    val coroutineScope = rememberCoroutineScope()
    val prefs by repository.livePreferences.collectAsState()
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NothingTokens.NothingBlack)
            .verticalScroll(scrollState)
            .padding(horizontal = NothingTokens.ScreenHorizontalPadding, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Back Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(NothingTokens.NothingSurfaceMedium)
                    .border(1.dp, NothingTokens.NothingBorderSubtle, CircleShape)
                    .clickable { onNavigateBack() },
                contentAlignment = Alignment.Center
            ) {
                Text(text = "<", color = NothingTokens.NothingTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "GEOMETRY CALIBRATION",
                    color = NothingTokens.NothingTextPrimary,
                    fontSize = 18.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "CUTOUT ALIGNMENT & SIZING",
                    color = NothingTokens.NothingTextTertiary,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        var isExpandedCalibrationTab by remember { mutableStateOf(false) }

        // Mode Switch: COMPACT PILL vs EXPANDED U-NOTCH
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(NothingTokens.NothingSurfaceMedium)
                .border(1.dp, NothingTokens.NothingBorderSubtle, RoundedCornerShape(12.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val modes = listOf("COMPACT PILL" to false, "EXPANDED U-NOTCH" to true)
            modes.forEach { (label, isExpandedTab) ->
                val selected = isExpandedCalibrationTab == isExpandedTab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(9.dp))
                        .background(if (selected) Color(0xFFD71921) else Color.Transparent)
                        .clickable { isExpandedCalibrationTab = isExpandedTab }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = if (selected) Color.White else NothingTokens.NothingTextSecondary,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        IslandPreview(
            initialMode = if (isExpandedCalibrationTab) {
                com.nothing.island.ai.ui.components.IslandPreviewMode.MUSIC_EXPANDED
            } else {
                com.nothing.island.ai.ui.components.IslandPreviewMode.IDLE
            },
            pillWidthDp = prefs.compactWidthDp,
            pillHeightDp = prefs.compactHeightDp,
            cornerRadiusDp = prefs.cornerRadiusDp,
            topOffsetDp = prefs.topMarginDp,
            isCameraCutoutEnabled = prefs.isCameraCutoutEnabled,
            expandedOffsetXDp = prefs.expandedOffsetX,
            expandedOffsetYDp = prefs.expandedOffsetY,
            expandedNotchWidthDp = prefs.expandedNotchWidth,
            expandedNotchDepthDp = prefs.expandedNotchDepth,
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (!isExpandedCalibrationTab) {
            NothingSectionHeader(title = "COMPACT CAPSULE DIMENSIONS", isAccent = true)
            NothingCard(variant = NothingCardVariant.STANDARD) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    NothingStepperSlider(
                        value = prefs.compactWidthDp,
                        onValueChange = {
                            repository.updateLiveCalibration(compactWidthDp = it)
                            coroutineScope.launch { repository.persistCurrentCalibration() }
                        },
                        valueRange = 80f..260f,
                        stepSize = 2f,
                        label = "Compact Width",
                        formattedValue = "${prefs.compactWidthDp.toInt()} DP"
                    )

                    NothingStepperSlider(
                        value = prefs.compactHeightDp,
                        onValueChange = {
                            repository.updateLiveCalibration(compactHeightDp = it)
                            coroutineScope.launch { repository.persistCurrentCalibration() }
                        },
                        valueRange = 24f..60f,
                        stepSize = 2f,
                        label = "Compact Height",
                        formattedValue = "${prefs.compactHeightDp.toInt()} DP"
                    )

                    NothingStepperSlider(
                        value = prefs.cornerRadiusDp,
                        onValueChange = {
                            repository.updateLiveCalibration(cornerRadiusDp = it)
                            coroutineScope.launch { repository.persistCurrentCalibration() }
                        },
                        valueRange = 8f..30f,
                        stepSize = 1f,
                        label = "Corner Radius",
                        formattedValue = "${prefs.cornerRadiusDp.toInt()} DP"
                    )

                    NothingStepperSlider(
                        value = prefs.topMarginDp,
                        onValueChange = {
                            repository.updateLiveCalibration(topMarginDp = it)
                            coroutineScope.launch { repository.persistCurrentCalibration() }
                        },
                        valueRange = 0f..40f,
                        stepSize = 1f,
                        label = "Compact Top Margin (Y)",
                        formattedValue = "${prefs.topMarginDp.toInt()} DP"
                    )
                }
            }
        } else {
            NothingSectionHeader(title = "EXPANDED U-NOTCH CALIBRATION", isAccent = true)
            NothingCard(variant = NothingCardVariant.STANDARD) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Camera Cutout Notch",
                                color = NothingTokens.NothingTextPrimary,
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Contour expanded cards around front camera lens",
                                color = NothingTokens.NothingTextTertiary,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        com.nothing.island.ai.ui.components.NothingSwitch(
                            checked = prefs.isCameraCutoutEnabled,
                            onCheckedChange = { enabled ->
                                coroutineScope.launch {
                                    repository.updateCameraCutoutEnabled(enabled)
                                }
                            }
                        )
                    }

                    NothingStepperSlider(
                        value = prefs.expandedOffsetX,
                        onValueChange = {
                            repository.updateLiveCalibration(expandedOffsetX = it)
                            coroutineScope.launch { repository.persistCurrentCalibration() }
                        },
                        valueRange = -60f..60f,
                        stepSize = 1f,
                        label = "Notch X Offset",
                        formattedValue = "${prefs.expandedOffsetX.toInt()} DP"
                    )

                    NothingStepperSlider(
                        value = prefs.expandedOffsetY,
                        onValueChange = {
                            repository.updateLiveCalibration(expandedOffsetY = it)
                            coroutineScope.launch { repository.persistCurrentCalibration() }
                        },
                        valueRange = -30f..30f,
                        stepSize = 1f,
                        label = "Card Y Offset",
                        formattedValue = "${prefs.expandedOffsetY.toInt()} DP"
                    )

                    NothingStepperSlider(
                        value = prefs.expandedNotchWidth,
                        onValueChange = {
                            repository.updateLiveCalibration(expandedNotchWidth = it)
                            coroutineScope.launch { repository.persistCurrentCalibration() }
                        },
                        valueRange = 24f..64f,
                        stepSize = 1f,
                        label = "U-Notch Width",
                        formattedValue = "${prefs.expandedNotchWidth.toInt()} DP"
                    )

                    NothingStepperSlider(
                        value = prefs.expandedNotchDepth,
                        onValueChange = {
                            repository.updateLiveCalibration(expandedNotchDepth = it)
                            coroutineScope.launch { repository.persistCurrentCalibration() }
                        },
                        valueRange = 16f..48f,
                        stepSize = 1f,
                        label = "U-Notch Depth",
                        formattedValue = "${prefs.expandedNotchDepth.toInt()} DP"
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(NothingTokens.NothingSurfaceMedium)
                            .border(1.dp, Color(0xFFD71921).copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                            .clickable {
                                coroutineScope.launch {
                                    repository.resetExpandedCalibration()
                                }
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "RESET EXPANDED",
                            color = Color(0xFFD71921),
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
