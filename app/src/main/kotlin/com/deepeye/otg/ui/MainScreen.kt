package com.deepeye.otg.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.draw.blur
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
        DeepSpaceBackground {
            Column(modifier = Modifier.fillMaxSize()) {
                // ── GLASS HEADER ──
                LiquidGlassHeader(
                    state = sessionState.state,
                    onRemoteClick = onRemoteUnlock,
                    onLogoClick = { showConsole = !showConsole }
                )

                // ── GLASS PILL BRAND TABS ──
                LiquidBrandPills(
                    brands = brands,
                    selectedBrand = selectedBrand,
                    onBrandSelected = {
                        selectedBrand = it
                        onSelectModel()
                    }
                )

                // ── GLASS MODEL SELECTOR ──
                LiquidModelSelector(
                    selectedModel = "SELECT MODEL",
                    onSelectClick = onSelectModel
                )

                // ── FEATURE GRID ──
                FeatureListScreen(onOperationSelected = onOperationSelected)
            }
        }

        // ── CONSOLE OVERLAY ──
        if (showConsole || queueState is SessionState.ExecutingOperation) {
            ConsoleOverlay(logs = logs, onClose = { showConsole = false })
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
//  Liquid Glass Header
//  glass backdrop + logo + pulsing green dot + REMOTE pill
// ═══════════════════════════════════════════════════════════════════

@Composable
private fun LiquidGlassHeader(
    state: ConnState,
    onRemoteClick: () -> Unit,
    onLogoClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.05f))
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Logo
        Text(
            text = "DeepEyeUnlocker",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            letterSpacing = (-0.5).sp,
            modifier = Modifier.clickable { onLogoClick() }
        )

        Spacer(modifier = Modifier.weight(1f))

        // REMOTE pill with green dot
        GlassPill(
            modifier = Modifier.clickable { onRemoteClick() }
        ) {
            Spacer(modifier = Modifier.width(12.dp))
            // Pulsing green dot
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(
                        when (state) {
                            ConnState.CONNECTED_READY -> DeepEyeColors.Tier1Green
                            ConnState.DISCONNECTED, ConnState.ERROR -> DeepEyeColors.Tier3Red
                            else -> DeepEyeColors.Tier2Yellow
                        },
                        CircleShape
                    )
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "REMOTE",
                color = Color.White.copy(alpha = 0.90f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
            Spacer(modifier = Modifier.width(12.dp))
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
//  Liquid Brand Pills — horizontal scroll, frosted glass pills
//  Selected pill: purple glow + brighter bg
// ═══════════════════════════════════════════════════════════════════

@Composable
private fun LiquidBrandPills(
    brands: List<String>,
    selectedBrand: String,
    onBrandSelected: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        brands.forEach { brand ->
            val isSelected = brand == selectedBrand
            Surface(
                modifier = Modifier.clickable { onBrandSelected(brand) },
                shape = RoundedCornerShape(50),
                color = if (isSelected)
                    DeepEyeColors.PrimaryGlow.copy(alpha = 0.25f)
                else
                    Color.White.copy(alpha = 0.05f),
                border = BorderStroke(
                    1.dp,
                    if (isSelected) Color.White.copy(alpha = 0.30f)
                    else Color.White.copy(alpha = 0.10f)
                )
            ) {
                Text(
                    text = brand,
                    color = if (isSelected) Color.White else Color.White.copy(alpha = 0.60f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
//  Liquid Model Selector — gradient-border glass card
// ═══════════════════════════════════════════════════════════════════

@Composable
private fun LiquidModelSelector(
    selectedModel: String,
    onSelectClick: () -> Unit
) {
    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onSelectClick() },
            cornerRadius = 20.dp
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("📱", fontSize = 18.sp, color = Color.White.copy(alpha = 0.6f))
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = selectedModel,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                Text("▾", color = Color.White.copy(alpha = 0.40f), fontSize = 16.sp)
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
//  Console Overlay — glass terminal
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
            .padding(top = 60.dp, bottom = 16.dp, start = 16.dp, end = 16.dp)
    ) {
        GlassCard(
            modifier = Modifier
                .fillMaxSize()
                .clickable(enabled = false) {},
            cornerRadius = 16.dp
        ) {
            // Traffic light dots + title
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(Modifier.size(8.dp).background(DeepEyeColors.TrafficRed, CircleShape))
                    Box(Modifier.size(8.dp).background(DeepEyeColors.TrafficYellow, CircleShape))
                    Box(Modifier.size(8.dp).background(DeepEyeColors.TrafficGreen, CircleShape))
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    "TERMINAL OUTPUT",
                    color = DeepEyeColors.TextTertiary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = onClose, modifier = Modifier.size(24.dp)) {
                    Text("✕", color = DeepEyeColors.Tier3Red, fontSize = 14.sp)
                }
            }

            HorizontalDivider(
                color = Color.White.copy(alpha = 0.10f),
                modifier = Modifier.padding(vertical = 8.dp)
            )

            // Log entries
            androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                items(logs.size) { i ->
                    val log = logs[i]
                    val color = when (log.type) {
                        "ERROR" -> DeepEyeColors.Tier3Red
                        "SUCCESS" -> DeepEyeColors.TerminalGreen
                        "WARNING" -> DeepEyeColors.TerminalYellow
                        else -> DeepEyeColors.TerminalInfo
                    }
                    val prefix = when (log.type) {
                        "ERROR" -> "[error]"
                        "SUCCESS" -> "[success]"
                        "WARNING" -> "[warning]"
                        else -> "[info]"
                    }
                    Text(
                        text = "$prefix ${log.message}",
                        color = color,
                        fontSize = 11.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(vertical = 1.dp)
                    )
                }
            }
        }
    }
}
