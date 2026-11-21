package com.nothing.island.ai.ui.screens

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.nothing.island.ai.ui.components.IslandPreview
import com.nothing.island.ai.ui.components.NothingCard
import com.nothing.island.ai.ui.components.NothingCardVariant
import com.nothing.island.ai.ui.components.NothingPaletteChips
import com.nothing.island.ai.ui.components.NothingSectionHeader
import com.nothing.island.ai.ui.components.NothingSegmentedControl
import com.nothing.island.ai.ui.components.NothingStepperSlider
import com.nothing.island.ai.ui.components.NothingSwitchRow
import com.nothing.island.ai.ui.theme.NothingTokens

/**
 * Nothing OS 3.x Animation Studio Screen
 *
 * Implements physics motion curves (Liquid, Bouncy, Droplet) and
 * multi-color border glow controls with steppers and palette chips.
 */
@Composable
fun AnimationStudioScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val context = androidx.compose.ui.platform.LocalContext.current
    val repository = remember { com.nothing.island.ai.data.IslandPreferencesRepository.getInstance(context) }
    val savedPrefs by repository.animationPreferencesFlow().collectAsState(
        initial = com.nothing.island.ai.ui.AnimationPreferences.DEFAULTS
    )
    val scope = rememberCoroutineScope()

    var physicsMode by remember { mutableStateOf(savedPrefs.physicsMode) }
    var borderGlowEnabled by remember { mutableStateOf(savedPrefs.borderGlowEnabled) }
    var speedVal by remember { mutableStateOf(savedPrefs.glowSpeed) }
    var thicknessVal by remember { mutableStateOf(savedPrefs.glowThickness) }
    LaunchedEffect(savedPrefs) {
        physicsMode = savedPrefs.physicsMode
        borderGlowEnabled = savedPrefs.borderGlowEnabled
        speedVal = savedPrefs.glowSpeed
        thicknessVal = savedPrefs.glowThickness
    }
    fun persist(
        mode: String = physicsMode,
        glow: Boolean = borderGlowEnabled,
        speed: Float = speedVal,
        thickness: Float = thicknessVal,
    ) {
        scope.launch {
            repository.updateAnimation(
                com.nothing.island.ai.ui.AnimationPreferences(
                    physicsMode = mode,
                    borderGlowEnabled = glow,
                    glowSpeed = speed,
                    glowThickness = thickness,
                )
            )
        }
    }

    var paletteColors by remember {
        mutableStateOf(
            listOf(
                Color(0xFF007AFF),
                Color(0xFFFF3B30),
                Color(0xFFFFCC00),
                Color(0xFF34C759),
                Color(0xFF5856D6)
            )
        )
    }
    var selectedColor by remember { mutableStateOf<Color?>(paletteColors.firstOrNull()) }

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
                    text = "ANIMATION STUDIO",
                    color = NothingTokens.NothingTextPrimary,
                    fontSize = 18.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "PHYSICS CURVES & BORDER GLOW",
                    color = NothingTokens.NothingTextTertiary,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        IslandPreview()

        Spacer(modifier = Modifier.height(16.dp))

        // Section: Notch Swipe Physics
        NothingSectionHeader(title = "NOTCH SWIPE ANIMATION PHYSICS", isAccent = true)
        NothingCard(variant = NothingCardVariant.STANDARD) {
            Text(
                text = "Transition Dynamic Model",
                color = NothingTokens.NothingTextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Physics behavior when dual popups expand or contract in the small notch",
                color = NothingTokens.NothingTextSecondary,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(12.dp))

            val modes = listOf("LIQUID", "BOUNCY", "DROPLET")
            NothingSegmentedControl(
                items = modes,
                selectedItem = physicsMode,
                onItemSelected = { physicsMode = it; persist(mode = it) },
                labelProvider = { it }
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Section: Border Glow
        NothingSectionHeader(title = "BORDER GLOW ANIMATION")
        NothingCard(variant = NothingCardVariant.STANDARD) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                NothingSwitchRow(
                    title = "Enable Border Animation",
                    description = "Play customizable border glow animations around the cutout",
                    checked = borderGlowEnabled,
                    onCheckedChange = { borderGlowEnabled = it; persist(glow = it) }
                )

                NothingStepperSlider(
                    value = speedVal,
                    onValueChange = { speedVal = it; persist(speed = it) },
                    valueRange = 0.5f..3.0f,
                    stepSize = 0.1f,
                    label = "Animation Speed",
                    formattedValue = String.format("%.1fx", speedVal),
                    enabled = borderGlowEnabled
                )

                NothingStepperSlider(
                    value = thicknessVal,
                    onValueChange = { thicknessVal = it; persist(thickness = it) },
                    valueRange = 1.0f..4.0f,
                    stepSize = 0.5f,
                    label = "Border Thickness",
                    formattedValue = "${thicknessVal.toInt()} DP",
                    enabled = borderGlowEnabled
                )

                Text(
                    text = "GRADIENT PALETTE",
                    color = NothingTokens.NothingTextSecondary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )

                NothingPaletteChips(
                    colors = paletteColors,
                    selectedColor = selectedColor,
                    onColorSelected = { selectedColor = it },
                    onRemoveColor = { col ->
                        paletteColors = paletteColors.filter { it != col }
                    },
                    onAddColorClick = {
                        paletteColors = paletteColors + Color(0xFF00FFCC)
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
