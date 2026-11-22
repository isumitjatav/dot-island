package com.nothing.island.ai.ui.components

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/**
 * Expanded card shape with an authentic, hardware-aligned U-shaped notch
 * smoothly carved out of the top center around the camera punch-hole lens.
 *
 * Contoured with smooth shoulder transitions and rounded bottom wrapping
 * the physical camera circle, matching Nothing OS hardware geometry and reference.
 */
fun notchedCardShape(
    notchWidthDp: Float = 52f,
    notchDepthDp: Float = 26f,
    cornerRadiusDp: Float = 26f,
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
            val nw = notchWidthDp.dp.toPx().coerceAtMost(w - 2f * r)
            val nd = notchDepthDp.dp.toPx()
            val cx = w / 2f
            val notchLeft = cx - nw / 2f
            val notchRight = cx + nw / 2f

            // Smooth shoulder and bottom radii
            val shoulderR = (10f).dp.toPx().coerceAtMost(nw / 4f)
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
