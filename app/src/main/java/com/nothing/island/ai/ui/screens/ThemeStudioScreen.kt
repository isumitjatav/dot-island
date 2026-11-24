package com.nothing.island.ai.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.nothing.island.ai.data.IslandPreferencesRepository
import com.nothing.island.ai.ui.components.IslandPreview
import com.nothing.island.ai.ui.components.IslandPreviewMode
import com.nothing.island.ai.ui.components.NothingSectionHeader
import com.nothing.island.ai.ui.theme.GlyphArcCompactDecorations
import com.nothing.island.ai.ui.theme.IslandSignatureTheme
import com.nothing.island.ai.ui.theme.NothingTokens
import com.nothing.island.ai.ui.theme.TapeDeckReelsIndicator

/**
 * Data model for Theme Presets
 */
data class ThemePresetItem(
    val name: String,
    val primaryColor: Color,
    val isOriginal: Boolean = true
)

/**
 * Preserves true hue for dark swatches instead of clamping everything to Nothing Red.
 * Only pure black/near-zero defaults to Nothing Red for visibility.
 */
private fun effectiveAccent(color: Color): Color {
    if (color == NothingTokens.NothingBlack || color == Color.Transparent) {
        return NothingTokens.NothingRed
    }
    val luminance = 0.299f * color.red + 0.587f * color.green + 0.114f * color.blue
    if (luminance < 0.18f) {
        // Boost dark swatches (Midnight, Deep Space, Forest Rain) so they pop against black without losing their hue
        return Color(
            red = (color.red * 2.2f + 0.15f).coerceAtMost(1f),
            green = (color.green * 2.2f + 0.15f).coerceAtMost(1f),
            blue = (color.blue * 2.2f + 0.25f).coerceAtMost(1f),
            alpha = 1f
        )
    }
    return color
}

/** Near-black pill background with a subtle tint of the chosen accent. */
private fun backgroundFor(accent: Color): Color = Color(
    red = (0.025f + accent.red * 0.05f).coerceAtMost(0.12f),
    green = (0.025f + accent.green * 0.05f).coerceAtMost(0.12f),
    blue = (0.025f + accent.blue * 0.05f).coerceAtMost(0.12f),
    alpha = 1f
)

/**
 * Nothing OS 3.x Theme Studio Screen
 *
 * Implements:
 * 1. 5 Signature Themes (Nothing Classic, Glyph Arc, Tape Deck, Glassmorphic Frosted, High-Contrast Inverted)
 * 2. 2-column grid gallery of 32+ named colorway presets
 */
