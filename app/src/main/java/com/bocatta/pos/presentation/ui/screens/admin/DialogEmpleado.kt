package com.bocatta.pos.presentation.ui.screens.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bocatta.pos.domain.model.Usuario

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialogEmpleado(
    user: Usuario,
    onDismiss: () -> Unit,
    onSave: (Usuario) -> Unit
) {
    var tabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Datos", "Horario", "Permisos", "Salario", "Zona")
    var nombre by remember { mutableStateOf(user.nombre) }
    var rol by remember { mutableStateOf(user.rol.name) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("EDITAR EMPLEADO", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                SecondaryTabRow(selectedTabIndex = tabIndex) {
                    tabs.forEachIndexed { i, label ->
                        Tab(selected = tabIndex == i, onClick = { tabIndex = i }, text = { Text(label, fontSize = 11.sp) })
                    }
                }
                Spacer(Modifier.height(16.dp))
                when (tabIndex) {
                    0 -> {
                        OutlinedTextField(value = nombre, onValueChange = { nombre = it }, label = { Text("Nombre") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("Rol:", style = MaterialTheme.typography.labelSmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("ADMIN", "VENDEDOR", "COCINERO", "GERENTE").forEach { r ->
                                FilterChip(selected = rol == r, onClick = { rol = r }, label = { Text(r, fontSize = 11.sp) })
                            }
                        }
                    }
                    1 -> Text("Configura horario desde la sección de Turnos.", color = MaterialTheme.colorScheme.outline, fontSize = 12.sp)
                    2 -> Text("Los permisos se gestionan desde la configuración del empleado.", color = MaterialTheme.colorScheme.outline, fontSize = 12.sp)
                    3 -> Text("Configura el salario desde la pestaña Sueldos.", color = MaterialTheme.colorScheme.outline, fontSize = 12.sp)
                    4 -> Text("Asigna la zona desde la pestaña Zonas.", color = MaterialTheme.colorScheme.outline, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(user.copy(nombre = nombre))
                onDismiss()
            }, enabled = nombre.isNotBlank()) { Text("GUARDAR", fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
        shape = RoundedCornerShape(20.dp)
    )
}

