# Complete Feature Parity Implementation Roadmap (Final Reconciled Validation)
**Target Device:** Nothing Phone (4a) (`Frogger` / `A069`), Android 17 / API 37  
**Package:** `com.nothing.island.ai`  
**Git Baseline Commit:** `73db7aa` (`master`, working tree clean)  
**Operational Model:** 100% Free & Ad-Free (89 Offline-Capable, 5 Network-Dependent, 1 Optional Cloud AI)  
**Scope:** Reconciled Chronological Implementation & Testing Plan for 100% Observable Parity  
**Reference Applications:** Dynamic Notch (`com.dynamicisland.notchscreenview`, v6.60) & Dynamic Island Pro (`tr.com.eywin.dynamic.island.notch`, v1.9.2)

---

## 1. Executive Strategy & Guiding Principles

This roadmap organizes all **75 unfinished requirements** (16 Partially Implemented, 15 UI-Only, 44 Missing) across the 95 total master requirements (20 Baseline Verified + 75 Unfinished = 95 Total) into 7 self-contained implementation batches across Sprints 2 through 8.

### Core Architectural Mandates:
1. **Reference Parity First:** No personal or unrelated innovations are permitted prior to achieving 100% observable parity with both reference applications.
2. **Dual Visual Tier Architecture:**
   - **Tier 1 — Reference-Parity Baseline (Default):** Complete visual, motion, color, and timing parity with the reference applications using independently authored, non-infringing assets and procedural shaders.
   - **Tier 2 — Nothing OS Native Tier (Optional Extras):** Dot-Matrix, monochrome, and Nothing Red styling provided as dedicated selectable options.
3. **100% Free & Ad-Free:** Every commercial reference feature—including all 34 paywalled, VIP, and coin-metered capabilities—is engineered as an open, 100% free, and completely ad-free capability in Nothing Island (zero paywalls, zero subscriptions, zero ads, zero coin meters). Offline-capable features (89 requirements) operate fully locally on-device; network-dependent live services (5 requirements: REQ-58, 61, 83, 84, 95) operate when network connectivity is available; and AI capabilities (1 requirement: REQ-94) provide an offline on-device local model baseline with optional cloud AI endpoints.
4. **Single Overlay Invariant:** Retain `IslandAccessibilityService` at `TYPE_ACCESSIBILITY_OVERLAY` (layer 24000) as the single window owner. All subsystems stream events into `IslandStateManager`.
5. **Separation of Engineering & Verification:** Implementation effort is strictly separated from asset motion design and physical hardware verification cycles.

```mermaid
flowchart TD
    subgraph Phase A: Interactions & Foundations
        B1[Batch 1: Core System Events, Audio & Gesture Foundations\n17 Requirements: REQ-20, 26, 28, 36, 37, 48, 51, 60, 61, 62, 64, 67, 68, 69, 70, 71, 75]
    end

    subgraph Phase B: Visual Aesthetics & Themes
        B2[Batch 2: Exhaustive Theme & Animation Parity\n8 Requirements: REQ-05, 08, 23, 57, 59, 65, 66, 76]
    end

    subgraph Phase C: In-Island Productivity
        B3[Batch 3: Quick Settings & App Shortcuts Dock\n4 Requirements: REQ-72, 73, 74, 77]
    end

    subgraph Phase D: Real-Time Subsystems
        B4[Batch 4: Telecom & Call Intelligence Subsystem\n12 Requirements: REQ-40-47, 78-81]
        B5[Batch 5: Peripheral Intelligence & Bluetooth Management\n5 Requirements: REQ-01, 02, 03, 54, 55]
    end

    subgraph Phase E: Advanced Customization, Ambient & AI
        B6[Batch 6: Advanced Customization, StandBy Clocks & Weather\n23 Requirements: REQ-11-14, 17, 30-33, 38, 50, 52, 58, 82, 85-92, 95]
        B7[Batch 7: Localization, Live Tracking & Free AI Companion\n6 Requirements: REQ-10, 39, 83, 84, 93, 94]
    end

    B1 --> B2
    B2 --> B3
    B3 --> B4
    B4 --> B5
    B5 --> B6
    B6 --> B7
```

---

## 2. Chronological Implementation Batches (Sprints 2 to 8)

