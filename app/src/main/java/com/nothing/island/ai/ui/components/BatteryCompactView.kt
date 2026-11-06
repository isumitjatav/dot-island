package com.nothing.island.ai.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nothing.island.ai.NothingBlack
import com.nothing.island.ai.NothingBorder
import com.nothing.island.ai.NothingDarkGray
import com.nothing.island.ai.NothingMutedText
import com.nothing.island.ai.NothingWhite
import com.nothing.island.ai.battery.BatteryPreferences
import com.nothing.island.ai.battery.BatteryState
import com.nothing.island.ai.battery.ChargingSource
import com.nothing.island.ai.battery.ChargingStatus
import com.nothing.island.ai.switcher.SwipeDirection

/**
 * BatteryCompactView
 *
 * Symmetrical 200dp capsule wrapping the centered front-facing camera on Nothing Phone (4a):
 * [ Left: 73dp Battery Icon + Animated Bolt ] [ Center: 54dp Hole Clearance ] [ Right: 73dp Percentage + Source Tag ] = 200dp.
 */
@Composable
fun BatteryCompactView(
    batteryState: BatteryState,
    preferences: BatteryPreferences = BatteryPreferences(),
    onTap: () -> Unit,
    onSwipe: (SwipeDirection) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val pillShape = RoundedCornerShape(18.dp)
    val accentColor = Color(preferences.accentColorHex)

    var totalDrag by remember { mutableFloatStateOf(0f) }
    val swipeThresholdPx = 35f

    val isCharging = batteryState.isCharging
    val isLowBattery = batteryState.isLowBattery
    val isOverheat = batteryState.isOverheating

    val transition = rememberInfiniteTransition(label = "CompactBoltTransition")
    val boltAlpha by transition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "boltAlpha"
    )

    val borderColor = when {
        isOverheat -> Color(0xFFFF5252)
        isLowBattery -> Color(0xFFFFB300)
        isCharging && preferences.glowIntensity > 0f -> accentColor.copy(alpha = (0.5f * preferences.glowIntensity).coerceIn(0.2f, 0.8f))
        else -> NothingBorder
    }

    Box(
        modifier = modifier
            .width(200.dp)
            .height(36.dp)
            .clip(pillShape)
            .background(NothingBlack)
            .border(1.dp, borderColor, pillShape)
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { totalDrag = 0f },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        totalDrag += dragAmount
                    },
                    onDragEnd = {
                        if (totalDrag > swipeThresholdPx) {
                            onSwipe(SwipeDirection.RIGHT)
                        } else if (totalDrag < -swipeThresholdPx) {
                            onSwipe(SwipeDirection.LEFT)
                        }
                        totalDrag = 0f
                    },
                    onDragCancel = { totalDrag = 0f }
                )
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                onTap()
            },
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Left Wing: 73dp (Battery Silhouette Icon + Lightning Bolt)
            Box(
                modifier = Modifier
                    .width(73.dp)
                    .fillMaxHeight()
                    .padding(start = 10.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Custom Battery Gauge Icon
                    Canvas(modifier = Modifier.size(width = 22.dp, height = 12.dp)) {
                        val strokeW = 1.5.dp.toPx()
                        val corner = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                        val bodyW = size.width - 3.dp.toPx()
                        val bodyH = size.height

                        // Battery Body Outline
                        drawRoundRect(
                            color = if (isLowBattery) Color(0xFFFFB300) else Color.White.copy(alpha = 0.85f),
                            topLeft = Offset(0f, 0f),
                            size = Size(bodyW, bodyH),
                            cornerRadius = corner,
                            style = Stroke(width = strokeW)
                        )

                        // Battery Terminal Nipple
                        drawRoundRect(
                            color = Color.White.copy(alpha = 0.6f),
                            topLeft = Offset(bodyW + 1.dp.toPx(), bodyH * 0.25f),
                            size = Size(2.dp.toPx(), bodyH * 0.5f),
                            cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx())
                        )

                        // Inner Charge Level Fill
                        val fillRatio = (batteryState.percentage.coerceIn(0, 100) / 100f)
                        val innerPad = strokeW + 1.dp.toPx()
                        val innerW = (bodyW - (innerPad * 2)) * fillRatio
                        val innerH = bodyH - (innerPad * 2)

                        if (innerW > 0f) {
                            val fillColor = when {
                                isOverheat -> Color(0xFFFF5252)
                                isLowBattery -> Color(0xFFFFB300)
                                isCharging -> accentColor
                                else -> Color.White
                            }
                            drawRoundRect(
                                color = fillColor,
                                topLeft = Offset(innerPad, innerPad),
                                size = Size(innerW, innerH),
                                cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx())
                            )
                        }
                    }

                    // Mini Charging Bolt Glyph
                    if (isCharging) {
                        Canvas(modifier = Modifier.size(width = 9.dp, height = 14.dp)) {
                            val boltPath = Path().apply {
                                moveTo(size.width * 0.65f, 0f)
                                lineTo(0f, size.height * 0.55f)
                                lineTo(size.width * 0.45f, size.height * 0.55f)
                                lineTo(size.width * 0.35f, size.height)
                                lineTo(size.width, size.height * 0.45f)
                                lineTo(size.width * 0.55f, size.height * 0.45f)
                                close()
                            }
                            drawPath(
                                path = boltPath,
                                color = accentColor.copy(alpha = if (preferences.reducedMotion) 1f else boltAlpha)
                            )
                        }
                    }
                }
            }

            // 2. Center Zone: 54dp empty clearance (camera punch-hole)
            Spacer(modifier = Modifier.width(54.dp))

            // 3. Right Wing: 73dp (Percentage + Source Tag)
            Box(
                modifier = Modifier
                    .width(73.dp)
                    .fillMaxHeight()
                    .padding(end = 10.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val wattage = batteryState.wattageW
                    val isFast = wattage != null && wattage >= 18f
                    val tagText = when {
                        isOverheat -> "HOT"
                        isLowBattery -> "LOW"
                        batteryState.isFullyCharged -> "FULL"
                        isFast -> "FAST"
                        batteryState.source == ChargingSource.WIRELESS -> "WLS"
                        batteryState.source == ChargingSource.AC -> "AC"
                        batteryState.source == ChargingSource.USB -> "USB"
                        else -> "BAT"
                    }

                    // Source Tag Pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                when {
                                    isOverheat -> Color(0x33FF5252)
                                    isLowBattery -> Color(0x33FFB300)
                                    isCharging -> accentColor.copy(alpha = 0.2f)
                                    else -> Color.White.copy(alpha = 0.1f)
                                }
                            )
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = tagText,
                            color = when {
                                isOverheat -> Color(0xFFFF5252)
                                isLowBattery -> Color(0xFFFFB300)
                                isCharging -> accentColor
                                else -> NothingMutedText
                            },
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Monospace Percentage
                    Text(
                        text = "${batteryState.percentage}%",
                        color = when {
                            isOverheat -> Color(0xFFFF5252)
                            isLowBattery -> Color(0xFFFFB300)
                            else -> NothingWhite
                        },
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
