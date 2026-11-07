# OMNIROUTE TASK PROMPT: SPRINT 2 (BATCH 1) FOUNDATIONS
## Nothing Island — Core Domain Models, DataStore Persistence & Gesture Settings

You are the **Autonomous Domain & Persistence Engineer** in a dual-loop development team for **Nothing Island**, a high-performance Jetpack Compose status-overlay for Nothing Phone (4a) running Android 17 / API 37 (`com.nothing.island.ai`).

Your partner agent (**Antigravity**) handles Android OS system services, WindowManager touch pass-through, hardware receivers, and physical-device verification.

---

### MISSION OBJECTIVE
Implement the core domain models, DataStore preference persistence, sealed engine events, settings UI composables, and exhaustive unit test suites for the assigned **Batch 1 (Sprint 2)** requirements:

- **`REQ-26`**: DND auto-expand suppression override toggle (`dndAutoExpandOverride: Boolean`, default: `false`).
- **`REQ-28`**: Hide sensitive notification content on keyguard toggle (`hideSensitiveOnKeyguard: Boolean`, default: `true`).
- **`REQ-48`**: Small notch disappear mode selector (`NotchDisappearMode` enum: `AUTO`, `NEVER`, `ON_READ`, default: `AUTO`).
- **`REQ-51`**: Expand silent channel notifications toggle (`expandSilentNotifications: Boolean`, default: `false`).
- **`REQ-67`**: Single tap custom action assignment (`singleTapAction: GestureAction`, default: `EXPAND_COLLAPSE`).
- **`REQ-68`**: Long-press (500ms) gesture action assignment (`longPressAction: GestureAction`, default: `NOTIFICATIONS_HUB`).
- **`REQ-69`**: Swipe left custom action assignment when no notification is active (`swipeLeftAction: GestureAction`, default: `NONE`).
- **`REQ-70`**: Swipe right custom action assignment when no notification is active (`swipeRightAction: GestureAction`, default: `NONE`).
- **`REQ-71`**: Swipe down custom action assignment (`swipeDownAction: GestureAction`, default: `NONE`).
- **Domain Event Foundations**: Sealed event classes in `IslandEvent.kt` for momentary alert pills (`RingerModeChanged`, `TorchStateChanged`, `WifiStateChanged`, `DndStateChanged`, `BatteryThresholdWarning`).

---

### REPOSITORY ARCHITECTURE & FILE LOCATIONS

1. **New Preferences Model**:
   - Path: `app/src/main/java/com/nothing/island/ai/gestures/GesturePreferences.kt`
   - Contents:
     - `enum class GestureAction { NONE, EXPAND_COLLAPSE, NOTIFICATIONS_HUB, TORCH, CAMERA, SETTINGS, SCREENSHOT, CLEAR_ALL }`
     - `enum class NotchDisappearMode { AUTO, NEVER, ON_READ }`
     - `enum class RingerState { SILENT, VIBRATE, NORMAL }`
     - `data class GesturePreferences(...)` with all assigned defaults.

2. **DataStore Repository Integration**:
   - Path: `app/src/main/java/com/nothing/island/ai/data/IslandPreferencesRepository.kt`
   - Tasks:
     - Define `Preferences.Key` constants:
       - `DND_AUTO_EXPAND_OVERRIDE = booleanPreferencesKey("dnd_auto_expand_override")`
       - `HIDE_SENSITIVE_ON_KEYGUARD = booleanPreferencesKey("hide_sensitive_on_keyguard")`
       - `NOTCH_DISAPPEAR_MODE = stringPreferencesKey("notch_disappear_mode")`
       - `EXPAND_SILENT_NOTIFICATIONS = booleanPreferencesKey("expand_silent_notifications")`
       - `SINGLE_TAP_ACTION = stringPreferencesKey("gesture_single_tap_action")`
       - `LONG_PRESS_ACTION = stringPreferencesKey("gesture_long_press_action")`
       - `SWIPE_LEFT_ACTION = stringPreferencesKey("gesture_swipe_left_action")`
       - `SWIPE_RIGHT_ACTION = stringPreferencesKey("gesture_swipe_right_action")`
       - `SWIPE_DOWN_ACTION = stringPreferencesKey("gesture_swipe_down_action")`
     - Expose `val gesturePreferencesFlow: Flow<GesturePreferences>`
     - Implement update methods: `updateGesturePreferences()`, `setGestureAction()`, `setNotchDisappearMode()`, `setDndAutoExpandOverride()`, `setHideSensitiveOnKeyguard()`, `setExpandSilentNotifications()`.

