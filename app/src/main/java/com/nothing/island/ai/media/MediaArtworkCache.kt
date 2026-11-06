package com.nothing.island.ai.media

import android.graphics.Bitmap
import android.util.LruCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

/**
 * High-performance, memory-efficient in-memory artwork cache and color extractor for Nothing Island.
 * Prevents redundant bitmap decoding and GC thrashing during media playback.
 */
object MediaArtworkCache {
    private const val MAX_CACHE_ENTRIES = 20
    private const val SAMPLE_DIMENSION = 24

    private val cache = object : LruCache<String, Bitmap>(MAX_CACHE_ENTRIES) {
        override fun sizeOf(key: String, value: Bitmap): Int {
            return 1
        }
    }

    private val colorCache = LruCache<String, Color>(MAX_CACHE_ENTRIES)

    /**
     * Retrieves cached artwork bitmap or null if not cached.
     */
    fun get(key: String): Bitmap? = synchronized(cache) {
        cache.get(key)
    }

    /**
     * Stores artwork bitmap in cache and extracts/caches its dominant adaptive color.
     */
    fun put(key: String, bitmap: Bitmap): Color {
        synchronized(cache) {
            cache.put(key, bitmap)
        }
        val dominant = extractDominantColor(bitmap)
        synchronized(colorCache) {
            colorCache.put(key, dominant)
        }
        return dominant
    }

    /**
     * Retrieves cached dominant color for the key.
     */
    fun getDominantColor(key: String): Color? = synchronized(colorCache) {
        colorCache.get(key)
    }

    /**
     * Computes a vibrant dominant color from a downsampled bitmap sample.
     * Prioritizes saturated, vivid colors for Nothing OS glowing aesthetics.
     */
    fun extractDominantColor(bitmap: Bitmap): Color {
        return try {
            val scaled = Bitmap.createScaledBitmap(bitmap, SAMPLE_DIMENSION, SAMPLE_DIMENSION, false)
            val pixels = IntArray(SAMPLE_DIMENSION * SAMPLE_DIMENSION)
            scaled.getPixels(pixels, 0, SAMPLE_DIMENSION, 0, 0, SAMPLE_DIMENSION, SAMPLE_DIMENSION)
            if (scaled != bitmap && !scaled.isRecycled) {
                scaled.recycle()
            }

            var bestScore = -1f
            var bestColor = Color(0xFFD71921L) // Fallback Nothing Red

            for (c in pixels) {
                val r = (c shr 16) and 0xFF
                val g = (c shr 8) and 0xFF
                val b = c and 0xFF

                // Convert RGB to HSV to check saturation and brightness
                val hsv = FloatArray(3)
                android.graphics.Color.RGBToHSV(r, g, b, hsv)
                val sat = hsv[1]
                val value = hsv[2]

                // Ignore pure blacks (< 0.15) and pure whites (> 0.95 with low sat)
                if (value < 0.15f || (value > 0.90f && sat < 0.15f)) continue

                // Score based on saturation and moderate-to-high brightness
                val score = sat * 1.5f + value
                if (score > bestScore) {
                    bestScore = score
                    bestColor = Color(r, g, b, 255)
                }
            }
            bestColor
        } catch (e: Exception) {
            Color(0xFFD71921L)
        }
    }

    /**
     * Clears all cached artwork and colors.
     */
    fun clear() {
        synchronized(cache) {
            cache.evictAll()
        }
        synchronized(colorCache) {
            colorCache.evictAll()
        }
    }
}
