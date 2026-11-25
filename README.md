# Dot Island

**Dynamic Capsule, Live Activities & Reactive Pixel Companions for Android**

[![Live Landing Page](https://img.shields.io/badge/Landing_Page-Live-d71921?style=for-the-badge)](https://sumitsinghjatav64-creator.github.io/nothing-island/)
[![Latest APK](https://img.shields.io/badge/Download-Latest_APK-111111?style=for-the-badge&logo=android)](https://github.com/sumitsinghjatav64-creator/nothing-island/releases/latest)
[![Privacy Policy](https://img.shields.io/badge/Privacy-100%25_On--Device-1c1c1e?style=for-the-badge)](https://sumitsinghjatav64-creator.github.io/nothing-island/privacy.html)

---

## Core Features

- **Universal Camera Cutout Calibration (`124dp × 36dp` Capsule)**  
  Reads hardware `DisplayCutout` insets automatically and provides fine X/Y offset calibration with centered, unclipped spring-physics expansion around center or corner punch-holes.
- **3-State Call UI & Loudspeaker Routing**  
  Seamlessly switches across compact active call pill, incoming answer/decline controls, and connected call cards with Android 12+ loudspeaker routing and duplicate system heads-up suppression.
- **Live Music & Charging Pills**  
  Interruptible spring-animated media player and compact playback pill, audio visualizers, smart 90-second time-decayed notification tap priority, and charging telemetry cards.
- **Interactive Compose `Canvas` Mascots (`DotCat`, `GlyphOrb`, `TapeBuddy`)**  
  Lightweight reactive pixel companions configurable via `MascotStudioScreen` inside **Studios**.
- **Bluetooth Peripherals Tracker**  
  Detects connected Bluetooth earbuds/audio devices and battery status to display peripheral connection capsules.
- **Game-Mode Auto-Hide**  
  Automatically hides the floating capsule when a `CATEGORY_GAME` app or landscape immersive fullscreen session enters the foreground and restores it immediately on exit.

---

## Tech Stack & Architecture

- **Language & UI:** Kotlin, Jetpack Compose (BOM `2024.12.01`), Material 3, pure AMOLED dot-matrix styling (`#000000` / `#111111` / `#FFFFFF` / `#D71921`), `minSdk 29` (Android 10+), `targetSdk 36`
- **Overlay Windowing:** `IslandOverlayService` (`WindowManager` centered top-cutout overlay with outside-bounds touch passthrough) + optional `IslandAccessibilityService` overlay anchor and `GLOBAL_ACTION_TAKE_SCREENSHOT` gesture support
- **System Integrations:** `IslandNotificationListenerService` for notification filtering, shade sync, and `MediaSession` tracking; Jetpack DataStore for local preferences
- **Automated Verification:** **358** JVM unit tests (`testDebugUnitTest`) + **11** on-device instrumentation tests (`connectedDebugAndroidTest`)

---

## 3-Step Installation & Setup

1. **Download APK:** Grab the signed `DotIsland-v1.1.0.apk` from the [Latest Release](https://github.com/sumitsinghjatav64-creator/nothing-island/releases/latest).
2. **Grant Core Permissions:** Allow installation from unknown sources on your Android device, then open **Dot Island** and enable **Display over other apps** and **Notification access** *(optional: enable Accessibility in System Settings for the global screenshot gesture)*.
3. **Calibrate & Start:** Open **Studios → Geometry Calibration** to align the capsule around your camera punch-hole, choose your theme or mascot, and tap **Start Dot Island**.

---

## Privacy & Local-Only Processing

Dot Island processes notifications, media metadata, call state, Bluetooth device state, and battery telemetry **100% locally on your device in memory**. It contains zero ads, zero analytics SDKs, and transmits zero user data off-device. See the full [Privacy Policy](https://sumitsinghjatav64-creator.github.io/nothing-island/privacy.html).
