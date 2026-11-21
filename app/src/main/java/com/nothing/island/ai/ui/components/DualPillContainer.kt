package com.nothing.island.ai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nothing.island.ai.ui.theme.NothingPrimaryBackground
import com.nothing.island.ai.ui.theme.NothingPrimaryText
import com.nothing.island.ai.ui.theme.NothingSecondaryText
import com.nothing.island.ai.ui.theme.NothingTokens

/**
 * Two symmetrical satellite pills on either side of the camera cutout.
 * Original implementation of the dual-pill split layout.
 */
@Composable
fun DualPillContainer(
    leftLabel: String,
    leftGlyph: String,
    rightLabel: String,
    rightGlyph: String,
    accentColor: Color,
    onLeftTap: () -> Unit,
    onRightTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Left satellite
        Box(
            modifier = Modifier
                .width(86.dp)
                .height(36.dp)
                .clip(shape)
                .background(NothingPrimaryBackground)
                .border(1.dp, NothingTokens.NothingBorderSubtle, shape)
                .clickable(onClick = onLeftTap)
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(leftGlyph, color = accentColor, fontSize = 12.sp)
                Text(
                    leftLabel,
                    color = NothingPrimaryText,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        // Center camera clearance
        Box(modifier = Modifier.size(36.dp))

        // Right satellite
        Box(
            modifier = Modifier
                .width(86.dp)
                .height(36.dp)
                .clip(shape)
                .background(NothingPrimaryBackground)
                .border(1.dp, NothingTokens.NothingBorderSubtle, shape)
                .clickable(onClick = onRightTap)
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(rightGlyph, color = NothingSecondaryText, fontSize = 12.sp)
                Text(
                    rightLabel,
                    color = NothingPrimaryText,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
