package com.deepeye.otg.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ═══════════════════════════════════════════════════════════════════
//  DeepEye Design System — Stitch Theme Tokens
// ═══════════════════════════════════════════════════════════════════

object DeepEyeColors {
    // Core palette — mapped from Stitch designs
    val Primary        = Color(0xFF6C3EF4)   // #6c3ef4
    val PrimaryHover   = Color(0xFF5A32D0)
    val DarkBackground = Color(0xFF0D0D1A)   // #0d0d1a
    val SurfaceDark    = Color(0xFF1A1A2E)   // #1a1a2e
    val SurfaceVariant = Color(0xFF2A2A4A)   // #2a2a4a
    val TextPrimary    = Color(0xFFF8F8F2)   // #f8f8f2
    val TextSecondary  = Color(0xFF9CA3AF)   // #9ca3af

    // Tier colors
    val Tier1Green     = Color(0xFF22C55E)
    val Tier1Bg        = Color(0xFF052E16)
    val Tier2Amber     = Color(0xFFF59E0B)
    val Tier2Bg        = Color(0xFF1C1207)
    val Tier3Red       = Color(0xFFEF4444)
    val Tier3Bg        = Color(0xFF2D0A0A)

    // Terminal
    val TerminalGreen  = Color(0xFF4ADE80)
    val TerminalBg     = Color(0xFF1A1A2E)

    // Legacy aliases
    val SafeGreen      = Tier1Green
    val RestrictedRed  = Tier3Red
    val IndigoAccent   = Primary
    val CyanAccent     = Color(0xFF00F2FF)
    val GlassWhite     = Color(0xFFFFFFFF).copy(alpha = 0.05f)
    val GlassBorder    = Color(0xFFFFFFFF).copy(alpha = 0.1f)

    // Logo gradient
    val LogoGradient = Brush.horizontalGradient(
        colors = listOf(Color(0xFFEC4899), Color(0xFFF97316))
    )
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(DeepEyeColors.SurfaceDark)
            .border(1.dp, DeepEyeColors.SurfaceVariant, RoundedCornerShape(8.dp))
            .padding(12.dp),
        content = content
    )
}

@Composable
fun OperationTierBadge(tier: Int) {
    val (label, fg, bg) = when (tier) {
        1 -> Triple("TIER 1", DeepEyeColors.Tier1Green, DeepEyeColors.Tier1Bg)
        2 -> Triple("TIER 2", DeepEyeColors.Tier2Amber, DeepEyeColors.Tier2Bg)
        3 -> Triple("TIER 3", DeepEyeColors.Tier3Red, DeepEyeColors.Tier3Bg)
        else -> Triple("N/A", DeepEyeColors.TextSecondary, DeepEyeColors.SurfaceVariant)
    }
    Surface(
        color = bg,
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
            color = fg,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = DeepEyeColors.Primary
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(32.dp),
        shape = RoundedCornerShape(4.dp),
        colors = ButtonDefaults.buttonColors(containerColor = containerColor),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
    ) {
        Text(text = text, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}

// Legacy alias
@Composable
fun PrimaryIconButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = DeepEyeColors.Primary
) = PrimaryButton(text, onClick, modifier, containerColor)
