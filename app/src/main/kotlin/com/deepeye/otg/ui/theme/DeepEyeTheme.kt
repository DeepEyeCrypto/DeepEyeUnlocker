package com.deepeye.otg.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

/**
 * ThemeMode — persisted via ThemePreferences.
 */
enum class ThemeMode {
    SYSTEM,
    DARK,
    LIGHT,
    MONET,
}

// ── Dark scheme — gold accent + AMOLED bg ──────────
private val DarkColorScheme = darkColorScheme(
    background = DeepEyeColors.Background,
    surface = DeepEyeColors.Surface,
    primary = DeepEyeColors.GoldAccent,
    onPrimary = Color(0xFF0A0A0A),
    secondary = DeepEyeColors.TealSecondary,
    onSecondary = Color(0xFF0A0A0A),
    tertiary = DeepEyeColors.PrimaryCyan,
    onTertiary = Color(0xFF0A0A0A),
    onBackground = DeepEyeColors.TextPrimary,
    onSurface = DeepEyeColors.TextPrimary,
    error = DeepEyeColors.Error,
    surfaceVariant = Color(0xFF1A1A1A),
    outline = DeepEyeColors.BorderGlass,
)

// ── Light scheme (minimal — app is dark-first) ─────
private val LightColorScheme = lightColorScheme(
    background = Color(0xFFF5F5F5),
    surface = Color(0xFFFFFFFF),
    primary = DeepEyeColors.GoldAccent,
    onPrimary = Color(0xFF0A0A0A),
    secondary = DeepEyeColors.TealSecondary,
    onSecondary = Color.White,
    onBackground = Color(0xFF1A1A1A),
    onSurface = Color(0xFF1A1A1A),
    error = DeepEyeColors.Error,
    surfaceVariant = Color(0xFFE8E8E8),
    outline = Color(0xFFCCCCCC),
)

@Composable
fun DeepEyeTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    content: @Composable () -> Unit,
) {
    val colorScheme = when (themeMode) {
        ThemeMode.SYSTEM -> if (isSystemInDarkTheme()) DarkColorScheme else LightColorScheme
        ThemeMode.DARK -> DarkColorScheme
        ThemeMode.LIGHT -> LightColorScheme
        ThemeMode.MONET -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val ctx = LocalContext.current
                dynamicDarkColorScheme(ctx).copy(
                    background = DeepEyeColors.Background,
                    surface = DeepEyeColors.Surface,
                )
            } else {
                DarkColorScheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = DeepEyeTypography,
        shapes = DeepEyeShapes,
        content = content,
    )
}
