package com.nothing.island.ai.battery

import androidx.compose.ui.graphics.Color

/**
 * Visual presentation styles for Nothing Island Charging Animation Studio.
 */
enum class ChargingAnimationStyle {
    /**
     * Default: Concentric orbiting energy rings with particle ticks around the cutout.
     */
    ENERGY_RING,

    /**
     * Dynamic jagged electrical arcing and surging light pulses along the perimeter.
     */
    ELECTRIC_PULSE,

    /**
     * Fluid wave fill rising to battery percentage with surface ripple physics.
     */
    LIQUID_ENERGY,

    /**
     * Multi-layered ambient breathing aura with Nothing Red / chromatic glow.
     */
    CHARGING_AURA,

    /**
     * Orbiting plasma orbs circulating in an elliptical vortex around the punch-hole.
     */
    PLASMA_FLOW;

    companion object {
        val DEFAULT = ENERGY_RING
    }
}

/**
 * Immutable configuration for Nothing Island Battery & Charging subsystem.
 */
data class BatteryPreferences(
    val enabled: Boolean = true,
    val chargingAnimationEnabled: Boolean = true,
    val animationStyle: ChargingAnimationStyle = ChargingAnimationStyle.ENERGY_RING,
    val animationSpeed: Float = 1.0f,               // 0.5f to 2.0f
    val animationIntensity: Float = 1.0f,           // 0.5f to 1.5f
    val accentColorHex: Long = 0xFFD71920L,         // Nothing Red default
    val glowIntensity: Float = 0.6f,                // 0.0f to 1.0f
    val reducedMotion: Boolean = false,
    val displayDurationMs: Long = 4500L,            // 2000ms to 10000ms
    val lowBatteryThreshold: Int = 20,              // 5% to 30%, default 20%
    val temperatureWarningEnabled: Boolean = true,
    val temperatureWarningThresholdC: Float = 45.0f,// 40.0°C to 50.0°C
    val autoExpandOnPlugged: Boolean = true
) {
    companion object {
        val DEFAULTS = BatteryPreferences()
        const val MIN_DURATION_MS = 2000L
        const val MAX_DURATION_MS = 10000L

        // Preset Accents for Charging Style Studio
        val PRESET_ACCENTS = listOf(
            0xFFD71920L, // Nothing Red
            0xFF00E676L, // Glyph Green (Charged)
            0xFFFFFFFFL, // Nothing Pure White
            0xFF00E5FFL, // Cyan Pulse
            0xFFFF9100L  // Amber Glow
        )
    }

    fun resolveAccent(): Color {
        return Color(accentColorHex)
    }
}
