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
    val showChronometer: Boolean = false,
    val subText: String? = null,
)

enum class CallPhase { INCOMING, DIALING, ONGOING }

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
        val allText = "${signal.title.orEmpty()} ${signal.text.orEmpty()} ${signal.subText.orEmpty()}".lowercase()

        // 1. Incoming call check: actions contain Answer / Accept / Decline / Hang Up, OR text contains "incoming"
        val hasAnswerAction = actions.any { it.contains("answer") || it.contains("accept") }
        val isExplicitIncoming = hasAnswerAction || allText.contains("incoming call") || allText.contains("incoming voice")

        // 2. Outgoing dialing / connecting check vs ongoing connected call:
        // When a call is actively connected, Android dialers (Google Phone, Nothing OS, AOSP) set
        // showChronometer = true or display live elapsed duration (e.g. "00:42") in text/subtext.
        // Before the remote party answers, showChronometer is false and no elapsed timer is present.
        val durationRegex = Regex("""\b\d{1,2}:\d{2}(?::\d{2})?\b""")
        val hasDurationText = durationRegex.containsMatchIn(signal.text.orEmpty()) ||
                durationRegex.containsMatchIn(signal.subText.orEmpty())
        val isOngoing = signal.showChronometer || hasDurationText

        val phase = when {
            isExplicitIncoming -> CallPhase.INCOMING
            isOngoing -> CallPhase.ONGOING
            else -> CallPhase.DIALING
        }

        val isVideoCall = actions.any { it.contains("video") } || allText.contains("video call")

        return CallNotificationInfo(
            callerDisplayName = caller,
            phase = phase,
            isVideoCall = isVideoCall,
        )
    }
}