### Batch 1: Core System Events, Audio & Gesture Foundations (Sprint 2)
* **Strategic Objective:** Deliver the highest-visibility day-to-day momentary hardware alert pills, audio gestures, and interactive gesture bindings, closing the interactive feedback gap.
* **Requirements Addressed (17 Requirements):**
  - `REQ-20`: Long-press volume drag gesture on media island.
  - `REQ-26`: DND auto-expand suppression override toggle.
  - `REQ-28`: Hide sensitive content on keyguard toggle.
  - `REQ-36`: Silent mode momentary feedback pill (slashed bell + "Silent", crimson red).
  - `REQ-37`: Sound On / Normal ringer mode pill (white bell + "Sound On").
  - `REQ-48`: Small notch disappear mode selector (Auto / Never / On Read).
  - `REQ-51`: Expand silent channel notifications toggle.
  - `REQ-60`: Flashlight / Torch toggle feedback pill (amber torch glyph + "Torch On/Off").
  - `REQ-61`: Wi-Fi state transition feedback capsule (Wi-Fi glyph + SSID name / Disconnected; network-dependent telemetry).
  - `REQ-62`: DND / Bedtime crescent moon feedback pill ("DND On/Off").
  - `REQ-64`: Low battery warning pill at 20% and 10% thresholds.
  - `REQ-67`: Single tap customizable action assignment (Torch, Camera, Expand, etc.).
  - `REQ-68`: Long-press gesture detector (500ms) with customizable action binding.
  - `REQ-69`: Swipe left custom action assignment when no notification is active.
  - `REQ-70`: Swipe right custom action assignment when no notification is active.
  - `REQ-71`: Swipe down gesture action assignment (Custom configurable action on downward swipe).
  - `REQ-75`: Screen Unlock / User Present feedback capsule (padlock glyph + "Unlocked"; triggers on keyguard dismissal via `ACTION_USER_PRESENT`, without identifying or inferring the authentication method).
* **Implementation Scope (5 Days):**
  1. Create `SystemEventsReceiver` in `com.nothing.island.ai.receiver` handling `RINGER_MODE_CHANGED_ACTION`, `ACTION_USER_PRESENT`, and `ACTION_INTERRUPTION_FILTER_CHANGED`.
  2. Implement `TorchCallbackHandler` via `CameraManager.registerTorchCallback()`.
  3. Implement `NetworkStateCallback` via `ConnectivityManager.registerNetworkCallback()`.
  4. Extend `IslandEvent` sealed interface with `RingerChanged`, `TorchToggled`, `UnlockEvent`, `NetworkChanged`, `DndChanged`, and `LowBattery`.
  5. Build `SystemEventCompactView.kt` in Compose overlay.
  6. Enhance `IslandTouchHandler` with `combinedClickable`, vertical drag for volume, and directional swipes.
* **Verification & Acceptance Scope (2 Days):**
  - Physical ringer switch, quick tile torch toggle, Wi-Fi toggle, and lock/unlock cycle verified on Nothing Phone (4a).
  - 15-second screen recordings of each momentary capsule.

---

### Batch 2: Exhaustive Theme & Animation Parity (Sprint 3)
* **Strategic Objective:** Replicate the complete visual catalog across both reference apps (52 global themes, 24 wave themes, 39 charging animations) with reference-accurate visual parity.
* **Requirements Addressed (8 Requirements):**
  - `REQ-05`: 2-column visual grid of charging themes.
  - `REQ-08`: Complete 52 Global Notch Colorway Themes (`q04.java` / `p04.java` port: 10 VIP + 42 Free).
  - `REQ-23`: Dual popup animation physics curve selector (`Liquid`, `Bouncy`, `Droplet`).
  - `REQ-57`: Theme confirmation bottom sheet modal dialog.
  - `REQ-59`: 6-metric hardware charging telemetry grid (Voltage, Temp, Current $\mu A$, Wattage, Health, Time-to-full).
  - `REQ-65`: Complete 39 Kinetic Notch Charging Animations (`lb/d1.java` port: 22 kinetic characters, 10 energy effects, 5 silhouettes, 2 event specials).
  - `REQ-66`: Complete 24 Wave Charging Themes (`DynamicWaveView.java` procedural liquid sine wave).
  - `REQ-76`: Pulse / Heartbeat bounce animation (`animateScaleUpAndDown`).
