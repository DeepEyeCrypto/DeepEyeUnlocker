package com.deepeye.otg.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ── Neon Palette ──────────────────────────────────────────────
object DeepEyeColors {
    // Backgrounds
    val BG_VOID        = Color(0xFF050508)   // deepest black
    val BG_SURFACE     = Color(0xFF0A0A12)   // card base
    val BG_ELEVATED    = Color(0xFF0F0F1A)   // elevated surfaces
    val BG_OVERLAY     = Color(0xFF141420)   // modal/sheet bg
    
    // Neon Accents
    val NEON_PURPLE    = Color(0xFF7C3AED)
    val NEON_CYAN      = Color(0xFF00FFFF)
    val NEON_GREEN     = Color(0xFF39FF14)
    val NEON_PINK      = Color(0xFFFF007F)
    val NEON_ORANGE    = Color(0xFFFF6B00)
    val NEON_YELLOW    = Color(0xFFFFD700)
    val NEON_BLUE      = Color(0xFF2979FF)
    
    // Pastel Tints (for text/icons)
    val PURPLE_LIGHT   = Color(0xFFC4B5FD)
    val CYAN_LIGHT     = Color(0xFFB2F5FD)
    val GREEN_LIGHT    = Color(0xFFBBF7D0)
    val PINK_LIGHT     = Color(0xFFFDA4AF)
    
    // Neutrals
    val WHITE_HIGH     = Color(0xFFEAEAF0)
    val WHITE_MED      = Color(0xFF9898AA)
    val WHITE_LOW      = Color(0xFF3A3A4A)
    val WHITE_GHOST    = Color(0xFF1A1A25)
    
    // Signal Colors
    val SIGNAL_FULL    = NEON_GREEN
    val SIGNAL_NONE    = NEON_PINK
    val SIGNAL_PARTIAL = NEON_CYAN
    
    // Method Colors
    val METHOD_OTG     = Color(0xFFA78BFA)  // purple
    val METHOD_ADB     = Color(0xFF60A5FA)  // blue
    val METHOD_CYBER   = Color(0xFF22D3EE)  // cyan
    val METHOD_EDL     = Color(0xFFFB923C)  // orange
    val METHOD_FORCE   = Color(0xFFF87171)  // red
}

// ── Typography ────────────────────────────────────────────────
object DeepEyeType {
    val HEADER     = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Black,  letterSpacing = 1.5.sp)
    val SUBHEADER  = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold,   letterSpacing = 0.8.sp)
    val BODY       = TextStyle(fontSize = 9.sp,  fontWeight = FontWeight.Normal, letterSpacing = 0.2.sp)
    val CAPTION    = TextStyle(fontSize = 7.sp,  fontWeight = FontWeight.Medium, letterSpacing = 0.3.sp)
    val MICRO      = TextStyle(fontSize = 6.sp,  fontWeight = FontWeight.Bold,   letterSpacing = 0.5.sp)
    val MONO       = TextStyle(fontSize = 8.sp,  fontWeight = FontWeight.Normal, fontFeatureSettings = "tnum")
}
