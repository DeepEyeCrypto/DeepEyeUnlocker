package com.deepeye.otg.ui.gsmg

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deepeye.otg.data.gsmg.BypassFeature

/**
 * A12BypassCard
 * Premium UI component for A12+ GSMG features.
 */
@Composable
fun A12BypassCard(
    feature: BypassFeature,
    onRun: () -> Unit,
    progress: Int = 0,
    currentPhase: String = "Ready"
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1A1A1A)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = feature.displayName,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Cyan
                )
                
                Surface(
                    color = if (feature.signalAfter) Color(0xFF2E7D32) else Color(0xFF555555),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = if (feature.signalAfter) "Full Signal" else "WiFi Only",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        fontSize = 10.sp,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = feature.description,
                fontSize = 14.sp,
                color = Color.Gray
            )

            if (progress > 0) {
                Spacer(modifier = Modifier.height(16.dp))
                LinearProgressIndicator(
                    progress = { progress / 100f },
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.Cyan,
                    trackColor = Color(0xFF333333)
                )
                Text(
                    text = "$currentPhase ($progress%)",
                    fontSize = 12.sp,
                    color = Color.Cyan,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = onRun,
                modifier = Modifier.align(Alignment.End),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF00BCD4)
                )
            ) {
                Text("Run Bypass (${feature.costCredits} Cr)")
            }
        }
    }
}
