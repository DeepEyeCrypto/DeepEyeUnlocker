package com.deepeye.otg.ui.components

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.*
import androidx.compose.ui.unit.Dp
import com.deepeye.otg.viewmodel.UsbViewModel
import kotlinx.coroutines.flow.StateFlow
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deepeye.otg.domain.models.ProtocolFamily
import com.deepeye.otg.ui.theme.DeepEyeColors
import com.deepeye.otg.ui.theme.DeepEyeType
import com.deepeye.otg.usb.UsbLifecycleState

/**
 * Maps ProtocolFamily to the corresponding Stitch Design Accent.
 */
@Composable
fun ProtocolFamily.getAccentColor(): Color = when (this) {
    ProtocolFamily.BROM, ProtocolFamily.MTK, ProtocolFamily.PRELOADER -> DeepEyeColors.NEON_GREEN
    ProtocolFamily.ADB -> DeepEyeColors.NEON_BLUE
    ProtocolFamily.EDL, ProtocolFamily.QC, ProtocolFamily.ROUTER -> DeepEyeColors.NEON_PURPLE
    ProtocolFamily.FASTBOOT -> DeepEyeColors.NEON_ORANGE
    ProtocolFamily.APPLE_DFU, ProtocolFamily.APPLE_RECOVERY, ProtocolFamily.APPLE_NORMAL -> DeepEyeColors.WHITE_HIGH
    ProtocolFamily.SAMSUNG, ProtocolFamily.ODIN -> DeepEyeColors.NEON_CYAN
    else -> DeepEyeColors.NEON_PURPLE
}

@Composable
fun ProtocolBadge(family: ProtocolFamily) {
    val accent = family.getAccentColor()
    val label = when (family) {
        ProtocolFamily.BROM, ProtocolFamily.MTK, ProtocolFamily.PRELOADER -> "MTK BROM"
        ProtocolFamily.EDL, ProtocolFamily.QC -> "QC EDL"
        ProtocolFamily.SAMSUNG, ProtocolFamily.ODIN -> "ODIN"
        ProtocolFamily.FASTBOOT -> "FASTBOOT"
        ProtocolFamily.ADB -> "ADB"
        else -> family.name.uppercase()
    }
    Surface(
        shape = CircleShape,
        color = accent.copy(alpha = 0.15f),
        border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(accent)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = label,
                style = DeepEyeType.CAPTION.copy(fontSize = 11.sp).copy(fontSize = 9.sp),
                color = accent
            )
        }
    }
}

@Composable
fun ConfidenceBadge(confidence: Int) {
    val color = when {
        confidence >= 90 -> DeepEyeColors.NEON_GREEN
        confidence >= 60 -> DeepEyeColors.NEON_YELLOW
        else -> DeepEyeColors.NEON_PINK
    }
    
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Default.VerifiedUser,
            contentDescription = null,
            modifier = Modifier.size(12.dp),
            tint = color.copy(alpha = 0.7f)
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = "$confidence% CONFIDENCE",
            style = DeepEyeType.CAPTION.copy(fontSize = 11.sp).copy(fontSize = 9.sp),
            color = DeepEyeColors.WHITE_MED
        )
    }
}

@Composable
fun ConnectionAuraCard(
    state: UsbLifecycleState,
    hazeState: dev.chrisbanes.haze.HazeState,
    content: @Composable BoxScope.() -> Unit
) {
    val isConnected = state.isConnected
    val protocolFamily = when (state) {
        is UsbLifecycleState.Connected -> state.protocolFamily
        is UsbLifecycleState.DeviceDetected -> state.protocolFamily
        is UsbLifecycleState.Connecting -> state.protocolFamily
        else -> ProtocolFamily.UNKNOWN
    }
    val accent = when (state) {
        is UsbLifecycleState.PermissionPending -> DeepEyeColors.NEON_YELLOW
        is UsbLifecycleState.PermissionDenied,
        is UsbLifecycleState.Error,
        is UsbLifecycleState.Dead,
        is UsbLifecycleState.NoOtgSupport -> DeepEyeColors.NEON_PINK
        else -> if (isConnected || state is UsbLifecycleState.DeviceDetected || state is UsbLifecycleState.Connecting) {
            protocolFamily.getAccentColor()
        } else {
            DeepEyeColors.WHITE_MED
        }
    }

    val pulseAlpha by rememberInfiniteTransition(label = "aura_pulse").animateFloat(
        initialValue = 0.4f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    GlassCard(
        hazeState = hazeState,
        modifier = Modifier.fillMaxWidth(),
        accentColor = accent,
    ) {
        // High-fidelity top edge glow matching protocol
        androidx.compose.foundation.Canvas(
            modifier = Modifier.matchParentSize()
        ) {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(accent.copy(alpha = 0.2f * pulseAlpha), Color.Transparent),
                    startY = 0f,
                    endY = 40f
                ),
                size = androidx.compose.ui.geometry.Size(size.width, 40f)
            )
        }
        
        content()
    }
}

