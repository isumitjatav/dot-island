# NOTHING ISLAND — R4.6: ACCESSIBILITY OVERLAY IMPLEMENTATION & HARDWARE VERIFICATION REPORT

**Project:** Nothing Island (`com.nothing.island.ai`)  
**Hardware Target:** Nothing Phone (4a) (`A069` / `FroggerIND`)  
**Target Serial:** `00294661P008434`  
**Operating System:** Android 17 / API 37 (Build UQ1A.240205.004)  
**Display Specification:** 1224 × 2720 px @ 480 dpi (`density = 3.0`)  
**Physical Cutout:** Centered punch hole at `centerX = 612.0 px`, `centerY = 81.6 px`, `radius = 40.8 px`  
**Status:** **FULLY IMPLEMENTED, DEPLOYED & EMPIRICALLY VERIFIED ON PHYSICAL HARDWARE**  
**Date:** November 5, 2026  

---

## 1. EXECUTIVE SUMMARY

Milestone R4.6 successfully migrated the Nothing Island overlay host architecture from the legacy `TYPE_APPLICATION_OVERLAY` (Layer 21000) to an `AccessibilityService`-based host (`IslandAccessibilityService`) utilizing `TYPE_ACCESSIBILITY_OVERLAY` (`ty=2032`, WindowManager Layer **311000**).

### Primary Engineering Achievements:
1. **Direct Punch-Hole Touch Delivery Verified:** Physical input taps directly over the hardware camera punch hole (`X = 612, Y = 81`) now immediately invoke the island expansion event (`IslandTouch: IslandOverlayView root Box clicked, isExpanded=false -> onToggleExpand`). SystemUI's `StatusBar` (Layer 22000 / 26000) no longer consumes or intercepts taps.
2. **Transparent Bridge Discarded:** The R4 temporary touch extension (`58dp` height) has been completely removed. The compact overlay window is now sized to the true visible capsule: **124dp × 36dp** (`372 × 108 px`), `y = 9dp` (`27 px`), with a clean `18dp` corner radius.
3. **Single-Owner Mutual Exclusion Confirmed:** When `IslandAccessibilityService` is enabled, `IslandOverlayService` recognizes the active accessibility anchor and strictly suppresses competing window creation (`"Accessibility service is active as sole overlay owner. IslandOverlayService will not mount a competing window"`). WindowManager `dumpsys` confirms strictly ONE overlay window exists.
4. **Touch Pass-Through Preserved:** Touch events outside the `124dp × 36dp` capsule (`touchableRegion=SkRegion((426,27,798,135))`) pass through unobstructed to underlying applications, status bar icons, and navigation bars.
5. **Zero-Data Privacy Contract:** Configured with minimum required accessibility flags (`canRetrieveWindowContent="false"`, `canPerformGestures="false"`, `canRequestFilterKeyEvents="false"`). Nothing Island reads zero user data and monitors zero input keystrokes.

---

## 2. SYSTEM ARCHITECTURE & CODE IMPLEMENTATION

### 2.1 File Map & Responsibilities

