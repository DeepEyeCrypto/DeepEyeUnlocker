package com.deepeye.otg.ui.screens.forensics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deepeye.otg.ui.components.GlassCard
import com.deepeye.otg.ui.components.NeonButton
import com.deepeye.otg.ui.components.NeonButtonStyle
import com.deepeye.otg.ui.state.ForensicsState
import com.deepeye.otg.ui.theme.DeepEyeColors

@Composable
fun IntelDashboardScreen(
    state: ForensicsState,
    onFetchIntel: (String) -> Unit
) {
    val report = state.intelReport

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── Device Header ─────────────────────────────
        GlassCard(hazeState = null, cornerRadius = 16.dp, accentColor = DeepEyeColors.GoldAccent) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(DeepEyeColors.GoldAccent.copy(0.1f), MaterialTheme.shapes.medium),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Public, null, tint = DeepEyeColors.GoldAccent)
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(
                        report?.model ?: "Detection Pending...",
                        color = DeepEyeColors.TextPrimary,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        "Risk Level: ${report?.riskLevel ?: "UNKNOWN"}",
                        color = when(report?.riskLevel) {
                            "CRITICAL" -> DeepEyeColors.Error
                            "MODERATE" -> DeepEyeColors.Warning
                            else -> DeepEyeColors.TextMuted
                        },
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }

        // ── Intel Summary ─────────────────────────────
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SummaryItem(
                label = "Vulnerabilities",
                value = report?.cveCount?.toString() ?: "0",
                icon = Icons.Default.BugReport,
                modifier = Modifier.weight(1f)
            )
            SummaryItem(
                label = "Intel Score",
                value = if(report != null) "A+" else "N/A",
                icon = Icons.Default.Verified,
                modifier = Modifier.weight(1f)
            )
        }

        // ── CVE List ──────────────────────────────────
        Text(
            "KNOWN VULNERABILITIES (CVE)",
            style = MaterialTheme.typography.labelSmall,
            color = DeepEyeColors.TextMuted,
            letterSpacing = 1.sp
        )

        GlassCard(
            hazeState = null,
            modifier = Modifier.weight(1f),
            cornerRadius = 16.dp
        ) {
            if (report == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    NeonButton(
                        text = "Query Intel Database",
                        onClick = { onFetchIntel("SM-G998B") }, // Mock model
                        icon = Icons.Default.CloudDownload,
                        style = NeonButtonStyle.SECONDARY
                    )
                }
            } else if (report.vulnerabilities.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No known CVEs for this firmware version.", color = DeepEyeColors.TextMuted)
                }
            } else {
                LazyColumn(Modifier.padding(8.dp)) {
                    items(report.vulnerabilities) { v ->
                        CveItem(v.id, v.score, v.description)
                        Divider(color = DeepEyeColors.SurfaceSubtle, thickness = 0.5.dp)
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryItem(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier) {
    GlassCard(hazeState = null, cornerRadius = 12.dp, modifier = modifier) {
        Column(Modifier.padding(12.dp)) {
            Icon(icon, null, tint = DeepEyeColors.TextFaint, modifier = Modifier.size(16.dp))
            Spacer(Modifier.height(4.dp))
            Text(value, color = DeepEyeColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Text(label, color = DeepEyeColors.TextMuted, fontSize = 10.sp)
        }
    }
}

@Composable
private fun CveItem(id: String, score: Double, desc: String) {
    Column(Modifier.fillMaxWidth().padding(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(id, color = DeepEyeColors.GoldAccent, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Box(
                modifier = Modifier
                    .background(
                        if(score >= 9.0) DeepEyeColors.Error.copy(0.2f) else DeepEyeColors.Warning.copy(0.2f),
                        MaterialTheme.shapes.extraSmall
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    score.toString(),
                    color = if(score >= 9.0) DeepEyeColors.Error else DeepEyeColors.Warning,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(desc, color = DeepEyeColors.TextMuted, fontSize = 11.sp, lineHeight = 14.sp)
    }
}
