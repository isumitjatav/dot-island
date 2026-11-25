package com.nothing.island.ai.media

import android.app.PendingIntent
import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color

/**
 * Immutable snapshot representing active or last-known media playback state.
 */
data class MediaTrackState(
    val sessionId: String = "",
    val packageName: String = "",
    val appDisplayName: String = "MUSIC",
    val trackTitle: String = "No Title",
    val artistName: String = "Unknown Artist",
    val albumName: String = "",
    val artworkBitmap: Bitmap? = null,
    val artworkKey: String? = null,
    val dominantColor: Color? = null,
    val isPlaying: Boolean = false,
    val playbackState: Int = 0, // android.media.session.PlaybackState.STATE_*
    val durationMs: Long = 0L,
    val currentPositionMs: Long = 0L,
    val playbackSpeed: Float = 1.0f,
    val lastPositionUpdateTime: Long = System.currentTimeMillis(),
    val canSeek: Boolean = true,
    val canSkipNext: Boolean = true,
    val canSkipPrevious: Boolean = true,
    val audioOutputName: String = "Phone Speaker",
    val isHistorical: Boolean = false,
    val sessionActivity: PendingIntent? = null,
    val repeatMode: Int = 0 // 0 = REPEAT_MODE_NONE, 1 = REPEAT_MODE_ONE, 2 = REPEAT_MODE_ALL
) {
    companion object {
        val EMPTY = MediaTrackState(
            sessionId = "",
            packageName = "",
            appDisplayName = "NOTHING PLAYING",
            trackTitle = "Nothing Playing",
            artistName = "Audio Standby",
            isPlaying = false,
            isHistorical = true
        )
    }

    /**
     * Calculates estimated real-time playback position taking into account
     * playback speed and elapsed wall-clock time since last position update.
     */
    fun computeCurrentPosition(nowMs: Long = System.currentTimeMillis()): Long {
        if (!isPlaying || durationMs <= 0L) {
            return currentPositionMs.coerceIn(0L, if (durationMs > 0L) durationMs else Long.MAX_VALUE)
        }
        val elapsed = (nowMs - lastPositionUpdateTime).coerceAtLeast(0L)
        val advanced = (elapsed * playbackSpeed).toLong()
        val estimated = currentPositionMs + advanced
        return estimated.coerceIn(0L, durationMs)
    }

    /**
     * Returns the current playback progress as a normalized 0.0f..1.0f fraction.
     */
    fun progressFraction(nowMs: Long = System.currentTimeMillis()): Float {
        if (durationMs <= 0L) return 0f
        return (computeCurrentPosition(nowMs).toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    }

    /**
     * Checks if semantic track identity matches another state (title, artist, album, package).
     */
    fun isSameTrack(other: MediaTrackState?): Boolean {
        if (other == null) return false
        return packageName == other.packageName &&
                trackTitle == other.trackTitle &&
                artistName == other.artistName &&
                albumName == other.albumName
    }
}
