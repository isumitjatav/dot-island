# Nothing Island ProGuard / R8 Rules

# Preserve all app models, themes, state classes, services, and receivers
-keep class com.nothing.island.ai.** { *; }
-keepclassmembers class com.nothing.island.ai.** { *; }
-keep enum com.nothing.island.ai.** { *; }

# Keep reflection targets used for WindowManager touch-region passthrough and Bluetooth metadata
-keepclassmembers class android.view.ViewTreeObserver$InternalInsetsInfo { *; }
-keep interface android.view.ViewTreeObserver$OnComputeInternalInsetsListener { *; }
-keepclassmembers class android.bluetooth.BluetoothDevice {
    public int getBatteryLevel();
    public byte[] getMetadata(int);
}

# Preserve DataStore preferences and coroutines
-keepclassmembers class * extends androidx.datastore.preferences.core.Preferences { *; }
-keep class androidx.datastore.** { *; }
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler

# Compose Runtime and animations
-keep class androidx.compose.animation.core.** { *; }
-keepclassmembers class androidx.compose.animation.core.** { *; }
-keep class androidx.compose.runtime.** { *; }
-keepclassmembers class androidx.compose.runtime.** { *; }
