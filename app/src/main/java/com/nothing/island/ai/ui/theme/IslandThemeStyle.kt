package com.nothing.island.ai.ui.theme

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The 5 Signature Themes inspired by authentic Nothing OS hardware & industrial design:
 * 1. Nothing Classic: Pitch-black, crisp white monospace/dot-matrix, Nothing Red indicators, solid white action pills.
 * 2. Glyph Arc: Pitch-black, glowing white curved arcs on left & right pill edges, segmented white/dark-gray LED progress bars.
 * 3. Tape Deck: Matte charcoal (#1E1E1E), dual cassette reels, Nothing Red mini waveform, cassette-housed album art.
 * 4. Glassmorphic Frosted: Translucent dark frosted container (#222222 at 82% alpha) with specular white border and glass pill buttons.
 * 5. High-Contrast Inverted: Crisp off-white (#F2F2F2) surface, pure #000000 text/icons, Nothing Red alert highlights, solid black action pills.
 */
enum class IslandSignatureTheme(
    val id: String,
    val displayName: String,
    val description: String,
    val pillBackgroundColorHex: Long,
    val accentColorHex: Long,
    val pillBackgroundColor: Color,
    val accentColor: Color,
    val surfaceColor: Color,
    val textColorPrimary: Color,
    val textColorSecondary: Color,
    val borderColor: Color,
    val primaryButtonBackground: Color,
    val primaryButtonTextColor: Color,
    val isGlassmorphic: Boolean = false,
    val isInverted: Boolean = false
) {
    NOTHING_CLASSIC(
        id = "nothing_classic",
        displayName = "Nothing Classic",
        description = "Pitch-black canvas with white dot-matrix text and Nothing Red accents",
        pillBackgroundColorHex = 0xFF000000L,
        accentColorHex = 0xFFD71921L,
        pillBackgroundColor = Color(0xFF000000),
        accentColor = Color(0xFFD71921),
        surfaceColor = Color(0xFF141414),
        textColorPrimary = Color(0xFFFFFFFF),
        textColorSecondary = Color(0xFF9E9E9E),
        borderColor = Color(0xFF262626),
        primaryButtonBackground = Color(0xFFF2F2F2),
        primaryButtonTextColor = Color(0xFF000000)
    ),

    GLYPH_ARC(
        id = "glyph_arc",
        displayName = "Glyph Arc",
        description = "Illuminated white dual arcs hugging edges with segmented LED meters",
        pillBackgroundColorHex = 0xFF000000L,
        accentColorHex = 0xFFFFFFFFL,
        pillBackgroundColor = Color(0xFF000000),
        accentColor = Color(0xFFFFFFFF),
        surfaceColor = Color(0xFF141414),
        textColorPrimary = Color(0xFFFFFFFF),
        textColorSecondary = Color(0xFF888888),
        borderColor = Color(0xFF333333),
        primaryButtonBackground = Color(0xFF1C1C1C),
        primaryButtonTextColor = Color(0xFFFFFFFF)
    ),

    TAPE_DECK(
        id = "tape_deck",
        displayName = "Tape Deck",
        description = "Matte charcoal finish, dual rotating cassette reels and red waveform",
        pillBackgroundColorHex = 0xFF1E1E1EL,
        accentColorHex = 0xFFD71921L,
        pillBackgroundColor = Color(0xFF1E1E1E),
        accentColor = Color(0xFFD71921),
        surfaceColor = Color(0xFF262626),
        textColorPrimary = Color(0xFFFFFFFF),
        textColorSecondary = Color(0xFF9E9E9E),
        borderColor = Color(0xFF383838),
        primaryButtonBackground = Color(0xFFE5E5E5),
        primaryButtonTextColor = Color(0xFF1E1E1E)
    ),

    GLASSMORPHIC_FROSTED(
        id = "glassmorphic_frosted",
        displayName = "Glassmorphic Frosted",
        description = "Translucent frosted acrylic with specular highlight border and glass pills",
        pillBackgroundColorHex = 0xD1222222L,
        accentColorHex = 0xFFFFFFFFL,
        pillBackgroundColor = Color(0xD1222222), // 82% alpha #222222
        accentColor = Color(0xFFFFFFFF),
        surfaceColor = Color(0xB3333333), // 70% alpha #333333
        textColorPrimary = Color(0xFFFFFFFF),
        textColorSecondary = Color(0xCCFFFFFF),
        borderColor = Color(0x38FFFFFF), // specular white
        primaryButtonBackground = Color(0xB3333333),
        primaryButtonTextColor = Color(0xFFFFFFFF),
        isGlassmorphic = true
    ),

    HIGH_CONTRAST_INVERTED(
        id = "high_contrast_inverted",
        displayName = "High-Contrast Inverted",
        description = "Crisp off-white housing with black dot-matrix typography and red badge",
        pillBackgroundColorHex = 0xFFF2F2F2L,
        accentColorHex = 0xFFD71921L,
        pillBackgroundColor = Color(0xFFF2F2F2),
        accentColor = Color(0xFFD71921),
        surfaceColor = Color(0xFFE5E5E5),
        textColorPrimary = Color(0xFF000000),
        textColorSecondary = Color(0xFF555555),
        borderColor = Color(0xFFCCCCCC),
        primaryButtonBackground = Color(0xFF000000),
        primaryButtonTextColor = Color(0xFFFFFFFF),
        isInverted = true
    );

    companion object {
        fun resolve(presetName: String): IslandSignatureTheme {
            val normalized = presetName.trim()
            return when {
                normalized.equals("Glyph Arc", ignoreCase = true) -> GLYPH_ARC
                normalized.equals("Tape Deck", ignoreCase = true) -> TAPE_DECK
                normalized.equals("Glassmorphic Frosted", ignoreCase = true) -> GLASSMORPHIC_FROSTED
                normalized.equals("High-Contrast Inverted", ignoreCase = true) -> HIGH_CONTRAST_INVERTED
                normalized.equals("Nothing Classic", ignoreCase = true) -> NOTHING_CLASSIC
                normalized.equals("Classic Black", ignoreCase = true) -> NOTHING_CLASSIC
                normalized.equals("Nothing Minimal", ignoreCase = true) -> NOTHING_CLASSIC
                else -> NOTHING_CLASSIC
            }
        }
    }
}

/**
 * CompositionLocal delivering the active signature theme down the Compose tree.
 */
val LocalIslandTheme = staticCompositionLocalOf { IslandSignatureTheme.NOTHING_CLASSIC }

// =============================================================================
// NATIVE COMPOSE DECORATIONS FOR SIGNATURE THEMES
// =============================================================================

/**
 * Glyph Arc: Draws glowing white curved arcs on the left and right semicircles
 * of the compact pill container without using static bitmaps.
 */
@Composable
fun GlyphArcCompactDecorations(
    modifier: Modifier = Modifier,
    arcColor: Color = Color.White
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val h = size.height
        val w = size.width
        if (h <= 0f || w <= 0f || w < h || h.isNaN() || w.isNaN()) return@Canvas

        val strokeWidthPx = 2.4.dp.toPx()
        val glowWidthPx = 5.0.dp.toPx()
        val arcSize = (h - strokeWidthPx - 3.dp.toPx()).coerceAtLeast(1f)

        // Left Arc (hugging left edge)
        val leftRect = androidx.compose.ui.geometry.Rect(
            offset = Offset(strokeWidthPx / 2f + 1.5.dp.toPx(), strokeWidthPx / 2f + 1.5.dp.toPx()),
            size = Size(arcSize, arcSize)
        )

        // Right Arc (hugging right edge)
        val rightRect = androidx.compose.ui.geometry.Rect(
            offset = Offset((w - h + strokeWidthPx / 2f + 1.5.dp.toPx()).coerceAtLeast(0f), strokeWidthPx / 2f + 1.5.dp.toPx()),
            size = Size(arcSize, arcSize)
        )

        // 1. Soft Glow underlayers
        drawArc(
            color = arcColor.copy(alpha = 0.35f),
            startAngle = 110f,
            sweepAngle = 140f,
            useCenter = false,
            topLeft = leftRect.topLeft,
            size = leftRect.size,
            style = Stroke(width = glowWidthPx, cap = StrokeCap.Round)
        )
        drawArc(
            color = arcColor.copy(alpha = 0.35f),
            startAngle = -70f,
            sweepAngle = 140f,
            useCenter = false,
            topLeft = rightRect.topLeft,
            size = rightRect.size,
            style = Stroke(width = glowWidthPx, cap = StrokeCap.Round)
        )

        // 2. Crisp Laser White arcs
        drawArc(
            color = arcColor.copy(alpha = 0.95f),
            startAngle = 115f,
            sweepAngle = 130f,
            useCenter = false,
            topLeft = leftRect.topLeft,
            size = leftRect.size,
            style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
        )
        drawArc(
            color = arcColor.copy(alpha = 0.95f),
            startAngle = -65f,
            sweepAngle = 130f,
            useCenter = false,
            topLeft = rightRect.topLeft,
            size = rightRect.size,
            style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
        )
    }
}

/**
 * Tape Deck: Dual cassette reel indicator on compact pill.
 * Gently rotates when isPlaying is true with zero divide-by-zero risk.
 */
@Composable
fun TapeDeckReelsIndicator(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    sizeDp: Dp = 14.dp,
    reelColor: Color = Color(0xFFDCDCDC)
) {
    val infiniteTransition = rememberInfiniteTransition(label = "tape_reels")
    val rawRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "reel_spin"
    )
    val rotation = if (isPlaying) rawRotation else 0f

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SingleCassetteReel(rotation = if (isPlaying) rotation else 0f, sizeDp = sizeDp, reelColor = reelColor)
        SingleCassetteReel(rotation = if (isPlaying) rotation else 0f, sizeDp = sizeDp, reelColor = reelColor)
    }
}

