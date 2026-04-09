package com.deepeye.otg.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.*

/**
 * Spotlight-style Bottom Navigation Bar
 * Matches the React spotlight-button component with animated beam effects
 */
@Composable
fun SpotlightBottomBar(
    destinations: List<SpotlightNavDestination>,
    activeDestination: SpotlightNavDestination,
    onDestinationSelected: (SpotlightNavDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    val activeIndex = destinations.indexOf(activeDestination)
    
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
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black.copy(0.92f))
            .drawBehind {
                // Top white indicator line — matches React's white 2px line
                val lineWidth = itemWidth.toPx()
                val startX = indicatorX * lineWidth
                drawLine(
                    color = Color.White,
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
                        color = Color.White.copy(0.1f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(12.dp.toPx()),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = 0.8.dp.toPx()
                        )
                    )
                }
            )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            destinations.forEachIndexed { index, destination ->
                SpotlightNavItem(
                    destination = destination,
                    isActive = activeDestination == destination,
                    indicatorPosition = indicatorX,
                    position = index,
                    onClick = { onDestinationSelected(destination) },
                    itemWidth = itemWidth
                )
            }
        }
    }
}

/**
 * Single Navigation Item with Spotlight Beam Effect
 */
@Composable
fun SpotlightNavItem(
    destination: SpotlightNavDestination,
    isActive: Boolean,
    indicatorPosition: Float,
    position: Int,
    onClick: () -> Unit,
    itemWidth: Dp = 52.dp
) {
    // Distance-based spotlight spread — matches React logic exactly
    val distance = kotlin.math.abs(indicatorPosition - position)
    val spotlightOpacity by animateFloatAsState(
        targetValue = if (isActive) 1f else maxOf(0f, 1f - distance * 0.6f),
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "spotlight"
    )
    
    val iconTint by animateColorAsState(
        targetValue = if (isActive) Color.White else Color.White.copy(0.35f),
        animationSpec = tween(200),
        label = "tint"
    )
    
    Box(
        modifier = Modifier
            .width(itemWidth)
            .height(52.dp)
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
                .size(width = itemWidth, height = 48.dp)
                .align(Alignment.TopCenter)
                .drawBehind {
                    // Upward cone gradient — white beam from top center
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = spotlightOpacity * 0.45f),
                                Color.White.copy(alpha = spotlightOpacity * 0.15f),
                                Color.Transparent
                            ),
                            startY = 0f,
                            endY = size.height
                        ),
                        blendMode = BlendMode.Screen
                    )
                    // Radial soft center
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = spotlightOpacity * 0.3f),
                                Color.Transparent
                            ),
                            center = Offset(size.width / 2f, 0f),
                            radius = size.width * 0.9f
                        )
                    )
                }
        )
        
        // Icon
        Icon(
            imageVector = destination.icon,
            contentDescription = destination.label,
            tint = iconTint,
            modifier = Modifier.size(if (isActive) 22.dp else 20.dp)
        )
    }
}

/**
 * Navigation Destinations for Spotlight Bottom Bar
 */
enum class SpotlightNavDestination(
    val icon: ImageVector,
    val label: String
) {
    DASHBOARD(Icons.Filled.Home,        "Dashboard"),
    LAB      (Icons.Filled.Science,     "Lab"),
    BYPASS   (Icons.Filled.FlashOn,     "Bypass"),
    TOOL     (Icons.Filled.Build,       "Tool"),
    ARCHIVE  (Icons.Filled.Archive,     "Archive"),
    SETTINGS (Icons.Filled.Settings,    "Settings"),
    SHARE    (Icons.Filled.Share,       "Share"),
    PROFILE  (Icons.Filled.Person,      "Profile"),
}
