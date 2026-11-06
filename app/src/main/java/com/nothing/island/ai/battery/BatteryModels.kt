package com.nothing.island.ai.battery

import android.os.BatteryManager

/**
 * Charging power source classification.
 */
enum class ChargingSource {
    AC,
    USB,
    WIRELESS,
    DOCK,
    NONE,
    UNKNOWN;

    companion object {
        fun fromPlugged(plugged: Int): ChargingSource {
            return when (plugged) {
                0 -> NONE
                BatteryManager.BATTERY_PLUGGED_AC -> AC
                BatteryManager.BATTERY_PLUGGED_USB -> USB
                BatteryManager.BATTERY_PLUGGED_WIRELESS -> WIRELESS
                8 -> DOCK // BatteryManager.BATTERY_PLUGGED_DOCK (API 33+)
                else -> if (plugged > 0) UNKNOWN else NONE
            }
        }
    }
}

/**
 * Android Battery charging status.
 */
enum class ChargingStatus {
    CHARGING,
    DISCHARGING,
    FULL,
    NOT_CHARGING,
    UNKNOWN;

    companion object {
        fun fromStatus(status: Int): ChargingStatus {
            return when (status) {
                BatteryManager.BATTERY_STATUS_CHARGING -> CHARGING
                BatteryManager.BATTERY_STATUS_DISCHARGING -> DISCHARGING
                BatteryManager.BATTERY_STATUS_FULL -> FULL
                BatteryManager.BATTERY_STATUS_NOT_CHARGING -> NOT_CHARGING
                else -> UNKNOWN
            }
        }
    }
}

/**
 * Battery hardware health condition.
 */
enum class BatteryHealth {
    GOOD,
    OVERHEAT,
    DEAD,
    OVER_VOLTAGE,
    UNSPECIFIED_FAILURE,
    COLD,
    UNKNOWN;

    companion object {
        fun fromHealth(health: Int): BatteryHealth {
            return when (health) {
                BatteryManager.BATTERY_HEALTH_GOOD -> GOOD
                BatteryManager.BATTERY_HEALTH_OVERHEAT -> OVERHEAT
                BatteryManager.BATTERY_HEALTH_DEAD -> DEAD
                BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> OVER_VOLTAGE
                BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> UNSPECIFIED_FAILURE
                BatteryManager.BATTERY_HEALTH_COLD -> COLD
                else -> UNKNOWN
            }
        }
    }
}
