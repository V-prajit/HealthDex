package com.example.phms.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Update your theme's color scheme
private val RetroLightColorScheme = lightColorScheme(
    primary = retroPrimary,
    onPrimary = retroOnPrimary,
    primaryContainer = retroPrimaryLight,
    onPrimaryContainer = retroOnPrimary,

    secondary = retroSecondary,
    onSecondary = retroOnSecondary,
    secondaryContainer = retroSecondaryLight,
    onSecondaryContainer = retroOnSecondary,

    tertiary = retroAccent,
    onTertiary = retroOnPrimary,
    tertiaryContainer = retroAccent.copy(alpha = 0.7f),
    onTertiaryContainer = retroOnPrimary,

    background = retroBgLight,
    onBackground = retroOnBgLight,
    surface = retroSurfaceLight,
    onSurface = retroOnBgLight,
    surfaceVariant = Color(0xFFEEEEEE),
    onSurfaceVariant = retroOnBgLight,

    error = retroError,
    onError = retroOnPrimary,
    errorContainer = retroError.copy(alpha = 0.7f),
    onErrorContainer = retroOnPrimary
)

// Define Retro Dark Theme Colors
private val RetroDarkColorScheme = darkColorScheme(
    primary = retroPrimaryLight,
    onPrimary = retroOnBgDark,
    primaryContainer = retroPrimaryDark,
    onPrimaryContainer = retroOnPrimary,

    secondary = retroSecondary,
    onSecondary = retroOnBgDark,
    secondaryContainer = retroSecondaryDark,
    onSecondaryContainer = retroOnPrimary,

    tertiary = retroAccent,
    onTertiary = retroOnBgDark,
    tertiaryContainer = retroAccent.copy(alpha = 0.5f),
    onTertiaryContainer = retroOnPrimary,

    background = retroBgDark,
    onBackground = retroOnBgDark,
    surface = retroSurfaceDark,
    onSurface = retroOnBgDark,
    surfaceVariant = Color(0xFF3E3E60),
    onSurfaceVariant = retroOnBgDark,

    error = retroError,
    onError = retroOnPrimary,
    errorContainer = retroError.copy(alpha = 0.7f),
    onErrorContainer = retroOnPrimary
)

@Composable
fun PHMSTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false, // Keep dynamic color off to use Retro theme
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        // Apply Retro themes based on darkTheme flag
        darkTheme -> RetroDarkColorScheme
        else -> RetroLightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Set status bar to match the primary container or background
            window.statusBarColor = colorScheme.primaryContainer.toArgb()
            // Set navigation bar to match background or surface variant
            window.navigationBarColor = colorScheme.background.toArgb() // Match background

            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            // Set navigation bar icons light/dark based on theme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography, // Keep existing Typography or adjust if needed
        shapes = Shapes,       // Keep existing Shapes (currently 0dp rounding)
        content = content
    )
}