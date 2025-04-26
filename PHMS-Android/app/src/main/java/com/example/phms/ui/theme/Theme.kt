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

// Apply Pokémon Light Theme Colors
private val LightColorScheme = lightColorScheme(
    primary = pokeBlue,
    onPrimary = pokeOnPrimary,
    primaryContainer = pokeLightBlue, // Or another light variant
    onPrimaryContainer = Color.Black,

    secondary = pokeYellow,
    onSecondary = pokeOnSecondary,
    secondaryContainer = pokeLightYellow,
    onSecondaryContainer = Color.Black, // Or a dark yellow/brown

    tertiary = pokeRed,
    onTertiary = pokeOnPrimary,
    tertiaryContainer = Color(0xFFFFCDD2), // Light red variant
    onTertiaryContainer = Color.Black,

    background = pokeBackgroundLight,
    onBackground = Color.Black,
    surface = pokeSurfaceLight,
    onSurface = Color.Black,
    surfaceVariant = Color(0xFFE0E0E0), // Slightly darker gray variant
    onSurfaceVariant = Color.Black,

    error = pokeRed, // Use pokeRed for error
    onError = pokeOnError,
    errorContainer = Color(0xFFFFCDD2),
    onErrorContainer = Color.Black
)

// Apply Pokémon Dark Theme Colors
private val DarkColorScheme = darkColorScheme(
    primary = pokeBlue, // Keep blue primary, maybe slightly lighter?
    onPrimary = pokeOnPrimary,
    primaryContainer = pokeDarkBlue,
    onPrimaryContainer = Color.White,

    secondary = pokeYellow, // Keep yellow secondary
    onSecondary = pokeOnSecondary,
    secondaryContainer = Color(0xFF4D4100), // Dark yellow/brown
    onSecondaryContainer = pokeLightYellow,

    tertiary = pokeRed, // Keep red tertiary
    onTertiary = pokeOnPrimary,
    tertiaryContainer = Color(0xFF8B0000), // Dark red
    onTertiaryContainer = Color(0xFFFFCDD2),

    background = pokeBackgroundDark,
    onBackground = Color.White,
    surface = pokeSurfaceDark,
    onSurface = Color.White,
    surfaceVariant = Color(0xFF424242), // Darker gray variant
    onSurfaceVariant = Color(0xFFCACACA),

    error = Color(0xFFFFB4AB), // Lighter red for dark theme errors
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFB4AB)
)


@Composable
fun PHMSTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false, // Keep dynamic color off to use Pokemon theme
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        // Apply Pokemon themes based on darkTheme flag
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Set status bar to match the primary container or a specific theme color
            window.statusBarColor = colorScheme.primaryContainer.toArgb() // Or pokeBlue.toArgb() etc.
            // Set navigation bar to match background or surface variant
            window.navigationBarColor = colorScheme.surfaceVariant.toArgb() // Match bottom bar suggestion

            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            // Set navigation bar icons light/dark based on theme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}