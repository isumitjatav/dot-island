package com.nothing.island.ai.ui.components

import android.media.AudioManager
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nothing.island.ai.ui.theme.NothingPrimaryAccent
import com.nothing.island.ai.ui.theme.NothingPrimaryBackground
import com.nothing.island.ai.ui.theme.NothingPrimaryText
import com.nothing.island.ai.ui.theme.NothingSecondaryText
import com.nothing.island.ai.ui.theme.NothingSuccess
import com.nothing.island.ai.ui.theme.NothingTokens

/** Identifiers for the built-in quick tools. */
object QuickTools {
    const val DATA = "data"
    const val WIFI = "wifi"
    const val BLUETOOTH = "bluetooth"
    const val TORCH = "torch"
    const val LOCATION = "location"

    val DEFAULT = listOf(DATA, WIFI, BLUETOOTH, TORCH, LOCATION)

    fun label(id: String) = when (id) {
        DATA -> "MOBILE DATA"
        WIFI -> "WI-FI"
        BLUETOOTH -> "BLUETOOTH"
        TORCH -> "TORCH"
        LOCATION -> "LOCATION"
        else -> id.uppercase()
    }

    fun glyph(id: String) = when (id) {
        DATA -> "↕"
        WIFI -> "◕"
        BLUETOOTH -> "ᛒ"
        TORCH -> "⚡"
        LOCATION -> "⌖"
        else -> "●"
    }
}

data class QuickTool(
    val id: String,
    val label: String,
    val glyph: String,
    val active: Boolean = false,
    val action: () -> Unit,
)

/**
 * Premium reference-style overlay quick controls drawer: large tiles, app
 * shortcut and favorite contact rows, and a bottom volume/history/AI bar.
 */
@Composable
fun QuickToolsDrawer(
    tools: List<QuickTool>,
    onCollapse: () -> Unit,
    onVolumeChange: (Int) -> Unit = {},
    onHistory: () -> Unit = {},
    onAi: () -> Unit = {},
    modifier: Modifier = Modifier,
    isCameraCutoutEnabled: Boolean = false,
) {
    val context = LocalContext.current
    val scroll = rememberScrollState()
    val cardShape = if (isCameraCutoutEnabled) notchedCardShape(cornerRadiusDp = 24f) else RoundedCornerShape(24.dp)

    Column(
        modifier = modifier
            .fillMaxSize()
            .clip(cardShape)
            .background(NothingPrimaryBackground)
            .border(1.dp, NothingTokens.NothingBorderSubtle, cardShape)
            .verticalScroll(scroll)
            .padding(16.dp),
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("QUICK CONTROLS", color = NothingPrimaryText, fontSize = 13.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(NothingTokens.NothingSurfaceMedium)
                    .clickable { onCollapse() }
                    .padding(horizontal = 10.dp, vertical = 5.dp),
            ) { Text("DONE", color = NothingSecondaryText, fontSize = 10.sp, fontFamily = FontFamily.Monospace) }
        }

        Spacer(Modifier.height(14.dp))

        // Large toggle tiles
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            tools.take(5).forEach { tool ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (tool.active) Color(0xFFE8E8E8) else NothingTokens.NothingSurfaceMedium)
                            .border(1.dp, if (tool.active) Color.Transparent else NothingTokens.NothingBorderSubtle, RoundedCornerShape(16.dp))
                            .clickable { tool.action() },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(tool.glyph, color = if (tool.active) Color.Black else NothingPrimaryText, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(tool.label, color = NothingSecondaryText, fontSize = 8.sp, fontFamily = FontFamily.Monospace, maxLines = 1)
                }
            }
        }

        Spacer(Modifier.height(18.dp))
        SectionRow("APP SHORTCUTS")
        Spacer(Modifier.height(8.dp))
        PlaceholderTiles()

        Spacer(Modifier.height(14.dp))
        SectionRow("FAVORITE CONTACTS")
        Spacer(Modifier.height(8.dp))
        PlaceholderTiles()

        Spacer(Modifier.height(18.dp))

        // Bottom control row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .weight(1.1f)
                    .height(40.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(NothingTokens.NothingSurfaceMedium),
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("−", color = NothingPrimaryText, fontSize = 18.sp, modifier = Modifier
                        .clickable { onVolumeChange(-1) }
                        .padding(horizontal = 10.dp))
                    Text("♪", color = NothingSecondaryText, fontSize = 13.sp)
                    Text("+", color = NothingPrimaryText, fontSize = 16.sp, modifier = Modifier
                        .clickable { onVolumeChange(1) }
                        .padding(horizontal = 10.dp))
                }
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(NothingTokens.NothingSurfaceMedium)
                    .clickable { onHistory() },
                contentAlignment = Alignment.Center,
            ) { Text("☎ HISTORY", color = NothingPrimaryText, fontSize = 10.sp, fontFamily = FontFamily.Monospace) }
            Box(
                modifier = Modifier
                    .weight(0.6f)
                    .height(40.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(NothingPrimaryAccent.copy(alpha = 0.25f))
                    .clickable { onAi() },
                contentAlignment = Alignment.Center,
            ) { Text("Ai", color = NothingPrimaryText, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun SectionRow(title: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, color = NothingPrimaryText, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
        Text("EDIT", color = NothingSecondaryText, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun PlaceholderTiles() {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
        repeat(5) {
            Box(
                Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Transparent)
                    .border(1.dp, NothingTokens.NothingBorderSubtle, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) { Text("✎", color = NothingSecondaryText, fontSize = 13.sp) }
        }
    }
}
