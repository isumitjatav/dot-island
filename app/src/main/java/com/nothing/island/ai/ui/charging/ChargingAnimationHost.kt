package com.nothing.island.ai.ui.charging

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.nothing.island.ai.battery.ChargingAnimationStyle

/**
 * ChargingAnimationHost
 *
 * Dispatches and renders one of the 12 authentic Nothing OS charging animations
 * based on [ChargingAnimationStyle], with fallbacks for legacy styles.
 */
@Composable
fun ChargingAnimationHost(
    style: ChargingAnimationStyle,
    percentage: Int,
    isCharging: Boolean,
    accentColor: Color,
    speed: Float = 1.0f,
    intensity: Float = 1.0f,
    glowIntensity: Float = 0.6f,
    reducedMotion: Boolean = false,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        when (style) {
            ChargingAnimationStyle.GLYPH_ARC,
            ChargingAnimationStyle.ENERGY_RING -> {
                GlyphArcAnimation(
                    percentage = percentage,
                    isCharging = isCharging,
                    accentColor = accentColor,
                    speed = speed,
                    intensity = intensity,
                    glowIntensity = glowIntensity,
                    reducedMotion = reducedMotion,
                    modifier = Modifier.matchParentSize()
                )
            }
            ChargingAnimationStyle.DOT_MATRIX_REACTOR -> {
                DotMatrixReactorAnimation(
                    percentage = percentage,
                    isCharging = isCharging,
                    accentColor = accentColor,
                    speed = speed,
                    intensity = intensity,
                    glowIntensity = glowIntensity,
                    reducedMotion = reducedMotion,
                    modifier = Modifier.matchParentSize()
                )
            }
            ChargingAnimationStyle.WIREFRAME_ORBIT -> {
                WireframeOrbitAnimation(
                    percentage = percentage,
                    isCharging = isCharging,
                    accentColor = accentColor,
                    speed = speed,
                    intensity = intensity,
                    glowIntensity = glowIntensity,
                    reducedMotion = reducedMotion,
                    modifier = Modifier.matchParentSize()
                )
            }
            ChargingAnimationStyle.NOTHING_RED_SURGE,
            ChargingAnimationStyle.CHARGING_AURA -> {
                NothingRedSurgeAnimation(
                    percentage = percentage,
                    isCharging = isCharging,
                    accentColor = accentColor,
                    speed = speed,
                    intensity = intensity,
                    glowIntensity = glowIntensity,
                    reducedMotion = reducedMotion,
                    modifier = Modifier.matchParentSize()
                )
            }
            ChargingAnimationStyle.LIQUID_MERCURY,
            ChargingAnimationStyle.LIQUID_ENERGY -> {
                LiquidMercuryAnimation(
                    percentage = percentage,
                    isCharging = isCharging,
                    accentColor = accentColor,
                    speed = speed,
                    intensity = intensity,
                    glowIntensity = glowIntensity,
                    reducedMotion = reducedMotion,
                    modifier = Modifier.matchParentSize()
                )
            }
            ChargingAnimationStyle.ELECTRIC_PULSE -> {
                ElectricPulseAnimation(
                    percentage = percentage,
                    isCharging = isCharging,
                    accentColor = accentColor,
                    speed = speed,
                    intensity = intensity,
                    glowIntensity = glowIntensity,
                    reducedMotion = reducedMotion,
                    modifier = Modifier.matchParentSize()
                )
            }
            ChargingAnimationStyle.CYBER_HORIZON -> {
                CyberHorizonAnimation(
                    percentage = percentage,
                    isCharging = isCharging,
                    accentColor = accentColor,
                    speed = speed,
                    intensity = intensity,
                    glowIntensity = glowIntensity,
                    reducedMotion = reducedMotion,
                    modifier = Modifier.matchParentSize()
                )
            }
            ChargingAnimationStyle.PLASMA_FLOW -> {
                PlasmaFlowAnimation(
                    percentage = percentage,
                    isCharging = isCharging,
                    accentColor = accentColor,
                    speed = speed,
                    intensity = intensity,
                    glowIntensity = glowIntensity,
                    reducedMotion = reducedMotion,
                    modifier = Modifier.matchParentSize()
                )
            }
            ChargingAnimationStyle.DIGITAL_GAUGE -> {
                DigitalGaugeAnimation(
                    percentage = percentage,
                    isCharging = isCharging,
                    accentColor = accentColor,
                    speed = speed,
                    intensity = intensity,
                    glowIntensity = glowIntensity,
                    reducedMotion = reducedMotion,
                    modifier = Modifier.matchParentSize()
                )
            }
            ChargingAnimationStyle.WATTAGE_REACTOR -> {
                WattageReactorAnimation(
                    percentage = percentage,
                    isCharging = isCharging,
                    accentColor = accentColor,
                    speed = speed,
                    intensity = intensity,
                    glowIntensity = glowIntensity,
                    reducedMotion = reducedMotion,
                    modifier = Modifier.matchParentSize()
                )
            }
            ChargingAnimationStyle.MINIMALIST_BOLT -> {
                MinimalistBoltAnimation(
                    percentage = percentage,
                    isCharging = isCharging,
                    accentColor = accentColor,
                    speed = speed,
                    intensity = intensity,
                    glowIntensity = glowIntensity,
                    reducedMotion = reducedMotion,
                    modifier = Modifier.matchParentSize()
                )
            }
            ChargingAnimationStyle.BATTERY_CELL -> {
                BatteryCellAnimation(
                    percentage = percentage,
                    isCharging = isCharging,
                    accentColor = accentColor,
                    speed = speed,
                    intensity = intensity,
                    glowIntensity = glowIntensity,
                    reducedMotion = reducedMotion,
                    modifier = Modifier.matchParentSize()
                )
            }
        }
    }
}
