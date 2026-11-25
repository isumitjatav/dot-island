package com.nothing.island.ai.weather

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Explicit UI state contract for Dashboard weather telemetry.
 * Never fabricates temperatures or conditions when no backend provider is configured.
 */
sealed interface WeatherState {
    data object Loading : WeatherState

    data class Available(
        val temperatureCelsius: Int,
        val conditionLabel: String,
        val locationLabel: String? = null,
        val updatedAtMs: Long = System.currentTimeMillis()
    ) : WeatherState

    data class Unavailable(
        val reason: String = "Weather provider not configured"
    ) : WeatherState

    data class PermissionRequired(
        val missingPermission: String = "Location permission required"
    ) : WeatherState

    data class Error(
        val message: String
    ) : WeatherState
}

interface WeatherProvider {
    val state: StateFlow<WeatherState>
    suspend fun refresh()
}

/**
 * Default safe offline provider until a weather API integration is configured.
 */
class UnavailableWeatherProvider(
    reason: String = "Weather provider not configured"
) : WeatherProvider {
    private val _state = MutableStateFlow<WeatherState>(WeatherState.Unavailable(reason))
    override val state: StateFlow<WeatherState> = _state.asStateFlow()

    override suspend fun refresh() {
        // Intentionally no network calls or fabricated weather data.
    }
}

object WeatherStateFormatter {
    fun formatChipLabel(state: WeatherState): String = when (state) {
        is WeatherState.Loading -> "LOADING WEATHER"
        is WeatherState.Available -> "${state.temperatureCelsius}°C • ${state.conditionLabel.uppercase()}"
        is WeatherState.PermissionRequired -> "LOCATION NEEDED"
        is WeatherState.Error -> "WEATHER ERROR"
        is WeatherState.Unavailable -> "WEATHER OFF"
    }

    fun formatContentDescription(state: WeatherState): String = when (state) {
        is WeatherState.Loading -> "Weather information is loading"
        is WeatherState.Available -> buildString {
            append("${state.temperatureCelsius} degrees Celsius, ${state.conditionLabel}")
            state.locationLabel?.takeIf { it.isNotBlank() }?.let { append(" in $it") }
        }
        is WeatherState.PermissionRequired -> "Weather unavailable: ${state.missingPermission}"
        is WeatherState.Error -> "Weather error: ${state.message}"
        is WeatherState.Unavailable -> "Weather unavailable: ${state.reason}"
    }
}
