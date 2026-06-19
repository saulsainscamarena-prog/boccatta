package com.bocatta.pos.presentation.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class BocattaSemanticColors(
    val success: Color,
    val onSuccess: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
    val warning: Color,
    val onWarning: Color,
    val warningContainer: Color,
    val onWarningContainer: Color
)

internal val LightBocattaSemanticColors = BocattaSemanticColors(
    success = Color(0xFF2E7D32),
    onSuccess = Color(0xFFFFFFFF),
    successContainer = Color(0xFFC8E6C9),
    onSuccessContainer = Color(0xFF0B3D10),
    warning = Color(0xFF9A4D00),
    onWarning = Color(0xFFFFFFFF),
    warningContainer = Color(0xFFFFDDBA),
    onWarningContainer = Color(0xFF321400)
)

internal val DarkBocattaSemanticColors = BocattaSemanticColors(
    success = Color(0xFF81C784),
    onSuccess = Color(0xFF0B3D10),
    successContainer = Color(0xFF1B5E20),
    onSuccessContainer = Color(0xFFC8E6C9),
    warning = Color(0xFFFFB870),
    onWarning = Color(0xFF4A2800),
    warningContainer = Color(0xFF6D3900),
    onWarningContainer = Color(0xFFFFDDBA)
)

internal val LocalBocattaSemanticColors = staticCompositionLocalOf {
    LightBocattaSemanticColors
}

val MaterialTheme.bocattaSemanticColors: BocattaSemanticColors
    @Composable
    @ReadOnlyComposable
    get() = LocalBocattaSemanticColors.current
