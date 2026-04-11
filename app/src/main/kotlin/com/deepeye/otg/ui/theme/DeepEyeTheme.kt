package com.deepeye.otg.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
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
    
    tertiary = DeepEyeColors.NEON_CYAN,
    onTertiary = DeepEyeColors.BG_VOID,
    
    background = DeepEyeColors.BG_VOID,
    onBackground = DeepEyeColors.WHITE_HIGH,
    
    surface = DeepEyeColors.BG_SURFACE,
    onSurface = DeepEyeColors.WHITE_HIGH,
    surfaceVariant = DeepEyeColors.BG_SURFACE.copy(alpha = 0.6f),
    onSurfaceVariant = DeepEyeColors.WHITE_MED,
    
    outline = DeepEyeColors.WHITE_LOW.copy(0.3f),
    outlineVariant = DeepEyeColors.WHITE_HIGH.copy(alpha = 0.05f),
    
    error = DeepEyeColors.NEON_PINK,
    onError = DeepEyeColors.WHITE_HIGH,
    
    inverseSurface = DeepEyeColors.WHITE_HIGH,
    inverseOnSurface = DeepEyeColors.BG_VOID,
)

// ── Light Color Scheme (Clean, Professional) ──────────────────────────────────
private val DeepEyeLightColorScheme = lightColorScheme(
    primary = DeepEyeColors.NEON_PURPLE,
    onPrimary = Color.White,
    primaryContainer = DeepEyeColors.NEON_PURPLE.copy(alpha = 0.12f),
    onPrimaryContainer = DeepEyeColors.NEON_PURPLE.copy(alpha = 0.8f),
    
    secondary = DeepEyeColors.NEON_BLUE,
    onSecondary = Color.White,
    secondaryContainer = DeepEyeColors.NEON_BLUE.copy(alpha = 0.12f),
    onSecondaryContainer = DeepEyeColors.NEON_BLUE.copy(alpha = 0.8f),
    
    tertiary = DeepEyeColors.NEON_CYAN,
    onTertiary = Color.White,
    
    background = Color(0xFFF8F9FA),
    onBackground = Color(0xFF1A1A2E),
    
    surface = Color.White,
    onSurface = Color(0xFF1A1A2E),
    surfaceVariant = Color(0xFFF0F0F5),
    onSurfaceVariant = Color(0xFF4A4A5A),
    
    outline = Color(0xFFD0D0D8),
    outlineVariant = Color(0xFFE8E8F0),
    
    error = DeepEyeColors.NEON_PINK,
    onError = Color.White,
    
    inverseSurface = DeepEyeColors.BG_VOID,
    inverseOnSurface = DeepEyeColors.WHITE_HIGH,
)

// ── Material You (Monet) Enhanced Dark Scheme ─────────────────────────────────
@Composable
private fun rememberMonetDarkColorScheme(): ColorScheme {
    val context = LocalContext.current
    return remember(context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val base = dynamicDarkColorScheme(context)
            // Keep DeepEye accents while respecting user's wallpaper colors
            base.copy(
                primary = DeepEyeColors.NEON_PURPLE,
                secondary = DeepEyeColors.NEON_BLUE,
                tertiary = DeepEyeColors.NEON_CYAN,
                error = DeepEyeColors.NEON_PINK,
            )
        } else {
            DeepEyeDarkColorScheme
        }
    }
}

// ── Material You (Monet) Enhanced Light Scheme ────────────────────────────────
@Composable
private fun rememberMonetLightColorScheme(): ColorScheme {
    val context = LocalContext.current
    return remember(context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val base = dynamicLightColorScheme(context)
            // Keep DeepEye accents while respecting user's wallpaper colors
            base.copy(
                primary = DeepEyeColors.NEON_PURPLE,
                secondary = DeepEyeColors.NEON_BLUE,
                tertiary = DeepEyeColors.NEON_CYAN,
                error = DeepEyeColors.NEON_PINK,
            )
        } else {
            DeepEyeLightColorScheme
        }
    }
}

// ── Theme Mode Enum ───────────────────────────────────────────────────────────
enum class ThemeMode {
    SYSTEM,    // Follow system dark/light
    DARK,      // Always dark
    LIGHT,     // Always light
    MONET,     // Material You dynamic (auto dark/light based on system)
}

private val DeepEyeTypography = Typography(
    displayLarge = DeepEyeType.HEADER.copy(fontSize = 32.sp),
    titleLarge = DeepEyeType.SUBHEADER.copy(fontSize = 20.sp),
    bodyLarge = DeepEyeType.BODY.copy(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = DeepEyeType.BODY.copy(fontSize = 14.sp),
    labelSmall = DeepEyeType.CAPTION.copy(fontSize = 11.sp)
)

@Composable
fun DeepEyeTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val view = androidx.compose.ui.platform.LocalView.current
    val systemDark = isSystemInDarkTheme()
    
    // Determine if we should use dark theme
    val useDarkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.MONET -> systemDark // Monet follows system for dark/light
    }
    
    // Determine if we should use dynamic colors (Material You)
    val useDynamicColor = themeMode == ThemeMode.MONET && 
                          Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    // ── Edge-to-Edge Sync ──────────────────────────────────────────────────
    if (!view.isInEditMode) {
        androidx.compose.runtime.SideEffect {
            val window = (context as? android.app.Activity)?.window ?: return@SideEffect
            
            if (useDarkTheme) {
                window.statusBarColor = DeepEyeColors.BG_VOID.toArgb()
                window.navigationBarColor = DeepEyeColors.BG_VOID.toArgb()
                
                val controller = androidx.core.view.WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = false // Keep icons white
                controller.isAppearanceLightNavigationBars = false
            } else {
                window.statusBarColor = Color(0xFFF8F9FA).toArgb()
                window.navigationBarColor = Color(0xFFF8F9FA).toArgb()
                
                val controller = androidx.core.view.WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = true // Dark icons on light bg
                controller.isAppearanceLightNavigationBars = true
            }
        }
    }

    val colorScheme = when {
        useDynamicColor -> {
            if (useDarkTheme) rememberMonetDarkColorScheme() else rememberMonetLightColorScheme()
        }
        useDarkTheme -> DeepEyeDarkColorScheme
        else -> DeepEyeLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = DeepEyeTypography,
        content = content
    )
}
