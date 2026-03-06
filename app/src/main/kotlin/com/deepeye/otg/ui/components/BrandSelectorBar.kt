package com.deepeye.otg.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
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
import com.deepeye.otg.data.BrandData
import com.deepeye.otg.data.BrandInfo

@Composable
fun BrandSelectorBar(
    selectedIndex: Int,
    onBrandSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    // Auto-scroll selected brand into center view
    LaunchedEffect(selectedIndex) {
        listState.animateScrollToItem(
            index = (selectedIndex - 1).coerceAtLeast(0)
        )
    }

    Column(modifier = modifier) {

        // ── Brand Chips Row ───────────────────────────────────
        LazyRow(
            state = listState,
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            items(
                items = BrandData.brands,
                key = { it.index }
            ) { brand ->
                BrandChip(
                    brand = brand,
                    isSelected = brand.index == selectedIndex,
                    onClick = { onBrandSelected(brand.index) }
                )
            }
        }

        // ── Selected Brand Info Bar ───────────────────────────
        // (same style as ConnectionMode description bar)
        val selectedBrand = BrandData.get(selectedIndex)
        BrandInfoBar(brand = selectedBrand)
    }
}

@Composable
private fun BrandChip(
    brand: BrandInfo,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // ── Spring scale — identical to ConnectionModeChip ───────
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.93f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "brandChipScale"
    )

    // ── Color animations ──────────────────────────────────────
    val bgColor by animateColorAsState(
        targetValue = if (isSelected)
            brand.brandColor.copy(alpha = 0.15f)
        else
            Color.White.copy(alpha = 0.65f),
        animationSpec = tween(200),
        label = "brandBg"
    )

    val textColor by animateColorAsState(
        targetValue = if (isSelected)
            brand.brandColor
        else
            MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(200),
        label = "brandText"
    )

    val borderColor by animateColorAsState(
        targetValue = if (isSelected)
            brand.brandColor.copy(alpha = 0.55f)
        else
            Color.White.copy(alpha = 0.45f),
        animationSpec = tween(200),
        label = "brandBorder"
    )

    // ── Chip layout — IDENTICAL structure to ConnectionModeChip
    Box(
        modifier = Modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .shadow(
                elevation = if (isSelected) 5.dp else 1.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = brand.brandColor.copy(alpha = 0.20f),
                spotColor   = brand.brandColor.copy(alpha = 0.12f)
            )
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(20.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 7.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Brand color indicator dot (same as chipset dot in modes)
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(
                        brand.brandColor.copy(
                            alpha = if (isSelected) 1f else 0.45f
                        )
                    )
            )

            // Brand name
            Text(
                text = brand.name,
                color = textColor,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold
                             else FontWeight.Medium,
                letterSpacing = 0.2.sp
            )

            // Chipset family badge (QC / MTK / BOTH)
            // Only show on selected chip to reduce visual noise
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(5.dp))
                        .background(brand.brandColor.copy(alpha = 0.18f))
                        .padding(horizontal = 5.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = brand.chipsetFamily,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = brand.brandColor,
                        letterSpacing = 0.4.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun BrandInfoBar(brand: BrandInfo) {
    // Same structure as ConnectionModeInfoBar
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 8.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(brand.brandColor.copy(alpha = 0.07f))
            .border(
                1.dp,
                brand.brandColor.copy(alpha = 0.18f),
                RoundedCornerShape(10.dp)
            )
            .padding(horizontal = 12.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Popular chipsets info
        Text(
            text = "Popular: ${brand.popularChipsets}",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )

        // Chipset family pill — same as [QC] / [MTK] / [ALL] in modes
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(brand.brandColor.copy(alpha = 0.15f))
                .border(
                    1.dp,
                    brand.brandColor.copy(alpha = 0.35f),
                    RoundedCornerShape(6.dp)
                )
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = brand.chipsetFamily,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = brand.brandColor,
                letterSpacing = 0.5.sp
            )
        }
    }
}
