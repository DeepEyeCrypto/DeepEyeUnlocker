package com.deepeye.otg.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun RemoteShareScreen(
    status: String,
    subStatus: String,
    sessionCode: String?,
    isDeviceDetected: Boolean,
    onStartSharing: () -> Unit,
    onConnectRemote: (String) -> Unit,
    onBack: () -> Unit
) {
    var sessionIdInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepEyeColors.DarkBackground)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Text("←", color = Color.White, fontSize = 24.sp)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "Remote Tunnel",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(40.dp))

        // Status Card
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(
                            if (isDeviceDetected) DeepEyeColors.SafeGreen else Color.Gray,
                            RoundedCornerShape(6.dp)
                        )
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(status, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(subStatus, color = DeepEyeColors.TextSecondary, fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (sessionCode != null) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text("Your Session ID:", color = DeepEyeColors.TextSecondary, fontSize = 12.sp)
                Text(
                    sessionCode,
                    color = DeepEyeColors.CyanAccent,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 4.sp
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        PrimaryIconButton(
            text = if (sessionCode == null) "START SHARING" else "STOP SHARING",
            onClick = onStartSharing,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            containerColor = if (sessionCode == null) DeepEyeColors.IndigoAccent else DeepEyeColors.RestrictedRed
        )

        Spacer(modifier = Modifier.height(48.dp))
        
        Divider(color = DeepEyeColors.GlassBorder)
        Spacer(modifier = Modifier.height(24.dp))

        Text("Connect to Remote Device", color = Color.White, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))

        TextField(
            value = sessionIdInput,
            onValueChange = { sessionIdInput = it },
            placeholder = { Text("Enter Session ID...") },
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.colors(
                unfocusedContainerColor = Color(0xFF1A1A1E),
                focusedContainerColor = Color(0xFF1A1A1E),
                unfocusedTextColor = Color.White,
                focusedTextColor = Color.White
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = { onConnectRemote(sessionIdInput) },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C2C30))
        ) {
            Text("CONNECT")
        }
    }
}
