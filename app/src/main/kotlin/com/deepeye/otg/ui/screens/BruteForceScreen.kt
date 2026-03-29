package com.deepeye.otg.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deepeye.otg.exploit.BruteForceExecutor
import com.deepeye.otg.exploit.BruteForcePayloads
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BruteForceScreen(
    executor: BruteForceExecutor,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var isRunning by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0) }
    var status by remember { mutableStateOf("Ready") }
    val logs = remember { mutableStateListOf<String>() }
    var selectedType by remember { mutableStateOf("4-Digit Common") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("PIN Brute-Force Auditor") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Text("←") }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text("Audit Strategy", style = MaterialTheme.typography.titleSmall)
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = selectedType == "4-Digit Common", onClick = { selectedType = "4-Digit Common" })
                Text("4-Digit Common")
                Spacer(modifier = Modifier.width(8.dp))
                RadioButton(selected = selectedType == "6-Digit Common", onClick = { selectedType = "6-Digit Common" })
                Text("6-Digit Common")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    scope.launch {
                        isRunning = true
                        logs.clear()
                        val pins = if (selectedType == "4-Digit Common") 
                            BruteForcePayloads.COMMON_4_DIGIT 
                        else 
                            BruteForcePayloads.COMMON_6_DIGIT
                        
                        executor.runBruteForce(
                            pins = pins,
                            onProgress = { msg, p ->
                                status = msg
                                progress = p
                                logs.add(msg)
                            }
                        )
                        isRunning = false
                        status = "Audit Complete"
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isRunning
            ) {
                Text(if (isRunning) "Auditing..." else "Start Security Audit")
            }

            if (isRunning) {
                Button(
                    onClick = { executor.stop() },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Stop Audit")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            LinearProgressIndicator(
                progress = { progress / 100f },
                modifier = Modifier.fillMaxWidth()
            )
            Text("$progress% - $status", modifier = Modifier.padding(top = 8.dp))

            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                modifier = Modifier.fillMaxWidth().weight(1f),
                color = Color.Black,
                shape = MaterialTheme.shapes.medium
            ) {
                LazyColumn(modifier = Modifier.padding(8.dp)) {
                    items(logs) { log ->
                        Text(
                            text = "[AUDIT] $log",
                            color = Color.Green,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}
