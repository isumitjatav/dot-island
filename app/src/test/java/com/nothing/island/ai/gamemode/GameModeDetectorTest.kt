package com.nothing.island.ai.gamemode

import android.content.pm.ApplicationInfo
import android.content.res.Configuration
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit test suite for [GameModeDetector] foreground game detection and auto-hide resolution.
 */
class GameModeDetectorTest {

    private lateinit var detector: GameModeDetector

    private val gamePackages = setOf(
        "com.epicgames.fortnite",
        "com.supercell.clashofclans",
        "com.miHoYo.GenshinImpact",
        "com.mojang.minecraftpe"
    )

    private val nonGamePackages = setOf(
        "com.whatsapp",
        "com.spotify.music",
        "com.google.android.youtube",
        "com.android.chrome"
    )

    @Before
    fun setUp() {
        detector = GameModeDetector(
            ownPackageName = "com.nothing.island.ai",
            packageCategoryResolver = { pkg ->
                if (gamePackages.contains(pkg)) {
                    ApplicationInfo.CATEGORY_GAME
                } else if (nonGamePackages.contains(pkg)) {
                    ApplicationInfo.CATEGORY_UNDEFINED
                } else {
                    null
                }
            },
            launcherPackagesProvider = {
                setOf("com.nothing.launcher", "com.google.android.apps.nexuslauncher")
            }
        )
    }

    // =========================================================================
    // 1. CATEGORY_GAME DETECTION
    // =========================================================================

    @Test
    fun testCategoryGame_hidesOverlay_whenAutoHideEnabled() {
        val isGame = detector.isGame("com.epicgames.fortnite", Configuration.ORIENTATION_PORTRAIT)
        assertTrue("Fortnite must be detected as a game via CATEGORY_GAME", isGame)

        val shouldHide = detector.shouldHideOverlay(
            autoHideInGames = true,
            foregroundPackage = "com.epicgames.fortnite",
            orientation = Configuration.ORIENTATION_PORTRAIT
        )
        assertTrue("Overlay must hide when autoHideInGames = true and game is foreground", shouldHide)
    }

    @Test
    fun testCategoryGame_leavesVisible_whenAutoHideDisabled() {
        val shouldHide = detector.shouldHideOverlay(
            autoHideInGames = false,
            foregroundPackage = "com.epicgames.fortnite",
            orientation = Configuration.ORIENTATION_PORTRAIT
        )
        assertFalse("Overlay must stay visible when autoHideInGames = false even if game is foreground", shouldHide)
    }

    // =========================================================================
    // 2. LANDSCAPE IMMERSIVE FULLSCREEN DETECTION
    // =========================================================================

    @Test
    fun testLandscapeFullscreen_hidesOverlay_whenAutoHideEnabled() {
        // Non-game app running in landscape orientation
        val isGameOrFullscreen = detector.isGame("com.spotify.music", Configuration.ORIENTATION_LANDSCAPE)
        assertTrue("Landscape orientation for non-system app must trigger game-mode fullscreen", isGameOrFullscreen)

        val shouldHide = detector.shouldHideOverlay(
            autoHideInGames = true,
            foregroundPackage = "com.spotify.music",
            orientation = Configuration.ORIENTATION_LANDSCAPE
        )
        assertTrue("Overlay must hide during landscape fullscreen when autoHideInGames = true", shouldHide)
    }

    @Test
    fun testLandscapeFullscreen_leavesVisible_whenAutoHideDisabled() {
        val shouldHide = detector.shouldHideOverlay(
            autoHideInGames = false,
            foregroundPackage = "com.spotify.music",
            orientation = Configuration.ORIENTATION_LANDSCAPE
        )
        assertFalse("Overlay must stay visible during landscape fullscreen when autoHideInGames = false", shouldHide)
    }

    // =========================================================================
    // 3. EXIT GAME / RESTORE OVERLAY
    // =========================================================================

    @Test
    fun testExitGame_toLauncher_restoresOverlay() {
        // Step 1: In game -> hidden
        assertTrue(detector.shouldHideOverlay(true, "com.miHoYo.GenshinImpact", Configuration.ORIENTATION_LANDSCAPE))

        // Step 2: User presses home -> launcher active
        val shouldHideOnHome = detector.shouldHideOverlay(
            autoHideInGames = true,
            foregroundPackage = "com.nothing.launcher",
            orientation = Configuration.ORIENTATION_PORTRAIT
        )
        assertFalse("Exiting game to Nothing Launcher must immediately restore overlay", shouldHideOnHome)
    }

    @Test
    fun testExitGame_toNormalApp_restoresOverlay() {
        // Step 1: In game -> hidden
        assertTrue(detector.shouldHideOverlay(true, "com.mojang.minecraftpe", Configuration.ORIENTATION_LANDSCAPE))

        // Step 2: Switch to messaging app in portrait
        val shouldHideOnChat = detector.shouldHideOverlay(
            autoHideInGames = true,
            foregroundPackage = "com.whatsapp",
            orientation = Configuration.ORIENTATION_PORTRAIT
        )
        assertFalse("Switching from game to WhatsApp portrait must restore overlay", shouldHideOnChat)
    }

    // =========================================================================
    // 4. EXCLUSIONS
    // =========================================================================

    @Test
    fun testExclusions_ownPackageNeverHides() {
        assertFalse(
            "Own app must never trigger game hide in portrait",
            detector.shouldHideOverlay(true, "com.nothing.island.ai", Configuration.ORIENTATION_PORTRAIT)
        )
        assertFalse(
            "Own app must never trigger game hide in landscape",
            detector.shouldHideOverlay(true, "com.nothing.island.ai", Configuration.ORIENTATION_LANDSCAPE)
        )
    }

    @Test
    fun testExclusions_launchersNeverHide() {
        assertFalse(
            "Nothing Launcher must never trigger game hide in portrait",
            detector.shouldHideOverlay(true, "com.nothing.launcher", Configuration.ORIENTATION_PORTRAIT)
        )
        assertFalse(
            "Nothing Launcher must never trigger game hide in landscape",
            detector.shouldHideOverlay(true, "com.nothing.launcher", Configuration.ORIENTATION_LANDSCAPE)
        )
        assertFalse(
            "Nexus Launcher must never trigger game hide",
            detector.shouldHideOverlay(true, "com.google.android.apps.nexuslauncher", Configuration.ORIENTATION_PORTRAIT)
        )
    }

    @Test
    fun testExclusions_systemPackagesNeverHide() {
        assertFalse(
            "Android framework must never trigger game hide",
            detector.shouldHideOverlay(true, "android", Configuration.ORIENTATION_PORTRAIT)
        )
        assertFalse(
            "SystemUI must never trigger game hide even in landscape",
            detector.shouldHideOverlay(true, "com.android.systemui", Configuration.ORIENTATION_LANDSCAPE)
        )
    }

    @Test
    fun testExclusions_nullOrBlankPackageNeverHides() {
        assertFalse("Null package must not hide overlay", detector.shouldHideOverlay(true, null))
        assertFalse("Blank package must not hide overlay", detector.shouldHideOverlay(true, ""))
        assertFalse("Whitespace package must not hide overlay", detector.shouldHideOverlay(true, "   "))
    }
}
