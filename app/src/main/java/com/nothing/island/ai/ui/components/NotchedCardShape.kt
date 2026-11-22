package com.nothing.island.ai.ui.components

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.max

/**
 * Expanded card shape with a U-shaped notch carved out of the top center where
 * the punch-hole camera sits. The compact capsule "docks" into this notch when
 * the island expands, so the camera is never covered.
 *
 * The path is built manually (outer rounded rect + a carved pill at top center)
 * so it works on all Compose versions without Path.combine.
 */
fun notchedCardShape(
    notchWidthDp: Float = 132f,
    notchDepthDp: Float = 22f,
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
            val notchLeft = (w - nw) / 2f
            val notchRight = notchLeft + nw
            val nr = nd / 2f // rounded bottom of the notch

            val path = Path().apply {
                // Start at top-left after the corner radius
                moveTo(0f, r)
                // top-left corner arc
                quadraticBezierTo(0f, 0f, r, 0f)
                // top edge up to the notch
                lineTo(notchLeft, 0f)
                // dip down into the notch (rounded U)
                lineTo(notchLeft, nr)
                quadraticBezierTo(notchLeft, nd, notchLeft + nr, nd)
                lineTo(notchRight - nr, nd)
                quadraticBezierTo(notchRight, nd, notchRight, nr)
                lineTo(notchRight, 0f)
                // continue top edge to top-right corner
                lineTo(w - r, 0f)
                quadraticBezierTo(w, 0f, w, r)
                // right edge down
                lineTo(w, h - r)
                quadraticBezierTo(w, h, w - r, h)
                // bottom edge
                lineTo(r, h)
                quadraticBezierTo(0f, h, 0f, h - r)
                // left edge up
                close()
            }
            Outline.Generic(path)
        }
    }
}
