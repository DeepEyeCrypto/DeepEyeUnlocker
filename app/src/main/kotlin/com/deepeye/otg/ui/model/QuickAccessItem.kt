package com.deepeye.otg.ui.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * QuickAccessItem - Represents a feature/tool in the Quick Access grid
 */
data class QuickAccessItem(
    val id: String,
    val label: String,
    val icon: ImageVector,
    val iconTint: Color,
    val navTarget: String,
    val category: QuickAccessCategory,
    val isEnabled: Boolean = true,
    val description: String = ""
)

/**
 * QuickAccessCategory - Categories for organizing Quick Access items
 */
enum class QuickAccessCategory(val displayName: String) {
    BYPASS("Bypass"),
    TOOLS("Tools"),
    FLASH("Flash"),
    RESEARCH("Research"),
    APPLE("Apple")
}
