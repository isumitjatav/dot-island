package com.nothing.island.ai.ui.components

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.util.Log
import com.nothing.island.ai.MainActivity
import com.nothing.island.ai.hardware.TorchCallbackHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class QuickToolActionType {
    DIRECT_TOGGLE,
    SETTINGS_FALLBACK,
    IN_APP_NAVIGATION
}

data class QuickToolSpec(
    val id: String,
    val label: String,
    val actionType: QuickToolActionType,
    val contentDescription: String,
    val fallbackTag: String? = null
)

sealed interface QuickToolExecutionResult {
    data class DirectToggleSuccess(val toolId: String, val isEnabled: Boolean?) : QuickToolExecutionResult
    data class SettingsOpened(val toolId: String, val settingsAction: String) : QuickToolExecutionResult
    data class InAppNavigation(val toolId: String) : QuickToolExecutionResult
    data class Unavailable(val toolId: String, val reason: String) : QuickToolExecutionResult
    data class Debounced(val toolId: String) : QuickToolExecutionResult
}

interface QuickToolPlatformBridge {
    fun toggleTorch(): Boolean
    fun isTorchEnabled(): Boolean
    fun launchSettings(primaryAction: String, fallbackAction: String? = null): String?
    fun launchAppMain(): Boolean
}

class AndroidQuickToolPlatformBridge(
    private val context: Context,
    private val torchHandler: TorchCallbackHandler
) : QuickToolPlatformBridge {

    override fun toggleTorch(): Boolean = torchHandler.toggleTorch()

    override fun isTorchEnabled(): Boolean = torchHandler.isEnabled()

    override fun launchSettings(primaryAction: String, fallbackAction: String?): String? {
        val candidates = listOfNotNull(primaryAction, fallbackAction, Settings.ACTION_SETTINGS)
        val pm = context.packageManager
        for (action in candidates) {
            val intent = Intent(action).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val resolvable = runCatching { pm?.resolveActivity(intent, 0) != null }.getOrDefault(false)
            if (resolvable) {
                val started = runCatching {
                    context.startActivity(intent)
                    true
                }.getOrDefault(false)
                if (started) return action
            }
        }
        return null
    }

    override fun launchAppMain(): Boolean {
        return runCatching {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            context.startActivity(intent)
            true
        }.getOrDefault(false)
    }
}

/**
 * Coordinates Quick Tool executions:
 * - Preserves direct camera torch toggling.
 * - Uses explicit Settings fallback for Android-protected toggles (Data, Wi-Fi, Bluetooth, Location, Hotspot, Sound).
 * - Debounces rapid duplicate launches and handles missing Settings activities cleanly.
 */
