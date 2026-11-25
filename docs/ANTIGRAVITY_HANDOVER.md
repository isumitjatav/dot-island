# [HISTORICAL — SUPERSEDED] NOTHING ISLAND — ANTIGRAVITY PROJECT HANDOVER

> **HISTORICAL NOTICE:** This document captures an early Milestone **R3.1** snapshot (November 5, 2026) and is **stale**. It predates Git initialization, DataStore persistence, Notification/Media/Telephony/Bluetooth engines, Android instrumentation tests, and R8 release hardening. Refer to [HANDOVER_2026-11-22.md](./HANDOVER_2026-11-22.md) and [RELEASE_BUILD_CHECKLIST.md](./RELEASE_BUILD_CHECKLIST.md) for current platform status.

**Document Version:** 1.0.0  
**Handover Date:** November 5, 2026  
**Project Name:** Nothing Island  
**Application Package:** `com.nothing.island.ai`  
**Target Hardware:** Nothing Phone (4a) (`A069` / `FroggerIND`)  
**OS Version:** Android 17 (API 37)  
**Display Profile:** 1224 × 2720 @ 480dpi (`density = 3.0`)  
**Workspace Root:** `C:\Users\User\.gemini\antigravity\scratch\nothing-island`  

---

## 1. EXECUTIVE SUMMARY & CURRENT ARCHITECTURAL STATE

Nothing Island is an ultra-minimalist, high-performance floating Dynamic Island implementation engineered specifically for the centered punch-hole camera of the **Nothing Phone (4a)**. It follows the signature Nothing OS aesthetic (monochrome capsule, red recording accent, dot-matrix typographic styling, and breathing glow).

As of Milestone **R3.1**, Nothing Island runs on a **production-hardened, two-tier architecture**:
1. **Window Hosting Layer (`TYPE_ACCESSIBILITY_OVERLAY`):** Anchored at AOSP Base Layer 24000 by `IslandAccessibilityService`, sitting above SystemUI's StatusBar (Layer 22000). This grants 100% direct touch reception across the physical front camera punch-hole cutout while maintaining pass-through for touches outside its 124dp × 36dp capsule.
2. **Unified Reactive Event & State Engine:** Powered by a dual-flow Kotlin coroutines staging pipeline (`IslandEventEngine`), priority-driven preemption arbiter (`IslandPriorityManager`), decoupled visual state repository (`IslandStateRepository`), and reactive WindowManager dimension coordinator (`IslandOverlayController`).

The application is fully verified on physical Nothing Phone (4a) hardware and passes 100% of its automated unit test suite (22/22 tests).

---

## 2. COMPLETED MILESTONES HISTORY

| Milestone | Title | Key Deliverables & Achievements | Status |
|:---:|:---|:---|:---:|
| **M1** | Project Bootstrap & UI Scaffold | Established Gradle project, Jetpack Compose UI, Nothing OS design tokens, NDot typography, and settings scaffolding. | **CLOSED** |
| **M2** | Floating Overlay Architecture | Implemented initial WindowManager `TYPE_APPLICATION_OVERLAY` host, cut-out detection, and `IslandOverlayService` foreground lifecycle. | **CLOSED** |
| **M3** | Touch Interaction & APK Reverse Engineering | Decompiled and reverse-engineered reference apps (`com.dynamicisland.notchscreenview` and `tr.com.eywin.dynamic.island.notch`); stabilized touch coordinates. | **CLOSED** |
| **M4.1** | Geometry Calibration & Bridges | Calibrated camera offsets (`x = 0`, `y = 27px` / 9dp); introduced coordinate bridge experiments. | **CLOSED** |
| **R4** | Floating Overlay Stabilization | Restored pure floating capsule aesthetic (124dp × 36dp, 18dp corner radius); eliminated box-shaped distortion. | **CLOSED** |
| **R4.5** | Accessibility Overlay Architecture Investigation | Comprehensive architectural study identifying Layer 24000 migration path to eliminate status bar touch clipping. | **CLOSED** |
| **R4.6** | Accessibility Overlay Implementation | Implemented `IslandAccessibilityService` at Layer 24000; enabled direct punch-hole camera touch interaction. | **CLOSED** |
| **R4.6.1** | Compact Indicator Cleanup | Removed permanent decorative dots; established zero-indicator idle aesthetic in accordance with Nothing OS minimalism. | **CLOSED** |
| **R3** | Unified Event & State Engine | Built reactive Flow event bus, 10-domain sealed event hierarchy (`IslandEvent`), priority tiers (P0–P100), and state repository. | **CLOSED** |
| **R3.1** | Targeted Reliability Hardening | Resolved 9 forensic audit findings: dual-flow critical isolation, non-destructive media coalescing, timer purge, semantic deduplication, call fallback restoration, idle badge preservation, detach cleanup, and reactive WindowManager resizing. | **CLOSED & VERIFIED** |

