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
    val BackgroundDark    = Color(0xFF080A12)
    val SurfaceDark       = Color(0xFF111827)
    val Primary           = Color(0xFFA855F7)
    
    // ── Glassmorphism Surface ──────────────────────────────────────────────────
    val GlassSurface      = Color(0x661E293B)
    val GlassBorder       = Color(0x4D94A3B8)
    val GlassBorderActive = Primary.copy(alpha = 0.3f)
    
    // ── Text Colors ────────────────────────────────────────────────────────────
    val TextPrimary       = Color(0xFFE2E8F0)
    val TextSecondary     = Color(0xFF94A3B8)
    val TextMono          = Color(0xFF22D3EE)
    
    // ── Mode Accents ───────────────────────────────────────────────────────────
    val AccentBrom        = Color(0xFF22C55E)
    val AccentAdb         = Color(0xFF3B82F6)
    val AccentEdl         = Color(0xFFA855F7)
    val AccentSamsung     = Color(0xFF22D3EE)
    val AccentError       = Color(0xFFF87171)
    val AccentFastboot    = Color(0xFFF59E0B)
    val AccentApple       = Color(0xFFE2E8F0)
    val AccentSuccess     = Color(0xFF34D399)
    val AccentWarning     = Color(0xFFF59E0B)
    
    // ── Connectivity States (V3.0) ──────────────────────────────────────────────
    val ConnectionPulse   = Primary
    val ConnectionIdle    = Color(0xFF6B7280)
    val ConnectionError   = AccentError
    
    // ── Risk Hierarchy (Mission Safety) ─────────────────────────────────────────
    val RiskSafe          = AccentSuccess.copy(alpha = 0.15f)
    val RiskAdvanced      = AccentWarning.copy(alpha = 0.15f)
    val RiskDanger        = AccentError.copy(alpha = 0.15f)
    
    val BorderSafe        = AccentSuccess.copy(alpha = 0.3f)
    val BorderAdvanced    = AccentWarning.copy(alpha = 0.3f)
    val BorderDanger      = AccentError.copy(alpha = 0.3f)
    
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
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-1).sp
    )
    
    val TitleLarge = TextStyle(
        fontSize = 20.sp,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.sp
    )
    
    val BodyMedium = TextStyle(
        fontSize = 14.sp,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.sp
    )
    
    val LabelSmall = TextStyle(
        fontSize = 11.sp,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.5.sp
    )
    
    val MonoCode = TextStyle(
        fontSize = 12.sp,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Medium
    )

    // ── Semantic Design Language Extension (Phase 1) ─────────────────────────────
    object Semantic {
        // Base Layers
        val BackgroundBase      = BackgroundDark
        val BackgroundElevated  = SurfaceDark
        
        // Interaction Surfaces
        val SurfaceCard         = SurfaceDark
        val SurfaceGlass        = GlassSurface
        val BorderSubtle        = GlassBorder
        val BorderActive        = GlassBorderActive
        
        // Protocol Status Accents
        val StatusConnected     = AccentSuccess
        val StatusIdle          = ConnectionIdle
        val StatusHandshaking   = AccentWarning
        
        // Domain Accents
        val ProtocolMtk         = AccentBrom
        val ProtocolAdb         = AccentAdb
        val ProtocolEdl         = AccentEdl
        val ProtocolSamsung     = AccentSamsung
        val ProtocolFastboot    = AccentFastboot
        val ProtocolApple       = AccentApple
        
        // Mission Risk Containers
        val RiskSafeFill        = RiskSafe
        val RiskAdvancedFill    = RiskAdvanced
        val RiskDangerFill      = RiskDanger
        
        val RiskSafeBorder      = BorderSafe
        val RiskAdvancedBorder  = BorderAdvanced
        val RiskDangerBorder    = BorderDanger
        
        // Typography Hierarchy
        val TextMain            = TextPrimary
        val TextMuted           = TextSecondary
        val TextTechnical       = TextMono
        
        // Terminal System
        val TerminalBackground  = Color(0xFF030303)
        val TerminalText        = TextMono
    }
}
