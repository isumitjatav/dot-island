# Nothing Island — Milestone R6 Phase 3.3 Architecture Plan (Ordering & Registration Consistency)
## In-Memory Notification History & Decoupled PendingIntent Lifecycle Architecture

**Milestone:** R6 — Notification Engine  
**Phase:** 3.3 — In-Memory Notification History Repository  
**Mode:** READ-ONLY — PLANNING ONLY (Implementation NOT Authorized)  
**Target Hardware:** Nothing Phone (4a) (`A069` / `FroggerIND`), Android 17 / API 37  
**Document Status:** FINAL ARCHITECTURE SPECIFICATION / AWAITING USER APPROVAL  

---

## 1. Executive Summary & Core Architectural Pillars

Milestone R6 Phase 3.3 establishes the in-memory **Notification History Engine** for Nothing Island.

Following the final ordering and registration consistency review, this specification formalizes seven core system pillars:
1. **Defensible Serialized Ingestion & Residual Platform Limitations:**
   * Android AOSP does **not** provide a transaction sequence number on `StatusBarNotification`.
   * `sbn.postTime` is a **freshness signal**, not a guaranteed unique event version. Equal `postTime` values cannot reliably distinguish older from newer source events.
   * Ingress sequence represents **callback processing order only**, not original creation order.
   * We implement a single-threaded FIFO ingestion pipeline (`Dispatchers.Default.limitedParallelism(1)`) to eliminate concurrent mutations and enforce deterministic processing, while adopting a **conservative strategy** for ambiguous update/removal races (removals win on ambiguous timestamps to prevent ghost notifications).
2. **Strict 5-Step Action Registration & History Consistency Lifecycle:**
   * **Step 1:** Serialize and validate incoming event.
   * **Step 2:** Register or replace actions in `NotificationActionBridge` $\to$ obtain `actionGeneration`.
   * **Step 3:** Record/update the history entry with that `actionGeneration`.
   * **Step 4 (Rollback Guarantee):** If history insertion fails or is rejected, immediately invoke `NotificationActionBridge.unregisterIfGenerationMatches(key, actionGeneration)` to prevent orphaned PendingIntents without clobbering newer registrations.
   * **Step 5:** Emit the presentation event when appropriate.
3. **Strict Privacy Boundary & Dynamic Projection:** Raw notification data is never rendered directly. Compose views observe `PrivacySafeNotificationPresentation` evaluated dynamically against real-time keyguard state. Stale unmasked content is instantly invalidated upon locking.
4. **Permanent Removal vs Temporary Secret-Notification Suppression:** Toggling preview mode to `HIDDEN` permanently purges applicable history entries and action registrations. Locking the device **temporarily suppresses** `VISIBILITY_SECRET` notifications from all UI presentation **without deleting the underlying history entry from memory**. Upon unlock, the entry becomes selectable again.
5. **Independent Historical Notification Selection & Anti-Hijack Guarantee:** Users can manually select and inspect an older history entry in State B (`NOTIFICATION_FOCUSED`) independently of active popups. A later notification arrival in the background must never hijack or overwrite an active manual inspection. Eviction or privacy removal of a selected entry safely falls back to the Dashboard or collapses.
6. **Safe Concurrent Lifecycle Management:** Repository synchronization (`lock`) is used for internal data structures, but `PendingIntent` execution (`intent.send()`) and external action unregistration callbacks are strictly executed outside repository locks, eliminating binder deadlocks.
7. **Preservation Across Listener Disconnection & Monotonic Timer Semantics:** In-memory history remains intact across listener unbind/rebind cycles while the application process lives. Actions are cleared on disconnect and repopulated alongside `isSystemActive` restoration upon silent reconnection reconciliation. Ordinary updates preserve the original popup countdown deadline without extension.

---

## 2. Event Freshness Guarantees & Honest Platform Limitations

