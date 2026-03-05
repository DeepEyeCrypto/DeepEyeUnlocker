package com.deepeye.otg.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deepeye.otg.usb.DeepEyeOperation

// ═══════════════════════════════════════════════════════════════════
//  Feature List — 6 groups matching Stitch design (A–F)
//  2-column grid with category headers, tier badges, RUN buttons
// ═══════════════════════════════════════════════════════════════════

@Composable
fun FeatureListScreen(
    onOperationSelected: (DeepEyeOperation) -> Unit
) {
    val ops = DeepEyeOperation.entries

    // Groups matching the Stitch design exactly
    val categories = listOf(
        "GROUP A UNLOCK OPERATIONS" to listOf(
            ops.find { it.name == "UNLOCK_BOOTLOADER" },
            ops.find { it.name == "ERASE_FRP" },
            ops.find { it.name == "FACTORY_RESET" },
            ops.find { it.name == "DEMO_UNLOCK" }
        ).filterNotNull(),

        "GROUP B SECURITY REPAIR" to listOf(
            ops.find { it.name == "REMOVE_SCREEN_LOCK" },
            ops.find { it.name == "REMOVE_MI_CLOUD" },
            ops.find { it.name == "LOCK_STATE_ANALYSIS" },
            ops.find { it.name == "SAFE_WIPE" }
        ).filterNotNull(),

        "GROUP C FRP & ACCOUNT" to listOf(
            ops.find { it.name == "EFRP_MDM_HOOK" },
            ops.find { it.name == "MTK_METAMODE_FRP" },
            ops.find { it.name == "MDM_REMOVE" },
            ops.find { it.name == "NETWORK_UNLOCK" }
        ).filterNotNull(),

        "GROUP D FIRMWARE & PARTITIONS" to listOf(
            ops.find { it.name == "WRITE_FIRMWARE" },
            ops.find { it.name == "READ_FIRMWARE" },
            ops.find { it.name == "PARTITION_MANAGER" },
            ops.find { it.name == "BACKUP_EFS" },
            ops.find { it.name == "RESTORE_EFS" }
        ).filterNotNull(),

        "GROUP E IMEI & NETWORK" to listOf(
            ops.find { it.name == "IMEI_CHECK" },
            ops.find { it.name == "IMEI_RESTORE" },
            ops.find { it.name == "MODEM_REPAIR" }
        ).filterNotNull(),

        "GROUP F ADVANCED & DIAGNOSTICS" to listOf(
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
            // Section header — full width
            item(span = { GridItemSpan(2) }) {
                CategoryHeader(title)
            }
            // Operation cards
            items(
                items = ops,
                key = { op -> op.name }
            ) { op ->
                OperationCard(op) { onOperationSelected(op) }
            }
        }

        // Bottom spacer for navigation bar
        item(span = { GridItemSpan(2) }) {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
//  Category Header — bold text with bottom border (Stitch style)
// ═══════════════════════════════════════════════════════════════════

@Composable
private fun CategoryHeader(title: String) {
    Column(modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)) {
        Text(
            text = title,
            color = DeepEyeColors.TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        HorizontalDivider(
            thickness = 1.dp,
            color = DeepEyeColors.SurfaceVariant
        )
    }
}

// ═══════════════════════════════════════════════════════════════════
//  Operation Card — icon + tier badge + label + RUN button
//  Height: 128dp (matching Stitch h-32 = 8rem ≈ 128dp)
// ═══════════════════════════════════════════════════════════════════

@Composable
private fun OperationCard(
    op: DeepEyeOperation,
    onClick: () -> Unit
) {
    val icon = operationIcon(op)

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(128.dp)
    ) {
        // Top row: icon + tier badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Text(icon, fontSize = 20.sp, color = DeepEyeColors.Primary)
            OperationTierBadge(op.tier)
        }

        Spacer(modifier = Modifier.weight(1f))

        // Label
        Text(
            text = op.label,
            color = DeepEyeColors.TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            lineHeight = 18.sp,
            maxLines = 2
        )

        Spacer(modifier = Modifier.height(8.dp))

        // RUN button — full width
        PrimaryButton(
            text = "RUN",
            onClick = onClick,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// ═══════════════════════════════════════════════════════════════════
//  Icon mapping — emoji stand-ins for Material Symbols
//  (Stitch uses Material Symbols via web font; Compose uses emoji/icons)
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
