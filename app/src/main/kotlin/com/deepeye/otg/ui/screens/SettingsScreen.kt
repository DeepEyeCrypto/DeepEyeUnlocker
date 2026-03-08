package com.deepeye.otg.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deepeye.otg.BuildConfig
import com.deepeye.otg.ui.theme.GlassTokens
import com.deepeye.otg.viewmodel.UsbViewModel

@Composable
fun SettingsScreen(viewModel: UsbViewModel) {
    val perfMode by viewModel.performanceMode.collectAsState()
    val licenseStatus by viewModel.licenseStatus.collectAsState()
    val activeLicense by viewModel.currentLicense.collectAsState()
    
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(scrollState)
    ) {
        Spacer(modifier = Modifier.height(100.dp)) // Headroom for TopBar
        
        Text(
            "SETTINGS",
            fontSize = 28.sp,
            fontWeight = FontWeight.Black,
            color = Color.White,
            letterSpacing = 1.sp
        )
        
        Spacer(modifier = Modifier.height(24.dp))

        // ── System Configuration ────────────────────────────────
        SettingsSection(title = "SYSTEM CONFIGURATION") {
            SettingsToggle(
                title = "High-Performance Mode",
                subtitle = "Enables advanced GPU rasterization and Haze blurring.",
                checked = !perfMode, // Toggle logic is reversed in ViewModel (togglePerformance)
                onCheckedChange = { viewModel.togglePerformance() }
            )
            
            HorizontalDivider(color = Color.White.copy(alpha = 0.05f), modifier = Modifier.padding(vertical = 12.dp))
            
            SettingsToggle(
                title = "Deep Log Collection",
                subtitle = "Verbose logging for JNI bridge and USB transport.",
                checked = true,
                onCheckedChange = {}
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ── Authentication & License ──────────────────────────
        SettingsSection(title = "IDENTITY & LICENSING") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "License Status",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        licenseStatus.name,
                        color = if (licenseStatus == com.deepeye.otg.domain.models.LicenseStatus.ACTIVE) Color.Cyan else Color.Gray,
                        fontSize = 12.sp
                    )
                }
                
                if (licenseStatus != com.deepeye.otg.domain.models.LicenseStatus.ACTIVE) {
                    com.deepeye.otg.ui.components.GlassButton(
                        label = "ACTIVATE",
                        onClick = { viewModel.setActivationVisibility(true) },
                        modifier = Modifier.width(100.dp),
                        accent = true
                    )
                }
            }
            
            if (activeLicense != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.2f))
                        .padding(12.dp)
                ) {
                    Text("ACTIVE TIER", color = Color.Gray, fontSize = 9.sp)
                    Text(activeLicense?.tier?.name ?: "UNKNOWN", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text("LICENSE KEY", color = Color.Gray, fontSize = 9.sp)
                    Text(activeLicense?.key?.chunked(4)?.joinToString("-") ?: "N/A", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ── About ───────────────────────────────────────────────
        SettingsSection(title = "ABOUT DEEPEYE") {
            InfoRow("Version", "v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
            InfoRow("Engine", "Native Core v2026.18")
            InfoRow("Architecture", "arm64-v8a / armeabi-v7a")
            InfoRow("Build Date", "2026-03-08")
        }

        Spacer(modifier = Modifier.height(120.dp)) // Space for Bottom nav
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(GlassTokens.GlassSurface)
            .border(1.dp, GlassTokens.cardBorderColor, RoundedCornerShape(20.dp))
            .padding(20.dp)
    ) {
        Text(
            title,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Cyan.copy(alpha = 0.6f),
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(16.dp))
        content()
    }
}

@Composable
private fun SettingsToggle(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = Color.Gray, fontSize = 11.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.Cyan,
                checkedTrackColor = Color.Cyan.copy(alpha = 0.3f),
                uncheckedThumbColor = Color.Gray,
                uncheckedTrackColor = Color.Transparent,
                uncheckedBorderColor = Color.Gray.copy(alpha = 0.5f)
            )
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color.Gray, fontSize = 12.sp)
        Text(value, color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
    }
}
