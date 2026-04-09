package com.deepeye.otg.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.deepeye.otg.ui.theme.DeepEyeColors
import com.deepeye.otg.ui.theme.DeepEyeType
import com.deepeye.otg.viewmodel.UsbViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogScreen(
    mainViewModel: UsbViewModel,
    onBack: () -> Unit
) {
    val logs by mainViewModel.logs.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            listState.animateScrollToItem(logs.size - 1)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.List, null, tint = DeepEyeColors.NEON_PURPLE, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("SYSTEM LOGS", style = DeepEyeType.CAPTION.copy(fontSize = 11.sp), color = DeepEyeColors.WHITE_HIGH)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                    }
                },
                actions = {
                    TextButton(
                        onClick = { /* TODO: Add clear logs functionality */ }
                    ) {
                        Text("CLEAR", color = DeepEyeColors.NEON_PURPLE)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = DeepEyeColors.BG_VOID.copy(alpha = 0.9f)
                )
            )
        },
        containerColor = Color.Black
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Total entries: ${logs.size}",
                    style = DeepEyeType.CAPTION.copy(fontSize = 11.sp),
                    color = DeepEyeColors.WHITE_MED
                )
            }

            Divider(color = DeepEyeColors.WHITE_LOW.copy(0.3f).copy(alpha = 0.3f))

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(logs) { log ->
                    val logColor = when (log.type.uppercase()) {
                        "ERROR", "FAIL" -> DeepEyeColors.NEON_PINK
                        "SUCCESS", "OK" -> DeepEyeColors.NEON_GREEN
                        "WARNING", "WARN" -> DeepEyeColors.NEON_YELLOW
                        "INFO" -> DeepEyeColors.NEON_PURPLE
                        else -> DeepEyeColors.WHITE_MED
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = log.timestamp,
                            style = DeepEyeType.CAPTION.copy(fontSize = 11.sp),
                            color = DeepEyeColors.WHITE_MED,
                            modifier = Modifier.width(80.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(logColor.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = log.type,
                                style = DeepEyeType.CAPTION.copy(fontSize = 11.sp),
                                color = logColor,
                                fontSize = 10.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = log.message,
                            style = DeepEyeType.BODY.copy(fontSize = 14.sp),
                            color = DeepEyeColors.WHITE_HIGH,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}
