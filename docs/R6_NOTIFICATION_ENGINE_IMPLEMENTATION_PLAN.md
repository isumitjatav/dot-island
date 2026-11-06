# Nothing Island — Milestone R6: Notification Engine Architecture & Implementation Plan

**Milestone:** R6 — Notification Engine  
**Target Package:** `com.nothing.island.ai`  
**Target Device:** Nothing Phone (4a), Model A069 / FroggerIND, Android 17 / API 37  
**Display:** 1084 × 2412 (effective viewport) / 1224 × 2720 physical @ 480dpi (density 3.0), Centered Punch-Hole Cutout  
**Status:** Architecture Review Revisions (Plan Only — Pending Approval)  

---

## 1. Executive Summary & Dual-Reference Architectural Synthesis

Milestone R6 establishes a native, privacy-first notification presentation and interaction subsystem for Nothing Island. As a core product requirement, Nothing Island synthesizes the customization capabilities of both benchmark reference applications:

1. **Dynamic Notch Notification Bar:** Multi-notification handling, action button invocation, per-app inclusion/exclusion, custom preview depth (full, title-only, app-only, hidden), and inline interaction patterns.
2. **Dynamic Island Pro:** Centralized reactive state flow, configurable auto-dismiss display durations, priority arbitration, background fallback restoration, and lock-screen privacy masking.

### Visual & Platform Integrity
- **Original Assets & Nothing OS Identity:** All presentation components utilize original Jetpack Compose UI adhering strictly to Nothing OS design guidelines (monochrome palette, Nothing Red accents, NDOT / JetBrains Mono typography, clean borders, zero cartoonish third-party assets).
- **Phased Customization Rollout:** This specification provides the complete backend architectural foundation for all customization options (data models, DataStore preference keys, pipeline filter hooks, and action dispatchers) while cleanly delineating **R6 Core Foundation** from **subsequent user-facing customization UI** (e.g. rich per-app settings screens in R6.1+).

---

## 2. Codebase Audit & Architectural Invariants

### 2.1 Component State Audit

| Component | Status | Finding & Architectural Invariant |
| :--- | :--- | :--- |
| `NotificationListenerService` | **MISSING** | Does not exist in the codebase. Must be created as `IslandNotificationListenerService`. |
| `AndroidManifest.xml` | **PARTIAL** | Missing `<service>` declaration for `IslandNotificationListenerService` with `android.permission.BIND_NOTIFICATION_LISTENER_SERVICE`. Permission `POST_NOTIFICATIONS` is present. Permission `INTERNET` is completely absent (guaranteeing zero network egress). |
| `SettingsScreen.kt` | **READY** | Already implements permission status check (`NotificationManagerCompat.getEnabledListenerPackages`) and deep-link intent (`Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS`). |
| `IslandEvent.kt` | **READY** | `NotificationPosted` (Priority 55, timeout 4500ms) and `NotificationDismissed` (Priority 55, timeout 0ms) are defined. Framework objects (`Drawable`, `PendingIntent`) must remain separated via `NotificationActionBridge`. |
| `IslandPriorityManager.kt` | **READY** | Priority 55 is fully wired with 4500ms automatic timeout, preemption over media/timers (P40/P25), preemption under incoming/active calls (P100/P85), and dismissal fallback handling. |
| `IslandStateRepository.kt` | **READY** | Exposes presentation StateFlow. Requires presentation detail mapping for notification preview modes. |
| Overlay Window Anchor | **INVIOLABLE** | `IslandAccessibilityService` remains the **sole overlay window owner** at AOSP Layer 24000 (`TYPE_ACCESSIBILITY_OVERLAY`). `IslandNotificationListenerService` is strictly a headless background service with zero WindowManager access. |

---

## 3. Notification Customization Architecture & DataStore Integration

### 3.1 Preferences Data Model & Schema

All notification preferences are integrated into the existing Jetpack Preferences DataStore infrastructure via `IslandPreferencesRepository`, preserving the atomic, persistent storage verified in Milestone R5.

