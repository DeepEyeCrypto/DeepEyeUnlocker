package com.deepeye.otg.ui.theme

import androidx.compose.ui.graphics.Color

object DeepEyeColors {
    val Background = Color(0xFF0A0B0F)
    val Surface = Color(0xFF111318)
    val Surface2 = Color(0xFF181B22)
    val Surface3 = Color(0xFF1F2330)
    val TerminalBackground = Color(0xFF0D0F14)

    val GlassWhite = Color(0x12FFFFFF)
    val GlassBorder = Color(0x1AFFFFFF)
    val GlassHighlight = Color(0x14FFFFFF)
    val Shadow = Color(0x66000000)

    val PrimaryCyan = Color(0xFF00E5FF)
    val PrimaryDim = Color(0xFF00B8CC)
    val Success = Color(0xFF00FF88)
    val Warning = Color(0xFFFFB800)
    val Error = Color(0xFFFF4444)
    val PurpleDim = Color(0xFF7C4DFF)
    val BlueAccent = Color(0xFF2E90FF)

    val TextPrimary = Color(0xFFEAEEF4)
    val TextSecondary = Color(0xFF8B919E)
    val TextFaint = Color(0xFF3D4452)

    val Connected = Success
    val Scanning = PrimaryCyan
    val Disconnected = TextFaint

    // Compatibility aliases used across the existing Android Compose codebase.
    val BG_VOID = Background
    val BG_SURFACE = Surface
    val BG_ELEVATED = Surface2
    val BG_OVERLAY = TerminalBackground

    val NEON_CYAN = PrimaryCyan
    val NEON_PURPLE = PurpleDim
    val NEON_GREEN = Success
    val NEON_PINK = Error
    val NEON_ORANGE = Warning
    val NEON_YELLOW = Warning
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
}
