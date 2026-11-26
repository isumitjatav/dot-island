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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material3.Icon
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

internal fun normalizeEarbudDisplayName(deviceName: String?): String =
    deviceName?.trim()?.takeIf { it.isNotEmpty() } ?: "Bluetooth Audio"

internal fun formatEarbudStatusBadge(event: IslandEvent.BluetoothDeviceConnected): String {
    val snapshot = EarbudBatteryParser.sanitizeSnapshot(
        primary = event.batteryLevelPercentage,
        left = event.leftBatteryPercentage,
        right = event.rightBatteryPercentage,
        caseLevel = event.caseBatteryPercentage,
    )
    val pct = snapshot.representativePercent()
    return if (pct != null) "$pct%" else "CONNECTED"
}

/**
 * Compact centered Bluetooth headphones / earbuds pill (`height = 38.dp`, `widthIn(min = 188.dp, max = 236.dp)`).
 * Renders inside the visible overlay capsule without dropping off-screen, showing `#D71921`/white headphones
 * iconography, normalized device name, and battery percentage (or `CONNECTED` badge when unavailable).
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
        caseLevel = event.caseBatteryPercentage,
    )
    val representativeBattery = snapshot.representativePercent()
    val badgeText = formatEarbudStatusBadge(event)
    val detailLine = formatEarbudBatteryDetailText(event)
    val safeName = normalizeEarbudDisplayName(event.deviceName)
    val nothingRed = Color(0xFFD71921)
    val pillShape = RoundedCornerShape(19.dp)

    val a11yDescription = buildString {
        append("$safeName connected")
        if (representativeBattery != null) {
            append(", battery $representativeBattery percent")
        } else {
            append(", connected badge shown")
        }
        if (detailLine != null) {
            append(", $detailLine")
        }
    }

    Box(
        modifier = modifier
            .widthIn(min = 188.dp, max = 236.dp)
            .height(38.dp)
            .clip(pillShape)
            .background(Color.Black)
            .border(1.dp, nothingRed, pillShape)
            .semantics { contentDescription = a11yDescription }
            .clickable(onClick = onTap)
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(nothingRed.copy(alpha = 0.20f))
                    .border(1.dp, nothingRed, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Headphones,
                    contentDescription = "Bluetooth Audio",
                    tint = Color.White,
                    modifier = Modifier.size(13.dp),
                )
            }

            Text(
                text = safeName,
                color = NothingPrimaryText,
                fontSize = 11.5.sp,
                lineHeight = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                softWrap = false,
                modifier = Modifier.weight(1f),
            )

            val badgeColor = when {
                representativeBattery != null && representativeBattery <= 15 -> Color(0xFFE6AA43)
                representativeBattery != null -> NothingSuccess
                else -> nothingRed
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(badgeColor.copy(alpha = 0.22f))
                    .border(1.dp, badgeColor.copy(alpha = 0.75f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = badgeText,
                    color = Color.White,
                    fontSize = 9.5.sp,
                    lineHeight = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    softWrap = false,
                )
            }
        }
    }
}

/** Disconnected variant: compact centered pill without dropping below the capsule window. */
@Composable
fun EarbudDisconnectedView(
    event: IslandEvent.BluetoothDeviceDisconnected,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val safeName = normalizeEarbudDisplayName(event.deviceName)
    val pillShape = RoundedCornerShape(19.dp)
    Box(
        modifier = modifier
            .widthIn(min = 188.dp, max = 236.dp)
            .height(38.dp)
            .clip(pillShape)
            .background(Color.Black)
            .border(1.dp, NothingDivider, pillShape)
            .semantics { contentDescription = "$safeName disconnected" }
            .clickable(onClick = onTap)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Canvas(modifier = Modifier.requiredSize(18.dp)) {
                drawEarbuds(this, NothingSecondaryText)
            }
            Text(
                text = "$safeName off",
                color = NothingSecondaryText,
                fontSize = 11.sp,
                lineHeight = 14.sp,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }
    }
}
