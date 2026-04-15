package com.deepeye.otg.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PhoneIphone
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deepeye.otg.ui.components.GlassCard
import com.deepeye.otg.ui.components.LogConsole
import com.deepeye.otg.ui.components.NeonButton
import com.deepeye.otg.ui.components.NeonButtonStyle
import com.deepeye.otg.ui.components.StatusIndicator
import com.deepeye.otg.ui.components.toConsoleEntries
import com.deepeye.otg.ui.theme.DeepEyeColors
import com.deepeye.otg.ui.viewmodel.LogEntry
import com.deepeye.otg.usb.UsbLifecycleState

/**
 * HomeScreen — Stage 8 Glassmorphism Redesign
 * GSMG-inspired dark premium UI with gold accent CTA.
 * Signature unchanged — drop-in replacement.
 */
@Composable
fun HomeScreen(
    selectedSession: UsbLifecycleState,
    recentLogs: List<LogEntry>,
    connectedCount: Int,
    modifier: Modifier = Modifier,
    onNavigateMtk: () -> Unit,
    onNavigateEdl: () -> Unit,
    onNavigateSamsung: () -> Unit,
    onNavigateFrp: () -> Unit,
    onNavigateDevices: () -> Unit,
    onNavigateApple: () -> Unit,
    onNavigateLogs: () -> Unit,
) {
    val session = sessionPresentation(selectedSession)

    // Animated ambient glow
    val infiniteTransition = rememberInfiniteTransition(label = "bg")
    val animOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "bgOffset",
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DeepEyeColors.Background),
    ) {
        // ── Animated radial glow — gold top-right ───
        Box(
            modifier = Modifier
                .size(300.dp)
                .offset(
                    x = (200 + (animOffset * 40)).dp,
                    y = (-60 + (animOffset * 30)).dp,
                )
                .alpha(0.7f)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            DeepEyeColors.GoldAccent.copy(alpha = 0.08f),
                            Color.Transparent,
                        ),
                    ),
                ),
        )

        // ── Animated radial glow — teal bottom-left ─
        Box(
            modifier = Modifier
                .size(250.dp)
                .offset(
                    x = (-80 + (animOffset * 20)).dp,
                    y = (500 + (animOffset * -30)).dp,
                )
                .alpha(0.7f)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            DeepEyeColors.TealSecondary.copy(alpha = 0.06f),
                            Color.Transparent,
                        ),
                    ),
                ),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Spacer(Modifier.height(8.dp))

            // ── Header ──────────────────────────────
            HomeHeader(connectedCount = connectedCount)

            // ── Stats Row ───────────────────────────
            HomeStatsRow()

            // ── Gold CTA — iPhone Firmware Card ─────
            IphoneFirmwareCard(onTap = onNavigateApple)

            // ── Quick Access Grid ───────────────────
            Text(
                "QUICK ACCESS",
                style = MaterialTheme.typography.labelSmall,
                color = DeepEyeColors.TextMuted,
            )

            QuickToolsGrid(
                onMtk = onNavigateMtk,
                onEdl = onNavigateEdl,
                onSamsung = onNavigateSamsung,
                onApple = onNavigateApple,
                onFrp = onNavigateFrp,
                onDevices = onNavigateDevices,
            )

            // ── Device Status Card ──────────────────
            DeviceStatusCard(session = session, onTap = onNavigateDevices)

            // ── Recent Activity ─────────────────────
            RecentActivitySection(
                recentLogs = recentLogs,
                onNavigateLogs = onNavigateLogs,
            )

            Spacer(Modifier.height(84.dp))
        }
    }
}

// ── Header ──────────────────────────────────────────
@Composable
private fun HomeHeader(connectedCount: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                "DEEPEYE",
                style = MaterialTheme.typography.displayLarge,
                color = DeepEyeColors.GoldAccent,
            )
            Text(
                "UNLOCKER v2027.18",
                style = MaterialTheme.typography.labelSmall,
                color = DeepEyeColors.TextMuted,
            )
        }
        // Status pill
        GlassCard(
            hazeState = null,
            cornerRadius = 12.dp,
            accentColor = if (connectedCount > 0) DeepEyeColors.Success else Color.Transparent,
            performanceMode = true,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            if (connectedCount > 0) DeepEyeColors.Success else DeepEyeColors.TextFaint,
                            CircleShape,
                        ),
                )
                Text(
                    if (connectedCount > 0) "$connectedCount LIVE" else "IDLE",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (connectedCount > 0) DeepEyeColors.Success else DeepEyeColors.TextMuted,
                )
            }
        }
    }
}

// ── Stats Row ───────────────────────────────────────
@Composable
private fun HomeStatsRow() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        listOf(
            Triple("2,847", "Unlocked", Icons.Default.LockOpen),
            Triple("99.2%", "Success", Icons.Default.CheckCircle),
            Triple("4", "Platforms", Icons.Default.Devices),
        ).forEach { (value, label, icon) ->
            GlassCard(
                hazeState = null,
                modifier = Modifier.weight(1f),
                cornerRadius = 12.dp,
                performanceMode = true,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        icon, null,
                        tint = DeepEyeColors.GoldAccent,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        value,
                        style = MaterialTheme.typography.titleMedium,
                        color = DeepEyeColors.TextPrimary,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        label,
                        style = MaterialTheme.typography.labelSmall,
                        color = DeepEyeColors.TextMuted,
                    )
                }
            }
        }
    }
}

