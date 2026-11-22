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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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

        IslandPreview(
            pillWidthDp = prefs.compactWidthDp,
            pillHeightDp = prefs.compactHeightDp,
            cornerRadiusDp = prefs.cornerRadiusDp,
            topOffsetDp = prefs.topMarginDp
        )

        Spacer(modifier = Modifier.height(16.dp))

        NothingSectionHeader(title = "COMPACT CAPSULE DIMENSIONS", isAccent = true)
        NothingCard(variant = NothingCardVariant.STANDARD) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                NothingStepperSlider(
                    value = prefs.compactWidthDp,
                    onValueChange = { repository.updateLiveCalibration(compactWidthDp = it) },
                    valueRange = 80f..260f,
                    stepSize = 2f,
                    label = "Compact Width",
                    formattedValue = "${prefs.compactWidthDp.toInt()} DP"
                )

                NothingStepperSlider(
                    value = prefs.compactHeightDp,
                    onValueChange = { repository.updateLiveCalibration(compactHeightDp = it) },
                    valueRange = 24f..60f,
                    stepSize = 2f,
                    label = "Compact Height",
                    formattedValue = "${prefs.compactHeightDp.toInt()} DP"
                )

                NothingStepperSlider(
                    value = prefs.cornerRadiusDp,
                    onValueChange = { repository.updateLiveCalibration(cornerRadiusDp = it) },
                    valueRange = 8f..30f,
                    stepSize = 1f,
                    label = "Corner Radius",
                    formattedValue = "${prefs.cornerRadiusDp.toInt()} DP"
                )

                NothingStepperSlider(
                    value = prefs.topMarginDp,
                    onValueChange = { repository.updateLiveCalibration(topMarginDp = it) },
                    valueRange = 0f..40f,
                    stepSize = 1f,
                    label = "Top Margin Offset",
                    formattedValue = "${prefs.topMarginDp.toInt()} DP"
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        NothingSectionHeader(title = "EXPANDED CARD CUTOUT", isAccent = true)
        NothingCard(variant = NothingCardVariant.STANDARD) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                    Switch(
                        checked = prefs.isCameraCutoutEnabled,
                        onCheckedChange = { enabled ->
                            coroutineScope.launch {
                                repository.updateCameraCutoutEnabled(enabled)
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = NothingTokens.NothingTextPrimary,
                            checkedTrackColor = NothingTokens.NothingRed,
                            uncheckedThumbColor = NothingTokens.NothingTextTertiary,
                            uncheckedTrackColor = NothingTokens.NothingSurfaceMedium
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
