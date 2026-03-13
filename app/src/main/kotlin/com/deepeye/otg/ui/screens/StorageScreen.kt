package com.deepeye.otg.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.deepeye.otg.usb.gpt.GptStructure
import com.deepeye.otg.ui.theme.StitchTokens
import com.deepeye.otg.viewmodel.StorageViewModel
import com.deepeye.otg.viewmodel.ForensicSearchHit
import com.deepeye.otg.viewmodel.SecurityArtifact
import com.deepeye.otg.ui.components.HexPeekDialog
import com.deepeye.otg.ui.components.ForensicIntelPanel

@Composable
fun StorageScreen(
    viewModel: StorageViewModel = hiltViewModel()
) {
    val partitions by viewModel.partitions.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val totalSize by viewModel.totalSize.collectAsState()
    val hexData by viewModel.hexPeekData.collectAsState()
    val actionStatus by viewModel.actionStatus.collectAsState()
    
    val selectedPartition by viewModel.selectedPartition.collectAsState()
    val currentPath by viewModel.currentPath.collectAsState()
    val dirFiles by viewModel.directoryFiles.collectAsState()

    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val isSearchingInternal by viewModel.isSearchingInternal.collectAsState()

    val securityArtifacts by viewModel.securityArtifacts.collectAsState()
    val lastReportPath by viewModel.lastReportPath.collectAsState()

    val tunnelUrl by viewModel.tunnelUrl.collectAsState()
    val remoteActive by viewModel.isRemoteActive.collectAsState()
    val remoteLogs by viewModel.remoteActivityLogs.collectAsState()

    val aiAnalysis by viewModel.aiAssistant.analysis.collectAsState()
    val aiConfidence by viewModel.aiAssistant.confidence.collectAsState()
    val aiIsProcessing by viewModel.aiAssistant.isProcessing.collectAsState()

    var activeView by remember { mutableStateOf("MAP") } 
    var showPatchDialog by remember { mutableStateOf<ForensicSearchHit?>(null) }
    var showRemoteLogs by remember { mutableStateOf(false) }

    LaunchedEffect(selectedPartition) {
        activeView = if (selectedPartition != null) "EXPLORER" else "MAP"
    }

    val tunnelPulse = rememberInfiniteTransition(label = "pulse")
    val tunnelAlpha by tunnelPulse.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse),
        label = "alpha"
    )

    Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF020202))
                .padding(16.dp)
                .then(
                    if (remoteActive) Modifier.border(2.dp, StitchTokens.Primary.copy(alpha = tunnelAlpha), RoundedCornerShape(0.dp))
                    else Modifier
                )
        ) {
            StorageHeader(
                activeView = activeView,
                isScanning = isScanning,
                remoteActive = remoteActive,
                onScan = { viewModel.scanStorage() },
                onViewChange = { activeView = it },
                onAudit = { 
                    activeView = "AUDIT"
                    viewModel.runSecurityDeepAudit() 
                },
                onExport = { viewModel.generateFullForensicReport() },
                onRemoteToggle = { 
                    if (remoteActive) viewModel.stopRemoteTunnel() else viewModel.startRemoteTunnel()
                }
            )

            Spacer(Modifier.height(16.dp))

            when (activeView) {
                "SEARCH" -> {
                    ForensicSearchView(
                        query = searchQuery,
                        results = searchResults,
                        isSearching = isSearchingInternal,
                        onSearch = { viewModel.searchPhysical(it) },
                        onHitClick = { showPatchDialog = it }
                    )
                }
                "AUDIT" -> {
                    ForensicAuditView(
                        artifacts = securityArtifacts,
                        isAnalyzing = isScanning,
                        aiAnalysis = aiAnalysis,
                        aiConfidence = aiConfidence,
                        aiIsProcessing = aiIsProcessing,
                        reportPath = lastReportPath
                    )
                }
                "EXPLORER" -> {
                    selectedPartition?.let { partition ->
                        PartitionExplorerHeader(
                            partition = partition,
                            path = currentPath,
                            onBack = { 
                                viewModel.resetExplorer()
                                activeView = "MAP"
                            }
                        )
                        Spacer(Modifier.height(16.dp))
                        FileSystemBrowser(
                            files = dirFiles,
                            onDirClick = { viewModel.enterDirectory(it) },
                            onFileClick = { viewModel.extractFile(it) }
                        )
                    }
                }
                "MAP" -> {
                    ForensicIntelPanel(
                        analysis = aiAnalysis,
                        confidence = aiConfidence,
                        isProcessing = aiIsProcessing
                    )

                    Spacer(Modifier.height(16.dp))

                    if (isScanning && hexData == null) {
                        ScanningProgress()
                    } else if (partitions.isEmpty()) {
                        EmptyState()
                    } else {
                        PartitionMap(partitions, totalSize)
                        Spacer(Modifier.height(24.dp))
                        PartitionList(
                            partitions = partitions,
                            onPeek = { viewModel.peekPartition(it) },
                            onDump = { viewModel.dumpPartition(it) },
                            onExplore = { viewModel.browsePartition(it) }
                        )
                    }
                }
            }
        }

        // Remote Active Overlay (Floating Link)
        if (remoteActive && tunnelUrl != null) {
            Surface(
                color = Color.Black,
                shape = RoundedCornerShape(bottomStart = 8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, StitchTokens.Primary),
                modifier = Modifier.align(Alignment.TopEnd).padding(top = 80.dp)
            ) {
                Row(
                    Modifier.padding(horizontal = 12.dp, vertical = 6.dp).clickable { showRemoteLogs = !showRemoteLogs },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(6.dp).background(StitchTokens.Primary, CircleShape))
                    Spacer(Modifier.width(8.dp))
                    Text("REMOTE: ${tunnelUrl?.split("/")?.last()}", color = Color.White, fontSize = 9.sp, style = StitchTokens.MonoCode)
                    Spacer(Modifier.width(8.dp))
                    Icon(if (showRemoteLogs) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null, tint = Color.Gray, modifier = Modifier.size(12.dp))
                }
            }
        }

        // Remote Console
        AnimatedVisibility(
            visible = showRemoteLogs && remoteActive,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
            modifier = Modifier.align(Alignment.TopEnd).padding(top = 115.dp, end = 16.dp)
        ) {
            Surface(
                color = Color(0xFF0A0A0A),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF222222)),
                modifier = Modifier.width(280.dp).heightIn(max = 300.dp)
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text("REMOTE ACTIVITY CONSOLE", color = StitchTokens.Primary, fontWeight = FontWeight.Black, fontSize = 10.sp)
                    Spacer(Modifier.height(8.dp))
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(remoteLogs) { log ->
                            Text(log, color = Color.LightGray, fontSize = 9.sp, style = StitchTokens.MonoCode)
                        }
                    }
                }
            }
        }

        // Action Status Overlay
        val statusValue = actionStatus
        AnimatedVisibility(
            visible = statusValue != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp)
        ) {
            Surface(
                color = Color.Black,
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, StitchTokens.Primary),
                shadowElevation = 8.dp
            ) {
                Text(
                    statusValue ?: "",
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                    style = StitchTokens.MonoCode,
                    fontSize = 12.sp
                )
            }
        }

        hexData?.let { hex ->
            HexPeekDialog(
                hex = hex,
                onDismiss = { viewModel.closeHexPeek() }
            )
        }

        showPatchDialog?.let { hit ->
            LivePatchDialog(
                hit = hit,
                onDismiss = { showPatchDialog = null },
                onPatch = { hex ->
                    viewModel.applyLivePatch(hit.partition, hit.offset, hex)
                    showPatchDialog = null
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LivePatchDialog(
    hit: ForensicSearchHit,
    onDismiss: () -> Unit,
    onPatch: (String) -> Unit
) {
    var patchHex by remember { mutableStateOf(hit.context) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0A0A0A),
        title = { Text("LIVE BIT-PATCH INJECTION", color = Color.Red, fontWeight = FontWeight.Black, fontSize = 16.sp) },
        text = {
            Column {
                Text(
                    "WARNING: Direct hardware modification can permanently brick the device. Ensure the patch sequence is valid for this partition.",
                    color = Color.Yellow,
                    fontSize = 11.sp
                )
                Spacer(Modifier.height(16.dp))
                Text("PARTITION: ${hit.partition.uppercase()}", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text("OFFSET: 0x${"%X".format(hit.offset)}", color = StitchTokens.Primary, style = StitchTokens.MonoCode, fontSize = 11.sp)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = patchHex,
                    onValueChange = { patchHex = it },
                    label = { Text("PATCH BYTES (HEX)", fontSize = 10.sp) },
                    textStyle = StitchTokens.MonoCode.copy(fontSize = 12.sp, color = Color.White),
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        unfocusedContainerColor = Color.Transparent,
                        focusedContainerColor = Color.Transparent,
                        unfocusedIndicatorColor = Color(0xFF333333),
                        focusedIndicatorColor = Color.Red
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onPatch(patchHex) },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
            ) {
                Text("INJECT PATCH", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = Color.Gray)
            }
        }
    )
}

@Composable
fun StorageHeader(
    activeView: String,
    isScanning: Boolean,
    remoteActive: Boolean,
    onScan: () -> Unit,
    onViewChange: (String) -> Unit,
    onAudit: () -> Unit,
    onExport: () -> Unit,
    onRemoteToggle: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.Memory,
                contentDescription = null,
                tint = if (remoteActive) StitchTokens.Primary else Color.Gray,
                modifier = Modifier.size(28.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    "STORAGE ANALYZER",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    letterSpacing = 1.sp
                )
                Row {
                    Text(
                        "MODE: ",
                        color = Color.DarkGray,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        activeView,
                        color = StitchTokens.Primary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconButton(
                onClick = onRemoteToggle,
                modifier = Modifier.size(36.dp).background(if (remoteActive) StitchTokens.Primary.copy(0.1f) else Color.Transparent, RoundedCornerShape(8.dp))
            ) { Icon(Icons.Default.CloudSync, null, tint = if (remoteActive) StitchTokens.Primary else Color.Gray, modifier = Modifier.size(18.dp)) }

            IconButton(
                onClick = { onViewChange("MAP") },
                modifier = Modifier.size(36.dp).background(if (activeView == "MAP") Color.White.copy(0.1f) else Color.Transparent, RoundedCornerShape(8.dp))
            ) { Icon(Icons.Default.Map, null, tint = if (activeView == "MAP") StitchTokens.Primary else Color.Gray, modifier = Modifier.size(18.dp)) }
            
            IconButton(
                onClick = { onViewChange("SEARCH") },
                modifier = Modifier.size(36.dp).background(if (activeView == "SEARCH") Color.White.copy(0.1f) else Color.Transparent, RoundedCornerShape(8.dp))
            ) { Icon(Icons.Default.Search, null, tint = if (activeView == "SEARCH") StitchTokens.Primary else Color.Gray, modifier = Modifier.size(18.dp)) }

            IconButton(
                onClick = onAudit,
                modifier = Modifier.size(36.dp).background(if (activeView == "AUDIT") Color.White.copy(0.1f) else Color.Transparent, RoundedCornerShape(8.dp))
            ) { Icon(Icons.Default.Security, null, tint = if (activeView == "AUDIT") StitchTokens.Primary else Color.Gray, modifier = Modifier.size(18.dp)) }

            IconButton(
                onClick = onExport,
                modifier = Modifier.size(36.dp).background(Color.White.copy(0.05f), RoundedCornerShape(8.dp))
            ) { Icon(Icons.AutoMirrored.Filled.Assignment, null, tint = Color.LightGray, modifier = Modifier.size(18.dp)) }

            Button(
                onClick = onScan,
                enabled = !isScanning,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(0.05f)),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(0.1f)),
                contentPadding = PaddingValues(horizontal = 12.dp)
            ) {
                Icon(Icons.Default.Refresh, null, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text("SCAN", fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ForensicAuditView(
    artifacts: List<SecurityArtifact>,
    isAnalyzing: Boolean,
    aiAnalysis: String,
    aiConfidence: Float,
    aiIsProcessing: Boolean,
    reportPath: String?
) {
    Column {
        ForensicIntelPanel(
            analysis = aiAnalysis,
            confidence = aiConfidence,
            isProcessing = aiIsProcessing
        )

        Spacer(Modifier.height(16.dp))

        if (reportPath != null) {
            Surface(
                color = Color(0xFF101010),
                shape = RoundedCornerShape(6.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, StitchTokens.Primary.copy(0.3f)),
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Description, null, tint = StitchTokens.Primary)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("CASE REPORT FINALIZED", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text(reportPath.split("/").last(), color = Color.Gray, fontSize = 10.sp, style = StitchTokens.MonoCode)
                    }
                }
            }
        }

        if (isAnalyzing && artifacts.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = StitchTokens.Primary, strokeWidth = 2.dp)
                Spacer(Modifier.height(12.dp))
                Text("COMPILING FORENSIC EVIDENCE...", color = Color.Gray, fontSize = 10.sp, style = StitchTokens.MonoCode)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(artifacts) { artifact ->
                    SecurityArtifactCard(artifact)
                }
            }
        }
    }
}

// ... rest of the components stay same as previous ...
@Composable
fun SecurityArtifactCard(artifact: SecurityArtifact) {
    Surface(
        color = Color(0xFF0F0A0A),
        shape = RoundedCornerShape(6.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF251515)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.VpnKey, null, tint = Color.Red.copy(0.6f), modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(16.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(artifact.type, color = Color.White, fontWeight = FontWeight.Black, fontSize = 12.sp)
                    Spacer(Modifier.width(8.dp))
                    Text(artifact.partition.uppercase(), color = Color.Gray, fontWeight = FontWeight.Bold, fontSize = 9.sp)
                }
                Text("at 0x${"%08X".format(artifact.offset)}", color = StitchTokens.Primary, fontSize = 10.sp, style = StitchTokens.MonoCode)
                Spacer(Modifier.height(4.dp))
                Text(artifact.desc, color = Color.LightGray, fontSize = 11.sp)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForensicSearchView(
    query: String,
    results: List<ForensicSearchHit>,
    isSearching: Boolean,
    onSearch: (String) -> Unit,
    onHitClick: (ForensicSearchHit) -> Unit
) {
    Column {
        TextField(
            value = query,
            onValueChange = onSearch,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search raw strings or hex pattern...", color = Color.Gray, fontSize = 13.sp) },
            leadingIcon = { Icon(Icons.Default.Fingerprint, null, tint = StitchTokens.Primary) },
            trailingIcon = { if (isSearching) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp) },
            colors = TextFieldDefaults.colors(
                unfocusedContainerColor = Color(0xFF0F0F0F),
                focusedContainerColor = Color(0xFF0F0F0F),
                unfocusedIndicatorColor = Color(0xFF222222),
                focusedIndicatorColor = StitchTokens.Primary
            ),
            shape = RoundedCornerShape(8.dp),
            singleLine = true
        )

        Spacer(Modifier.height(16.dp))

        if (results.isEmpty() && !isSearching) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("NO RESULTS FOUND", color = Color.DarkGray, fontSize = 12.sp, style = StitchTokens.MonoCode)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(results) { hit ->
                    SearchHitCard(hit, onClick = { onHitClick(hit) })
                }
            }
        }
    }
}

@Composable
fun SearchHitCard(hit: ForensicSearchHit, onClick: () -> Unit) {
    Surface(
        color = Color(0xFF0A0A0A),
        shape = RoundedCornerShape(6.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF151515)),
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("LBA: ${hit.lba}", color = StitchTokens.Primary, fontWeight = FontWeight.Black, fontSize = 10.sp)
                    Spacer(Modifier.width(12.dp))
                    Text("OFFSET: 0x${"%08X".format(hit.offset)}", color = Color.Gray, fontSize = 10.sp, style = StitchTokens.MonoCode)
                }
                Text(hit.partition.uppercase(), color = Color.White.copy(0.3f), fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                hit.context,
                color = Color.White,
                style = StitchTokens.MonoCode,
                fontSize = 11.sp,
                modifier = Modifier.background(Color.Black.copy(0.3f)).padding(8.dp).fillMaxWidth()
            )
        }
    }
}

