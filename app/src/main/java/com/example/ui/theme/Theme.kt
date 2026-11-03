package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val LyricLabDarkColorScheme = darkColorScheme(
    primary = ElectricVioletLight,
    onPrimary = StudioBlack,
    primaryContainer = ElectricViolet,
    onPrimaryContainer = TextPrimary,
    secondary = NeonCyan,
    onSecondary = StudioBlack,
    secondaryContainer = StudioSurfaceElevated,
    onSecondaryContainer = NeonCyan,
    tertiary = ElectricBlue,
    onTertiary = TextPrimary,
    background = StudioBlack,
    onBackground = TextPrimary,
    surface = StudioGraphite,
    onSurface = TextPrimary,
    surfaceVariant = StudioSurface,
    onSurfaceVariant = TextSecondary,
    outline = StudioBorder,
    error = MetricRed,
    onError = TextPrimary
)

@Composable
fun LyricLabTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LyricLabDarkColorScheme,
        typography = Typography,
        content = content
    )
}
