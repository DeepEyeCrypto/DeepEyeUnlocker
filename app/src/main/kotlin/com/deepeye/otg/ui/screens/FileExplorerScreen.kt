package com.deepeye.otg.ui.screens

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deepeye.otg.ui.theme.DeepEyeColors
import com.deepeye.otg.ui.theme.DeepEyeType
import com.deepeye.otg.viewmodel.UsbViewModel
import org.json.JSONArray
import org.json.JSONObject

/**
 * Stage 50.2 — Forensic File System Explorer.
 * Allows browsing through decrypted partition data.
 */
@Composable
fun FileExplorerScreen(viewModel: UsbViewModel) {
    val currentPath by viewModel.currentPath.collectAsStateWithLifecycle()
    val filesJson by viewModel.directoryFiles.collectAsStateWithLifecycle()
    val hexPreview by viewModel.fileContentHex.collectAsStateWithLifecycle()
    
    val files = remember(filesJson) {
        val list = mutableListOf<FileEntry>()
        try {
            val array = JSONArray(filesJson)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(FileEntry(
                    name = obj.getString("name"),
                    isDir = obj.getBoolean("isDir"),
                    size = obj.optLong("size", 0L),
                    path = obj.getString("path")
                ))
            }
        } catch (e: Exception) {}
        list.sortedWith(compareByDescending<FileEntry> { it.isDir }.thenBy { it.name })
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = {
                    if (currentPath != "/") {
                        val parent = currentPath.substringBeforeLast("/", "").ifEmpty { "/" }
                        viewModel.browsePath(parent)
                    } else {
                        viewModel.setNav(NavTarget.LAB_HOME)
                    }
                }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = DeepEyeColors.WHITE_HIGH)
                }
                Spacer(Modifier.width(8.dp))
                Column {
                    Text("File Explorer", style = DeepEyeType.SUBHEADER.copy(fontSize = 20.sp), color = DeepEyeColors.WHITE_HIGH)
                    Text(currentPath, style = DeepEyeType.CAPTION.copy(fontSize = 11.sp), color = DeepEyeColors.NEON_PURPLE)
                }
            }

            Spacer(Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp))
                    .background(DeepEyeColors.BG_SURFACE.copy(0.6f))
            ) {
                items(files) { file ->
                    FileRow(file) {
                        if (file.isDir) {
                            viewModel.browsePath(file.path)
                        } else {
                            viewModel.openFile(file.path)
                        }
                    }
                    HorizontalDivider(color = DeepEyeColors.WHITE_LOW.copy(0.3f).copy(alpha = 0.5f), thickness = 0.5.dp)
                }
            }
        }

        hexPreview?.let { hex ->
            com.deepeye.otg.ui.components.HexPeekDialog(
                hex = hex,
                onDismiss = { viewModel.closeFilePreview() }
            )
        }
    }
}

@Composable
private fun FileRow(file: FileEntry, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (file.isDir) Icons.Default.Folder else Icons.Default.Description,
            contentDescription = null,
            tint = if (file.isDir) DeepEyeColors.NEON_PURPLE else DeepEyeColors.WHITE_MED,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(file.name, style = DeepEyeType.BODY.copy(fontSize = 14.sp), color = DeepEyeColors.WHITE_HIGH, fontWeight = FontWeight.Bold)
            if (!file.isDir) {
                Text(formatSize(file.size), style = DeepEyeType.CAPTION.copy(fontSize = 11.sp), color = DeepEyeColors.WHITE_MED)
            }
        }
    }
}

private fun formatSize(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val exp = (Math.log(bytes.toDouble()) / Math.log(1024.0)).toInt()
    val pre = "KMGTPE"[exp - 1]
    return "%.1f %sB".format(bytes / Math.pow(1024.0, exp.toDouble()), pre)
}

data class FileEntry(val name: String, val isDir: Boolean, val size: Long, val path: String)
