package com.deepeye.otg.ui.screens.apple

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.deepeye.otg.ui.components.GlassCard
import com.deepeye.otg.ui.theme.DeepEyeColors
import com.deepeye.otg.ui.theme.JetBrainsMonoFamily
import java.util.UUID

@Composable
fun GoldCtaButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(50.dp),
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = DeepEyeColors.GoldAccent,
            contentColor = Color.Black,
            disabledContainerColor = DeepEyeColors.Surface2,
            disabledContentColor = DeepEyeColors.TextMuted
        )
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Black
        )
    }
}

@Composable
fun HelloScreenBypassScreen(
    viewModel: HelloScreenViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // ── Device Input Card ───────────────────────
        GlassCard(
            hazeState = null,
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 16.dp
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    "DEVICE DETECTION",
                    style = MaterialTheme.typography.labelSmall,
                    color = DeepEyeColors.TextMuted
                )
                Spacer(Modifier.height(12.dp))

                // Model input
                OutlinedTextField(
                    value = state.model,
                    onValueChange = { viewModel.onModelChanged(it) },
                    label = { Text("iPhone Model (e.g. iPhone14,2)") },
                    placeholder = { Text("iPhone12,1") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DeepEyeColors.GoldAccent,
                        unfocusedBorderColor = DeepEyeColors.BorderGlass
                    )
                )
                Spacer(Modifier.height(8.dp))

                // iOS Version input
                OutlinedTextField(
                    value = state.iosVersion,
                    onValueChange = { viewModel.onIosVersionChanged(it) },
                    label = { Text("iOS Version (e.g. 17.2)") },
                    placeholder = { Text("16.7.8") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DeepEyeColors.GoldAccent,
                        unfocusedBorderColor = DeepEyeColors.BorderGlass
                    )
                )
                Spacer(Modifier.height(12.dp))

                // Check eligibility button
                GoldCtaButton(
                    text = "CHECK BYPASS ELIGIBILITY",
                    onClick = { viewModel.checkEligibility() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = state.model.isNotBlank()
                        && state.iosVersion.isNotBlank()
                        && !state.isLoading
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // ── Eligibility Result Card ─────────────────
        AnimatedVisibility(
            visible = state.eligibilityResult != null,
            enter = fadeIn() + expandVertically()
        ) {
            state.eligibilityResult?.let { result ->
                EligibilityResultCard(
                    result = result,
                    onRunBypass = { viewModel.runBypass() },
                    isRunning = state.isBypassing
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // ── DFU Instructions Card ───────────────────
        AnimatedVisibility(
            visible = state.dfuInstructions.isNotEmpty()
        ) {
            DfuInstructionsCard(state.dfuInstructions)
        }

        Spacer(Modifier.height(16.dp))

        // ── Bypass Progress ─────────────────────────
        AnimatedVisibility(
            visible = state.isBypassing || state.bypassLog.isNotEmpty()
        ) {
            BypassProgressCard(
                log = state.bypassLog,
                isRunning = state.isBypassing,
                progress = state.bypassProgress
            )
        }
    }
}

// ── Eligibility Card ────────────────────────────────
@Composable
private fun EligibilityResultCard(
    result: JSONObjectWrapper,
    onRunBypass: () -> Unit,
    isRunning: Boolean
) {
    val eligible = result.eligible
    val method   = result.bestMethod
    val chip     = result.chip
    val rate     = result.successRate

    GlassCard(
        hazeState = null,
        modifier = Modifier.fillMaxWidth(),
        accentColor = if (eligible) DeepEyeColors.Success else DeepEyeColors.Error,
        cornerRadius = 16.dp
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (eligible) Icons.Default.CheckCircle
                    else Icons.Default.Cancel,
                    contentDescription = null,
                    tint = if (eligible) DeepEyeColors.Success
                           else DeepEyeColors.Error,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    if (eligible) "BYPASS POSSIBLE ✅"
                    else "NOT SUPPORTED ❌",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (eligible) DeepEyeColors.Success
                            else DeepEyeColors.Error
                )
            }

            Spacer(Modifier.height(12.dp))

            // Chip + method info
            listOf(
                "Chip" to chip,
                "Method" to method.uppercase(),
                "Success Rate" to "$rate%",
            ).forEach { (k, v) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(k,
                        style = MaterialTheme.typography.bodyMedium,
                        color = DeepEyeColors.TextMuted)
                    Text(v,
                        style = MaterialTheme.typography.bodyMedium,
                        color = DeepEyeColors.TextPrimary,
                        fontWeight = FontWeight.SemiBold)
                }
            }

            if (eligible) {
                Spacer(Modifier.height(16.dp))
                GoldCtaButton(
                    text = if (isRunning) "BYPASSING..." else "▶ RUN BYPASS",
                    onClick = onRunBypass,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isRunning
                )
            }
        }
    }
}

// ── DFU Instructions ───────────────────────────────
@Composable
private fun DfuInstructionsCard(instructions: List<String>) {
    GlassCard(
        hazeState = null,
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 16.dp
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "DFU MODE INSTRUCTIONS",
                style = MaterialTheme.typography.labelSmall,
                color = DeepEyeColors.TextMuted
            )
            Spacer(Modifier.height(10.dp))
            instructions.forEach { step ->
                Row(
                    modifier = Modifier.padding(vertical = 3.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text("›",
                        color = DeepEyeColors.GoldAccent,
                        modifier = Modifier.width(20.dp))
                    Text(step,
                        style = MaterialTheme.typography.bodyMedium,
                        color = DeepEyeColors.TextPrimary)
                }
            }
        }
    }
}

// ── Bypass Progress ─────────────────────────────────
@Composable
private fun BypassProgressCard(
    log: List<String>,
    isRunning: Boolean,
    progress: Float
) {
    GlassCard(
        hazeState = null,
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 16.dp
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "BYPASS LOG",
                    style = MaterialTheme.typography.labelSmall,
                    color = DeepEyeColors.TextMuted
                )
                if (isRunning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = DeepEyeColors.GoldAccent,
                        strokeWidth = 2.dp
                    )
                }
            }
            if (isRunning) {
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth(),
                    color = DeepEyeColors.GoldAccent,
                    trackColor = DeepEyeColors.BorderGlass
                )
            }
            Spacer(Modifier.height(8.dp))
            log.takeLast(8).forEach { line ->
                Text(
                    line,
                    style = MaterialTheme.typography.labelSmall,
                    color = when {
                        "✅" in line || "success" in line.lowercase() ->
                            DeepEyeColors.Success
                        "❌" in line || "error" in line.lowercase() ->
                            DeepEyeColors.Error
                        else -> DeepEyeColors.TextMuted
                    },
                    fontFamily = JetBrainsMonoFamily
                )
            }
        }
    }
}

// Simple wrapper to avoid passing JSONObject through compose state
data class JSONObjectWrapper(
    val eligible: Boolean,
    val bestMethod: String,
    val chip: String,
    val successRate: Int,
    val instructions: List<String>
)
