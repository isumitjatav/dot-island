# NOTHING ISLAND — MILESTONE R3 POST-IMPLEMENTATION AUDIT

**Audit Date:** November 5, 2026  
**Target:** Nothing Phone (4a) (`A069` / `FroggerIND`) | Android 17 (API 37)  
**Package:** `com.nothing.island.ai.engine`  
**Audited Files:**
- `app/src/main/java/com/nothing/island/ai/engine/IslandEvent.kt`
- `app/src/main/java/com/nothing/island/ai/engine/IslandEventEngine.kt`
- `app/src/main/java/com/nothing/island/ai/engine/IslandPriorityManager.kt`
- `app/src/main/java/com/nothing/island/ai/engine/IslandStateRepository.kt`
- `app/src/main/java/com/nothing/island/ai/IslandOverlayController.kt`
- `app/src/test/java/com/nothing/island/ai/engine/IslandEngineUnitTest.kt`

---

## 1. AUDIT EXECUTIVE SUMMARY

A forensic source-code audit of Milestone R3 was conducted to evaluate event-bus reliability, priority arbitration, state flow consistency, lifecycle safety, and test validity. 

While the core reactive unidirectional data flow (UDF) is clean and fully operational on device, the audit identified **9 specific technical findings**:
- **1 Critical finding:** `BufferOverflow.DROP_OLDEST` unconditionally evicts oldest events regardless of priority when buffer capacity is reached, contradicting claimed zero-drop guarantees for critical events.
- **3 High findings:** Media coalescing mistakenly suppresses legitimate pause/track-change transitions; completed timers resurrect themselves upon timeout expiration; default constructor timestamps break production event deduplication.
- **5 Medium findings:** Incomplete fallback caching for persistent alerts; lifecycle events altering idle tags to `EVENT P0`; missing repository reset on overlay detach; WindowManager bounds not reacting to programmatic state changes; and insufficient load volume in unit test 13.

No application code has been modified during this audit per strict review instructions.

---

## 2. DETAILED AUDIT FINDINGS

### Finding 1: Unconditional Eviction of Critical Events via `DROP_OLDEST`
- **File & Line:** `app/src/main/java/com/nothing/island/ai/engine/IslandEventEngine.kt`, lines 30–34, 77–83
- **Severity:** **CRITICAL**
- **Actual Behavior:**  
  `IslandEventEngine` configures its backing `MutableSharedFlow` with `onBufferOverflow = BufferOverflow.DROP_OLDEST` and `extraBufferCapacity = 64`. Kotlin's standard `SharedFlow` has no priority awareness. When the buffer fills up (e.g. during rapid background event bursts or when the collector coroutine is suspended/yielding), the oldest event at the head of the buffer is silently dropped. If an `IncomingCallRinging` (Priority 100) or `SafetyAlert` (Priority 70) sits in the buffer waiting for collector pickup, subsequent lower-priority events will cause `DROP_OLDEST` to evict the critical call! Furthermore, when `subscriptionCount == 0`, `replay = 0` causes `tryEmit` to discard events without caching.
- **Reproduction Scenario:**  
  Emit `IncomingCallRinging` into `IslandEventEngine` followed immediately by 65 low-priority events before the consumer coroutine executes. The `IncomingCallRinging` event is evicted and never delivered to `IslandPriorityManager`.
- **Recommended Correction:**  
  Implement a priority-aware staging queue or a dedicated high-priority channel for critical events (`priority >= 70`):
  1. For critical events, use a dedicated un-droppable channel or guarantee priority-sorted retention where low-priority events are discarded before any critical event can be evicted.
  2. Implement an active state retention cache for critical events during lifecycle transitions when `subscriptionCount == 0`.

---

### Finding 2: False Suppression of Play/Pause & Track Changes in Media Coalescing
- **File & Line:** `app/src/main/java/com/nothing/island/ai/engine/IslandEventEngine.kt`, lines 103–115
- **Severity:** **HIGH**
- **Actual Behavior:**  
  `shouldCoalesce` checks only `is IslandEvent.MediaPlaybackChanged` and throttles any subsequent `MediaPlaybackChanged` event arriving within 250ms under the key `"MediaTick_${event.packageName}"`. It does not inspect whether the playback state or track metadata actually changed. If a user pauses playback (`isPlaying: true -> false`) or skips to the next track (`trackTitle: "Song A" -> "Song B"`) within 250ms of an automated playback position tick, the pause/track-change event is treated as a routine position tick and **dropped**!
