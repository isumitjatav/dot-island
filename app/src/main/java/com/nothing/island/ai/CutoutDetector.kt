package com.nothing.island.ai

import android.content.Context
import android.graphics.Rect
import android.os.Build
import android.view.DisplayCutout
import android.view.WindowManager

/**
 * Geometric description of the display cutout and camera punch-hole.
 */
data class CutoutGeometry(
    val hasCutout: Boolean,
    val boundingRect: Rect,
    val safeInsetTopPx: Int,
    val centerXpx: Float,
    val centerYpx: Float,
    val widthPx: Float,
    val heightPx: Float,
    val density: Float,
    val source: String
) {
    val safeInsetTopDp: Float get() = safeInsetTopPx / density
    val centerXDp: Float get() = centerXpx / density
    val centerYDp: Float get() = centerYpx / density
    val widthDp: Float get() = widthPx / density
    val heightDp: Float get() = heightPx / density
}

/**
 * CutoutDetector
 *
 * Uses WindowInsets and DisplayCutout APIs when available, and provides hardware-verified
 * calibration geometry for the Nothing Phone (4a) (model A069 / FroggerIND).
 */
object CutoutDetector {

    // Verified hardware measurements for Nothing Phone (4a) A069 (1224 x 2720 @ 480dpi, density=3.0)
    const val A069_DISPLAY_WIDTH_PX = 1224
    const val A069_DISPLAY_HEIGHT_PX = 2720
    const val A069_CUTOUT_LEFT_PX = 559
    const val A069_CUTOUT_RIGHT_PX = 665
    const val A069_CUTOUT_SAFE_INSET_TOP_PX = 162 // 54 dp
    const val A069_CAMERA_CENTER_X_PX = 612.0f   // 204 dp
    const val A069_CAMERA_CENTER_Y_PX = 81.6f    // 27.2 dp
    const val A069_CUTOUT_WIDTH_PX = 106.0f      // 35.33 dp

    // Configurable calibration defaults in DP
    const val DEFAULT_TOP_MARGIN_DP = 9.0f              // Vertical offset for compact capsule (27px on 3.0 density)
    const val DEFAULT_EXPANDED_TOP_MARGIN_DP = 0.0f     // Vertical offset for expanded card (docks at top glass)
    const val DEFAULT_COMPACT_WIDTH_DP = 124.0f         // Width of compact pill (372px)
    const val DEFAULT_COMPACT_VISIBLE_HEIGHT_DP = 36.0f // True visual height of capsule (108px)
    const val DEFAULT_COMPACT_CORNER_RADIUS_DP = 18.0f  // Half of height = perfect capsule silhouette (54px)
    const val DEFAULT_COMPACT_TOUCH_HEIGHT_DP = 58.0f   // WindowManager touch bridge height (174px, crosses 162px status bar)
    const val DEFAULT_COMPACT_HEIGHT_DP = 58.0f         // Backward-compatible touch window height
    const val DEFAULT_EXPANDED_WIDTH_DP = 390.0f
    const val DEFAULT_EXPANDED_HEIGHT_DP = 210.0f

    /**
     * Detects cutout geometry from system WindowMetrics or hardware baseline.
     */
    fun detectCutout(context: Context): CutoutGeometry {
        val density = context.resources.displayMetrics.density

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val wm = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
            val metrics = wm?.currentWindowMetrics
            val insets = metrics?.windowInsets
            val cutout: DisplayCutout? = insets?.displayCutout

            if (cutout != null && cutout.boundingRects.isNotEmpty()) {
                val rect = cutout.boundingRects.firstOrNull { it.top == 0 } ?: cutout.boundingRects.first()
                val safeTop = cutout.safeInsetTop

                val centerX = rect.exactCenterX()
                val centerY = if (rect.height() > 0) rect.exactCenterY() else safeTop / 2.0f
                val width = rect.width().toFloat()
                val height = rect.height().toFloat()

                return CutoutGeometry(
                    hasCutout = true,
                    boundingRect = rect,
                    safeInsetTopPx = safeTop,
                    centerXpx = centerX,
                    centerYpx = centerY,
                    widthPx = width,
                    heightPx = height,
                    density = density,
                    source = "DisplayCutout API"
                )
            }
        }

        // Fallback: Hardware-calibrated profile for Nothing Phone (4a) A069
        return CutoutGeometry(
            hasCutout = true,
            boundingRect = Rect(
                A069_CUTOUT_LEFT_PX,
                0,
                A069_CUTOUT_RIGHT_PX,
                A069_CUTOUT_SAFE_INSET_TOP_PX
            ),
            safeInsetTopPx = A069_CUTOUT_SAFE_INSET_TOP_PX,
            centerXpx = A069_CAMERA_CENTER_X_PX,
            centerYpx = A069_CAMERA_CENTER_Y_PX,
            widthPx = A069_CUTOUT_WIDTH_PX,
            heightPx = A069_CUTOUT_SAFE_INSET_TOP_PX.toFloat(),
            density = if (density > 0f) density else 3.0f,
            source = "Nothing Phone (4a) Hardware Calibration"
        )
    }

    /**
     * Converts DP value to physical pixels using display density.
     */
    fun dpToPx(dp: Float, context: Context): Int {
        val density = context.resources.displayMetrics.density
        return (dp * density + 0.5f).toInt()
    }
}
