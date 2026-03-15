package com.deepeye.otg.ui.device

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.deepeye.otg.data.device.DeviceProtocol

// Deepeye dark theme colors
private val BG       = Color(0xFF0A0A0A)
private val SURFACE  = Color(0xFF111111)
private val ACCENT   = Color(0xFF2196F3)
private val GREEN    = Color(0xFF00E676)
private val ORANGE   = Color(0xFFFF9800)
private val RED      = Color(0xFFFF1744)
private val TEXT     = Color(0xFFEEEEEE)
private val SUBTEXT  = Color(0xFF888888)

@Composable
fun DeviceSupportScreen(
    viewModel: DeviceSupportViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BG)
            .padding(16.dp)
    ) {
        // Header
        Text("DEVICE DATABASE",
            color = TEXT, fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp)
        Text("${state.totalDevices} devices · 29 brands",
            color = SUBTEXT, fontSize = 12.sp)

        Spacer(Modifier.height(12.dp))

        // Protocol stats bar
        ProtocolStatsRow(state.protocolCounts)

        Spacer(Modifier.height(16.dp))

        // Search
        OutlinedTextField(
            value         = state.searchQuery,
            onValueChange = viewModel::onSearch,
            placeholder   = { Text("Search brand or model...",
                               color = SUBTEXT) },
            modifier = Modifier.fillMaxWidth(),
            colors   = OutlinedTextFieldDefaults.colors(
                focusedBorderColor   = ACCENT,
                unfocusedBorderColor = Color(0xFF333333),
                focusedTextColor     = TEXT,
                unfocusedTextColor   = TEXT,
                cursorColor          = ACCENT,
            ),
            singleLine = true,
        )

        Spacer(Modifier.height(12.dp))

        if (state.searchQuery.length >= 2) {
            // Search results
            SearchResultsList(
                results  = state.searchResults,
                onSelect = { entry ->
                    viewModel.onBrandSelected(entry.brand)
                    viewModel.onModelSelected(entry.model)
                }
            )
        } else {
            // Brand → Model picker
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Brand column
                BrandPicker(
                    brands   = state.brands,
                    selected = state.selectedBrand,
                    onSelect = viewModel::onBrandSelected,
                    modifier = Modifier.weight(1f),
                )
                // Model column
                ModelPicker(
                    models   = state.models,
                    selected = state.selectedModel,
                    onSelect = viewModel::onModelSelected,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        // Routing result
        state.routingResult?.let { result ->
            Spacer(Modifier.height(16.dp))
            RoutingResultCard(result)
        }
    }
}

@Composable
fun ProtocolStatsRow(counts: Map<DeviceProtocol, Int>) {
    val total = counts.values.sum().takeIf { it > 0 } ?: 1
    Row(
        Modifier.fillMaxWidth().height(8.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        counts.entries.sortedByDescending { it.value }.forEach { (proto, count) ->
            Box(
                Modifier
                    .weight(count.toFloat())
                    .fillMaxHeight()
                    .background(proto.color())
            )
        }
    }
    Spacer(Modifier.height(4.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        counts.entries.sortedByDescending { it.value }.forEach { (proto, count) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).background(proto.color()))
                Spacer(Modifier.width(4.dp))
                Text("${proto.shortName()} $count",
                    color = SUBTEXT, fontSize = 10.sp)
            }
        }
    }
}

