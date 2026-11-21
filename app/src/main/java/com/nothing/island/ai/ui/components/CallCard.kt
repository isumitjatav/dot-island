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
    Box(modifier = modifier) {
    Box(
        modifier = Modifier
            .width(200.dp)
            .height(36.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(if (isRinging) NothingPrimaryAccent.copy(alpha = 0.18f + glow * 0.12f) else NothingPrimaryBackground)
            .border(1.dp, if (isRinging) NothingPrimaryAccent else NothingDivider, RoundedCornerShape(18.dp))
            .clickable(onClick = onTap)
            .padding(horizontal = if (showEnd) 26.dp else 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (isRinging) NothingPrimaryAccent else NothingSuccess)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = caller,
                color = NothingPrimaryText,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = if (isRinging) {
                    if ((event as IslandEvent.IncomingCallRinging).isVideoCall) "VIDEO" else "CALL"
                } else {
                    formatCallDuration((event as? IslandEvent.CallConnected)?.durationSeconds ?: 0L)
                },
                color = if (isRinging) NothingPrimaryAccent else NothingSecondaryText,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
            )
        }
    }

    // Reference-style red call-end button attached to the left of the pill.
    if (showEnd) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = (-6).dp)
                .size(34.dp)
                .clip(CircleShape)
                .background(NothingPrimaryAccent)
                .border(3.dp, NothingPrimaryBackground, CircleShape)
                .clickable(onClick = onEndCall!!),
            contentAlignment = Alignment.Center,
        ) {
            Text("✕", color = NothingPrimaryText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
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
) {
    val isRinging = event is IslandEvent.IncomingCallRinging
    val caller = when (event) {
        is IslandEvent.IncomingCallRinging -> event.callerDisplayName
        is IslandEvent.CallConnected -> event.callerDisplayName
        else -> "Call"
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(24.dp))
            .background(NothingPrimaryBackground)
            .border(1.dp, NothingDivider, RoundedCornerShape(24.dp))
            .padding(20.dp),
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
    }
}