### 2.1 Android Platform Realities & Limitations
An honest evaluation of the Android AOSP notification framework reveals critical platform constraints:
1. **No Monotonic Source Transaction Sequence:** Android does **not** provide a notification sequence counter, revision number, or monotonic transaction ID through public `StatusBarNotification` or `NotificationListenerService` APIs.
2. **`sbn.postTime` Is a Freshness Signal, Not an Event Version:**
   * `postTime` is set when the notification is initially posted or re-posted, but applications frequently update notifications in-place (e.g. progress bar updates, playback position ticks) without modifying `postTime`.
   * Multiple rapid updates from an app can carry identical millisecond `postTime` values. Equal `postTime` values **cannot reliably distinguish older from newer source events**.
3. **Ingress Sequence Represents Local Dispatch Order Only:**
   * Ingress sequence numbers generated at the listener ingress represent the order callbacks arrived at our process, which may differ from the chronological creation order if the OS or binder dispatched them with microsecond jitter.
4. **Platform Honesty Guarantee:** We do **not** claim perfect detection of every delayed stale callback. Instead, we implement a defensible, serialized-ingestion architecture with conservative tie-breaking to guarantee that local state cannot be corrupted by out-of-order execution.

### 2.2 Serialized Ingestion Pipeline
* `IslandNotificationListenerService` directs all incoming callbacks (`onNotificationPosted`, `onNotificationRemoved`) into a single-threaded FIFO channel processed by a dedicated serialized dispatcher:
  ```kotlin
  val ingestionDispatcher = Dispatchers.Default.limitedParallelism(1)
  ```
* **Why Preserved:** Serialized ingestion guarantees:
  1. Zero concurrent race conditions between `onNotificationPosted` and `onNotificationRemoved`.
  2. Strict FIFO state machine progression.
  3. Safe lock-free handoffs to downstream components.

### 2.3 Conservative Event Freshness & Race Handling
When an incoming notification callback arrives for an existing key in `NotificationHistoryRepository`:
* **Case 1: Strictly Newer Post Time (`incomingPostTime > stored.sourcePostTime`):**
  Accepted as a genuinely fresh update.
* **Case 2: Strictly Older Post Time (`incomingPostTime < stored.sourcePostTime`):**
  Rejected as an in-flight stale update.
* **Case 3: Ambiguous Identical Post Time (`incomingPostTime == stored.sourcePostTime`):**
  Ingress sequence is checked (`incomingSequence > stored.ingestSequence`). If the content payload has changed, the text is updated in-place, but `isSystemActive` is **not** resurrected if the entry was previously marked removed.
* **Conservative Update/Removal Race Rule:**
  * If a removal event arrives, the repository records `isSystemActive = false` and `lastRemovalPostTime = sbn.postTime`.
  * If a subsequent update arrives with `incomingPostTime <= lastRemovalPostTime`:
    It is **REJECTED**. The removal is treated as authoritative. A notification is **only resurrected** if `incomingPostTime > lastRemovalPostTime` (strictly greater), preventing ghost notifications from lingering in the active state.

---

## 3. Action Registration & History Consistency Lifecycle

### 3.1 Strict 5-Step Ingestion & Consistency Sequence
To prevent orphaned `PendingIntent` references and ensure atomic consistency across `NotificationActionBridge` and `NotificationHistoryRepository`:

```mermaid
sequenceDiagram
    autonumber
    participant AOSP as Android Listener Callback
    participant NLS as Serialized Ingestion Pipeline
    participant NAB as NotificationActionBridge (Owns Generation)
    participant NHR as NotificationHistoryRepository
    participant PM as IslandPriorityManager

    AOSP->>NLS: onNotificationPosted(sbn)
    Note over NLS: 1. Serialize & Validate Incoming Event
    NLS->>NAB: 2. Register Actions (Atomic Compute)
    NAB-->>NLS: Return actionGeneration (e.g. Gen 15)
    
    NLS->>NHR: 3. Record/Update History Entry (with Gen 15)
    
    alt History Insertion/Update FAILS or REJECTED
        NHR-->>NLS: Result: FALSE (Rejected Stale / Error)
        Note over NLS,NAB: 4. IMMEDIATE ROLLBACK!
        NLS->>NAB: unregisterIfGenerationMatches(key, Gen 15)
        Note over NAB: Cleans orphaned intents; preserves newer generations!
    else History Insertion SUCCEEDS
        NHR-->>NLS: Result: TRUE (Accepted)
        Note over NLS,PM: 5. Emit Presentation Event
        NLS->>PM: emit NotificationPosted / Refresh Text
    end
```

