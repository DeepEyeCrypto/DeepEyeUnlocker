# ── Optimization — ENABLE for production build (Stage J) ──────
# Remove -dontoptimize to allow R8 to perform structural shrinking
# -dontoptimize

# ── JNI Bridge — CRITICAL: keep all native methods ──────────
-keep class com.deepeye.otg.NativeBridge { *; }
-keep class com.deepeye.otg.repair.NvBridge { *; }
-keep class com.deepeye.otg.usb.IRecoveryBridge { *; }
-keep class com.deepeye.otg.usb.IRecoveryBridge$Companion { *; }
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
# [HARDENED] Allowing obfuscation of internal states
-keep class com.deepeye.otg.usb.UsbTransport { *; }
-keep class com.deepeye.otg.protocol.** { *; }
-keep class com.deepeye.otg.protocol.ProtocolDetector { *; }
-keep class com.deepeye.otg.protocol.mtk.MtkSession { *; }
-keep class com.deepeye.otg.protocol.qualcomm.QcomSession { *; }

# ── Domain Models — CRITICAL for state-driven UI ──────────
-keep class com.deepeye.otg.domain.models.** { *; }
-keep class com.deepeye.otg.domain.engine.** { *; }
-keep class com.deepeye.otg.policy.** { *; }
-keep class com.deepeye.otg.data.gsmg.** { *; }
-keep class com.deepeye.otg.data.hardware.** { *; }
-keep class com.deepeye.otg.intelligence.vulndb.** { *; }

# ── Service layer — OBFUSCATE forensic logic ───────────────
-keep class com.deepeye.otg.service.MassExtractor { *; }
-keep class com.deepeye.otg.service.ReportManager { *; }

# ── Exploit Research — STRIP metadata (Stage J) ────────────
-keep class com.deepeye.otg.exploit.ExploitPayload { *; }
-keepattributes *Annotation*,Signature
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
-keep class com.deepeye.otg.data.gsmg.BypassEvent { *; }
-keep class com.deepeye.otg.data.gsmg.BypassEvent$** { *; }
-keep class com.deepeye.otg.usb.IosOtgError { *; }
-keep class com.deepeye.otg.usb.IosOtgError$** { *; }
-keep class com.deepeye.otg.protocol.mtk.MtkBromProtocol$MtkError { *; }
-keep class com.deepeye.otg.protocol.mtk.MtkBromProtocol$MtkError$** { *; }
-keepclassmembers enum com.deepeye.otg.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ── Reflection-backed JSON fields ───────────────────────────
-keepclassmembers class * {
    @com.squareup.moshi.Json <fields>;
}
-dontwarn com.squareup.moshi.**

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
