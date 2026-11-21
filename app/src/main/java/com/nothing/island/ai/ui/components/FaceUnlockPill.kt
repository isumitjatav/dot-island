package com.nothing.island.ai.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nothing.island.ai.ui.theme.NothingPrimaryBackground
import com.nothing.island.ai.ui.theme.NothingSecondaryText
import com.nothing.island.ai.ui.theme.NothingTokens

/**
 * Face unlock radar pill: a cyan squircle with an animated sweep arc around
 * the camera cutout. Original implementation.
 */
@Composable
fun FaceUnlockPill(
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var started by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { started = true }

    val sweep by animateFloatAsState(
        targetValue = if (started) 360f else 0f,
        animationSpec = tween(durationMillis = 1200),
        label = "faceUnlockSweep",
    )

    Box(
        modifier = modifier
            .width(200.dp)
            .height(36.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(NothingPrimaryBackground)
            .border(1.dp, NothingTokens.NothingBorderSubtle, RoundedCornerShape(18.dp))
            .clickable(onClick = onTap)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Canvas(modifier = Modifier.size(22.dp)) {
                val cyan = Color(0xFF00E5FF)
                val cx = size.width / 2f
                val cy = size.height / 2f
                val radius = size.minDimension * 0.38f
                // Squircle-ish rounded square outline
                drawRoundRect(
                    color = cyan.copy(alpha = 0.5f),
                    topLeft = Offset(cx - radius, cy - radius),
                    size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(5.dp.toPx(), 5.dp.toPx()),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx()),
                )
                // Radar sweep arc
                drawArc(
                    color = cyan,
                    startAngle = -90f,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = Offset(cx - radius, cy - radius),
                    size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx()),
                )
            }
            Text(
                text = "FACE UNLOCKED",
                color = NothingSecondaryText,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
            )
        }
    }
}
