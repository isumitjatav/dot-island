package com.nothing.island.ai.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.nothing.island.ai.ui.theme.NothingTokens
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.round

/** Reactive behavioral states for Dot Island pixel companions. */
enum class MascotReactionState {
    IDLE,
    MUSIC_PLAYING,
    CHARGING,
    ALERT_CALL,
    PET_HOP
}

/**
 * Resolves the active companion reaction state with deterministic precedence:
 * PET_HOP > ALERT_CALL > CHARGING > MUSIC_PLAYING > IDLE. Pure and unit-testable.
 */
fun resolveMascotReactionState(
    isCallOrAlertActive: Boolean,
    isMusicPlaying: Boolean,
    isCharging: Boolean,
    isPetActive: Boolean = false
): MascotReactionState = when {
    isPetActive -> MascotReactionState.PET_HOP
    isCallOrAlertActive -> MascotReactionState.ALERT_CALL
    isCharging -> MascotReactionState.CHARGING
    isMusicPlaying -> MascotReactionState.MUSIC_PLAYING
    else -> MascotReactionState.IDLE
}

/**
 * Selects the exact pixel-grid frame for [sprite] given [reactionState] and [frameTick].
 * Falls back gracefully to base [MascotSprites.Sprite.rows] when a sprite lacks bespoke frames.
 */
fun selectMascotFrame(
    sprite: MascotSprites.Sprite,
    reactionState: MascotReactionState,
    frameTick: Int
): List<String> {
    val frames = when (reactionState) {
        MascotReactionState.PET_HOP -> sprite.happyFrames ?: sprite.alertFrames ?: sprite.idleFrames
        MascotReactionState.ALERT_CALL -> sprite.alertFrames ?: sprite.idleFrames
        MascotReactionState.CHARGING -> sprite.chargingFrames ?: sprite.idleFrames
        MascotReactionState.MUSIC_PLAYING -> sprite.musicFrames ?: sprite.walkFrames ?: sprite.idleFrames
        MascotReactionState.IDLE -> sprite.walkFrames ?: sprite.idleFrames
    } ?: return sprite.rows
    if (frames.isEmpty()) return sprite.rows
    val safeIndex = ((frameTick % frames.size) + frames.size) % frames.size
    return frames[safeIndex]
}

/**
 * Draws a pixel sprite grid with whole-pixel snapping so edges stay razor-sharp on AMOLED panels.
 */
private fun drawSpriteGrid(
    draw: DrawScope,
    rows: List<String>,
    palette: Map<Char, Color>,
    alpha: Float = 1f,
) {
    if (rows.isEmpty() || rows[0].isEmpty()) return
    val rowCount = rows.size
    val colCount = rows[0].length
    val rawCell = min(draw.size.width / colCount, draw.size.height / rowCount)
    val cell = if (rawCell >= 2f) floor(rawCell) else rawCell
    val startX = round((draw.size.width - colCount * cell) / 2f)
    val startY = round((draw.size.height - rowCount * cell) / 2f)
    rows.forEachIndexed { r, row ->
        row.forEachIndexed { c, ch ->
            palette[ch]?.let { baseColor ->
                draw.drawRect(
                    color = baseColor.copy(alpha = alpha),
                    topLeft = Offset(startX + c * cell, startY + r * cell),
                    size = Size(cell, cell),
                )
            }
        }
    }
}

/** Draws tiny rising #D71921 heart and white spark pixels during a tap/pet interaction. */
private fun drawPetParticles(draw: DrawScope, progress: Float) {
    if (progress <= 0f || progress >= 1f) return
    val alpha = (1f - progress).coerceIn(0f, 1f)
    val unit = (min(draw.size.width, draw.size.height) * 0.12f).coerceAtLeast(2f)
    val cx = draw.size.width * 0.5f
    val rise = progress * draw.size.height * 0.45f
    val yBase = draw.size.height * 0.22f - rise
    val red = NothingTokens.NothingRed.copy(alpha = alpha)
    val spark = Color.White.copy(alpha = alpha * 0.85f)
    draw.drawRect(red, Offset(cx - unit * 1.5f, yBase - unit), Size(unit, unit))
    draw.drawRect(red, Offset(cx + unit * 0.5f, yBase - unit), Size(unit, unit))
    draw.drawRect(red, Offset(cx - unit * 0.5f, yBase), Size(unit, unit))
    draw.drawRect(spark, Offset(cx - unit * 3.5f, yBase + unit * 0.4f), Size(unit, unit))
    draw.drawRect(spark, Offset(cx + unit * 2.5f, yBase - unit * 0.4f), Size(unit, unit))
}

/**
 * Compact reactive pixel companion rendered on the wing of active compact capsules
 * or standalone inside studios. Uses lambda [offset] and [graphicsLayer] for zero recomposition.
 */
