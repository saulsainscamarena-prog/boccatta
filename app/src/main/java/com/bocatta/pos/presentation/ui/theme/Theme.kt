package com.bocatta.pos.presentation.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = BocattaPrimary,
    onPrimary = Color.White,
    secondary = BocattaSecondary,
    onSecondary = Color.White,
    background = BocattaBg,
    onBackground = BocattaOnSurface,
    surface = BocattaSurface,
    onSurface = BocattaOnSurface,
    error = BocattaDanger
)

private val DarkColorScheme = darkColorScheme(
    primary = BocattaNeonMagenta,
    onPrimary = Color.White,
    secondary = BocattaNeonCyan,
    onSecondary = Color.White,
    background = BocattaBgDark,
    onBackground = BocattaOnSurfaceDark,
    surface = BocattaSurfaceDark,
    onSurface = BocattaOnSurfaceDark,
    error = BocattaDanger,
    outline = Color.White.copy(alpha = 0.12f)
)

@Composable
fun BocattaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Forzamos Dark Theme para la estetica Premium si el usuario lo desea, 
    // pero respetamos la preferencia del sistema por ahora.
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = androidx.compose.ui.platform.LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? android.app.Activity)?.window ?: return@SideEffect
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
            window.statusBarColor = colorScheme.background.toArgb()
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
