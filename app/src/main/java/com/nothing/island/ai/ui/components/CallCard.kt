package com.nothing.island.ai.ui.components

import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CallEnd
import androidx.compose.material.icons.rounded.Dialpad
import androidx.compose.material.icons.rounded.FiberManualRecord
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nothing.island.ai.engine.IslandEvent
import com.nothing.island.ai.ui.theme.NothingSecondaryText

/** Formats an elapsed call duration as mm:ss. Pure and unit-testable. */
fun formatCallDuration(totalSeconds: Long): String {
    val safe = totalSeconds.coerceAtLeast(0L)
    return "%02d:%02d".format(safe / 60, safe % 60)
}

/** Pure UI state for expandable in-call controls & DTMF keypad. */
data class CallControlsUiState(
    val controlsExpanded: Boolean = true,
    val numpadExpanded: Boolean = false,
    val dtmfDigits: String = "",
) {
    fun toggleControls(): CallControlsUiState {
        val next = !controlsExpanded
        return copy(
            controlsExpanded = next,
            numpadExpanded = if (!next) false else numpadExpanded,
        )
    }

    fun toggleNumpad(): CallControlsUiState =
        copy(
            controlsExpanded = if (!numpadExpanded) true else controlsExpanded,
            numpadExpanded = !numpadExpanded,
        )

    fun appendDtmfDigit(digit: Char): CallControlsUiState {
        if (digit !in DTMF_VALID_CHARS || dtmfDigits.length >= MAX_DTMF_BUFFER) return this
        return copy(dtmfDigits = dtmfDigits + digit)
    }

    fun backspaceDtmf(): CallControlsUiState =
        if (dtmfDigits.isEmpty()) this else copy(dtmfDigits = dtmfDigits.dropLast(1))

    fun clearDtmf(): CallControlsUiState =
        if (dtmfDigits.isEmpty()) this else copy(dtmfDigits = "")

    companion object {
        const val DTMF_VALID_CHARS = "0123456789*#"
        const val MAX_DTMF_BUFFER = 24
    }
}

/** Maps a keypad character ('0'..'9', '*', '#') to an Android ToneGenerator DTMF constant. */
fun dtmfToneForChar(digit: Char): Int = when (digit) {
    '0' -> ToneGenerator.TONE_DTMF_0
    '1' -> ToneGenerator.TONE_DTMF_1
    '2' -> ToneGenerator.TONE_DTMF_2
    '3' -> ToneGenerator.TONE_DTMF_3
    '4' -> ToneGenerator.TONE_DTMF_4
    '5' -> ToneGenerator.TONE_DTMF_5
    '6' -> ToneGenerator.TONE_DTMF_6
    '7' -> ToneGenerator.TONE_DTMF_7
    '8' -> ToneGenerator.TONE_DTMF_8
    '9' -> ToneGenerator.TONE_DTMF_9
    '*' -> ToneGenerator.TONE_DTMF_S
    '#' -> ToneGenerator.TONE_DTMF_P
    else -> ToneGenerator.TONE_DTMF_0
}

/**
 * State 1: Compact Ongoing / Incoming Call Pill.
 * Clean single capsule (~200.dp wide, centered on 36.dp camera cutout dead-zone).
 * Left of cutout: crisp green phone vector icon (Icons.Rounded.Call, #00C853) + live green tnum timer (00:05).
 * Right of cutout: animated green voice waveform equalizer bars (#00C853).
 * Symmetrical, zero external red button clutter, perfectly matching Nothing OS design.
 */
