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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
            Text("Quick Controls", color = NothingSecondaryText, fontSize = 14.sp)
            Box(
                Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(NothingTokens.NothingSurfaceMedium)
                    .padding(horizontal = 12.dp, vertical = 5.dp)
            ) { Text("Edit", color = NothingPrimaryText, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
        }

        Spacer(Modifier.height(14.dp))

        // Toggle row — reflects and controls the real overlay tools
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            QuickTools.DEFAULT.forEach { id ->
                val active = id in enabled
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (active) Color(0xFFE8E8E8) else NothingTokens.NothingSurfaceMedium)
                            .clickable { toggle(id) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            QuickTools.glyph(id),
                            color = if (active) Color.Black else NothingSecondaryText,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Text(
                        QuickTools.label(id),
                        color = NothingSecondaryText,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                    )
                }
            }
        }

        Spacer(Modifier.height(18.dp))

        // App Shortcuts placeholder row (reference styling)
        SectionLabel("App Shortcuts")
        Spacer(Modifier.height(8.dp))
        PlaceholderRow()

        Spacer(Modifier.height(16.dp))

        // Favorite Contacts placeholder row
        SectionLabel("Favorite Contacts")
        Spacer(Modifier.height(8.dp))
        PlaceholderRow()

        Spacer(Modifier.height(18.dp))

        // Bottom control row: volume + history + AI
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .weight(1.2f)
                    .height(40.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(NothingTokens.NothingSurfaceMedium),
                contentAlignment = Alignment.Center
            ) { Text("◀  🔊  ▶", color = NothingPrimaryText, fontSize = 13.sp) }
            Box(
                Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(NothingTokens.NothingSurfaceMedium),
                contentAlignment = Alignment.Center
            ) { Text("☎ History", color = NothingPrimaryText, fontSize = 11.sp) }
            Box(
                Modifier
                    .weight(0.7f)
                    .height(40.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(NothingPrimaryAccent.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center
            ) { Text("Ai", color = NothingPrimaryText, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, color = NothingPrimaryText, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        Box(
            Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(NothingTokens.NothingSurfaceMedium)
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) { Text("Edit", color = NothingPrimaryText, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun PlaceholderRow() {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        repeat(5) {
            Box(
                Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Transparent)
                    .border(1.dp, NothingTokens.NothingBorderSubtle, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) { Text("✎", color = NothingSecondaryText, fontSize = 14.sp) }
        }
    }
}
