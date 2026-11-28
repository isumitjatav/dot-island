package com.nothing.island.ai.navigation

/**
 * Screen destinations for the Nothing Island Navigation Architecture.
 *
 * Implements the 3 primary floating bar destinations (Home, Style, Alerts)
 * plus dedicated Settings sheet and legacy studio references.
 */
sealed class IslandDestination(
    val route: String,
    val title: String,
    val shortLabel: String
) {
    // --- 3 Primary Floating Pill Navigation Destinations ---
    data object Home : IslandDestination("home", "DOT ISLAND", "HOME")
    data object Style : IslandDestination("style", "STYLE & THEMES", "STYLE")
    data object Alerts : IslandDestination("alerts", "ALERTS & SIGNALS", "ALERTS")

    // --- Settings & Modal Destinations ---
    data object Settings : IslandDestination("settings", "SETTINGS", "SETTINGS")

    // --- Backward Compatible Aliases & Studio Sub-screens ---
    data object Notifications : IslandDestination("notifications", "ALERTS & SIGNALS", "ALERTS")
    data object ThemeStudio : IslandDestination("studio_theme", "STYLE & THEMES", "STYLE")
    data object MascotStudio : IslandDestination("studio_mascot", "STYLE & THEMES", "STYLE")
    data object Music : IslandDestination("music", "MUSIC STUDIO", "MUSIC")
    data object Battery : IslandDestination("battery", "CHARGING STUDIO", "BATTERY")
    data object Studios : IslandDestination("studios", "STUDIOS HUB", "STUDIOS")
    data object AnimationStudio : IslandDestination("studio_anim", "ANIMATION STUDIO", "ANIM")
    data object GestureStudio : IslandDestination("studio_gesture", "GESTURE STUDIO", "GESTURE")
    data object PeripheralsStudio : IslandDestination("studio_peripherals", "PERIPHERALS & SYSTEM", "DEVICES")
    data object GeometryStudio : IslandDestination("studio_geometry", "GEOMETRY CALIBRATION", "CALIBRATE")
    data object CallStudio : IslandDestination("studio_call", "CALL STUDIO", "CALL")

    companion object {
        /**
         * The 3 primary floating pill navigation destinations: Home, Style, Alerts.
         */
        val bottomNavDestinations: List<IslandDestination>
            get() = listOf(Home, Style, Alerts)

        /**
         * Sub-studios accessible from the Studios Hub screen.
         */
        val studioHubDestinations: List<IslandDestination>
            get() = listOf(ThemeStudio, AnimationStudio, GestureStudio, PeripheralsStudio, MascotStudio, GeometryStudio, CallStudio, Settings)
    }
}
