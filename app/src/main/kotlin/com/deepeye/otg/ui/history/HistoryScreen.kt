package com.deepeye.otg.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.deepeye.otg.data.BypassHistoryEntry
import com.deepeye.otg.viewmodel.BypassViewModel

@Composable
fun HistoryScreen(vm: BypassViewModel = viewModel()) {
    val history by vm.bypassHistory.collectAsState(emptyList())

    Column(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF06060F))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("HISTORY", color = Color.White, fontSize = 12.sp,
                fontWeight = FontWeight.Black, letterSpacing = 1.5.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                // Export button
                Box(
                    Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF00FFFF).copy(0.1f))
                        .border(0.5.dp, Color(0xFF00FFFF).copy(0.3f), RoundedCornerShape(8.dp))
                        .clickable { vm.exportHistory() }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text("↑ EXPORT", color = Color(0xFF00FFFF),
                        fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
                // Clear button
                Box(
                    Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFFF4444).copy(0.1f))
                        .border(0.5.dp, Color(0xFFFF4444).copy(0.3f), RoundedCornerShape(8.dp))
                        .clickable { vm.clearHistory() }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text("✕ CLEAR", color = Color(0xFFFF4444),
                        fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Stats row
        val successCount = history.count { it.success }
        val failCount    = history.count { !it.success }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("✓ $successCount success", color = Color(0xFF39FF14), fontSize = 9.sp)
            Text("✗ $failCount failed",    color = Color(0xFFFF4444), fontSize = 9.sp)
            Text("${history.size} total",  color = Color.White.copy(0.3f), fontSize = 9.sp)
        }

        if (history.isEmpty()) {
            Box(Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                Text("No bypass history yet", color = Color.White.copy(0.25f), fontSize = 11.sp)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                items(history, key = { it.id }) { entry ->
                    HistoryCard(entry)
                }
            }
        }
    }
}

@Composable
fun HistoryCard(entry: BypassHistoryEntry) {
    val color = if (entry.success) Color(0xFF39FF14) else Color(0xFFFF4444)
    val ts = remember(entry.timestamp) {
        java.text.SimpleDateFormat("dd MMM HH:mm", java.util.Locale.US)
            .format(java.util.Date(entry.timestamp))
    }

    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(0.05f))
            .border(0.5.dp, color.copy(0.2f), RoundedCornerShape(8.dp))
            .padding(10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    if (entry.success) "✓" else "✗",
                    color = color, fontSize = 11.sp, fontWeight = FontWeight.Black
                )
                Text(entry.carrier, color = Color.White, fontSize = 10.sp,
                    fontWeight = FontWeight.Bold)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(entry.method, color = color.copy(0.7f), fontSize = 8.sp)
                Text("•", color = Color.White.copy(0.2f), fontSize = 8.sp)
                Text(entry.deviceModel.take(20), color = Color.White.copy(0.4f), fontSize = 8.sp)
            }
        }
        Text(ts, color = Color.White.copy(0.25f), fontSize = 8.sp)
    }
}