* **Implementation Scope (8 Days) + Motion Design (6 Days):**
  1. Port all 52 theme specifications into `ThemeCatalog.kt` using Compose `Brush.linearGradient` and `SolidColor`.
  2. Independently recreate vector motion assets for the 22 kinetic mascot characters (Ghost, AmongUs-style Astronaut, Crab, Car, Bee, Heart, Emoji, Snail, Hand, Watermelon, Shark, Flash, Beetle, Pumpkin, Fruit, Magic Wand, Robot, Shoes, Snowman, Ninja, Ball, Juice).
  3. Independently author the 10 neon energy effect shaders and animations (Charge Bolt 1 & 2, Crystal Shower, Potion, Bio Leaf, Morning Star, Flaming Spear, Magic Elixir, Freeze Bolt, Cherry Petals).
  4. Create `WaveChargingVisualizer.kt` rendering dual-harmonic sine waves onto Compose `Canvas` using path clipping and customizable fluid palettes.
  5. Implement `PulseAnimationSpec` applying infinite repeating scale bounce ($1.0 \to 1.06 \to 1.0$) during idle active states.
  6. Wire `BatteryManager` hardware metrics ($\mu A$, W, time-to-full) into expanded charging card.
* **Verification & Acceptance Scope (3 Days):**
  - Side-by-side visual comparison against reference screen recordings.
  - Display pacing verification targeting 60Hz/120Hz display refresh rates on Nothing Phone (4a) OLED display, maintaining <5% janky frames during continuous animations as measured via Android Studio Profiler and Perfetto.

---

### Batch 3: Quick Settings & App Shortcuts Dock (Sprint 4)
* **Strategic Objective:** Transform Nothing Island into an assistive productivity hub matching Dynamic Island Pro's 4-slot dock and Dynamic Notch's 17-toggle quick panel.
* **Requirements Addressed (4 Requirements):**
  - `REQ-72`: In-Island Expanded 4-Slot Quick Actions Bar (`quick_action_0..3`).
  - `REQ-73`: In-Island Expanded 4-Slot App Drawer Shortcuts (`app_shortcut_0..3`).
  - `REQ-74`: In-Island Fast QR / Barcode Scanner Module (CameraX + MLKit).
  - `REQ-77`: 17 Quick Settings Toggles in Expanded Island (`QuickSettingType.java`: Dark Mode, Wi-Fi, Bluetooth, Mobile Data, Mute, Torch, Airplane, DND, Rotation, Hotspot, Location, Focus, Vibrate, Nearby, Screen Share, Reading, Battery Saver).
* **Implementation Scope (6 Days):**
  1. Build `QuickSettingsGrid.kt` rendering responsive 4-column quick toggles with active hardware state synchronization.
  2. Implement `QuickActionDispatcher` managing system toggles (Torch via CameraManager, Mute via AudioManager, Screen Rotation via Settings.System, etc.).
  3. Build `AppDrawerOverlayManager` storing 4 user-selected package names in DataStore and launching via `getLaunchIntentForPackage`.
  4. Create `QRCodeScannerActivity` with CameraX analysis use case and MLKit Barcode Scanning.
* **Verification & Acceptance Scope (2 Days):**
  - Tap Torch tile inside expanded island; camera LED immediately illuminates.
  - Launch selected app shortcut directly from island; target app foregrounds cleanly.
  - Scan QR code; result dialog displays instant decoded payload.

---

### Batch 4: Telecom & Call Intelligence Subsystem (Sprint 5)
* **Strategic Objective:** Implement carrier-grade call screening, incoming call presentation HUDs, and telecom logging while respecting Android 17 telephony boundaries.
* **Requirements Addressed (12 Requirements):**
  - `REQ-40`: Master System Call Switch.
  - `REQ-41`: Two-Tier Call Presentation (Compact Timer vs Expanded Card).
  - `REQ-42`: Native OEM Dialer Collision Suppression.
  - `REQ-43`: Third-Party VoIP Call Integration (WhatsApp, Telegram).
  - `REQ-44`: Post-Call Summary Theme Selector.
  - `REQ-45`: Lockscreen Incoming Call Display Toggle.
  - `REQ-46`: Game Mode Call Presentation Mode.
  - `REQ-47`: Blocked Contacts Blacklist Picker.
  - `REQ-78`: Native Android CallScreeningService Integration.
  - `REQ-79`: Multilingual Call & Telecom Keyword Matcher.
  - `REQ-80`: Post-Call Summary HUD Card.
  - `REQ-81`: Call History Viewer & Activity Logger.
