package com.deepeye.otg.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deepeye.otg.ui.theme.GlassTokens
import dev.chrisbanes.haze.HazeState
import org.json.JSONObject

@Composable
fun DeviceIdentityCard(
    metadataJson: String?,
    hazeState: HazeState,
    performanceMode: Boolean
) {
    val data = remember(metadataJson) {
        metadataJson?.let {
            try { JSONObject(it) } catch (e: Exception) { null }
        }
    }

    AnimatedVisibility(
        visible = data != null,
        enter = expandVertically(),
        exit = shrinkVertically()
    ) {
        GlassCard(
            hazeState = hazeState,
            performanceMode = performanceMode,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "DEEP IDENTITY SCANNED",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF6750A4),
                        letterSpacing = 1.sp
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF34D399).copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("VERIFIED", fontSize = 8.sp, color = Color(0xFF059669), fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                IdentityRow("MODEL", data?.optString("model", "Unknown") ?: "Unknown")
                IdentityRow("CHIPSET", data?.optString("soc", "Unknown") ?: "Unknown")
                IdentityRow("SERIAL", data?.optString("serial", "N/A") ?: "N/A")
                IdentityRow("SECURITY", "v${data?.optString("security_patch", "2024-01-01")}")
                IdentityRow("SYSTEM", "Android ${data?.optString("version", "13")}")
                
                if (data?.has("fused") == true) {
                    IdentityRow("SBC/DAA", if (data.optBoolean("fused")) "ENABLED (SECURE)" else "DISABLED")
                }
            }
        }
    }
}

@Composable
private fun IdentityRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
        Text(text = value, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
    }
}
