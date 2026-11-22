package com.nothing.island.ai.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nothing.island.ai.data.IslandPreferencesRepository
import com.nothing.island.ai.telephony.CallPreferences
import com.nothing.island.ai.ui.components.NothingCard
import com.nothing.island.ai.ui.components.NothingCardVariant
import com.nothing.island.ai.ui.components.NothingSectionHeader
import com.nothing.island.ai.ui.components.NothingSwitchRow
import com.nothing.island.ai.ui.theme.NothingPrimaryAccent
import com.nothing.island.ai.ui.theme.NothingPrimaryText
import com.nothing.island.ai.ui.theme.NothingSecondaryText
import com.nothing.island.ai.ui.theme.NothingSuccess
import com.nothing.island.ai.ui.theme.NothingTokens
import kotlinx.coroutines.launch

/**
 * Dedicated Call studio — reference-style settings with a live animated call
 * pill preview whose controls respond to the toggles.
 */
@Composable
fun CallStudioScreen(onNavigateBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val repository = remember { IslandPreferencesRepository.getInstance(context) }
    val prefs by repository.callPreferencesFlow().collectAsState(initial = CallPreferences.DEFAULTS)
    val scope = rememberCoroutineScope()
    val scroll = rememberScrollState()

    fun update(transform: (CallPreferences) -> CallPreferences) {
        scope.launch { repository.updateCallPreferences(transform(prefs)) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NothingTokens.NothingBlack)
            .verticalScroll(scroll)
            .padding(horizontal = NothingTokens.ScreenHorizontalPadding, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Header
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(NothingTokens.NothingSurfaceMedium)
                    .border(1.dp, NothingTokens.NothingBorderSubtle, CircleShape)
                    .clickable { onNavigateBack() },
                contentAlignment = Alignment.Center,
            ) { Text("<", color = NothingPrimaryText, fontSize = 16.sp, fontWeight = FontWeight.Bold) }

            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("CALL", color = NothingPrimaryText, fontSize = 18.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                Text("SYSTEM & VOIP CALL NOTCH", color = NothingTokens.NothingTextTertiary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(NothingTokens.NothingSurfaceMedium)
                    .clickable {
                        scope.launch { repository.updateCallPreferences(CallPreferences.DEFAULTS) }
                    }
                    .padding(horizontal = 12.dp, vertical = 7.dp),
            ) { Text("RESET", color = NothingPrimaryAccent, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold) }
        }

        Spacer(Modifier.height(12.dp))

        // Default-dialer hint banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(NothingTokens.NothingSurfaceLow)
                .border(1.dp, NothingTokens.NothingBorderSubtle, RoundedCornerShape(14.dp))
                .padding(14.dp),
        ) {
            Column {
                Text(
                    "For the call notch to work, set Nothing Island as your default phone app.",
                    color = NothingSecondaryText,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                )
                Spacer(Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(NothingTokens.NothingBlue)
                        .clickable {
                            runCatching {
                                val intent = Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                context.startActivity(intent)
                            }
                        }
                        .padding(horizontal = 14.dp, vertical = 7.dp),
                ) { Text("SET DEFAULT", color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold) }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Master enable
        NothingCard(variant = NothingCardVariant.STANDARD) {
            NothingSwitchRow(
                title = "Enable System Call",
                description = "Show incoming and ongoing calls on the camera notch.",
                checked = prefs.systemCallEnabled,
                onCheckedChange = { enabled -> update { it.copy(systemCallEnabled = enabled) } },
                badgeText = if (prefs.systemCallEnabled) "ACTIVE" else "OFF",
            )
        }

        Spacer(Modifier.height(18.dp))

        NothingSectionHeader(title = "LIVE CALL PREVIEW", isAccent = true)
        Spacer(Modifier.height(8.dp))
        LiveCallPreview(showEndButton = prefs.doubleIncomingCallActions && prefs.systemCallEnabled)
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Long press to expand or collapse. Tap to open the call screen.",
            color = NothingTokens.NothingTextTertiary,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
        )

        Spacer(Modifier.height(18.dp))

        NothingSectionHeader(title = "CALL BEHAVIOUR")
        NothingCard(variant = NothingCardVariant.STANDARD) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                NothingSwitchRow(
                    title = "Double Incoming Call Actions",
                    description = "Show the red end-call button on the compact pill. Hide the notch when a full-screen call takes over.",
                    checked = prefs.doubleIncomingCallActions,
                    onCheckedChange = { enabled -> update { it.copy(doubleIncomingCallActions = enabled) } },
                    badgeText = "END BTN",
                )
                NothingSwitchRow(
                    title = "Auto-Expand Call Card",
                    description = "Automatically expand into a full card when a call connects, instead of staying in compact pill mode.",
                    checked = prefs.autoExpandCall,
                    onCheckedChange = { enabled -> update { it.copy(autoExpandCall = enabled) } },
                    badgeText = if (prefs.autoExpandCall) "EXPANDED" else "COMPACT",
                )
                NothingSwitchRow(
                    title = "Other Apps / VoIP Calls",
                    description = "Handle calls from third-party and VoIP applications on the notch.",
                    checked = prefs.otherAppsCalls,
                    onCheckedChange = { enabled -> update { it.copy(otherAppsCalls = enabled) } },
                    badgeText = "VOIP",
                )
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

/** Animated reference-style call pill preview. */
@Composable
private fun LiveCallPreview(showEndButton: Boolean) {
    val pulse = rememberInfiniteTransition(label = "callPreviewPulse")
    val glow by pulse.animateFloat(
        initialValue = 0.15f, targetValue = 0.4f,
        animationSpec = infiniteRepeatable(tween(1000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glow",
    )
    val tick by pulse.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1000, easing = FastOutSlowInEasing), RepeatMode.Restart),
        label = "tick",
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(NothingTokens.NothingSurfaceLowest)
            .border(1.dp, NothingTokens.NothingBorderSubtle, RoundedCornerShape(22.dp))
            .padding(vertical = 26.dp, horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(modifier = Modifier) {
            Box(
                modifier = Modifier
                    .width(280.dp)
                    .height(56.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color(0xFF0A0A0A))
                    .border(1.dp, NothingSuccess.copy(alpha = glow), RoundedCornerShape(28.dp)),
                contentAlignment = Alignment.CenterStart,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 14.dp, end = 12.dp)) {
                    // Avatar
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(listOf(Color(0xFF7B4BFF), Color(0xFFFF5FA2)))
                            ),
                        contentAlignment = Alignment.Center,
                    ) { Text("DW", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold) }

                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("00:0${(tick * 9).toInt().coerceIn(0, 9)}", color = NothingSuccess, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        Text("Daisy Williams", color = NothingPrimaryText, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    }

                    // Speaker circle
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF2A2A2A))
                            .border(1.dp, NothingTokens.NothingBorderSubtle, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) { Text("◖", color = NothingSecondaryText, fontSize = 16.sp) }
                }
            }

            // Animated red end button
            AnimatedVisibility(
                visible = showEndButton,
                enter = slideInHorizontally { -it } + fadeIn(animationSpec = tween(260, easing = FastOutSlowInEasing)),
                exit = slideOutHorizontally { -it } + fadeOut(animationSpec = tween(200)),
                modifier = Modifier.align(Alignment.CenterStart),
            ) {
                Box(
                    modifier = Modifier
                        .offset(x = (-8).dp)
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(NothingPrimaryAccent)
                        .border(3.dp, NothingTokens.NothingSurfaceLowest, CircleShape),
                    contentAlignment = Alignment.Center,
                ) { Text("✕", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold) }
            }
        }
    }
}
