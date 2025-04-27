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

private val RetroLightColorScheme = lightColorScheme( // Changed name from RetroLightColorScheme
    primary = healthdexPrimary,
    onPrimary = healthdexOnPrimary,
    primaryContainer = healthdexPrimaryContainer,
    onPrimaryContainer = healthdexOnPrimaryContainer,

    secondary = healthdexSecondary,
    onSecondary = healthdexOnSecondary,
    secondaryContainer = healthdexSecondaryContainer,
    onSecondaryContainer = healthdexOnSecondaryContainer,

    tertiary = healthdexTertiary,
    onTertiary = healthdexOnTertiary,
    tertiaryContainer = healthdexTertiaryContainer,
    onTertiaryContainer = healthdexOnTertiaryContainer,

    error = healthdexError,
    onError = healthdexOnError,
    errorContainer = healthdexErrorContainer,
    onErrorContainer = healthdexOnErrorContainer,

    background = healthdexBackground,
    onBackground = healthdexOnBackground,
    surface = healthdexSurface,
    onSurface = healthdexOnSurface,
    surfaceVariant = healthdexSurfaceVariant,
    onSurfaceVariant = healthdexOnSurfaceVariant,

    outline = healthdexOutline,

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
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> RetroDarkColorScheme
        else -> RetroLightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primaryContainer.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()

            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
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