package com.nothing.island.ai

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.core.content.ContextCompat
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.nothing.island.ai.battery.ChargingAnimationStyle
import com.nothing.island.ai.data.IslandPreferencesRepository
import com.nothing.island.ai.navigation.IslandDestination
import com.nothing.island.ai.notifications.NotificationPreviewMode
import com.nothing.island.ai.telephony.CallController
import com.nothing.island.ai.ui.components.NothingBottomNavigation
import com.nothing.island.ai.ui.screens.AnimationStudioScreen
import com.nothing.island.ai.ui.screens.ChargingStudioScreen
import com.nothing.island.ai.ui.screens.GeometryStudioScreen
import com.nothing.island.ai.ui.screens.GestureStudioScreen
import com.nothing.island.ai.ui.screens.HomeScreen
import com.nothing.island.ai.ui.screens.MusicStudioScreen
import com.nothing.island.ai.ui.screens.StyleScreen
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

    private val requestRuntimePermissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results[Manifest.permission.ANSWER_PHONE_CALLS] == true) {
            android.util.Log.i("MainActivity", "ANSWER_PHONE_CALLS granted — in-island call controls enabled")
        } else if (results.containsKey(Manifest.permission.ANSWER_PHONE_CALLS)) {
            android.util.Log.w("MainActivity", "ANSWER_PHONE_CALLS denied — call controls fall back to dialer notification actions")
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            results[Manifest.permission.BLUETOOTH_CONNECT] == true
        ) {
            android.util.Log.i("MainActivity", "BLUETOOTH_CONNECT granted — refreshing connected audio devices")
            IslandOverlayController.getInstance(applicationContext)
                .bluetoothEarbudTracker
                .refreshConnectedDevices(emitConnectedEvent = false)
        }
        if (results[Manifest.permission.READ_CONTACTS] == true) {
            android.util.Log.i("MainActivity", "READ_CONTACTS granted — saved caller photos enabled")
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(intent)
        updateStates()

        val missingPermissions = buildList {
            if (!CallController.canControlCalls(this@MainActivity)) {
                add(Manifest.permission.ANSWER_PHONE_CALLS)
            }
            if (ContextCompat.checkSelfPermission(
                    this@MainActivity,
                    Manifest.permission.READ_CONTACTS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                add(Manifest.permission.READ_CONTACTS)
            }
            if (ContextCompat.checkSelfPermission(
                    this@MainActivity,
                    Manifest.permission.READ_PHONE_STATE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                add(Manifest.permission.READ_PHONE_STATE)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                ContextCompat.checkSelfPermission(
                    this@MainActivity,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                add(Manifest.permission.BLUETOOTH_CONNECT)
            }
        }
        if (missingPermissions.isNotEmpty()) {
            requestRuntimePermissions.launch(missingPermissions.toTypedArray())
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

                    val showSettingsSheet = remember { mutableStateOf(false) }

                    // Back Navigation: dismiss Settings sheet if open, otherwise return to Home
                    BackHandler(enabled = showSettingsSheet.value || currentDestination.value != IslandDestination.Home) {
                        if (showSettingsSheet.value) {
                            showSettingsSheet.value = false
                        } else {
                            currentDestination.value = IslandDestination.Home
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(NothingTokens.NothingBlack)
                            .statusBarsPadding()
                    ) {
                        // Screen Container with Crossfade Transitions
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(bottom = 64.dp)
                        ) {
                            Crossfade(
                                targetState = currentDestination.value,
                                animationSpec = tween(durationMillis = 180),
                                label = "ScreenTransition"
                            ) { destination ->
                                when (destination) {
                                    // 3 Primary Destinations
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
                                            },
                                            onOpenSettings = {
                                                showSettingsSheet.value = true
                                            }
                                        )
                                    }
                                    is IslandDestination.Style,
                                    is IslandDestination.ThemeStudio,
                                    is IslandDestination.MascotStudio -> {
                                        StyleScreen()
                                    }
                                    is IslandDestination.Alerts,
                                    is IslandDestination.Notifications -> {
                                        NotificationStudioScreen()
                                    }
                                    is IslandDestination.Settings -> {
                                        SettingsScreen(
                                            geometry = geometry,
                                            onNavigateBack = {
                                                currentDestination.value = IslandDestination.Home
                                            },
                                            onReopenOnboarding = {
                                                reopenOnboarding.value = true
                                            }
                                        )
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
                                    is IslandDestination.AnimationStudio -> {
                                        AnimationStudioScreen(
                                            onNavigateBack = {
                                                currentDestination.value = IslandDestination.Home
                                            }
                                        )
                                    }
                                    is IslandDestination.GestureStudio -> {
                                        GestureStudioScreen(
                                            onNavigateBack = {
                                                currentDestination.value = IslandDestination.Home
                                            }
                                        )
                                    }
                                    is IslandDestination.PeripheralsStudio -> {
                                        PeripheralsStudioScreen(
                                            onNavigateBack = {
                                                currentDestination.value = IslandDestination.Home
                                            }
                                        )
                                    }
                                    is IslandDestination.GeometryStudio -> {
                                        GeometryStudioScreen(
                                            onNavigateBack = {
                                                currentDestination.value = IslandDestination.Home
                                            }
                                        )
                                    }
                                    is IslandDestination.CallStudio -> {
                                        com.nothing.island.ai.ui.screens.CallStudioScreen(
                                            onNavigateBack = {
                                                currentDestination.value = IslandDestination.Home
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Floating Translucent Pill Bottom Navigation Bar (Surfaced for Primary Tabs)
                        if (IslandDestination.bottomNavDestinations.contains(currentDestination.value) ||
                            currentDestination.value is IslandDestination.Notifications ||
                            currentDestination.value is IslandDestination.ThemeStudio ||
                            currentDestination.value is IslandDestination.MascotStudio
                        ) {
                            NothingBottomNavigation(
                                currentDestination = currentDestination.value,
                                onDestinationSelected = { dest ->
                                    currentDestination.value = dest
                                },
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .navigationBarsPadding()
                            )
                        }

                        // Minimal Settings Sheet
                        if (showSettingsSheet.value) {
                            ModalBottomSheet(
                                onDismissRequest = { showSettingsSheet.value = false },
                                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                                containerColor = Color(0xFF141414),
                                dragHandle = {
                                    Box(
                                        modifier = Modifier
                                            .padding(vertical = 12.dp)
                                            .size(width = 38.dp, height = 4.dp)
                                            .clip(CircleShape)
                                            .background(Color(0x60FFFFFF))
                                    )
                                }
                            ) {
                                SettingsScreen(
                                    geometry = geometry,
                                    onNavigateBack = { showSettingsSheet.value = false },
                                    onReopenOnboarding = {
                                        showSettingsSheet.value = false
                                        reopenOnboarding.value = true
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
