package com.nothing.island.ai.notifications

/**
 * Display preview mode for incoming notifications in Nothing Island.
 */
enum class NotificationPreviewMode {
    FULL,       // Shows app glyph, sender/title, and message body snippet
    TITLE_ONLY, // Shows app glyph and sender/title; hides message text snippet
    APP_ONLY,   // Shows app glyph and generic "New Notification" label
    HIDDEN      // Suppresses visual expansion; silent registration only
}

/**
 * Notification animation dynamic style.
 */
enum class NotificationAnimationStyle {
    NOTHING_SNAP,   // Crisp Nothing OS mechanical transition
    NOTHING_SMOOTH, // Spring-damped fluid expansion
    REDUCED_MOTION  // Instant alpha fade without geometric resizing
}

/**
 * Immutable configuration for notification handling and customization.
 * Extensible for future per-app rules and visual themes.
 *
 * PRIVACY GUARANTEE: Never persists notification message contents. Only preferences/settings.
 */
data class NotificationPreferences(
    val enabled: Boolean = true,
    val displayDurationMs: Long = 4500L,                          // 2000ms to 10000ms
    val previewMode: NotificationPreviewMode = NotificationPreviewMode.FULL,
    val compactExpansionEnabled: Boolean = true,                 // Expand pill horizontally on alert
    val autoExpandOnNotification: Boolean = true,                // R7.1: Open the full Island on a new notification
    val allowSilentNotifications: Boolean = false,               // Filter IMPORTANCE_LOW/MIN
    val allowOngoingNotifications: Boolean = false,              // Filter ongoing / sticky services
    val showOnLockscreen: Boolean = false,                       // Allow presentation on keyguard
    val hideSensitiveContent: Boolean = true,                    // Mask private notifications
    val animationStyle: NotificationAnimationStyle = NotificationAnimationStyle.NOTHING_SNAP,
    val excludedPackages: Set<String> = emptySet(),               // Blacklisted package names
    val syncDismissWithAndroid: Boolean = false,                 // Default: OFF. When true, user dismissal syncs to Android shade
    val swipeToDismissEnabled: Boolean = true                     // REQ-49: Direct horizontal swipe-to-dismiss gesture
) {
    companion object {
        val DEFAULTS = NotificationPreferences()
        const val MIN_DURATION_MS = 2000L
        const val MAX_DURATION_MS = 10000L
    }
}
