package com.deepeye.otg.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deepeye.otg.data.ConnectionMode

@Composable
fun ConnectionModeBar(
    selectedMode: ConnectionMode,
    onModeSelected: (ConnectionMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    // Auto-scroll selected into view
    val selectedIndex = ConnectionMode.entries.indexOf(selectedMode)
    LaunchedEffect(selectedMode) {
        listState.animateScrollToItem(
            index = (selectedIndex - 1).coerceAtLeast(0)
        )
    }

    Column(modifier = modifier) {

        // ── Mode Chip Row ─────────────────────────────────────
        LazyRow(
            state = listState,
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            items(
                items = ConnectionMode.entries,
                key = { it.name }
            ) { mode ->
                ConnectionModeChip(
                    mode = mode,
                    isSelected = mode == selectedMode,
                    onClick = { onModeSelected(mode) }
                )
            }
        }

        // ── Selected Mode Info Bar ────────────────────────────
        ConnectionModeInfoBar(mode = selectedMode)
    }
}

@Composable
private fun ConnectionModeChip(
    mode: ConnectionMode,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "chipScale"
    )

    val bgColor by animateColorAsState(
        targetValue = if (isSelected)
            MaterialTheme.colorScheme.primaryContainer
        else
            Color.White.copy(alpha = 0.65f),
        label = "chipBg"
    )

    val textColor by animateColorAsState(
        targetValue = if (isSelected)
            MaterialTheme.colorScheme.primary
        else
            MaterialTheme.colorScheme.onSurfaceVariant,
        label = "chipText"
    )

    val borderColor by animateColorAsState(
        targetValue = if (isSelected)
            MaterialTheme.colorScheme.primary.copy(alpha = 0.60f)
        else
            Color.White.copy(alpha = 0.45f),
        label = "chipBorder"
    )

    Box(
        modifier = Modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .shadow(
                elevation = if (isSelected) 4.dp else 1.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = mode.chipsetColor().copy(alpha = 0.15f),
                spotColor = mode.chipsetColor().copy(alpha = 0.10f)
            )
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(20.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            // Chipset indicator dot
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(mode.chipsetColor().copy(
                        alpha = if (isSelected) 1f else 0.5f
                    ))
            )

            Text(
                text = mode.label,
                color = textColor,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold
                             else FontWeight.Medium,
                letterSpacing = 0.3.sp
            )

            // Hardware warning dot
            if (mode.requiresHardware) {
                Text(
                    text = "⚡",
                    fontSize = 9.sp,
                    modifier = Modifier.padding(start = 1.dp)
                )
            }
        }
    }
}

@Composable
private fun ConnectionModeInfoBar(mode: ConnectionMode) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 8.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(
                color = mode.chipsetColor().copy(alpha = 0.08f)
            )
            .border(
                1.dp,
                mode.chipsetColor().copy(alpha = 0.20f),
                RoundedCornerShape(10.dp)
            )
            .padding(horizontal = 12.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = mode.description,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )

        // Chipset badge
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(mode.chipsetColor().copy(alpha = 0.15f))
                .border(
                    1.dp,
                    mode.chipsetColor().copy(alpha = 0.35f),
                    RoundedCornerShape(6.dp)
                )
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = mode.chipset,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = mode.chipsetColor(),
                letterSpacing = 0.5.sp
            )
        }
    }
}
