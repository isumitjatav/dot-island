package com.nothing.island.ai.gamemode

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build

/**
 * GameModeDetector
 *
 * Evaluates whether an active game or immersive landscape fullscreen session is in the foreground,
 * adhering to the following rules:
 * 1. Category Game Detection:
 *    - ApplicationInfo.category == ApplicationInfo.CATEGORY_GAME (API 26+) or ApplicationInfo.FLAG_IS_GAME.
 * 2. Landscape Immersive Fullscreen Detection:
 *    - Configuration.ORIENTATION_LANDSCAPE while a non-system app is in the foreground.
 * 3. Exclusions:
 *    - Island's own package (com.nothing.island.ai) never triggers game-hide.
 *    - Default launchers (e.g. com.nothing.launcher, system home activities) never trigger game-hide.
 *    - Core system packages (android, com.android.systemui) never trigger game-hide.
 */
class GameModeDetector(
    val ownPackageName: String = "com.nothing.island.ai",
    val packageCategoryResolver: (String) -> Int? = { null },
    val launcherPackagesProvider: () -> Set<String> = { emptySet() }
) {
    constructor(
        context: Context,
        launcherPackagesProvider: () -> Set<String> = { queryDefaultLaunchers(context.packageManager) }
    ) : this(
        ownPackageName = context.packageName,
        packageCategoryResolver = { pkg ->
            try {
                val appInfo = context.packageManager.getApplicationInfo(pkg, 0)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    appInfo.category
                } else {
                    if ((appInfo.flags and ApplicationInfo.FLAG_IS_GAME) != 0) {
                        ApplicationInfo.CATEGORY_GAME
                    } else {
                        ApplicationInfo.CATEGORY_UNDEFINED
                    }
                }
            } catch (_: Exception) {
                null
            }
        },
        launcherPackagesProvider = launcherPackagesProvider
    )

    companion object {
        val SYSTEM_PACKAGES = setOf(
            "android",
            "com.android.systemui",
            "com.google.android.systemui"
        )

        val TRANSIENT_SYSTEM_PACKAGES = setOf(
            "android",
            "com.android.systemui",
            "com.google.android.systemui",
            "com.nothing.gamemode",
            "com.android.permissioncontroller",
            "com.google.android.permissioncontroller",
            "com.android.packageinstaller",
            "com.google.android.packageinstaller"
        )

        val KNOWN_GAME_PREFIXES = listOf(
            "com.pubg.",
            "com.tencent.ig",
            "com.mojang.",
            "com.activision.",
            "com.garena.",
            "com.dts.freefire",
            "com.supercell.",
            "com.mihoyo."
        )

        val KNOWN_LAUNCHERS = setOf(
            "com.nothing.launcher",
            "com.google.android.apps.nexuslauncher",
            "com.android.launcher3",
            "com.android.launcher"
        )

        /**
         * Returns true for transient OS surfaces (SystemUI volume/shade/alerts, IME keyboards,
         * Nothing GameMode HUD overlay, permission dialogs) so foreground changes never break
         * active Game Mode auto-hide state.
         */
        fun isTransientSystemWindow(packageName: String?): Boolean {
            if (packageName.isNullOrBlank()) return true
            val lower = packageName.trim().lowercase()
            if (lower in TRANSIENT_SYSTEM_PACKAGES) return true
            return lower.contains("inputmethod") ||
                lower.contains(".keyboard") ||
                lower.contains("gboard") ||
                lower.contains("swiftkey")
        }

        fun queryDefaultLaunchers(packageManager: PackageManager?): Set<String> {
            if (packageManager == null) return KNOWN_LAUNCHERS
            return try {
                val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
                val activities = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
                val set = activities.mapNotNull { it.activityInfo?.packageName }.toMutableSet()
                set.addAll(KNOWN_LAUNCHERS)
                set
            } catch (_: Exception) {
                KNOWN_LAUNCHERS
            }
        }
    }

    /**
     * Determines whether the given foreground package / orientation represents an active game or
     * landscape fullscreen session.
     */
    fun isGame(
        foregroundPackage: String?,
        orientation: Int = Configuration.ORIENTATION_PORTRAIT,
        isSystemBarsHidden: Boolean = false
    ): Boolean {
        if (foregroundPackage.isNullOrBlank()) return false
        if (foregroundPackage == ownPackageName) return false
        if (SYSTEM_PACKAGES.contains(foregroundPackage)) return false
        if (isLauncher(foregroundPackage)) return false

        // 1. Check CATEGORY_GAME (API 26+) or known game package prefixes
        val category = packageCategoryResolver(foregroundPackage)
        if (category == ApplicationInfo.CATEGORY_GAME) {
            return true
        }

        val lowerPkg = foregroundPackage.lowercase()
        if (KNOWN_GAME_PREFIXES.any { lowerPkg.startsWith(it) }) {
            return true
        }

        // 2. Landscape immersive fullscreen: ORIENTATION_LANDSCAPE while a non-system app is in foreground
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            return true
        }

        return false
    }

    fun isLauncher(packageName: String): Boolean {
        if (KNOWN_LAUNCHERS.contains(packageName)) return true
        if (launcherPackagesProvider().contains(packageName)) return true
        return false
    }

    /**
     * Resolves whether Nothing Island overlay should be hidden given current preferences and foreground state.
     */
    fun shouldHideOverlay(
        autoHideInGames: Boolean,
        foregroundPackage: String?,
        orientation: Int = Configuration.ORIENTATION_PORTRAIT,
        isSystemBarsHidden: Boolean = false
    ): Boolean {
        if (!autoHideInGames) return false
        return isGame(foregroundPackage, orientation, isSystemBarsHidden)
    }
}