* **Telephony & VoIP Control Architectural Separation:**
  1. *Caller Identification:* Cellular incoming numbers and contact names extracted via `CallScreeningService` (`onScreenCall`) without default dialer status. VoIP caller details parsed from `Notification.CallStyle` extras (`EXTRA_CALL_PERSON`).
  2. *Call Presentation:* Two-tier overlay HUD rendered via `TYPE_ACCESSIBILITY_OVERLAY` or `TYPE_APPLICATION_OVERLAY`.
  3. *Call Answering:* Cellular answering via `TelecomManager.acceptRingingCall()` (`ANSWER_PHONE_CALLS` permission). VoIP answering dispatched via third-party notification Answer `PendingIntent` (subject to Android 14–17 Background Activity Launch / BAL restrictions).
  4. *Call Ending / Rejection:* Cellular rejection via `CallScreeningService.respondToCall()` prior to ringing, or via `InCallService`. Third-party VoIP call ending via notification Decline `PendingIntent`.
  5. *In-Call Audio Controls (Mute & Speakerphone):* Cellular mute/speakerphone requires active `InCallService` or audio focus communication device routing (`setCommunicationDevice()`). Third-party VoIP apps (WhatsApp, Telegram) do NOT expose in-call mute or speaker toggles via notifications or public APIs; universal VoIP call control is technically impossible and is explicitly not promised.
* **Implementation Scope (10 Days):**
  1. Declare `IslandCallScreeningService` in manifest extending `android.telecom.CallScreeningService`.
  2. Implement `PhoneCallStateReceiver` listening to `TelephonyManager.ACTION_PHONE_STATE_CHANGED`.
  3. Build `CallCompactView.kt` (compact active timer) and `CallExpandedView.kt` (caller avatar, contact name, in-call actions).
  4. Port regex pattern tables for 30+ languages into `TelecomKeywordMatcher.kt`.
  5. Create Room database `CallLogDatabase` storing call history.
  6. Implement `CallSummaryHUD.kt` with working `Intent.ACTION_DIAL` callback button and theme customization.
* **Verification & Acceptance Scope (4 Days):**
  - Place cellular voice call; verify instant island card before dialer loads.
  - Answer call; verify compact pill displays ticking call timer (`00:01`, `00:02`).
  - Terminate call; verify post-call summary card displays accurate elapsed time.
  - Test WhatsApp audio call; verify incoming card displays WhatsApp badge and dispatches Answer/Decline intents where exposed.
---

### Batch 5: Peripheral Intelligence & Bluetooth Management (Sprint 6)
* **Strategic Objective:** Implement comprehensive Bluetooth peripheral tracking matching commercial references, complete with category silhouettes and battery rings.
* **Requirements Addressed (5 Requirements):**
  - `REQ-01`: Bluetooth connection master switch.
  - `REQ-02`: Bluetooth compact capsule with circular battery gauge ring.
  - `REQ-03`: Bluetooth expanded peripheral card (device name + battery percentage).
  - `REQ-54`: 5 Bluetooth device category silhouettes (Headphones, Buds, Watch, Speaker, Car Audio).
  - `REQ-55`: Custom Bluetooth device icon upload via Android Photo Picker.
* **Implementation Scope (5 Days) + Motion Design (2 Days):**
  1. Build `BluetoothConnectionReceiver` handling `BluetoothDevice.ACTION_ACL_CONNECTED` and `ACTION_ACL_DISCONNECTED`.
  2. Implement `BluetoothBatteryExtractor` querying Bluetooth GATT battery service (UUID `0x180F`) and vendor intent extras (`android.bluetooth.device.extra.BATTERY_LEVEL`).
  3. Classify devices using `BluetoothDevice.getBluetoothClass().getDeviceClass()`.
  4. Author curated vector assets for all 5 device categories in both reference and Nothing styles.
  5. Add Photo Picker integration storing persistent image URIs in DataStore for custom peripheral avatars.
  6. Build `BluetoothCompactView.kt` and `BluetoothExpandedView.kt` in Compose overlay.
* **Verification & Acceptance Scope (2 Days):**
  - Connect Nothing Ear (2) and Bluetooth car audio; verify correct category silhouette and live battery ring.
  - Assign custom gallery photo to earbuds; verify photo renders sharply inside capsule.

---

