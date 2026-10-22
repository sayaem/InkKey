package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = InkPrimaryDark,
    onPrimary = InkOnPrimaryDark,
    primaryContainer = InkPrimaryContainerDark,
    onPrimaryContainer = InkOnPrimaryContainerDark,
    secondary = InkSecondaryDark,
    surface = InkSurfaceDark,
    onSurface = InkOnSurfaceDark,
    surfaceVariant = InkSurfaceVariantDark,
    onSurfaceVariant = InkOnSurfaceVariantDark,
    background = InkBackgroundDark,
    onBackground = InkOnSurfaceDark,
    outline = InkOutlineDark
)

private val LightColorScheme = lightColorScheme(
    primary = InkPrimaryLight,
    onPrimary = InkOnPrimaryLight,
    primaryContainer = InkPrimaryContainerLight,
    onPrimaryContainer = InkOnPrimaryContainerLight,
    secondary = InkSecondaryLight,
    surface = InkSurfaceLight,
    onSurface = InkOnSurfaceLight,
    surfaceVariant = InkSurfaceVariantLight,
    onSurfaceVariant = InkOnSurfaceVariantLight,
    background = InkBackgroundLight,
    onBackground = InkOnSurfaceLight,
    outline = InkOutlineLight
)

@Composable
fun InkKeyTheme(
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
