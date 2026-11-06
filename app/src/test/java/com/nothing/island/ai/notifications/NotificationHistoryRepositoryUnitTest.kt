package com.nothing.island.ai.notifications

import android.app.Notification
import com.nothing.island.ai.engine.IslandEvent
import com.nothing.island.ai.engine.IslandPriority
import com.nothing.island.ai.engine.IslandPriorityManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * NotificationHistoryRepositoryUnitTest
 *
 * Comprehensive unit test suite for Milestone R6 Phase 3.3:
 * In-Memory Notification History & Decoupled PendingIntent Lifecycle.
 *
 * Verifies:
 * - 30-entry capacity limit and FIFO tail eviction
 * - Reverse-chronological ordering (newest first)
 * - In-place updates preserving firstPostTime and moving to head
 * - Event freshness rules (sourcePostTime and ingressSequence)
 * - Ambiguous update/removal race arbitration (conservative resurrection)
 * - Lock-free action bridge unregistration
 * - Privacy-safe projections across lock-screen transitions
 * - VISIBILITY_SECRET temporary suppression without in-memory deletion
 * - Monotonic generation tokens and stale-cleanup protection
 * - Decoupled action retention across timeout expiration and preemption
 * - Popup deadline preservation during in-place notification updates
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NotificationHistoryRepositoryUnitTest {

    private lateinit var repository: NotificationHistoryRepository
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @Before
    fun setUp() {
        NotificationActionBridge.clearAll()
        repository = NotificationHistoryRepositoryImpl(NotificationActionBridge)
    }

    @After
    fun tearDown() {
        NotificationActionBridge.clearAll()
        repository.clearAll()
    }

    // 1. Basic Ingestion & Storage
    @Test
    fun testRecordSingleNotification() {
        val committed = repository.recordNotification(
            key = "0|com.whatsapp|1|null|1001",
            packageName = "com.whatsapp",
            appDisplayName = "WhatsApp",
            title = "Alice",
            text = "Hey there!",
            sourcePostTime = 1000L,
            ingestSequence = 1L
        )

        assertTrue(committed)
        assertEquals(1, repository.size)
        val entry = repository.getEntry("0|com.whatsapp|1|null|1001")
        assertNotNull(entry)
        assertEquals("WhatsApp", entry?.appDisplayName)
        assertEquals("Alice", entry?.rawTitle)
        assertEquals("Hey there!", entry?.rawText)
        assertEquals(1000L, entry?.sourcePostTime)
        assertEquals(1L, entry?.ingestSequence)
        assertTrue(entry?.isSystemActive ?: false)
    }

    // 2. Capacity Enforcement (Max 30 items)
    @Test
    fun testCapacityEnforcementCapAt30() {
        for (i in 1..35) {
            repository.recordNotification(
                key = "key_$i",
                packageName = "pkg_$i",
                appDisplayName = "App $i",
                title = "Title $i",
                text = "Message $i",
                sourcePostTime = i * 1000L,
                ingestSequence = i.toLong()
            )
        }

        assertEquals(30, repository.size)
        // First 5 entries (key_1 .. key_5) should have been evicted (oldest first)
        for (i in 1..5) {
            assertNull("key_$i should be evicted", repository.getEntry("key_$i"))
        }
        // Entries 6..35 should remain in history
        for (i in 6..35) {
            assertNotNull("key_$i should exist", repository.getEntry("key_$i"))
        }
    }

    // 3. Newest-First Ordering
    @Test
    fun testNewestFirstOrdering() {
        repository.recordNotification(
            key = "first",
            packageName = "pkg",
            appDisplayName = "App",
            title = "First",
            text = "1",
            sourcePostTime = 1000L,
            ingestSequence = 1L
        )
        repository.recordNotification(
            key = "second",
            packageName = "pkg",
            appDisplayName = "App",
            title = "Second",
            text = "2",
            sourcePostTime = 2000L,
            ingestSequence = 2L
        )
        repository.recordNotification(
            key = "third",
            packageName = "pkg",
            appDisplayName = "App",
            title = "Third",
            text = "3",
            sourcePostTime = 3000L,
            ingestSequence = 3L
        )

        val history = repository.history.value
        assertEquals(3, history.size)
        assertEquals("third", history[0].key)
        assertEquals("second", history[1].key)
        assertEquals("first", history[2].key)
    }

    // 4. In-Place Update Preserves FirstPostTime and Increments UpdateCount
    @Test
    fun testInPlaceUpdatePreservesFirstPostTime() {
        val originalFirstPostTime = 123456L
        repository.recordNotification(
            key = "key_chat",
            packageName = "com.whatsapp",
            appDisplayName = "WhatsApp",
            title = "Alice",
            text = "Initial message",
            postTime = originalFirstPostTime,
            sourcePostTime = 1000L,
            ingestSequence = 1L
        )

        val initialEntry = repository.getEntry("key_chat")
        assertNotNull(initialEntry)
        assertEquals(originalFirstPostTime, initialEntry?.firstPostTime)
        assertEquals(0, initialEntry?.updateCount)

        // Post update with new content and newer timestamp
        val updateTime = 123999L
        val updated = repository.recordNotification(
            key = "key_chat",
            packageName = "com.whatsapp",
            appDisplayName = "WhatsApp",
            title = "Alice",
            text = "Updated message",
            postTime = updateTime,
            sourcePostTime = 2000L,
            ingestSequence = 2L
        )

        assertTrue(updated)
        assertEquals(1, repository.size)
        val updatedEntry = repository.getEntry("key_chat")
        assertNotNull(updatedEntry)
        assertEquals("Updated message", updatedEntry?.rawText)
        assertEquals(originalFirstPostTime, updatedEntry?.firstPostTime) // Preserved!
        assertEquals(updateTime, updatedEntry?.lastUpdateTime)
        assertEquals(1, updatedEntry?.updateCount)
    }

    // 5. In-Place Update Moves Entry to the Front
    @Test
    fun testInPlaceUpdateMovesToFront() {
        repository.recordNotification(key = "k1", packageName = "p", appDisplayName = "A", title = "T1", text = "1", sourcePostTime = 100L, ingestSequence = 1L)
        repository.recordNotification(key = "k2", packageName = "p", appDisplayName = "A", title = "T2", text = "2", sourcePostTime = 200L, ingestSequence = 2L)
        repository.recordNotification(key = "k3", packageName = "p", appDisplayName = "A", title = "T3", text = "3", sourcePostTime = 300L, ingestSequence = 3L)

        // Initial list: k3, k2, k1
        assertEquals("k3", repository.history.value[0].key)
        assertEquals("k2", repository.history.value[1].key)
        assertEquals("k1", repository.history.value[2].key)

        // Update k1
        repository.recordNotification(key = "k1", packageName = "p", appDisplayName = "A", title = "T1 updated", text = "1 renewed", sourcePostTime = 400L, ingestSequence = 4L)

        // New list order: k1 moved to front! k1, k3, k2
        val updatedHistory = repository.history.value
        assertEquals("k1", updatedHistory[0].key)
        assertEquals("k3", updatedHistory[1].key)
        assertEquals("k2", updatedHistory[2].key)
    }

    // 6. Event Freshness Rejects Stale sourcePostTime
    @Test
    fun testEventFreshnessRejectsStalePostTime() {
        repository.recordNotification(
            key = "msg_1",
            packageName = "p",
            appDisplayName = "A",
            title = "Newer message",
            text = "Body newer",
            sourcePostTime = 5000L,
            ingestSequence = 2L
        )

        // Stale callback with older sourcePostTime (4000L < 5000L)
        val accepted = repository.recordNotification(
            key = "msg_1",
            packageName = "p",
            appDisplayName = "A",
            title = "Older delayed message",
            text = "Body older",
            sourcePostTime = 4000L,
            ingestSequence = 3L
        )

        assertFalse("Stale sourcePostTime must be rejected", accepted)
        val stored = repository.getEntry("msg_1")
        assertEquals("Newer message", stored?.rawTitle)
        assertEquals("Body newer", stored?.rawText)
        assertEquals(5000L, stored?.sourcePostTime)
    }

    // 7. Event Freshness Rejects Stale Ingress Sequence When sourcePostTime is Equal
    @Test
    fun testEventFreshnessRejectsStaleIngressSequenceWhenPostTimeEqual() {
        repository.recordNotification(
            key = "msg_2",
            packageName = "p",
            appDisplayName = "A",
            title = "Original Title",
            text = "Original Text",
            sourcePostTime = 3000L,
            ingestSequence = 10L
        )

        // Callback with equal postTime (3000L) but smaller sequence (9L < 10L)
        val accepted = repository.recordNotification(
            key = "msg_2",
            packageName = "p",
            appDisplayName = "A",
            title = "Stale Title",
            text = "Stale Text",
            sourcePostTime = 3000L,
            ingestSequence = 9L
        )

        assertFalse("Stale sequence with equal postTime must be rejected", accepted)
        assertEquals("Original Title", repository.getEntry("msg_2")?.rawTitle)
    }

    // 8. Event Freshness Accepts Newer Ingress Sequence When sourcePostTime is Equal
    @Test
    fun testEventFreshnessAcceptsNewerIngressSequenceWhenPostTimeEqual() {
        repository.recordNotification(
            key = "msg_3",
            packageName = "p",
            appDisplayName = "A",
            title = "Initial Title",
            text = "Initial Text",
            sourcePostTime = 3000L,
            ingestSequence = 10L
        )

        // Callback with equal postTime (3000L) and greater sequence (11L > 10L)
        val accepted = repository.recordNotification(
            key = "msg_3",
            packageName = "p",
            appDisplayName = "A",
            title = "Updated Title",
            text = "Updated Text",
            sourcePostTime = 3000L,
            ingestSequence = 11L
        )

        assertTrue("Newer sequence with equal postTime must be accepted", accepted)
        assertEquals("Updated Title", repository.getEntry("msg_3")?.rawTitle)
    }

    // 9. Conservative Resurrection After Removal
    @Test
    fun testResurrectionAfterRemovalRequiresStrictlyNewerPostTime() {
        repository.recordNotification(
            key = "race_key",
            packageName = "p",
            appDisplayName = "A",
            title = "Active",
            text = "T",
            sourcePostTime = 5000L,
            ingestSequence = 1L
        )

        // System notification removed with removal timestamp 5000L
        repository.markSystemNotificationRemoved("race_key", removalPostTime = 5000L, removalSequence = 2L)
        assertFalse(repository.getEntry("race_key")?.isSystemActive ?: true)

        // An update arrives with postTime 5000L (equal to removalPostTime) -> ambiguous race, rejected!
        val rejectedSameTime = repository.recordNotification(
            key = "race_key",
            packageName = "p",
            appDisplayName = "A",
            title = "Resurrect attempt",
            text = "T",
            sourcePostTime = 5000L,
            ingestSequence = 3L
        )
        assertFalse("Resurrection with postTime <= removalPostTime must be rejected", rejectedSameTime)
        assertFalse(repository.getEntry("race_key")?.isSystemActive ?: true)

        // A truly fresh update arrives with strictly newer postTime (6000L > 5000L) -> accepted!
        val acceptedNewer = repository.recordNotification(
            key = "race_key",
            packageName = "p",
            appDisplayName = "A",
            title = "True Resurrected",
            text = "T",
            sourcePostTime = 6000L,
            ingestSequence = 4L
        )
        assertTrue("Resurrection with postTime > removalPostTime must succeed", acceptedNewer)
        assertTrue(repository.getEntry("race_key")?.isSystemActive ?: false)
    }

    // 10. Mark System Notification Removed Retains Entry and Actions
    @Test
    fun testMarkSystemNotificationRemovedRetainsEntryAndActions() {
        val gen = NotificationActionBridge.register(
            key = "notif_retain",
            packageName = "p",
            contentIntent = null,
            token = 1000L
        )

        repository.recordNotification(
            key = "notif_retain",
            packageName = "p",
            appDisplayName = "A",
            title = "Persistent",
            text = "Body",
            sourcePostTime = 1000L,
            actionGeneration = gen
        )

        // Mark removed from shade
        repository.markSystemNotificationRemoved("notif_retain", removalPostTime = 1500L, removalSequence = 2L)

        // History entry still exists!
        assertEquals(1, repository.size)
        val entry = repository.getEntry("notif_retain")
        assertNotNull(entry)
        assertFalse("isSystemActive should be false", entry?.isSystemActive ?: true)

        // ActionBridge still retains actions!
        assertNotNull("Actions must survive system notification removal for history interaction", NotificationActionBridge.get("notif_retain"))
        assertEquals(1, NotificationActionBridge.getCachedCount())
    }

    // 11. Explicit Remove Entry Releases Actions
    @Test
    fun testExplicitRemoveEntryReleasesActions() {
        val gen = NotificationActionBridge.register(
            key = "to_delete",
            packageName = "p",
            contentIntent = null,
            token = 1000L
        )

        repository.recordNotification(
            key = "to_delete",
            packageName = "p",
            appDisplayName = "A",
            title = "T",
            text = "B",
            actionGeneration = gen
        )
        assertEquals(1, repository.size)
        assertEquals(1, NotificationActionBridge.getCachedCount())

        val removed = repository.removeEntry("to_delete")
        assertTrue(removed)
        assertEquals(0, repository.size)
        assertNull(repository.getEntry("to_delete"))
        // Lock-free invariant: ActionBridge unregisters entry
        assertNull(NotificationActionBridge.get("to_delete"))
        assertEquals(0, NotificationActionBridge.getCachedCount())
    }

    // 12. Clear All Purges History and Releases Actions
    @Test
    fun testClearAllPurgesHistoryAndReleasesActions() {
        for (i in 1..5) {
            val gen = NotificationActionBridge.register(key = "k_$i", packageName = "p", contentIntent = null, token = i.toLong())
            repository.recordNotification(key = "k_$i", packageName = "p", appDisplayName = "A", title = "T$i", text = "B$i", actionGeneration = gen)
        }
        assertEquals(5, repository.size)
        assertEquals(5, NotificationActionBridge.getCachedCount())

        repository.clearAll()
        assertEquals(0, repository.size)
        assertEquals(0, NotificationActionBridge.getCachedCount())
    }

    // 13. Hidden Preview Mode Rejects History Entry
    @Test
    fun testHiddenPreviewModeRejectsHistoryEntry() {
        val committed = repository.recordNotification(
            key = "hidden_key",
            packageName = "p",
            appDisplayName = "A",
            title = "T",
            text = "B",
            previewMode = NotificationPreviewMode.HIDDEN
        )
        assertFalse("Preview mode HIDDEN must be rejected from history", committed)
        assertEquals(0, repository.size)
    }

    // 14. Privacy-Safe Projection: FULL Preview Mode Unlocked
    @Test
    fun testToPrivacySafePresentationFullUnlocked() {
        val entry = NotificationHistoryEntry(
            key = "k",
            packageName = "com.example",
            appDisplayName = "ExampleApp",
            rawTitle = "Meeting at 3",
            rawText = "Please join the conference room",
            firstPostTime = 1000L,
            lastUpdateTime = 1000L,
            previewMode = NotificationPreviewMode.FULL
        )

        val presentation = entry.toPrivacySafePresentation(
            isKeyguardLocked = false,
            preferences = NotificationPreferences.DEFAULTS
        )

        assertNotNull(presentation)
        assertEquals("Meeting at 3", presentation?.title)
        assertEquals("Please join the conference room", presentation?.body)
        assertFalse(presentation?.isMasked ?: true)
    }

    // 15. Privacy-Safe Projection: TITLE_ONLY
    @Test
    fun testToPrivacySafePresentationTitleOnly() {
        val entry = NotificationHistoryEntry(
            key = "k",
            packageName = "com.example",
            appDisplayName = "ExampleApp",
            rawTitle = "Meeting at 3",
            rawText = "Secret location",
            firstPostTime = 1000L,
            lastUpdateTime = 1000L,
            previewMode = NotificationPreviewMode.TITLE_ONLY
        )

        val presentation = entry.toPrivacySafePresentation(
            isKeyguardLocked = false,
            preferences = NotificationPreferences.DEFAULTS
        )

        assertNotNull(presentation)
        assertEquals("Meeting at 3", presentation?.title)
        assertEquals("", presentation?.body) // Body hidden!
        assertFalse(presentation?.isMasked ?: true)
    }

    // 16. Privacy-Safe Projection: APP_ONLY
    @Test
    fun testToPrivacySafePresentationAppOnly() {
        val entry = NotificationHistoryEntry(
            key = "k",
            packageName = "com.example",
            appDisplayName = "ExampleApp",
            rawTitle = "Meeting at 3",
            rawText = "Secret message",
            firstPostTime = 1000L,
            lastUpdateTime = 1000L,
            previewMode = NotificationPreviewMode.APP_ONLY
        )

        val presentation = entry.toPrivacySafePresentation(
            isKeyguardLocked = false,
            preferences = NotificationPreferences.DEFAULTS
        )

        assertNotNull(presentation)
        assertEquals("ExampleApp", presentation?.title)
        assertEquals("New notification", presentation?.body)
        assertTrue(presentation?.isMasked ?: false)
    }

    // 17. Privacy-Safe Projection: Keyguard Locked Masks Sensitive Content
    @Test
    fun testToPrivacySafePresentationKeyguardLockedMasksSensitive() {
        val entry = NotificationHistoryEntry(
            key = "k",
            packageName = "com.bank",
            appDisplayName = "BankApp",
            rawTitle = "OTP: 123456",
            rawText = "Your security code is 123456",
            firstPostTime = 1000L,
            lastUpdateTime = 1000L,
            visibility = 0 // Public
        )

        val presentation = entry.toPrivacySafePresentation(
            isKeyguardLocked = true,
            preferences = NotificationPreferences.DEFAULTS.copy(hideSensitiveContent = true)
        )

        assertNotNull(presentation)
        assertEquals("BankApp", presentation?.title)
        assertEquals("New notification", presentation?.body)
        assertTrue(presentation?.isMasked ?: false)
    }

    // 18. Privacy-Safe Projection: Keyguard Locked Masks VISIBILITY_PRIVATE
    @Test
    fun testToPrivacySafePresentationKeyguardLockedMasksPrivateVisibility() {
        val entry = NotificationHistoryEntry(
            key = "k",
            packageName = "com.chat",
            appDisplayName = "ChatApp",
            rawTitle = "Private Chat",
            rawText = "Confidential conversation",
            firstPostTime = 1000L,
            lastUpdateTime = 1000L,
            visibility = Notification.VISIBILITY_PRIVATE
        )

        val presentation = entry.toPrivacySafePresentation(
            isKeyguardLocked = true,
            preferences = NotificationPreferences.DEFAULTS.copy(hideSensitiveContent = false)
        )

        assertNotNull(presentation)
        assertEquals("ChatApp", presentation?.title)
        assertEquals("New notification", presentation?.body)
        assertTrue(presentation?.isMasked ?: false)
    }

    // 19. Privacy-Safe Projection: VISIBILITY_SECRET Suppressed on Lock Screen Without Deletion
    @Test
    fun testToPrivacySafePresentationSecretSuppressedOnKeyguardLockWithoutDeletion() {
        val entry = NotificationHistoryEntry(
            key = "k_secret",
            packageName = "com.security",
            appDisplayName = "SecurityApp",
            rawTitle = "Two-Factor Alert",
            rawText = "Critical security notification",
            firstPostTime = 1000L,
            lastUpdateTime = 1000L,
            visibility = Notification.VISIBILITY_SECRET
        )

        // When locked -> presentation is null (temporarily suppressed from UI)
        val lockedPresentation = entry.toPrivacySafePresentation(
            isKeyguardLocked = true,
            preferences = NotificationPreferences.DEFAULTS
        )
        assertNull("VISIBILITY_SECRET must return null on lock screen", lockedPresentation)

        // In repository: entry remains fully intact in memory!
        repository.recordNotification(
            key = "k_secret",
            packageName = "com.security",
            appDisplayName = "SecurityApp",
            title = "Two-Factor Alert",
            text = "Critical security notification",
            visibility = Notification.VISIBILITY_SECRET
        )
        assertNotNull("Entry must still reside in repository memory while keyguard is locked", repository.getEntry("k_secret"))

        // When unlocked -> presentation returns immediately without data loss!
        val unlockedPresentation = entry.toPrivacySafePresentation(
            isKeyguardLocked = false,
            preferences = NotificationPreferences.DEFAULTS
        )
        assertNotNull(unlockedPresentation)
        assertEquals("Two-Factor Alert", unlockedPresentation?.title)
        assertEquals("Critical security notification", unlockedPresentation?.body)
    }

    // 20. ActionBridge Generation Monotonicity
    @Test
    fun testActionBridgeGenerationMonotonicity() {
        val gen1 = NotificationActionBridge.register(key = "k1", packageName = "p", contentIntent = null)
        val gen2 = NotificationActionBridge.register(key = "k2", packageName = "p", contentIntent = null)
        val gen3 = NotificationActionBridge.register(key = "k1", packageName = "p", contentIntent = null)

        assertTrue(gen2 > gen1)
        assertTrue(gen3 > gen2)
    }

    // 21. ActionBridge Unregister With Matching Generation Succeeds
    @Test
    fun testActionBridgeUnregisterWithMatchingGenerationSucceeds() {
        val gen = NotificationActionBridge.register(key = "key_x", packageName = "p", contentIntent = null)
        assertEquals(1, NotificationActionBridge.getCachedCount())

        val unregistered = NotificationActionBridge.unregisterIfGenerationMatches("key_x", gen)
        assertTrue(unregistered)
        assertEquals(0, NotificationActionBridge.getCachedCount())
    }

    // 22. ActionBridge Unregister With Stale Generation Fails
    @Test
    fun testActionBridgeUnregisterWithStaleGenerationFails() {
        val genOld = NotificationActionBridge.register(key = "key_y", packageName = "p", contentIntent = null)
        val genNew = NotificationActionBridge.register(key = "key_y", packageName = "p", contentIntent = null)

        // Attempting to unregister using the old generation must fail and leave the newer entry intact
        val unregistered = NotificationActionBridge.unregisterIfGenerationMatches("key_y", genOld)
        assertFalse(unregistered)
        assertEquals(1, NotificationActionBridge.getCachedCount())
        assertEquals(genNew, NotificationActionBridge.get("key_y")?.generation)
    }

    // 23. Eviction From History Repository Releases Actions
    @Test
    fun testTailEvictionReleasesMatchingActions() {
        // Fill to capacity 30
        for (i in 1..30) {
            val gen = NotificationActionBridge.register(key = "evict_$i", packageName = "p", contentIntent = null, token = i.toLong())
            repository.recordNotification(key = "evict_$i", packageName = "p", appDisplayName = "A", title = "T", text = "B", sourcePostTime = i * 100L, actionGeneration = gen)
        }
        assertEquals(30, repository.size)
        assertEquals(30, NotificationActionBridge.getCachedCount())

        // Insert 31st item -> oldest ("evict_1") must be evicted and its actions released
        val gen31 = NotificationActionBridge.register(key = "evict_31", packageName = "p", contentIntent = null, token = 3100L)
        repository.recordNotification(key = "evict_31", packageName = "p", appDisplayName = "A", title = "T", text = "B", sourcePostTime = 3100L, actionGeneration = gen31)

        assertEquals(30, repository.size)
        assertNull(repository.getEntry("evict_1"))
        assertNull("Evicted notification actions must be released from ActionBridge", NotificationActionBridge.get("evict_1"))
        assertEquals(30, NotificationActionBridge.getCachedCount())
    }

    // 24. Action Rollback When History Commit Fails
    @Test
    fun testActionRollbackWhenHistoryCommitFails() {
        // Record initial notification with postTime = 5000L
        val genInitial = NotificationActionBridge.register(key = "fail_key", packageName = "p", contentIntent = null)
        repository.recordNotification(key = "fail_key", packageName = "p", appDisplayName = "A", title = "T", text = "B", sourcePostTime = 5000L, actionGeneration = genInitial)

        // An update arrives, register actions -> gets new generation
        val genNew = NotificationActionBridge.register(key = "fail_key", packageName = "p", contentIntent = null)
        // Commit is attempted with stale postTime 4000L -> recordNotification returns false!
        val committed = repository.recordNotification(key = "fail_key", packageName = "p", appDisplayName = "A", title = "Stale", text = "Stale", sourcePostTime = 4000L, actionGeneration = genNew)
        assertFalse(committed)

        // Listener service executes rollback safeguard
        NotificationActionBridge.unregisterIfGenerationMatches("fail_key", genNew)

        // Stored entry remains untouched
        assertEquals(5000L, repository.getEntry("fail_key")?.sourcePostTime)
    }

    // 25. In-Place Notification Update Preserves Active Timeout Deadline
    @Test
    fun testInPlaceNotificationUpdatePreservesPopupDeadline() = testScope.runTest {
        var resolvedPresentationId: String? = null
        val priorityManager = IslandPriorityManager(testScope) { _, _, presId ->
            resolvedPresentationId = presId
        }

        val key = "0|com.whatsapp|100|null|5555"
        val originalEvent = IslandEvent.NotificationPosted(
            id = key,
            packageName = "com.whatsapp",
            title = "Alice",
            text = "First line",
            timeoutMs = 5000L
        )

        priorityManager.processEvent(originalEvent)
        testScheduler.advanceTimeBy(50L)
        val initialPresId = resolvedPresentationId
        assertEquals(key, priorityManager.activeEvent?.id)

        // Fast forward 3000ms (2000ms left before original 5000ms deadline)
        testScheduler.advanceTimeBy(3000L)

        // In-place update arrives for the same key
        val updatedEvent = IslandEvent.NotificationPosted(
            id = key,
            packageName = "com.whatsapp",
            title = "Alice",
            text = "Second line added",
            timeoutMs = 5000L
        )
        priorityManager.processEvent(updatedEvent)
        testScheduler.advanceTimeBy(50L)

        // Content updated in-place
        assertEquals("Second line added", (priorityManager.activeEvent as? IslandEvent.NotificationPosted)?.text)
        // Presentation ID remains identical (not canceled/restarted)
        assertEquals(initialPresId, resolvedPresentationId)

        // Fast forward another 2000ms -> original 5000ms deadline expires now!
        testScheduler.advanceTimeBy(2000L)
        assertNull("Active presentation must expire at original deadline without extension", priorityManager.activeEvent)
    }
}
