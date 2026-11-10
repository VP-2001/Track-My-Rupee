package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

enum class AppThemeMode(val displayName: String) {
    SYSTEM("System default"),
    DARK("Dark theme"),
    LIGHT("Light theme")
}

@Immutable
data class ExtendedColors(
    val income: Color,
    val incomeContainer: Color,
    val warning: Color,
    val warningContainer: Color,
    val danger: Color,
    val dangerContainer: Color,
    val accentGold: Color
)

val LocalExtendedColors = staticCompositionLocalOf {
    ExtendedColors(
        income = DarkIncome,
        incomeContainer = DarkIncomeContainer,
        warning = DarkWarning,
        warningContainer = DarkWarningContainer,
        danger = DarkDanger,
        dangerContainer = DarkDangerContainer,
        accentGold = DarkAccentGold
    )
}

val MaterialTheme.extendedColors: ExtendedColors
    @Composable
    get() = LocalExtendedColors.current

private val BrandDarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = DarkPrimary,
    onSecondary = DarkOnPrimary,
    secondaryContainer = DarkSurfaceVariant,
    onSecondaryContainer = DarkOnSurface,
    tertiary = DarkAccentGold,
    onTertiary = Color(0xFF422800),
    tertiaryContainer = DarkWarningContainer,
    onTertiaryContainer = Color(0xFFFFD54F),
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    surfaceDim = DarkSurfaceDim,
    onBackground = DarkOnSurface,
    onSurface = DarkOnSurface,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    outlineVariant = DarkOutline.copy(alpha = 0.6f),
    error = DarkDanger,
    errorContainer = DarkDangerContainer,
    onError = Color(0xFF601410),
    onErrorContainer = DarkOnDangerContainer
)

private val BrandLightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,
    secondary = LightPrimary,
    onSecondary = LightOnPrimary,
    secondaryContainer = LightSurfaceVariant,
    onSecondaryContainer = LightOnSurface,
    tertiary = LightAccentGold,
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = LightWarningContainer,
    onTertiaryContainer = LightWarning,
    background = LightBackground,
    surface = LightSurface,
    surfaceVariant = LightSurfaceVariant,
    surfaceDim = LightSurfaceDim,
    onBackground = LightOnSurface,
    onSurface = LightOnSurface,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
    outlineVariant = LightOutline.copy(alpha = 0.6f),
    error = LightDanger,
    errorContainer = LightDangerContainer,
    onError = Color(0xFFFFFFFF),
    onErrorContainer = LightOnDangerContainer
)

@Composable
fun MyApplicationTheme(
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
        AppThemeMode.DARK -> true
        AppThemeMode.LIGHT -> false
    }

    val colorScheme = if (isDark) BrandDarkColorScheme else BrandLightColorScheme
    val extendedColors = if (isDark) {
        ExtendedColors(
            income = DarkIncome,
            incomeContainer = DarkIncomeContainer,
            warning = DarkWarning,
            warningContainer = DarkWarningContainer,
            danger = DarkDanger,
            dangerContainer = DarkDangerContainer,
            accentGold = DarkAccentGold
        )
    } else {
        ExtendedColors(
            income = LightIncome,
            incomeContainer = LightIncomeContainer,
            warning = LightWarning,
            warningContainer = LightWarningContainer,
            danger = LightDanger,
            dangerContainer = LightDangerContainer,
            accentGold = LightAccentGold
        )
    }

    CompositionLocalProvider(LocalExtendedColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
