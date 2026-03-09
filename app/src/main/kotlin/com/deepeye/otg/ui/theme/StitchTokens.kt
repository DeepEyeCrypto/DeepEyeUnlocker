package com.deepeye.otg.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

/**
 * Design DNA extracted from Google Stitch Managed Design System.
 * Source: DeepEye OTG UI Overhaul Project
 * Variant: Professional Security Tool (Dark First)
 */
object StitchTokens {
    // ── Primary Colors ──────────────────────────────────────────────────────────
    val BackgroundDark    = Color(0xFF0D0D1A) // From Stitch background-dark
    val SurfaceDark       = Color(0xFF13132B) // From Stitch surface-dark
    val Primary           = Color(0xFF135BEC) // From Stitch primary (Neon Blue)
    
    // ── Glassmorphism Surface ──────────────────────────────────────────────────
    val GlassSurface      = Color(0x0FFFFFFF) // 6% white translucency
    val GlassBorder       = Color(0x1AFFFFFF) // 10% white border
    val GlassBorderActive = Primary.copy(alpha = 0.3f)
    
    // ── Text Colors ────────────────────────────────────────────────────────────
    val TextPrimary       = Color(0xFFF1F5F9) // Slate 100 (Deep White)
    val TextSecondary     = Color(0xFF94A3B8) // Slate 400 (Secondary Label)
    val TextMono          = Color(0xFF64FFDA) // Terminal Green (Accent)
    
    // ── Mode Accents ───────────────────────────────────────────────────────────
    val AccentBrom        = Color(0xFFFF6B35) // MTK BROM Orange
    val AccentAdb         = Color(0xFF00E5FF) // ADB Cyan
    val AccentEdl         = Color(0xFFFF1744) // EDL Red
    val AccentError       = Color(0xFFFF1744) // Alias for theme error color
    val AccentFastboot    = Color(0xFF2979FF) // Fastboot Blue
    val AccentApple       = Color(0xFFE0E0E0) // Apple DFU/Recovery Silver (spec Stage 20)
    val AccentSuccess     = Color(0xFF4ADE80) // Success Green
    val AccentWarning     = Color(0xFFFFD740) // Amber Warning
    
    // ── Spacing & Shapes ────────────────────────────────────────────────────────
    val GridBase          = 4.dp
    val PaddingNone       = 0.dp
    val PaddingXs         = 4.dp
    val PaddingSm         = 8.dp
    val PaddingMd         = 16.dp
    val PaddingLg         = 24.dp
    val PaddingXl         = 32.dp
    
    val RadiusDefault     = 16.dp
    val RadiusLarge       = 24.dp
    val RadiusFull        = 9999.dp
    
    // ── Typography Scale ────────────────────────────────────────────────────────
    val DisplayLarge = TextStyle(
        fontSize = 32.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-1).sp
    )
    
    val TitleLarge = TextStyle(
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.sp
    )
    
    val BodyMedium = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.sp
    )
    
    val LabelSmall = TextStyle(
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.5.sp
    )
    
    val MonoCode = TextStyle(
        fontSize = 12.sp,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Medium
    )
}
