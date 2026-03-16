package com.deepeye.otg.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.deepeye.otg.intelligence.vulndb.*
import com.deepeye.otg.ui.state.*
import com.deepeye.otg.viewmodel.research.CveDashboardViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ──────────────────────────────────────────────────────────────
// CVE Intelligence Dashboard Screen
// DeepEye OTG — Full Production UI
// ──────────────────────────────────────────────────────────────

private val CyanAccent = Color(0xFF00E5FF)
private val SurfaceDark = Color(0xFF0A0A0A)
private val CardDark = Color(0xFF151515)
private val BorderDark = Color(0xFF333333)
private val MonoFont = FontFamily.Monospace

/**
 * CVE Intelligence Dashboard Screen.
 * Visualizes the current vulnerability database and patch state research.
 */
@Composable
fun CveDashboardScreen(
    viewModel: CveDashboardViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceDark)
    ) {
        // ── Header ──
        CveDashboardHeader(
            totalCount = state.totalCount,
            lastSync = state.lastSyncAt,
            onRefresh = { viewModel.onAction(CveDashboardAction.RefreshAll) },
            onBack = onNavigateBack
        )

        // ── Stats Row (Component Breakdown) ──
        CveStatsRow(
            stats = state.componentStats,
            onComponentClick = { viewModel.onAction(CveDashboardAction.FilterByComponent(it)) },
            activeComponents = state.filter.components
        )

        // ── Search & Filter ──
        CveSearchAndFilterHeader(
            query = state.searchQuery,
            onQueryChange = { viewModel.onAction(CveDashboardAction.Search(it)) },
            activeFilters = state.filter.hasActiveFilters,
            onClearFilters = { viewModel.onAction(CveDashboardAction.ClearFilters) },
            onToggleActiveExploits = { viewModel.onAction(CveDashboardAction.ToggleExploitFilter(it)) },
            isActiveExploitFilter = state.filter.exploitedOnly,
            resultCount = state.filteredEntries.size,
            totalCount = state.totalCount
        )

        // ── Main List ──
        Box(modifier = Modifier.weight(1f)) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(state.filteredEntries, key = { it.cveId }) { entry ->
                    CveEntryCard(
                        entry = entry,
                        onClick = { viewModel.onAction(CveDashboardAction.SelectEntry(entry.cveId)) }
                    )
                }

                if (state.filteredEntries.isEmpty() && !state.isLoading) {
                    item {
                        EmptyCveState(
                            hasFilters = state.filter.hasActiveFilters || state.searchQuery.isNotEmpty(),
                            onClearFilters = { viewModel.onAction(CveDashboardAction.ClearFilters) },
                            onImportSeed = { viewModel.onAction(CveDashboardAction.ImportSeedData) }
                        )
                    }
                }
            }

            // Loading Overlay
            if (state.isLoading || state.importProgress != null) {
                CveLoadingOverlay(state.importProgress)
            }
        }
    }

    // Detail Dialog
    state.selectedEntry?.let { entry ->
        CveDetailDialog(
            entry = entry,
            onDismiss = { viewModel.onAction(CveDashboardAction.ClearSelection) }
        )
    }
}

// ──────────────────────────────────────────────────────────────
// Header
// ──────────────────────────────────────────────────────────────