---

## 3. PROJECT DIRECTORY STRUCTURE

```
nothing-island/
├── app/
│   ├── build.gradle.kts
│   ├── src/
│   │   ├── main/
│   │   │   ├── AndroidManifest.xml
│   │   │   ├── java/com/nothing/island/ai/
│   │   │   │   ├── MainActivity.kt               # Entrypoint & Settings activity
│   │   │   │   ├── CutoutDetector.kt             # Hardware punch-hole geometry metrics
│   │   │   │   ├── IslandAccessibilityService.kt # Layer 24000 overlay window host
│   │   │   │   ├── IslandOverlayService.kt       # Legacy/fallback foreground service
│   │   │   │   ├── IslandOverlayController.kt    # Singleton overlay window manager
│   │   │   │   ├── IslandOverlayView.kt          # Compose UI capsule & expanded card
│   │   │   │   ├── IslandState.kt                # Legacy Compose UI state model
│   │   │   │   ├── OverlayLifecycleOwner.kt      # Standalone Compose LifecycleOwner
│   │   │   │   ├── OverlayPermissionManager.kt   # Permission helper (Overlay & A11y)
│   │   │   │   ├── engine/                       # Core Reactive Engine (Milestone R3/R3.1)
│   │   │   │   │   ├── IslandEvent.kt            # Sealed event hierarchy & semantic payload equality
│   │   │   │   │   ├── IslandEventEngine.kt      # Dual-flow bounded event bus & coalescing
│   │   │   │   │   ├── IslandPriorityManager.kt  # Priority preemption & fallback arbiter
│   │   │   │   │   └── IslandStateRepository.kt  # StateFlow single source of truth
│   │   │   │   ├── navigation/
│   │   │   │   │   └── IslandDestination.kt      # In-app navigation routes
│   │   │   │   └── ui/
│   │   │   │       ├── components/               # Custom Nothing OS dot-matrix widgets
│   │   │   │       ├── screens/                  # Home, Appearance, Features, Settings
│   │   │   │       └── theme/                    # Color, Typography, Theme
│   │   │   └── res/
│   │   │       ├── values/strings.xml
│   │   │       └── xml/accessibility_service_config.xml
│   │   └── test/java/com/nothing/island/ai/engine/
│   │       └── IslandEngineUnitTest.kt           # 22 automated unit tests
├── docs/                                         # Comprehensive milestone reports
│   ├── ANTIGRAVITY_HANDOVER.md                   # This document
│   ├── NOTHING_ISLAND_MASTER_ROADMAP.md          # Multi-milestone roadmap
│   ├── R3_1_RELIABILITY_HARDENING_REPORT.md      # R3.1 hardening completion report
│   ├── R3_POST_IMPLEMENTATION_AUDIT.md           # R3 forensic audit report
│   ├── R3_UNIFIED_EVENT_STATE_IMPLEMENTATION_REPORT.md
│   ├── R4_6_1_INDICATOR_CLEANUP_REPORT.md
│   ├── R4_6_ACCESSIBILITY_OVERLAY_IMPLEMENTATION_REPORT.md
│   ├── R4_5_ACCESSIBILITY_OVERLAY_ARCHITECTURE_REPORT.md
│   ├── R4_OVERLAY_STABILIZATION_REPORT.md
│   └── ... (Reverse engineering reports)
├── build.gradle.kts
├── settings.gradle.kts
└── gradlew.bat
```

