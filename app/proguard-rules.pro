# DeepEye Unlocker — ProGuard Rules (v2026.32.0)

# 1. Compose Stability
-keepclassmembers class * extends androidx.compose.runtime.Immutable { *; }
-keepclassmembers class * extends androidx.compose.runtime.Stable { *; }
-keep @androidx.compose.runtime.Composable class * { *; }

# 2. Protocol Models (Sacred)
-keep class com.deepeye.otg.data.gsmg.** { *; }
-keep class com.deepeye.otg.domain.models.** { *; }
-keep class com.deepeye.otg.protocol.** { *; }

# 3. Timber/Logging
-keep class timber.log.Timber { *; }
-dontwarn timber.log.**

# 4. Hilt/Dagger
-keep class * implements dagger.hilt.internal.GeneratedComponent { *; }
-keep class * implements dagger.hilt.internal.ComponentEntryPoint { *; }

# 5. USB/Hardware
-keep class android.hardware.usb.** { *; }