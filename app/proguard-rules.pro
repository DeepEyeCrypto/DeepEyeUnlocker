# ── Optimization — ENABLE for production build (Stage J) ──────
# Remove -dontoptimize to allow R8 to perform structural shrinking
# -dontoptimize

# ── JNI Bridge — CRITICAL: keep all native methods ──────────
-keep class com.deepeye.otg.NativeBridge { *; }
-keep class com.deepeye.otg.repair.NvBridge { *; }
-keepclassmembers class com.deepeye.otg.NativeBridge {
    native <methods>;
}
-keepclassmembers class com.deepeye.otg.repair.NvBridge {
    native <methods>;
}

# ── Keep all classes with native methods ────────────────────
-keepclasseswithmembernames class * {
    native <methods>;
}

# ── Hilt / Dagger — CRITICAL for dependency injection ───────
-keep class * extends android.app.Application
-keep @com.google.dagger.hilt.android.HiltAndroidApp class *
-keep @dagger.hilt.android.lifecycle.HiltViewModel class *
-keep class com.deepeye.otg.di.** { *; }
-keep class com.deepeye.otg.Hilt_* { *; }
-keep class * implements dagger.hilt.internal.GeneratedComponent { *; }
-keep class * implements dagger.hilt.internal.UnsafeCasts { *; }

# ── USB + Protocol core ─────────────────────────────────────
-keep class com.deepeye.otg.usb.** { *; }
-keep class com.deepeye.otg.protocol.** { *; }
-keep class com.deepeye.otg.engine.** { *; }
-keep class com.deepeye.otg.repair.** { *; }

# ── Domain Models — CRITICAL for state-driven UI ──────────
-keep class com.deepeye.otg.domain.models.** { *; }
-keep class com.deepeye.otg.policy.** { *; }

-keep class com.deepeye.otg.service.** { *; }
-keep class com.deepeye.otg.data.** { *; }

# ── Exploit Research — HARDEN payloads (Stage J) ──────────
-keep class com.deepeye.otg.exploit.** { *; }
-keep class com.deepeye.otg.fuzz.** { *; }
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-dontwarn com.deepeye.otg.exploit.**

# ── OkHttp + Okio ───────────────────────────────────────────
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**

# ── Zip4j ───────────────────────────────────────────────────
-keep class net.lingala.zip4j.** { *; }
-dontwarn net.lingala.zip4j.**

# ── Sealed class & Enums ────────────────────────────────────
-keep class com.deepeye.otg.usb.SessionState { *; }
-keep class com.deepeye.otg.usb.SessionState$* { *; }

# ── Remove debug/verbose logging in release ─────────────────
-assumenosideeffects class android.util.Log {
    public static int d(...);
    public static int v(...);
    public static int i(java.lang.String, java.lang.String);
}

# ── Kotlin metadata ─────────────────────────────────────────
-keepattributes SourceFile,LineNumberTable
-keepattributes RuntimeVisibleAnnotations
-renamesourcefileattribute SourceFile
