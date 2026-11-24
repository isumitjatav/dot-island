package com.nothing.island.ai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nothing.island.ai.NothingBlack
import com.nothing.island.ai.NothingBorder
import com.nothing.island.ai.NothingMutedText
import com.nothing.island.ai.NothingWhite
import com.nothing.island.ai.media.MediaTrackState
import com.nothing.island.ai.media.MusicPreferences
import com.nothing.island.ai.switcher.SwipeDirection
import com.nothing.island.ai.ui.visualizers.MusicVisualizerHost

/**
 * Compact presentation for active media playback on Nothing Island.
 * Symmetrical 200dp capsule wrapping the centered front-facing camera:
 * [ Left: 73dp Artwork ] [ Center: 54dp Hole Clearance ] [ Right: 73dp Visualizer + Title ] = 200dp.
 */
@Composable
fun MusicCompactView(
    trackState: MediaTrackState,
    preferences: MusicPreferences = MusicPreferences.DEFAULTS,
    onTap: () -> Unit,
    onSwipe: (SwipeDirection) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val theme = com.nothing.island.ai.ui.theme.LocalIslandTheme.current
    val pillShape = RoundedCornerShape(18.dp)
    val accentColor = theme.accentColor

    var totalDrag by remember { mutableFloatStateOf(0f) }
    val swipeThresholdPx = 35f

    val progressFraction = if (trackState.durationMs > 0L) {
        (trackState.computeCurrentPosition().toFloat() / trackState.durationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val pillBorderColor = when {
        theme.isGlassmorphic -> Color(0x45FFFFFF)
        theme.isInverted -> Color(0xFFD0D0D0)
        else -> theme.borderColor
    }

    Box(
        modifier = modifier
            .width(200.dp)
            .height(36.dp)
            .clip(pillShape)
            .background(theme.pillBackgroundColor)
            .border(1.dp, pillBorderColor, pillShape)
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
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                onTap()
            },
        contentAlignment = Alignment.Center
    ) {
        // Live track progress border clockwise around compact pill when music is playing
        if (trackState.isPlaying) {
            com.nothing.island.ai.ui.theme.CompactMusicProgressBorder(
                progressFraction = progressFraction,
                accentColor = theme.compactProgressColor,
                cornerRadiusDp = 18.dp,
                modifier = Modifier.matchParentSize()
            )
        }
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Left Wing: 73dp (Album Artwork + Status)
            Box(
                modifier = Modifier
                    .width(73.dp)
                    .fillMaxHeight()
                    .padding(start = 8.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MusicArtworkView(
                        bitmap = trackState.artworkBitmap,
                        fallbackChar = trackState.trackTitle.firstOrNull() ?: 'M',
                        shape = preferences.artworkShape,
                        isPlaying = trackState.isPlaying,
                        rotationEnabled = preferences.albumRotationEnabled,
                        sizeDp = 24.dp,
                        accentColor = accentColor,
                        glowIntensity = preferences.glowIntensity
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    if (trackState.isPlaying) {
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(accentColor)
                        )
                    }
                }
            }

            // 2. Center Zone: 54dp empty clearance (camera punch-hole)
            Spacer(modifier = Modifier.width(54.dp))

            // 3. Right Wing: 73dp (Visualizer + Track title)
            Box(
                modifier = Modifier
                    .width(73.dp)
                    .fillMaxHeight()
                    .padding(end = 8.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    // Mini live visualizer
                    Box(
                        modifier = Modifier
                            .width(28.dp)
                            .height(16.dp)
                    ) {
                        MusicVisualizerHost(
                            style = preferences.visualizerStyle,
                            isPlaying = trackState.isPlaying,
                            accentColor = accentColor,
                            preferences = preferences
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // Concise track label
                    Text(
                        text = if (trackState.isPlaying) {
                            if (theme.id == "tape_deck") trackState.trackTitle.uppercase() else trackState.trackTitle
                        } else {
                            "PAUSED"
                        },
                        color = if (trackState.isPlaying) theme.textColorPrimary else theme.textColorSecondary,
                        fontSize = 10.sp,
                        fontFamily = theme.bodyFont,
                        fontWeight = theme.bodyFontWeight,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
