package com.nothing.island.ai.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.nothing.island.ai.CutoutDetector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class IslandPreferencesUnitTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private lateinit var testDispatcher: TestDispatcher
    private lateinit var testScope: TestScope
    private lateinit var repoScope: CoroutineScope
    private lateinit var testDataStoreFile: File
    private lateinit var testDataStore: DataStore<Preferences>
    private lateinit var repository: IslandPreferencesRepository

    @Before
    fun setUp() {
        testDispatcher = StandardTestDispatcher()
        testScope = TestScope(testDispatcher)
        repoScope = CoroutineScope(SupervisorJob() + testDispatcher)
        testDataStoreFile = temporaryFolder.newFile("test_island_preferences.preferences_pb")
        testDataStore = PreferenceDataStoreFactory.create(
            scope = repoScope,
            produceFile = { testDataStoreFile }
        )
        repository = IslandPreferencesRepository.createForTesting(testDataStore, repoScope)
    }

    @After
    fun tearDown() {
        repository.close()
        repoScope.cancel()
    }

    // -------------------------------------------------------------
    // Test 1: Verify Hardware Baseline Defaults for Nothing Phone (4a)
    // -------------------------------------------------------------
    @Test
    fun test1_hardwareBaselineDefaults() = testScope.runTest {
        val defaults = IslandPreferencesRepository.HARDWARE_DEFAULTS

        assertEquals("Default compact width must be 124dp", 124.0f, defaults.compactWidthDp, 0.001f)
        assertEquals("Default compact height must be 36dp", 36.0f, defaults.compactHeightDp, 0.001f)
        assertEquals("Default top margin must be 9dp (27px)", 9.0f, defaults.topMarginDp, 0.001f)
        assertEquals("Default expanded top margin must be 0dp (status bar dock)", 0.0f, defaults.expandedTopMarginDp, 0.001f)
        assertEquals("Default horizontal offset must be 0dp (centered)", 0.0f, defaults.horizontalOffsetDp, 0.001f)
        assertEquals("Default corner radius must be 18dp (half height = perfect capsule)", 18.0f, defaults.cornerRadiusDp, 0.001f)
        assertEquals("Default expanded width must be 350dp", 350.0f, defaults.expandedWidthDp, 0.001f)
        assertEquals("Default expanded height must be 210dp", 210.0f, defaults.expandedHeightDp, 0.001f)
        assertTrue("Default pulse must be enabled", defaults.isPulsing)
        assertTrue("Default haptic feedback must be enabled", defaults.hapticFeedbackEnabled)
        assertFalse("Default reduced motion must be disabled", defaults.reducedMotionEnabled)
        assertEquals("Default preset must be Nothing Minimal", "Nothing Minimal", defaults.presetName)
        assertEquals("Default accent color must be Nothing Red", 0xFFD71920L, defaults.accentColorHex)
        assertFalse("Default camera cutout notch must be disabled", defaults.isCameraCutoutEnabled)
    }

    // -------------------------------------------------------------
    // Test 2: Initial DataStore Flow Emission Matches Defaults
    // -------------------------------------------------------------
    @Test
    fun test2_initialFlowEmissionMatchesDefaults() = testScope.runTest {
        val initialPrefs = repository.preferencesFlow.first()

        assertEquals(124.0f, initialPrefs.compactWidthDp, 0.001f)
        assertEquals(36.0f, initialPrefs.compactHeightDp, 0.001f)
        assertEquals(9.0f, initialPrefs.topMarginDp, 0.001f)
        assertEquals(0.0f, initialPrefs.expandedTopMarginDp, 0.001f)
        assertEquals(0.0f, initialPrefs.horizontalOffsetDp, 0.001f)
        assertEquals(18.0f, initialPrefs.cornerRadiusDp, 0.001f)
        assertTrue(initialPrefs.isPulsing)
        assertFalse(initialPrefs.isCameraCutoutEnabled)
    }

    @Test
    fun test_cameraCutoutEnabledToggle() = testScope.runTest {
        assertFalse(repository.livePreferences.value.isCameraCutoutEnabled)
        repository.updateCameraCutoutEnabled(true)
        advanceUntilIdle()
        assertTrue(repository.livePreferences.value.isCameraCutoutEnabled)
        val persisted = repository.preferencesFlow.first()
        assertTrue(persisted.isCameraCutoutEnabled)

        repository.updateCameraCutoutEnabled(false)
        advanceUntilIdle()
        assertFalse(repository.livePreferences.value.isCameraCutoutEnabled)
    }

    @Test
    fun test_decoupledVerticalOffsets() = testScope.runTest {
        // Compact offset can be changed without altering expanded offset
        repository.updateLiveCalibration(topMarginDp = 12f)
        repository.persistCurrentCalibration()
        advanceUntilIdle()
        assertEquals(12f, repository.livePreferences.value.topMarginDp, 0.001f)
        assertEquals(0f, repository.livePreferences.value.expandedTopMarginDp, 0.001f)

        // Expanded offset can be changed independently
        repository.updateExpandedTopMargin(6f)
        advanceUntilIdle()
        assertEquals(12f, repository.livePreferences.value.topMarginDp, 0.001f)
        assertEquals(6f, repository.livePreferences.value.expandedTopMarginDp, 0.001f)
        val persisted = repository.preferencesFlow.first()
        assertEquals(12f, persisted.topMarginDp, 0.001f)
        assertEquals(6f, persisted.expandedTopMarginDp, 0.001f)
    }

    // -------------------------------------------------------------
    // Test 3: Live In-Memory Calibration Updates Immediately
    // -------------------------------------------------------------
    @Test
    fun test3_liveInMemoryCalibrationUpdatesImmediately() = testScope.runTest {
        // Live update happens instantaneously without waiting for disk I/O
        repository.updateLiveCalibration(
            compactWidthDp = 140f,
            compactHeightDp = 40f,
            cornerRadiusDp = 20f,
            topMarginDp = 12f,
            horizontalOffsetDp = 4f
        )

        val live = repository.livePreferences.value
        assertEquals(140f, live.compactWidthDp, 0.001f)
        assertEquals(40f, live.compactHeightDp, 0.001f)
        assertEquals(20f, live.cornerRadiusDp, 0.001f)
        assertEquals(12f, live.topMarginDp, 0.001f)
        assertEquals(4f, live.horizontalOffsetDp, 0.001f)
    }

    // -------------------------------------------------------------
    // Test 4: Persist Calibration Saves to DataStore and Emits
    // -------------------------------------------------------------
    @Test
    fun test4_persistCalibrationSavesToDataStoreAndEmits() = testScope.runTest {
        repository.saveCalibration(
            compactWidthDp = 150f,
            compactHeightDp = 42f,
            cornerRadiusDp = 21f,
            topMarginDp = 15f,
            horizontalOffsetDp = -5f,
            accentColorHex = 0xFFFF2A32L,
            presetName = "Custom"
        )
        advanceUntilIdle()

        val persisted = repository.preferencesFlow.first()
        assertEquals(150f, persisted.compactWidthDp, 0.001f)
        assertEquals(42f, persisted.compactHeightDp, 0.001f)
        assertEquals(21f, persisted.cornerRadiusDp, 0.001f)
        assertEquals(15f, persisted.topMarginDp, 0.001f)
        assertEquals(-5f, persisted.horizontalOffsetDp, 0.001f)
        assertEquals(0xFFFF2A32L, persisted.accentColorHex)
        assertEquals("Custom", persisted.presetName)
    }

    // -------------------------------------------------------------
    // Test 5: Persist Current Calibration from In-Memory State
    // -------------------------------------------------------------
    @Test
    fun test5_persistCurrentCalibration() = testScope.runTest {
        repository.updateLiveCalibration(
            compactWidthDp = 135f,
            compactHeightDp = 38f
        )
        repository.persistCurrentCalibration()
        advanceUntilIdle()

        val persisted = repository.preferencesFlow.first()
        assertEquals(135f, persisted.compactWidthDp, 0.001f)
        assertEquals(38f, persisted.compactHeightDp, 0.001f)
    }

    // -------------------------------------------------------------
    // Test 6: Apply Preset Updates Memory and DataStore
    // -------------------------------------------------------------
    @Test
    fun test6_applyPreset() = testScope.runTest {
        repository.applyPreset(
            presetName = "Futuristic Red",
            widthDp = 142f,
            heightDp = 38f,
            cornerRadiusDp = 19f,
            accentColorHex = 0xFFFF2A32L
        )
        advanceUntilIdle()

        val live = repository.livePreferences.value
        assertEquals("Futuristic Red", live.presetName)
        assertEquals(142f, live.compactWidthDp, 0.001f)
        assertEquals(38f, live.compactHeightDp, 0.001f)
        assertEquals(19f, live.cornerRadiusDp, 0.001f)
        assertEquals(0xFFFF2A32L, live.accentColorHex)

        val persisted = repository.preferencesFlow.first()
        assertEquals("Futuristic Red", persisted.presetName)
        assertEquals(142f, persisted.compactWidthDp, 0.001f)
        assertEquals(38f, persisted.compactHeightDp, 0.001f)
        assertEquals(19f, persisted.cornerRadiusDp, 0.001f)
        assertEquals(0xFFFF2A32L, persisted.accentColorHex)
    }

    // -------------------------------------------------------------
    // Test 7: Reset to Hardware Defaults Clears Customizations
    // -------------------------------------------------------------
    @Test
    fun test7_resetToHardwareDefaults() = testScope.runTest {
        // First modify values
        repository.saveCalibration(
            compactWidthDp = 160f,
            compactHeightDp = 44f,
            cornerRadiusDp = 22f,
            topMarginDp = 18f,
            horizontalOffsetDp = 10f
        )
        advanceUntilIdle()

        var current = repository.preferencesFlow.first()
        assertEquals(160f, current.compactWidthDp, 0.001f)

        // Now reset
        repository.resetToHardwareDefaults()
        advanceUntilIdle()

        val resetLive = repository.livePreferences.value
        assertEquals(124.0f, resetLive.compactWidthDp, 0.001f)
        assertEquals(36.0f, resetLive.compactHeightDp, 0.001f)
        assertEquals(9.0f, resetLive.topMarginDp, 0.001f)
        assertEquals(0.0f, resetLive.horizontalOffsetDp, 0.001f)
        assertEquals(18.0f, resetLive.cornerRadiusDp, 0.001f)
        assertTrue(resetLive.isPulsing)

        val resetPersisted = repository.preferencesFlow.first()
        assertEquals(124.0f, resetPersisted.compactWidthDp, 0.001f)
        assertEquals(36.0f, resetPersisted.compactHeightDp, 0.001f)
        assertEquals(9.0f, resetPersisted.topMarginDp, 0.001f)
        assertEquals(0.0f, resetPersisted.horizontalOffsetDp, 0.001f)
        assertEquals(18.0f, resetPersisted.cornerRadiusDp, 0.001f)
    }

    // -------------------------------------------------------------
    // Test 8: Pulse and Feature Toggles Persistence
    // -------------------------------------------------------------
    @Test
    fun test8_featureTogglesPersistence() = testScope.runTest {
        repository.updatePulseEnabled(false)
        repository.updateHapticFeedback(false)
        repository.updateReducedMotion(true)
        advanceUntilIdle()

        val persisted = repository.preferencesFlow.first()
        assertFalse("Pulse must be false", persisted.isPulsing)
        assertFalse("Haptic must be false", persisted.hapticFeedbackEnabled)
        assertTrue("Reduced motion must be true", persisted.reducedMotionEnabled)

        // Toggle pulse back
        repository.updatePulseEnabled(true)
        advanceUntilIdle()
        assertTrue("Pulse must be true", repository.preferencesFlow.first().isPulsing)
    }

    // -------------------------------------------------------------
    // Test 9: Safe IOException Fallback Resilience
    // -------------------------------------------------------------
    @Test
    fun test9_ioExceptionHandling() = testScope.runTest {
        // Create a mock DataStore that throws IOException on read
        val failingDataStore = object : DataStore<Preferences> {
            override val data: Flow<Preferences> = flow {
                throw IOException("Simulated corrupted disk storage read error")
            }
            override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
                throw IOException("Simulated write error")
            }
        }

        val fallbackRepo = IslandPreferencesRepository.createForTesting(failingDataStore, repoScope)
        try {
            // Should catch IOException cleanly and emit safe default preferences
            val result = fallbackRepo.preferencesFlow.first()
            assertEquals("Fallback must retain default width", 124.0f, result.compactWidthDp, 0.001f)
            assertEquals("Fallback must retain default height", 36.0f, result.compactHeightDp, 0.001f)
            assertEquals("Fallback must retain default top margin", 9.0f, result.topMarginDp, 0.001f)
            assertTrue("Fallback must retain default pulse", result.isPulsing)
        } finally {
            fallbackRepo.close()
        }
    }

    // -------------------------------------------------------------
    // Test 10: Canonical DP to Pixel Calibration Math at Nothing Phone (4a) Density
    // -------------------------------------------------------------
    @Test
    fun test10_densityConversionCalculations() {
        val density = 3.0f // Verified Nothing Phone (4a) density (480 dpi)

        val compactWidthDp = 124.0f
        val compactHeightDp = 36.0f
        val topMarginDp = 9.0f
        val cornerRadiusDp = 18.0f
        val touchBridgeHeightDp = 58.0f

        val compactWidthPx = (compactWidthDp * density + 0.5f).toInt()
        val compactHeightPx = (compactHeightDp * density + 0.5f).toInt()
        val topMarginPx = (topMarginDp * density + 0.5f).toInt()
        val cornerRadiusPx = (cornerRadiusDp * density + 0.5f).toInt()
        val touchBridgeHeightPx = (touchBridgeHeightDp * density + 0.5f).toInt()

        assertEquals("Compact width in px must be 372", 372, compactWidthPx)
        assertEquals("Compact visual height in px must be 108", 108, compactHeightPx)
        assertEquals("Top margin in px must be 27", 27, topMarginPx)
        assertEquals("Corner radius in px must be 54", 54, cornerRadiusPx)
        assertEquals("Touch bridge height in px must be 174", 174, touchBridgeHeightPx)
    }

    // -------------------------------------------------------------
    // Test 11: Music Preferences Persistence (REQ-18, REQ-19, REQ-21, REQ-22)
    // -------------------------------------------------------------
    @Test
    fun test11_musicPreferencesDataStorePersistence() = testScope.runTest {
        val initial = repository.musicPreferencesFlow.first()
        assertTrue(initial.backgroundBlurEnabled)
        assertTrue(initial.fastForwardButtonsEnabled)
        assertTrue(initial.onlyShowWhilePlaying)
        assertTrue(initial.excludedPackages.isEmpty())

        // Test background blur update
        repository.updateMusicBackgroundBlur(false)
        testScheduler.advanceUntilIdle()
        assertFalse(repository.musicPreferencesFlow.first().backgroundBlurEnabled)

        // Test fast forward buttons update
        repository.updateMusicFastForwardButtons(false)
        testScheduler.advanceUntilIdle()
        assertFalse(repository.musicPreferencesFlow.first().fastForwardButtonsEnabled)

        // Test only show while playing update
        repository.updateMusicOnlyShowWhilePlaying(false)
        testScheduler.advanceUntilIdle()
        assertFalse(repository.musicPreferencesFlow.first().onlyShowWhilePlaying)

        // Test excluded packages update and toggle
        repository.updateMusicExcludedPackages(setOf("com.spotify.music"))
        testScheduler.advanceUntilIdle()
        assertTrue(repository.musicPreferencesFlow.first().excludedPackages.contains("com.spotify.music"))

        repository.toggleMusicPackageExclusion("com.google.android.apps.youtube.music")
        testScheduler.advanceUntilIdle()
        assertEquals(2, repository.musicPreferencesFlow.first().excludedPackages.size)
        assertTrue(repository.musicPreferencesFlow.first().excludedPackages.contains("com.google.android.apps.youtube.music"))

        // Untoggle spotify
        repository.toggleMusicPackageExclusion("com.spotify.music")
        testScheduler.advanceUntilIdle()
        assertEquals(1, repository.musicPreferencesFlow.first().excludedPackages.size)
        assertFalse(repository.musicPreferencesFlow.first().excludedPackages.contains("com.spotify.music"))
    }
}
