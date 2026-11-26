package com.nothing.island.ai.ui

import com.nothing.island.ai.ui.components.MascotReactionState
import com.nothing.island.ai.ui.components.MascotSprites
import com.nothing.island.ai.ui.components.mascotByName
import com.nothing.island.ai.ui.components.resolveMascotReactionState
import com.nothing.island.ai.ui.components.selectMascotFrame
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verifies Dot Island's upgraded mascot registry, legacy alias mapping,
 * reactive state precedence, and multi-frame sprite selection.
 */
class MascotReactionUnitTest {

    @Test
    fun testFlagshipSpritesAndLegacyAliasResolution() {
        assertEquals(MascotSprites.DOTCAT, mascotByName("CAT"))
        assertEquals(MascotSprites.DOTCAT, mascotByName("DOTCAT"))
        assertEquals(MascotSprites.DOTCAT, mascotByName("DotCat"))
        assertEquals(MascotSprites.GLYPH_ORB, mascotByName("GLYPH_ORB"))
        assertEquals(MascotSprites.TAPE_BUDDY, mascotByName("TAPE_BUDDY"))
        assertEquals(MascotSprites.DOTCAT, mascotByName("UNKNOWN_SPRITE"))
        assertTrue(MascotSprites.ALL.indexOf(MascotSprites.DOTCAT) == 0)
        assertTrue(MascotSprites.ALL.indexOf(MascotSprites.GLYPH_ORB) == 1)
        assertTrue(MascotSprites.ALL.indexOf(MascotSprites.TAPE_BUDDY) == 2)
    }

    @Test
    fun testReactionStatePrecedenceOrder() {
        assertEquals(
            MascotReactionState.PET_HOP,
            resolveMascotReactionState(
                isCallOrAlertActive = true,
                isMusicPlaying = true,
                isCharging = true,
                isPetActive = true
            )
        )
        assertEquals(
            MascotReactionState.ALERT_CALL,
            resolveMascotReactionState(
                isCallOrAlertActive = true,
                isMusicPlaying = true,
                isCharging = true
            )
        )
        assertEquals(
            MascotReactionState.CHARGING,
            resolveMascotReactionState(
                isCallOrAlertActive = false,
                isMusicPlaying = true,
                isCharging = true
            )
        )
        assertEquals(
            MascotReactionState.MUSIC_PLAYING,
            resolveMascotReactionState(
                isCallOrAlertActive = false,
                isMusicPlaying = true,
                isCharging = false
            )
        )
        assertEquals(
            MascotReactionState.IDLE,
            resolveMascotReactionState(
                isCallOrAlertActive = false,
                isMusicPlaying = false,
                isCharging = false
            )
        )
    }

    @Test
    fun testDotCatProvidesDistinctFramesPerReactionState() {
        val idle = selectMascotFrame(MascotSprites.DOTCAT, MascotReactionState.IDLE, 0)
        val music = selectMascotFrame(MascotSprites.DOTCAT, MascotReactionState.MUSIC_PLAYING, 1)
        val charging = selectMascotFrame(MascotSprites.DOTCAT, MascotReactionState.CHARGING, 0)
        val alert = selectMascotFrame(MascotSprites.DOTCAT, MascotReactionState.ALERT_CALL, 0)
        val happy = selectMascotFrame(MascotSprites.DOTCAT, MascotReactionState.PET_HOP, 0)

        assertEquals(10, idle.size)
        assertNotEquals(idle, music)
        assertNotEquals(idle, charging)
        assertNotEquals(idle, alert)
        assertNotEquals(idle, happy)
    }
}
