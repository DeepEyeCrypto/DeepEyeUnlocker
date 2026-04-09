package com.deepeye.otg.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deepeye.otg.data.HWIDEngine
import com.deepeye.otg.ui.theme.DeepEyeColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivationOverlay(
    onDismiss: () -> Unit,
    onActivate: (String) -> Unit
) {
    var key by remember { mutableStateOf("") }
    val hwid = remember { HWIDEngine.getHWID() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 400.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(DeepEyeColors.BG_SURFACE.copy(0.6f))
                .border(1.dp, DeepEyeColors.WHITE_LOW.copy(0.4f), RoundedCornerShape(24.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "DEEPEYE CLOUD",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Cyan.copy(alpha = 0.7f),
                letterSpacing = 2.sp
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                "Activate License",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(24.dp))

            // HWID Display
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.3f))
                    .padding(12.dp)
            ) {
                Text(
                    "HARDWARE ID",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
                Text(
                    hwid,
                    fontFamily = FontFamily.Monospace,
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Key Input
            OutlinedTextField(
                value = key,
                onValueChange = { key = it.uppercase() },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("XXXX-XXXX-XXXX-XXXX", color = Color.Gray) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Cyan,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel", color = Color.Gray)
                }

                Button(
                    onClick = { if (key.length > 5) onActivate(key) },
                    modifier = Modifier.weight(1.5f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    contentPadding = PaddingValues()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFF00F2FE), Color(0xFF4FACFE))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Activate Now", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
