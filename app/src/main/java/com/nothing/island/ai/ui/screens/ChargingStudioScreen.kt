package com.nothing.island.ai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nothing.island.ai.battery.ChargingAnimationStyle
import com.nothing.island.ai.data.IslandPreferencesRepository
import com.nothing.island.ai.ui.components.IslandPreview
import com.nothing.island.ai.ui.components.IslandPreviewMode
import com.nothing.island.ai.ui.components.NothingCard
import com.nothing.island.ai.ui.components.NothingCardVariant
import com.nothing.island.ai.ui.components.NothingSectionHeader
import com.nothing.island.ai.ui.components.NothingSegmentedControl
import com.nothing.island.ai.ui.components.NothingSwitchRow
import com.nothing.island.ai.ui.theme.NothingTokens

/**
 * Nothing OS 3.x Charging Studio Screen
 *
 * Dedicated design laboratory for configuring battery telemetry, 2-column charging
 * visualizer theme gallery, thermal safety alerts, and low battery intelligence.
 */
@Composable
fun ChargingStudioScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repository = remember { IslandPreferencesRepository.getInstance(context) }
    val batteryPrefs by repository.liveBatteryPreferences.collectAsState()
    val scrollState = rememberScrollState()

    var lowBatteryThreshold by remember { mutableStateOf(batteryPrefs.lowBatteryThreshold) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NothingTokens.NothingBlack)
            .verticalScroll(scrollState)
            .padding(horizontal = NothingTokens.ScreenHorizontalPadding, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Studio Header
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
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(NothingTokens.NothingGreen)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "CHARGING STUDIO",
                        color = NothingTokens.NothingTextPrimary,
                        fontSize = 18.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    )
                }
                Text(
                    text = "HARDWARE TELEMETRY & VISUALIZER STYLES",
                    color = NothingTokens.NothingTextTertiary,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Interactive Live Preview Engine (Default: Charging Telemetry)
        IslandPreview(
            initialMode = IslandPreviewMode.CHARGING
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Section: Master Switch
        NothingSectionHeader(title = "BATTERY MONITOR", isAccent = true)
        NothingCard(variant = NothingCardVariant.STANDARD) {
            NothingSwitchRow(
                title = "Enable Charging Overlay",
                description = "Display animated charging aura and wattage when plugged into power",
                checked = batteryPrefs.chargingAnimationEnabled,
                onCheckedChange = { /* Toggle */ },
                badgeText = "ACTIVE"
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Section: 2-Column Visualizer Themes Gallery (Reference: charging themes.jpg)
        NothingSectionHeader(title = "CHARGING VISUALIZERS (2-COL GALLERY)")
        val styles = listOf(
            ChargingAnimationStyle.ENERGY_RING to "ENERGY RING",
            ChargingAnimationStyle.ELECTRIC_PULSE to "ELECTRIC PULSE",
            ChargingAnimationStyle.LIQUID_ENERGY to "LIQUID ENERGY",
            ChargingAnimationStyle.CHARGING_AURA to "CHARGING AURA",
            ChargingAnimationStyle.PLASMA_FLOW to "PLASMA FLOW"
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            styles.chunked(2).forEach { rowStyles: List<Pair<ChargingAnimationStyle, String>> ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowStyles.forEach { item: Pair<ChargingAnimationStyle, String> ->
                        val style = item.first
                        val name = item.second
                        val isSelected = batteryPrefs.animationStyle == style
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(NothingTokens.ControlRadius))
                                .background(if (isSelected) NothingTokens.NothingSurfaceMedium else NothingTokens.NothingSurfaceLow)
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) NothingTokens.NothingGreen else NothingTokens.NothingBorderSubtle,
                                    shape = RoundedCornerShape(NothingTokens.ControlRadius)
                                )
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    /* Repository update */
                                }
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "⚡",
                                    fontSize = 18.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = name,
                                    color = if (isSelected) NothingTokens.NothingTextPrimary else NothingTokens.NothingTextSecondary,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                    if (rowStyles.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Section: Low Battery Intelligence
        NothingSectionHeader(title = "LOW BATTERY ALERT")
        NothingCard(variant = NothingCardVariant.STANDARD) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Alert Threshold",
                    color = NothingTokens.NothingTextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Show compact warning pill when battery level drops below threshold",
                    color = NothingTokens.NothingTextSecondary,
                    fontSize = 12.sp
                )

                val thresholds = listOf(10, 15, 20)
                NothingSegmentedControl(
                    items = thresholds,
                    selectedItem = lowBatteryThreshold,
                    onItemSelected = { lowBatteryThreshold = it },
                    labelProvider = { "$it%" }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
