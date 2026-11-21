package com.nothing.island.ai

/**
 * Visual presentation modes for Nothing Island.
 */
enum class IslandMode {
    /**
     * Compact pill wrapping the physical punch-hole camera.
     */
    COLLAPSED,

    /**
     * Expanded card with status details, metrics, and interactive controls.
     */
    EXPANDED
}

/**
 * Specific content presentation state when Nothing Island is in [IslandMode.EXPANDED].
 */
enum class ExpandedViewType {
    /**
     * State A: Normal Dashboard
     * Triggered by tapping the idle compact island.
     * Displays current time, date, compact weather, and recent notification previews.
     */
    DASHBOARD,

    /**
     * State B: Notification-Focused View
     * Triggered by tapping an incoming notification on the island.
     * Displays notification details, actions, and history.
     * STRICT INVARIANT: ZERO clock or weather displayed.
     */
    NOTIFICATION_FOCUSED,

    /**
     * State C: Music Player
     * Triggered by tapping the active compact music pill, automatic track expansion,
     * or horizontal swipe via Activity Switcher.
     * Displays album art, track/artist info, audio output, scrubber, controls, and visualizer.
     */
    MUSIC,

    /**
     * State D: Battery & Charging Intelligence
     * Triggered by tapping the active charging pill, automatic cable connection,
     * safety alerts (low battery / overheat), or horizontal swipe via Activity Switcher.
     * Displays live charging visualizer, battery level, wattage, voltage, temp, and honest ETA.
     */
    BATTERY,

    /**
     * State E: Incoming / Active Call (caller identity, duration, answer & decline).
     * Triggered by a call notification captured by the telephony engine.
     */
    CALL,

    /**
     * State F: Quick tools drawer with flashlight, QR, music, call and settings.
     * Triggered by a swipe-down gesture on the compact pill.
     */
    QUICK_TOOLS
}

/**
 * State container for the Nothing Island overlay.
 */
data class IslandState(
    val mode: IslandMode = IslandMode.COLLAPSED,
    val expandedViewType: ExpandedViewType = ExpandedViewType.DASHBOARD,
    val title: String = "NOTHING ISLAND",
    val subtitle: String = "ACTIVE • CALIBRATED",
    val statusText: String = "FROGGERIND A069",
    val isPulsing: Boolean = true,
    val tag: String = "NOTHING ISLAND"
)
