package com.nothing.island.ai.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nothing.island.ai.battery.ChargingAnimationStyle
import com.nothing.island.ai.media.MusicVisualizerStyle
import com.nothing.island.ai.ui.charging.ChargingAnimationHost
import com.nothing.island.ai.ui.theme.NothingTokens

/**
 * Preview states supported by the Interactive Preview Engine.
 * Matches all scenarios discovered across the 20 reference screenshots.
 */
enum class IslandPreviewMode(val label: String, val isExpanded: Boolean) {
    IDLE("IDLE", false),
    NOTIF_COMPACT("NOTIF (C)", false),
    NOTIF_EXPANDED("NOTIF (E)", true),
    MUSIC_COMPACT("MUSIC (C)", false),
    MUSIC_EXPANDED("MUSIC (E)", true),
    CHARGING("CHARGING", false),
    BLUETOOTH_COMPACT("BT (C)", false),
    BLUETOOTH_EXPANDED("BT (E)", true)
}

/**
 * Nothing OS 3.x Interactive Live Island Preview Engine
 *
 * Simulates the camera punch-hole cutout of the Nothing Phone (4a) and demonstrates
 * authentic simulated overlay states across all 8 supported display modes.
 *
 * Note: Clearly labeled as simulated demonstration data.
 */
@Composable
fun IslandPreview(
    modifier: Modifier = Modifier,
    initialMode: IslandPreviewMode = IslandPreviewMode.IDLE,
    pillWidthDp: Float = NothingTokens.CutoutWidthDp,
    pillHeightDp: Float = NothingTokens.CutoutHeightDp,
    cornerRadiusDp: Float = NothingTokens.CutoutCornerRadiusDp,
    topOffsetDp: Float = NothingTokens.CutoutTopOffsetDp,
    horizontalOffsetDp: Float = 0f,
    accentColor: Color = NothingTokens.NothingRed,
    pillBackgroundColor: Color = NothingTokens.NothingBlack,
    visualizerStyle: MusicVisualizerStyle = MusicVisualizerStyle.VERTICAL_EQUALIZER,
    isCameraCutoutEnabled: Boolean = false,
    expandedOffsetXDp: Float = 0f,
    expandedOffsetYDp: Float = 0f,
    expandedNotchWidthDp: Float = 38f,
    expandedNotchDepthDp: Float = 28f,
    onModeChanged: ((IslandPreviewMode) -> Unit)? = null,
    // Backward compatibility parameters for legacy callers
    isExpanded: Boolean = false,
    onToggleExpanded: (() -> Unit)? = null,
    title: String = "DOT ISLAND",
    statusText: String = "SIMULATED PREVIEW",
    theme: com.nothing.island.ai.ui.theme.IslandSignatureTheme = com.nothing.island.ai.ui.theme.IslandSignatureTheme.NOTHING_CLASSIC
) {
    var currentMode by remember {
        mutableStateOf(if (isExpanded) IslandPreviewMode.MUSIC_EXPANDED else initialMode)
    }
    androidx.compose.runtime.LaunchedEffect(initialMode) {
        currentMode = initialMode
    }
    val haptic = LocalHapticFeedback.current

    NothingCard(
        modifier = modifier,
        variant = NothingCardVariant.STANDARD,
        contentPadding = 14.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Preview Header with Title & Simulated Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(accentColor)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "LIVE ISLAND PREVIEW",
                        color = NothingTokens.NothingTextSecondary,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.4.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(NothingTokens.NothingSurfaceMedium)
                        .border(1.dp, NothingTokens.NothingBorderSubtle, RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "SIMULATED",
                        color = NothingTokens.NothingAmber,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // State Selector Pill Bar
            val scrollState = rememberScrollState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                IslandPreviewMode.values().forEach { mode ->
                    val isSelected = mode == currentMode
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) NothingTokens.NothingSurfaceHigh else NothingTokens.NothingSurfaceLowest)
                            .border(
                                width = 1.dp,
                                color = if (isSelected) NothingTokens.NothingBorderProminent else NothingTokens.NothingBorderSubtle,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                currentMode = mode
                                onModeChanged?.invoke(mode)
                                if (onToggleExpanded != null && mode.isExpanded != isExpanded) {
                                    onToggleExpanded()
                                }
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = mode.label,
                            color = if (isSelected) NothingTokens.NothingTextPrimary else NothingTokens.NothingTextTertiary,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Simulated Phone Bezel Screen Area (Recessed AMOLED Well)
            val containerHeight = if (currentMode.isExpanded) 205.dp else 115.dp
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(containerHeight)
                    .clip(RoundedCornerShape(20.dp))
                    .background(NothingTokens.NothingSurfaceLowest)
                    .border(1.dp, NothingTokens.NothingBorderSubtle, RoundedCornerShape(20.dp))
                    .animateContentSize(animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)),
                contentAlignment = Alignment.TopCenter
            ) {
                // Physical Punch-Hole Camera Representation (Centered at x=0, y=9dp)
                Box(
                    modifier = Modifier
                        .offset(x = 0.dp, y = (topOffsetDp + 5).dp)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF030303))
                        .border(1.dp, Color(0xFF181818), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1F2937))
                    )
                }

                // Dynamic Overlay Preview Content
                val previewYOffsetDp = if (currentMode.isExpanded) {
                    (topOffsetDp + expandedOffsetYDp).coerceAtLeast(0f)
                } else {
                    topOffsetDp
                }
                androidx.compose.runtime.CompositionLocalProvider(
                    LocalExpandedNotchConfig provides ExpandedNotchConfig(
                        notchWidthDp = expandedNotchWidthDp,
                        notchDepthDp = expandedNotchDepthDp,
                        expandedOffsetXDp = expandedOffsetXDp,
                    )
                ) {
                Box(
                    modifier = Modifier
                        .offset(x = if (currentMode.isExpanded) 0.dp else horizontalOffsetDp.dp, y = previewYOffsetDp.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            // Tap toggles compact <-> expanded equivalent
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            currentMode = when (currentMode) {
                                IslandPreviewMode.IDLE -> IslandPreviewMode.NOTIF_EXPANDED
                                IslandPreviewMode.NOTIF_COMPACT -> IslandPreviewMode.NOTIF_EXPANDED
                                IslandPreviewMode.NOTIF_EXPANDED -> IslandPreviewMode.NOTIF_COMPACT
                                IslandPreviewMode.MUSIC_COMPACT -> IslandPreviewMode.MUSIC_EXPANDED
                                IslandPreviewMode.MUSIC_EXPANDED -> IslandPreviewMode.MUSIC_COMPACT
                                IslandPreviewMode.CHARGING -> IslandPreviewMode.IDLE
                                IslandPreviewMode.BLUETOOTH_COMPACT -> IslandPreviewMode.BLUETOOTH_EXPANDED
                                IslandPreviewMode.BLUETOOTH_EXPANDED -> IslandPreviewMode.BLUETOOTH_COMPACT
                            }
                            onModeChanged?.invoke(currentMode)
                        }
                ) {
                    val effectiveBg = when (theme) {
                        com.nothing.island.ai.ui.theme.IslandSignatureTheme.HIGH_CONTRAST_INVERTED -> Color(0xFFF2F2F2)
                        com.nothing.island.ai.ui.theme.IslandSignatureTheme.GLASSMORPHIC_FROSTED -> Color(0xD1222222)
                        com.nothing.island.ai.ui.theme.IslandSignatureTheme.TAPE_DECK -> Color(0xFF1E1E1E)
                        else -> pillBackgroundColor
                    }
                    val effectiveAccent = when (theme) {
                        com.nothing.island.ai.ui.theme.IslandSignatureTheme.GLYPH_ARC -> Color.White
                        else -> accentColor
                    }

                    when (currentMode) {
                        IslandPreviewMode.IDLE -> PreviewIdlePill(pillWidthDp, pillHeightDp, cornerRadiusDp, effectiveBg, effectiveAccent)
                        IslandPreviewMode.NOTIF_COMPACT -> PreviewNotifCompact(effectiveBg, effectiveAccent, theme)
                        IslandPreviewMode.NOTIF_EXPANDED -> PreviewNotifExpanded(effectiveBg, effectiveAccent, isCameraCutoutEnabled, theme)
                        IslandPreviewMode.MUSIC_COMPACT -> PreviewMusicCompact(effectiveBg, effectiveAccent, visualizerStyle, theme)
                        IslandPreviewMode.MUSIC_EXPANDED -> PreviewMusicExpanded(effectiveBg, effectiveAccent, visualizerStyle, isCameraCutoutEnabled, theme)
                        IslandPreviewMode.CHARGING -> PreviewChargingPill(effectiveBg, effectiveAccent)
                        IslandPreviewMode.BLUETOOTH_COMPACT -> PreviewBluetoothCompact(effectiveBg, effectiveAccent)
                        IslandPreviewMode.BLUETOOTH_EXPANDED -> PreviewBluetoothExpanded(effectiveBg, effectiveAccent)
                    }

                    // Signature Theme Compact Overlays
                    if (!currentMode.isExpanded) {
                        when (theme) {
                            com.nothing.island.ai.ui.theme.IslandSignatureTheme.GLYPH_ARC -> {
                                com.nothing.island.ai.ui.theme.GlyphArcCompactDecorations(arcColor = Color.White)
                            }
                            com.nothing.island.ai.ui.theme.IslandSignatureTheme.TAPE_DECK -> {
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .padding(horizontal = 8.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    com.nothing.island.ai.ui.theme.TapeDeckReelsIndicator(
                                        isPlaying = currentMode == IslandPreviewMode.MUSIC_COMPACT,
                                        sizeDp = 10.dp
                                    )
                                }
                            }
                            else -> Unit
                        }
                    }
                }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Clarifying simulated footer
            Text(
                text = "SIMULATED PREVIEW — TAP TO TOGGLE EXPANSION",
                color = NothingTokens.NothingTextTertiary,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 0.8.sp
            )
        }
    }
}