@Composable
fun ThemeStudioScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repository = remember { IslandPreferencesRepository.getInstance(context) }
    val prefs by repository.livePreferences.collectAsState()
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    val selectedPreset = prefs.presetName.ifBlank { "Nothing Classic" }
    var toastMessage by remember { mutableStateOf<String?>(null) }
    val activeSignatureTheme = remember(selectedPreset) { IslandSignatureTheme.resolve(selectedPreset) }

    val presetList = remember {
        listOf(
            ThemePresetItem("Classic Black", NothingTokens.NothingBlack),
            ThemePresetItem("Minimal Gray", Color(0xFF1E1E1E)),
            ThemePresetItem("Midnight", Color(0xFF0D1B2A)),
            ThemePresetItem("Amethyst", Color(0xFF5E239D)),
            ThemePresetItem("Sunset", Color(0xFFD35400)),
            ThemePresetItem("Neon Cyber", Color(0xFF00FFCC)),
            ThemePresetItem("Royal Gold", Color(0xFFD4AF37)),
            ThemePresetItem("Ocean Breeze", Color(0xFF0077B6)),
            ThemePresetItem("Crimson Fire", Color(0xFFC0392B)),
            ThemePresetItem("Deep Space", Color(0xFF1A1A24)),
            ThemePresetItem("Forest Rain", Color(0xFF2D5A27)),
            ThemePresetItem("Purple Haze", Color(0xFF6B4C9A)),
            ThemePresetItem("Cherry Blossom", Color(0xFFFFB7B2)),
            ThemePresetItem("Golden Hour", Color(0xFFE5A93B)),
            ThemePresetItem("The Matrix", Color(0xFF00FF41)),
            ThemePresetItem("Cyber Pink", Color(0xFFFF2A85)),
            ThemePresetItem("Mint Fresh", Color(0xFF2ECC71)),
            ThemePresetItem("Silver Lining", Color(0xFFBDC3C7)),
            ThemePresetItem("Carbon Fiber", Color(0xFF2C3E50)),
            ThemePresetItem("Electric Blue", Color(0xFF00D2FF)),
            ThemePresetItem("Sweet Candy", Color(0xFFFF70A6)),
            ThemePresetItem("Northern Lights", Color(0xFF00E5FF)),
            ThemePresetItem("Pure White", Color(0xFFFFFFFF)),
            ThemePresetItem("Vampire Red", Color(0xFF8B0000)),
            ThemePresetItem("Emerald City", Color(0xFF50C878)),
            ThemePresetItem("Cosmic Void", Color(0xFF050510)),
            ThemePresetItem("Rose Gold", Color(0xFFB76E79)),
            ThemePresetItem("Ice Cold", Color(0xFFA8DADC)),
            ThemePresetItem("Lemon Twist", Color(0xFFF1C40F)),
            ThemePresetItem("Twilight", Color(0xFF34495E)),
            ThemePresetItem("Hot Lava", Color(0xFFFF4500)),
            ThemePresetItem("Sea Foam", Color(0xFF70C1B3))
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NothingTokens.NothingBlack)
            .verticalScroll(scrollState)
            .padding(horizontal = NothingTokens.ScreenHorizontalPadding, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Back Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(NothingTokens.NothingSurfaceMedium)
                    .border(1.dp, NothingTokens.NothingBorderSubtle, CircleShape)
                    .clickable { onNavigateBack() },
                contentAlignment = Alignment.Center
            ) {
                Text(text = "<", color = NothingTokens.NothingTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "THEME STUDIO",
                    color = NothingTokens.NothingTextPrimary,
                    fontSize = 18.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "5 SIGNATURE THEMES + 32 PRESETS",
                    color = NothingTokens.NothingTextTertiary,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Live preview reflects the theme currently applied to the real overlay.
        IslandPreview(
            pillBackgroundColor = Color(prefs.pillBackgroundColorHex),
            accentColor = Color(prefs.accentColorHex),
            initialMode = IslandPreviewMode.NOTIF_COMPACT,
            theme = activeSignatureTheme
        )

        // Animated confirmation badge
        AnimatedVisibility(
            visible = toastMessage != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            toastMessage?.let { msg ->
                Box(
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF1E1E1E))
                        .border(1.dp, activeSignatureTheme.accentColor, RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = msg,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // =====================================================================
        // SECTION 1: 5 SIGNATURE THEMES
        // =====================================================================
        NothingSectionHeader(title = "SIGNATURE THEMES", isAccent = true)

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            IslandSignatureTheme.entries.forEach { signatureTheme ->
                val isSelected = selectedPreset.equals(signatureTheme.displayName, ignoreCase = true)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(NothingTokens.ControlRadius))
                        .background(
                            when {
                                isSelected -> NothingTokens.NothingSurfaceMedium
                                else -> NothingTokens.NothingSurfaceLow
                            }
                        )
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) signatureTheme.accentColor else NothingTokens.NothingBorderSubtle,
                            shape = RoundedCornerShape(NothingTokens.ControlRadius)
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            toastMessage = "APPLIED: ${signatureTheme.displayName.uppercase()}"
                            scope.launch {
                                repository.updatePillTheme(
                                    backgroundHex = signatureTheme.pillBackgroundColorHex,
                                    accentHex = signatureTheme.accentColorHex,
                                    presetName = signatureTheme.displayName,
                                )
                                delay(2000)
                                toastMessage = null
                            }
                        }
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            // Mini dynamic badge representing theme
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(signatureTheme.pillBackgroundColor)
                                    .border(1.dp, signatureTheme.borderColor, RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                when (signatureTheme) {
                                    IslandSignatureTheme.NOTHING_CLASSIC -> {
                                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(signatureTheme.accentColor))
                                    }
                                    IslandSignatureTheme.GLYPH_ARC -> {
                                        GlyphArcCompactDecorations(arcColor = Color.White)
                                    }
                                    IslandSignatureTheme.TAPE_DECK -> {
                                        TapeDeckReelsIndicator(isPlaying = true, sizeDp = 10.dp)
                                    }
                                    IslandSignatureTheme.GLASSMORPHIC_FROSTED -> {
                                        Box(
                                            modifier = Modifier
                                                .size(20.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color(0x38FFFFFF))
                                                .border(1.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                                        )
                                    }
                                    IslandSignatureTheme.HIGH_CONTRAST_INVERTED -> {
                                        Box(
                                            modifier = Modifier
                                                .size(16.dp, 8.dp)
                                                .clip(RoundedCornerShape(2.dp))
                                                .background(signatureTheme.accentColor)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Text(
                                    text = signatureTheme.displayName,
                                    color = if (isSelected) NothingTokens.NothingTextPrimary else NothingTokens.NothingTextSecondary,
                                    fontSize = 13.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = signatureTheme.description,
                                    color = NothingTokens.NothingTextTertiary,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    maxLines = 1
                                )
                            }
                        }

                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(signatureTheme.accentColor.copy(alpha = 0.2f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "ACTIVE",
                                    color = signatureTheme.accentColor,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // =====================================================================
        // SECTION 2: 32 COLORWAY PRESET GALLERY
        // =====================================================================
        NothingSectionHeader(title = "PRESET COLORWAY GALLERY", isAccent = false)

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            presetList.chunked(2).forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowItems.forEach { preset ->
                        val isSelected = preset.name == selectedPreset
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(NothingTokens.ControlRadius))
                                .background(NothingTokens.NothingSurfaceLow)
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) NothingTokens.NothingRed else NothingTokens.NothingBorderSubtle,
                                    shape = RoundedCornerShape(NothingTokens.ControlRadius)
                                )
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    val accent = effectiveAccent(preset.primaryColor)
                                    val bg = backgroundFor(accent)
                                    val accentHex = (accent.toArgb().toLong()) and 0xFFFFFFFFL
                                    val bgArgb = (bg.toArgb().toLong()) and 0xFFFFFFFFL
                                    val safeBgHex = if (bgArgb == 0L || (bgArgb and 0xFF000000L) == 0L) 0xFF000000L else bgArgb
                                    toastMessage = "APPLIED: ${preset.name.uppercase()}"
                                    scope.launch {
                                        repository.updatePillTheme(
                                            backgroundHex = safeBgHex,
                                            accentHex = accentHex,
                                            presetName = preset.name,
                                        )
                                        delay(2000)
                                        toastMessage = null
                                    }
                                }
                                .padding(12.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(preset.primaryColor)
                                            .border(1.dp, NothingTokens.NothingBorderSubtle, CircleShape)
                                    )
                                    if (isSelected) {
                                        Text(
                                            text = "✓",
                                            color = NothingTokens.NothingRed,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = preset.name,
                                    color = if (isSelected) NothingTokens.NothingTextPrimary else NothingTokens.NothingTextSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                    if (rowItems.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