---

## 4. IMPORTANT SOURCE FILES & RESPONSIBILITIES

### Core Engine Files (`com.nothing.island.ai.engine`)

1. **`IslandEvent.kt`**
   - Sealed interface defining 10 domain event categories: Notifications, Media, Battery, Telephony, Biometric Unlock, Bluetooth, Timers, Recording, Touch Interactions, and Lifecycle.
   - Defines strict priority tiers: `CRITICAL_TELEPHONY` (100), `ACTIVE_TELEPHONY` (85), `SAFETY_ALERT` (70), `NOTIFICATION` (55), `MEDIA_PLAYBACK` (40), `LIVE_ACTIVITY` (25), `RECORDING_INDICATOR` (20), `SYSTEM_HARDWARE` (15), `USER_INTERACTION` (10), `IDLE_BASE` (0).
   - Contains `hasIdenticalBusinessPayload(other)` for semantic deduplication excluding generation timestamps.

2. **`IslandEventEngine.kt`**
   - Centralized, bounded, priority-aware Kotlin Flow event bus.
   - Dual-buffer staging: `_criticalEvents` (`capacity = 32`) isolated from `_standardEvents` (`capacity = 64`).
   - Zero-subscriber offline cache (`MAX_RETAINED_CRITICAL = 16`) ensuring incoming calls arriving during service reconnect are never dropped.
   - Intelligent coalescing for high-frequency progress ticks that preserves play/pause and track-change transitions.

3. **`IslandPriorityManager.kt`**
   - Mutex-protected priority arbiter enforcing preemption rules:
     - **Rule 1 (Higher Priority):** Immediate preemption of active presentation.
     - **Rule 2 (Lower Priority):** Foreground rejection; cached in `backgroundFallbacks` if persistent (`timeoutMs == 0L`).
     - **Rule 3 (Equal Priority):** Deterministic replacement (Priority 0 lifecycle events bypass replacement to preserve Base Idle).
   - Manages cancellable coroutine timeout jobs and automatically restores highest background fallback upon expiration.
   - Purges `"TIMER"` fallback upon countdown completion to prevent timer resurrection.

4. **`IslandStateRepository.kt`**
   - Single source of truth exposing `state: StateFlow<IslandMasterState>`.
   - Completely decoupled from Android Views and Context.
   - Translates domain events into presentation titles, subtitles, and badges (`toLegacyIslandState()`).
   - Supports display mode toggling (`COLLAPSED` vs `EXPANDED`).

### Overlay Host & UI Files (`com.nothing.island.ai`)

5. **`IslandAccessibilityService.kt`**
   - Host service for `TYPE_ACCESSIBILITY_OVERLAY` (2032).
   - Mounts at Layer 24000 upon `onServiceConnected()`.
   - Operates under a strict Zero-Data Privacy Contract (all data retrieval, gesture injection, and key monitoring capabilities set to `false`).

6. **`IslandOverlayController.kt`**
   - Centralized singleton controller managing window lifecycle, ComposeView mounting, and touch listener configuration.
   - Observes `repository.state.map { it.mode }.distinctUntilChanged()` to reactively update WindowManager layout params on `mainHandler`.
   - Dispatches `repository.resetToIdle()` in `detachInternal()` to prevent stale presentation leakage across reconnection.

7. **`IslandOverlayView.kt`**
   - Jetpack Compose root overlay view rendering either the compact 124dp × 36dp capsule or the expanded 350dp × 210dp card.
   - Features Nothing OS dot-matrix typography, animated breathing accent glow, and touch handling for expand/pulse toggles.

