package com.nothing.island.ai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nothing.island.ai.NothingBlack
import com.nothing.island.ai.NothingBorder
import com.nothing.island.ai.NothingDarkGray
import com.nothing.island.ai.NothingMutedText
import com.nothing.island.ai.NothingWhite
import com.nothing.island.ai.battery.BatteryPreferences
import com.nothing.island.ai.battery.BatteryState
import com.nothing.island.ai.switcher.SwipeDirection
import com.nothing.island.ai.ui.charging.ChargingAnimationHost

/**
 * State D: Battery & Charging Expanded View
 *
 * Interactive charging intelligence presentation within Nothing Island's 350dp x 210dp window.
 * Displays live charging animations, verified honest telemetry (temperature, voltage, wattage, ETA),
 * and supports Activity Switcher horizontal gestures.
 */
@Composable
fun BatteryExpandedView(
    batteryState: BatteryState,
    preferences: BatteryPreferences = BatteryPreferences(),
    onCollapse: () -> Unit,
    onSwipe: (SwipeDirection) -> Unit = {},
    topClearanceDp: Dp = 48.dp,
    modifier: Modifier = Modifier
) {
    val cardShape = RoundedCornerShape(26.dp)
    val accentColor = Color(preferences.accentColorHex)

    var totalDrag by remember { mutableFloatStateOf(0f) }
    val swipeThresholdPx = 40f

    val isCharging = batteryState.isCharging
    val isLowBattery = batteryState.isLowBattery
    val isOverheat = batteryState.isOverheating

    val borderColor = when {
        isOverheat -> Color(0xFFFF5252)
        isLowBattery -> Color(0xFFFFB300)
        isCharging && preferences.glowIntensity > 0f -> accentColor.copy(alpha = (0.5f * preferences.glowIntensity).coerceIn(0.2f, 0.8f))
        else -> NothingBorder
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(cardShape)
            .background(NothingBlack)
            .border(1.dp, borderColor, cardShape)
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { totalDrag = 0f },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        totalDrag += dragAmount
                    },
                    onDragEnd = {
                        if (totalDrag > swipeThresholdPx) {
                            onSwipe(SwipeDirection.RIGHT)
                        } else if (totalDrag < -swipeThresholdPx) {
                            onSwipe(SwipeDirection.LEFT)
                        }
                        totalDrag = 0f
                    },
                    onDragCancel = { totalDrag = 0f }
                )
            }
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. Top Punch-Hole Clearance Spacer (48dp / 144px below status bar)
            Spacer(modifier = Modifier.height(topClearanceDp))

            // 2. Header Row: Title, Status Badge, and Collapse Chip
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Text(
                        text = if (isCharging) "CHARGING INTELLIGENCE" else "BATTERY INTELLIGENCE",
                        color = NothingWhite,
                        fontSize = 11.5.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    // Status Pill
                    val wattage = batteryState.wattageW
                    val isFast = wattage != null && wattage >= 18f
                    val statusText = when {
                        isOverheat -> "OVERHEAT"
                        isLowBattery -> "LOW BATTERY"
                        batteryState.isFullyCharged -> "FULLY CHARGED"
                        isFast -> "FAST CHARGING"
                        isCharging -> "CHARGING"
                        else -> "DISCHARGING"
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                when {
                                    isOverheat -> Color(0x33FF5252)
                                    isLowBattery -> Color(0x33FFB300)
                                    isCharging -> accentColor.copy(alpha = 0.2f)
                                    else -> NothingDarkGray
                                }
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = statusText,
                            color = when {
                                isOverheat -> Color(0xFFFF5252)
                                isLowBattery -> Color(0xFFFFB300)
                                isCharging -> accentColor
                                else -> NothingMutedText
                            },
                            fontSize = 8.5.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(NothingDarkGray)
                        .clickable { onCollapse() }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "COLLAPSE",
                        color = NothingWhite,
                        fontSize = 10.5.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // 3. Center Stage: Active Compose Charging Visualizer + Monospace Level
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(NothingDarkGray)
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Animation Canvas
                Box(
                    modifier = Modifier
                        .size(80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    ChargingAnimationHost(
                        style = preferences.animationStyle,
                        percentage = batteryState.percentage,
                        isCharging = isCharging,
                        accentColor = accentColor,
                        speed = preferences.animationSpeed,
                        intensity = preferences.animationIntensity,
                        glowIntensity = preferences.glowIntensity,
                        reducedMotion = preferences.reducedMotion,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Level Readout & Subtitle
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "${batteryState.percentage}",
                            color = NothingWhite,
                            fontSize = 32.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-1).sp
                        )
                        Text(
                            text = "%",
                            color = accentColor,
                            fontSize = 18.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 3.dp)
                        )
                    }

                    Text(
                        text = batteryState.sourceLabel,
                        color = NothingMutedText,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 4. Telemetry Shelf (4 Honest Metrics: Temp, Voltage, Wattage, ETA)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF181818))
                    .border(1.dp, NothingBorder, RoundedCornerShape(10.dp))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TelemetryMetricItem(
                    label = "TEMP",
                    value = batteryState.formattedTemperature,
                    valueColor = if (isOverheat) Color(0xFFFF5252) else NothingWhite
                )

                TelemetryMetricItem(
                    label = "VOLTAGE",
                    value = batteryState.formattedVoltage
                )

                val currentWattage = batteryState.wattageW
                TelemetryMetricItem(
                    label = "WATTAGE",
                    value = batteryState.formattedWattage,
                    valueColor = if (currentWattage != null && currentWattage >= 18f) accentColor else NothingWhite
                )

                TelemetryMetricItem(
                    label = "EST. ETA",
                    value = batteryState.formattedEta,
                    valueColor = if (isCharging) accentColor else NothingMutedText
                )
            }
        }
    }
}

@Composable
private fun TelemetryMetricItem(
    label: String,
    value: String,
    valueColor: Color = NothingWhite,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            color = NothingMutedText,
            fontSize = 8.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
        Spacer(modifier = Modifier.height(1.dp))
        Text(
            text = value,
            color = valueColor,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}
