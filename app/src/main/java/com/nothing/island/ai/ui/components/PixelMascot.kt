package com.nothing.island.ai.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.IntOffset
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nothing.island.ai.ui.theme.NothingTokens
import kotlinx.coroutines.launch

/** Draws an 8x8 grid sprite centered and scaled to the canvas. */
private fun drawSprite(
    draw: androidx.compose.ui.graphics.drawscope.DrawScope,
    sprite: MascotSprites.Sprite,
    cell: Float,
    alpha: Float = 1f,
) {
    val rows = sprite.rows.size
    val cols = sprite.rows[0].length
    val startX = (draw.size.width - cols * cell) / 2f
    val startY = (draw.size.height - rows * cell) / 2f
    sprite.rows.forEachIndexed { r, row ->
        row.forEachIndexed { c, ch ->
            sprite.palette[ch]?.let { baseColor ->
                draw.drawRect(
                    color = baseColor.copy(alpha = alpha),
                    topLeft = Offset(startX + c * cell, startY + r * cell),
                    size = Size(cell, cell),
                )
            }
        }
    }
}

/**
 * Compact pill with a pixel mascot that walks back and forth and performs a
 * one-shot hop on tap (up then back down automatically).
 */
@Composable
fun MascotPillPreview(
    modifier: Modifier = Modifier,
    sprite: MascotSprites.Sprite = MascotSprites.CAT,
) {
    val scope = rememberCoroutineScope()
    val hop = remember { Animatable(0f) }

    // Continuous walk back and forth; duration and easing vary per sprite.
    val walk = rememberInfiniteTransition(label = "mascotWalk")
    val (duration, easing) = when (sprite.motion) {
        MascotSprites.Motion.WALK -> 4000 to FastOutSlowInEasing
        MascotSprites.Motion.BOUNCE -> 3200 to FastOutSlowInEasing
        MascotSprites.Motion.FLOAT -> 6000 to FastOutSlowInEasing
        MascotSprites.Motion.WADDLE -> 5000 to FastOutSlowInEasing
    }
    val walkX by walk.animateFloat(
        initialValue = 8f,
        targetValue = 88f,
        animationSpec = infiniteRepeatable(
            animation = tween(duration, easing = easing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "mascotWalkX",
    )

    // Smooth bob + tilt reads as walking without flickering leg frames.
    val bob by walk.animateFloat(
        initialValue = 0f,
        targetValue = -3f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (sprite.motion == MascotSprites.Motion.FLOAT) 1800 else 1100),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "mascotBob",
    )
    val tilt by walk.animateFloat(
        initialValue = -8f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "mascotTilt",
    )
    val activeSprite = sprite

    // One-shot hop: crisp upward launch followed by interruptible spring settle.
    fun doHop() {
        scope.launch {
            hop.animateTo(-12f, animationSpec = tween(90, easing = FastOutSlowInEasing))
            hop.animateTo(0f, animationSpec = spring(dampingRatio = 0.65f, stiffness = 480f))
        }
    }

    Box(
        modifier = modifier
            .size(width = 124.dp, height = 36.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(NothingTokens.NothingBlack)
            .border(1.dp, NothingTokens.NothingBorderSubtle, RoundedCornerShape(18.dp))
            .clickable { doHop() },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(
            modifier = Modifier
                .size(20.dp)
                .offset {
                    IntOffset(
                        x = (walkX.dp - 44.dp).roundToPx(),
                        y = (hop.value + bob).dp.roundToPx(),
                    )
                }
                .graphicsLayer { rotationZ = if (sprite.motion == MascotSprites.Motion.FLOAT) 0f else tilt },
        ) {
            drawSprite(draw = this, sprite = activeSprite, cell = size.width / activeSprite.rows[0].length)
        }
    }
}

/** Resolves a persisted sprite name to a Sprite, defaulting to CAT. */
fun mascotByName(name: String): MascotSprites.Sprite =
    MascotSprites.ALL.firstOrNull { it.name == name } ?: MascotSprites.CAT

/**
 * A row of the available mascots. Tap one to select it; the selected one is
 * shown with a red outline. This is the picker UI for the feature.
 */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun MascotPicker(
    selected: MascotSprites.Sprite,
    onSelect: (MascotSprites.Sprite) -> Unit,
    modifier: Modifier = Modifier,
) {
    androidx.compose.foundation.layout.FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        MascotSprites.ALL.forEach { sprite ->
            val isSelected = sprite.name == selected.name
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(NothingTokens.NothingSurfaceLow)
                    .border(
                        if (isSelected) 2.dp else 1.dp,
                        if (isSelected) NothingTokens.NothingRed else NothingTokens.NothingBorderSubtle,
                        RoundedCornerShape(10.dp),
                    )
                    .clickable { onSelect(sprite) },
                contentAlignment = Alignment.Center,
            ) {
                Canvas(modifier = Modifier.fillMaxSize(0.7f)) {
                    drawSprite(
                        draw = this,
                        sprite = sprite,
                        cell = size.width / sprite.rows[0].length,
                        alpha = if (isSelected) 1f else 0.6f,
                    )
                }
            }
        }
    }
}
