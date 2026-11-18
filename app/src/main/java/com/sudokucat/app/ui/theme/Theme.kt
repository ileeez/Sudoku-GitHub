package com.sudokucat.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sudokucat.app.data.prefs.ThemeMode

data class AppColors(
    val fish: Color,
    val fishDark: Color,
    val success: Color,
    val warning: Color,
    val danger: Color,
    val lavender: Color,
    val mint: Color,
    val pink: Color,
    val line: Color,
    val muted: Color,
    val highlight: Color,
    val peerHighlight: Color,
    val sameNumber: Color,
    val gridBackground: Color,
    val catShadow: Color
)

private val LightAppColors = AppColors(
    fish = CatPalette.Amber,
    fishDark = Color(0xFFD49A3F),
    success = Color(0xFF4F9C7E),
    warning = Color(0xFFD2923F),
    danger = CatPalette.Error,
    lavender = CatPalette.Lavender,
    mint = CatPalette.Mint,
    pink = CatPalette.Pink,
    line = Color(0xFFE0DAEC),
    muted = Color(0xFF777183),
    highlight = CatPalette.LavenderSoft,
    peerHighlight = Color(0x0D7367C9),
    sameNumber = Color(0x1F7367C9),
    gridBackground = CatPalette.Paper,
    catShadow = Color(0x1F17151F)
)

private val DarkAppColors = AppColors(
    fish = Color(0xFFF4CC80),
    fishDark = Color(0xFFC79647),
    success = Color(0xFF8BD4B7),
    warning = Color(0xFFF0C070),
    danger = Color(0xFFF08C8C),
    lavender = CatPalette.Lavender,
    mint = CatPalette.Mint,
    pink = CatPalette.Pink,
    line = CatPalette.NightLine,
    muted = CatPalette.NightMuted,
    highlight = Color(0x335E52A8),
    peerHighlight = Color(0x14C8BDF5),
    sameNumber = Color(0x26C8BDF5),
    gridBackground = CatPalette.NightSurface,
    catShadow = Color(0x66000000)
)

val LocalAppColors = staticCompositionLocalOf { LightAppColors }

object CatTheme {
    val colors: AppColors
        @Composable get() = LocalAppColors.current
}

private val LightScheme = lightColorScheme(
    primary = CatPalette.Purple,
    onPrimary = Color.White,
    primaryContainer = CatPalette.LavenderSoft,
    onPrimaryContainer = CatPalette.PurpleDeep,
    secondary = CatPalette.Pink,
    onSecondary = CatPalette.Charcoal,
    secondaryContainer = CatPalette.PinkSoft,
    onSecondaryContainer = CatPalette.Charcoal,
    tertiary = CatPalette.Mint,
    onTertiary = CatPalette.Charcoal,
    tertiaryContainer = CatPalette.MintSoft,
    onTertiaryContainer = CatPalette.Charcoal,
    background = CatPalette.WarmWhite,
    onBackground = CatPalette.Charcoal,
    surface = CatPalette.Paper,
    onSurface = CatPalette.Charcoal,
    surfaceVariant = Color(0xFFF1ECF7),
    onSurfaceVariant = Color(0xFF625C70),
    outline = Color(0xFFB8B1C5),
    outlineVariant = Color(0xFFE6E0EF),
    error = CatPalette.Error,
    onError = Color.White,
    errorContainer = Color(0xFFFBE3E3),
    onErrorContainer = Color(0xFF7A2525)
)

private val DarkScheme = darkColorScheme(
    primary = CatPalette.Lavender,
    onPrimary = CatPalette.Charcoal,
    primaryContainer = Color(0xFF403877),
    onPrimaryContainer = Color(0xFFEDE8FF),
    secondary = CatPalette.Pink,
    onSecondary = CatPalette.Charcoal,
    secondaryContainer = Color(0xFF533540),
    onSecondaryContainer = Color(0xFFFBE6EB),
    tertiary = CatPalette.Mint,
    onTertiary = CatPalette.Charcoal,
    tertiaryContainer = Color(0xFF28473E),
    onTertiaryContainer = Color(0xFFE2F5EE),
    background = CatPalette.Night,
    onBackground = Color(0xFFF4F0FF),
    surface = CatPalette.NightSurface,
    onSurface = Color(0xFFF4F0FF),
    surfaceVariant = CatPalette.NightElevated,
    onSurfaceVariant = Color(0xFFC6C0D2),
    outline = Color(0xFF676173),
    outlineVariant = CatPalette.NightLine,
    error = Color(0xFFF08C8C),
    onError = CatPalette.Night,
    errorContainer = Color(0xFF572C2C),
    onErrorContainer = Color(0xFFFFDAD6)
)

val CatShapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(36.dp)
)

@Composable
fun SudokuCatTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val scheme = if (dark) DarkScheme else LightScheme
    val appColors = if (dark) DarkAppColors else LightAppColors
    CompositionLocalProvider(LocalAppColors provides appColors) {
        MaterialTheme(
            colorScheme = scheme,
            typography = CatTypography,
            shapes = CatShapes,
            content = content
        )
    }
}
