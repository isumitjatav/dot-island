package com.nothing.island.ai.media

import androidx.compose.ui.graphics.Color

/**
 * Visualizer presentation styles available in Nothing Island Music Style Studio.
 */
enum class MusicVisualizerStyle {
    /**
     * Default: Dynamic vertical segmented bars with Nothing OS dot-matrix aesthetics.
     */
    VERTICAL_EQUALIZER,

    /**
     * Smooth undulating audio waveform curve with peak dynamics.
     */
    WAVEFORM,

    /**
     * Orbiting radial equalizer spikes radiating around the album artwork perimeter.
     */
    CIRCULAR,

    /**
     * Classic vinyl record styling with concentric groove reflection rings and subtle rotation markers.
     */
    VINYL,

    /**
     * Minimalist breathing ambient glow ring pulsating rhythmically to the audio beat.
     */
    MINIMAL_PULSE
}

/**
 * Geometric shape options for album artwork presentation.
 */
enum class ArtworkShape {
    CIRCLE,
    SQUIRCLE,
    ROUNDED_RECT
}

/**
 * Immutable configuration settings for Music & Visualizer presentation in Nothing Island.
 * Persisted persistently via Jetpack Preferences DataStore in [IslandPreferencesRepository].
 */
data class MusicPreferences(
    val visualizerStyle: MusicVisualizerStyle = MusicVisualizerStyle.VERTICAL_EQUALIZER,
    val accentColorHex: Long = 0xFFD71920L, // NothingRed
    val useAdaptiveAccent: Boolean = true,
    val animationSpeed: Float = 1.0f,       // 0.5f to 2.0f
    val animationIntensity: Float = 1.0f,   // 0.5f to 1.5f
    val albumRotationEnabled: Boolean = true,
    val artworkShape: ArtworkShape = ArtworkShape.CIRCLE,
    val glowIntensity: Float = 0.6f,        // 0.0f to 1.0f
    val reducedMotion: Boolean = false,
    val autoExpandDurationSeconds: Float = 4.5f, // 2.0f to 10.0f
    val isAutoExpandEnabled: Boolean = true
) {
    companion object {
        val DEFAULTS = MusicPreferences()
        
        // Preset Colors for Music Style Studio
        val PRESET_ACCENTS = listOf(
            0xFFD71920L, // Nothing Red
            0xFF8A2BE2L, // Nothing Purple / Violet
            0xFFFFFFFFL, // Nothing Pure White
            0xFF00E5FFL, // Cyan Pulse
            0xFFFF9100L  // Amber Glow
        )
    }

    /**
     * Resolves the primary visualizer accent color, optionally adapting to track artwork
     * dominant color if enabled.
     */
    fun resolveAccent(adaptiveColor: Color? = null): Color {
        return if (useAdaptiveAccent && adaptiveColor != null && adaptiveColor != Color.Unspecified) {
            adaptiveColor
        } else {
            Color(accentColorHex)
        }
    }
}
