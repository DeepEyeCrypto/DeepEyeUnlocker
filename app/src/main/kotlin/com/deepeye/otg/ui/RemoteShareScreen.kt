package com.deepeye.otg.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deepeye.otg.ui.components.GlassButton
import com.deepeye.otg.ui.components.GlassCard
import com.deepeye.otg.ui.theme.DeepEyeColors
import dev.chrisbanes.haze.HazeState

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
    val hazeState = remember { HazeState() }
    var sessionIdInput by remember { mutableStateOf("") }
    val clipboardManager = LocalClipboardManager.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(DeepEyeColors.BG_SURFACE, DeepEyeColors.BG_VOID)))
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.08f))
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.Black)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = "REMOTE ACCESS",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Text(
                    text = "Encrypted WebSocket Relay (Stage Q)",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Black.copy(alpha = 0.5f)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 100.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Mode A: Share Local Device (Provider)
            RemoteSectionHeader(
                icon = Icons.Default.Link,
                title = "SHARE LOCAL DEVICE",
                description = "Generate a session ID to share your USB connection."
            )

            GlassCard(
                hazeState = hazeState,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = status.uppercase(),
                        color = if (sessionCode != null) Color(0xFF166534) else Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Text(
                        text = subStatus,
                        color = Color.Black.copy(0.6f),
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    if (sessionCode != null) {
                        Spacer(modifier = Modifier.height(20.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.medium)
                                .background(Color.Black.copy(0.05f))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = sessionCode,
                                style = MaterialTheme.typography.headlineMedium,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFF6750A4)
                            )
                            IconButton(onClick = { clipboardManager.setText(AnnotatedString(sessionCode)) }) {
                                Icon(Icons.Default.ContentCopy, "Copy", tint = Color(0xFF6750A4).copy(0.7f), modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    
                    GlassButton(
                        label = if (sessionCode == null) "START RELAY" else "TERMINATE",
                        onClick = onStartSharing,
                        accent = sessionCode == null,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Mode B: Connect to Remote Device (Operator)
            RemoteSectionHeader(
                icon = Icons.Default.VpnKey,
                title = "JOIN REMOTE SESSION",
                description = "Enter a code to operate a remote USB device."
            )

            GlassCard(
                hazeState = hazeState,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    TextField(
                        value = sessionIdInput,
                        onValueChange = { sessionIdInput = it },
                        placeholder = { Text("6-digit Session ID", color = Color.Black.copy(0.3f)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.small),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Black.copy(0.02f),
                            unfocusedContainerColor = Color.Black.copy(0.02f),
                            focusedIndicatorColor = Color(0xFF6750A4),
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    GlassButton(
                        label = "CONNECT OPERATOR",
                        onClick = { onConnectRemote(sessionIdInput) },
                        accent = false,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
fun RemoteSectionHeader(icon: ImageVector, title: String, description: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = Color(0xFF6750A4), modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = Color.Black)
            Text(text = description, fontSize = 10.sp, color = Color.Black.copy(0.4f))
        }
    }
}
