# Nothing Island ProGuard / R8 Rules

# Preserve all app models, themes, state classes, and serializers
-keep class com.nothing.island.ai.** { *; }
-keepclassmembers class com.nothing.island.ai.** { *; }
-keep enum com.nothing.island.ai.** { *; }

# Preserve DataStore preferences and Protobuf/reflection if used
-keepclassmembers class * extends androidx.datastore.preferences.core.Preferences { *; }
-keep class androidx.datastore.** { *; }

# Compose Runtime and animations
-keep class androidx.compose.animation.core.** { *; }
-keepclassmembers class androidx.compose.animation.core.** { *; }
-keep class androidx.compose.runtime.** { *; }
-keepclassmembers class androidx.compose.runtime.** { *; }