@Composable
fun CallCompactView(
    event: IslandEvent,
    onTap: () -> Unit,
    onEndCall: (() -> Unit)? = null,
    showEndButton: Boolean = true,
    isSpeakerOn: Boolean = false,
    onToggleSpeaker: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val isRinging = event is IslandEvent.IncomingCallRinging
    val isDialing = (event as? IslandEvent.CallConnected)?.isDialing == true
    val durationSeconds = (event as? IslandEvent.CallConnected)?.durationSeconds ?: 0L
    val contactPhoto: android.graphics.Bitmap? = when (event) {
        is IslandEvent.IncomingCallRinging -> event.contactPhoto
        is IslandEvent.CallConnected -> event.contactPhoto
        else -> null
    }

    val pulse = rememberInfiniteTransition(label = "compactCallWaveform")
    val waveHeights = listOf(
        pulse.animateFloat(
            initialValue = 4f, targetValue = 13f,
            animationSpec = infiniteRepeatable(tween(420, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "wave1"
        ),
        pulse.animateFloat(
            initialValue = 11f, targetValue = 5f,
            animationSpec = infiniteRepeatable(tween(360, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "wave2"
        ),
        pulse.animateFloat(
            initialValue = 6f, targetValue = 15f,
            animationSpec = infiniteRepeatable(tween(480, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "wave3"
        ),
        pulse.animateFloat(
            initialValue = 12f, targetValue = 6f,
            animationSpec = infiniteRepeatable(tween(390, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "wave4"
        ),
        pulse.animateFloat(
            initialValue = 5f, targetValue = 11f,
            animationSpec = infiniteRepeatable(tween(440, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "wave5"
        )
    )

    val dialPulseAlpha by pulse.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(tween(750, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "dialPulse"
    )

    val theme = com.nothing.island.ai.ui.theme.LocalIslandTheme.current
    val greenCallColor = Color(0xFF00C853)

    Box(
        modifier = modifier
            .width(200.dp)
            .height(36.dp)
            .clip(CircleShape)
            .background(theme.pillBackgroundColor)
            .border(
                1.dp,
                if (theme.isGlassmorphic) Color(0x45FFFFFF) else if (theme.isInverted) Color(0xFFD0D0D0) else theme.borderColor,
                CircleShape
            )
            .clickable(onClick = onTap)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left of camera cutout: Circular caller photo (24dp) when saved, else green phone icon + timer
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                if (contactPhoto != null) {
                    Image(
                        bitmap = contactPhoto.asImageBitmap(),
                        contentDescription = "Caller",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .requiredSize(24.dp)
                            .clip(CircleShape)
                            .border(1.dp, Color(0x40FFFFFF), CircleShape)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.Call,
                        contentDescription = "Call",
                        tint = greenCallColor,
                        modifier = Modifier.requiredSize(15.dp)
                    )
                }
                Text(
                    text = when {
                        isRinging -> if ((event as? IslandEvent.IncomingCallRinging)?.isVideoCall == true) "VIDEO" else "CALL"
                        isDialing -> "CALLING..."
                        else -> formatCallDuration(durationSeconds)
                    },
                    color = if (isDialing) greenCallColor.copy(alpha = dialPulseAlpha) else greenCallColor,
                    fontSize = if (isDialing) 10.sp else 12.sp,
                    lineHeight = 16.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Center 40dp punch-hole clearance dead-zone
            Spacer(modifier = Modifier.width(40.dp))

            // Right of camera cutout: Animated green voice waveform bars (subtle pulse while dialing)
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp, Alignment.End)
            ) {
                waveHeights.forEach { animatedHeight ->
                    Box(
                        modifier = Modifier
                            .width(2.5.dp)
                            .height(if (isDialing) (animatedHeight.value * 0.6f).dp else animatedHeight.value.dp)
                            .clip(RoundedCornerShape(1.25.dp))
                            .background(if (isDialing) greenCallColor.copy(alpha = dialPulseAlpha) else greenCallColor)
                    )
                }
            }
        }
    }
}

/**
 * State 2 & 3: Expanded Call Capsule (344.dp x 78.dp, CircleShape / stadium capsule).
 * Matches authentic reference layout:
 * - Left: 48.dp circular caller photo or gradient initials avatar.
 * - Center: "Incoming Call" / live duration timer ("00:01") + bold caller name.
 * - Right (44.dp circles, 10.dp gap):
 *     - Ringing: Red Decline (#FF3B30) + Green Answer (#34C759).
 *     - Connected / Dialing: Speaker toggle (#3A3A3C / #FFFFFF active) + Red Hang-up (#FF3B30).
 */
@Composable
fun CallExpandedView(
    event: IslandEvent,
    elapsedSeconds: Long,
    onAnswer: () -> Unit,
    onDecline: () -> Unit,
    onToggleSpeaker: (() -> Unit)? = null,
    isSpeakerOn: Boolean = false,
    modifier: Modifier = Modifier,
    isCameraCutoutEnabled: Boolean = false,
    isMuted: Boolean = false,
    onToggleMute: (() -> Unit)? = null,
    isOnHold: Boolean = false,
    onToggleHold: (() -> Unit)? = null,
    isRecording: Boolean = false,
    onToggleRecord: (() -> Unit)? = null,
    onOpenDialerScreen: (() -> Unit)? = null,
    controlsExpanded: Boolean? = null,
    onToggleControlsExpanded: (() -> Unit)? = null,
    numpadExpanded: Boolean? = null,
    onToggleNumpadExpanded: (() -> Unit)? = null,
    showMascot: Boolean = false,
    mascotSpriteName: String = "DOTCAT",
    reducedMotionEnabled: Boolean = false,
) {
    val isRinging = event is IslandEvent.IncomingCallRinging
    val isEnded = event is IslandEvent.CallEnded
    val isDialing = (event as? IslandEvent.CallConnected)?.isDialing == true
    val caller = when (event) {
        is IslandEvent.IncomingCallRinging -> event.callerDisplayName
        is IslandEvent.CallConnected -> event.callerDisplayName
        is IslandEvent.CallEnded -> "Call Ended"
        else -> "Call"
    }

    val contactBitmap: android.graphics.Bitmap? = when (event) {
        is IslandEvent.IncomingCallRinging -> event.contactPhoto
        is IslandEvent.CallConnected -> event.contactPhoto
        else -> null
    }

    val theme = com.nothing.island.ai.ui.theme.LocalIslandTheme.current
    val haptics = LocalHapticFeedback.current
    val greenCallColor = Color(0xFF22C55E)
    val answerGreen = Color(0xFF22C55E)
    val declineRed = Color(0xFFD71921)
    val controlSurface = Color(0xFF141414)
    val controlBorder = Color(0x28FFFFFF)

    var localUiState by remember(event.id) {
        mutableStateOf(
            CallControlsUiState(
                controlsExpanded = !isRinging && !isEnded,
                numpadExpanded = false,
                dtmfDigits = "",
            )
        )
    }
    val effectiveControlsExpanded = !isEnded && (controlsExpanded ?: localUiState.controlsExpanded)
    val effectiveNumpadExpanded = !isEnded && effectiveControlsExpanded && (numpadExpanded ?: localUiState.numpadExpanded)

    val toneGenerator = remember {
        runCatching { ToneGenerator(AudioManager.STREAM_DTMF, 80) }.getOrNull()
    }
    DisposableEffect(Unit) {
        onDispose {
            runCatching { toneGenerator?.release() }
        }
    }

    val topClearance = if (isCameraCutoutEnabled) 42.dp else 12.dp
    val cornerRadius = if (effectiveControlsExpanded || effectiveNumpadExpanded) 30.dp else if (isCameraCutoutEnabled) 36.dp else 39.dp

    Column(
        modifier = modifier
            .width(344.dp)
            .clip(RoundedCornerShape(cornerRadius))
            .background(theme.pillBackgroundColor)
            .border(
                width = 1.dp,
                color = if (theme.isGlassmorphic) Color(0x45FFFFFF) else if (theme.isInverted) Color(0xFFD0D0D0) else theme.borderColor,
                shape = RoundedCornerShape(cornerRadius)
            )
            .animateContentSize(
                animationSpec = spring(dampingRatio = 0.78f, stiffness = 420f)
            )
            .padding(
                top = topClearance,
                start = 14.dp,
                end = 14.dp,
                bottom = if (isEnded || !effectiveControlsExpanded) 12.dp else 14.dp
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: 44.dp circular caller photo with 1.dp border / monochrome dot-matrix initials avatar
            Box(
                modifier = Modifier
                    .requiredSize(44.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1A1A1D))
                    .border(1.dp, Color(0x38FFFFFF), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (contactBitmap != null) {
                    Image(
                        bitmap = contactBitmap.asImageBitmap(),
                        contentDescription = caller,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    val initials = TextBreakUtils.extractAvatarInitials(caller, maxInitials = 2, fallback = "C")
                    Text(
                        text = initials,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false,
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            // Center: Status / Timer (+ ON HOLD badge) + Caller Name
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    when {
                        isRinging -> {
                            Text(
                                text = if ((event as? IslandEvent.IncomingCallRinging)?.isVideoCall == true) "Incoming Video" else "Incoming Call",
                                color = theme.textColorSecondary,
                                fontSize = 12.sp,
                                lineHeight = 16.sp,
                                fontFamily = theme.bodyFont,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        isDialing -> {
                            val dialTransition = rememberInfiniteTransition(label = "expandedDialPulse")
                            val dialAlpha by dialTransition.animateFloat(
                                initialValue = 0.35f,
                                targetValue = 1f,
                                animationSpec = infiniteRepeatable(tween(750, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                                label = "expandedDialDot"
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(greenCallColor.copy(alpha = dialAlpha))
                                )
                                Text(
                                    text = "CALLING...",
                                    color = greenCallColor.copy(alpha = dialAlpha),
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                        isEnded -> {
                            Text(
                                text = "DISCONNECTED",
                                color = declineRed,
                                fontSize = 11.sp,
                                lineHeight = 15.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                        else -> {
                            Text(
                                text = formatCallDuration(elapsedSeconds),
                                color = if (isOnHold) theme.textColorSecondary else greenCallColor,
                                fontSize = 13.sp,
                                lineHeight = 17.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    if (isOnHold && !isEnded) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(declineRed)
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "ON HOLD",
                                color = Color.White,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }

                Spacer(Modifier.height(2.dp))

                Text(
                    text = caller,
                    color = theme.textColorPrimary,
                    fontSize = 17.sp,
                    lineHeight = 22.sp,
                    fontFamily = theme.displayFont,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.width(8.dp))

            // Right: Smiling Chibi (when mascot enabled) above Controls toggle + Decline/Answer or Hang-up
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                if (showMascot && !isEnded) {
                    ChibiExpandedCallHeaderBadge(
                        spriteName = mascotSpriteName,
                        reducedMotionEnabled = reducedMotionEnabled,
                    )
                    Spacer(Modifier.height(2.dp))
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (!isEnded) {
                        Box(
                            modifier = Modifier
                                .requiredSize(34.dp)
                                .clip(CircleShape)
                                .background(controlSurface)
                                .border(1.dp, controlBorder, CircleShape)
                                .clickable {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    if (onToggleControlsExpanded != null) {
                                        onToggleControlsExpanded()
                                    } else {
                                        localUiState = localUiState.toggleControls()
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (effectiveControlsExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                                contentDescription = if (effectiveControlsExpanded) "Hide Call Controls" else "Show Call Controls",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    if (isRinging) {
                        Box(
                            modifier = Modifier
                                .requiredSize(42.dp)
                                .clip(CircleShape)
                                .background(declineRed)
                                .clickable(onClick = onDecline),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CallEnd,
                                contentDescription = "Decline Call",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .requiredSize(42.dp)
                                .clip(CircleShape)
                                .background(answerGreen)
                                .clickable(onClick = onAnswer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Call,
                                contentDescription = "Answer Call",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    } else if (!isEnded) {
                        Box(
                            modifier = Modifier
                                .requiredSize(42.dp)
                                .clip(CircleShape)
                                .background(declineRed)
                                .clickable(onClick = onDecline),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CallEnd,
                                contentDescription = "Hang up Call",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        // Expandable Primary Action Grid: Mute, Speaker, Hold, Numpad, Record
        AnimatedVisibility(
            visible = effectiveControlsExpanded,
            enter = expandVertically(spring(dampingRatio = 0.8f, stiffness = 420f)) + fadeIn(tween(150)),
            exit = shrinkVertically(spring(dampingRatio = 0.85f, stiffness = 480f)) + fadeOut(tween(110))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    InCallDotMatrixPill(
                        label = if (isMuted) "UNMUTE" else "MUTE",
                        icon = if (isMuted) Icons.Rounded.MicOff else Icons.Rounded.Mic,
                        isActive = isMuted,
                        accentColor = declineRed,
                        surfaceColor = controlSurface,
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onToggleMute?.invoke()
                        },
                        modifier = Modifier.weight(1f)
                    )
                    InCallDotMatrixPill(
                        label = "SPEAKER",
                        icon = Icons.AutoMirrored.Rounded.VolumeUp,
                        isActive = isSpeakerOn,
                        accentColor = declineRed,
                        surfaceColor = controlSurface,
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onToggleSpeaker?.invoke()
                        },
                        modifier = Modifier.weight(1f)
                    )
                    InCallDotMatrixPill(
                        label = if (isOnHold) "RESUME" else "HOLD",
                        icon = if (isOnHold) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                        isActive = isOnHold,
                        accentColor = declineRed,
                        surfaceColor = controlSurface,
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onToggleHold?.invoke()
                        },
                        modifier = Modifier.weight(1f)
                    )
                    InCallDotMatrixPill(
                        label = "KEYPAD",
                        icon = Icons.Rounded.Dialpad,
                        isActive = effectiveNumpadExpanded,
                        accentColor = declineRed,
                        surfaceColor = controlSurface,
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            if (onToggleNumpadExpanded != null) {
                                onToggleNumpadExpanded()
                            } else {
                                localUiState = localUiState.toggleNumpad()
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                    InCallDotMatrixPill(
                        label = if (isRecording) "REC ON" else "RECORD",
                        icon = Icons.Rounded.FiberManualRecord,
                        isActive = isRecording,
                        accentColor = declineRed,
                        surfaceColor = controlSurface,
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onToggleRecord?.invoke()
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Inline 3x4 DTMF Numpad (1-9, *, 0, #) + Monospace Readout & Backspace
                AnimatedVisibility(
                    visible = effectiveNumpadExpanded,
                    enter = expandVertically(spring(dampingRatio = 0.8f, stiffness = 400f)) + fadeIn(tween(150)),
                    exit = shrinkVertically(spring(dampingRatio = 0.85f, stiffness = 480f)) + fadeOut(tween(100))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(controlSurface)
                            .border(1.dp, controlBorder, RoundedCornerShape(18.dp))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.Black)
                                .border(1.dp, Color(0x1FFFFFFF), RoundedCornerShape(10.dp))
                                .padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = localUiState.dtmfDigits.ifEmpty { "ENTER DTMF TONES..." },
                                color = if (localUiState.dtmfDigits.isEmpty()) Color(0x66FFFFFF) else Color.White,
                                fontSize = if (localUiState.dtmfDigits.isEmpty()) 11.sp else 15.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            if (localUiState.dtmfDigits.isNotEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .requiredSize(28.dp)
                                        .clip(CircleShape)
                                        .clickable {
                                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            localUiState = localUiState.backspaceDtmf()
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.Backspace,
                                        contentDescription = "Backspace digit",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            if (onOpenDialerScreen != null) {
                                Spacer(Modifier.width(4.dp))
                                Box(
                                    modifier = Modifier
                                        .requiredSize(28.dp)
                                        .clip(CircleShape)
                                        .clickable { onOpenDialerScreen() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.OpenInNew,
                                        contentDescription = "Open Phone Screen",
                                        tint = declineRed,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                        }

                        val keyRows = listOf(
                            listOf('1', '2', '3'),
                            listOf('4', '5', '6'),
                            listOf('7', '8', '9'),
                            listOf('*', '0', '#')
                        )
                        keyRows.forEach { rowKeys ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowKeys.forEach { digit ->
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(38.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color(0xFF1F1F21))
                                            .border(1.dp, Color(0x1EFFFFFF), RoundedCornerShape(12.dp))
                                            .clickable {
                                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                localUiState = localUiState.appendDtmfDigit(digit)
                                                runCatching {
                                                    toneGenerator?.startTone(dtmfToneForChar(digit), 140)
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = digit.toString(),
                                            color = Color.White,
                                            fontSize = 16.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InCallDotMatrixPill(
    label: String,
    icon: ImageVector,
    isActive: Boolean,
    accentColor: Color,
    surfaceColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (isActive) accentColor else surfaceColor)
            .border(
                width = 1.dp,
                color = if (isActive) Color(0x66FFFFFF) else Color(0x22FFFFFF),
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = Color.White,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = label,
            color = Color.White,
            fontSize = 8.5.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip
        )
    }
}
