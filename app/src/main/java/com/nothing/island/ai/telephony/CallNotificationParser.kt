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
    val callType: Int? = null,
    val hasAnswerIntent: Boolean = false,
    val notificationWhenMs: Long? = null,
)

enum class CallPhase { INCOMING, DIALING, ONGOING }

data class CallNotificationInfo(
    val callerDisplayName: String,
    val phase: CallPhase,
    val isVideoCall: Boolean,
    val isDialing: Boolean = (phase == CallPhase.DIALING),
    val connectedAtMs: Long = 0L,
)

data class RawCallAction<T>(
    val title: String,
    val intent: T?,
    val semanticAction: Int = 0,
    val iconHint: String = "",
)

data class ParsedCallActions<T>(
    val answer: T? = null,
    val decline: T? = null,
    val hangUp: T? = null,
    val speaker: T? = null,
    val mute: T? = null,
    val hold: T? = null,
    val record: T? = null,
    val speakerStateHint: Boolean? = null,
    val muteStateHint: Boolean? = null,
    val holdStateHint: Boolean? = null,
    val recordStateHint: Boolean? = null,
)

object CallNotificationParser {
    const val CATEGORY_CALL = "call"
    const val CALL_TYPE_INCOMING = 1
    const val CALL_TYPE_ONGOING = 2
    const val CALL_TYPE_SCREENING = 3
    private const val SEMANTIC_ACTION_MUTE = 8
    private const val SEMANTIC_ACTION_UNMUTE = 9

    fun <T> matchCallActions(
        actions: List<RawCallAction<T>>,
        callText: String? = null,
        callSubText: String? = null,
    ): ParsedCallActions<T> {
        var answer: T? = null
        var decline: T? = null
        var hangUp: T? = null
        var speaker: T? = null
        var mute: T? = null
        var hold: T? = null
        var record: T? = null

        var speakerHint: Boolean? = null
        var muteHint: Boolean? = null
        var holdHint: Boolean? = null
        var recordHint: Boolean? = null

        for (action in actions) {
            val title = action.title.trim().lowercase()
            val icon = action.iconHint.trim().lowercase()
            val combined = "$title $icon"
            val intent = action.intent

            when {
                title.contains("answer") || title.contains("accept") -> {
                    if (answer == null) answer = intent
                }
                title.contains("decline") || title.contains("reject") -> {
                    if (decline == null) decline = intent
                }
                title.contains("hang") || title == "end" || title.contains("end call") ||
                    title.contains("disconnect") || title.contains("cancel") -> {
                    if (hangUp == null) hangUp = intent
                }
                combined.contains("speaker") || combined.contains("audio") ||
                    combined.contains("earpiece") || combined.contains("bluetooth") ||
                    combined.contains("headset") -> {
                    if (speaker == null) speaker = intent
                    speakerHint = when {
                        title.contains("turn off speaker") || title.contains("speaker off") ||
                            title.contains("earpiece") || title.contains("handset") -> true
                        title.contains("turn on speaker") -> false
                        else -> speakerHint
                    }
                }
                action.semanticAction == SEMANTIC_ACTION_MUTE ||
                    action.semanticAction == SEMANTIC_ACTION_UNMUTE ||
                    combined.contains("mute") || combined.contains("mic") -> {
                    if (mute == null) mute = intent
                    muteHint = when {
                        action.semanticAction == SEMANTIC_ACTION_UNMUTE ||
                            title.contains("unmute") || title.contains("muted") -> true
                        action.semanticAction == SEMANTIC_ACTION_MUTE ||
                            title.contains("mute") -> false
                        else -> muteHint
                    }
                }
                combined.contains("hold") || combined.contains("resume") || combined.contains("unhold") -> {
                    if (hold == null) hold = intent
                    holdHint = when {
                        title.contains("resume") || title.contains("unhold") -> true
                        title.contains("hold") -> false
                        else -> holdHint
                    }
                }
                combined.contains("record") -> {
                    if (record == null) record = intent
                    recordHint = title.contains("stop") || title.contains("recording")
                }
            }
        }

        val statusText = "${callText.orEmpty()} ${callSubText.orEmpty()}".lowercase()
        if (statusText.contains("on hold") || statusText.contains("holding")) {
            holdHint = true
        }
        if (statusText.contains("muted")) {
            muteHint = true
        }

        return ParsedCallActions(
            answer = answer,
            decline = decline,
            hangUp = hangUp,
            speaker = speaker,
            mute = mute,
            hold = hold,
            record = record,
            speakerStateHint = speakerHint,
            muteStateHint = muteHint,
            holdStateHint = holdHint,
            recordStateHint = recordHint,
        )
    }

