package com.bocatta.pos.presentation.ui.screens.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bocatta.pos.domain.model.Rol
import com.bocatta.pos.domain.model.Usuario

@Composable
fun DialogEmpleado(
    user: Usuario,
    onDismiss: () -> Unit,
    onSave: (Usuario) -> Unit
) {
    var nombre by remember { mutableStateOf(user.nombre) }
    // Solo roles válidos según el enum Rol del proyecto
    val rolesValidos = listOf(Rol.VENDEDOR, Rol.ADMIN, Rol.DUEÑO)
    var rolSeleccionado by remember { mutableStateOf(user.rol ?: Rol.VENDEDOR) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("EDITAR EMPLEADO", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                Text("Rol:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    rolesValidos.forEach { rol ->
                        FilterChip(
                            selected = rolSeleccionado == rol,
                            onClick = { rolSeleccionado = rol },
                            label = { Text(rol.name, fontSize = 11.sp) }
                        )
                    }
                }
                Text(
                    "Los permisos, horario y salario se configuran en sus respectivas pestañas.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(user.copy(nombre = nombre.trim(), rol = rolSeleccionado))
                    onDismiss()
                },
                enabled = nombre.isNotBlank()
            ) { Text("GUARDAR", fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
        shape = RoundedCornerShape(20.dp)
    )
}
