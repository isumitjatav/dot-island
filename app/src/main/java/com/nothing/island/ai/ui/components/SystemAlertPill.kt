package com.nothing.island.ai.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nothing.island.ai.NothingBlack
import com.nothing.island.ai.NothingBorder
import com.nothing.island.ai.NothingRed
import com.nothing.island.ai.NothingWhite
import com.nothing.island.ai.engine.IslandEvent
import com.nothing.island.ai.gestures.RingerState

private val AmberAlert = Color(0xFFFFC107)
private val SoftPurple = Color(0xFFB388FF)
private val CyanWifi = Color(0xFF80D8FF)

/**
 * SystemAlertPill renders momentary hardware pills (REQ-36, REQ-37, REQ-60, REQ-61, REQ-62, REQ-64, REQ-75)
 * matching Nothing OS dot-matrix minimalist aesthetics.
 */
@Composable
fun SystemAlertPill(
    event: IslandEvent,
    modifier: Modifier = Modifier
) {
    val (iconColor, titleText, statusText) = when (event) {
        is IslandEvent.RingerModeChanged -> {
            when (event.ringerMode) {
                RingerState.SILENT -> Triple(NothingRed, "Silent", "")
                RingerState.VIBRATE -> Triple(NothingWhite, "Vibrate", "")
                RingerState.NORMAL -> Triple(NothingWhite, "Sound On", "")
            }
        }
        is IslandEvent.TorchStateChanged -> {
            val text = if (event.isEnabled) "Torch On" else "Torch Off"
            val color = if (event.isEnabled) AmberAlert else NothingWhite.copy(alpha = 0.6f)
            Triple(color, text, "")
        }
        is IslandEvent.WifiStateChanged -> {
            val title = if (event.isConnected) (event.ssid ?: "Wi-Fi") else "Wi-Fi"
            val subtitle = if (event.isConnected) "Connected" else "Disconnected"
            val color = if (event.isConnected) CyanWifi else NothingWhite.copy(alpha = 0.5f)
            Triple(color, title, subtitle)
        }
        is IslandEvent.DndStateChanged -> {
            val text = if (event.isEnabled) "DND On" else "DND Off"
            val color = if (event.isEnabled) SoftPurple else NothingWhite.copy(alpha = 0.6f)
            Triple(color, text, "")
        }
        is IslandEvent.BatteryThresholdWarning -> {
            val color = if (event.isCritical) NothingRed else AmberAlert
            val title = if (event.isCritical) "Critical Battery" else "Low Battery"
            Triple(color, title, "${event.batteryLevel}%")
        }
        is IslandEvent.DeviceUnlocked -> {
            Triple(NothingWhite, "Unlocked", "")
        }
        else -> Triple(NothingWhite, "System Alert", "")
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(18.dp))
            .background(NothingBlack)
            .border(1.dp, NothingBorder, RoundedCornerShape(18.dp))
            .padding(horizontal = 14.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            // Icon
            Canvas(modifier = Modifier.size(18.dp)) {
                when (event) {
                    is IslandEvent.RingerModeChanged -> {
                        drawBellIcon(this, iconColor, event.ringerMode == RingerState.SILENT)
                    }
                    is IslandEvent.TorchStateChanged -> {
                        drawTorchIcon(this, iconColor, event.isEnabled)
                    }
                    is IslandEvent.WifiStateChanged -> {
                        drawWifiIcon(this, iconColor, event.isConnected)
                    }
                    is IslandEvent.DndStateChanged -> {
                        drawMoonIcon(this, iconColor, event.isEnabled)
                    }
                    is IslandEvent.BatteryThresholdWarning -> {
                        drawBatteryIcon(this, iconColor, event.batteryLevel)
                    }
                    is IslandEvent.DeviceUnlocked -> {
                        drawLockIcon(this, iconColor, isUnlocked = true)
                    }
                    else -> {
                        drawCircle(color = iconColor, radius = size.minDimension / 4)
                    }
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Label
            Text(
                text = titleText.uppercase(),
                color = iconColor,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (statusText.isNotEmpty()) {
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = statusText.uppercase(),
                    color = NothingWhite.copy(alpha = 0.7f),
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1
                )
            }
        }
    }
}

// ==========================================
// VECTOR ICON CANVAS HELPERS
// ==========================================

private fun drawBellIcon(scope: androidx.compose.ui.graphics.drawscope.DrawScope, color: Color, isSlashed: Boolean) {
    with(scope) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.5f, h * 0.15f)
            cubicTo(w * 0.3f, h * 0.15f, w * 0.25f, h * 0.45f, w * 0.2f, h * 0.7f)
            lineTo(w * 0.8f, h * 0.7f)
            cubicTo(w * 0.75f, h * 0.45f, w * 0.7f, h * 0.15f, w * 0.5f, h * 0.15f)
            close()
        }
        drawPath(path, color = color, style = Stroke(width = 2f))
        drawCircle(color = color, radius = 2f, center = Offset(w * 0.5f, h * 0.85f))

        if (isSlashed) {
            drawLine(
                color = color,
                start = Offset(w * 0.1f, h * 0.1f),
                end = Offset(w * 0.9f, h * 0.9f),
                strokeWidth = 2.5f,
                cap = StrokeCap.Round
            )
        }
    }
}