```kotlin
/**
 * Display preview mode for incoming notifications.
 */
enum class NotificationPreviewMode {
    FULL,       // Shows app glyph, sender/title, and message body snippet
    TITLE_ONLY, // Shows app glyph and sender/title; hides message text
    APP_ONLY,   // Shows app glyph and generic "New Notification" label
    HIDDEN      // Suppresses visual expansion; silent registration only
}

/**
 * Notification animation dynamic style.
 */
enum class NotificationAnimationStyle {
    NOTHING_SNAP,   // Crisp, fast Nothing OS mechanical transition
    NOTHING_SMOOTH, // Spring-damped fluid expansion
    REDUCED_MOTION  // Instant alpha fade without geometric resizing
}

/**
 * Immutable configuration for notification handling.
 */
data class NotificationPreferences(
    val enabled: Boolean = true,
    val displayDurationMs: Long = 4500L,                          // 2000ms to 10000ms
    val previewMode: NotificationPreviewMode = NotificationPreviewMode.FULL,
    val compactExpansionEnabled: Boolean = true,                 // Expand pill horizontally on alert
    val allowSilentNotifications: Boolean = false,               // Filter IMPORTANCE_LOW/MIN
    val allowOngoingNotifications: Boolean = false,              // Filter ongoing / sticky services
    val showOnLockscreen: Boolean = false,                       // Allow presentation on keyguard
    val hideSensitiveContent: Boolean = true,                    // Mask private notifications
    val animationStyle: NotificationAnimationStyle = NotificationAnimationStyle.NOTHING_SNAP,
    val excludedPackages: Set<String> = emptySet()               // Blacklisted package names
)
```

### 3.2 DataStore Preference Keys

```kotlin
object NotificationPreferenceKeys {
    val NOTIFICATION_ENABLED = booleanPreferencesKey("notif_enabled")
    val NOTIFICATION_DURATION_MS = longPreferencesKey("notif_duration_ms")
    val NOTIFICATION_PREVIEW_MODE = stringPreferencesKey("notif_preview_mode")
    val NOTIFICATION_COMPACT_EXPAND = booleanPreferencesKey("notif_compact_expand")
    val NOTIFICATION_ALLOW_SILENT = booleanPreferencesKey("notif_allow_silent")
    val NOTIFICATION_ALLOW_ONGOING = booleanPreferencesKey("notif_allow_ongoing")
    val NOTIFICATION_SHOW_ON_LOCKSCREEN = booleanPreferencesKey("notif_show_lockscreen")
    val NOTIFICATION_HIDE_SENSITIVE = booleanPreferencesKey("notif_hide_sensitive")
    val NOTIFICATION_ANIMATION_STYLE = stringPreferencesKey("notif_anim_style")
    val NOTIFICATION_EXCLUDED_PACKAGES = stringSetPreferencesKey("notif_excluded_packages")
}
```

### 3.3 Scope Boundaries: Foundation vs UI Work

To maintain manageable, verified milestones:
- **Milestone R6 Scope (Foundation & Pipeline):**
  - Implement the `NotificationPreferences` data model and default preferences in `IslandPreferencesRepository`.
  - Connect all filtering, duration, preview mode, and exclusion checks in `NotificationFilter` and `IslandPriorityManager`.
  - Provide safe, verified default values (4.5s duration, `FULL` preview, silent/ongoing suppressed, self-excluded).
- **Subsequent UI Scope (Milestone R6.1+):**
  - Build dedicated `NotificationSettingsScreen` with per-app installed application picker, toggles for ongoing/silent alerts, preview mode dropdown, and duration sliders.

---

## 4. Multi-Stage Notification Filtering & Ingestion Pipeline

### 4.1 Ingestion Flowchart

