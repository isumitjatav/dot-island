package com.nothing.island.ai

import android.media.session.PlaybackState
import com.nothing.island.ai.engine.IslandEvent
import com.nothing.island.ai.engine.IslandMasterState
import com.nothing.island.ai.engine.IslandStateRepository
import com.nothing.island.ai.media.MediaTrackState
import com.nothing.island.ai.media.ResumableMediaCache
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests covering:
 * 1. EarbudsConnected keeps IslandMode.COLLAPSED (never opens an expanded blank container).
 * 2. CallState.RINGING blocks auto-collapse timers while ExpandedViewType.CALL is open.
 * 3. ResumableMediaCache persists across STATE_PAUSED / STATE_STOPPED and clears on onSessionDestroyed().
 */
class OverlayRegressionAndResumeUnitTest {

    @Test
    fun earbudsConnected_neverEntersOrRemainsInExpandedMode() {
        val repo = IslandStateRepository(
            IslandMasterState(
                mode = IslandMode.EXPANDED,
                expandedViewType = ExpandedViewType.DASHBOARD,
                isAutoExpanded = true,
            )
        )

        val earbudEvent = IslandEvent.BluetoothDeviceConnected(
            id = "bt_1",
            deviceName = "Nothing Ear (a)",
            deviceAddress = "AA:BB:CC:DD:EE:01",
            batteryLevelPercentage = 85,
        )
        repo.updateFromResolvedEvent(
            event = earbudEvent,
            priority = earbudEvent.priority,
            presentationId = "pres_bt_1",
        )

        assertEquals(IslandMode.COLLAPSED, repo.state.value.mode)
        assertFalse(repo.state.value.isAutoExpanded)

        // Tapping toggleMode while the earbud pill is active must keep mode COLLAPSED
        repo.toggleMode()
        assertEquals(IslandMode.COLLAPSED, repo.state.value.mode)
    }

    @Test
    fun ringingCallCard_suppressesAutoCollapseTimers() {
        val ringingExpanded = IslandMasterState(
            mode = IslandMode.EXPANDED,
            expandedViewType = ExpandedViewType.CALL,
            activeEvent = IslandEvent.IncomingCallRinging(
                id = "call_1",
                callId = "call_1",
                callerDisplayName = "Alex Rivera",
            ),
        )
        assertTrue(IslandOverlayController.shouldBlockAutoCollapse(ringingExpanded))

        val musicExpanded = ringingExpanded.copy(
            expandedViewType = ExpandedViewType.MUSIC,
            activeEvent = null,
        )
        assertFalse(IslandOverlayController.shouldBlockAutoCollapse(musicExpanded))
    }

    @Test
    fun resumableMediaCache_persistsAcrossPausedAndStopped_clearsOnSessionDestroyed() {
        val cache = ResumableMediaCache()
        assertNull(cache.state.value)

        val playing = MediaTrackState(
            sessionId = "s_metrolist",
            packageName = "com.metrolist.music",
            appDisplayName = "Metrolist",
            trackTitle = "Midnight City",
            artistName = "M83",
            isPlaying = true,
            playbackState = PlaybackState.STATE_PLAYING,
            durationMs = 243_000L,
            currentPositionMs = 45_000L,
        )
        cache.onTrackObserved(playing)

        // Transitioning to PAUSED or STOPPED keeps metadata cached for swipe-to-resume
        cache.onPlaybackPausedOrStopped(PlaybackState.STATE_PAUSED)
        val pausedCached = cache.state.value
        assertNotNull(pausedCached)
        assertFalse(pausedCached!!.isPlaying)
        assertTrue(pausedCached.isHistorical)
        assertEquals("Midnight City", pausedCached.trackTitle)
        assertEquals("com.metrolist.music", pausedCached.packageName)

        cache.onPlaybackPausedOrStopped(PlaybackState.STATE_STOPPED)
        assertNotNull(cache.state.value)

        // Swiping app from Recents destroys session -> cache clears immediately
        cache.onSessionDestroyed()
        assertNull(cache.state.value)
    }
}
