package com.deepeye.otg.ui.apple

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhoneIphone
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.deepeye.otg.data.repository.AppleDeviceState
import com.deepeye.otg.ui.components.ExploitMethodCard
import com.deepeye.otg.ui.components.ExploitMethodModel
import com.deepeye.otg.ui.components.ExploitRisk
import com.deepeye.otg.ui.components.LogConsole
import com.deepeye.otg.ui.components.NeonButton
import com.deepeye.otg.ui.components.NeonButtonStyle
import com.deepeye.otg.ui.components.SectionHeader
import com.deepeye.otg.ui.components.toConsoleEntries
import com.deepeye.otg.ui.theme.DeepEyeColors
import com.deepeye.otg.usb.DeviceMatrix
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning

private data class AppleMethodAction(
    val id: String,
    val model: ExploitMethodModel,
    val run: () -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppleProToolsScreen(
    viewModel: AppleDeviceViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val scanner = remember(activity) {
        activity?.let {
            GmsBarcodeScanning.getClient(
                it,
                GmsBarcodeScannerOptions.Builder()
                    .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                    .enableAutoZoom()
                    .build(),
            )
        }
    }

    var imeiInput by rememberSaveable { mutableStateOf("") }
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var selectedMethodId by rememberSaveable { mutableStateOf("") }
    var currentStep by rememberSaveable { mutableIntStateOf(0) }

    LaunchedEffect(selectedTab, state.detectedMode) {
        selectedMethodId = apple_methods_for_tab(selectedTab, state.detectedMode, viewModel).firstOrNull()?.id.orEmpty()
    }

    LaunchedEffect(state.isRefreshing, state.successMessage, state.errorMessage) {
        currentStep = when {
            state.isRefreshing -> 2
            !state.errorMessage.isNullOrBlank() -> 1
            !state.successMessage.isNullOrBlank() -> 3
            else -> currentStep
        }
    }

    val methods = apple_methods_for_tab(selectedTab, state.detectedMode, viewModel)
    val selectedMethod = methods.firstOrNull { it.id == selectedMethodId } ?: methods.firstOrNull()
    val modeLabel = when (state.detectedMode) {
        DeviceMatrix.AppleMode.DFU -> "DFU"
        DeviceMatrix.AppleMode.RECOVERY -> "Recovery"
        DeviceMatrix.AppleMode.NORMAL -> "Normal"
        DeviceMatrix.AppleMode.WTF -> "WTF"
        DeviceMatrix.AppleMode.PWNED_DFU -> "Pwned DFU"
        else -> "Idle"
    }
    val subtitle = when (val appleState = state.appleDeviceState) {
        is AppleDeviceState.Detected -> "${appleState.device.deviceName} • ${appleState.mode}"
        is AppleDeviceState.Error -> appleState.reason
        else -> "Connect Apple device in Normal, Recovery, or DFU mode"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        SectionHeader(title = "Apple ProTools", count = modeLabel, accentColor = DeepEyeColors.PurpleDim)

        com.deepeye.otg.ui.components.GlassCard(
            hazeState = null,
            accentColor = DeepEyeColors.PurpleDim,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(
                        imageVector = Icons.Default.PhoneIphone,
                        contentDescription = null,
                        tint = DeepEyeColors.PurpleDim,
                    )
                    Column {
                        Text(
                            text = "Apple logo + ProTools",
                            style = MaterialTheme.typography.titleLarge,
                            color = DeepEyeColors.TextPrimary,
                        )
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = DeepEyeColors.TextSecondary,
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf(
                        "Normal" to (state.detectedMode == DeviceMatrix.AppleMode.NORMAL),
                        "Recovery" to (state.detectedMode == DeviceMatrix.AppleMode.RECOVERY),
                        "DFU" to (state.detectedMode == DeviceMatrix.AppleMode.DFU || state.detectedMode == DeviceMatrix.AppleMode.PWNED_DFU || state.detectedMode == DeviceMatrix.AppleMode.WTF),
                    ).forEach { (label, selected) ->
                        FilterChip(
                            selected = selected,
                            onClick = {},
                            label = { Text(label) },
                            leadingIcon = if (selected) {
                                { Icon(Icons.Default.PhoneIphone, contentDescription = null) }
                            } else {
                                null
                            },
                        )
                    }
                }
            }
        }

        OutlinedTextField(
            value = imeiInput,
            onValueChange = { imeiInput = it.filter(Char::isDigit).take(15) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("IMEI / Serial") },
            singleLine = true,
            trailingIcon = {
                IconButton(
                    onClick = {
                        val currentScanner = scanner ?: return@IconButton
                        activity ?: return@IconButton
                        currentStep = 0
                        currentScanner.startScan()
                            .addOnSuccessListener { barcode ->
                                imeiInput = barcode.rawValue.orEmpty().filter(Char::isDigit).take(15)
                            }
                    },
                ) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = DeepEyeColors.PurpleDim)
                }
            },
        )

        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            contentColor = DeepEyeColors.PurpleDim,
            edgePadding = 0.dp,
        ) {
            listOf("iCloud", "Hello", "PIN", "Apple ID", "MDM").forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title) },
                )
            }
        }

        methods.forEach { item ->
            ExploitMethodCard(
                method = item.model,
                selected = item.id == selectedMethodId,
                onClick = { selectedMethodId = item.id },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        SectionHeader(title = "Workflow", count = listOf("Input", "Check", "Bypass", "Done")[currentStep.coerceIn(0, 3)], accentColor = DeepEyeColors.PurpleDim)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            listOf("Input", "Check", "Bypass", "Done").forEachIndexed { index, label ->
                FilterChip(
                    selected = currentStep >= index,
                    onClick = {},
                    label = { Text(label) },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            NeonButton(
                text = "Check",
                onClick = {
                    currentStep = 1
                    viewModel.refreshAppleDevice()
                },
                style = NeonButtonStyle.SECONDARY,
                modifier = Modifier.weight(1f),
            )
            NeonButton(
                text = selectedMethod?.model?.name ?: "Run Selected",
                onClick = {
                    currentStep = 2
                    selectedMethod?.run?.invoke()
                },
                loading = state.isRefreshing,
                modifier = Modifier.weight(1f),
            )
        }

        if (!state.successMessage.isNullOrBlank()) {
            Text(
                text = state.successMessage.orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                color = DeepEyeColors.Success,
            )
        }
        if (!state.errorMessage.isNullOrBlank()) {
            Text(
                text = state.errorMessage.orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                color = DeepEyeColors.Error,
            )
        }

        LogConsole(
            entries = if (state.irecoveryOutput.isNullOrBlank()) {
                emptyList()
            } else {
                listOf(
                    com.deepeye.otg.ui.viewmodel.LogEntry(
                        message = state.irecoveryOutput.orEmpty(),
                        type = if (state.errorMessage.isNullOrBlank()) "EXPLOIT" else "ERROR",
                        timestamp = "APPLE",
                    ),
                ).toConsoleEntries()
            },
            title = "Apple Console",
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp),
        )

        Spacer(modifier = Modifier.height(84.dp))
    }
}

