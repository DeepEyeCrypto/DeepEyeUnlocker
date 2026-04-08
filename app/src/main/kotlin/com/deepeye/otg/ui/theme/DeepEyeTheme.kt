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

// ── Dark-First Color Scheme (Mode: Security/Terminal) ──────────────────────────
private val DeepEyeDarkColorScheme = darkColorScheme(
    primary = StitchTokens.Primary,
    onPrimary = Color.White,
    primaryContainer = StitchTokens.Primary.copy(alpha = 0.2f),
    onPrimaryContainer = StitchTokens.TextPrimary,
    
    secondary = StitchTokens.AccentAdb,
    onSecondary = Color.Black,
    secondaryContainer = StitchTokens.AccentAdb.copy(alpha = 0.2f),
    onSecondaryContainer = StitchTokens.TextPrimary,
    
    background = StitchTokens.BackgroundDark,
    onBackground = StitchTokens.TextPrimary,
    
    surface = StitchTokens.SurfaceDark,
    onSurface = StitchTokens.TextPrimary,
    surfaceVariant = StitchTokens.GlassSurface,
    onSurfaceVariant = StitchTokens.TextSecondary,
    
    outline = StitchTokens.GlassBorder,
    outlineVariant = Color.White.copy(alpha = 0.05f),
    
    error = Color(0xFFFF1744), // Direct color to avoid reference issues
    onError = Color.White
)

private val DeepEyeTypography = Typography(
    displayLarge = StitchTokens.DisplayLarge,
    titleLarge = StitchTokens.TitleLarge,
    bodyLarge = StitchTokens.BodyMedium.copy(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = StitchTokens.BodyMedium,
    labelSmall = StitchTokens.LabelSmall
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
            window.statusBarColor = StitchTokens.BackgroundDark.toArgb()
            window.navigationBarColor = StitchTokens.BackgroundDark.toArgb()
            
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