// ── iPhone Firmware Gold CTA Card (GSMG style) ─────
@Composable
private fun IphoneFirmwareCard(onTap: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        label = "ctaScale",
    )

    GlassCard(
        hazeState = null,
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        cornerRadius = 16.dp,
        accentColor = DeepEyeColors.GoldAccent,
        onClick = onTap,
        performanceMode = true,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            DeepEyeColors.GoldAccent.copy(0.15f),
                            DeepEyeColors.GoldAccent.copy(0.03f),
                        ),
                    ),
                ),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "iPhone Firmware",
                        style = MaterialTheme.typography.headlineMedium,
                        color = DeepEyeColors.GoldAccent,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Signature & Activation Bypass",
                        style = MaterialTheme.typography.bodyMedium,
                        color = DeepEyeColors.TextMuted,
                    )
                }
                Icon(
                    Icons.Default.PhoneIphone, null,
                    tint = DeepEyeColors.GoldAccent.copy(alpha = 0.8f),
                    modifier = Modifier.size(36.dp),
                )
            }
        }
    }
}

// ── Quick Tools Grid ────────────────────────────────
private data class QuickTool(
    val label: String,
    val icon: ImageVector,
    val color: Color,
    val onClick: () -> Unit,
)

@Composable
private fun QuickToolsGrid(
    onMtk: () -> Unit,
    onEdl: () -> Unit,
    onSamsung: () -> Unit,
    onApple: () -> Unit,
    onFrp: () -> Unit,
    onDevices: () -> Unit,
) {
    val tools = listOf(
        QuickTool("MTK\nFlash", Icons.Default.Memory, DeepEyeColors.TealSecondary, onMtk),
        QuickTool("Qualcomm\nEDL", Icons.Default.FlashOn, DeepEyeColors.PurpleDim, onEdl),
        QuickTool("Samsung\nOdin", Icons.Default.PhoneAndroid, DeepEyeColors.BlueAccent, onSamsung),
        QuickTool("Apple\nChain", Icons.Default.PhoneIphone, DeepEyeColors.GoldAccent, onApple),
        QuickTool("IMEI\nRepair", Icons.Default.SimCard, DeepEyeColors.TealSecondary, onDevices),
        QuickTool("DA\nTools", Icons.Default.Build, DeepEyeColors.Warning, onDevices),
    )

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.height(220.dp),
        userScrollEnabled = false,
    ) {
        items(tools) { tool ->
            GlassCard(
                hazeState = null,
                modifier = Modifier.aspectRatio(1f),
                cornerRadius = 14.dp,
                accentColor = tool.color.copy(alpha = 0.3f),
                onClick = tool.onClick,
                performanceMode = true,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        tool.icon, null,
                        tint = tool.color,
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        tool.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = DeepEyeColors.TextPrimary,
                        textAlign = TextAlign.Center,
                        lineHeight = 14.sp,
                    )
                }
            }
        }
    }
}

// ── Device Status Card ──────────────────────────────
@Composable
private fun DeviceStatusCard(
    session: SessionPresentation,
    onTap: () -> Unit,
) {
    GlassCard(
        hazeState = null,
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 16.dp,
        accentColor = session.accent,
        onClick = onTap,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = session.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = DeepEyeColors.TextPrimary,
                        fontWeight = FontWeight.Bold,
                    )
                    StatusIndicator(
                        state = session.status,
                        label = session.subtitle,
                    )
                }
                // Badge
                Box(
                    modifier = Modifier
                        .background(
                            session.accent.copy(alpha = 0.15f),
                            shape = MaterialTheme.shapes.extraSmall,
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        session.badge,
                        style = MaterialTheme.typography.labelSmall,
                        color = session.accent,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            // Field rows
            session.fields.take(3).forEach { field ->
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        field.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = DeepEyeColors.TextMuted,
                    )
                    Text(
                        field.value,
                        style = MaterialTheme.typography.labelSmall,
                        color = DeepEyeColors.TextPrimary,
                    )
                }
            }

            NeonButton(
                text = "Open Device Tools",
                onClick = onTap,
                icon = Icons.Default.Usb,
            )
        }
    }
}

// ── Recent Activity ─────────────────────────────────
@Composable
private fun RecentActivitySection(
    recentLogs: List<LogEntry>,
    onNavigateLogs: () -> Unit,
) {
    GlassCard(
        hazeState = null,
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 16.dp,
        performanceMode = true,
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "RECENT ACTIVITY",
                style = MaterialTheme.typography.labelSmall,
                color = DeepEyeColors.TextMuted,
            )
            Spacer(Modifier.height(12.dp))

            if (recentLogs.isEmpty()) {
                // Placeholder entries
                listOf(
                    Triple("FRP Bypass", "Realme 14x MT6835T", DeepEyeColors.Success),
                    Triple("IMEI Repair", "Redmi Note 13 Pro", DeepEyeColors.GoldAccent),
                    Triple("DA Flash", "Samsung A54 5G", DeepEyeColors.TealSecondary),
                ).forEach { (action, device, color) ->
                    ActivityRow(action = action, device = device, color = color)
                }
            } else {
                // Real log entries
                LogConsole(
                    entries = recentLogs.takeLast(6).toConsoleEntries(),
                    title = "Recent Session",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                )
            }
        }
    }

    NeonButton(
        text = "Open Full Log Viewer",
        onClick = onNavigateLogs,
        style = NeonButtonStyle.SECONDARY,
    )
}

@Composable
private fun ActivityRow(action: String, device: String, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(8.dp)
                .background(color, CircleShape),
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                action,
                style = MaterialTheme.typography.bodyMedium,
                color = DeepEyeColors.TextPrimary,
            )
            Text(
                device,
                style = MaterialTheme.typography.labelSmall,
                color = DeepEyeColors.TextMuted,
            )
        }
        Text(
            "✓",
            color = color,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
