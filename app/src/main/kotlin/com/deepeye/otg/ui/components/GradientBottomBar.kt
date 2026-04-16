package com.deepeye.otg.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Assignment
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import kotlin.math.min

/**
 * Gradient Bottom Navigation Bar
 * Features:
 * - 6 navigation items with unique gradient pairs
 * - Animated icon container size (52dp selected, 44dp unselected)
 * - Radial glow behind selected icon
 * - Linear gradient background for selected item
 * - Animated label visibility (fade + scale)
 * - Dark glass background with rounded top corners
 */

data class GradientNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val gradientStart: Color,
    val gradientEnd: Color
)

val GradientNavItems = listOf(
    GradientNavItem(
        route = "home",
        label = "Home",
        icon = Icons.Rounded.Home,
        gradientStart = Color(0xFFa955ff),
        gradientEnd = Color(0xFFea51ff)
    ),
    GradientNavItem(
        route = "devices",
        label = "Devices",
        icon = Icons.Rounded.PhoneAndroid,
        gradientStart = Color(0xFF56CCF2),
        gradientEnd = Color(0xFF2F80ED)
    ),
    GradientNavItem(
        route = "bypass",
        label = "Bypass",
        icon = Icons.Rounded.FlashOn,
        gradientStart = Color(0xFFFF9966),
        gradientEnd = Color(0xFFFF5E62)
    ),
    GradientNavItem(
        route = "network",
        label = "Network",
        icon = Icons.Rounded.Wifi,
        gradientStart = Color(0xFF80FF72),
        gradientEnd = Color(0xFF7EE8FA)
    ),
    GradientNavItem(
        route = "logs",
        label = "Logs",
        icon = Icons.AutoMirrored.Rounded.Assignment,
        gradientStart = Color(0xFFffa9c6),
        gradientEnd = Color(0xFFf434e2)
    ),
    GradientNavItem(
        route = "settings",
        label = "Settings",
        icon = Icons.Rounded.Tune,
        gradientStart = Color(0xFFFFD700),
        gradientEnd = Color(0xFFFFA500)
    )
)

/**
 * Feature count for Bypass tab badge (99 features)
 */
const val BYPASS_FEATURE_COUNT = 99

@Composable
fun GradientBottomBar(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .background(Color(0xFF0A0A0C))
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            GradientNavItems.forEach { navItem ->
                GradientNavItemComponent(
                    navItem = navItem,
                    selected = currentRoute == navItem.route,
                    onNavigate = onNavigate
                )
            }
        }
    }
}

@Composable
private fun GradientNavItemComponent(
    navItem: GradientNavItem,
    selected: Boolean,
    onNavigate: (String) -> Unit
) {
    val animatedSize by animateDpAsState(
        targetValue = if (selected) 52.dp else 44.dp,
        animationSpec = spring(dampingRatio = 0.5f),
        label = "nav_item_size"
    )
    val glowAlpha by animateFloatAsState(
        targetValue = if (selected) 0.3f else 0f,
        animationSpec = tween(400),
        label = "nav_glow_alpha"
    )
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onNavigate(navItem.route) }
            .padding(vertical = 4.dp)
    ) {
        // Glow container
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(animatedSize)
                .drawBehind {
                    if (selected) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    navItem.gradientStart.copy(alpha = glowAlpha),
                                    Color.Transparent
                                )
                            ),
                            radius = min(size.width, size.height) * 0.8f
                        )
                    }
                }
                .clip(RoundedCornerShape(14.dp))
                .background(
                    if (selected)
                        Brush.linearGradient(
                            colors = listOf(
                                navItem.gradientStart.copy(alpha = 0.2f),
                                navItem.gradientEnd.copy(alpha = 0.2f)
                            )
                        )
                    else
                        Brush.linearGradient(
                            colors = listOf(Color.Transparent, Color.Transparent)
                        )
                )
                .padding(10.dp)
        ) {
            Icon(
                imageVector = navItem.icon,
                contentDescription = navItem.label,
                tint = if (selected)
                    navItem.gradientStart
                else
                    Color(0xFF555555),
                modifier = Modifier.size(if (selected) 22.dp else 20.dp)
            )
            
            // Badge for Bypass tab (99 features)
            if (navItem.route == "bypass") {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 4.dp, y = (-4).dp)
                        .size(14.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    navItem.gradientStart,
                                    navItem.gradientEnd
                                )
                            ),
                            shape = androidx.compose.foundation.shape.CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "99",
                        fontSize = 7.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        modifier = Modifier.offset(y = (-0.5).dp)
                    )
                }
            }
        }

        // Label only for selected
        AnimatedVisibility(
            visible = selected,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut()
        ) {
            Text(
                text = navItem.label,
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                color = navItem.gradientStart,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}
