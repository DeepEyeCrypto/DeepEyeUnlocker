package com.deepeye.otg.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.deepeye.otg.data.model.ExploitLog
import com.deepeye.otg.ui.theme.DeepEyeColors
import com.deepeye.otg.ui.viewmodel.LogEntry

data class ConsoleEntry(
    val message: String,
    val type: String,
    val timestamp: String = "",
)

@JvmName("logEntriesToConsoleEntries")
fun List<LogEntry>.toConsoleEntries(): List<ConsoleEntry> = map {
    ConsoleEntry(message = it.message, type = it.type, timestamp = it.timestamp)
}

@JvmName("exploitLogsToConsoleEntries")
fun List<ExploitLog>.toConsoleEntries(): List<ConsoleEntry> = map {
    ConsoleEntry(
        message = it.message,
        type = if (it.isError) "ERROR" else "SUCCESS",
    )
}

private fun consoleTypeColor(type: String): Color = when (type.uppercase()) {
    "INFO" -> DeepEyeColors.TextSecondary
    "SUCCESS", "OK" -> DeepEyeColors.Success
    "ERROR", "FAIL" -> DeepEyeColors.Error
    "WARN", "WARNING" -> DeepEyeColors.Warning
    "DATA", "ADB", "USB", "MTK" -> DeepEyeColors.PrimaryCyan
    "EXPLOIT" -> DeepEyeColors.PurpleDim
    else -> DeepEyeColors.TextPrimary
}

@Composable
fun LogConsole(
    entries: List<ConsoleEntry>,
    modifier: Modifier = Modifier,
    title: String = "Live Console",
    onClear: (() -> Unit)? = null,
    onExport: (() -> Unit)? = null,
    onShare: (() -> Unit)? = null,
) {
    val listState = rememberLazyListState()
    val clipboardManager = LocalClipboardManager.current
    val allText = remember(entries) {
        entries.joinToString(separator = "\n") { entry ->
            buildString {
                if (entry.timestamp.isNotBlank()) {
                    append('[')
                    append(entry.timestamp)
                    append("] ")
                }
                append('[')
                append(entry.type)
                append("] ")
                append(entry.message)
            }
        }
    }

    LaunchedEffect(entries.size) {
        if (entries.isNotEmpty()) {
            listState.animateScrollToItem(entries.lastIndex)
        }
    }

    GlassCard(
        hazeState = null,
        modifier = modifier,
        accentColor = DeepEyeColors.PrimaryCyan.copy(alpha = 0.65f),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = DeepEyeColors.TextPrimary,
                    )
                    Text(
                        text = "${entries.size} entries",
                        style = MaterialTheme.typography.labelSmall,
                        color = DeepEyeColors.TextSecondary,
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = {
                        clipboardManager.setText(AnnotatedString(allText))
                    }) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, tint = DeepEyeColors.PrimaryCyan)
                    }
                    if (onExport != null) {
                        IconButton(onClick = onExport) {
                            Icon(Icons.Default.FileUpload, contentDescription = null, tint = DeepEyeColors.TextSecondary)
                        }
                    }
                    if (onShare != null) {
                        IconButton(onClick = onShare) {
                            Icon(Icons.Default.IosShare, contentDescription = null, tint = DeepEyeColors.TextSecondary)
                        }
                    }
                    if (onClear != null) {
                        IconButton(onClick = onClear) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = DeepEyeColors.Error)
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DeepEyeColors.TerminalBackground, RoundedCornerShape(14.dp))
                    .padding(12.dp),
            ) {
                if (entries.isEmpty()) {
                    Text(
                        text = "Awaiting live session logs…",
                        style = MaterialTheme.typography.bodySmall,
                        color = DeepEyeColors.TextSecondary,
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        state = listState,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        itemsIndexed(entries) { index, entry ->
                            val typeColor = consoleTypeColor(entry.type)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top,
                            ) {
                                Text(
                                    text = index.inc().toString().padStart(3, '0'),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = DeepEyeColors.TextFaint,
                                    modifier = Modifier.padding(top = 2.dp),
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        if (entry.timestamp.isNotBlank()) {
                                            Text(
                                                text = entry.timestamp,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = DeepEyeColors.TextFaint,
                                            )
                                        }
                                        Text(
                                            text = "[${entry.type}]",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = typeColor,
                                        )
                                    }
                                    Text(
                                        text = entry.message,
                                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = com.deepeye.otg.ui.theme.JetBrainsMono),
                                        color = typeColor,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
