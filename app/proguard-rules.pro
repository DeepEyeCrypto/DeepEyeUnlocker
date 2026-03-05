# ═══════════════════════════════════════════════════════════════
# DeepEye Unlocker — ProGuard / R8 Rules
# ═══════════════════════════════════════════════════════════════

# ── JNI Bridge — CRITICAL: keep all native methods ──────────
-keep class com.deepeye.otg.NativeBridge { *; }
-keepclassmembers class com.deepeye.otg.NativeBridge {
    native <methods>;
}

# ── Keep all classes with native methods ────────────────────
-keepclasseswithmembernames class * {
    native <methods>;
}

# ── USB + Protocol classes (used via reflection) ────────────
-keep class com.deepeye.otg.usb.** { *; }
-keep class com.deepeye.otg.engine.** { *; }

# ── Sealed class — exhaustive when() matching ───────────────
-keep class com.deepeye.otg.usb.SessionState { *; }
-keep class com.deepeye.otg.usb.SessionState$* { *; }
-keep class com.deepeye.otg.usb.DeepEyeOperation { *; }
-keep class com.deepeye.otg.usb.ProtocolFamily { *; }

# ── ViewModel — keep for ViewModelProvider ──────────────────
-keep class * extends androidx.lifecycle.ViewModel { *; }

# ── Protocol probe + detected protocol enums ────────────────
-keep class com.deepeye.otg.ProtocolProbe { *; }
-keep class com.deepeye.otg.DetectedProtocol { *; }

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
