package com.nothing.island.ai.ui

/** Persisted settings for the Animation Studio. */
data class AnimationPreferences(
    val physicsMode: String = "LIQUID",
    val borderGlowEnabled: Boolean = true,
    val glowSpeed: Float = 1.2f,
    val glowThickness: Float = 2.0f,
) {
    companion object {
        val DEFAULTS = AnimationPreferences()
    }
}
