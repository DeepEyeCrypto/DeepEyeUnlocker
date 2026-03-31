# DeepEye Unlocker — ProGuard Rules (v2026.32.0)

# 1. Compose Stability
-keepclassmembers class * extends androidx.compose.runtime.Immutable { *; }
-keepclassmembers class * extends androidx.compose.runtime.Stable { *; }
-keep @androidx.compose.runtime.Composable class * { *; }

# 2. Protocol Models (Sacred)
-keep class com.deepeye.otg.data.gsmg.** { *; }
-keep class com.deepeye.otg.domain.models.** { *; }
-keep class com.deepeye.otg.protocol.** { *; }


# 4. Hilt/Dagger
-keep class * implements dagger.hilt.internal.GeneratedComponent { *; }
-keep class * implements dagger.hilt.internal.ComponentEntryPoint { *; }

# 5. USB/Hardware
-keep class android.hardware.usb.** { *; }

# 6. Suppress R8 missing-class warnings (Hilt/Guava transitive deps)
-dontwarn com.google.j2objc.annotations.**
-dontwarn com.google.errorprone.annotations.**
-dontwarn javax.annotation.**
-dontwarn javax.inject.**
-dontwarn sun.misc.Unsafe
-dontwarn org.checkerframework.**
-dontwarn afu.org.checkerframework.**
-dontwarn com.google.crypto.tink.**