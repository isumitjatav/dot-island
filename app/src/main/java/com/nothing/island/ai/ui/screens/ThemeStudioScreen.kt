package com.nothing.island.ai.ui.screens

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.nothing.island.ai.data.IslandPreferencesRepository
import com.nothing.island.ai.ui.components.IslandPreview
import com.nothing.island.ai.ui.components.NothingCard
import com.nothing.island.ai.ui.components.NothingCardVariant
import com.nothing.island.ai.ui.components.NothingSectionHeader
import com.nothing.island.ai.ui.theme.NothingTokens

/**
 * Data model for Theme Presets discovered in reference screenshots
 */
data class ThemePresetItem(
    val name: String,
    val primaryColor: Color,
    val isOriginal: Boolean = true
)

/**
 * Nothing OS 3.x Theme Studio Screen
 *
 * Implements 2-column grid gallery of 32+ named colorway presets
 * (Classic Black, Minimal Gray, Neon Cyber, The Matrix, Sunset, etc.)
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

    var selectedPreset by remember { mutableStateOf(prefs.presetName) }

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
                    text = "32+ PRESET COLORWAYS",
                    color = NothingTokens.NothingTextTertiary,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        IslandPreview()

        Spacer(modifier = Modifier.height(16.dp))

        NothingSectionHeader(title = "PRESET COLORWAY GALLERY", isAccent = true)

        // 2-Column Grid
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
                                    selectedPreset = preset.name
                                    val argb = (preset.primaryColor.value.toLong() and 0xFFFFFFFFL)
                                    scope.launch {
                                        repository.updatePillTheme(
                                            backgroundHex = argb,
                                            accentHex = argb,
                                            presetName = preset.name,
                                        )
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
