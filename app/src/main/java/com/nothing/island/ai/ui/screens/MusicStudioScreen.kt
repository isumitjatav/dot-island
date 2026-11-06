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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nothing.island.ai.data.IslandPreferencesRepository
import com.nothing.island.ai.media.MusicVisualizerStyle
import com.nothing.island.ai.ui.components.IslandPreview
import com.nothing.island.ai.ui.components.IslandPreviewMode
import com.nothing.island.ai.ui.components.NothingCard
import com.nothing.island.ai.ui.components.NothingCardVariant
import com.nothing.island.ai.ui.components.NothingSectionHeader
import com.nothing.island.ai.ui.components.NothingSegmentedControl
import com.nothing.island.ai.ui.components.NothingSwitchRow
import com.nothing.island.ai.ui.theme.NothingTokens

/**
 * Nothing OS 3.x Music Studio Screen
 *
 * Dedicated design laboratory for configuring media playback aesthetics,
 * vinyl artwork rotation, waveform visualizers, and fast-forward transport buttons.
 */
@Composable
fun MusicStudioScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repository = remember { IslandPreferencesRepository.getInstance(context) }
    val musicPrefs by repository.liveMusicPreferences.collectAsState()
    val scrollState = rememberScrollState()

    var showFastForwardButtons by remember { mutableStateOf(true) }
    var longPressVolumeEnabled by remember { mutableStateOf(true) }
    var albumPhotoAsBg by remember { mutableStateOf(false) }
    var onlyShowWhilePlaying by remember { mutableStateOf(true) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NothingTokens.NothingBlack)
            .verticalScroll(scrollState)
            .padding(horizontal = NothingTokens.ScreenHorizontalPadding, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Studio Header
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
                            .background(NothingTokens.NothingBlue)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "MUSIC STUDIO",
                        color = NothingTokens.NothingTextPrimary,
                        fontSize = 18.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    )
                }
                Text(
                    text = "VINYL ROTATION, WAVEFORMS & TRANSPORT",
                    color = NothingTokens.NothingTextTertiary,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Interactive Live Preview Engine (Default: Expanded Music Player)
        IslandPreview(
            initialMode = IslandPreviewMode.MUSIC_EXPANDED
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Section: Master Switch
        NothingSectionHeader(title = "MEDIA SESSION LISTENER", isAccent = true)
        NothingCard(variant = NothingCardVariant.STANDARD) {
            NothingSwitchRow(
                title = "Enable Music Player",
                description = "Dock ongoing media playback to the front camera cutout",
                checked = true,
                onCheckedChange = { /* Toggle */ },
                badgeText = "ACTIVE"
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Section: Artwork Presentation (Vinyl vs Static)
        NothingSectionHeader(title = "ALBUM ARTWORK PRESENTATION")
        NothingCard(variant = NothingCardVariant.STANDARD) {
            Text(
                text = "Vinyl Record Animation",
                color = NothingTokens.NothingTextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Rotate album artwork smoothly like a classic vinyl disc during active playback",
                color = NothingTokens.NothingTextSecondary,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
            Spacer(modifier = Modifier.height(12.dp))

            val rotationOptions = listOf(true, false)
            NothingSegmentedControl(
                items = rotationOptions,
                selectedItem = musicPrefs.albumRotationEnabled,
                onItemSelected = { enabled ->
                    /* Repository update */
                },
                labelProvider = { enabled ->
                    if (enabled) "ROTATION ON (VINYL)" else "ROTATION OFF (STATIC)"
                }
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Section: Waveform Visualizer Style
        NothingSectionHeader(title = "WAVEFORM VISUALIZER")
        NothingCard(variant = NothingCardVariant.STANDARD) {
            Text(
                text = "Active Visualizer Style",
                color = NothingTokens.NothingTextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))

            val visualizerStyles = listOf(
                MusicVisualizerStyle.VERTICAL_EQUALIZER to "EQUALIZER",
                MusicVisualizerStyle.WAVEFORM to "WAVEFORM",
                MusicVisualizerStyle.CIRCULAR to "CIRCULAR",
                MusicVisualizerStyle.VINYL to "VINYL",
                MusicVisualizerStyle.MINIMAL_PULSE to "MINIMAL PULSE"
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                visualizerStyles.chunked(2).forEach { rowStyles: List<Pair<MusicVisualizerStyle, String>> ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowStyles.forEach { item: Pair<MusicVisualizerStyle, String> ->
                            val style = item.first
                            val name = item.second
                            val isSelected = musicPrefs.visualizerStyle == style
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(NothingTokens.ControlRadius))
                                    .background(if (isSelected) NothingTokens.NothingSurfaceMedium else NothingTokens.NothingSurfaceLowest)
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) NothingTokens.NothingBlue else NothingTokens.NothingBorderSubtle,
                                        shape = RoundedCornerShape(NothingTokens.ControlRadius)
                                    )
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        /* Repository update */
                                    }
                                    .padding(vertical = 10.dp, horizontal = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = name,
                                    color = if (isSelected) NothingTokens.NothingTextPrimary else NothingTokens.NothingTextSecondary,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                        if (rowStyles.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Section: Secondary Controls (From Reference Screenshots)
        NothingSectionHeader(title = "SECONDARY SETTINGS")
        NothingCard(variant = NothingCardVariant.STANDARD) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                NothingSwitchRow(
                    title = "Show Fast-Forward Buttons",
                    description = "Displays 10-second skip forward and rewind buttons in player",
                    checked = showFastForwardButtons,
                    onCheckedChange = { showFastForwardButtons = it },
                    badgeText = "REF UI"
                )

                NothingSwitchRow(
                    title = "Long Press : Volume Control",
                    description = "Press and hold notch, then swipe left/right to adjust volume",
                    checked = longPressVolumeEnabled,
                    onCheckedChange = { longPressVolumeEnabled = it },
                    badgeText = "GESTURE"
                )

                NothingSwitchRow(
                    title = "Album Photo as Player Bg",
                    description = "Render sampled blurred album artwork as player card background",
                    checked = albumPhotoAsBg,
                    onCheckedChange = { albumPhotoAsBg = it }
                )

                NothingSwitchRow(
                    title = "Only show while Playing Audio",
                    description = "Hide the Island pill during pauses or video playback",
                    checked = onlyShowWhilePlaying,
                    onCheckedChange = { onlyShowWhilePlaying = it }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
