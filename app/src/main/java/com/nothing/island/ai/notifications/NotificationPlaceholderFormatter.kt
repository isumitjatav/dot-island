package com.nothing.island.ai.notifications

/**
 * Centralizes privacy-masked and fallback notification labels and accessibility descriptions,
 * ensuring generic labels remain intentional, privacy-safe, and TalkBack-friendly.
 */
object NotificationPlaceholderFormatter {
    const val MASKED_GENERIC_LABEL = "New notification"

    fun accessibleDescription(
        appDisplayName: String,
        title: String?,
        body: String?,
        isMasked: Boolean
    ): String {
        val safeApp = appDisplayName.ifBlank { "App" }
        return if (isMasked) {
            "$safeApp: $MASKED_GENERIC_LABEL, sensitive content hidden"
        } else {
            val parts = listOfNotNull(
                safeApp,
                title?.takeIf { it.isNotBlank() },
                body?.takeIf { it.isNotBlank() }
            )
            parts.joinToString(", ")
        }
    }
}
