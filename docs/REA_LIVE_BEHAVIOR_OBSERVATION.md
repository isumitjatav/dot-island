# REA Live Behavior Observation — Reference Apps

**Date:** 2026-11-19
**Method:** REA MCP ADB tools (`list_adb_devices`, `list_adb_packages`,
`inspect_adb_package`, `inspect_adb_window`) plus direct `adb screencap` on the
connected Nothing Phone (4a) (`A069` / `FroggerIND`, serial `00294661P008434`).

## Scope and boundary

This document records **observed behavior** only — screens, component names,
versions, permission posture and navigation structure. It deliberately does
**not** copy decompiled source or proprietary assets from the reference apps.
Any feature implemented in Nothing Island is an original reimplementation of
the observed behavior.

## Reference A — Dynamic Notch

- Package: `com.dynamicisland.notchscreenview`
- Version: **6.61** (code 61), installed from Google Play
- Requested permissions: **50**
- Observed components (from `dumpsys package`):
  - `SplashActivity` (MAIN), `MainActivity`, `BeginPermissionActivity`
  - `PremiumActivity`, `ReferToGetPremiumActivity`, `FirebaseEmptyActivity`
  - `OuterDialogTransparentActivity` (PROCESS_TEXT)
  - `service.CallEndSummaryScreen` (PHONE_STATE)
  - `callendservices.PhoneStateReceiver` (PHONE_STATE, NEW_OUTGOING_CALL)
  - `service.ChargingService$PowerConnectionReceiver`
    (POWER_CONNECTED / DISCONNECTED / BATTERY_CHANGED)

### Observed launch flow
1. Splash: "Dynamic Notch Bar — Smart Alerts | Quick Controls | Seamless
   Experience", "Loved by 8.1M+ Users", V6.61.
2. Full-screen **Yandex interstitial ad** ("Gem Slide Puzzle") after splash.
3. `BeginPermissionActivity`: "Allow Necessary Permissions" — Accessibility
   and Notification toggles shown ON by default; Call and Battery
   Optimization OFF; "We protect your data / No data is collected or shared"
   badge. A "Skip" button is present.
4. `MainActivity`: header "Dynamic Notch", hamburger menu, gift/AD icon, help
   icon. Promo carousel ("New Charging — Small Notch Charging Animations").
   Enable toggle "Dynamic Notch Bar" (OFF). Sections: Display, "Hide System
   (Double) Notification", Quick Controls (OFF). Multiple banner/native ads
   (Rupee112 loan, TopOn banner at bottom).

### Behavioral takeaways for Nothing Island
- Accessibility + Notification access are the core permission pair.
- Call handling is notification/`PHONE_STATE` based, not a dialer replacement.
- Charging state uses a sticky `ACTION_BATTERY_CHANGED` receiver.
- Heavy ad monetization is the app's business model; our open-source build
  should be ad-free.

## Reference B — Dynamic Island Pro

- Package: `tr.com.eywin.dynamic.island.notch`
- Version: **1.9.2** (code 21), installed from Google Play
- Requested permissions: **28**
- Observed components: `SplashActivity`, `MainActivity`, Firebase Auth
  activities, `service.RestartServiceReceiver` (BOOT_COMPLETED,
  MY_PACKAGE_REPLACED, QUICKBOOT_POWERON, RESTART_OVERLAY_SERVICE),
  battery-low/okay and power-connect receivers.

### Observed launch flow
1. Splash: black background, a **connected Bluetooth device pill** —
   "Connected / AirPods Pro" with a **green 75% ring** — above "Dynamic
   Island PRO — Bring your notch to life", with a loading bar.
2. **Premium paywall** ("Unlock Premium") with an explicit comparison table:
   - Remove all ads — PREMIUM only
   - Unlock AI assistant — PREMIUM only
   - Premium-only mascots — PREMIUM only
   - Unlimited customization — PREMIUM only
   - Pricing: "First 3 days free, then ₹400.00/week", renews automatically.
3. After closing paywall, `MainActivity`: "Dynamic Island PRO" header with
   UPGRADE button, "Setup Required" banner, promo card, "Notch is Not Active"
   card with "Complete Setup", a floating mascot button (robot with gradient
   glow), bottom tabs: Home / (lightning) / palette / settings.

### Behavioral takeaways for Nothing Island
- The four premium pillars are exactly: ad removal, AI assistant, mascots,
  unlimited customization. Our open-source app can offer all four for free.
- The Bluetooth earbud pill with battery ring is a highlighted visual.
- A floating mascot launcher is a distinctive navigation affordance.
- Service survivability relies on a boot/package-replaced restart receiver.

## Evidence IDs (REA session)
- Device list: `ev_6caa39e270ffea88429cc679a22ebdeba4fbbbd6e39412cd15d28bf944a236ca`
- Ref A package: `ev_6067377c7595f9458e4d20a326916b97cf7aea3bc16777ac4abd507de96f0f00`
- Ref A APK inventory: `ev_d7776d8b33ed24f1e77275d6de79c7b0094200adc2c9906d5e98488871f510ff`
- Ref A APK sha256: `1bbf152ffcd286ff51e03f793b3aa984ada8a7abb87d0167153818d5a8d2c022`
- Screenshots: `refA_live.png`, `refA_ui.png`, `refA_home.png`,
  `refB_home.png`, `refB_main.png` (temp directory)

## Limitations
- Deep method-level decompilation was **not** performed: no JADX provider
  configured. Component names came from `dumpsys package`, not source.
- Screen captures are point-in-time observations; animations and timings were
  not measured frame-by-frame.
- The apps were launched from a cold state; some observed screens (ads,
  paywall) depend on account/network state.
