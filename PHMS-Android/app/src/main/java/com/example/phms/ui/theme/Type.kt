package com.example.phms.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.phms.R

val PokemonClassicFontFamily = FontFamily(
    Font(R.font.pixeloperator, FontWeight.Normal)
)

val Typography = Typography(

    /* ── Display (rarely shown on phone, but nice to have) ───────── */

    displayLarge  = TextStyle(
        fontFamily    = PokemonClassicFontFamily,
        fontWeight    = FontWeight.Normal,
        fontSize      = 72.sp,   // ≈ 1.33 × headlineLarge
        lineHeight    = 88.sp,
        letterSpacing = (-0.25).sp
    ),
    displayMedium = TextStyle(
        fontFamily    = PokemonClassicFontFamily,
        fontWeight    = FontWeight.Normal,
        fontSize      = 60.sp,   // ≈ 1.11 × headlineLarge
        lineHeight    = 74.sp
    ),
    displaySmall  = TextStyle(
        fontFamily    = PokemonClassicFontFamily,
        fontWeight    = FontWeight.Normal,
        fontSize      = 48.sp,   // ≈ 0.89 × headlineLarge
        lineHeight    = 60.sp
    ),

    /* ── Headlines (taken from your prompt) ──────────────────────── */

    headlineLarge  = TextStyle(
        fontFamily = PokemonClassicFontFamily,
        fontSize   = 54.sp,
        lineHeight = 78.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = PokemonClassicFontFamily,
        fontSize   = 44.sp,
        lineHeight = 64.sp
    ),
    headlineSmall  = TextStyle(
        fontFamily = PokemonClassicFontFamily,
        fontSize   = 32.sp,
        lineHeight = 48.sp
    ),

    /* ── Titles (section headers, dialogs, etc.) ─────────────────── */

    titleLarge  = TextStyle(
        fontFamily    = PokemonClassicFontFamily,
        fontSize      = 30.sp,   // sits halfway between headlineSmall & bodyLarge
        lineHeight    = 40.sp
    ),
    titleMedium = TextStyle(
        fontFamily    = PokemonClassicFontFamily,
        fontSize      = 24.sp,
        lineHeight    = 32.sp,
        letterSpacing = 0.15.sp
    ),
    titleSmall  = TextStyle(
        fontFamily    = PokemonClassicFontFamily,
        fontSize      = 20.sp,
        lineHeight    = 28.sp,
        letterSpacing = 0.1.sp
    ),

    /* ── Body copy ───────────────────────────────────────────────── */

    bodyLarge  = TextStyle(
        fontFamily    = PokemonClassicFontFamily,
        fontSize      = 20.sp,
        lineHeight    = 28.sp,
        letterSpacing = 0.4.sp
    ),
    bodyMedium = TextStyle(
        fontFamily    = PokemonClassicFontFamily,
        fontSize      = 18.sp,
        lineHeight    = 26.sp,
        letterSpacing = 0.3.sp
    ),
    bodySmall  = TextStyle(
        fontFamily    = PokemonClassicFontFamily,
        fontSize      = 16.sp,
        lineHeight    = 22.sp,
        letterSpacing = 0.4.sp
    ),

    /* ── Labels (buttons, chips, bottom nav) ─────────────────────── */

    labelLarge  = TextStyle(
        fontFamily    = PokemonClassicFontFamily,
        fontSize      = 18.sp,
        lineHeight    = 24.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily    = PokemonClassicFontFamily,
        fontSize      = 16.sp,
        lineHeight    = 20.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall  = TextStyle(
        fontFamily    = PokemonClassicFontFamily,
        fontSize      = 14.sp,
        lineHeight    = 18.sp,
        letterSpacing = 0.5.sp
    )
)