- **Reproduction Scenario:**  
  Spotify emits a progress tick at t=0ms (`isPlaying = true, position = 15000`). At t=60ms, the user presses Pause in Spotify (`isPlaying = false`). `shouldCoalesce` computes `now - lastTime = 60ms < 250ms` and returns `true`. The pause event is rejected (`emit()` returns `false`). Nothing Island continues displaying the media as playing.
- **Recommended Correction:**  
  In `shouldCoalesce`, verify that playback state and track metadata are identical before treating the event as a position tick:
  ```kotlin
  is IslandEvent.MediaPlaybackChanged -> {
      val last = lastEventRef.get() as? IslandEvent.MediaPlaybackChanged
      if (last != null && (last.isPlaying != event.isPlaying || 
                           last.trackTitle != event.trackTitle || 
                           last.artistName != event.artistName)) {
          return false // State or track changed: do not coalesce
      }
      "MediaTick_${event.packageName}"
  }
  ```

---

### Finding 3: Completed Timers Resurrected on Fallback Expiration
- **File & Line:** `app/src/main/java/com/nothing/island/ai/engine/IslandPriorityManager.kt`, lines 187–189, 146–171
- **Severity:** **HIGH**
- **Actual Behavior:**  
  `TimerStarted` and `TimerTick` are stored in `backgroundFallbacks["TIMER"]`. When the countdown finishes, `TimerFinished` (Priority 55, `timeoutMs = 5000L`) arrives and is presented. However, `IslandPriorityManager` contains no handler to remove `"TIMER"` from `backgroundFallbacks` upon timer completion. When `TimerFinished`'s 5000ms expiration timer fires, `restoreHighestFallbackLocked()` checks `backgroundFallbacks`, finds the old `TimerTick` / `TimerStarted`, and **restores it back to active presentation**, resurrecting the completed countdown indefinitely!
- **Reproduction Scenario:**  
  Start a timer (`TimerStarted`). Let it count down. The timer finishes (`TimerFinished` arrives, displays for 5000ms). Advance time by 5001ms. Instead of reverting to Base Idle, Nothing Island displays the finished timer as running again.
- **Recommended Correction:**  
  In `processEvent`, explicitly remove `"TIMER"` from `backgroundFallbacks` upon receiving `TimerFinished`:
  ```kotlin
  is IslandEvent.TimerFinished -> {
      backgroundFallbacks.remove("TIMER")
      // Allow TimerFinished to be processed as active temporary presentation
  }
  ```

---

### Finding 4: Ineffective Production Deduplication Due to Constructor Timestamps
- **File & Line:** `app/src/main/java/com/nothing/island/ai/engine/IslandEventEngine.kt`, lines 93–100; masked in `IslandEngineUnitTest.kt`, lines 112–126
- **Severity:** **HIGH**
- **Actual Behavior:**  
  `isDuplicate` checks `previous == current`. In Kotlin, data class `equals()` compares all primary constructor properties. Every `IslandEvent` data class includes `override val timestamp: Long = System.currentTimeMillis()`. When producers emit events in production, each event instantiation creates a distinct timestamp. Consequently, `previous == current` evaluates to `false` even if all business payload data (`id`, `title`, `text`, `packageName`) is identical! `test3_eventDeduplication` in unit tests passed only because `timestamp = 1000L` was manually hardcoded on both instances.
- **Reproduction Scenario:**  
  Emit `NotificationPosted(id = "1", packageName = "p", title = "A", text = "B")` twice in rapid succession in production. Because `timestamp` differs by microseconds, the second identical event is accepted and emitted.
- **Recommended Correction:**  
  Exclude `timestamp` from business equality checks, or implement a dedicated content hash / deduplication key:
  ```kotlin
  private fun isDuplicate(previous: IslandEvent, current: IslandEvent, now: Long): Boolean {
      if (now - lastEmissionTime.get() > deduplicationWindowMs) return false
      if (previous.source != current.source) return false
      if (previous.javaClass != current.javaClass) return false
      if (previous.id != current.id) return false
      
      // Compare business payloads excluding timestamp
      return previous.hasIdenticalPayload(current)
  }
  ```

