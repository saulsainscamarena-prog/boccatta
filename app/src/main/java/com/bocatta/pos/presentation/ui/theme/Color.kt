package com.bocatta.pos.presentation.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val m3Primary = mapOf(
    0 to Color(0xFF000000), 10 to Color(0xFF1B5E20),
    20 to Color(0xFF2E7D32), 30 to Color(0xFF388E3C),
    40 to Color(0xFF43A047), 50 to Color(0xFF4CAF50),
    60 to Color(0xFF66BB6A), 70 to Color(0xFF81C784),
    80 to Color(0xFFA5D6A7), 90 to Color(0xFFC8E6C9),
    95 to Color(0xFFE8F5E9), 99 to Color(0xFFF1F8E9),
    100 to Color(0xFFFFFFFF)
)

private val m3Secondary = mapOf(
    0 to Color(0xFF000000), 10 to Color(0xFF1D192B),
    20 to Color(0xFF332D41), 30 to Color(0xFF4A4458),
    40 to Color(0xFF625B71), 50 to Color(0xFF7A7389),
    60 to Color(0xFF958DA0), 70 to Color(0xFFB0A7BC),
    80 to Color(0xFFCCC2DC), 90 to Color(0xFFE8DEF8),
    95 to Color(0xFFF6EDFF), 99 to Color(0xFFFFFBFE),
    100 to Color(0xFFFFFFFF)
)

private val m3Tertiary = mapOf(
    0 to Color(0xFF000000), 10 to Color(0xFF31111D),
    20 to Color(0xFF492532), 30 to Color(0xFF633B48),
    40 to Color(0xFF7D5260), 50 to Color(0xFF986977),
    60 to Color(0xFFB58392), 70 to Color(0xFFD29DAC),
    80 to Color(0xFFEFB8C8), 90 to Color(0xFFFFD9E4),
    95 to Color(0xFFFFECF1), 99 to Color(0xFFFFFBFE),
    100 to Color(0xFFFFFFFF)
)

private val m3Neutral = mapOf(
    0 to Color(0xFF000000), 10 to Color(0xFF1C1B1F),
    20 to Color(0xFF313033), 30 to Color(0xFF484649),
    40 to Color(0xFF605D62), 50 to Color(0xFF79767A),
    60 to Color(0xFF939094), 70 to Color(0xFFAEAAAE),
    80 to Color(0xFFC9C5CA), 90 to Color(0xFFE6E1E5),
    95 to Color(0xFFF4EFF4), 99 to Color(0xFFFFFBFE),
    100 to Color(0xFFFFFFFF)
)

private val m3NeutralVariant = mapOf(
    0 to Color(0xFF000000), 10 to Color(0xFF1D1A22),
    20 to Color(0xFF322F37), 30 to Color(0xFF49454F),
    40 to Color(0xFF605D66), 50 to Color(0xFF79747E),
    60 to Color(0xFF938F99), 70 to Color(0xFFAEA9B4),
    80 to Color(0xFFCAC4D0), 90 to Color(0xFFE7E0EC),
    95 to Color(0xFFF5EEFA), 99 to Color(0xFFFFFBFE),
    100 to Color(0xFFFFFFFF)
)

private val m3Error = mapOf(
    0 to Color(0xFF000000), 10 to Color(0xFF410E0B),
    20 to Color(0xFF601410), 30 to Color(0xFF8C1D18),
    40 to Color(0xFFB3261E), 50 to Color(0xFFDC362E),
    60 to Color(0xFFE46962), 70 to Color(0xFFEC928E),
    80 to Color(0xFFF2B8B5), 90 to Color(0xFFF9DED7),
    95 to Color(0xFFFCEEE8), 99 to Color(0xFFFFFBFE),
    100 to Color(0xFFFFFFFF)
)

val LightColorScheme = lightColorScheme(
    primary = m3Primary[40]!!,
    onPrimary = Color.White,
    primaryContainer = m3Primary[90]!!,
    onPrimaryContainer = m3Primary[10]!!,
    secondary = m3Secondary[40]!!,
    onSecondary = Color.White,
    secondaryContainer = m3Secondary[90]!!,
    onSecondaryContainer = m3Secondary[10]!!,
    tertiary = m3Tertiary[40]!!,
    onTertiary = Color.White,
    tertiaryContainer = m3Tertiary[90]!!,
    onTertiaryContainer = m3Tertiary[10]!!,
    error = m3Error[40]!!,
    onError = Color.White,
    errorContainer = m3Error[90]!!,
    onErrorContainer = m3Error[10]!!,
    background = m3Neutral[99]!!,
    onBackground = m3Neutral[10]!!,
    surface = m3Neutral[99]!!,
    onSurface = m3Neutral[10]!!,
    surfaceVariant = m3NeutralVariant[90]!!,
    onSurfaceVariant = m3NeutralVariant[30]!!,
    outline = m3NeutralVariant[50]!!,
    outlineVariant = m3NeutralVariant[80]!!,
    inverseSurface = m3Neutral[20]!!,
    inverseOnSurface = m3Neutral[95]!!,
    inversePrimary = m3Primary[80]!!,
    surfaceTint = m3Primary[40]!!,
    scrim = Color.Black
)

