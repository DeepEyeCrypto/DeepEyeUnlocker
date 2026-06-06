package com.deepeye.otg.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deepeye.otg.ui.theme.DeepEyeColors
import com.deepeye.otg.ui.theme.DeepEyeType
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.Surface
import kotlin.math.cos
import kotlin.math.sin

// Helper function for angle conversion
private fun Double.toRadians(): Double = this * kotlin.math.PI / 180.0

// ── NeonBadge ─────────────────────────────────────────────────
@Composable
fun NeonBadge(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    dotPulse: Boolean = false
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(3.dp))
            .background(color.copy(0.12f))
            .border(0.5.dp, color.copy(0.35f), RoundedCornerShape(3.dp))
            .padding(horizontal = 5.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        if (dotPulse) {
            val scale by rememberInfiniteTransition(label = "dot").animateFloat(
                0.7f, 1f,
                infiniteRepeatable(tween(800), RepeatMode.Reverse),
                label = "s"
            )
            Box(
                Modifier
                    .size(4.dp)
                    .graphicsLayer { scaleX = scale; scaleY = scale }
                    .background(color, RoundedCornerShape(50))
            )
        }
        Text(text = text, color = color, style = DeepEyeType.MICRO)
    }
}

// ── SimpleGlassCard ───────────────────────────────────────────
// Lightweight version for atoms
@Composable
fun SimpleGlassCard(
    modifier: Modifier = Modifier,
    glowColor: Color = DeepEyeColors.NEON_PURPLE,
    borderAnimated: Boolean = true,
    cornerRadius: Dp = 12.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val sweepAngle by if (borderAnimated) {
        rememberInfiniteTransition(label = "border").animateFloat(
            0f, 360f,
            infiniteRepeatable(tween(5000, easing = LinearEasing)),
            label = "angle"
        )
    } else remember { mutableStateOf(0f) }

    Box(
        modifier = modifier
            .drawBehind {
                val r = cornerRadius.toPx()
                val sw = 1.2.dp.toPx()
                
                // Base fill
                drawRoundRect(Color(0xFF0A0A12), cornerRadius = CornerRadius(r))
                
                // Corner glow (top-left accent)
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(glowColor.copy(0.08f), Color.Transparent),
                        center = Offset(0f, 0f), radius = size.width * 0.65f
                    )
                )
                
                // Glass top sheen
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        listOf(Color.White.copy(0.04f), Color.Transparent),
                        endY = size.height * 0.28f
                    ),
                    cornerRadius = CornerRadius(r)
                )
                
                // Animated rotating border
                val sweepCenter = Offset(
                    size.width / 2f + size.width / 2f * 
                        cos(sweepAngle.toDouble().toRadians()).toFloat(),
                    size.height / 2f + size.height / 2f * 
                        sin(sweepAngle.toDouble().toRadians()).toFloat()
                )
                val path = Path().apply {
                    addRoundRect(RoundRect(sw/2, sw/2, size.width-sw/2, size.height-sw/2, r, r))
                }
                drawPath(
                    path,
                    brush = Brush.sweepGradient(
                        listOf(glowColor.copy(0.05f), glowColor.copy(0.7f), glowColor.copy(0.05f)),
                        center = sweepCenter
                    ),
                    style = Stroke(sw, pathEffect = PathEffect.cornerPathEffect(r))
                )
            }
            .clip(RoundedCornerShape(cornerRadius)),
        content = content
    )
}

// ── PulsingDot ────────────────────────────────────────────────
@Composable
fun PulsingDot(color: Color, size: Dp = 5.dp, active: Boolean = true) {
    val scale by if (active) {
        rememberInfiniteTransition(label = "d").animateFloat(
            0.6f, 1f, infiniteRepeatable(tween(900), RepeatMode.Reverse), "ds"
        )
    } else remember { mutableStateOf(0.6f) }
    Box(
        Modifier.size(size)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .background(if (active) color else color.copy(0.4f), RoundedCornerShape(50))
    )
}

// ── NeonDivider ───────────────────────────────────────────────
@Composable
fun NeonHorizontalDivider(color: Color = DeepEyeColors.WHITE_LOW, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(0.5.dp)
            .background(
                Brush.horizontalGradient(listOf(Color.Transparent, color, Color.Transparent))
            )
    )
}

// ── AnimatedCounter ───────────────────────────────────────────
@Composable
fun AnimatedCounter(count: Int, color: Color, label: String, size: TextUnit = 9.sp) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        PulsingDot(color = color, size = 5.dp, active = count > 0)
        Text(
            "$count",
            color = color,
            fontSize = size,
            fontWeight = FontWeight.Black,
            style = TextStyle(fontFeatureSettings = "tnum")
        )
        Text(label, color = color.copy(0.5f), fontSize = size * 0.8f)
    }
}

// ── GoldCtaButton ─────────────────────────────────────────────
@Composable
fun GoldCtaButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "press"
    )

    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .height(48.dp)
            .clip(RoundedCornerShape(10.dp)),
        color = if (enabled) DeepEyeColors.GoldAccent else DeepEyeColors.Surface3,
        interactionSource = interactionSource
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .background(
                    if (enabled) {
                        Brush.verticalGradient(
                            listOf(
                                DeepEyeColors.GoldAccent,
                                DeepEyeColors.GoldHover
                            )
                        )
                    } else Brush.verticalGradient(
                        listOf(
                            DeepEyeColors.Surface2,
                            DeepEyeColors.Surface3
                        )
                    )
                )
        ) {
            // Glass sheen
            Box(
                Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.5f)
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.White.copy(0.15f), Color.Transparent)
                        )
                    )
            )
            
            Text(
                text = text.uppercase(),
                color = if (enabled) Color.Black else DeepEyeColors.TextMuted,
                style = DeepEyeType.PARA_BOLD,
                letterSpacing = 1.sp
            )
        }
    }
}
