package com.deepeye.otg.ui.screens

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deepeye.otg.domain.models.*
import com.deepeye.otg.usb.UsbLifecycleState
import com.deepeye.otg.ui.theme.DeepEyeColors
import com.deepeye.otg.viewmodel.UsbViewModel
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeChild
import kotlinx.coroutines.delay

@Composable
fun ForensicLabScreen(
    viewModel: UsbViewModel,
    hazeState: HazeState,
    perfMode: Boolean
) {
    val session by viewModel.lifecycleState.collectAsStateWithLifecycle()
    val operations = DeepEyeOperation.values()
    
    val safeOps = operations.filter { it.effectiveRisk == RiskLevel.SAFE }
    val advancedOps = operations.filter { it.effectiveRisk == RiskLevel.ADVANCED }
    val dangerOps = operations.filter { it.effectiveRisk == RiskLevel.DANGER }

    var executingOp by remember { mutableStateOf<DeepEyeOperation?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .then(if (perfMode) Modifier.hazeChild(hazeState) else Modifier)
            .padding(16.dp)
    ) {
        ForensicLabHeader(session)
        
        Spacer(Modifier.height(24.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            if (safeOps.isNotEmpty()) {
                item { RiskSectionHeader("Safe Operations", RiskLevel.SAFE) }
                items(safeOps) { op ->
                    LabActionCard(op, onExecute = { executingOp = op })
                }
            }

            if (advancedOps.isNotEmpty()) {
                item { RiskSectionHeader("Advanced Procedures", RiskLevel.ADVANCED) }
                items(advancedOps) { op ->
                    LabActionCard(op, onExecute = { executingOp = op })
                }
            }

            if (dangerOps.isNotEmpty()) {
                item { RiskSectionHeader("Critical / Destructive", RiskLevel.DANGER) }
                items(dangerOps) { op ->
                    LabActionCard(op, onExecute = { executingOp = op })
                }
            }
        }
    }

    executingOp?.let { op ->
        OperationConfirmationDialog(
            operation = op,
            onDismiss = { executingOp = null },
            onConfirm = {
                viewModel.queueOperation(op)
                executingOp = null
            }
        )
    }
}

@Composable
fun RiskSectionHeader(title: String, risk: RiskLevel) {
    val color = when(risk) {
        RiskLevel.SAFE -> Color(0xFF4ADE80)
        RiskLevel.ADVANCED -> Color(0xFFFACC15)
        RiskLevel.DANGER -> Color(0xFFEF4444)
    }
    
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp, 16.dp).background(color, RoundedCornerShape(2.dp)))
        Spacer(Modifier.width(12.dp))
        Text(
            title.uppercase(),
            color = Color.White,
            fontWeight = FontWeight.Black,
            fontSize = 12.sp,
            letterSpacing = 1.sp
        )
    }
}

@Composable
fun LabActionCard(
    operation: DeepEyeOperation,
    onExecute: () -> Unit
) {
    var holdProgress by remember { mutableStateOf(0f) }
    val isDanger = operation.effectiveRisk == RiskLevel.DANGER
    
    val cardColor = when(operation.effectiveRisk) {
        RiskLevel.SAFE -> Color(0xFF1A1A1A)
        RiskLevel.ADVANCED -> Color(0xFF221F10)
        RiskLevel.DANGER -> Color(0xFF251010)
    }

    LaunchedEffect(holdProgress) {
        if (holdProgress >= 1f) {
            onExecute()
            holdProgress = 0f
        }
    }

    Surface(
        color = cardColor,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp, 
            if (holdProgress > 0f) DeepEyeColors.NEON_PURPLE.copy(alpha = holdProgress) else Color.White.copy(0.05f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { if (!isDanger) onExecute() },
                    onLongPress = { /* Handled by internal progress logic */ },
                    onPress = {
                        if (isDanger) {
                            val startTime = System.currentTimeMillis()
                            while (tryAwaitRelease()) {
                                val elapsed = System.currentTimeMillis() - startTime
                                holdProgress = (elapsed / 1500f).coerceIn(0f, 1f)
                                delay(16)
                            }
                            holdProgress = 0f
                        }
                    }
                )
            }
    ) {
        Box {
            // Background Progress for DANGER
            if (isDanger && holdProgress > 0f) {
                Box(
                    Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(holdProgress)
                        .background(Color.White.copy(0.05f))
                )
            }

            Row(
                Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(0.05f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isDanger) Icons.Default.Warning else Icons.Default.Science,
                        contentDescription = null,
                        tint = if (isDanger) Color.Red else Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(Modifier.width(16.dp))

                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            operation.label,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Spacer(Modifier.width(8.dp))
                        RiskBadge(operation.effectiveRisk)
                    }
                    Text(
                        operation.description.ifEmpty { "Perform diagnostic or extraction task." },
                        color = Color.Gray,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }

                Icon(
                    imageVector = if (isDanger) Icons.Default.Fingerprint else Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = if (isDanger) Color.Red.copy(alpha = 0.5f) else Color.DarkGray
                )
            }
        }
    }
}

@Composable
fun RiskBadge(risk: RiskLevel) {
    val (color, text) = when(risk) {
        RiskLevel.SAFE -> Color(0xFF4ADE80) to "SAFE"
        RiskLevel.ADVANCED -> Color(0xFFFACC15) to "ADVANCED"
        RiskLevel.DANGER -> Color(0xFFEF4444) to "DANGER"
    }
    
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(4.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.2f))
    ) {
        Text(
            text,
            color = color,
            fontSize = 8.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun ForensicLabHeader(session: UsbLifecycleState) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                "MISSION HUB: LAB",
                color = DeepEyeColors.NEON_PURPLE,
                fontWeight = FontWeight.Black,
                fontSize = 12.sp,
                letterSpacing = 2.sp
            )
            Text(
                "MISSION READY",
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = 28.sp
            )
        }
        
        Surface(
            color = Color.White.copy(0.05f),
            shape = CircleShape,
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(0.1f))
        ) {
            Icon(
                Icons.Default.History,
                null,
                tint = Color.Gray,
                modifier = Modifier.padding(8.dp).size(20.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OperationConfirmationDialog(
    operation: DeepEyeOperation,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0F0F0F),
        title = {
            Text(
                "CONFIRM OPERATION",
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = 18.sp
            )
        },
        text = {
            Column {
                Text(
                    "You are about to execute: ${operation.label}",
                    color = Color.LightGray,
                    fontSize = 14.sp
                )
                if (operation.effectiveRisk == RiskLevel.DANGER) {
                    Spacer(Modifier.height(12.dp))
                    Surface(
                        color = Color.Red.copy(alpha = 0.1f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.Red.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ErrorOutline, null, tint = Color.Red)
                            Spacer(Modifier.width(12.dp))
                            Text(
                                "CRITICAL: This operation is destructive and cannot be undone.",
                                color = Color.Red,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (operation.effectiveRisk == RiskLevel.DANGER) Color.Red else DeepEyeColors.NEON_PURPLE
                )
            ) {
                Text("EXECUTE", fontWeight = FontWeight.Black)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = Color.Gray)
            }
        }
    )
}
