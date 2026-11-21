package com.nothing.island.ai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nothing.island.ai.ui.theme.NothingPrimaryAccent
import com.nothing.island.ai.ui.theme.NothingPrimaryBackground
import com.nothing.island.ai.ui.theme.NothingPrimaryText
import com.nothing.island.ai.ui.theme.NothingSecondaryText
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
        DATA -> "DATA"
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
 * Expanded quick tools drawer with large circular icon buttons (64dp).
 * Default set: data, Wi-Fi, Bluetooth, torch, location.
 */
@Composable
fun QuickToolsDrawer(
    tools: List<QuickTool>,
    onCollapse: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(24.dp))
            .background(NothingPrimaryBackground)
            .border(1.dp, NothingTokens.NothingBorderSubtle, RoundedCornerShape(24.dp))
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "ASSISTIVE",
            color = NothingPrimaryAccent,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 2.sp,
        )
        Spacer(Modifier.height(20.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            tools.take(5).forEach { tool ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(58.dp)
                            .clip(CircleShape)
                            .background(if (tool.active) NothingPrimaryAccent.copy(alpha = 0.18f) else Color(0xFF161616))
                            .border(
                                1.dp,
                                if (tool.active) NothingPrimaryAccent else NothingTokens.NothingBorderSubtle,
                                CircleShape,
                            )
                            .clickable { tool.action() },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            tool.glyph,
                            color = if (tool.active) NothingPrimaryAccent else NothingPrimaryText,
                            fontSize = 24.sp,
                        )
                    }
                    Text(
                        text = tool.label,
                        color = NothingSecondaryText,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = "TAP OUTSIDE TO CLOSE",
            color = NothingSecondaryText,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
        )
    }
}
