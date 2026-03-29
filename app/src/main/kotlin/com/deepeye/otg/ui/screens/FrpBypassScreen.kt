package com.deepeye.otg.ui.screens

import android.hardware.usb.UsbDevice
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.deepeye.otg.viewmodel.FrpViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FrpBypassScreen(
    viewModel: FrpViewModel,
    device: UsbDevice?,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var androidVersion by remember { mutableStateOf("10") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("FRP Bypass Orchestrator") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("←")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (device == null) {
                Text("No device connected", color = Color.Red)
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Device: ${device.manufacturerName} ${device.productName}", style = MaterialTheme.typography.titleMedium)
                        Text("VID: 0x${Integer.toHexString(device.vendorId).uppercase()} | PID: 0x${Integer.toHexString(device.productId).uppercase()}")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = androidVersion,
                    onValueChange = { if (it.all { char -> char.isDigit() }) androidVersion = it },
                    label = { Text("Android Version") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isRunning
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { viewModel.startBypass(device, androidVersion.toIntOrNull() ?: 10) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isRunning
                ) {
                    Text(if (uiState.isRunning) "Bypassing..." else "Start FRP Bypass")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (uiState.isRunning || uiState.progress > 0) {
                LinearProgressIndicator(
                    progress = { uiState.progress / 100f },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("${uiState.progress}% - ${uiState.statusMessage}", modifier = Modifier.padding(top = 8.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("Execution Logs", style = MaterialTheme.typography.titleSmall, modifier = Modifier.align(Alignment.Start))
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(top = 8.dp),
                color = Color.Black,
                shape = MaterialTheme.shapes.medium
            ) {
                LazyColumn(modifier = Modifier.padding(8.dp)) {
                    items(uiState.logs) { log ->
                        Text(
                            text = log,
                            color = if (log.contains("ERROR")) Color.Red else if (log.contains("SUCCESS")) Color.Green else Color.LightGray,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        )
                    }
                }
            }
            
            uiState.error?.let {
                AlertDialog(
                    onDismissRequest = { viewModel.clearState() },
                    title = { Text("Error") },
                    text = { Text(it) },
                    confirmButton = {
                        TextButton(onClick = { viewModel.clearState() }) { Text("OK") }
                    }
                )
            }

            uiState.success?.let {
                AlertDialog(
                    onDismissRequest = { viewModel.clearState() },
                    title = { Text("Success") },
                    text = { Text(it) },
                    confirmButton = {
                        TextButton(onClick = { viewModel.clearState() }) { Text("Done") }
                    }
                )
            }
        }
    }
}