### Batch 6: Advanced Customization, StandBy Clocks & Weather (Sprint 7)
* **Strategic Objective:** Deliver the high-end personalization features, ambient dock modes, dynamic weather animations, and custom notification styling.
* **Requirements Addressed (23 Requirements):**
  - `REQ-11`: Flashy border glow animation master switch.
  - `REQ-12`: Border animation speed stepper ($1x-5x$).
  - `REQ-13`: Border thickness stepper ($1-8dp$).
  - `REQ-14`: Multi-color border palette chips with dynamic gradient assembler.
  - `REQ-17`: Vinyl album artwork rotation toggle.
  - `REQ-30`: Granular notification typography steppers (dynamically scale overlay text).
  - `REQ-31`: Custom notification background and accent color pickers.
  - `REQ-32`: Dynamic app icon adaptive color extraction via Palette.
  - `REQ-33`: Small notch display alignment toggle (Left / Center / Right).
  - `REQ-38`: Dual-capsule split island layout.
  - `REQ-50`: Game Mode notification suppression toggle.
  - `REQ-52`: Small notch icon badge presentation styles (4 modes).
  - `REQ-58`: Download progress live activity tracking.
  - `REQ-82`: StandBy landscape charging dock mode and ambient clocks.
  - `REQ-85`: Live timer & stopwatch countdown capsule.
  - `REQ-86`: Privacy & media recording status badges.
  - `REQ-87`: Notch silhouette / cutout shape customization (Pill, Teardrop, Waterdrop, Circle).
  - `REQ-88`: App-wise dynamic notch color tinting matching foreground app.
  - `REQ-89`: Flashy gradient border glow shader.
  - `REQ-90`: Heads-Up Popup Suppression (Double Notification Prevention - Experimental / ADB-Assisted; requires `WRITE_SECURE_SETTINGS` via ADB to set `heads_up_notifications_enabled = 0`; standard NotificationListener cannot reliably prevent native heads-up popup before SystemUI renders it).
  - `REQ-91`: Notch auto-kill on full-screen / gaming applications.
  - `REQ-92`: Camera punch-hole flashlight / notification LED ring.
  - `REQ-95`: Weather live activity and 44 dynamic weather animations (22 conditions $\times$ 2 layouts).
* **Implementation Scope (9 Days) + Motion Design (6 Days):**
  1. Implement `StandByOverlayHost.kt` triggered by landscape orientation while charging, rendering ambient split-screen digital/analog clocks and calendar.
  2. Author/recreate 44 weather animations representing 22 weather conditions across Big and Small layouts.
  3. Implement `GradientBorderModifier.kt` using Compose `drawWithContent` and rotating `Brush.sweepGradient`.
  4. Wire `NotificationStudioScreen.kt` typography steppers to dynamic font size multipliers in `NotificationCard.kt`.
  5. Implement foreground package detection via Accessibility to suppress overlay in blacklisted apps.
  6. Implement `PaletteExtractionEngine` caching foreground app icon palettes and tinting island background.
  7. Build live timer parser reading active Clock notifications and updating live second countdowns.
* **Verification & Acceptance Scope (3 Days):**
  - Rotate phone horizontally while plugged in; verify ambient StandBy clock activates.
  - Toggle glowing border; customize speed to 3x and 3 colors; verify smooth rotating gradient.
  - Launch Netflix fullscreen; verify island completely hides automatically.

---

### Batch 7: Localization, Live Tracking & Free AI Companion (Sprint 8)
* **Strategic Objective:** Final polish, global accessibility, live activity scrapers, and delivering a 100% free, private on-device AI assistant.
* **Requirements Addressed (6 Requirements):**
  - `REQ-10`: Keyguard Unlock Feedback & Lockscreen State Card (`Intent.ACTION_USER_PRESENT`; visual unlock feedback upon keyguard dismissal, without identifying or inferring the authentication method).
  - `REQ-39`: Onboarding swipe gesture tutorial.
  - `REQ-83`: Live sports scores scraping (Cricket, Football, NBA).
  - `REQ-84`: Food and package delivery order tracking scraping.
  - `REQ-93`: Localized multi-language support (30+ language resources in `res/values-{lang}/strings.xml`).
  - `REQ-94`: On-device AI assistant integration ("Chat with AI") without coin meters or paywalls.
* **Implementation Scope (5 Days) + Motion Design (2 Days):**
  1. Extract all hardcoded strings across Studio and overlay into `strings.xml`.
  2. Port 30+ localized string catalogs from reference `aapt_strings.txt` into standard Android resource folders.
  3. Package a lightweight quantized ONNX / MediaPipe local LLM (e.g. Gemma-2B / Qwen-0.5B via MediaPipe GenAI SDK) or free API key provider, offering instant offline answers directly from expanded island.
  4. Build keyguard-unlock feedback listener/receiver (`Intent.ACTION_USER_PRESENT`), lockscreen state card, and interactive onboarding tutorial pager.
  5. Perform Android Studio GPU profiler trace; eliminate overdraw; verify zero memory leaks using LeakCanary.
