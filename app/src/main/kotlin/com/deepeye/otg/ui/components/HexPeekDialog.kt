package com.deepeye.otg.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.chrisbanes.haze.HazeState

@Composable
fun HexPeekDialog(
    hex: String,
    onDismiss: () -> Unit
) {
    val hazeState = remember { HazeState() }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.85f)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "FORENSIC SECTOR PREVIEW",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.Cyan
                )
                
                GlassButton(
                    label = "DISMISS",
                    onClick = onDismiss,
                    modifier = Modifier.width(90.dp),
                    accent = false
                )
            }

            GlassCard(
                hazeState = hazeState,
                modifier = Modifier.weight(1f).fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = hex,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = Color.Green.copy(0.7f),
                        lineHeight = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Viewing offset: 0x00000000 (Physical Sector 0)",
                fontSize = 10.sp,
                color = Color.White.copy(0.5f),
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}
