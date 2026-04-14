package com.deepeye.otg.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.deepeye.otg.ui.components.DeviceInfoCard
import com.deepeye.otg.ui.components.GlassCard
import com.deepeye.otg.ui.components.RadarAnimation
import com.deepeye.otg.ui.components.SectionHeader
import com.deepeye.otg.ui.theme.DeepEyeColors
import com.deepeye.otg.usb.UsbLifecycleState

@Composable
fun DeviceScanScreen(
    selectedSession: UsbLifecycleState,
    modifier: Modifier = Modifier,
) {
    val session = sessionPresentation(selectedSession)
    val stage = when (selectedSession) {
        is UsbLifecycleState.Connected -> "Ready"
        is UsbLifecycleState.DeviceDetected, is UsbLifecycleState.PermissionPending -> "Identifying"
        is UsbLifecycleState.Connecting -> "Connecting"
        is UsbLifecycleState.Error, is UsbLifecycleState.PermissionDenied, is UsbLifecycleState.Dead -> "Fault"
        else -> "Scanning"
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            SectionHeader(title = "Device Scan", count = stage)

            RadarAnimation(
                modifier = Modifier.size(260.dp),
                accentColor = session.accent,
                active = stage != "Fault",
                centerIcon = if (stage == "Ready") Icons.Default.PhoneAndroid else Icons.Default.Usb,
            )

            GlassCard(hazeState = null, accentColor = session.accent) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = session.title,
                        style = MaterialTheme.typography.titleLarge,
                        color = DeepEyeColors.TextPrimary,
                    )
                    Text(
                        text = session.subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = DeepEyeColors.TextSecondary,
                    )
                    Text(
                        text = stage.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = session.accent,
                    )
                }
            }
        }

        if (session.fields.isNotEmpty()) {
            DeviceInfoCard(
                title = session.title,
                brand = session.badge,
                subtitle = session.subtitle,
                fields = session.fields,
                thumbnail = if (stage == "Ready") Icons.Default.PhoneAndroid else Icons.Default.Usb,
                status = session.status,
                accentColor = session.accent,
                active = stage == "Ready",
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(220.dp),
            )
        }
    }
}
