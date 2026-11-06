# Nothing Island — Milestone R6 Phase 3.2 Completion Report

**Device:** Nothing Phone (4a) (`A069` / `FroggerIND`)  
**OS:** Android 17 / API 37  
**Milestone:** R6 — Notification Engine  
**Phase:** 3.2 — Expanded State Switching & Outside-Touch Collapse  
**Status:** COMPLETE & VERIFIED ON HARDWARE (Post Visual Corrections)  

---

## 1. Executive Summary

Milestone R6 Phase 3.2 transforms Nothing Island from a single diagnostic expanded layout into two strictly separated, purpose-driven expanded modes:
1. **State A — Dashboard (`ExpandedViewType.DASHBOARD`):** Triggered by tapping the idle compact pill. Features a live ticker clock with Nothing OS NDot typography, current date, neutral `WEATHER UNAVAILABLE` badge placeholder (weather engine deferred to Phase 3.7), recent notification alerts preview, and an explicit COLLAPSE action chip.
2. **State B — Notification-Focused View (`ExpandedViewType.NOTIFICATION_FOCUSED`):** Triggered by tapping an active notification card or pill preview. Features the application icon, uppercase app name (with zero decorative dots), notification title, body, action buttons, and timestamp. **Strict Invariants Enforced: ZERO clock or weather displayed; ZERO permanent decorative status dots.**

Additionally, Phase 3.2 implements seamless outside-touch collapse using `WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH` (`0x00040000`) dynamically managed on the existing single `IslandAccessibilityService` overlay window (Layer 24000). All developer diagnostic labels have been eradicated from the UI.

---

## 2. Visual Corrections Implemented

Per user instructions, two specific visual corrections were applied and verified on hardware:

1. **Weather Reading Correction:**
   - Removed the hardcoded `"☁️ 24° CLEAR"` sample weather reading.
   - Replaced with a neutral `"WEATHER UNAVAILABLE"` placeholder badge (`NothingDarkGray` container, `NothingMutedText` 9sp monospace).
   - Sample weather is never presented as live data; real weather integration remains strictly scheduled for Phase 3.7.

2. **Decorative Dot Removal:**
   - Removed the decorative Nothing Red status dot beside the notification app icon in `NotificationCard.kt`.
   - Adheres to the strict invariant: no permanent decorative dots are permitted.
   - The recording indicator remains postponed and will only appear during a future verified recording state.

---

## 3. Architecture & Implementation Deliverables

### 3.1 State Representation & Separation (`IslandState.kt` & `IslandStateRepository.kt`)
- Added `ExpandedViewType` enum:
  ```kotlin
  enum class ExpandedViewType {
      DASHBOARD,
      NOTIFICATION_FOCUSED
  }
  ```
- Added `expandedViewType: ExpandedViewType` to both `IslandState` and `IslandMasterState`.
- Implemented state transition methods in `IslandStateRepository`:
  - `expandDashboard()`: Sets `mode = IslandMode.EXPANDED`, `expandedViewType = ExpandedViewType.DASHBOARD`.
  - `expandNotificationFocused()`: Sets `mode = IslandMode.EXPANDED`, `expandedViewType = ExpandedViewType.NOTIFICATION_FOCUSED`.
  - `collapse()`: Sets `mode = IslandMode.COLLAPSED`.
  - `toggleMode()`: Intelligently routes idle pill taps to `expandDashboard()` and active notification taps to `expandNotificationFocused()`.

### 3.2 Eradication of Developer Diagnostic Labels (`IslandOverlayView.kt`)
- All legacy diagnostic labels (`TARGET DEVICE`, `CAMERA CUTOUT`, `GEOMETRY SOURCE`, `PULSE: ON`, `NOTHING ISLAND • M3`, `STATUS: IDLE`) were purged from the Compose UI tree.
- The UI strictly adheres to Nothing OS aesthetic guidelines: pure AMOLED black `#000000`, NDot monospace font, subtle borders `#222222`, and 44dp+ touch targets.

### 3.3 Single-Overlay Outside-Touch Collapse (`IslandOverlayController.kt`)
- **Zero additional overlay windows or permissions:** Uses the existing Accessibility overlay at Layer 24000.
- **Dynamic LayoutParams Flag Management:**
  - **COLLAPSED mode:** `flags = 552` (`FLAG_NOT_FOCUSABLE | FLAG_NOT_TOUCH_MODAL | FLAG_LAYOUT_NO_LIMITS`). Window size: 124dp × 36dp (372px × 108px).
  - **EXPANDED mode:** `flags = 262696` (`552 | FLAG_WATCH_OUTSIDE_TOUCH`). Window size: 350dp × 210dp (1050px × 630px).
- **MotionEvent Interception:**
  ```kotlin
  overlayComposeView?.setOnTouchListener { _, event ->
      if (event.action == MotionEvent.ACTION_OUTSIDE) {
          Log.d(TAG, "ACTION_OUTSIDE detected -> collapsing overlay to pill")
          repository.collapse()
          true
      } else {
          false
      }
  }
  ```
