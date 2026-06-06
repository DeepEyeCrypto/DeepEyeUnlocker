package com.deepeye.otg.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deepeye.otg.ui.theme.DeepEyeType
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeChild

@Composable
fun GlassTabRow(
    tabs: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    hazeState: HazeState,
    performanceMode: Boolean = false
) {
    val hazeStyle = remember {
        HazeStyle(
            backgroundColor = com.deepeye.otg.ui.theme.DeepEyeColors.BG_VOID,
            tint = HazeTint(Color.White.copy(alpha = 0.05f)),
            blurRadius = 24.dp,
            noiseFactor = 0.02f
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .then(
                if (!performanceMode) Modifier.hazeChild(state = hazeState, style = hazeStyle)
                else Modifier.background(com.deepeye.otg.ui.theme.DeepEyeColors.BG_SURFACE)
            )
            .border(1.dp, com.deepeye.otg.ui.theme.DeepEyeColors.WHITE_LOW.copy(0.3f), RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center
    ) {
        ScrollableTabRow(
            selectedTabIndex = selectedIndex,
            containerColor = Color.Transparent,
            divider = {},
            edgePadding = 8.dp,
            indicator = { tabPositions ->
                SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedIndex]),
                    height = 2.dp,
                    color = com.deepeye.otg.ui.theme.DeepEyeColors.NEON_PURPLE
                )
            }
        ) {
            tabs.forEachIndexed { index, title ->
                val selected = selectedIndex == index
                Tab(
                    selected = selected,
                    onClick = { onSelect(index) },
                    modifier = Modifier.height(48.dp)
                ) {
                    Text(
                        text = title.uppercase(),
                        style = com.deepeye.otg.ui.theme.DeepEyeType.CAPTION.copy(fontSize = 11.sp),
                        color = if (selected) com.deepeye.otg.ui.theme.DeepEyeColors.WHITE_HIGH else com.deepeye.otg.ui.theme.DeepEyeColors.WHITE_MED,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }
        }
    }
}
