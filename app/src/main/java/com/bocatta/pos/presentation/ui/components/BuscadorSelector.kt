package com.bocatta.pos.presentation.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class ItemSeleccionable<T>(
    val id: String,
    val nombre: String,
    val data: T,
    val cantidad: Double = 1.0,
    val unidad: String = "pz"
)

@Composable
fun <T> BuscadorSelector(
    label: String,
    items: List<T>,
    selectedItems: List<ItemSeleccionable<T>>,
    filterPredicate: (T, String) -> Boolean,
    itemLabel: (T) -> String,
    onCreateNew: ((String) -> Unit)? = null,
    onAddItem: (T) -> Unit,
    onRemoveItem: (T) -> Unit,
    onUpdateCantidad: ((T, Double) -> Unit)? = null
) {
    var query by remember { mutableStateOf("") }
    var showDropdown by remember { mutableStateOf(false) }

    Column {
        // Items seleccionados
        selectedItems.forEach { item ->
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(0.3f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
            ) {
                Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(item.nombre, modifier = Modifier.weight(1f), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    if (onUpdateCantidad != null) {
                        OutlinedTextField(
                            value = if (item.cantidad == item.cantidad.toLong().toDouble()) item.cantidad.toInt().toString() else "%.1f".format(item.cantidad),
                            onValueChange = { v -> onUpdateCantidad(item.data, v.toDoubleOrNull() ?: item.cantidad) },
                            modifier = Modifier.width(60.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            textStyle = MaterialTheme.typography.labelSmall
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(item.unidad, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
                    } else {
                        Text("${item.cantidad.toInt()} ${item.unidad}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.6f))
                    }
                    IconButton(onClick = { onRemoveItem(item.data) }, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Default.Close, "Quitar", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }

        // Búsqueda
        Box {
            OutlinedTextField(
                value = query, onValueChange = { query = it; showDropdown = it.isNotEmpty() },
                placeholder = { Text("Buscar $label...", fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, "Buscar", modifier = Modifier.size(18.dp)) },
                trailingIcon = {
                    if (query.isNotEmpty()) IconButton(onClick = { query = ""; showDropdown = false }) {
                        Icon(Icons.Default.Clear, "Limpiar", modifier = Modifier.size(18.dp))
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall
            )
            if (showDropdown && query.isNotBlank()) {
                val scoredItems = items
                    .map { item -> item to SearchHelper.calcularScoreRelevancia(query, itemLabel(item)) }
                    .filter { (_, score) -> score > 0 }
                    .filter { (item, _) -> selectedItems.none { s -> s.data == item } }
                    .sortedByDescending { (_, score) -> score }
                    .map { (item, _) -> item }

                val filtered = scoredItems.take(5)

                val tieneSimilares = items.any { item ->
                    val score = SearchHelper.calcularScoreRelevancia(query, itemLabel(item))
                    score >= 30
                }

                if (filtered.isNotEmpty() || onCreateNew != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 8.dp,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().padding(top = 52.dp)
                    ) {
                        Column {
                            filtered.forEach { item ->
                                Surface(
                                    onClick = { onAddItem(item); query = ""; showDropdown = false },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(itemLabel(item), Modifier.padding(horizontal = 16.dp, vertical = 12.dp), fontSize = 13.sp)
                                }
                            }
                            if (onCreateNew != null) {
                                HorizontalDivider()

                                if (tieneSimilares) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.errorContainer.copy(0.15f),
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Row(
                                            Modifier.padding(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                Icons.Default.Warning,
                                                contentDescription = "Duplicado",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                "Ya existe un registro similar. Evita duplicados.",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.error,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                Surface(
                                    onClick = { onCreateNew(query); query = ""; showDropdown = false },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Add, "Agregar", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                        Spacer(Modifier.width(8.dp))
                                        Text("Crear nuevo \"$query\"...", fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
