package com.nothing.island.ai.ui

import com.nothing.island.ai.ui.components.ChibiState
import com.nothing.island.ai.ui.components.ChibiVariant
import com.nothing.island.ai.ui.components.IdleCyclePhase
import com.nothing.island.ai.ui.components.NotifMascotPhase
import com.nothing.island.ai.ui.components.computeCompactStageWindowSpec
import com.nothing.island.ai.ui.components.computeIdlePatrolPose
import com.nothing.island.ai.ui.components.resolveChibiState
import com.nothing.island.ai.ui.components.resolveChibiVariant
import com.nothing.island.ai.ui.components.resolveChibiZIndex
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * Unit tests for articulated Chibi Companion state resolution, layering zIndex,
 * and center camera cutout hop clearance.
 */
class ChibiMascotUnitTest {

    @Test
    fun resolveChibiState_coversAllSixExpressiveMotionStatesAndPriority() {
        assertEquals(
            ChibiState.IDLE,
            resolveChibiState(
                isCallActive = false,
                isMusicPlaying = false,
                hasActiveNotification = false,
                isCharging = false,
                isPetActive = false,
            )
        )
        assertEquals(
            ChibiState.CHARGING,
            resolveChibiState(
                isCallActive = false,
                isMusicPlaying = false,
                hasActiveNotification = false,
                isCharging = true,
            )
        )
        assertEquals(
            ChibiState.MUSIC,
            resolveChibiState(
                isCallActive = false,
                isMusicPlaying = true,
                hasActiveNotification = false,
                isCharging = true,
            )
        )
        assertEquals(
            ChibiState.NOTIFICATION,
            resolveChibiState(
                isCallActive = false,
                isMusicPlaying = true,
                hasActiveNotification = true,
                isCharging = true,
            )
        )
        assertEquals(
            ChibiState.CALL,
            resolveChibiState(
                isCallActive = true,
                isMusicPlaying = true,
                hasActiveNotification = true,
                isCharging = true,
            )
        )
        assertEquals(
            ChibiState.PET,
            resolveChibiState(
                isCallActive = true,
                isMusicPlaying = true,
                hasActiveNotification = true,
                isCharging = true,
                isPetActive = true,
            )
        )
    }

    @Test
    fun resolveChibiZIndex_peeksBehindCapsuleAndHangsOrDancesInFront() {
        assertEquals(0f, resolveChibiZIndex(ChibiState.NOTIFICATION, NotifMascotPhase.PEEK), 0.001f)
        assertEquals(2f, resolveChibiZIndex(ChibiState.NOTIFICATION, NotifMascotPhase.HANG), 0.001f)
        assertEquals(2f, resolveChibiZIndex(ChibiState.MUSIC, NotifMascotPhase.PEEK), 0.001f)
        assertEquals(2f, resolveChibiZIndex(ChibiState.CHARGING, NotifMascotPhase.PEEK), 0.001f)
        assertEquals(2f, resolveChibiZIndex(ChibiState.CALL, NotifMascotPhase.PEEK), 0.001f)
        assertEquals(2f, resolveChibiZIndex(ChibiState.PET, NotifMascotPhase.PEEK), 0.001f)
    }

    @Test
    fun idlePatrol_sleepsLeftForFiveSecondsAndHopsClearOverCenterCameraCutout() {
        val (sleepPhase, sleepX, sleepY) = computeIdlePatrolPose(2_500L)
        assertEquals(IdleCyclePhase.SLEEP_LEFT, sleepPhase)
        assertTrue("Left shoulder sleep must stay left of cutout", sleepX <= -30f)
        assertTrue("Idle sleep Y must stay strictly below top capsule edge (y >= 0)", sleepY >= 0f)

        // Midpoint of HOP_TO_RIGHT (6_000..7_200 -> 6_600ms) crosses x=0 directly over camera cutout
        val (hopPhase, hopX, hopY) = computeIdlePatrolPose(6_600L)
        assertEquals(IdleCyclePhase.HOP_TO_RIGHT, hopPhase)
        assertTrue("Midpoint should pass center cutout zone", abs(hopX) <= 18f)
        assertTrue("Hop Y must stay strictly below top capsule border (y >= 0)", hopY >= 0f)

        val (wavePhase, waveX, waveY) = computeIdlePatrolPose(8_000L)
        assertEquals(IdleCyclePhase.WAVE_RIGHT, wavePhase)
        assertTrue("Right shoulder wave must stay right of cutout", waveX >= 30f)
        assertTrue("Right wave Y must stay strictly below top capsule edge", waveY >= 0f)
    }