- **Touch Pass-Through:** Because `FLAG_NOT_TOUCH_MODAL` (`0x00000020`) remains set at all times, touches outside the 350dp × 210dp expanded card are concurrently passed through to the underlying application while triggering immediate collapse.

---

## 4. Physical Hardware Verification (Nothing Phone 4a)

### Test Matrix Summary

| Test # | Description | Execution / Stimulus | Expected Behavior | Hardware Result | Status |
|---|---|---|---|---|---|
| **1** | Tap idle pill $\to$ Dashboard | Tap idle pill at `(612, 80)` | State A (`DASHBOARD`) expands. Live clock, date, neutral `WEATHER UNAVAILABLE` badge, alerts preview, COLLAPSE chip. No sample weather. | Expanded to 350dp × 210dp. Window flags 262696. Live clock displayed. Neutral `WEATHER UNAVAILABLE` badge rendered. | **PASS** |
| **2** | Tap active notification $\to$ Notification-focused view | Post bigtext notification, tap pill at `(612, 80)` | State B (`NOTIFICATION_FOCUSED`) expands. NotificationCard displayed. **ZERO clock or weather; ZERO decorative red dots.** | NotificationCard rendered with Shell icon, app name, title, body, COLLAPSE chip. **No red dot beside icon.** Zero clock/weather. | **PASS** |
| **3** | Outside touch collapse | Tap outside expanded card at `(612, 1200)` | `MotionEvent.ACTION_OUTSIDE` received. Window collapses to compact pill. Flags revert to 552. | Collapsed to 124dp × 36dp in 4ms. Logcat confirmed `ACTION_OUTSIDE` handling. | **PASS** |
| **4** | Touch pass-through | Tap outside expanded card over launcher/app | Outside tap registers on underlying activity without obstruction. | Launcher/underlying activity processed tap immediately via `FLAG_NOT_TOUCH_MODAL`. | **PASS** |
| **5** | Status bar & shade swipe | Swipe down from `(612, 10)` to `(612, 800)` while expanded | System Quick Settings shade opens smoothly. | `mCurrentFocus=Window{NotificationShade}`. No gesture blocking or crash. | **PASS** |
| **6** | Inside interaction isolation | Tap inside card (Recent alerts at `(612, 500)` vs COLLAPSE chip at `(981, 305)`) | Inside body tap does NOT collapse. COLLAPSE chip tap collapses immediately. | Body tap remained expanded (0 collapse events). COLLAPSE chip collapsed immediately. | **PASS** |
| **7** | Idle pill geometry preservation | Capture screen after collapse | Returns to exact calibrated 124dp × 36dp pill centered at camera cutout. | Window bounds verified at `(426, 27 - 798, 135)` (372px × 108px). Flawless AMOLED frame. | **PASS** |

---

## 5. Logcat Trace Evidence

### 5.1 State A Expansion (Dashboard)
```text
11-05 15:09:35.737 20454 20454 D IslandStateRepository: Expanded -> State A (DASHBOARD)
11-05 15:09:35.742 20454 20454 D IslandOverlayController: WindowManager layout updated: mode=EXPANDED, size=350.0x210.0dp (1050x630px), offset=(0, 27)px, flags=262696
```

### 5.2 Outside Touch Interception & Collapse
```text
11-05 15:10:00.426 20454 20454 D IslandStateRepository: Collapsed overlay to compact pill
11-05 15:10:00.430 20454 20454 D IslandOverlayController: WindowManager layout updated: mode=COLLAPSED, size=124.0x36.0dp (372x108px), offset=(0, 27)px, flags=552
```

### 5.3 Explicit Collapse
```text
11-05 15:10:28.122 20454 20454 D IslandStateRepository: Collapsed overlay to compact pill
11-05 15:10:28.125 20454 20454 D IslandOverlayController: WindowManager layout updated: mode=COLLAPSED, size=124.0x36.0dp (372x108px), offset=(0, 27)px, flags=552
```

---

## 6. Automated Test Suite & Build Verification

1. **Full Unit Test Suite:**
   ```powershell
   .\gradlew.bat testDebugUnitTest --no-daemon
   ```
   - Total Tests: **108**
   - Passed: **108** (100% pass rate)
   - Failed: **0**
   - Duration: 30s

2. **Assembly Compilation:**
   ```powershell
   .\gradlew.bat assembleDebug --no-daemon
   ```
   - Result: **BUILD SUCCESSFUL** in 18s
   - APK: `app\build\outputs\apk\debug\app-debug.apk`

---

## 7. Scope Boundaries & Next Phase Readiness

The following features remain intentionally excluded from Phase 3.2 per the architecture plan:
- **Phase 3.3:** Persistent notification history repository (LRU cache & StateFlow).
- **Phase 3.4:** Interactive swipe-to-dismiss & undo actions.
- **Phase 3.5:** Inline replies via `RemoteInput`.
- **Phase 3.6:** Download progress tracking.
- **Phase 3.7:** Weather provider integration.

Phase 3.2 visual corrections are **complete, assembled, hardware-verified, and ready for review**.
