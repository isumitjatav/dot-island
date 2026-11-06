# NOTHING ISLAND — SPRINT 2 IMPLEMENTATION PLAN
## System Events Subsystem Architecture & Execution Specification

**Project:** Nothing Island  
**Repository:** `C:\Users\User\.gemini\antigravity\scratch\nothing-island`  
**Target Hardware:** Nothing Phone (4a) — Model A069 (`Frogger`), Android 17 / API 37  
**Baseline Commit:** `bd859c9` (Sprint 1.1 Hardened)  
**Package:** `com.nothing.island.ai`  
**Status:** Architectural Plan (DO NOT IMPLEMENT IN SPRINT 1.1)  

---

### 1. Executive Mission & Goals

Sprint 2 expands Nothing Island from media, battery, and notification tracking into comprehensive **System Hardware & Status Telemetry**. It introduces momentary, non-intrusive feedback capsules for everyday hardware state transitions:
1. **Silent & Ring Mode Capsules** (`REQ-36`, `REQ-37` / `NL-REQ-33`)
2. **Device Unlock Feedback** (`REQ-35`, `REQ-10` / `screen unlock.jpg`)
3. **Flashlight / Torch Feedback** (`REQ-60` / `NL-REQ-33a`)
4. **Wi-Fi Network Feedback** (`REQ-61` / `NL-REQ-33b`)
5. **Bedtime & Do Not Disturb Feedback** (`REQ-62` / `NL-REQ-33c`)

All capsules adhere strictly to the **Nothing OS Design Language** (pure monochrome with Nothing Red and Cyan accents, 200dp symmetrical punch-hole clearance, monospace typography) and the **Android 17 / API 37 security framework** (`RECEIVER_NOT_EXPORTED`, lifecycle-safe callbacks).

---

### 2. Requirements & Specification Traceability

| Requirement | Reference Trigger | Android Framework Source | Visual Presentation & Timing |
| :--- | :--- | :--- | :--- |
| **REQ-36: Silent Mode** | Volume down to silent, mute toggle, or physical slider | `AudioManager.RINGER_MODE_CHANGED_ACTION` | 200dp pill: Red slashed-bell icon (`#D71921`) + `SILENT` text (2500ms timeout) |
| **REQ-37: Sound On / Ring** | Volume up or unmuting device | `AudioManager.RINGER_MODE_CHANGED_ACTION` | 200dp pill: White bell icon (`#FFFFFF`) + `SOUND ON` text (2500ms timeout) |
| **REQ-37a: Vibrate Mode** | Switching to vibrate mode | `AudioManager.RINGER_MODE_CHANGED_ACTION` | 200dp pill: Slashed vibrating bell (`#FFFFFF`) + `VIBRATE` text (2500ms timeout) |
| **REQ-35: Device Unlock** | Screen unlocked past keyguard | `Intent.ACTION_USER_PRESENT` | 200dp pill: Animated open padlock or Face ID squircle (`#00E5FF`) + `UNLOCKED` (1800ms timeout) |
| **REQ-60: Flashlight State** | Quick settings torch toggle | `CameraManager.TorchCallback` | 200dp pill: Torch beam vector (`#FFFFFF` on / `#888888` off) + `TORCH ON` / `TORCH OFF` (2000ms timeout) |
| **REQ-61: Wi-Fi Status** | Network connect/disconnect | `ConnectivityManager.NetworkCallback` | 200dp pill: Wi-Fi signal arcs (`#00E5FF`) + SSID or `WI-FI CONNECTED` / `DISCONNECTED` (2500ms timeout) |
| **REQ-62: Bedtime / DND** | DND schedule or manual toggle | `NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED` | 200dp pill: Crescent moon vector (`#FFFFFF`) + `DO NOT DISTURB` / `DND OFF` (2500ms timeout) |

---

### 3. Architecture & Codebase Integration

