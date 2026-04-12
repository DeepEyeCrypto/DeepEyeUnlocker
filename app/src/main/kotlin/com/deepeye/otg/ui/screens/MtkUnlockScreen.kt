package com.deepeye.otg.ui.screens

import android.hardware.usb.UsbDevice
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.deepeye.otg.data.model.*
import com.deepeye.otg.viewmodel.MtkUnlockViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MtkUnlockScreen(
    currentDevice: UsbDevice? = null,
    viewModel: MtkUnlockViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // File picker launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val path = it.path ?: return@let
            viewModel.selectImage(path)
            Toast.makeText(context, "File selected", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        // Title
        Text(
            text = "🔧 MTK Unlock Tool",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            text = "BROM Exploit • FRP Remove • Bootloader Unlock • NVRAM Backup",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Error Banner
        state.errorMessage?.let { error ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Error,
                        contentDescription = null,
                        tint = Color.Red,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = error,
                        color = Color.Red,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { viewModel.clearError() }) {
                        Icon(Icons.Default.Close, contentDescription = "Dismiss")
                    }
                }
            }
        }

        // Success Banner
        state.successMessage?.let { success ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color.Green,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = success,
                        color = Color.Green,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { viewModel.clearSuccess() }) {
                        Icon(Icons.Default.Close, contentDescription = "Dismiss")
                    }
                }
            }
        }

        // 1. DEVICE INFO CARD
        MtkDeviceInfoCard(
            deviceInfo = state.deviceInfo,
            isDetecting = state.isDetecting,
            onDetectUsb = { currentDevice?.let { viewModel.detectDevice(it) } },
            onDetectAdb = { viewModel.detectDeviceAdb() }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 2. SELECT OPERATION
        OperationSelectorCard(
            selectedOperation = state.selectedOperation,
            onOperationSelect = { viewModel.selectOperation(it) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 3. EXECUTE SINGLE OPERATION
        ExecuteOperationCard(
            operation = state.selectedOperation,
            isExecuting = state.isExecuting,
            onExecute = { viewModel.executeOperation(currentDevice) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 4. OPERATIONS QUEUE
        if (state.operations.isNotEmpty()) {
            OperationsQueueCard(
                operations = state.operations,
                onRemove = { viewModel.removeOperation(it) },
                onClearAll = { viewModel.clearOperations() }
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            Button(
                onClick = { viewModel.executeAllOperations(currentDevice) },
                enabled = !state.isExecuting,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Execute All Operations")
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }

        // 5. QUICK ACTIONS
        QuickActionsCard(
            onRemoveFrp = { 
                viewModel.selectOperation(MtkUnlockOperation.REMOVE_FRP)
                viewModel.executeOperation(currentDevice)
            },
            onUnlockBootloader = {
                viewModel.selectOperation(MtkUnlockOperation.UNLOCK_BOOTLOADER)
                viewModel.executeOperation(currentDevice)
            },
            onFormatUserdata = {
                viewModel.selectOperation(MtkUnlockOperation.FORMAT_USERDATA)
                viewModel.executeOperation(currentDevice)
            },
            onReadNvram = {
                viewModel.selectOperation(MtkUnlockOperation.READ_NVRAM)
                viewModel.executeOperation(currentDevice)
            },
            onBypassDa = {
                viewModel.selectOperation(MtkUnlockOperation.DA_AUTH_BYPASS)
                viewModel.executeOperation(currentDevice)
            },
            onDisableVerity = {
                viewModel.selectOperation(MtkUnlockOperation.DISABLE_VERITY)
                viewModel.executeOperation(currentDevice)
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 6. LOGS SECTION
        LogsSection(
            logs = state.logs,
            onClearLogs = { viewModel.clearOperations() }
        )
    }
}

@Composable
fun MtkDeviceInfoCard(
    deviceInfo: MtkDeviceInfo?,
    isDetecting: Boolean,
    onDetectUsb: () -> Unit,
    onDetectAdb: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📱 Device Information",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Row {
                    Button(
                        onClick = onDetectAdb,
                        enabled = !isDetecting,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.Usb, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("ADB")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onDetectUsb,
                        enabled = !isDetecting
                    ) {
                        if (isDetecting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Icon(Icons.Default.Usb, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Detect BROM")
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (deviceInfo != null) {
                MtkInfoRow("Chip", deviceInfo.chip.chipName)
                MtkInfoRow("Chip ID", deviceInfo.chipId)
                if (deviceInfo.brand.isNotEmpty()) {
                    MtkInfoRow("Brand", deviceInfo.brand)
                    MtkInfoRow("Model", deviceInfo.model)
                    MtkInfoRow("Android", deviceInfo.androidVer)
                    MtkInfoRow("Build ID", deviceInfo.buildId)
                }
                MtkInfoRow("Mode", deviceInfo.connectMode.name)
                if (deviceInfo.daAuthRequired) {
                    MtkInfoRow("DA Auth", "Required ⚠️", Color.Red)
                }
            } else {
                Text(
                    text = "No device detected. Connect MTK device in BROM mode or via ADB.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
fun OperationSelectorCard(
    selectedOperation: MtkUnlockOperation,
    onOperationSelect: (MtkUnlockOperation) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "⚙️ Select Operation",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Operation chips grid
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MtkUnlockOperation.entries.forEach { operation ->
                    FilterChip(
                        selected = selectedOperation == operation,
                        onClick = { onOperationSelect(operation) },
                        label = { Text(operation.name.replace("_", " ")) }
                    )
                }
            }
        }
    }
}

@Composable
fun ExecuteOperationCard(
    operation: MtkUnlockOperation,
    isExecuting: Boolean,
    onExecute: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "🚀 Execute: ${operation.name.replace("_", " ")}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Button(
                onClick = onExecute,
                enabled = !isExecuting,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isExecuting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isExecuting) "Executing..." else "Execute Operation")
            }
        }
    }
}

@Composable
fun OperationsQueueCard(
    operations: List<MtkFlashTask>,
    onRemove: (String) -> Unit,
    onClearAll: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📋 Operations Queue (${operations.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onClearAll) {
                    Text("Clear All")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            operations.forEach { task ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = task.operation.name.replace("_", " "),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        if (task.partition.isNotEmpty()) {
                            Text(
                                text = "Partition: ${task.partition}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    IconButton(onClick = { onRemove(task.id) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Remove")
                    }
                }
                HorizontalDivider()
            }
        }
    }
}

@Composable
fun QuickActionsCard(
    onRemoveFrp: () -> Unit,
    onUnlockBootloader: () -> Unit,
    onFormatUserdata: () -> Unit,
    onReadNvram: () -> Unit,
    onBypassDa: () -> Unit,
    onDisableVerity: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "⚡ Quick Actions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Grid of quick action buttons
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    QuickActionButton(
                        icon = Icons.Default.LockOpen,
                        label = "Remove FRP",
                        onClick = onRemoveFrp,
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionButton(
                        icon = Icons.Default.LockOpen,
                        label = "Unlock BL",
                        onClick = onUnlockBootloader,
                        modifier = Modifier.weight(1f)
                    )
                }
                
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    QuickActionButton(
                        icon = Icons.Default.DeleteForever,
                        label = "Format Data",
                        onClick = onFormatUserdata,
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionButton(
                        icon = Icons.Default.Save,
                        label = "Read NVRAM",
                        onClick = onReadNvram,
                        modifier = Modifier.weight(1f)
                    )
                }
                
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    QuickActionButton(
                        icon = Icons.Default.Shield,
                        label = "Bypass DA",
                        onClick = onBypassDa,
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionButton(
                        icon = Icons.Default.Build,
                        label = "Disable Verity",
                        onClick = onDisableVerity,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun QuickActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(label)
    }
}

@Composable
fun LogsSection(
    logs: List<String>,
    onClearLogs: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E1E1E)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📜 Operation Logs",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                TextButton(onClick = onClearLogs) {
                    Text("Clear", color = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (logs.isEmpty()) {
                Text(
                    text = "No logs yet. Execute an operation to see logs here.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp)
                ) {
                    items(logs) { log ->
                        Text(
                            text = log,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF00FF00),
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MtkInfoRow(label: String, value: String, valueColor: Color = Color.Unspecified) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = valueColor
        )
    }
}

// FlowRow helper (simple implementation)
@Composable
fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable () -> Unit
) {
    Column(
        modifier = modifier,
        verticalArrangement = verticalArrangement
    ) {
        content()
    }
}
