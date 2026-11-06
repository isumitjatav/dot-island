# Reference Video & Media Inventory — NectarLink & Reference Audit

**Document ID:** `SPEC-TRACE-2026-NL1`  
**Date:** November 6, 2026  
**Reference Directory:** `reference/nectarlink/`  
**Target Hardware:** Nothing Phone (4a), Android 17 / API 37 (Screen: 1224 × 2720 px)  
**Accepted Baseline:** R8 (Battery & Charging Intelligence, 235 Unit Tests)  
**Status:** COMPLETE (8 Videos + 3 Screenshots fully audited)

---

## 1. Complete File Inventory

| # | Filename | Type | Dimensions / Duration | Size | Source Application | Screen / Functional Area |
|---|---|---|---|---|---|---|
| 1 | `screen-20261106-142641-1793955290358.mp4` | MP4 Video | 720 × 1600 · 01:51 (111.12s) | 42.53 MB | Dynamic Notch (v6.60) | Charging Animation Gallery (`SmallChargingNotchAnimActivity`) |
| 2 | `screen-20261106-142924-1793955515723.mp4` | MP4 Video | 720 × 1600 · 00:48 (48.90s) | 21.16 MB | Dynamic Notch (v6.60) | System Call & VoIP Configuration (`Call`) |
| 3 | `screen-20261106-143024-1793955579091.mp4` | MP4 Video | 720 × 1600 · 00:45 (45.09s) | 20.92 MB | Dynamic Notch (v6.60) | Notification Settings & Disappear Policies (`Notification`) |
| 4 | `screen-20261106-143139-1793955665349.mp4` | MP4 Video | 720 × 1600 · 00:34 (34.12s) | 6.42 MB | Dynamic Notch (v6.60) | Bluetooth Device Categories & Custom Assets (`Bluetooth Connection`) |
| 5 | `screen-20261106-143243-1793955755316.mp4` | MP4 Video | 720 × 1600 · 00:07 (7.71s) | 1.43 MB | Dynamic Island Pro (v1.9.2) | Notification Preview & Auto-Collapse Lifecycle (`Notifications`) |
| 6 | `screen-20261106-143306-1793955775869.mp4` | MP4 Video | 720 × 1600 · 00:10 (10.83s) | 3.69 MB | Dynamic Island Pro (v1.9.2) | Incoming Call Actions & Preview (`Incoming Call`) |
| 7 | `screen-20261106-143329-1793955799702.mp4` | MP4 Video | 720 × 1600 · 00:09 (9.62s) | 3.66 MB | Dynamic Island Pro (v1.9.2) | Compact Music Equalizer Animation (`Music`) |
| 8 | `screen-20261106-143416-1793955826518.mp4` | MP4 Video | 720 × 1600 · 00:29 (29.62s) | 11.08 MB | Dynamic Island Pro (v1.9.2) | Theme Selection Dialog & Presets Grid (`Theme`) |
| 9 | `Screenshot_20261106-143535.png` | PNG Image | 1080 × 1440 | 582 KB | Dynamic Island Pro / Capsule | Download Tracking Live Activity (Compact + Expanded Wavy Bar) |
| 10 | `Screenshot_20261106-143550.png` | PNG Image | 1080 × 1440 | 698 KB | Dynamic Island Pro / Capsule | Live Charging Stats (6-Metric Telemetry Grid) |
| 11 | `Screenshot_20261106-143608.png` | PNG Image | 1080 × 1440 | 689 KB | Dynamic Island Pro / Capsule | Various Capsule Events (18 Compact Pill Archetypes) |

*(Note: Unrelated non-media files in directory: `Assignment BME UNIT-II.pdf`, `kaggle_survey_2020_responses.csv.zip`, and 3 Jupyter notebooks).*

---

## 2. Granular Feature & Requirements Inventory

