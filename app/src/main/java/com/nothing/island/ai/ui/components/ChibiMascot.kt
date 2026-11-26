package com.nothing.island.ai.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

/** Six expressive motion states for the articulated Chibi Companion. */
enum class ChibiState {
    IDLE,
    CALL,
    MUSIC,
    NOTIFICATION,
    CHARGING,
    PET,
}

/** Notification entrance transition: peek behind capsule top -> hang off capsule bottom. */
enum class NotifMascotPhase {
    PEEK,
    HANG,
}

/** Sub-phases of the IDLE cycle: left shoulder nap -> wake stretch -> hop over camera -> right wave. */
enum class IdleCyclePhase {
    SLEEP_LEFT,
    STRETCH,
    HOP_TO_RIGHT,
    WAVE_RIGHT,
    HOP_TO_LEFT,
}

/** Style accent variant matching persisted companion choices. */
enum class ChibiVariant {
    DOT_CHIBI,
    GLYPH_CHIBI,
    TAPE_CHIBI,
}

private val ChibiWhite = Color(0xFFFFFFFF)
private val ChibiOutline = Color(0xFF141414)
private val ChibiRed = Color(0xFFD71921)
private val ChibiMuted = Color(0xFF9CA3AF)

fun resolveChibiVariant(spriteName: String): ChibiVariant {
    val norm = spriteName.trim().uppercase()
    return when {
        norm.contains("GLYPH") || norm.contains("ORB") -> ChibiVariant.GLYPH_CHIBI
        norm.contains("TAPE") -> ChibiVariant.TAPE_CHIBI
        else -> ChibiVariant.DOT_CHIBI
    }
}

/**
 * Pure priority resolver determining which [ChibiState] the companion adopts
 * for the current island state.
 */
fun resolveChibiState(
    isCallActive: Boolean,
    isMusicPlaying: Boolean,
    hasActiveNotification: Boolean,
    isCharging: Boolean,
    isPetActive: Boolean = false,
): ChibiState = when {
    isPetActive -> ChibiState.PET
    isCallActive -> ChibiState.CALL
    hasActiveNotification -> ChibiState.NOTIFICATION
    isMusicPlaying -> ChibiState.MUSIC
    isCharging -> ChibiState.CHARGING
    else -> ChibiState.IDLE
}

/**
 * Resolves the chibi's zIndex relative to the capsule layer (`zIndex = 1f`).
 * Notification peek slips behind the pill (`0f`), while hanging/dancing/pushing/pet sit in front (`2f`).
 */
fun resolveChibiZIndex(state: ChibiState, notifPhase: NotifMascotPhase): Float = when {
    state == ChibiState.NOTIFICATION && notifPhase == NotifMascotPhase.PEEK -> 0f
    else -> 2f
}

/**
 * Computes the horizontal and vertical patrol offset (in dp relative to capsule top-center)
 * during IDLE cycle progress, elevating the hop cleanly across the center camera punch-hole
 * safe zone `|x| <= cameraSafeHalfWidthDp` while staying strictly below the top capsule edge (`y >= 0`).
 */
fun computeIdlePatrolPose(
    cycleElapsedMs: Long,
    shoulderXAbsDp: Float = 38f,
    cameraSafeHalfWidthDp: Float = 18f,
): Triple<IdleCyclePhase, Float, Float> {
    val totalMs = 11_000L
    val t = ((cycleElapsedMs % totalMs) + totalMs) % totalMs
    val restingY = 11f
    return when {
        t < 5_000L -> Triple(IdleCyclePhase.SLEEP_LEFT, -shoulderXAbsDp, restingY)
        t < 6_000L -> {
            val p = (t - 5_000L) / 1_000f
            Triple(IdleCyclePhase.STRETCH, -shoulderXAbsDp, (restingY - sin(p * PI).toFloat() * 3f).coerceAtLeast(1f))
        }
        t < 7_200L -> {
            val p = (t - 6_000L) / 1_200f
            val x = -shoulderXAbsDp + (2f * shoulderXAbsDp * p)
            val arcLift = sin(p * PI).toFloat() * 8f
            val cameraBoost = if (abs(x) <= cameraSafeHalfWidthDp) 2.5f else 0f
            val y = (restingY - arcLift - cameraBoost).coerceAtLeast(0.5f)
            Triple(IdleCyclePhase.HOP_TO_RIGHT, x, y)
        }
        t < 9_800L -> Triple(IdleCyclePhase.WAVE_RIGHT, shoulderXAbsDp, restingY - 1f)
        else -> {
            val p = (t - 9_800L) / 1_200f
            val x = shoulderXAbsDp - (2f * shoulderXAbsDp * p)
            val arcLift = sin(p * PI).toFloat() * 8f
            val cameraBoost = if (abs(x) <= cameraSafeHalfWidthDp) 2.5f else 0f
            val y = (restingY - arcLift - cameraBoost).coerceAtLeast(0.5f)
            Triple(IdleCyclePhase.HOP_TO_LEFT, x, y)
        }
    }
}

