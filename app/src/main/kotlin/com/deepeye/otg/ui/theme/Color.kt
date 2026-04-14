package com.deepeye.otg.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * DeepEye Color Tokens — single source of truth.
 * Stage 8: merged gold-accent (GSMG-inspired) + original cyan palette.
 */
object DeepEyeColors {
    // ── Core backgrounds (AMOLED-optimized) ─────────
    val Background = Color(0xFF0A0A0A)
    val Surface = Color(0xFF111318)
    val Surface2 = Color(0xFF181B22)
    val Surface3 = Color(0xFF1F2330)
    val TerminalBackground = Color(0xFF0D0F14)

    // ── Glass tokens ────────────────────────────────
    val GlassWhite = Color(0x12FFFFFF)
    val GlassBorder = Color(0x1AFFFFFF)
    val GlassHighlight = Color(0x14FFFFFF)
    val SurfaceGlass = Color(0x14FFFFFF)    // 8% white — frosted
    val SurfaceGlass2 = Color(0x1FFFFFFF)   // 12% white — elevated
    val BorderGlass = Color(0x1AFFFFFF)     // 10% white border
    val Shadow = Color(0x66000000)

    // ── Gold accent (GSMG-inspired CTA) ─────────────
    val GoldAccent = Color(0xFFF5C518)
    val GoldHover = Color(0xFFE6B400)

    // ── Primary palette ─────────────────────────────
    val PrimaryCyan = Color(0xFF00E5FF)
    val PrimaryDim = Color(0xFF00B8CC)
    val TealSecondary = Color(0xFF00BFA5)
    val Success = Color(0xFF00FF88)
    val Warning = Color(0xFFFFB800)
    val Error = Color(0xFFFF4444)
    val PurpleDim = Color(0xFF7C4DFF)
    val BlueAccent = Color(0xFF2E90FF)

    // ── Text hierarchy ──────────────────────────────
    val TextPrimary = Color(0xFFF0F0F0)
    val TextSecondary = Color(0xFF8B919E)
    val TextMuted = Color(0xFF888888)
    val TextFaint = Color(0xFF3D4452)

    // ── Status ──────────────────────────────────────
    val Connected = Success
    val Scanning = PrimaryCyan
    val Disconnected = TextFaint

    // ── Compatibility aliases ───────────────────────
    val BG_VOID = Background
    val BG_SURFACE = Surface
    val BG_ELEVATED = Surface2
    val BG_OVERLAY = TerminalBackground

    val NEON_CYAN = PrimaryCyan
    val NEON_PURPLE = PurpleDim
    val NEON_GREEN = Success
    val NEON_PINK = Error
    val NEON_ORANGE = Warning
    val NEON_YELLOW = GoldAccent
    val NEON_BLUE = BlueAccent

    val WHITE_HIGH = TextPrimary
    val WHITE_MED = TextSecondary
    val WHITE_LOW = TextFaint
    val WHITE_GHOST = Surface3

    val SIGNAL_FULL = Success
    val SIGNAL_NONE = Error
    val SIGNAL_PARTIAL = PrimaryCyan

    val METHOD_OTG = PurpleDim
    val METHOD_ADB = BlueAccent
    val METHOD_CYBER = PrimaryCyan
    val METHOD_EDL = Warning
    val METHOD_FORCE = Error

    val Transparent = Color(0x00000000)
}
