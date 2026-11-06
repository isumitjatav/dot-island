package com.nothing.island.ai.notifications

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import com.nothing.island.ai.engine.DownloadProgressState
import com.nothing.island.ai.engine.IslandPriority
import com.nothing.island.ai.ui.components.NotificationCardResolver
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * NotificationFinalBatchUnitTest
 *
 * Exhaustive regression and validation test suite for R6 Final Implementation Batch:
 * 1. Inline Notification Replies (RemoteInput detection, reply dispatching, dead intent handling, zero persistence)
 * 2. Download Progress Tracking (Extraction, determinate/indeterminate/paused/completed/failed state computation, ongoing exemption)
 * 3. Grouping & In-Place Updates (Group summary suppression vs standalone, in-place deduplicated update stream)
 * 4. Readability & Formatting (Resolver typography, text wrapping, and progress projection)
 */
class NotificationFinalBatchUnitTest {

    private lateinit var filter: NotificationFilter
    private lateinit var repository: NotificationHistoryRepositoryImpl

    @Before
    fun setUp() {
        filter = NotificationFilter(hostPackageName = "com.nothing.island.ai")
        repository = NotificationHistoryRepositoryImpl()
        NotificationActionBridge.clearAll()
    }

    @After
    fun tearDown() {
        NotificationActionBridge.clearAll()
        repository.clearAll()
        filter.reset()
    }

    // =========================================================================
    // 1. INLINE NOTIFICATION REPLIES (RemoteInput)
    // =========================================================================

    @Test
    fun testRemoteInputActionDetection() {
        val replyAction = NotificationActionItem(
            title = "Reply",
            actionIntent = null,
            isReply = true,
            remoteInputs = null
        )
        val regularAction = NotificationActionItem(
            title = "Mark as Read",
            actionIntent = null,
            isReply = false,
            remoteInputs = null
        )

        assertTrue("Reply action must flag isReply=true", replyAction.isReply)
        assertFalse("Regular action must flag isReply=false", regularAction.isReply)
    }

    @Test
    fun testSendReplyToNonexistentNotificationReturnsNotFound() {
        val result = NotificationActionBridge.sendReply(
            context = null,
            key = "non_existent_key",
            actionIndex = 0,
            replyText = "Hello"
        )

        assertEquals("Missing key must yield ReplyResult.NOT_FOUND", ReplyResult.NOT_FOUND, result)
    }

    @Test
    fun testSendReplyToNonReplyActionReturnsNotReply() {
        val key = "msg_key_1"
        val regularAction = NotificationActionItem(
            title = "Archive",
            actionIntent = null,
            isReply = false
        )

        NotificationActionBridge.register(
            key = key,
            packageName = "com.google.android.gm",
            contentIntent = null,
            actions = listOf(regularAction)
        )

        val result = NotificationActionBridge.sendReply(
            context = null,
            key = key,
            actionIndex = 0,
            replyText = "Hello"
        )

        assertEquals("Invoking sendReply on non-reply action must yield ReplyResult.NOT_REPLY", ReplyResult.NOT_REPLY, result)
    }

    @Test
    fun testZeroReplyTextPersistence() {
        // Verify domain models do NOT contain fields to persist private user reply text
        val entry = NotificationHistoryEntry(
            key = "test_key",
            packageName = "com.whatsapp",
            appDisplayName = "WhatsApp",
            rawTitle = "Alice",
            rawText = "See you soon",
            firstPostTime = 1000L,
            lastUpdateTime = 1000L
        )

        val presentation = entry.toPrivacySafePresentation(
            isKeyguardLocked = false,
            preferences = NotificationPreferences.DEFAULTS
        )

        assertNotNull(presentation)
        // Check declared fields of NotificationHistoryEntry via reflection to guarantee zero reply text storage
        val entryFieldNames = NotificationHistoryEntry::class.java.declaredFields.map { it.name }
        assertFalse("NotificationHistoryEntry must never store reply text", entryFieldNames.contains("replyText"))
        assertFalse("NotificationHistoryEntry must never store userReply", entryFieldNames.contains("userReply"))

        val presentationFieldNames = PrivacySafeNotificationPresentation::class.java.declaredFields.map { it.name }
        assertFalse("PrivacySafeNotificationPresentation must never store reply text", presentationFieldNames.contains("replyText"))
    }

    // =========================================================================
    // 2. DOWNLOAD PROGRESS TRACKING
    // =========================================================================

    @Test
    fun testDeterminateDownloadProgressState() {
        val raw = RawNotificationData(
            key = "download_1",
            packageName = "com.android.providers.downloads",
            appLabel = "Downloads",
            title = "ubuntu-24.04.iso",
            text = "Downloading... 45%",
            category = Notification.CATEGORY_PROGRESS,
            isOngoing = true,
            progress = 45,
            maxProgress = 100,
            isIndeterminate = false,
            hasProgress = true,
            progressState = DownloadProgressState.DOWNLOADING
        )

        val event = filter.filter(raw, NotificationPreferences.DEFAULTS)
        assertNotNull(event)
        assertEquals("Download event priority must be NOTIFICATION", IslandPriority.NOTIFICATION, event!!.priority)
        assertEquals("Actively downloading events must have timeoutMs=0 (persistent)", 0L, event.timeoutMs)
        assertTrue("Event must flag hasProgress=true", event.hasProgress)
        assertEquals(45, event.progress)
        assertEquals(100, event.maxProgress)
        assertEquals(DownloadProgressState.DOWNLOADING, event.progressState)
    }