8. **`CutoutDetector.kt`**
   - Detects hardware punch-hole dimensions and display metrics.
   - Standard defaults for Nothing Phone (4a): compact width 124dp (372px), compact height 36dp (108px), top margin 9dp (27px), corner radius 18dp (54px), expanded width 350dp (1050px), expanded height 210dp (630px).

---

## 5. DEPENDENCY & ENVIRONMENT SPECIFICATIONS

- **Gradle Version:** `8.11.1` (`gradle/wrapper/gradle-wrapper.properties`)
- **Android Gradle Plugin (AGP):** `8.9.1`
- **Kotlin Version:** `2.1.0`
- **Compose Compiler Plugin:** `2.1.0`
- **Compose BOM:** `2024.12.01`
- **Java Compatibility:** Java 17 (`JavaVersion.VERSION_17`, `jvmTarget = "17"`)
- **Android SDK:**
  - `compileSdk = 36` (Android 16 / VanillaIceCream preview extension)
  - `targetSdk = 36`
  - `minSdk = 29` (Android 10)
- **Key Dependencies:**
  - `androidx.compose.material3:material3`
  - `androidx.activity:activity-compose:1.9.3`
  - `androidx.lifecycle:lifecycle-runtime-ktx:2.8.7`
  - `org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1`
  - `junit:junit:4.13.2`

---

## 6. PERMISSIONS & ACCESSIBILITY CONFIGURATION

### Declared Permissions (`AndroidManifest.xml`)
- `android.permission.SYSTEM_ALERT_WINDOW`: Required for fallback `TYPE_APPLICATION_OVERLAY`.
- `android.permission.FOREGROUND_SERVICE`: Required for background lifecycle persistence.
- `android.permission.FOREGROUND_SERVICE_SPECIAL_USE`: Android 14+ subtype declaration (`Floating camera cutout UI overlay`).
- `android.permission.POST_NOTIFICATIONS`: Service notification channel posting.
- `android.permission.BIND_ACCESSIBILITY_SERVICE`: Guarding `IslandAccessibilityService`.

### Accessibility Configuration (`res/xml/accessibility_service_config.xml`)
```xml
<accessibility-service xmlns:android="http://schemas.android.com/apk/res/android"
    android:description="@string/accessibility_service_description"
    android:summary="@string/accessibility_service_summary"
    android:accessibilityEventTypes="typeWindowStateChanged"
    android:accessibilityFeedbackType="feedbackGeneric"
    android:notificationTimeout="100"
    android:accessibilityFlags="flagDefault"
    android:canRetrieveWindowContent="false"
    android:canPerformGestures="false"
    android:canRequestFilterKeyEvents="false"
    android:canRequestTouchExplorationMode="false" />
```
> **Privacy Guarantee:** The service does **not** inspect screen content, monitor keystrokes, harvest text, or inject synthetic gestures. It serves solely as an anchor to host the floating UI above the status bar.

---

## 7. KNOWN LIMITATIONS & ARCHITECTURAL BOUNDARIES

1. **Bounded Critical Event Buffer Limits:**
   - Offline zero-subscriber queue: Max 16 events. If >16 critical alerts arrive while overlay is detached, oldest are dropped.
   - In-flight buffer capacity: Max 32 events. If consumer is blocked and >32 critical alerts arrive, oldest are dropped via `DROP_OLDEST`.
2. **Feature Engines Not Yet Implemented:**
   - In strict compliance with the roadmap, the following background producer engines are scaffolded in the event model but have not yet been implemented:
     - Notification Engine (`NotificationListenerService`) -> Milestone R6.
     - Media Engine (`MediaSessionManager`).
     - Telephony Engine (`TelecomManager` / `PhoneStateListener`).
     - Battery & Charging Engine (`BroadcastReceiver`).
     - Live Activities & Timer Engine.
     - Recording Indicator Engine (`AudioRecordingCallback` / privacy indicator hooks).
