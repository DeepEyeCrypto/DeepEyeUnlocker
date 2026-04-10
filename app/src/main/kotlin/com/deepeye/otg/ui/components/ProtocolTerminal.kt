package com.deepeye.otg.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.*
import androidx.compose.ui.unit.*
import com.deepeye.otg.viewmodel.ProtocolLog

@Composable
fun ProtocolTerminal(
    logs:     List<ProtocolLog>,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()

    // Auto-scroll to bottom on new log
    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) listState.animateScrollToItem(logs.size - 1)
    }

    Box(
        modifier = modifier
            .background(Color(0xFF020208), RoundedCornerShape(8.dp))
            .border(1.dp, Color(0xFF39FF14).copy(alpha = 0.2f), RoundedCornerShape(8.dp))
            .padding(8.dp)
    ) {
        LazyColumn(state = listState) {
            items(logs) { log ->
                val color = when (log.level) {
                    "SUCCESS" -> Color(0xFF39FF14)
                    "ERROR"   -> Color(0xFFFF007F)
                    "WARN"    -> Color(0xFFFFD700)
                    else      -> Color(0xFF00FFFF).copy(alpha = 0.8f)
                }
                Row(Modifier.fillMaxWidth().padding(vertical = 1.dp)) {
                    Text(
                        text       = "[${log.time}] ",
                        color      = Color.White.copy(alpha = 0.3f),
                        fontSize   = 9.sp,
                        fontFamily = FontFamily.Monospace,
                    )
                    Text(
                        text       = log.message,
                        color      = color,
                        fontSize   = 9.sp,
                        fontFamily = FontFamily.Monospace,
                    )
                }
            }
        }
        // Blinking cursor at bottom
        if (logs.isEmpty()) {
            Text(
                text       = "$ waiting for device..._",
                color      = Color(0xFF39FF14).copy(alpha = 0.5f),
                fontSize   = 9.sp,
                fontFamily = FontFamily.Monospace,
                modifier   = Modifier.align(Alignment.TopStart)
            )
        }
    }
}