@Composable
fun PartitionExplorerHeader(
    partition: GptStructure.GptEntry,
    path: String,
    onBack: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
        }
        Spacer(Modifier.width(8.dp))
        Column {
            Text(
                "EXPLORING: ${partition.name.uppercase()}",
                color = StitchTokens.Primary,
                fontWeight = FontWeight.Black,
                fontSize = 14.sp
            )
            Text(
                path,
                color = Color.Gray,
                style = StitchTokens.MonoCode,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
fun FileSystemBrowser(
    files: List<FileEntry>,
    onDirClick: (String) -> Unit,
    onFileClick: (FileEntry) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF080808))
            .border(1.dp, Color(0xFF151515), RoundedCornerShape(8.dp))
    ) {
        items(files) { file ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { 
                        if (file.isDir) onDirClick(file.path)
                    }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (file.isDir) Icons.Default.Folder else Icons.Default.Description,
                    contentDescription = null,
                    tint = if (file.isDir) Color.Cyan else Color.Gray,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(file.name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    if (!file.isDir) {
                        Text("${file.size} bytes", color = Color.DarkGray, fontSize = 10.sp)
                    }
                }
                
                if (!file.isDir) {
                    IconButton(onClick = { onFileClick(file) }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Save, null, tint = StitchTokens.Primary, modifier = Modifier.size(18.dp))
                    }
                } else {
                    Icon(Icons.Default.ChevronRight, null, tint = Color.DarkGray, modifier = Modifier.size(16.dp))
                }
            }
            HorizontalDivider(color = Color(0xFF151515))
        }
    }
}

