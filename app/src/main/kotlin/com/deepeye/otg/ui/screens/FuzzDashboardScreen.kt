package com.deepeye.otg.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deepeye.otg.ui.state.*
import com.deepeye.otg.viewmodel.research.FuzzDashboardViewModel

// ──────────────────────────────────────────────────────────────
// Fuzz Dashboard Screen
// DeepEye OTG — Full Production UI
// ──────────────────────────────────────────────────────────────

private val NeonRed = Color(0xFFFF1744)
private val NeonOrange = Color(0xFFFF9100)
private val NeonGreen = Color(0xFF00E676)
private val NeonCyan = Color(0xFF00E5FF)
private val NeonPurple = Color(0xFFAA00FF)
private val SurfaceDark = Color(0xFF0A0A0A)
private val CardDark = Color(0xFF151515)
private val BorderDark = Color(0xFF333333)
private val MonoFont = FontFamily.Monospace

/**
 * Fuzz Dashboard Screen — Crash Reproduction & Fuzzing Harness.
 * Real-time monitoring of fuzzing campaigns with crash triage.
 */
@Composable
fun FuzzDashboardScreen(
    viewModel: FuzzDashboardViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceDark)
    ) {
        // ── Header ──
        FuzzHeader(
            sessionId = state.sessionId,
            isRunning = state.isRunning,
            isPaused = state.isPaused,
            onBack = onNavigateBack
        )

        // ── Live Stats Bar ──
        FuzzStatsBar(
            totalExecutions = state.totalExecutions,
            totalCrashes = state.totalCrashes,
            uniqueCrashes = state.uniqueCrashes,
            executionsPerSecond = state.executionsPerSecond,
            elapsedMs = state.elapsedMs,
            corpusSize = state.corpusSize,
            isRunning = state.isRunning
        )

        // ── Control Bar ──
        FuzzControlBar(
            isRunning = state.isRunning,
            isPaused = state.isPaused,
            hasCompleted = state.hasCompleted,
            onStart = { viewModel.onAction(FuzzAction.StartFuzzing) },
            onStop = { viewModel.onAction(FuzzAction.StopFuzzing) },
            onPause = { viewModel.onAction(FuzzAction.PauseFuzzing) },
            onResume = { viewModel.onAction(FuzzAction.ResumeFuzzing) },
            onGenerateCorpus = { viewModel.onAction(FuzzAction.GenerateCorpus) },
            onReplay = { viewModel.onAction(FuzzAction.ReplayCrashes) }
        )

        // ── Error Banner ──
        state.error?.let { error ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .background(NeonRed.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                    .border(1.dp, NeonRed.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = "⚠ $error",
                    color = NeonRed,
                    fontSize = 12.sp,
                    fontFamily = MonoFont
                )
            }
        }

        // ── Main Content ──
        if (state.crashes.isEmpty() && !state.isRunning) {
            FuzzEmptyState(
                hasCompleted = state.hasCompleted,
                corpusSize = state.corpusSize,
                onGenerateCorpus = { viewModel.onAction(FuzzAction.GenerateCorpus) },
                onStart = { viewModel.onAction(FuzzAction.StartFuzzing) }
            )
        } else {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // ── Crash List (2/3 width) ──
                LazyColumn(
                    modifier = Modifier.weight(2f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    item {
                        Text(
                            text = "CRASHES (${state.crashes.size})",
                            color = NeonRed,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = MonoFont,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }

                    items(state.crashes) { crash ->
                        CrashEntryCard(
                            crash = crash,
                            onClick = { viewModel.onAction(FuzzAction.SelectCrash(crash.testCaseId)) }
                        )
                    }
                }

                // ── Config Panel (1/3 width) ──
                FuzzConfigPanel(
                    config = state.config,
                    isRunning = state.isRunning,
                    onUpdateConfig = { viewModel.onAction(FuzzAction.UpdateConfig(it)) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────
// Header
// ──────────────────────────────────────────────────────────────

@Composable
private fun FuzzHeader(
    sessionId: String,
    isRunning: Boolean,
    isPaused: Boolean,
    onBack: () -> Unit
) {
    // Pulsing animation for active indicator
    val infiniteTransition = rememberInfiniteTransition(label = "fuzz_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardDark)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Back button
        IconButton(
            onClick = onBack,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                Icons.Default.ArrowBack,
                contentDescription = "Back",
                tint = Color.White.copy(alpha = 0.6f),
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Title + status
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "FUZZ HARNESS",
                color = NeonRed,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                fontFamily = MonoFont,
                letterSpacing = 2.sp
            )
            if (sessionId.isNotEmpty()) {
                Text(
                    text = "SESSION: $sessionId",
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 10.sp,
                    fontFamily = MonoFont
                )
            }
        }

        // Active indicator
        if (isRunning) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .alpha(if (isPaused) 0.4f else pulseAlpha)
                        .background(if (isPaused) NeonOrange else NeonRed)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isPaused) "PAUSED" else "FUZZING",
                    color = if (isPaused) NeonOrange else NeonRed,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = MonoFont
                )
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────
// Live Stats Bar
// ──────────────────────────────────────────────────────────────

@Composable
private fun FuzzStatsBar(
    totalExecutions: Long,
    totalCrashes: Long,
    uniqueCrashes: Long,
    executionsPerSecond: Double,
    elapsedMs: Long,
    corpusSize: Int,
    isRunning: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatMetric(
            label = "EXEC",
            value = formatCount(totalExecutions),
            accent = NeonCyan,
            modifier = Modifier.weight(1f)
        )
        StatMetric(
            label = "EXEC/SEC",
            value = "%.1f".format(executionsPerSecond),
            accent = NeonGreen,
            modifier = Modifier.weight(1f)
        )
        StatMetric(
            label = "CRASHES",
            value = totalCrashes.toString(),
            accent = NeonRed,
            modifier = Modifier.weight(1f)
        )
        StatMetric(
            label = "UNIQUE",
            value = uniqueCrashes.toString(),
            accent = NeonOrange,
            modifier = Modifier.weight(1f)
        )
        StatMetric(
            label = "CORPUS",
            value = corpusSize.toString(),
            accent = NeonPurple,
            modifier = Modifier.weight(1f)
        )
        StatMetric(
            label = "ELAPSED",
            value = formatElapsed(elapsedMs),
            accent = Color.White.copy(alpha = 0.7f),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatMetric(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(CardDark, RoundedCornerShape(8.dp))
            .border(1.dp, BorderDark, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.35f),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = MonoFont,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            color = accent,
            fontSize = 16.sp,
            fontWeight = FontWeight.Black,
            fontFamily = MonoFont,
            textAlign = TextAlign.Center
        )
    }
}

// ──────────────────────────────────────────────────────────────
// Control Bar
// ──────────────────────────────────────────────────────────────

@Composable
private fun FuzzControlBar(
    isRunning: Boolean,
    isPaused: Boolean,
    hasCompleted: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onGenerateCorpus: () -> Unit,
    onReplay: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!isRunning) {
            // Start button
            FuzzActionButton(
                label = if (hasCompleted) "RESTART" else "START",
                icon = Icons.Default.PlayArrow,
                accent = NeonGreen,
                onClick = onStart,
                modifier = Modifier.weight(1f)
            )
        } else {
            // Pause / Resume
            if (isPaused) {
                FuzzActionButton(
                    label = "RESUME",
                    icon = Icons.Default.PlayArrow,
                    accent = NeonGreen,
                    onClick = onResume,
                    modifier = Modifier.weight(1f)
                )
            } else {
                FuzzActionButton(
                    label = "PAUSE",
                    icon = Icons.Default.Pause,
                    accent = NeonOrange,
                    onClick = onPause,
                    modifier = Modifier.weight(1f)
                )
            }

            // Stop button
            FuzzActionButton(
                label = "STOP",
                icon = Icons.Default.Stop,
                accent = NeonRed,
                onClick = onStop,
                modifier = Modifier.weight(1f)
            )
        }

        // Corpus generation
        FuzzActionButton(
            label = "GEN CORPUS",
            icon = Icons.Default.Dataset,
            accent = NeonPurple,
            onClick = onGenerateCorpus,
            modifier = Modifier.weight(1f)
        )

        // Replay crashes
        FuzzActionButton(
            label = "REPLAY",
            icon = Icons.Default.Replay,
            accent = NeonCyan,
            onClick = onReplay,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun FuzzActionButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(accent.copy(alpha = 0.08f))
            .border(1.dp, accent.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = accent,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            color = accent,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = MonoFont,
            maxLines = 1
        )
    }
}

// ──────────────────────────────────────────────────────────────
// Crash Entry Card
// ──────────────────────────────────────────────────────────────

@Composable
private fun CrashEntryCard(
    crash: CrashListItem,
    onClick: () -> Unit
) {
    val severityColor = when (crash.severity.uppercase()) {
        "CRITICAL" -> NeonRed
        "HIGH" -> Color(0xFFFF6D00)
        "MEDIUM" -> NeonOrange
        "LOW" -> Color(0xFFFDD835)
        else -> Color.White.copy(alpha = 0.5f)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(CardDark)
            .border(1.dp, BorderDark, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        // Top row: test case ID + severity badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = crash.testCaseId,
                color = NeonCyan,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = MonoFont,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )

            // Severity badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(severityColor.copy(alpha = 0.15f))
                    .border(1.dp, severityColor.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = crash.severity.uppercase(),
                    color = severityColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = MonoFont,
                    letterSpacing = 1.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Details row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Bucket
            Column {
                Text(
                    text = "BUCKET",
                    color = Color.White.copy(alpha = 0.3f),
                    fontSize = 9.sp,
                    fontFamily = MonoFont,
                    letterSpacing = 1.sp
                )
                Text(
                    text = crash.bucket.ifEmpty { "—" },
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 11.sp,
                    fontFamily = MonoFont,
                    maxLines = 1
                )
            }

            // Crash type
            Column {
                Text(
                    text = "TYPE",
                    color = Color.White.copy(alpha = 0.3f),
                    fontSize = 9.sp,
                    fontFamily = MonoFont,
                    letterSpacing = 1.sp
                )
                Text(
                    text = crash.crashType.ifEmpty { "—" },
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 11.sp,
                    fontFamily = MonoFont,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Component
            Column {
                Text(
                    text = "COMPONENT",
                    color = Color.White.copy(alpha = 0.3f),
                    fontSize = 9.sp,
                    fontFamily = MonoFont,
                    letterSpacing = 1.sp
                )
                Text(
                    text = crash.component.ifEmpty { "—" },
                    color = NeonPurple.copy(alpha = 0.8f),
                    fontSize = 11.sp,
                    fontFamily = MonoFont,
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Timestamp
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "TIME",
                    color = Color.White.copy(alpha = 0.3f),
                    fontSize = 9.sp,
                    fontFamily = MonoFont,
                    letterSpacing = 1.sp
                )
                Text(
                    text = formatTimestamp(crash.timestamp),
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 11.sp,
                    fontFamily = MonoFont
                )
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────
// Configuration Panel
// ──────────────────────────────────────────────────────────────

@Composable
private fun FuzzConfigPanel(
    config: FuzzConfigState,
    isRunning: Boolean,
    onUpdateConfig: (FuzzConfigState) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(10.dp))
            .background(CardDark)
            .border(1.dp, BorderDark, RoundedCornerShape(10.dp))
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Section header
        Text(
            text = "⚙ CONFIGURATION",
            color = NeonCyan,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = MonoFont,
            letterSpacing = 1.sp
        )

        HorizontalDivider(color = BorderDark)

        // Target surface
        ConfigItem(label = "TARGET SURFACE", value = config.targetSurface)

        // Max iterations
        ConfigItem(label = "MAX ITERATIONS", value = formatCount(config.maxIterations))

        // Duration
        ConfigItem(label = "DURATION", value = "${config.maxDurationMinutes} min")

        // Max input size
        ConfigItem(label = "MAX INPUT", value = "${config.maxInputSize} bytes")

        // Workers
        ConfigItem(label = "WORKERS", value = config.parallelWorkers.toString())

        // Save all
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "SAVE ALL INPUTS",
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 10.sp,
                fontFamily = MonoFont,
                letterSpacing = 1.sp
            )
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(
                        if (config.saveAllInputs) NeonGreen.copy(alpha = 0.2f)
                        else Color.White.copy(alpha = 0.05f)
                    )
                    .border(
                        1.dp,
                        if (config.saveAllInputs) NeonGreen.copy(alpha = 0.5f)
                        else Color.White.copy(alpha = 0.15f),
                        RoundedCornerShape(3.dp)
                    )
            ) {
                if (config.saveAllInputs) {
                    Text(
                        text = "✓",
                        color = NeonGreen,
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        HorizontalDivider(color = BorderDark)

        // Status summary
        Text(
            text = "STATUS",
            color = Color.White.copy(alpha = 0.3f),
            fontSize = 9.sp,
            fontFamily = MonoFont,
            letterSpacing = 1.sp
        )
        Text(
            text = if (isRunning) "Campaign in progress" else "Ready to start",
            color = if (isRunning) NeonGreen else Color.White.copy(alpha = 0.5f),
            fontSize = 11.sp,
            fontFamily = MonoFont
        )
    }
}

@Composable
private fun ConfigItem(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.4f),
            fontSize = 10.sp,
            fontFamily = MonoFont,
            letterSpacing = 1.sp
        )
        Text(
            text = value,
            color = Color.White.copy(alpha = 0.9f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = MonoFont
        )
    }
}

// ──────────────────────────────────────────────────────────────
// Empty State
// ──────────────────────────────────────────────────────────────

@Composable
private fun FuzzEmptyState(
    hasCompleted: Boolean,
    corpusSize: Int,
    onGenerateCorpus: () -> Unit,
    onStart: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Icon
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(NeonRed.copy(alpha = 0.08f))
                .border(1.dp, NeonRed.copy(alpha = 0.2f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (hasCompleted) "✓" else "⚡",
                fontSize = 32.sp,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = if (hasCompleted) "CAMPAIGN COMPLETE" else "NO ACTIVE CAMPAIGN",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = MonoFont,
            letterSpacing = 2.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (hasCompleted) {
                "Fuzzing campaign finished. No crashes found during this run."
            } else {
                "Start a new fuzzing campaign to discover crashes in USB HID parsing surfaces."
            },
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 12.sp,
            fontFamily = MonoFont,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 420.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // CTA buttons
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (corpusSize == 0) {
                FuzzActionButton(
                    label = "GENERATE SEED CORPUS",
                    icon = Icons.Default.Dataset,
                    accent = NeonPurple,
                    onClick = onGenerateCorpus
                )
            }

            FuzzActionButton(
                label = if (hasCompleted) "NEW CAMPAIGN" else "START FUZZING",
                icon = Icons.Default.PlayArrow,
                accent = NeonGreen,
                onClick = onStart
            )
        }

        if (corpusSize > 0) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "CORPUS: $corpusSize entries loaded",
                color = NeonPurple.copy(alpha = 0.6f),
                fontSize = 10.sp,
                fontFamily = MonoFont
            )
        }
    }
}

// ──────────────────────────────────────────────────────────────
// Utility Formatters
// ──────────────────────────────────────────────────────────────

private fun formatCount(count: Long): String = when {
    count >= 1_000_000 -> "%.1fM".format(count / 1_000_000.0)
    count >= 1_000 -> "%.1fK".format(count / 1_000.0)
    else -> count.toString()
}

private fun formatElapsed(ms: Long): String {
    val totalSec = ms / 1000
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) "%dh%02dm".format(h, m) else "%dm%02ds".format(m, s)
}

private fun formatTimestamp(ts: Long): String {
    if (ts == 0L) return "—"
    val fmt = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US)
    return fmt.format(java.util.Date(ts))
}