/** Tight compact overlay window specification matching visible capsule + active chibi overhang only. */
data class CompactStageWindowSpec(
    val widthDp: Float,
    val heightDp: Float,
    val capsuleWidthDp: Float,
    val capsuleHeightDp: Float,
)

fun computeCallCompactChibiXOffsetDp(pillWidthDp: Float): Float =
    (pillWidthDp / 2f) - 4f

fun computeCallCompactChibiYOffsetDp(pillTopDp: Float = 0f): Float =
    pillTopDp + 22f

fun computeCompactStageWindowSpec(
    compactWidthDp: Float,
    compactHeightDp: Float,
    mascotEnabled: Boolean,
    isDualPill: Boolean,
    isEarbudActive: Boolean,
    isSystemAlertActive: Boolean,
    isCallActive: Boolean,
    hasActiveNotification: Boolean,
    isMusicPlaying: Boolean,
    isChargingActive: Boolean,
): CompactStageWindowSpec = when {
    isDualPill -> CompactStageWindowSpec(220f, compactHeightDp, 220f, compactHeightDp)
    isEarbudActive -> CompactStageWindowSpec(236f, 38f, 236f, 38f)
    isSystemAlertActive -> CompactStageWindowSpec(220f, 36f, 220f, 36f)
    isCallActive -> CompactStageWindowSpec(
        widthDp = if (mascotEnabled) 232f else 200f,
        heightDp = if (mascotEnabled) maxOf(compactHeightDp, 44f) else compactHeightDp,
        capsuleWidthDp = 200f,
        capsuleHeightDp = compactHeightDp,
    )
    hasActiveNotification -> CompactStageWindowSpec(
        widthDp = 200f,
        heightDp = if (mascotEnabled) 68f else compactHeightDp,
        capsuleWidthDp = 200f,
        capsuleHeightDp = compactHeightDp,
    )
    isMusicPlaying && !isChargingActive -> CompactStageWindowSpec(
        widthDp = if (mascotEnabled) 300f else 224f,
        heightDp = if (mascotEnabled) 44f else compactHeightDp,
        capsuleWidthDp = 224f,
        capsuleHeightDp = compactHeightDp,
    )
    isChargingActive -> CompactStageWindowSpec(
        widthDp = 200f,
        heightDp = if (mascotEnabled) 64f else compactHeightDp,
        capsuleWidthDp = 200f,
        capsuleHeightDp = compactHeightDp,
    )
    else -> CompactStageWindowSpec(
        widthDp = compactWidthDp,
        heightDp = compactHeightDp,
        capsuleWidthDp = compactWidthDp,
        capsuleHeightDp = compactHeightDp,
    )
}

/**
 * Stage container constants: centered black capsule (`zIndex = 1f`).
 */
object ChibiStageMetrics {
    const val STAGE_WIDTH_DP = 280f
    const val STAGE_HEIGHT_DP = 92f
    const val CAPSULE_Z_INDEX = 1f
    const val IDLE_CHIBI_SPRITE_DP = 16f
    const val CHIBI_SPRITE_DP = 28f
}

/**
 * Overlay-hosted articulated Chibi Companion rendered on a transparent 280x92dp stage.
 * Physically interacts with the capsule edges across all 6 motion states and reports its live
 * screen-root hitbox via [onMascotBoundsChanged] so outside touches pass through cleanly.
 */
