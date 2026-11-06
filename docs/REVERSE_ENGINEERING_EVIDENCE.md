# Reverse-Engineering Evidence Index: Dynamic Spot & Dynamic Island Pro

**Project:** Nothing Island (`com.nothing.island.ai`)  
**Auditor:** Senior Android Build & Systems Engineer  
**Date:** 2026-11-04  
**Classification Standard:**
- **VERIFIED**: Directly observed, decompiled, extracted, or validated on physical device / APK.
- **PARTIALLY VERIFIED**: Observable artifact exists, but internal implementation details are incomplete.
- **INFERRED**: Deduced from external UI behavior or standard Android patterns; not verified internally.
- **UNKNOWN**: No reliable evidence found in history or on disk.

---

## 1. Physical APK Artifacts Recovered

Both original production APKs pulled directly from the physical Nothing Phone (4a) (`A069` / `FroggerIND`) remain preserved on the local filesystem:

| Application | Package Identifier | Local File Path | File Size | SHA-256 Digest | Evidence Status |
|---|---|---|---|---|---|
| **dynamicSpot** | `com.jamworks.dynamicspot` | `C:\Users\User\.gemini\antigravity\scratch\dynamicspot\dynamicspot_base.apk` | 13,300,229 bytes (13.3 MB) | `535c4594cab3b9ae8454c30d288c4f057142cf9e0fd535e8a9a12135a33a3ef9` | **VERIFIED** |
| **Dynamic Island Pro** | `tr.com.eywin.dynamic.island.notch` | `C:\Users\User\.gemini\antigravity\scratch\dynamicisland_pro\dynamicisland_base.apk` | 53,722,521 bytes (53.7 MB) | `3616a566a55b8281b0b4236b0f01648690b82184c2561e5958cfb5dff1136cc6` | **VERIFIED** |

---

## 2. REA Tool Execution & Extraction Manifests

The **REA (`morluto/rea`)** safe artifact graph extraction outputs are preserved in `AppData\Local\Temp`:

### A. dynamicSpot Extraction
- **Temp Directory:** `C:\Users\User\AppData\Local\Temp\rea-extracted-0f01de20-4964-4437-ba6a-d005f10c596a`
- **Manifest ID:** `agm_75cca108991f35c3a45077eb08a8a5ff511d3835a836599973737f3e43f01e05`
- **Artifact Count:** 1,013 nodes, 1,082 occurrences, 1,081 edges.
- **Key Materialized Files:**
  - `AndroidManifest.xml` (AXML binary)
  - `classes.dex` (Dalvik executable)
  - `resources.arsc` (Compiled resource table, 2,208,592 bytes)
  - `res/layout/` (82 decompiled custom XML layout structures)

### B. Dynamic Island Pro Extraction
- **Temp Directory:** `C:\Users\User\AppData\Local\Temp\rea-extracted-cc16636a-3422-4362-9a08-c1b41265b4bf`
- **Evidence ID:** `ev_834ff32607a620fac4f3662de7e2fe39e3d477c747b033176cf1c415c1086e24`
- **Key Materialized Files:**
  - `AndroidManifest.xml` (AXML binary)
  - `classes.dex`, `classes2.dex` (Dalvik executables)
  - `res/layout/` (114 decompiled custom XML layout structures)

---

## 3. Verified Permissions & Manifest Declarations (via AAPT)

Extracted using Android SDK Build-Tools 35.0.0 / 36.0.0 `aapt.exe`:

### A. dynamicSpot (`com.jamworks.dynamicspot` v2.01, code 200142)
- `compileSdkVersion`: 36 | `targetSdkVersion`: 36 | `minSdkVersion`: 28
- **Permissions:**
  - `android.permission.SYSTEM_ALERT_WINDOW` [**VERIFIED** - Overlay window creation]
  - `android.permission.BIND_ACCESSIBILITY_SERVICE` [**VERIFIED** - Accessibility gesture interception]
  - `android.permission.FOREGROUND_SERVICE` [**VERIFIED** - Service persistence]
  - `android.permission.POST_NOTIFICATIONS` [**VERIFIED** - Android 13+ status notifications]
  - `android.permission.RECEIVE_SENSITIVE_NOTIFICATIONS` [**VERIFIED** - Android 15/16 sensitive notification listener]
  - `android.permission.RECORD_AUDIO` [**VERIFIED** - Real-time audio waveform visualizer]
  - `android.permission.MODIFY_AUDIO_SETTINGS` [**VERIFIED** - Audio routing/volume controls]
  - `android.permission.BIND_INCALL_SERVICE` [**VERIFIED** - Direct telephony call screen replacement]
  - `android.permission.READ_CONTACTS` [**VERIFIED** - Caller identification popups]
  - `android.permission.BLUETOOTH` / `BLUETOOTH_CONNECT` [**VERIFIED** - Headset/battery connection status]
  - `android.permission.CAMERA` [**VERIFIED** - Flashlight toggle]
  - `android.permission.WRITE_SETTINGS` / `WRITE_SECURE_SETTINGS` [**VERIFIED** - System setting adjustment]
  - `android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` [**VERIFIED** - Doze bypass]
  - `android.permission.VIBRATE` / `WAKE_LOCK` / `RECEIVE_BOOT_COMPLETED` [**VERIFIED** - Haptics, wake, boot]
  - `com.android.vending.BILLING` / `com.google.android.gms.permission.AD_ID` [**VERIFIED** - Monetization]

