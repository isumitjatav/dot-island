package com.nothing.island.ai.battery

import com.nothing.island.ai.ExpandedViewType
import com.nothing.island.ai.IslandMode
import com.nothing.island.ai.engine.IslandEvent

/**
 * BatteryAutoExpansionPolicy
 *
 * Pure, deterministic decision layer for R8 battery and charging auto-expansion.
 * Contains ZERO Android framework dependencies so every branch is JVM unit testable.
 *
 * Two independent decisions are modelled:
 *  1. [evaluateExpansion]  — should an arriving battery/charging event pop the Island open?
 *  2. [evaluateCollapse]   — is it safe to collapse for the battery event that opened it?
 */
object BatteryAutoExpansionPolicy {

    const val MIN_DURATION_MS = 2000L
    const val MAX_DURATION_MS = 10000L

    /**
     * Result of an expansion evaluation.
     */
    sealed interface ExpansionOutcome {
        data class Expand(val durationMs: Long) : ExpansionOutcome
        data class Skip(val reason: String) : ExpansionOutcome
    }

    /**
     * Reason tags. Stable strings so they can be asserted in unit tests and grepped in Logcat.
     */
    object Reasons {
        const val BATTERY_DISABLED = "BATTERY_DISABLED"
        const val AUTO_EXPAND_DISABLED = "AUTO_EXPAND_DISABLED"
        const val NOT_A_BATTERY_EVENT = "NOT_A_BATTERY_EVENT"
        const val ALREADY_HANDLED = "ALREADY_HANDLED_PRESENTATION"
        const val ALREADY_EXPANDED = "ISLAND_ALREADY_EXPANDED"
        const val USER_INTERACTING = "USER_INTERACTING"
    }

    /**
     * Whether an arriving battery presentation is eligible to open the Island.
     */
    fun evaluateExpansion(
        event: IslandEvent?,
        presentationId: String,
        lastHandledPresentationId: String?,
        mode: IslandMode,
        expandedViewType: ExpandedViewType,
        preferences: BatteryPreferences,
        isUserInteracting: Boolean
    ): ExpansionOutcome {
        // Master battery toggle
        if (!preferences.enabled) {
            return ExpansionOutcome.Skip(Reasons.BATTERY_DISABLED)
        }

        // Eligibility check
        when (event) {
            is IslandEvent.ChargingConnected -> {
                if (!preferences.autoExpandOnPlugged) {
                    return ExpansionOutcome.Skip(Reasons.AUTO_EXPAND_DISABLED)
                }
            }
            is IslandEvent.BatteryStateChanged -> {
                if (!event.isLowBattery) {
                    return ExpansionOutcome.Skip(Reasons.NOT_A_BATTERY_EVENT)
                }
            }
            is IslandEvent.BatteryWarning -> {
                if (!event.isOverheat) {
                    return ExpansionOutcome.Skip(Reasons.NOT_A_BATTERY_EVENT)
                }
            }
            else -> {
                return ExpansionOutcome.Skip(Reasons.NOT_A_BATTERY_EVENT)
            }
        }

        // Active user interaction prevents disruptive expansion
        if (isUserInteracting) {
            return ExpansionOutcome.Skip(Reasons.USER_INTERACTING)
        }

        // Deduplication against current presentation
        if (presentationId == lastHandledPresentationId) {
            return ExpansionOutcome.Skip(Reasons.ALREADY_HANDLED)
        }

        // Do not interrupt other active expanded screens (e.g. user interacting with Dashboard or Music)
        if (mode == IslandMode.EXPANDED && expandedViewType != ExpandedViewType.BATTERY) {
            return ExpansionOutcome.Skip(Reasons.ALREADY_EXPANDED)
        }

        val duration = preferences.displayDurationMs.coerceIn(MIN_DURATION_MS, MAX_DURATION_MS)
        return ExpansionOutcome.Expand(duration)
    }

    /**
     * Evaluates whether an automatically opened battery card should collapse after duration expiration.
     */
    fun evaluateCollapse(
        currentPresentationId: String?,
        expectedPresentationId: String,
        mode: IslandMode,
        expandedViewType: ExpandedViewType,
        isUserInteracting: Boolean,
        hasActiveBatteryEvent: Boolean
    ): Boolean {
        if (currentPresentationId != expectedPresentationId) return false
        if (mode != IslandMode.EXPANDED) return false
        if (expandedViewType != ExpandedViewType.BATTERY) return false
        if (isUserInteracting) return false
        return true
    }
}
