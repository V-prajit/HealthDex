package com.example.phms.ui.theme

import androidx.compose.ui.graphics.Color

// Light Theme Colors
val PrimaryLight = Color(0xFF06A3B7)  // Teal blue - trustworthy, medical
val OnPrimaryLight = Color(0xFFFFFFFF)
val PrimaryContainerLight = Color(0xFFB8EAEF)
val OnPrimaryContainerLight = Color(0xFF001F24)

val SecondaryLight = Color(0xFFF5A742)  // Warm orange-yellow
val OnSecondaryLight = Color(0xFF000000)
val SecondaryContainerLight = Color(0xFFFFDDB3)
val OnSecondaryContainerLight = Color(0xFF261A00)

val TertiaryLight = Color(0xFF9256D9)  // Soft purple
val OnTertiaryLight = Color(0xFFFFFFFF)
val TertiaryContainerLight = Color(0xFFEADDFF)
val OnTertiaryContainerLight = Color(0xFF22005D)

val BackgroundLight = Color(0xFFF5F7FA)  // Very light blue-gray
val OnBackgroundLight = Color(0xFF191C1E)
val SurfaceLight = Color(0xFFFFFFFF)
val OnSurfaceLight = Color(0xFF191C1E)
val SurfaceVariantLight = Color(0xFFE8EFF4)
val OnSurfaceVariantLight = Color(0xFF40484C)

val ErrorLight = Color(0xFFB3261E)
val SuccessLight = Color(0xFF6AA84F)

// Dark Theme Colors
val PrimaryDark = Color(0xFF4FD8EB)
val OnPrimaryDark = Color(0xFF00363D)
val PrimaryContainerDark = Color(0xFF004F58)
val OnPrimaryContainerDark = Color(0xFFB8EAEF)

val SecondaryDark = Color(0xFFFFB955)
val OnSecondaryDark = Color(0xFF412D00)
val SecondaryContainerDark = Color(0xFF5C4200)
val OnSecondaryContainerDark = Color(0xFFFFDDB3)

val TertiaryDark = Color(0xFFD0BCFF)
val OnTertiaryDark = Color(0xFF381E72)
val TertiaryContainerDark = Color(0xFF4F378B)
val OnTertiaryContainerDark = Color(0xFFEADDFF)

val BackgroundDark = Color(0xFF121212)
val OnBackgroundDark = Color(0xFFE1E3E5)
val SurfaceDark = Color(0xFF1E1E1E)
val OnSurfaceDark = Color(0xFFE1E3E5)
val SurfaceVariantDark = Color(0xFF2D2D2D)
val OnSurfaceVariantDark = Color(0xFFC0C8CD)

val ErrorDark = Color(0xFFF2B8B5)

// Vital Sign Chart Colors (refined from existing)
val ChartRed = Color(0xFFE57373)      // Heart Rate
val ChartBlue = Color(0xFF64B5F6)     // Blood Pressure
val ChartOrange = Color(0xFFF5A742)   // Glucose - matched to secondary
val ChartPurple = Color(0xFF9256D9)   // Cholesterol - matched to tertiary
val ChartGridColor = Color.Gray.copy(alpha = 0.3f)

// Note Tag Colors (refined)
val TagDiet = Color(0xFFEF5350)       // Red for diet
val TagMedication = Color(0xFF06A3B7) // Primary teal for medication
val TagHealth = Color(0xFF66BB6A)     // Green for health
val TagMisc = Color(0xFFF5A742)       // Secondary orange for misc