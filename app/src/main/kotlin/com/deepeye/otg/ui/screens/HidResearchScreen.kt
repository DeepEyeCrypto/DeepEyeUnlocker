package com.deepeye.otg.ui.screens

import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deepeye.otg.ui.state.*
import com.deepeye.otg.viewmodel.research.HidResearchViewModel

// ──────────────────────────────────────────────────────────────
// HID Research Screen
// DeepEye OTG — Full Production UI
// ──────────────────────────────────────────────────────────────

private val ElectricBlue = Color(0xFF448AFF)
private val NeonCyan = Color(0xFF00E5FF)
private val NeonGreen = Color(0xFF00E676)
private val NeonOrange = Color(0xFFFF9100)
private val NeonRed = Color(0xFFFF1744)
private val NeonPurple = Color(0xFFAA00FF)
private val SurfaceDark = Color(0xFF0A0A0A)
private val CardDark = Color(0xFF151515)
private val BorderDark = Color(0xFF333333)
private val MonoFont = FontFamily.Monospace

/**
 * HID Research Screen — Descriptor Parsing & Variant Tracking.
 * Multi-tab interface for USB HID analysis tooling.
 */
@Composable
fun HidResearchScreen(
    viewModel: HidResearchViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceDark)
    ) {
        // ── Header ──
        HidHeader(onBack = onNavigateBack)

        // ── Tab Bar ──
        HidTabBar(
            selectedTab = state.selectedTab,
            onSelectTab = { viewModel.onAction(HidAction.SelectTab(it)) }
        )

        // ── Tab Content ──
        when (state.selectedTab) {
            HidTab.PARSER -> HidParserTab(
                descriptor = state.currentDescriptor,
                items = state.parsedItems,
                malformations = state.malformations,
                collections = state.collections,
                error = state.error
            )
            HidTab.VARIANTS -> HidVariantsTab(
                variants = state.trackedVariants,
                comparison = state.comparisonResult,
                onSelectVariant = { viewModel.onAction(HidAction.SelectVariant(it)) }
            )
            HidTab.CORPUS -> HidCorpusTab(
                corpusGenerated = state.corpusGenerated,
                corpusFileCount = state.corpusFileCount,
                onGenerate = { viewModel.onAction(HidAction.GenerateCorpus) }
            )
            HidTab.CRASH_REPORTS -> HidCrashReportsTab()
        }
    }
}

// ──────────────────────────────────────────────────────────────
// Header
// ──────────────────────────────────────────────────────────────

@Composable
private fun HidHeader(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardDark)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
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

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "HID RESEARCH LAB",
                color = ElectricBlue,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                fontFamily = MonoFont,
                letterSpacing = 2.sp
            )
            Text(
                text = "USB HID Descriptor Analysis & Variant Tracking",
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 10.sp,
                fontFamily = MonoFont
            )
        }

        // HID protocol spec badge
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(ElectricBlue.copy(alpha = 0.1f))
                .border(1.dp, ElectricBlue.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = "HID 1.11",
                color = ElectricBlue,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = MonoFont
            )
        }
    }
}

// ──────────────────────────────────────────────────────────────
// Tab Bar
// ──────────────────────────────────────────────────────────────

