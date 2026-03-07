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
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.text.font.FontWeight
import com.deepeye.otg.usb.OemCompatibilityLayer
import com.deepeye.otg.usb.OemType

@Composable
fun OemWarningBanner() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val oem = OemCompatibilityLayer.currentOem
    val isMIUIOptimized = remember(oem) { OemCompatibilityLayer.isBatteryOptimized(context) }
    
    val warning = when {
        oem == OemType.XIAOMI && isMIUIOptimized -> "⚠ MIUI Battery Saver: Set to 'No Restrictions' for stable USB."
        oem == OemType.XIAOMI -> "⚠ MIUI: Keep screen ON during USB operations."
        oem == OemType.VIVO -> "⚠ Vivo: Enable OTG in Settings > Additional Settings"
        oem == OemType.HUAWEI || oem == OemType.HONOR -> "⚠ Huawei/Honor: USB connection may need 3 attempts"
        oem == OemType.OPPO || oem == OemType.REALME -> "⚠ ColorOS: Keep app in foreground for USB permission"
        else -> null
    } ?: return

    var dismissed by remember { mutableStateOf(false) }
    if (!dismissed) {
        GlassCard(
            hazeState = null,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(warning, fontSize = 11.sp, color = Color(0xFFF59E0B))
                    if (oem == OemType.XIAOMI && isMIUIOptimized) {
                        TextButton(
                            onClick = {
                                val intent = Intent(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                    data = android.net.Uri.parse("package:${context.packageName}")
                                }
                                context.startActivity(intent)
                            },
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier.height(24.dp)
                        ) {
                            Text("FIX NOW", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Cyan)
                        }
                    }
                }
                IconButton(onClick = { dismissed = true }) {
                    Icon(Icons.Default.Close, null, tint = Color(0xFFF59E0B), modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}
