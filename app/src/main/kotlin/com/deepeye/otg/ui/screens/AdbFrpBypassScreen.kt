package com.deepeye.otg.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.deepeye.otg.ui.theme.DeepEyeColors
import com.deepeye.otg.viewmodel.AdbBypassMethod
import com.deepeye.otg.viewmodel.AdbFrpBypassViewModel
import com.deepeye.otg.viewmodel.Risk

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdbFrpBypassScreen(
    viewModel: AdbFrpBypassViewModel = hiltViewModel(),
    deviceModel: String = "RMX3845",
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(deviceModel) {
        viewModel.setDeviceModel(deviceModel)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ADB FRP Bypass") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.clearLog() }) {
                        Icon(Icons.Default.Delete, "Clear Log")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 80.dp)
        ) {
            // Device Info Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = DeepEyeColors.Surface
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Default.PhoneAndroid,
                            contentDescription = null,
                            tint = DeepEyeColors.GoldAccent,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                "Target: $deviceModel",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Connection: ${uiState.connectionType} (ADB)",
                                style = MaterialTheme.typography.bodySmall,
                                color = DeepEyeColors.TextMuted
                            )
                        }
                    }
                }
            }

            // Run All Button
            Button(
                onClick = { viewModel.runAllMethods() },
                enabled = !uiState.isRunning,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DeepEyeColors.GoldAccent
                )
            ) {
                if (uiState.isRunning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Running...")
                } else {
                    Icon(Icons.Default.PlayArrow, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Run All Methods")
                }
            }

            Spacer(Modifier.height(16.dp))

            // Methods List
            Text(
                "Bypass Methods",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(viewModel.bypassMethods) { method ->
                    MethodCard(
                        method = method,
                        isRunning = uiState.isRunning,
                        isCurrentMethod = uiState.currentMethod == method.id,
                        onClick = { viewModel.runMethod(method.id) }
                    )
                }
            }

            // Execution Log
            if (uiState.log.isNotEmpty()) {
                Text(
                    "Execution Log",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 200.dp, max = 400.dp)
                        .padding(16.dp),
                    color = Color.Black,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    LazyColumn(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        val logLines = uiState.log.split("\n")
                        items(logLines) { line ->
                            Text(
                                text = line,
                                color = when {
                                    line.contains("✅") || line.contains("✓") -> Color(0xFF4CAF50)
                                    line.contains("❌") || line.contains("✗") -> Color(0xFFFF5252)
                                    line.contains("▶") -> Color(0xFF2196F3)
                                    line.contains("━") -> Color(0xFFFF9800)
                                    else -> Color.LightGray
                                },
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            // Last Result Summary
            uiState.lastResult?.let { result ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (result.success)
                            Color(0xFF4CAF50).copy(alpha = 0.1f)
                        else
                            Color(0xFFFF5252).copy(alpha = 0.1f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = if (result.success) "✅ SUCCESS" else "❌ FAILED",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (result.success) Color(0xFF4CAF50) else Color(0xFFFF5252)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Method: ${result.method}",
                            style = MaterialTheme.typography.bodySmall,
                            color = DeepEyeColors.TextMuted
                        )
                        Text(
                            "Success Rate: ${(result.successRate * 100).toInt()}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = DeepEyeColors.TextMuted
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MethodCard(
    method: AdbBypassMethod,
    isRunning: Boolean,
    isCurrentMethod: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        enabled = !isRunning,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentMethod)
                DeepEyeColors.GoldAccent.copy(alpha = 0.1f)
            else
                DeepEyeColors.Surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon based on risk
            Icon(
                imageVector = when (method.risk) {
                    Risk.LOW -> Icons.Default.CheckCircle
                    Risk.MEDIUM -> Icons.Default.Warning
                    Risk.HIGH -> Icons.Default.Error
                },
                contentDescription = null,
                tint = when (method.risk) {
                    Risk.LOW -> Color(0xFF4CAF50)
                    Risk.MEDIUM -> Color(0xFFFF9800)
                    Risk.HIGH -> Color(0xFFFF5252)
                },
                modifier = Modifier.size(24.dp)
            )

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        method.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        method.successRate,
                        style = MaterialTheme.typography.labelSmall,
                        color = DeepEyeColors.GoldAccent,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    method.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = DeepEyeColors.TextMuted
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (method.requiresRoot) "⚠️ Root Required" else "✓ No Root",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (method.requiresRoot) Color(0xFFFF9800) else Color(0xFF4CAF50)
                    )
                }
            }

            if (isCurrentMethod && isRunning) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
            }
        }
    }
}