* **Verification & Acceptance Scope (2 Days):**
  - Change device language to Spanish, German, Japanese; verify all Studio controls and island cards render localized text.
  - Invoke AI assistant from island; ask a question offline; receive instant response.
  - Profile 60Hz/120Hz scrolling and transitions via Android Studio GPU Profiler / Perfetto; verify jank rate <5% (frames >8.33ms at 120Hz or >16.66ms at 60Hz) and P99 frame render time <16ms.

---

## 3. Reassessed Project Effort & Schedule Breakdown

The revised implementation and testing schedule separates core software engineering from visual asset recreation and physical device acceptance:

| Batch / Subsystem | Target Sprint | Engineering Effort | Asset & Motion Design | Physical Acceptance | Total Working Days |
|---|---|---|---|---|---|
| **Batch 1: Core System Events & Gestures** | Sprint 2 | 5 Days | 0 Days | 2 Days | **7 Days** |
| **Batch 2: Exhaustive Themes & Animations** | Sprint 3 | 8 Days | 6 Days | 3 Days | **17 Days** |
| **Batch 3: Quick Settings & App Shortcuts** | Sprint 4 | 6 Days | 0 Days | 2 Days | **8 Days** |
| **Batch 4: Telecom & Call Intelligence** | Sprint 5 | 10 Days | 0 Days | 4 Days | **14 Days** |
| **Batch 5: Peripheral & Bluetooth Hub** | Sprint 6 | 5 Days | 2 Days | 2 Days | **9 Days** |
| **Batch 6: Advanced Customization & Weather**| Sprint 7 | 9 Days | 6 Days | 3 Days | **18 Days** |
| **Batch 7: Localization, Sports & AI** | Sprint 8 | 5 Days | 2 Days | 2 Days | **9 Days** |
| **Total Project Effort** | **Sprints 2–8** | **48 Working Days** | **16 Working Days** | **18 Working Days** | **82 Working Days** |

*Estimated Calendar Duration: ~3.5 to 4 Months (assuming 1 Full-Time Equivalent Developer + Visual Asset Pipeline).*

---

## 4. Android 17 Platform Risk Mitigation & Fallbacks

| Subsystem | Primary Compliant Mechanism | Risk / Restriction | Fallback Strategy |
|---|---|---|---|
| **Heads-Up Suppression (`REQ-90`)** | Optional ADB permission (`android.permission.WRITE_SECURE_SETTINGS`) | `NotificationListenerService` cannot reliably suppress another app's native heads-up banner before SystemUI renders it. No standard public runtime permission grants overlay suppression over system heads-up notifications. | Document as an optional ADB-assisted experimental feature (`adb shell pm grant com.nothing.island.ai android.permission.WRITE_SECURE_SETTINGS`). On unrooted devices without ADB, Nothing Island renders in parallel with system shade. |
| **Cellular Calls (`REQ-40`, `41`, `78`)** | `CallScreeningService` + `ANSWER_PHONE_CALLS` | `InCallService` requires default dialer role for full in-call mute/speaker control on some OEMs | Operate as Companion In-Call HUD displaying caller details, call duration, and dialing intent; active mute/speaker falls back to communication device routing. |
| **VoIP Calls (`REQ-43`)** | `NotificationListenerService` + `Notification.CallStyle` | WhatsApp, Telegram, etc. do NOT expose in-call mute/speaker toggles via notifications; BAL restrictions can block background call UI launch. | Support incoming caller presentation and Answer/Decline action dispatch where provided. Clearly document that universal VoIP in-call controls are unsupported by third-party apps. |
| **Quick Settings (`REQ-77`)** | `CameraManager`, `AudioManager`, `Settings.System` | Wi-Fi / BT / Data direct toggles blocked since Android 10 | Launch native System Settings Panels (`Settings.Panel.ACTION_INTERNET_CONNECTIVITY`) with 1 tap. |
| **AI Assistant (`REQ-94`)** | MediaPipe GenAI / Quantized Local ONNX model | Nothing Phone (4a) lacks pre-installed Google AICore | Offline quantized local model bundled in asset splits + optional user API key for cloud LLMs. |
| **Bluetooth Battery (`REQ-01`..`03`)**| Android HFP intent extras + GATT Battery Service | Proprietary vendor beacons (e.g. Apple AirPods) | Integrated open-source BLE beacon advertisement parser for vendor accessories. |