@Composable
private fun CveDashboardHeader(
    totalCount: Int,
    lastSync: Long,
    onRefresh: () -> Unit,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0D0D0D))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.Gray,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(Modifier.width(8.dp))
            Column {
                Text(
                    "CVE INTELLIGENCE",
                    color = CyanAccent,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                val syncText = if (lastSync > 0) {
                    val fmt = SimpleDateFormat("HH:mm:ss", Locale.US)
                    "VDB v2026.1 • $totalCount Indices • Sync ${fmt.format(Date(lastSync))}"
                } else {
                    "VDB v2026.1 • $totalCount Indices"
                }
                Text(syncText, color = Color.Gray, fontSize = 11.sp, fontFamily = MonoFont)
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            // Pulse dot if data present
            if (totalCount > 0) {
                val infiniteTransition = rememberInfiniteTransition(label = "pulseDot")
                val dotAlpha by infiniteTransition.animateFloat(
                    initialValue = 0.3f, targetValue = 1f,
                    animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse),
                    label = "dotPulse"
                )
                Box(
                    Modifier
                        .size(8.dp)
                        .alpha(dotAlpha)
                        .background(Color(0xFF4ADE80), CircleShape)
                )
                Spacer(Modifier.width(12.dp))
            }
            IconButton(onClick = onRefresh) {
                Icon(Icons.Default.Refresh, "Refresh", tint = CyanAccent)
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────
// Stats Row (Component Breakdown)
// ──────────────────────────────────────────────────────────────

@Composable
private fun CveStatsRow(
    stats: List<ComponentStat>,
    onComponentClick: (String) -> Unit,
    activeComponents: Set<String>
) {
    if (stats.isEmpty()) return

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(stats.take(8)) { stat ->
            val isActive = activeComponents.contains(stat.component)
            ComponentStatChip(stat, isActive) { onComponentClick(stat.component) }
        }
    }
}

@Composable
private fun ComponentStatChip(stat: ComponentStat, isActive: Boolean, onClick: () -> Unit) {
    val borderColor = if (isActive) CyanAccent else Color(0xFF444444)
    val bgColor = if (isActive) CyanAccent.copy(alpha = 0.12f) else Color(0xFF1A1A1A)

    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        color = bgColor,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stat.component, color = if (isActive) CyanAccent else Color.LightGray, fontSize = 11.sp)
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                stat.cnt.toString(),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                modifier = Modifier
                    .background(Color.White.copy(alpha = 0.08f), CircleShape)
                    .padding(horizontal = 5.dp, vertical = 1.dp)
            )
        }
    }
}

// ──────────────────────────────────────────────────────────────
// Search & Filter Header
// ──────────────────────────────────────────────────────────────