    @Test
    fun testIndeterminateDownloadProgressState() {
        val raw = RawNotificationData(
            key = "download_indet",
            packageName = "com.google.android.apps.docs",
            appLabel = "Drive",
            title = "Uploading video",
            text = "Preparing upload...",
            category = Notification.CATEGORY_PROGRESS,
            isOngoing = true,
            progress = 0,
            maxProgress = 0,
            isIndeterminate = true,
            hasProgress = true,
            progressState = DownloadProgressState.INDETERMINATE
        )

        val event = filter.filter(raw, NotificationPreferences.DEFAULTS)
        assertNotNull(event)
        assertEquals(0L, event!!.timeoutMs)
        assertTrue(event.isIndeterminate)
        assertEquals(DownloadProgressState.INDETERMINATE, event.progressState)
    }

    @Test
    fun testPausedDownloadState() {
        val raw = RawNotificationData(
            key = "download_paused",
            packageName = "com.android.providers.downloads",
            appLabel = "Downloads",
            title = "game_assets.zip",
            text = "Download paused - Wi-Fi required",
            category = Notification.CATEGORY_PROGRESS,
            isOngoing = false,
            progress = 60,
            maxProgress = 100,
            isIndeterminate = false,
            hasProgress = true,
            progressState = DownloadProgressState.PAUSED
        )

        val event = filter.filter(raw, NotificationPreferences.DEFAULTS)
        assertNotNull(event)
        assertEquals(DownloadProgressState.PAUSED, event!!.progressState)
        assertEquals("Paused download should obey normal display timeout", 4500L, event.timeoutMs)
    }

    @Test
    fun testCompletedDownloadState() {
        val raw = RawNotificationData(
            key = "download_done",
            packageName = "com.android.providers.downloads",
            appLabel = "Downloads",
            title = "document.pdf",
            text = "Download complete",
            category = Notification.CATEGORY_PROGRESS,
            isOngoing = false,
            progress = 100,
            maxProgress = 100,
            isIndeterminate = false,
            hasProgress = true,
            progressState = DownloadProgressState.COMPLETED
        )

        val event = filter.filter(raw, NotificationPreferences.DEFAULTS)
        assertNotNull(event)
        assertEquals(DownloadProgressState.COMPLETED, event!!.progressState)
    }

    @Test
    fun testFailedDownloadState() {
        val raw = RawNotificationData(
            key = "download_failed",
            packageName = "com.android.providers.downloads",
            appLabel = "Downloads",
            title = "large_archive.tar.gz",
            text = "Download failed: storage full",
            category = Notification.CATEGORY_PROGRESS,
            isOngoing = false,
            progress = 20,
            maxProgress = 100,
            isIndeterminate = false,
            hasProgress = true,
            progressState = DownloadProgressState.FAILED
        )

        val event = filter.filter(raw, NotificationPreferences.DEFAULTS)
        assertNotNull(event)
        assertEquals(DownloadProgressState.FAILED, event!!.progressState)
    }

    @Test
    fun testDownloadOngoingNotificationExemptedFromOngoingSuppression() {
        // By default, allowOngoingNotifications is FALSE
        val prefs = NotificationPreferences(allowOngoingNotifications = false)

        val downloadRaw = RawNotificationData(
            key = "dl_ongoing",
            packageName = "com.example.downloader",
            appLabel = "Downloader",
            title = "firmware.bin",
            text = "Downloading... 15%",
            isOngoing = true,
            hasProgress = true,
            progress = 15,
            maxProgress = 100,
            progressState = DownloadProgressState.DOWNLOADING
        )

        val result = filter.filter(downloadRaw, prefs)
        assertNotNull("Active download must be exempted from generic ongoing suppression", result)
        assertEquals(DownloadProgressState.DOWNLOADING, result!!.progressState)

        // Non-download generic ongoing notification MUST still be dropped
        val genericOngoingRaw = RawNotificationData(
            key = "generic_ongoing",
            packageName = "com.example.service",
            appLabel = "Service",
            title = "Background Service",
            text = "Service is active",
            isOngoing = true,
            hasProgress = false
        )

        val droppedResult = filter.filter(genericOngoingRaw, prefs)
        assertNull("Generic ongoing service must be dropped when allowOngoingNotifications=false", droppedResult)
    }

    // =========================================================================
    // 3. GROUPING & IN-PLACE DEDUPLICATED UPDATES
    // =========================================================================

