package com.nothing.island.ai.ui.components

import android.content.Context
import android.content.Intent
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nothing.island.ai.NothingBlack
import com.nothing.island.ai.NothingBorder
import com.nothing.island.ai.NothingDarkGray
import com.nothing.island.ai.NothingMutedText
import com.nothing.island.ai.NothingRed
import com.nothing.island.ai.NothingWhite
import com.nothing.island.ai.media.MediaTrackState
import com.nothing.island.ai.media.MusicPreferences
import com.nothing.island.ai.switcher.SwipeDirection
import com.nothing.island.ai.ui.visualizers.MusicVisualizerHost
import kotlinx.coroutines.delay

/**
 * Formats a duration in milliseconds to "m:ss" format.
 */
fun formatDurationMs(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}

/**
 * Safely launches the active media app via its sessionActivity PendingIntent,
 * falling back to getLaunchIntentForPackage.
 */
private fun launchActiveMediaApp(
    context: Context,
    trackState: MediaTrackState,
    onCollapse: () -> Unit
) {
    runCatching {
        var launched = false
        val sessionActivity = trackState.sessionActivity
        if (sessionActivity != null) {
            try {
                val options = android.app.ActivityOptions.makeBasic().toBundle()
                sessionActivity.send(context, 0, null, null, null, null, options)
                launched = true
            } catch (e: Exception) {
                Log.w("MusicExpandedView", "PendingIntent send failed, falling back to launch intent", e)
            }
        }
        if (!launched && trackState.packageName.isNotBlank()) {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(trackState.packageName)?.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (launchIntent != null) {
                context.startActivity(launchIntent)
                launched = true
            }
        }
        if (launched) {
            onCollapse()
        }
    }.onFailure { e ->
        Log.e("MusicExpandedView", "Failed to launch media app", e)
    }
}

/**
 * State C: Music Expanded Player
 *
 * Full-featured interactive music player presentation within Nothing Island's 350dp x 210dp window.
 * Strictly adheres to Nothing OS dot-matrix and monochrome design ethos.
 */
