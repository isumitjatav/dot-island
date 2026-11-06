package com.nothing.island.ai.navigation

/**
 * Screen destinations for the Nothing Island five-screen architecture.
 */
sealed class IslandDestination(
    val route: String,
    val title: String,
    val shortLabel: String
) {
    data object Home : IslandDestination("home", "NOTHING ISLAND", "HOME")
    data object Appearance : IslandDestination("appearance", "APPEARANCE", "STYLE")
    data object Features : IslandDestination("features", "FEATURES", "MODULES")
    data object Gemini : IslandDestination("gemini", "GEMINI AI", "AI")
    data object Settings : IslandDestination("settings", "SETTINGS", "SETUP")

    companion object {
        val bottomNavDestinations: List<IslandDestination>
            get() = listOf(Home, Appearance, Features, Gemini)
    }
}