private fun apple_methods_for_tab(
    tabIndex: Int,
    mode: DeviceMatrix.AppleMode?,
    viewModel: AppleDeviceViewModel,
): List<AppleMethodAction> = when (tabIndex) {
    0 -> listOf(
        AppleMethodAction(
            id = "icloud-check",
            model = ExploitMethodModel(
                id = "icloud-check",
                icon = Icons.Default.PhoneIphone,
                name = "Activation Check",
                description = "Query activation / iCloud status from current USB mode.",
                risk = ExploitRisk.LOW,
                accentColor = DeepEyeColors.PurpleDim,
            ),
            run = viewModel::refreshAppleDevice,
        ),
        AppleMethodAction(
            id = "icloud-shell",
            model = ExploitMethodModel(
                id = "icloud-shell",
                icon = Icons.Default.PhoneIphone,
                name = "GetEnv Snapshot",
                description = "Collect boot variables using iRecovery console access.",
                risk = ExploitRisk.MED,
                accentColor = DeepEyeColors.PurpleDim,
            ),
            run = { viewModel.sendIrecoveryCommand("getenv") },
        ),
    )

    1 -> listOf(
        AppleMethodAction(
            id = "hello-exit",
            model = ExploitMethodModel(
                id = "hello-exit",
                icon = Icons.Default.PhoneIphone,
                name = "Exit Recovery",
                description = "Return to normal boot flow after Hello / restore staging.",
                risk = ExploitRisk.LOW,
                accentColor = DeepEyeColors.PurpleDim,
            ),
            run = if (mode == DeviceMatrix.AppleMode.RECOVERY) viewModel::exitRecovery else viewModel::refreshAppleDevice,
        ),
        AppleMethodAction(
            id = "hello-dfu",
            model = ExploitMethodModel(
                id = "hello-dfu",
                icon = Icons.Default.PhoneIphone,
                name = "Enter DFU",
                description = "Transition recovery devices into DFU path for advanced servicing.",
                risk = ExploitRisk.MED,
                accentColor = DeepEyeColors.PurpleDim,
            ),
            run = if (mode == DeviceMatrix.AppleMode.RECOVERY) viewModel::enterDfu else viewModel::refreshAppleDevice,
        ),
    )

    2 -> listOf(
        AppleMethodAction(
            id = "pin-probe",
            model = ExploitMethodModel(
                id = "pin-probe",
                icon = Icons.Default.PhoneIphone,
                name = "PIN Probe",
                description = "Run non-destructive environment probe for passcode-related state.",
                risk = ExploitRisk.MED,
                accentColor = DeepEyeColors.PurpleDim,
            ),
            run = { viewModel.sendIrecoveryCommand("getenv auto-boot") },
        ),
        AppleMethodAction(
            id = "pin-refresh",
            model = ExploitMethodModel(
                id = "pin-refresh",
                icon = Icons.Default.PhoneIphone,
                name = "Refresh Mode",
                description = "Re-query device mode and update live session metadata.",
                risk = ExploitRisk.LOW,
                accentColor = DeepEyeColors.PurpleDim,
            ),
            run = viewModel::refreshAppleDevice,
        ),
    )

    3 -> listOf(
        AppleMethodAction(
            id = "appleid-refresh",
            model = ExploitMethodModel(
                id = "appleid-refresh",
                icon = Icons.Default.PhoneIphone,
                name = "Apple ID Check",
                description = "Inspect current state and prepare research workflow notes.",
                risk = ExploitRisk.LOW,
                accentColor = DeepEyeColors.PurpleDim,
            ),
            run = viewModel::refreshAppleDevice,
        ),
    )

    else -> listOf(
        AppleMethodAction(
            id = "mdm-profile",
            model = ExploitMethodModel(
                id = "mdm-profile",
                icon = Icons.Default.PhoneIphone,
                name = "Profile Snapshot",
                description = "Collect available configuration context from active Apple mode.",
                risk = ExploitRisk.MED,
                accentColor = DeepEyeColors.PurpleDim,
            ),
            run = { viewModel.sendIrecoveryCommand("getenv") },
        ),
    )
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
