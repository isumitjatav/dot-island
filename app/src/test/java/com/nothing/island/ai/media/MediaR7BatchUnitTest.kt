package com.nothing.island.ai.media

import androidx.compose.ui.graphics.Color
import com.nothing.island.ai.switcher.IslandActivitySwitcher
import com.nothing.island.ai.switcher.IslandInterface
import com.nothing.island.ai.switcher.SwipeDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit test suite for Nothing Island R7 Media Player, Activity Switcher, and Music Style Studio models.
 */
class MediaR7BatchUnitTest {

    @Before
    fun setUp() {
        MediaArtworkCache.clear()
    }

    // =========================================================================
    // 1. MUSIC PREFERENCES & STYLE STUDIO TESTS
    // =========================================================================

    @Test
    fun testMusicPreferences_defaultValues_conformToSpecification() {
        val defaults = MusicPreferences.DEFAULTS
        assertEquals(MusicVisualizerStyle.VERTICAL_EQUALIZER, defaults.visualizerStyle)
        assertEquals(0xFFD71920L, defaults.accentColorHex) // Nothing Red default
        assertTrue(defaults.useAdaptiveAccent)
        assertEquals(1.0f, defaults.animationSpeed, 0.001f)
        assertEquals(1.0f, defaults.animationIntensity, 0.001f)
        assertTrue(defaults.albumRotationEnabled)
        assertEquals(ArtworkShape.CIRCLE, defaults.artworkShape)
        assertEquals(0.6f, defaults.glowIntensity, 0.001f)
        assertFalse(defaults.reducedMotion)
        assertEquals(4.5f, defaults.autoExpandDurationSeconds, 0.001f)
        assertTrue(defaults.isAutoExpandEnabled)
    }

    @Test
    fun testMusicPreferences_allVisualizerStylesAvailable() {
        val styles = MusicVisualizerStyle.values()
        assertEquals(5, styles.size)
        assertTrue(styles.contains(MusicVisualizerStyle.VERTICAL_EQUALIZER))
        assertTrue(styles.contains(MusicVisualizerStyle.WAVEFORM))
        assertTrue(styles.contains(MusicVisualizerStyle.CIRCULAR))
        assertTrue(styles.contains(MusicVisualizerStyle.VINYL))
        assertTrue(styles.contains(MusicVisualizerStyle.MINIMAL_PULSE))
    }

    @Test
    fun testMusicPreferences_artworkShapesAvailable() {
        val shapes = ArtworkShape.values()
        assertEquals(3, shapes.size)
        assertTrue(shapes.contains(ArtworkShape.CIRCLE))
        assertTrue(shapes.contains(ArtworkShape.SQUIRCLE))
        assertTrue(shapes.contains(ArtworkShape.ROUNDED_RECT))
    }

    @Test
    fun testMusicPreferences_resolveAccent_adaptiveVsStatic() {
        val adaptivePrefs = MusicPreferences.DEFAULTS.copy(
            useAdaptiveAccent = true,
            accentColorHex = 0xFFD71920L
        )
        val extractedColor = Color(0xFF00FF00) // Vibrant green from artwork
        val resolvedAdaptive = adaptivePrefs.resolveAccent(extractedColor)
        assertEquals(extractedColor, resolvedAdaptive)

        val staticPrefs = adaptivePrefs.copy(useAdaptiveAccent = false)
        val resolvedStatic = staticPrefs.resolveAccent(extractedColor)
        assertEquals(Color(0xFFD71920L), resolvedStatic)

        // When extracted color is null, fallback to configured hex accent
        val fallback = adaptivePrefs.resolveAccent(null)
        assertEquals(Color(0xFFD71920L), fallback)
    }

    // =========================================================================
    // 2. MEDIA TRACK STATE TESTS
    // =========================================================================