@Composable
fun ScanningProgress() {
    val infiniteTransition = rememberInfiniteTransition(label = "scan")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = StitchTokens.Primary, strokeWidth = 2.dp)
            Spacer(Modifier.height(16.dp))
            Text(
                "MAPPING GPT STRUCTURE...",
                color = StitchTokens.Primary.copy(alpha = alpha),
                style = StitchTokens.MonoCode,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun EmptyState() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.DeveloperBoard, null, tint = Color.DarkGray, modifier = Modifier.size(64.dp))
            Spacer(Modifier.height(12.dp))
            Text("NO STORAGE MAP LOADED", color = Color.Gray, fontSize = 12.sp)
            Text("Connect device in BROM/EDL/ADB mode to scan", color = Color.DarkGray, fontSize = 10.sp)
        }
    }
}

@Composable
fun PartitionMap(partitions: List<GptStructure.GptEntry>, totalSize: Long) {
    Text("PARTITION DISTRIBUTION", color = Color.LightGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(8.dp))
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(32.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFF111111))
            .border(1.dp, Color(0xFF222222), RoundedCornerShape(4.dp))
    ) {
        partitions.forEachIndexed { index, partition ->
            val weight = partition.sizeInBytes.toFloat() / totalSize.toFloat()
            if (weight > 0.001f) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(weight)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    colorForIndex(index),
                                    colorForIndex(index).copy(alpha = 0.7f)
                                )
                            )
                        )
                        .border(0.5.dp, Color.Black.copy(0.3f))
                )
            }
        }
    }
}