// =============================================================================
// Simulated Preview State Composables
// =============================================================================

@Composable
private fun PreviewIdlePill(widthDp: Float, heightDp: Float, radiusDp: Float, bg: Color, accent: Color) {
    Box(
        modifier = Modifier
            .width(widthDp.dp)
            .height(heightDp.dp)
            .clip(RoundedCornerShape(radiusDp.dp))
            .background(bg)
            .border(1.dp, accent.copy(alpha = 0.55f), RoundedCornerShape(radiusDp.dp)),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(accent)
            )
            Spacer(modifier = Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(accent)
            )
        }
    }
}

@Composable
private fun PreviewNotifCompact(
    bg: Color,
    accent: Color,
    theme: com.nothing.island.ai.ui.theme.IslandSignatureTheme = com.nothing.island.ai.ui.theme.IslandSignatureTheme.NOTHING_CLASSIC
) {
    val borderColor = when {
        theme.isGlassmorphic -> Color(0x45FFFFFF)
        theme.isInverted -> Color(0xFFCCCCCC)
        else -> accent.copy(alpha = 0.6f)
    }
    Box(
        modifier = Modifier
            .width(184.dp)
            .height(36.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(bg)
            .border(1.dp, borderColor, RoundedCornerShape(18.dp))
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App Icon (WhatsApp green simulation)
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF25D366)),
                contentAlignment = Alignment.Center
            ) {
                Text("W", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.width(6.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "WhatsApp",
                    color = theme.textColorPrimary,
                    fontSize = 11.sp,
                    fontFamily = theme.bodyFont,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Text(
                    text = "Meeting at 3 PM?",
                    color = theme.textColorSecondary,
                    fontSize = 9.sp,
                    fontFamily = theme.bodyFont,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Text(
                text = "Now",
                color = theme.textColorSecondary,
                fontSize = 9.sp,
                fontFamily = theme.displayFont
            )
        }
    }
}

@Composable
private fun PreviewNotifExpanded(
    bg: Color,
    accent: Color,
    isCameraCutoutEnabled: Boolean = false,
    theme: com.nothing.island.ai.ui.theme.IslandSignatureTheme = com.nothing.island.ai.ui.theme.IslandSignatureTheme.NOTHING_CLASSIC
) {
    val shape = if (isCameraCutoutEnabled) currentNotchedCardShape(cornerRadiusDp = 22f) else RoundedCornerShape(22.dp)
    val cardBorder = when {
        theme.isGlassmorphic -> Color(0x38FFFFFF)
        theme.isInverted -> Color(0xFFCCCCCC)
        else -> NothingTokens.NothingBorderProminent
    }
    Box(
        modifier = Modifier
            .width(300.dp)
            .height(160.dp)
            .clip(shape)
            .background(bg)
            .border(1.dp, cardBorder, shape)
            .padding(12.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF25D366)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("W", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "WHATSAPP • 2m ago",
                    color = theme.textColorSecondary,
                    fontSize = 10.sp,
                    fontFamily = theme.bodyFont,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "CLEAR ALL",
                    color = accent,
                    fontSize = 10.sp,
                    fontFamily = theme.bodyFont,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Body Row
            Row(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(if (theme.isInverted) Color(0xFFE5E5E5) else NothingTokens.NothingSurfaceHigh),
                    contentAlignment = Alignment.Center
                ) {
                    Text("JD", color = theme.textColorPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "John Doe",
                        color = theme.textColorPrimary,
                        fontSize = 13.sp,
                        fontFamily = theme.displayFont,
                        fontWeight = theme.displayFontWeight
                    )
                    Text(
                        text = "Are we still on for the design review meeting today?",
                        color = theme.textColorSecondary,
                        fontSize = 11.sp,
                        fontFamily = theme.bodyFont,
                        maxLines = 2,
                        lineHeight = 15.sp
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Action Buttons
            val chipBg = when {
                theme.isInverted -> Color(0xFFE5E5E5)
                theme.isGlassmorphic -> Color(0xB3333333)
                else -> NothingTokens.NothingSurfaceMedium
            }
            val chipBorder = when {
                theme.isGlassmorphic -> Color(0x28FFFFFF)
                theme.isInverted -> Color(0xFFCCCCCC)
                else -> NothingTokens.NothingBorderSubtle
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                PreviewActionChip("REPLY", theme.textColorPrimary, chipBg, chipBorder, theme.bodyFont)
                PreviewActionChip("MARK READ", theme.textColorSecondary, chipBg, chipBorder, theme.bodyFont)
                PreviewActionChip("MUTE", theme.textColorSecondary, chipBg, chipBorder, theme.bodyFont)
            }
        }
    }
}

@Composable
private fun PreviewActionChip(
    text: String,
    textColor: Color,
    bg: Color = NothingTokens.NothingSurfaceMedium,
    borderColor: Color = NothingTokens.NothingBorderSubtle,
    fontFamily: FontFamily = FontFamily.Monospace
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 9.sp,
            fontFamily = fontFamily,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun PreviewMusicCompact(
    bg: Color,
    accent: Color,
    visualizerStyle: MusicVisualizerStyle,
    theme: com.nothing.island.ai.ui.theme.IslandSignatureTheme = com.nothing.island.ai.ui.theme.IslandSignatureTheme.NOTHING_CLASSIC
) {
    val infiniteTransition = rememberInfiniteTransition(label = "VinylSpin")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "VinylAngle"
    )

    val compactBorderColor = when {
        theme.isGlassmorphic -> Color(0x45FFFFFF)
        theme.isInverted -> Color(0xFFCCCCCC)
        else -> NothingTokens.NothingBorderSubtle
    }

    Box(
        modifier = Modifier
            .width(190.dp)
            .height(36.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(bg)
            .border(1.dp, compactBorderColor, RoundedCornerShape(18.dp)),
        contentAlignment = Alignment.Center
    ) {
        com.nothing.island.ai.ui.theme.CompactMusicProgressBorder(
            progressFraction = 0.42f,
            accentColor = theme.compactProgressColor,
            cornerRadiusDp = 18.dp,
            modifier = Modifier.matchParentSize()
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Rotating Vinyl Disc
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .rotate(rotation)
                    .clip(CircleShape)
                    .background(if (theme.isInverted) Color(0xFFE5E5E5) else Color(0xFF151515))
                    .border(1.dp, if (theme.isInverted) Color(0xFFCCCCCC) else Color(0xFF333333), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(accent)
                )
            }

            Text(
                text = "Circles",
                color = theme.textColorPrimary,
                fontSize = 11.sp,
                fontFamily = theme.bodyFont,
                fontWeight = theme.bodyFontWeight,
                maxLines = 1
            )

            // Animated visualizer reflecting the selected style
            PreviewMusicVisualizer(visualizerStyle, if (theme == com.nothing.island.ai.ui.theme.IslandSignatureTheme.GLYPH_ARC) Color.White else NothingTokens.NothingBlue)
        }
    }
}

@Composable
private fun PreviewMusicExpanded(
    bg: Color,
    accent: Color,
    visualizerStyle: MusicVisualizerStyle,
    isCameraCutoutEnabled: Boolean = false,
    theme: com.nothing.island.ai.ui.theme.IslandSignatureTheme = com.nothing.island.ai.ui.theme.IslandSignatureTheme.NOTHING_CLASSIC
) {
    val shape = if (isCameraCutoutEnabled) currentNotchedCardShape(cornerRadiusDp = 22f) else RoundedCornerShape(22.dp)
    val cardBorder = when {
        theme.isGlassmorphic -> Color(0x38FFFFFF)
        theme.isInverted -> Color(0xFFCCCCCC)
        else -> NothingTokens.NothingBorderProminent
    }

    Box(
        modifier = Modifier
            .width(300.dp)
            .height(160.dp)
            .clip(shape)
            .background(bg)
            .border(1.dp, cardBorder, shape)
            .padding(12.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Top Row: Vinyl + Info + Waveform
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (theme.isInverted) Color(0xFFE5E5E5) else Color(0xFF181818))
                        .border(1.dp, if (theme.isInverted) Color(0xFFCCCCCC) else Color(0xFF333333), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(accent))
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Circles",
                        color = theme.textColorPrimary,
                        fontSize = 13.sp,
                        fontFamily = theme.displayFont,
                        fontWeight = theme.displayFontWeight,
                        maxLines = 1
                    )
                    Text(
                        text = "Post Malone",
                        color = theme.textColorSecondary,
                        fontSize = 11.sp,
                        fontFamily = theme.bodyFont,
                        maxLines = 1
                    )
                }

                PreviewMusicVisualizer(visualizerStyle, if (theme == com.nothing.island.ai.ui.theme.IslandSignatureTheme.GLYPH_ARC) Color.White else NothingTokens.NothingBlue)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Track Progress Bar / LED
            if (theme == com.nothing.island.ai.ui.theme.IslandSignatureTheme.GLYPH_ARC) {
                com.nothing.island.ai.ui.theme.SegmentedLedProgressBar(
                    progress = 0.42f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(14.dp)
                        .padding(vertical = 2.dp),
                    activeColor = Color.White
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("1:30", color = theme.textColorSecondary, fontSize = 9.sp, fontFamily = theme.displayFont)
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(if (theme.isInverted) Color(0xFFCCCCCC) else NothingTokens.NothingSurfaceHighest)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.42f)
                                .fillMaxHeight()
                                .background(if (theme == com.nothing.island.ai.ui.theme.IslandSignatureTheme.TAPE_DECK) Color(0xFFD71921) else theme.accentColor)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("3:35", color = theme.textColorSecondary, fontSize = 9.sp, fontFamily = theme.displayFont)
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Transport Controls: (10) |<< || >>| (10)
            val btnColor = if (theme.isInverted) Color(0xFF000000) else NothingTokens.NothingTextPrimary
            val btnMuted = if (theme.isInverted) Color(0xFF666666) else NothingTokens.NothingTextSecondary
            val centerBtnBg = when {
                theme.isInverted -> Color(0xFFE5E5E5)
                theme == com.nothing.island.ai.ui.theme.IslandSignatureTheme.GLYPH_ARC -> Color(0xFF1C1C1C)
                theme.isGlassmorphic -> Color(0xB3333333)
                else -> NothingTokens.NothingSurfaceMedium
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("(10)", color = btnMuted, fontSize = 11.sp, fontFamily = theme.bodyFont, fontWeight = FontWeight.Bold)
                Text("|<<", color = btnColor, fontSize = 13.sp, fontFamily = theme.bodyFont, fontWeight = FontWeight.Bold)
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(centerBtnBg)
                        .then(if (theme.isGlassmorphic) Modifier.border(1.dp, Color(0x28FFFFFF), CircleShape) else Modifier),
                    contentAlignment = Alignment.Center
                ) {
                    Text("||", color = btnColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Text(">>|", color = btnColor, fontSize = 13.sp, fontFamily = theme.bodyFont, fontWeight = FontWeight.Bold)
                Text("(10)", color = btnMuted, fontSize = 11.sp, fontFamily = theme.bodyFont, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun PreviewChargingPill(bg: Color, accent: Color) {
    Box(
        modifier = Modifier
            .width(180.dp)
            .height(36.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(bg)
            .border(1.dp, accent, RoundedCornerShape(18.dp))
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier.size(24.dp),
                contentAlignment = Alignment.Center
            ) {
                ChargingAnimationHost(
                    style = ChargingAnimationStyle.GLYPH_ARC,
                    percentage = 68,
                    isCharging = true,
                    accentColor = accent,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Text(
                text = "CHARGING 68%",
                color = accent,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
            // Circular progress ring
            PreviewProgressRing(0.68f, accent)
        }
    }
}

@Composable
private fun PreviewBluetoothCompact(bg: Color, accent: Color) {
    Box(
        modifier = Modifier
            .width(160.dp)
            .height(36.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(bg)
            .border(1.dp, NothingTokens.NothingBorderSubtle, RoundedCornerShape(18.dp))
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("🎧", fontSize = 12.sp)
            Text(
                text = "Ear (2)",
                color = NothingTokens.NothingTextPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
            PreviewProgressRing(0.75f, accent, "75")
        }
    }
}

@Composable
private fun PreviewBluetoothExpanded(bg: Color, accent: Color) {
    Box(
        modifier = Modifier
            .width(260.dp)
            .height(64.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .border(1.dp, NothingTokens.NothingBorderProminent, RoundedCornerShape(20.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("🎧", fontSize = 20.sp)
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "CONNECTED",
                    color = accent,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Nothing Ear (2)",
                    color = NothingTokens.NothingTextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            PreviewProgressRing(0.75f, accent, "75%")
        }
    }
}

@Composable
private fun PreviewProgressRing(
    progress: Float,
    color: Color,
    centerText: String? = null
) {
    Box(
        modifier = Modifier.size(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(22.dp)) {
            // Background track
            drawCircle(
                color = Color(0x33FFFFFF),
                style = Stroke(width = 2.dp.toPx())
            )
            // Progress arc
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
            )
        }
        if (centerText != null) {
            Text(
                text = centerText,
                color = color,
                fontSize = 7.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/** Live visualizer that visually changes with the selected music style. */
@Composable
private fun PreviewMusicVisualizer(style: MusicVisualizerStyle, color: Color) {
    val t = rememberInfiniteTransition(label = "visStyle")
    val phase by t.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(700, easing = LinearEasing), RepeatMode.Reverse),
        label = "visPhase"
    )
    when (style) {
        MusicVisualizerStyle.VERTICAL_EQUALIZER -> PreviewWaveformBars(color)
        MusicVisualizerStyle.WAVEFORM -> {
            Canvas(modifier = Modifier.size(width = 34.dp, height = 18.dp)) {
                val w = size.width; val h = size.height
                val path = androidx.compose.ui.graphics.Path()
                path.moveTo(0f, h / 2f)
                for (x in 0..w.toInt() step 4) {
                    val y = h / 2f + kotlin.math.sin((x / w) * 6.28f + phase * 6.28f) * (h * 0.35f)
                    path.lineTo(x.toFloat(), y)
                }
                drawPath(
                    path, color,
                    style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
                )
            }
        }
        MusicVisualizerStyle.CIRCULAR -> {
            Box(modifier = Modifier.size(18.dp), contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.size(18.dp)) {
                    drawCircle(color, radius = size.minDimension / 2f, style = Stroke(1.5.dp.toPx()))
                    drawArc(color, -90f, 280f * phase, false, style = Stroke(1.5.dp.toPx(), cap = StrokeCap.Round))
                }
            }
        }
        MusicVisualizerStyle.VINYL -> {
            Box(modifier = Modifier.size(18.dp).rotate(phase * 360f), contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.size(18.dp)) {
                    drawCircle(Color(0xFF333333), radius = size.minDimension / 2f)
                    drawCircle(color, radius = size.minDimension / 6f)
                }
            }
        }
        MusicVisualizerStyle.MINIMAL_PULSE -> {
            Box(modifier = Modifier.size(18.dp), contentAlignment = Alignment.Center) {
                Box(modifier = Modifier.size((8 + phase * 8).dp).clip(CircleShape).background(color.copy(alpha = 0.5f)))
                Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(color))
            }
        }
    }
}

@Composable
private fun PreviewWaveformBars(color: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "WaveformAnim")
    val bar1 by infiniteTransition.animateFloat(
        initialValue = 4f, targetValue = 16f,
        animationSpec = infiniteRepeatable(tween(350), RepeatMode.Reverse), label = "b1"
    )
    val bar2 by infiniteTransition.animateFloat(
        initialValue = 14f, targetValue = 6f,
        animationSpec = infiniteRepeatable(tween(420), RepeatMode.Reverse), label = "b2"
    )
    val bar3 by infiniteTransition.animateFloat(
        initialValue = 8f, targetValue = 18f,
        animationSpec = infiniteRepeatable(tween(300), RepeatMode.Reverse), label = "b3"
    )
    val bar4 by infiniteTransition.animateFloat(
        initialValue = 12f, targetValue = 5f,
        animationSpec = infiniteRepeatable(tween(480), RepeatMode.Reverse), label = "b4"
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom,
        modifier = Modifier.height(18.dp)
    ) {
        Box(modifier = Modifier.width(2.dp).height(bar1.dp).background(color, RoundedCornerShape(1.dp)))
        Box(modifier = Modifier.width(2.dp).height(bar2.dp).background(color, RoundedCornerShape(1.dp)))
        Box(modifier = Modifier.width(2.dp).height(bar3.dp).background(color, RoundedCornerShape(1.dp)))
        Box(modifier = Modifier.width(2.dp).height(bar4.dp).background(color, RoundedCornerShape(1.dp)))
    }
}