    @Test
    fun testMediaTrackState_computeCurrentPosition_honorsSpeedAndPause() {
        val baseTime = 1000000L
        val playingTrack = MediaTrackState(
            sessionId = "test_sess",
            packageName = "com.spotify.music",
            trackTitle = "Track 1",
            artistName = "Artist 1",
            isPlaying = true,
            durationMs = 200000L,
            currentPositionMs = 50000L,
            playbackSpeed = 1.0f,
            lastPositionUpdateTime = baseTime
        )

        // Playing at 1.0x after 5000ms
        val simulatedNow = baseTime + 5000L
        val calculated = playingTrack.computeCurrentPosition(nowMs = simulatedNow)
        assertEquals(55000L, calculated)

        // Paused track does NOT advance
        val pausedTrack = playingTrack.copy(isPlaying = false)
        val pausedPos = pausedTrack.computeCurrentPosition(nowMs = simulatedNow)
        assertEquals(50000L, pausedPos)

        // Coerces to duration boundary
        val pastEnd = playingTrack.computeCurrentPosition(nowMs = baseTime + 300000L)
        assertEquals(200000L, pastEnd)
    }

    @Test
    fun testMediaTrackState_progressFraction() {
        val track = MediaTrackState(
            sessionId = "s1",
            packageName = "pkg",
            trackTitle = "Song",
            artistName = "Band",
            durationMs = 100000L,
            currentPositionMs = 25000L
        )
        assertEquals(0.25f, track.progressFraction(), 0.001f)
        assertEquals(0.50f, track.copy(currentPositionMs = 50000L).progressFraction(), 0.001f)
        assertEquals(0.0f, track.copy(currentPositionMs = 0L).progressFraction(), 0.001f)
        assertEquals(1.0f, track.copy(currentPositionMs = 150000L).progressFraction(), 0.001f)

        // Zero duration returns 0
        val zeroDurationTrack = track.copy(durationMs = 0L)
        assertEquals(0f, zeroDurationTrack.progressFraction(), 0.001f)
    }

    @Test
    fun testMediaTrackState_isSameTrack() {
        val t1 = MediaTrackState(sessionId = "s1", packageName = "p1", trackTitle = "Song A", artistName = "Art 1")
        val t1Clone = MediaTrackState(sessionId = "s1", packageName = "p1", trackTitle = "Song A", artistName = "Art 1", currentPositionMs = 2000L)
        val t2 = MediaTrackState(sessionId = "s1", packageName = "p1", trackTitle = "Song B", artistName = "Art 1")

        assertTrue(t1.isSameTrack(t1Clone))
        assertFalse(t1.isSameTrack(t2))
        assertFalse(t1.isSameTrack(null))
    }

    // =========================================================================
    // 3. ACTIVITY SWITCHER NAVIGATION TESTS
    // =========================================================================

    @Test
    fun testActivitySwitcher_initialState_isDashboard() {
        val switcher = IslandActivitySwitcher()
        assertEquals(IslandInterface.DASHBOARD, switcher.currentInterface.value)
    }

    @Test
    fun testActivitySwitcher_navigateTo_updatesCurrentAndPushesBackStack() {
        val switcher = IslandActivitySwitcher()
        switcher.navigateTo(IslandInterface.MUSIC)
        assertEquals(IslandInterface.MUSIC, switcher.currentInterface.value)

        // Swipe right traverses backward to DASHBOARD
        val candidates = setOf(IslandInterface.DASHBOARD, IslandInterface.MUSIC)
        val handled = switcher.handleSwipe(SwipeDirection.RIGHT, candidates)
        assertTrue(handled)
        assertEquals(IslandInterface.DASHBOARD, switcher.currentInterface.value)
    }

    @Test
    fun testActivitySwitcher_swipeLeft_navigatesToForwardStackOrNextCandidate() {
        val switcher = IslandActivitySwitcher()
        val candidates = setOf(IslandInterface.DASHBOARD, IslandInterface.MUSIC)

        // From Dashboard with Music candidate available, swipe LEFT advances to Music
        val handled = switcher.handleSwipe(SwipeDirection.LEFT, candidates)
        assertTrue(handled)
        assertEquals(IslandInterface.MUSIC, switcher.currentInterface.value)

        // Swipe RIGHT goes back to Dashboard
        switcher.handleSwipe(SwipeDirection.RIGHT, candidates)
        assertEquals(IslandInterface.DASHBOARD, switcher.currentInterface.value)

        // Swipe LEFT now uses the forward stack to return to Music
        switcher.handleSwipe(SwipeDirection.LEFT, candidates)
        assertEquals(IslandInterface.MUSIC, switcher.currentInterface.value)
    }

