package com.bocatta.pos.presentation.ui.theme

import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.bocatta.pos.domain.model.ThemeConfigV2
import kotlin.math.roundToInt

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
    dynamicColor: Boolean,
    themeConfig: ThemeConfigV2? = null
): ColorScheme {
    if (themeConfig?.enabled == true) {
        return buildCustomColorScheme(themeConfig, darkTheme)
    }
    if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        return dynamicPlatformColorScheme(darkTheme)
    }
    return if (darkTheme) DarkColorScheme else LightColorScheme
}

@RequiresApi(Build.VERSION_CODES.S)
@SuppressLint("NewApi")
@Composable
private fun dynamicPlatformColorScheme(darkTheme: Boolean): ColorScheme {
    val context = LocalContext.current
    return if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
}

// Aliases para retrocompatibilidad durante la migración
val BocattaLightColorScheme get() = LightColorScheme
val BocattaDarkColorScheme get() = DarkColorScheme
@Composable fun getBocattaColorScheme(darkTheme: Boolean, dynamicColor: Boolean) = getColorScheme(darkTheme, dynamicColor)

fun previewCustomColorScheme(config: ThemeConfigV2, darkTheme: Boolean = true): ColorScheme {
    return buildCustomColorScheme(config.copy(enabled = true), darkTheme)
}

private fun buildCustomColorScheme(config: ThemeConfigV2, darkTheme: Boolean): ColorScheme {
    val primary = parseHexColor(config.primaryHex) ?: parseHexColor("#FFB394")!!
    val secondary = parseHexColor(config.secondaryHex) ?: parseHexColor("#5B4035")!!
    val tertiary = parseHexColor(config.tertiaryHex) ?: parseHexColor("#F2C078")!!

    return if (darkTheme) {
        DarkColorScheme.copy(
            primary = lighten(primary, 0.30f),
            onPrimary = readableOn(lighten(primary, 0.30f)),
            primaryContainer = darken(primary, 0.45f),
            onPrimaryContainer = lighten(primary, 0.72f),
            secondary = lighten(secondary, 0.34f),
            onSecondary = readableOn(lighten(secondary, 0.34f)),
            secondaryContainer = darken(secondary, 0.35f),
            onSecondaryContainer = lighten(secondary, 0.78f),
            tertiary = lighten(tertiary, 0.26f),
            onTertiary = readableOn(lighten(tertiary, 0.26f)),
            tertiaryContainer = darken(tertiary, 0.40f),
            onTertiaryContainer = lighten(tertiary, 0.74f),
            surfaceTint = lighten(primary, 0.30f),
            inversePrimary = darken(primary, 0.10f)
        )
    } else {
        LightColorScheme.copy(
            primary = darken(primary, 0.10f),
            onPrimary = readableOn(darken(primary, 0.10f)),
            primaryContainer = lighten(primary, 0.72f),
            onPrimaryContainer = darken(primary, 0.58f),
            secondary = darken(secondary, 0.08f),
            onSecondary = readableOn(darken(secondary, 0.08f)),
            secondaryContainer = lighten(secondary, 0.76f),
            onSecondaryContainer = darken(secondary, 0.58f),
            tertiary = darken(tertiary, 0.10f),
            onTertiary = readableOn(darken(tertiary, 0.10f)),
            tertiaryContainer = lighten(tertiary, 0.70f),
            onTertiaryContainer = darken(tertiary, 0.58f),
            surfaceTint = darken(primary, 0.10f),
            inversePrimary = lighten(primary, 0.34f)
        )
    }
}

fun parseHexColor(value: String): Color? {
    val clean = value.trim().removePrefix("#")
    if (!Regex("^[0-9a-fA-F]{6}$").matches(clean)) return null
    val argb = 0xFF000000 or clean.toLong(16)
    return Color(argb)
}

fun colorToHex(color: Color): String {
    val r = (color.red * 255).roundToInt().coerceIn(0, 255)
    val g = (color.green * 255).roundToInt().coerceIn(0, 255)
    val b = (color.blue * 255).roundToInt().coerceIn(0, 255)
    return "#%02X%02X%02X".format(r, g, b)
}

private fun readableOn(color: Color): Color {
    return if (relativeLuminance(color) > 0.48f) Color(0xFF1F1A17) else Color.White
}

private fun lighten(color: Color, amount: Float): Color = mix(color, Color.White, amount)
private fun darken(color: Color, amount: Float): Color = mix(color, Color.Black, amount)

private fun mix(start: Color, end: Color, amount: Float): Color {
    val a = amount.coerceIn(0f, 1f)
    return Color(
        red = start.red + (end.red - start.red) * a,
        green = start.green + (end.green - start.green) * a,
        blue = start.blue + (end.blue - start.blue) * a,
        alpha = 1f
    )
}

private fun relativeLuminance(color: Color): Float {
    fun channel(v: Float): Float {
        return if (v <= 0.03928f) v / 12.92f else {
            val adjusted = (v + 0.055f) / 1.055f
            adjusted * adjusted * adjusted
        }
    }
    return 0.2126f * channel(color.red) + 0.7152f * channel(color.green) + 0.0722f * channel(color.blue)
}