@Composable
fun MascotReactionSprite(
    sprite: MascotSprites.Sprite,
    reactionState: MascotReactionState = MascotReactionState.IDLE,
    petTrigger: Int = 0,
    sizeDp: Dp = 20.dp,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val hop = remember { Animatable(0f) }
    val particleProgress = remember { Animatable(0f) }
    var isPetActive by remember { mutableStateOf(false) }

    val motion = rememberInfiniteTransition(label = "mascotReactionMotion")
    val frameClock by motion.animateFloat(
        initialValue = 0f,
        targetValue = 12f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (reactionState) {
                    MascotReactionState.ALERT_CALL -> 2800
                    MascotReactionState.MUSIC_PLAYING -> 3400
                    MascotReactionState.CHARGING -> 4200
                    else -> 6400
                },
                easing = LinearEasing,
            ),
            repeatMode = RepeatMode.Restart,
        ),
        label = "mascotFrameClock",
    )

    val bob by motion.animateFloat(
        initialValue = 0f,
        targetValue = when (reactionState) {
            MascotReactionState.MUSIC_PLAYING -> -2.5f
            MascotReactionState.ALERT_CALL -> -3f
            MascotReactionState.CHARGING -> -1.5f
            else -> -1.5f
        },
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (reactionState) {
                    MascotReactionState.MUSIC_PLAYING -> 380
                    MascotReactionState.ALERT_CALL -> 280
                    MascotReactionState.CHARGING -> 620
                    else -> 1100
                },
                easing = FastOutSlowInEasing,
            ),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "mascotReactionBob",
    )

    val chargingAlpha by motion.animateFloat(
        initialValue = 0.72f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(550, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "mascotChargingPulse",
    )

    LaunchedEffect(petTrigger) {
        if (petTrigger > 0) {
            isPetActive = true
            scope.launch {
                particleProgress.snapTo(0f)
                particleProgress.animateTo(1f, animationSpec = tween(520, easing = FastOutSlowInEasing))
            }
            hop.animateTo(-7f, animationSpec = tween(85, easing = FastOutSlowInEasing))
            hop.animateTo(0f, animationSpec = spring(dampingRatio = 0.62f, stiffness = 500f))
            delay(220)
            isPetActive = false
        }
    }

    val effectiveState = if (isPetActive) MascotReactionState.PET_HOP else reactionState

    Canvas(
        modifier = modifier
            .size(sizeDp)
            .offset { IntOffset(0, (hop.value + bob).dp.roundToPx()) }
            .graphicsLayer {
                alpha = if (effectiveState == MascotReactionState.CHARGING) chargingAlpha else 1f
            },
    ) {
        val frame = selectMascotFrame(sprite, effectiveState, frameClock.toInt())
        drawSpriteGrid(draw = this, rows = frame, palette = sprite.palette)
        drawPetParticles(draw = this, progress = particleProgress.value)
    }
}

/**
 * Compact pill with a pixel mascot that patrols without obscuring a center camera cutout,
 * reacts to Music / Charging / Call / Alerts, and performs a playful hop + heart burst on tap.
 */
