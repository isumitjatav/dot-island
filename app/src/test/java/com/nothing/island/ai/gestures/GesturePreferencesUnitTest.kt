package com.nothing.island.ai.gestures

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.nothing.island.ai.data.IslandPreferencesRepository
import com.nothing.island.ai.engine.IslandEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
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

@OptIn(ExperimentalCoroutinesApi::class)
class GesturePreferencesUnitTest {

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
        testDataStoreFile = temporaryFolder.newFile("test_gesture_preferences.preferences_pb")
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

    @Test
    fun defaultPreferences_matchBaselineSpecifications() = testScope.runTest {
        val initial = repository.gesturePreferencesFlow.first()

        assertEquals(GestureAction.EXPAND_COLLAPSE, initial.singleTapAction)
        assertEquals(GestureAction.NOTIFICATIONS_HUB, initial.longPressAction)
        assertEquals(GestureAction.NONE, initial.swipeLeftAction)
        assertEquals(GestureAction.NONE, initial.swipeRightAction)
        assertEquals(GestureAction.NONE, initial.swipeDownAction)
        assertEquals(500L, initial.longPressDurationMs)
        assertFalse(initial.dndAutoExpandOverride)
        assertTrue(initial.hideSensitiveOnKeyguard)
        assertEquals(NotchDisappearMode.AUTO, initial.notchDisappearMode)
        assertFalse(initial.expandSilentNotifications)
    }

    @Test
    fun updateGesturePreferences_persistsAllValues() = testScope.runTest {
        val updated = GesturePreferences(
            singleTapAction = GestureAction.TORCH,
            longPressAction = GestureAction.CAMERA,
            swipeLeftAction = GestureAction.CLEAR_ALL,
            swipeRightAction = GestureAction.SETTINGS,
            swipeDownAction = GestureAction.SCREENSHOT,
            longPressDurationMs = 600L,
            dndAutoExpandOverride = true,
            hideSensitiveOnKeyguard = false,
            notchDisappearMode = NotchDisappearMode.NEVER,
            expandSilentNotifications = true
        )

        repository.updateGesturePreferences(updated)
        val read = repository.gesturePreferencesFlow.first()

        assertEquals(GestureAction.TORCH, read.singleTapAction)
        assertEquals(GestureAction.CAMERA, read.longPressAction)
        assertEquals(GestureAction.CLEAR_ALL, read.swipeLeftAction)
        assertEquals(GestureAction.SETTINGS, read.swipeRightAction)
        assertEquals(GestureAction.SCREENSHOT, read.swipeDownAction)
        assertEquals(600L, read.longPressDurationMs)
        assertTrue(read.dndAutoExpandOverride)
        assertFalse(read.hideSensitiveOnKeyguard)
        assertEquals(NotchDisappearMode.NEVER, read.notchDisappearMode)
        assertTrue(read.expandSilentNotifications)
    }

    @Test
    fun individualActionSetters_persistCorrectly() = testScope.runTest {
        repository.setSingleTapAction(GestureAction.CAMERA)
        assertEquals(GestureAction.CAMERA, repository.gesturePreferencesFlow.first().singleTapAction)

        repository.setLongPressAction(GestureAction.TORCH)
        assertEquals(GestureAction.TORCH, repository.gesturePreferencesFlow.first().longPressAction)

        repository.setSwipeLeftAction(GestureAction.EXPAND_COLLAPSE)
        assertEquals(GestureAction.EXPAND_COLLAPSE, repository.gesturePreferencesFlow.first().swipeLeftAction)

        repository.setSwipeRightAction(GestureAction.NOTIFICATIONS_HUB)
        assertEquals(GestureAction.NOTIFICATIONS_HUB, repository.gesturePreferencesFlow.first().swipeRightAction)

        repository.setSwipeDownAction(GestureAction.CLEAR_ALL)
        assertEquals(GestureAction.CLEAR_ALL, repository.gesturePreferencesFlow.first().swipeDownAction)

        repository.setDndAutoExpandOverride(true)
        assertTrue(repository.gesturePreferencesFlow.first().dndAutoExpandOverride)

        repository.setHideSensitiveOnKeyguard(false)
        assertFalse(repository.gesturePreferencesFlow.first().hideSensitiveOnKeyguard)

        repository.setNotchDisappearMode(NotchDisappearMode.ON_READ)
        assertEquals(NotchDisappearMode.ON_READ, repository.gesturePreferencesFlow.first().notchDisappearMode)

        repository.setExpandSilentNotifications(true)
        assertTrue(repository.gesturePreferencesFlow.first().expandSilentNotifications)
    }

