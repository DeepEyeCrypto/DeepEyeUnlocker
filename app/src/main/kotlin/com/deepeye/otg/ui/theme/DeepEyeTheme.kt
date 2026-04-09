package com.deepeye.otg.ui.theme

import android.os.Build
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// DeepEyeType is defined in StitchTokens.kt

// ── Dark-First Color Scheme (Mode: Security/Terminal) ──────────────────────────
private val DeepEyeDarkColorScheme = darkColorScheme(
    primary = DeepEyeColors.NEON_PURPLE,
    onPrimary = DeepEyeColors.WHITE_HIGH,
    primaryContainer = DeepEyeColors.NEON_PURPLE.copy(alpha = 0.2f),
    onPrimaryContainer = DeepEyeColors.WHITE_HIGH,
    
    secondary = DeepEyeColors.NEON_BLUE,
    onSecondary = DeepEyeColors.BG_VOID,
    secondaryContainer = DeepEyeColors.NEON_BLUE.copy(alpha = 0.2f),
    onSecondaryContainer = DeepEyeColors.WHITE_HIGH,
    
    background = DeepEyeColors.BG_VOID,
    onBackground = DeepEyeColors.WHITE_HIGH,
    
    surface = DeepEyeColors.BG_SURFACE,
    onSurface = DeepEyeColors.WHITE_HIGH,
    surfaceVariant = DeepEyeColors.BG_SURFACE.copy(alpha = 0.6f),
    onSurfaceVariant = DeepEyeColors.WHITE_MED,
    
    outline = DeepEyeColors.WHITE_LOW.copy(0.3f),
    outlineVariant = DeepEyeColors.WHITE_HIGH.copy(alpha = 0.05f),
    
    error = DeepEyeColors.NEON_PINK,
    onError = DeepEyeColors.WHITE_HIGH
)

private val DeepEyeTypography = Typography(
    displayLarge = DeepEyeType.HEADER.copy(fontSize = 32.sp),
    titleLarge = DeepEyeType.SUBHEADER.copy(fontSize = 20.sp),
    bodyLarge = DeepEyeType.BODY.copy(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = DeepEyeType.BODY.copy(fontSize = 14.sp),
    labelSmall = DeepEyeType.CAPTION.copy(fontSize = 11.sp)
)

@Composable
fun DeepEyeTheme(
    darkTheme: Boolean = true, // Default to Dark
    dynamicColor: Boolean = false, // Disable dynamic colors to keep brand aesthetic
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val view = androidx.compose.ui.platform.LocalView.current
    
    // ── Edge-to-Edge Sync ──────────────────────────────────────────────────
    if (!view.isInEditMode) {
        androidx.compose.runtime.SideEffect {
            val window = (context as? android.app.Activity)?.window ?: return@SideEffect
            window.statusBarColor = DeepEyeColors.BG_VOID.toArgb()
            window.navigationBarColor = DeepEyeColors.BG_VOID.toArgb()
            
            val controller = androidx.core.view.WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = false // Keep icons white
            controller.isAppearanceLightNavigationBars = false
        }
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> DeepEyeDarkColorScheme // Force Dark for Security tool vibe
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = DeepEyeTypography,
        content = content
    )
}
