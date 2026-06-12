package com.bocatta.pos.feature.admin.ui.screens.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bocatta.pos.domain.model.EstadoMesa
import com.bocatta.pos.domain.model.Mesa
import com.bocatta.pos.domain.model.Zona
import com.bocatta.pos.feature.ventas.viewmodel.MesaViewModel

@Composable
fun TabZonas(vm: MesaViewModel) {
    val zonas = vm.zonas
    val mesas = vm.mesas
    var showZonaDialog by remember { mutableStateOf(false) }
    var showMesaDialog by remember { mutableStateOf(false) }
    var editingZona by remember { mutableStateOf<Zona?>(null) }
    var editingMesa by remember { mutableStateOf<Mesa?>(null) }
    var selectedZonaId by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("ZONAS", fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.weight(1f))
            IconButton(onClick = { editingZona = null; showZonaDialog = true }) { Icon(Icons.Default.Add, "Agregar") }
        }
        Spacer(Modifier.height(8.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
            zonas.filter { it.activo }.forEach { zona ->
                item(key = zona.id) {
                    val count = mesas.count { it.zonaId == zona.id }
                    ElevatedCard(
                        onClick = { selectedZonaId = if (selectedZonaId == zona.id) "" else zona.id },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = if (selectedZonaId == zona.id) MaterialTheme.colorScheme.primaryContainer.copy(0.3f) else MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(zona.nombre, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                Text("$count mesas", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                                IconButton(onClick = { editingZona = zona; showZonaDialog = true }) { Icon(Icons.Default.Edit, "Editar", modifier = Modifier.size(18.dp)) }
                                IconButton(onClick = { vm.eliminarZona(zona.id) }) { Icon(Icons.Default.Delete, "Eliminar", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp)) }
                            }
                            if (selectedZonaId == zona.id) {
                                Spacer(Modifier.height(8.dp))
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    mesas.filter { it.zonaId == zona.id }.forEach { mesa ->
                                        val color = when (mesa.estado) {
                                            EstadoMesa.LIBRE -> MaterialTheme.colorScheme.primary
                                            EstadoMesa.OCUPADA -> MaterialTheme.colorScheme.error
                                            EstadoMesa.RESERVADA -> MaterialTheme.colorScheme.tertiary
                                            EstadoMesa.INACTIVA -> Color.Gray
                                        }
                                        Surface(color = color.copy(0.15f), shape = RoundedCornerShape(10.dp), modifier = Modifier.width(80.dp).padding(2.dp)) {
                                            Column(Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text(mesa.numero.toString(), fontWeight = FontWeight.Black, fontSize = 18.sp, color = color)
                                                Text(mesa.estado.name.take(4), fontSize = 8.sp, color = Color.White.copy(0.7f))
                                            }
                                        }
                                    }
                                    IconButton(onClick = { editingMesa = Mesa(zonaId = zona.id); showMesaDialog = true }) {
                                        Icon(Icons.Default.Add, "Agregar", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showZonaDialog) {
        DialogZona(zonaInicial = editingZona, onSave = { vm.guardarZona(it) }, onDismiss = { showZonaDialog = false; editingZona = null })
    }
    val mesaParaEditar = editingMesa
    if (showMesaDialog && mesaParaEditar != null) {
        DialogMesa(mesaInicial = mesaParaEditar, zonas = zonas, onSave = { vm.guardarMesa(it) }, onDismiss = { showMesaDialog = false; editingMesa = null })
    }
}

@Composable
private fun DialogZona(zonaInicial: Zona?, onSave: (Zona) -> Unit, onDismiss: () -> Unit) {
    var nombre by remember { mutableStateOf(zonaInicial?.nombre ?: "") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("${if (zonaInicial != null) "Editar" else "Nueva"} zona", fontWeight = FontWeight.Bold) },
        text = { OutlinedTextField(value = nombre, onValueChange = { nombre = it }, label = { Text("Nombre") }, placeholder = { Text("Ej: Terraza, Bar") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) },
        confirmButton = { Button(onClick = { onSave(Zona(id = zonaInicial?.id ?: "", nombre = nombre.uppercase(java.util.Locale.getDefault()))); onDismiss() }, enabled = nombre.isNotBlank()) { Text("Guardar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }, shape = RoundedCornerShape(20.dp))
}

@Composable
private fun DialogMesa(mesaInicial: Mesa, zonas: List<Zona>, onSave: (Mesa) -> Unit, onDismiss: () -> Unit) {
    var numero by remember { mutableStateOf(if (mesaInicial.numero > 0) mesaInicial.numero.toString() else "") }
    var capacidad by remember { mutableStateOf(if (mesaInicial.capacidad > 0) mesaInicial.capacidad.toString() else "4") }
    var zonaId by remember { mutableStateOf(mesaInicial.zonaId.ifBlank { zonas.firstOrNull()?.id ?: "" }) }

    AlertDialog(onDismissRequest = onDismiss, title = { Text("Nueva mesa", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = numero, onValueChange = { numero = it.filter { c -> c.isDigit() } }, label = { Text("Número") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                OutlinedTextField(value = capacidad, onValueChange = { capacidad = it.filter { c -> c.isDigit() } }, label = { Text("Capacidad") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                Text("Zona:", style = MaterialTheme.typography.labelSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    zonas.filter { it.activo }.forEach { z -> FilterChip(selected = zonaId == z.id, onClick = { zonaId = z.id }, label = { Text(z.nombre, fontSize = 10.sp) }) }
                }
            }
        },
        confirmButton = { Button(onClick = { onSave(Mesa(id = mesaInicial.id, numero = numero.toIntOrNull() ?: 0, capacidad = capacidad.toIntOrNull() ?: 4, zonaId = zonaId)); onDismiss() }, enabled = numero.isNotBlank()) { Text("Guardar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }, shape = RoundedCornerShape(20.dp))
}