@Composable
fun BrandPicker(
    brands: List<String>, selected: String?,
    onSelect: (String) -> Unit, modifier: Modifier
) {
    Card(modifier = modifier.height(280.dp),
         colors = CardDefaults.cardColors(containerColor = SURFACE)) {
        Column(Modifier.padding(8.dp)) {
            Text("BRAND", color = SUBTEXT, fontSize = 10.sp,
                 letterSpacing = 1.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            LazyColumn {
                items(brands) { brand ->
                    Text(
                        brand,
                        color    = if (brand == selected) ACCENT else TEXT,
                        fontSize = 13.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(brand) }
                            .padding(vertical = 4.dp, horizontal = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ModelPicker(
    models: List<String>, selected: String?,
    onSelect: (String) -> Unit, modifier: Modifier
) {
    Card(modifier = modifier.height(280.dp),
         colors = CardDefaults.cardColors(containerColor = SURFACE)) {
        Column(Modifier.padding(8.dp)) {
            Text("MODEL", color = SUBTEXT, fontSize = 10.sp,
                 letterSpacing = 1.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            if (models.isEmpty()) {
                Text("Select brand first",
                    color = SUBTEXT, fontSize = 12.sp,
                    modifier = Modifier.padding(4.dp))
            } else {
                LazyColumn {
                    items(models) { model ->
                        Text(
                            model,
                            color    = if (model == selected) ACCENT else TEXT,
                            fontSize = 12.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(model) }
                                .padding(vertical = 3.dp, horizontal = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SearchResultsList(
    results: List<com.deepeye.otg.data.device.DeviceEntry>,
    onSelect: (com.deepeye.otg.data.device.DeviceEntry) -> Unit,
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        items(results) { entry ->
            Card(
                modifier = Modifier.fillMaxWidth().clickable { onSelect(entry) },
                colors   = CardDefaults.cardColors(containerColor = SURFACE),
            ) {
                Row(
                    Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically,
                ) {
                    Column {
                        Text("${entry.brand} ${entry.model}",
                            color = TEXT, fontSize = 13.sp,
                            fontWeight = FontWeight.Medium)
                        Text("${entry.series} · ${entry.year} · ${entry.type}",
                            color = SUBTEXT, fontSize = 11.sp)
                    }
                    ProtocolChip(entry.protocol)
                }
            }
        }
    }
}

@Composable
fun RoutingResultCard(result: com.deepeye.otg.data.device.ProtocolRouter.RoutingResult) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors   = CardDefaults.cardColors(containerColor = SURFACE),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("PROTOCOL DETECTED",
                color = SUBTEXT, fontSize = 10.sp, letterSpacing = 1.sp)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ProtocolChip(result.protocol)
                Column {
                    Text(result.protocol.displayName(),
                        color = TEXT, fontSize = 16.sp,
                        fontWeight = FontWeight.Bold)
                    Text(result.reason,
                        color = SUBTEXT, fontSize = 11.sp)
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ConfidenceBadge(result.confidence)
                result.entry?.let {
                    Text("${it.year} · ${it.type}",
                        color = SUBTEXT, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
fun ProtocolChip(protocol: DeviceProtocol) {
    Box(
        modifier = Modifier
            .background(protocol.color().copy(alpha = 0.15f),
                        MaterialTheme.shapes.small)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(protocol.shortName(),
            color = protocol.color(),
            fontSize = 11.sp, fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp)
    }
}

@Composable
fun ConfidenceBadge(confidence: com.deepeye.otg.data.device.ProtocolRouter.RoutingConfidence) {
    val color = when (confidence) {
        com.deepeye.otg.data.device.ProtocolRouter.RoutingConfidence.HIGH    -> Color(0xFF00E676)
        com.deepeye.otg.data.device.ProtocolRouter.RoutingConfidence.MEDIUM  -> Color(0xFFFF9800)
        com.deepeye.otg.data.device.ProtocolRouter.RoutingConfidence.LOW     -> Color(0xFFFF1744)
        com.deepeye.otg.data.device.ProtocolRouter.RoutingConfidence.UNKNOWN -> Color(0xFF888888)
    }
    Text("${confidence.name} confidence",
        color = color, fontSize = 10.sp,
        fontWeight = FontWeight.Medium)
}

// Extension helpers
fun DeviceProtocol.color() = when (this) {
    DeviceProtocol.MTK_V6         -> Color(0xFF00BCD4)
    DeviceProtocol.MTK_BROM       -> Color(0xFF009688)
    DeviceProtocol.MTK_OR_QC      -> Color(0xFFFF9800)
    DeviceProtocol.QC_EDL         -> Color(0xFF9C27B0)
    DeviceProtocol.SAMSUNG_ODIN   -> Color(0xFF1976D2)
    DeviceProtocol.UNKNOWN        -> Color(0xFF555555)
}
fun DeviceProtocol.shortName() = when (this) {
    DeviceProtocol.MTK_V6         -> "MTK V6"
    DeviceProtocol.MTK_BROM       -> "MTK BROM"
    DeviceProtocol.MTK_OR_QC      -> "MTK/QC"
    DeviceProtocol.QC_EDL         -> "QC EDL"
    DeviceProtocol.SAMSUNG_ODIN   -> "ODIN"
    DeviceProtocol.UNKNOWN        -> "UNKNOWN"
}
fun DeviceProtocol.displayName() = when (this) {
    DeviceProtocol.MTK_V6         -> "MediaTek V6 Protocol"
    DeviceProtocol.MTK_BROM       -> "MediaTek Classic BROM"
    DeviceProtocol.MTK_OR_QC      -> "MediaTek or Qualcomm (detect)"
    DeviceProtocol.QC_EDL         -> "Qualcomm EDL + Firehose"
    DeviceProtocol.SAMSUNG_ODIN   -> "Samsung ODIN"
    DeviceProtocol.UNKNOWN        -> "Unknown Protocol"
}
