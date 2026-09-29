package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = CyanPrimary,
    onPrimary = DarkNavy900,
    primaryContainer = CyanContainer,
    onPrimaryContainer = OnCyanContainer,
    secondary = EmeraldSuccess,
    onSecondary = Color.Black,
    secondaryContainer = EmeraldContainer,
    onSecondaryContainer = OnEmerald,
    tertiary = AmberWarning,
    background = DarkNavy900,
    onBackground = SlateGray100,
    surface = DarkNavy800,
    onSurface = SlateGray100,
    surfaceVariant = DarkNavy700,
    onSurfaceVariant = SlateGray300,
    error = RedError,
    onError = Color.White,
    errorContainer = RedErrorContainer,
    outline = DarkNavy600
)

private val LightColorScheme = lightColorScheme(
    primary = CyanPrimaryDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0F7FA),
    onPrimaryContainer = Color(0xFF00363A),
    secondary = Color(0xFF059669),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD1FAE5),
    onSecondaryContainer = Color(0xFF064E3B),
    tertiary = Color(0xFFD97706),
    background = LightBackground,
    onBackground = TextDarkPrimary,
    surface = LightSurface,
    onSurface = TextDarkPrimary,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = TextDarkSecondary,
    error = RedError,
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2),
    outline = Color(0xFFCBD5E1)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
