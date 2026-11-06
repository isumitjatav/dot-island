package com.nothing.island.ai.ui.visualizers

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.nothing.island.ai.media.MusicPreferences
import com.nothing.island.ai.media.MusicVisualizerStyle

/**
 * Unified host composable that dispatches the configured [MusicVisualizerStyle].
 */
@Composable
fun MusicVisualizerHost(
    style: MusicVisualizerStyle,
    isPlaying: Boolean,
    accentColor: Color,
    preferences: MusicPreferences = MusicPreferences.DEFAULTS,
    modifier: Modifier = Modifier
) {
    val speed = preferences.animationSpeed
    val intensity = preferences.animationIntensity
    val glow = preferences.glowIntensity
    val reducedMotion = preferences.reducedMotion

    when (style) {
        MusicVisualizerStyle.VERTICAL_EQUALIZER -> {
            VerticalEqualizerVisualizer(
                isPlaying = isPlaying,
                accentColor = accentColor,
                speed = speed,
                intensity = intensity,
                glowIntensity = glow,
                reducedMotion = reducedMotion,
                modifier = modifier
            )
        }
        MusicVisualizerStyle.WAVEFORM -> {
            WaveformVisualizer(
                isPlaying = isPlaying,
                accentColor = accentColor,
                speed = speed,
                intensity = intensity,
                glowIntensity = glow,
                reducedMotion = reducedMotion,
                modifier = modifier
            )
        }
        MusicVisualizerStyle.CIRCULAR -> {
            CircularVisualizer(
                isPlaying = isPlaying,
                accentColor = accentColor,
                speed = speed,
                intensity = intensity,
                glowIntensity = glow,
                reducedMotion = reducedMotion,
                modifier = modifier
            )
        }
        MusicVisualizerStyle.VINYL -> {
            VinylStyleVisualizer(
                isPlaying = isPlaying,
                accentColor = accentColor,
                speed = speed,
                intensity = intensity,
                glowIntensity = glow,
                reducedMotion = reducedMotion,
                modifier = modifier
            )
        }
        MusicVisualizerStyle.MINIMAL_PULSE -> {
            MinimalPulseVisualizer(
                isPlaying = isPlaying,
                accentColor = accentColor,
                speed = speed,
                intensity = intensity,
                glowIntensity = glow,
                reducedMotion = reducedMotion,
                modifier = modifier
            )
        }
    }
}