```
StatusBarNotification (AOSP NotificationListenerService)
                    │
                    ▼
     [ STAGE 1: MANDATORY EXCLUSIONS ]
     ├─ sbn.packageName == context.packageName ──────────► DROP (Self Foreground Service ID 4040)
     ├─ category == CATEGORY_TRANSPORT || hasMediaStyle() ► DROP (Reserved for R7 Media Engine)
     └─ category == CATEGORY_CALL ────────────────────────► DROP (Reserved for R9 Telephony Engine)
                    │ Passed
                    ▼
     [ STAGE 2: CONFIGURABLE EXCLUSIONS ]
     ├─ !prefs.enabled ───────────────────────────────────► DROP (Master Toggle Off)
     ├─ prefs.excludedPackages.contains(sbn.packageName) ─► DROP (Per-App Blacklist)
     ├─ sbn.isOngoing && !prefs.allowOngoingNotifications ► DROP (Ongoing Services Suppressed)
     └─ isLowImportance && !prefs.allowSilentNotifications ► DROP (Silent / Min Importance Suppressed)
                    │ Passed
                    ▼
     [ STAGE 3: GROUP & SUMMARY ARBITRATION ]
     ├─ isGroupSummary && hasActiveGroupChildren(key) ────► DROP / COALESCE (Prevents Duplicate Spurt)
     └─ isGroupSummary && !hasActiveGroupChildren(key) ───► ALLOW (Standalone Group Summary)
                    │ Passed
                    ▼
     [ STAGE 4: METADATA EXTRACTION & PRIVACY MASKING ]
     ├─ Check KeyguardManager.isKeyguardLocked()
     ├─ Apply PreviewMode (FULL, TITLE_ONLY, APP_ONLY, HIDDEN)
     ├─ Register PendingIntent in NotificationActionBridge
     └─ Package into IslandEvent.NotificationPosted (P55, durationMs)
                    │
                    ▼
     [ STAGE 5: ISLAND EVENT ENGINE DISPATCH ]
     └─ eventEngine.emit(event)
```

### 4.2 Categorization of Exclusions

#### A. Mandatory Exclusions (Hardcoded & Inviolable)
1. **Self-Package:** `sbn.packageName == context.packageName`
   - *Rationale:* Nothing Island maintains a persistent foreground service (`IslandOverlayService`, ID 4040). Flashing Island's own service notification would cause an infinite visual feedback loop.
2. **Media Playback:** `category == Notification.CATEGORY_TRANSPORT` or `notification.extras.containsKey(Notification.EXTRA_MEDIA_SESSION)` or notification has `MediaStyle`.
   - *Rationale:* Milestone R7 is dedicated to full media session tracking, waveform rendering, and scrubbers. Treating media player position ticks as notification popups causes severe visual thrashing.
3. **Telephony Calls:** `category == Notification.CATEGORY_CALL`.
   - *Rationale:* Milestone R9 handles full-screen and floating call screening with dedicated P100/P85 priority arbitration.

#### B. Configurable Exclusions (User-Governed via Preferences)
1. **Ongoing Notifications (`sbn.isOngoing` or `FLAG_ONGOING_EVENT`):**
   - *Default:* Filtered out.
   - *User Option:* Can be permitted for specific categories (e.g., active navigation, live sports, download progress).
2. **Low Importance / Silent Alerts:**
   - *Evaluation:* `ranking.importance < NotificationListenerService.Ranking.IMPORTANCE_DEFAULT` or `sbn.notification.priority < Notification.PRIORITY_DEFAULT`.
   - *Default:* Suppressed from heads-up popup.
   - *User Option:* Toggled via `allowSilentNotifications`.
3. **Per-App Inclusions/Exclusions (`excludedPackages`):**
   - User can blacklist noisy apps or whitelist high-priority apps.
4. **Lockscreen Presentation:**
   - Evaluated dynamically against `KeyguardManager.isKeyguardLocked()`. If `showOnLockscreen` is false, alerts are suppressed while device is locked.

### 4.3 Grouped vs. Child Notification Handling

