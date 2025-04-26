package com.example.phms.ui.theme

import androidx.compose.ui.graphics.Color

// --- Pokémon Theme Colors ---
val pokeBlue = Color(0xFF5DB1DF) // Classic Pokémon Blue
val pokeYellow = Color(0xFFFAD37A) // Pikachu Yellow
val pokeRed = Color(0xFFE36776)
val pokeGreen = Color(0xFF85D6AD)
val pokePurple = Color(0xFFAD85D6)
val pokeLightBlue = Color(0xFFADD8E6) // Lighter blue for backgrounds/variants
val pokeLightYellow = Color(0xFFFFFACD) // Lemonyellow for backgrounds/variants
val pokeDarkBlue = Color(0xFF2A3A99) // Darker blue for dark theme / borders
val pokeBackgroundLight = Color(0xFFF0F0F0) // Light gray background
val pokeBackgroundDark = Color(0xFF202124) // Dark gray background
val pokeSurfaceLight = Color(0xFFFFFFFF)
val pokeSurfaceDark = Color(0xFF303134)
val pokeOnPrimary = Color.White // White text on Blue/Red
val pokeOnSecondary = Color.Black // Black text on Yellow
val pokeOnError = Color.White
val pokeBorder = Color(0xFF2A5A80)
val pokeChatUserBg = Color(0xFFFEF33F) // Yellow for User Bubble (from image)
val pokeChatAssistantBg = Color(0xFFFFFEE8) // Off-white/Cream for Assistant Bubble (from image)
val pokeChatBorderColor = Color(0xFF2A5A80) // Dark blue border (similar to image frame)
val pokeChatScreenBg = Color(0xFFADD8E6) // Light blue background for the screen (similar to image frame)
val pokeChatTextColor = Color.Black
val PrimaryLight = Color(0xFF06A3B7)
val OnPrimaryLight = Color(0xFFFFFFFF)
val PrimaryContainerLight = Color(0xFFB8EAEF)
val OnPrimaryContainerLight = Color(0xFF001F24)

// Primary colors
val retroPrimary = Color(0xFF3F51B5)        // Deeper blue, less harsh than current retroBlue
val retroPrimaryLight = Color(0xFF757DE8)   // Lighter variant
val retroPrimaryDark = Color(0xFF002984)    // Darker variant for contrast
val retroOnPrimary = Color.White            // White text on primary colors

// Secondary colors
val retroSecondary = Color(0xFFFF9800)      // Warm orange - softer than current retroOrange
val retroSecondaryLight = Color(0xFFFFCC80) // Light orange
val retroSecondaryDark = Color(0xFFEF6C00)  // Dark orange
val retroOnSecondary = Color.Black          // Black text on secondary colors

// Accent colors (for highlights, buttons)
val retroAccent = Color(0xFF4CAF50)         // Green - for success states/positive actions
val retroError = Color(0xFFE53935)          // Soft red for errors - less harsh than current retroRed
val retroWarning = Color(0xFFFFEB3B)        // Yellow for warnings/alerts

// Background and surface colors
val retroBgLight = Color(0xFFF8F8F8)        // Very light gray with slight warmth
val retroBgDark = Color(0xFF1E1E32)         // Deep blue-gray, easier on eyes than pure black
val retroSurfaceLight = Color(0xFFFFFFFF)   // White
val retroSurfaceDark = Color(0xFF2D2D44)    // Deep gray-purple
val retroOnBgLight = Color(0xFF212121)      // Very dark gray for text
val retroOnBgDark = Color(0xFFE0E0E0)       // Light gray for text in dark mode

// Accent colors for charts and different categories
val retroChartBlue = Color(0xFF5C6BC0)      // Softer blue
val retroChartRed = Color(0xFFEF5350)       // Softer red
val retroChartGreen = Color(0xFF66BB6A)     // Softer green
val retroChartYellow = Color(0xFFFFD54F)    // Softer yellow
val retroChartPurple = Color(0xFFAB47BC)    // Softer purple

val SecondaryLight = Color(0xFFF5A742)
val OnSecondaryLight = Color(0xFF000000)
val SecondaryContainerLight = Color(0xFFFFDDB3)
val OnSecondaryContainerLight = Color(0xFF261A00)

val TertiaryLight = Color(0xFF9256D9)
val OnTertiaryLight = Color(0xFFFFFFFF)
val TertiaryContainerLight = Color(0xFFEADDFF)
val OnTertiaryContainerLight = Color(0xFF22005D)

val BackgroundLight = Color(0xFFF5F7FA)
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

val ChartRed = retroChartRed
val ChartBlue = retroChartBlue
val ChartOrange = retroChartYellow
val ChartPurple = retroChartPurple
val ChartGridColor = Color.Gray.copy(alpha = 0.2f)

// Note Tag Colors (refined)
val TagDiet = Color(0xFFEF5350)
val TagMedication = Color(0xFF06A3B7)
val TagHealth = Color(0xFF66BB6A)
val TagMisc = Color(0xFFF5A742)