### 3.2 Rollback & Cleanup Guarantee
* If `NHR.recordNotification(...)` returns `false` (e.g. event rejected as stale, or global preview mode changed to `HIDDEN` concurrently):
  ```kotlin
  val actionGen = actionBridge.register(key, sbn.packageName, contentIntent, actions)
  val accepted = historyRepository.recordNotification(key, ..., actionGeneration = actionGen)
  if (!accepted) {
      // ROLLBACK: Release orphaned PendingIntent immediately
      actionBridge.unregisterIfGenerationMatches(key, actionGen)
      Log.d(TAG, "Rolled back action registration $actionGen for key=$key following history rejection.")
  }
  ```
* **Guarantees Enforced:**
  1. **Zero Orphaned PendingIntents:** Failed or rejected updates cannot leave dangling binder references in memory.
  2. **Safe Newer Generation Protection:** `unregisterIfGenerationMatches` checks `storedGeneration == actionGen`. If a newer registration has already occurred, the rollback is a no-op and cannot delete the newer actions.

---

## 4. Strict Privacy Boundary & Dynamic Projection Engine

### 4.1 The Invariant
* **Raw notification data must NEVER reach the UI composition layer directly.**
* The domain model `NotificationHistoryEntry` holds sanitized string values in volatile heap memory, but Compose views never consume this class directly.
* UI views consume exclusively `PrivacySafeNotificationPresentation`.

### 4.2 Domain Models

```kotlin
package com.nothing.island.ai.notifications

/**
 * Immutable in-memory notification record held in the repository.
 * Resides exclusively in volatile heap memory. NEVER written to DataStore or disk.
 */
data class NotificationHistoryEntry(
    val key: String,                     // StatusBarNotification.key (stable AOSP identity)
    val packageName: String,             // Originating package name
    val appDisplayName: String,          // Resolved human-readable app label
    val rawTitle: String,                // Sanitized title
    val rawText: String,                 // Sanitized body
    val rawSubText: String? = null,      // Optional subtext (e.g. group chat name)
    val category: String? = null,        // AOSP category
    val firstPostTime: Long,             // Original arrival timestamp (for relative age)
    val lastUpdateTime: Long,            // Timestamp of most recent update
    val sourcePostTime: Long = 0L,       // sbn.postTime used for event freshness arbitration
    val ingestSequence: Long = 0L,       // Monotonic sequence assigned at listener ingress
    val lastRemovalPostTime: Long = 0L,  // sbn.postTime of most recent removal event
    val lastRemovalSequence: Long = 0L,  // Ingress sequence of most recent removal event
    val isSystemActive: Boolean = true,  // True if still active in Android status bar shade
    val isOngoing: Boolean = false,      // Whether notification was posted as ongoing
    val isClearable: Boolean = true,     // Whether notification can be dismissed by user
    val visibility: Int = 0,             // Notification.VISIBILITY_PUBLIC (0), PRIVATE (1), SECRET (-1)
    val previewMode: NotificationPreviewMode = NotificationPreviewMode.FULL,
    val actionGeneration: Long = 0L,     // Generation token assigned by NotificationActionBridge
    val updateCount: Int = 0             // Number of content updates applied
)

/**
 * Privacy-safe UI projection consumed by Compose views (Dashboard & NotificationCard).
 * Dynamically generated; never statically cached across lock-state transitions.
 */
data class PrivacySafeNotificationPresentation(
    val key: String,
    val packageName: String,
    val appDisplayName: String,
    val title: String,
    val body: String,
    val subText: String?,
    val isMasked: Boolean,
    val isSystemActive: Boolean,
    val timestamp: Long,
    val previewMode: NotificationPreviewMode
)
```

### 4.3 Temporary Secret-Notification Suppression vs Permanent Removal

