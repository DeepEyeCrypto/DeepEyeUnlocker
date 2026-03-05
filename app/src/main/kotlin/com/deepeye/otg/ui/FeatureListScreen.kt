package com.deepeye.otg.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deepeye.otg.usb.DeepEyeOperation

// ═══════════════════════════════════════════════════════════════════
//  Feature List v2 — Liquid Glass
//  - Frosted pill category headers with purple accent text
//  - Glass cards with gradient border effect
//  - Gradient RUN buttons with press animation
//  - 6 groups (A–F) matching Stitch screen3_main_v2
// ═══════════════════════════════════════════════════════════════════

@Composable
fun FeatureListScreen(
    onOperationSelected: (DeepEyeOperation) -> Unit
) {
    val ops = DeepEyeOperation.entries

    val categories = listOf(
        "Group A Unlock Operations" to listOf(
            ops.find { it.name == "UNLOCK_BOOTLOADER" },
            ops.find { it.name == "ERASE_FRP" },
            ops.find { it.name == "FACTORY_RESET" },
            ops.find { it.name == "DEMO_UNLOCK" }
        ).filterNotNull(),

        "Group B Security Repair" to listOf(
            ops.find { it.name == "REMOVE_SCREEN_LOCK" },
            ops.find { it.name == "REMOVE_MI_CLOUD" },
            ops.find { it.name == "LOCK_STATE_ANALYSIS" },
            ops.find { it.name == "SAFE_WIPE" }
        ).filterNotNull(),

        "Group C FRP & Account" to listOf(
            ops.find { it.name == "EFRP_MDM_HOOK" },
            ops.find { it.name == "MTK_METAMODE_FRP" },
            ops.find { it.name == "MDM_REMOVE" },
            ops.find { it.name == "NETWORK_UNLOCK" }
        ).filterNotNull(),

        "Group D Firmware & Partitions" to listOf(
            ops.find { it.name == "WRITE_FIRMWARE" },
            ops.find { it.name == "READ_FIRMWARE" },
            ops.find { it.name == "PARTITION_MANAGER" },
            ops.find { it.name == "BACKUP_EFS" },
            ops.find { it.name == "RESTORE_EFS" }
        ).filterNotNull(),

        "Group E IMEI & Network" to listOf(
            ops.find { it.name == "IMEI_CHECK" },
            ops.find { it.name == "IMEI_RESTORE" },
            ops.find { it.name == "MODEM_REPAIR" }
        ).filterNotNull(),

        "Group F Advanced & Diagnostics" to listOf(
            ops.find { it.name == "DEEP_DEVICE_INFO" },
            ops.find { it.name == "ADB_ENABLE" },
            ops.find { it.name == "ONE_CLICK_ROOT" },
            ops.find { it.name == "APP_MANAGER" }
        ).filterNotNull()
    )

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        categories.forEach { (title, ops) ->
            // ── Category pill header ──
            item(span = { GridItemSpan(2) }) {
                CategoryPillHeader(title)
            }
            // ── Operation glass cards ──
            items(
                items = ops,
                key = { op -> op.name }
            ) { op ->
                LiquidOperationCard(op) { onOperationSelected(op) }
            }
        }

        // Bottom spacer for nav bar
        item(span = { GridItemSpan(2) }) {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
//  Category Pill Header — frosted pill with purple accent
//  Stitch: bg-primary-glow/10 backdrop-blur rounded-full
// ═══════════════════════════════════════════════════════════════════

@Composable
private fun CategoryPillHeader(title: String) {
    Box(modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)) {
        Surface(
            color = DeepEyeColors.PrimaryGlow.copy(alpha = 0.10f),
            shape = RoundedCornerShape(50),
            modifier = Modifier.border(
                1.dp,
                DeepEyeColors.PrimaryGlow.copy(alpha = 0.20f),
                RoundedCornerShape(50)
            )
        ) {
            Text(
                text = title.uppercase(),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                color = DeepEyeColors.AccentPurple,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
//  Liquid Operation Card — glass card with gradient border
//  140dp min height, icon + tier badge + label + gradient RUN
// ═══════════════════════════════════════════════════════════════════

@Composable
private fun LiquidOperationCard(
    op: DeepEyeOperation,
    onClick: () -> Unit
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp),
        cornerRadius = 20.dp
    ) {
        // Top row: icon + tier badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Text(
                operationIcon(op),
                fontSize = 22.sp,
                color = DeepEyeColors.AccentPurple
            )
            OperationTierBadge(op.tier)
        }

        Spacer(modifier = Modifier.weight(1f))

        // Label
        Text(
            text = op.label,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            lineHeight = 18.sp,
            maxLines = 2
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Gradient RUN button
        GradientRunButton(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// ═══════════════════════════════════════════════════════════════════
//  Icon mapping
// ═══════════════════════════════════════════════════════════════════

private fun operationIcon(op: DeepEyeOperation): String = when (op) {
    DeepEyeOperation.UNLOCK_BOOTLOADER -> "🔓"
    DeepEyeOperation.ERASE_FRP -> "🗑️"
    DeepEyeOperation.FACTORY_RESET -> "🔄"
    DeepEyeOperation.DEMO_UNLOCK -> "🏪"
    DeepEyeOperation.REMOVE_SCREEN_LOCK -> "📱"
    DeepEyeOperation.REMOVE_MI_CLOUD -> "☁️"
    DeepEyeOperation.LOCK_STATE_ANALYSIS -> "🔑"
    DeepEyeOperation.SAFE_WIPE -> "🛡️"
    DeepEyeOperation.EFRP_MDM_HOOK -> "🏢"
    DeepEyeOperation.MTK_METAMODE_FRP -> "⚙️"
    DeepEyeOperation.MDM_REMOVE -> "💼"
    DeepEyeOperation.NETWORK_UNLOCK -> "📶"
    DeepEyeOperation.WRITE_FIRMWARE -> "⬇️"
    DeepEyeOperation.READ_FIRMWARE -> "💾"
    DeepEyeOperation.PARTITION_MANAGER -> "📊"
    DeepEyeOperation.BACKUP_EFS -> "📁"
    DeepEyeOperation.RESTORE_EFS -> "📂"
    DeepEyeOperation.IMEI_CHECK -> "✅"
    DeepEyeOperation.IMEI_RESTORE -> "📋"
    DeepEyeOperation.MODEM_REPAIR -> "📡"
    DeepEyeOperation.DEEP_DEVICE_INFO -> "ℹ️"
    DeepEyeOperation.ADB_ENABLE -> "🐛"
    DeepEyeOperation.ONE_CLICK_ROOT -> "💻"
    DeepEyeOperation.APP_MANAGER -> "📲"
}
