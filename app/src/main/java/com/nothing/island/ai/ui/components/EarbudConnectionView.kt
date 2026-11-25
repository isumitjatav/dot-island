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
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nothing.island.ai.bluetooth.EarbudBatteryParser
import com.nothing.island.ai.engine.IslandEvent
import com.nothing.island.ai.ui.theme.NothingDivider
import com.nothing.island.ai.ui.theme.NothingMutedText
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

internal fun formatEarbudBatteryDetailText(event: IslandEvent.BluetoothDeviceConnected): String? {
    val parts = buildList {
        event.leftBatteryPercentage?.takeIf { it in 0..100 }?.let { add("L $it%") }
        event.rightBatteryPercentage?.takeIf { it in 0..100 }?.let { add("R $it%") }
        event.caseBatteryPercentage?.takeIf { it in 0..100 }?.let { add("C $it%") }
    }
    return if (parts.isNotEmpty()) parts.joinToString(" · ") else null
}

/**
 * Two-stage springy compact earbud pill:
 *  1. Springs downward smoothly into view.
 *  2. Widens to reveal device name, optional L/R/Case telemetry, and battery ring.
 *
 * Unknown battery values are rendered honestly as "--" with a muted track instead of faking 100%.
 */
@Composable
fun EarbudConnectionView(
    event: IslandEvent.BluetoothDeviceConnected,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snapshot = EarbudBatteryParser.sanitizeSnapshot(
        primary = event.batteryLevelPercentage,
        left = event.leftBatteryPercentage,
        right = event.rightBatteryPercentage,
        caseLevel = event.caseBatteryPercentage
    )
    val representativeBattery = snapshot.representativePercent()
    val hasBattery = representativeBattery != null
    val sweepFraction = if (representativeBattery != null) (representativeBattery / 100f).coerceIn(0f, 1f) else 0f
    val detailLine = formatEarbudBatteryDetailText(event)
    val safeName = event.deviceName.ifBlank { "Bluetooth Audio" }

    var dropped by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        dropped = true
    }

    val dropY by animateDpAsState(
        targetValue = if (dropped) 72.dp else 0.dp,
        animationSpec = spring(
            dampingRatio = 0.38f,
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

    val a11yDescription = buildString {
        append("$safeName connected")
        if (representativeBattery != null) {
            append(", battery $representativeBattery percent")
        } else {
            append(", battery level unavailable")
        }
        if (detailLine != null) {
            append(", $detailLine")
        }
    }

    Box(
        modifier = modifier
            .offset { IntOffset(0, dropY.roundToPx()) }
            .width(pillWidth)
            .height(pillHeight)
            .clip(RoundedCornerShape(20.dp))
            .background(NothingPrimaryBackground)
            .border(1.dp, NothingDivider, RoundedCornerShape(20.dp))
            .semantics { contentDescription = a11yDescription }
            .clickable(onClick = onTap)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Canvas(modifier = Modifier.size(34.dp)) {
                drawEarbuds(this, NothingPrimaryText)
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (detailLine != null) "CONNECTED · $detailLine" else "CONNECTED",
                    color = NothingSecondaryText,
                    fontSize = 10.5.sp,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    softWrap = false,
                )
                Text(
                    text = safeName,
                    color = NothingPrimaryText,
                    fontSize = 17.sp,
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
                    if (representativeBattery != null) {
                        drawArc(
                            color = (if (representativeBattery <= 15) Color(0xFFE6AA43) else NothingSuccess).copy(alpha = ringAlpha),
                            startAngle = -90f,
                            sweepAngle = 360f * sweepFraction,
                            useCenter = false,
                            topLeft = Offset(inset, inset),
                            size = arcSize,
                            style = Stroke(width = stroke, cap = StrokeCap.Round),
                        )
                    }
                }
                Text(
                    text = if (representativeBattery != null) "$representativeBattery" else "--",
                    color = if (representativeBattery != null) NothingPrimaryText else NothingMutedText,
                    fontSize = 12.sp,
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
    val safeName = event.deviceName.ifBlank { "Bluetooth Audio" }
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    val dropY by animateDpAsState(
        targetValue = if (shown) 72.dp else 0.dp,
        animationSpec = spring(dampingRatio = 0.38f, stiffness = Spring.StiffnessMediumLow),
        label = "earbudOffDropY",
    )
    Box(
        modifier = modifier
            .offset { IntOffset(0, dropY.roundToPx()) }
            .width(200.dp)
            .height(40.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(NothingPrimaryBackground)
            .border(1.dp, NothingDivider, RoundedCornerShape(20.dp))
            .semantics { contentDescription = "$safeName disconnected" }
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
                text = "$safeName disconnected",
                color = NothingSecondaryText,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
