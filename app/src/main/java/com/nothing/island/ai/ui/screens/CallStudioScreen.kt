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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CallEnd
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
                    "For the call notch to work, set Dot Island as your default phone app.",
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

        var previewState by remember { mutableStateOf(CallPreviewState.EXPANDED_INCOMING) }

        // 3-State Preview Mode Selector Pills
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                CallPreviewState.EXPANDED_INCOMING to "Incoming",
                CallPreviewState.EXPANDED_CONNECTED to "Connected",
                CallPreviewState.COMPACT_ONGOING to "Compact"
            ).forEach { (stateOption, label) ->
                val isSelected = previewState == stateOption
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) NothingPrimaryText else NothingTokens.NothingSurfaceMedium)
                        .clickable { previewState = stateOption }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) Color.Black else NothingPrimaryText,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        LiveCallPreview(
            previewState = previewState,
            onToggleExpand = {
                previewState = when (previewState) {
                    CallPreviewState.COMPACT_ONGOING -> CallPreviewState.EXPANDED_CONNECTED
                    else -> CallPreviewState.COMPACT_ONGOING
                }
            }
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Long Press to expand or collapse. Tap to open call screen.",
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

enum class CallPreviewState {
    EXPANDED_INCOMING,
    EXPANDED_CONNECTED,
    COMPACT_ONGOING
}

/** Animated reference-style 3-state call preview. */
@Composable
private fun LiveCallPreview(
    previewState: CallPreviewState,
    onToggleExpand: () -> Unit
) {
    val pulse = rememberInfiniteTransition(label = "callPreviewPulse")
    val tick by pulse.animateFloat(
        initialValue = 1f, targetValue = 9f,
        animationSpec = infiniteRepeatable(tween(8000, easing = androidx.compose.animation.core.LinearEasing), RepeatMode.Restart),
        label = "tick",
    )
    val waveHeights = listOf(
        pulse.animateFloat(4f, 13f, infiniteRepeatable(tween(420, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "w1"),
        pulse.animateFloat(11f, 5f, infiniteRepeatable(tween(360, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "w2"),
        pulse.animateFloat(6f, 15f, infiniteRepeatable(tween(480, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "w3"),
        pulse.animateFloat(12f, 6f, infiniteRepeatable(tween(390, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "w4"),
        pulse.animateFloat(5f, 11f, infiniteRepeatable(tween(440, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "w5"),
    )

    var isSpeakerActive by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(NothingTokens.NothingSurfaceLowest)
            .border(1.dp, NothingTokens.NothingBorderSubtle, RoundedCornerShape(22.dp))
            .padding(vertical = 24.dp, horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        when (previewState) {
            CallPreviewState.COMPACT_ONGOING -> {
                // Symmetrical ~200dp x 36dp stadium pill matching reference
                Box(
                    modifier = Modifier
                        .width(200.dp)
                        .height(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF000000))
                        .border(1.dp, Color(0xFF262626), CircleShape)
                        .clickable(onClick = onToggleExpand)
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Call,
                                contentDescription = "Active Call",
                                tint = Color(0xFF00C853),
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "00:0${tick.toInt().coerceIn(1, 9)}",
                                color = Color(0xFF00C853),
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            modifier = Modifier.height(16.dp)
                        ) {
                            waveHeights.forEach { h ->
                                Box(
                                    modifier = Modifier
                                        .width(2.5.dp)
                                        .height(h.value.dp)
                                        .clip(RoundedCornerShape(1.25.dp))
                                        .background(Color(0xFF00C853))
                                )
                            }
                        }
                    }
                }
            }

            CallPreviewState.EXPANDED_INCOMING -> {
                // 344dp x 78dp capsule matching reference
                Box(
                    modifier = Modifier
                        .width(344.dp)
                        .height(78.dp)
                        .clip(RoundedCornerShape(39.dp))
                        .background(Color(0xFF000000))
                        .border(1.dp, Color(0xFF262626), RoundedCornerShape(39.dp))
                        .clickable(onClick = onToggleExpand)
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(Color(0xFF7B4BFF), Color(0xFFFF5FA2)))),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("DW", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        }

                        Spacer(Modifier.width(12.dp))

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text("Incoming Call", color = Color(0xFF8E8E93), fontSize = 12.sp)
                            Spacer(Modifier.height(2.dp))
                            Text("Daisy Williams", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        }

                        Spacer(Modifier.width(8.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFF3B30)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.CallEnd, contentDescription = "Decline", tint = Color.White, modifier = Modifier.size(22.dp))
                            }
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF34C759)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.Call, contentDescription = "Answer", tint = Color.White, modifier = Modifier.size(22.dp))
                            }
                        }
                    }
                }
            }

            CallPreviewState.EXPANDED_CONNECTED -> {
                // 344dp x 78dp capsule matching reference
                Box(
                    modifier = Modifier
                        .width(344.dp)
                        .height(78.dp)
                        .clip(RoundedCornerShape(39.dp))
                        .background(Color(0xFF000000))
                        .border(1.dp, Color(0xFF262626), RoundedCornerShape(39.dp))
                        .clickable(onClick = onToggleExpand)
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(Color(0xFF7B4BFF), Color(0xFFFF5FA2)))),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("DW", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        }

                        Spacer(Modifier.width(12.dp))

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text("00:0${tick.toInt().coerceIn(1, 9)}", color = Color(0xFF00C853), fontSize = 13.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(2.dp))
                            Text("Daisy Williams", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        }

                        Spacer(Modifier.width(8.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(if (isSpeakerActive) Color.White else Color(0xFF3A3A3C))
                                    .clickable { isSpeakerActive = !isSpeakerActive },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.VolumeUp, contentDescription = "Speaker", tint = if (isSpeakerActive) Color.Black else Color.White, modifier = Modifier.size(22.dp))
                            }
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFF3B30)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.CallEnd, contentDescription = "Hang Up", tint = Color.White, modifier = Modifier.size(22.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