Android communication apps (WhatsApp, Gmail, Telegram, Slack) emit both individual message notifications and conversation group summaries.
- **Child Notification:** Individual message card with content (`(flags & FLAG_GROUP_SUMMARY) == 0`).
- **Group Summary:** Aggregated container card (`(flags & FLAG_GROUP_SUMMARY) != 0`).

**Arbitration Logic:**
1. Maintain an in-memory sliding registry of active group keys: `ConcurrentHashMap<String, MutableSet<String>>` mapping `groupKey` to `childNotificationKeys`.
2. When a child notification arrives:
   - Record child key under `groupKey`.
   - Deliver child alert to `IslandEventEngine`.
3. When a group summary arrives:
   - Check if active child keys exist for `groupKey`.
   - If child keys exist: **Suppress the group summary** to prevent duplicate visual alerting.
   - If no child keys exist (e.g. standalone notification "3 new messages from 2 chats"): **Deliver the summary alert** so messages are not missed.
4. When a notification is removed:
   - Remove key from the active group tracking registry.

---

## 5. Headless Service Lifecycle & Active Notification Reconciliation

`IslandNotificationListenerService` operates strictly as a background headless service managed by the Android operating system.

### 5.1 Service Connection (`onListenerConnected()`)
When the OS binds to the listener service:
1. Set `@Volatile isConnected = true`.
2. Register active listener instance in static reference `currentServiceInstance = this` (weak/guarded) for dismissal invocation.
3. **Silent Initial Reconciliation:**
   - Query `activeNotifications: Array<StatusBarNotification>?`.
   - Populate the internal `NotificationActionBridge` and active group tracking cache.
   - **DO NOT** dispatch historical notifications into `IslandEventEngine`. Blasting 30 active notifications on service launch would overwhelm the UI and degrade user experience.
   - Mark reconciled keys as historical. Only notifications posted after `onListenerConnected()` trigger foreground island alerts.

### 5.2 Service Disconnection & Cleanup (`onListenerDisconnected()`)
When the OS unbinds the service (or user revokes permission):
1. Set `isConnected = false`.
2. Clear static listener reference: `currentServiceInstance = null`.
3. Clean up `NotificationActionBridge` volatile memory caches.
4. If an active notification is currently displayed on the island: dispatch `IslandEvent.UserDismissCurrent` to return the island cleanly to base idle or background fallback.
5. **Rebinding Resilience:** If disconnection was unexpected (e.g. OS memory trimming) and permission remains granted, attempt `requestRebind(ComponentName(this, IslandNotificationListenerService::class.java))`.

---

## 6. Event Identity, Updates, Multi-Alert Queuing, and Dismissal Synchronization

### 6.1 Stable Notification Identity
- Android generates a globally unique, stable string key for every notification: `sbn.key` (e.g. `0|com.whatsapp|1001|null|10123`).
- `sbn.key` serves as the canonical `id` in `IslandEvent.NotificationPosted` and `targetNotificationId` in `IslandEvent.NotificationDismissed`.

### 6.2 Handling Notification Updates
When an app posts an update with the same `sbn.key` (e.g. a messaging app updating a chat with a second message):
1. `NotificationFilter` detects existing key.
2. Checks whether the business payload has changed via `hasIdenticalBusinessPayload()`.
3. If payload changed (new message text):
   - Updates cached `PendingIntent` and metadata.
   - Emits updated `NotificationPosted` with refreshed timestamp and duration.
   - In UI: crossfades the message text smoothly without collapsing and re-expanding the pill width.

### 6.3 Multiple Simultaneous Notifications (Presentation Queue & Preemption)
When multiple distinct notifications arrive in rapid succession (e.g. App A, then App B 1.5 seconds later):
- **Priority Manager Behavior:** Both events have equal priority (`Priority 55`).
- Under equal priority rules:
  1. The newer notification (App B) activates cleanly.
  2. The previous 4.5-second timeout is cancelled and restarted for App B.
  3. Presentation details in `IslandStateRepository` are updated.
- **Stacking Indicator:** In expanded mode, a badge indicates `"1 OF 2 NOTIFICATIONS"` or shows the count of recent unread alerts captured during the active window.