#### Operation A: Permanent History Removal (Preview Mode -> HIDDEN)
* When user settings transition to `previewMode == NotificationPreviewMode.HIDDEN`:
  1. `historyRepository.clearAll()` permanently purges all history entries from memory.
  2. `NotificationActionBridge.clearAll()` permanently clears all action references.
  3. If State B or an island capsule is open, it immediately closes and resets to Base Idle.

#### Operation B: Temporary Lock-Screen Suppression (`VISIBILITY_SECRET` = -1)
* When the device locks (`isKeyguardLocked = true`):
  1. **Memory Preservation:** The underlying `NotificationHistoryEntry` is **NOT deleted** from `NotificationHistoryRepository`.
  2. **UI Omission:** `projectedHistory` completely omits `VISIBILITY_SECRET` entries (`mapNotNull` returns `null`). They do not appear in State A Recent Alerts (not even as "New notification").
  3. **State B Active Fallback:** If State B is currently displaying a secret notification when the device locks, `IslandOverlayController` immediately detects that the projected presentation is `null` and **safely falls back to State A Dashboard or collapses to the compact pill**.
  4. **Dynamic Unlock Restoration:** When the device unlocks (`isKeyguardLocked = false`), the dynamic projection immediately restores the secret notification with its full unmasked content. It becomes selectable again in the Dashboard.
  5. **Zero Stale Exposure:** At no point during lock or unlock are unmasked secret strings rendered while the keyguard is engaged.

---

## 5. Historical Notification Selection Architecture

### 5.1 Decoupled Presentation State in `IslandMasterState`
In `IslandMasterState` and `IslandStateRepository`, active visual presentation is strictly separated into:
1. `activeEvent: IslandEvent?`: The currently active system/island event (e.g. active incoming notification popup, ongoing call, media).
2. `selectedNotificationKey: String?`: The notification explicitly selected by the user for manual inspection from the history list.

### 5.2 State Machine & Anti-Hijacking Invariant
1. **New Notification Arrival:** Selects that notification for its initial visual presentation (`selectedNotificationKey = event.id`).
2. **Popup Timeout:** 4.5s countdown elapses; overlay collapses to compact pill (124dp × 36dp); `selectedNotificationKey` reverts to `null`; history remains intact.
3. **Manual Selection from Dashboard (State A):** User taps an alert row; `repository.selectHistoricalNotification(key)` sets `selectedNotificationKey = key` and opens State B (`NOTIFICATION_FOCUSED`).
4. **Anti-Hijacking Guarantee:** If a user is actively in State B inspecting an alert, subsequent background notifications **must NOT hijack or overwrite the active card**. They record to history silently.
5. **Revalidation & Safe Fallback:** Before rendering, the selected key is validated against current lock privacy rules. If evicted or suppressed by keyguard lock, State B **safely falls back to State A Dashboard or collapses to idle**.
6. **Geometry Preserved:** Calibrated 124dp × 36dp compact pill, 350dp × 210dp expanded card, `FLAG_WATCH_OUTSIDE_TOUCH` collapse, and Layer 24000 single overlay strictly preserved. ZERO clock/weather in State B!

---

## 6. Safe Concurrent Lifecycle Management

* `NotificationHistoryRepository` synchronizes internal list and map structures using standard repository synchronization (`private val lock = Any()`).
* **Deadlock Prevention Rule:** `PendingIntent` execution (`intent.send()`) and external action unregistration callbacks are **never** invoked while holding repository locks. All cleanup targets are collected within the lock and fired after exiting the lock.

---

## 7. Comprehensive Test Matrix (29 Unit + 8 Hardware Tests)

### 7.1 Focused JVM Unit Tests (`NotificationHistoryRepositoryUnitTest.kt`)

