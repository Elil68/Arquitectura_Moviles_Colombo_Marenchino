package com.example.tp1.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = SlateBluePrimaryDark,
    onPrimary = SlateBlueOnPrimaryDark,
    primaryContainer = SlateBluePrimaryContainerDark,
    onPrimaryContainer = SlateBlueOnPrimaryContainerDark,
    secondary = SlateBlueSecondaryDark,
    onSecondary = SlateBlueOnSecondaryDark,
    secondaryContainer = SlateBlueSecondaryContainerDark,
    onSecondaryContainer = SlateBlueOnSecondaryContainerDark,
    tertiary = SlateBlueTertiaryDark,
    onTertiary = SlateBlueOnTertiaryDark,
    background = SlateBlueBackgroundDark,
    onBackground = SlateBlueOnBackgroundDark,
    surface = SlateBlueSurfaceDark,
    onSurface = SlateBlueOnSurfaceDark,
    surfaceVariant = SlateBlueSurfaceVariantDark,
    onSurfaceVariant = SlateBlueOnSurfaceVariantDark,
    outline = SlateBlueOutlineDark,
    error = SlateBlueErrorDark,
    onError = SlateBlueOnErrorDark
)

private val LightColorScheme = lightColorScheme(
    primary = SlateBluePrimaryLight,
    onPrimary = SlateBlueOnPrimaryLight,
    primaryContainer = SlateBluePrimaryContainerLight,
    onPrimaryContainer = SlateBlueOnPrimaryContainerLight,
    secondary = SlateBlueSecondaryLight,
    onSecondary = SlateBlueOnSecondaryLight,
    secondaryContainer = SlateBlueSecondaryContainerLight,
    onSecondaryContainer = SlateBlueOnSecondaryContainerLight,
    tertiary = SlateBlueTertiaryLight,
    onTertiary = SlateBlueOnTertiaryLight,
    background = SlateBlueBackgroundLight,
    onBackground = SlateBlueOnBackgroundLight,
    surface = SlateBlueSurfaceLight,
    onSurface = SlateBlueOnSurfaceLight,
    surfaceVariant = SlateBlueSurfaceVariantLight,
    onSurfaceVariant = SlateBlueOnSurfaceVariantLight,
    outline = SlateBlueOutlineLight,
    error = SlateBlueErrorLight,
    onError = SlateBlueOnErrorLight
)

@Composable
fun TP1Theme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Paleta propia "Azul corporativo": desactivado por defecto para no perder
    // la identidad visual con el color dinámico del wallpaper (Android 12+).
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
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
