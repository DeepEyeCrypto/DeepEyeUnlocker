# ── Optimization — DISABLE if hanging ─────────────────────────
-dontoptimize

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

# ── USB + Protocol core ─────────────────────────────────────
-keep class com.deepeye.otg.usb.** { *; }
-keep class com.deepeye.otg.protocol.** { *; }
-keep class com.deepeye.otg.engine.** { *; }
-keep class com.deepeye.otg.repair.** { *; }

# ── Domain Models — CRITICAL for state-driven UI ──────────
-keep class com.deepeye.otg.domain.models.** { *; }
-keep class com.deepeye.otg.policy.** { *; }

# ── Service / Auth Infrastructure (Stage C) ───────────────
-keep class com.deepeye.otg.service.** { *; }
-keep class com.deepeye.otg.data.** { *; }

# ── Sealed class & Enums ────────────────────────────────────
-keep class com.deepeye.otg.usb.SessionState { *; }
-keep class com.deepeye.otg.usb.SessionState$* { *; }

# ── Protocol probe + detected protocol enums ────────────────
-keep class com.deepeye.otg.ProtocolProbe { *; }
-keep class com.deepeye.otg.DetectedProtocol { *; }
-keep class com.deepeye.otg.data.ConnectionMode { *; }

# ── Compose — required for reflection-based tooling ─────────
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# ── Kotlin metadata ─────────────────────────────────────────
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes SourceFile,LineNumberTable
-keepattributes RuntimeVisibleAnnotations

# ── Kotlin serialization (future-proof) ─────────────────────
-keepattributes InnerClasses
-dontnote kotlinx.serialization.**

# ── Remove debug/verbose logging in release ─────────────────
-assumenosideeffects class android.util.Log {
    public static int d(...);
    public static int v(...);
}

# ── Crashlytics / stack traces — keep line numbers ──────────
-renamesourcefileattribute SourceFile