### 6.4 Two-Way Dismissal Synchronization & Stale Protection
1. **System-to-Island Dismissal:**
   - User swipes notification away in the Android notification drawer.
   - OS triggers `onNotificationRemoved(sbn)`.
   - Service dispatches `IslandEvent.NotificationDismissed(targetNotificationId = sbn.key)`.
   - `IslandPriorityManager` checks:
     ```kotlin
     if (activeEvent is IslandEvent.NotificationPosted && activeEvent.id == event.targetNotificationId) {
         dismissActivePresentation(reason = "NotificationDismissed")
     }
     ```
   - **Stale Protection:** If Notification A is dismissed after Notification B has already become active, `activeEvent.id` (B) does not match `event.targetNotificationId` (A). The active presentation of B remains completely undisturbed.
2. **Island-to-System Dismissal:**
   - User taps "DISMISS" or swipes card on Island overlay.
   - Island dispatches `IslandEvent.UserDismissCurrent` and calls `IslandNotificationListenerService.cancelSystemNotification(event.id)`.
   - Service calls `cancelNotification(key)` on the active listener instance, clearing the alert from the Android system drawer.

---

## 7. Clean Architecture Boundaries & Action Invocation

### 7.1 Separation of Platform-Independent Models from Framework Objects
To avoid memory leaks, serialization failures, and JVM unit test mocking complexities, Android framework objects (`PendingIntent`, `Drawable`, `Bitmap`, `Icon`) are **strictly prohibited** from the core `IslandEvent` hierarchy.

```
┌────────────────────────────────────────────────────────┐
│               IslandEvent.NotificationPosted           │
│  - id: String (sbn.key)                                │
│  - packageName: String                                 │
│  - appLabel: String                                    │
│  - title: String                                       │
│  - text: String                                        │
│  - subText: String?                                    │
│  - category: String?                                   │
│  - hasContentIntent: Boolean                           │
│  - isClearable: Boolean                                │
│  - timestamp: Long                                     │
│  - timeoutMs: Long                                     │
└────────────────────────────────────────────────────────┘
                           │
                 Decoupled Bridge Boundary
                           │
┌────────────────────────────────────────────────────────┐
│               NotificationActionBridge                 │
│  - intentRegistry: ConcurrentHashMap<String, PendingIntent>
│  - actionRegistry: ConcurrentHashMap<String, List<ActionItem>>
│  - fun executeContentIntent(key: String, ctx: Context) │
│  - fun executeAction(key: String, actionIndex: Int)    │
│  - fun clear(key: String)                              │
└────────────────────────────────────────────────────────┘
```

### 7.2 Safe Invocation of Notification Actions
```kotlin
object NotificationActionBridge {
    private const val TAG = "NotifActionBridge"
    private val contentIntents = ConcurrentHashMap<String, PendingIntent>()

    fun register(key: String, intent: PendingIntent?) {
        if (intent != null) contentIntents[key] = intent else contentIntents.remove(key)
    }

    fun executeContentIntent(key: String): Boolean {
        val intent = contentIntents[key] ?: return false
        return try {
            intent.send()
            Log.d(TAG, "Successfully fired PendingIntent for key=$key")
            true
        } catch (e: PendingIntent.CanceledException) {
            Log.w(TAG, "PendingIntent canceled for key=$key", e)
            false
        }
    }

    fun clear(key: String) {
        contentIntents.remove(key)
    }
}
```

---

## 8. Privacy, Security & Threat Model Safeguards

1. **Zero Content Persistence Guarantee:**
   - Notification titles, sender names, and message bodies exist strictly in volatile RAM.
   - Message payloads are **never written** to Jetpack DataStore, SharedPreferences, SQLite, Room, or disk files.
   - Upon timeout, dismissal, or service termination, all notification records are immediately cleared and garbage collected.
