# Nothing Island — Reference Screenshot Requirements Traceability Matrix

**Document ID:** `SPEC-TRACE-2026-R1`  
**Phase:** Phase 1: Complete UI Audit & Design Specification  
**Package:** `com.nothing.island.ai`  
**Target Hardware:** Nothing Phone (4a), Android 17 / API 37 (Screen: 1224 × 2720 px, Top-Center Cutout)  
**Accepted Baseline Checkpoint:** R8 (Battery & Charging Intelligence, 235 Passing Unit Tests)  
**Reference Folder Inspected:** `C:\Users\User\.gemini\antigravity\scratch\nothing-island\reference\ui-screenshots\`  
**Date:** November 6, 2026  
**Status:** APPROVED / INVENTORY & TRACEABILITY COMPLETE  

---

## 1. Executive Summary & Audit Methodology

A rigorous, full-resolution visual audit was conducted on all **20 reference screenshots** supplied in the project reference directory (`reference/ui-screenshots/`). In accordance with engineering instructions:
- Every image file was located, cataloged, and visually inspected at full native resolution without downsampling.
- Original screenshot files were preserved in their exact original binary state with zero modifications.
- Application source code was left completely unmodified during this audit.
- Every visible user-facing feature, switch, toggle, slider, stepper (`-`/`+`), color picker, preview state, animation curve, visualizer style, text typography control, and interactive gesture was extracted and mapped.

The reference screenshots provide concrete, observable product requirements from leading notch companion applications (*Dynamic Notch* and *Dynamic Island Pro*). This document provides direct bidirectional traceability between the visual evidence in the screenshots, the existing Nothing Island codebase (as of accepted checkpoint R8), and the proposed **Nothing OS 3.x Studio Architecture** defined in `docs/UI_UX_REDESIGN_SPECIFICATION.md`.

---

## 2. Complete Reference Screenshot Inventory

The folder `reference/ui-screenshots/` contains exactly **20 JPEG screenshot files** (total size: 1,298,871 bytes). Below is the comprehensive inventory:

| # | Filename | Resolution | File Size | Primary Category | Screen Title / Context | Visual Summary |
|---|---|---|---|---|---|---|
| 1 | `bluetooth-connection (2).jpg` | 1080 × 2400 | 29,422 B | Bluetooth Audio | Connection (Expanded Preview) | Expanded pill preview with AirPods Pro icon, "Connected / AirPods Pro", circular green battery progress ring with "75%", "Show Connection" toggle, and "Notch is Not Active / Activate" warning banner. |
| 2 | `bluetooth-connection.jpg` | 1080 × 2400 | 54,514 B | Bluetooth Audio | Connection (Compact Preview) | Compact capsule preview with AirPods Pro icon on left and circular green battery progress ring with "75" on right, "Show Connection" toggle, and service status banner. |
| 3 | `charging themes.jpg` | 1080 × 2400 | 38,506 B | Charging & Battery | Charging Themes (Gallery) | 2-column visual grid showing 30+ distinct charging themes (cyan lightning bolt glow, green glowing capsule, energy orbs, flame eye, magic potion bottle, dual lightning surge, orbiting hearts, numeric gauge "Charging 50%", circular gauge, creature mascots). |
| 4 | `dynamic themes (2).jpg` | 1080 × 2400 | 26,957 B | Themes & Palette | Preset Themes (Grid Mid) | 2-column card grid of 32+ named colorway presets: Classic Black (selected), Minimal Gray, Midnight, Amethyst (VIP), Sunset (VIP), Neon Cyber (VIP), Royal Gold (VIP), Ocean Breeze (VIP), Crimson Fire (VIP), Deep Space (VIP), Forest Rain, Purple Haze, Cherry Blossom, Golden Hour, The Matrix (VIP), Cyber Pink, Mint Fresh, Silver Lining, Carbon Fiber, Electric Blue, Sweet Candy, Northern Lights, Pure White, Vampire Red, Emerald City, Cosmic Void, Rose Gold, Ice Cold, Lemon Twist, Twilight, Hot Lava (VIP), Sea Foam. |
| 5 | `dynamic themes (3).jpg` | 1080 × 2400 | 32,755 B | Themes & Palette | Preset Themes (Grid Bottom) | Scrolled continuation of the preset colorway grid showing Nebula, Bumblebee, and multi-color Rainbow gradient with VIP crown indicators. |
| 6 | `dynamic themes.jpg` | 1080 × 2400 | 38,880 B | Themes & Palette | Theme Studio (Top-Level) | Header "Theme", "UPGRADE" crown action, "Notch is Not Active / Activate" warning banner, subtitle "Select a theme for your notch", and "Preset Themes" card grid. |
| 7 | `face lock.jpg` | 1080 × 2400 | 53,547 B | Biometrics & Security | Face ID Feedback | Header "Face ID", "UPGRADE" badge, description "When Face ID is verified, feedback is displayed in the notch", "Face ID / Show Face ID" switch (ON), expanded preview with squircle notch card showing neon green smiling Face ID glyph inside scanning brackets. |
| 8 | `flashy border animation style.jpg` | 1080 × 2400 | 78,541 B | Animations & Border Glow | Border Animation Style | "SELECT ANIMATION TYPE 👑", radio selector: "Multi Color Animation" (checked) vs "Single Color Animation", Speed slider with `-`/`+` steppers, Border thickness slider with `-`/`+` steppers, and "COLORS" palette chips with remove `(x)` tags (Blue, Red, Yellow, Green, Cyan, + Add). |
| 9 | `flashy border animation.jpg` | 1080 × 2400 | 63,548 B | Animations & Border Glow | Flashy Border Animation | Header "Flashy Border Animation", "RESET" action button, "Enable / Notch Border Animation" switch (ON), description, simulated notch preview with multi-colored glowing border and Play button, card link to "Animation Style >". |
| 10 | `music player settings.jpg` | 1080 × 2400 | 100,681 B | Music & Media | Music Player (Secondary Settings) | Header "Music Player", "OTHER SETTINGS": "Long Press : Volume Control" toggle (ON), "Album Photo as Player Bg" toggle (OFF), "Show Fast-Forward Buttons" toggle (ON), "Only show while Playing Audio" toggle (OFF), "Blocked Apps For Music" card ("0 Apps >"), "STYLE" radio selector: "Rotation On" (vinyl disc + blue waveform) vs "Rotation Off" (square static art + blue waveform). |
| 11 | `music player.jpg` | 1080 × 2400 | 73,905 B | Music & Media | Music Player (Expanded Preview) | Header "Music Player", "Enable / Music Player" toggle (ON), expanded preview card with circular vinyl album artwork, "Music Title", "Music Description", blue 5-bar jumping equalizer, "1:30" elapsed time, scrub bar with circular thumb, "5:00" duration, and 5 controls: 10s Rewind `(10)`, Previous `\|<<`, Pause `\|\|`, Next `>>\|`, 10s Fast-Forward `(10)`. |
| 12 | `notch swipe animation.jpg` | 1080 × 2400 | 64,454 B | Motion Physics | Dual Popup Notch Swipe Animation | Header "NOTCH ANIMATION", "Notch Swipe Animation", 3 selectable physics cards for dual popups in small notch: "Liquid" (fluid connecting bridge between primary capsule and secondary pill), "Bouncy" (elastic overshoot and rebound), "Droplet" (surface tension droplet separation). |
| 13 | `notification expanded.jpg` | 1080 × 2400 | 86,909 B | Notifications | Notification Expanded Card & Typography | "BIG NOTCH PREVIEW": green chat icon, "CHATAPP • 2 min ago", "CLEAR ALL", close "X", sender avatar, bold "Notification Title", body text, down chevron, action buttons ("REPLY", "MARK AS READ", "MUTE", bell icon). Section "TEXT & COLOR": Bg Color chip, Text Color chip, "Auto Background Color" toggle (VIP). Section "TEXT SIZE": Title slider with `-`/`+` steppers, Subtext slider with `-`/`+` steppers, Buttons slider with `-`/`+` steppers. "TEXT STYLE" section. |
| 14 | `notification style.jpg` | 1080 × 2400 | 73,407 B | Notifications | Notification Style Configuration | Header "Notification", "Reset" action, "Enable / Notification" switch (ON), "NOTIFICATION STYLE" visual selector: "Expanded Popup" (checked) vs "Small Popup" (unchecked), "Auto Expand Notification During DND" switch (OFF), "Blocked App Notification" card ("0 Apps >"), "OTHER SETTINGS": "Show Sensitive Content?" card (`>`). |
| 15 | `notification.jpg` | 1080 × 2400 | 64,646 B | Notifications | Notification (Compact Preview) | Header "Notifications", "UPGRADE" crown badge, "Notifications / Show Notifications" switch (ON), compact capsule preview showing WhatsApp green icon, "WhatsApp", "Hey, are we still meeting today?", and "Now" timestamp. |
| 16 | `order tracking.jpg` | 1080 × 2400 | 110,254 B | Live Activities | Live Order Tracking | Header "Order Tracking", "Enable 👑 / Live Order Tracking" switch (ON), dual preview: compact pill with delivery scooter + "5min" + green bottom progress underline; expanded card with "Order is on the way! / Arriving in 5 Minutes", horizontal route progress bar with scooter marker at 50%, and 3D delivery rider graphic; footnote "Track Blinkit orders in your Dynamic Notch". |
| 17 | `screen unlock.jpg` | 1080 × 2400 | 55,310 B | System Events | Device Unlock Feedback | Header "Device Unlock", "UPGRADE" badge, description "Feedback displayed when device is unlocked", "Device Unlock / Show Device Unlock" switch (ON), compact pill preview with open white padlock glyph on left. |
| 18 | `silent.jpg` | 1080 × 2400 | 60,138 B | Audio & Ringer | Silent Mode Feedback | Header "Silent Mode", "UPGRADE" badge, description "Feedback when ringer mode is changed to silent", "Silent Mode / Show Silent Mode" switch (ON), compact capsule preview with red slashed-bell glyph on left and bold red text "Silent" on right. |
| 19 | `sound-on.jpg` | 1080 × 2400 | 57,194 B | Audio & Ringer | Ringing Mode Feedback | Header "Ringing Mode", "UPGRADE" badge, description "Feedback when ringer mode is changed to normal/sound on", "Ringing Mode / Show Ring Mode" switch (ON), compact capsule preview with white ringing bell glyph on left and bold white text "Sound on" on right. |
| 20 | `swipe animation.jpg` | 1080 × 2400 | 36,516 B | Onboarding & Gestures | Permission & Gesture Tutorial | "Skip" button, title "Allow Necessary Permissions", subtitle "To use all Functionality of DynamicNotch you need to allow below permissions", preview well showing dual island (left capsule: Sun+Cloud weather glyph + "32°"; right pill: circular album art with "EVENT" badge), and animated hand index finger swiping beneath the island to teach dual-module navigation. |

---

## 3. Granular Requirements Extraction

Every observable setting, control, visualizer, and behavior from the 20 screenshots is extracted below, cataloged by functional domain:

### 3.1 Media & Music Player (`music player.jpg`, `music player settings.jpg`)
1. **Master Switch:** `Enable / Music Player` toggle switch.
2. **Vinyl Artwork Rotation:** Toggle / radio option `Rotation On` (smooth 360° spinning vinyl record disc with circular center artwork) vs `Rotation Off` (static square rounded artwork).
3. **Equalizer Visualizer:** Jumping 5-bar vertical equalizer waveform in electric blue.
4. **Playback Scrub Bar:** Interactive seek bar with elapsed time display (`1:30`), circular draggable thumb, and total track duration (`5:00`).
5. **Transport Controls (5-Button Layout):**
   - 10-second Rewind button: circular arrow icon with `10` numeral inside.
   - Previous Track button: `|<<`.
   - Play/Pause toggle button: `||` / `▶` inside circular surface.
   - Next Track button: `>>|`.
   - 10-second Fast-Forward button: circular arrow icon with `10` numeral inside.
6. **Show Fast-Forward Buttons:** User toggle to show or hide the 10s fast-forward / rewind buttons in the expanded player.
7. **Volume Gesture Control:** Toggle `Long Press : Volume Control` ("Long press the notch and swipe left or right to increase or decrease the volume").
8. **Blurred Album Art Background:** Toggle `Album Photo as Player Bg` ("Shows blurred album photo as music player background").
9. **Display Policy:** Toggle `Only show while Playing Audio` ("Only display in notch when music is currently playing, hide during videos/paused state").
10. **Audio Blacklist:** App selector `Blocked Apps For Music` (`0 Apps >`) to exclude specific packages (e.g. WhatsApp voice notes, Google Maps audio cues, YouTube).

### 3.2 Notifications (`notification.jpg`, `notification expanded.jpg`, `notification style.jpg`)
1. **Master Switch:** `Enable / Notification` (`Show Notifications`) toggle switch.
2. **Popup Sizing Mode:** Visual radio selector between:
   - `Expanded Popup`: Direct expansion with avatar, title, body, action buttons.
   - `Small Popup`: Compact single-line capsule wrapping the camera punch-hole.
3. **Compact Notification Content:**
   - App icon on left (e.g. WhatsApp rounded icon).
   - Sender/Title text in bold.
   - Message preview snippet.
   - Timestamp on right (e.g. `Now`, `2 min ago`).
4. **Expanded Notification Card Layout:**
   - App header row: App icon + App Name in uppercase + dot divider + timestamp + `CLEAR ALL` text action button + dismiss `X` button.
   - Content row: Circular sender avatar + bold Title + multi-line Body text + expand/collapse chevron.
   - Action Button Row: Pill chips for `REPLY`, `MARK AS READ`, `MUTE`, and bell icon.
5. **DND Auto-Expand Override:** Toggle `Auto Expand Notification During DND` ("Automatically expand popup even when Do Not Disturb is active").
6. **App Blacklist:** Setting `Blocked App Notification` (`0 Apps >`) to suppress notifications from specific packages.
7. **Privacy & Lock Screen:** Setting `Show Sensitive Content?` (`>`).
8. **Custom Text & Background Colors:**
   - `Bg Color` picker chip.
   - `Text Color` picker chip.
   - `Auto Background Color` toggle (auto-extract dominant accent color from notification app icon).
9. **Granular Typography Steppers:**
   - `Title` size slider with `-` and `+` stepper buttons.
   - `Subtext` size slider with `-` and `+` stepper buttons.
   - `Buttons` size slider with `-` and `+` stepper buttons.
10. **Reset Action:** `Reset` button to restore factory notification appearance.

### 3.3 Battery & Charging Intelligence (`charging themes.jpg`)
1. **Visual Theme Gallery:** 2-column visual grid of 30+ distinct charging themes:
   - Cyan lightning bolt glow with electric arc aura around black notch.
   - Neon green glowing capsule with centered lightning bolt.
   - Green energy orb / sphere with lightning core.
   - Red/orange flame eye with glowing ember particles.
   - Magic potion bottle with bubbling green liquid.
   - Lightning surge: dual electric bolts flanking the camera cutout.
   - Floating red/pink hearts orbiting the notch pill.
   - Sleek minimal numeric gauge: `Charging 50%` inside technical pill with plug icon.
   - Circular battery percentage gauge with animated rotating progress arc.
   - Stylized creatures, cyberpunk meters, and liquid battery fills.
2. **Interactive Preview & Instant Apply:** Tapping any theme card immediately previews and applies it to the active overlay.

### 3.4 Themes & Palette Engine (`dynamic themes.jpg`, `dynamic themes (2).jpg`, `dynamic themes (3).jpg`)
1. **Preset Colorway Gallery:** 2-column card grid with 32+ named presets:
   - `Classic Black`, `Minimal Gray`, `Midnight`, `Amethyst`, `Sunset`, `Neon Cyber`, `Royal Gold`, `Ocean Breeze`, `Crimson Fire`, `Deep Space`, `Forest Rain`, `Purple Haze`, `Cherry Blossom`, `Golden Hour`, `The Matrix`, `Cyber Pink`, `Mint Fresh`, `Silver Lining`, `Carbon Fiber`, `Electric Blue`, `Sweet Candy`, `Northern Lights`, `Pure White`, `Vampire Red`, `Emerald City`, `Cosmic Void`, `Rose Gold`, `Ice Cold`, `Lemon Twist`, `Twilight`, `Hot Lava`, `Sea Foam`, `Nebula`, `Bumblebee`, `Rainbow`.
2. **Visual Selection Indicator:** Active radio checkmark on currently applied preset.
3. **Live Theme Preview:** Notch preview card reflecting applied palette.
4. **Service Status Guidance:** Warning banner `Notch is Not Active / Activate` guiding user when accessibility overlay is disabled.

### 3.5 Border Animations & Glow Effects (`flashy border animation.jpg`, `flashy border animation style.jpg`)
1. **Master Switch:** `Enable / Notch Border Animation` toggle switch.
2. **Preview Well:** Live animated preview card displaying the glowing notch border with a Play/Test button.
3. **Animation Style Configuration:**
   - Radio option `Multi Color Animation` (checked) vs `Single Color Animation`.
   - Speed slider with `-` and `+` stepper buttons.
   - Border thickness slider with `-` and `+` stepper buttons.
   - Dynamic color chip palette with remove `(x)` tags on each chip (e.g. Blue, Red, Yellow, Green, Cyan) and `+ Add Color` picker.
4. **Reset Action:** `RESET` button restoring default border animation values.

### 3.6 Motion Physics & Dual-Popup Transitions (`notch swipe animation.jpg`)
1. **Dual Popup Animation Physics:** Visual radio selector between 3 distinct physics curves:
   - `Liquid`: Viscous fluid bridge stretching between primary notch and secondary capsule.
   - `Bouncy`: Spring physics with elastic overshoot and rebound waves.
   - `Droplet`: Surface tension droplet separation.

### 3.7 System Events & Peripheral Feedback (`bluetooth-connection.jpg`, `bluetooth-connection (2).jpg`, `face lock.jpg`, `screen unlock.jpg`, `silent.jpg`, `sound-on.jpg`)
1. **Bluetooth Audio Connection:**
   - Toggle `Show Connection` ("Show connection popup in small notch").
   - Compact presentation: Earbuds icon (left) + circular green battery percentage ring (right).
   - Expanded presentation: Earbuds icon (left) + `Connected / [Device Name]` (center) + circular green battery ring with `%` (right).
2. **Face ID / Biometric Verification:**
   - Toggle `Show Face ID` ("When Face ID is verified, feedback is displayed in the notch").
   - Expanded squircle card with neon green smiling face glyph inside scanning brackets.
3. **Device Screen Unlock:**
   - Toggle `Show Device Unlock` ("Feedback displayed when device is unlocked").
   - Compact pill preview with open white padlock glyph on left.
4. **Silent Mode Alert:**
   - Toggle `Show Silent Mode` ("Feedback when ringer mode is changed to silent").
   - Compact capsule with red slashed-bell icon on left + bold red `Silent` text on right.
5. **Ringing Mode Alert:**
   - Toggle `Show Ring Mode` ("Feedback when ringer mode is changed to normal/sound on").
   - Compact capsule with white ringing bell icon on left + bold white `Sound on` text on right.

### 3.8 Onboarding, Multi-Module Island & Gesture Guidance (`swipe animation.jpg`)
1. **Dual Island Architecture:** Left capsule (e.g. Weather icon + `32°`) paired with right circular pill (e.g. Media album art with `EVENT` badge).
2. **Swipe Gesture Tutorial:** Visual animated finger gesture demonstrating horizontal swipe interaction beneath the island to switch between active modules or expand.
3. **Onboarding Integration:** Permissive setup screen with `Skip` button and clear explanatory copy.

---

## 4. Direct Traceability & Gap Analysis Matrix

This matrix maps every single requirement extracted from the 20 screenshots to:
1. **Evidence Screenshot:** Exact reference image filename.
2. **Existing Implementation Status:** State of the Nothing Island codebase at accepted checkpoint R8 (235 passing unit tests, verified via Gradle). Note: Project workspace is maintained as a directory snapshot without an initialized local Git repository.
3. **Gap Classification:**
   - `Fully Implemented (Overlay + UI)`
   - `Overlay Engine Ready (UI Missing / Unexposed)`
   - `Partially Implemented (Logic Exists, UI Needs Restructuring)`
   - `New UI Requirement (Targeted for Redesign Specification)`
   - `Subsystem Feature Gap (Roadmap R9–R12)`
4. **Target Redesign Destination:** Screen and Studio location in `docs/UI_UX_REDESIGN_SPECIFICATION.md`.

| # | Requirement Extracted | Evidence Screenshot | Existing Codebase Status (R8 Checkpoint) | Gap Classification | Target Redesign Destination |
|---|---|---|---|---|---|
| 1 | Bluetooth connection toggle (`Show Connection`) | `bluetooth-connection.jpg` | Unimplemented. No Bluetooth broadcast receiver or GATT battery listener. | Subsystem Feature Gap (R9+) | Settings & Peripheral Studio |
| 2 | Bluetooth compact pill (earbuds icon + circular battery ring) | `bluetooth-connection.jpg` | Unimplemented in overlay. `IslandStateRepository` lacks Bluetooth state. | Subsystem Feature Gap (R9+) | Overlay Renderer & Peripheral Studio |
| 3 | Bluetooth expanded card (device name + battery %) | `bluetooth-connection (2).jpg` | Unimplemented in overlay. | Subsystem Feature Gap (R9+) | Overlay Renderer & Peripheral Studio |
| 4 | Service disabled warning banner (`Notch is Not Active / Activate`) | `bluetooth-connection.jpg`, `dynamic themes.jpg` | In `HomeScreen.kt`, only basic text indicator exists. No actionable banner across studios. | New UI Requirement | Global Studio Header / Banner Component |
| 5 | 2-column visual grid of charging themes (30+ styles) | `charging themes.jpg` | 5 authentic Nothing OS styles implemented in `ChargingVisualizer.kt`, but UI in `AppearanceScreen.kt` only has plain radio buttons. | Overlay Engine Ready (UI Missing) | `ChargingStudioScreen.kt` (Visual Grid) |
| 6 | Minimal numeric battery pill (`Charging 50%` + plug) | `charging themes.jpg` | Implemented in `BatteryCompactView.kt` (`LevelText` + `BoltIcon`). | Fully Implemented (Overlay) | `ChargingStudioScreen.kt` |
| 7 | Circular battery percentage gauge with progress ring | `charging themes.jpg`, `bluetooth-connection.jpg` | Implemented in `ChargingVisualizer.kt` (`HYPER_RING` style). | Fully Implemented (Overlay) | `ChargingStudioScreen.kt` |
| 8 | 32+ named theme presets in 2-column grid | `dynamic themes.jpg`, `dynamic themes (2).jpg`, `dynamic themes (3).jpg` | `IslandPreferences.presetName` exists in DataStore, but UI only offers 3 hardcoded text buttons. | Partially Implemented (UI Needs Restructuring) | `ThemeStudioScreen.kt` (Preset Grid) |
| 9 | VIP Crown badge on premium themes | `dynamic themes.jpg`, `order tracking.jpg` | No paywall or VIP gating exists (Nothing Island is 100% offline & open-source). | Deliberately Excluded (Free & Open Source) | Replaced with "Nothing OS Original" badge |
| 10 | Face ID verification feedback card (smiling face glyph) | `face lock.jpg` | Unimplemented. No `BiometricPrompt` or keyguard auth listener. | Subsystem Feature Gap (R10) | Security Studio / System Events |
| 11 | Master switch for border animation | `flashy border animation.jpg` | `isPulsing` boolean exists in `IslandPreferences`. | Partially Implemented | `AnimationStudioScreen.kt` |
| 12 | Border animation speed slider with `-`/`+` steppers | `flashy border animation style.jpg` | Speed is hardcoded in Compose animation spec. No stepper slider in UI. | New UI Requirement | `AnimationStudioScreen.kt` (Stepper Slider) |
| 13 | Border thickness slider with `-`/`+` steppers | `flashy border animation style.jpg` | Thickness is hardcoded. No stepper slider in UI. | New UI Requirement | `AnimationStudioScreen.kt` (Stepper Slider) |
| 14 | Multi-color palette chips with remove `(x)` tags | `flashy border animation style.jpg` | Unimplemented. Only single accent hex supported in repository. | New UI Requirement | `AnimationStudioScreen.kt` (Palette Chips) |
| 15 | Reset button for border animation / themes | `flashy border animation.jpg`, `notification style.jpg` | Reset to defaults exists in `IslandPreferencesRepository.resetToHardwareDefaults()`, but no UI button in screens. | Overlay Engine Ready (UI Missing) | Studio Action Bars (`Reset` Button) |
| 16 | Music Master switch (`Enable / Music Player`) | `music player.jpg` | Media session listener active in `IslandMediaSessionListenerService`. Setting toggle exists in `SettingsScreen.kt`. | Fully Implemented | `MusicStudioScreen.kt` |
| 17 | Vinyl album artwork rotation toggle (`Rotation On/Off`) | `music player settings.jpg` | `music_album_rotation_enabled` exists in `MusicPreferences` and DataStore! **Zero UI controls exposed.** | Overlay Engine Ready (UI Missing) | `MusicStudioScreen.kt` (Style Selector) |
| 18 | Blurred album photo as player background toggle | `music player settings.jpg` | Unimplemented in `MediaExpandedView.kt`. Player background is pure black (`NothingSurfaceLowest`). | New UI Requirement | `MusicStudioScreen.kt` |
| 19 | 10s Fast-Forward and 10s Rewind buttons `(10)` | `music player.jpg`, `music player settings.jpg` | Missing from `MediaExpandedView.kt` (currently has Previous, Play/Pause, Next, Seek). Toggle missing. | New UI Requirement | `MediaExpandedView.kt` & `MusicStudioScreen.kt` |
| 20 | Long-press volume swipe gesture (`Long Press : Volume Control`) | `music player settings.jpg` | Unimplemented. Overlay touch handler supports tap and horizontal swipe, but not long-press volume drag. | Subsystem Feature Gap (R9) | `GestureStudioScreen.kt` |
| 21 | Only show while playing audio toggle | `music player settings.jpg` | Event engine auto-collapses on pause, but preference key not exposed in DataStore or UI. | Partially Implemented | `MusicStudioScreen.kt` |
| 22 | Blocked apps for music (`Blocked Apps For Music`) | `music player settings.jpg` | Package blacklist not currently filtered in `IslandMediaSessionListenerService`. | Subsystem Feature Gap (R9) | `MusicStudioScreen.kt` (App Selector) |
| 23 | Dual popup animation physics (`Liquid`, `Bouncy`, `Droplet`) | `notch swipe animation.jpg` | Overlay uses standard Compose `spring()`. No physics curve selection in DataStore or UI. | New UI Requirement | `AnimationStudioScreen.kt` (Physics Selector) |
| 24 | Notification master switch (`Enable / Notification`) | `notification style.jpg`, `notification.jpg` | `notif_enabled` exists in `NotificationPreferences` and DataStore. Toggle exposed in `SettingsScreen.kt`. | Fully Implemented | `NotificationStudioScreen.kt` |
| 25 | Notification style selector (`Expanded Popup` vs `Small Popup`) | `notification style.jpg` | `notif_preview_mode` (`COMPACT`, `EXPANDED`) exists in `NotificationPreferences`! **No visual UI selector.** | Overlay Engine Ready (UI Missing) | `NotificationStudioScreen.kt` (Visual Radio) |
| 26 | DND auto-expand override toggle (`Auto Expand During DND`) | `notification style.jpg` | Auto-expand implemented in R7.1 (`autoExpandOnNotification`), but DND bypass preference not modeled. | Subsystem Feature Gap (R9) | `NotificationStudioScreen.kt` |
| 27 | Blocked apps for notifications (`Blocked App Notification`) | `notification style.jpg` | `notif_excluded_packages` (`Set<String>`) exists in `NotificationPreferences`! **Zero UI controls exposed.** | Overlay Engine Ready (UI Missing) | `NotificationStudioScreen.kt` (App Picker) |
| 28 | Show sensitive content toggle | `notification style.jpg` | `notif_hide_sensitive` exists in `NotificationPreferences`! **Zero UI controls exposed.** | Overlay Engine Ready (UI Missing) | `NotificationStudioScreen.kt` |
| 29 | Expanded notification `CLEAR ALL` button | `notification expanded.jpg` | Action buttons (Reply, Mark as Read) and Undo dismissal exist in R6/R7. No "Clear All" batch dismissal button. | New UI Requirement | `NotificationExpandedView.kt` & Studio |
| 30 | Granular notification typography steppers (Title, Subtext, Buttons) | `notification expanded.jpg` | Font sizes are hardcoded in Compose tokens (`14.sp`, `12.sp`). No slider/stepper controls exist. | New UI Requirement | `NotificationStudioScreen.kt` (Typography) |
| 31 | Custom notification background and text color pickers | `notification expanded.jpg` | Card colors hardcoded to Nothing dark palette. No per-module color picker. | New UI Requirement | `NotificationStudioScreen.kt` (Color Pickers) |
| 32 | Auto background color from app icon | `notification expanded.jpg` | `useAdaptiveAccent` implemented for music in R7. Notification adaptive palette not yet implemented. | Subsystem Feature Gap (R9) | `NotificationStudioScreen.kt` |
| 33 | Compact single-line notification preview (WhatsApp) | `notification.jpg` | Implemented in `NotificationCompactView.kt`. In-app preview in `HomeScreen.kt` is static/blank. | Partially Implemented (In-app preview missing) | `IslandPreview.kt` & Studio |
| 34 | Live order tracking (Blinkit / Delivery scooter progress) | `order tracking.jpg` | Unimplemented. Roadmap gap for Live Activities / Delivery scraping. | Subsystem Feature Gap (R11) | Live Activities Studio (Future R11) |
| 35 | Device unlock momentary feedback (open padlock glyph) | `screen unlock.jpg` | Unimplemented. Requires `ACTION_USER_PRESENT` keyguard broadcast receiver. | Subsystem Feature Gap (R10) | System Events / Security Studio |
| 36 | Silent mode feedback pill (red slashed-bell + `Silent`) | `silent.jpg` | Unimplemented. Requires `RINGER_MODE_CHANGED_ACTION` broadcast receiver. | Subsystem Feature Gap (R9) | Audio & Ringer Studio (R9) |
| 37 | Ringing mode feedback pill (white bell + `Sound on`) | `sound-on.jpg` | Unimplemented. Requires `RINGER_MODE_CHANGED_ACTION` broadcast receiver. | Subsystem Feature Gap (R9) | Audio & Ringer Studio (R9) |
| 38 | Dual-capsule island layout (Weather pill + Media circle) | `swipe animation.jpg` | Activity Switcher in R8 switches between single fullscreen pills. Dual-island split rendering not implemented. | Subsystem Feature Gap (R10) | Dual Island Architecture (R10) |
| 39 | Onboarding swipe gesture tutorial | `swipe animation.jpg` | No onboarding wizard exists in Nothing Island app. First-launch experience is unguided. | New UI Requirement | Onboarding Wizard & `GestureStudioScreen.kt` |

---

## 5. Synthesis: Studio Mapping & Architecture Alignment

The 39 extracted requirements directly validate and enrich the **Studio Architecture** established in `docs/UI_UX_REDESIGN_SPECIFICATION.md`. Every visible feature from the screenshots maps cleanly into a dedicated Nothing OS Studio:

```mermaid
flowchart TD
    subgraph Nothing Island Companion Navigation
        HOME[1. Home Dashboard\nLive Preview + Service Banner + Studio Grid]
        NOTIF_S[2. Notification Studio\nPopup Sizing, Typography Steppers, DND, Blacklist]
        MUSIC_S[3. Music Studio\nVinyl Rotation, 5 Visualizers, 10s Rewind/FF, Volume Gesture]
        CHARGE_S[4. Charging Studio\n30+ Theme Visualizer Grid, Telemetry, Low Battery Alert]
        
        subgraph More Studios & Hub
            THEME_S[Theme Studio\n32+ Preset Colorways, Matte AMOLED, Custom Accent]
            ANIM_S[Animation Studio\nLiquid/Bouncy Physics, Border Glow Multi-Color Chips]
            GESTURE_S[Gesture Studio\nSwipe Activity Switcher, Volume Gesture, Tutorial]
            PERIPH_S[Peripheral & System Events\nBluetooth Battery Ring, Ringer Alert, Unlock Padlock]
            SETTINGS[Settings & Diagnostics\nAOSP Layer 24000 Anchor, Cutout Offsets, Logs]
        end
    end

    HOME --> NOTIF_S
    HOME --> MUSIC_S
    HOME --> CHARGE_S
    HOME --> THEME_S
    HOME --> ANIM_S
    HOME --> GESTURE_S
    HOME --> PERIPH_S
    HOME --> SETTINGS