#### 3.1 Event Engine Hierarchy (`IslandEvent.kt`)
Add dedicated event classes under `com.nothing.island.ai.engine.IslandEvent`:
```kotlin
// 1. Audio / Ringer Mode
data class RingerModeChanged(
    override val id: String,
    override val timestamp: Long = System.currentTimeMillis(),
    override val source: String = "RingerEngine",
    override val priority: Int = IslandPriority.SYSTEM_HARDWARE, // 15
    override val timeoutMs: Long = 2500L,
    val mode: RingerMode // SILENT, VIBRATE, NORMAL
) : IslandEvent

// 2. Device Unlock (Already declared in IslandEvent.kt)
// IslandEvent.DeviceUnlocked(unlockMethod = UnlockMethod.FINGERPRINT/PIN/FACE)
// Priority: IslandPriority.SYSTEM_HARDWARE (15), Timeout: 1800L

// 3. Torch / Flashlight
data class FlashlightStateChanged(
    override val id: String,
    override val timestamp: Long = System.currentTimeMillis(),
    override val source: String = "TorchEngine",
    override val priority: Int = IslandPriority.SYSTEM_HARDWARE, // 15
    override val timeoutMs: Long = 2000L,
    val isEnabled: Boolean
) : IslandEvent

// 4. Wi-Fi Connectivity
data class WifiStateChanged(
    override val id: String,
    override val timestamp: Long = System.currentTimeMillis(),
    override val source: String = "WifiEngine",
    override val priority: Int = IslandPriority.SYSTEM_HARDWARE, // 15
    override val timeoutMs: Long = 2500L,
    val isConnected: Boolean,
    val ssid: String? = null
) : IslandEvent

// 5. Zen Mode / DND / Bedtime
data class ZenModeChanged(
    override val id: String,
    override val timestamp: Long = System.currentTimeMillis(),
    override val source: String = "ZenModeEngine",
    override val priority: Int = IslandPriority.SYSTEM_HARDWARE, // 15
    override val timeoutMs: Long = 2500L,
    val isDndActive: Boolean,
    val filterType: Int
) : IslandEvent
```

#### 3.2 Priority Arbitration & Fallback Invariants (`IslandPriorityManager.kt`)
- **Priority Tier:** `IslandPriority.SYSTEM_HARDWARE = 15`.
- **Preemption Rule:**
  - System events preempt base idle (`IDLE_BASE = 0`) and continuous battery fallback.
  - When media is playing (`MEDIA_PLAYBACK = 40`) or incoming call/notification is active (`NOTIFICATION = 55`, `CRITICAL_TELEPHONY = 100`), system events yield or present a non-intrusive brief capsule before restoring the persistent media fallback via `restoreHighestFallbackLocked()`.
- **Automatic Expiration:** All system events carry strict `timeoutMs` (1800ms - 2500ms). When the timeout triggers, `IslandPriorityManager` automatically dismisses the pill and restores the highest background fallback.

#### 3.3 Hardware Telemetry Subsystems (`com.nothing.island.ai.system`)
Create focused, decoupled listeners:
1. `RingerModeTracker.kt`:
   - Registers dynamic receiver for `AudioManager.RINGER_MODE_CHANGED_ACTION` with `Context.RECEIVER_NOT_EXPORTED`.
   - Reads `audioManager.ringerMode`.
2. `TorchStateTracker.kt`:
   - Calls `cameraManager.registerTorchCallback(callback, mainHandler)`.
   - Tracks primary rear camera torch ID.
   - Cleans up `cameraManager.unregisterTorchCallback(callback)` on destroy.
3. `WifiStateTracker.kt`:
   - Registers `connectivityManager.registerDefaultNetworkCallback(networkCallback)`.
   - Checks `NetworkCapabilities.hasTransport(TRANSPORT_WIFI)`.
   - Safely reads SSID when permission allows or falls back to `"WI-FI CONNECTED"`.
4. `ZenModeTracker.kt`:
   - Registers receiver for `NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED` (`RECEIVER_NOT_EXPORTED`).
   - Reads `notificationManager.currentInterruptionFilter`.
5. `DeviceUnlock Integration`:
   - Directly wire into existing `IslandOverlayController.kt`'s `keyguardReceiver` (`Intent.ACTION_USER_PRESENT`).

