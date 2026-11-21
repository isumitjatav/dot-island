package com.nothing.island.ai.battery

import androidx.compose.ui.graphics.Color

/**
 * Visual presentation styles for Nothing Island Charging Animation Studio.
 * Includes the 12 Nothing-native charging styles and legacy aliases.
 */
enum class ChargingAnimationStyle(
    val id: String,
    val displayName: String,
    val category: String = "NOTHING SIGNATURE",
    val description: String = ""
) {
    // 12 Nothing-native charging styles
    GLYPH_ARC(
        "glyph_arc",
        "Glyph Arc",
        "NOTHING SIGNATURE",
        "Segmented Glyph lighting ring with orbiting white arc"
    ),
    DOT_MATRIX_REACTOR(
        "dot_matrix_reactor",
        "Dot Matrix",
        "NOTHING SIGNATURE",
        "LED dot-matrix ring pulsating around the camera lens"
    ),
    WIREFRAME_ORBIT(
        "wireframe_orbit",
        "Wireframe Orbit",
        "NOTHING SIGNATURE",
        "Dual counter-rotating technical wireframe rings"
    ),
    NOTHING_RED_SURGE(
        "nothing_red_surge",
        "Red Surge",
        "NOTHING SIGNATURE",
        "Iconic Nothing Red energy wave tracking charge level"
    ),
    LIQUID_MERCURY(
        "liquid_mercury",
        "Liquid Mercury",
        "FLUID & ELEMENTAL",
        "Monochrome fluid reservoir with buoyant meniscus physics"
    ),
    ELECTRIC_PULSE(
        "electric_pulse",
        "Electric Pulse",
        "FLUID & ELEMENTAL",
        "High-frequency neon electrical discharge shimmer"
    ),
    CYBER_HORIZON(
        "cyber_horizon",
        "Cyber Horizon",
        "FLUID & ELEMENTAL",
        "Dual cyan laser lines sweeping laterally across the notch"
    ),
    PLASMA_FLOW(
        "plasma_flow",
        "Plasma Flow",
        "FLUID & ELEMENTAL",
        "Multi-stop angular sweep gradient with continuous rotation"
    ),
    DIGITAL_GAUGE(
        "digital_gauge",
        "Digital Gauge",
        "TECHNICAL & MINIMAL",
        "Precision 360-degree calibrated technical tick dial"
    ),
    WATTAGE_REACTOR(
        "wattage_reactor",
        "Wattage Reactor",
        "TECHNICAL & MINIMAL",
        "Kinetic power bars scaling directly with charging speed"
    ),
    MINIMALIST_BOLT(
        "minimalist_bolt",
        "Minimalist Bolt",
        "TECHNICAL & MINIMAL",
        "Subtle high-contrast lightning glyph with pulse glow"
    ),
    BATTERY_CELL(
        "battery_cell",
        "Battery Cell",
        "TECHNICAL & MINIMAL",
        "Classic battery enclosure with segmented dot fill"
    ),

    // Original scene-fill styles (percentage fills with a scene character)
    SCENE_PUMPKIN(
        "scene_pumpkin",
        "Pumpkin Patch",
        "SCENE FILL",
        "Purple scene fills with a bobbing pumpkin at the charge boundary"
    ),
    SCENE_ROBOT(
        "scene_robot",
        "Robot Buddy",
        "SCENE FILL",
        "Teal scene fills with a little robot at the charge boundary"
    ),
    SCENE_SNOWMAN(
        "scene_snowman",
        "Snowman",
        "SCENE FILL",
        "Blue scene fills with a snowman at the charge boundary"
    ),
    SCENE_DRAGONFLY(
        "scene_dragonfly",
        "Dragonfly",
        "SCENE FILL",
        "Sky scene fills with a dragonfly at the charge boundary"
    ),

    // Legacy entries preserved for backward compatibility
    @Deprecated("Mapped to GLYPH_ARC")
    ENERGY_RING(
        "energy_ring",
        "Energy Ring",
        "LEGACY",
        "Concentric orbiting energy rings"
    ),
    @Deprecated("Mapped to LIQUID_MERCURY")
    LIQUID_ENERGY(
        "liquid_energy",
        "Liquid Energy",
        "LEGACY",
        "Fluid wave fill rising to battery percentage"
    ),
    @Deprecated("Mapped to NOTHING_RED_SURGE")
    CHARGING_AURA(
        "charging_aura",
        "Charging Aura",
        "LEGACY",
        "Multi-layered ambient breathing halo"
    );

    companion object {
        val DEFAULT = GLYPH_ARC

        val ACTIVE_12: List<ChargingAnimationStyle> = listOf(
            GLYPH_ARC,
            DOT_MATRIX_REACTOR,
            WIREFRAME_ORBIT,
            NOTHING_RED_SURGE,
            LIQUID_MERCURY,
            ELECTRIC_PULSE,
            CYBER_HORIZON,
            PLASMA_FLOW,
            DIGITAL_GAUGE,
            WATTAGE_REACTOR,
            MINIMALIST_BOLT,
            BATTERY_CELL,
            SCENE_PUMPKIN,
            SCENE_ROBOT,
            SCENE_SNOWMAN,
            SCENE_DRAGONFLY
        )

        fun fromString(name: String?): ChargingAnimationStyle {
            if (name == null) return DEFAULT
            return try {
                valueOf(name)
            } catch (_: Exception) {
                entries.find { it.id.equals(name, ignoreCase = true) } ?: DEFAULT
            }
        }
    }
}

/**
 * Immutable configuration for Nothing Island Battery & Charging subsystem.
 */
data class BatteryPreferences(
    val enabled: Boolean = true,
    val chargingAnimationEnabled: Boolean = true,
    val animationStyle: ChargingAnimationStyle = ChargingAnimationStyle.GLYPH_ARC,
    val animationSpeed: Float = 1.0f,               // 0.5f to 2.0f
    val animationIntensity: Float = 1.0f,           // 0.5f to 1.5f
    val accentColorHex: Long = 0xFFD71920L,         // Nothing Red default
    val glowIntensity: Float = 0.6f,                // 0.0f to 1.0f
    val reducedMotion: Boolean = false,
    val displayDurationMs: Long = 4500L,            // 2000ms to 10000ms
    val lowBatteryThreshold: Int = 20,              // 5% to 30%, default 20%
    val temperatureWarningEnabled: Boolean = true,
    val temperatureWarningThresholdC: Float = 45.0f,// 40.0°C to 50.0°C
    val autoExpandOnPlugged: Boolean = true,
    val showSegmentedBatteryGauge: Boolean = true
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
