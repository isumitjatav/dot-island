package com.nothing.island.ai.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

data class ExpandedNotchConfig(
    val notchWidthDp: Float = 38f,
    val notchDepthDp: Float = 28f,
    val physicalCameraCenterXPx: Float? = null,
    val expandedCardLeftXPx: Float? = null,
    val expandedOffsetXDp: Float = 0f,
)

val LocalExpandedNotchConfig = compositionLocalOf<ExpandedNotchConfig?> { null }

/**
 * Computes the U-notch X center inside an expanded card strictly from the physical camera center
 * relative to the expanded card's left edge, plus the calibrated X offset, clamped away from corners.
 */
fun computeNotchCenterXPx(
    physicalCameraCenterX: Float,
    expandedCardLeftX: Float,
    expandedOffsetXPx: Float,
    cardWidthPx: Float,
    notchWidthPx: Float,
    cornerRadiusPx: Float,
): Float {
    val minCenter = cornerRadiusPx + (notchWidthPx / 2f)
    val maxCenter = (cardWidthPx - cornerRadiusPx - (notchWidthPx / 2f)).coerceAtLeast(minCenter)
    val raw = (physicalCameraCenterX - expandedCardLeftX) + expandedOffsetXPx
    return raw.coerceIn(minCenter, maxCenter)
}

@Composable
fun currentNotchedCardShape(
    defaultNotchWidthDp: Float = 38f,
    defaultNotchDepthDp: Float = 28f,
    cornerRadiusDp: Float = 28f,
): Shape {
    val cfg = LocalExpandedNotchConfig.current
    return if (cfg != null) {
        notchedCardShape(
            notchWidthDp = cfg.notchWidthDp,
            notchDepthDp = cfg.notchDepthDp,
            cornerRadiusDp = cornerRadiusDp,
            physicalCameraCenterXPx = cfg.physicalCameraCenterXPx,
            expandedCardLeftXPx = cfg.expandedCardLeftXPx,
            expandedOffsetXDp = cfg.expandedOffsetXDp,
        )
    } else {
        notchedCardShape(
            notchWidthDp = defaultNotchWidthDp,
            notchDepthDp = defaultNotchDepthDp,
            cornerRadiusDp = cornerRadiusDp,
        )
    }
}

/**
 * Expanded card shape with an authentic, hardware-aligned U-shaped notch
 * smoothly carved out of the top edge around the camera punch-hole lens.
 */
fun notchedCardShape(
    notchWidthDp: Float = 38f,
    notchDepthDp: Float = 28f,
    cornerRadiusDp: Float = 28f,
    physicalCameraCenterXPx: Float? = null,
    expandedCardLeftXPx: Float? = null,
    expandedOffsetXDp: Float = 0f,
): Shape = object : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val w = size.width
        val h = size.height
        return with(density) {
            val r = cornerRadiusDp.dp.toPx().coerceAtMost(minOf(w, h) / 2f)
            val nw = notchWidthDp.dp.toPx().coerceAtMost((w - 2f * r).coerceAtLeast(8f))
            val nd = notchDepthDp.dp.toPx().coerceAtMost(h * 0.6f)
            val offsetXPx = expandedOffsetXDp.dp.toPx()
            val cx = if (physicalCameraCenterXPx != null && expandedCardLeftXPx != null) {
                computeNotchCenterXPx(
                    physicalCameraCenterX = physicalCameraCenterXPx,
                    expandedCardLeftX = expandedCardLeftXPx,
                    expandedOffsetXPx = offsetXPx,
                    cardWidthPx = w,
                    notchWidthPx = nw,
                    cornerRadiusPx = r,
                )
            } else {
                val minCenter = r + nw / 2f
                val maxCenter = (w - r - nw / 2f).coerceAtLeast(minCenter)
                (w / 2f + offsetXPx).coerceIn(minCenter, maxCenter)
            }
            val notchLeft = cx - nw / 2f
            val notchRight = cx + nw / 2f

            // Smooth shoulder and bottom radii
            val shoulderR = (6f).dp.toPx().coerceAtMost(nw / 4f)
            val bottomSpan = nw - 2f * shoulderR
            val bottomR = (bottomSpan / 2f).coerceAtMost(nd - shoulderR)

            val path = Path().apply {
                // Start at top-left after the corner radius
                moveTo(0f, r)
                // Top-left corner arc
                quadraticTo(0f, 0f, r, 0f)
                // Top edge up to the notch
                lineTo(notchLeft, 0f)

                // Left shoulder: smoothly curve down from top horizontal line
                quadraticTo(
                    notchLeft + shoulderR, 0f,
                    notchLeft + shoulderR, shoulderR
                )

                // Left wall straight run down to bottom arc if needed
                if (nd > shoulderR + bottomR) {
                    lineTo(notchLeft + shoulderR, nd - bottomR)
                }

                // Bottom rounded arc hugging the camera punch-hole
                quadraticTo(
                    notchLeft + shoulderR, nd,
                    cx, nd
                )
                quadraticTo(
                    notchRight - shoulderR, nd,
                    notchRight - shoulderR, if (nd > shoulderR + bottomR) nd - bottomR else shoulderR
                )

                // Right shoulder: straight run up then curve smoothly to top horizontal line
                if (nd > shoulderR + bottomR) {
                    lineTo(notchRight - shoulderR, shoulderR)
                }
                quadraticTo(
                    notchRight - shoulderR, 0f,
                    notchRight, 0f
                )

                // Continue top edge to top-right corner
                lineTo(w - r, 0f)
                quadraticTo(w, 0f, w, r)
                // Right edge down
                lineTo(w, h - r)
                quadraticTo(w, h, w - r, h)
                // Bottom edge
                lineTo(r, h)
                quadraticTo(0f, h, 0f, h - r)
                // Left edge up
                close()
            }
            Outline.Generic(path)
        }
    }
}
