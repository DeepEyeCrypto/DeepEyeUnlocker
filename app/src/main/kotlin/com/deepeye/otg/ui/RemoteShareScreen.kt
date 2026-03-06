package com.deepeye.otg.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.deepeye.otg.ui.components.GlassButton
import com.deepeye.otg.ui.theme.GlassTokens

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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GlassTokens.backgroundBrush),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "Remote Share", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(text = status)
            Text(text = subStatus)
            
            Spacer(modifier = Modifier.height(24.dp))
            
            if (sessionCode != null) {
                Text(text = "SESSION: $sessionCode", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(24.dp))
                GlassButton(label = "STOP SHARING", onClick = onStartSharing, accent = false)
            } else {
                GlassButton(label = "START SHARING", onClick = onStartSharing, accent = true)
            }
            
            Spacer(modifier = Modifier.height(48.dp))
            
            OutlinedTextField(
                value = sessionIdInput,
                onValueChange = { sessionIdInput = it },
                label = { Text("Enter Session ID") }
            )
            Spacer(modifier = Modifier.height(16.dp))
            GlassButton(label = "CONNECT", onClick = { onConnectRemote(sessionIdInput) }, accent = false)
            
            Spacer(modifier = Modifier.height(32.dp))
            GlassButton(label = "BACK", onClick = onBack, accent = false)
        }
    }
}
