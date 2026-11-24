package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val YaPulseDarkColorScheme = darkColorScheme(
    primary = YandexAmber,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF332900),
    onPrimaryContainer = YandexAmber,
    secondary = CoralPulse,
    onSecondary = Color.White,
    tertiary = EmeraldGlow,
    onTertiary = Color.Black,
    background = AmoledBlack,
    onBackground = SoftWhite,
    surface = ElevatedObsidian,
    onSurface = SoftWhite,
    surfaceVariant = CardGlass,
    onSurfaceVariant = MutedSlate,
    outline = CardGlassBorder,
    error = DangerCoral,
    onError = Color.White
)

@Composable
fun YaPulseTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = YaPulseDarkColorScheme,
        typography = YaPulseTypography,
        content = content
    )
}
