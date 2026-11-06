package com.nothing.island.ai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nothing.island.ai.ui.theme.NothingMutedText
import com.nothing.island.ai.ui.theme.NothingPrimaryAccent
import com.nothing.island.ai.ui.theme.NothingPrimaryText

/**
 * Renders a Nothing-styled dot matrix glyph icon.
 */
@Composable
fun DotMatrixGlyph(
    pattern: List<List<Boolean>>,
    modifier: Modifier = Modifier,
    dotSize: Dp = 3.dp,
    spacing: Dp = 2.dp,
    activeColor: Color = NothingPrimaryText,
    inactiveColor: Color = Color.Transparent
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(spacing),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        pattern.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
                row.forEach { active ->
                    Box(
                        modifier = Modifier
                            .size(dotSize)
                            .clip(CircleShape)
                            .background(if (active) activeColor else inactiveColor)
                    )
                }
            }
        }
    }
}

/**
 * Pre-defined Nothing OS inspired 5x5 dot patterns.
 */
object DotMatrixPatterns {
    val IslandIcon = listOf(
        listOf(false, true, true, true, false),
        listOf(true, false, false, false, true),
        listOf(true, false, true, false, true),
        listOf(true, false, false, false, true),
        listOf(false, true, true, true, false)
    )

    val SparkleAI = listOf(
        listOf(false, false, true, false, false),
        listOf(false, true, true, true, false),
        listOf(true, true, true, true, true),
        listOf(false, true, true, true, false),
        listOf(false, false, true, false, false)
    )

    val Gear = listOf(
        listOf(false, true, false, true, false),
        listOf(true, true, true, true, true),
        listOf(false, true, false, true, false),
        listOf(true, true, true, true, true),
        listOf(false, true, false, true, false)
    )
}
