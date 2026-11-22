package com.nothing.island.ai.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nothing.island.ai.engine.IslandEvent
import com.nothing.island.ai.ui.theme.NothingDivider
import com.nothing.island.ai.ui.theme.NothingElevatedSurface
import com.nothing.island.ai.ui.theme.NothingPrimaryAccent
import com.nothing.island.ai.ui.theme.NothingPrimaryBackground
import com.nothing.island.ai.ui.theme.NothingPrimaryText
import com.nothing.island.ai.ui.theme.NothingSecondaryText
import com.nothing.island.ai.ui.theme.NothingSuccess

/** Formats an elapsed call duration as mm:ss. Pure and unit-testable. */
fun formatCallDuration(totalSeconds: Long): String {
    val safe = totalSeconds.coerceAtLeast(0L)
    return "%02d:%02d".format(safe / 60, safe % 60)
}

/**
 * R9 compact call pill: 200dp symmetrical capsule with a breathing red accent
 * over the camera punch hole.
 */
@Composable
fun CallCompactView(
    event: IslandEvent,
    onTap: () -> Unit,
    onEndCall: (() -> Unit)? = null,
    showEndButton: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val caller = when (event) {
        is IslandEvent.IncomingCallRinging -> event.callerDisplayName
        is IslandEvent.CallConnected -> event.callerDisplayName
        is IslandEvent.CallEnded -> "Call ended"
        else -> "Call"
    }
    val isRinging = event is IslandEvent.IncomingCallRinging

    val pulse = rememberInfiniteTransition(label = "callPulse")
    val glow by pulse.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "callGlow",
    )

    val showEnd = !isRinging && showEndButton && onEndCall != null

    // Reference layout: the red END button is a separate circle attached to the
    // LEFT OUTSIDE the pill. The pill itself shows: green phone icon, live timer,
    // and an audio waveform — no caller name, no answer button, no overlap.
    Box(modifier = modifier, contentAlignment = Alignment.CenterStart) {
        Box(
            modifier = Modifier
                .padding(start = if (showEnd) 18.dp else 0.dp)
                .width(200.dp)
                .height(36.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(if (isRinging) NothingPrimaryAccent.copy(alpha = 0.18f + glow * 0.12f) else NothingPrimaryBackground)
                .border(1.dp, if (isRinging) NothingPrimaryAccent else NothingDivider, RoundedCornerShape(18.dp))
                .clickable(onClick = onTap)
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Green phone glyph
                Text(
                    text = "☎",
                    color = NothingSuccess,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.width(10.dp))
                // Live timer (green, matches reference)
                Text(
                    text = if (isRinging) {
                        if ((event as IslandEvent.IncomingCallRinging).isVideoCall) "VIDEO" else "CALL"
                    } else {
                        formatCallDuration((event as? IslandEvent.CallConnected)?.durationSeconds ?: 0L)
                    },
                    color = if (isRinging) NothingPrimaryAccent else NothingSuccess,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.weight(1f))
                // Audio waveform bars
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    listOf(6f, 12f, 8f, 16f, 10f).forEachIndexed { index, h ->
                        val b by pulse.animateFloat(
                            initialValue = h * 0.6f, targetValue = h,
                            animationSpec = infiniteRepeatable(
                                tween(300 + index * 60, easing = FastOutSlowInEasing),
                                RepeatMode.Reverse,
                            ),
                            label = "wave$index",
                        )
                        Box(
                            Modifier
                                .width(2.dp)
                                .height(b.dp)
                                .clip(RoundedCornerShape(1.dp))
                                .background(NothingSuccess)
                        )
                    }
                }
            }
        }

        // Red END button attached to the left edge, fully outside the pill.
        if (showEnd) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(NothingPrimaryAccent)
                    .border(3.dp, NothingPrimaryBackground, CircleShape)
                    .clickable(onClick = onEndCall!!),
                contentAlignment = Alignment.Center,
            ) {
                Text("✕", color = NothingPrimaryText, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/**
 * R9 expanded call card with caller identity, live duration, and real answer /
 * decline controls backed by the dialer notification's PendingIntents.
 */
@Composable
fun CallExpandedView(
    event: IslandEvent,
    elapsedSeconds: Long,
    onAnswer: () -> Unit,
    onDecline: () -> Unit,
    modifier: Modifier = Modifier,
    isCameraCutoutEnabled: Boolean = false,
) {
    val isRinging = event is IslandEvent.IncomingCallRinging
    val caller = when (event) {
        is IslandEvent.IncomingCallRinging -> event.callerDisplayName
        is IslandEvent.CallConnected -> event.callerDisplayName
        else -> "Call"
    }

    val cardShape = if (isCameraCutoutEnabled) notchedCardShape(cornerRadiusDp = 24f) else RoundedCornerShape(24.dp)

    val topPadding = if (isCameraCutoutEnabled) 36.dp else 20.dp
    val bottomPadding = if (isCameraCutoutEnabled) 16.dp else 20.dp

    Column(
        modifier = modifier
            .fillMaxSize()
            .clip(cardShape)
            .background(NothingPrimaryBackground)
            .border(1.dp, NothingDivider, cardShape)
            .padding(start = 20.dp, end = 20.dp, top = topPadding, bottom = bottomPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = if (isRinging) "INCOMING CALL" else "ACTIVE CALL",
            color = if (isRinging) NothingPrimaryAccent else NothingSuccess,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 2.sp,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = caller,
            color = NothingPrimaryText,
            fontSize = 20.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = when {
                isRinging && (event as? IslandEvent.IncomingCallRinging)?.isVideoCall == true -> "VIDEO CALL"
                event is IslandEvent.CallConnected -> formatCallDuration(elapsedSeconds)
                isRinging -> "INCOMING"
                else -> "CALL"
            },
            color = NothingSecondaryText,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
        )
        Spacer(Modifier.height(20.dp))
        if (isRinging) {
            // Incoming call: DECLINE + ANSWER
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(NothingElevatedSurface)
                        .border(1.dp, NothingDivider, RoundedCornerShape(14.dp))
                        .clickable(onClick = onDecline),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("DECLINE", color = NothingSecondaryText, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(NothingPrimaryAccent)
                        .clickable(onClick = onAnswer),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("ANSWER", color = NothingPrimaryText, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
            }
        } else {
            // Ongoing call: a single END CALL button (no ANSWER)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(NothingPrimaryAccent)
                    .clickable(onClick = onDecline),
                contentAlignment = Alignment.Center,
            ) {
                Text("END CALL", color = NothingPrimaryText, fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            }
        }
    }
}
