package com.nothing.island.ai.ui.components

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nothing.island.ai.NothingBlack
import com.nothing.island.ai.NothingBorder
import com.nothing.island.ai.NothingDarkGray
import com.nothing.island.ai.NothingMutedText
import com.nothing.island.ai.NothingRed
import com.nothing.island.ai.NothingWhite
import com.nothing.island.ai.battery.BatteryPreferences
import com.nothing.island.ai.battery.BatteryState
import com.nothing.island.ai.battery.ChargingAnimationStyle
import com.nothing.island.ai.battery.ChargingSource
import com.nothing.island.ai.battery.ChargingStatus
import com.nothing.island.ai.ui.charging.ChargingAnimationHost

/**
 * Charging Style Studio
 *
 * Dedicated customization studio for Nothing Island Battery & Charging Intelligence:
 * - 5 Selectable Compose charging animations: Energy Ring, Electric Pulse, Liquid Energy, Charging Aura, Plasma Flow.
 * - Live dynamic preview responding in real-time.
 * - Accent color selection swatches (Nothing Red, Glyph Green, Pure White, Cyan Pulse, Amber Glow).
 * - Sliders: Speed, Intensity, Glow, Display Duration, Low Battery Threshold.
 * - Switches: Reduced Motion, Auto-Expand on Cable Plugged, Temperature Warning Alert.
 * - Draggable preview battery percentage slider.
 * - Apply and Restore Default controls with DataStore persistence.
 */
