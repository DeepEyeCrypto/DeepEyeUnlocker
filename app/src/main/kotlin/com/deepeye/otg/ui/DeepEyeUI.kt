package com.deepeye.otg.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ═══════════════════════════════════════════════════════════════════
//  DeepEye Design System — Glassmorphism Theme
// ═══════════════════════════════════════════════════════════════════

object DeepEyeColors {
    val DarkBackground = Color(0xFF0A0A0B)
    val SurfaceDark    = Color(0xFF161618)
    val IndigoAccent   = Color(0xFF6366F1)
    val CyanAccent     = Color(0xFF00F2FF)
    val SafeGreen      = Color(0xFF22C55E)
    val RestrictedRed  = Color(0xFFEF4444)
    val GlassWhite     = Color(0xFFFFFFFF).copy(alpha = 0.05f)
    val GlassBorder    = Color(0xFFFFFFFF).copy(alpha = 0.1f)
    val TextPrimary    = Color(0xFFFFFFFF)
    val TextSecondary  = Color(0xFF9CA3AF)
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(DeepEyeColors.GlassWhite)
            .border(1.dp, DeepEyeColors.GlassBorder, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column { content() }
    }
}

@Composable
fun OperationTierBadge(tier: Int) {
    val (label, color) = when (tier) {
        1 -> "SAFE" to DeepEyeColors.SafeGreen
        2 -> "POLICY" to Color(0xFFF59E0B)
        3 -> "RESTRICTED" to DeepEyeColors.RestrictedRed
        else -> "N/A" to DeepEyeColors.TextSecondary
    }
    Surface(
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(4.dp),
        modifier = Modifier.padding(bottom = 4.dp)
    ) {
        Text(
            text = "TIER $tier · $label",
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun PrimaryIconButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = DeepEyeColors.IndigoAccent
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(40.dp),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(containerColor = containerColor),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
    ) {
        Text(text = text, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}
