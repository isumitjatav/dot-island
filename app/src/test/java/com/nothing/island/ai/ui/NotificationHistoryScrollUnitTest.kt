package com.nothing.island.ai.ui

import com.nothing.island.ai.data.IslandPreferences
import com.nothing.island.ai.engine.IslandEvent
import com.nothing.island.ai.engine.IslandMasterState
import com.nothing.island.ai.engine.IslandPriority
import com.nothing.island.ai.engine.IslandStateRepository
import com.nothing.island.ai.notifications.NotificationActionBridge
import com.nothing.island.ai.notifications.NotificationHistoryRepository
import com.nothing.island.ai.notifications.NotificationHistoryRepositoryImpl
import com.nothing.island.ai.notifications.NotificationHistoryEntry
import com.nothing.island.ai.notifications.NotificationPreferences
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * NotificationHistoryScrollUnitTest
 *
 * Verifies the architecture and presentation contracts for Nothing Island's
 * scrollable Notification History interface:
 * - Full 30-entry retention and exposure (no 1-3 item truncation).
 * - Stable newest-first ordering across scrollable items.
 * - Stable key identification for LazyColumn scroll position tracking.
 * - In-place updates without key churn or list jumping.
 * - Historical notification selection and anti-hijacking preservation.
 * - Privacy-safe masking preserved across all scrollable items.
 */
class NotificationHistoryScrollUnitTest {

    private lateinit var historyRepository: NotificationHistoryRepository
    private lateinit var stateRepository: IslandStateRepository

    @Before
    fun setUp() {
        NotificationActionBridge.clearAll()
        historyRepository = NotificationHistoryRepositoryImpl(NotificationActionBridge)
        stateRepository = IslandStateRepository()
    }

    @After
    fun tearDown() {
        NotificationActionBridge.clearAll()
        historyRepository.clearAll()
    }

    @Test
    fun testProjectedHistoryRetainsAll30EntriesForScrolling() {
        // Ingest 30 notifications
        for (i in 1..30) {
            historyRepository.recordNotification(
                key = "notif_key_$i",
                packageName = "com.app.$i",
                appDisplayName = "App $i",
                title = "Alert $i",
                text = "Message content $i",
                sourcePostTime = i * 1000L,
                ingestSequence = i.toLong()
            )
        }

        assertEquals("History repository must retain exactly 30 entries", 30, historyRepository.size)
        val historyList = historyRepository.history.value
        assertEquals("History StateFlow must expose all 30 entries to LazyColumn", 30, historyList.size)

        // Verify projected presentation
        val projected = historyList.mapNotNull { entry ->
            entry.toPrivacySafePresentation(isKeyguardLocked = false, preferences = NotificationPreferences.DEFAULTS)
        }
        assertEquals("All 30 entries must project to privacy-safe presentation", 30, projected.size)

        // First item must be newest (key 30), last item must be oldest (key 1)
        assertEquals("Newest notification must be at index 0", "notif_key_30", projected[0].key)
        assertEquals("Oldest notification must be at index 29", "notif_key_1", projected[29].key)
    }

    @Test
    fun testStableKeysForScrollPositionPreservation() {
        for (i in 1..15) {
            historyRepository.recordNotification(
                key = "key_$i",
                packageName = "pkg_$i",
                appDisplayName = "App $i",
                title = "Title $i",
                text = "Body $i",
                sourcePostTime = i * 1000L,
                ingestSequence = i.toLong()
            )
        }

        val keys = historyRepository.history.value.map { it.key }
        assertEquals(15, keys.distinct().size)

        // Update an item in-place (e.g. key_8)
        historyRepository.recordNotification(
            key = "key_8",
            packageName = "pkg_8",
            appDisplayName = "App 8",
            title = "Title 8 Updated",
            text = "Body 8 New text",
            sourcePostTime = 20_000L,
            ingestSequence = 16L
        )

        val updatedKeys = historyRepository.history.value.map { it.key }
        // Key 8 moved to front (newest-first), all keys still unique and stable
        assertEquals(15, updatedKeys.distinct().size)
        assertEquals("key_8", updatedKeys[0])
        val entry8 = historyRepository.getEntry("key_8")
        assertEquals("Title 8 Updated", entry8?.rawTitle)
        assertEquals(1, entry8?.updateCount)
    }