#### 3.4 DataStore Preferences (`SystemEventsPreferences.kt`)
Extend `IslandPreferencesRepository.kt` with a dedicated `SystemEventsPreferences` model:
- `systemEventsEnabled: Boolean = true` (Master switch)
- `ringerCapsuleEnabled: Boolean = true`
- `deviceUnlockCapsuleEnabled: Boolean = true`
- `flashlightCapsuleEnabled: Boolean = true`
- `wifiCapsuleEnabled: Boolean = true`
- `zenModeCapsuleEnabled: Boolean = true`
- Backed by DataStore with safe `IOException` recovery and fallback defaults.

#### 3.5 Compose UI Architecture (`SystemEventCompactView.kt`)
- Symmetrical 200dp x 36dp capsule centered over camera cutout (`NothingBlack` background, 1dp `NothingBorder`, 18dp corner radius).
- **Left Wing (73dp):** Hardware vector icon (Red slashed bell, cyan padlock, white torch beam, cyan Wi-Fi waves, or moon).
- **Center (54dp):** Transparent camera punch-hole clearance.
- **Right Wing (73dp):** High-contrast monospace state badge (`SILENT`, `SOUND ON`, `TORCH ON`, `WI-FI`, `DND`).
- Tap action: dismisses immediately or collapses.

---

### 4. Android 17 / API 37 Compatibility & Safety Rules

1. **Receiver Export Safety:** Every `registerReceiver` call must explicitly declare `Context.RECEIVER_NOT_EXPORTED`. Missing flags cause runtime `SecurityException` on Android 14+.
2. **Camera HAL Resource Safety:** `CameraManager.TorchCallback` must be safely unregistered in `detachInternal()` to avoid draining battery or preventing other apps from using the camera.
3. **Connectivity Leak Prevention:** `ConnectivityManager.NetworkCallback` must be unregistered in `detachInternal()` to avoid leaking network capabilities.
4. **Overlay Invariant Preservation:** Single-overlay window invariant (Type 2032 primary, Type 2038 fallback) must remain strictly intact.

---

### 5. Step-by-Step Implementation Sequence (Sprint 2)

```
[Step 1: Data Models & Events]
  └── Add RingerModeChanged, FlashlightStateChanged, WifiStateChanged, ZenModeChanged to IslandEvent.kt
  └── Add SystemEventsPreferences to IslandPreferencesRepository.kt

[Step 2: Hardware Telemetry Trackers]
  └── Create RingerModeTracker, TorchStateTracker, WifiStateTracker, ZenModeTracker in com.nothing.island.ai.system
  └── Wire ACTION_USER_PRESENT in IslandOverlayController.kt

[Step 3: UI Capsule Components]
  └── Build SystemEventCompactView.kt with Nothing OS vector iconography and monospace readouts
  └── Wire into IslandOverlayView.kt compact pill dispatcher

[Step 4: Settings & Studio Integration]
  └── Add System Events Studio UI in settings pager with real-time preview toggles

[Step 5: Unit & Integration Tests]
  └── Author SystemEventsUnitTest.kt covering event priority, preemption, and timeouts

[Step 6: Physical Device Verification]
  └── Test ringer mode changes, flashlight toggles, screen unlock, and Wi-Fi transitions on Nothing Phone (4a)
```

---

### 6. Verification & Test Plan

1. **Unit Testing:**
   - Verify `IslandPriorityManager` properly assigns Priority 15 to all system events.
   - Verify preemption over IDLE (0) and deference/restoration under MEDIA (40).
   - Verify timeout timers cancel and cleanly restore background fallbacks.
2. **Hardware ADB Testing Commands:**
   - *Ringer Mode:* `adb shell cmd audio set-ringer-mode 0` (Silent), `1` (Vibrate), `2` (Normal).
   - *Device Unlock:* `adb shell input keyevent 26` (Lock) then `adb shell input keyevent 82` (Unlock).
   - *Flashlight:* `adb shell cmd statusbar torch` (Toggle flashlight on/off).
   - *Wi-Fi:* `adb shell svc wifi disable` / `adb shell svc wifi enable`.
   - *DND:* `adb shell cmd notification set_interruption_filter 2` (Priority) / `1` (All).

---

### 7. Explicit Constraints Reminder

- **Sprint 2 implementation is NOT started during Sprint 1.1.**
- This plan provides the complete, pre-vetted architecture for immediate, flawless execution upon prompt direction.
