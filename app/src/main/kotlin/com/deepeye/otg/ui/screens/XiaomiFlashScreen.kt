package com.deepeye.otg.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
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
import com.deepeye.otg.data.model.FlashStatus
import com.deepeye.otg.data.model.XiaomiPartition
import com.deepeye.otg.viewmodel.XiaomiFlashViewModel
import java.io.File
import java.text.DecimalFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun XiaomiFlashScreen(
    viewModel: XiaomiFlashViewModel = hiltViewModel()
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
            Toast.makeText(context, "Image selected", Toast.LENGTH_SHORT).show()
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
            text = "🔥 Xiaomi Flash Tool",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            text = "Fastboot/EDL Flashing • Bootloader Unlock • Partition Manager",
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

        // 1. DEVICE INFO CARD
        DeviceInfoCard(
            deviceInfo = state.deviceInfo,
            isDetecting = state.isDetecting,
            onDetect = { viewModel.detectDevice() }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 2. ADD FLASH TASK
        AddTaskCard(
            selectedPartition = state.selectedPartition,
            selectedImagePath = state.selectedImagePath,
            onPartitionSelect = { viewModel.selectPartition(it) },
            onImagePick = { filePickerLauncher.launch("*/*") },
            onAddTask = {
                val imagePath = state.selectedImagePath
                if (imagePath != null) {
                    val file = File(imagePath)
                    viewModel.addFlashTask(
                        partition = state.selectedPartition,
                        imagePath = imagePath,
                        size = file.length()
                    )
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 3. FLASH TASKS LIST
        if (state.flashTasks.isNotEmpty()) {
            FlashTasksCard(
                tasks = state.flashTasks,
                onRemoveTask = { viewModel.removeTask(it) },
                onClearAll = { viewModel.clearTasks() }
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        // 4. ACTION BUTTONS
        ActionButtonsSection(
            isFlashing = state.isFlashing,
            tasksCount = state.flashTasks.size,
            onStartFlash = { viewModel.startFlashing() },
            onUnlockBootloader = { viewModel.unlockBootloader() },
            onRebootFastboot = { viewModel.rebootToFastboot() },
            onRebootRecovery = { viewModel.rebootToRecovery() },
            onRebootSystem = { viewModel.rebootToSystem() },
            onRebootEDL = { viewModel.rebootToEDL() },
            onWipeData = { viewModel.wipeData() }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 5. LOGS SECTION
        if (state.logs.isNotEmpty()) {
            LogsSection(logs = state.logs)
        }
    }
}

@Composable
fun DeviceInfoCard(
    deviceInfo: com.deepeye.otg.data.model.XiaomiDeviceInfo?,
    isDetecting: Boolean,
    onDetect: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📱 Device Information",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Button(
                    onClick = onDetect,
                    enabled = !isDetecting
                ) {
                    if (isDetecting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(if (isDetecting) "Detecting..." else "Detect Device")
                }
            }

            if (deviceInfo != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Divider()
                Spacer(modifier = Modifier.height(12.dp))

                InfoRow("Model", deviceInfo.model)
                InfoRow("Codename", deviceInfo.codename)
                InfoRow("Android", deviceInfo.androidVersion)
                InfoRow("MIUI", deviceInfo.miuiVersion.ifEmpty { "N/A" })
                InfoRow("Bootloader", deviceInfo.bootloaderStatus)
                InfoRow("Anti-Rollback", deviceInfo.antiRollback)

                Spacer(modifier = Modifier.height(8.dp))
                Row {
                    FlashModeChip(deviceInfo.flashMode.name)
                    Spacer(modifier = Modifier.width(8.dp))
                    BootloaderBadge(deviceInfo.bootloaderStatus)
                }
            }
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun FlashModeChip(mode: String) {
    Surface(
        color = Color(0xFFE3F2FD),
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(
            text = "🔌 $mode",
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium
        )
    }
}

@Composable
fun BootloaderBadge(status: String) {
    val color = if (status == "unlocked") Color(0xFFC8E6C9) else Color(0xFFFFCDD2)
    Surface(
        color = color,
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(
            text = if (status == "unlocked") "🔓 Unlocked" else "🔒 Locked",
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium
        )
    }
}

@Composable
fun AddTaskCard(
    selectedPartition: XiaomiPartition,
    selectedImagePath: String?,
    onPartitionSelect: (XiaomiPartition) -> Unit,
    onImagePick: () -> Unit,
    onAddTask: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "➕ Add Flash Task",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Partition selector
            Text("Partition:", style = MaterialTheme.typography.labelLarge)
            Spacer(modifier = Modifier.height(4.dp))
            PartitionSelector(
                selected = selectedPartition,
                onSelect = onPartitionSelect
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Image picker
            Text("Image File:", style = MaterialTheme.typography.labelLarge)
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onImagePick,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(selectedImagePath?.substringAfterLast('/') ?: "Pick Image")
                }
                Button(
                    onClick = onAddTask,
                    enabled = selectedImagePath != null
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add")
                }
            }
        }
    }
}

@Composable
fun PartitionSelector(
    selected: XiaomiPartition,
    onSelect: (XiaomiPartition) -> Unit
) {
    Column {
        XiaomiPartition.entries.chunked(3).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                row.forEach { partition ->
                    val isSelected = partition == selected
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSelect(partition) },
                        label = { Text(partition.label, style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@Composable
fun FlashTasksCard(
    tasks: List<com.deepeye.otg.data.model.XiaomiFlashTask>,
    onRemoveTask: (String) -> Unit,
    onClearAll: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📋 Flash Queue (${tasks.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onClearAll) {
                    Text("Clear All")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            tasks.forEach { task ->
                TaskItem(task = task, onRemove = { onRemoveTask(task.id) })
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun TaskItem(
    task: com.deepeye.otg.data.model.XiaomiFlashTask,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (task.status) {
                FlashStatus.SUCCESS -> Color(0xFFE8F5E9)
                FlashStatus.FAILED -> Color(0xFFFFEBEE)
                FlashStatus.FLASHING -> Color(0xFFFFF3E0)
                else -> MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.partition.label.uppercase(),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = task.imagePath.substringAfterLast('/'),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onRemove, enabled = task.status == FlashStatus.PENDING) {
                    Icon(Icons.Default.Delete, contentDescription = "Remove")
                }
            }

            if (task.status == FlashStatus.FLASHING || task.status == FlashStatus.SUCCESS) {
                Spacer(modifier = Modifier.height(8.dp))
                val animatedProgress by animateFloatAsState(
                    targetValue = task.progress,
                    label = "progress"
                )
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = task.logOutput,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            } else if (task.status == FlashStatus.PENDING) {
                Text(
                    text = "⏳ Pending - ${formatXiaomiFileSize(task.imageSize)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ActionButtonsSection(
    isFlashing: Boolean,
    tasksCount: Int,
    onStartFlash: () -> Unit,
    onUnlockBootloader: () -> Unit,
    onRebootFastboot: () -> Unit,
    onRebootRecovery: () -> Unit,
    onRebootSystem: () -> Unit,
    onRebootEDL: () -> Unit,
    onWipeData: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "⚡ Actions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Flash button
            Button(
                onClick = onStartFlash,
                enabled = !isFlashing && tasksCount > 0,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isFlashing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Flashing...")
                } else {
                    Icon(Icons.Default.FlashOn, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Start Flashing ($tasksCount partitions)")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Divider()
            Spacer(modifier = Modifier.height(12.dp))

            // Bootloader unlock
            OutlinedButton(
                onClick = onUnlockBootloader,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Unlock Bootloader")
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Reboot options
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(onClick = onRebootSystem, modifier = Modifier.weight(1f)) {
                    Text("System")
                }
                OutlinedButton(onClick = onRebootRecovery, modifier = Modifier.weight(1f)) {
                    Text("Recovery")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(onClick = onRebootFastboot, modifier = Modifier.weight(1f)) {
                    Text("Fastboot")
                }
                OutlinedButton(onClick = onRebootEDL, modifier = Modifier.weight(1f)) {
                    Text("EDL")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Wipe data
            Button(
                onClick = onWipeData,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5722)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Wipe Data/Factory Reset")
            }
        }
    }
}

@Composable
fun LogsSection(logs: List<String>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "📝 Flash Logs",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 300.dp)
                    .background(Color(0xFF1E1E1E), RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                LazyColumn {
                    items(logs) { log ->
                        Text(
                            text = log.substringAfter("] ", log),
                            color = Color(0xFF00FF00),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

fun formatXiaomiFileSize(bytes: Long): String {
    return when {
        bytes >= 1024 * 1024 * 1024 -> "${"%.2f".format(bytes / 1024.0 / 1024.0 / 1024.0)} GB"
        bytes >= 1024 * 1024 -> "${"%.2f".format(bytes / 1024.0 / 1024.0)} MB"
        bytes >= 1024 -> "${"%.2f".format(bytes / 1024.0)} KB"
        else -> "$bytes B"
    }
}
