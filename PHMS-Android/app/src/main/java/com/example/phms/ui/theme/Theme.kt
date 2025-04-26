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

// Define Retro Light Theme Colors
private val RetroLightColorScheme = lightColorScheme(
    primary = retroBlue,
    onPrimary = retroOnPrimary,
    primaryContainer = retroBlue.copy(alpha = 0.8f), // Lighter variant
    onPrimaryContainer = retroOnPrimary,

    secondary = retroOrange,
    onSecondary = retroOnSecondary,
    secondaryContainer = retroOrange.copy(alpha = 0.7f),
    onSecondaryContainer = retroOnSecondary,

    tertiary = retroRed,
    onTertiary = retroOnTertiary,
    tertiaryContainer = retroRed.copy(alpha = 0.7f),
    onTertiaryContainer = retroOnTertiary,

    background = retroLightCream,
    onBackground = retroOnBackgroundLight,
    surface = retroLightCream, // Can be same as background or slightly different
    onSurface = retroOnSurfaceLight,
    surfaceVariant = Color(0xFFE0E0E0), // A light grey variant
    onSurfaceVariant = retroOnSurfaceLight,

    error = retroDeepRed,
    onError = retroOnError,
    errorContainer = retroDeepRed.copy(alpha = 0.7f),
    onErrorContainer = retroOnError
)

// Define Retro Dark Theme Colors
private val RetroDarkColorScheme = darkColorScheme(
    primary = retroBlue, // Keep the bright blue
    onPrimary = retroOnPrimary,
    primaryContainer = retroDarkBlue, // Use the deep blue for containers
    onPrimaryContainer = retroOnPrimary,

    secondary = retroOrange, // Keep the bright orange
    onSecondary = retroOnSecondary,
    secondaryContainer = retroOrange.copy(alpha = 0.3f), // Darker orange container
    onSecondaryContainer = retroOnSecondary,

    tertiary = retroRed, // Keep the bright red
    onTertiary = retroOnTertiary,
    tertiaryContainer = retroDeepRed, // Use the deep red for containers
    onTertiaryContainer = retroOnError,

    background = retroDarkBlue, // Use the deep blue as background
    onBackground = retroOnBackgroundDark,
    surface = retroDarkSurface, // Use a slightly different dark surface
    onSurface = retroOnSurfaceDark,
    surfaceVariant = Color(0xFF303148), // Darker variant
    onSurfaceVariant = retroOnSurfaceDark,

    error = retroRed, // Use bright red for error state
    onError = retroOnTertiary,
    errorContainer = retroDeepRed,
    onErrorContainer = retroOnError
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