@Composable
fun MascotPillPreview(
    modifier: Modifier = Modifier,
    sprite: MascotSprites.Sprite = MascotSprites.DOTCAT,
    reactionState: MascotReactionState = MascotReactionState.IDLE,
    petTrigger: Int = 0,
    avoidCenterCutout: Boolean = false,
    enableSelfTap: Boolean = true,
    renderContainer: Boolean = true,
) {
    val scope = rememberCoroutineScope()
    val hop = remember { Animatable(0f) }
    val particleProgress = remember { Animatable(0f) }
    var isPetActive by remember { mutableStateOf(false) }

    val walk = rememberInfiniteTransition(label = "mascotWalk")
    val (duration, easing) = when (sprite.motion) {
        MascotSprites.Motion.WALK -> 4000 to FastOutSlowInEasing
        MascotSprites.Motion.BOUNCE -> 3200 to FastOutSlowInEasing
        MascotSprites.Motion.FLOAT -> 6000 to FastOutSlowInEasing
        MascotSprites.Motion.WADDLE -> 5000 to FastOutSlowInEasing
    }
    val walkX by walk.animateFloat(
        initialValue = if (avoidCenterCutout) 38f else -40f,
        targetValue = if (avoidCenterCutout) 66f else 40f,
        animationSpec = infiniteRepeatable(
            animation = tween(duration, easing = easing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "mascotWalkX",
    )

    val frameClock by walk.animateFloat(
        initialValue = 0f,
        targetValue = 12f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (reactionState) {
                    MascotReactionState.ALERT_CALL -> 2600
                    MascotReactionState.MUSIC_PLAYING -> 3400
                    MascotReactionState.CHARGING -> 4200
                    else -> 6400
                },
                easing = LinearEasing,
            ),
            repeatMode = RepeatMode.Restart,
        ),
        label = "mascotFrameClock",
    )

    val bob by walk.animateFloat(
        initialValue = 0f,
        targetValue = when (reactionState) {
            MascotReactionState.MUSIC_PLAYING -> -3f
            MascotReactionState.ALERT_CALL -> -3.5f
            else -> -2.5f
        },
        animationSpec = infiniteRepeatable(
            animation = tween(
                when (reactionState) {
                    MascotReactionState.MUSIC_PLAYING -> 380
                    MascotReactionState.ALERT_CALL -> 280
                    else -> if (sprite.motion == MascotSprites.Motion.FLOAT) 1800 else 1000
                },
                easing = FastOutSlowInEasing,
            ),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "mascotBob",
    )

    val tilt by walk.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "mascotTilt",
    )

    val pulseGlow by walk.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(680, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "mascotBorderPulse",
    )

    fun doHop() {
        scope.launch {
            isPetActive = true
            launch {
                particleProgress.snapTo(0f)
                particleProgress.animateTo(1f, animationSpec = tween(550, easing = FastOutSlowInEasing))
            }
            hop.animateTo(-11f, animationSpec = tween(85, easing = FastOutSlowInEasing))
            hop.animateTo(0f, animationSpec = spring(dampingRatio = 0.62f, stiffness = 480f))
            delay(200)
            isPetActive = false
        }
    }

    LaunchedEffect(petTrigger) {
        if (petTrigger > 0) {
            doHop()
        }
    }

    val effectiveState = if (isPetActive) MascotReactionState.PET_HOP else reactionState
    val borderColor = when (effectiveState) {
        MascotReactionState.CHARGING -> NothingTokens.NothingRed.copy(alpha = pulseGlow)
        MascotReactionState.ALERT_CALL -> Color.White.copy(alpha = 0.45f)
        MascotReactionState.MUSIC_PLAYING -> NothingTokens.NothingRed.copy(alpha = 0.45f)
        else -> NothingTokens.NothingBorderSubtle
    }

    if (renderContainer) {
        Box(
            modifier = modifier
                .size(width = 124.dp, height = 36.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(NothingTokens.NothingBlack)
                .border(1.dp, borderColor, RoundedCornerShape(18.dp))
                .then(if (enableSelfTap) Modifier.clickable { doHop() } else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(
                modifier = Modifier
                    .size(22.dp)
                    .offset {
                        IntOffset(
                            x = walkX.dp.roundToPx(),
                            y = (hop.value + bob).dp.roundToPx(),
                        )
                    }
                    .graphicsLayer {
                        rotationZ = if (sprite.motion == MascotSprites.Motion.FLOAT || effectiveState == MascotReactionState.ALERT_CALL) 0f else tilt
                    },
            ) {
                val frame = selectMascotFrame(sprite, effectiveState, frameClock.toInt())
                drawSpriteGrid(draw = this, rows = frame, palette = sprite.palette)
                drawPetParticles(draw = this, progress = particleProgress.value)
            }
        }
    } else {
        // Transparent headroom overlay: perches mascot on right shoulder above 36.dp bottom-aligned pill.
        Box(
            modifier = modifier
                .fillMaxSize()
                .then(if (enableSelfTap) Modifier.clickable { doHop() } else Modifier),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Canvas(
                modifier = Modifier
                    .size(24.dp)
                    .offset {
                        IntOffset(
                            x = walkX.dp.roundToPx(),
                            y = (-24f + hop.value + bob).dp.roundToPx(),
                        )
                    }
                    .graphicsLayer {
                        rotationZ = if (sprite.motion == MascotSprites.Motion.FLOAT || effectiveState == MascotReactionState.ALERT_CALL) 0f else tilt
                    },
            ) {
                val frame = selectMascotFrame(sprite, effectiveState, frameClock.toInt())
                drawSpriteGrid(draw = this, rows = frame, palette = sprite.palette)
                drawPetParticles(draw = this, progress = particleProgress.value)
            }
        }
    }
}

/** Resolves a persisted sprite name (including legacy "CAT") to a Sprite, defaulting to DOTCAT. */
fun mascotByName(name: String): MascotSprites.Sprite {
    val trimmed = name.trim()
    if (trimmed.equals("CAT", ignoreCase = true) || trimmed.equals("DotCat", ignoreCase = true)) {
        return MascotSprites.DOTCAT
    }
    return MascotSprites.ALL.firstOrNull {
        it.name.equals(trimmed, ignoreCase = true) || it.displayName.equals(trimmed, ignoreCase = true)
    } ?: MascotSprites.DOTCAT
}

/**
 * A row of the available mascots. Tap one to select it; the selected one is
 * shown with a red outline.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MascotPicker(
    selected: MascotSprites.Sprite,
    onSelect: (MascotSprites.Sprite) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        MascotSprites.ALL.forEach { sprite ->
            val isSelected = sprite.name == selected.name
            Box(
                modifier = Modifier
                    .size(44.dp)
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
                Canvas(modifier = Modifier.fillMaxSize(0.68f)) {
                    drawSpriteGrid(
                        draw = this,
                        rows = sprite.rows,
                        palette = sprite.palette,
                        alpha = if (isSelected) 1f else 0.65f,
                    )
                }
            }
        }
    }
}