### 2.1 Media 1: Charging Gallery Live Preview (`screen-20261106-142641-1793955290358.mp4`)
- **[NL-REQ-01] Live Gallery Simulation:** Interactive selection of notch animations in a 2-column grid. Tapping a theme (e.g. Petals at `00:30`, Heart at `01:00`, Robot at `01:30`, Ball at `01:45`) updates the persistent top preview card immediately.
- **[NL-REQ-02] Real Hardware Battery Reflection:** Preview header reflects actual physical device battery percentage (100% on Nothing Phone 4a) with live visualizer rendering.

### 2.2 Media 2: System Call & VoIP Engine (`screen-20261106-142924-1793955515723.mp4`)
- **[NL-REQ-03] Master Call Switch:** `Enable System Call` toggle switch (`00:02`).
- **[NL-REQ-04] Two-Tier Call Presentation:**
  - Compact pill: Elapsed call duration timer (`00:07`) on left + jumping green equalizer on right (`00:05`).
  - Expanded card: Contact avatar ("Daisy Williams"), duration timer (`00:11`), speakerphone toggle button, red end-call button (`00:08`).
- **[NL-REQ-05] Native Call Collision Suppression:** `Double Incoming Call Actions` toggle ("Hide notch call UI when native system full-screen call UI appears") (`00:15`).
- **[NL-REQ-06] VoIP / Third-Party Call Support:** `Other Apps / Voip Calls` toggle for WhatsApp, Telegram, etc. (`00:20`).
- **[NL-REQ-07] Post-Call Summary Theme:** Radio selector for post-call card appearance: `Light Mode`, `Dark Mode`, `System Theme` (`00:25`).
- **[NL-REQ-08] Keyguard Call Visibility:** `Show Incoming Call Notch in Lock Screen` toggle (`00:32`).
- **[NL-REQ-09] Game Mode Call Policies:** Independent switches for `Show Incoming Call Notch` and `Show Ongoing Call Notch` during full-screen games (`00:38`).
- **[NL-REQ-10] Contact Blacklist:** `Blocked Contacts` picker (`00:44`).

### 2.3 Media 3: Advanced Notification Engine (`screen-20261106-143024-1793955579091.mp4`)
- **[NL-REQ-11] Notification Sizing Mode:** Visual selector between `Expanded Popup` and `Small Popup` (`00:02`).
- **[NL-REQ-12] DND Auto-Expand Bypass:** `Auto Expand Notification During DND` toggle switch (`00:05`).
- **[NL-REQ-13] Small Notch Disappear Policy:** 3-way radio selection: `Auto Disappear`, `Never Disappear Popup`, `Disappear only after Read` (`00:12`).
- **[NL-REQ-14] Configurable Expanded Duration:** Stepper slider for popup persistence (`Expanded Popup Visibility`: 1–10s, default 5s, `-`/`+` steppers) (`00:18`).
- **[NL-REQ-15] Swipe to Dismiss Gesture:** Direct `Swipe to remove notification` toggle switch (`00:24`).
- **[NL-REQ-16] Game Mode Notification Policy:** `Notifications During Games` toggle switch (`00:28`).
- **[NL-REQ-17] Silent App Expansion Policy:** `Expand Silent App Notifications` toggle switch (`00:33`).
- **[NL-REQ-18] Small Notch Icon Badge Presentation:** 4-way selector: `Profile/Number badge`, `Number badge`, `Arrow icon`, `Nothing when no cover image` (`00:38`).
- **[NL-REQ-19] Manual Service Sync:** `Refresh Notification` button (`00:42`).

### 2.4 Media 4: Categorized Bluetooth Assets (`screen-20261106-143139-1793955665349.mp4`)
- **[NL-REQ-20] Master Bluetooth Switch:** `Enable Bluetooth Connection` toggle (`00:02`).
- **[NL-REQ-21] Real-Time Bluetooth Preview:** Pill displaying device icon with blue BT glyph, subtext "Bluetooth Connected", device title, and circular green battery ring with percentage (`00:05`).
- **[NL-REQ-22] Device Type Categorization:** Accordion list for 5 distinct device classes:
  1. `TWS / Earbuds` (5 presets: dual buds, single stem, open bean case, open AirPods case, mono headset) (`00:08`).
  2. `Neckbands / Wired Bluetooth` (5 presets: sport neckband, contoured band, standard wired buds) (`00:12`).
  3. `Headphones` (5 presets: over-ear studio, sleek wireless, gaming headset with boom mic) (`00:18`).
  4. `Smartwatch` (5 presets: squircle watch, square watch, circular sport watch, rugged watch, smart band) (`00:22`).
  5. `Speakers` (5 presets: soundbar, spherical orb, cylindrical pill, party box, large tower) (`00:26`).
