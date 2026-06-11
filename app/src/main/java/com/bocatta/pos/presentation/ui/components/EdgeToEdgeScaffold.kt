package com.bocatta.pos.presentation.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Scaffold wrapper que consume [WindowInsets.safeDrawing] para soporte
 * edge-to-edge en toda la app.
 *
 * Usar en lugar de [Scaffold] directamente para evitar que el contenido
 * se superponga con la barra de estado / navegación.
 */
@Composable
fun EdgeToEdgeScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        modifier = modifier,
        topBar = topBar,
        bottomBar = bottomBar,
        floatingActionButton = floatingActionButton,
        contentWindowInsets = WindowInsets.safeDrawing,
        content = content
    )
}
