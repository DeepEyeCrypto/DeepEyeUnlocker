package com.deepeye.otg.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deepeye.otg.usb.OemCompatibilityLayer
import com.deepeye.otg.usb.OemType

@Composable
fun OemWarningBanner() {
    val oem = OemCompatibilityLayer.currentOem
    val warning = when (oem) {
        OemType.VIVO -> "⚠ Vivo: Enable OTG in Settings > Additional Settings"
        OemType.XIAOMI -> "⚠ MIUI: Keep screen ON during USB operations"
        OemType.HUAWEI, OemType.HONOR -> "⚠ Huawei/Honor: USB connection may need 3 attempts"
        OemType.OPPO, OemType.REALME -> "⚠ ColorOS: Keep app in foreground for USB permission"
        else -> null
    } ?: return

    var dismissed by remember { mutableStateOf(false) }
    if (!dismissed) {
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            borderColor = Color(0xFFF59E0B)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(warning, fontSize = 11.sp, color = Color(0xFFF59E0B), modifier = Modifier.weight(1f))
                IconButton(onClick = { dismissed = true }) {
                    Icon(Icons.Default.Close, null, tint = Color(0xFFF59E0B), modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}
