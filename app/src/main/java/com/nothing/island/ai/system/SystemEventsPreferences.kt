package com.nothing.island.ai.system

/**
 * Sprint 2 — persisted toggles for momentary system-hardware feedback capsules.
 * All toggles default to enabled so existing behavior is preserved on upgrade.
 */
data class SystemEventsPreferences(
    val systemEventsEnabled: Boolean = true,
    val ringerCapsuleEnabled: Boolean = true,
    val silentModeCapsuleEnabled: Boolean = true,
    val ringModeCapsuleEnabled: Boolean = true,
    val deviceUnlockCapsuleEnabled: Boolean = true,
    val flashlightCapsuleEnabled: Boolean = true,
    val wifiCapsuleEnabled: Boolean = true,
    val zenModeCapsuleEnabled: Boolean = true,
    val batteryWarningCapsuleEnabled: Boolean = true,
    val bluetoothCapsuleEnabled: Boolean = true,
) {
    companion object {
        val DEFAULTS = SystemEventsPreferences()
    }
}
