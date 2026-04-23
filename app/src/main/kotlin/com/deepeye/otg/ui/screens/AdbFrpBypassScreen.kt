package com.deepeye.otg.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import com.deepeye.otg.viewmodel.AdbFrpBypassViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdbFrpBypassScreen(
    viewModel: AdbFrpBypassViewModel = hiltViewModel(),
    deviceModel: String = "RMX3845",
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var serial by remember { mutableStateOf("") }

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

            // Serial Input
            OutlinedTextField(
                value = serial,
                onValueChange = { serial = it },
                label = { Text("Device Serial") },
                placeholder = { Text("e.g. R5CR80XXXXX") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = DeepEyeColors.GoldAccent,
                    unfocusedBorderColor = DeepEyeColors.BorderGlass
                )
            )

            Spacer(Modifier.height(12.dp))

            // Run Bypass Button
            Button(
                onClick = {
                    if (serial.isNotBlank()) {
                        viewModel.runBypass(serial)
                    }
                },
                enabled = !uiState.isRunning && serial.isNotBlank(),
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
                    Text("Run FRP Bypass")
                }
            }

            Spacer(Modifier.height(16.dp))

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
                    Column(
                        modifier = Modifier
                            .padding(12.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        val logLines = uiState.log.split("\n")
                        logLines.forEach { line ->
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
        }
    }
}
