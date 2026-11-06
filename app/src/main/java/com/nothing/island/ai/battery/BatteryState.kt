package com.nothing.island.ai.battery

import java.util.Locale
import kotlin.math.abs

/**
 * Immutable snapshot of battery hardware readings and computed metrics.
 *
 * HONESTY INVARIANT: If underlying sensor readings are unavailable or invalid,
 * wattage and ETA return null (displayed as "--"). Zero fabricated metrics.
 */
data class BatteryState(
    val level: Int = 100,
    val scale: Int = 100,
    val status: ChargingStatus = ChargingStatus.UNKNOWN,
    val source: ChargingSource = ChargingSource.NONE,
    val health: BatteryHealth = BatteryHealth.GOOD,
    val voltageMv: Int = 0,
    val temperatureC: Float = 0f,
    val currentMicroAmps: Int = 0,
    val chargeTimeRemainingMs: Long? = null,
    val estimatedTimeRemainingMs: Long? = null,
    val isCharging: Boolean = false,
    val isLowBattery: Boolean = false,
    val isFullyCharged: Boolean = false,
    val isOverheating: Boolean = false
) {
    /**
     * Normalized battery percentage [0..100].
     */
    val percentage: Int
        get() = if (scale > 0) ((level.toFloat() / scale.toFloat()) * 100f).toInt().coerceIn(0, 100) else level.coerceIn(0, 100)

    /**
     * Honest charging wattage in Watts, computed only if voltage > 0 and current is nonzero.
     * Returns null if readings are unavailable, zero, or uncalibrated.
     */
    val wattageW: Float?
        get() {
            if (!isCharging || voltageMv <= 0) return null
            val absCurrentUa = abs(currentMicroAmps).toLong()
            if (absCurrentUa <= 0 || absCurrentUa == Int.MAX_VALUE.toLong()) return null
            // Power (Watts) = (Voltage (mV) * Current (uA)) / 10^9
            val watts = (voltageMv.toLong() * absCurrentUa).toFloat() / 1_000_000_000f
            return if (watts in 0.1f..150.0f) watts else null
        }

    /**
     * Human-readable formatted voltage (e.g. "4.44V"), or "--" if unavailable.
     */
    val formattedVoltage: String
        get() = if (voltageMv > 0) String.format(Locale.US, "%.2fV", voltageMv / 1000f) else "--"

    /**
     * Human-readable formatted temperature (e.g. "35.0°C"), or "--" if unavailable.
     */
    val formattedTemperature: String
        get() = if (temperatureC > 0f) String.format(Locale.US, "%.1f°C", temperatureC) else "--"

    /**
     * Human-readable formatted wattage (e.g. "18.5W"), or "--" if unavailable.
     */
    val formattedWattage: String
        get() = wattageW?.let { String.format(Locale.US, "%.1fW", it) } ?: "--"

    /**
     * Human-readable formatted ETA (e.g. "35m until full"), or "--" if unavailable.
     */
    val formattedEta: String
        get() {
            val remainingMs = chargeTimeRemainingMs?.takeIf { it > 0 }
                ?: estimatedTimeRemainingMs?.takeIf { it > 0 }
                ?: return "--"
            val totalSeconds = remainingMs / 1000
            val minutes = (totalSeconds / 60) % 60
            val hours = totalSeconds / 3600
            return when {
                hours > 0 -> "${hours}h ${minutes}m"
                minutes > 0 -> "${minutes} min"
                else -> "< 1 min"
            }
        }

    /**
     * Concise label for charging power source.
     */
    val sourceLabel: String
        get() = when (source) {
            ChargingSource.AC -> "AC CHARGER"
            ChargingSource.USB -> "USB POWER"
            ChargingSource.WIRELESS -> "WIRELESS"
            ChargingSource.DOCK -> "DOCK"
            ChargingSource.NONE -> "BATTERY"
            ChargingSource.UNKNOWN -> "CHARGING"
        }

    companion object {
        val INITIAL = BatteryState()
    }
}