    @Test
    fun testActivitySwitcher_pruningRemovesEndedActivities() {
        val switcher = IslandActivitySwitcher()
        switcher.navigateTo(IslandInterface.MUSIC)
        switcher.navigateTo(IslandInterface.NOTIFICATION_FOCUSED)
        assertEquals(IslandInterface.NOTIFICATION_FOCUSED, switcher.currentInterface.value)

        // Now Music ends (e.g. playback stopped, app killed), so candidates only include DASHBOARD and NOTIFICATION
        val reducedCandidates = setOf(IslandInterface.DASHBOARD, IslandInterface.NOTIFICATION_FOCUSED)

        // Swiping RIGHT should bypass the pruned MUSIC entry and go directly to DASHBOARD
        val handled = switcher.handleSwipe(SwipeDirection.RIGHT, reducedCandidates)
        assertTrue(handled)
        assertEquals(IslandInterface.DASHBOARD, switcher.currentInterface.value)
    }

    @Test
    fun testActivitySwitcher_interruptPreservation_restoresInterface() {
        val switcher = IslandActivitySwitcher()
        switcher.navigateTo(IslandInterface.MUSIC)
        assertEquals(IslandInterface.MUSIC, switcher.currentInterface.value)

        // Incoming call or notification interrupts
        switcher.onInterruptStarted()
        switcher.navigateTo(IslandInterface.NOTIFICATION_FOCUSED)
        assertEquals(IslandInterface.NOTIFICATION_FOCUSED, switcher.currentInterface.value)

        // Interruption finishes while Music candidate is still active
        val candidates = setOf(IslandInterface.DASHBOARD, IslandInterface.MUSIC)
        switcher.onInterruptEnded(candidates)
        assertEquals(IslandInterface.MUSIC, switcher.currentInterface.value)
    }

    @Test
    fun testActivitySwitcher_interruptPreservation_fallsBackIfPreservedEnded() {
        val switcher = IslandActivitySwitcher()
        switcher.navigateTo(IslandInterface.MUSIC)

        switcher.onInterruptStarted()
        switcher.navigateTo(IslandInterface.NOTIFICATION_FOCUSED)

        // During the interruption, media was killed, so candidates only have DASHBOARD
        val remainingCandidates = setOf(IslandInterface.DASHBOARD)
        switcher.onInterruptEnded(remainingCandidates)
        assertEquals(IslandInterface.DASHBOARD, switcher.currentInterface.value)
    }

    @Test
    fun testActivitySwitcher_swipeRight_fallback_whenBackStackEmpty() {
        val switcher = IslandActivitySwitcher()
        assertEquals(IslandInterface.DASHBOARD, switcher.currentInterface.value)

        // With candidates DASHBOARD and MUSIC, swiping RIGHT when backStack is empty falls back to MUSIC
        val candidates = setOf(IslandInterface.DASHBOARD, IslandInterface.MUSIC)
        val handled = switcher.handleSwipe(SwipeDirection.RIGHT, candidates)
        assertTrue(handled)
        assertEquals(IslandInterface.MUSIC, switcher.currentInterface.value)
    }

    @Test
    fun testActivitySwitcher_syncCurrentInterface() {
        val switcher = IslandActivitySwitcher()
        assertEquals(IslandInterface.DASHBOARD, switcher.currentInterface.value)

        // External tap expands Music
        switcher.syncCurrentInterface(IslandInterface.MUSIC)
        assertEquals(IslandInterface.MUSIC, switcher.currentInterface.value)

        // Now swiping RIGHT traverses backStack to DASHBOARD
        val candidates = setOf(IslandInterface.DASHBOARD, IslandInterface.MUSIC)
        val handled = switcher.handleSwipe(SwipeDirection.RIGHT, candidates)
        assertTrue(handled)
        assertEquals(IslandInterface.DASHBOARD, switcher.currentInterface.value)
    }
}
