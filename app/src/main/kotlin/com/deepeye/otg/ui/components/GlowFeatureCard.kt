package com.deepeye.otg.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deepeye.otg.ui.theme.DeepEyeColors

/**
 * Enhanced Feature Card with animated glow effects
 * Used for showcasing features with premium visual feedback
 */
@Composable
fun GlowFeatureCard(
    icon: ImageVector,
    title: String,
    description: String,
    glowColor: Color = DeepEyeColors.NEON_PURPLE,
    isEnabled: Boolean = true,
    badge: String? = null,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    // Animated glow intensity
    val glowIntensity by animateFloatAsState(
        targetValue = if (isPressed) 0.6f else 0.3f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 300f),
        label = "glow_intensity"
    )
    
    // Pulsing animation for enabled state
    val pulseAlpha by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 0.2f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = 0.7f),
        label = "scale"
    )
    
    val shape = RoundedCornerShape(16.dp)
    
    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = if (isEnabled) 1f else 0.5f
            }
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick
                    )
                } else Modifier
            )
            .drawBehind {
                val cornerRadius = 16.dp.toPx()
                
                // Background
                drawRoundRect(
                    color = Color(0xFF0D0D15),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius)
                )
                
                // Outer glow
                if (isEnabled) {
                    drawRoundRect(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                glowColor.copy(alpha = glowIntensity),
                                glowColor.copy(alpha = glowIntensity * 0.5f),
                                Color.Transparent
                            ),
                            center = Offset(size.width / 2, 0f),
                            radius = size.width * 0.8f
                        ),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                    )
                }
            }
            .clip(shape)
            .border(
                width = 1.dp,
                color = if (isEnabled) glowColor.copy(alpha = 0.3f) else Color.Gray.copy(alpha = 0.2f),
                shape = shape
            )
            .padding(16.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header row with icon and badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon with glow
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(glowColor.copy(alpha = 0.15f))
                        .padding(10.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = glowColor,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                
                // Badge if provided
                if (badge != null) {
                    NeonBadge(
                        text = badge,
                        color = glowColor,
                        dotPulse = isEnabled
                    )
                }
            }
            
            // Title
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            
            // Description
            Text(
                text = description,
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.6f),
                lineHeight = 18.sp
            )
        }
    }
}

/**
 * Compact version for lists/grids
 */
@Composable
fun GlowFeatureCardCompact(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    glowColor: Color = DeepEyeColors.NEON_PURPLE,
    isActive: Boolean = false,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = 0.7f),
        label = "compact_scale"
    )
    
    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick
                    )
                } else Modifier
            )
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF0D0D15))
            .border(
                width = 1.dp,
                color = if (isActive) glowColor.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(12.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (isActive) glowColor.copy(alpha = 0.2f)
                        else Color.White.copy(alpha = 0.05f)
                    )
                    .padding(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = if (isActive) glowColor else Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxSize()
                )
            }
            
            // Text
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isActive) Color.White else Color.White.copy(alpha = 0.8f)
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }
            }
            
            // Active indicator
            if (isActive) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(RoundedCornerShape(50))
                        .background(glowColor)
                )
            }
        }
    }
}
