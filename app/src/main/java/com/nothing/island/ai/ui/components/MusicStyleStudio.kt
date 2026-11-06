package com.nothing.island.ai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import com.nothing.island.ai.media.ArtworkShape
import com.nothing.island.ai.media.MediaTrackState
import com.nothing.island.ai.media.MusicPreferences
import com.nothing.island.ai.media.MusicVisualizerStyle

/**
 * Music Style Studio
 *
 * Dedicated customization studio for Nothing Island Music & Visualizers:
 * - 5 Selectable genuine Compose visualizers: Vertical Equalizer, Waveform, Circular, Vinyl, Minimal Pulse.
 * - Live dynamic preview responding in real-time.
 * - Accent color selection & adaptive artwork extraction.
 * - Dynamic controls: Speed, Intensity, Glow, Artwork Shape, Rotation, Reduced Motion, Auto-expansion.
 * - Apply and Restore Default controls with DataStore persistence.
 */
@Composable
fun MusicStyleStudio(
    currentPreferences: MusicPreferences,
    onApplyPreferences: (MusicPreferences) -> Unit,
    onRestoreDefaults: () -> Unit,
    modifier: Modifier = Modifier
) {
    var workingPrefs by remember(currentPreferences) { mutableStateOf(currentPreferences) }

    // Mock track for live interactive preview
    val mockTrack = remember {
        MediaTrackState(
            sessionId = "studio_preview_session",
            packageName = "com.spotify.music",
            appDisplayName = "SPOTIFY",
            trackTitle = "Glyph Rhythms",
            artistName = "Nothing Studio",
            albumName = "OS 3.0",
            isPlaying = true,
            durationMs = 214000L,
            currentPositionMs = 84000L,
            audioOutputName = "Nothing Ear (2)",
            dominantColor = Color(workingPrefs.accentColorHex)
        )
    }

    val activeAccent = workingPrefs.resolveAccent(Color(workingPrefs.accentColorHex))

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
                    text = "MUSIC STYLE STUDIO",
                    color = NothingWhite,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "VISUALIZERS & AMBIENT AUDIO TUNING",
                    color = NothingMutedText,
                    fontSize = 9.5.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(activeAccent.copy(alpha = 0.2f))
                    .border(1.dp, activeAccent, RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = workingPrefs.visualizerStyle.name.replace('_', ' '),
                    color = activeAccent,
                    fontSize = 8.5.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // =====================================================================
        // 1. LIVE INTERACTIVE PREVIEW
        // =====================================================================
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(NothingBlack)
                .border(1.dp, activeAccent.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "LIVE AUDIO-REACTIVE PREVIEW",
                color = NothingMutedText,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )

            // Compact Capsule Preview
            MusicCompactView(
                trackState = mockTrack,
                preferences = workingPrefs,
                onTap = {},
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        // =====================================================================
        // 2. VISUALIZER STYLE PICKER (5 Genuine Working Styles)
        // =====================================================================
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "VISUALIZER STYLE",
                color = NothingWhite,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                MusicVisualizerStyle.values().take(3).forEach { style ->
                    val isSelected = workingPrefs.visualizerStyle == style
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) activeAccent else Color(0xFF141414))
                            .border(1.dp, if (isSelected) activeAccent else NothingBorder, RoundedCornerShape(8.dp))
                            .clickable {
                                workingPrefs = workingPrefs.copy(visualizerStyle = style)
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = style.name.replace('_', '\n'),
                            color = if (isSelected) NothingWhite else NothingMutedText,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 11.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                MusicVisualizerStyle.values().drop(3).forEach { style ->
                    val isSelected = workingPrefs.visualizerStyle == style
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) activeAccent else Color(0xFF141414))
                            .border(1.dp, if (isSelected) activeAccent else NothingBorder, RoundedCornerShape(8.dp))
                            .clickable {
                                workingPrefs = workingPrefs.copy(visualizerStyle = style)
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = style.name.replace('_', ' '),
                            color = if (isSelected) NothingWhite else NothingMutedText,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        }

        // =====================================================================
        // 3. ACCENT COLOR & ADAPTIVE TOGGLE
        // =====================================================================
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "ADAPTIVE ARTWORK ACCENT",
                        color = NothingWhite,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Extracts accent glow from album artwork",
                        color = NothingMutedText,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Switch(
                    checked = workingPrefs.useAdaptiveAccent,
                    onCheckedChange = { workingPrefs = workingPrefs.copy(useAdaptiveAccent = it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = NothingWhite,
                        checkedTrackColor = activeAccent,
                        uncheckedThumbColor = NothingMutedText,
                        uncheckedTrackColor = NothingBorder
                    )
                )
            }

            Text(
                text = "DEFAULT ACCENT PALETTE",
                color = NothingMutedText,
                fontSize = 9.5.sp,
                fontFamily = FontFamily.Monospace
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                MusicPreferences.PRESET_ACCENTS.forEach { hex ->
                    val color = Color(hex)
                    val isSelected = workingPrefs.accentColorHex == hex
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) NothingWhite else NothingBorder,
                                shape = CircleShape
                            )
                            .clickable {
                                workingPrefs = workingPrefs.copy(accentColorHex = hex)
                            }
                    )
                }
            }
        }

        // =====================================================================
        // 4. ARTWORK GEOMETRY & ROTATION
        // =====================================================================
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "ARTWORK PRESENTATION SHAPE",
                color = NothingWhite,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ArtworkShape.values().forEach { shape ->
                    val isSelected = workingPrefs.artworkShape == shape
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) activeAccent else Color(0xFF141414))
                            .border(1.dp, if (isSelected) activeAccent else NothingBorder, RoundedCornerShape(8.dp))
                            .clickable {
                                workingPrefs = workingPrefs.copy(artworkShape = shape)
                            }
                            .padding(vertical = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = shape.name.replace('_', ' '),
                            color = if (isSelected) NothingWhite else NothingMutedText,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "ALBUM ARTWORK ROTATION",
                        color = NothingWhite,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Smoothly spins artwork while playing",
                        color = NothingMutedText,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Switch(
                    checked = workingPrefs.albumRotationEnabled,
                    onCheckedChange = { workingPrefs = workingPrefs.copy(albumRotationEnabled = it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = NothingWhite,
                        checkedTrackColor = activeAccent,
                        uncheckedThumbColor = NothingMutedText,
                        uncheckedTrackColor = NothingBorder
                    )
                )
            }
        }

        // =====================================================================
        // 5. DYNAMICS & GLOW SLIDERS
        // =====================================================================
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "ANIMATION SPEED",
                    color = NothingWhite,
                    fontSize = 10.5.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = String.format("%.1fx", workingPrefs.animationSpeed),
                    color = activeAccent,
                    fontSize = 10.5.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
            Slider(
                value = workingPrefs.animationSpeed,
                onValueChange = { workingPrefs = workingPrefs.copy(animationSpeed = it) },
                valueRange = 0.5f..2.0f,
                steps = 6,
                colors = SliderDefaults.colors(
                    thumbColor = activeAccent,
                    activeTrackColor = activeAccent,
                    inactiveTrackColor = Color(0xFF262626)
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "ANIMATION INTENSITY",
                    color = NothingWhite,
                    fontSize = 10.5.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = String.format("%.1fx", workingPrefs.animationIntensity),
                    color = activeAccent,
                    fontSize = 10.5.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
            Slider(
                value = workingPrefs.animationIntensity,
                onValueChange = { workingPrefs = workingPrefs.copy(animationIntensity = it) },
                valueRange = 0.5f..1.5f,
                steps = 4,
                colors = SliderDefaults.colors(
                    thumbColor = activeAccent,
                    activeTrackColor = activeAccent,
                    inactiveTrackColor = Color(0xFF262626)
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "AMBIENT GLOW INTENSITY",
                    color = NothingWhite,
                    fontSize = 10.5.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = String.format("%d%%", (workingPrefs.glowIntensity * 100).toInt()),
                    color = activeAccent,
                    fontSize = 10.5.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
            Slider(
                value = workingPrefs.glowIntensity,
                onValueChange = { workingPrefs = workingPrefs.copy(glowIntensity = it) },
                valueRange = 0.0f..1.0f,
                steps = 5,
                colors = SliderDefaults.colors(
                    thumbColor = activeAccent,
                    activeTrackColor = activeAccent,
                    inactiveTrackColor = Color(0xFF262626)
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "REDUCED MOTION",
                        color = NothingWhite,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Dampens visualizer movements to calm shifts",
                        color = NothingMutedText,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Switch(
                    checked = workingPrefs.reducedMotion,
                    onCheckedChange = { workingPrefs = workingPrefs.copy(reducedMotion = it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = NothingWhite,
                        checkedTrackColor = activeAccent,
                        uncheckedThumbColor = NothingMutedText,
                        uncheckedTrackColor = NothingBorder
                    )
                )
            }
        }

        // =====================================================================
        // 6. SMART MUSIC EXPANSION
        // =====================================================================
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "AUTO-EXPAND ON NEW TRACK",
                        color = NothingWhite,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Briefly expands music player when track changes",
                        color = NothingMutedText,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Switch(
                    checked = workingPrefs.isAutoExpandEnabled,
                    onCheckedChange = { workingPrefs = workingPrefs.copy(isAutoExpandEnabled = it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = NothingWhite,
                        checkedTrackColor = activeAccent,
                        uncheckedThumbColor = NothingMutedText,
                        uncheckedTrackColor = NothingBorder
                    )
                )
            }

            if (workingPrefs.isAutoExpandEnabled) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "PREVIEW EXPAND DURATION",
                        color = NothingMutedText,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = String.format("%.1fs", workingPrefs.autoExpandDurationSeconds),
                        color = activeAccent,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
                Slider(
                    value = workingPrefs.autoExpandDurationSeconds,
                    onValueChange = { workingPrefs = workingPrefs.copy(autoExpandDurationSeconds = it) },
                    valueRange = 2.0f..10.0f,
                    steps = 8,
                    colors = SliderDefaults.colors(
                        thumbColor = activeAccent,
                        activeTrackColor = activeAccent,
                        inactiveTrackColor = Color(0xFF262626)
                    )
                )
            }
        }

        // =====================================================================
        // 7. ACTION BUTTONS: APPLY & RESTORE DEFAULTS
        // =====================================================================
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(NothingDarkGray)
                    .border(1.dp, NothingBorder, RoundedCornerShape(8.dp))
                    .clickable {
                        workingPrefs = MusicPreferences.DEFAULTS
                        onRestoreDefaults()
                    }
                    .padding(vertical = 10.dp),
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
                    .clip(RoundedCornerShape(8.dp))
                    .background(activeAccent)
                    .clickable {
                        onApplyPreferences(workingPrefs)
                    }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "APPLY SETTINGS",
                    color = NothingWhite,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
