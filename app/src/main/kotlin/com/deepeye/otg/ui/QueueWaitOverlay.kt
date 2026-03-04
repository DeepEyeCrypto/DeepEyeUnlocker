package com.deepeye.otg.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.deepeye.otg.usb.SessionState

@Composable
fun QueueWaitOverlay(
    session: SessionState,
    onCancel: () -> Unit,
    onDismiss: () -> Unit,
    onRetry: () -> Unit
) {
    when (session) {
        is SessionState.WaitingForDevice -> {
            WaitingForDeviceScreen(
                queuedOp = session.queuedOp,
                onCancel = onCancel
            )
        }
        is SessionState.ReenumerationWait -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                ReenumerationWaitBanner()
            }
        }
        is SessionState.ExecutingOperation -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                ExecutingOperationOverlay(
                    op = session.op,
                    protocol = session.protocol,
                    progress = session.progress,
                    statusMsg = session.statusMsg
                )
            }
        }
        is SessionState.OperationComplete -> {
            Box(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                OperationCompleteBanner(
                    op = session.op,
                    success = session.success,
                    message = session.message,
                    onDismiss = onDismiss
                )
            }
        }
        is SessionState.PermissionDenied -> {
            PermissionDeniedScreen(onRetry = onRetry)
        }
        is SessionState.Error -> {
            ErrorScreen(message = session.message, onRetry = onRetry)
        }
        else -> {}
    }
}
