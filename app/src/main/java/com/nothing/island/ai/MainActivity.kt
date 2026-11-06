package com.nothing.island.ai

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.lifecycleScope
import com.nothing.island.ai.data.IslandPreferencesRepository
import com.nothing.island.ai.navigation.IslandDestination
import com.nothing.island.ai.notifications.NotificationPreviewMode
import com.nothing.island.ai.ui.components.NothingBottomNavigation
import com.nothing.island.ai.ui.screens.AppearanceScreen
import com.nothing.island.ai.ui.screens.FeaturesScreen
import com.nothing.island.ai.ui.screens.GeminiScreen
import com.nothing.island.ai.ui.screens.HomeScreen
import com.nothing.island.ai.ui.screens.SettingsScreen
import com.nothing.island.ai.ui.theme.NothingIslandTheme
import com.nothing.island.ai.ui.theme.NothingPrimaryBackground
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val hasOverlayPermissionState = mutableStateOf(false)
    private val isServiceRunningState = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(intent)
        updateStates()

        setContent {
            NothingIslandTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = NothingPrimaryBackground
                ) {
                    val currentDestination = remember { mutableStateOf<IslandDestination>(IslandDestination.Home) }
                    val context = LocalContext.current
                    val geometry = remember { CutoutDetector.detectCutout(context) }

                    // Android Back Navigation: Returns to Home from secondary screens
                    BackHandler(enabled = currentDestination.value != IslandDestination.Home) {
                        currentDestination.value = IslandDestination.Home
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(NothingPrimaryBackground)
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
                                animationSpec = tween(durationMillis = 200),
                                label = "ScreenTransition"
                            ) { destination ->
                                when (destination) {
                                    is IslandDestination.Home -> {
                                        HomeScreen(
                                            hasOverlayPermission = hasOverlayPermissionState.value,
                                            isServiceRunning = isServiceRunningState.value,
                                            geometry = geometry,
                                            onRequestPermission = {
                                                val intent = OverlayPermissionManager.createOverlayPermissionIntent(this@MainActivity)
                                                startActivity(intent)
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
                                    is IslandDestination.Appearance -> {
                                        AppearanceScreen()
                                    }
                                    is IslandDestination.Features -> {
                                        FeaturesScreen()
                                    }
                                    is IslandDestination.Gemini -> {
                                        GeminiScreen()
                                    }
                                    is IslandDestination.Settings -> {
                                        SettingsScreen(
                                            geometry = geometry,
                                            onNavigateBack = {
                                                currentDestination.value = IslandDestination.Home
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Bottom Navigation Bar (Hidden when in Settings)
                        if (currentDestination.value != IslandDestination.Settings) {
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
    }

    private fun updateStates() {
        hasOverlayPermissionState.value = OverlayPermissionManager.hasOverlayPermission(this) ||
                OverlayPermissionManager.isAccessibilityServiceEnabled(this)
        isServiceRunningState.value = IslandOverlayService.isRunning || IslandAccessibilityService.isRunning
    }
}
