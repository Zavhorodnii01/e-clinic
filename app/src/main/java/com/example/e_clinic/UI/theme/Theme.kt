package com.example.e_clinic.UI.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.e_clinic.UI.theme.EClinicDarkTheme
import com.example.e_clinic.UI.theme.EClinicLightTheme


// Define your primary colors
val DarkBlue = Color(0xFF0D1B2A)  // Elegant dark blue
val Blue = Color(0xFF1B263B)      // Deep blue
val LightBlue = Color(0xFF415A77) // Soft blue
val White = Color(0xFFFFFFFF)      // Pure white
val Black = Color(0xFF000000)      // Black

private val DarkColorScheme = darkColorScheme(
    primary = DarkBlue,
    secondary = Blue,
    tertiary = LightBlue,
    background = Black,
    surface = DarkBlue,
    onPrimary = White,
    onSecondary = White,
    onTertiary = White,
    onBackground = White,
    onSurface = White,
)

private val LightColorScheme = lightColorScheme(
    primary = Blue,
    secondary = LightBlue,
    tertiary = DarkBlue,
    background = White,
    surface = LightBlue,
    onPrimary = Black,
    onSecondary = Black,
    onTertiary = Black,
    onBackground = Black,
    onSurface = Black,
)

@Composable
fun EClinicTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) EClinicDarkTheme else EClinicLightTheme
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