@Composable
fun MissionTopBar(
    viewModel: UsbViewModel,
    sessions: Map<String, UsbLifecycleState>,
    selectedKey: String?,
    onSelect: (String) -> Unit,
    compactMode: Boolean
) {
    val showDebug by (viewModel.showDebugPanel as kotlinx.coroutines.flow.StateFlow<Boolean>).collectAsStateWithLifecycle()
    val selectedSession = selectedKey?.let(sessions::get)
    val selectedLabel = when (selectedSession) {
        is UsbLifecycleState.Connected -> selectedSession.deviceName
        is UsbLifecycleState.DeviceDetected -> selectedSession.brand
        is UsbLifecycleState.Connecting -> "Establishing link"
        is UsbLifecycleState.PermissionPending -> "USB permission pending"
        is UsbLifecycleState.PermissionDenied -> "USB permission denied"
        is UsbLifecycleState.Error -> selectedSession.message
        is UsbLifecycleState.Dead -> selectedSession.reason
        else -> if (sessions.isEmpty()) "Connected Devices" else "${sessions.size} device(s) available"
    }

    Surface(
        color = DeepEyeColors.BG_SURFACE.copy(alpha = 0.8f),
        modifier = Modifier
            .fillMaxWidth()
            .height(if (compactMode) 136.dp else 104.dp) // Adjusted heights for new responsive header
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            MissionQueueHeader(
                queueTitle = "DeepEyeUnlocker",
                queueValue = selectedLabel
            )

            Spacer(Modifier.height(4.dp))

            SessionSelectorRow(
                sessions = sessions,
                selectedKey = selectedKey,
                onSelect = onSelect,
                compactMode = compactMode,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = if (compactMode) 16.dp else 24.dp)
            )
        }
    }
}

@Composable
private fun SessionSelectorRow(
    sessions: Map<String, UsbLifecycleState>,
    selectedKey: String?,
    onSelect: (String) -> Unit,
    compactMode: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        sessions.forEach { (key, state) ->
            val isSelected = key == selectedKey
            val protocol = when (state) {
                is UsbLifecycleState.Connected -> state.protocolFamily
                is UsbLifecycleState.DeviceDetected -> state.protocolFamily
                is UsbLifecycleState.Connecting -> state.protocolFamily
                else -> ProtocolFamily.UNKNOWN
            }
            val accent = if (state.isConnected || state is UsbLifecycleState.DeviceDetected) {
                protocol.getAccentColor()
            } else {
                DeepEyeColors.WHITE_MED
            }

            Box(
                modifier = Modifier
                    .size(if (compactMode) 32.dp else 28.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) accent.copy(alpha = 0.15f) else Color.Transparent)
                    .border(1.dp, if (isSelected) accent else Color.White.copy(alpha = 0.1f), CircleShape)
                    .clickable { onSelect(key) },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (state.isConnected) Icons.Default.Smartphone else Icons.Default.Usb,
                    contentDescription = null,
                    modifier = Modifier.size(if (compactMode) 16.dp else 14.dp),
                    tint = if (isSelected) accent else DeepEyeColors.WHITE_MED
                )
            }
        }
    }
}

@Composable
private fun TelemetryBadge(label: String, value: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = label, style = DeepEyeType.CAPTION.copy(fontSize = 11.sp).copy(fontSize = 8.sp), color = DeepEyeColors.WHITE_MED)
        Spacer(Modifier.width(4.dp))
        Text(text = value, style = DeepEyeType.MONO.copy(fontSize = 12.sp).copy(fontSize = 10.sp), color = color)
    }
}

@Composable
fun MissionQueueHeader(
    queueTitle: String,
    queueValue: String,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .background(DeepEyeColors.BG_SURFACE.copy(alpha = 0.5f))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        val compact = maxWidth < 390.dp

        if (compact) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HeaderTextBlock(
                    title = queueTitle,
                    value = queueValue,
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                HeaderTextBlock(
                    title = queueTitle,
                    value = queueValue,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun HeaderTextBlock(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = title,
            color = DeepEyeColors.NEON_PURPLE,
            style = DeepEyeType.CAPTION.copy(fontSize = 11.sp).copy(fontSize = 11.sp, letterSpacing = 0.8.sp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = value,
            color = DeepEyeColors.WHITE_HIGH,
            style = DeepEyeType.BODY.copy(fontSize = 14.sp).copy(fontSize = 13.sp),
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun BottomActionButtons(
    compact: Boolean,
    onBug: () -> Unit,
    onRemoteShare: () -> Unit = {},
    onSettings: () -> Unit,
) {
    val buttonSize = if (compact) 38.dp else 42.dp
    val iconSize = if (compact) 18.dp else 20.dp
    val spacing = if (compact) 6.dp else 8.dp

    Row(
        horizontalArrangement = Arrangement.spacedBy(spacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircleActionButton(
            size = buttonSize,
            iconSize = iconSize,
            onClick = onBug
        ) {
            Icon(
                imageVector = Icons.Default.BugReport,
                contentDescription = "Bug Tools",
                tint = DeepEyeColors.NEON_PURPLE,
                modifier = Modifier.size(iconSize)
            )
        }

        CircleActionButton(
            size = buttonSize,
            iconSize = iconSize,
            onClick = onRemoteShare
        ) {
            Icon(
                imageVector = Icons.Default.Share,
                contentDescription = "Remote Share",
                tint = Color(0xFF6750A4),
                modifier = Modifier.size(iconSize)
            )
        }

        CircleActionButton(
            size = buttonSize,
            iconSize = iconSize,
            onClick = onSettings
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Settings",
                tint = DeepEyeColors.WHITE_MED,
                modifier = Modifier.size(iconSize)
            )
        }
    }
}

@Composable
private fun CircleActionButton(
    size: Dp,
    iconSize: Dp,
    onClick: () -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.08f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
        content = content
    )
}
