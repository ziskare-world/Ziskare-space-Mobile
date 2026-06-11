package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val CosmicDarkColorScheme = darkColorScheme(
    primary = NeonCyan,
    secondary = RoyalPurple,
    tertiary = SystemSuccess,
    background = BackgroundDark,
    surface = SurfaceDark,
    error = SystemError,
    onPrimary = BackgroundDark,
    onSecondary = PureWhite,
    onBackground = PureWhite,
    onSurface = PureWhite,
    onTertiary = BackgroundDark
)

@Composable
fun MyApplicationTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = CosmicDarkColorScheme,
        typography = Typography,
        content = content
    )
}