    fun parse(signal: CallSignal): CallNotificationInfo? {
        if (signal.category != CATEGORY_CALL) return null

        val caller = listOf(signal.title, signal.text)
            .firstOrNull { !it.isNullOrBlank() }
            ?.trim()
            ?: return null

        val actions = signal.actionTitles.map { it.lowercase() }
        val allText = "${signal.title.orEmpty()} ${signal.text.orEmpty()} ${signal.subText.orEmpty()}".lowercase()

        // 1. Incoming call check: Notification.CallStyle EXTRA_CALL_TYPE / answer PendingIntent takes priority,
        // then action titles or explicit incoming text.
        val hasAnswerAction = signal.hasAnswerIntent ||
                actions.any { it.contains("answer") || it.contains("accept") }
        val isExplicitIncoming = signal.callType == CALL_TYPE_INCOMING ||
                signal.callType == CALL_TYPE_SCREENING ||
                hasAnswerAction ||
                allText.contains("incoming call") ||
                allText.contains("incoming voice")

        // 2. Outgoing dialing / connecting check vs ongoing connected call:
        // Google Dialer & AOSP CallStyle set EXTRA_CALL_TYPE = CALL_TYPE_ONGOING immediately when
        // placing an outgoing call, but leave EXTRA_SHOW_CHRONOMETER = false (and subtext/text
        // like "Calling..." / "Dialing" / "Ringing") until the recipient actually answers.
        // Only when the chronometer is active (with valid notification.when when supplied) or an
        // explicit elapsed MM:SS duration string appears is the call actually connected.
        val durationRegex = Regex("""\b\d{1,2}:\d{2}(?::\d{2})?\b""")
        val hasDurationText = durationRegex.containsMatchIn(signal.text.orEmpty()) ||
                durationRegex.containsMatchIn(signal.subText.orEmpty())
        val statusText = "${signal.text.orEmpty()} ${signal.subText.orEmpty()}".lowercase()
        val hasDialingKeyword = statusText.contains("calling") ||
                statusText.contains("dialing") ||
                statusText.contains("ringing") ||
                statusText.contains("connecting") ||
                statusText.contains("outgoing")
        val hasValidWhen = signal.notificationWhenMs == null || signal.notificationWhenMs > 0L
        val chronometerConnected = signal.showChronometer && hasValidWhen
        val isOngoing = when {
            isExplicitIncoming -> false
            chronometerConnected -> true
            signal.callType == CALL_TYPE_ONGOING && !signal.showChronometer && !hasDurationText -> false
            hasDialingKeyword && !hasDurationText -> false
            hasDurationText -> true
            else -> false
        }

        val phase = when {
            isExplicitIncoming -> CallPhase.INCOMING
            isOngoing -> CallPhase.ONGOING
            else -> CallPhase.DIALING
        }

        val isVideoCall = actions.any { it.contains("video") } || allText.contains("video call")
        val connectedAtMs = if (phase == CallPhase.ONGOING && chronometerConnected) {
            signal.notificationWhenMs ?: 0L
        } else {
            0L
        }

        return CallNotificationInfo(
            callerDisplayName = caller,
            phase = phase,
            isVideoCall = isVideoCall,
            isDialing = phase == CallPhase.DIALING,
            connectedAtMs = connectedAtMs,
        )
    }

    /**
     * Computes call timer seconds: always holds at 0 while dialing, otherwise anchors to
     * [connectedAtMs] (`notification.when`) when present.
     */
    fun computeCallDurationSeconds(
        isDialing: Boolean,
        connectedAtMs: Long,
        nowMs: Long = System.currentTimeMillis(),
    ): Long {
        if (isDialing) return 0L
        if (connectedAtMs <= 0L) return 0L
        return ((nowMs - connectedAtMs) / 1000L).coerceAtLeast(0L)
    }
}
