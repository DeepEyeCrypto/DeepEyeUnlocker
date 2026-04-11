# DeepEye Unlocker — ProGuard Rules (v2026.32.0)

# 1. Compose Stability
-keepclassmembers class * extends androidx.compose.runtime.Immutable { *; }
-keepclassmembers class * extends androidx.compose.runtime.Stable { *; }
-keep @androidx.compose.runtime.Composable class * { *; }

# 2. Protocol Models (Sacred)
-keep class com.deepeye.otg.data.gsmg.** { *; }
-keep class com.deepeye.otg.domain.models.** { *; }
-keep class com.deepeye.otg.protocol.** { *; }

# Protocol / Device ViewModels
-keep class com.deepeye.otg.device.**      { *; }
-keep class com.deepeye.otg.viewmodel.**   { *; }
-keep enum  com.deepeye.otg.device.**      { *; }

# USB Serial library
-keep class com.hoho.android.usbserial.** { *; }

# Kotlinx serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keep class kotlinx.serialization.** { *; }

# ADB lib
-keep class com.tananaev.adblib.** { *; }


# 4. Hilt/Dagger - COMPLETE OFFICIAL RULES
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.lifecycle.HiltViewModelGenerator { *; }

# Keep all Hilt generated classes
-keep class **HiltComponents.java { *; }
-keep class **HiltModules.java { *; }
-keep class **HiltModules$BindsModule { *; }
-keep class **HiltModules$KeyModule { *; }
-keep class **_HiltComponents { *; }
-keep class **_HiltModules { *; }

# Keep ViewModel constructors with @Inject
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    @javax.inject.Inject <init>(...);
}

# Keep Hilt internal lifecycle classes
-keep class dagger.hilt.android.internal.lifecycle.** { *; }
-keep class dagger.hilt.android.internal.managers.** { *; }
-keep class dagger.hilt.internal.** { *; }

# Keep generated factories and component implementations
-keep class * implements dagger.hilt.internal.GeneratedComponent { *; }
-keep class * implements dagger.hilt.internal.GeneratedComponentManager { *; }
-keep class * implements dagger.hilt.internal.ComponentEntryPoint { *; }
-keep class * implements dagger.hilt.internal.TestComponentEntryPoint { *; }

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

# Hilt complete rules
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keep class kotlinx.coroutines.** { *; }
-keepattributes *Annotation*
-dontwarn kotlinx.**
-dontwarn dagger.**