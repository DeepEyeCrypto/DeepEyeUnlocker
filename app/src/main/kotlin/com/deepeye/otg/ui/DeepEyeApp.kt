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
import com.deepeye.otg.ui.screens.*
import com.deepeye.otg.usb.SessionState
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import com.deepeye.otg.RemoteShareActivity
import com.deepeye.otg.viewmodel.UsbViewModel

@Composable
fun DeepEyeApp(viewModel: UsbViewModel) {
    val state by viewModel.queueState.collectAsState()
    val context = LocalContext.current

    // Base layer: Main UI is ALWAYS visible
    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        MainScreen(
            viewModel = viewModel
        )

        // Overlay Layer for Active Operations/States
        AnimatedContent(
            targetState = state,
            transitionSpec = {
                fadeIn(animationSpec = tween(400)) togetherWith 
                fadeOut(animationSpec = tween(400))
            },
            label = "OverlayTransition"
        ) { targetState ->
            when (targetState) {
                is SessionState.ExecutingOperation -> {
                    val logs by viewModel.logs.collectAsState()
                    ExecutingScreen(
                        op = targetState.op,
                        progress = targetState.progress,
                        statusMsg = targetState.statusMsg,
                        logs = logs,
                        onCancel = { viewModel.resetToIdle() }
                    )
                }
                
                is SessionState.WaitingForDevice -> WaitingScreen(
                    op = targetState.queuedOp,
                    onCancel = { viewModel.cancelWaiting() }
                )
                
                is SessionState.OperationComplete -> CompleteScreen(
                    op = targetState.op,
                    success = targetState.success,
                    message = targetState.message,
                    onDismiss = { viewModel.resetToIdle() },
                    onViewAudit = { viewModel.exportSessionReport() }
                )
                
                is SessionState.Error -> ErrorScreen(
                    message = targetState.message,
                    onRetry = {
                        if (targetState.queuedOp != null) {
                            viewModel.queueOperation(targetState.queuedOp)
                        } else {
                            viewModel.resetToIdle()
                        }
                    }
                )
                
                is SessionState.PermissionDenied -> ErrorScreen(
                    message = "USB permission denied. Request again?",
                    onRetry = {
                        if (targetState.queuedOp != null) {
                            viewModel.queueOperation(targetState.queuedOp)
                        } else {
                            viewModel.resetToIdle()
                        }
                    }
                )

                is SessionState.TestHarness -> TestHarnessScreen(viewModel = viewModel)
                
                is SessionState.Reporting -> ReportingScreen(
                    reportFile = targetState.reportFile,
                    viewModel = viewModel
                )
                
                is SessionState.PartitionPreview -> PartitionManagerScreen(
                    partitions = targetState.partitions,
                    viewModel = viewModel
                )

                // Idle and ConnectedReady show nothing on top, 
                // but MainScreen reacts to them natively
                else -> Unit 
            }
        }
    }
}
