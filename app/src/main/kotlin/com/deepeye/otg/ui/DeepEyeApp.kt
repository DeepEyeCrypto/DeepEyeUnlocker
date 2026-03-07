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
import com.deepeye.otg.viewmodel.UsbViewModel

@Composable
fun DeepEyeApp(viewModel: UsbViewModel) {
    val state by viewModel.queueState.collectAsState()

    // Base padding for safe geometry
    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        AnimatedContent(
            targetState = state,
            transitionSpec = {
                fadeIn(animationSpec = tween(400)) togetherWith 
                fadeOut(animationSpec = tween(400))
            },
            label = "ScreenTransition"
        ) { targetState ->
            when (targetState) {
                is SessionState.Idle -> MainScreen(viewModel = viewModel)
                
                is SessionState.WaitingForDevice -> WaitingScreen(
                    op = targetState.queuedOp,
                    onCancel = { viewModel.cancelWaiting() }
                )
                
                is SessionState.PermissionPending,
                is SessionState.DeviceFound,
                is SessionState.ProtocolDetect,
                is SessionState.ReenumerationWait -> WaitingScreen(
                    op = targetState.let { 
                        // Safe extraction for all intermediate states
                        when(it) {
                            is SessionState.DeviceFound -> it.queuedOp
                            is SessionState.PermissionPending -> it.queuedOp
                            is SessionState.ProtocolDetect -> it.queuedOp
                            is SessionState.ReenumerationWait -> it.queuedOp
                            else -> null
                        }
                    },
                    onCancel = { viewModel.cancelWaiting() }
                )
                
                is SessionState.ConnectedReady -> MainScreen(viewModel = viewModel)
                
                is SessionState.ExecutingOperation -> {
                    val logs by viewModel.logs.collectAsState()
                    ExecutingScreen(
                        op = targetState.op,
                        progress = targetState.progress,
                        statusMsg = targetState.statusMsg,
                        logs = logs,
                        onCancel = { viewModel.resetToIdle() } // Real stop logic needed depending on backend
                    )
                }
                
                is SessionState.OperationComplete -> CompleteScreen(
                    op = targetState.op,
                    success = targetState.success,
                    message = targetState.message,
                    onDismiss = { viewModel.resetToIdle() }
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
                    message = "USB permission denied by user. Request again?",
                    onRetry = {
                        if (targetState.queuedOp != null) {
                            viewModel.queueOperation(targetState.queuedOp)
                        } else {
                            viewModel.resetToIdle()
                        }
                    }
                )
                
                is SessionState.ConnectedMtpOnly -> MtpOnlyScreen(
                    onBack = { viewModel.resetToIdle() }
                )
                
                is SessionState.TestHarness -> TestHarnessScreen(
                    viewModel = viewModel
                )
            }
        }
    }
}