| # | Test Name | Specific Invariant Verified |
|---|---|---|
| **1** | `testNewNotificationInsertedAtTop` | New alert inserted at index 0 with `updateCount = 0`. |
| **2** | `testNewestFirstOrdering` | Multiple incoming alerts maintain strict reverse-chronological ordering. |
| **3** | `testExistingNotificationUpdateInPlace` | Existing key update refreshes content, increments `updateCount`, preserves `firstPostTime`, size remains 1. |
| **4** | `testStableIdentityHandling` | Correct lookup and mutation using `StatusBarNotification.key`. |
| **5** | `testCapacityEnforcementAt30` | 35 notifications ingested; repository size strictly caps at exactly 30. |
| **6** | `testOldestEntryEvictionReleasesOnlyEvictedActions` | Ingesting entry 31 evicts entry 1 and unregisters *only* entry 1's actions in `NotificationActionBridge`. |
| **7** | `testActionGenerationSoleOwnershipInActionBridge` | Verifies `NotificationActionBridge` generates monotonic tokens and `NotificationHistoryEntry` stores matching generation. |
| **8** | `testStaleActionTokenCleanup` | Stale generation token cannot unregister actions belonging to a newer registration generation. |
| **9** | `testDelayedOlderUpdateRejectedByEventFreshness` | Callback with older `sourcePostTime` arriving out of order is rejected by event freshness check; newer content preserved. |
| **10** | `testIngestSequenceTieBreaksIdenticalPostTime` | Identical `sourcePostTime` callbacks use `ingestSequence` to deterministically reject delayed callbacks. |
| **11** | `testAmbiguousUpdateRemovalRaceFavorsRemoval` | When an update has `postTime <= removalPostTime`, removal is authoritative to prevent ghost notifications. |
| **12** | `testActionRegistrationRollbackOnHistoryFailure` | If history insertion fails after action registration, rollback unregisters actions without clobbering newer registrations. |
| **13** | `testConcurrentRegistrationReplacementInActionBridge` | Concurrent `register()` calls on same key preserve the highest generation token via atomic compute. |
| **14** | `testNewerRepostAfterRemovalReactivatesEntry` | Genuinely newer post after removal restores `isSystemActive = true` and updates content. |
| **15** | `testHiddenPreviewModeExcludedFromHistory` | Alerts arriving under `HIDDEN` preview mode never enter history. |
| **16** | `testHiddenPreferenceChangePurgesExistingHistoryAndActions` | Toggling `previewMode` to `HIDDEN` immediately purges all existing history entries and clears action bridge. |
| **17** | `testSecretVisibilitySuppressedOnLockWithoutHistoryDeletion` | Locking keyguard omits `VISIBILITY_SECRET` notification from `projectedHistory`, but entry remains intact in repository memory. |
| **18** | `testSecretNotificationStateBFallbackOnLock` | When State B is active on a secret notification, toggling `isKeyguardLocked = true` immediately triggers safe fallback to Dashboard or collapse. |
| **19** | `testSecretNotificationReselectableAfterUnlock` | Unlocking keyguard restores secret notification in `projectedHistory`, and selecting it opens State B with full unmasked content. |
| **20** | `testLockscreenMasking` | `projectedHistory` dynamically replaces title with app label and body with "New notification" when locked. |
| **21** | `testDynamicUnlockUnmasksContent` | Unlocking keyguard immediately restores unmasked titles and bodies across all projected entries. |
| **22** | `testRemovalRetainsHistoryWithInactiveFlag` | Calling `markSystemNotificationRemoved` sets `isSystemActive = false` but retains entry in history. |
| **23** | `testPendingIntentRetainedAcrossVisualTimeout` | 4.5s popup timer expiration leaves `NotificationActionBridge` actions intact and executable. |
| **24** | `testPendingIntentRetainedAcrossShadeRemoval` | System shade dismissal sets `isSystemActive = false` without purging actions from `NotificationActionBridge`. |
| **25** | `testCanceledExceptionCleansDeadAction` | Firing an action that throws `PendingIntent.CanceledException` safely catches and unregisters the dead action. |
| **26** | `testListenerDisconnectClearsActionsPreservesHistory` | Listener disconnect clears `NotificationActionBridge` but leaves history entries intact (`isSystemActive = false`). |
| **27** | `testListenerReconnectionRestoresSystemActiveWithoutDuplicates` | Reconnecting reconciles active notifications, restores `isSystemActive = true`, registers fresh actions, zero duplicates. |
| **28** | `testOrdinaryUpdateDoesNotResetPopupDeadline` | Updating an active notification refreshes presentation text while preserving the original timeout countdown. |
| **29** | `testIncomingNotificationDoesNotHijackManualHistoricalSelection` | While in State B inspecting a selected historical entry, an incoming notification updates history in background without replacing the selected view. |

