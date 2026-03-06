package com.deepeye.otg.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ═══════════════════════════════════════════════════════════════════
//  DeepEye Design System v2 — LIQUID GLASS
//  Ported from Stitch "Liquid Glass" designs (screen3/4/5)
// ═══════════════════════════════════════════════════════════════════

object DeepEyeColors {
    // Core palette — deep space gradient
    val BgStart         = Color(0xFF05050F)
    val BgEnd           = Color(0xFF0A0015)
    val DeepSpace       = Color(0xFF0D0D1A)

    // Purple accent system
    val PrimaryGlow     = Color(0xFF7C4DFF)
    val AccentPurple    = Color(0xFF9C6FFF)
    val GradientStart   = Color(0xFF9747FF)
    val GradientEnd     = Color(0xFF6B2FE0)

    // Glass
    val GlassWhite      = Color.White.copy(alpha = 0.10f)
    val GlassBg         = Color.White.copy(alpha = 0.03f)
    val GlassBorder     = Color.White.copy(alpha = 0.12f)
    val GlassBorderLight = Color.White.copy(alpha = 0.20f)
    val GlassCardBg     = Color.White.copy(alpha = 0.05f)

    // Tier colors — brighter for glass theme
    val Tier1Green      = Color(0xFF69FF47)
    val Tier2Yellow     = Color(0xFFFFD740)
    val Tier3Red        = Color(0xFFFF6E6E)

    // Text
    val TextPrimary     = Color.White
    val TextSecondary   = Color.White.copy(alpha = 0.60f)
    val TextTertiary    = Color.White.copy(alpha = 0.40f)

    // Terminal
    val TerminalGreen   = Color(0xFF4ADE80)
    val TerminalYellow  = Color(0xFFFACC15)
    val TerminalInfo    = Color.White.copy(alpha = 0.60f)

    // Orbs
    val OrbPurple       = Color(0xFF6C3EF4).copy(alpha = 0.15f)
    val OrbBlue         = Color(0xFF3E7FF4).copy(alpha = 0.10f)

    // Traffic lights
    val TrafficRed      = Color(0xFFEF4444).copy(alpha = 0.5f)
    val TrafficYellow   = Color(0xFFEAB308).copy(alpha = 0.5f)
    val TrafficGreen    = Color(0xFF22C55E).copy(alpha = 0.5f)

    // Legacy aliases (backward compat)
    val Primary         = PrimaryGlow
    val DarkBackground  = BgStart
    val SurfaceDark     = Color(0xFF1A1A2E)
    val SurfaceVariant  = Color(0xFF2A2A4A)
    val IndigoAccent    = PrimaryGlow
    val CyanAccent      = Color(0xFF00F2FF)
    val SafeGreen       = Tier1Green
    val RestrictedRed   = Tier3Red

    // Gradients
    val BgGradient = Brush.linearGradient(
        colors = listOf(BgStart, BgEnd),
        start = Offset(0f, 0f),
        end = Offset(Float.MAX_VALUE, Float.MAX_VALUE)
    )

    val ButtonGradient = Brush.horizontalGradient(
        colors = listOf(GradientStart, GradientEnd)
    )

    val ProgressGradient = Brush.horizontalGradient(
        colors = listOf(AccentPurple, Color(0xFFC084FC))
    )

    val GradientBorderBrush = Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.30f),
            Color.White.copy(alpha = 0.05f)
        )
    )
}

// ═══════════════════════════════════════════════════════════════════
//  Deep Space Background — gradient + blurred orbs
// ═══════════════════════════════════════════════════════════════════

@Composable
fun DeepSpaceBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DeepEyeColors.BgGradient)
    ) {
        // Purple orb — top left (large + low alpha instead of blur)
        Box(
            modifier = Modifier
                .size(400.dp)
                .offset(x = (-100).dp, y = (-100).dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF6C3EF4).copy(alpha = 0.20f),
                            Color(0xFF6C3EF4).copy(alpha = 0.05f),
                            Color.Transparent
                        )
                    ),
                    CircleShape
                )
        )
        // Blue orb — bottom right
        Box(
            modifier = Modifier
                .size(400.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 100.dp, y = 100.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF3E7FF4).copy(alpha = 0.15f),
                            Color(0xFF3E7FF4).copy(alpha = 0.03f),
                            Color.Transparent
                        )
                    ),
                    CircleShape
                )
        )
        content()
    }
}

// ═══════════════════════════════════════════════════════════════════
//  Glass Card — frosted glass with gradient border
// ═══════════════════════════════════════════════════════════════════

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(DeepEyeColors.GlassCardBg)
            .border(1.dp, DeepEyeColors.GlassBorder, RoundedCornerShape(cornerRadius))
            .padding(16.dp),
        content = content
    )
}

// ═══════════════════════════════════════════════════════════════════
//  Glass Pill — frosted pill button/badge
// ═══════════════════════════════════════════════════════════════════

@Composable
fun GlassPill(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.05f))
            .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(50)),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}

// ═══════════════════════════════════════════════════════════════════
//  Tier Badge — pill with glow border
// ═══════════════════════════════════════════════════════════════════

@Composable
fun OperationTierBadge(tier: Int) {
    val (label, fg) = when (tier) {
        1 -> "TIER 1" to DeepEyeColors.Tier1Green
        2 -> "TIER 2" to DeepEyeColors.Tier2Yellow
        3 -> "TIER 3" to DeepEyeColors.Tier3Red
        else -> "N/A" to DeepEyeColors.TextSecondary
    }
    Surface(
        color = fg.copy(alpha = 0.10f),
        shape = RoundedCornerShape(50),
        modifier = Modifier.border(1.dp, fg.copy(alpha = 0.30f), RoundedCornerShape(50))
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            color = fg,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

// ═══════════════════════════════════════════════════════════════════
//  Gradient RUN Button
// ═══════════════════════════════════════════════════════════════════

@Composable
fun GradientRunButton(
    text: String = "RUN",
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(DeepEyeColors.ButtonGradient)
            .clickableNoRipple(onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(vertical = 10.dp),
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
    }
}

// Alias for backward compat
@Suppress("UNUSED_PARAMETER")
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    @Suppress("unused") containerColor: Color = DeepEyeColors.PrimaryGlow
) = GradientRunButton(text = text, onClick = onClick, modifier = modifier)

@Suppress("UNUSED_PARAMETER")
@Composable
fun PrimaryIconButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    @Suppress("unused") containerColor: Color = DeepEyeColors.PrimaryGlow
) = GradientRunButton(text = text, onClick = onClick, modifier = modifier)

private fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier = composed {
    clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        onClick = onClick
    )
}
