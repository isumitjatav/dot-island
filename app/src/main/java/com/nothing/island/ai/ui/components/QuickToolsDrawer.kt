package com.nothing.island.ai.ui.components

import android.media.AudioManager
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nothing.island.ai.ui.theme.NothingPrimaryAccent
import com.nothing.island.ai.ui.theme.NothingPrimaryBackground
import com.nothing.island.ai.ui.theme.NothingPrimaryText
import com.nothing.island.ai.ui.theme.NothingSecondaryText
import com.nothing.island.ai.ui.theme.NothingSuccess
import com.nothing.island.ai.ui.theme.NothingTokens
import kotlinx.coroutines.launch
import kotlin.math.abs
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
    val actionType: QuickToolActionType = QuickToolExecutor.describeTool(id).actionType,
    val contentDescription: String = QuickToolExecutor.describeTool(id).contentDescription,
    val fallbackTag: String? = QuickToolExecutor.describeTool(id).fallbackTag,
    val action: () -> Unit,
)

private class DotSpark(
    var x: Float,
    var y: Float,
    var radius: Float,
    var alpha: Float,
    var vx: Float,
    var vy: Float
)

/**
 * Animated Quick Tool Tile with spring-loaded squash and shockwave ring micro-interaction.
 */
@Composable
fun QuickToolTile(
    tool: QuickTool,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val scale = remember { Animatable(1f) }
    val shockwave = remember { Animatable(0f) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier.semantics(mergeDescendants = true) {
            contentDescription = tool.contentDescription
        },
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .graphicsLayer {
                    scaleX = scale.value
                    scaleY = scale.value
                }
                .clip(RoundedCornerShape(16.dp))
                .background(if (tool.active) Color(0xFFE8E8E8) else NothingTokens.NothingSurfaceMedium)
                .border(
                    1.dp,
                    if (tool.active) Color.Transparent else NothingTokens.NothingBorderSubtle,
                    RoundedCornerShape(16.dp)
                )
                .clickable {
                    coroutineScope.launch {
                        scale.animateTo(0.88f, tween(40))
                        scale.animateTo(
                            1.0f,
                            spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        )
                    }
                    coroutineScope.launch {
                        shockwave.snapTo(0f)
                        shockwave.animateTo(1f, tween(320, easing = LinearOutSlowInEasing))
                    }
                    tool.action()
                },
            contentAlignment = Alignment.Center,
        ) {
            // Animated shockwave ring on tap (Sakuga / Animata micro-interaction)
            if (shockwave.value > 0f && shockwave.value < 1f) {
                val waveAlpha = (1f - shockwave.value) * 0.75f
                val waveSize = (24f + shockwave.value * 28f).dp
                Box(
                    modifier = Modifier
                        .size(waveSize)
                        .border(
                            width = (1.5f * (1f - shockwave.value)).dp,
                            color = (if (tool.active) Color.Black else NothingPrimaryAccent).copy(alpha = waveAlpha),
                            shape = CircleShape
                        )
                )
            }

            Text(
                text = tool.glyph,
                color = if (tool.active) Color.Black else NothingPrimaryText,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )

            if (tool.actionType == QuickToolActionType.SETTINGS_FALLBACK) {
                Text(
                    text = "↗",
                    color = if (tool.active) Color.Black.copy(alpha = 0.7f) else NothingSecondaryText,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 4.dp, end = 6.dp)
                )
            }
        }
        Text(
            text = if (tool.fallbackTag != null) "${tool.label} ↗" else tool.label,
            color = NothingSecondaryText,
            fontSize = 8.sp,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Authentic Nothing OS-style interactive slider with Sakuga VFX velocity-driven
 * squash & stretch physics, trailing dot-matrix sparks, and damped spring recoil.
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
    val scope = rememberCoroutineScope()
    var isDragging by remember { mutableStateOf(false) }
    val animPercent = remember { Animatable(percent.coerceIn(0f, 1f)) }
    var dragVelocity by remember { mutableFloatStateOf(0f) }
    var lastDragX by remember { mutableFloatStateOf(0f) }
    var lastDragTime by remember { mutableLongStateOf(0L) }
    var sliderWidthPx by remember { mutableFloatStateOf(1f) }

    // External updates animate cleanly with a fast crisp spring (<300ms); active drag tracks 1:1 via snapTo.
    LaunchedEffect(percent, isDragging) {
        if (!isDragging) {
            animPercent.animateTo(
                targetValue = percent.coerceIn(0f, 1f),
                animationSpec = spring(
                    dampingRatio = 0.85f,
                    stiffness = 560f
                )
            )
        }
    }
    val animatedPercent = animPercent.value

    // Velocity-driven squash & stretch (Sakuga VFX principle)
    val targetStretchX = 1f + (abs(dragVelocity) / 3800f).coerceIn(0f, 0.20f)
    val targetSquashY = 1f - (abs(dragVelocity) / 7000f).coerceIn(0f, 0.12f)
    val stretchX by animateFloatAsState(
        targetValue = if (isDragging) targetStretchX else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "slider_stretch"
    )
    val squashY by animateFloatAsState(
        targetValue = if (isDragging) targetSquashY else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "slider_squash"
    )

    // Trailing dot-matrix spark particles
    val particles = remember { mutableStateListOf<DotSpark>() }

    LaunchedEffect(isDragging) {
        if (isDragging) {
            while (isDragging) {
                withFrameNanos {
                    val iterator = particles.listIterator()
                    while (iterator.hasNext()) {
                        val p = iterator.next()
                        p.x += p.vx
                        p.y += p.vy
                        p.alpha -= 0.05f
                        if (p.alpha <= 0f) {
                            iterator.remove()
                        }
                    }
                    // Spawn new spark if moving fast
                    if (abs(dragVelocity) > 200f && particles.size < 16) {
                        val edgeX = animatedPercent * sliderWidthPx
                        particles.add(
                            DotSpark(
                                x = edgeX + (kotlin.random.Random.nextFloat() - 0.5f) * 10f,
                                y = 23f + (kotlin.random.Random.nextFloat() - 0.5f) * 14f,
                                radius = kotlin.random.Random.nextFloat() * 2f + 1.2f,
                                alpha = 0.85f,
                                vx = -kotlin.math.sign(dragVelocity) * (kotlin.random.Random.nextFloat() * 2f + 0.5f),
                                vy = (kotlin.random.Random.nextFloat() - 0.5f) * 1.5f
                            )
                        )
                    }
                }
            }
        } else {
            particles.clear()
            dragVelocity = 0f
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(46.dp)
            .graphicsLayer {
                scaleX = stretchX
                scaleY = squashY
            }
            .clip(RoundedCornerShape(23.dp))
            .background(NothingTokens.NothingSurfaceMedium)
            .border(1.dp, NothingTokens.NothingBorderSubtle, RoundedCornerShape(23.dp))
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val newP = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                    scope.launch {
                        animPercent.animateTo(newP, spring(dampingRatio = 0.85f, stiffness = 560f))
                    }
                    onValueChange(newP)
                }
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        isDragging = true
                        lastDragX = offset.x
                        lastDragTime = System.currentTimeMillis()
                        dragVelocity = 0f
                        sliderWidthPx = size.width.toFloat().coerceAtLeast(1f)
                        val newP = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                        scope.launch { animPercent.snapTo(newP) }
                        onValueChange(newP)
                    },
                    onDragEnd = {
                        isDragging = false
                        dragVelocity = 0f
                    },
                    onDragCancel = {
                        isDragging = false
                        dragVelocity = 0f
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val now = System.currentTimeMillis()
                        val dt = (now - lastDragTime).coerceAtLeast(1L)
                        val dx = change.position.x - lastDragX
                        dragVelocity = (dx / dt) * 1000f
                        lastDragTime = now
                        lastDragX = change.position.x
                        sliderWidthPx = size.width.toFloat().coerceAtLeast(1f)

                        val newP = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                        scope.launch { animPercent.snapTo(newP) }
                        onValueChange(newP)
                    }
                )
            }
    ) {
        // Filled Progress Track
        val fillWidthFraction = animatedPercent.coerceIn(0.02f, 1f)
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(fillWidthFraction)
                .background(activeColor.copy(alpha = 0.28f))
        )

        // Trailing particles Canvas
        if (particles.isNotEmpty()) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                particles.forEach { p ->
                    if (p.alpha > 0f) {
                        drawCircle(
                            color = activeColor.copy(alpha = p.alpha.coerceIn(0f, 1f)),
                            radius = p.radius,
                            center = Offset(p.x, p.y)
                        )
                    }
                }
            }
        }

        // Leading pill thumb edge
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(fillWidthFraction)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(3.dp)
                    .fillMaxHeight(0.55f)
                    .clip(RoundedCornerShape(1.5.dp))
                    .background(activeColor.copy(alpha = 0.85f))
            )
        }

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
                text = "${(animatedPercent * 100).roundToInt()}%",
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
    accentColor: Color = NothingPrimaryAccent,
) {
    val scroll = rememberScrollState()
    val cardShape = if (isCameraCutoutEnabled) currentNotchedCardShape(cornerRadiusDp = 28f) else RoundedCornerShape(28.dp)
    var isEditing by remember { mutableStateOf(false) }

    val theme = com.nothing.island.ai.ui.theme.LocalIslandTheme.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .clip(cardShape)
            .background(theme.pillBackgroundColor)
            .border(1.dp, theme.borderColor, cardShape)
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
        // Top Punch-hole clearance spacer (36dp camera punch hole + 6dp safety margin)
        val topClearance = 42.dp
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
                        QuickToolTile(
                            tool = tool,
                            modifier = Modifier.weight(1f)
                        )
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
                activeColor = accentColor
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