3. **Settings Persistence:**
   - Custom calibration offsets and pulse toggles currently reside in in-memory state repository (`IslandStateRepository`). They reset to defaults upon process recreation. (Targeted for resolution in Milestone R5).

---

## 8. EXACT CLI COMMANDS FOR TESTING & DEPLOYMENT

### Build & Unit Testing
```powershell
# Run the complete 22-test automated suite cleanly
.\gradlew.bat testDebugUnitTest --rerun-tasks

# Assemble debug APK
.\gradlew.bat assembleDebug

# Combined test and build
.\gradlew.bat testDebugUnitTest assembleDebug
```

### Physical Target Installation & Service Management
```powershell
# Install debug APK to connected device
adb -s <DEVICE_SERIAL> install -r app\build\outputs\apk\debug\app-debug.apk

# Launch MainActivity
adb -s <DEVICE_SERIAL> shell am start -n com.nothing.island.ai/.MainActivity

# Check currently enabled accessibility services
adb -s <DEVICE_SERIAL> shell settings get secure enabled_accessibility_services

# Enable Nothing Island Accessibility Service
adb -s <DEVICE_SERIAL> shell settings put secure enabled_accessibility_services "com.nothing.island.ai/.IslandAccessibilityService:tr.com.eywin.dynamic.island.notch/.service.OverlayAccessibilityService:com.dynamicisland.notchscreenview/.service.MyAccesibilityService"
adb -s <DEVICE_SERIAL> shell settings put secure accessibility_enabled 1

# Verify service connection and overlay mount in logcat
adb -s <DEVICE_SERIAL> logcat -d -s IslandAccessibilityService IslandOverlayController IslandStateRepository IslandTouch
```

### Physical Touch Simulation (Nothing Phone 4a Coordinates)
```powershell
# Tap front camera punch-hole cutout (toggles expand/collapse)
adb -s <DEVICE_SERIAL> shell input tap 612 81

# Tap left capsule lobe
adb -s <DEVICE_SERIAL> shell input tap 450 81

# Tap right capsule lobe
adb -s <DEVICE_SERIAL> shell input tap 770 81

# Tap expanded card to collapse
adb -s <DEVICE_SERIAL> shell input tap 612 300

# Tap outside capsule (verifies pass-through)
adb -s <DEVICE_SERIAL> shell input tap 200 81
```

---

## 9. RECOMMENDED NEXT MILESTONE: R5 PERSISTENT SETTINGS

### Milestone Scope
**Milestone R5: Persistent Settings & Calibration Storage**

### Key Objectives
1. **Jetpack DataStore Integration:**
   - Add `androidx.datastore:datastore-preferences` dependency.
   - Replace volatile in-memory calibration values with persistent storage.
2. **Persistent Properties:**
   - Horizontal offset (`xOffsetPx`, default `0`).
   - Vertical top margin (`topMarginPx`, default `27px` / 9dp).
   - Compact pill width/height (`124dp × 36dp`).
   - Expanded card width/height (`350dp × 210dp`).
   - Breathing accent pulse enabled/disabled state.
   - Accessibility overlay preference vs fallback application overlay.
3. **Repository Bridge:**
   - Bind `DataStore` preferences flow into `IslandStateRepository` so preferences are automatically restored on cold boot and overlay service attachment.
4. **Settings UI Connection:**
   - Connect sliders and toggles in `SettingsScreen.kt` to update `DataStore` reactively.

---

## 10. GIT & WORKSPACE STATUS

- **Workspace Path:** `C:\Users\User\.gemini\antigravity\scratch\nothing-island`
- **Git Status:** Git repository is **not initialized** in this scratch workspace. All files, reports, and build artifacts are intact on local disk.
- **Security Check:** Zero passwords, API keys, private tokens, or hardware-specific private identifiers are included in code or documentation.
- **Build Status:** **VERIFIED (Gradle build clean, 22/22 unit tests passing).**

---
*End of Handover Document. Ready for transfer to the next agent.*