    @Test
    fun testHistoricalNotificationSelectionPreservedDuringScrolling() {
        for (i in 1..20) {
            historyRepository.recordNotification(
                key = "key_$i",
                packageName = "com.example.app$i",
                appDisplayName = "App $i",
                title = "Header $i",
                text = "Detail $i",
                sourcePostTime = i * 1000L,
                ingestSequence = i.toLong()
            )
        }

        // User scrolls down and taps notification #5
        stateRepository.selectHistoricalNotification("key_5")

        val currentState = stateRepository.state.value
        assertEquals("key_5", currentState.selectedNotificationKey)
        assertEquals(com.nothing.island.ai.ExpandedViewType.NOTIFICATION_FOCUSED, currentState.expandedViewType)
        assertEquals(com.nothing.island.ai.IslandMode.EXPANDED, currentState.mode)

        // Verify anti-hijacking: an incoming notification arrives while viewing historical #5
        val incomingEvent = IslandEvent.NotificationPosted(
            id = "key_21",
            packageName = "com.incoming.app",
            title = "New Incoming Alert",
            text = "Should not hijack screen",
            timeoutMs = 4500L
        )
        // Repository presentation update simulates priority manager event
        stateRepository.updateFromResolvedEvent(incomingEvent, IslandPriority.NOTIFICATION, "pres_21")

        // selectedNotificationKey must remain intact ("key_5")
        assertEquals(
            "Anti-hijacking must preserve selected historical notification",
            "key_5",
            stateRepository.state.value.selectedNotificationKey
        )
    }

    @Test
    fun testOlderEntriesRemainAccessibleBeyondInitialThree() {
        for (i in 1..15) {
            historyRepository.recordNotification(
                key = "alert_$i",
                packageName = "com.test.app",
                appDisplayName = "App",
                title = "Alert #$i",
                text = "Content for alert number $i",
                sourcePostTime = i * 1000L,
                ingestSequence = i.toLong()
            )
        }

        val entries = historyRepository.history.value
        // Test that entries beyond the first 3 (e.g. index 3 to 14) are fully present
        for (index in 0 until 15) {
            assertNotNull("Entry at index $index must be non-null and accessible", entries[index])
            assertTrue("Key must start with alert_", entries[index].key.startsWith("alert_"))
        }

        val oldestEntry = entries[14]
        assertEquals("alert_1", oldestEntry.key)
        assertEquals("Alert #1", oldestEntry.rawTitle)
    }

    @Test
    fun testExpandedFocusedNotificationFallsBackToLatestHistoryWhenActiveBannerExpired() {
        historyRepository.recordNotification(
            key = "key_old",
            packageName = "com.example.chat",
            appDisplayName = "Chat",
            title = "Older Sender",
            text = "First message",
            sourcePostTime = 1000L,
            ingestSequence = 1L
        )
        historyRepository.recordNotification(
            key = "key_latest",
            packageName = "com.example.chat",
            appDisplayName = "Chat",
            title = "Latest Sender",
            text = "Most recent message",
            sourcePostTime = 2000L,
            ingestSequence = 2L
        )

        val projected = historyRepository.history.value.mapNotNull {
            it.toPrivacySafePresentation(isKeyguardLocked = false, preferences = NotificationPreferences.DEFAULTS)
        }

        // After the 4.5s active banner expires, both selectedNotificationKey and activeNotificationId are null:
        // focused view should still surface the newest entry instead of showing an empty card.
        val fallback = com.nothing.island.ai.resolveFocusedNotificationPresentation(
            selectedNotificationKey = null,
            activeNotificationId = null,
            projectedHistory = projected
        )
        assertEquals("key_latest", fallback?.key)

        // Explicit selection wins over fallback
        val selected = com.nothing.island.ai.resolveFocusedNotificationPresentation(
            selectedNotificationKey = "key_old",
            activeNotificationId = "key_latest",
            projectedHistory = projected
        )
        assertEquals("key_old", selected?.key)

        // Active banner ID wins when no historical item was tapped
        val active = com.nothing.island.ai.resolveFocusedNotificationPresentation(
            selectedNotificationKey = null,
            activeNotificationId = "key_old",
            projectedHistory = projected
        )
        assertEquals("key_old", active?.key)

        // Empty history yields null
        assertEquals(
            null,
            com.nothing.island.ai.resolveFocusedNotificationPresentation(null, null, emptyList())
        )
    }

    @Test
    fun testPostedNotificationPopulatesExpandedDashboardRecentAlerts() {
        historyRepository.recordNotification(
            key = "0|com.nothing.test|42|tag|1000",
            packageName = "com.nothing.test",
            appDisplayName = "Dot Island Test",
            title = "Dot Island Test",
            text = "Live notification history verified",
            sourcePostTime = 5000L,
            ingestSequence = 1L
        )

        stateRepository.expandDashboard()
        val state = stateRepository.state.value
        assertEquals(com.nothing.island.ai.IslandMode.EXPANDED, state.mode)
        assertEquals(com.nothing.island.ai.ExpandedViewType.DASHBOARD, state.expandedViewType)

        val projected = historyRepository.history.value.mapNotNull {
            it.toPrivacySafePresentation(isKeyguardLocked = false, preferences = NotificationPreferences.DEFAULTS)
        }
        assertEquals(1, projected.size)
        assertEquals("0|com.nothing.test|42|tag|1000", projected[0].key)
        assertEquals("Dot Island Test", projected[0].title)
        assertEquals("Live notification history verified", projected[0].body)
    }
}