@Composable
private fun HidTabBar(
    selectedTab: HidTab,
    onSelectTab: (HidTab) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardDark.copy(alpha = 0.6f))
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        HidTab.entries.forEach { tab ->
            val isSelected = tab == selectedTab
            val tabColor = when (tab) {
                HidTab.PARSER -> ElectricBlue
                HidTab.VARIANTS -> NeonPurple
                HidTab.CORPUS -> NeonGreen
                HidTab.CRASH_REPORTS -> NeonRed
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        if (isSelected) tabColor.copy(alpha = 0.15f)
                        else Color.Transparent
                    )
                    .then(
                        if (isSelected) Modifier.border(1.dp, tabColor.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                        else Modifier
                    )
                    .clickable { onSelectTab(tab) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = tab.name.replace("_", " "),
                    color = if (isSelected) tabColor else Color.White.copy(alpha = 0.4f),
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    fontFamily = MonoFont,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────
// Parser Tab
// ──────────────────────────────────────────────────────────────

@Composable
private fun HidParserTab(
    descriptor: HidDescriptorSummary?,
    items: List<HidItemDisplay>,
    malformations: List<HidMalformationDisplay>,
    collections: List<HidCollectionDisplay>,
    error: String?
) {
    if (descriptor == null && error == null) {
        // No descriptor loaded
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(ElectricBlue.copy(alpha = 0.08f))
                    .border(1.dp, ElectricBlue.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "◇", fontSize = 28.sp, color = ElectricBlue)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "NO DESCRIPTOR LOADED",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = MonoFont,
                letterSpacing = 2.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Parse a USB HID Report Descriptor to analyze its structure,\ndetect malformations, and identify crash patterns.",
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 11.sp,
                fontFamily = MonoFont,
                textAlign = TextAlign.Center
            )
        }
        return
    }

    // Error display
    error?.let {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .background(NeonRed.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                .border(1.dp, NeonRed.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                .padding(12.dp)
        ) {
            Text(text = "⚠ $it", color = NeonRed, fontSize = 12.sp, fontFamily = MonoFont)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        // ── Summary Card ──
        descriptor?.let { desc ->
            item {
                ParserSummaryCard(desc)
            }
        }

        // ── Malformations ──
        if (malformations.isNotEmpty()) {
            item {
                SectionLabel("MALFORMATIONS (${malformations.size})", NeonRed)
            }
            items(malformations) { malform ->
                MalformationCard(malform)
            }
        }

        // ── Collections ──
        if (collections.isNotEmpty()) {
            item {
                SectionLabel("COLLECTIONS (${collections.size})", ElectricBlue)
            }
            items(collections) { col ->
                CollectionCard(col)
            }
        }

        // ── Items (first 50) ──
        if (items.isNotEmpty()) {
            item {
                SectionLabel("ITEMS (${items.size})", NeonCyan)
            }
            items(items.take(50)) { hidItem ->
                HidItemRow(hidItem)
            }
            if (items.size > 50) {
                item {
                    Text(
                        text = "... and ${items.size - 50} more items",
                        color = Color.White.copy(alpha = 0.3f),
                        fontSize = 10.sp,
                        fontFamily = MonoFont,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ParserSummaryCard(desc: HidDescriptorSummary) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(CardDark)
            .border(1.dp, BorderDark, RoundedCornerShape(10.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "DESCRIPTOR ANALYSIS",
                color = ElectricBlue,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = MonoFont,
                letterSpacing = 1.sp
            )

            // Health badge
            val healthColor = when {
                desc.hasCriticalIssues -> NeonRed
                !desc.isWellFormed -> NeonOrange
                else -> NeonGreen
            }
            val healthText = when {
                desc.hasCriticalIssues -> "CRITICAL"
                !desc.isWellFormed -> "MALFORMED"
                else -> "WELL-FORMED"
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(healthColor.copy(alpha = 0.15f))
                    .border(1.dp, healthColor.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = healthText,
                    color = healthColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = MonoFont,
                    letterSpacing = 1.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Metric grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MiniMetric("SIZE", "${desc.rawSize}B", NeonCyan, Modifier.weight(1f))
            MiniMetric("ITEMS", "${desc.totalItems}", ElectricBlue, Modifier.weight(1f))
            MiniMetric("COLLECTIONS", "${desc.totalCollections}", NeonPurple, Modifier.weight(1f))
            MiniMetric("MALFORMS", "${desc.malformationCount}", NeonRed, Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Usage pages
        if (desc.usagePages.isNotEmpty()) {
            Text(
                text = "USAGE PAGES: ${desc.usagePages.joinToString(", ")}",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 10.sp,
                fontFamily = MonoFont
            )
        }

        // Report IDs
        if (desc.reportIds.isNotEmpty()) {
            Text(
                text = "REPORT IDs: ${desc.reportIds.joinToString(", ")}",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 10.sp,
                fontFamily = MonoFont
            )
        }
    }
}

@Composable
private fun MiniMetric(label: String, value: String, accent: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(accent.copy(alpha = 0.06f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = label, color = Color.White.copy(alpha = 0.3f), fontSize = 8.sp, fontFamily = MonoFont, letterSpacing = 1.sp)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = value, color = accent, fontSize = 14.sp, fontWeight = FontWeight.Black, fontFamily = MonoFont)
    }
}

@Composable
private fun MalformationCard(malform: HidMalformationDisplay) {
    val sevColor = when (malform.severity.uppercase()) {
        "CRITICAL" -> NeonRed
        "ERROR" -> Color(0xFFFF6D00)
        "WARNING" -> NeonOrange
        else -> Color.White.copy(alpha = 0.5f)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(sevColor.copy(alpha = 0.06f))
            .border(1.dp, sevColor.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Severity pip
        Box(
            modifier = Modifier
                .padding(top = 3.dp)
                .size(8.dp)
                .clip(CircleShape)
                .background(sevColor)
        )
        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = malform.type.uppercase(),
                    color = sevColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = MonoFont
                )
                Text(
                    text = "0x${"%04X".format(malform.offset)}",
                    color = Color.White.copy(alpha = 0.3f),
                    fontSize = 10.sp,
                    fontFamily = MonoFont
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = malform.description,
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 10.sp,
                fontFamily = MonoFont
            )
        }
    }
}

@Composable
private fun CollectionCard(col: HidCollectionDisplay) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(CardDark)
            .border(1.dp, BorderDark, RoundedCornerShape(6.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Depth indicator
        repeat(col.depth) {
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(20.dp)
                    .background(ElectricBlue.copy(alpha = 0.3f))
            )
            Spacer(modifier = Modifier.width(6.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = col.typeName,
                color = ElectricBlue,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = MonoFont
            )
            Text(
                text = "${col.usagePage} • Usage 0x${"%02X".format(col.usage)} • ${col.itemCount} items",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 10.sp,
                fontFamily = MonoFont
            )
        }
    }
}

@Composable
private fun HidItemRow(item: HidItemDisplay) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Offset
        Text(
            text = "%04X".format(item.offset),
            color = Color.White.copy(alpha = 0.25f),
            fontSize = 10.sp,
            fontFamily = MonoFont,
            modifier = Modifier.width(40.dp)
        )

        // Type badge
        val typeColor = when (item.type) {
            "MAIN" -> NeonGreen
            "GLOBAL" -> ElectricBlue
            "LOCAL" -> NeonOrange
            else -> Color.White.copy(alpha = 0.3f)
        }
        Text(
            text = item.type.take(3),
            color = typeColor,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = MonoFont,
            modifier = Modifier.width(30.dp)
        )

        // Tag name
        Text(
            text = item.tagName,
            color = Color.White.copy(alpha = 0.9f),
            fontSize = 11.sp,
            fontFamily = MonoFont,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        // Data value
        Text(
            text = item.dataValue.toString(),
            color = NeonCyan.copy(alpha = 0.7f),
            fontSize = 11.sp,
            fontFamily = MonoFont,
            modifier = Modifier.width(60.dp),
            textAlign = TextAlign.End
        )

        // Raw hex
        Text(
            text = item.rawHex,
            color = Color.White.copy(alpha = 0.2f),
            fontSize = 9.sp,
            fontFamily = MonoFont,
            modifier = Modifier.width(80.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ──────────────────────────────────────────────────────────────
// Variants Tab
// ──────────────────────────────────────────────────────────────

@Composable
private fun HidVariantsTab(
    variants: List<VariantListItem>,
    comparison: ComparisonDisplay?,
    onSelectVariant: (String) -> Unit
) {
    if (variants.isEmpty()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "△", fontSize = 32.sp, color = NeonPurple.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "NO TRACKED VARIANTS",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = MonoFont,
                letterSpacing = 2.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Add descriptor variants from the Parser tab to begin tracking.",
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 11.sp,
                fontFamily = MonoFont,
                textAlign = TextAlign.Center
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        // Comparison result
        comparison?.let { comp ->
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(NeonPurple.copy(alpha = 0.06f))
                        .border(1.dp, NeonPurple.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = "COMPARISON: ${comp.variantA} ↔ ${comp.variantB}",
                        color = NeonPurple,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = MonoFont
                    )
                    Text(
                        text = "${comp.diffCount} byte diffs • ${comp.summary}",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 10.sp,
                        fontFamily = MonoFont
                    )
                }
            }
        }

        item {
            SectionLabel("TRACKED VARIANTS (${variants.size})", NeonPurple)
        }

        items(variants) { variant ->
            VariantCard(variant, onClick = { onSelectVariant(variant.id) })
        }
    }
}

@Composable
private fun VariantCard(variant: VariantListItem, onClick: () -> Unit) {
    val catColor = when (variant.category.uppercase()) {
        "CRASH_TRIGGER" -> NeonRed
        "MALFORMED" -> NeonOrange
        "EDGE_CASE" -> Color(0xFFFDD835)
        "BASELINE" -> NeonGreen
        "PATCHED" -> ElectricBlue
        else -> Color.White.copy(alpha = 0.5f)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(CardDark)
            .border(1.dp, BorderDark, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Crash indicator
        if (variant.hasCrash) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(NeonRed.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "⚡", fontSize = 10.sp)
            }
            Spacer(modifier = Modifier.width(10.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = variant.name,
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = MonoFont,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${variant.driverFamily} • ${variant.descriptorSize}B • ${variant.effectCount} effects",
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 10.sp,
                fontFamily = MonoFont
            )
        }

        // Category badge
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(catColor.copy(alpha = 0.12f))
                .border(1.dp, catColor.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                .padding(horizontal = 6.dp, vertical = 3.dp)
        ) {
            Text(
                text = variant.category.replace("_", " "),
                color = catColor,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = MonoFont,
                letterSpacing = 0.5.sp
            )
        }
    }
}

// ──────────────────────────────────────────────────────────────
// Corpus Tab
// ──────────────────────────────────────────────────────────────

@Composable
private fun HidCorpusTab(
    corpusGenerated: Boolean,
    corpusFileCount: Int,
    onGenerate: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (!corpusGenerated) {
            // Generate corpus CTA
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(NeonGreen.copy(alpha = 0.08f))
                    .border(1.dp, NeonGreen.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Dataset,
                    contentDescription = null,
                    tint = NeonGreen,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "GENERATE REFERENCE CORPUS",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = MonoFont,
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Generate valid, edge-case, malformed, and random\nHID descriptors for offline analysis.",
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 11.sp,
                fontFamily = MonoFont,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(NeonGreen.copy(alpha = 0.1f))
                    .border(1.dp, NeonGreen.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    .clickable(onClick = onGenerate)
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "GENERATE CORPUS",
                    color = NeonGreen,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = MonoFont
                )
            }
        } else {
            // Corpus generated
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(NeonGreen.copy(alpha = 0.15f))
                    .border(1.dp, NeonGreen.copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "✓", fontSize = 32.sp, color = NeonGreen)
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "CORPUS READY",
                color = NeonGreen,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = MonoFont,
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "$corpusFileCount descriptors generated across 4 categories:\nvalid, edge-case, malformed, and random.",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 11.sp,
                fontFamily = MonoFont,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Corpus category breakdown
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CorpusCategoryChip("VALID", NeonGreen)
                CorpusCategoryChip("EDGE", Color(0xFFFDD835))
                CorpusCategoryChip("MALFORMED", NeonOrange)
                CorpusCategoryChip("RANDOM", NeonPurple)
            }
        }
    }
}

@Composable
private fun CorpusCategoryChip(label: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.1f))
            .border(1.dp, color.copy(alpha = 0.25f), RoundedCornerShape(6.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = MonoFont,
            letterSpacing = 1.sp
        )
    }
}

// ──────────────────────────────────────────────────────────────
// Crash Reports Tab
// ──────────────────────────────────────────────────────────────

@Composable
private fun HidCrashReportsTab() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(NeonRed.copy(alpha = 0.08f))
                .border(1.dp, NeonRed.copy(alpha = 0.2f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "⚠", fontSize = 28.sp, color = NeonRed.copy(alpha = 0.6f))
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "NO CRASH REPORTS",
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = MonoFont,
            letterSpacing = 2.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Crash reports will appear here when descriptors trigger\ncrashes during analysis or testing.",
            color = Color.White.copy(alpha = 0.4f),
            fontSize = 11.sp,
            fontFamily = MonoFont,
            textAlign = TextAlign.Center
        )
    }
}

// ──────────────────────────────────────────────────────────────
// Shared Components
// ──────────────────────────────────────────────────────────────

@Composable
private fun SectionLabel(text: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(14.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = MonoFont,
            letterSpacing = 1.sp
        )
    }
}
