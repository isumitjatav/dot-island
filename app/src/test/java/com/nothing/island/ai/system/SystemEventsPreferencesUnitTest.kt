package com.nothing.island.ai.system

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.nothing.island.ai.data.IslandPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
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
class SystemEventsPreferencesUnitTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private lateinit var testScope: TestScope
    private lateinit var repoScope: CoroutineScope
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var repository: IslandPreferencesRepository

    @Before
    fun setUp() {
        val dispatcher = StandardTestDispatcher()
        testScope = TestScope(dispatcher)
        repoScope = CoroutineScope(SupervisorJob() + dispatcher)
        val file: File = temporaryFolder.newFile("test_system_events.preferences_pb")
        dataStore = PreferenceDataStoreFactory.create(scope = repoScope, produceFile = { file })
        repository = IslandPreferencesRepository.createForTesting(dataStore, repoScope)
    }

    @After
    fun tearDown() {
        repository.close()
        repoScope.cancel()
    }

    @Test
    fun defaults_enable_all_feedback_capsules() = testScope.runTest {
        val prefs = repository.systemEventsPreferencesFlow.first()
        assertTrue(prefs.systemEventsEnabled)
        assertTrue(prefs.ringerCapsuleEnabled)
        assertTrue(prefs.silentModeCapsuleEnabled)
        assertTrue(prefs.ringModeCapsuleEnabled)
        assertTrue(prefs.deviceUnlockCapsuleEnabled)
        assertTrue(prefs.flashlightCapsuleEnabled)
        assertTrue(prefs.wifiCapsuleEnabled)
        assertTrue(prefs.zenModeCapsuleEnabled)
        assertTrue(prefs.batteryWarningCapsuleEnabled)
    }

    @Test
    fun updateSystemEventsPreferences_persistsAllValues() = testScope.runTest {
        val updated = SystemEventsPreferences(
            systemEventsEnabled = true,
            ringerCapsuleEnabled = false,
            silentModeCapsuleEnabled = false,
            ringModeCapsuleEnabled = true,
            deviceUnlockCapsuleEnabled = false,
            flashlightCapsuleEnabled = false,
            wifiCapsuleEnabled = false,
            zenModeCapsuleEnabled = false,
            batteryWarningCapsuleEnabled = true,
        )
        repository.updateSystemEventsPreferences(updated)

        val persisted = repository.systemEventsPreferencesFlow.first()
        assertEquals(updated, persisted)
    }

    @Test
    fun livePreferences_updateImmediatelyForZeroLatencyUI() = testScope.runTest {
        repository.updateLiveSystemEventsPreferences(
            SystemEventsPreferences(systemEventsEnabled = false)
        )
        assertFalse(repository.liveSystemEventsPreferences.value.systemEventsEnabled)
        assertTrue(repository.liveSystemEventsPreferences.value.wifiCapsuleEnabled)
    }
}
