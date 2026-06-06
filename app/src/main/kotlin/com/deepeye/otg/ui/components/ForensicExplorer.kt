package com.deepeye.otg.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
// Removed missing DeepEyeColors import

data class CarvedFileUi(
    val name: String,
    val type: String,
    val size: String,
    val isDeleted: Boolean = true
)

@Composable
fun ForensicExplorer(
    carvedFiles: List<CarvedFileUi>,
    onRepair: (CarvedFileUi) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black.copy(alpha = 0.3f))
            .padding(16.dp)
    ) {
        Text(
            text = "DEEP CARVING RESULTS",
            color = Color(0xFF6750A4),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        if (carvedFiles.isEmpty()) {
            Box(Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                Text("No deleted fragments found", color = Color.Gray, fontSize = 14.sp)
            }
        } else {
            LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                items(carvedFiles) { file ->
                    CarvedFileItem(file, onRepair)
                    HorizontalDivider(color = Color.DarkGray.copy(alpha = 0.5f))
                }
            }
        }
    }
}

@Composable
fun CarvedFileItem(file: CarvedFileUi, onRepair: (CarvedFileUi) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(file.name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text("${file.type} • ${file.size} • Deleted", color = Color.Red.copy(alpha = 0.7f), fontSize = 11.sp)
        }
        
        Button(
            onClick = { onRepair(file) },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9C6FFF)),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            shape = RoundedCornerShape(4.dp),
            modifier = Modifier.height(28.dp)
        ) {
            Text("RECOVER", fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}
