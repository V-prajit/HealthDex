package com.example.phms.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.phms.R

val PokemonClassicFontFamily = FontFamily(
    Font(R.font.fontwah, FontWeight.Normal)
)

val DefaultFontFamily = FontFamily.Default


val Typography = Typography(
    // Apply Pokemon Font to ALL styles
    displayLarge = TextStyle(
        fontFamily = PokemonClassicFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.25).sp
    ),
    displayMedium = TextStyle(
        fontFamily = PokemonClassicFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 45.sp,
        lineHeight = 52.sp,
        letterSpacing = 0.sp
    ),
    displaySmall = TextStyle(
        fontFamily = PokemonClassicFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = 0.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = PokemonClassicFontFamily,
        fontWeight = FontWeight.Normal, // Adjust if the font has different weights
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = PokemonClassicFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = PokemonClassicFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontFamily = PokemonClassicFontFamily,
        fontWeight = FontWeight.Normal, // Use Normal if the font doesn't have Bold
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = PokemonClassicFontFamily,
        fontWeight = FontWeight.Normal, // Use Normal if the font doesn't have Medium
        fontSize = 16.sp, // Adjusted size, was 18
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),
    titleSmall = TextStyle(
        fontFamily = PokemonClassicFontFamily,
        fontWeight = FontWeight.Normal, // Use Normal if the font doesn't have Medium
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = PokemonClassicFontFamily, // Apply Pokemon font
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp // Pokemon font might need different spacing
    ),
    bodyMedium = TextStyle(
        fontFamily = PokemonClassicFontFamily, // Apply Pokemon font
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp // Pokemon font might need different spacing
    ),
    bodySmall = TextStyle(
        fontFamily = PokemonClassicFontFamily, // Apply Pokemon font
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp // Pokemon font might need different spacing
    ),
    labelLarge = TextStyle(
        fontFamily = PokemonClassicFontFamily,
        fontWeight = FontWeight.Normal, // Use Normal if font doesn't have Medium
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = PokemonClassicFontFamily,
        fontWeight = FontWeight.Normal, // Use Normal if font doesn't have Medium
        fontSize = 12.sp, // Original size
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    ),
    // Style for Bottom Navigation - explicitly small
    labelSmall = TextStyle(
        fontFamily = PokemonClassicFontFamily,
        fontWeight = FontWeight.Normal, // Use Normal if font doesn't have Medium
        fontSize = 10.sp, // Make this smaller for the bottom bar
        lineHeight = 14.sp, // Adjust line height accordingly
        letterSpacing = 0.5.sp
    )
)