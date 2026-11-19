package com.nothing.island.ai.notifications

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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class HideSystemNotificationUnitTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private lateinit var testScope: TestScope
    private lateinit var repoScope: CoroutineScope
    private lateinit var repository: IslandPreferencesRepository

    @Before
    fun setUp() {
        val dispatcher = StandardTestDispatcher()
        testScope = TestScope(dispatcher)
        repoScope = CoroutineScope(SupervisorJob() + dispatcher)
        val file: File = temporaryFolder.newFile("test_hide_system.preferences_pb")
        val dataStore: DataStore<Preferences> =
            PreferenceDataStoreFactory.create(scope = repoScope, produceFile = { file })
        repository = IslandPreferencesRepository.createForTesting(dataStore, repoScope)
    }

    @After
    fun tearDown() {
        repository.close()
        repoScope.cancel()
    }

    @Test
    fun hideSystemNotification_defaultsToOff() = testScope.runTest {
        assertFalse(repository.notificationPreferencesFlow.first().hideSystemNotification)
    }

    @Test
    fun hideSystemNotification_persistsAcrossReads() = testScope.runTest {
        repository.updateHideSystemNotification(true)
        assertTrue(repository.notificationPreferencesFlow.first().hideSystemNotification)
        assertTrue(repository.liveNotificationPreferences.value.hideSystemNotification)
    }

    /** Mirrors the guard used in IslandNotificationListenerService. */
    private fun shouldCancelSystemAlert(
        prefs: NotificationPreferences,
        accepted: Boolean,
        isClearable: Boolean,
        isOngoing: Boolean,
    ): Boolean = accepted && prefs.hideSystemNotification && isClearable && !isOngoing

    @Test
    fun suppression_requiresPreferenceAndClearableNonOngoing() = testScope.runTest {
        val on = repository.notificationPreferencesFlow.first().copy(hideSystemNotification = true)
        assertTrue(shouldCancelSystemAlert(on, true, isClearable = true, isOngoing = false))
        assertFalse(shouldCancelSystemAlert(on, false, isClearable = true, isOngoing = false))
        assertFalse(shouldCancelSystemAlert(on, true, isClearable = false, isOngoing = false))
        assertFalse(shouldCancelSystemAlert(on, true, isClearable = true, isOngoing = true))

        val off = repository.notificationPreferencesFlow.first().copy(hideSystemNotification = false)
        assertFalse(shouldCancelSystemAlert(off, true, isClearable = true, isOngoing = false))
    }
}
