package com.deepeye.otg.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.deepeye.otg.ui.screens.CompleteScreen
import com.deepeye.otg.ui.screens.DeepEyeMainScreen
import com.deepeye.otg.ui.screens.ErrorScreen
import com.deepeye.otg.ui.screens.ExecutingScreen
import com.deepeye.otg.ui.screens.PartitionManagerScreen
import com.deepeye.otg.ui.screens.ReportingScreen
import com.deepeye.otg.ui.screens.TestHarnessScreen
import com.deepeye.otg.ui.screens.WaitingScreen
import com.deepeye.otg.ui.settings.ThemePreferences
import com.deepeye.otg.ui.theme.DeepEyeTheme
import com.deepeye.otg.ui.theme.ThemeMode
import com.deepeye.otg.usb.SessionState
import com.deepeye.otg.viewmodel.UsbViewModel

@Composable
fun DeepEyeApp(viewModel: UsbViewModel) {
    val state by viewModel.queueState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val themeMode by ThemePreferences.getThemeModeFlow(context)
        .collectAsState(initial = ThemeMode.SYSTEM)

    DeepEyeTheme(themeMode = themeMode) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing),
        ) {
            DeepEyeMainScreen(viewModel = viewModel)

            AnimatedContent(
                targetState = state,
                transitionSpec = {
                    fadeIn(animationSpec = tween(400)) togetherWith
                        fadeOut(animationSpec = tween(400))
                },
                label = "OverlayTransition",
            ) { targetState ->
                when (targetState) {
                    is SessionState.ExecutingOperation -> {
                        val logs by viewModel.logs.collectAsStateWithLifecycle()
                        ExecutingScreen(
                            op = targetState.op,
                            progress = targetState.progress,
                            statusMsg = targetState.statusMsg,
                            logs = logs,
                            onCancel = { viewModel.resetToIdle() },
                        )
                    }

                    is SessionState.WaitingForDevice -> WaitingScreen(
                        op = targetState.queuedOp,
                        onCancel = { viewModel.cancelWaiting() },
                    )

                    is SessionState.OperationComplete -> CompleteScreen(
                        op = targetState.op,
                        success = targetState.success,
                        message = targetState.message,
                        onDismiss = { viewModel.resetToIdle() },
                        onViewAudit = { viewModel.exportSessionReport() },
                    )

                    is SessionState.Error -> ErrorScreen(
                        message = targetState.message,
                        onRetry = {
                            if (targetState.queuedOp != null) {
                                viewModel.queueOperation(targetState.queuedOp)
                            } else {
                                viewModel.resetToIdle()
                            }
                        },
                    )

                    is SessionState.PermissionDenied -> ErrorScreen(
                        message = "USB permission denied. Request again?",
                        onRetry = {
                            if (targetState.queuedOp != null) {
                                viewModel.queueOperation(targetState.queuedOp)
                            } else {
                                viewModel.resetToIdle()
                            }
                        },
                    )

                    is SessionState.TestHarness -> TestHarnessScreen(viewModel = viewModel)

                    is SessionState.Reporting -> ReportingScreen(
                        reportFile = targetState.reportFile,
                        viewModel = viewModel,
                    )

                    is SessionState.PartitionPreview -> PartitionManagerScreen(
                        partitions = targetState.partitions,
                        viewModel = viewModel,
                    )

                    else -> Unit
                }
            }
        }
    }
}
