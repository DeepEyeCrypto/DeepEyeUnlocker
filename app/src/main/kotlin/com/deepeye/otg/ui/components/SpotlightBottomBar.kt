package com.deepeye.otg.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.*
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

/**
 * Spotlight-style Bottom Navigation Bar
 * Features:
 * - Animated spotlight beam effects (vertical + radial gradients)
 * - Spring animations for smooth transitions
 * - Distance-based opacity for neighboring items
 * - Dark glass background with blur effect
 * - White 2px top indicator line
 * - Icon scaling (20dp → 22dp) on activation
 * - Haptic feedback on selection
 * - Full theme support (Dark/Light/Monet)
 */
@Composable
fun SpotlightBottomBar(
    destinations: List<SpotlightNavDestination>,
    activeDestination: SpotlightNavDestination,
    onDestinationSelected: (SpotlightNavDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    val activeIndex = destinations.indexOf(activeDestination)
    val haptic = LocalHapticFeedback.current
    
    // Theme-aware colors
    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val backgroundColor = if (isDarkTheme) {
        Color.Black.copy(0.92f)
    } else {
        Color.White.copy(0.95f)
    }
    val indicatorColor = if (isDarkTheme) Color.White else MaterialTheme.colorScheme.primary
    val beamColor = if (isDarkTheme) Color.White else MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
    val borderColor = if (isDarkTheme) Color.White.copy(0.1f) else Color.Black.copy(0.08f)
    val activeIconColor = if (isDarkTheme) Color.White else MaterialTheme.colorScheme.primary
    val inactiveIconColor = if (isDarkTheme) Color.White.copy(0.35f) else Color.Black.copy(0.45f)
    
    // Smooth animated indicator position (float)
    val indicatorX by animateFloatAsState(
        targetValue = activeIndex.toFloat(),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "indicator"
    )
    
    val itemWidth = 52.dp
    
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .drawBehind {
                // Top indicator line — animated white/accent 2px line
                val lineWidth = itemWidth.toPx()
                val startX = indicatorX * lineWidth
                drawLine(
                    color = indicatorColor,
                    start = Offset(startX, 0f),
                    end = Offset(startX + lineWidth, 0f),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
            // Outer border glow
            .then(
                Modifier.drawBehind {
                    drawRoundRect(
                        color = borderColor,
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx()),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = 0.8.dp.toPx()
                        )
                    )
                }
            )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            destinations.forEachIndexed { index, destination ->
                SpotlightNavItem(
                    destination = destination,
                    isActive = activeDestination == destination,
                    indicatorPosition = indicatorX,
                    position = index,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onDestinationSelected(destination)
                    },
                    itemWidth = itemWidth,
                    beamColor = beamColor,
                    activeIconColor = activeIconColor,
                    inactiveIconColor = inactiveIconColor,
                )
            }
        }
    }
}

/**
 * Single Navigation Item with Spotlight Beam Effect
 * Features:
 * - Distance-based opacity for smooth spotlight spread
 * - Vertical gradient beam (upward cone)
 * - Radial gradient soft center
 * - Icon scaling with spring animation
 * - Theme-aware colors
 */
@Composable
fun SpotlightNavItem(
    destination: SpotlightNavDestination,
    isActive: Boolean,
    indicatorPosition: Float,
    position: Int,
    onClick: () -> Unit,
    itemWidth: Dp = 52.dp,
    beamColor: Color = Color.White,
    activeIconColor: Color = Color.White,
    inactiveIconColor: Color = Color.White.copy(0.35f),
) {
    // Distance-based spotlight spread — matches React logic exactly
    val distance = kotlin.math.abs(indicatorPosition - position)
    val spotlightOpacity by animateFloatAsState(
        targetValue = if (isActive) 1f else maxOf(0f, 1f - distance * 0.6f),
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "spotlight"
    )
    
    // Icon color animation
    val iconTint by animateColorAsState(
        targetValue = if (isActive) activeIconColor else inactiveIconColor,
        animationSpec = tween(200),
        label = "tint"
    )
    
    // Icon scale with spring animation
    val iconScale by animateFloatAsState(
        targetValue = if (isActive) 1.1f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "iconScale"
    )
    
    // Pulse animation for active state (moved outside drawBehind)
    val pulseAlpha = if (isActive) {
        val pulseTransition = rememberInfiniteTransition(label = "pulse")
        pulseTransition.animateFloat(
            initialValue = 0.1f,
            targetValue = 0.3f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulseAlpha"
        ).value
    } else {
        0f
    }
    
    Box(
        modifier = Modifier
            .width(itemWidth)
            .height(56.dp)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        // Spotlight beam (top glow cone) — layout-safe drawBehind
        Box(
            modifier = Modifier
                .size(width = itemWidth, height = 52.dp)
                .align(Alignment.TopCenter)
                .drawBehind {
                    // Upward cone gradient — beam from top center
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                beamColor.copy(alpha = spotlightOpacity * 0.45f),
                                beamColor.copy(alpha = spotlightOpacity * 0.15f),
                                Color.Transparent
                            ),
                            startY = 0f,
                            endY = size.height
                        ),
                        blendMode = BlendMode.Screen
                    )
                    // Radial soft center — creates the "spotlight" effect
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                beamColor.copy(alpha = spotlightOpacity * 0.3f),
                                Color.Transparent
                            ),
                            center = Offset(size.width / 2f, 0f),
                            radius = size.width * 0.9f
                        )
                    )
                    
                    // Secondary glow for active state (using pre-calculated pulseAlpha)
                    if (isActive && pulseAlpha > 0f) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    beamColor.copy(alpha = pulseAlpha),
                                    Color.Transparent
                                ),
                                center = Offset(size.width / 2f, size.height * 0.2f),
                                radius = size.width * 1.2f
                            )
                        )
                    }
                }
        )
        
        // Icon with scale animation
        Icon(
            imageVector = destination.icon,
            contentDescription = destination.label,
            tint = iconTint,
            modifier = Modifier
                .graphicsLayer {
                    scaleX = iconScale
                    scaleY = iconScale
                }
                .size(if (isActive) 22.dp else 20.dp)
        )
    }
}

/**
 * Navigation Destinations for Spotlight Bottom Bar
 * 8 destinations: Dashboard, Device, Lab, Bypass, Tool, Archive, Share, Profile
 */
enum class SpotlightNavDestination(
    val icon: ImageVector,
    val label: String
) {
    DASHBOARD(Icons.Filled.Home,        "Dashboard"),
    DEVICE   (Icons.Filled.Smartphone,  "Device"),
    LAB      (Icons.Filled.Science,     "Lab"),
    BYPASS   (Icons.Filled.FlashOn,     "Bypass"),
    TOOL     (Icons.Filled.Build,       "Tool"),
    ARCHIVE  (Icons.Filled.Archive,     "Archive"),
    SHARE    (Icons.Filled.Share,       "Share"),
    PROFILE  (Icons.Filled.Person,      "Profile"),
}
