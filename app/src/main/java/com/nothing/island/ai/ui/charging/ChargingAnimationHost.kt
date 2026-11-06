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
 * Dispatches and renders one of the 5 authentic Nothing OS charging animations
 * based on [ChargingAnimationStyle].
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
            ChargingAnimationStyle.ENERGY_RING -> {
                EnergyRingAnimation(
                    percentage = percentage,
                    isCharging = isCharging,
                    accentColor = accentColor,
                    speed = speed,
                    intensity = intensity,
                    glowIntensity = glowIntensity,
                    reducedMotion = reducedMotion
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
                    reducedMotion = reducedMotion
                )
            }
            ChargingAnimationStyle.LIQUID_ENERGY -> {
                LiquidEnergyAnimation(
                    percentage = percentage,
                    isCharging = isCharging,
                    accentColor = accentColor,
                    speed = speed,
                    intensity = intensity,
                    glowIntensity = glowIntensity,
                    reducedMotion = reducedMotion
                )
            }
            ChargingAnimationStyle.CHARGING_AURA -> {
                ChargingAuraAnimation(
                    percentage = percentage,
                    isCharging = isCharging,
                    accentColor = accentColor,
                    speed = speed,
                    intensity = intensity,
                    glowIntensity = glowIntensity,
                    reducedMotion = reducedMotion
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
                    reducedMotion = reducedMotion
                )
            }
        }
    }
}