@Composable
fun ChibiStageCompanion(
    state: ChibiState,
    variant: ChibiVariant = ChibiVariant.DOT_CHIBI,
    pillWidthDp: Float,
    pillHeightDp: Float,
    pillTopDp: Float,
    batteryPercent: Int = 50,
    isCallRinging: Boolean = false,
    notificationKey: String? = null,
    reducedMotionEnabled: Boolean = false,
    onPet: () -> Unit = {},
    onMascotBoundsChanged: (Int, Int, Int, Int) -> Unit = { _, _, _, _ -> },
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val petHop = remember { Animatable(0f) }
    val heartProgress = remember { Animatable(0f) }
    var isPetPlaying by remember { mutableStateOf(false) }
    var petCount by remember { mutableIntStateOf(0) }

    var notifPhase by remember(notificationKey, state) {
        mutableStateOf(NotifMascotPhase.PEEK)
    }
    LaunchedEffect(notificationKey, state) {
        if (state == ChibiState.NOTIFICATION) {
            notifPhase = NotifMascotPhase.PEEK
            if (!reducedMotionEnabled) {
                delay(1_050L)
                notifPhase = NotifMascotPhase.HANG
            } else {
                notifPhase = NotifMascotPhase.HANG
            }
        }
    }

    var idleClockMs by remember { mutableStateOf(0L) }
    LaunchedEffect(state, reducedMotionEnabled) {
        if (state == ChibiState.IDLE && !reducedMotionEnabled) {
            while (true) {
                delay(32L)
                idleClockMs += 32L
            }
        }
    }

    fun triggerPet() {
        petCount++
        onPet()
        scope.launch {
            isPetPlaying = true
            launch {
                heartProgress.snapTo(0f)
                heartProgress.animateTo(1f, animationSpec = tween(700, easing = FastOutSlowInEasing))
            }
            if (!reducedMotionEnabled) {
                petHop.snapTo(0f)
                petHop.animateTo(-14f, animationSpec = tween(110, easing = FastOutSlowInEasing))
                petHop.animateTo(0f, animationSpec = spring(dampingRatio = 0.55f, stiffness = 460f))
            }
            delay(280L)
            isPetPlaying = false
        }
    }

    val activeState = if (isPetPlaying) ChibiState.PET else state
    val infinite = rememberInfiniteTransition(label = "chibiInfinite")
    val animBeat by infinite.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "chibiBeat",
    )

    val idlePose = if (reducedMotionEnabled) {
        Triple(IdleCyclePhase.WAVE_RIGHT, 38f, 10f)
    } else {
        computeIdlePatrolPose(idleClockMs)
    }

    val targetZIndex = resolveChibiZIndex(activeState, notifPhase)
    val spriteSizeDp = if (activeState == ChibiState.IDLE || activeState == ChibiState.CALL) {
        ChibiStageMetrics.IDLE_CHIBI_SPRITE_DP
    } else {
        ChibiStageMetrics.CHIBI_SPRITE_DP
    }

    // Compute (xDp from stage horizontal center, yDp from stage top)
    val pillBottomDp = pillTopDp + pillHeightDp
    val halfPill = pillWidthDp / 2f

    var targetX = 38f
    var targetY = pillTopDp + 10f
    var bodyTiltDeg = 0f
    var leftArmAngle = 0f
    var rightArmAngle = 0f

    val pulse = if (reducedMotionEnabled) 0f else sin(animBeat)
    val fastPulse = if (reducedMotionEnabled) 0f else sin(animBeat * 2f)

    when (activeState) {
        ChibiState.IDLE -> {
            val (phase, patrolX, idleOffsetY) = idlePose
            targetX = patrolX
            // Anchor shifted downward (+10dp) inside the 36dp pill so ears & Zzz remain strictly below top border (y >= 0)
            targetY = (pillTopDp + idleOffsetY).coerceAtLeast(pillTopDp)
            when (phase) {
                IdleCyclePhase.SLEEP_LEFT -> {
                    bodyTiltDeg = -6f
                    leftArmAngle = 15f
                    rightArmAngle = 15f
                }
                IdleCyclePhase.STRETCH -> {
                    bodyTiltDeg = 0f
                    leftArmAngle = -135f
                    rightArmAngle = -135f
                }
                IdleCyclePhase.HOP_TO_RIGHT, IdleCyclePhase.HOP_TO_LEFT -> {
                    bodyTiltDeg = if (phase == IdleCyclePhase.HOP_TO_RIGHT) 8f else -8f
                    leftArmAngle = -55f
                    rightArmAngle = -55f
                }
                IdleCyclePhase.WAVE_RIGHT -> {
                    bodyTiltDeg = pulse * 3f
                    leftArmAngle = 12f
                    rightArmAngle = -125f + fastPulse * 20f
                }
            }
        }

        ChibiState.CALL -> {
            // Nudged downward onto the lower-right curve of the call pill so 5G+ in the status bar stays unobstructed.
            targetX = computeCallCompactChibiXOffsetDp(pillWidthDp)
            targetY = (computeCallCompactChibiYOffsetDp(pillTopDp) - abs(fastPulse) * 1.5f).coerceAtLeast(pillTopDp + 12f)
            if (isCallRinging) {
                bodyTiltDeg = fastPulse * 6f
                leftArmAngle = if (fastPulse > 0f) 35f else -65f
                rightArmAngle = if (fastPulse <= 0f) 35f else -65f
            } else {
                bodyTiltDeg = pulse * 4f
                leftArmAngle = -130f + pulse * 12f
                rightArmAngle = -130f - pulse * 12f
            }
        }

        ChibiState.MUSIC -> {
            // Steps outside the right edge of the music pill (pillRight + 6dp) and dances
            targetX = halfPill + 6f + (spriteSizeDp / 2f)
            targetY = pillTopDp + 6f - abs(fastPulse) * 4f
            bodyTiltDeg = pulse * 9f
            leftArmAngle = -75f + fastPulse * 42f
            rightArmAngle = -75f - fastPulse * 42f
        }

        ChibiState.NOTIFICATION -> {
            targetX = 56f
            if (notifPhase == NotifMascotPhase.PEEK) {
                // Behind top edge of pill (zIndex 0f)
                targetY = pillTopDp
                leftArmAngle = -35f
                rightArmAngle = -35f
            } else {
                // Gripping bottom border with feet dangling/swaying underneath (zIndex 2f)
                targetY = pillBottomDp - 4f
                bodyTiltDeg = pulse * 10f
                leftArmAngle = -155f
                rightArmAngle = -155f
            }
        }

        ChibiState.CHARGING -> {
            val rawLeadX = (-76f + 148f * (batteryPercent.coerceIn(0, 100) / 100f))
            targetX = if (abs(rawLeadX) < 24f) 38f else rawLeadX
            targetY = pillBottomDp - 6f
            bodyTiltDeg = 16f + abs(fastPulse) * 4f
            leftArmAngle = -85f
            rightArmAngle = -95f
        }

        ChibiState.PET -> {
            targetX = 38f
            targetY = (pillTopDp + 8f).coerceAtLeast(pillTopDp)
            bodyTiltDeg = fastPulse * 8f
            leftArmAngle = -140f
            rightArmAngle = -140f
        }
    }

    val animatedX by animateFloatAsState(
        targetValue = targetX,
        animationSpec = if (reducedMotionEnabled) tween(1) else spring(dampingRatio = 0.72f, stiffness = 360f),
        label = "chibiX",
    )
    val animatedY by animateFloatAsState(
        targetValue = targetY,
        animationSpec = if (reducedMotionEnabled) tween(1) else spring(dampingRatio = 0.72f, stiffness = 360f),
        label = "chibiY",
    )

    val petHopY = if (activeState == ChibiState.IDLE || activeState == ChibiState.CALL) {
        (animatedY + (petHop.value * 0.5f)).coerceAtLeast(pillTopDp)
    } else {
        animatedY + petHop.value
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .zIndex(targetZIndex),
        contentAlignment = Alignment.TopCenter,
    ) {
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = animatedX.dp.roundToPx(),
                        y = petHopY.dp.roundToPx(),
                    )
                }
                .onGloballyPositioned { coords ->
                    val p = coords.positionInRoot()
                    val s = coords.size
                    onMascotBoundsChanged(
                        p.x.toInt(),
                        p.y.toInt(),
                        (p.x + s.width).toInt(),
                        (p.y + s.height).toInt(),
                    )
                }
                .size(spriteSizeDp.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) {
                    triggerPet()
                }
                .graphicsLayer {
                    rotationZ = if (reducedMotionEnabled) 0f else bodyTiltDeg
                },
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawArticulatedChibi(
                    variant = variant,
                    state = activeState,
                    idlePhase = idlePose.first,
                    notifPhase = notifPhase,
                    isCallRinging = isCallRinging,
                    beatRad = if (reducedMotionEnabled) 0f else animBeat,
                    leftArmDeg = leftArmAngle,
                    rightArmDeg = rightArmAngle,
                    heartProgress = heartProgress.value,
                    reducedMotion = reducedMotionEnabled,
                )
            }
        }
    }
}

