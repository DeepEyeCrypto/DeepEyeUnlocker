package com.deepeye.otg.ui.device

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.deepeye.otg.device.*
import com.deepeye.otg.ui.components.ProtocolTerminal
import com.deepeye.otg.viewmodel.DeviceViewModel
import com.deepeye.otg.viewmodel.ProtocolLog

// ═══════════════════════════════════════════════════════════════
//  DEVICE DASHBOARD — full protocol console + device info
// ═══════════════════════════════════════════════════════════════

@Composable
fun DeviceDashboardScreen(
    deviceViewModel: DeviceViewModel = viewModel(),
    onNavigateToXiaomiFlash: (() -> Unit)? = null,
    onNavigateToMtkUnlock: (() -> Unit)? = null
) {
    val devices    by deviceViewModel.devices.collectAsStateWithLifecycle()
    val active     by deviceViewModel.activeDevice.collectAsStateWithLifecycle()
    val deviceInfo by deviceViewModel.deviceInfo.collectAsStateWithLifecycle()
    val chipInfo   by deviceViewModel.chipInfo.collectAsStateWithLifecycle()
    val logs       by deviceViewModel.logs.collectAsStateWithLifecycle()
    val progress   by deviceViewModel.flashProgress.collectAsStateWithLifecycle()
    val connecting by deviceViewModel.isConnecting.collectAsStateWithLifecycle()

    var activeTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("DEVICE", "TERMINAL", "TESTPOINT")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF06060F), Color(0xFF0A0A14))
                )
            )
    ) {
        // ── Header ──────────────────────────────────────────────
        ConnectionStatusBanner(device = active, chipInfo = chipInfo, isConnecting = connecting)

        // ── Tab Bar ─────────────────────────────────────────────
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            tabs.forEachIndexed { i, tab ->
                val isActive = activeTab == i
                val tabColor = when (i) {
                    0 -> Color(0xFF39FF14)
                    1 -> Color(0xFF00FFFF)
                    2 -> Color(0xFFA78BFA)
                    else -> Color.White
                }
                Box(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isActive) tabColor.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.03f))
                        .border(
                            0.5.dp,
                            if (isActive) tabColor.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.06f),
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { activeTab = i }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        tab,
                        color = if (isActive) tabColor else Color.White.copy(alpha = 0.35f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }
        }

        // ── Content ──────────────────────────────────────────────
        when (activeTab) {
            0 -> DeviceInfoTab(
                device = active,
                deviceInfo = deviceInfo,
                chipInfo = chipInfo,
                progress = progress,
                vm = deviceViewModel,
                onNavigateToXiaomiFlash = onNavigateToXiaomiFlash,
                onNavigateToMtkUnlock = onNavigateToMtkUnlock
            )
            1 -> ProtocolTerminalTab(
                logs = logs,
                device = active,
                deviceInfo = deviceInfo,
                vm = deviceViewModel
            )
            2 -> TestpointTab(
                deviceInfo = deviceInfo,
                chipInfo = chipInfo,
                vm = deviceViewModel
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  CONNECTION STATUS BANNER
// ═══════════════════════════════════════════════════════════════

@Composable
private fun ConnectionStatusBanner(
    device: DetectedDevice?,
    chipInfo: MtkChipInfo?,
    isConnecting: Boolean
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse),
        label = "pulse"
    )

    val (color, label, icon) = when (device?.mode) {
        DeviceMode.BROM       -> Triple(Color(0xFFA78BFA), "MTK BROM", "⚡")
        DeviceMode.PRELOADER  -> Triple(Color(0xFF00FFFF), "PRELOADER", "💫")
        DeviceMode.EDL        -> Triple(Color(0xFFFF007F), "QUALCOMM EDL 9008", "🔴")
        DeviceMode.FASTBOOT   -> Triple(Color(0xFFFB923C), "FASTBOOT", "🔓")
        DeviceMode.ADB        -> Triple(Color(0xFF39FF14), "ADB CONNECTED", "📱")
        DeviceMode.MTP        -> Triple(Color(0xFFEAB308), "MTP", "📁")
        DeviceMode.RECOVERY   -> Triple(Color(0xFF00FFFF), "RECOVERY", "♻️")
        else                  -> Triple(Color.White.copy(alpha = 0.2f), "NO DEVICE", "🔌")
    }

    Box(
        Modifier
            .fillMaxWidth()
            .padding(10.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(color.copy(alpha = 0.06f))
            .border(1.dp, color.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Pulsing dot
                Box(
                    Modifier
                        .size(10.dp)
                        .background(
                            if (device != null) color.copy(alpha = pulseAlpha) else Color.White.copy(alpha = 0.1f),
                            CircleShape
                        )
                )
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(icon, fontSize = 14.sp)
                        Text(
                            label,
                            color = color,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.5.sp
                        )
                    }
                    if (device != null) {
                        Text(
                            buildString {
                                append("VID:${"%04X".format(device.vid)} PID:${"%04X".format(device.pid)}")
                                device.productName?.let { append(" • $it") }
                                chipInfo?.let { append(" • ${it.chipName}") }
                            },
                            color = Color.White.copy(alpha = 0.4f),
                            fontSize = 9.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else {
                        Text(
                            "Connect device via USB OTG cable",
                            color = Color.White.copy(alpha = 0.25f),
                            fontSize = 9.sp
                        )
                    }
                }
            }
            if (isConnecting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = color,
                    strokeWidth = 2.dp
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  TAB 1: DEVICE INFO
// ═══════════════════════════════════════════════════════════════

@Composable
private fun DeviceInfoTab(
    device: DetectedDevice?,
    deviceInfo: AdbDeviceInfo?,
    chipInfo: MtkChipInfo?,
    progress: FlashProgress?,
    vm: DeviceViewModel,
    onNavigateToXiaomiFlash: (() -> Unit)? = null,
    onNavigateToMtkUnlock: (() -> Unit)? = null
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        // Quick Actions
        item(key = "actions") {
            QuickActionsSection(
                device = device,
                vm = vm,
                onNavigateToXiaomiFlash = onNavigateToXiaomiFlash,
                onNavigateToMtkUnlock = onNavigateToMtkUnlock
            )
        }

        // ADB Device Info Grid
        if (deviceInfo != null) {
            item(key = "info_grid") {
                DeviceInfoGrid(info = deviceInfo)
            }
        }

        // MTK Chip Info
        if (chipInfo != null) {
            item(key = "chip_info") {
                ChipInfoCard(chip = chipInfo)
            }
        }

        // Flash Progress
        if (progress != null) {
            item(key = "flash_progress") {
                FlashProgressCard(progress = progress)
            }
        }

        // Connected devices list
        if (device == null) {
            item(key = "empty") {
                EmptyDeviceState()
            }
        }
    }
}

@Composable
private fun QuickActionsSection(
    device: DetectedDevice?,
    vm: DeviceViewModel,
    onNavigateToXiaomiFlash: (() -> Unit)? = null,
    onNavigateToMtkUnlock: (() -> Unit)? = null
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            "QUICK ACTIONS",
            color = Color.White.copy(alpha = 0.35f),
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp
        )
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ActionChip("🔍 Scan", Color(0xFF7C3AED)) { vm.scanDevices() }

            if (device?.mode == DeviceMode.BROM) {
                ActionChip("⚡ BROM", Color(0xFFA78BFA)) { vm.connectBrom() }
            }
            if (device?.mode == DeviceMode.EDL) {
                ActionChip("🔴 EDL", Color(0xFFFF007F)) { vm.connectEdl() }
            }
            if (device?.mode == DeviceMode.ADB) {
                ActionChip("↺ Reboot", Color(0xFF39FF14)) {
                    device.serial?.let { vm.adbReboot(it, "system") }
                }
                ActionChip("⚡ Bootloader", Color(0xFFFB923C)) {
                    device.serial?.let { vm.adbReboot(it, "bootloader") }
                }
                ActionChip("⚙ Recovery", Color(0xFF00FFFF)) {
                    device.serial?.let { vm.adbReboot(it, "recovery") }
                }
                ActionChip("🔴 EDL", Color(0xFFFF007F)) {
                    device.serial?.let { vm.adbReboot(it, "edl") }
                }
            }
            if (device?.mode == DeviceMode.FASTBOOT) {
                ActionChip("↺ Reboot", Color(0xFF39FF14)) {
                    device.serial?.let { vm.fastbootFlash(it, "", "") } // reboot only
                }
            }
            
            // Xiaomi Flash Tool - Always visible
            if (onNavigateToXiaomiFlash != null) {
                ActionChip("🔥 Xiaomi Flash", Color(0xFFFF6B35)) {
                    onNavigateToXiaomiFlash()
                }
            }
            
            // MTK Unlock Tool - Always visible
            if (onNavigateToMtkUnlock != null) {
                ActionChip("🔧 MTK Unlock", Color(0xFF00BCD4)) {
                    onNavigateToMtkUnlock()
                }
            }
        }
    }
}

