package com.nothing.island.ai.telephony

/**
 * R9 — pure, device-free parsing of call notification signals.
 *
 * Call state is derived from the notification listener (which users already
 * grant for island notifications) instead of restricted telephony permissions.
 * Nothing here depends on Android classes, so it is fully unit-testable.
 */
data class CallSignal(
    val category: String?,
    val title: String?,
    val text: String?,
    val actionTitles: List<String>,
)

enum class CallPhase { INCOMING, ONGOING }

data class CallNotificationInfo(
    val callerDisplayName: String,
    val phase: CallPhase,
    val isVideoCall: Boolean,
)

object CallNotificationParser {
    const val CATEGORY_CALL = "call"

    fun parse(signal: CallSignal): CallNotificationInfo? {
        if (signal.category != CATEGORY_CALL) return null

        val caller = listOf(signal.title, signal.text)
            .firstOrNull { !it.isNullOrBlank() }
            ?.trim()
            ?: return null

        val actions = signal.actionTitles.map { it.lowercase() }
        val isIncoming = actions.any { title ->
            title.contains("answer") || title.contains("accept") ||
                title.contains("reject") || title.contains("decline") || title.contains("hang")
        }
        val isVideoCall = actions.any { it.contains("video") }

        return CallNotificationInfo(
            callerDisplayName = caller,
            phase = if (isIncoming) CallPhase.INCOMING else CallPhase.ONGOING,
            isVideoCall = isVideoCall,
        )
    }
}
