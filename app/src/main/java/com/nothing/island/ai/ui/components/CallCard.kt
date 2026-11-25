package com.nothing.island.ai.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CallEnd
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
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
            // Left of camera cutout: Green phone vector icon + live green tnum timer
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Call,
                    contentDescription = "Call",
                    tint = greenCallColor,
                    modifier = Modifier.requiredSize(15.dp)
                )
                Text(
                    text = when {
                        isRinging -> if ((event as? IslandEvent.IncomingCallRinging)?.isVideoCall == true) "VIDEO" else "CALL"
                        isDialing -> "CALLING"
                        else -> formatCallDuration(durationSeconds)
                    },
                    color = greenCallColor,
                    fontSize = 12.sp,
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

            // Right of camera cutout: Animated green voice waveform bars
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
                            .height(animatedHeight.value.dp)
                            .clip(RoundedCornerShape(1.25.dp))
                            .background(if (isDialing) NothingSecondaryText else greenCallColor)
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
) {
    val isRinging = event is IslandEvent.IncomingCallRinging
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
    val greenCallColor = Color(0xFF00C853)
    val answerGreen = Color(0xFF34C759)
    val declineRed = Color(0xFFFF3B30)
    val speakerDark = Color(0xFF3A3A3C)

    val topClearance = if (isCameraCutoutEnabled) 42.dp else 0.dp
    val capsuleHeight = if (isCameraCutoutEnabled) 120.dp else 78.dp
    val cornerRadius = if (isCameraCutoutEnabled) 36.dp else 39.dp

    Box(
        modifier = modifier
            .width(344.dp)
            .height(capsuleHeight)
            .clip(RoundedCornerShape(cornerRadius))
            .background(theme.pillBackgroundColor)
            .border(
                width = 1.dp,
                color = if (theme.isGlassmorphic) Color(0x45FFFFFF) else if (theme.isInverted) Color(0xFFD0D0D0) else theme.borderColor,
                shape = RoundedCornerShape(cornerRadius)
            )
            .padding(top = topClearance, start = 14.dp, end = 14.dp),
        contentAlignment = if (isCameraCutoutEnabled) Alignment.CenterStart else Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: 48.dp circular caller photo / avatar
            Box(
                modifier = Modifier
                    .requiredSize(48.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF7B4BFF), Color(0xFFFF5FA2))
                        )
                    ),
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
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false,
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            // Center: Status / Timer + Caller Name
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                if (isRinging) {
                    Text(
                        text = if ((event as? IslandEvent.IncomingCallRinging)?.isVideoCall == true) "Incoming Video Call" else "Incoming Call",
                        color = theme.textColorSecondary,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        fontFamily = theme.bodyFont,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                } else if (isDialing) {
                    Text(
                        text = "Calling...",
                        color = theme.textColorSecondary,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        fontFamily = theme.bodyFont,
                        maxLines = 1,
                        softWrap = false
                    )
                } else {
                    Text(
                        text = formatCallDuration(elapsedSeconds),
                        color = greenCallColor,
                        fontSize = 13.sp,
                        lineHeight = 17.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        softWrap = false
                    )
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

            // Right: Action buttons (44.dp circles, 10.dp gap)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (isRinging) {
                    // Red Decline button (phone down vector icon)
                    Box(
                        modifier = Modifier
                            .requiredSize(44.dp)
                            .clip(CircleShape)
                            .background(declineRed)
                            .clickable(onClick = onDecline),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CallEnd,
                            contentDescription = "Decline Call",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Green Answer button (phone vector icon)
                    Box(
                        modifier = Modifier
                            .requiredSize(44.dp)
                            .clip(CircleShape)
                            .background(answerGreen)
                            .clickable(onClick = onAnswer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Call,
                            contentDescription = "Answer Call",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                } else {
                    // Connected or Dialing: Speaker toggle + Hang up
                    val speakerBg = if (isSpeakerOn) Color.White else speakerDark
                    val speakerTint = if (isSpeakerOn) Color.Black else Color.White

                    Box(
                        modifier = Modifier
                            .requiredSize(44.dp)
                            .clip(CircleShape)
                            .background(speakerBg)
                            .clickable(onClick = { onToggleSpeaker?.invoke() }),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.VolumeUp,
                            contentDescription = "Toggle Speaker",
                            tint = speakerTint,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Red Hang-up button (phone down vector icon)
                    Box(
                        modifier = Modifier
                            .requiredSize(44.dp)
                            .clip(CircleShape)
                            .background(declineRed)
                            .clickable(onClick = onDecline),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CallEnd,
                            contentDescription = "Hang up Call",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}
