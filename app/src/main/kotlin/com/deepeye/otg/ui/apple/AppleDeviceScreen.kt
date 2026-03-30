package com.deepeye.otg.ui.apple

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.deepeye.otg.data.repository.AppleDeviceState
import com.deepeye.otg.usb.DeviceMatrix

@Composable
fun AppleDeviceScreen(
    viewModel: AppleDeviceViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Show error snackbar
    state.errorMessage?.let { error ->
        LaunchedEffect(error) {
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    // Show success snackbar
    state.successMessage?.let { success ->
        LaunchedEffect(success) {
            snackbarHostState.showSnackbar(success)
            viewModel.clearSuccess()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Apple Device Manager",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            // Device state card
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Device State",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = when (val deviceState = state.appleDeviceState) {
                            is AppleDeviceState.Idle -> "No Apple device detected"
                            is AppleDeviceState.Detected -> "Apple device detected: ${deviceState.device.deviceName} (${deviceState.mode})"
                            is AppleDeviceState.Error -> "Error: ${deviceState.reason}"
                            else -> "Unknown state"
                        }
                    )
                    state.detectedMode?.let { mode ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "Mode: $mode")
                    }
                }
            }

            // Actions card
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Actions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (state.isRefreshing) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else {
                        ModeAwareActions(
                            mode = state.detectedMode,
                            onRefresh = viewModel::refreshAppleDevice,
                            onExitRecovery = viewModel::exitRecovery,
                            onEnterDfu = viewModel::enterDfu,
                            onSendIrecovery = viewModel::sendIrecoveryCommand
                        )
                    }
                }
            }

            // Output card
            state.irecoveryOutput?.let { output ->
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Output",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = output,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ModeAwareActions(
    mode: DeviceMatrix.AppleMode?,
    onRefresh: () -> Unit,
    onExitRecovery: () -> Unit,
    onEnterDfu: () -> Unit,
    onSendIrecovery: (String) -> Unit
) {
    when (mode) {
        DeviceMatrix.AppleMode.NORMAL -> NormalModeActions(onRefresh = onRefresh)
        DeviceMatrix.AppleMode.RECOVERY -> RecoveryModeActions(
            onRefresh = onRefresh,
            onExitRecovery = onExitRecovery,
            onEnterDfu = onEnterDfu,
            onSendIrecovery = onSendIrecovery
        )
        DeviceMatrix.AppleMode.DFU -> DfuModeActions(
            onRefresh = onRefresh,
            onSendIrecovery = onSendIrecovery
        )
        DeviceMatrix.AppleMode.WTF -> WtfModeActions(
            onRefresh = onRefresh,
            onSendIrecovery = onSendIrecovery
        )
        DeviceMatrix.AppleMode.PWNED_DFU -> PwnedDfuModeActions(
            onRefresh = onRefresh,
            onSendIrecovery = onSendIrecovery
        )
        else -> {
            Text(
                text = "Connect an Apple device (Normal/Recovery/DFU) to unlock mode-specific actions.",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun NormalModeActions(onRefresh: () -> Unit) {
    Text(
        text = "Normal Mode: identity and device-info operations are available.",
        style = MaterialTheme.typography.bodyMedium
    )
    Spacer(modifier = Modifier.height(8.dp))
    Button(onClick = onRefresh, modifier = Modifier.fillMaxWidth()) {
        Text("Refresh Device Info")
    }
}

@Composable
private fun RecoveryModeActions(
    onRefresh: () -> Unit,
    onExitRecovery: () -> Unit,
    onEnterDfu: () -> Unit,
    onSendIrecovery: (String) -> Unit
) {
    Text(
        text = "Recovery Mode: iRecovery commands + recovery transitions available.",
        style = MaterialTheme.typography.bodyMedium
    )
    Spacer(modifier = Modifier.height(8.dp))
    Button(onClick = onRefresh, modifier = Modifier.fillMaxWidth()) {
        Text("Refresh Recovery Info")
    }
    Spacer(modifier = Modifier.height(8.dp))
    IrecoveryCommandRow(onSendIrecovery = onSendIrecovery)
    Spacer(modifier = Modifier.height(8.dp))
    Button(onClick = onExitRecovery, modifier = Modifier.fillMaxWidth()) {
        Text("Exit Recovery Mode")
    }
    Spacer(modifier = Modifier.height(8.dp))
    Button(onClick = onEnterDfu, modifier = Modifier.fillMaxWidth()) {
        Text("Enter DFU Mode")
    }
}

@Composable
private fun DfuModeActions(
    onRefresh: () -> Unit,
    onSendIrecovery: (String) -> Unit
) {
    Text(
        text = "DFU/WTF/Pwned DFU: exploit and low-level iRecovery commands are available.",
        style = MaterialTheme.typography.bodyMedium
    )
    Spacer(modifier = Modifier.height(8.dp))
    Button(onClick = onRefresh, modifier = Modifier.fillMaxWidth()) {
        Text("Probe DFU State")
    }
    Spacer(modifier = Modifier.height(8.dp))
    IrecoveryCommandRow(onSendIrecovery = onSendIrecovery)
}

@Composable
private fun WtfModeActions(
    onRefresh: () -> Unit,
    onSendIrecovery: (String) -> Unit
) {
    Text(
        text = "WTF Mode: pre-DFU transitional state. iBSS upload may be required before exploit chain.",
        style = MaterialTheme.typography.bodyMedium
    )
    Spacer(modifier = Modifier.height(8.dp))
    Button(onClick = onRefresh, modifier = Modifier.fillMaxWidth()) {
        Text("Probe WTF State")
    }
    Spacer(modifier = Modifier.height(8.dp))
    IrecoveryCommandRow(onSendIrecovery = onSendIrecovery)
}

@Composable
private fun PwnedDfuModeActions(
    onRefresh: () -> Unit,
    onSendIrecovery: (String) -> Unit
) {
    Text(
        text = "Pwned DFU: exploit-ready state confirmed. Advanced ramdisk/checkm8 paths are available.",
        style = MaterialTheme.typography.bodyMedium
    )
    Spacer(modifier = Modifier.height(8.dp))
    Button(onClick = onRefresh, modifier = Modifier.fillMaxWidth()) {
        Text("Probe Pwned DFU State")
    }
    Spacer(modifier = Modifier.height(8.dp))
    IrecoveryCommandRow(onSendIrecovery = onSendIrecovery)
}

@Composable
private fun IrecoveryCommandRow(onSendIrecovery: (String) -> Unit) {
    var cmd by remember { mutableStateOf("getenv") }
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = cmd,
            onValueChange = { cmd = it },
            label = { Text("iRecovery Command") },
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Button(onClick = { onSendIrecovery(cmd) }) {
            Text("Send")
        }
    }
}
