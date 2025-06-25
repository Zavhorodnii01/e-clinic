package com.example.e_clinic.UI.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

val eClinicLightPrimary = Color(0xFF006A60) // A calming, rich teal
val eClinicLightOnPrimary = Color(0xFFFFFFFF)
val eClinicLightPrimaryContainer = Color(0xFF74F8E5)
val eClinicLightOnPrimaryContainer = Color(0xFF00201C)

val eClinicLightSecondary = Color(0xFF4A635F) // A slightly desaturated teal/grey
val eClinicLightOnSecondary = Color(0xFFFFFFFF)
val eClinicLightSecondaryContainer = Color(0xFFCCE8E3)
val eClinicLightOnSecondaryContainer = Color(0xFF051F1C)

val eClinicLightTertiary = Color(0xFF6B5F00) // A soft, muted gold/mustard for gentle warmth
val eClinicLightOnTertiary = Color(0xFFFFFFFF)
val eClinicLightTertiaryContainer = Color(0xFFF9E465)
val eClinicLightOnTertiaryContainer = Color(0xFF201C00)

val eClinicLightError = Color(0xFFBA1A1A)
val eClinicLightOnError = Color(0xFFFFFFFF)
val eClinicLightErrorContainer = Color(0xFFFFDAD6)
val eClinicLightOnErrorContainer = Color(0xFF410002)

val eClinicLightBackground = Color(0xFFFAFDFB) // Very light, slightly off-white
val eClinicLightOnBackground = Color(0xFF191C1B)
val eClinicLightSurface = Color(0xFFFAFDFB)
val eClinicLightOnSurface = Color(0xFF191C1B)
val eClinicLightSurfaceVariant = Color(0xFFDAE5E2) // For cards, dialogs - slightly different from background
val eClinicLightOnSurfaceVariant = Color(0xFF3F4947)
val eClinicLightOutline = Color(0xFF6F7977)

// Primary Palette (Dark)
val eClinicDarkPrimary = Color(0xFF53DBC9) // Lighter, vibrant teal for dark mode
val eClinicDarkOnPrimary = Color(0xFF003731)
val eClinicDarkPrimaryContainer = Color(0xFF005048)
val eClinicDarkOnPrimaryContainer = Color(0xFF74F8E5)

// Secondary Palette (Dark)
val eClinicDarkSecondary = Color(0xFFB0CCC7) // Lighter desaturated teal/grey
val eClinicDarkOnSecondary = Color(0xFF1C3531)
val eClinicDarkSecondaryContainer = Color(0xFF334B47)
val eClinicDarkOnSecondaryContainer = Color(0xFFCCE8E3)

// Tertiary Palette (Dark)
val eClinicDarkTertiary = Color(0xFFDCC84E) // Lighter muted gold/mustard
val eClinicDarkOnTertiary = Color(0xFF383100)
val eClinicDarkTertiaryContainer = Color(0xFF514700)
val eClinicDarkOnTertiaryContainer = Color(0xFFF9E465)

// Standard Colors (Dark)
val eClinicDarkError = Color(0xFFFFB4AB)
val eClinicDarkOnError = Color(0xFF690005)
val eClinicDarkErrorContainer = Color(0xFF93000A)
val eClinicDarkOnErrorContainer = Color(0xFFFFDAD6)

// Backgrounds and Surfaces (Dark): Comfortable, Not too stark
val eClinicDarkBackground = Color(0xFF191C1B) // Dark grey, slightly tinted
val eClinicDarkOnBackground = Color(0xFFE1E3E1)
val eClinicDarkSurface = Color(0xFF191C1B)
val eClinicDarkOnSurface = Color(0xFFE1E3E1)
val eClinicDarkSurfaceVariant = Color(0xFF3F4947) // Darker variant for cards
val eClinicDarkOnSurfaceVariant = Color(0xFFBEC9C6)
val eClinicDarkOutline = Color(0xFF899390)


val EClinicLightTheme = lightColorScheme(
    primary = eClinicLightPrimary,
    onPrimary = eClinicLightOnPrimary,
    primaryContainer = eClinicLightPrimaryContainer,
    onPrimaryContainer = eClinicLightOnPrimaryContainer,
    secondary = eClinicLightSecondary,
    onSecondary = eClinicLightOnSecondary,
    secondaryContainer = eClinicLightSecondaryContainer,
    onSecondaryContainer = eClinicLightOnSecondaryContainer,
    tertiary = eClinicLightTertiary,
    onTertiary = eClinicLightOnTertiary,
    tertiaryContainer = eClinicLightTertiaryContainer,
    onTertiaryContainer = eClinicLightOnTertiaryContainer,
    error = eClinicLightError,
    onError = eClinicLightOnError,
    errorContainer = eClinicLightErrorContainer,
    onErrorContainer = eClinicLightOnErrorContainer,
    background = eClinicLightBackground,
    onBackground = eClinicLightOnBackground,
    surface = eClinicLightSurface,
    onSurface = eClinicLightOnSurface,
    surfaceVariant = eClinicLightSurfaceVariant,
    onSurfaceVariant = eClinicLightOnSurfaceVariant,
    outline = eClinicLightOutline,
    inverseOnSurface = Color(0xFFEFF1EF), // Default Material inverse
    inverseSurface = Color(0xFF2E3130),   // Default Material inverse
    inversePrimary = Color(0xFF53DBC9),   // Lighter version of primary for dark on light
    surfaceTint = eClinicLightPrimary,
    outlineVariant = Color(0xFFBEC9C6), // Default Material
    scrim = Color(0xFF000000),          // Default Material
)

val EClinicDarkTheme = darkColorScheme(
    primary = eClinicDarkPrimary,
    onPrimary = eClinicDarkOnPrimary,
    primaryContainer = eClinicDarkPrimaryContainer,
    onPrimaryContainer = eClinicDarkOnPrimaryContainer,
    secondary = eClinicDarkSecondary,
    onSecondary = eClinicDarkOnSecondary,
    secondaryContainer = eClinicDarkSecondaryContainer,
    onSecondaryContainer = eClinicDarkOnSecondaryContainer,
    tertiary = eClinicDarkTertiary,
    onTertiary = eClinicDarkOnTertiary,
    tertiaryContainer = eClinicDarkTertiaryContainer,
    onTertiaryContainer = eClinicDarkOnTertiaryContainer,
    error = eClinicDarkError,
    onError = eClinicDarkOnError,
    errorContainer = eClinicDarkErrorContainer,
    onErrorContainer = eClinicDarkOnErrorContainer,
    background = eClinicDarkBackground,
    onBackground = eClinicDarkOnBackground,
    surface = eClinicDarkSurface,
    onSurface = eClinicDarkOnSurface,
    surfaceVariant = eClinicDarkSurfaceVariant,
    onSurfaceVariant = eClinicDarkOnSurfaceVariant,
    outline = eClinicDarkOutline,
    inverseOnSurface = Color(0xFF191C1B),   // Default Material inverse
    inverseSurface = Color(0xFFE1E3E1),     // Default Material inverse
    inversePrimary = Color(0xFF006A60),     // Original light primary for light on dark
    surfaceTint = eClinicDarkPrimary,
    outlineVariant = Color(0xFF3F4947),   // Default Material
    scrim = Color(0xFF000000),
)