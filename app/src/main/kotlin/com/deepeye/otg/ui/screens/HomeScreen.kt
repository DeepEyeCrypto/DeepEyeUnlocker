package com.deepeye.otg.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deepeye.otg.data.QuickAccessRepository
import com.deepeye.otg.ui.components.GlassCard
import com.deepeye.otg.ui.components.LogConsole
import com.deepeye.otg.ui.components.NeonButton
import com.deepeye.otg.ui.components.NeonButtonStyle
import com.deepeye.otg.ui.components.StatusIndicator
import com.deepeye.otg.ui.components.toConsoleEntries
import com.deepeye.otg.ui.model.QuickAccessCategory
import com.deepeye.otg.ui.model.QuickAccessItem
import com.deepeye.otg.ui.theme.DeepEyeColors
import com.deepeye.otg.ui.viewmodel.LogEntry
import com.deepeye.otg.usb.UsbLifecycleState

/**
 * HomeScreen — Stage 8 Glassmorphism Redesign
 * GSMG-inspired dark premium UI with gold accent CTA.
 * Signature unchanged — drop-in replacement.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    selectedSession: UsbLifecycleState,
    recentLogs: List<LogEntry>,
    connectedCount: Int,
    modifier: Modifier = Modifier,
    onNavigate: (String) -> Unit,
) {
    val session = sessionPresentation(selectedSession)
    val allItems = remember { QuickAccessRepository.getAllItems() }
    val itemsByCategory = remember { allItems.groupBy { it.category } }

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
            IphoneFirmwareCard(onTap = { onNavigate("IPHONE_15_RESEARCH") })

            // ── Quick Access Grid — Responsive ────────────
            QuickAccessSection(
                itemsByCategory = itemsByCategory,
                onItemClick = { item ->
                    onNavigate(item.navTarget)
                }
            )

            // ── Device Status Card ──────────────────
            DeviceStatusCard(session = session, onTap = { onNavigate("DEVICES") })

            // ── Recent Activity ─────────────────────
            RecentActivitySection(
                recentLogs = recentLogs,
                onNavigateLogs = { onNavigate("LOG_SCREEN") },
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

// ── Quick Access Section — Category Tabs + Responsive Grid ─
@Composable
private fun QuickAccessSection(
    itemsByCategory: Map<QuickAccessCategory, List<QuickAccessItem>>,
    onItemClick: (QuickAccessItem) -> Unit
) {
    var selectedCategory by remember { mutableStateOf(QuickAccessCategory.BYPASS) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        // Section header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "QUICK ACCESS",
                style = MaterialTheme.typography.labelSmall,
                color = DeepEyeColors.TextMuted,
                letterSpacing = 1.5.sp
            )
            Text(
                text = "${itemsByCategory.values.sumOf { it.size }} features",
                style = MaterialTheme.typography.labelSmall,
                color = DeepEyeColors.GoldAccent,
                fontWeight = FontWeight.Bold
            )
        }

        // Category tabs — horizontal scroll
        ScrollableTabRow(
            selectedTabIndex = QuickAccessCategory.entries.indexOf(selectedCategory),
            modifier = Modifier.fillMaxWidth(),
            edgePadding = 12.dp,
            containerColor = Color.Transparent,
            contentColor = DeepEyeColors.GoldAccent,
            divider = {},
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier
                        .tabIndicatorOffset(tabPositions[QuickAccessCategory.entries.indexOf(selectedCategory)]),
                    color = DeepEyeColors.GoldAccent,
                )
            }
        ) {
            QuickAccessCategory.entries.forEach { category ->
                val count = itemsByCategory[category]?.size ?: 0
                Tab(
                    selected = selectedCategory == category,
                    onClick = { selectedCategory = category },
                    text = {
                        Text(
                            text = "${category.displayName} ($count)",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (selectedCategory == category)
                                DeepEyeColors.GoldAccent
                            else
                                DeepEyeColors.TextMuted,
                            maxLines = 1
                        )
                    }
                )
            }
        }

        HorizontalDivider(
            color = DeepEyeColors.TextFaint,
            thickness = 0.5.dp
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Grid for selected category
        val items = itemsByCategory[selectedCategory] ?: emptyList()
        QuickAccessGrid(
            items = items,
            onClick = onItemClick
        )

        Spacer(modifier = Modifier.height(80.dp)) // bottom nav space
    }
}

// ── Quick Access Grid — Chunked Rows ─────────────────────
@Composable
private fun QuickAccessGrid(
    items: List<QuickAccessItem>,
    onClick: (QuickAccessItem) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items.chunked(3).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowItems.forEach { item ->
                    QuickAccessCard(
                        item = item,
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f),
                        onClick = { onClick(item) }
                    )
                }
                // Empty fillers for last row
                repeat(3 - rowItems.size) {
                    Spacer(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                    )
                }
            }
        }
    }
}

// ── Quick Access Card ──────────────────────────────────
@Composable
private fun QuickAccessCard(
    item: QuickAccessItem,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.93f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "scale"
    )

    Card(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        pressed = true
                        tryAwaitRelease()
                        pressed = false
                    },
                    onTap = { onClick() }
                )
            },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = DeepEyeColors.Surface
        ),
        border = BorderStroke(
            1.dp,
            item.iconTint.copy(alpha = 0.3f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(item.iconTint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.label,
                    tint = item.iconTint,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = item.label,
                style = MaterialTheme.typography.labelSmall,
                color = DeepEyeColors.TextPrimary,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                fontSize = 9.sp,
                lineHeight = 12.sp
            )
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