2. **Zero Network Egress (Physical Guarantee):**
   - `AndroidManifest.xml` does not declare `android.permission.INTERNET`.
   - Android OS enforces sandboxed network isolation: the application is physically incapable of transmitting notification contents over the network.
3. **Zero Content Logging:**
   - In both debug and release builds, logging is strictly limited to structural metadata (`packageName`, `category`, `sbn.key`).
   - Logging of `EXTRA_TITLE`, `EXTRA_TEXT`, or `EXTRA_BIG_TEXT` is strictly forbidden.
4. **Lock-Screen Sensitive-Content Masking:**
   - Check `KeyguardManager.isKeyguardLocked()` and notification visibility flags:
     - `Notification.VISIBILITY_SECRET`: Completely suppressed while screen is locked.
     - `Notification.VISIBILITY_PRIVATE`: Displays app icon and "New Notification"; message body is masked.
     - If user preference `hideSensitiveContent` is enabled: message body is masked whenever device is locked, regardless of app default flags.
5. **Realistic Security Scope & Threat Disclosure:**
   - Protections rely upon the standard Android AOSP application sandbox. Rooted devices, accessibility sniffers, or compromised OS kernels could bypass application-layer boundaries. Nothing Island claims zero telemetry and robust on-device privacy, backed by manifest restrictions and open verification.

---

## 9. Geometry & Cutout Validation for Nothing Phone (4a)

### 9.1 Hardware Baseline Invariants
- **Device Model:** Nothing Phone (4a) (`A069` / `FroggerIND`)
- **Punch-Hole Cutout:** Top-centered, ~11mm diameter (54dp / 162px @ 480dpi).
- **Idle Capsule Dimensions:** Width `124dp`, Height `36dp`, Corner Radius `18dp`, Top Margin `9dp` (27px).
- **Boundary Rule:** The idle capsule geometry established in Milestone R4.6 and calibrated in Milestone R5 must remain **completely unchanged** when no notification is active.

### 9.2 Compact Notification Preview Geometry
When a notification arrives and `compactExpansionEnabled` is true:
- **Height & Vertical Alignment:** Remains exactly `36dp` (108px), preserving seamless alignment with the status bar icons.
- **Corner Radius:** Remains exactly `18dp` (full pill radius).
- **Animated Width Expansion:**
  - Base width: `124dp` -> Expands symmetrically to `200dp` (centered on camera punch-hole).
  - Animation spec: `animateDpAsState(targetValue = 200.dp, animationSpec = tween(280, easing = FastOutSlowInEasing))`.
- **Symmetric Layout around Centered Punch-Hole:**
  - `[ Left Wing: 73dp ]` `[ Centered Punch-Hole: 54dp ]` `[ Right Wing: 73dp ]` = `200dp` total width.
  - **Left Wing:** App icon / glyph (16dp × 16dp) centered with 8dp start padding.
  - **Punch-Hole Clearance:** 54dp center zone remains completely clear of text or interactive elements.
  - **Right Wing:** Sender name or notification title truncated to 12 characters (Monospace, 11sp, `NothingWhite`).

### 9.3 Expanded Mode Geometry (`NotificationCard`)
- **Dimensions:** Width `350dp`, Height `210dp`, Corner Radius `26dp`.
- **Top Spacer:** `48dp` (144px) clearance ensuring header text and action chips render strictly below the physical punch-hole and status bar gesture zone.

---

## 10. Comprehensive Test Matrix

### 10.1 Automated Unit Tests (`app/src/test/java/com/nothing/island/ai/notifications/`)

