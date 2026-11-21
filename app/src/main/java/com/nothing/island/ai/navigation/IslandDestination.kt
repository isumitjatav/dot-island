package com.nothing.island.ai.navigation

/**
 * Screen destinations for the Nothing Island Studio Navigation Architecture.
 *
 * Implements the 5 primary destinations (Home, Notifications, Music, Battery, Studios)
 * plus dedicated Studios sub-screens, while preserving legacy destination objects
 * for backward compatibility.
 */
sealed class IslandDestination(
    val route: String,
    val title: String,
    val shortLabel: String
) {
    // --- 5 Primary Destinations (Bottom Bar) ---
    data object Home : IslandDestination("home", "NOTHING ISLAND", "HOME")
    data object Notifications : IslandDestination("notifications", "NOTIFICATION STUDIO", "NOTIFS")
    data object Music : IslandDestination("music", "MUSIC STUDIO", "MUSIC")
    data object Battery : IslandDestination("battery", "CHARGING STUDIO", "BATTERY")
    data object Studios : IslandDestination("studios", "STUDIOS HUB", "STUDIOS")

    // --- Studios Sub-Destinations (Accessed via Studios Hub) ---
    data object ThemeStudio : IslandDestination("studio_theme", "THEME STUDIO", "THEME")
    data object AnimationStudio : IslandDestination("studio_anim", "ANIMATION STUDIO", "ANIM")
    data object GestureStudio : IslandDestination("studio_gesture", "GESTURE STUDIO", "GESTURE")
    data object PeripheralsStudio : IslandDestination("studio_peripherals", "PERIPHERALS & SYSTEM", "DEVICES")
    data object MascotStudio : IslandDestination("studio_mascot", "MASCOT STUDIO", "MASCOT")
    data object GeometryStudio : IslandDestination("studio_geometry", "GEOMETRY CALIBRATION", "CALIBRATE")
    data object Settings : IslandDestination("settings", "SYSTEM SETTINGS", "SETUP")

    // --- Preserved Legacy Destinations (Backward Compatibility) ---
    data object Appearance : IslandDestination("appearance", "APPEARANCE", "STYLE")
    data object Features : IslandDestination("features", "FEATURES", "MODULES")
    data object Gemini : IslandDestination("gemini", "GEMINI AI", "AI")

    companion object {
        /**
         * The 5 primary destinations surfaced in the bottom navigation bar.
         */
        val bottomNavDestinations: List<IslandDestination>
            get() = listOf(Home, Notifications, Music, Battery, Studios)

        /**
         * Sub-studios accessible from the Studios Hub screen.
         */
        val studioHubDestinations: List<IslandDestination>
            get() = listOf(ThemeStudio, AnimationStudio, GestureStudio, PeripheralsStudio, MascotStudio, GeometryStudio, Settings)
    }
}