- **[NL-REQ-23] Custom Device Asset Import:** `[Add Custom]` button per category to import user PNG/SVG icons (`00:10`).
- **[NL-REQ-24] Live Icon Switcher:** Tapping any preset updates the active notch preview dynamically (`00:30`).

### 2.5 Media 5, 6, 7: Dynamic Island Pro Engine & Motion Loop
- **[NL-REQ-25] Service Status Banner:** `⚠️ Notch is Not Active | Activate` red banner present across all studio headers (`v5` `00:01`, `v6` `00:01`, `v7` `00:01`, `v8` `00:01`).
- **[NL-REQ-26] Notification Motion Lifecycle:** Notification expands from idle notch -> displays app icon, title, snippet, "Now" timestamp -> holds 3s -> smoothly collapses back to idle notch (`v5` `00:01`–`00:07`).
- **[NL-REQ-27] Compact Call Controls:** Incoming call pill preview displaying circular contact avatar ("S"), subtext "mobile", contact name "Sarah Johnson", red decline button and green accept button (`v6` `00:01`–`00:09`).
- **[NL-REQ-28] Jumping Equalizer Waveform:** Compact music pill preview with album art square on left and dynamic bouncing 5-bar green equalizer on right (`v7` `00:01`–`00:08`).

### 2.6 Media 8: Themes Grid & Confirmation Dialog (`screen-20261106-143416-1793955826518.mp4`)
- **[NL-REQ-29] Theme Confirmation Dialog:** Tapping a preset opens a confirmation bottom sheet / modal with preview notch, theme name, and `Set Theme` (Blue) / `Cancel` buttons (`00:02`).
- **[NL-REQ-30] 40+ Colorway Presets:** Complete visual roster: Classic Black, Minimal Gray, Midnight, Amethyst, Sunset, Neon Cyber, Royal Gold, Ocean Breeze, Crimson Fire, Deep Space, Forest Rain, Purple Haze, Cherry Blossom, Golden Hour, Carbon Fiber, Electric Blue, Sweet Candy, Northern Lights, Pure White, Vampire Red, Emerald City, Cosmic Void, Rose Gold, Ice Cold, Nebula, Bumblebee, Slate, Rainbow, Stormy Night, Bubblegum, Toxic Green, Ruby Red, Deep Sea, Pumpkin, Titanium, Influencer, Glitch, Copper, Lavender, Stealth, Miami Vice, Miami Vibes, Zen Garden, Pastel Dream (`00:04`–`00:28`).

### 2.7 Screenshots 1, 2, 3: Promotional Live Activities & Capsule Archetypes
- **[NL-REQ-31] Download Tracking Capsule & Card (`Screenshot_20261106-143535.png`):**
  - Compact: Play Store icon badge + circular progress ring around camera cutout.
  - Expanded: App icon badge + "Google Play Store · 51 seconds..." + dual-mode progress bar (wavy active section + flat remaining section) + circular chevron action button `>`.
- **[NL-REQ-32] 6-Metric Live Charging Telemetry Grid (`Screenshot_20261106-143550.png`):**
  - Compact: Plug icon + battery % ("34%").
  - Expanded: 2x3 telemetry grid:
    1. Elapsed/Remaining Time (`05:59`)
    2. Battery Level (`34%`)
    3. Voltage (`3.8 V`)
    4. Temperature (`40.3°C`)
    5. Current (`≈ 0.46 A`)
    6. Power / Wattage (`1.7 W`)
    - Action button: Circular right chevron `>` linking to Battery Studio.
