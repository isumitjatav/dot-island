package com.nothing.island.ai.gestures

/**
 * Customizable action target executable via Nothing Island touch gestures.
 */
enum class GestureAction {
    NONE,
    EXPAND_COLLAPSE,
    NOTIFICATIONS_HUB,
    TORCH,
    CAMERA,
    SETTINGS,
    SCREENSHOT,
    CLEAR_ALL
}

/**
 * Mode determining when the small compact notch pill automatically disappears.
 */
enum class NotchDisappearMode {
    AUTO,      // Automatically dismisses after standard timeout
    NEVER,     // Persists indefinitely until user interaction or event cancel
    ON_READ    // Dismisses only once notification/event has been viewed or interacted with
}

/**
 * System ringer mode states.
 */
enum class RingerState {
    SILENT,
    VIBRATE,
    NORMAL
}

/**
 * Immutable data class representing the complete gesture and behavior configuration
 * for Nothing Island.
 */
data class GesturePreferences(
    val singleTapAction: GestureAction = GestureAction.EXPAND_COLLAPSE,
    val longPressAction: GestureAction = GestureAction.NOTIFICATIONS_HUB,
    val swipeLeftAction: GestureAction = GestureAction.NONE,
    val swipeRightAction: GestureAction = GestureAction.NONE,
    val swipeDownAction: GestureAction = GestureAction.NONE,
    val longPressDurationMs: Long = 500L,
    val dndAutoExpandOverride: Boolean = false,
    val hideSensitiveOnKeyguard: Boolean = true,
    val notchDisappearMode: NotchDisappearMode = NotchDisappearMode.AUTO,
    val expandSilentNotifications: Boolean = false
)