| Test Suite | Test Method | Target Specification |
| :--- | :--- | :--- |
| `NotificationFilterTest` | `testSelfPackageExcluded` | `com.nothing.island.ai` notifications return null (dropped). |
| `NotificationFilterTest` | `testMediaTransportCategoryExcluded` | `CATEGORY_TRANSPORT` and `MediaStyle` return null (dropped). |
| `NotificationFilterTest` | `testCallCategoryExcluded` | `CATEGORY_CALL` returns null (dropped). |
| `NotificationFilterTest` | `testOngoingNotificationRespectsPreferences` | Filtered when `allowOngoingNotifications=false`; accepted when `true`. |
| `NotificationFilterTest` | `testSilentNotificationRespectsPreferences` | Filtered when `allowSilentNotifications=false`; accepted when `true`. |
| `NotificationFilterTest` | `testPerAppExclusionBlacklist` | Package present in `excludedPackages` is rejected immediately. |
| `NotificationFilterTest` | `testGroupSummarySuppressionWhenChildrenActive` | Group summary rejected if child key recorded for same `groupKey`. |
| `NotificationFilterTest` | `testStandaloneGroupSummaryAccepted` | Group summary accepted if no active child keys recorded. |
| `NotificationFilterTest` | `testPreviewModesMetadataMasking` | Verifies `FULL`, `TITLE_ONLY`, `APP_ONLY`, `HIDDEN` payload formatting. |
| `NotificationFilterTest` | `testLockscreenMaskingWhenKeyguardActive` | Body text suppressed when `isKeyguardLocked = true`. |
| `NotificationEngineTest` | `testNotificationPostedPreemptsIdle` | Emitting `NotificationPosted` transitions state from idle to P55 alert. |
| `NotificationEngineTest` | `testNotificationPostedPreemptsMedia` | Emitting `NotificationPosted` (P55) preempts background media (P40). |
| `NotificationEngineTest` | `testIncomingCallPreemptsNotification` | Emitting `IncomingCallRinging` (P100) preempts active notification (P55). |
| `NotificationEngineTest` | `testAutoCollapseAfterConfigurableDuration` | Verified for 2000ms, 4500ms, and 8000ms timeout settings. |
| `NotificationEngineTest` | `testDismissalRestoresMediaFallback` | Dismissing notification restores active Spotify playback fallback. |
| `NotificationEngineTest` | `testStaleDismissalDoesNotDismissNewerEvent` | Dismissal for Event A does not cancel newly active Event B. |
| `NotificationEngineTest` | `testRapidConsecutiveNotificationsQueuing` | Verifies equal-priority preemption and timeout restart for rapid arrivals. |
| `NotificationEngineTest` | `testSemanticDeduplicationWithin250ms` | Identical business payload from same app within 250ms is deduplicated. |
| `NotificationActionBridgeTest` | `testActionInvocationAndCleanup` | Verifies `executeContentIntent` calls `PendingIntent.send()` and handles cancel. |
| `RegressionTest` | `testR31EngineReliabilityInvariants` | All 22 R3.1 engine tests continue to pass 100%. |
| `RegressionTest` | `testR5PersistentPreferencesInvariants` | All 10 R5 DataStore preferences tests continue to pass 100%. |

### 10.2 Physical Hardware Test Plan on Nothing Phone (4a)

1. **Permission Grant & Service Binding:**
   - Execute: `adb shell "cmd notification allow_listener com.nothing.island.ai/com.nothing.island.ai.notifications.IslandNotificationListenerService"`
   - Open Nothing Island `SettingsScreen`, verify "Notification Listener Access" indicator displays "ACTIVE".
2. **Reconciliation & Silent Startup:**
   - Populate status bar with existing notifications (WhatsApp, Telegram).
   - Kill and relaunch Nothing Island process (`adb shell am force-stop com.nothing.island.ai && am start -n com.nothing.island.ai/.MainActivity`).
   - Verify: Overlay starts in idle capsule mode (124dp × 36dp) with **ZERO** notification flooding.
3. **Notification Ingestion & Symmetrical Expansion:**
   - Execute: `adb shell "cmd notification post -t 'Nothing OS' test_alert 'Clean notification preview'"`
   - Verify: Compact capsule expands horizontally to 200dp, symmetrically wrapping the centered punch-hole.
   - Verify: App glyph on left, truncated preview on right.
   - Verify: Auto-collapses cleanly after exactly 4.5 seconds to 124dp × 36dp.