- **[NL-REQ-33] 18 Capsule Event Archetypes (`Screenshot_20261106-143608.png`):**
  - Battery Charging (Bolt + Battery icon)
  - Ringer Mute / Vibrate (Muted bell + Red/Yellow/Pink `X` badge)
  - Media Playing (Art + Equalizer bars)
  - Sound Active (Speaker + Yellow checkmark)
  - Headphone Connected (Mint/White headphone + Checkmark badge)
  - Flashlight Active (Blue flashlight + Beam indicator)
  - Wi-Fi Status (Wi-Fi waves + Smiley face badge)
  - TWS Earbuds (Earbuds icon + `50%` battery)
  - Bedtime / DND (Green crescent moon badge)

---

## 3. Traceability & Disposition Against Screenshot Requirements (REQ-1 to REQ-39)

| NectarLink Req ID | Description | Source Media & Timestamp | Relationship to Existing 39 Screenshot Reqs | Disposition / Architectural Impact |
|---|---|---|---|---|
| **NL-REQ-01** | Live charging gallery interactive selection | `v1` (00:30–01:45) | Confirms & expands **REQ-5** | Proves interactive preview updates top notch immediately. |
| **NL-REQ-02** | Real hardware battery reflection (100%) | `v1` (00:02–01:50) | Confirms **REQ-6**, **REQ-7** | Implemented in `BatteryCompactView.kt`. |
| **NL-REQ-03** | Master system call switch | `v2` (00:02) | **NEW REQUIREMENT** | Target: R9 Telecom Subsystem & Settings Studio. |
| **NL-REQ-04** | Two-tier call presentation (pill vs card) | `v2` (00:05–00:11) | **NEW REQUIREMENT** | Target: R9 Telecom Subsystem (`CallCompactView` / `CallExpandedView`). |
| **NL-REQ-05** | Double incoming call suppression | `v2` (00:15) | **NEW REQUIREMENT** | Prevents overlay overlap with native Nothing OS phone app. |
| **NL-REQ-06** | VoIP call support (WhatsApp/Telegram) | `v2` (00:20) | **NEW REQUIREMENT** | Target: R9 Telecom Subsystem. |
| **NL-REQ-07** | Post-call summary theme picker | `v2` (00:25) | **NEW REQUIREMENT** | Target: R9 Telecom Studio. |
| **NL-REQ-08** | Lockscreen call display toggle | `v2` (00:32) | **NEW REQUIREMENT** | Keyguard window manager policy. |
| **NL-REQ-09** | Game mode call toggles | `v2` (00:38) | **NEW REQUIREMENT** | Game dashboard integration. |
| **NL-REQ-10** | Blocked contacts blacklist | `v2` (00:44) | **NEW REQUIREMENT** | Telecom blacklist repository. |
| **NL-REQ-11** | Notification popup sizing (Small vs Expanded) | `v3` (00:02) | Confirms **REQ-25** | Wired to DataStore `notif_preview_mode`. |
| **NL-REQ-12** | DND auto-expand bypass toggle | `v3` (00:05) | Confirms **REQ-26** | Target: `NotificationStudioScreen.kt`. |
| **NL-REQ-13** | 3-Way disappear mode (Auto/Never/After Read) | `v3` (00:12) | **NEW REQUIREMENT** | Enhances notification dismissal lifecycle. |
| **NL-REQ-14** | Expanded popup visibility duration stepper (5s) | `v3` (00:18) | **RESOLVES CONFLICT** | Replaces fixed 4.0s timeout with configurable stepper slider (1–10s). |
| **NL-REQ-15** | Swipe to remove notification toggle | `v3` (00:24) | **NEW REQUIREMENT** | Overlay touch gesture preference. |
| **NL-REQ-16** | Notification game mode policy | `v3` (00:28) | **NEW REQUIREMENT** | Suppress popups during gaming sessions. |
| **NL-REQ-17** | Expand silent app notifications toggle | `v3` (00:33) | **NEW REQUIREMENT** | Notification manager channel filtering. |
| **NL-REQ-18** | Small notch icon style selector (4 modes) | `v3` (00:38) | **NEW REQUIREMENT** | Badge customization for compact notch. |
| **NL-REQ-19** | Refresh notification service button | `v3` (00:42) | **NEW REQUIREMENT** | Diagnostic service sync action. |
| **NL-REQ-20** | Master Bluetooth toggle | `v4` (00:02) | Confirms **REQ-1** | Target: Peripheral Studio. |
| **NL-REQ-21** | Bluetooth preview with battery ring | `v4` (00:05) | Confirms **REQ-2**, **REQ-3** | Implemented in R9 Peripheral Subsystem. |
| **NL-REQ-22** | 5 Bluetooth device categories & silhouettes | `v4` (00:08–00:26) | **NEW REQUIREMENT** | TWS, Neckbands, Headphones, Watch, Speakers. |
| **NL-REQ-23** | Add custom device icon import | `v4` (00:10) | **NEW REQUIREMENT** | Image URI picker in DataStore. |
| **NL-REQ-24** | Live Bluetooth icon switcher | `v4` (00:30) | **NEW REQUIREMENT** | In-studio dynamic preview update. |
| **NL-REQ-25** | Inactive service warning banner | `v5`, `v6`, `v7`, `v8` | Confirms **REQ-4** | Reusable `NothingWarningBanner` across studios. |
| **NL-REQ-26** | Notification preview collapse animation loop | `v5` (00:01–00:07) | Confirms **REQ-33** | Verifies 3.0s auto-collapse physics. |
| **NL-REQ-27** | Compact incoming call preview actions | `v6` (00:01–00:09) | **NEW REQUIREMENT** | Compact call action pills. |
| **NL-REQ-28** | Dynamic bouncing 5-bar equalizer | `v7` (00:01–00:08) | Confirms **REQ-16** | 5-bar vertical jumping audio visualizer. |
| **NL-REQ-29** | Theme confirmation bottom sheet / dialog | `v8` (00:02) | **NEW REQUIREMENT** | Set Theme / Cancel modal interaction. |
| **NL-REQ-30** | 40+ Colorway Presets Grid | `v8` (00:04–00:28) | Confirms & expands **REQ-8** | Expands palette roster from 32 to 40+ presets. |
| **NL-REQ-31** | Download tracking live activity (Wavy bar) | `Screenshot_143535` | **NEW REQUIREMENT** | Target: R11 Live Activities (Downloads & Play Store). |
| **NL-REQ-32** | 6-Metric charging telemetry grid | `Screenshot_143550` | **EXPANDS REQ-5, REQ-7** | Replaces basic battery % with comprehensive 6-tile grid. |
| **NL-REQ-33** | 18 Compact Capsule Archetypes | `Screenshot_143608` | **EXPANDS REQ-36, REQ-37** | Adds Flashlight, Wi-Fi, DND Bedtime, and TWS capsules. |

