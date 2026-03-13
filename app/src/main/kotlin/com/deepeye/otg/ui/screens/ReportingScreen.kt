package com.deepeye.otg.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deepeye.otg.ui.components.GlassButton
import com.deepeye.otg.ui.components.GlassCard
import com.deepeye.otg.ui.theme.GlassTokens
import dev.chrisbanes.haze.HazeState
import java.io.File

@Composable
fun ReportingScreen(
    reportFile: File?,
    viewModel: com.deepeye.otg.viewmodel.UsbViewModel
) {
    val hazeState = remember { HazeState() }
    val content = remember(reportFile) {
        if (reportFile?.extension == "pdf") {
            "OFFICIAL FORENSIC AUDIT TRAIL (PDF)\n\n" +
            "Location: ${reportFile.absolutePath}\n" +
            "Size: ${reportFile.length() / 1024} KB\n\n" +
            "The document has been digitally signed and is ready for legal handover.\n" +
            "Use 'SHARE REPORT' to export to secure storage."
        } else {
            reportFile?.readText() ?: "Error: Report file not found."
        }
    }

    var showPasswordDialog by remember { androidx.compose.runtime.mutableStateOf(false) }
    var password by remember { androidx.compose.runtime.mutableStateOf("") }

    if (showPasswordDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showPasswordDialog = false },
            title = { Text("Seal Forensic Vault") },
            text = {
                Column {
                    Text("Enter a password to encrypt this forensic report (AES-256).", fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    androidx.compose.material3.TextField(
                        value = password,
                        onValueChange = { password = it },
                        placeholder = { Text("Vault Password") },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation()
                    )
                }
            },
            confirmButton = {
                GlassButton(
                    label = "ENCRYPT",
                    onClick = {
                        reportFile?.let { viewModel.encryptToVault(it, password) }
                        showPasswordDialog = false
                    },
                    accent = true
                )
            },
            dismissButton = {
                GlassButton(label = "CANCEL", onClick = { showPasswordDialog = false }, accent = false)
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.8f))
            .padding(24.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "FORENSIC AUDIT REPORT",
                style = MaterialTheme.typography.titleLarge,
                color = if (reportFile?.extension == "deepvault") Color(0xFFFBBF24) else Color(0xFF4ADE80),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            GlassCard(
                hazeState = hazeState,
                modifier = Modifier.weight(1f).fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f))
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    Text(
                        text = if (reportFile?.extension == "deepvault") {
                            "🔐 ENCRYPTED FORENSIC VAULT\n\n" +
                            "File: ${reportFile.name}\n" +
                            "Type: DeepEye Secure Container (.deepvault)\n" +
                            "Encryption: AES-256-ZIP (Standard Forensic grade)\n\n" +
                            "This file is now locked and safe for transmission."
                        } else content,
                        color = Color.White.copy(0.8f),
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                GlassButton(
                    label = "BACK",
                    onClick = { viewModel.dismissReport() },
                    modifier = Modifier.weight(0.5f),
                    accent = false
                )
                
                if (reportFile?.extension != "deepvault") {
                    GlassButton(
                        label = "VAULT",
                        onClick = { showPasswordDialog = true },
                        modifier = Modifier.weight(1f),
                        accent = true
                    )
                } else {
                    GlassButton(
                        label = "CLOUD",
                        onClick = { viewModel.syncToCloud(reportFile) },
                        modifier = Modifier.weight(1f),
                        accent = true
                    )
                }
                
                GlassButton(
                    label = "SHARE",
                    onClick = { reportFile?.let { viewModel.shareReport(it) } },
                    modifier = Modifier.weight(1f),
                    accent = true
                )
            }
        }

        val syncStatus by viewModel.cloudSyncStatus.collectAsState()
        if (syncStatus.syncing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    androidx.compose.material3.CircularProgressIndicator(color = Color.Cyan)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "UPLOADING TO SECURE CLOUD...",
                        color = Color.Cyan,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        "${syncStatus.progress}%",
                        color = Color.White,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        } else if (syncStatus.result != null) {
            androidx.compose.material3.Snackbar(
                modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                containerColor = if (syncStatus.isError) Color.Red else Color(0xFF065F46)
            ) {
                Text(syncStatus.result ?: "", color = Color.White)
            }
        }
    }
}
