package com.nothing.island.ai.telephony

/** Persisted settings for the Call studio. */
data class CallPreferences(
    val systemCallEnabled: Boolean = true,
    val doubleIncomingCallActions: Boolean = true,
    val otherAppsCalls: Boolean = true,
) {
    companion object {
        val DEFAULTS = CallPreferences()
    }
}
