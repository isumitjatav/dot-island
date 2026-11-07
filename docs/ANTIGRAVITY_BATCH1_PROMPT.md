# ANTIGRAVITY TASK PROMPT: SPRINT 2 (BATCH 1) PLATFORM & INTEGRATION
## Nothing Island — AOSP Hardware Receivers, Gesture Touch Engine & Overlay UI Pacing

You are the **Platform, System Architecture & Verification Engineer** for **Nothing Island** on Android 17 / API 37 (`com.nothing.island.ai`) targeted at Nothing Phone (4a) (`Frogger` / `A069`).

Your partner agent (**OmniRoute**) generates the pure DataStore keys, preference flows, event models, settings UI, and data unit tests.

---

### MISSION OBJECTIVE
Build the Android 17 platform bindings, broadcast receivers, hardware callbacks, gesture touch mechanics, and momentary pill rendering for the assigned **Batch 1 (Sprint 2)** requirements:

- **`REQ-36` & `REQ-37`**: Ringer mode state receiver & momentary pill presentation (Silent slashed bell in crimson red vs. Sound On white bell).
- **`REQ-60`**: Flashlight / Torch hardware callback (`CameraManager.TorchCallback`) & momentary alert pill (amber torch glyph + "Torch On/Off").
- **`REQ-61`**: Wi-Fi network transition callback (`ConnectivityManager.NetworkCallback`) & capsule (Wi-Fi glyph + SSID or "Disconnected" with Android 17 location guardrails).
- **`REQ-62`**: DND / Bedtime interruption filter receiver & pill (crescent moon glyph + "DND On/Off").
- **`REQ-64`**: Low battery warning pills at 20% (amber) and 10% (crimson red) thresholds with auto-dismiss timeout.
- **`REQ-75`**: Screen Unlock / User Present feedback capsule (`Intent.ACTION_USER_PRESENT` keyguard dismissal, strictly zero biometric telemetry).
- **`REQ-20`**: Long-press volume drag gesture on active media island (500ms hold -> horizontal drag adjusts `AudioManager.STREAM_MUSIC` volume).
- **`REQ-67` – `REQ-71` Touch Engine Execution**: Connect gesture triggers (single tap, 500ms long press, swipe left, swipe right, swipe down) to their configured `GestureAction` handlers (Torch toggle, Camera launch, Notifications Hub, Expand/Collapse, etc.).

---

### REPOSITORY ARCHITECTURE & FILE LOCATIONS

1. **System Events Broadcast Receiver**:
   - Path: `app/src/main/java/com/nothing/island/ai/receiver/SystemEventsReceiver.kt`
   - Actions to handle:
     - `AudioManager.RINGER_MODE_CHANGED_ACTION` -> emits `RingerModeChanged` (`REQ-36`, `REQ-37`)
     - `NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED` -> emits `DndStateChanged` (`REQ-62`)
     - `Intent.ACTION_USER_PRESENT` -> emits `DeviceUnlocked` (`REQ-75`)
     - `Intent.ACTION_BATTERY_CHANGED` / `ACTION_BATTERY_LOW` -> emits `BatteryThresholdWarning` (`REQ-64`)

2. **Hardware Callbacks (Torch & Network)**:
   - Path: `app/src/main/java/com/nothing/island/ai/hardware/TorchCallbackHandler.kt`
     - Uses `CameraManager.registerTorchCallback()` on the main camera ID (`"0"`).
     - Emits `TorchStateChanged(isEnabled)` on state changes.
   - Path: `app/src/main/java/com/nothing/island/ai/hardware/NetworkCallbackHandler.kt`
     - Uses `ConnectivityManager.registerNetworkCallback()` with `NetworkRequest.Builder().addTransportType(TRANSPORT_WIFI)`.
     - Resolves SSID via `WifiInfo` only when `ACCESS_FINE_LOCATION` is granted, falling back to `"Wi-Fi Connected"` cleanly.

3. **Momentary Pill UI Composables**:
   - Path: `app/src/main/java/com/nothing/island/ai/ui/components/SystemAlertPill.kt`
   - Design:
     - Nothing OS dot-matrix aesthetic (Ndot font, monochrome capsule with Nothing Red accent `0xFFD71920L` or amber `0xFFFFC107L`).
     - Compact entry/exit spring physics (stiffness: `Spring.StiffnessMediumLow`, damping: `Spring.DampingRatioLowBouncy`).
     - Auto-dismiss timers (2500ms for ringer/torch/DND, 3000ms for Wi-Fi, 4000ms for low battery).

4. **Gesture Touch Engine & Volume Drag**:
   - Path: `app/src/main/java/com/nothing/island/ai/gestures/IslandGestureDetector.kt`
   - Touch mechanics:
     - Disambiguate Single Tap, 500ms Long Press, and Directional Swipes (Left, Right, Down) without stealing WindowManager touches from underlying apps when collapsed.
     - `REQ-20` Media Volume Drag: detect horizontal drag after long-press activation on media island; calculate delta and call `AudioManager.setStreamVolume()`.

5. **Engine Integration & Event Arbitration**:
   - Path: `app/src/main/java/com/nothing/island/ai/engine/IslandEngine.kt`
   - Ingest events from `SystemEventsReceiver`, `TorchCallbackHandler`, and `NetworkCallbackHandler`.
   - Maintain event priority arbitration (System momentary pills preempt idle and media, but yield to incoming calls or critical emergency alerts).

6. **Unit & Integration Tests**:
   - Path: `app/src/test/java/com/nothing/island/ai/receiver/SystemEventsReceiverUnitTest.kt`
   - Path: `app/src/test/java/com/nothing/island/ai/hardware/HardwareCallbacksUnitTest.kt`
   - Path: `app/src/test/java/com/nothing/island/ai/gestures/IslandGestureDetectorUnitTest.kt`

---

### STRICT CONSTRAINTS & PLATFORM RULES
1. **Public Android 17 Guardrails**:
   - No private/hidden APIs.
   - Zero biometric telemetry: `ACTION_USER_PRESENT` provides keyguard-unlock feedback only and must never attempt to query biometric sensor IDs.
2. **Display Pacing & Jank Budget**:
   - Compose animations must sustain <5% jank and P99 frame delivery <16ms.
   - Total RSS memory footprint of the overlay service must remain <85MB.
3. **Single Overlay Window Owner**:
   - Preserve WindowManager overlay window invariant (single overlay view root, `TYPE_APPLICATION_OVERLAY` 2038).

---

### VERIFICATION GATE
1. Run all unit tests locally:
   ```powershell
   .\gradlew testDebugUnitTest
   ```
2. Build debug APK:
   ```powershell
   .\gradlew assembleDebug
   ```
3. Deploy & physical device validation on Nothing Phone (4a):
   ```powershell
   adb install -r app\build\outputs\apk\debug\app-debug.apk
   ```