/**
 * Smiling chibi badge rendered cleanly above the Answer/Decline controls in ExpandedViewType.CALL.
 */
@Composable
fun ChibiExpandedCallHeaderBadge(
    spriteName: String,
    reducedMotionEnabled: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val variant = resolveChibiVariant(spriteName)
    val infinite = rememberInfiniteTransition(label = "chibiCallHeader")
    val beat by infinite.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "chibiCallHeaderBeat",
    )
    val bobDp = if (reducedMotionEnabled) 0f else -abs(sin(beat)) * 1.5f
    Box(
        modifier = modifier
            .offset { IntOffset(0, bobDp.dp.roundToPx()) }
            .size(20.dp),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawArticulatedChibi(
                variant = variant,
                state = ChibiState.CALL,
                idlePhase = IdleCyclePhase.WAVE_RIGHT,
                notifPhase = NotifMascotPhase.PEEK,
                isCallRinging = false,
                beatRad = if (reducedMotionEnabled) 0f else beat,
                leftArmDeg = -130f,
                rightArmDeg = -130f,
                heartProgress = 0f,
                reducedMotion = reducedMotionEnabled,
            )
        }
    }
}

private fun DrawScope.drawArticulatedChibi(
    variant: ChibiVariant,
    state: ChibiState,
    idlePhase: IdleCyclePhase,
    notifPhase: NotifMascotPhase,
    isCallRinging: Boolean,
    beatRad: Float,
    leftArmDeg: Float,
    rightArmDeg: Float,
    heartProgress: Float,
    reducedMotion: Boolean,
) {
    val w = size.width
    val h = size.height
    val cx = w / 2f
    val strokeW = (w * 0.06f).coerceAtLeast(1.5f)
    val outlineStroke = Stroke(width = strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round)

    // 1. Dangling or standing feet
    val legSwing = if (state == ChibiState.NOTIFICATION && notifPhase == NotifMascotPhase.HANG && !reducedMotion) {
        sin(beatRad * 2f) * (w * 0.06f)
    } else if ((state == ChibiState.IDLE && (idlePhase == IdleCyclePhase.HOP_TO_RIGHT || idlePhase == IdleCyclePhase.HOP_TO_LEFT)) && !reducedMotion) {
        sin(beatRad * 3f) * (w * 0.05f)
    } else {
        0f
    }
    val footY = h * 0.84f
    val footR = w * 0.075f
    drawCircle(ChibiWhite, radius = footR, center = Offset(cx - w * 0.11f + legSwing, footY))
    drawCircle(ChibiOutline, radius = footR, center = Offset(cx - w * 0.11f + legSwing, footY), style = outlineStroke)
    drawCircle(ChibiWhite, radius = footR, center = Offset(cx + w * 0.11f - legSwing, footY))
    drawCircle(ChibiOutline, radius = footR, center = Offset(cx + w * 0.11f - legSwing, footY), style = outlineStroke)

    // 2. Compact torso pill
    val torsoW = w * 0.34f
    val torsoH = h * 0.24f
    val torsoTop = h * 0.58f
    drawRoundRect(
        color = ChibiWhite,
        topLeft = Offset(cx - torsoW / 2f, torsoTop),
        size = Size(torsoW, torsoH),
        cornerRadius = CornerRadius(torsoW / 2f, torsoW / 2f),
    )
    drawRoundRect(
        color = ChibiOutline,
        topLeft = Offset(cx - torsoW / 2f, torsoTop),
        size = Size(torsoW, torsoH),
        cornerRadius = CornerRadius(torsoW / 2f, torsoW / 2f),
        style = outlineStroke,
    )
    // Red collar badge dot
    drawCircle(color = ChibiRed, radius = w * 0.045f, center = Offset(cx, torsoTop + torsoH * 0.45f))

    // 3. Articulated left & right arms
    val armLen = w * 0.19f
    val armStroke = w * 0.095f
    val lShoulder = Offset(cx - torsoW * 0.46f, torsoTop + torsoH * 0.28f)
    val rShoulder = Offset(cx + torsoW * 0.46f, torsoTop + torsoH * 0.28f)

    rotate(degrees = leftArmDeg, pivot = lShoulder) {
        drawLine(ChibiOutline, start = lShoulder, end = Offset(lShoulder.x - armLen, lShoulder.y), strokeWidth = armStroke + strokeW, cap = StrokeCap.Round)
        drawLine(ChibiWhite, start = lShoulder, end = Offset(lShoulder.x - armLen, lShoulder.y), strokeWidth = armStroke, cap = StrokeCap.Round)
    }
    rotate(degrees = rightArmDeg, pivot = rShoulder) {
        drawLine(ChibiOutline, start = rShoulder, end = Offset(rShoulder.x + armLen, rShoulder.y), strokeWidth = armStroke + strokeW, cap = StrokeCap.Round)
        drawLine(ChibiWhite, start = rShoulder, end = Offset(rShoulder.x + armLen, rShoulder.y), strokeWidth = armStroke, cap = StrokeCap.Round)
    }

    // 4. Rounded Chibi Head + Ears
    val headCenter = Offset(cx, h * 0.38f)
    val headRx = w * 0.29f
    val headRy = h * 0.25f

    // Pointed cat/fox ears (or visor/halo accent for variants)
    val leftEar = Path().apply {
        moveTo(cx - headRx * 0.80f, headCenter.y - headRy * 0.25f)
        lineTo(cx - headRx * 0.92f, headCenter.y - headRy * 1.38f)
        lineTo(cx - headRx * 0.18f, headCenter.y - headRy * 0.85f)
        close()
    }
    val rightEar = Path().apply {
        moveTo(cx + headRx * 0.80f, headCenter.y - headRy * 0.25f)
        lineTo(cx + headRx * 0.92f, headCenter.y - headRy * 1.38f)
        lineTo(cx + headRx * 0.18f, headCenter.y - headRy * 0.85f)
        close()
    }
    drawPath(leftEar, ChibiWhite)
    drawPath(leftEar, ChibiOutline, style = outlineStroke)
    drawPath(rightEar, ChibiWhite)
    drawPath(rightEar, ChibiOutline, style = outlineStroke)

    // Red inner ear accents
    drawCircle(ChibiRed, radius = w * 0.036f, center = Offset(cx - headRx * 0.65f, headCenter.y - headRy * 0.90f))
    drawCircle(ChibiRed, radius = w * 0.036f, center = Offset(cx + headRx * 0.65f, headCenter.y - headRy * 0.90f))

    // Head oval
    val headTopLeft = Offset(cx - headRx, headCenter.y - headRy)
    val headSize = Size(headRx * 2f, headRy * 2f)
    drawRoundRect(
        color = ChibiWhite,
        topLeft = headTopLeft,
        size = headSize,
        cornerRadius = CornerRadius(headRx * 0.95f, headRy * 0.95f),
    )
    drawRoundRect(
        color = ChibiOutline,
        topLeft = headTopLeft,
        size = headSize,
        cornerRadius = CornerRadius(headRx * 0.95f, headRy * 0.95f),
        style = outlineStroke,
    )

    // Variant brow accent
    if (variant == ChibiVariant.GLYPH_CHIBI) {
        drawArc(
            color = ChibiRed,
            startAngle = 210f,
            sweepAngle = 120f,
            useCenter = false,
            topLeft = Offset(cx - headRx * 0.5f, headCenter.y - headRy * 0.88f),
            size = Size(headRx, headRy * 0.5f),
            style = Stroke(width = strokeW * 0.85f, cap = StrokeCap.Round),
        )
    } else if (variant == ChibiVariant.TAPE_CHIBI) {
        drawLine(
            color = ChibiRed,
            start = Offset(cx - headRx * 0.55f, headCenter.y - headRy * 0.65f),
            end = Offset(cx + headRx * 0.55f, headCenter.y - headRy * 0.65f),
            strokeWidth = strokeW,
            cap = StrokeCap.Round,
        )
    }

    // 5. Expressive Eyes & Mouth
    val eyeY = headCenter.y + headRy * 0.05f
    val eyeSpread = headRx * 0.42f
    val eyeR = w * 0.05f
    val isSleeping = state == ChibiState.IDLE && idlePhase == IdleCyclePhase.SLEEP_LEFT

    if (isSleeping) {
        // Closed calm eyes + floating Zzz
        drawLine(ChibiOutline, Offset(cx - eyeSpread - eyeR, eyeY), Offset(cx - eyeSpread + eyeR, eyeY), strokeWidth = strokeW, cap = StrokeCap.Round)
        drawLine(ChibiOutline, Offset(cx + eyeSpread - eyeR, eyeY), Offset(cx + eyeSpread + eyeR, eyeY), strokeWidth = strokeW, cap = StrokeCap.Round)

        if (!reducedMotion) {
            val zRise = ((beatRad / (2f * PI.toFloat())) % 1f)
            // Keep Zzz strictly inside sprite vertical bounds [strokeW .. h*0.30] below top capsule edge
            val zY = (h * (0.28f - zRise * 0.16f)).coerceAtLeast(strokeW)
            val zX = cx + headRx * 0.85f
            val zs = w * 0.085f
            val zAlpha = (1f - zRise).coerceIn(0.2f, 1f)
            drawLine(ChibiRed.copy(alpha = zAlpha), Offset(zX, zY), Offset(zX + zs, zY), strokeWidth = strokeW * 0.85f, cap = StrokeCap.Round)
            drawLine(ChibiRed.copy(alpha = zAlpha), Offset(zX + zs, zY), Offset(zX, zY + zs), strokeWidth = strokeW * 0.85f, cap = StrokeCap.Round)
            drawLine(ChibiRed.copy(alpha = zAlpha), Offset(zX, zY + zs), Offset(zX + zs, zY + zs), strokeWidth = strokeW * 0.85f, cap = StrokeCap.Round)
        }
    } else if (state == ChibiState.CHARGING) {
        // Determined push eyes
        drawCircle(ChibiRed, radius = eyeR, center = Offset(cx - eyeSpread, eyeY))
        drawCircle(ChibiRed, radius = eyeR, center = Offset(cx + eyeSpread, eyeY))
        // Spark flecks at leading push hands
        if (!reducedMotion) {
            val sparkShift = (sin(beatRad * 3f) * w * 0.05f)
            drawCircle(ChibiRed, radius = w * 0.032f, center = Offset(cx + headRx + w * 0.14f, torsoTop + sparkShift))
            drawCircle(ChibiWhite, radius = w * 0.025f, center = Offset(cx + headRx + w * 0.22f, torsoTop - sparkShift))
        }
    } else {
        // Signature red glowing eyes
        drawCircle(ChibiRed, radius = eyeR, center = Offset(cx - eyeSpread, eyeY))
        drawCircle(ChibiRed, radius = eyeR, center = Offset(cx + eyeSpread, eyeY))
        // Happy tiny smile
        val mouthY = headCenter.y + headRy * 0.42f
        drawArc(
            color = ChibiOutline,
            startAngle = 15f,
            sweepAngle = 150f,
            useCenter = false,
            topLeft = Offset(cx - w * 0.075f, mouthY - w * 0.04f),
            size = Size(w * 0.15f, w * 0.09f),
            style = Stroke(width = strokeW * 0.8f, cap = StrokeCap.Round),
        )
    }

    // 6. Floating music notes (♪) or rising pet pixel heart (♥)
    if (state == ChibiState.MUSIC && !reducedMotion) {
        val noteLift = ((beatRad / (2f * PI.toFloat())) % 1f) * h * 0.22f
        val noteX = cx + headRx * 0.9f
        val noteY = h * 0.25f - noteLift
        drawCircle(ChibiRed, radius = w * 0.04f, center = Offset(noteX, noteY))
        drawLine(ChibiRed, start = Offset(noteX + w * 0.03f, noteY), end = Offset(noteX + w * 0.03f, noteY - h * 0.14f), strokeWidth = strokeW * 0.85f, cap = StrokeCap.Round)
        drawLine(ChibiRed, start = Offset(noteX + w * 0.03f, noteY - h * 0.14f), end = Offset(noteX + w * 0.12f, noteY - h * 0.10f), strokeWidth = strokeW * 0.85f, cap = StrokeCap.Round)
    }

    if (heartProgress > 0.01f && heartProgress < 0.99f) {
        val hx = cx
        val hy = h * (0.14f - heartProgress * 0.14f)
        val alpha = (1f - heartProgress).coerceIn(0f, 1f)
        val p = w * 0.038f
        val heartColor = ChibiRed.copy(alpha = alpha)
        drawRect(heartColor, Offset(hx - 2 * p, hy), Size(p, p))
        drawRect(heartColor, Offset(hx + p, hy), Size(p, p))
        drawRect(heartColor, Offset(hx - 3 * p, hy + p), Size(2 * p, p))
        drawRect(heartColor, Offset(hx, hy + p), Size(2 * p, p))
        drawRect(heartColor, Offset(hx - 2 * p, hy + 2 * p), Size(3 * p, p))
        drawRect(heartColor, Offset(hx - p, hy + 3 * p), Size(p, p))
    }

    if (state == ChibiState.CALL && isCallRinging && !reducedMotion) {
        // Subtle happy cheek dots + tight hand-tap ripple rings anchored strictly within ±6dp of hands
        drawCircle(ChibiRed.copy(alpha = 0.65f), radius = w * 0.025f, center = Offset(cx - eyeSpread * 1.25f, eyeY + eyeR * 1.6f))
        drawCircle(ChibiRed.copy(alpha = 0.65f), radius = w * 0.025f, center = Offset(cx + eyeSpread * 1.25f, eyeY + eyeR * 1.6f))

        val ripplePhase = ((beatRad / PI.toFloat()) % 1f).let { if (it < 0f) it + 1f else it }
        val maxRipplePx = 6.dp.toPx()
        val rippleRadius = (maxRipplePx * (0.35f + 0.65f * ripplePhase)).coerceAtMost(maxRipplePx)
        val rippleAlpha = (1f - ripplePhase) * 0.55f
        val leftHandCenter = computeCallHandTip(lShoulder, armLen, leftArmDeg, isLeft = true)
        val rightHandCenter = computeCallHandTip(rShoulder, armLen, rightArmDeg, isLeft = false)
        drawCircle(
            color = ChibiRed.copy(alpha = rippleAlpha),
            radius = rippleRadius,
            center = leftHandCenter,
            style = Stroke(width = strokeW * 0.65f),
        )
        drawCircle(
            color = ChibiRed.copy(alpha = rippleAlpha),
            radius = rippleRadius,
            center = rightHandCenter,
            style = Stroke(width = strokeW * 0.65f),
        )
    }
    if (ChibiMuted != Color.Transparent) { /* keeps ChibiMuted referenced cleanly if extended */ }
}

internal fun computeCallHandTip(
    shoulder: Offset,
    armLength: Float,
    armAngleDeg: Float,
    isLeft: Boolean,
): Offset {
    val rad = armAngleDeg * (PI.toFloat() / 180f)
    val baseX = if (isLeft) -armLength else armLength
    val dx = baseX * kotlin.math.cos(rad)
    val dy = baseX * kotlin.math.sin(rad)
    return Offset(shoulder.x + dx, shoulder.y + dy)
}
