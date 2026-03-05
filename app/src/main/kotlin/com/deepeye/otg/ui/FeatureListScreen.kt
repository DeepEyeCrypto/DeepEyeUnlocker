package com.deepeye.otg.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deepeye.otg.usb.DeepEyeOperation

@Composable
fun FeatureListScreen(
    onOperationSelected: (DeepEyeOperation) -> Unit
) {
    val operations = DeepEyeOperation.entries
    val categories = listOf(
        "GROUP A UNLOCK OPERATIONS" to operations.filter { it.ordinal in 0..5 },
        "GROUP B SECURITY REPAIR" to operations.filter { it.ordinal in 6..10 },
        "GROUP C FRP & ACCOUNT" to operations.filter { it.ordinal in 11..15 },
        "GROUP D FIRMWARE & PARTITIONS" to operations.filter { it.ordinal in 16..20 },
        "GROUP E IMEI & NETWORK" to operations.filter { it.ordinal in 21..24 }
    )

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        categories.forEach { (title, ops) ->
            item(span = { GridItemSpan(2) }) {
                CategoryHeader(title)
            }
            items(
                items = ops,
                key = { op -> op.name }
            ) { op ->
                OperationItem(op) { onOperationSelected(op) }
            }
        }
    }
}

@Composable
private fun CategoryHeader(title: String) {
    Text(
        text = title,
        color = DeepEyeColors.CyanAccent,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
    )
}

@Composable
private fun OperationItem(
    op: DeepEyeOperation,
    onClick: () -> Unit
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("🛠", fontSize = 16.sp)
                OperationTierBadge(op.tier)
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = op.label,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 16.sp,
                maxLines = 2,
                modifier = Modifier.weight(1f)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            PrimaryIconButton(
                text = "RUN",
                onClick = onClick,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
