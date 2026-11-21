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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nothing.island.ai.data.IslandPreferencesRepository
import com.nothing.island.ai.ui.components.MascotPicker
import com.nothing.island.ai.ui.components.MascotPillPreview
import com.nothing.island.ai.ui.components.MascotSprites
import com.nothing.island.ai.ui.components.mascotByName
import com.nothing.island.ai.ui.theme.NothingCardBackground
import com.nothing.island.ai.ui.theme.NothingDivider
import com.nothing.island.ai.ui.theme.NothingMutedText
import com.nothing.island.ai.ui.theme.NothingPrimaryAccent
import com.nothing.island.ai.ui.theme.NothingPrimaryBackground
import com.nothing.island.ai.ui.theme.NothingPrimaryText
import com.nothing.island.ai.ui.theme.NothingSecondaryText
import com.nothing.island.ai.ui.theme.NothingTokens
import kotlinx.coroutines.launch

/** Full mascot settings screen: enable toggle, live preview, and sprite picker. */
@Composable
fun MascotStudioScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val repository = remember { IslandPreferencesRepository.getInstance(context) }
    val prefs by repository.livePreferences.collectAsState()
    val scope = rememberCoroutineScope()

    var selected by remember { mutableStateOf(mascotByName(prefs.mascotSpriteName)) }
    var enabled by remember { mutableStateOf(prefs.mascotEnabled) }

    fun persist() {
        scope.launch { repository.updateMascot(enabled, selected.name) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NothingPrimaryBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Back header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(NothingTokens.NothingSurfaceMedium)
                    .border(1.dp, NothingTokens.NothingBorderSubtle, CircleShape)
                    .clickable { onNavigateBack() },
                contentAlignment = Alignment.Center,
            ) {
                Text("<", color = NothingPrimaryText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    "MASCOT STUDIO",
                    color = NothingPrimaryText,
                    fontSize = 18.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "PIXEL COMPANIONS IN YOUR ISLAND",
                    color = NothingMutedText,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Live preview
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(NothingCardBackground)
                .border(1.dp, NothingDivider, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                MascotPillPreview(sprite = selected)
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "${selected.name} · TAP TO JUMP",
                    color = NothingSecondaryText,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp,
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Enable toggle
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(NothingCardBackground)
                .border(1.dp, NothingDivider, RoundedCornerShape(14.dp))
                .clickable {
                    enabled = !enabled
                    persist()
                }
                .padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(
                        "Enable Pixel Mascot",
                        color = NothingPrimaryText,
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "Walk inside the island when idle",
                        color = NothingSecondaryText,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                    )
                }
                Switch(
                    checked = enabled,
                    onCheckedChange = { value ->
                        enabled = value
                        persist()
                    },
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Sprite picker
        Text(
            text = "CHOOSE YOUR COMPANION",
            color = NothingPrimaryAccent,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 2.sp,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(NothingCardBackground)
                .border(1.dp, NothingDivider, RoundedCornerShape(14.dp))
                .padding(16.dp),
        ) {
            MascotPicker(
                selected = selected,
                onSelect = { sprite ->
                    selected = sprite
                    persist()
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(Modifier.height(24.dp))
    }
}
