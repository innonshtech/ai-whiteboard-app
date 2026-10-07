package com.avitrisphare.whiteboard.core.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

val LocalAvitriSphareColors = staticCompositionLocalOf { AvitriSphareColors.Dark }

private val DarkColorScheme = darkColorScheme(
    primary = AvitriSphareColors.Dark.brandGreen,
    secondary = AvitriSphareColors.Dark.actionGreen,
    background = AvitriSphareColors.Dark.backgroundDark,
    surface = AvitriSphareColors.Dark.surfaceDark,
    onPrimary = AvitriSphareColors.Dark.textPrimaryDark,
    onSecondary = AvitriSphareColors.Dark.textPrimaryDark,
    onBackground = AvitriSphareColors.Dark.textPrimaryDark,
    onSurface = AvitriSphareColors.Dark.textPrimaryDark,
    surfaceVariant = AvitriSphareColors.Dark.backgroundDark,
    onSurfaceVariant = AvitriSphareColors.Dark.textMutedDark,
    outline = AvitriSphareColors.Dark.borderStandardDark
)

private val LightColorScheme = lightColorScheme(
    primary = AvitriSphareColors.Light.brandGreen,
    secondary = AvitriSphareColors.Light.actionGreen,
    background = AvitriSphareColors.Light.backgroundLight,
    surface = AvitriSphareColors.Light.surfaceLight,
    onPrimary = AvitriSphareColors.Light.textPrimaryLight,
    onSecondary = AvitriSphareColors.Light.textPrimaryLight,
    onBackground = AvitriSphareColors.Light.textPrimaryLight,
    onSurface = AvitriSphareColors.Light.textPrimaryLight,
    surfaceVariant = AvitriSphareColors.Light.backgroundLight,
    onSurfaceVariant = AvitriSphareColors.Light.textMutedLight,
    outline = AvitriSphareColors.Light.borderStandardLight
)

@Composable
fun AvitriSphareTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) AvitriSphareColors.Dark else AvitriSphareColors.Light
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    CompositionLocalProvider(LocalAvitriSphareColors provides colors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AvitriSphareTypography,
            content = content
        )
    }
}
