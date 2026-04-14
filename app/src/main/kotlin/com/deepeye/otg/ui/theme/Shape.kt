package com.deepeye.otg.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

object DeepEyeRadii {
    val extraSmall = 10.dp
    val small = 12.dp
    val medium = 16.dp
    val large = 20.dp
    val extraLarge = 28.dp
}

val DeepEyeShapes = Shapes(
    extraSmall = RoundedCornerShape(DeepEyeRadii.extraSmall),
    small = RoundedCornerShape(DeepEyeRadii.small),
    medium = RoundedCornerShape(DeepEyeRadii.medium),
    large = RoundedCornerShape(DeepEyeRadii.large),
    extraLarge = RoundedCornerShape(DeepEyeRadii.extraLarge),
)
