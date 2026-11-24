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
import androidx.compose.ui.graphics.Color
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
 * R9 compact call pill: sleek ~130dp trimmed capsule matching Nothing OS geometry
 * without colliding with status bar icons or overlapping the red end button.
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
    val caller = when (event) {
        is IslandEvent.IncomingCallRinging -> event.callerDisplayName
        is IslandEvent.CallConnected -> event.callerDisplayName
        is IslandEvent.CallEnded -> "Call ended"
        else -> "Call"
    }
    val isRinging = event is IslandEvent.IncomingCallRinging
    val isDialing = (event as? IslandEvent.CallConnected)?.isDialing == true

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

    // Symmetrical, neatly arranged Row with zero overlap:
    // [ (✕) red end button ] (6dp space) [ sleek pill: 🔊 00:25 |||| ]
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        // Red END button attached cleanly to the left edge without overlapping
        if (showEnd) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(NothingPrimaryAccent)
                    .border(1.5.dp, NothingPrimaryBackground, CircleShape)
                    .clickable(onClick = { onEndCall?.invoke() }),
                contentAlignment = Alignment.Center,
            ) {
                Text("✕", color = NothingPrimaryText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(6.dp))
        }

        // Main Call Capsule (trimmed to 104dp with end button, or 124dp standalone)
        val theme = com.nothing.island.ai.ui.theme.LocalIslandTheme.current
        val pillWidth = if (showEnd) 104.dp else 124.dp
        val compactBorderColor = when {
            isRinging -> theme.accentColor
            theme.isGlassmorphic -> Color(0x45FFFFFF)
            theme.isInverted -> Color(0xFFD0D0D0)
            else -> theme.borderColor
        }

        Box(
            modifier = Modifier
                .width(pillWidth)
                .height(32.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(if (isRinging) theme.accentColor.copy(alpha = 0.18f + glow * 0.12f) else theme.pillBackgroundColor)
                .border(1.dp, compactBorderColor, RoundedCornerShape(16.dp))
                .clickable(onClick = onTap)
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                // Phone or speaker glyph (tappable to toggle speakerphone)
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .then(
                            if (onToggleSpeaker != null) {
                                Modifier.clickable(onClick = { onToggleSpeaker() })
                            } else {
                                Modifier
                            }
                        )
                        .padding(horizontal = 2.dp, vertical = 2.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (isSpeakerOn) "🔊" else "☎",
                        color = if (isRinging) theme.accentColor else if (isDialing) theme.textColorSecondary else if (theme.isInverted) Color(0xFF000000) else NothingSuccess,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }

                Spacer(Modifier.width(4.dp))

                // Live timer or dialing text
                Text(
                    text = when {
                        isRinging -> if ((event as IslandEvent.IncomingCallRinging).isVideoCall) "VIDEO" else "CALL"
                        isDialing -> "CALLING"
                        else -> formatCallDuration((event as? IslandEvent.CallConnected)?.durationSeconds ?: 0L)
                    },
                    color = if (isRinging) theme.accentColor else if (isDialing) theme.textColorSecondary else if (theme.isInverted) Color(0xFF000000) else NothingSuccess,
                    fontSize = 11.sp,
                    fontFamily = theme.displayFont,
                    fontWeight = theme.displayFontWeight,
                    maxLines = 1,
                )

                Spacer(Modifier.weight(1f))

                // Audio waveform bars (compact 4 bars)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(1.5.dp),
                ) {
                    listOf(4f, 10f, 6f, 12f).forEachIndexed { index, h ->
                        val b by pulse.animateFloat(
                            initialValue = h * 0.5f, targetValue = h,
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
                                .background(if (isDialing) NothingSecondaryText else NothingSuccess)
                        )
                    }
                }
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
        else -> "Call"
    }

    val cardShape = if (isCameraCutoutEnabled) notchedCardShape(cornerRadiusDp = 28f) else RoundedCornerShape(28.dp)
    val theme = com.nothing.island.ai.ui.theme.LocalIslandTheme.current

    val topPadding = if (isCameraCutoutEnabled) 20.dp else 16.dp
    val bottomPadding = if (isCameraCutoutEnabled) 14.dp else 16.dp

    Column(
        modifier = modifier
            .fillMaxSize()
            .clip(cardShape)
            .background(theme.pillBackgroundColor)
            .border(1.dp, theme.borderColor, cardShape)
            .padding(start = 20.dp, end = 20.dp, top = topPadding, bottom = bottomPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = when {
                isRinging -> "INCOMING CALL"
                isDialing -> "OUTGOING CALL"
                else -> "ACTIVE CALL"
            },
            color = when {
                isRinging -> theme.accentColor
                isDialing -> theme.textColorSecondary
                else -> if (theme.isInverted) Color(0xFF000000) else NothingSuccess
            },
            fontSize = 10.sp,
            fontFamily = theme.bodyFont,
            letterSpacing = 2.sp,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = caller,
            color = theme.textColorPrimary,
            fontSize = 20.sp,
            fontFamily = theme.displayFont,
            fontWeight = theme.displayFontWeight,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = when {
                isRinging && (event as? IslandEvent.IncomingCallRinging)?.isVideoCall == true -> "VIDEO CALL"
                isRinging -> "INCOMING"
                isDialing -> "CALLING..."
                event is IslandEvent.CallConnected -> formatCallDuration(elapsedSeconds)
                else -> "CALL"
            },
            color = theme.textColorSecondary,
            fontSize = 12.sp,
            fontFamily = theme.displayFont,
        )
        Spacer(Modifier.height(20.dp))
        if (isRinging) {
            // Incoming call: DECLINE + ANSWER
            val declineBg = when {
                theme.isInverted -> Color(0xFFE4E4E4)
                theme.isGlassmorphic -> Color(0xB3333333)
                else -> NothingElevatedSurface
            }
            val declineText = when {
                theme.isInverted -> Color(0xFF000000)
                else -> NothingSecondaryText
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(declineBg)
                        .border(1.dp, if (theme.isGlassmorphic) Color(0x28FFFFFF) else if (theme.isInverted) Color(0xFFCCCCCC) else NothingDivider, RoundedCornerShape(14.dp))
                        .clickable(onClick = onDecline),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("DECLINE", color = declineText, fontSize = 11.sp, fontFamily = theme.bodyFont)
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
                    Text("ANSWER", color = NothingPrimaryText, fontSize = 11.sp, fontFamily = theme.bodyFont, fontWeight = FontWeight.Bold)
                }
            }
        } else {
            // Ongoing or Dialing call: SPEAKER + END CALL
            val speakerBg = when {
                isSpeakerOn -> if (theme.isInverted) Color(0xFFCCCCCC) else NothingPrimaryText.copy(alpha = 0.15f)
                theme.isInverted -> Color(0xFFE4E4E4)
                theme.isGlassmorphic -> Color(0xB3333333)
                else -> NothingElevatedSurface
            }
            val speakerTextColor = when {
                theme.isInverted -> Color(0xFF000000)
                isSpeakerOn -> NothingPrimaryText
                else -> NothingSecondaryText
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (onToggleSpeaker != null) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(speakerBg)
                            .border(1.dp, if (isSpeakerOn) (if (theme.isInverted) Color(0xFF000000) else NothingPrimaryText) else if (theme.isGlassmorphic) Color(0x28FFFFFF) else if (theme.isInverted) Color(0xFFCCCCCC) else NothingDivider, RoundedCornerShape(14.dp))
                            .clickable(onClick = onToggleSpeaker),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = if (isSpeakerOn) "🔊 SPEAKER ON" else "🔈 SPEAKER",
                            color = speakerTextColor,
                            fontSize = 11.sp,
                            fontFamily = theme.bodyFont,
                            fontWeight = if (isSpeakerOn) FontWeight.Bold else FontWeight.Normal,
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(theme.primaryButtonBackground)
                        .then(
                            if (theme.isGlassmorphic) Modifier.border(1.dp, androidx.compose.ui.graphics.Color(0x28FFFFFF), RoundedCornerShape(14.dp))
                            else if (theme == com.nothing.island.ai.ui.theme.IslandSignatureTheme.GLYPH_ARC) Modifier.border(1.dp, androidx.compose.ui.graphics.Color(0xFF333333), RoundedCornerShape(14.dp))
                            else Modifier
                        )
                        .clickable(onClick = onDecline),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("END CALL", color = theme.primaryButtonTextColor, fontSize = 11.sp, fontFamily = theme.bodyFont, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