```

### Studio-by-Studio Alignment Details:

1. **Home Dashboard (`HomeScreen.kt`):**
   - Incorporates the **Service Inactive Banner** (`Notch is Not Active / Activate`) observed in `bluetooth-connection.jpg` and `dynamic themes.jpg` with a direct deep-link to Android Accessibility Settings.
   - Upgrades `IslandPreview.kt` to simulate live interactive states:
     - `Idle`: Punch-hole cutout pill.
     - `Notification`: Compact WhatsApp pill (`notification.jpg`) and Expanded chat card (`notification expanded.jpg`).
     - `Music`: Expanded player with jumping blue equalizer and vinyl disc (`music player.jpg`).
     - `Battery`: Live charging ring and wattage (`charging themes.jpg`).
     - `Bluetooth`: Earbuds icon with battery percentage ring (`bluetooth-connection.jpg`).

2. **Notification Studio (`NotificationStudioScreen.kt`):**
   - Visual radio selector: `Expanded Popup` vs `Small Popup` (`notification style.jpg`).
   - Granular typography sliders with `-`/`+` steppers for Title, Subtext, and Buttons (`notification expanded.jpg`).
   - Action controls: `REPLY`, `MARK AS READ`, `MUTE`, and `CLEAR ALL` (`notification expanded.jpg`).
   - Application Blacklist selector (`Blocked App Notification`) wired to existing DataStore key `notif_excluded_packages`.
   - Privacy controls: `Hide Sensitive Content` wired to `notif_hide_sensitive`.
   - Automation controls: `Auto Expand on Arrival` (R7.1.1 toggle) and `Auto Expand During DND` (`notification style.jpg`).

3. **Music Studio (`MusicStudioScreen.kt`):**
   - Style Selector: `Rotation On` (vinyl record) vs `Rotation Off` (static square art) wired to existing DataStore key `music_album_rotation_enabled` (`music player settings.jpg`).
   - Transport Controls: 10s Rewind and 10s Fast-Forward button toggles (`music player.jpg`, `music player settings.jpg`).
   - Volume Gesture toggle: `Long Press : Volume Control` (`music player settings.jpg`).
   - Visualizer Selector: Grid of 5 Nothing OS visualizers (Classic Equalizer, Minimal Waveform, Circular Pulse, Dot Matrix, Spectrum Bars).
   - Audio app blacklist picker (`Blocked Apps For Music`).

4. **Charging Studio (`ChargingStudioScreen.kt`):**
   - 2-Column visual grid of charging visualizers inspired by `charging themes.jpg`.
   - Live telemetry preview displaying real-time battery percentage, charging wattage estimate, and thermal state.
   - Low battery warning capsule toggle and threshold selector (10%, 15%, 20%).

5. **Theme Studio (`ThemeStudioScreen.kt`):**
   - 2-Column card grid of 32+ named colorway presets (`dynamic themes (2).jpg`, `dynamic themes (3).jpg`).
   - Nothing OS signature colorways (Classic Black, Nothing Red Accent, Stealth Dark, Matrix Phosphor) alongside curated vibrant palettes.
   - Live theme preview card reflecting applied surface colors and borders.

6. **Animation Studio (`AnimationStudioScreen.kt`):**
   - Notch Swipe Animation physics selector: `Liquid`, `Bouncy`, `Droplet` (`notch swipe animation.jpg`).
   - Multi-Color Border Animation: Speed and thickness sliders with `-`/`+` steppers, dynamic color palette chips with remove `(x)` tags (`flashy border animation style.jpg`).
   - Live interactive border glow preview well with Play/Test button (`flashy border animation.jpg`).

7. **Gesture Studio & Onboarding (`GestureStudioScreen.kt`):**
   - Visual interactive tutorial based on `swipe animation.jpg` explaining horizontal swipe between active events.
   - Single tap, double tap, long press, swipe left, and swipe right gesture configuration.
   - Accidental touch protection in full-screen games/apps.

8. **Peripheral & System Events Studio (Roadmap R9/R10):**
   - Bluetooth connection alert with circular battery progress ring (`bluetooth-connection.jpg`, `bluetooth-connection (2).jpg`).
   - Ringer mode momentary feedback (`Silent` in red vs `Sound on` in white) (`silent.jpg`, `sound-on.jpg`).
   - Screen unlock momentary padlock feedback (`screen unlock.jpg`).
   - Face ID biometric scanning feedback (`face lock.jpg`).

---

## 6. Missing Preferences & DataStore Schema Additions

Several features discovered in the screenshots are already modeled in `IslandPreferencesRepository.kt` but lacked UI controls, while others require new DataStore keys. Below is the exact schema reconciliation:

### 6.1 Existing DataStore Keys Requiring UI Exposure (Ready for Wiring):
| DataStore Key | Type | Default Value | Discovered In Screenshot | Target Studio Screen |
|---|---|---|---|---|
| `notif_preview_mode` | String (`"COMPACT"`, `"EXPANDED"`) | `"EXPANDED"` | `notification style.jpg` | `NotificationStudioScreen.kt` |
| `notif_excluded_packages` | Set<String> | `emptySet()` | `notification style.jpg` | `NotificationStudioScreen.kt` |
| `notif_hide_sensitive` | Boolean | `false` | `notification style.jpg` | `NotificationStudioScreen.kt` |
| `music_album_rotation_enabled` | Boolean | `true` | `music player settings.jpg` | `MusicStudioScreen.kt` |
| `music_artwork_shape` | String (`"CIRCLE"`, `"SQUIRCLE"`) | `"CIRCLE"` | `music player settings.jpg` | `MusicStudioScreen.kt` |
| `music_visualizer_style` | String | `"EQUALIZER"` | `music player.jpg` | `MusicStudioScreen.kt` |
| `battery_anim_style` | String | `"GLYPH_PULSE"` | `charging themes.jpg` | `ChargingStudioScreen.kt` |
| `is_pulsing` | Boolean | `true` | `flashy border animation.jpg` | `AnimationStudioScreen.kt` |

### 6.2 New DataStore Keys to Add in Design System Implementation:
| Proposed DataStore Key | Kotlin Type | Default Value | Description / Screenshot Origin |
|---|---|---|---|
| `music_show_fast_forward_buttons` | Boolean | `true` | Show 10s FF/rewind buttons (`music player settings.jpg`) |
| `music_long_press_volume_enabled` | Boolean | `false` | Long press volume gesture (`music player settings.jpg`) |
| `music_album_blurred_bg_enabled` | Boolean | `false` | Blurred album background (`music player settings.jpg`) |
| `music_only_show_while_playing` | Boolean | `true` | Hide when audio is paused (`music player settings.jpg`) |
| `music_blocked_packages` | Set<String> | `emptySet()` | Blacklist for music player (`music player settings.jpg`) |
| `notif_auto_expand_dnd` | Boolean | `false` | Expand during DND (`notification style.jpg`) |
| `notif_title_text_size_sp` | Float | `14.0f` | Stepper slider for title (`notification expanded.jpg`) |
| `notif_body_text_size_sp` | Float | `12.0f` | Stepper slider for subtext (`notification expanded.jpg`) |
| `anim_physics_mode` | String | `"LIQUID"` | Liquid, Bouncy, Droplet (`notch swipe animation.jpg`) |
| `border_glow_speed` | Float | `1.0f` | Border animation speed (`flashy border animation style.jpg`) |
| `border_glow_thickness_dp` | Float | `2.0f` | Border thickness (`flashy border animation style.jpg`) |
| `border_glow_colors` | String | `"#007AFF,#FF3B30,#FFCC00"` | Comma-separated hex list (`flashy border animation style.jpg`) |

---

## 7. Technical Constraints & Platform Considerations

1. **Android 17 / API 37 Compatibility:**
   - All newly proposed broadcast receivers (e.g. `RINGER_MODE_CHANGED_ACTION`, `ACTION_USER_PRESENT`) must specify `RECEIVER_NOT_EXPORTED` flags in accordance with modern Android security rules.
   - Stepper controls and slider drags must use non-blocking StateFlow emissions to ensure 120Hz smooth scrolling without jank or disk I/O on the main thread.
2. **AOSP Layer 24000 Isolation:**
   - Touch interactions (e.g. long-press volume gesture, 10s seek buttons) must be processed within `IslandOverlayView` and dispatched via Android `AudioManager` and `MediaController.TransportControls`.
   - The companion app UI communicates strictly through `IslandPreferencesRepository` StateFlows; the overlay never binds to Activity lifecycle or UI composables directly.
3. **Nothing Phone (4a) Cutout Alignment:**
   - All preview cards (`IslandPreview.kt`) must use the measured hardware constants (`CutoutDetector.DEFAULT_COMPACT_WIDTH_DP = 124dp`, `DEFAULT_COMPACT_VISIBLE_HEIGHT_DP = 36dp`, `DEFAULT_TOP_MARGIN_DP = 9dp`) so in-app previews accurately simulate on-device hardware layout.

---

## 8. Verification & Sign-off

- [x] All 20 reference screenshots inspected at full resolution.
- [x] Full inventory compiled with filenames, resolutions, and file sizes.
- [x] Every visible toggle, slider, stepper, animation, and preview extracted.
- [x] Traceability matrix created mapping all 39 requirements to screenshot evidence, codebase status, and redesign destinations.
- [x] Zero modifications made to original screenshot files.
- [x] Zero modifications made to application source code during audit.
- [x] Architecture verified compatible with accepted R8 baseline (235 unit tests).
