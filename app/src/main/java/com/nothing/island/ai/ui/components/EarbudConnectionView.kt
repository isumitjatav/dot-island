package com.nothing.island.ai.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nothing.island.ai.engine.IslandEvent
import com.nothing.island.ai.ui.theme.NothingDivider
import com.nothing.island.ai.ui.theme.NothingPrimaryBackground
import com.nothing.island.ai.ui.theme.NothingPrimaryText
import com.nothing.island.ai.ui.theme.NothingSecondaryText
import com.nothing.island.ai.ui.theme.NothingSuccess

/** Earbud icon geometry: two buds with stems. */
private fun drawEarbuds(
    draw: androidx.compose.ui.graphics.drawscope.DrawScope,
    color: Color,
) {
    val w = draw.size.width
    val h = draw.size.height
    val budR = w * 0.16f
    val cy = h * 0.38f
    draw.drawCircle(color = color, radius = budR, center = Offset(w * 0.30f, cy))
    draw.drawCircle(color = color, radius = budR, center = Offset(w * 0.70f, cy))
    val stemW = w * 0.10f
    val stemTop = cy + budR * 0.4f
    val stemBottom = h * 0.92f
    draw.drawLine(color = color, start = Offset(w * 0.30f, stemTop), end = Offset(w * 0.30f, stemBottom), strokeWidth = stemW, cap = StrokeCap.Round)
    draw.drawLine(color = color, start = Offset(w * 0.70f, stemTop), end = Offset(w * 0.70f, stemBottom), strokeWidth = stemW, cap = StrokeCap.Round)
}

/**
 * Two-stage springy compact earbud pill (all in compact mode):
 *  1. On appearance the pill springs DOWNWARD from above.
 *  2. It then springs HORIZONTALLY wider to reveal device name + battery ring.
 * When the host removes the view, Compose animates it back up via [onDismiss].
 *
 * No giant expanded card — just a wider capsule, exactly as requested.
 */
@Composable
fun EarbudConnectionView(
    event: IslandEvent.BluetoothDeviceConnected,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val battery = event.batteryLevelPercentage
    val clamped = (battery ?: 100).coerceIn(0, 100)

    // The pill starts at the camera cutout, then DROPS DOWN into the screen
    // with a springy bounce while widening — matching the user's sketch.
    var dropped by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        dropped = true
    }

    val dropY by animateDpAsState(
        targetValue = if (dropped) 72.dp else 0.dp,
        animationSpec = spring(
            dampingRatio = 0.38f,      // low damping => visible bounce/overshoot
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "earbudDropY",
    )
    val pillWidth by animateDpAsState(
        targetValue = if (dropped) 320.dp else 124.dp,
        animationSpec = spring(
            dampingRatio = 0.45f,
            stiffness = Spring.StiffnessLow,
        ),
        label = "earbudWidth",
    )
    val pillHeight = 52.dp

    // Gentle breathing on the ring
    val pulse = rememberInfiniteTransition(label = "earbudPulse")
    val ringAlpha by pulse.animateFloat(
        initialValue = 0.75f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "earbudRingAlpha",
    )

    Box(
        modifier = modifier
            .offset(y = dropY)
            .width(pillWidth)
            .height(pillHeight)
            .clip(RoundedCornerShape(20.dp))
            .background(NothingPrimaryBackground)
            .border(1.dp, NothingDivider, RoundedCornerShape(20.dp))
            .clickable(onClick = onTap)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Canvas(modifier = Modifier.size(34.dp)) {
                drawEarbuds(this, NothingPrimaryText)
            }
            Spacer(Modifier.width(14.dp))
            androidx.compose.foundation.layout.Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "CONNECTED",
                    color = NothingSecondaryText,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.sp,
                    softWrap = false,
                )
                Text(
                    text = event.deviceName,
                    color = NothingPrimaryText,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(10.dp))
            Box(modifier = Modifier.size(38.dp), contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val stroke = 3.dp.toPx()
                    val inset = stroke / 2
                    val arcSize = Size(size.width - stroke, size.height - stroke)
                    drawArc(
                        color = NothingDivider,
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = Offset(inset, inset),
                        size = arcSize,
                        style = Stroke(width = stroke),
                    )
                    drawArc(
                        color = (if (clamped <= 15) Color(0xFFE6AA43) else NothingSuccess).copy(alpha = ringAlpha),
                        startAngle = -90f,
                        sweepAngle = 360f * (clamped / 100f),
                        useCenter = false,
                        topLeft = Offset(inset, inset),
                        size = arcSize,
                        style = Stroke(width = stroke, cap = StrokeCap.Round),
                    )
                }
                Text(
                    text = if (battery != null) "$clamped" else "·",
                    color = NothingPrimaryText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                )
            }
        }
    }
}

/** Disconnected variant: springs up and shows a dimmed compact pill. */
@Composable
fun EarbudDisconnectedView(
    event: IslandEvent.BluetoothDeviceDisconnected,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    val dropY by animateDpAsState(
        targetValue = if (shown) 72.dp else 0.dp,
        animationSpec = spring(dampingRatio = 0.38f, stiffness = Spring.StiffnessMediumLow),
        label = "earbudOffDropY",
    )
    Box(
        modifier = modifier
            .offset(y = dropY)
            .width(200.dp)
            .height(40.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(NothingPrimaryBackground)
            .border(1.dp, NothingDivider, RoundedCornerShape(20.dp))
            .clickable(onClick = onTap)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Canvas(modifier = Modifier.size(22.dp)) {
                drawEarbuds(this, NothingSecondaryText)
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = "${event.deviceName} disconnected",
                color = NothingSecondaryText,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
