package com.bocatta.pos.presentation.ui.screens.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bocatta.pos.domain.model.Usuario
import com.bocatta.pos.presentation.viewmodel.AdminViewModel

@Composable
fun TabEmpleados(vm: AdminViewModel) {
    var searchQuery by remember { mutableStateOf("") }
    var empleadoEditar by remember { mutableStateOf<Usuario?>(null) }

    // Dialogo de edicion — conectado a vm.actualizarUsuario
    empleadoEditar?.let { usuario ->
        DialogEmpleado(
            user = usuario,
            onDismiss = { empleadoEditar = null },
            onSave = { actualizado ->
                vm.actualizarUsuario(actualizado)
                empleadoEditar = null
            }
        )
    }

    val empleados = vm.usuarios.filter {
        searchQuery.isBlank() || it.nombre.lowercase().contains(searchQuery.lowercase())
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        OutlinedTextField(
            value = searchQuery, onValueChange = { searchQuery = it },
            placeholder = { Text("Buscar empleado...") },
            leadingIcon = { Icon(Icons.Default.Search, "Buscar") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp), singleLine = true
        )
        Spacer(Modifier.height(12.dp))

        if (empleados.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.People, "Personas",
                        Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(0.2f)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (vm.usuarios.isEmpty()) "No hay empleados registrados"
                        else "Sin resultados para \"$searchQuery\"",
                        color = MaterialTheme.colorScheme.onSurface.copy(0.5f)
                    )
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(empleados, key = { it.uid }) { user ->
                    ElevatedCard(
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            Modifier.padding(start = 14.dp, top = 10.dp, bottom = 10.dp, end = 4.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(user.nombre.uppercase(), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text(
                                    user.rol?.name ?: "Sin rol",
                                    color = MaterialTheme.colorScheme.outline,
                                    fontSize = 12.sp
                                )
                                if (user.correo.isNotBlank()) {
                                    Text(
                                        user.correo,
                                        color = MaterialTheme.colorScheme.outline.copy(0.7f),
                                        fontSize = 11.sp
                                    )
                                }
                            }
                            IconButton(onClick = { empleadoEditar = user }) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "Editar empleado",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
