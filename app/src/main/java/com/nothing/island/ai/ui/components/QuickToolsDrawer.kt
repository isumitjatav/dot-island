package com.nothing.island.ai.ui.components

import android.media.AudioManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nothing.island.ai.ui.theme.NothingPrimaryAccent
import com.nothing.island.ai.ui.theme.NothingPrimaryBackground
import com.nothing.island.ai.ui.theme.NothingPrimaryText
import com.nothing.island.ai.ui.theme.NothingSecondaryText
import com.nothing.island.ai.ui.theme.NothingSuccess
import com.nothing.island.ai.ui.theme.NothingTokens
import kotlin.math.roundToInt

/** Identifiers for the built-in quick tools. */
object QuickTools {
    const val DATA = "data"
    const val WIFI = "wifi"
    const val BLUETOOTH = "bluetooth"
    const val TORCH = "torch"
    const val LOCATION = "location"
    const val HOTSPOT = "hotspot"
    const val SOUND = "sound"
    const val SETTINGS = "settings"

    val DEFAULT = listOf(DATA, WIFI, BLUETOOTH, TORCH, LOCATION)
    val ALL = listOf(DATA, WIFI, BLUETOOTH, TORCH, LOCATION, HOTSPOT, SOUND, SETTINGS)

    fun label(id: String) = when (id) {
        DATA -> "MOBILE DATA"
        WIFI -> "WI-FI"
        BLUETOOTH -> "BLUETOOTH"
        TORCH -> "TORCH"
        LOCATION -> "LOCATION"
        HOTSPOT -> "HOTSPOT"
        SOUND -> "SOUND"
        SETTINGS -> "SETTINGS"
        else -> id.uppercase()
    }

