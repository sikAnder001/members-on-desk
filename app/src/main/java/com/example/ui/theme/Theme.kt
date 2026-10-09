package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    onPrimary = Color(0xFF381E72),
    primaryContainer = Color(0xFF5A259F),
    onPrimaryContainer = Color(0xFFEADDFF),
    secondary = Pink80,
    onSecondary = Color(0xFF561E47),
    secondaryContainer = Color(0xFF702B5F),
    onSecondaryContainer = Color(0xFFFFD7F0),
    tertiary = TertiaryPink,
    background = Color(0xFF140E1F),
    surface = Color(0xFF1E142F),
    surfaceVariant = Color(0xFF32234D),
    onSurface = Color(0xFFF3EDFD),
    onSurfaceVariant = Color(0xFFD0BCFF)
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryPurple,               // Vibrant Purple #7C3AED
    onPrimary = Color.White,
    primaryContainer = PrimaryPurpleContainer, // #EDE9FE
    onPrimaryContainer = OnPrimaryPurpleContainer,
    secondary = SecondaryMagenta,          // Vibrant Magenta #C026D3
    onSecondary = Color.White,
    secondaryContainer = SecondaryMagentaContainer, // #FAE8FF
    onSecondaryContainer = OnSecondaryMagentaContainer,
    tertiary = TertiaryPink,               // Magenta/Pink #DB2777
    onTertiary = Color.White,
    tertiaryContainer = TertiaryPinkContainer,
    onTertiaryContainer = OnTertiaryPinkContainer,
    background = Color(0xFFFAF7FF),
    surface = Color.White,
    surfaceVariant = Color(0xFFF3E8FF),
    onBackground = Color(0xFF1E1236),
    onSurface = Color(0xFF1E1236),
    onSurfaceVariant = Color(0xFF6B5A8A)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Always enforce our custom Purple/Magenta theme instead of device wallpaper
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