    @Test
    fun compactStageWindowSpec_shrinksToExactCapsuleWhenIdle() {
        val idleSpec = computeCompactStageWindowSpec(
            compactWidthDp = 124f,
            compactHeightDp = 36f,
            mascotEnabled = true,
            isDualPill = false,
            isEarbudActive = false,
            isSystemAlertActive = false,
            isCallActive = false,
            hasActiveNotification = false,
            isMusicPlaying = false,
            isChargingActive = false,
        )
        assertEquals(124f, idleSpec.widthDp, 0.001f)
        assertEquals(36f, idleSpec.heightDp, 0.001f)

        val musicSpec = computeCompactStageWindowSpec(
            compactWidthDp = 124f,
            compactHeightDp = 36f,
            mascotEnabled = true,
            isDualPill = false,
            isEarbudActive = false,
            isSystemAlertActive = false,
            isCallActive = false,
            hasActiveNotification = false,
            isMusicPlaying = true,
            isChargingActive = false,
        )
        assertEquals(300f, musicSpec.widthDp, 0.001f)
        assertEquals(224f, musicSpec.capsuleWidthDp, 0.001f)
        assertEquals(44f, musicSpec.heightDp, 0.001f)

        val earbudSpec = computeCompactStageWindowSpec(
            compactWidthDp = 124f,
            compactHeightDp = 36f,
            mascotEnabled = true,
            isDualPill = false,
            isEarbudActive = true,
            isSystemAlertActive = false,
            isCallActive = false,
            hasActiveNotification = false,
            isMusicPlaying = false,
            isChargingActive = false,
        )
        assertEquals(236f, earbudSpec.widthDp, 0.001f)
        assertEquals(38f, earbudSpec.heightDp, 0.001f)
        val callSpec = computeCompactStageWindowSpec(
            compactWidthDp = 124f,
            compactHeightDp = 36f,
            mascotEnabled = true,
            isDualPill = false,
            isEarbudActive = false,
            isSystemAlertActive = false,
            isCallActive = true,
            hasActiveNotification = false,
            isMusicPlaying = false,
            isChargingActive = false,
        )
        assertEquals(232f, callSpec.widthDp, 0.001f)
        assertEquals(44f, callSpec.heightDp, 0.001f)
        assertEquals(200f, callSpec.capsuleWidthDp, 0.001f)
    }

    @Test
    fun callCompactChibi_positionsLowerRightCurveBelowStatusBarRow() {
        val pillWidth = 200f
        val offsetX = com.nothing.island.ai.ui.components.computeCallCompactChibiXOffsetDp(pillWidth)
        val offsetY = com.nothing.island.ai.ui.components.computeCallCompactChibiYOffsetDp(0f)
        assertEquals(96f, offsetX, 0.001f)
        assertTrue("Compact call chibi X must hug lower-right pill curve without overlapping 5G+", offsetX < pillWidth / 2f)
        assertTrue("Compact call chibi Y must shift downward below top status bar row", offsetY >= 20f)
    }

    @Test
    fun callHandTip_staysWithinSixDpOfShoulderAndNeverCrossesAnswer() {
        val shoulder = androidx.compose.ui.geometry.Offset(10f, 20f)
        val leftTip = com.nothing.island.ai.ui.components.computeCallHandTip(
            shoulder = shoulder,
            armLength = 5f,
            armAngleDeg = 35f,
            isLeft = true,
        )
        val dx = kotlin.math.abs(leftTip.x - shoulder.x)
        val dy = kotlin.math.abs(leftTip.y - shoulder.y)
        assertTrue(dx <= 5.01f && dy <= 5.01f)
    }

    @Test
    fun resolveChibiVariant_mapsStoredSpriteNames() {
        assertEquals(ChibiVariant.DOT_CHIBI, resolveChibiVariant("DOTCAT"))
        assertEquals(ChibiVariant.GLYPH_CHIBI, resolveChibiVariant("GLYPH_ORB"))
        assertEquals(ChibiVariant.TAPE_CHIBI, resolveChibiVariant("TAPE_BUDDY"))
    }
}
