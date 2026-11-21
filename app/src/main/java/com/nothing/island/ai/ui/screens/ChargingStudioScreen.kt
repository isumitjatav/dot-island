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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nothing.island.ai.IslandOverlayController
import com.nothing.island.ai.battery.BatteryPreferences
import com.nothing.island.ai.battery.ChargingAnimationStyle
import com.nothing.island.ai.data.IslandPreferencesRepository
import com.nothing.island.ai.ui.charging.ChargingAnimationHost
import com.nothing.island.ai.ui.components.NothingCard
import com.nothing.island.ai.ui.components.NothingCardVariant
import com.nothing.island.ai.ui.components.NothingSectionHeader
import com.nothing.island.ai.ui.components.NothingSegmentedControl
import com.nothing.island.ai.ui.components.NothingSwitchRow
import com.nothing.island.ai.ui.theme.NothingTokens
import kotlinx.coroutines.launch

/**
 * Nothing OS 3.x Charging Studio Screen (Phase 3)
 *
 * Dedicated design laboratory for configuring 12 Nothing-native charging animations,
 * sticky live charging preview, hardware telemetry, and low battery safety alerts.
 */
@Composable
fun ChargingStudioScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    val repository = remember { IslandPreferencesRepository.getInstance(context) }
    val batteryPrefs by repository.liveBatteryPreferences.collectAsState()

    val overlayController = remember { IslandOverlayController.getInstance(context) }
    val batteryState by overlayController.batteryMonitor.batteryState.collectAsState()

    val scrollState = rememberScrollState()
    val isRealCharging = batteryState.isCharging
    val activePercentage = if (isRealCharging) batteryState.percentage else 68
    val accentColor = batteryPrefs.resolveAccent()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NothingTokens.NothingBlack)
            .verticalScroll(scrollState)
            .padding(horizontal = NothingTokens.ScreenHorizontalPadding, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Studio Header
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
                            .background(if (isRealCharging) NothingTokens.NothingGreen else NothingTokens.NothingAmber)
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
                    text = "12 NOTHING-NATIVE KINETIC CHARGING ANIMATIONS",
                    color = NothingTokens.NothingTextTertiary,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 2. Sticky Live Charging Preview Card
        NothingCard(
            variant = NothingCardVariant.STANDARD,
            contentPadding = 14.dp
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Status Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (isRealCharging) NothingTokens.NothingGreen else NothingTokens.NothingAmber)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isRealCharging) "LIVE CHARGING TELEMETRY" else "SIMULATED PREVIEW",
                            color = NothingTokens.NothingTextSecondary,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(NothingTokens.NothingSurfaceMedium)
                            .border(1.dp, NothingTokens.NothingBorderSubtle, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (isRealCharging) "HARDWARE CONNECTED" else "DISCONNECTED",
                            color = if (isRealCharging) NothingTokens.NothingGreen else NothingTokens.NothingAmber,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Center Stage: Active Kinetic Charging Animation
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(NothingTokens.NothingSurfaceLowest)
                        .border(1.dp, NothingTokens.NothingBorderSubtle, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    ChargingAnimationHost(
                        style = batteryPrefs.animationStyle,
                        percentage = activePercentage,
                        isCharging = true,
                        accentColor = accentColor,
                        speed = batteryPrefs.animationSpeed,
                        intensity = batteryPrefs.animationIntensity,
                        glowIntensity = batteryPrefs.glowIntensity,
                        reducedMotion = batteryPrefs.reducedMotion,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Telemetry & Selected Theme Title
                Text(
                    text = "${batteryPrefs.animationStyle.displayName.uppercase()} • $activePercentage%",
                    color = NothingTokens.NothingTextPrimary,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Live Telemetry Grid Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(NothingTokens.NothingSurfaceLowest)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("POWER", color = NothingTokens.NothingTextTertiary, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                        Text(if (isRealCharging) batteryState.formattedWattage else "45.0W", color = NothingTokens.NothingTextPrimary, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("VOLTAGE", color = NothingTokens.NothingTextTertiary, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                        Text(if (isRealCharging) batteryState.formattedVoltage else "4.40V", color = NothingTokens.NothingTextPrimary, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("TEMP", color = NothingTokens.NothingTextTertiary, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                        Text(if (isRealCharging) batteryState.formattedTemperature else "32.5°C", color = NothingTokens.NothingTextPrimary, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("SOURCE", color = NothingTokens.NothingTextTertiary, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                        Text(if (isRealCharging) batteryState.sourceLabel else "SIMULATED", color = if (isRealCharging) NothingTokens.NothingGreen else NothingTokens.NothingAmber, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }
                }

                if (!isRealCharging) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "SIMULATED PREVIEW — DEMONSTRATES DYNAMICS WHEN DISCONNECTED",
                        color = NothingTokens.NothingTextTertiary,
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. Master Switch
        NothingSectionHeader(title = "BATTERY MONITOR", isAccent = true)
        NothingCard(variant = NothingCardVariant.STANDARD) {
            NothingSwitchRow(
                title = "Enable Charging Overlay",
                description = "Display animated kinetic charging visualizer on notch during active charge",
                checked = batteryPrefs.chargingAnimationEnabled,
                onCheckedChange = { checked ->
                    val updated = batteryPrefs.copy(chargingAnimationEnabled = checked)
                    repository.updateLiveBatteryPreferences(updated)
                    coroutineScope.launch { repository.updateBatteryPreferences(updated) }
                },
                badgeText = if (batteryPrefs.chargingAnimationEnabled) "ACTIVE" else "OFF"
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 4. Two-Column Animation Gallery (All 12 Styles)
        NothingSectionHeader(title = "CHARGING THEMES (16 STYLES)")
        val styles = ChargingAnimationStyle.ACTIVE_12

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            styles.chunked(2).forEach { rowStyles ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    rowStyles.forEach { style ->
                        val isSelected = batteryPrefs.animationStyle == style

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(NothingTokens.ControlRadius))
                                .background(if (isSelected) NothingTokens.NothingSurfaceMedium else NothingTokens.NothingSurfaceLow)
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) NothingTokens.NothingGreen else NothingTokens.NothingBorderSubtle,
                                    shape = RoundedCornerShape(NothingTokens.ControlRadius)
                                )
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    val updated = batteryPrefs.copy(animationStyle = style)
                                    repository.updateLiveBatteryPreferences(updated)
                                    coroutineScope.launch { repository.updateBatteryPreferences(updated) }
                                }
                                .padding(10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // Category Pill & Selected Indicator Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = when (style.category) {
                                            "NOTHING SIGNATURE" -> "SIGNATURE"
                                            "FLUID & ELEMENTAL" -> "ELEMENTAL"
                                            "SCENE FILL" -> "SCENE"
                                            else -> "MINIMAL"
                                        },
                                        color = if (isSelected) NothingTokens.NothingGreen else NothingTokens.NothingTextTertiary,
                                        fontSize = 8.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )

                                    if (isSelected) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(NothingTokens.NothingGreen)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Miniature Animated Preview Box
                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(CircleShape)
                                        .background(NothingTokens.NothingSurfaceLowest),
                                    contentAlignment = Alignment.Center
                                ) {
                                    ChargingAnimationHost(
                                        style = style,
                                        percentage = activePercentage,
                                        isCharging = true,
                                        accentColor = if (isSelected) accentColor else Color.White,
                                        speed = batteryPrefs.animationSpeed,
                                        intensity = batteryPrefs.animationIntensity,
                                        glowIntensity = if (isSelected) batteryPrefs.glowIntensity else 0.3f,
                                        reducedMotion = batteryPrefs.reducedMotion,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Title & Description
                                Text(
                                    text = style.displayName.uppercase(),
                                    color = if (isSelected) NothingTokens.NothingTextPrimary else NothingTokens.NothingTextSecondary,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                                Text(
                                    text = style.description,
                                    color = NothingTokens.NothingTextTertiary,
                                    fontSize = 9.sp,
                                    maxLines = 1,
                                    lineHeight = 11.sp
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

        Spacer(modifier = Modifier.height(16.dp))

        // 5. Preset Accent Colors
        NothingSectionHeader(title = "CHARGING GLOW ACCENT")
        NothingCard(variant = NothingCardVariant.STANDARD) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Select Accent Glow",
                    color = NothingTokens.NothingTextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BatteryPreferences.PRESET_ACCENTS.forEach { hex ->
                        val chipColor = Color(hex)
                        val isChosen = batteryPrefs.accentColorHex == hex

                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(chipColor)
                                .border(
                                    width = if (isChosen) 2.5.dp else 1.dp,
                                    color = if (isChosen) Color.White else NothingTokens.NothingBorderSubtle,
                                    shape = CircleShape
                                )
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    val updated = batteryPrefs.copy(accentColorHex = hex)
                                    repository.updateLiveBatteryPreferences(updated)
                                    coroutineScope.launch { repository.updateBatteryPreferences(updated) }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isChosen) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(if (hex == 0xFFFFFFFFL) NothingTokens.NothingBlack else Color.White)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 6. Motion & Safety Controls
        NothingSectionHeader(title = "MOTION & SAFETY ALERTS")
        NothingCard(variant = NothingCardVariant.STANDARD) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Reduced Motion Switch
                NothingSwitchRow(
                    title = "Reduced Motion Mode",
                    description = "Halt continuous rotation and orbital particle streams",
                    checked = batteryPrefs.reducedMotion,
                    onCheckedChange = { checked ->
                        val updated = batteryPrefs.copy(reducedMotion = checked)
                        repository.updateLiveBatteryPreferences(updated)
                        coroutineScope.launch { repository.updateBatteryPreferences(updated) }
                    }
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Low Battery Alert Threshold
                Text(
                    text = "Low Battery Alert Threshold",
                    color = NothingTokens.NothingTextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Show warning pill when battery level drops below threshold",
                    color = NothingTokens.NothingTextSecondary,
                    fontSize = 11.sp
                )

                val thresholds = listOf(10, 15, 20)
                NothingSegmentedControl(
                    items = thresholds,
                    selectedItem = batteryPrefs.lowBatteryThreshold,
                    onItemSelected = { threshold ->
                        val updated = batteryPrefs.copy(lowBatteryThreshold = threshold)
                        repository.updateLiveBatteryPreferences(updated)
                        coroutineScope.launch { repository.updateBatteryPreferences(updated) }
                    },
                    labelProvider = { "$it%" }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
