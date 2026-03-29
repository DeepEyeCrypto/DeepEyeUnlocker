package com.deepeye.otg.ui.screens

import android.hardware.usb.UsbDevice
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.deepeye.otg.viewmodel.MtkDeviceViewModel
import com.deepeye.otg.viewmodel.MtkOperationState

@Composable
fun MtkDeviceScreen(
    currentDevice: UsbDevice? = null,
    viewModel: MtkDeviceViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val progress by viewModel.progress.collectAsStateWithLifecycle()

    LaunchedEffect(currentDevice) {
        currentDevice?.let { viewModel.onDeviceDetected(it) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "MediaTek Device Panel",
            style = MaterialTheme.typography.headlineSmall
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                when (val s = state) {
                    is MtkOperationState.Idle -> Text("No MTK operation running")
                    is MtkOperationState.ModeDetected -> Text("Mode=${s.mode} Chip=${s.chip}")
                    is MtkOperationState.BromHandshaking -> Text("Executing BROM handshake...")
                    is MtkOperationState.SendingDa -> Text("Sending Download Agent...")
                    is MtkOperationState.DaReady -> Text("DA ready. Flash ops available.")
                    is MtkOperationState.Progress -> Text("${(s.percent * 100).toInt()}% • ${s.stage}")
                    is MtkOperationState.Success -> Text("Success: ${s.message}")
                    is MtkOperationState.Error -> Text("Error: ${s.reason}")
                }

                LinearProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Button(
            onClick = { currentDevice?.let(viewModel::startBromHandshake) },
            enabled = currentDevice != null,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Start BROM Handshake")
        }

        Spacer(modifier = Modifier.height(4.dp))

        Button(
            onClick = { viewModel.unlockBootloader() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Unlock Bootloader")
        }
    }
}

