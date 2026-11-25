package com.nothing.island.ai.weather

import com.nothing.island.ai.notifications.NotificationPlaceholderFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WeatherAndNotificationPlaceholderUnitTest {

    @Test
    fun weatherStates_formatExplicitLabelsAndDescriptionsWithoutFakeData() {
        assertEquals("LOADING WEATHER", WeatherStateFormatter.formatChipLabel(WeatherState.Loading))
        assertEquals("LOCATION NEEDED", WeatherStateFormatter.formatChipLabel(WeatherState.PermissionRequired()))
        assertEquals("WEATHER ERROR", WeatherStateFormatter.formatChipLabel(WeatherState.Error("Timeout")))
        assertEquals("WEATHER OFF", WeatherStateFormatter.formatChipLabel(WeatherState.Unavailable()))

        val available = WeatherState.Available(temperatureCelsius = 24, conditionLabel = "Clear", locationLabel = "Bengaluru")
        assertEquals("24°C • CLEAR", WeatherStateFormatter.formatChipLabel(available))
        assertTrue(WeatherStateFormatter.formatContentDescription(available).contains("24 degrees Celsius"))
    }

    @Test
    fun unavailableWeatherProvider_emitsUnavailableStateExplicitly() {
        val provider = UnavailableWeatherProvider()
        val state = provider.state.value
        assertTrue(state is WeatherState.Unavailable)
        assertTrue(WeatherStateFormatter.formatContentDescription(state).contains("Weather unavailable"))
    }

    @Test
    fun notificationPlaceholderFormatter_hidesSensitiveContentAndFormatsAccessibleLabels() {
        val maskedDesc = NotificationPlaceholderFormatter.accessibleDescription(
            appDisplayName = "WhatsApp",
            title = "Secret Sender",
            body = "OTP 123456",
            isMasked = true
        )
        assertTrue(maskedDesc.contains("WhatsApp"))
        assertTrue(maskedDesc.contains("sensitive content hidden"))
        assertTrue(!maskedDesc.contains("OTP 123456"))

        val visibleDesc = NotificationPlaceholderFormatter.accessibleDescription(
            appDisplayName = "Messages",
            title = "Alex",
            body = "On my way",
            isMasked = false
        )
        assertEquals("Messages, Alex, On my way", visibleDesc)
    }
}
