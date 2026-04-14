package com.deepeye.otg.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.deepeye.otg.ui.components.LogConsole
import com.deepeye.otg.ui.components.SectionHeader
import com.deepeye.otg.ui.components.toConsoleEntries
import com.deepeye.otg.ui.theme.DeepEyeColors
import com.deepeye.otg.ui.viewmodel.LogEntry
import com.deepeye.otg.viewmodel.UsbViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DeepEyeLogScreen(mainViewModel: UsbViewModel) {
    val logs by mainViewModel.logs.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var filter by rememberSaveable { mutableStateOf("ALL") }

    val filteredLogs = remember(logs, filter) {
        logs.filter { entry ->
            when (filter) {
                "INFO" -> entry.type.equals("INFO", ignoreCase = true)
                "SUCCESS" -> entry.type.equals("SUCCESS", ignoreCase = true) || entry.type.equals("OK", ignoreCase = true)
                "ERROR" -> entry.type.equals("ERROR", ignoreCase = true) || entry.type.equals("FAIL", ignoreCase = true)
                else -> true
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        SectionHeader(title = "System Logs", count = filteredLogs.size.toString())

        Text(
            text = "Real-time session output with export and share actions.",
            style = MaterialTheme.typography.bodySmall,
            color = DeepEyeColors.TextSecondary,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            listOf("ALL", "INFO", "SUCCESS", "ERROR").forEach { type ->
                FilterChip(
                    selected = filter == type,
                    onClick = { filter = type },
                    label = { Text(type) },
                )
            }
        }

        LogConsole(
            entries = filteredLogs.toConsoleEntries(),
            title = "Live Stream",
            onClear = { mainViewModel.clearLogs() },
            onExport = {
                val file = write_log_file(context, filteredLogs)
                Toast.makeText(context, "Log exported: ${file.name}", Toast.LENGTH_SHORT).show()
            },
            onShare = {
                val file = write_log_file(context, filteredLogs)
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, "DeepEye session logs")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(intent, "Share logs"))
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(520.dp),
        )

        Spacer(modifier = Modifier.height(84.dp))
    }
}

private fun write_log_file(context: android.content.Context, logs: List<LogEntry>): File {
    val directory = context.externalCacheDir ?: context.cacheDir
    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
    val file = File(directory, "deepeye_logs_$timestamp.txt")
    val content = logs.joinToString(separator = "\n") { entry ->
        "[${entry.timestamp}] [${entry.type}] ${entry.message}"
    }
    file.writeText(content)
    return file
}