@Composable
fun ChargingStyleStudio(
    currentPreferences: BatteryPreferences,
    onApplyPreferences: (BatteryPreferences) -> Unit,
    onRestoreDefaults: () -> Unit,
    modifier: Modifier = Modifier
) {
    var workingPrefs by remember(currentPreferences) { mutableStateOf(currentPreferences) }
    var previewPercentage by remember { mutableIntStateOf(85) }

    val activeAccent = Color(workingPrefs.accentColorHex)

    val previewState = remember(previewPercentage, workingPrefs) {
        BatteryState(
            level = previewPercentage,
            scale = 100,
            status = ChargingStatus.CHARGING,
            source = ChargingSource.AC,
            voltageMv = 4436,
            temperatureC = 35.0f,
            currentMicroAmps = 2000000,
            chargeTimeRemainingMs = 1800000L,
            isCharging = true,
            isLowBattery = previewPercentage <= workingPrefs.lowBatteryThreshold,
            isFullyCharged = previewPercentage >= 100,
            isOverheating = false
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(NothingDarkGray)
            .border(1.dp, NothingBorder, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Section Title & Badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "CHARGING STYLE STUDIO",
                    color = NothingWhite,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "MILESTONE R8 • 5 CHARGING STYLES",
                    color = NothingMutedText,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(activeAccent.copy(alpha = 0.2f))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = workingPrefs.animationStyle.name.replace('_', ' '),
                    color = activeAccent,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Live Dynamic Interactive Preview
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(NothingBlack)
                .border(1.dp, NothingBorder, RoundedCornerShape(14.dp))
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "LIVE INTERACTIVE PREVIEW",
                    color = NothingMutedText,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )

                // Animation Canvas with Central Readout
                Box(
                    modifier = Modifier.size(110.dp),
                    contentAlignment = Alignment.Center
                ) {
                    ChargingAnimationHost(
                        style = workingPrefs.animationStyle,
                        percentage = previewPercentage,
                        isCharging = true,
                        accentColor = activeAccent,
                        speed = workingPrefs.animationSpeed,
                        intensity = workingPrefs.animationIntensity,
                        glowIntensity = workingPrefs.glowIntensity,
                        reducedMotion = workingPrefs.reducedMotion,
                        modifier = Modifier.fillMaxSize()
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$previewPercentage%",
                            color = NothingWhite,
                            fontSize = 18.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "AC CHARGING",
                            color = activeAccent,
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Interactive Percentage Dragger
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "TEST LEVEL:",
                        color = NothingMutedText,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )

                    Slider(
                        value = previewPercentage.toFloat(),
                        onValueChange = { previewPercentage = it.toInt() },
                        valueRange = 0f..100f,
                        colors = SliderDefaults.colors(
                            thumbColor = activeAccent,
                            activeTrackColor = activeAccent,
                            inactiveTrackColor = NothingBorder
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    Text(
                        text = "$previewPercentage%",
                        color = NothingWhite,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(36.dp)
                    )
                }
            }
        }

        // Style Selector Chips (5 Styles)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "CHARGING ANIMATION STYLE",
                color = NothingMutedText,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ChargingAnimationStyle.values().take(3).forEach { style ->
                    StudioChip(
                        label = when (style) {
                            ChargingAnimationStyle.ENERGY_RING -> "ENERGY RING"
                            ChargingAnimationStyle.ELECTRIC_PULSE -> "ELECTRIC PULSE"
                            ChargingAnimationStyle.LIQUID_ENERGY -> "LIQUID ENERGY"
                            else -> style.name
                        },
                        isSelected = workingPrefs.animationStyle == style,
                        accentColor = activeAccent,
                        modifier = Modifier.weight(1f),
                        onClick = { workingPrefs = workingPrefs.copy(animationStyle = style) }
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ChargingAnimationStyle.values().drop(3).forEach { style ->
                    StudioChip(
                        label = when (style) {
                            ChargingAnimationStyle.CHARGING_AURA -> "CHARGING AURA"
                            ChargingAnimationStyle.PLASMA_FLOW -> "PLASMA FLOW"
                            else -> style.name
                        },
                        isSelected = workingPrefs.animationStyle == style,
                        accentColor = activeAccent,
                        modifier = Modifier.weight(1f),
                        onClick = { workingPrefs = workingPrefs.copy(animationStyle = style) }
                    )
                }
            }
        }

        // Accent Color Swatches
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "ACCENT COLOR PRESET",
                color = NothingMutedText,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )

            val colorSwatches = listOf(
                0xFFD71920L to "Nothing Red",
                0xFF00E676L to "Glyph Green",
                0xFFFFFFFFL to "Pure White",
                0xFF00E5FFL to "Cyan Pulse",
                0xFFFFB300L to "Amber Glow"
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                colorSwatches.forEach { (colorHex, name) ->
                    val swatchColor = Color(colorHex)
                    val isSelected = workingPrefs.accentColorHex == colorHex

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(swatchColor)
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) NothingWhite else NothingBorder,
                                shape = CircleShape
                            )
                            .clickable {
                                workingPrefs = workingPrefs.copy(accentColorHex = colorHex)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(if (colorHex == 0xFFFFFFFFL) NothingBlack else NothingWhite)
                            )
                        }
                    }
                }
            }
        }

        // Dynamic Sliders
        StudioSlider(
            label = "Animation Speed",
            value = workingPrefs.animationSpeed,
            range = 0.5f..2.0f,
            format = "%.1fx",
            accentColor = activeAccent,
            onValueChange = { workingPrefs = workingPrefs.copy(animationSpeed = it) }
        )

        StudioSlider(
            label = "Animation Intensity",
            value = workingPrefs.animationIntensity,
            range = 0.5f..1.5f,
            format = "%.1fx",
            accentColor = activeAccent,
            onValueChange = { workingPrefs = workingPrefs.copy(animationIntensity = it) }
        )

        StudioSlider(
            label = "Ambient Glow",
            value = workingPrefs.glowIntensity,
            range = 0.0f..1.0f,
            format = "%.1f",
            accentColor = activeAccent,
            onValueChange = { workingPrefs = workingPrefs.copy(glowIntensity = it) }
        )

        StudioSlider(
            label = "Alert Display Duration",
            value = workingPrefs.displayDurationMs / 1000f,
            range = 2.0f..10.0f,
            format = "%.1fs",
            accentColor = activeAccent,
            onValueChange = { workingPrefs = workingPrefs.copy(displayDurationMs = (it * 1000).toLong()) }
        )

        StudioSlider(
            label = "Low Battery Threshold",
            value = workingPrefs.lowBatteryThreshold.toFloat(),
            range = 5f..30f,
            format = "%.0f%%",
            accentColor = activeAccent,
            onValueChange = { workingPrefs = workingPrefs.copy(lowBatteryThreshold = it.toInt()) }
        )

        // Switches
        StudioSwitchRow(
            title = "Auto-Expand on Cable Plugged",
            subtitle = "Open Nothing Island card when charger is connected",
            checked = workingPrefs.autoExpandOnPlugged,
            accentColor = activeAccent,
            onCheckedChange = { workingPrefs = workingPrefs.copy(autoExpandOnPlugged = it) }
        )

        StudioSwitchRow(
            title = "Temperature Warning Alert",
            subtitle = "Prominent safety alert if battery exceeds 45°C",
            checked = workingPrefs.temperatureWarningEnabled,
            accentColor = activeAccent,
            onCheckedChange = { workingPrefs = workingPrefs.copy(temperatureWarningEnabled = it) }
        )

        StudioSwitchRow(
            title = "Reduced Motion",
            subtitle = "Simplify charging visualizers for power saving",
            checked = workingPrefs.reducedMotion,
            accentColor = activeAccent,
            onCheckedChange = { workingPrefs = workingPrefs.copy(reducedMotion = it) }
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Action Buttons: Apply & Restore Defaults
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF1E1E1E))
                    .border(1.dp, NothingBorder, RoundedCornerShape(10.dp))
                    .clickable {
                        workingPrefs = BatteryPreferences()
                        onRestoreDefaults()
                    }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "RESTORE DEFAULTS",
                    color = NothingMutedText,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(activeAccent)
                    .clickable {
                        onApplyPreferences(workingPrefs)
                    }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "APPLY SETTINGS",
                    color = if (workingPrefs.accentColorHex == 0xFFFFFFFFL) NothingBlack else NothingWhite,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun StudioChip(
    label: String,
    isSelected: Boolean,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) accentColor.copy(alpha = 0.2f) else NothingBlack)
            .border(
                1.dp,
                if (isSelected) accentColor else NothingBorder,
                RoundedCornerShape(8.dp)
            )
            .clickable { onClick() }
            .padding(vertical = 8.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isSelected) NothingWhite else NothingMutedText,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1
        )
    }
}

@Composable
private fun StudioSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    format: String,
    accentColor: Color,
    onValueChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = NothingWhite,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = String.format(format, value),
                color = accentColor,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }

        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = accentColor,
                activeTrackColor = accentColor,
                inactiveTrackColor = NothingBorder
            )
        )
    }
}

@Composable
private fun StudioSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    accentColor: Color,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = NothingWhite,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                color = NothingMutedText,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = NothingWhite,
                checkedTrackColor = accentColor,
                uncheckedThumbColor = NothingMutedText,
                uncheckedTrackColor = NothingBlack
            )
        )
    }
}
