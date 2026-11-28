# Build, Emulator Test & Device Deployment Rules

1. **Emulator First (Pixel_8_PunchHole_API_35):**
   - Before installing on any physical phone, check db devices for an emulator-* target.
   - If no emulator is running, launch emulator -avd Pixel_8_PunchHole_API_35 -no-snapshot-load in the background and wait until db -e wait-for-device shell getprop sys.boot_completed returns 1.
2. **Verify on Emulator (-e):**
   - Run ./gradlew.bat testDebugUnitTest assembleDebug
   - Install onto the emulator only using db -e install -r app\build\outputs\apk\debug\app-debug.apk.
   - Launch the app (db -e shell monkey -p com.nothing.island.ai 1) and verify db -e logcat -d -s AndroidRuntime:E shows zero crashes.
3. **Deploy Release to Physical Phone (-d) Only After Emulator Passes:**
   - Build the signed release APK (./gradlew.bat assembleRelease) and verify ndroid.permission.INTERNET and DebugEventReceiver are absent from the merged release manifest.
   - Check db devices for a physical USB/wireless device (db -d).
   - Install on the physical device with db -d install -r app\build\outputs\apk\release\app-release.apk (only run db -d uninstall com.nothing.island.ai first if INSTALL_FAILED_UPDATE_INCOMPATIBLE occurs).
