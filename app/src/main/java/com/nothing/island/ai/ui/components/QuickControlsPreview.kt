package com.nothing.island.ai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.nothing.island.ai.data.IslandPreferencesRepository
import com.nothing.island.ai.ui.theme.NothingPrimaryAccent
import com.nothing.island.ai.ui.theme.NothingPrimaryText
import com.nothing.island.ai.ui.theme.NothingSecondaryText
import com.nothing.island.ai.ui.theme.NothingTokens
import kotlinx.coroutines.launch

/**
 * Reference-style Quick Controls settings: a live preview panel whose toggles
 * control which tools appear in the island's quick controls drawer.
 */
@Composable
fun QuickControlsPreview(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val repository = remember { IslandPreferencesRepository.getInstance(context) }
    val scope = rememberCoroutineScope()
    val enabled by repository.quickToolsFlow().collectAsState(initial = QuickTools.DEFAULT.toSet())
    var previewVolume by remember { mutableFloatStateOf(0.75f) }
    var previewBrightness by remember { mutableFloatStateOf(0.6f) }

    fun toggle(id: String) {
        val next = if (id in enabled) enabled - id else enabled + id
        scope.launch { repository.updateQuickTools(next) }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0xFF080808))
            .border(1.dp, NothingTokens.NothingBorderSubtle, RoundedCornerShape(22.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Quick Controls Preview", color = NothingSecondaryText, fontSize = 14.sp)
            Box(
                Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(NothingTokens.NothingSurfaceMedium)
                    .padding(horizontal = 12.dp, vertical = 5.dp)
            ) { Text("LIVE", color = NothingPrimaryText, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
        }

        Spacer(Modifier.height(14.dp))

        // Tool toggles grid — reflects and controls the real overlay tools
        val rows = QuickTools.ALL.chunked(4)
        rows.forEach { rowItems ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowItems.forEach { id ->
                    val active = id in enabled
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (active) NothingPrimaryAccent.copy(alpha = 0.3f) else NothingTokens.NothingSurfaceMedium)
                                .border(1.dp, if (active) NothingPrimaryAccent else NothingTokens.NothingBorderSubtle, RoundedCornerShape(14.dp))
                                .clickable { toggle(id) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = QuickTools.glyph(id),
                                color = if (active) Color.White else NothingSecondaryText,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        Text(
                            text = QuickTools.label(id),
                            color = if (active) Color.White else NothingSecondaryText,
                            fontSize = 7.5.sp,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                        )
                    }
                }
                repeat(4 - rowItems.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Hardware Controls Preview: Volume & Brightness Sliders
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            NothingSliderControl(
                glyph = "♪",
                label = "VOLUME",
                percent = previewVolume,
                onValueChange = { previewVolume = it },
                activeColor = NothingPrimaryAccent
            )

            NothingSliderControl(
                glyph = "☀",
                label = "BRIGHTNESS",
                percent = previewBrightness,
                onValueChange = { previewBrightness = it },
                activeColor = Color(0xFFFFD54F)
            )
        }
    }
}
