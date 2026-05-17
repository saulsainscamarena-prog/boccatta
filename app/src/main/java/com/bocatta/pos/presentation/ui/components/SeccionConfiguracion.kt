package com.bocatta.pos.presentation.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SeccionConfiguracion(
    titulo: String,
    icono: ImageVector = Icons.Default.Info,
    ayuda: String = "",
    ejemplos: String = "",
    contraejemplos: String = "",
    content: @Composable () -> Unit
) {
    var mostrarAyuda by remember { mutableStateOf(false) }

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(titulo, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            if (ayuda.isNotBlank()) {
                IconButton(onClick = { mostrarAyuda = !mostrarAyuda }, modifier = Modifier.size(20.dp)) {
                    Icon(icono, "Ayuda", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
        if (mostrarAyuda && ayuda.isNotBlank()) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(0.3f),
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text(ayuda, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.7f))
                    if (ejemplos.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text("✅ $ejemplos", fontSize = 10.sp, color = MaterialTheme.colorScheme.tertiary)
                    }
                    if (contraejemplos.isNotBlank()) {
                        Spacer(Modifier.height(2.dp))
                        Text("❌ $contraejemplos", fontSize = 10.sp, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        content()
    }
}