4. **Touch Expansion & Interaction:**
   - Trigger notification via ADB.
   - Tap expanded capsule before 4.5s timeout.
   - Verify: Expands to full `NotificationCard` (350dp × 210dp) below the camera punch-hole.
   - Tap "COLLAPSE" -> returns to compact pill.
5. **Two-Way Dismissal Verification:**
   - Trigger notification via ADB.
   - Tap "DISMISS" on Island card -> verify Island collapses AND notification is removed from Android notification shade (`adb shell cmd notification list`).
   - Post second notification, swipe away from Android notification drawer -> verify Island card closes immediately.
6. **Multi-Notification Arrival:**
   - Trigger Alert A, then 1 second later trigger Alert B.
   - Verify: Alert B cleanly updates presentation, resetting the 4.5s countdown.
7. **Permission Revocation Resilience:**
   - Revoke permission via ADB (`adb shell cmd notification disallow_listener ...`).
   - Verify: Service unbinds cleanly without crashing; UI updates status; overlay remains intact at Layer 24000.

---

## 11. Phased Implementation Roadmap & Acceptance Criteria

```
┌──────────────────────────────────────────────────────────────────────────────┐
│ Phase 1: Service Skeleton, Manifest Wiring & Permission Reconciliation       │
│ - Create IslandNotificationListenerService (headless)                        │
│ - Declare BIND_NOTIFICATION_LISTENER_SERVICE in AndroidManifest.xml          │
│ - Implement onListenerConnected() with silent historical reconciliation      │
│ - Acceptance: Service binds on hardware and logs onListenerConnected() cleanly│
└──────────────────────────────────────┬───────────────────────────────────────┘
                                       │
                                       ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│ Phase 2: Processing Pipeline, Filtering & NotificationActionBridge          │
│ - Implement NotificationFilter (mandatory & configurable rules)              │
│ - Implement group summary vs child arbitration                               │
│ - Implement NotificationActionBridge (PendingIntent decoupled execution)     │
│ - Implement NotificationPreferences schema in IslandPreferencesRepository    │
│ - Acceptance: 100% unit tests pass across filtering and deduplication        │
└──────────────────────────────────────┬───────────────────────────────────────┘
                                       │
                                       ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│ Phase 3: Presentation Layer (Dynamic Capsule & NotificationCard)             │
│ - Build NotificationCard composable adhering strictly to Nothing OS design   │
│ - Implement 200dp symmetric horizontal expansion in IslandOverlayView        │
│ - Wire tap-to-expand, PendingIntent execution, and dismiss synchronization   │
│ - Acceptance: Smooth 200dp expansion on hardware; 4.5s auto-collapse; dismiss│
└──────────────────────────────────────┬───────────────────────────────────────┘
                                       │
                                       ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│ Phase 4: Hardware Verification & Milestone Closure                           │
│ - Execute complete physical test matrix on Nothing Phone (4a)                │
│ - Verify zero layer leaks, zero window recreation at Layer 24000             │
│ - Produce docs/R6_NOTIFICATION_ENGINE_IMPLEMENTATION_REPORT.md               │
│ - Acceptance: 100% test pass rate, hardware signed off                       │
└──────────────────────────────────────────────────────────────────────────────┘
```

---

## 12. Approval Sign-Off Request

This revised architecture plan incorporates all eight review requirements:
1. Notification customization architecture and persistent preferences schema.
2. Distinct mandatory vs configurable filtering with group summary arbitration.
3. Silent initial reconciliation and robust service connection lifecycle.
4. Stable `sbn.key` identity, multi-notification queues, and stale dismissal protection.
5. Strict architectural boundaries decoupling `PendingIntent` via `NotificationActionBridge`.
6. Zero content persistence, zero network egress, and lockscreen masking.
7. Validated Nothing Phone (4a) centered cutout geometry (124dp idle / 200dp preview).
8. Expanded 21-test unit matrix and physical ADB hardware test suite.

**Awaiting user approval to proceed with Phase 1.**