class QuickToolExecutor(
    private val bridge: QuickToolPlatformBridge,
    private val onExpandMusic: () -> Unit = {},
    private val onCollapse: () -> Unit = {},
    private val debounceWindowMs: Long = 450L,
    private val timeProvider: () -> Long = System::currentTimeMillis
) {
    companion object {
        private const val TAG = "QuickToolExecutor"

        fun describeTool(id: String): QuickToolSpec = when (id) {
            QuickTools.TORCH -> QuickToolSpec(
                id = id,
                label = QuickTools.label(id),
                actionType = QuickToolActionType.DIRECT_TOGGLE,
                contentDescription = "Toggle flashlight torch",
                fallbackTag = null
            )
            QuickTools.DATA -> QuickToolSpec(
                id = id,
                label = QuickTools.label(id),
                actionType = QuickToolActionType.SETTINGS_FALLBACK,
                contentDescription = "Open Mobile Data settings (system protected toggle)",
                fallbackTag = "SETTINGS"
            )
            QuickTools.WIFI -> QuickToolSpec(
                id = id,
                label = QuickTools.label(id),
                actionType = QuickToolActionType.SETTINGS_FALLBACK,
                contentDescription = "Open Wi-Fi settings (system protected toggle)",
                fallbackTag = "SETTINGS"
            )
            QuickTools.BLUETOOTH -> QuickToolSpec(
                id = id,
                label = QuickTools.label(id),
                actionType = QuickToolActionType.SETTINGS_FALLBACK,
                contentDescription = "Open Bluetooth settings (system protected toggle)",
                fallbackTag = "SETTINGS"
            )
            QuickTools.LOCATION -> QuickToolSpec(
                id = id,
                label = QuickTools.label(id),
                actionType = QuickToolActionType.SETTINGS_FALLBACK,
                contentDescription = "Open Location settings (system protected toggle)",
                fallbackTag = "SETTINGS"
            )
            QuickTools.HOTSPOT -> QuickToolSpec(
                id = id,
                label = QuickTools.label(id),
                actionType = QuickToolActionType.SETTINGS_FALLBACK,
                contentDescription = "Open Hotspot and tethering settings (system protected toggle)",
                fallbackTag = "SETTINGS"
            )
            QuickTools.SOUND -> QuickToolSpec(
                id = id,
                label = QuickTools.label(id),
                actionType = QuickToolActionType.SETTINGS_FALLBACK,
                contentDescription = "Open Sound settings",
                fallbackTag = "SETTINGS"
            )
            "music" -> QuickToolSpec(
                id = id,
                label = "MUSIC",
                actionType = QuickToolActionType.IN_APP_NAVIGATION,
                contentDescription = "Expand Music controller"
            )
            QuickTools.SETTINGS -> QuickToolSpec(
                id = id,
                label = QuickTools.label(id),
                actionType = QuickToolActionType.IN_APP_NAVIGATION,
                contentDescription = "Open Dot Island settings"
            )
            else -> QuickToolSpec(
                id = id,
                label = QuickTools.label(id),
                actionType = QuickToolActionType.SETTINGS_FALLBACK,
                contentDescription = "Open ${QuickTools.label(id)}"
            )
        }
    }

    private val _lastResult = MutableStateFlow<QuickToolExecutionResult?>(null)
    val lastResult: StateFlow<QuickToolExecutionResult?> = _lastResult.asStateFlow()

    private var lastLaunchToolId: String? = null
    private var lastLaunchAtMs: Long = Long.MIN_VALUE

    fun execute(id: String): QuickToolExecutionResult {
        if (id == QuickTools.TORCH) {
            val toggled = runCatching { bridge.toggleTorch() }.getOrDefault(false)
            val result = if (toggled) {
                QuickToolExecutionResult.DirectToggleSuccess(id, bridge.isTorchEnabled())
            } else {
                QuickToolExecutionResult.Unavailable(id, "Camera flash unit unavailable")
            }
            _lastResult.value = result
            return result
        }

        if (id == "music") {
            onExpandMusic()
            val result = QuickToolExecutionResult.InAppNavigation(id)
            _lastResult.value = result
            return result
        }

        val now = timeProvider()
        if (lastLaunchToolId == id && lastLaunchAtMs != Long.MIN_VALUE && (now - lastLaunchAtMs) < debounceWindowMs) {
            Log.d(TAG, "Debounced duplicate quick tool tap for $id")
            val debounced = QuickToolExecutionResult.Debounced(id)
            _lastResult.value = debounced
            return debounced
        }

        val result = when (id) {
            QuickTools.DATA -> launchSettingsAction(id, Settings.ACTION_DATA_ROAMING_SETTINGS, Settings.ACTION_WIRELESS_SETTINGS)
            QuickTools.WIFI -> launchSettingsAction(id, Settings.ACTION_WIFI_SETTINGS, Settings.ACTION_WIRELESS_SETTINGS)
            QuickTools.BLUETOOTH -> launchSettingsAction(id, Settings.ACTION_BLUETOOTH_SETTINGS, Settings.ACTION_SETTINGS)
            QuickTools.LOCATION -> launchSettingsAction(id, Settings.ACTION_LOCATION_SOURCE_SETTINGS, Settings.ACTION_SETTINGS)
            QuickTools.HOTSPOT -> launchSettingsAction(id, Settings.ACTION_WIRELESS_SETTINGS, Settings.ACTION_SETTINGS)
            QuickTools.SOUND -> launchSettingsAction(id, Settings.ACTION_SOUND_SETTINGS, Settings.ACTION_SETTINGS)
            QuickTools.SETTINGS -> {
                if (bridge.launchAppMain()) {
                    lastLaunchToolId = id
                    lastLaunchAtMs = now
                    QuickToolExecutionResult.InAppNavigation(id)
                } else {
                    QuickToolExecutionResult.Unavailable(id, "Unable to open Dot Island settings")
                }
            }
            else -> QuickToolExecutionResult.Unavailable(id, "Unknown quick tool id=$id")
        }

        if (result !is QuickToolExecutionResult.Unavailable) {
            onCollapse()
        }
        _lastResult.value = result
        return result
    }

    private fun launchSettingsAction(
        id: String,
        primaryAction: String,
        fallbackAction: String?
    ): QuickToolExecutionResult {
        val launchedAction = runCatching { bridge.launchSettings(primaryAction, fallbackAction) }.getOrNull()
        return if (launchedAction != null) {
            lastLaunchToolId = id
            lastLaunchAtMs = timeProvider()
            QuickToolExecutionResult.SettingsOpened(id, launchedAction)
        } else {
            Log.w(TAG, "No Settings activity available to handle $id ($primaryAction)")
            QuickToolExecutionResult.Unavailable(id, "Settings activity unavailable on device")
        }
    }
}
