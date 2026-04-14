package com.deepeye.otg.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val deep_eye_dark_color_scheme = darkColorScheme(
    primary = DeepEyeColors.PrimaryCyan,
    onPrimary = androidx.compose.ui.graphics.Color.Black,
    primaryContainer = androidx.compose.ui.graphics.Color(0xFF002B33),
    onPrimaryContainer = DeepEyeColors.TextPrimary,
    secondary = DeepEyeColors.Success,
    onSecondary = androidx.compose.ui.graphics.Color.Black,
    secondaryContainer = androidx.compose.ui.graphics.Color(0xFF003D23),
    onSecondaryContainer = DeepEyeColors.TextPrimary,
    tertiary = DeepEyeColors.PurpleDim,
    onTertiary = DeepEyeColors.TextPrimary,
    tertiaryContainer = androidx.compose.ui.graphics.Color(0xFF24124D),
    onTertiaryContainer = DeepEyeColors.TextPrimary,
    background = DeepEyeColors.Background,
    onBackground = DeepEyeColors.TextPrimary,
    surface = DeepEyeColors.Surface,
    onSurface = DeepEyeColors.TextPrimary,
    surfaceVariant = DeepEyeColors.Surface2,
    onSurfaceVariant = DeepEyeColors.TextSecondary,
    error = DeepEyeColors.Error,
    onError = DeepEyeColors.TextPrimary,
    errorContainer = androidx.compose.ui.graphics.Color(0xFF3F1212),
    onErrorContainer = DeepEyeColors.TextPrimary,
    outline = DeepEyeColors.TextFaint,
    outlineVariant = DeepEyeColors.GlassBorder,
)

private val deep_eye_light_color_scheme = lightColorScheme(
    primary = DeepEyeColors.PrimaryDim,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = androidx.compose.ui.graphics.Color(0xFFD7FAFF),
    onPrimaryContainer = androidx.compose.ui.graphics.Color(0xFF002B33),
    secondary = androidx.compose.ui.graphics.Color(0xFF007A44),
    onSecondary = androidx.compose.ui.graphics.Color.White,
    tertiary = DeepEyeColors.PurpleDim,
    onTertiary = androidx.compose.ui.graphics.Color.White,
    background = androidx.compose.ui.graphics.Color(0xFFF6FBFF),
    onBackground = androidx.compose.ui.graphics.Color(0xFF111318),
    surface = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
    onSurface = androidx.compose.ui.graphics.Color(0xFF111318),
    surfaceVariant = androidx.compose.ui.graphics.Color(0xFFE8F2F7),
    onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFF55606F),
    error = DeepEyeColors.Error,
    onError = androidx.compose.ui.graphics.Color.White,
    outline = androidx.compose.ui.graphics.Color(0xFF8392A5),
    outlineVariant = androidx.compose.ui.graphics.Color(0xFFD2DCE4),
)

enum class ThemeMode {
    SYSTEM,
    DARK,
    LIGHT,
    MONET,
}

private fun monet_color_scheme(
    dark_theme: Boolean,
    base: ColorScheme,
): ColorScheme = base.copy(
    primary = if (dark_theme) DeepEyeColors.PrimaryCyan else DeepEyeColors.PrimaryDim,
    secondary = if (dark_theme) DeepEyeColors.Success else androidx.compose.ui.graphics.Color(0xFF007A44),
    tertiary = DeepEyeColors.PurpleDim,
    error = DeepEyeColors.Error,
    outline = if (dark_theme) DeepEyeColors.TextFaint else androidx.compose.ui.graphics.Color(0xFF8392A5),
    outlineVariant = DeepEyeColors.GlassBorder,
)

@Composable
fun DeepEyeTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val view = LocalView.current
    val system_dark = isSystemInDarkTheme()

    val dark_theme = when (themeMode) {
        ThemeMode.SYSTEM -> system_dark
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.MONET -> system_dark
    }

    val color_scheme = when {
        themeMode == ThemeMode.MONET && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val dynamic = if (dark_theme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            monet_color_scheme(dark_theme = dark_theme, base = dynamic)
        }
        dark_theme -> deep_eye_dark_color_scheme
        else -> deep_eye_light_color_scheme
    }

    if (!view.isInEditMode) {
        SideEffect {
            val window = (context as? Activity)?.window ?: return@SideEffect
            val status_color = color_scheme.background.toArgb()
            window.statusBarColor = status_color
            window.navigationBarColor = status_color
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !dark_theme
            controller.isAppearanceLightNavigationBars = !dark_theme
        }
    }

    MaterialTheme(
        colorScheme = color_scheme,
        typography = DeepEyeTypography,
        shapes = DeepEyeShapes,
        content = content,
    )
}
