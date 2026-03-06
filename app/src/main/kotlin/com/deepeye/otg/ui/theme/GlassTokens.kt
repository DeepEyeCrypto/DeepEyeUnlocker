package com.deepeye.otg.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

object GlassTokens {
    val BackgroundStart = Color(0xFFFFFBFE)
    val BackgroundEnd = Color(0xFFF3EEFF)
    
    val GlassSurface = Color.White.copy(alpha = 0.60f)
    val GlassBorderLight = Color.White.copy(alpha = 0.80f)
    val GlassBorderDark = Color.White.copy(alpha = 0.30f)
    
    // Performance Safe: Pre-allocated brushes
    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(BackgroundStart, BackgroundEnd)
    )
    
    val glassBrush = Brush.linearGradient(
        colors = listOf(
            GlassSurface.copy(alpha = 0.65f),
            GlassSurface.copy(alpha = 0.45f)
        )
    )

    val accentBtnBrush = Brush.horizontalGradient(
        colors = listOf(Color(0xFF6750A4), Color(0xFF5B21B6))
    )

    // Solid border colors (No Brush allowed in borders)
    val cardBorderColor = Color.White.copy(alpha = 0.40f)

    // Log terminal colors
    val LogBackground = Color(0xFF1C1B1F)
    val LogSuccess = Color(0xFF4ADE80)
    val LogError = Color(0xFFF87171)
    val LogWarning = Color(0xFFFBBF24)
    val LogNormal = Color(0xFFD1D5DB)

    data class TierColors(
        val fill: Color,
        val border: Color,
        val text: Color
    )

    fun tierColors(tier: Int): TierColors {
        return when (tier) {
            1 -> TierColors(
                fill = Color(0xFFDCFCE7),
                border = Color(0xFF86EFAC),
                text = Color(0xFF166534)
            )
            2 -> TierColors(
                fill = Color(0xFFFEF9C3),
                border = Color(0xFFFDE047),
                text = Color(0xFF854D0E)
            )
            3 -> TierColors(
                fill = Color(0xFFFEE2E2),
                border = Color(0xFFFCA5A5),
                text = Color(0xFF991B1B)
            )
            else -> TierColors(
                fill = Color(0xFFF3F4F6),
                border = Color(0xFFD1D5DB),
                text = Color(0xFF4B5563)
            )
        }
    }
}