@Composable
fun PartitionList(
    partitions: List<GptStructure.GptEntry>,
    onPeek: (GptStructure.GptEntry) -> Unit,
    onDump: (GptStructure.GptEntry) -> Unit,
    onExplore: (GptStructure.GptEntry) -> Unit
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Row(Modifier.fillMaxWidth().padding(bottom = 8.dp).padding(horizontal = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("NAME", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text("ACTIONS", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
        items(partitions) { partition ->
            PartitionRow(
                partition = partition,
                onPeek = { onPeek(partition) },
                onDump = { onDump(partition) },
                onExplore = { onExplore(partition) }
            )
        }
    }
}

@Composable
fun PartitionRow(
    partition: GptStructure.GptEntry,
    onPeek: () -> Unit,
    onDump: () -> Unit,
    onExplore: () -> Unit
) {
    val sizeText = formatSize(partition.sizeInBytes)
    
    Surface(
        color = Color(0xFF0A0A0A),
        shape = RoundedCornerShape(6.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF151515)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    Modifier
                        .size(8.dp)
                        .background(colorForPartition(partition.name), RoundedCornerShape(2.dp))
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(partition.name.uppercase(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text(sizeText, color = StitchTokens.Primary, fontWeight = FontWeight.Black, fontSize = 11.sp)
                }
            }
            
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Surface(
                    onClick = onPeek,
                    color = Color.White.copy(0.05f),
                    shape = RoundedCornerShape(4.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.Cyan.copy(0.3f))
                ) {
                    Text(
                        "PEEK",
                        color = Color.Cyan,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                    )
                }

                Surface(
                    onClick = onExplore,
                    color = Color.White.copy(0.05f),
                    shape = RoundedCornerShape(4.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.Yellow.copy(0.3f))
                ) {
                    Text(
                        "OPEN",
                        color = Color.Yellow,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                    )
                }

                Surface(
                    onClick = onDump,
                    color = Color.White.copy(0.05f),
                    shape = RoundedCornerShape(4.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(0.3f))
                ) {
                    Text(
                        "DUMP",
                        color = Color.White,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

private fun formatSize(bytes: Long): String {
    val mb = bytes / (1024 * 1024)
    return if (mb >= 1024) {
        "%.2f GB".format(mb / 1024f)
    } else {
        "$mb MB"
    }
}

private fun colorForIndex(index: Int): Color {
    val colors = listOf(
        Color(0xFF3B82F6), // Blue
        Color(0xFF10B981), // Green
        Color(0xFFF59E0B), // Amber
        Color(0xFFEF4444), // Red
        Color(0xFF8B5CF6), // Violet
        Color(0xFF06B6D4), // Cyan
        Color(0xFFEC4899)  // Pink
    )
    return colors[index % colors.size]
}

private fun colorForPartition(name: String): Color = when (name.lowercase()) {
    "system", "super" -> Color(0xFF3B82F6)
    "userdata" -> Color(0xFF10B981)
    "vendor" -> Color(0xFFF59E0B)
    "boot", "recovery" -> Color(0xFFEF4444)
    "cache" -> Color(0xFF8B5CF6)
    else -> Color(0xFF6B7280)
}

fun CircleShape() = RoundedCornerShape(50)
