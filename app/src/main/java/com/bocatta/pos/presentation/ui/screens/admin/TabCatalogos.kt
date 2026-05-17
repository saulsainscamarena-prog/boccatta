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
import com.bocatta.pos.domain.model.OpcionCatalogo
import com.bocatta.pos.domain.model.TipoCatalogo
import com.bocatta.pos.presentation.viewmodel.CatalogoViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TabCatalogos(vm: CatalogoViewModel) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabTitles = TipoCatalogo.entries.map { it.name }
    var showDialog by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<OpcionCatalogo?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    val opciones by vm.opciones.collectAsState()

    val filtered = if (searchQuery.isBlank()) opciones
    else opciones.filter { it.nombre.lowercase().contains(searchQuery.lowercase()) }

    Scaffold(contentWindowInsets = WindowInsets.safeDrawing, 
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { editingItem = null; showDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                icon = { Icon(Icons.Default.Add, null) },
                text = { Text("NUEVO") }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = searchQuery, onValueChange = { searchQuery = it },
                placeholder = { Text("Buscar en todos los catálogos...") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )
            ScrollableTabRow(selectedTabIndex = selectedTab, edgePadding = 16.dp) {
                tabTitles.forEachIndexed { i, title ->
                    Tab(selected = selectedTab == i, onClick = { selectedTab = i },
                        text = { Text(title.replace("_", " "), fontSize = 11.sp, fontWeight = FontWeight.Bold) })
                }
            }
            val tipoActual = TipoCatalogo.entries[selectedTab]
            val itemsTipo = filtered.filter { it.tipo == tipoActual }

            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                if (itemsTipo.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Inventory2, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurface.copy(0.2f))
                                Spacer(Modifier.height(8.dp))
                                Text("Sin registros", color = MaterialTheme.colorScheme.onSurface.copy(0.4f))
                            }
                        }
                    }
                }
                items(itemsTipo, key = { it.id }) { item ->
                    OpcionCard(
                        item = item,
                        onEdit = { editingItem = item; showDialog = true },
                        onDelete = { vm.eliminar(item.id) },
                        onToggleActive = {
                            vm.guardar(item.copy(activo = !item.activo))
                        }
                    )
                }
            }
        }
    }

    if (showDialog) {
        DialogoOpcionCatalogo(
            itemInicial = editingItem,
            onSave = { vm.guardar(it) },
            onDismiss = { showDialog = false; editingItem = null }
        )
    }
}

@Composable
private fun OpcionCard(
    item: OpcionCatalogo,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleActive: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (item.activo) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(0.5f)
        )
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(item.nombre, fontWeight = FontWeight.Bold, fontSize = 15.sp,
                    color = if (item.activo) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(0.4f))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (item.esPremium) Surface(color = MaterialTheme.colorScheme.error.copy(0.1f), shape = RoundedCornerShape(4.dp)) {
                        Text("Premium", Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 9.sp, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                    }
                    if (item.costoExtra > 0) Text("+\$${"%.0f".format(item.costoExtra)}", fontSize = 10.sp, color = MaterialTheme.colorScheme.tertiary)
                }
            }
            Switch(checked = item.activo, onCheckedChange = { onToggleActive() }, modifier = Modifier.padding(end = 4.dp))
            IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp)) }
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DialogoOpcionCatalogo(
    itemInicial: OpcionCatalogo?,
    onSave: (OpcionCatalogo) -> Unit,
    onDismiss: () -> Unit
) {
    var nombre by remember { mutableStateOf(itemInicial?.nombre ?: "") }
    var tipo by remember { mutableStateOf(itemInicial?.tipo ?: TipoCatalogo.TOPPING) }
    var esPremium by remember { mutableStateOf(itemInicial?.esPremium ?: false) }
    var costoExtra by remember { mutableStateOf(itemInicial?.costoExtra?.toString() ?: "0") }
    var expandedTipo by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${if (itemInicial != null) "Editar" else "Nuevo"} ítem", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = nombre, onValueChange = { nombre = it },
                    label = { Text("Nombre*") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))

                ExposedDropdownMenuBox(expanded = expandedTipo, onExpandedChange = { expandedTipo = it }) {
                    OutlinedTextField(value = tipo.name, onValueChange = {}, readOnly = true,
                        label = { Text("Tipo") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expandedTipo) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                    ExposedDropdownMenu(expanded = expandedTipo, onDismissRequest = { expandedTipo = false }) {
                        TipoCatalogo.entries.forEach { t ->
                            DropdownMenuItem(text = { Text(t.name.replace("_", " ")) }, onClick = { tipo = t; expandedTipo = false })
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Premium", modifier = Modifier.weight(1f))
                    Switch(checked = esPremium, onCheckedChange = { esPremium = it })
                }

                if (esPremium) {
                    OutlinedTextField(value = costoExtra, onValueChange = { costoExtra = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("Costo extra \$") }, modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal),
                        shape = RoundedCornerShape(12.dp))
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(OpcionCatalogo(
                    id = itemInicial?.id ?: "",
                    nombre = nombre, tipo = tipo,
                    esPremium = esPremium, activo = true,
                    costoExtra = costoExtra.toDoubleOrNull() ?: 0.0
                ))
                onDismiss()
            }, enabled = nombre.isNotBlank()) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
        shape = RoundedCornerShape(20.dp)
    )
}


