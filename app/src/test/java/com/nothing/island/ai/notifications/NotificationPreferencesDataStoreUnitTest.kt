package com.nothing.island.ai.notifications

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.nothing.island.ai.data.IslandPreferencesRepository
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

/**
 * NotificationPreferencesDataStoreUnitTest
 *
 * Verifies persistent DataStore storage of all Milestone R6 notification customization options:
 * - Master enable/disable
 * - Display duration
 * - Preview modes (FULL, TITLE_ONLY, APP_ONLY, HIDDEN)
 * - Compact expansion toggle
 * - Silent and ongoing notification preferences
 * - Lockscreen and sensitive-content masking
 * - Per-app exclusions (blacklist) and toggle operations
 * - Safe IOException fallback resilience
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NotificationPreferencesDataStoreUnitTest {

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
        testDataStoreFile = temporaryFolder.newFile("test_notif_preferences.preferences_pb")
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
    fun testInitialNotificationPreferencesMatchDefaults() = testScope.runTest {
        val initial = repository.notificationPreferencesFlow.first()

        assertTrue("Notifications enabled by default", initial.enabled)
        assertEquals(4500L, initial.displayDurationMs)
        assertEquals(NotificationPreviewMode.FULL, initial.previewMode)
        assertTrue(initial.compactExpansionEnabled)
        assertTrue("Swipe to dismiss enabled by default", initial.swipeToDismissEnabled)
        assertFalse(initial.allowSilentNotifications)
        assertFalse(initial.allowOngoingNotifications)
        assertFalse(initial.showOnLockscreen)
        assertTrue(initial.hideSensitiveContent)
        assertEquals(NotificationAnimationStyle.NOTHING_SNAP, initial.animationStyle)
        assertTrue(initial.excludedPackages.isEmpty())
    }

    @Test
    fun testUpdateNotificationEnabledPersists() = testScope.runTest {
        repository.updateNotificationEnabled(false)
        testScheduler.advanceUntilIdle()

        val updated = repository.notificationPreferencesFlow.first()
        assertFalse(updated.enabled)
    }

    @Test
    fun testUpdateNotificationDurationPersists() = testScope.runTest {
        repository.updateNotificationDuration(7500L)
        testScheduler.advanceUntilIdle()

        val updated = repository.notificationPreferencesFlow.first()
        assertEquals(7500L, updated.displayDurationMs)
    }

    @Test
    fun testUpdateNotificationPreviewModePersists() = testScope.runTest {
        repository.updateNotificationPreviewMode(NotificationPreviewMode.TITLE_ONLY)
        testScheduler.advanceUntilIdle()

        val updated = repository.notificationPreferencesFlow.first()
        assertEquals(NotificationPreviewMode.TITLE_ONLY, updated.previewMode)
    }

    @Test
    fun testUpdateNotificationOngoingAndSilentPersists() = testScope.runTest {
        repository.updateNotificationAllowOngoing(true)
        repository.updateNotificationAllowSilent(true)
        testScheduler.advanceUntilIdle()

        val updated = repository.notificationPreferencesFlow.first()
        assertTrue(updated.allowOngoingNotifications)
        assertTrue(updated.allowSilentNotifications)
    }

    @Test
    fun testUpdateNotificationSwipeToDismissPersists() = testScope.runTest {
        repository.updateNotificationSwipeDismiss(false)
        testScheduler.advanceUntilIdle()

        val updated = repository.notificationPreferencesFlow.first()
        assertFalse(updated.swipeToDismissEnabled)

        repository.updateNotificationSwipeDismiss(true)
        testScheduler.advanceUntilIdle()

        val restored = repository.notificationPreferencesFlow.first()
        assertTrue(restored.swipeToDismissEnabled)
    }

    @Test
    fun testUpdateNotificationLockscreenAndSensitivePersists() = testScope.runTest {
        repository.updateNotificationShowOnLockscreen(true)
        repository.updateNotificationHideSensitive(false)
        testScheduler.advanceUntilIdle()

        val updated = repository.notificationPreferencesFlow.first()
        assertTrue(updated.showOnLockscreen)
        assertFalse(updated.hideSensitiveContent)
    }

    @Test
    fun testUpdateNotificationExcludedPackagesAndTogglePersists() = testScope.runTest {
        // 1. Initial exclusion set
        repository.updateNotificationExcludedPackages(setOf("com.spam.ads", "com.game.alert"))
        testScheduler.advanceUntilIdle()

        val prefs1 = repository.notificationPreferencesFlow.first()
        assertEquals(2, prefs1.excludedPackages.size)
        assertTrue(prefs1.excludedPackages.contains("com.spam.ads"))

        // 2. Toggle new package
        repository.toggleNotificationPackageExclusion("com.promo.app")
        testScheduler.advanceUntilIdle()

        val prefs2 = repository.notificationPreferencesFlow.first()
        assertEquals(3, prefs2.excludedPackages.size)
        assertTrue(prefs2.excludedPackages.contains("com.promo.app"))

        // 3. Toggle existing package (removes it)
        repository.toggleNotificationPackageExclusion("com.spam.ads")
        testScheduler.advanceUntilIdle()

        val prefs3 = repository.notificationPreferencesFlow.first()
        assertEquals(2, prefs3.excludedPackages.size)
        assertFalse(prefs3.excludedPackages.contains("com.spam.ads"))
    }

    @Test
    fun testNotificationPreferencesIOExceptionFallback() = testScope.runTest {
        val failingDataStore = object : DataStore<Preferences> {
            override val data: Flow<Preferences> = flow {
                throw IOException("Corrupted disk storage read error")
            }
            override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
                throw IOException("Write failure")
            }
        }

        val fallbackRepo = IslandPreferencesRepository.createForTesting(failingDataStore, repoScope)
        try {
            val result = fallbackRepo.notificationPreferencesFlow.first()
            assertTrue("Fallback must retain default enabled", result.enabled)
            assertEquals("Fallback must retain default duration", 4500L, result.displayDurationMs)
            assertEquals("Fallback must retain default preview mode", NotificationPreviewMode.FULL, result.previewMode)
        } finally {
            fallbackRepo.close()
        }
    }
}