| File Path | Action | Architectural Purpose |
|:---|:---:|:---|
| [`IslandAccessibilityService.kt`](file:///C:/Users/User/.gemini/antigravity/scratch/nothing-island/app/src/main/java/com/nothing/island/ai/IslandAccessibilityService.kt) | **Created** | Core host service running with `BIND_ACCESSIBILITY_SERVICE`. Attaches and detaches the `TYPE_ACCESSIBILITY_OVERLAY` window. |
| [`accessibility_service_config.xml`](file:///C:/Users/User/.gemini/antigravity/scratch/nothing-island/app/src/main/res/xml/accessibility_service_config.xml) | **Created** | System configuration defining zero-data flags and passive event filter (`typeWindowStateChanged`). |
| [`strings.xml`](file:///C:/Users/User/.gemini/antigravity/scratch/nothing-island/app/src/main/res/values/strings.xml) | **Created** | Transparent user-facing descriptions and privacy disclosures. |
| [`AndroidManifest.xml`](file:///C:/Users/User/.gemini/antigravity/scratch/nothing-island/app/src/main/AndroidManifest.xml) | **Modified** | Declared service with permission check and accessibility metadata. |
| [`IslandOverlayController.kt`](file:///C:/Users/User/.gemini/antigravity/scratch/nothing-island/app/src/main/java/com/nothing/island/ai/IslandOverlayController.kt) | **Modified** | Centralized singleton supporting `attachAccessibilityOverlay()` (Type 2032) and single-owner arbitration. |
| [`IslandOverlayService.kt`](file:///C:/Users/User/.gemini/antigravity/scratch/nothing-island/app/src/main/java/com/nothing/island/ai/IslandOverlayService.kt) | **Modified** | Enforces mutual exclusion: skips window mounting when accessibility host is active. |
| [`OverlayPermissionManager.kt`](file:///C:/Users/User/.gemini/antigravity/scratch/nothing-island/app/src/main/java/com/nothing/island/ai/OverlayPermissionManager.kt) | **Modified** | Added `isAccessibilityServiceEnabled()` and navigation helpers. |
| [`SettingsScreen.kt`](file:///C:/Users/User/.gemini/antigravity/scratch/nothing-island/app/src/main/java/com/nothing/island/ai/ui/screens/SettingsScreen.kt) | **Modified** | Added "ACCESSIBILITY OVERLAY ANCHOR (R4.6)" card with live state and privacy disclosures. |
| [`MainActivity.kt`](file:///C:/Users/User/.gemini/antigravity/scratch/nothing-island/app/src/main/java/com/nothing/island/ai/MainActivity.kt) | **Modified** | State updates recognizing accessibility host. |

---

## 3. PHYSICAL HARDWARE VERIFICATION LOGS & EVIDENCE

Testing was performed directly on the authorized physical hardware:
- **Device:** Nothing Phone (4a) / `A069`
- **ADB Serial:** `00294661P008434`
- **Android Version:** 17 (API 37)

### 3.1 WindowManager Inspection (`dumpsys window windows`)

Compact state window dump confirms exact layer, bounds, and touchable region:
```text
Window #2 Window{6b501c0 u0 com.nothing.island.ai}:
  mDisplayId=0 mSession=Session{5350615 16386:u0a10312} mClient=android.os.BinderProxy@e3c9743
  mOwnerUid=10312 showForAllUsers=false package=com.nothing.island.ai appop=CREATE_ACCESSIBILITY_OVERLAY
  mAttrs={(0,27)(372x108) gr=TOP CENTER sim={adjust=pan} layoutInDisplayCutoutMode=always ty=ACCESSIBILITY_OVERLAY fmt=TRANSLUCENT
    fl=NOT_FOCUSABLE NOT_TOUCH_MODAL LAYOUT_NO_LIMITS HARDWARE_ACCELERATED
    pfl=FIT_INSETS_CONTROLLED
    bhv=DEFAULT
    frameRateBoostOnTouch=true}
  Requested w=372 h=108
  mBaseLayer=311000 mSubLayer=0    mToken=WindowToken{... type=2032}
  3 visible windows: [
    6b501c0 com.nothing.island.ai, frame=[Rect(426, 27 - 798, 135)], touchableRegion=SkRegion((426,27,798,135)),
    ecdadff StatusBar, frame=[Rect(0, 0 - 1224, 162)], touchableRegion=SkRegion((0,0,1224,162)),
    2e929b2 com.nothing.island.ai/com.nothing.island.ai.MainActivity, frame=[Rect(0, 0 - 1224, 2720)]
  ]
```

**Key Proofs:**
- Window Type: `ty=ACCESSIBILITY_OVERLAY` (`2032`).
- Base Layer: `mBaseLayer=311000` (strictly above StatusBar).
- Window Dimensions: `372 × 108 px` = **`124dp × 36dp`** at 480dpi (`density = 3.0`).
- Display Positioning: `X = 426..798` (Screen Center = `612.0 px`), `Y = 27..135` (`y = 9dp`).
- Topmost Input Target: On `Display #0`, `com.nothing.island.ai` is indexed #1, before `StatusBar`.

---

### 3.2 Physical Touch Test Matrix

| Test ID | Test Target | Touch Coordinates `(X, Y)` | Measured Logcat Output | WindowManager Result | Status |
|:---:|:---|:---:|:---|:---|:---:|
| **T1** | **Direct Punch Hole** | `(612, 81)` | `IslandTouch: IslandOverlayView root Box clicked, isExpanded=false`<br/>`IslandOverlayController: Overlay resized to mode: EXPANDED (350.0 x 210.0 dp)` | Shrunk to Expanded: `Rect(87, 27 - 1137, 657)` (`1050x630 px`) | **PASSED** |
| **T2** | **Collapse Control** | `(966, 268)` | `IslandTouch: IslandOverlayView COLLAPSE button clicked`<br/>`IslandOverlayController: Overlay resized to mode: COLLAPSED (124.0 x 36.0 dp)` | Shrunk back to: `Rect(426, 27 - 798, 135)` (`372x108 px`) | **PASSED** |
| **T3** | **Left Ear Capsule** | `(480, 81)` | `IslandTouch: IslandOverlayView root Box clicked, isExpanded=false`<br/>`IslandOverlayController: Overlay resized to mode: EXPANDED` | Resized to `1050x630 px` | **PASSED** |
| **T4** | **Right Ear Capsule** | `(740, 81)` | `IslandTouch: IslandOverlayView root Box clicked, isExpanded=false`<br/>`IslandOverlayController: Overlay resized to mode: EXPANDED` | Resized to `1050x630 px` | **PASSED** |
| **T5** | **Outside Status Bar (Clock)** | `(100, 81)` | *Zero overlay events logged* | Touch delivered to Status Bar | **PASSED** |
| **T6** | **Outside Status Bar (Battery)** | `(1100, 81)` | *Zero overlay events logged* | Touch delivered to Status Bar | **PASSED** |
| **T7** | **Outside App Area** | `(612, 300)` | *Zero overlay events logged* | Touch delivered to underlying App | **PASSED** |

---

### 3.3 Single-Owner Mutual Exclusion & Lifecycle Test

1. **Service Disablement Cleanup:**
   - Accessibility service disabled via settings:
   - WindowManager dump immediately showed:
     ```text
     Display #0: [ecdadff StatusBar, 2e929b2 com.nothing.island.ai/com.nothing.island.ai.MainActivity]
     ```
   - Overlay window `42fc27e` was completely and instantaneously destroyed with zero leaked views.
2. **Service Re-enablement:**
   - Accessibility service restored:
   - Exactly ONE new window `6b501c0` mounted cleanly at Layer 311000.
3. **Competing Foreground Service Block:**
   - Command: `adb shell am start-foreground-service -n com.nothing.island.ai/.IslandOverlayService`
   - Logcat verification:
     ```text
     IslandOverlayService: IslandOverlayService created.
     IslandOverlayService: Accessibility service is active as sole overlay owner. IslandOverlayService will not mount a competing window.
     ```
   - WindowManager verified: Still strictly ONE overlay window.

---

## 4. ZERO-DATA PRIVACY SPECIFICATION

The production configuration file [`app/src/main/res/xml/accessibility_service_config.xml`](file:///C:/Users/User/.gemini/antigravity/scratch/nothing-island/app/src/main/res/xml/accessibility_service_config.xml) enforces strict sandboxing:

```xml
<?xml version="1.0" encoding="utf-8"?>
<accessibility-service xmlns:android="http://schemas.android.com/apk/res/android"
    android:description="@string/accessibility_service_description"
    android:summary="@string/accessibility_service_summary"
    android:accessibilityEventTypes="typeWindowStateChanged"
    android:accessibilityFeedbackType="feedbackGeneric"
    android:notificationTimeout="100"
    android:accessibilityFlags="flagDefault"
    android:canRetrieveWindowContent="false"
    android:canPerformGestures="false"
    android:canRequestFilterKeyEvents="false"
    android:canRequestTouchExplorationMode="false" />
```

| Flag | Value | Privacy Guarantee |
|:---|:---:|:---|
| `canRetrieveWindowContent` | `false` | The app cannot inspect view hierarchies, read screen text, passwords, or messages. |
| `canPerformGestures` | `false` | The app cannot inject artificial touch inputs or control other apps. |
| `canRequestFilterKeyEvents` | `false` | The app cannot log or intercept hardware keystrokes or button presses. |
| `canRequestTouchExplorationMode` | `false` | Screen reading exploration mode is fully disabled. |

---

## 5. COMPARATIVE MILESTONE MATRIX

| Feature / Metric | M3 Baseline | R4 Bridge Stabilization | R4.6 Accessibility Host (Final) |
|:---|:---:|:---:|:---:|
| **Host Service** | `IslandOverlayService` | `IslandOverlayService` | `IslandAccessibilityService` |
| **Window Type** | `TYPE_APPLICATION_OVERLAY` | `TYPE_APPLICATION_OVERLAY` | `TYPE_ACCESSIBILITY_OVERLAY` (`2032`) |
| **AOSP Window Layer** | 21000 (Below StatusBar) | 21000 (Below StatusBar) | **311000 (Above StatusBar)** |
| **Cutout Direct Tap** | Blocked by StatusBar | Blocked by StatusBar | **100% Responsive (`X=612, Y=81`)** |
| **Touch Workaround** | None (Taps failed) | 22dp Transparent Bridge | **None needed (True capsule bounds)** |
| **Compact Dimensions** | 124dp × 36dp (Non-interactive) | 124dp × 58dp (Bottom interactive) | **124dp × 36dp (Fully interactive)** |
| **Visual Silhouette** | Rounded Pill | Rounded Pill | **Pixel-Perfect Rounded Capsule** |
| **Outside Pass-Through** | Yes | Yes | **Yes (SkRegion touchable limits)** |
| **Mutual Exclusion** | Single service | Single service | **Centralized Singleton Arbitration** |

---

## 6. CONCLUSION

Milestone R4.6 is completely accomplished and empirically proven on the physical Nothing Phone (4a) hardware. The Dynamic Island now responds natively, instantly, and completely to touches directly over and around the physical camera cutout while preserving Nothing OS aesthetics and a zero-data privacy footprint.

Milestone R4.6 is signed off.
