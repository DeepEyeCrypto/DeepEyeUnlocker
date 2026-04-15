package com.deepeye.otg.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.PhoneIphone
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.deepeye.otg.R
import com.deepeye.otg.ui.apple.AppleProToolsScreen
import com.deepeye.otg.ui.device.DeviceSupportScreen
import com.deepeye.otg.ui.theme.DeepEyeColors
import com.deepeye.otg.ui.screens.qualcomm.EdlScreen
import com.deepeye.otg.ui.screens.samsung.SamsungToolsScreen
import com.deepeye.otg.viewmodel.UsbViewModel

private enum class DeepEyeRootTab(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Default.Home),
    DEVICES("Devices", Icons.Default.Usb),
    APPLE("Apple", Icons.Default.PhoneIphone),
    LOGS("Logs", Icons.Default.List),
    SETTINGS("Settings", Icons.Default.Settings),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeepEyeMainScreen(viewModel: UsbViewModel) {
    val sessions by viewModel.sessions.collectAsStateWithLifecycle()
    val selectedKey by viewModel.selectedDeviceKey.collectAsStateWithLifecycle()
    val currentNav by viewModel.currentNav.collectAsStateWithLifecycle()
    val logs by viewModel.logs.collectAsStateWithLifecycle()
    val statusMsg by viewModel.statusMsg.collectAsStateWithLifecycle()

    var rootTab by rememberSaveable { mutableStateOf(DeepEyeRootTab.HOME) }

    val selectedSession = selectedKey?.let { sessions[it] } ?: sessions.values.firstOrNull() ?: com.deepeye.otg.usb.UsbLifecycleState.Idle

    Scaffold(
        containerColor = DeepEyeColors.Background,
        topBar = {
            DeepEyeTopBar(
                statusMsg = statusMsg,
                connected = sessions.isNotEmpty(),
                notificationCount = logs.takeLast(99).size,
            )
        },
        bottomBar = {
            DeepEyeBottomBar(
                selected = rootTab,
                onSelect = { tab ->
                    rootTab = tab
                    when (tab) {
                        DeepEyeRootTab.HOME -> viewModel.setNav(NavTarget.DASHBOARD)
                        DeepEyeRootTab.DEVICES -> viewModel.setNav(NavTarget.DEVICES)
                        DeepEyeRootTab.LOGS -> viewModel.setNav(NavTarget.LOG_SCREEN)
                        DeepEyeRootTab.SETTINGS -> viewModel.setNav(NavTarget.SETTINGS)
                        DeepEyeRootTab.APPLE -> Unit
                    }
                },
            )
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(DeepEyeColors.Background, DeepEyeColors.Surface),
                    ),
                )
                .padding(paddingValues),
        ) {
            when {
                rootTab == DeepEyeRootTab.APPLE -> AppleProToolsScreen()

                currentNav == NavTarget.MTK_EXPLOIT || currentNav == NavTarget.MTK_UNLOCK -> DeepEyeMtkScreen(mainViewModel = viewModel)

                currentNav == NavTarget.EDL_CONSOLE -> EdlScreen(
                    onBack = {
                        rootTab = DeepEyeRootTab.DEVICES
                        viewModel.setNav(NavTarget.DEVICES)
                    },
                )

                currentNav == NavTarget.SAMSUNG_ODIN -> SamsungToolsScreen(
                    onBack = {
                        rootTab = DeepEyeRootTab.DEVICES
                        viewModel.setNav(NavTarget.DEVICES)
                    },
                )

                currentNav == NavTarget.FORENSICS_LAB -> {
                    val forensicsViewModel: com.deepeye.otg.viewmodel.research.ForensicsViewModel = androidx.hilt.navigation.compose.hiltViewModel()
                    com.deepeye.otg.ui.screens.forensics.ForensicsDashboardScreen(
                        viewModel = forensicsViewModel,
                        onBack = {
                            rootTab = DeepEyeRootTab.DEVICES
                            viewModel.setNav(NavTarget.DEVICES)
                        }
                    )
                }

                currentNav == NavTarget.MISSION_HUB -> com.deepeye.otg.ui.gsmg.BypassScreen()

                currentNav == NavTarget.DEVICE_SUPPORT -> DeviceSupportScreen()

                currentNav == NavTarget.LOG_SCREEN || rootTab == DeepEyeRootTab.LOGS -> DeepEyeLogScreen(mainViewModel = viewModel)

                currentNav == NavTarget.SETTINGS || rootTab == DeepEyeRootTab.SETTINGS -> DeepEyeSettingsScreen(viewModel = viewModel)

                currentNav == NavTarget.DEVICES || rootTab == DeepEyeRootTab.DEVICES -> DeepEyeDevicesScreen(
                    selectedSession = selectedSession,
                    recentLogs = logs,
                    onNavigateMtk = {
                        rootTab = DeepEyeRootTab.DEVICES
                        viewModel.setNav(NavTarget.MTK_EXPLOIT)
                    },
                    onNavigateEdl = {
                        rootTab = DeepEyeRootTab.DEVICES
                        viewModel.setNav(NavTarget.EDL_CONSOLE)
                    },
                    onNavigateSamsung = {
                        rootTab = DeepEyeRootTab.DEVICES
                        viewModel.setNav(NavTarget.SAMSUNG_ODIN)
                    },
                    onNavigateFrp = {
                        rootTab = DeepEyeRootTab.DEVICES
                        viewModel.setNav(NavTarget.MISSION_HUB)
                    },
                    onNavigateDiagnostics = {
                        rootTab = DeepEyeRootTab.DEVICES
                        viewModel.setNav(NavTarget.DEVICE_SUPPORT)
                    },
                )

                else -> HomeScreen(
                    selectedSession = selectedSession,
                    recentLogs = logs,
                    connectedCount = sessions.size,
                    onNavigateDevices = {
                        rootTab = DeepEyeRootTab.DEVICES
                        viewModel.setNav(NavTarget.DEVICES)
                    },
                    onNavigateApple = { rootTab = DeepEyeRootTab.APPLE },
                    onNavigateLogs = {
                        rootTab = DeepEyeRootTab.LOGS
                        viewModel.setNav(NavTarget.LOG_SCREEN)
                    },
                    onNavigateMtk = {
                        rootTab = DeepEyeRootTab.DEVICES
                        viewModel.setNav(NavTarget.MTK_EXPLOIT)
                    },
                    onNavigateEdl = {
                        rootTab = DeepEyeRootTab.DEVICES
                        viewModel.setNav(NavTarget.EDL_CONSOLE)
                    },
                    onNavigateSamsung = {
                        rootTab = DeepEyeRootTab.DEVICES
                        viewModel.setNav(NavTarget.SAMSUNG_ODIN)
                    },
                    onNavigateFrp = {
                        rootTab = DeepEyeRootTab.DEVICES
                        viewModel.setNav(NavTarget.MISSION_HUB)
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeepEyeTopBar(
    statusMsg: String,
    connected: Boolean,
    notificationCount: Int,
) {
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            scrolledContainerColor = Color.Transparent,
        ),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_deepeye_logo),
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                )
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "DeepEye Unlocker",
                        color = DeepEyeColors.TextPrimary,
                        style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = statusMsg,
                        color = DeepEyeColors.TextSecondary,
                        style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                    )
                }
            }
        },
        actions = {
            Row(
                modifier = Modifier.padding(end = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Wifi,
                        contentDescription = null,
                        tint = if (connected) DeepEyeColors.PrimaryCyan else DeepEyeColors.TextFaint,
                    )
                    Box(
                        modifier = Modifier
                            .padding(start = 4.dp)
                            .size(6.dp)
                            .background(
                                color = if (connected) DeepEyeColors.Success else DeepEyeColors.TextFaint,
                                shape = CircleShape,
                            ),
                    )
                }

                Box(contentAlignment = Alignment.TopEnd) {
                    Icon(
                        imageVector = Icons.Default.NotificationsNone,
                        contentDescription = null,
                        tint = DeepEyeColors.TextPrimary,
                    )
                    if (notificationCount > 0) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .background(DeepEyeColors.PrimaryCyan, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = notificationCount.coerceAtMost(9).toString(),
                                color = Color.Black,
                                style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                                modifier = Modifier.alpha(0.92f),
                            )
                        }
                    }
                }
            }
        },
    )
}

@Composable
private fun DeepEyeBottomBar(
    selected: DeepEyeRootTab,
    onSelect: (DeepEyeRootTab) -> Unit,
) {
    NavigationBar(
        containerColor = DeepEyeColors.Surface,
        tonalElevation = 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .background(DeepEyeColors.Surface),
    ) {
        DeepEyeRootTab.values().forEach { tab ->
            val isSelected = tab == selected
            NavigationBarItem(
                selected = isSelected,
                onClick = { onSelect(tab) },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = Color.Transparent,
                    selectedIconColor = DeepEyeColors.PrimaryCyan,
                    selectedTextColor = DeepEyeColors.PrimaryCyan,
                    unselectedIconColor = DeepEyeColors.TextSecondary,
                    unselectedTextColor = DeepEyeColors.TextSecondary,
                ),
                icon = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(imageVector = tab.icon, contentDescription = tab.label)
                        Box(
                            modifier = Modifier
                                .height(2.dp)
                                .fillMaxWidth(0.55f)
                                .background(
                                    color = if (isSelected) DeepEyeColors.PrimaryCyan else Color.Transparent,
                                    shape = CircleShape,
                                ),
                        )
                    }
                },
                label = { Text(tab.label) },
            )
        }
    }
}