private fun drawTorchIcon(scope: androidx.compose.ui.graphics.drawscope.DrawScope, color: Color, isLit: Boolean) {
    with(scope) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.35f, h * 0.35f)
            lineTo(w * 0.25f, h * 0.15f)
            lineTo(w * 0.75f, h * 0.15f)
            lineTo(w * 0.65f, h * 0.35f)
            lineTo(w * 0.65f, h * 0.85f)
            lineTo(w * 0.35f, h * 0.85f)
            close()
        }
        drawPath(path, color = color, style = Stroke(width = 2f))
        if (isLit) {
            drawLine(color = color, start = Offset(w * 0.5f, 0f), end = Offset(w * 0.5f, h * 0.1f), strokeWidth = 2f)
            drawLine(color = color, start = Offset(w * 0.2f, 0f), end = Offset(w * 0.3f, h * 0.1f), strokeWidth = 2f)
            drawLine(color = color, start = Offset(w * 0.8f, 0f), end = Offset(w * 0.7f, h * 0.1f), strokeWidth = 2f)
        }
    }
}

private fun drawWifiIcon(scope: androidx.compose.ui.graphics.drawscope.DrawScope, color: Color, isConnected: Boolean) {
    with(scope) {
        val w = size.width
        val h = size.height
        drawCircle(color = color, radius = 2f, center = Offset(w * 0.5f, h * 0.8f))
        drawArc(
            color = color,
            startAngle = 210f,
            sweepAngle = 120f,
            useCenter = false,
            topLeft = Offset(w * 0.25f, h * 0.45f),
            size = Size(w * 0.5f, h * 0.5f),
            style = Stroke(width = 2f, cap = StrokeCap.Round)
        )
        drawArc(
            color = color,
            startAngle = 210f,
            sweepAngle = 120f,
            useCenter = false,
            topLeft = Offset(w * 0.1f, h * 0.2f),
            size = Size(w * 0.8f, h * 0.8f),
            style = Stroke(width = 2f, cap = StrokeCap.Round)
        )
        if (!isConnected) {
            drawLine(
                color = color,
                start = Offset(w * 0.15f, h * 0.15f),
                end = Offset(w * 0.85f, h * 0.85f),
                strokeWidth = 2f,
                cap = StrokeCap.Round
            )
        }
    }
}

private fun drawMoonIcon(scope: androidx.compose.ui.graphics.drawscope.DrawScope, color: Color, isEnabled: Boolean) {
    with(scope) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.6f, h * 0.15f)
            cubicTo(w * 0.3f, h * 0.2f, w * 0.2f, h * 0.6f, w * 0.4f, h * 0.85f)
            cubicTo(w * 0.2f, h * 0.7f, w * 0.25f, h * 0.35f, w * 0.6f, h * 0.15f)
            close()
        }
        drawPath(path, color = color)
    }
}

private fun drawBatteryIcon(scope: androidx.compose.ui.graphics.drawscope.DrawScope, color: Color, level: Int) {
    with(scope) {
        val w = size.width
        val h = size.height
        // Battery body
        drawRect(
            color = color,
            topLeft = Offset(w * 0.15f, h * 0.25f),
            size = Size(w * 0.65f, h * 0.5f),
            style = Stroke(width = 2f)
        )
        // Battery terminal
        drawRect(
            color = color,
            topLeft = Offset(w * 0.8f, h * 0.38f),
            size = Size(w * 0.08f, h * 0.24f)
        )
        // Fill level
        val fillWidth = (w * 0.55f) * (level.coerceIn(0, 100) / 100f)
        drawRect(
            color = color,
            topLeft = Offset(w * 0.2f, h * 0.32f),
            size = Size(fillWidth, h * 0.36f)
        )
    }
}

private fun drawLockIcon(scope: androidx.compose.ui.graphics.drawscope.DrawScope, color: Color, isUnlocked: Boolean) {
    with(scope) {
        val w = size.width
        val h = size.height
        // Lock body
        drawRect(
            color = color,
            topLeft = Offset(w * 0.25f, h * 0.45f),
            size = Size(w * 0.5f, h * 0.42f),
            style = Stroke(width = 2f)
        )
        // Shackle
        val shackleOffset = if (isUnlocked) Offset(w * 0.35f, h * 0.12f) else Offset(w * 0.35f, h * 0.2f)
        drawArc(
            color = color,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = shackleOffset,
            size = Size(w * 0.3f, h * 0.35f),
            style = Stroke(width = 2f, cap = StrokeCap.Round)
        )
    }
}