@Composable
private fun CveSearchAndFilterHeader(
    query: String,
    onQueryChange: (String) -> Unit,
    activeFilters: Boolean,
    onClearFilters: () -> Unit,
    onToggleActiveExploits: (Boolean) -> Unit,
    isActiveExploitFilter: Boolean,
    resultCount: Int,
    totalCount: Int
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        // Search bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .background(Color(0xFF1A1A1A), RoundedCornerShape(10.dp))
                .border(1.dp, Color(0xFF2A2A2A), RoundedCornerShape(10.dp))
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Search,
                    contentDescription = null,
                    tint = Color(0xFF666666),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(10.dp))
                Box(modifier = Modifier.weight(1f)) {
                    if (query.isEmpty()) {
                        Text(
                            "Search CVE IDs, components, summaries…",
                            color = Color(0xFF555555),
                            fontSize = 13.sp
                        )
                    }
                    BasicTextField(
                        value = query,
                        onValueChange = onQueryChange,
                        singleLine = true,
                        textStyle = TextStyle(
                            color = Color.White,
                            fontSize = 13.sp,
                            fontFamily = MonoFont
                        ),
                        cursorBrush = SolidColor(CyanAccent),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (query.isNotEmpty()) {
                    IconButton(
                        onClick = { onQueryChange("") },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.Clear, "Clear", tint = Color.Gray, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // Filter chips row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Active Exploitation toggle
                FilterChip(
                    selected = isActiveExploitFilter,
                    onClick = { onToggleActiveExploits(!isActiveExploitFilter) },
                    label = { Text("ACTIVE EXPLOITS", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                    leadingIcon = if (isActiveExploitFilter) {
                        { Icon(Icons.Default.CheckCircle, null, Modifier.size(14.dp)) }
                    } else null,
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = Color(0xFF1A1A1A),
                        selectedContainerColor = Color(0xFFFF1744).copy(alpha = 0.15f),
                        labelColor = Color.Gray,
                        selectedLabelColor = Color(0xFFFF1744),
                        selectedLeadingIconColor = Color(0xFFFF1744)
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = Color(0xFF333333),
                        selectedBorderColor = Color(0xFFFF1744).copy(alpha = 0.5f),
                        enabled = true,
                        selected = isActiveExploitFilter
                    )
                )

                if (activeFilters) {
                    TextButton(
                        onClick = onClearFilters,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            Icons.Default.FilterListOff,
                            null,
                            tint = Color(0xFFFF9100),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("CLEAR", color = Color(0xFFFF9100), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Result count
            Text(
                "$resultCount / $totalCount",
                color = Color(0xFF666666),
                fontSize = 11.sp,
                fontFamily = MonoFont
            )
        }

        Spacer(Modifier.height(8.dp))
    }
}

// ──────────────────────────────────────────────────────────────
// CVE Entry Card
// ──────────────────────────────────────────────────────────────

@Composable
private fun CveEntryCard(
    entry: CveEntry,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = CardDark),
        border = BorderStroke(1.dp, BorderDark),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    entry.cveId,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    fontFamily = MonoFont
                )
                CveSeverityBadge(entry.cvssScore)
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                entry.title,
                color = Color(0xFFCCCCCC),
                fontSize = 13.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        entry.component.uppercase(),
                        color = CyanAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    ExploitStatusBadge(entry.exploitedInWild ?: false, entry.cisaKev)
                }

                // Version chip
                if (entry.affectedVersions.isNotEmpty()) {
                    Text(
                        "VER: ${entry.affectedVersions.joinToString(", ")}",
                        color = Color(0xFF666666),
                        fontSize = 10.sp,
                        fontFamily = MonoFont
                    )
                }
            }

            // Confidence & CWE row
            if (entry.cwe.isNotEmpty() || entry.confidence != ConfidenceLevel.UNKNOWN) {
                Spacer(Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (entry.cwe.isNotEmpty()) {
                        Text(
                            entry.cwe,
                            color = Color(0xFF888888),
                            fontSize = 10.sp,
                            fontFamily = MonoFont,
                            modifier = Modifier
                                .background(Color(0xFF222222), RoundedCornerShape(3.dp))
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        )
                    }
                    ConfidenceBadge(entry.confidence)
                }
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────
// Badges
// ──────────────────────────────────────────────────────────────

@Composable
private fun CveSeverityBadge(score: Double?) {
    val color = when {
        score == null -> Color.Gray
        score >= 9.0 -> Color(0xFFFF1744)
        score >= 7.0 -> Color(0xFFFF9100)
        score >= 4.0 -> Color(0xFFFFD600)
        else -> Color(0xFF00E676)
    }

    val label = when {
        score == null -> "??"
        score >= 9.0 -> "CRIT"
        score >= 7.0 -> "HIGH"
        score >= 4.0 -> "MED"
        else -> "LOW"
    }

    Row(
        modifier = Modifier
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = score?.toString() ?: "??",
            color = color,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = MonoFont
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = label,
            color = color.copy(alpha = 0.7f),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ExploitStatusBadge(exploited: Boolean, cisa: Boolean) {
    if (!exploited && !cisa) return

    val text = if (cisa) "⚡ CISA KEV" else "◈ EXPLOITED"
    val color = if (cisa) Color(0xFFFF1744) else Color(0xFFFF9100)

    Text(
        text,
        color = color,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.5.sp,
        modifier = Modifier
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(3.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    )
}

@Composable
private fun ConfidenceBadge(level: ConfidenceLevel) {
    val (text, color) = when (level) {
        ConfidenceLevel.CONFIRMED -> "CONFIRMED" to Color(0xFF4ADE80)
        ConfidenceLevel.HIGH -> "HIGH" to Color(0xFF4ADE80)
        ConfidenceLevel.MEDIUM -> "MEDIUM" to Color(0xFFFFD600)
        ConfidenceLevel.LOW -> "LOW" to Color(0xFFFF9100)
        ConfidenceLevel.UNKNOWN -> "UNVERIFIED" to Color(0xFF666666)
    }

    Text(
        text,
        color = color,
        fontSize = 9.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.5.sp,
        modifier = Modifier
            .background(color.copy(alpha = 0.08f), RoundedCornerShape(3.dp))
            .padding(horizontal = 5.dp, vertical = 1.dp)
    )
}

// ──────────────────────────────────────────────────────────────
// Loading Overlay
// ──────────────────────────────────────────────────────────────

@Composable
private fun CveLoadingOverlay(progress: Float?) {
    val infiniteTransition = rememberInfiniteTransition(label = "loadPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.75f))
            .clickable(enabled = false) { },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Scanning ring
            CircularProgressIndicator(
                modifier = Modifier.size(64.dp).alpha(pulseAlpha),
                color = CyanAccent,
                strokeWidth = 3.dp,
                progress = { progress ?: 0f }
            )

            Spacer(Modifier.height(20.dp))

            Text(
                text = if (progress != null) "IMPORTING CVE DATA" else "LOADING",
                color = CyanAccent,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 3.sp,
                fontFamily = MonoFont
            )

            if (progress != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "${(progress * 100).toInt()}%",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = MonoFont
                )

                Spacer(Modifier.height(12.dp))

                // Progress bar
                Box(
                    modifier = Modifier
                        .width(200.dp)
                        .height(3.dp)
                        .background(Color(0xFF333333), RoundedCornerShape(2.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progress.coerceIn(0f, 1f))
                            .background(CyanAccent, RoundedCornerShape(2.dp))
                    )
                }
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────
// Empty State
// ──────────────────────────────────────────────────────────────

@Composable
private fun EmptyCveState(
    hasFilters: Boolean,
    onClearFilters: () -> Unit,
    onImportSeed: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            if (hasFilters) Icons.Default.FilterList else Icons.Default.Security,
            contentDescription = null,
            tint = Color(0xFF333333),
            modifier = Modifier.size(64.dp)
        )

        Spacer(Modifier.height(20.dp))

        Text(
            text = if (hasFilters) "NO MATCHING ENTRIES" else "CVE DATABASE EMPTY",
            color = Color(0xFF555555),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = if (hasFilters)
                "No CVEs match the current filters.\nTry broadening your search."
            else
                "Import seed data to populate the intelligence\ndatabase with Android framework research entries.",
            color = Color(0xFF444444),
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )

        Spacer(Modifier.height(24.dp))

        if (hasFilters) {
            OutlinedButton(
                onClick = onClearFilters,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanAccent),
                border = BorderStroke(1.dp, CyanAccent.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.FilterListOff, null, Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("CLEAR ALL FILTERS", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        } else {
            Button(
                onClick = onImportSeed,
                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Download, null, Modifier.size(16.dp), tint = Color.Black)
                Spacer(Modifier.width(8.dp))
                Text(
                    "IMPORT SEED DATA",
                    color = Color.Black,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(8.dp))

            Text(
                "Critical vulnerabilities for Android 12–16 pipeline testing",
                color = Color(0xFF444444),
                fontSize = 11.sp,
                fontFamily = MonoFont
            )
        }
    }
}

// ──────────────────────────────────────────────────────────────
// Detail Dialog
// ──────────────────────────────────────────────────────────────

@Composable
private fun CveDetailDialog(
    entry: CveEntry,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.85f),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF111111)),
            border = BorderStroke(1.dp, CyanAccent.copy(alpha = 0.2f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(20.dp)
            ) {
                // ── Header Section ──
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                entry.cveId,
                                color = Color.White,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = MonoFont
                            )
                            Spacer(Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    entry.component.uppercase(),
                                    color = CyanAccent,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp
                                )
                                entry.cwe?.let { cwe ->
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        cwe,
                                        color = Color.Gray,
                                        fontSize = 11.sp,
                                        fontFamily = MonoFont,
                                        modifier = Modifier
                                            .background(Color(0xFF222222), RoundedCornerShape(3.dp))
                                            .padding(horizontal = 5.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, "Close", tint = Color.Gray)
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                }

                // ── Severity & Status Row ──
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // CVSS Score card
                        DetailMetricCard(
                            label = "CVSS SCORE",
                            value = entry.cvssScore?.toString() ?: "N/A",
                            accent = entry.cvssScore?.let { score ->
                                when {
                                    score >= 9.0 -> Color(0xFFFF1744)
                                    score >= 7.0 -> Color(0xFFFF9100)
                                    else -> CyanAccent
                                }
                            } ?: Color.Gray,
                            modifier = Modifier.weight(1f)
                        )
                        DetailMetricCard(
                            label = "BUG CLASS",
                            value = entry.bugClass.name.replace("_", " "),
                            accent = CyanAccent,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(Modifier.height(20.dp))
                }

                // ── Title / Summary ──
                item {
                    DetailSection("DESCRIPTION")
                    Spacer(Modifier.height(8.dp))
                    Text(
                        entry.title.ifEmpty { "No description available." },
                        color = Color(0xFFDDDDDD),
                        fontSize = 14.sp,
                        lineHeight = 22.sp
                    )
                    Spacer(Modifier.height(20.dp))
                }

                // ── Affected Versions ──
                item {
                    DetailSection("AFFECTED VERSIONS")
                    Spacer(Modifier.height(8.dp))
                    if (entry.affectedVersions.isEmpty()) {
                        Text("None specified", color = Color(0xFF555555), fontSize = 12.sp)
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            entry.affectedVersions.forEach { version ->
                                Text(
                                    "iOS $version",
                                    color = Color(0xFFFF9100),
                                    fontSize = 12.sp,
                                    fontFamily = MonoFont,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .background(Color(0xFFFF9100).copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }

                // ── Fixed State ──
                item {
                    DetailSection("FIX LEVEL")
                    Spacer(Modifier.height(8.dp))
                    if (entry.patchedInSpl == null) {
                        Text(
                            "⚠ NO OFFICIAL PATCH OBSERVED",
                            color = Color(0xFFFF1744),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = MonoFont
                        )
                    } else {
                        Text(
                            "RESOLVED IN SPL: ${entry.patchedInSpl}",
                            color = Color(0xFF4ADE80),
                            fontSize = 12.sp,
                            fontFamily = MonoFont,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .background(Color(0xFF4ADE80).copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Spacer(Modifier.height(16.dp))
                }

                // ── Source References ──
                if (entry.sources.isNotEmpty()) {
                    item {
                        DetailSection("RESOURCES")
                        Spacer(Modifier.height(8.dp))
                        entry.sources.forEach { ref ->
                            Row(
                                modifier = Modifier.padding(vertical = 2.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text("→", color = CyanAccent, fontSize = 12.sp)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    ref,
                                    color = CyanAccent.copy(alpha = 0.8f),
                                    fontSize = 11.sp,
                                    fontFamily = MonoFont,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                    }
                }

                // ── Notes ──
                if (entry.notes.isNotBlank()) {
                    item {
                        DetailSection("ANALYST NOTES")
                        Spacer(Modifier.height(8.dp))
                        Text(
                            entry.notes,
                            color = Color(0xFFAAAAAA),
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            fontFamily = MonoFont,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF1A1A1A), RoundedCornerShape(6.dp))
                                .border(1.dp, Color(0xFF2A2A2A), RoundedCornerShape(6.dp))
                                .padding(12.dp)
                        )
                        Spacer(Modifier.height(16.dp))
                    }
                }

                // ── Metadata Footer ──
                item {
                    HorizontalDivider(color = Color(0xFF222222))
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
                        Text(
                            "Sync: ${fmt.format(Date(entry.updatedAt))}",
                            color = Color(0xFF555555),
                            fontSize = 10.sp,
                            fontFamily = MonoFont
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Verified,
                                null,
                                tint = Color(0xFF4ADE80),
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                "FORENSIC VERIFIED",
                                color = Color(0xFF4ADE80),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailMetricCard(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(Color(0xFF1A1A1A), RoundedCornerShape(8.dp))
            .border(1.dp, accent.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Text(
            label,
            color = Color(0xFF666666),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Spacer(Modifier.height(4.dp))
        Text(
            value,
            color = accent,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun DetailSection(title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .width(3.dp)
                .height(14.dp)
                .background(CyanAccent, RoundedCornerShape(2.dp))
        )
        Spacer(Modifier.width(8.dp))
        Text(
            title,
            color = CyanAccent.copy(alpha = 0.8f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        )
    }
}
