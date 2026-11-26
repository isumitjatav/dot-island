package com.nothing.island.ai.gamemode

import android.view.accessibility.AccessibilityEvent

/**
 * Result of classifying a window state transition event with respect to the system
 * Notification Shade / Quick Settings panel.
 */
enum class SystemShadeTransition {
    SHADE_OPENED,
    SHADE_CLOSED,
    IGNORE,
}

/**
 * Pure detector identifying when SystemUI's Notification Shade / Quick Settings drawer
 * expands over the status bar (so Dot Island can hide temporarily without overlapping the
 * status bar clock or quick tiles) vs transient dialogs (volume slider, heads-up banners).
 */
object SystemUiShadeDetector {

    private const val SYSTEM_UI_PACKAGE = "com.android.systemui"

    private val IGNORED_SYSTEMUI_KEYWORDS = listOf(
        "volumedialog",
        "volume",
        "headsup",
        "toast",
        "screenshot",
        "edge",
        "keyguard",
        "bouncer",
    )

    private val SHADE_CLASS_KEYWORDS = listOf(
        "notificationshade",
        "shadewindow",
        "quicksettings",
        "qsfragment",
        "qspanel",
        "statusbarwindow",
        "notificationpanel",
        "controlcenter",
    )

    private val SHADE_TEXT_KEYWORDS = listOf(
        "notification shade",
        "quick settings",
        "control center",
    )

    const val DEFAULT_CLOSE_SETTLE_DEBOUNCE_MS = 750L

    fun detectShadeTransition(
        eventType: Int,
        eventPackage: String?,
        eventClassName: String?,
        eventText: String?,
        @Suppress("UNUSED_PARAMETER") ownPackageName: String,
        isShadeCurrentlyOpen: Boolean,
        nowMs: Long = 0L,
        lastShadeClosedAtMs: Long = 0L,
        closeSettleDebounceMs: Long = DEFAULT_CLOSE_SETTLE_DEBOUNCE_MS,
    ): SystemShadeTransition {
        if (eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            eventType != AccessibilityEvent.TYPE_WINDOWS_CHANGED
        ) {
            return SystemShadeTransition.IGNORE
        }
        val pkg = eventPackage?.trim().orEmpty()
        if (pkg.isEmpty()) return SystemShadeTransition.IGNORE

        if (pkg == SYSTEM_UI_PACKAGE) {
            val withinCloseSettleWindow =
                !isShadeCurrentlyOpen &&
                    nowMs > 0L &&
                    lastShadeClosedAtMs > 0L &&
                    (nowMs - lastShadeClosedAtMs) in 0 until closeSettleDebounceMs
            if (withinCloseSettleWindow) {
                return SystemShadeTransition.IGNORE
            }
            val clsLower = eventClassName?.trim()?.lowercase().orEmpty()
            val textLower = eventText?.trim()?.lowercase().orEmpty()
            if (IGNORED_SYSTEMUI_KEYWORDS.any { clsLower.contains(it) }) {
                return SystemShadeTransition.IGNORE
            }
            val isShadeClass = SHADE_CLASS_KEYWORDS.any { clsLower.contains(it) }
            val isShadeText = SHADE_TEXT_KEYWORDS.any { textLower.contains(it) }
            if (isShadeClass || isShadeText) {
                return SystemShadeTransition.SHADE_OPENED
            }
            return SystemShadeTransition.IGNORE
        }

        if (isShadeCurrentlyOpen &&
            !GameModeDetector.isTransientSystemWindow(pkg)
        ) {
            return SystemShadeTransition.SHADE_CLOSED
        }

        // Fallback if SystemUI explicitly reports StatusBar collapsed or non-shade window
        return SystemShadeTransition.IGNORE
    }
}
