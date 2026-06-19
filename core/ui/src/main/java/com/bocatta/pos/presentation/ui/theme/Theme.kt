package com.bocatta.pos.presentation.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.WindowCompat
import com.bocatta.pos.domain.model.ThemeConfigV2

@Composable
fun M3Theme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    themeConfig: ThemeConfigV2? = null,
    content: @Composable () -> Unit
) {
    val colorScheme = getColorScheme(
        darkTheme = darkTheme,
        dynamicColor = dynamicColor,
        themeConfig = themeConfig
    )

    val view = androidx.compose.ui.platform.LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? android.app.Activity)?.window ?: return@SideEffect
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    CompositionLocalProvider(
        LocalBocattaSemanticColors provides if (darkTheme) {
            DarkBocattaSemanticColors
        } else {
            LightBocattaSemanticColors
        }
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = M3Typography,
            shapes = M3Shapes,
            content = content
        )
    }
}

@Composable
fun BocattaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    themeConfig: ThemeConfigV2? = null,
    content: @Composable () -> Unit
) {
    M3Theme(
        darkTheme = darkTheme,
        dynamicColor = dynamicColor,
        themeConfig = themeConfig,
        content = content
    )
}
