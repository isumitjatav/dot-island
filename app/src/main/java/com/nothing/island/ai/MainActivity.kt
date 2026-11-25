package com.nothing.island.ai

import android.Manifest
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.lifecycleScope
import com.nothing.island.ai.battery.ChargingAnimationStyle
import com.nothing.island.ai.data.IslandPreferencesRepository
import com.nothing.island.ai.navigation.IslandDestination
import com.nothing.island.ai.notifications.NotificationPreviewMode
import com.nothing.island.ai.telephony.CallController
import com.nothing.island.ai.ui.components.NothingBottomNavigation
import com.nothing.island.ai.ui.screens.AnimationStudioScreen
import com.nothing.island.ai.ui.screens.AppearanceScreen
import com.nothing.island.ai.ui.screens.ChargingStudioScreen
import com.nothing.island.ai.ui.screens.FeaturesScreen
import com.nothing.island.ai.ui.screens.GeminiScreen
import com.nothing.island.ai.ui.screens.GeometryStudioScreen
import com.nothing.island.ai.ui.screens.GestureStudioScreen
import com.nothing.island.ai.ui.screens.HomeScreen
import com.nothing.island.ai.ui.screens.MusicStudioScreen
import com.nothing.island.ai.ui.theme.IslandSignatureTheme
import com.nothing.island.ai.ui.theme.LocalIslandTheme
import com.nothing.island.ai.ui.screens.NotificationStudioScreen
import com.nothing.island.ai.ui.screens.PeripheralsStudioScreen
import com.nothing.island.ai.ui.screens.SettingsScreen
import com.nothing.island.ai.ui.screens.StudiosHubScreen
import com.nothing.island.ai.ui.screens.ThemeStudioScreen
import com.nothing.island.ai.ui.theme.NothingIslandTheme
import com.nothing.island.ai.ui.theme.NothingTokens
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val hasOverlayPermissionState = mutableStateOf(false)
    private val isServiceRunningState = mutableStateOf(false)
    private val hasNotificationPermissionState = mutableStateOf(false)
    private val hasAccessibilityPermissionState = mutableStateOf(false)
    private val isIgnoringBatteryOptimizationsState = mutableStateOf(false)

    private val requestCallPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            android.util.Log.i("MainActivity", "ANSWER_PHONE_CALLS granted — in-island call controls enabled")
        } else {
            android.util.Log.w("MainActivity", "ANSWER_PHONE_CALLS denied — call controls fall back to dialer notification actions")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(intent)
        updateStates()

        if (!CallController.canControlCalls(this)) {
            requestCallPermission.launch(Manifest.permission.ANSWER_PHONE_CALLS)
        }

        setContent {
            val context = LocalContext.current
            val preferencesRepo = remember { IslandPreferencesRepository.getInstance(context) }
            val preferences by preferencesRepo.livePreferences.collectAsState()
            val signatureTheme = remember(preferences.presetName) {
                IslandSignatureTheme.resolve(preferences.presetName)
            }

            CompositionLocalProvider(LocalIslandTheme provides signatureTheme) {
                NothingIslandTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = NothingTokens.NothingBlack
                    ) {
                        val currentDestination = remember { mutableStateOf<IslandDestination>(IslandDestination.Home) }
                        val geometry = remember { CutoutDetector.detectCutout(context) }
                        val onboardingCompleted by preferencesRepo.onboardingCompleted.collectAsState()
                        val reopenOnboarding = remember { mutableStateOf(false) }

                        if (onboardingCompleted == null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(NothingTokens.NothingBlack)
                            )
                        } else if (onboardingCompleted == false || reopenOnboarding.value) {
                            BackHandler(enabled = reopenOnboarding.value) {
                                reopenOnboarding.value = false
                            }
                            com.nothing.island.ai.ui.screens.OnboardingScreen(
                                hasOverlayPermission = hasOverlayPermissionState.value,
                                hasNotificationListenerPermission = hasNotificationPermissionState.value,
                                hasAccessibilityPermission = hasAccessibilityPermissionState.value,
                                isIgnoringBatteryOptimizations = isIgnoringBatteryOptimizationsState.value,
                                onRequestOverlayPermission = {
                                    startActivity(OverlayPermissionManager.createOverlayPermissionIntent(this@MainActivity))
                                },
                                onRequestNotificationAccess = {
                                    startActivity(
                                        com.nothing.island.ai.notifications.NotificationPermissionManager
                                            .createNotificationAccessIntent(this@MainActivity)
                                    )
                                },
                                onRequestAccessibilityAccess = {
                                    startActivity(OverlayPermissionManager.createAccessibilitySettingsIntent())
                                },
                                onRequestBatteryOptimization = {
                                    OverlayPermissionManager.launchBatteryOptimizationSettings(this@MainActivity)
                                },
                                onSkip = {
                                    reopenOnboarding.value = false
                                    lifecycleScope.launch {
                                        preferencesRepo.setOnboardingCompleted(true)
                                    }
                                },
                                onFinish = {
                                    reopenOnboarding.value = false
                                    lifecycleScope.launch {
                                        preferencesRepo.setOnboardingCompleted(true)
                                    }
                                    if (hasOverlayPermissionState.value && !IslandOverlayService.isRunning) {
                                        IslandOverlayService.startService(this@MainActivity)
                                        isServiceRunningState.value = true
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(NothingTokens.NothingBlack)
                                    .statusBarsPadding()
                                    .navigationBarsPadding()
                            )
                        } else {

                    // Intelligent Back Navigation:
                    // Sub-studios return to Studios Hub; Primary tabs return to Home
                    BackHandler(enabled = currentDestination.value != IslandDestination.Home) {
                        if (IslandDestination.studioHubDestinations.contains(currentDestination.value)) {
                            currentDestination.value = IslandDestination.Studios
                        } else {
                            currentDestination.value = IslandDestination.Home
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(NothingTokens.NothingBlack)
                            .statusBarsPadding()
                            .navigationBarsPadding()
                    ) {
                        // Screen Container with Crossfade Transitions
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        ) {
                            Crossfade(
                                targetState = currentDestination.value,
                                animationSpec = tween(durationMillis = 180),
                                label = "ScreenTransition"
                            ) { destination ->
                                when (destination) {
                                    // 5 Primary Destinations
                                    is IslandDestination.Home -> {
                                        HomeScreen(
                                            hasOverlayPermission = hasOverlayPermissionState.value,
                                            isServiceRunning = isServiceRunningState.value,
                                            geometry = geometry,
                                            hasNotificationPermission = hasNotificationPermissionState.value,
                                            onRequestPermission = {
                                                val intent = OverlayPermissionManager.createOverlayPermissionIntent(this@MainActivity)
                                                startActivity(intent)
                                            },
                                            onRequestNotificationPermission = {
                                                startActivity(
                                                    com.nothing.island.ai.notifications.NotificationPermissionManager
                                                        .createNotificationAccessIntent(this@MainActivity)
                                                )
                                            },
                                            onToggleService = {
                                                if (IslandOverlayService.isRunning) {
                                                    IslandOverlayService.stopService(this@MainActivity)
                                                    isServiceRunningState.value = false
                                                } else {
                                                    IslandOverlayService.startService(this@MainActivity)
                                                    isServiceRunningState.value = true
                                                }
                                            },
                                            onNavigateToDestination = { newDest ->
                                                currentDestination.value = newDest
                                            }
                                        )
                                    }
                                    is IslandDestination.Notifications -> {
                                        NotificationStudioScreen()
                                    }
                                    is IslandDestination.Music -> {
                                        MusicStudioScreen()
                                    }
                                    is IslandDestination.Battery -> {
                                        ChargingStudioScreen()
                                    }
                                    is IslandDestination.Studios -> {
                                        StudiosHubScreen(
                                            onNavigateToDestination = { subDest ->
                                                currentDestination.value = subDest
                                            }
                                        )
                                    }

                                    // Studios Sub-Screens
                                    is IslandDestination.ThemeStudio -> {
                                        ThemeStudioScreen(
                                            onNavigateBack = {
                                                currentDestination.value = IslandDestination.Studios
                                            }
                                        )
                                    }
                                    is IslandDestination.AnimationStudio -> {
                                        AnimationStudioScreen(
                                            onNavigateBack = {
                                                currentDestination.value = IslandDestination.Studios
                                            }
                                        )
                                    }
                                    is IslandDestination.GestureStudio -> {
                                        GestureStudioScreen(
                                            onNavigateBack = {
                                                currentDestination.value = IslandDestination.Studios
                                            }
                                        )
                                    }
                                    is IslandDestination.PeripheralsStudio -> {
                                        PeripheralsStudioScreen(
                                            onNavigateBack = {
                                                currentDestination.value = IslandDestination.Studios
                                            }
                                        )
                                    }
                                    is IslandDestination.MascotStudio -> {
                                        com.nothing.island.ai.ui.screens.MascotStudioScreen(
                                            onNavigateBack = {
                                                currentDestination.value = IslandDestination.Studios
                                            }
                                        )
                                    }
                                    is IslandDestination.GeometryStudio -> {
                                        GeometryStudioScreen(
                                            onNavigateBack = {
                                                currentDestination.value = IslandDestination.Studios
                                            }
                                        )
                                    }
                                    is IslandDestination.Settings -> {
                                        SettingsScreen(
                                            geometry = geometry,
                                            onNavigateBack = {
                                                currentDestination.value = IslandDestination.Studios
                                            },
                                            onReopenOnboarding = {
                                                reopenOnboarding.value = true
                                            }
                                        )
                                    }
                                    is IslandDestination.CallStudio -> {
                                        com.nothing.island.ai.ui.screens.CallStudioScreen(
                                            onNavigateBack = {
                                                currentDestination.value = IslandDestination.Studios
                                            }
                                        )
                                    }

                                    // Preserved Legacy Destinations (Backward Compatibility)
                                    is IslandDestination.Appearance -> {
                                        AppearanceScreen()
                                    }
                                    is IslandDestination.Features -> {
                                        FeaturesScreen()
                                    }
                                    is IslandDestination.Gemini -> {
                                        GeminiScreen()
                                    }
                                }
                            }
                        }

                        // Bottom Navigation Bar (Surfaced for 5 Primary Destinations)
                        if (IslandDestination.bottomNavDestinations.contains(currentDestination.value)) {
                            NothingBottomNavigation(
                                currentDestination = currentDestination.value,
                                onDestinationSelected = { dest ->
                                    currentDestination.value = dest
                                }
                            )
                        }
                    }
                        }
                }
            }
        }
    }
}

    override fun onResume() {
        super.onResume()
        updateStates()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val previewModeExtra = intent?.getStringExtra("set_preview_mode")
        if (previewModeExtra != null) {
            try {
                val mode = NotificationPreviewMode.valueOf(previewModeExtra.uppercase())
                lifecycleScope.launch {
                    IslandPreferencesRepository.getInstance(applicationContext).updateNotificationPreviewMode(mode)
                }
            } catch (_: Exception) {}
        }

        val chargingThemeExtra = intent?.getStringExtra("set_charging_theme")
        if (chargingThemeExtra != null) {
            try {
                val style = ChargingAnimationStyle.fromString(chargingThemeExtra)
                val repo = IslandPreferencesRepository.getInstance(applicationContext)
                val current = repo.liveBatteryPreferences.value
                val updated = current.copy(animationStyle = style)
                repo.updateLiveBatteryPreferences(updated)
                lifecycleScope.launch {
                    repo.updateBatteryPreferences(updated)
                }
                IslandOverlayController.getInstance(applicationContext).expandBattery()
            } catch (_: Exception) {}
        }

        if (intent?.getBooleanExtra("collapse_island", false) == true) {
            IslandOverlayController.getInstance(applicationContext).collapse()
        } else if (intent?.getBooleanExtra("expand_battery", false) == true) {
            IslandOverlayController.getInstance(applicationContext).expandBattery()
        }
    }

    private fun updateStates() {
        hasOverlayPermissionState.value = OverlayPermissionManager.hasOverlayPermission(this) ||
                OverlayPermissionManager.isAccessibilityServiceEnabled(this)
        isServiceRunningState.value = IslandOverlayService.isRunning || IslandAccessibilityService.isRunning
        hasNotificationPermissionState.value =
            com.nothing.island.ai.notifications.NotificationPermissionManager
                .isNotificationListenerEnabled(this)
        hasAccessibilityPermissionState.value = OverlayPermissionManager.isAccessibilityServiceEnabled(this)
        isIgnoringBatteryOptimizationsState.value = OverlayPermissionManager.isIgnoringBatteryOptimizations(this)
    }
}