---

### Finding 5: Incomplete Fallback Caching for Incoming Calls & Unmatched Persistent Events
- **File & Line:** `app/src/main/java/com/nothing/island/ai/engine/IslandPriorityManager.kt`, lines 173–201
- **Severity:** **MEDIUM**
- **Actual Behavior:**  
  `cacheBackgroundEventIfPersistent` checks `if (event.timeoutMs == 0L)` but then utilizes an explicit `when` clause that only handles `MediaPlaybackChanged`, `CallConnected`, `TimerStarted`/`TimerTick`, and `RecordingStateChanged`. `IncomingCallRinging` (Priority 100, `timeoutMs = 0L`) is unhandled in the `when` block and falls into `else -> {}`. If an incoming call is ringing and an equal or higher system alert appears, the call is not preserved in `backgroundFallbacks` and cannot be restored.
- **Reproduction Scenario:**  
  An incoming call is ringing (`IncomingCallRinging`). A temporary priority-100 system alert arrives. Upon alert timeout, `restoreHighestFallbackLocked` finds no call in fallbacks and reverts to idle.
- **Recommended Correction:**  
  Add `is IslandEvent.IncomingCallRinging -> backgroundFallbacks["INCOMING_CALL"] = event` to the caching logic, or generalize fallback caching by `event.source`.

---

### Finding 6: Priority 0 System Events Alter Idle State Tag to "EVENT P0"
- **File & Line:** `app/src/main/java/com/nothing/island/ai/engine/IslandPriorityManager.kt`, lines 94–98; `IslandStateRepository.kt`, line 37
- **Severity:** **MEDIUM**
- **Actual Behavior:**  
  Base Idle has `activePriority = IslandPriority.IDLE_BASE` (0). System lifecycle events (`OverlayAttached`, `OverlayDetached`, `ScreenStateChanged`) also define `priority = 0`. Under Rule 3 (Equal Priority), when a lifecycle event is emitted, `activateEventLocked` sets `activeEvent = event`. In `IslandStateRepository.toLegacyIslandState()`, `tag = if (activeEvent != null) "EVENT P$activePriority" else "M3 ENGINE"`. Because `activeEvent != null`, the UI badge displays `"EVENT P0"` instead of `"M3 ENGINE"` during idle.
- **Reproduction Scenario:**  
  Overlay attaches and emits `OverlayAttached(isAccessibilityMode = true)`. The overlay tag changes from `"M3 ENGINE"` to `"EVENT P0"`.
- **Recommended Correction:**  
  System lifecycle events (`OverlayAttached`, `ScreenStateChanged`) should be handled as passive lifecycle notifications rather than active UI presentation events, or Rule 3 should require `newPriority > IslandPriority.IDLE_BASE` to preempt the base idle presentation.

---

### Finding 7: Missing State Reset in `IslandOverlayController.detachInternal()`
- **File & Line:** `app/src/main/java/com/nothing/island/ai/IslandOverlayController.kt`, lines 368–394
- **Severity:** **MEDIUM**
- **Actual Behavior:**  
  When the overlay window is detached in `detachInternal()`, `controllerScope?.cancel()` and `priorityManager?.reset()` are called, but `repository.resetToIdle()` is omitted. Because `repository` is a singleton property on `IslandOverlayController`, `repository.state.value` retains the last active event snapshot. When the overlay is re-attached, `collectAsState()` briefly renders the stale presentation until a new state is emitted.
- **Reproduction Scenario:**  
  Active notification is showing. Turn off screen (`detachInternal` called). Reconnect accessibility service. The old notification is briefly rendered before reverting.
- **Recommended Correction:**  
  Invoke `repository.resetToIdle()` inside `detachInternal()`:
  ```kotlin
  controllerScope?.cancel()
  controllerScope = null
  priorityManager?.reset()
  priorityManager = null
  eventEngine.reset()
  repository.resetToIdle()
  ```

---