@Composable
fun MusicExpandedView(
    trackState: MediaTrackState,
    preferences: MusicPreferences = MusicPreferences.DEFAULTS,
    onPlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onCollapse: () -> Unit,
    onSwipe: (SwipeDirection) -> Unit = {},
    onToggleRepeat: (Int) -> Unit = {},
    topClearanceDp: androidx.compose.ui.unit.Dp = 48.dp,
    modifier: Modifier = Modifier,
    isCameraCutoutEnabled: Boolean = false
) {
    val context = LocalContext.current
    val cardShape = if (isCameraCutoutEnabled) notchedCardShape() else RoundedCornerShape(26.dp)
    val accentColor = preferences.resolveAccent(trackState.dominantColor)

    var totalDrag by remember { mutableFloatStateOf(0f) }
    val swipeThresholdPx = 40f

    // Live position interpolation
    var currentSeekPosition by remember(trackState.currentPositionMs, trackState.isPlaying) {
        mutableLongStateOf(trackState.currentPositionMs)
    }
    var isUserScrubbing by remember { mutableStateOf(false) }
    var scrubFraction by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(trackState.isPlaying, trackState.lastPositionUpdateTime) {
        if (trackState.isPlaying && !isUserScrubbing) {
            while (true) {
                currentSeekPosition = trackState.computeCurrentPosition()
                delay(200)
            }
        }
    }

    val displayPositionMs = if (isUserScrubbing) {
        (scrubFraction * trackState.durationMs).toLong()
    } else {
        currentSeekPosition
    }

    val theme = com.nothing.island.ai.ui.theme.LocalIslandTheme.current

    val buttonBg = when {
        theme.isGlassmorphic -> Color(0xB3333333)
        theme.isInverted -> Color(0xFF000000)
        theme == com.nothing.island.ai.ui.theme.IslandSignatureTheme.GLYPH_ARC -> Color(0xFF1C1C1C)
        else -> NothingDarkGray
    }
    val buttonBorder = when {
        theme.isGlassmorphic -> Color(0x28FFFFFF)
        theme == com.nothing.island.ai.ui.theme.IslandSignatureTheme.GLYPH_ARC -> Color(0xFF333333)
        else -> Color.Transparent
    }
    val buttonIconColor = when {
        theme.isInverted -> Color(0xFFFFFFFF)
        else -> NothingWhite
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(cardShape)
            .background(theme.pillBackgroundColor)
            .border(
                1.dp,
                if (trackState.isPlaying && preferences.glowIntensity > 0f) {
                    accentColor.copy(alpha = 0.35f * preferences.glowIntensity)
                } else {
                    theme.borderColor
                },
                cardShape
            )
            .pointerInput(Unit) {
                var dragX = 0f
                var dragY = 0f
                detectDragGestures(
                    onDragStart = {
                        dragX = 0f
                        dragY = 0f
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragX += dragAmount.x
                        dragY += dragAmount.y
                    },
                    onDragEnd = {
                        val absX = kotlin.math.abs(dragX)
                        val absY = kotlin.math.abs(dragY)
                        if (absY > absX && dragY > swipeThresholdPx) {
                            onSwipe(SwipeDirection.DOWN)
                        } else if (absY > absX && dragY < -swipeThresholdPx) {
                            onCollapse()
                        } else if (absX > absY && dragX > swipeThresholdPx) {
                            onSwipe(SwipeDirection.RIGHT)
                        } else if (absX > absY && dragX < -swipeThresholdPx) {
                            onSwipe(SwipeDirection.LEFT)
                        }
                        dragX = 0f
                        dragY = 0f
                    },
                    onDragCancel = {
                        dragX = 0f
                        dragY = 0f
                    }
                )
            }
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        // REQ-18: Blurred album photo as player background
        if (preferences.backgroundBlurEnabled && trackState.artworkBitmap != null) {
            Image(
                bitmap = trackState.artworkBitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(28.dp)
                    .alpha(0.25f)
            )
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. Top Punch-hole clearance spacer (36dp camera punch hole + 6dp safety margin)
            val effectiveTopClearance = 42.dp
            Spacer(modifier = Modifier.height(effectiveTopClearance))

            // 2. Header Row: Source app, Audio device output, and Collapse chip
            val outputPillBg = when {
                theme.isInverted -> Color(0xFFE4E4E4)
                theme.isGlassmorphic -> Color(0xB3333333)
                else -> NothingDarkGray
            }
            val outputPillText = when {
                theme.isInverted -> Color(0xFF000000)
                else -> NothingMutedText
            }
            val collapseBg = when {
                theme.isInverted -> Color(0xFFE4E4E4)
                theme.isGlassmorphic -> Color(0xB3333333)
                else -> NothingDarkGray
            }
            val collapseText = when {
                theme.isInverted -> Color(0xFF000000)
                else -> NothingWhite
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = if (trackState.isHistorical) "AUDIO STANDBY" else trackState.appDisplayName.uppercase(),
                        color = theme.textColorPrimary,
                        fontSize = 11.5.sp,
                        lineHeight = 15.sp,
                        fontFamily = theme.bodyFont,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    // Audio output pill
                    Box(
                        modifier = Modifier
                            .widthIn(max = 104.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(outputPillBg)
                            .then(if (theme.isGlassmorphic) Modifier.border(1.dp, Color(0x28FFFFFF), RoundedCornerShape(6.dp)) else if (theme.isInverted) Modifier.border(1.dp, Color(0xFFCCCCCC), RoundedCornerShape(6.dp)) else Modifier)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = trackState.audioOutputName.uppercase(),
                            color = outputPillText,
                            fontSize = 8.5.sp,
                            lineHeight = 12.sp,
                            fontFamily = theme.bodyFont,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(collapseBg)
                        .then(if (theme.isGlassmorphic) Modifier.border(1.dp, Color(0x28FFFFFF), RoundedCornerShape(8.dp)) else if (theme.isInverted) Modifier.border(1.dp, Color(0xFFCCCCCC), RoundedCornerShape(8.dp)) else Modifier)
                        .clickable { onCollapse() }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = "COLLAPSE",
                        color = collapseText,
                        fontSize = 10.5.sp,
                        fontFamily = theme.bodyFont,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            // 3. Body Row: Artwork + Track Details + Mini Visualizer
            val bodyBg = when {
                theme.isGlassmorphic -> Color(0xB3333333)
                theme.isInverted -> Color(0xFFE5E5E5)
                theme == com.nothing.island.ai.ui.theme.IslandSignatureTheme.TAPE_DECK -> Color(0xFF262626)
                else -> NothingDarkGray
            }
            val bodyBorder = when {
                theme.isGlassmorphic -> Color(0x28FFFFFF)
                theme.isInverted -> Color(0xFFCCCCCC)
                else -> Color.Transparent
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(bodyBg)
                    .then(if (bodyBorder != Color.Transparent) Modifier.border(1.dp, bodyBorder, RoundedCornerShape(12.dp)) else Modifier)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Album Artwork
                if (theme == com.nothing.island.ai.ui.theme.IslandSignatureTheme.TAPE_DECK) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFE5E5E5))
                            .border(1.dp, Color(0xFFCCCCCC), RoundedCornerShape(8.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                launchActiveMediaApp(context, trackState, onCollapse)
                            }
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        MusicArtworkView(
                            bitmap = trackState.artworkBitmap,
                            fallbackChar = TextBreakUtils.extractSafeFallbackChar(trackState.trackTitle, 'M'),
                            shape = preferences.artworkShape,
                            isPlaying = trackState.isPlaying,
                            rotationEnabled = preferences.albumRotationEnabled,
                            sizeDp = 40.dp,
                            accentColor = theme.accentColor,
                            glowIntensity = preferences.glowIntensity
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            launchActiveMediaApp(context, trackState, onCollapse)
                        }
                    ) {
                        MusicArtworkView(
                            bitmap = trackState.artworkBitmap,
                            fallbackChar = TextBreakUtils.extractSafeFallbackChar(trackState.trackTitle, 'M'),
                            shape = preferences.artworkShape,
                            isPlaying = trackState.isPlaying,
                            rotationEnabled = preferences.albumRotationEnabled,
                            sizeDp = 48.dp,
                            accentColor = accentColor,
                            glowIntensity = preferences.glowIntensity
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Track title & Artist
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            launchActiveMediaApp(context, trackState, onCollapse)
                        },
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = trackState.trackTitle.ifBlank { "No Track Playing" },
                        color = theme.textColorPrimary,
                        fontSize = 13.5.sp,
                        fontFamily = theme.displayFont,
                        fontWeight = theme.displayFontWeight,
                        lineHeight = 19.sp,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = when {
                            trackState.artistName.isNotBlank() && trackState.albumName.isNotBlank() ->
                                "${trackState.artistName} • ${trackState.albumName}"
                            trackState.artistName.isNotBlank() -> trackState.artistName
                            trackState.albumName.isNotBlank() -> trackState.albumName
                            else -> trackState.appDisplayName
                        },
                        color = theme.textColorSecondary,
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        fontFamily = theme.bodyFont,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (trackState.isHistorical) {
                        Text(
                            text = "(LAST KNOWN PLAYBACK)",
                            color = Color(0xFFE5A93C),
                            fontSize = 8.5.sp,
                            fontFamily = theme.bodyFont,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Mini beat visualizer inside card
                Box(
                    modifier = Modifier
                        .requiredSize(width = 38.dp, height = 24.dp)
                        .padding(start = 4.dp)
                ) {
                    MusicVisualizerHost(
                        style = preferences.visualizerStyle,
                        isPlaying = trackState.isPlaying,
                        accentColor = accentColor,
                        preferences = preferences
                    )
                }
            }

            // 4. Scrubber & Timeline Bar
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                // Timeline elapsed / remaining
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatDurationMs(displayPositionMs),
                        color = theme.textColorSecondary,
                        fontSize = 9.sp,
                        fontFamily = theme.displayFont
                    )
                    Text(
                        text = formatDurationMs(trackState.durationMs),
                        color = theme.textColorSecondary,
                        fontSize = 9.sp,
                        fontFamily = theme.displayFont
                    )
                }

                // Interactive Scrubber Slider or Segmented LED
                val sliderPosition = if (trackState.durationMs > 0L) {
                    (displayPositionMs.toFloat() / trackState.durationMs.toFloat()).coerceIn(0f, 1f)
                } else 0f

                if (theme == com.nothing.island.ai.ui.theme.IslandSignatureTheme.GLYPH_ARC) {
                    com.nothing.island.ai.ui.theme.SegmentedLedProgressBar(
                        progress = sliderPosition,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(18.dp)
                            .padding(vertical = 4.dp),
                        activeColor = Color.White
                    )
                } else {
                    Slider(
                        value = if (isUserScrubbing) scrubFraction else sliderPosition,
                        onValueChange = { frac ->
                            isUserScrubbing = true
                            scrubFraction = frac
                        },
                        onValueChangeFinished = {
                            isUserScrubbing = false
                            val targetMs = (scrubFraction * trackState.durationMs).toLong()
                            onSeekTo(targetMs)
                        },
                        enabled = trackState.durationMs > 0L && trackState.canSeek && !trackState.isHistorical,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(18.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = if (theme == com.nothing.island.ai.ui.theme.IslandSignatureTheme.TAPE_DECK) Color(0xFFD71921) else accentColor,
                            activeTrackColor = if (theme == com.nothing.island.ai.ui.theme.IslandSignatureTheme.TAPE_DECK) Color(0xFFD71921) else accentColor,
                            inactiveTrackColor = Color(0xFF262626)
                        )
                    )
                }
            }

            // 5. Playback Transport Controls Row (REQ-19: Fast-forward / rewind controls)
            val isSeekable = trackState.canSeek && trackState.durationMs > 0L && !trackState.isHistorical
            val showFastForwardControls = preferences.fastForwardButtonsEnabled && isSeekable
            val canRewind = isSeekable && displayPositionMs > 0L
            val canFastForward = isSeekable && displayPositionMs < trackState.durationMs
            val canSkipPrev = (trackState.canSkipPrevious || isSeekable) && !trackState.isHistorical

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 10s Rewind Button (REQ-19: Only shown when session supports seeking with known duration)
                if (showFastForwardControls) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(NothingDarkGray)
                            .clickable(enabled = canRewind) {
                                val target = (displayPositionMs - 10000L).coerceAtLeast(0L)
                                onSeekTo(target)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "-10",
                            color = if (canRewind) NothingWhite else NothingMutedText,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))
                }

                val playPauseBg = when {
                    trackState.isPlaying -> theme.accentColor
                    theme == com.nothing.island.ai.ui.theme.IslandSignatureTheme.GLYPH_ARC -> Color(0xFF1C1C1C)
                    theme == com.nothing.island.ai.ui.theme.IslandSignatureTheme.HIGH_CONTRAST_INVERTED -> Color(0xFF000000)
                    theme == com.nothing.island.ai.ui.theme.IslandSignatureTheme.TAPE_DECK -> Color(0xFFE5E5E5)
                    theme.isGlassmorphic -> Color(0xB3333333)
                    else -> NothingWhite
                }
                val playPauseIconColor = when {
                    trackState.isPlaying -> NothingWhite
                    theme == com.nothing.island.ai.ui.theme.IslandSignatureTheme.GLYPH_ARC -> Color.White
                    theme == com.nothing.island.ai.ui.theme.IslandSignatureTheme.HIGH_CONTRAST_INVERTED -> Color.White
                    theme == com.nothing.island.ai.ui.theme.IslandSignatureTheme.TAPE_DECK -> Color(0xFF1E1E1E)
                    theme.isGlassmorphic -> Color.White
                    else -> NothingBlack
                }

                // Previous button
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(buttonBg)
                        .then(if (buttonBorder != Color.Transparent) Modifier.border(1.dp, buttonBorder, CircleShape) else Modifier)
                        .clickable(enabled = canSkipPrev) {
                            onSkipPrevious()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "⏮",
                        color = if (canSkipPrev) buttonIconColor else NothingMutedText,
                        fontSize = 13.sp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Play / Pause button
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(playPauseBg)
                        .then(if (buttonBorder != Color.Transparent) Modifier.border(1.5.dp, buttonBorder, CircleShape) else Modifier)
                        .clickable(enabled = !trackState.isHistorical) {
                            onPlayPause()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (trackState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (trackState.isPlaying) "Pause" else "Play",
                        tint = playPauseIconColor,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Next button
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(buttonBg)
                        .then(if (buttonBorder != Color.Transparent) Modifier.border(1.dp, buttonBorder, CircleShape) else Modifier)
                        .clickable(enabled = trackState.canSkipNext && !trackState.isHistorical) {
                            onSkipNext()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "⏭",
                        color = if (trackState.canSkipNext && !trackState.isHistorical) buttonIconColor else NothingMutedText,
                        fontSize = 13.sp
                    )
                }

                // 10s Fast-Forward Button (REQ-19: Only shown when session supports seeking with known duration)
                if (showFastForwardControls) {
                    Spacer(modifier = Modifier.width(10.dp))

                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(NothingDarkGray)
                            .clickable(enabled = canFastForward) {
                                val target = (displayPositionMs + 10000L).coerceAtMost(trackState.durationMs)
                                onSeekTo(target)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "+10",
                            color = if (canFastForward) NothingWhite else NothingMutedText,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Loop / Repeat Button
                val isRepeatActive = trackState.repeatMode == PlaybackStateCompat.REPEAT_MODE_ALL ||
                        trackState.repeatMode == PlaybackStateCompat.REPEAT_MODE_ONE

                val repeatBg = when {
                    isRepeatActive -> theme.accentColor.copy(alpha = 0.18f)
                    theme.isGlassmorphic -> Color(0xB3333333)
                    theme.isInverted -> Color(0xFFE4E4E4)
                    theme == com.nothing.island.ai.ui.theme.IslandSignatureTheme.GLYPH_ARC -> Color(0xFF1C1C1C)
                    else -> NothingDarkGray
                }
                val repeatBorder = when {
                    isRepeatActive -> theme.accentColor.copy(alpha = 0.5f)
                    theme.isGlassmorphic -> Color(0x28FFFFFF)
                    theme == com.nothing.island.ai.ui.theme.IslandSignatureTheme.GLYPH_ARC -> Color(0xFF333333)
                    else -> Color.Transparent
                }

                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(repeatBg)
                        .then(if (repeatBorder != Color.Transparent) Modifier.border(1.dp, repeatBorder, CircleShape) else Modifier)
                        .clickable(enabled = !trackState.isHistorical) {
                            val nextRepeatMode = when (trackState.repeatMode) {
                                PlaybackStateCompat.REPEAT_MODE_NONE -> PlaybackStateCompat.REPEAT_MODE_ALL
                                PlaybackStateCompat.REPEAT_MODE_ALL -> PlaybackStateCompat.REPEAT_MODE_ONE
                                PlaybackStateCompat.REPEAT_MODE_ONE -> PlaybackStateCompat.REPEAT_MODE_NONE
                                else -> PlaybackStateCompat.REPEAT_MODE_ALL
                            }
                            onToggleRepeat(nextRepeatMode)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (trackState.repeatMode == PlaybackStateCompat.REPEAT_MODE_ONE) {
                            Icons.Rounded.RepeatOne
                        } else {
                            Icons.Rounded.Repeat
                        },
                        contentDescription = when (trackState.repeatMode) {
                            PlaybackStateCompat.REPEAT_MODE_ONE -> "Repeat One"
                            PlaybackStateCompat.REPEAT_MODE_ALL -> "Repeat All"
                            else -> "Repeat Off"
                        },
                        tint = if (isRepeatActive) theme.accentColor else theme.textColorSecondary.copy(alpha = 0.45f),
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
        }
    }
}
