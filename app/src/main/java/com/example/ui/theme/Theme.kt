package com.example.ui.theme

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

private val SophisticatedDarkColorScheme =
  darkColorScheme(
    primary = SophisticatedPrimary,
    onPrimary = SophisticatedOnPrimary,
    primaryContainer = SophisticatedPrimaryContainer,
    onPrimaryContainer = SophisticatedOnPrimaryContainer,
    secondary = SophisticatedSecondary,
    onSecondary = SophisticatedOnSecondary,
    secondaryContainer = SophisticatedSecondaryContainer,
    onSecondaryContainer = SophisticatedOnSecondaryContainer,
    tertiary = SophisticatedTertiary,
    onTertiary = SophisticatedOnTertiary,
    tertiaryContainer = SophisticatedTertiaryContainer,
    onTertiaryContainer = SophisticatedOnTertiaryContainer,
    background = SophisticatedDarkBackground,
    surface = SophisticatedDarkSurface,
    surfaceVariant = SophisticatedDarkSurfaceVariant,
    surfaceDim = SophisticatedDarkSurfaceDim,
    onBackground = SophisticatedDarkOnSurface,
    onSurface = SophisticatedDarkOnSurface,
    onSurfaceVariant = SophisticatedDarkOnSurfaceVariant,
    outline = SophisticatedDarkBorder,
    outlineVariant = SophisticatedDarkBorder.copy(alpha = 0.6f),
    error = DangerRed,
    errorContainer = DangerRedContainer,
    onError = Color(0xFF601410),
    onErrorContainer = DangerRedOnContainer
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true,
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  MaterialTheme(
    colorScheme = SophisticatedDarkColorScheme,
    typography = Typography,
    content = content
  )
}

