package com.deepeye.otg.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deepeye.otg.UsbSessionState
import com.deepeye.otg.ConnState
import com.deepeye.otg.usb.DeepEyeOperation
import com.deepeye.otg.usb.SessionState

@Composable
fun MainScreen(
    sessionState: UsbSessionState,
    queueState: SessionState,
    logs: List<OtgActivity.LogEntry> = emptyList(),
    onSelectModel: () -> Unit,
    onRemoteUnlock: () -> Unit,
    onOperationSelected: (DeepEyeOperation) -> Unit
) {
    var selectedBrand by remember { mutableStateOf("Xiaomi") }
    var showConsole by remember { mutableStateOf(false) }
    val brands = listOf("Xiaomi", "Samsung", "Oppo", "Vivo", "Realme", "OnePlus")

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(DeepEyeColors.DarkBackground)
        ) {
            // ── HEADER (Stitch: sticky header with gradient logo + REMOTE button) ──
            StitchHeader(
                state = sessionState.state,
                onRemoteClick = onRemoteUnlock,
                onLogoClick = { showConsole = !showConsole }
            )

            // ── BRAND TABS (Stitch: horizontal scroll with bottom border indicator) ──
            StitchBrandTabs(
                brands = brands,
                selectedBrand = selectedBrand,
                onBrandSelected = {
                    selectedBrand = it
                    onSelectModel()
                }
            )

            // ── MODEL SELECTOR (Stitch: dropdown with smartphone icon) ──
            StitchModelSelector(
                selectedModel = "SELECT MODEL",
                onSelectClick = onSelectModel
            )

            // ── FEATURE GRID ──
            FeatureListScreen(onOperationSelected = onOperationSelected)
        }

        // ── CONSOLE OVERLAY ──
        if (showConsole || queueState is SessionState.ExecutingOperation) {
            ConsoleOverlay(
                logs = logs,
                onClose = { showConsole = false }
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
//  Header — gradient logo + green dot + REMOTE button
// ═══════════════════════════════════════════════════════════════════

@Composable
private fun StitchHeader(
    state: ConnState,
    onRemoteClick: () -> Unit,
    onLogoClick: () -> Unit
) {
    Surface(
        color = DeepEyeColors.SurfaceDark,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Gradient logo text
            Text(
                text = "DeepEyeUnlocker",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                style = LocalTextStyle.current.copy(
                    brush = DeepEyeColors.LogoGradient
                ),
                modifier = Modifier.clickable { onLogoClick() }
            )

            Spacer(modifier = Modifier.weight(1f))

            // Green dot
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(
                        when (state) {
                            ConnState.CONNECTED_READY -> DeepEyeColors.SafeGreen
                            ConnState.DISCONNECTED, ConnState.ERROR -> DeepEyeColors.RestrictedRed
                            else -> DeepEyeColors.Tier2Amber
                        },
                        CircleShape
                    )
            )

            Spacer(modifier = Modifier.width(8.dp))

            // REMOTE button
            OutlinedButton(
                onClick = onRemoteClick,
                shape = RoundedCornerShape(4.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = DeepEyeColors.Primary),
                border = BorderStroke(1.dp, DeepEyeColors.Primary),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Text(
                    text = "REMOTE",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp,
                    color = DeepEyeColors.Primary
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
//  Brand Tabs — horizontal scroll with underline indicator
// ═══════════════════════════════════════════════════════════════════

@Composable
private fun StitchBrandTabs(
    brands: List<String>,
    selectedBrand: String,
    onBrandSelected: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DeepEyeColors.SurfaceDark)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        brands.forEach { brand ->
            val isSelected = brand == selectedBrand
            Column(
                modifier = Modifier
                    .clickable { onBrandSelected(brand) }
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = brand,
                    color = if (isSelected) DeepEyeColors.TextPrimary else DeepEyeColors.TextSecondary,
                    fontSize = 14.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
                // Bottom border indicator
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(
                            if (isSelected) DeepEyeColors.Primary
                            else Color.Transparent
                        )
                )
            }
        }
    }

    // Divider below tabs
    HorizontalDivider(
        thickness = 1.dp,
        color = DeepEyeColors.SurfaceVariant
    )
}

// ═══════════════════════════════════════════════════════════════════
//  Model Selector — dropdown with smartphone icon
// ═══════════════════════════════════════════════════════════════════

@Composable
private fun StitchModelSelector(
    selectedModel: String,
    onSelectClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onSelectClick() },
            shape = RoundedCornerShape(8.dp),
            color = DeepEyeColors.SurfaceDark,
            border = BorderStroke(1.dp, DeepEyeColors.SurfaceVariant)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("📱", fontSize = 18.sp)
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = selectedModel,
                    color = DeepEyeColors.TextPrimary,
                    fontSize = 14.sp,
                    modifier = Modifier.weight(1f)
                )
                Text("▾", color = DeepEyeColors.TextSecondary, fontSize = 16.sp)
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
//  Console Overlay — execution log
// ═══════════════════════════════════════════════════════════════════

@Composable
fun ConsoleOverlay(
    logs: List<OtgActivity.LogEntry>,
    onClose: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f))
            .clickable { onClose() }
            .padding(top = 80.dp)
    ) {
        GlassCard(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clickable(enabled = false) {}
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Execution Log",
                    color = DeepEyeColors.TextPrimary,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp
                )
                IconButton(onClick = onClose) {
                    Text("✕", color = DeepEyeColors.RestrictedRed, fontSize = 18.sp)
                }
            }

            HorizontalDivider(color = DeepEyeColors.SurfaceVariant)

            Spacer(modifier = Modifier.height(8.dp))

            androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                items(logs.size) { i ->
                    val log = logs[i]
                    Row(modifier = Modifier.padding(vertical = 2.dp)) {
                        Text(
                            text = log.timestamp,
                            color = Color(0xFF64748B),
                            fontSize = 12.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            modifier = Modifier.width(70.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        val color = when (log.type) {
                            "ERROR" -> DeepEyeColors.RestrictedRed
                            "SUCCESS" -> DeepEyeColors.TerminalGreen
                            "WARNING" -> DeepEyeColors.Tier2Amber
                            else -> DeepEyeColors.TerminalGreen
                        }
                        Text(
                            text = log.message,
                            color = color,
                            fontSize = 12.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}
