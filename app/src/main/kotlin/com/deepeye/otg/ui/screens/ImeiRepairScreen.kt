package com.deepeye.otg.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.window.Dialog
import com.deepeye.otg.ui.components.GlassButton
import com.deepeye.otg.ui.components.GlassCard
import com.deepeye.otg.ui.theme.GlassTokens
import com.deepeye.otg.repair.NvBridge
import dev.chrisbanes.haze.HazeState

@Composable
fun ImeiRepairScreen(
    onRepair: (String, String) -> Unit,
    onRead: () -> Unit,
    currentImei1: String = "N/A",
    currentImei2: String = "N/A",
    hazeState: HazeState? = null,
    perfMode: Boolean = false
) {
    var imei1 by remember { mutableStateOf("") }
    var imei2 by remember { mutableStateOf("") }
    
    val isImei1Valid = remember(imei1) { NvBridge.verifyImeiChecksum(imei1) }
    val isImei2Valid = remember(imei2) { NvBridge.verifyImeiChecksum(imei2) }
    
    var showSafetyConfirm by remember { mutableStateOf(false) }
    var showSafeDumpDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .background(Color.Transparent),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "IDENTITY REPAIR CENTER",
            color = Color(0xFF6750A4),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        )
        Text(
            "G7 RESTRICTED PROTOCOL",
            color = Color.Gray,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(Modifier.height(32.dp))

        // Current Identity Card
        GlassCard(
            hazeState = hazeState,
            performanceMode = perfMode,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("CURRENT DEVICE IDENTITY", color = Color.Gray, fontSize = 11.sp)
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    IdentityStatusItem("IMEI 1", currentImei1)
                    IdentityStatusItem("IMEI 2", currentImei2)
                }
                Spacer(Modifier.height(16.dp))
                GlassButton(onClick = onRead, label = "READ FROM NVRAM", modifier = Modifier.fillMaxWidth())
            }
        }

        Spacer(Modifier.height(24.dp))

        // Repair Input Card
        GlassCard(
            hazeState = hazeState,
            performanceMode = perfMode,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("TARGET IDENTITY RESTORATION", color = Color(0xFF9C6FFF), fontSize = 11.sp)
                Spacer(Modifier.height(16.dp))
                
                ImeiInputField(
                    value = imei1,
                    onValueChange = { if(it.length <= 15) imei1 = it },
                    label = "NEW IMEI 1",
                    isValid = isImei1Valid
                )
                
                Spacer(Modifier.height(16.dp))
                
                ImeiInputField(
                    value = imei2,
                    onValueChange = { if(it.length <= 15) imei2 = it },
                    label = "NEW IMEI 2",
                    isValid = isImei2Valid
                )

                Spacer(Modifier.height(24.dp))
                
                Button(
                    onClick = { showSafetyConfirm = true },
                    enabled = isImei1Valid && isImei2Valid,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if(isImei1Valid && isImei2Valid) Color(0xFF6750A4) else Color.DarkGray
                    ),
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("START REPAIR CHAIN", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showSafetyConfirm) {
        RepairSafetyDialog(
            onCancel = { showSafetyConfirm = false },
            onConfirm = {
                showSafetyConfirm = false
                onRepair(imei1, imei2)
            }
        )
    }
}

@Composable
fun IdentityStatusItem(label: String, value: String) {
    Column {
        Text(label, color = Color.Gray, fontSize = 10.sp)
        Text(value, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImeiInputField(value: String, onValueChange: (String) -> Unit, label: String, isValid: Boolean) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 11.sp) },
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Black.copy(alpha = 0.3f),
            unfocusedContainerColor = Color.Black.copy(alpha = 0.2f),
            focusedIndicatorColor = if (isValid) Color(0xFF6750A4) else Color.Red,
            unfocusedIndicatorColor = if (value.isEmpty()) Color.Gray else if (isValid) Color(0xFF6750A4) else Color.Red
        ),
        suffix = { 
            if (value.length == 15) {
                Text(if (isValid) "✓" else "✗", color = if (isValid) Color(0xFF6750A4) else Color.Red) 
            }
        }
    )
}

@Composable
fun RepairSafetyDialog(onCancel: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("MANDATORY SAFETY PROTOCOL") },
        text = { 
            Text("Repairing identity is a restricted operation. DeepEye will perform a SafeDump backup of NVRAM before proceeding. Continue?") 
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("PROCEED", color = Color(0xFF6750A4)) }
        },
        dismissButton = {
            TextButton(onClick = onCancel) { Text("CANCEL", color = Color.Gray) }
        },
        containerColor = Color(0xFF1E1E1E),
        titleContentColor = Color.White,
        textContentColor = Color.LightGray
    )
}