    fun glyph(id: String) = when (id) {
        DATA -> "↕"
        WIFI -> "◕"
        BLUETOOTH -> "ᛒ"
        TORCH -> "⚡"
        LOCATION -> "⌖"
        HOTSPOT -> "◎"
        SOUND -> "🔔"
        SETTINGS -> "⚙"
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
 * Authentic Nothing OS-style interactive slider with smooth tap and drag tracking.
 */
@Composable
fun NothingSliderControl(
    glyph: String,
    label: String,
    percent: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    activeColor: Color = Color.White
) {
    var isDragging by remember { mutableStateOf(false) }
    var currentPercent by remember(percent) { mutableFloatStateOf(percent) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(46.dp)
            .clip(RoundedCornerShape(23.dp))
            .background(NothingTokens.NothingSurfaceMedium)
            .border(1.dp, NothingTokens.NothingBorderSubtle, RoundedCornerShape(23.dp))
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val newP = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                    currentPercent = newP
                    onValueChange(newP)
                }
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        isDragging = true
                        val newP = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                        currentPercent = newP
                        onValueChange(newP)
                    },
                    onDragEnd = { isDragging = false },
                    onDragCancel = { isDragging = false },
                    onDrag = { change, _ ->
                        change.consume()
                        val newP = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                        currentPercent = newP
                        onValueChange(newP)
                    }
                )
            }
    ) {
        // Filled Progress Track
        val fillWidthFraction = if (isDragging) currentPercent else percent
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(fillWidthFraction.coerceIn(0.02f, 1f))
                .background(activeColor.copy(alpha = 0.28f))
        )

        // Content Row: Glyph on left, Label + Percent on right
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = glyph,
                    color = NothingPrimaryText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = label,
                    color = NothingSecondaryText,
                    fontSize = 10.5.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
            Text(
                text = "${((if (isDragging) currentPercent else percent) * 100).roundToInt()}%",
                color = NothingPrimaryText,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Premium Nothing OS Quick Tools drawer with direct customization and
 * real-time Volume and Brightness sliders.
 */
@Composable
fun QuickToolsDrawer(
    tools: List<QuickTool>,
    onCollapse: () -> Unit,
    volumePercent: Float = 0.5f,
    onVolumeChange: (Float) -> Unit = {},
    brightnessPercent: Float = 0.5f,
    onBrightnessChange: (Float) -> Unit = {},
    allAvailableToolIds: List<String> = QuickTools.ALL,
    enabledToolIds: Set<String> = QuickTools.DEFAULT.toSet(),
    onToggleToolEnabled: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    isCameraCutoutEnabled: Boolean = false,
) {
    val scroll = rememberScrollState()
    val cardShape = if (isCameraCutoutEnabled) notchedCardShape(cornerRadiusDp = 28f) else RoundedCornerShape(28.dp)
    var isEditing by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .clip(cardShape)
            .background(NothingPrimaryBackground)
            .border(1.dp, NothingTokens.NothingBorderSubtle, cardShape)
            .pointerInput(Unit) {
                var dragY = 0f
                detectDragGestures(
                    onDragStart = { dragY = 0f },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragY += dragAmount.y
                    },
                    onDragEnd = {
                        if (dragY < -50f) {
                            onCollapse()
                        }
                        dragY = 0f
                    },
                    onDragCancel = { dragY = 0f }
                )
            }
            .verticalScroll(scroll)
            .padding(16.dp),
    ) {
        // Top Punch-hole clearance spacer
        val topClearance = if (isCameraCutoutEnabled) 18.dp else 12.dp
        Spacer(Modifier.height(topClearance))

        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "QUICK TOOLS",
                color = NothingPrimaryText,
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isEditing) NothingPrimaryAccent else NothingTokens.NothingSurfaceMedium)
                        .clickable { isEditing = !isEditing }
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                ) {
                    Text(
                        text = if (isEditing) "DONE" else "EDIT",
                        color = if (isEditing) Color.White else NothingSecondaryText,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(NothingTokens.NothingSurfaceMedium)
                        .clickable { onCollapse() }
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                ) {
                    Text(
                        text = "CLOSE",
                        color = NothingSecondaryText,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        if (isEditing) {
            Text(
                text = "TAP A TILE TO ADD OR REMOVE:",
                color = NothingSecondaryText,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))

            // Grid of all available tools for easy toggling
            val rows = allAvailableToolIds.chunked(4)
            rows.forEach { rowItems ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowItems.forEach { toolId ->
                        val isEnabled = toolId in enabledToolIds
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isEnabled) NothingPrimaryAccent.copy(alpha = 0.25f) else NothingTokens.NothingSurfaceMedium)
                            .border(
                                1.dp,
                                if (isEnabled) NothingPrimaryAccent else NothingTokens.NothingBorderSubtle,
                                RoundedCornerShape(14.dp)
                            )
                            .clickable { onToggleToolEnabled(toolId) }
                            .padding(4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = QuickTools.glyph(toolId),
                                    fontSize = 18.sp,
                                    color = if (isEnabled) Color.White else NothingSecondaryText
                                )
                                Text(
                                    text = QuickTools.label(toolId),
                                    fontSize = 7.5.sp,
                                    color = if (isEnabled) Color.White else NothingSecondaryText,
                                    fontFamily = FontFamily.Monospace,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                    // Pad empty spots in row
                    repeat(4 - rowItems.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        } else {
            // Normal view: Active Quick Tools tiles
            val toolChunks = tools.chunked(5)
            toolChunks.forEach { chunk ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(9.dp),
                ) {
                    chunk.forEach { tool ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (tool.active) Color(0xFFE8E8E8) else NothingTokens.NothingSurfaceMedium)
                                    .border(
                                        1.dp,
                                        if (tool.active) Color.Transparent else NothingTokens.NothingBorderSubtle,
                                        RoundedCornerShape(16.dp)
                                    )
                                    .clickable { tool.action() },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = tool.glyph,
                                    color = if (tool.active) Color.Black else NothingPrimaryText,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = tool.label,
                                color = NothingSecondaryText,
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 1
                            )
                        }
                    }
                    repeat(5 - chunk.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        Spacer(Modifier.height(18.dp))

        // Hardware Controls: Volume & Brightness Sliders (replacing old history/ai buttons)
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Volume Slider
            NothingSliderControl(
                glyph = "♪",
                label = "VOLUME",
                percent = volumePercent,
                onValueChange = onVolumeChange,
                activeColor = NothingPrimaryAccent
            )

            // Brightness Slider
            NothingSliderControl(
                glyph = "☀",
                label = "BRIGHTNESS",
                percent = brightnessPercent,
                onValueChange = onBrightnessChange,
                activeColor = Color(0xFFFFD54F)
            )
        }
    }
}

/**
 * Backward-compatible overload for legacy callers.
 */
@Composable
fun QuickToolsDrawer(
    tools: List<QuickTool>,
    onCollapse: () -> Unit,
    onVolumeChange: (Int) -> Unit = {},
    onHistory: () -> Unit = {},
    onAi: () -> Unit = {},
    modifier: Modifier = Modifier,
    isCameraCutoutEnabled: Boolean = false,
) {
    QuickToolsDrawer(
        tools = tools,
        onCollapse = onCollapse,
        volumePercent = 0.5f,
        onVolumeChange = { percent ->
            val delta = if (percent > 0.5f) 1 else -1
            onVolumeChange(delta)
        },
        brightnessPercent = 0.5f,
        onBrightnessChange = {},
        modifier = modifier,
        isCameraCutoutEnabled = isCameraCutoutEnabled
    )
}
