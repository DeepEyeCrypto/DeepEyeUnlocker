package com.deepeye.otg.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deepeye.otg.domain.models.DeepEyeOperation
import com.deepeye.otg.ui.components.GlassButton
import com.deepeye.otg.ui.components.GlassCard
import com.deepeye.otg.ui.components.GlassProgressBar
import com.deepeye.otg.ui.theme.StitchTokens
import com.deepeye.otg.ui.viewmodel.LogEntry
import dev.chrisbanes.haze.HazeState

@Composable
fun ExecutingScreen(
    op: DeepEyeOperation?,
    progress: Int,
    statusMsg: String,
    logs: List<LogEntry>,
    onCancel: () -> Unit
) {
    val hazeState = remember { HazeState() }
    val listState = rememberLazyListState()

    // Auto-scroll to bottom of logs
    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            listState.animateScrollToItem(logs.size - 1)
        }
    }

    // Blurred Background Overlay
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f))
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // ── Primary Header ──────────────────────────────────────────────────
            Text(
                "ENGINE EXECUTION ACTIVE",
                style = StitchTokens.LabelSmall,
                color = StitchTokens.Primary,
                letterSpacing = 2.sp
            )
            
            Spacer(modifier = Modifier.height(32.dp))

            // ── Progress Card ───────────────────────────────────────────────────
            GlassCard(
                hazeState = hazeState,
                accentColor = StitchTokens.Primary,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = op?.label?.uppercase() ?: "RUNNING OPERATION",
                                style = StitchTokens.TitleLarge.copy(fontSize = 18.sp),
                                color = StitchTokens.TextPrimary
                            )
                            Text(
                                text = "TASK ID: ${op?.id?.uppercase() ?: "UNKNOWN"}",
                                style = StitchTokens.LabelSmall,
                                color = StitchTokens.TextSecondary
                            )
                        }
                        
                        // Animated % display
                        Text(
                            text = "$progress%",
                            style = StitchTokens.DisplayLarge.copy(fontSize = 32.sp),
                            color = StitchTokens.Primary
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Premium Progress Bar
                    ProgressBarWithPulse(progress = progress / 100f)

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = statusMsg,
                        style = StitchTokens.MonoCode,
                        color = StitchTokens.Primary.copy(0.8f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Terminal Log Card ───────────────────────────────────────────────
            Text(
                "HARDWARE BUS LOGS",
                style = StitchTokens.LabelSmall,
                color = StitchTokens.TextSecondary,
                modifier = Modifier.align(Alignment.Start).padding(start = 8.dp, bottom = 8.dp)
            )

            GlassCard(
                hazeState = hazeState,
                modifier = Modifier.weight(1f).fillMaxWidth()
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(0.4f))
                        .padding(16.dp)
                ) {
                    itemsIndexed(logs, key = { index, _ -> "exec_log_$index" }) { _, log ->
                        LogLine(log)
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ── Abort Control ────────────────────────────────────────────────────
            GlassButton(
                label = "ABORT OPERATION",
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                accent = false
            )
            
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun LogLine(log: LogEntry) {
    val color = when (log.type) {
        "SUCCESS" -> Color(0xFF4ADE80)
        "ERROR" -> Color(0xFFF87171)
        "WARNING" -> Color(0xFFFBBF24)
        else -> StitchTokens.TextMono
    }
    
    Row(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = "[${log.timestamp}]",
            style = StitchTokens.MonoCode.copy(fontSize = 10.sp),
            color = Color.White.copy(alpha = 0.3f)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = log.message,
            style = StitchTokens.MonoCode,
            color = color,
            lineHeight = 18.sp
        )
    }
}

@Composable
private fun ProgressBarWithPulse(progress: Float) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(CircleShape)
            .background(Color.White.copy(0.05f))
    ) {
        // Track Background Pulse
        val pulseTrans = rememberInfiniteTransition(label = "pulse")
        val alpha by pulseTrans.animateFloat(0.3f, 0.6f, infiniteRepeatable(tween(1000), RepeatMode.Reverse), label = "alpha")
        
        Box(
            modifier = Modifier
                .fillMaxWidth(progress.coerceIn(0.01f, 1f))
                .fillMaxHeight()
                .clip(CircleShape)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(StitchTokens.Primary.copy(alpha), StitchTokens.Primary)
                    )
                )
        )
    }
}