### 7.2 Physical Hardware Test Matrix (Nothing Phone 4a)

| # | Hardware Test Scenario | Stimulus | Verification Step |
|---|---|---|---|
| **H1** | Live History Population | `cmd notification post` | Open Dashboard -> Alert appears in Recent Alerts list. |
| **H2** | In-Place Update Verification | Post update with same tag | Dashboard Recent Alerts shows updated text, no duplicate row. |
| **H3** | Tap History -> State B | Tap history alert in Recent Alerts | State B (`NOTIFICATION_FOCUSED`) opens displaying [`NotificationCard`](file:///C:/Users/User/.gemini/antigravity/scratch/nothing-island/app/src/main/java/com/nothing/island/ai/ui/components/NotificationCard.kt). Zero clock/weather. |
| **H4** | Anti-Hijack While Inspecting | In State B on older alert, post new alert via adb | State B remains on the older alert; does not hijack view. |
| **H5** | Action Chip Execution from History | Tap action button in State B | Target app launches immediately via `PendingIntent.send()`. |
| **H6** | Secret Notification Lock Fallback | Inspect secret alert in State B, lock screen | Overlay immediately closes or falls back to Dashboard; zero unmasked leak. |
| **H7** | Secret Notification Unlock Restoration | Unlock screen after lock suppression | Secret alert reappears in Dashboard and opens State B cleanly. |
| **H8** | Capacity Cap (35 Alerts) | Run bash loop posting 35 notifications | History count strictly 30. Oldest 5 evicted. Action bridge count matches history. |

---

## 8. File-by-File Implementation Plan

| File | Exact Scope of Changes |
|---|---|
| `app/.../notifications/NotificationHistoryEntry.kt` | **NEW FILE:** Immutable `NotificationHistoryEntry` domain model (with source post time and ingress sequence) and `PrivacySafeNotificationPresentation` projection. |
| `app/.../notifications/NotificationHistoryRepository.kt` | **NEW FILE:** Thread-safe, in-memory 30-capacity bounded repository with StateFlow emission, event freshness tie-breaking, and safe external cleanup callbacks outside locks. |
| `app/.../notifications/NotificationActionBridge.kt` | Monotonic `AtomicLong` generation tokens, atomic `compute()` registration replacement, `unregisterIfGenerationMatches(key, gen)`, catch `CanceledException`. |
| `app/.../notifications/IslandNotificationListenerService.kt` | Serialized FIFO ingestion dispatcher (`Dispatchers.Default.limitedParallelism(1)`), 5-step lifecycle execution with action rollback on history failure; mark removal without deleting actions; reconcile on reconnect. |
| `app/.../engine/IslandPriorityManager.kt` | Update `processEvent` to recognize ordinary updates and preserve timeout deadline; remove action unregistration on timeout; support independent historical selection with anti-hijack. |
| `app/.../IslandOverlayController.kt` | Expose `projectedHistory` StateFlow; maintain `selectedNotificationKey`; bind eviction to action bridge; safe fallback on secret lock. |
| `app/.../IslandOverlayView.kt` | Render Recent Alerts from `projectedHistory` in `DashboardCard`; handle tap to navigate to State B. |
| `app/src/test/.../NotificationHistoryRepositoryUnitTest.kt` | **NEW FILE:** 29 focused unit tests covering all repository invariants, capacity, event freshness, rollback, generations, and lifecycles. |

---

## 9. Scope & Strict Stop Conditions

* **Implementation is NOT authorized.**
* **Zero Kotlin source files, dependencies, manifests, or UI files were modified.**
* Swipe-to-dismiss, undo mechanics, inline replies, download tracking, and weather integration remain strictly excluded.