3. **Sealed IslandEvent Extensions**:
   - Path: `app/src/main/java/com/nothing/island/ai/engine/IslandEvent.kt`
   - Tasks:
     - Add data classes under `// 8. SYSTEM HARDWARE & MOMENTARY ALERTS`:
       - `data class RingerModeChanged(val ringerMode: RingerState, ...): IslandEvent` (priority = `IslandPriority.SYSTEM_HARDWARE`, timeoutMs = 2500L)
       - `data class TorchStateChanged(val isEnabled: Boolean, ...): IslandEvent` (priority = `IslandPriority.SYSTEM_HARDWARE`, timeoutMs = 2500L)
       - `data class WifiStateChanged(val isConnected: Boolean, val ssid: String? = null, ...): IslandEvent` (priority = `IslandPriority.SYSTEM_HARDWARE`, timeoutMs = 3000L)
       - `data class DndStateChanged(val isEnabled: Boolean, ...): IslandEvent` (priority = `IslandPriority.SYSTEM_HARDWARE`, timeoutMs = 2500L)
       - `data class BatteryThresholdWarning(val batteryLevel: Int, val isCritical: Boolean, ...): IslandEvent` (priority = `IslandPriority.SAFETY_ALERT`, timeoutMs = 4000L)
     - **CRITICAL**: Update `hasIdenticalBusinessPayload(other: IslandEvent)` in `IslandEvent` to handle all new event types without compilation errors.

4. **Settings UI Studio Composable**:
   - Path: `app/src/main/java/com/nothing/island/ai/ui/components/GestureSettingsStudio.kt`
   - Tasks:
     - Build a Jetpack Compose settings section adhering to the **Nothing OS Design System** (monochrome palette, Ndot red accent `0xFFD71920L`, clean cards).
     - Include pickers/dropdowns for all 5 gesture action targets (`singleTapAction`, `longPressAction`, `swipeLeftAction`, `swipeRightAction`, `swipeDownAction`).
     - Include `NotchDisappearMode` segmented selector (`Auto`, `Never`, `On Read`).
     - Include switch toggles for `dndAutoExpandOverride`, `hideSensitiveOnKeyguard`, and `expandSilentNotifications`.

5. **Exhaustive Unit Tests**:
   - Path: `app/src/test/java/com/nothing/island/ai/gestures/GesturePreferencesUnitTest.kt`
   - Tasks:
     - Verify default preference values on fresh DataStore instance.
     - Verify mutation persistence and Flow updates for every enum and boolean.
     - Verify fallback handling on unknown/corrupted string preference values.
     - Verify `IslandEvent.hasIdenticalBusinessPayload()` for all new event types.

6. **String Resources**:
   - Path: `app/src/main/res/values/strings.xml`
   - Tasks: Add descriptive titles and summaries for all new gesture actions and behavior switches.

---

### STRICT CONSTRAINTS & PLATFORM RULES
1. **Preserve Existing Architecture**: Do NOT modify or break existing tests in `app/src/test/`. All 253 existing tests must continue to pass.
2. **Deterministic Test Execution**: Ensure all tests run with `runTest` and `StandardTestDispatcher` using in-memory `TemporaryFolder` for DataStore PB files.
3. **No Android UI dependencies in test classes**: Ensure unit tests run under standard local JUnit 4 (`gradlew testDebugUnitTest`) without needing a physical device or emulator.
4. **Nothing OS Aesthetics**: Ensure any UI components use `@Composable` functions utilizing Nothing theme tokens (`IslandTheme`, `NothingRed`).

---

### VERIFICATION GATE
Your code must pass local verification cleanly:
```powershell
.\gradlew testDebugUnitTest
```
Ensure zero compilation warnings, zero broken imports, and all new unit test assertions report GREEN.
