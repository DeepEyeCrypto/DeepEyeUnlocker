package com.deepeye.otg.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.deepeye.otg.ui.theme.DeepEyeColors

data class DeviceField(
    val label: String,
    val value: String,
)

@Composable
fun DeviceInfoCard(
    title: String,
    brand: String,
    fields: List<DeviceField>,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    thumbnail: ImageVector,
    status: StatusIndicatorState = StatusIndicatorState.DISCONNECTED,
    accentColor: Color = DeepEyeColors.PrimaryCyan,
    active: Boolean = false,
) {
    val borderProgress by rememberInfiniteTransition(label = "deviceInfoSweep").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
        ),
        label = "deviceInfoBorderProgress",
    )

    GlassCard(
        hazeState = null,
        modifier = modifier,
        accentColor = if (active) accentColor else Color.Transparent,
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            if (active) {
                Canvas(modifier = Modifier.matchParentSize()) {
                    val startX = size.width * borderProgress
                    drawRoundRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                accentColor.copy(alpha = 0.40f),
                                Color.Transparent,
                            ),
                            startX = startX - size.width * 0.35f,
                            endX = startX,
                        ),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx()),
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(accentColor.copy(alpha = 0.14f), CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = thumbnail,
                                contentDescription = null,
                                tint = accentColor,
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleLarge,
                                color = DeepEyeColors.TextPrimary,
                            )
                            if (subtitle != null) {
                                Text(
                                    text = subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = DeepEyeColors.TextSecondary,
                                )
                            }
                        }
                    }

                    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = brand,
                            style = MaterialTheme.typography.labelSmall,
                            color = accentColor,
                            modifier = Modifier
                                .background(accentColor.copy(alpha = 0.14f), RoundedCornerShape(999.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                        StatusIndicator(state = status)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    repeat(4) { index ->
                        val alpha = if (active) 0.32f + (index * 0.16f) else 0.18f
                        Box(
                            modifier = Modifier
                                .width(8.dp)
                                .height((8 + index * 6).dp)
                                .background(accentColor.copy(alpha = alpha), RoundedCornerShape(99.dp)),
                        )
                    }
                }

                fields.chunked(2).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        row.forEach { field ->
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Text(
                                    text = field.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = DeepEyeColors.TextSecondary,
                                )
                                Text(
                                    text = field.value,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = DeepEyeColors.TextPrimary,
                                )
                            }
                        }
                        repeat(2 - row.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}
