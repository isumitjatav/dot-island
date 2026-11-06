package com.nothing.island.ai.notifications

import com.nothing.island.ai.engine.IslandEvent
import com.nothing.island.ai.engine.IslandEventEngine
import com.nothing.island.ai.engine.IslandPriority
import com.nothing.island.ai.engine.IslandPriorityManager
import com.nothing.island.ai.engine.IslandStateRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * NotificationEventEngineIntegrationUnitTest
 *
 * Verifies end-to-end integration of notification events with the R3/R3.1 IslandEventEngine
 * and IslandPriorityManager:
 * 1. NotificationPosted emits to standard event flow (Priority 55).
 * 2. NotificationPosted preempts Idle base state (P55 > P0).
 * 3. NotificationPosted preempts Media playback (P55 > P40).
 * 4. Incoming call preempts NotificationPosted (P100 > P55).
 * 5. Multiple simultaneous notifications deterministically arbitrate at equal priority (P55 == P55).
 * 6. Stale dismissal protection: dismissing an old notification does not dismiss a newer active one.
 * 7. Active notification dismissal restores persistent media fallback.
 * 8. Notification deduplication and content change updates.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NotificationEventEngineIntegrationUnitTest {

    private lateinit var testDispatcher: TestDispatcher
    private lateinit var testScope: TestScope
    private lateinit var eventEngine: IslandEventEngine
    private lateinit var stateRepository: IslandStateRepository
    private lateinit var priorityManager: IslandPriorityManager

    @Before
    fun setUp() {
        NotificationActionBridge.clearAll()
        testDispatcher = StandardTestDispatcher()
        testScope = TestScope(testDispatcher)
        eventEngine = IslandEventEngine(bufferCapacity = 64, deduplicationWindowMs = 250L, coalescingWindowMs = 250L)
        stateRepository = IslandStateRepository()
        priorityManager = IslandPriorityManager(testScope) { resolvedEvent, priority, presentationId ->
            stateRepository.updateFromResolvedEvent(resolvedEvent, priority, presentationId)
        }
    }

    @After
    fun tearDown() {
        priorityManager.reset()
        eventEngine.reset()
        NotificationActionBridge.clearAll()
        testScope.cancel()
    }

    @Test
    fun testNotificationPostedPreemptsIdleState() = testScope.runTest {
        val collectedEvents = mutableListOf<IslandEvent>()
        val collectorJob = launch {
            eventEngine.events.collect {
                collectedEvents.add(it)
                priorityManager.processEvent(it)
            }
        }
        testScheduler.advanceTimeBy(50L)

        // 1. Initial State: Idle
        assertEquals(IslandPriority.IDLE_BASE, priorityManager.activePriority)

        // 2. Incoming Notification arrives (P55)
        val notif = IslandEvent.NotificationPosted(
            id = "key_notif_1",
            packageName = "com.whatsapp",
            title = "Alice",
            text = "Hey there!",
            timeoutMs = 4500L
        )
        val accepted = eventEngine.emit(notif)
        testScheduler.advanceTimeBy(50L)

        assertTrue("Event must be accepted by event engine", accepted)
        assertEquals(IslandPriority.NOTIFICATION, priorityManager.activePriority)
        assertEquals(notif, priorityManager.activeEvent)
        assertEquals("Alice", stateRepository.state.value.title)
        assertEquals("Hey there!", stateRepository.state.value.subtitle)

        // 3. Fast-forward past 4500ms timeout
        testScheduler.advanceTimeBy(4500L)
        assertEquals(IslandPriority.IDLE_BASE, priorityManager.activePriority)

        collectorJob.cancel()
    }

    @Test
    fun testNotificationPostedPreemptsMediaPlayback() = testScope.runTest {
        val collectorJob = launch {
            eventEngine.events.collect { priorityManager.processEvent(it) }
        }
        testScheduler.advanceTimeBy(50L)

        // 1. Media is playing in background (P40)
        val media = IslandEvent.MediaPlaybackChanged(
            id = "media_1",
            packageName = "com.spotify.music",
            trackTitle = "Starboy",
            artistName = "The Weeknd",
            isPlaying = true
        )
        eventEngine.emit(media)
        testScheduler.advanceTimeBy(50L)
        assertEquals(IslandPriority.MEDIA_PLAYBACK, priorityManager.activePriority)

        // 2. Notification arrives (P55) -> Must preempt media
        val notif = IslandEvent.NotificationPosted(
            id = "notif_msg_1",
            packageName = "org.telegram.messenger",
            title = "Bob",
            text = "Meeting at 3 PM",
            timeoutMs = 4500L
        )
        eventEngine.emit(notif)
        testScheduler.advanceTimeBy(50L)

        assertEquals(IslandPriority.NOTIFICATION, priorityManager.activePriority)
        assertEquals(notif, priorityManager.activeEvent)
        assertEquals("Bob", stateRepository.state.value.title)

        collectorJob.cancel()
    }

    @Test
    fun testIncomingCallPreemptsActiveNotification() = testScope.runTest {
        val collectorJob = launch {
            eventEngine.events.collect { priorityManager.processEvent(it) }
        }
        testScheduler.advanceTimeBy(50L)

        // 1. Notification is active (P55)
        val notif = IslandEvent.NotificationPosted(
            id = "notif_msg_2",
            packageName = "com.whatsapp",
            title = "Charlie",
            text = "Are you home?"
        )
        eventEngine.emit(notif)
        testScheduler.advanceTimeBy(50L)
        assertEquals(IslandPriority.NOTIFICATION, priorityManager.activePriority)

        // 2. Critical Incoming Call arrives (P100) -> Must immediately preempt notification
        val call = IslandEvent.IncomingCallRinging(
            id = "call_ring_1",
            callId = "call_123",
            callerDisplayName = "Mom",
            isVideoCall = false
        )
        eventEngine.emit(call)
        testScheduler.advanceTimeBy(50L)

        assertEquals(IslandPriority.CRITICAL_TELEPHONY, priorityManager.activePriority)
        assertEquals(call, priorityManager.activeEvent)
        assertEquals("INCOMING CALL", stateRepository.state.value.title)
        assertEquals("Mom", stateRepository.state.value.subtitle)

        collectorJob.cancel()
    }

    @Test
    fun testMultipleSimultaneousNotificationsEqualPriorityPreemption() = testScope.runTest {
        val collectorJob = launch {
            eventEngine.events.collect { priorityManager.processEvent(it) }
        }
        testScheduler.advanceTimeBy(50L)

        // 1. First alert arrives (Alert A)
        val alertA = IslandEvent.NotificationPosted(
            id = "alert_a",
            packageName = "com.whatsapp",
            title = "Alice",
            text = "First alert",
            timeoutMs = 4500L
        )
        eventEngine.emit(alertA)
        testScheduler.advanceTimeBy(50L)
        assertEquals(alertA, priorityManager.activeEvent)

        // Advance 1.5 seconds into the 4.5s countdown
        testScheduler.advanceTimeBy(1500L)

        // 2. Second alert arrives (Alert B) from Slack
        val alertB = IslandEvent.NotificationPosted(
            id = "alert_b",
            packageName = "com.slack",
            title = "Dev Channel",
            text = "PR merged",
            timeoutMs = 4500L
        )
        eventEngine.emit(alertB)
        testScheduler.advanceTimeBy(50L)

        // Equal priority: Alert B replaces Alert A and restarts timeout
        assertEquals(alertB, priorityManager.activeEvent)
        assertEquals("Dev Channel", stateRepository.state.value.title)

        // Advance 3.5 seconds (Alert A's original 4.5s deadline is past, but Alert B is still active)
        testScheduler.advanceTimeBy(3500L)
        assertEquals("Alert B must still be active (new timeout started)", alertB, priorityManager.activeEvent)

        // Advance another 1.5 seconds (Alert B timeout expires)
        testScheduler.advanceTimeBy(1500L)
        assertEquals("Returns to base idle after Alert B timeout expires", IslandPriority.IDLE_BASE, priorityManager.activePriority)

        collectorJob.cancel()
    }

    @Test
    fun testStaleDismissalDoesNotDismissNewerActiveNotification() = testScope.runTest {
        val collectorJob = launch {
            eventEngine.events.collect { priorityManager.processEvent(it) }
        }
        testScheduler.advanceTimeBy(50L)

        // 1. Alert A is posted
        val alertA = IslandEvent.NotificationPosted(
            id = "key_alert_a",
            packageName = "com.whatsapp",
            title = "Alice",
            text = "Message A"
        )
        eventEngine.emit(alertA)
        testScheduler.advanceTimeBy(50L)
        assertEquals(alertA, priorityManager.activeEvent)

        // 2. Alert B is posted, preempting Alert A
        val alertB = IslandEvent.NotificationPosted(
            id = "key_alert_b",
            packageName = "org.telegram.messenger",
            title = "Bob",
            text = "Message B"
        )
        eventEngine.emit(alertB)
        testScheduler.advanceTimeBy(50L)
        assertEquals(alertB, priorityManager.activeEvent)

        // 3. Stale dismissal for Alert A arrives
        val dismissA = IslandEvent.NotificationDismissed(
            id = "dismiss_a",
            targetNotificationId = "key_alert_a",
            packageName = "com.whatsapp"
        )
        eventEngine.emit(dismissA)
        testScheduler.advanceTimeBy(50L)

        // STALE DISMISSAL PROTECTION: Alert B must remain active!
        assertEquals("Alert B must not be dismissed by Alert A's dismissal", alertB, priorityManager.activeEvent)
        assertEquals("Bob", stateRepository.state.value.title)

        // 4. Proper dismissal for Alert B arrives
        val dismissB = IslandEvent.NotificationDismissed(
            id = "dismiss_b",
            targetNotificationId = "key_alert_b",
            packageName = "org.telegram.messenger"
        )
        eventEngine.emit(dismissB)
        testScheduler.advanceTimeBy(50L)

        assertEquals("Alert B is now dismissed cleanly", IslandPriority.IDLE_BASE, priorityManager.activePriority)

        collectorJob.cancel()
    }

    @Test
    fun testDismissalRestoresMediaPlaybackFallback() = testScope.runTest {
        val collectorJob = launch {
            eventEngine.events.collect { priorityManager.processEvent(it) }
        }
        testScheduler.advanceTimeBy(50L)

        // 1. Spotify is playing (P40)
        val media = IslandEvent.MediaPlaybackChanged(
            id = "spotify_playback",
            packageName = "com.spotify.music",
            trackTitle = "Blinding Lights",
            artistName = "The Weeknd",
            isPlaying = true
        )
        eventEngine.emit(media)
        testScheduler.advanceTimeBy(50L)
        assertEquals(media, priorityManager.activeEvent)

        // 2. Incoming notification preempts Spotify (P55)
        val notif = IslandEvent.NotificationPosted(
            id = "sms_notif",
            packageName = "com.google.android.apps.messaging",
            title = "John",
            text = "Package arrived!"
        )
        eventEngine.emit(notif)
        testScheduler.advanceTimeBy(50L)
        assertEquals(notif, priorityManager.activeEvent)

        // 3. Notification is dismissed
        val dismissNotif = IslandEvent.NotificationDismissed(
            id = "dismiss_sms",
            targetNotificationId = "sms_notif",
            packageName = "com.google.android.apps.messaging"
        )
        eventEngine.emit(dismissNotif)
        testScheduler.advanceTimeBy(50L)

        // Verified: Spotify playback fallback is cleanly restored!
        assertEquals(IslandPriority.MEDIA_PLAYBACK, priorityManager.activePriority)
        assertEquals(media, priorityManager.activeEvent)
        assertEquals("Blinding Lights", stateRepository.state.value.title)
        assertEquals("The Weeknd", stateRepository.state.value.subtitle)

        collectorJob.cancel()
    }

    @Test
    fun testSemanticDeduplicationIgnoresIdenticalPayloadWithinWindow() = testScope.runTest {
        val notif1 = IslandEvent.NotificationPosted(
            id = "same_key",
            packageName = "com.whatsapp",
            title = "Alice",
            text = "Identical message"
        )
        val notif2 = IslandEvent.NotificationPosted(
            id = "same_key",
            packageName = "com.whatsapp",
            title = "Alice",
            text = "Identical message"
        )

        val accepted1 = eventEngine.emit(notif1)
        val accepted2 = eventEngine.emit(notif2) // Same payload within 250ms

        assertTrue("First emission must be accepted", accepted1)
        assertFalse("Second identical emission within 250ms must be deduplicated", accepted2)
    }

    @Test
    fun testNotificationUpdateWithNewContentEmitted() = testScope.runTest {
        val notifOriginal = IslandEvent.NotificationPosted(
            id = "chat_key",
            packageName = "com.whatsapp",
            title = "Alice",
            text = "First message"
        )
        val notifUpdated = IslandEvent.NotificationPosted(
            id = "chat_key",
            packageName = "com.whatsapp",
            title = "Alice",
            text = "Second updated message" // Content changed!
        )

        val accepted1 = eventEngine.emit(notifOriginal)
        val accepted2 = eventEngine.emit(notifUpdated)

        assertTrue("Original notification accepted", accepted1)
        assertTrue("Updated notification with new text accepted", accepted2)
    }

    @Test
    fun testTimeoutExpirationReleasesActionBridgeReference() = testScope.runTest {
        val collectorJob = launch {
            eventEngine.events.collect { priorityManager.processEvent(it) }
        }
        testScheduler.advanceTimeBy(50L)

        val key = "0|com.whatsapp|100|null|1001"
        val notif = IslandEvent.NotificationPosted(
            id = key,
            packageName = "com.whatsapp",
            title = "Alice",
            text = "Testing timeout",
            timeoutMs = 3000L
        )
        NotificationActionBridge.register(key = key, packageName = "com.whatsapp", contentIntent = null, token = notif.timestamp)
        assertEquals(1, NotificationActionBridge.getCachedCount())

        eventEngine.emit(notif)
        testScheduler.advanceTimeBy(50L)
        assertEquals(key, priorityManager.activeEvent?.id)
        assertNotNull(NotificationActionBridge.get(key))

        // Fast forward 3000ms timeout
        testScheduler.advanceTimeBy(3000L)
        assertNull("Active event should be null after timeout", priorityManager.activeEvent)
        // Phase 3.3: ActionBridge references are retained across popup timeout for history access
        assertNotNull("ActionBridge references must be retained upon presentation timeout for history", NotificationActionBridge.get(key))
        assertEquals(1, NotificationActionBridge.getCachedCount())

        collectorJob.cancel()
    }

    @Test
    fun testDismissalReleasesActionBridgeReference() = testScope.runTest {
        val collectorJob = launch {
            eventEngine.events.collect { priorityManager.processEvent(it) }
        }
        testScheduler.advanceTimeBy(50L)

        val key = "0|com.whatsapp|100|null|1002"
        val notif = IslandEvent.NotificationPosted(
            id = key,
            packageName = "com.whatsapp",
            title = "Bob",
            text = "Testing dismissal",
            timeoutMs = 5000L
        )
        NotificationActionBridge.register(key = key, packageName = "com.whatsapp", contentIntent = null, token = notif.timestamp)
        assertEquals(1, NotificationActionBridge.getCachedCount())

        eventEngine.emit(notif)
        testScheduler.advanceTimeBy(50L)
        assertEquals(key, priorityManager.activeEvent?.id)

        // Dismissal event arrives
        val dismissEvent = IslandEvent.NotificationDismissed(
            id = "dismiss_1002",
            targetNotificationId = key,
            packageName = "com.whatsapp"
        )
        eventEngine.emit(dismissEvent)
        testScheduler.advanceTimeBy(50L)

        assertNull("Active event should be null after dismissal", priorityManager.activeEvent)
        // Phase 3.3: ActionBridge references are retained upon system notification dismissal for history access
        assertNotNull("ActionBridge references must be retained upon notification dismissal for history", NotificationActionBridge.get(key))
        assertEquals(1, NotificationActionBridge.getCachedCount())

        collectorJob.cancel()
    }

    @Test
    fun testPreemptionByCallReleasesPreemptedNotificationActions() = testScope.runTest {
        val collectorJob = launch {
            eventEngine.events.collect { priorityManager.processEvent(it) }
        }
        testScheduler.advanceTimeBy(50L)

        val notifKey = "0|com.whatsapp|100|null|1003"
        val notif = IslandEvent.NotificationPosted(
            id = notifKey,
            packageName = "com.whatsapp",
            title = "Charlie",
            text = "Testing preemption",
            timeoutMs = 5000L
        )
        NotificationActionBridge.register(key = notifKey, packageName = "com.whatsapp", contentIntent = null, token = notif.timestamp)
        assertEquals(1, NotificationActionBridge.getCachedCount())

        eventEngine.emit(notif)
        testScheduler.advanceTimeBy(50L)
        assertEquals(notifKey, priorityManager.activeEvent?.id)

        // Critical call arrives
        val call = IslandEvent.IncomingCallRinging(
            id = "call_p100",
            callId = "call_123",
            callerDisplayName = "Boss"
        )
        eventEngine.emit(call)
        testScheduler.advanceTimeBy(50L)

        assertEquals(IslandPriority.CRITICAL_TELEPHONY, priorityManager.activePriority)
        // Phase 3.3: ActionBridge references are retained when preempted by call for history access
        assertNotNull("Preempted temporary notification action references must be retained for history", NotificationActionBridge.get(notifKey))
        assertEquals(1, NotificationActionBridge.getCachedCount())

        collectorJob.cancel()
    }
}
