package com.deepeye.otg.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.deepeye.otg.ui.theme.StitchTokens
import com.deepeye.otg.ui.viewmodel.LogEntry
import com.deepeye.otg.viewmodel.TerminalViewModel
import com.deepeye.otg.viewmodel.UsbViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalScreen(
    mainViewModel: UsbViewModel,
    terminalViewModel: TerminalViewModel = hiltViewModel()
) {
    val logs by terminalViewModel.logs.collectAsState()
    val isProcessing by terminalViewModel.isProcessing.collectAsState()
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }

    // Auto-scroll to bottom
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
                        Icon(Icons.Default.Terminal, null, tint = StitchTokens.Primary, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("FORENSIC CONSOLE", style = StitchTokens.LabelSmall, color = StitchTokens.TextPrimary)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { mainViewModel.setNav(NavTarget.DASHBOARD) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = StitchTokens.BackgroundDark.copy(alpha = 0.9f)
                )
            )
        },
        containerColor = Color.Black
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Logs Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color.Black)
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(logs) { log ->
                        Text(
                            text = log.message,
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                color = colorForLogType(log.type)
                            ),
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Input Area
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .border(1.dp, StitchTokens.Primary.copy(alpha = 0.3f), MaterialTheme.shapes.small),
                color = Color.DarkGray.copy(alpha = 0.1f),
                shape = MaterialTheme.shapes.small
            ) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.ChevronRight,
                        null,
                        tint = if (isProcessing) Color.Yellow else StitchTokens.Primary,
                        modifier = Modifier.size(20.dp)
                    )
                    
                    BasicTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 8.dp)
                            .focusRequester(focusRequester),
                        textStyle = TextStyle(
                            color = Color.White,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 14.sp
                        ),
                        cursorBrush = SolidColor(StitchTokens.Primary),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (inputText.isNotBlank() && !isProcessing) {
                                    terminalViewModel.executeCommand(inputText)
                                    inputText = ""
                                }
                            }
                        ),
                        singleLine = true
                    )
                    
                    if (isProcessing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = StitchTokens.Primary
                        )
                    }
                }
            }
        }
    }
    
    // Auto-focus the input field on start
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}

private fun colorForLogType(type: String): Color = when (type) {
    "COMMAND" -> StitchTokens.Primary
    "ERROR" -> Color(0xFFF87171)
    "SUCCESS" -> Color(0xFF4ADE80)
    "WARNING" -> Color(0xFFFACC15)
    "SYSTEM" -> Color.Gray
    "OUTPUT" -> Color.LightGray
    else -> Color.White
}