---

## 4. Conflict Analysis & Resolution

1. **Popup Dismissal Timeout:**
   - *Previous Spec:* Fixed 4.0s timeout for expanded notification popups.
   - *NectarLink Evidence (`v3` 00:18):* User-configurable stepper slider (`Expanded Popup Visibility`: 1 to 10 seconds, default 5s).
   - *Resolution:* Adopt the configurable duration stepper slider in `NotificationStudioScreen.kt`.

2. **Call Presentation Hierarchy:**
   - *Dynamic Island Pro (`v6`):* Showed accept/decline action buttons directly inside the compact notch pill.
   - *Dynamic Notch (`v2`):* Kept compact notch clean (timer + equalizer), moving large interactive buttons (speakerphone, decline, mute) to the expanded card.
   - *Resolution:* Adopt Dynamic Notch's 2-tier model. Overlay touch targets on small compact cutouts (36dp height) are error-prone; expanded card provides accessibility-compliant 48dp+ tap targets.

3. **Notification Persistence Policy:**
   - *Previous Spec:* Auto-dismiss after timeout only.
   - *NectarLink Evidence (`v3` 00:12):* 3 selectable modes: `Auto Disappear`, `Never Disappear Popup`, `Disappear only after Read`.
   - *Resolution:* Add `notif_disappear_mode` enum preference to support sticky persistent heads-up notifications when desired.
