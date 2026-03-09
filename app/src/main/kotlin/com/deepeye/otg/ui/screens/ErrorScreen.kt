package com.deepeye.otg.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deepeye.otg.ui.components.GlassButton
import com.deepeye.otg.ui.components.GlassCard
import com.deepeye.otg.ui.theme.StitchTokens
import dev.chrisbanes.haze.HazeState

@Composable
fun ErrorScreen(
    message: String,
    onRetry: () -> Unit
) {
    val hazeState = remember { HazeState() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.9f))
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Error Icon with Glow
            Box(contentAlignment = Alignment.Center) {
                Box(Modifier.size(100.dp).background(StitchTokens.AccentEdl.copy(0.1f), CircleShape))
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    tint = StitchTokens.AccentEdl
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            Text(
                "ENGINE FAULT DETECTED",
                style = StitchTokens.LabelSmall,
                color = StitchTokens.AccentEdl,
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = message,
                style = StitchTokens.TitleLarge.copy(lineHeight = 28.sp),
                color = StitchTokens.TextPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(64.dp))

            GlassButton(
                label = "RETRY / ACKNOWLEDGE",
                onClick = onRetry,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                accent = true
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                "Frequent errors may indicate cable failure or incompatible DA.",
                style = StitchTokens.LabelSmall,
                color = StitchTokens.TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
        }
    }
}
