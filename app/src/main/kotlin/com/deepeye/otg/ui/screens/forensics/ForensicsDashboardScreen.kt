package com.deepeye.otg.ui.screens.forensics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.deepeye.otg.ui.state.ForensicsAction
import com.deepeye.otg.ui.state.ForensicsTab
import com.deepeye.otg.ui.theme.DeepEyeColors
import com.deepeye.otg.viewmodel.research.ForensicsViewModel

@Composable
fun ForensicsDashboardScreen(
    viewModel: ForensicsViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepEyeColors.Background)
    ) {
        // ── Header ────────────────────────────────────
        ForensicsHeader(onBack = onBack)

        // ── Tabs ──────────────────────────────────────
        ForensicsTabs(
            selectedTab = state.selectedTab,
            onTabSelected = { viewModel.onAction(ForensicsAction.SelectTab(it)) }
        )

        // ── Content ───────────────────────────────────
        Box(modifier = Modifier.weight(1f)) {
            when (state.selectedTab) {
                ForensicsTab.INDEX -> PlaceholderContent("Filesystem Indexing Hub")
                ForensicsTab.TIMELINE -> PlaceholderContent("Forensic Timeline Analyzer")
                ForensicsTab.THREAT_SCAN -> MalwareScannerScreen(
                    state = state,
                    onStartScan = { viewModel.onAction(ForensicsAction.StartThreatScan) }
                )
                ForensicsTab.INTEL_HUB -> IntelDashboardScreen(
                    state = state,
                    onFetchIntel = { viewModel.onAction(ForensicsAction.FetchModelIntel(it)) }
                )
                ForensicsTab.VERIFICATION -> PlaceholderContent("Chain of Custody & Hash Verification")
                ForensicsTab.REPORTS -> PlaceholderContent("Forensic Report Generator")
            }
        }
    }
}

@Composable
private fun ForensicsHeader(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = DeepEyeColors.TextPrimary)
        }
        Spacer(Modifier.width(8.dp))
        Column {
            Text(
                "FORENSIC INTELLIGENCE",
                color = DeepEyeColors.GoldAccent,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Text(
                "NEURAL ANALYZER",
                color = DeepEyeColors.TextPrimary,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black
            )
        }
    }
}

@Composable
private fun ForensicsTabs(
    selectedTab: ForensicsTab,
    onTabSelected: (ForensicsTab) -> Unit
) {
    ScrollableTabRow(
        selectedTabIndex = selectedTab.ordinal,
        containerColor = Color.Transparent,
        contentColor = DeepEyeColors.GoldAccent,
        edgePadding = 16.dp,
        divider = {},
        indicator = { tabPositions ->
            TabRowDefaults.Indicator(
                Modifier.tabIndicatorOffset(tabPositions[selectedTab.ordinal]),
                color = DeepEyeColors.GoldAccent,
                height = 2.dp
            )
        }
    ) {
        ForensicsTab.values().forEach { tab ->
            Tab(
                selected = selectedTab == tab,
                onClick = { onTabSelected(tab) },
                text = {
                    Text(
                        tab.name.replace("_", " "),
                        fontSize = 11.sp,
                        fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Normal
                    )
                }
            )
        }
    }
}

@Composable
private fun PlaceholderContent(title: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Build, null, tint = DeepEyeColors.TextFaint, modifier = Modifier.size(48.dp))
            Spacer(Modifier.height(16.dp))
            Text(title, color = DeepEyeColors.TextMuted, fontWeight = FontWeight.Bold)
            Text("Module coming to neural core soon.", color = DeepEyeColors.TextFaint, fontSize = 12.sp)
        }
    }
}
