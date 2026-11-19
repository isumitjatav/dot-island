# REA Device State Notes — 2026-11-19

## Reference apps temporarily disabled for clean testing

Both reference apps were **respawning themselves** after `am force-stop`
(new PIDs each time) — Ref B has a `RestartServiceReceiver` for
`MY_PACKAGE_REPLACED` / `BOOT_COMPLETED` / `QUICKBOOT_POWERON`, and both use
sticky foreground services.

They were disabled (user 0) so they cannot interfere with Nothing Island
testing:

```powershell
adb shell pm disable-user --user 0 com.dynamicisland.notchscreenview
adb shell pm disable-user --user 0 tr.com.eywin.dynamic.island.notch
```

To re-enable them later:

```powershell
adb shell pm enable --user 0 com.dynamicisland.notchscreenview
adb shell pm enable --user 0 tr.com.eywin.dynamic.island.notch
```

## Current state
- Nothing Island accessibility service enabled:
  `com.nothing.island.ai/com.nothing.island.ai.IslandAccessibilityService`
- Reference app PIDs: empty (stopped/disabled)
- Hide System (Double) Notification: enabled in Nothing Island DataStore
- All notification toggles restored to their original values after testing
