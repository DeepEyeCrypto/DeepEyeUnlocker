package com.deepeye.otg.ui.screens

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deepeye.otg.ui.components.GlassButton
import com.deepeye.otg.ui.components.GlassCard
import com.deepeye.otg.ui.theme.GlassTokens
import dev.chrisbanes.haze.HazeState
import java.io.File

@Composable
fun ReportingScreen(
    reportFile: File?,
    viewModel: com.deepeye.otg.viewmodel.UsbViewModel
) {
    val hazeState = remember { HazeState() }
    val content = remember(reportFile) {
        reportFile?.readText() ?: "Error: Report file not found."
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.8f))
            .padding(24.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "FORENSIC AUDIT REPORT",
                style = MaterialTheme.typography.titleLarge,
                color = Color(0xFF4ADE80),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            GlassCard(
                hazeState = hazeState,
                modifier = Modifier.weight(1f).fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f))
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    Text(
                        text = content,
                        color = Color.White.copy(0.8f),
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                GlassButton(
                    label = "BACK",
                    onClick = { viewModel.dismissReport() },
                    modifier = Modifier.weight(1f),
                    accent = false
                )
                
                GlassButton(
                    label = "SHARE REPORT",
                    onClick = { reportFile?.let { viewModel.shareReport(it) } },
                    modifier = Modifier.weight(1f),
                    accent = true
                )
            }
        }
    }
}