@Composable
private fun SingleCassetteReel(
    rotation: Float,
    sizeDp: Dp,
    reelColor: Color
) {
    Box(
        modifier = Modifier
            .size(sizeDp)
            .clip(CircleShape)
            .background(Color(0xFF2B2B2B))
            .border(1.dp, Color(0xFF4A4A4A), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val spokeRadius = size.width * 0.28f
            val dotRadius = size.width * 0.08f

            // 3 circular reel holes
            for (i in 0 until 3) {
                val angleDeg = rotation + (i * 120f)
                val angleRad = Math.toRadians(angleDeg.toDouble())
                val dotCenter = Offset(
                    x = center.x + (spokeRadius * Math.cos(angleRad)).toFloat(),
                    y = center.y + (spokeRadius * Math.sin(angleRad)).toFloat()
                )
                drawCircle(
                    color = reelColor,
                    radius = dotRadius,
                    center = dotCenter
                )
            }
            // Center pin
            drawCircle(
                color = Color.Black,
                radius = dotRadius * 0.75f,
                center = center
            )
        }
    }
}

/**
 * Tape Deck: Mini waveform pulse on the right side of the compact pill.
 */
@Composable
fun TapeDeckWaveform(
    modifier: Modifier = Modifier,
    barCount: Int = 4,
    color: Color = Color(0xFFD71921)
) {
    val infiniteTransition = rememberInfiniteTransition(label = "tape_waveform")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "waveform_phase"
    )

    Row(
        modifier = modifier.height(12.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until barCount) {
            val factor = ((i + 1) * 0.25f + phase).let { v -> if (v > 1f) 2f - v else v }
            val barHeight = (4f + factor * 8f).dp
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(barHeight)
                    .clip(RoundedCornerShape(1.dp))
                    .background(color)
            )
        }
    }
}

/**
 * Glyph Arc: Segmented LED Progress Bar for music, battery, and sliders in expanded mode.
 */
@Composable
fun SegmentedLedProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    totalSegments: Int = 18,
    activeColor: Color = Color.White,
    inactiveColor: Color = Color(0xFF282828)
) {
    val safeProgress = if (progress.isNaN() || progress.isInfinite()) 0f else progress.coerceIn(0f, 1f)
    val safeSegments = totalSegments.coerceAtLeast(1)
    val activeCount = (safeProgress * safeSegments).toInt().coerceIn(0, safeSegments)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(10.dp),
        horizontalArrangement = Arrangement.spacedBy(2.5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until totalSegments) {
            val isActive = i < activeCount
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (isActive) activeColor else inactiveColor)
                    .then(
                        if (isActive) Modifier.border(0.5.dp, activeColor.copy(alpha = 0.5f), RoundedCornerShape(2.dp))
                        else Modifier
                    )
            )
        }
    }
}