### B. Dynamic Island Pro (`tr.com.eywin.dynamic.island.notch` v1.9.2, code 21)
- `compileSdkVersion`: 36 | `targetSdkVersion`: 36 | `minSdkVersion`: 24
- **Permissions:**
  - `android.permission.FOREGROUND_SERVICE` [**VERIFIED** - Persistent background service]
  - `android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK` [**VERIFIED** - Android 14+ FGS Media type]
  - `android.permission.MEDIA_CONTENT_CONTROL` [**VERIFIED** - Media session tracking & playback controls]
  - `android.permission.ANSWER_PHONE_CALLS` / `CALL_PHONE` [**VERIFIED** - In-island call answering and dialing]
  - `android.permission.CAMERA` [**VERIFIED** - Flashlight control & QR scanner overlay]
  - `android.permission.QUERY_ALL_PACKAGES` [**VERIFIED** - App drawer overlay package enumeration]
  - `android.permission.WRITE_SETTINGS` [**VERIFIED** - Quick controls screen brightness toggle]
  - `android.permission.MODIFY_AUDIO_SETTINGS` [**VERIFIED** - Quick controls volume toggle]
  - `android.permission.BLUETOOTH_CONNECT` [**VERIFIED** - Bluetooth battery display]
  - `android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` [**VERIFIED** - Battery optimization bypass]
  - `android.permission.POST_NOTIFICATIONS` [**VERIFIED** - Android 13+ notification permission]

---

## 4. Decompiled Layout XML Evidence

Direct inspection of `res/layout` in the materialized extraction folders confirms specific UI modules:

### A. dynamicSpot Specific Layouts
1. `popup_first.xml` — **[VERIFIED]** Primary compact pill anchored around notch/cutout.
2. `popup_second.xml` — **[VERIFIED]** The dual-pill split layout for simultaneous multi-activity multitasking.
3. `popup_music.xml` / `popup_music_test.xml` — **[VERIFIED]** Media player layout with waveform bars and album art.
4. `popup_call.xml` / `popup_call_active.xml` — **[VERIFIED]** Incoming and ongoing call cards with contact name and answer/decline buttons.
5. `popup_live.xml` — **[VERIFIED]** Real-time activity card for background events.
6. `popup_time.xml` — **[VERIFIED]** Timer and stopwatch countdown card.
7. `popup_lock.xml` — **[VERIFIED]** Dedicated lockscreen overlay display mode.
8. `popup_big.xml` / `popup_x_big.xml` — **[VERIFIED]** Expanded interaction card with action shortcuts.
9. `popup_big_reply.xml` / `popup_big_reply_wide.xml` — **[VERIFIED]** In-line text input field for direct notification messaging reply.
10. `get_bat.xml` — **[VERIFIED]** Charging animation and battery percentage capsule.

### B. Dynamic Island Pro Specific Layouts
1. `fragment_notch.xml` — **[VERIFIED]** Camera cutout positioning, notch calibration offsets, and screen anchor.
2. `progress_count_down.xml` / `linear_player_count_down.xml` / `timer_count_down.xml` — **[VERIFIED]** Circular radial progress ring encircling or bounding the punch-hole camera.
3. `app_drawer_overlay.xml` / `app_driwer_item.xml` — **[VERIFIED]** Swipe-down mini application drawer for quick app launching.
4. `controls.xml` — **[VERIFIED]** Quick control toggle drawer (Flashlight, Camera, Brightness, Sound, Bluetooth).
5. `activity_qrcode_overlay.xml` — **[VERIFIED]** Integrated camera viewfinder QR code scanner overlay.
6. `item_medilayout.xml` / `bm_exo_player_view.xml` — **[VERIFIED]** ExoPlayer-based media playback scrubber and metadata view.
7. `activity_internal_browser.xml` — **[VERIFIED]** Embedded lightweight webview overlay.

---

## 5. Decompiled Dalvik Bytecode Strings (via classes.dex inspection)

Direct string pattern extraction against the respective `classes.dex` binaries revealed core implementation classes and animation engines:

### A. dynamicSpot Bytecode Matches
- **Core Architecture Classes:**
  - `Lcom/jamworks/dynamicspot/OverlayServiceSpot;` — **[VERIFIED]** Main overlay manager service.
  - `Lcom/jamworks/dynamicspot/NotificationObserverSpot;` — **[VERIFIED]** Notification listener service.
  - `Lcom/jamworks/dynamicspot/activities/CallService;` — **[VERIFIED]** InCallService implementation.
  - `Lcom/jamworks/dynamicspot/SeekBarPreferenceManual;` — **[VERIFIED]** Manual millimeter calibration slider.
- **Animation Framework:**
  - `android.animation.ValueAnimator` — **[VERIFIED]**
  - `android.view.animation.OvershootInterpolator` — **[VERIFIED]** (Produces the fluid rubber-band spring bounce upon expansion)
  - `android.view.animation.AccelerateDecelerateInterpolator` — **[VERIFIED]**
  - `android.view.animation.PathInterpolator` — **[VERIFIED]**

### B. Dynamic Island Pro Bytecode Matches
- **Core Architecture Classes:**
  - `Ltr/com/eywin/dynamic/island/notch/service/OverlayAccessibilityService;` — **[VERIFIED]** Accessibility overlay service.
  - `Ltr/com/eywin/dynamic/island/notch/service/MyNotificationListenerService;` — **[VERIFIED]** Notification listener service.
  - `Ltr/com/eywin/dynamic/island/notch/service/GuardianForegroundService;` — **[VERIFIED]** Foreground keepalive engine.
  - `Ltr/com/eywin/dynamic/island/notch/service/RestartServiceReceiver;` — **[VERIFIED]** Watchdog auto-restart receiver.

---

## 6. Historical Conversation Transcript References

All prior reverse-engineering activities conducted in this conversation session are recorded in:
- `C:\Users\User\.gemini\antigravity\brain\73799a67-ab48-4447-942e-910ba56140aa\.system_generated\logs\transcript.jsonl`
- `C:\Users\User\.gemini\antigravity\brain\73799a67-ab48-4447-942e-910ba56140aa\.system_generated\logs\transcript_full.jsonl`

**Key Step Index Milestones:**
- `Step 225`: User initial request: *"can we use REA repo on dynamic spot and create it for my desktop ai"*
- `Step 273`: ADB pull of `com.jamworks.dynamicspot` base APK from Nothing Phone.
- `Step 275`: REA analysis run on `dynamicspot_base.apk`.
- `Step 286`: REA artifact extraction of 1,013 nodes into temp directory.
- `Step 296`: Bytecode string extraction confirming `OverlayServiceSpot`, `NotificationObserverSpot`, `ValueAnimator`, and `OvershootInterpolator`.
- `Step 300`: Full architectural report on dynamicSpot layout states.
- `Step 301`: User request: *"now run rea on dynamic island pro and mix the features of both apps"*.
- `Step 305`: Device package scan locating `tr.com.eywin.dynamic.island.notch`.
- `Step 313`: ADB pull of `dynamicisland_base.apk` (53.7 MB).
- `Step 315`: REA artifact extraction of Dynamic Island Pro.
- `Step 328`: Comparative analysis of dynamicSpot vs Dynamic Island Pro.
- `Step 332`: Generation of Master Plan artifact (`dynamic_island_master_plan.md`).
- `Step 344`: Export of standalone Desktop AI specification (`dynamic_island_complete_developer_handoff.md`).

---

## 7. Physical Device Screenshots Recovered

High-resolution screen captures from the physical Nothing Phone (4a) (`A069`) demonstrating the active M3 overlay:

| Screenshot File | Description | Relative Path | Verification |
|---|---|---|---|
| `nothing_island_collapsed_final.png` | M3 Compact Pill wrapped symmetrically around camera punch-hole between clock and network speed meter on home screen. | `C:\Users\User\.gemini\antigravity\brain\73799a67-ab48-4447-942e-910ba56140aa\nothing_island_collapsed_final.png` | **VERIFIED** |
| `nothing_island_expanded_final.png` | M3 Expanded Card showing 30dp camera clearance notch, Nothing typography, live telemetry, and interactive controls floating over apps. | `C:\Users\User\.gemini\antigravity\brain\73799a67-ab48-4447-942e-910ba56140aa\nothing_island_expanded_final.png` | **VERIFIED** |
| `nothing_island_dashboard.png` | MainActivity dashboard displaying live permission status, service toggle, and hardware cutout calibration readout. | `C:\Users\User\.gemini\antigravity\brain\73799a67-ab48-4447-942e-910ba56140aa\nothing_island_dashboard.png` | **VERIFIED** |
