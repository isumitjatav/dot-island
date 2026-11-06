package com.nothing.island.ai.engine

import com.nothing.island.ai.IslandMode
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class IslandEngineUnitTest {

    private lateinit var testDispatcher: TestDispatcher
    private lateinit var testScope: TestScope
    private lateinit var eventEngine: IslandEventEngine
    private lateinit var stateRepository: IslandStateRepository
    private lateinit var priorityManager: IslandPriorityManager

    @Before
    fun setUp() {
        testDispatcher = StandardTestDispatcher()
        testScope = TestScope(testDispatcher)
        eventEngine = IslandEventEngine(bufferCapacity = 64, deduplicationWindowMs = 250L, coalescingWindowMs = 250L)
        stateRepository = IslandStateRepository()
        priorityManager = IslandPriorityManager(testScope) { resolvedEvent, priority, presentationId ->
            stateRepository.updateFromResolvedEvent(resolvedEvent, priority, presentationId)
        }
    }

    // -------------------------------------------------------------
    // Test 1: Normal event submission and collection
    // -------------------------------------------------------------
    @Test
    fun test1_normalEventSubmissionAndCollection() = testScope.runTest {
        val collectedEvents = mutableListOf<IslandEvent>()
        val job = launch {
            eventEngine.events.collect { collectedEvents.add(it) }
        }
        testScheduler.advanceUntilIdle()

        val event = IslandEvent.NotificationPosted(
            id = "notif_1",
            packageName = "com.whatsapp",
            title = "Alice",
            text = "Hello Nothing Island"
        )
        val emitted = eventEngine.emit(event)
        testScheduler.advanceUntilIdle()

        assertTrue("Event should be successfully emitted", emitted)
        assertEquals(1, collectedEvents.size)
        assertEquals("notif_1", collectedEvents[0].id)
        job.cancel()
    }

    // -------------------------------------------------------------
    // Test 2: Multiple event collectors
    // -------------------------------------------------------------
    @Test
    fun test2_multipleEventCollectors() = testScope.runTest {
        val list1 = mutableListOf<IslandEvent>()
        val list2 = mutableListOf<IslandEvent>()
        val list3 = mutableListOf<IslandEvent>()

        val job1 = launch { eventEngine.events.collect { list1.add(it) } }
        val job2 = launch { eventEngine.events.collect { list2.add(it) } }
        val job3 = launch { eventEngine.events.collect { list3.add(it) } }

        testScheduler.advanceUntilIdle()
        assertEquals(3, eventEngine.subscriptionCount.value)

        val event = IslandEvent.DeviceUnlocked(
            id = "unlock_1",
            unlockMethod = UnlockMethod.FINGERPRINT
        )
        eventEngine.emit(event)
        testScheduler.advanceUntilIdle()

        assertEquals(1, list1.size)
        assertEquals(1, list2.size)
        assertEquals(1, list3.size)
        assertEquals("unlock_1", list1[0].id)
        assertEquals("unlock_1", list2[0].id)
        assertEquals("unlock_1", list3[0].id)

        job1.cancel()
        job2.cancel()
        job3.cancel()
    }

    // -------------------------------------------------------------
    // Test 3: Event deduplication across distinct timestamps (Semantic Deduplication)
    // -------------------------------------------------------------
    @Test
    fun test3_eventDeduplication() = testScope.runTest {
        val collected = mutableListOf<IslandEvent>()
        val job = launch { eventEngine.events.collect { collected.add(it) } }
        testScheduler.advanceUntilIdle()

        // Two events created with differing timestamps (simulating real production runtime)
        val event1 = IslandEvent.NotificationPosted(
            id = "notif_dup",
            timestamp = 1000L,
            packageName = "com.example",
            title = "Test",
            text = "Msg"
        )
        val event2 = IslandEvent.NotificationPosted(
            id = "notif_dup",
            timestamp = 1045L, // 45ms later, different constructor timestamp!
            packageName = "com.example",
            title = "Test",
            text = "Msg"
        )

        val firstEmit = eventEngine.emit(event1)
        val secondEmit = eventEngine.emit(event2) // Identical semantic payload within window
        testScheduler.advanceUntilIdle()

        assertTrue("First emission should succeed", firstEmit)
        assertFalse("Second emission with differing timestamp must be deduplicated via business payload", secondEmit)
        assertEquals(1, collected.size)

        job.cancel()
    }

    // -------------------------------------------------------------
    // Test 4: Event ordering
    // -------------------------------------------------------------
    @Test
    fun test4_eventOrdering() = testScope.runTest {
        val collected = mutableListOf<IslandEvent>()
        val job = launch { eventEngine.events.collect { collected.add(it) } }
        testScheduler.advanceUntilIdle()

        val e1 = IslandEvent.DeviceUnlocked(id = "order_1", unlockMethod = UnlockMethod.FACE)
        val e2 = IslandEvent.BluetoothDeviceConnected(id = "order_2", deviceName = "Ear (a)", deviceAddress = "00:11:22")
        val e3 = IslandEvent.ChargingConnected(id = "order_3", batteryLevel = 80, chargeType = "WIRELESS")

        eventEngine.emit(e1)
        eventEngine.emit(e2)
        eventEngine.emit(e3)
        testScheduler.advanceUntilIdle()

        assertEquals(3, collected.size)
        assertEquals("order_1", collected[0].id)
        assertEquals("order_2", collected[1].id)
        assertEquals("order_3", collected[2].id)

        job.cancel()
    }

    // -------------------------------------------------------------
    // Test 5: High-priority preemption
    // -------------------------------------------------------------
    @Test
    fun test5_highPriorityPreemption() = testScope.runTest {
        val mediaEvent = IslandEvent.MediaPlaybackChanged(
            id = "media_1",
            packageName = "com.spotify",
            trackTitle = "Starman",
            artistName = "David Bowie",
            isPlaying = true
        )
        priorityManager.processEvent(mediaEvent)
        assertEquals(IslandPriority.MEDIA_PLAYBACK, priorityManager.activePriority)
        assertEquals("Starman", stateRepository.state.value.title)

        // Incoming call (Priority 100) arrives -> immediately preempts media (Priority 40)
        val callEvent = IslandEvent.IncomingCallRinging(
            id = "call_1",
            callId = "call_101",
            callerDisplayName = "Carl Pei"
        )
        priorityManager.processEvent(callEvent)

        assertEquals(IslandPriority.CRITICAL_TELEPHONY, priorityManager.activePriority)
        assertEquals("INCOMING CALL", stateRepository.state.value.title)
        assertEquals("Carl Pei", stateRepository.state.value.subtitle)
    }

    // -------------------------------------------------------------
    // Test 6: Low-priority rejection while a higher priority event is active
    // -------------------------------------------------------------
    @Test
    fun test6_lowPriorityRejectionWhileHigherPriorityActive() = testScope.runTest {
        // High priority active: Incoming Call (P100)
        val callEvent = IslandEvent.IncomingCallRinging(
            id = "call_p100",
            callId = "c1",
            callerDisplayName = "Carl Pei"
        )
        priorityManager.processEvent(callEvent)
        assertEquals(IslandPriority.CRITICAL_TELEPHONY, priorityManager.activePriority)

        // Lower priority arrives: Notification (P55)
        val notifEvent = IslandEvent.NotificationPosted(
            id = "notif_p55",
            packageName = "com.slack",
            title = "Standup",
            text = "Starting now"
        )
        priorityManager.processEvent(notifEvent)

        // Call must NOT be overwritten
        assertEquals(IslandPriority.CRITICAL_TELEPHONY, priorityManager.activePriority)
        assertEquals("INCOMING CALL", stateRepository.state.value.title)
        assertEquals("call_p100", priorityManager.activeEvent?.id)
    }

    // -------------------------------------------------------------
    // Test 7: Equal-priority deterministic handling
    // -------------------------------------------------------------
    @Test
    fun test7_equalPriorityDeterministicHandling() = testScope.runTest {
        val notifA = IslandEvent.NotificationPosted(
            id = "notif_A",
            packageName = "com.sms",
            title = "Bank OTP",
            text = "123456"
        )
        priorityManager.processEvent(notifA)
        assertEquals("notif_A", priorityManager.activeEvent?.id)
        assertEquals("Bank OTP", stateRepository.state.value.title)

        // Equal priority notification arrives -> deterministically updates presentation
        val notifB = IslandEvent.NotificationPosted(
            id = "notif_B",
            packageName = "com.slack",
            title = "Deployment",
            text = "Success"
        )
        priorityManager.processEvent(notifB)
        assertEquals("notif_B", priorityManager.activeEvent?.id)
        assertEquals("Deployment", stateRepository.state.value.title)
    }

    // -------------------------------------------------------------
    // Test 8: Temporary event expiration
    // -------------------------------------------------------------
    @Test
    fun test8_temporaryEventExpiration() = testScope.runTest {
        val chargingEvent = IslandEvent.ChargingConnected(
            id = "charge_temp",
            batteryLevel = 90,
            chargeType = "CABLE",
            timeoutMs = 3000L
        )
        priorityManager.processEvent(chargingEvent)

        assertEquals(IslandPriority.SYSTEM_HARDWARE, priorityManager.activePriority)
        assertEquals("charge_temp", priorityManager.activeEvent?.id)

        // Advance virtual time by 3001 ms -> timeout expires
        testScheduler.advanceTimeBy(3001L)
        testScheduler.advanceUntilIdle()

        assertNull("Event should have expired", priorityManager.activeEvent)
        assertEquals(IslandPriority.IDLE_BASE, priorityManager.activePriority)
        assertEquals("NOTHING ISLAND", stateRepository.state.value.title)
    }

    // -------------------------------------------------------------
    // Test 9: Restoration of the previous eligible state
    // -------------------------------------------------------------
    @Test
    fun test9_restorationOfPreviousEligibleState() = testScope.runTest {
        // 1. Start persistent media playback (Priority 40)
        val mediaEvent = IslandEvent.MediaPlaybackChanged(
            id = "media_persistent",
            packageName = "com.spotify",
            trackTitle = "Heroes",
            artistName = "David Bowie",
            isPlaying = true
        )
        priorityManager.processEvent(mediaEvent)
        assertEquals("Heroes", stateRepository.state.value.title)

        // 2. Incoming temporary notification (Priority 55, timeout 4500ms) preempts media
        val notifEvent = IslandEvent.NotificationPosted(
            id = "notif_temp",
            packageName = "com.whatsapp",
            title = "Bob",
            text = "Hey!",
            timeoutMs = 4500L
        )
        priorityManager.processEvent(notifEvent)
        assertEquals("Bob", stateRepository.state.value.title)
        assertEquals(IslandPriority.NOTIFICATION, priorityManager.activePriority)

        // 3. Advance time until notification expires
        testScheduler.advanceTimeBy(4501L)
        testScheduler.advanceUntilIdle()

        // 4. Assert: Media playback is restored, NOT Base Idle!
        assertEquals("Heroes", stateRepository.state.value.title)
        assertEquals("David Bowie", stateRepository.state.value.subtitle)
        assertEquals(IslandPriority.MEDIA_PLAYBACK, priorityManager.activePriority)
        assertEquals("media_persistent", priorityManager.activeEvent?.id)
    }

    // -------------------------------------------------------------
    // Test 10: Cancellation of obsolete timeouts
    // -------------------------------------------------------------
    @Test
    fun test10_cancellationOfObsoleteTimeouts() = testScope.runTest {
        // Start temporary notification with 4500ms timeout
        val notif = IslandEvent.NotificationPosted(
            id = "notif_to_cancel",
            packageName = "com.whatsapp",
            title = "Alice",
            text = "Dinner?",
            timeoutMs = 4500L
        )
        priorityManager.processEvent(notif)
        assertEquals("Alice", stateRepository.state.value.title)

        // At 2000ms, incoming call arrives (Priority 100, persistent)
        testScheduler.advanceTimeBy(2000L)
        val call = IslandEvent.IncomingCallRinging(
            id = "call_p100",
            callId = "c2",
            callerDisplayName = "Emergency Contact"
        )
        priorityManager.processEvent(call)
        assertEquals("INCOMING CALL", stateRepository.state.value.title)

        // Advance past original notification's 4500ms mark (advance 4000ms more -> total 6000ms)
        testScheduler.advanceTimeBy(4000L)
        testScheduler.advanceUntilIdle()

        // Call must STILL be active; notification's obsolete timeout must NOT have dismissed the call!
        assertEquals("INCOMING CALL", stateRepository.state.value.title)
        assertEquals(IslandPriority.CRITICAL_TELEPHONY, priorityManager.activePriority)
        assertEquals("call_p100", priorityManager.activeEvent?.id)
    }

    // -------------------------------------------------------------
    // Test 11: Stale event rejection
    // -------------------------------------------------------------
    @Test
    fun test11_staleEventRejection() = testScope.runTest {
        // An old notification was processed and expired
        val notif1 = IslandEvent.NotificationPosted(
            id = "old_notif",
            packageName = "com.test",
            title = "Old",
            text = "Old",
            timeoutMs = 1000L
        )
        priorityManager.processEvent(notif1)
        testScheduler.advanceTimeBy(1001L)
        testScheduler.advanceUntilIdle()
        assertNull(priorityManager.activeEvent)

        // New notification is active
        val notif2 = IslandEvent.NotificationPosted(
            id = "new_notif",
            packageName = "com.test",
            title = "New",
            text = "New",
            timeoutMs = 3000L
        )
        priorityManager.processEvent(notif2)
        assertEquals("New", stateRepository.state.value.title)

        // Dismissal for old notification arrives -> must not dismiss new notification
        val staleDismiss = IslandEvent.NotificationDismissed(
            id = "dismiss_stale",
            targetNotificationId = "old_notif",
            packageName = "com.test"
        )
        priorityManager.processEvent(staleDismiss)

        assertEquals("New", stateRepository.state.value.title)
        assertEquals("new_notif", priorityManager.activeEvent?.id)
    }

    // -------------------------------------------------------------
    // Test 12: High-frequency event coalescing
    // -------------------------------------------------------------
    @Test
    fun test12_highFrequencyEventCoalescing() = testScope.runTest {
        val collected = mutableListOf<IslandEvent>()
        val job = launch { eventEngine.events.collect { collected.add(it) } }
        testScheduler.advanceUntilIdle()

        // Emit 5 TimerTicks in rapid succession (within 250ms coalescing window)
        var acceptedCount = 0
        for (sec in 60 downTo 56) {
            val tick = IslandEvent.TimerTick(
                id = "tick_$sec",
                timerId = "timer_main",
                remainingSeconds = sec.toLong()
            )
            if (eventEngine.emit(tick)) {
                acceptedCount++
            }
        }
        testScheduler.advanceUntilIdle()

        // Only the first tick should be emitted; rapid successive ticks are throttled/coalesced
        assertEquals("Only first tick should be accepted within window", 1, acceptedCount)
        assertEquals(1, collected.size)
        assertEquals(60L, (collected[0] as IslandEvent.TimerTick).remainingSeconds)

        job.cancel()
    }

    // -------------------------------------------------------------
    // Test 13: Critical event preservation under heavy buffer saturation (> 64 events)
    // -------------------------------------------------------------
    @Test
    fun test13_criticalEventPreservationUnderLoad() = testScope.runTest {
        val collected = mutableListOf<IslandEvent>()
        val job = launch { eventEngine.events.collect { collected.add(it) } }
        testScheduler.advanceUntilIdle()

        // 1. Emit critical incoming call first
        val criticalCall = IslandEvent.IncomingCallRinging(
            id = "critical_call_urgent",
            callId = "call_urgent",
            callerDisplayName = "VIP Caller"
        )
        val callEmitted = eventEngine.emit(criticalCall)
        assertTrue("Critical call emission must succeed", callEmitted)

        // 2. Flood event bus with 80 low-priority events WITHOUT advancing the collector scheduler
        // Buffer capacity is 64. In a naive DROP_OLDEST implementation, the critical call would be evicted!
        for (i in 1..80) {
            eventEngine.emit(IslandEvent.DeviceUnlocked(id = "unlock_$i", unlockMethod = UnlockMethod.FACE))
        }

        // 3. Now advance the test scheduler and let the collector drain the flows
        testScheduler.advanceUntilIdle()

        // The critical call MUST survive and be collected!
        assertTrue("Collected events must contain critical event even after 80 low-priority events saturated buffer",
            collected.any { it.id == "critical_call_urgent" })

        job.cancel()
    }

    // -------------------------------------------------------------
    // Test 14: Collector cancellation
    // -------------------------------------------------------------
    @Test
    fun test14_collectorCancellation() = testScope.runTest {
        val collected = mutableListOf<IslandEvent>()
        val job = launch { eventEngine.events.collect { collected.add(it) } }

        testScheduler.advanceUntilIdle()
        assertEquals(1, eventEngine.subscriptionCount.value)

        // Cancel collector job
        job.cancel()
        testScheduler.advanceUntilIdle()
        assertEquals(0, eventEngine.subscriptionCount.value)

        // Emit after cancellation
        eventEngine.emit(IslandEvent.DeviceUnlocked(id = "post_cancel", unlockMethod = UnlockMethod.FINGERPRINT))
        testScheduler.advanceUntilIdle()

        assertTrue("Cancelled collector should receive zero events", collected.isEmpty())
    }

    // -------------------------------------------------------------
    // Test 15: Service lifecycle cleanup
    // -------------------------------------------------------------
    @Test
    fun test15_serviceLifecycleCleanup() = testScope.runTest {
        // Setup active state with fallback
        priorityManager.processEvent(IslandEvent.MediaPlaybackChanged(
            id = "media_clean",
            packageName = "com.spotify",
            trackTitle = "Song",
            artistName = "Artist",
            isPlaying = true
        ))
        priorityManager.processEvent(IslandEvent.NotificationPosted(
            id = "notif_clean",
            packageName = "com.app",
            title = "Alert",
            text = "Msg",
            timeoutMs = 4000L
        ))

        assertNotNull(priorityManager.activeEvent)

        // Service shuts down -> reset priority manager and event engine
        priorityManager.reset()
        eventEngine.reset()

        assertNull("Active event must be cleared on reset", priorityManager.activeEvent)
        assertEquals(IslandPriority.IDLE_BASE, priorityManager.activePriority)
        assertEquals("NOTHING ISLAND", stateRepository.state.value.title)
    }

    // -------------------------------------------------------------
    // Test 16: User expansion/collapse state consistency
    // -------------------------------------------------------------
    @Test
    fun test16_userExpansionCollapseStateConsistency() = testScope.runTest {
        // User expands the island
        assertEquals(IslandMode.COLLAPSED, stateRepository.state.value.mode)
        stateRepository.toggleMode()
        assertEquals(IslandMode.EXPANDED, stateRepository.state.value.mode)

        // New event arrives
        priorityManager.processEvent(IslandEvent.NotificationPosted(
            id = "notif_during_expand",
            packageName = "com.app",
            title = "Notification",
            text = "Content",
            timeoutMs = 2000L
        ))

        // Mode must remain EXPANDED, user control is preserved!
        assertEquals("Mode must remain EXPANDED during event arrival", IslandMode.EXPANDED, stateRepository.state.value.mode)

        // Event expires
        testScheduler.advanceTimeBy(2001L)
        testScheduler.advanceUntilIdle()

        // Mode must STILL remain EXPANDED after event expiration
        assertEquals("Mode must remain EXPANDED after event expiration", IslandMode.EXPANDED, stateRepository.state.value.mode)

        // User manually collapses
        stateRepository.toggleMode()
        assertEquals(IslandMode.COLLAPSED, stateRepository.state.value.mode)
    }

    // -------------------------------------------------------------
    // Test 17: Media transition coalescing bypass (pause & track change preserved)
    // -------------------------------------------------------------
    @Test
    fun test17_mediaTransitionCoalescingBypass() = testScope.runTest {
        val collected = mutableListOf<IslandEvent>()
        val job = launch { eventEngine.events.collect { collected.add(it) } }
        testScheduler.advanceUntilIdle()

        // 1. Media starts playing Song A
        val playEvent = IslandEvent.MediaPlaybackChanged(
            id = "media_1",
            packageName = "com.spotify",
            trackTitle = "Song A",
            artistName = "Artist 1",
            isPlaying = true,
            currentPositionMs = 1000L
        )
        val emitted1 = eventEngine.emit(playEvent)
        assertTrue("Initial playback event must be emitted", emitted1)

        // 2. User pauses playback within 250ms coalescing window
        val pauseEvent = IslandEvent.MediaPlaybackChanged(
            id = "media_2",
            packageName = "com.spotify",
            trackTitle = "Song A",
            artistName = "Artist 1",
            isPlaying = false, // State transition: true -> false
            currentPositionMs = 1050L
        )
        val emitted2 = eventEngine.emit(pauseEvent)
        assertTrue("Pause transition must bypass coalescing and be emitted immediately", emitted2)

        // 3. User skips track within 250ms coalescing window
        val nextTrackEvent = IslandEvent.MediaPlaybackChanged(
            id = "media_3",
            packageName = "com.spotify",
            trackTitle = "Song B", // Track transition: Song A -> Song B
            artistName = "Artist 2",
            isPlaying = false,
            currentPositionMs = 0L
        )
        val emitted3 = eventEngine.emit(nextTrackEvent)
        assertTrue("Track change transition must bypass coalescing and be emitted immediately", emitted3)

        // 4. Routine position update arrives within 250ms with NO state or track change
        val routineTickEvent = IslandEvent.MediaPlaybackChanged(
            id = "media_4",
            packageName = "com.spotify",
            trackTitle = "Song B",
            artistName = "Artist 2",
            isPlaying = false,
            currentPositionMs = 50L
        )
        val emitted4 = eventEngine.emit(routineTickEvent)
        assertFalse("Routine position update without track/state change must be coalesced", emitted4)

        testScheduler.advanceUntilIdle()
        assertEquals(3, collected.size)
        job.cancel()
    }

    // -------------------------------------------------------------
    // Test 18: Completed timers removed from fallbacks (no resurrection)
    // -------------------------------------------------------------
    @Test
    fun test18_timerFinishedFallbackCleanup() = testScope.runTest {
        // 1. Timer countdown running
        val timerStart = IslandEvent.TimerStarted(
            id = "t_start",
            timerId = "timer_egg",
            label = "Boil Egg",
            totalDurationSeconds = 180L,
            remainingSeconds = 180L
        )
        priorityManager.processEvent(timerStart)
        assertEquals("TIMER: 180s", stateRepository.state.value.title)
        assertEquals(IslandPriority.LIVE_ACTIVITY, priorityManager.activePriority)

        // 2. Timer reaches 0: TimerFinished arrives (P55, timeoutMs = 5000L)
        val timerFinished = IslandEvent.TimerFinished(
            id = "t_fin",
            timerId = "timer_egg",
            label = "Boil Egg"
        )
        priorityManager.processEvent(timerFinished)
        assertEquals("TIMER FINISHED", stateRepository.state.value.title)
        assertEquals(IslandPriority.NOTIFICATION, priorityManager.activePriority)

        // 3. Advance virtual time past the 5000ms expiration deadline
        testScheduler.advanceTimeBy(5001L)
        testScheduler.advanceUntilIdle()

        // 4. Assert: Must cleanly revert to Base Idle, NOT resurrect the old timer!
        assertNull("Active event must be null after timer completion expiration", priorityManager.activeEvent)
        assertEquals(IslandPriority.IDLE_BASE, priorityManager.activePriority)
        assertEquals("NOTHING ISLAND", stateRepository.state.value.title)
    }

    // -------------------------------------------------------------
    // Test 19: Incoming call preserved across equal/higher priority alerts
    // -------------------------------------------------------------
    @Test
    fun test19_incomingCallFallbackPreservation() = testScope.runTest {
        // 1. Incoming call ringing (P100, persistent)
        val call = IslandEvent.IncomingCallRinging(
            id = "call_p100",
            callId = "call_vip",
            callerDisplayName = "Carl Pei"
        )
        priorityManager.processEvent(call)
        assertEquals("INCOMING CALL", stateRepository.state.value.title)
        assertEquals(IslandPriority.CRITICAL_TELEPHONY, priorityManager.activePriority)

        // 2. Temporary emergency alert arrives (P100, timeoutMs = 3000L)
        val emergencyAlert = IslandEvent.EmergencyAlert(
            id = "alert_p100",
            alertTitle = "FLASH FLOOD WARNING",
            alertMessage = "Seek higher ground immediately",
            timeoutMs = 3000L
        )
        priorityManager.processEvent(emergencyAlert)
        assertEquals("alert_p100", priorityManager.activeEvent?.id)
        assertEquals("FLASH FLOOD WARNING", stateRepository.state.value.title)

        // 3. Emergency alert expires after 3000ms
        testScheduler.advanceTimeBy(3001L)
        testScheduler.advanceUntilIdle()

        // 4. Assert: Incoming call was stored in background fallbacks and is restored!
        assertEquals("INCOMING CALL", stateRepository.state.value.title)
        assertEquals("Carl Pei", stateRepository.state.value.subtitle)
        assertEquals(IslandPriority.CRITICAL_TELEPHONY, priorityManager.activePriority)
        assertEquals("call_p100", priorityManager.activeEvent?.id)
    }

    // -------------------------------------------------------------
    // Test 20: Priority 0 lifecycle events preserve Base Idle badge ("M3 ENGINE")
    // -------------------------------------------------------------
    @Test
    fun test20_priority0LifecycleEventsPreserveIdlePresentation() = testScope.runTest {
        // Ensure starting at Base Idle
        assertEquals(IslandPriority.IDLE_BASE, priorityManager.activePriority)
        assertNull(priorityManager.activeEvent)
        assertEquals("M3 ENGINE", stateRepository.state.value.toLegacyIslandState().tag)

        // Emit overlay attached lifecycle event (P0)
        priorityManager.processEvent(IslandEvent.OverlayAttached(
            id = "overlay_attach",
            isAccessibilityMode = true
        ))

        // Presentation must remain Base Idle and badge must remain "M3 ENGINE" (NOT "EVENT P0")
        assertNull("Active event must remain null for P0 lifecycle events", priorityManager.activeEvent)
        assertEquals(IslandPriority.IDLE_BASE, priorityManager.activePriority)
        assertEquals("M3 ENGINE", stateRepository.state.value.toLegacyIslandState().tag)

        // Emit screen on lifecycle event (P0)
        priorityManager.processEvent(IslandEvent.ScreenStateChanged(
            id = "screen_on",
            isScreenOn = true
        ))

        assertNull("Active event must remain null for screen state changes", priorityManager.activeEvent)
        assertEquals("M3 ENGINE", stateRepository.state.value.toLegacyIslandState().tag)
    }

    // -------------------------------------------------------------
    // Test 21: Repository reset to idle preserves user display mode
    // -------------------------------------------------------------
    @Test
    fun test21_repositoryResetToIdlePreservesDisplayMode() = testScope.runTest {
        // Expand island
        stateRepository.setMode(IslandMode.EXPANDED)
        assertEquals(IslandMode.EXPANDED, stateRepository.state.value.mode)

        // Show active event
        priorityManager.processEvent(IslandEvent.NotificationPosted(
            id = "notif_reset",
            packageName = "com.test",
            title = "Test Title",
            text = "Test Text"
        ))
        assertEquals("Test Title", stateRepository.state.value.title)

        // Reset to idle (e.g. on overlay detach)
        stateRepository.resetToIdle()

        // Mode must remain EXPANDED, but active event cleared and title reset
        assertEquals(IslandMode.EXPANDED, stateRepository.state.value.mode)
        assertNull(stateRepository.state.value.activeEvent)
        assertEquals("NOTHING ISLAND", stateRepository.state.value.title)
    }

    // -------------------------------------------------------------
    // Test 22: Critical event retained across zero-subscriber intervals
    // -------------------------------------------------------------
    @Test
    fun test22_criticalEventRetainedAcrossZeroSubscribers() = testScope.runTest {
        // Zero active collectors
        assertEquals(0, eventEngine.subscriptionCount.value)

        // Critical call emitted while overlay is reconnecting / 0 subscribers
        val criticalCall = IslandEvent.IncomingCallRinging(
            id = "call_zero_sub",
            callId = "call_z1",
            callerDisplayName = "VIP Offline"
        )
        val emitted = eventEngine.emit(criticalCall)
        assertTrue("Critical event must be accepted and retained even with 0 subscribers", emitted)

        // Now overlay attaches and starts collecting
        val collected = mutableListOf<IslandEvent>()
        val job = launch { eventEngine.events.collect { collected.add(it) } }
        testScheduler.advanceUntilIdle()

        // Collector must immediately receive the retained critical call!
        assertEquals(1, collected.size)
        assertEquals("call_zero_sub", collected[0].id)

        job.cancel()
    }
}