    @Test
    fun corruptedOrUnknownEnumStrings_fallBackSafelyToDefaults() = testScope.runTest {
        testDataStore.edit { preferences ->
            preferences[IslandPreferencesRepository.GesturePreferencesKeys.SINGLE_TAP_ACTION] = "UNKNOWN_ACTION_XYZ"
            preferences[IslandPreferencesRepository.GesturePreferencesKeys.NOTCH_DISAPPEAR_MODE] = "INVALID_MODE_123"
        }

        val read = repository.gesturePreferencesFlow.first()
        assertEquals(GestureAction.EXPAND_COLLAPSE, read.singleTapAction)
        assertEquals(NotchDisappearMode.AUTO, read.notchDisappearMode)
    }

    @Test
    fun resetGesturePreferences_restoresDefaults() = testScope.runTest {
        repository.setSingleTapAction(GestureAction.TORCH)
        repository.setDndAutoExpandOverride(true)
        assertEquals(GestureAction.TORCH, repository.gesturePreferencesFlow.first().singleTapAction)

        repository.resetGesturePreferencesToDefaults()
        val read = repository.gesturePreferencesFlow.first()

        assertEquals(GestureAction.EXPAND_COLLAPSE, read.singleTapAction)
        assertFalse(read.dndAutoExpandOverride)
    }

    @Test
    fun islandEvents_hasIdenticalBusinessPayload_comparesCorrectly() {
        val ringer1 = IslandEvent.RingerModeChanged(ringerMode = RingerState.SILENT)
        val ringer2 = IslandEvent.RingerModeChanged(ringerMode = RingerState.SILENT)
        val ringer3 = IslandEvent.RingerModeChanged(ringerMode = RingerState.NORMAL)
        assertTrue(ringer1.hasIdenticalBusinessPayload(ringer2))
        assertFalse(ringer1.hasIdenticalBusinessPayload(ringer3))

        val torch1 = IslandEvent.TorchStateChanged(isEnabled = true)
        val torch2 = IslandEvent.TorchStateChanged(isEnabled = true)
        val torch3 = IslandEvent.TorchStateChanged(isEnabled = false)
        assertTrue(torch1.hasIdenticalBusinessPayload(torch2))
        assertFalse(torch1.hasIdenticalBusinessPayload(torch3))

        val wifi1 = IslandEvent.WifiStateChanged(isConnected = true, ssid = "Nothing_5G")
        val wifi2 = IslandEvent.WifiStateChanged(isConnected = true, ssid = "Nothing_5G")
        val wifi3 = IslandEvent.WifiStateChanged(isConnected = false, ssid = null)
        assertTrue(wifi1.hasIdenticalBusinessPayload(wifi2))
        assertFalse(wifi1.hasIdenticalBusinessPayload(wifi3))

        val dnd1 = IslandEvent.DndStateChanged(isEnabled = true)
        val dnd2 = IslandEvent.DndStateChanged(isEnabled = true)
        val dnd3 = IslandEvent.DndStateChanged(isEnabled = false)
        assertTrue(dnd1.hasIdenticalBusinessPayload(dnd2))
        assertFalse(dnd1.hasIdenticalBusinessPayload(dnd3))

        val bat1 = IslandEvent.BatteryThresholdWarning(batteryLevel = 20, isCritical = false)
        val bat2 = IslandEvent.BatteryThresholdWarning(batteryLevel = 20, isCritical = false)
        val bat3 = IslandEvent.BatteryThresholdWarning(batteryLevel = 10, isCritical = true)
        assertTrue(bat1.hasIdenticalBusinessPayload(bat2))
        assertFalse(bat1.hasIdenticalBusinessPayload(bat3))
    }
}
