package com.deepeye.otg.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deepeye.otg.ui.components.GlassBadge
import com.deepeye.otg.ui.theme.StitchTokens

sealed class MissionCategory(val title: String, val icon: ImageVector) {
    object Activation : MissionCategory("Activation", Icons.Default.FlashOn)
    object Fmi : MissionCategory("FMI Services", Icons.Default.CloudOff)
    object Jailbreak : MissionCategory("Jailbreak", Icons.Default.Security)
    object Advanced : MissionCategory("Advanced Modes", Icons.Default.SettingsInputComponent)
    object Toolbox : MissionCategory("Toolbox", Icons.Default.Build)
    object Mtk : MissionCategory("MTK Tools", Icons.Default.Memory)
}

data class MissionItem(
    val category: MissionCategory,
    val title: String,
    val description: String,
    val actionId: String,
    val isPremium: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MissionHubScreen(
    onNavigateBack: () -> Unit,
    onExecuteAction: (String) -> Unit
) {
    var selectedCategory by remember { mutableStateOf<MissionCategory>(MissionCategory.Activation) }

    val missionItems = listOf(
        // Activation
        MissionItem(MissionCategory.Activation, "Hello Activation", "Bypass Hello screen with Signal", "act_hello_signal", true),
        MissionItem(MissionCategory.Activation, "Hello (No Signal)", "WiFi-only iPod mode bypass", "act_hello_no_signal"),
        MissionItem(MissionCategory.Activation, "Passcode Bypass", "Unlock devices on passcode screen", "act_passcode", true),
        MissionItem(MissionCategory.Activation, "MDM Bypass", "Remove corporate management lock", "act_mdm"),
        
        // FMI
        MissionItem(MissionCategory.Fmi, "FMI OFF (Open Menu)", "Remove Apple ID from Home Screen", "fmi_off_open"),
        MissionItem(MissionCategory.Fmi, "iCloud Bypass", "Full activation lock removal", "fmi_icloud_bypass", true),
        
        // Jailbreak
        MissionItem(MissionCategory.Jailbreak, "Auto Jailbreak", "One-click integrated JB tool", "jb_auto"),
        MissionItem(MissionCategory.Jailbreak, "Checkra1n", "A5-A11 Hardware exploit JB", "jb_checkra1n"),
        MissionItem(MissionCategory.Jailbreak, "Palera1n", "iOS 15-16 Rootless JB", "jb_palera1n"),
        
        // Advanced
        MissionItem(MissionCategory.Advanced, "Enter Purple Mode", "Diagnostic mode for SN change", "adv_purple_enter"),
        MissionItem(MissionCategory.Advanced, "Boot Files Mode", "Token backup & restore path", "adv_bootfiles"),
        
        // Toolbox
        MissionItem(MissionCategory.Toolbox, "DFU Assistant", "Interactive mode transition guide", "tool_dfu_guide"),
        MissionItem(MissionCategory.Toolbox, "Exit Recovery", "Force reboot to normal mode", "tool_exit_recovery"),
        MissionItem(MissionCategory.Toolbox, "OTA Blocker", "Prevent automatic updates", "tool_ota_block"),
        MissionItem(MissionCategory.Toolbox, "Reset Lock", "Disable settings restore/reset", "tool_reset_lock"),
        
        // MTK
        MissionItem(MissionCategory.Mtk, "Brom Exploit", "Bypass SLA/DAA via BROM", "mtk_brom_exploit", true),
        MissionItem(MissionCategory.Mtk, "Read Backup", "Full partition dump via Brom", "mtk_read_backup"),
        MissionItem(MissionCategory.Mtk, "Unlock Bootloader", "Instant BL unlock via Brom", "mtk_bl_unlock", true),
        MissionItem(MissionCategory.Mtk, "Security Backup", "Backup NVRAM/EFS/Metadata", "mtk_security_backup")
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("MISSION HUB", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = Color.Transparent
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding).background(
            Brush.verticalGradient(listOf(StitchTokens.Semantic.BackgroundBase, Color.Black))
        )) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                // Category Selector
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    val categories = listOf(
                        MissionCategory.Activation, MissionCategory.Fmi, 
                        MissionCategory.Jailbreak, MissionCategory.Advanced, MissionCategory.Toolbox,
                        MissionCategory.Mtk
                    )
                    categories.forEach { category ->
                        CategoryTab(
                            category = category,
                            isSelected = selectedCategory == category,
                            onClick = { selectedCategory = category }
                        )
                    }
                }

                // Items Grid
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(missionItems.filter { it.category == selectedCategory }) { item ->
                        MissionCard(item = item, onClick = { onExecuteAction(item.actionId) })
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryTab(
    category: MissionCategory,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(8.dp)
            .width(64.dp)
    ) {
        Icon(
            imageVector = category.icon,
            contentDescription = category.title,
            tint = if (isSelected) StitchTokens.Primary else Color.Gray,
            modifier = Modifier.size(28.dp)
        )
        Text(
            text = category.title,
            fontSize = 10.sp,
            color = if (isSelected) Color.White else Color.Gray,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
fun MissionCard(
    item: MissionItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                if (item.isPremium) {
                    Spacer(modifier = Modifier.width(8.dp))
                    GlassBadge(
                        label = "PRO",
                        fillColor = StitchTokens.Primary.copy(alpha = 0.1f),
                        borderColor = StitchTokens.Primary.copy(alpha = 0.2f),
                        textColor = StitchTokens.Primary
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = item.description,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                maxLines = 3
            )
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = StitchTokens.Primary,
                modifier = Modifier.align(Alignment.End).size(20.dp)
            )
        }
    }
}
