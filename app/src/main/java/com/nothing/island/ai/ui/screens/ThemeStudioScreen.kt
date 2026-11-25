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
 * Nothing OS 3.x Theme Studio Screen
 *
 * Implements 5 Signature Themes:
 * Nothing Classic, Glyph Arc, Tape Deck, Glassmorphic Frosted, High-Contrast Inverted.
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
                    text = "5 SIGNATURE THEMES",
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

        Spacer(modifier = Modifier.height(24.dp))
    }
}