@Composable
private fun ActionChip(label: String, color: Color, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.12f))
            .border(0.5.dp, color.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp)
    ) {
        Text(label, color = color, fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun DeviceInfoGrid(info: AdbDeviceInfo) {
    val items = listOf(
        Triple("📱", "Model", info.model),
        Triple("🏷️", "Brand", info.brand),
        Triple("🤖", "Android", "${info.android} (SDK ${info.sdk})"),
        Triple("🔧", "Chipset", info.chipset),
        Triple("🔢", "Serial", info.serialNo),
        Triple("📡", "IMEI", info.imei.ifBlank { "N/A" }),
        Triple("🛡️", "Security", info.securityPatch),
        Triple("🔓", "FRP", info.frpStatus),
        Triple("🔒", "Bootloader", info.bootloaderStatus),
        Triple("📦", "A/B Slot", info.abPartition.ifBlank { "N/A" }),
    )

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            "DEVICE INFO",
            color = Color.White.copy(alpha = 0.35f),
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp
        )

        items.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { (emoji, label, value) ->
                    val isFrp = label == "FRP"
                    val isFrpEnabled = isFrp && value.contains("ENABLED", ignoreCase = true)
                    val valueColor = if (isFrpEnabled)
                        Color(0xFFFF4444) else Color.White.copy(alpha = 0.8f)
                    val borderColor = if (isFrpEnabled)
                        Color(0xFFFF4444).copy(alpha = 0.4f) else Color.White.copy(alpha = 0.07f)

                    Box(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White.copy(alpha = 0.04f))
                            .border(0.5.dp, borderColor, RoundedCornerShape(10.dp))
                            .padding(10.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(emoji, fontSize = 10.sp)
                                Text(
                                    label,
                                    color = Color.White.copy(alpha = 0.35f),
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Text(
                                value.take(28),
                                color = valueColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ChipInfoCard(chip: MtkChipInfo) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFA78BFA).copy(alpha = 0.06f))
            .border(0.5.dp, Color(0xFFA78BFA).copy(alpha = 0.25f), RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                "MTK CHIP INFO",
                color = Color(0xFFA78BFA),
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column {
                    Text("Chip", color = Color.White.copy(alpha = 0.35f), fontSize = 8.sp)
                    Text(chip.chipName, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Column {
                    Text("HW Code", color = Color.White.copy(alpha = 0.35f), fontSize = 8.sp)
                    Text(
                        "0x${chip.hwCode.toString(16).uppercase()}",
                        color = Color(0xFFA78BFA),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Column {
                    Text("Arch", color = Color.White.copy(alpha = 0.35f), fontSize = 8.sp)
                    Text(chip.arch, color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun FlashProgressCard(progress: FlashProgress) {
    val animatedPercent by animateIntAsState(
        targetValue = progress.percent,
        animationSpec = tween(300),
        label = "flash_pct"
    )

    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFFB923C).copy(alpha = 0.06f))
            .border(0.5.dp, Color(0xFFFB923C).copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        CircularProgressIndicator(
            progress = { animatedPercent / 100f },
            modifier = Modifier.size(36.dp),
            color = Color(0xFFFB923C),
            strokeWidth = 3.dp
        )
        Column(Modifier.weight(1f)) {
            Text(
                "FLASHING: ${progress.partition}",
                color = Color(0xFFFB923C),
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
            Text(
                "$animatedPercent% — ${progress.written / 1024}KB / ${progress.total / 1024}KB",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun EmptyDeviceState() {
    val infiniteTransition = rememberInfiniteTransition(label = "empty_pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 0.7f,
        animationSpec = infiniteRepeatable(tween(1500), RepeatMode.Reverse),
        label = "pulse_alpha"
    )

    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            Modifier
                .size(80.dp)
                .background(Color(0xFFA78BFA).copy(alpha = 0.1f * alpha), CircleShape)
                .border(1.dp, Color(0xFFA78BFA).copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Usb,
                contentDescription = null,
                tint = Color(0xFFA78BFA).copy(alpha = alpha),
                modifier = Modifier.size(36.dp)
            )
        }
        Text(
            "Connect a device via USB OTG",
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
        Text(
            "Supports MTK BROM • Qualcomm EDL • ADB • Fastboot",
            color = Color.White.copy(alpha = 0.25f),
            fontSize = 10.sp
        )
    }
}

// ═══════════════════════════════════════════════════════════════
//  TAB 2: PROTOCOL TERMINAL
// ═══════════════════════════════════════════════════════════════

@Composable
private fun ProtocolTerminalTab(
    logs: List<ProtocolLog>,
    device: DetectedDevice?,
    deviceInfo: AdbDeviceInfo?,
    vm: DeviceViewModel
) {
    var shellInput by remember { mutableStateOf("") }
    val quickCmds = listOf("getprop", "id", "pm list packages", "df -h", "cat /proc/cpuinfo", "ls /sdcard")

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Quick command chips (only for ADB)
        if (device?.mode == DeviceMode.ADB) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                quickCmds.forEach { cmd ->
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Color(0xFF39FF14).copy(alpha = 0.08f))
                            .border(0.5.dp, Color(0xFF39FF14).copy(alpha = 0.3f), RoundedCornerShape(50))
                            .clickable { shellInput = cmd }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(cmd, color = Color(0xFF39FF14), fontSize = 8.sp)
                    }
                }
            }

            // Shell input
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "$ ",
                    color = Color(0xFF39FF14).copy(alpha = 0.6f),
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
                BasicTextField(
                    value = shellInput,
                    onValueChange = { shellInput = it },
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .border(0.5.dp, Color(0xFF39FF14).copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    textStyle = TextStyle(
                        color = Color(0xFF39FF14),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    ),
                    singleLine = true,
                    cursorBrush = SolidColor(Color(0xFF39FF14))
                )
                Box(
                    Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF39FF14).copy(alpha = 0.2f))
                        .border(0.5.dp, Color(0xFF39FF14).copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .clickable {
                            if (device.serial != null && shellInput.isNotBlank()) {
                                vm.adbShell(device.serial!!, shellInput) { }
                                shellInput = ""
                            }
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        "RUN",
                        color = Color(0xFF39FF14),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Protocol action buttons for non-ADB
        if (device?.mode == DeviceMode.BROM) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ActionChip("⚡ Connect BROM", Color(0xFFA78BFA)) { vm.connectBrom() }
            }
        }
        if (device?.mode == DeviceMode.EDL) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ActionChip("🔴 Connect EDL", Color(0xFFFF007F)) { vm.connectEdl() }
            }
        }

        // Log terminal
        ProtocolTerminal(
            logs = logs,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )

        // Clear button
        Box(
            Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            Box(
                Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .border(0.5.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                    .clickable { vm.clearLogs() }
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text("CLEAR LOG", color = Color.White.copy(alpha = 0.3f), fontSize = 8.sp)
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  TAB 3: TESTPOINT GUIDE
// ═══════════════════════════════════════════════════════════════

@Composable
private fun TestpointTab(
    deviceInfo: AdbDeviceInfo?,
    chipInfo: MtkChipInfo?,
    vm: DeviceViewModel
) {
    val model = deviceInfo?.model ?: chipInfo?.chipName ?: ""
    val chipset = deviceInfo?.chipset ?: chipInfo?.chipName ?: ""

    val guide = remember(model, chipset) {
        if (model.isNotBlank() || chipset.isNotBlank()) {
            vm.getTestpointGuide(model, chipset)
        } else null
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        if (guide != null) {
            item(key = "guide_header") {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "📍 TESTPOINT GUIDE",
                        color = Color(0xFFA78BFA),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                    DifficultyIndicator(guide.difficulty)
                }
            }

            item(key = "guide_meta") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    InfoChip("Chipset", guide.chipset, Color(0xFFA78BFA))
                    InfoChip("Method", guide.method, Color(0xFF00FFFF))
                    InfoChip("VID:PID", "${"%04X".format(guide.bromVid)}:${"%04X".format(guide.bromPid)}", Color(0xFFFB923C))
                }
            }

            guide.warning?.let { warning ->
                item(key = "guide_warning") {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFFF4444).copy(alpha = 0.08f))
                            .border(0.5.dp, Color(0xFFFF4444).copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("⚠️", fontSize = 12.sp)
                        Text(
                            warning,
                            color = Color(0xFFFF4444).copy(alpha = 0.9f),
                            fontSize = 9.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            items(guide.steps.size, key = { "step_$it" }) { i ->
                val step = guide.steps[i]
                if (step.isBlank()) return@items
                Row(
                    Modifier.padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFA78BFA).copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "${i + 1}",
                            color = Color(0xFFA78BFA),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                    Text(
                        step,
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 10.sp,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        } else {
            item(key = "no_guide") {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("📍", fontSize = 32.sp)
                    Text(
                        "Connect a device to see testpoint guide",
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 12.sp
                    )
                    Text(
                        "Guides available for MTK, Qualcomm, Samsung, Huawei",
                        color = Color.White.copy(alpha = 0.2f),
                        fontSize = 9.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun DifficultyIndicator(level: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        repeat(5) { i ->
            Box(
                Modifier
                    .size(width = 8.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        if (i < level) {
                            when {
                                level <= 2 -> Color(0xFF39FF14)
                                level <= 3 -> Color(0xFFEAB308)
                                else -> Color(0xFFFF4444)
                            }
                        } else Color.White.copy(alpha = 0.1f)
                    )
            )
        }
    }
}

@Composable
private fun InfoChip(label: String, value: String, color: Color) {
    Column(
        Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.06f))
            .border(0.5.dp, color.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(label, color = color.copy(alpha = 0.5f), fontSize = 7.sp)
        Text(
            value.take(16),
            color = color,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