    @Test
    fun testGroupSummarySuppressedWhenActiveChildExists() {
        val groupKey = "0|com.whatsapp|family_chat"

        val child1 = RawNotificationData(
            key = "child_1",
            packageName = "com.whatsapp",
            title = "Alice",
            text = "Dinner is ready",
            isGroupSummary = false,
            groupKey = groupKey
        )
        val summary = RawNotificationData(
            key = "summary_1",
            packageName = "com.whatsapp",
            title = "WhatsApp",
            text = "2 new messages",
            isGroupSummary = true,
            groupKey = groupKey
        )

        // First child notification allowed
        val childResult = filter.filter(child1, NotificationPreferences.DEFAULTS)
        assertNotNull("Child alert must be allowed", childResult)

        // Subsequent group summary for the same group MUST be suppressed
        val summaryResult = filter.filter(summary, NotificationPreferences.DEFAULTS)
        assertNull("Group summary must be suppressed when active child notifications exist", summaryResult)
    }

    @Test
    fun testStandaloneGroupSummaryAllowedWhenNoActiveChildrenExist() {
        val groupKey = "0|com.whatsapp|work_chat"

        val summary = RawNotificationData(
            key = "summary_standalone",
            packageName = "com.whatsapp",
            title = "WhatsApp",
            text = "3 new messages from Work",
            isGroupSummary = true,
            groupKey = groupKey
        )

        val result = filter.filter(summary, NotificationPreferences.DEFAULTS)
        assertNotNull("Standalone group summary without prior active child alerts must be allowed", result)
    }

    @Test
    fun testInPlaceDownloadProgressUpdateDoesNotDuplicateOrReorderHistory() {
        val key = "download_repo_test"

        // Step 1: Initial download post at 10%
        val firstCommit = repository.recordNotification(
            key = key,
            packageName = "com.android.providers.downloads",
            appDisplayName = "Downloads",
            title = "big_file.zip",
            text = "10%",
            postTime = 1000L,
            sourcePostTime = 1000L,
            ingestSequence = 1L,
            isOngoing = true,
            hasProgress = true,
            progress = 10,
            maxProgress = 100,
            progressState = DownloadProgressState.DOWNLOADING
        )
        assertTrue(firstCommit)
        assertEquals(1, repository.size)

        // Add a second unrelated notification
        repository.recordNotification(
            key = "other_alert",
            packageName = "com.google.android.gm",
            appDisplayName = "Gmail",
            title = "Invoice",
            text = "Your bill",
            postTime = 2000L,
            sourcePostTime = 2000L,
            ingestSequence = 2L
        )
        assertEquals(2, repository.size)

        // The order is now: other_alert (index 0), big_file.zip (index 1)
        val initialHistory = repository.history.value
        assertEquals("other_alert", initialHistory[0].key)
        assertEquals("download_repo_test", initialHistory[1].key)

        // Step 2: Download progress tick to 50%
        val updateCommit = repository.recordNotification(
            key = key,
            packageName = "com.android.providers.downloads",
            appDisplayName = "Downloads",
            title = "big_file.zip",
            text = "50%",
            postTime = 3000L,
            sourcePostTime = 3000L,
            ingestSequence = 3L,
            isOngoing = true,
            hasProgress = true,
            progress = 50,
            maxProgress = 100,
            progressState = DownloadProgressState.DOWNLOADING
        )
        assertTrue(updateCommit)
        assertEquals("History size must not grow on progress update (zero duplicates)", 2, repository.size)

        // Verify in-place update: progress updated, entry retained at index 1 without jarring reshuffle
        val updatedHistory = repository.history.value
        assertEquals("other_alert", updatedHistory[0].key)
        assertEquals("download_repo_test", updatedHistory[1].key)
        assertEquals(50, updatedHistory[1].progress)
        assertEquals(DownloadProgressState.DOWNLOADING, updatedHistory[1].progressState)
    }

    // =========================================================================
    // 4. READABILITY & NOTIFICATION CARD RESOLVER
    // =========================================================================

    @Test
    fun testNotificationCardResolverWithProgressMetadata() {
        val state = NotificationCardResolver.resolve(
            packageName = "com.android.providers.downloads",
            rawTitle = "backup.tar.gz",
            rawText = "Downloading archive",
            timestamp = 1000L,
            previewMode = NotificationPreviewMode.FULL,
            appLabel = "Downloads",
            currentTimeMillis = 2000L,
            progress = 75,
            maxProgress = 100,
            hasProgress = true,
            progressState = DownloadProgressState.DOWNLOADING
        )

        assertTrue(state.shouldRender)
        assertEquals("Downloads", state.appDisplayName)
        assertEquals("backup.tar.gz", state.title)
        assertEquals("Downloading archive", state.body)
        assertTrue(state.hasProgress)
        assertEquals(75, state.progress)
        assertEquals(100, state.maxProgress)
        assertEquals(DownloadProgressState.DOWNLOADING, state.progressState)
    }

    @Test
    fun testNotificationCardResolverSuppressionUnderHiddenPreviewMode() {
        val state = NotificationCardResolver.resolve(
            packageName = "com.whatsapp",
            rawTitle = "Alice",
            rawText = "Secret message",
            previewMode = NotificationPreviewMode.HIDDEN
        )

        assertFalse("HIDDEN preview mode must yield shouldRender=false", state.shouldRender)
        assertNull(state.title)
        assertNull(state.body)
    }
}