val DarkColorScheme = darkColorScheme(
    primary = m3Primary[80]!!,
    onPrimary = m3Primary[20]!!,
    primaryContainer = m3Primary[30]!!,
    onPrimaryContainer = m3Primary[90]!!,
    secondary = m3Secondary[80]!!,
    onSecondary = m3Secondary[20]!!,
    secondaryContainer = m3Secondary[30]!!,
    onSecondaryContainer = m3Secondary[90]!!,
    tertiary = m3Tertiary[80]!!,
    onTertiary = m3Tertiary[20]!!,
    tertiaryContainer = m3Tertiary[30]!!,
    onTertiaryContainer = m3Tertiary[90]!!,
    error = m3Error[80]!!,
    onError = m3Error[20]!!,
    errorContainer = m3Error[30]!!,
    onErrorContainer = m3Error[90]!!,
    background = m3Neutral[10]!!,
    onBackground = m3Neutral[90]!!,
    surface = m3Neutral[10]!!,
    onSurface = m3Neutral[90]!!,
    surfaceVariant = m3NeutralVariant[30]!!,
    onSurfaceVariant = m3NeutralVariant[80]!!,
    outline = m3NeutralVariant[60]!!,
    outlineVariant = m3NeutralVariant[30]!!,
    inverseSurface = m3Neutral[90]!!,
    inverseOnSurface = m3Neutral[20]!!,
    inversePrimary = m3Primary[40]!!,
    surfaceTint = m3Primary[80]!!,
    scrim = Color.Black
)

@Composable
fun getColorScheme(
    darkTheme: Boolean,
    dynamicColor: Boolean
): ColorScheme {
    if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val context = LocalContext.current
        return if (darkTheme) dynamicDarkColorScheme(context)
        else dynamicLightColorScheme(context)
    }
    return if (darkTheme) DarkColorScheme else LightColorScheme
}

// Aliases para retrocompatibilidad durante la migración
val BocattaLightColorScheme get() = LightColorScheme
val BocattaDarkColorScheme get() = DarkColorScheme
@Composable fun getBocattaColorScheme(darkTheme: Boolean, dynamicColor: Boolean) = getColorScheme(darkTheme, dynamicColor)

val BocattaPrimary      = Color(0xFF1B5E20)
val BocattaPrimaryDark  = Color(0xFF4CAF50)
val BocattaSecondary    = Color(0xFF263238)

val BocattaNeonMagenta  = Color(0xFFFF4081)
val BocattaNeonCyan     = Color(0xFF00E5FF)
val BocattaNeonGreen    = Color(0xFF00FFD1)

val BocattaSuccess      = Color(0xFF43A047)
val BocattaWarning      = Color(0xFFFB8C00)
val BocattaDanger       = Color(0xFFE53935)

val BocattaBg           = Color(0xFFF5F6F8)
val BocattaSurface      = Color(0xFFFFFFFF)
val BocattaOnSurface    = Color(0xFF1A1A1A)
val BocattaSubtext      = Color(0xFF6C757D)

val BocattaBgDark       = Color(0xFF08090F)
val BocattaSurfaceDark  = Color(0xFF121212)
val BocattaOnSurfaceDark = Color(0xFFECECEC)
val BocattaGlass        = Color(0x33FFFFFF)
val BocattaGlassDark    = Color(0x33000000)

val CatCrepa    = Color(0xFFFF2D55)
val CatSnack    = Color(0xFFFF5E3A)
val CatPostre   = Color(0xFFBF5AF2)
val CatCombo    = Color(0xFF0A84FF)
val CatBebida   = Color(0xFF64D2FF)
val CatDefault  = Color(0xFF8E8E93)

val BocattaGlassBorder  = Color(0x26FFFFFF)
val BocattaGlowPrimary  = Color(0x40D81B60)
val BocattaSpaceBlue    = Color(0xFF1B1E2E)
val BocattaAccentNeon   = Color(0xFF00FFD1)

val GradientPrimary = listOf(BocattaPrimary, Color(0xFF8E24AA))
val GradientDark = listOf(BocattaBgDark, Color(0xFF1A1C29))
val GradientNeon = listOf(BocattaNeonMagenta, BocattaNeonCyan)
val GradientIndustrial = listOf(Color(0xFF2C3E50), Color(0xFF000000))
val GradientGlass = listOf(Color.White.copy(0.1f), Color.White.copy(0.02f))
