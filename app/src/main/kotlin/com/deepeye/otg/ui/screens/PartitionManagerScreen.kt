package com.deepeye.otg.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deepeye.otg.domain.models.PartitionItem
import com.deepeye.otg.ui.components.GlassButton
import com.deepeye.otg.ui.components.GlassCard
import com.deepeye.otg.ui.theme.GlassTokens
import com.deepeye.otg.viewmodel.UsbViewModel
import dev.chrisbanes.haze.HazeState

@Composable
fun PartitionManagerScreen(
    partitions: List<PartitionItem>,
    viewModel: UsbViewModel
) {
    val hazeState = remember { HazeState() }
    val hexData by viewModel.hexPeekData.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    val filtered = remember(partitions, searchQuery) {
        if (searchQuery.isBlank()) partitions 
        else partitions.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 24.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "PARTITION MANAGER",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Forensic Sector Access — ${partitions.size} partitions",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(0.6f)
                    )
                }
                
                GlassButton(
                    label = "CLOSE",
                    onClick = { viewModel.resetToIdle() },
                    modifier = Modifier.width(80.dp),
                    accent = false
                )
            }

            // Partition List
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filtered, key = { it.id }) { part ->
                    PartitionRow(
                        part = part,
                        hazeState = hazeState,
                        onPeek = { viewModel.peekPartition(part.name) },
                        onDump = { /* op_safe_dump handled via main queue if needed */ }
                    )
                }
            }
        }

        // Overlay for Hex View
        hexData?.let { hex ->
            com.deepeye.otg.ui.components.HexPeekDialog(
                hex = hex,
                onDismiss = { viewModel.closeHexPeek() }
            )
        }
    }
}

@Composable
fun PartitionRow(
    part: PartitionItem,
    hazeState: HazeState,
    onPeek: () -> Unit,
    onDump: () -> Unit
) {
    val isSystem = part.name.contains("system", ignoreCase = true)
    val isUser = part.name.contains("userdata", ignoreCase = true)
    val accentColor = if (isSystem) Color(0xFF60A5FA) else if (isUser) Color(0xFFFACC15) else Color.White.copy(0.5f)

    GlassCard(
        hazeState = hazeState,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = part.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color.White
                )
                Text(
                    text = "SIZE: ${part.sizeMb}",
                    fontSize = 11.sp,
                    color = Color.White.copy(0.6f)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(0.08f))
                        .clickable { onPeek() }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text("PEEK", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Cyan)
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(0.08f))
                        .clickable { onDump() }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text("DUMP", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}