### Finding 8: WindowManager Bounds Not Reactively Bound to StateFlow Mode
- **File & Line:** `app/src/main/java/com/nothing/island/ai/IslandOverlayController.kt`, lines 270–311
- **Severity:** **MEDIUM**
- **Actual Behavior:**  
  WindowManager window dimensions (`372x108 px` vs `1050x630 px`) are updated strictly when `toggleExpand()` is executed on `mainHandler`. If `IslandStateRepository.setMode(IslandMode.EXPANDED)` is invoked programmatically by an incoming event (e.g., an incoming call auto-expanding the island), Compose recomposes in expanded mode, but the physical WindowManager bounds remain `372x108 px`, causing the expanded card to be clipped and deformed into the capsule.
- **Reproduction Scenario:**  
  Call `repository.setMode(IslandMode.EXPANDED)` programmatically without calling `toggleExpand()`. The Compose tree renders expanded UI, but the overlay window frame remains compact (`372x108 px`).
- **Recommended Correction:**  
  In `attachAccessibilityOverlay`, launch a coroutine observing `repository.state.map { it.mode }.distinctUntilChanged()` that dispatches `wm.updateViewLayout(view, params)` on `mainHandler` whenever `mode` changes.

---

### Finding 9: Unit Test 13 Did Not Test Buffer Saturation
- **File & Line:** `app/src/test/java/com/nothing/island/ai/engine/IslandEngineUnitTest.kt`, lines 423–442
- **Severity:** **MEDIUM**
- **Actual Behavior:**  
  `test13_criticalEventPreservationUnderLoad` was designed to verify that critical events are not dropped under heavy event load. However, the test only emits 20 events into a buffer with capacity 64 (`bufferCapacity = 64`). Because the buffer was at 31% capacity and never reached saturation, `BufferOverflow.DROP_OLDEST` was never triggered during the test.
- **Reproduction Scenario:**  
  Modify `test13` to emit 70 events without advancing the collector. The test fails because `DROP_OLDEST` evicts the critical event.
- **Recommended Correction:**  
  Once Finding 1 (priority-aware staging / un-droppable critical channel) is addressed, update `test13` to emit > 64 events and verify true buffer-saturation resilience.

---

## 3. AUDIT MATRIX SUMMARY

| # | Component | Line(s) | Severity | Category | Description |
|:---:|:---|:---:|:---:|:---|:---|
| **1** | `IslandEventEngine.kt` | 30–34, 77–83 | **CRITICAL** | Event Bus Reliability | `DROP_OLDEST` drops critical events when buffer capacity (64) is reached |
| **2** | `IslandEventEngine.kt` | 103–115 | **HIGH** | Event Coalescing | Media position coalescing accidentally drops pause and track changes |
| **3** | `IslandPriorityManager.kt` | 187–189, 146–171 | **HIGH** | Priority Arbitration | Completed timers are resurrected upon `TimerFinished` expiration |
| **4** | `IslandEventEngine.kt` | 93–100 | **HIGH** | Deduplication | Data class `equals()` fails to deduplicate due to constructor timestamps |
| **5** | `IslandPriorityManager.kt` | 173–201 | **MEDIUM** | Priority Arbitration | `IncomingCallRinging` and general persistent events omitted from fallback cache |
| **6** | `IslandPriorityManager.kt` | 94–98 | **MEDIUM** | Presentation State | Priority 0 lifecycle events overwrite idle state and display `"EVENT P0"` |
| **7** | `IslandOverlayController.kt` | 368–394 | **MEDIUM** | Lifecycle Safety | `repository.resetToIdle()` not called on overlay detach, leaving stale state |
| **8** | `IslandOverlayController.kt` | 270–311 | **MEDIUM** | UI / WindowManager | WindowManager bounds only resize on click; programmatic expand clips UI |
| **9** | `IslandEngineUnitTest.kt` | 423–442 | **MEDIUM** | Test Verification | Test 13 only emits 20 events into 64-capacity buffer, masking overflow eviction |

---

## 4. CONCLUSION & RECOMMENDATIONS

Milestone R3's core architecture (reactive flow dispatch, priority preemption, decoupled Compose state repository, and `AccessibilityService` window ownership) is solid and verified on hardware.

However, the 9 findings documented above represent real edge cases and semantic gaps that must be corrected before Milestone R5 (Persistent Settings) and Milestone R6 (Notification Engine) are implemented.

**Status:** Audit complete. Awaiting user approval to proceed with targeted corrections.
