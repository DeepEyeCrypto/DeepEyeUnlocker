package com.deepeye.otg.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
// Removed missing DeepEyeColors import

@Composable
fun SafeDumpDialog(
    partitions: List<String>,
    onDismiss: () -> Unit,
    onStart: (String) -> Unit
) {
    var selectedPart by remember { mutableStateOf(partitions.firstOrNull() ?: "userdata") }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "FORENSIC ACQUISITION",
                    color = Color(0xFF6750A4),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(Modifier.height(8.dp))
                
                Text(
                    "Perform a bit-stream capture for carving.",
                    color = Color.Gray,
                    fontSize = 13.sp
                )
                
                Spacer(Modifier.height(24.dp))
                
                partitions.forEach { part ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = (part == selectedPart),
                                onClick = { selectedPart = part }
                            )
                            .padding(vertical = 12.dp, horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (part == selectedPart),
                            onClick = null,
                            colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF6750A4))
                        )
                        Text(
                            part,
                            color = if (part == selectedPart) Color.White else Color.Gray,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }
                }
                
                Spacer(Modifier.height(32.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) {
                        Text("CANCEL", color = Color.Gray)
                    }
                    Button(
                        onClick = { onStart(selectedPart) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6750A4)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(start = 16.dp)
                    ) {
                        Text("START DUMP", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
