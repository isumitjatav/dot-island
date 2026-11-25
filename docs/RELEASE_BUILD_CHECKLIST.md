# Nothing Island — Release Build & Verification Checklist

## 1. Signing & Secrets Configuration (Never Commit Keystores)

`app/build.gradle.kts` keeps debug and release signing strictly separate and never signs release builds with the debug keystore. Release signing is configured via **environment variables**, **Gradle project properties**, or git-ignored `local.properties`:

| Purpose | Gradle / `local.properties` Key | Environment Variable |
|---|---|---|
| Keystore file path | `nothing.release.storeFile` | `KEYSTORE_PATH` |
| Keystore password | `nothing.release.storePassword` | `KEYSTORE_PASSWORD` |
| Signing key alias | `nothing.release.keyAlias` | `KEY_ALIAS` |
| Signing key password | `nothing.release.keyPassword` | `KEY_PASSWORD` |
| Version code override | `nothing.versionCode` | `NOTHING_VERSION_CODE` |
| Version name override | `nothing.versionName` | `NOTHING_VERSION_NAME` |

> Keystores (`*.jks`, `*.keystore`, `*.p12`) and `local.properties` are excluded via `.gitignore`.

## 2. Code Shrinking & Resource Shrinking

Release builds enable R8 minification (`isMinifyEnabled = true`) and resource shrinking (`isShrinkResources = true`) using `proguard-android-optimize.txt` and `app/proguard-rules.pro`. Keep rules explicitly preserve:
- All `com.nothing.island.ai.**` services, broadcast receivers, enums, DataStore models, and Compose themes.
- Reflection targets for `ViewTreeObserver$InternalInsetsInfo` and `BluetoothDevice.getBatteryLevel()` / `getMetadata(int)`.
- Jetpack Compose runtime/animation and Kotlin coroutines classes.

## 3. Release Build Commands (PowerShell)

```powershell
# Unsigned release verification (when keystore is intentionally omitted locally):
.\gradlew.bat testDebugUnitTest lint assembleRelease

# Signed release build via environment variables:
$env:KEYSTORE_PATH="C:\path\to\release.jks"
$env:KEYSTORE_PASSWORD="..."
$env:KEY_ALIAS="..."
$env:KEY_PASSWORD="..."
$env:NOTHING_VERSION_CODE="2"
$env:NOTHING_VERSION_NAME="1.0.1"
.\gradlew.bat clean testDebugUnitTest lint assembleRelease
```

## 4. Pre-Release Smoke Verification Checklist

- [ ] `.\gradlew.bat testDebugUnitTest` passes 100%.
- [ ] `.\gradlew.bat lintDebug` reports zero blocking errors.
- [ ] `.\gradlew.bat connectedDebugAndroidTest` passes on target Android device / emulator.
- [ ] Verify release APK (`app-release.apk` or `app-release-unsigned.apk`) launches without `AndroidRuntime` or R8 `NoSuchMethodException` / missing `CompositionLocal` errors.
- [ ] Verify accessibility global screenshot gesture, Bluetooth earbud pill (or graceful fallback without `BLUETOOTH_CONNECT`), Quick Tools torch & Settings fallbacks, heads-up suppression rules, and Dashboard states.
