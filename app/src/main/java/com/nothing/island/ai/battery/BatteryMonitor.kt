package com.nothing.island.ai.battery

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.util.Log
import com.nothing.island.ai.data.IslandPreferencesRepository
import com.nothing.island.ai.engine.IslandEvent
import com.nothing.island.ai.engine.IslandEventEngine
import com.nothing.island.ai.engine.IslandPriority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * BatteryMonitor
 *
 * Real-time Android battery telemetry monitor for Nothing Phone (4a).
 * Reads battery status, voltage, temperature, instantaneous current, and charging source
 * directly from the Android OS BatteryManager and sticky battery broadcasts.
 *
 * Enforces zero-fabrication honest metrics and coordinates with [IslandEventEngine]
 * for priority arbitration.
 */
class BatteryMonitor(
    private val context: Context,
    private val eventEngine: IslandEventEngine,
    private val preferencesRepository: IslandPreferencesRepository,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) {
    companion object {
        private const val TAG = "BatteryMonitor"

        @Volatile
        private var instance: BatteryMonitor? = null

        fun getInstance(
            context: Context,
            eventEngine: IslandEventEngine,
            preferencesRepository: IslandPreferencesRepository
        ): BatteryMonitor {
            return instance ?: synchronized(this) {
                instance ?: BatteryMonitor(
                    context.applicationContext,
                    eventEngine,
                    preferencesRepository
                ).also { instance = it }
            }
        }
    }

    private val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager

    private val _batteryState = MutableStateFlow(BatteryState.INITIAL)
    val batteryState: StateFlow<BatteryState> = _batteryState.asStateFlow()

    private val etaEstimator = BatteryEtaEstimator()

    private var isRegistered = false
    private var previousStatus: ChargingStatus = ChargingStatus.UNKNOWN
    private var previousSource: ChargingSource = ChargingSource.NONE
    private var previousPercentage: Int = -1
    private var hasEmittedLowBatteryForCycle = false
    private var hasEmittedOverheatForCycle = false
    private var isInitialSnapshot = true

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent == null) return
            processBatteryIntent(intent)
        }
    }

    /**
     * Starts listening for system battery broadcasts.
     */
    fun startMonitoring() {
        if (isRegistered) return

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
        }

        try {
            // Register receiver and get initial sticky broadcast
            val initialIntent = context.registerReceiver(batteryReceiver, filter)
            isRegistered = true
            Log.i(TAG, "BatteryMonitor registered successfully.")

            if (initialIntent != null) {
                processBatteryIntent(initialIntent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register battery receiver", e)
        }
    }

    /**
     * Stops listening and releases receiver.
     */
    fun stopMonitoring() {
        if (!isRegistered) return
        try {
            context.unregisterReceiver(batteryReceiver)
            isRegistered = false
            Log.i(TAG, "BatteryMonitor unregistered.")
        } catch (e: Exception) {
            Log.w(TAG, "Error unregistering battery receiver", e)
        }
    }

    /**
     * Parses the battery intent and updates state flow & event engine.
     */
    fun processBatteryIntent(intent: Intent) {
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
        val percentage = if (level >= 0 && scale > 0) {
            (level * 100) / scale
        } else {
            _batteryState.value.percentage
        }

        val statusRaw = intent.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN)
        val chargingStatus = ChargingStatus.fromStatus(statusRaw)

        val pluggedRaw = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)
        val chargingSource = ChargingSource.fromPlugged(pluggedRaw)

        val voltageMv = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1)
        val tempRaw = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1)
        val temperatureC = if (tempRaw > 0) tempRaw / 10.0f else null

        val healthRaw = intent.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN)
        val health = BatteryHealth.fromHealth(healthRaw)

        // Instantaneous current (microamps)
        val currentNowUa = try {
            batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW) ?: 0
        } catch (_: Exception) {
            0
        }

        // Wattage calculation: P = V * I. Only compute when voltage > 0 and current is active.
        val wattage: Float? = if (voltageMv > 0 && currentNowUa != 0) {
            val rawW = (voltageMv.toDouble() * abs(currentNowUa.toDouble())) / 1e9
            if (rawW in 0.1..120.0) {
                (Math.round(rawW * 10.0) / 10.0).toFloat()
            } else {
                null
            }
        } else {
            null
        }

        // ETA calculation
        val apiEtaMs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                batteryManager?.computeChargeTimeRemaining() ?: -1L
            } catch (_: Exception) {
                -1L
            }
        } else {
            -1L
        }

        val isChargingNow = chargingStatus == ChargingStatus.CHARGING || chargingSource != ChargingSource.NONE

        if (isChargingNow) {
            etaEstimator.addSample(percentage)
        } else {
            etaEstimator.reset()
        }

        val estimatedEtaMs = if (isChargingNow) {
            etaEstimator.estimateRemainingMs(percentage)
        } else {
            null
        }

        val prefs = preferencesRepository.liveBatteryPreferences.value
        val isLow = percentage <= prefs.lowBatteryThreshold && !isChargingNow
        val isFull = chargingStatus == ChargingStatus.FULL || percentage >= 100
        val isOverheat = (temperatureC != null && temperatureC >= prefs.temperatureWarningThresholdC)

        val newState = BatteryState(
            level = if (level >= 0) level else 100,
            scale = if (scale > 0) scale else 100,
            status = chargingStatus,
            source = chargingSource,
            health = health,
            voltageMv = if (voltageMv > 0) voltageMv else 0,
            temperatureC = temperatureC ?: 0f,
            currentMicroAmps = currentNowUa,
            chargeTimeRemainingMs = if (apiEtaMs > 0L) apiEtaMs else null,
            estimatedTimeRemainingMs = estimatedEtaMs,
            isCharging = isChargingNow,
            isLowBattery = isLow,
            isFullyCharged = isFull,
            isOverheating = isOverheat
        )

        _batteryState.value = newState

        if (isInitialSnapshot) {
            isInitialSnapshot = false
            previousStatus = chargingStatus
            previousSource = chargingSource
            previousPercentage = percentage
            Log.d(TAG, "Initial battery snapshot recorded: $percentage%, status=$chargingStatus, source=$chargingSource")
            return
        }
        if (!prefs.enabled) {
            previousStatus = chargingStatus
            previousSource = chargingSource
            previousPercentage = percentage
            return
        }

        // 1. Charger Connected Transition
        val wasPlugged = previousSource != ChargingSource.NONE && previousSource != ChargingSource.UNKNOWN
        val isPlugged = chargingSource != ChargingSource.NONE && chargingSource != ChargingSource.UNKNOWN
        if (!wasPlugged && isPlugged) {
            hasEmittedLowBatteryForCycle = false
            etaEstimator.reset()
            val isFast = (wattage != null && wattage >= 18f)
            Log.i(TAG, "Charger connected: source=$chargingSource, level=$percentage%, isFast=$isFast")

            scope.launch {
                eventEngine.emit(
                    IslandEvent.ChargingConnected(
                        id = "charging_${System.currentTimeMillis()}",
                        batteryLevel = percentage,
                        chargeType = newState.sourceLabel,
                        isFastCharging = isFast,
                        timeoutMs = prefs.displayDurationMs
                    )
                )
            }
        }

        // 2. Charger Disconnected Transition
        if (wasPlugged && !isPlugged) {
            etaEstimator.reset()
            Log.i(TAG, "Charger disconnected: level=$percentage%")
            scope.launch {
                eventEngine.emit(
                    IslandEvent.ChargingDisconnected(
                        id = "unplug_${System.currentTimeMillis()}",
                        batteryLevel = percentage,
                        timeoutMs = 3000L
                    )
                )
            }
        }

        // 3. Low Battery Safety Alert Transition (P70)
        if (!isPlugged && previousPercentage > prefs.lowBatteryThreshold && percentage <= prefs.lowBatteryThreshold && !hasEmittedLowBatteryForCycle) {
            hasEmittedLowBatteryForCycle = true
            Log.w(TAG, "Low battery safety threshold reached: $percentage% <= ${prefs.lowBatteryThreshold}%")
            scope.launch {
                eventEngine.emit(
                    IslandEvent.BatteryStateChanged(
                        id = "low_battery_${System.currentTimeMillis()}",
                        batteryLevel = percentage,
                        isCharging = false,
                        isLowBattery = true,
                        priority = IslandPriority.SAFETY_ALERT,
                        timeoutMs = 5000L
                    )
                )
            }
        }

        // 4. Overheat Safety Alert Transition (P70)
        if (prefs.temperatureWarningEnabled && temperatureC != null && temperatureC >= prefs.temperatureWarningThresholdC && !hasEmittedOverheatForCycle) {
            hasEmittedOverheatForCycle = true
            Log.w(TAG, "Battery overheat warning triggered: $temperatureC°C >= ${prefs.temperatureWarningThresholdC}°C")
            scope.launch {
                eventEngine.emit(
                    IslandEvent.BatteryWarning(
                        id = "overheat_${System.currentTimeMillis()}",
                        batteryLevel = percentage,
                        temperatureC = temperatureC,
                        isOverheat = true,
                        priority = IslandPriority.SAFETY_ALERT,
                        timeoutMs = 5000L
                    )
                )
            }
        }

        // Reset threshold latches when conditions improve
        if (percentage > prefs.lowBatteryThreshold + 2) {
            hasEmittedLowBatteryForCycle = false
        }
        if (temperatureC != null && temperatureC < prefs.temperatureWarningThresholdC - 2.0f) {
            hasEmittedOverheatForCycle = false
        }

        previousStatus = chargingStatus
        previousSource = chargingSource
        previousPercentage = percentage
    }
}
