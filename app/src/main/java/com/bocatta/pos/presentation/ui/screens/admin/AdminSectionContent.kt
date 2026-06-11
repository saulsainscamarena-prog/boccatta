package com.bocatta.pos.presentation.ui.screens.admin

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bocatta.pos.R
import com.bocatta.pos.domain.model.*
import com.bocatta.pos.presentation.ui.components.BocattaButton
import com.bocatta.pos.presentation.ui.components.BocattaEmptyState
import com.bocatta.pos.presentation.ui.components.BocattaSectionTitle
import com.bocatta.pos.presentation.viewmodel.AdminViewModel
import com.bocatta.pos.presentation.viewmodel.InventoryViewModel
import java.text.SimpleDateFormat
import java.util.*

// ── TAB MENU ──────────────────────────────────────────────────────────────────

@Composable
fun TabMenu(vm: AdminViewModel) {
    var mostrarDialogo by remember { mutableStateOf(false) }
    var productoEditar by remember { mutableStateOf<SalesInventoryProductV2?>(null) }

    if (mostrarDialogo || productoEditar != null) {
        DialogProducto(
            productoInicial = productoEditar,
            vm = vm,
            onGuardar = { prod, receta ->
                if (productoEditar != null) vm.editarProducto(prod) else vm.agregarProducto(prod)
                if (receta.ingredientes.isNotEmpty()) vm.guardarReceta(receta)
                mostrarDialogo = false; productoEditar = null
            },
            onCancelar = { mostrarDialogo = false; productoEditar = null }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            BocattaButton(
                texto = stringResource(R.string.admin_btn_agregar_producto),
                onClick = { mostrarDialogo = true },
                modifier = Modifier.fillMaxWidth(),
                icono = Icons.Default.AddCircle
            )
            Spacer(Modifier.height(8.dp))
        }
        vm.productos.groupBy { it.categoria }.forEach { (cat, prods) ->
            item {
                BocattaSectionTitle(cat)
            }
            items(prods, key = { it.id }) { producto ->
                ElevatedCard(shape = RoundedCornerShape(14.dp)) {
                    Row(
                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Text(producto.emoji, fontSize = 24.sp)
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(producto.nombre, fontWeight = FontWeight.SemiBold)
                                Text(
                                    stringResource(R.string.admin_precios_sucursal, "%.0f".format(producto.precioVenta["atlixco"] ?: 0.0), "%.0f".format(producto.precioVenta["metepec"] ?: 0.0)),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                        Row {
                            IconButton(onClick = { productoEditar = producto }) {
                                Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.admin_icono_editar), tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = { vm.eliminarProducto(producto) }) {
                                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.admin_icono_eliminar), tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── TAB AUDITORÍA ─────────────────────────────────────────────────────────────

@Composable
fun TabAuditoria(vm: AdminViewModel) {
    val sdf = remember { SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()) }
    if (vm.cancelacionesPendientes.isEmpty() && vm.diferenciasInventario.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            BocattaEmptyState(
                icono = Icons.Default.VerifiedUser,
                titulo = stringResource(R.string.admin_sin_alertas),
                descripcion = stringResource(R.string.admin_sin_alertas_desc),
                modifier = Modifier.fillMaxWidth()
            )
        }
        return
    }
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            BocattaSectionTitle(stringResource(R.string.admin_auditoria_titulo))
            Text(stringResource(R.string.admin_auditoria_desc), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline)
            Spacer(Modifier.height(8.dp))
        }
        if (vm.diferenciasInventario.isNotEmpty()) {
            item {
                Text(stringResource(R.string.admin_auditoria_diferencias), fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
            }
            items(vm.diferenciasInventario, key = { it["id"]?.toString() ?: it.hashCode().toString() }) { log ->
                ElevatedCard(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(0.55f))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(log["insumoId"]?.toString()?.replace("_", " ") ?: stringResource(R.string.admin_producto_label), fontWeight = FontWeight.Black)
                            Text(
                                log["motivo"]?.let { stringResource(R.string.admin_motivo, it) } ?: stringResource(R.string.admin_sin_motivo),
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                "${stringResource(R.string.admin_sugerido, log["sugerido"] ?: 0)}  ${stringResource(R.string.admin_confirmado, log["confirmado"] ?: 0)}  ${stringResource(R.string.admin_sucursal_label, log["sucursal"] ?: "-")}",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                        Text(
                            sdf.format(Date(log["fecha"] as? Long ?: 0L)),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }
        if (vm.cancelacionesPendientes.isNotEmpty()) {
            item {
                Text(stringResource(R.string.admin_auditoria_cancelaciones), fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
            }
            items(vm.cancelacionesPendientes.reversed()) { log ->
                ElevatedCard(shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Row(modifier = Modifier.padding(14.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(log["productoNombre"]?.toString() ?: stringResource(R.string.admin_producto_label), fontWeight = FontWeight.Black)
                            Text(stringResource(R.string.admin_motivo, log["motivo"]?.toString() ?: ""), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                            Text(stringResource(R.string.admin_vendedor_label, log["vendedor"]?.toString() ?: ""), style = MaterialTheme.typography.labelSmall)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(sdf.format(Date(log["fecha"] as? Long ?: 0L)),
                                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            IconButton(onClick = { vm.revisarCancelacion(log["id"].toString()) }) {
                                Icon(Icons.Default.CheckCircle, contentDescription = stringResource(R.string.admin_marcar_revisado), tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── TAB RECETAS ───────────────────────────────────────────────────────────────

@Composable
fun TabRecetas(vm: AdminViewModel) {
    var productoParaReceta by remember { mutableStateOf<SalesInventoryProductV2?>(null) }

    productoParaReceta?.let { prod ->
        DialogReceta(
            producto = prod,
            insumosDisponibles = vm.insumosMaestros,
            recetaActual = vm.recetas[prod.id],
            onGuardar = { receta -> vm.guardarReceta(receta); productoParaReceta = null },
            onCancelar = { productoParaReceta = null }
        )
    }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            BocattaSectionTitle(stringResource(R.string.admin_recetas_titulo))
            Text(stringResource(R.string.admin_recetas_desc), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline)
            Spacer(Modifier.height(8.dp))
        }
        vm.productos.groupBy { it.categoria }.forEach { (cat, prods) ->
            item { Text(cat, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) }
            items(prods, key = { it.id }) { prod ->
                ElevatedCard(shape = RoundedCornerShape(12.dp), onClick = { productoParaReceta = prod }) {
                    Row(modifier = Modifier.padding(14.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text(prod.nombre, fontWeight = FontWeight.Bold)
                            val receta = vm.recetas[prod.id]
                            if (receta != null && receta.ingredientes.isNotEmpty())
                                Text(stringResource(R.string.admin_receta_ingredientes, receta.ingredientes.size), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                            else
                                Text(stringResource(R.string.admin_receta_sin_receta), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = stringResource(R.string.admin_receta_configurar), tint = MaterialTheme.colorScheme.outline)
                    }
                }
            }
        }
    }
}

// ── DIALOG RECETA ─────────────────────────────────────────────────────────────

@Composable
fun DialogReceta(
    producto: SalesInventoryProductV2,
    insumosDisponibles: List<InsumoV2>,
    recetaActual: RecetaV2?,
    onGuardar: (RecetaV2) -> Unit,
    onCancelar: () -> Unit
) {
    val ingredientesMap = remember { mutableStateMapOf<String, Double>().apply {
        recetaActual?.ingredientes?.forEach { put(it.insumoId, it.cantidad) }
    } }
    var expandInsumos by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text(stringResource(R.string.admin_dialog_receta_titulo, producto.nombre), fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.heightIn(max = 380.dp)) {
                Text(stringResource(R.string.admin_dialog_receta_desc), style = MaterialTheme.typography.bodySmall)
                Box {
                    BocattaButton(texto = stringResource(R.string.admin_dialog_agregar_ingrediente), onClick = { expandInsumos = true },
                        modifier = Modifier.fillMaxWidth(), icono = Icons.Default.Add)
                    DropdownMenu(expanded = expandInsumos, onDismissRequest = { expandInsumos = false }) {
                        insumosDisponibles.forEach { ins ->
                            DropdownMenuItem(text = { Text(ins.nombre) }, onClick = {
                                ingredientesMap[ins.id] = 0.0; expandInsumos = false
                            })
                        }
                    }
                }
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                    items(ingredientesMap.keys.toList()) { id ->
                        val ins = insumosDisponibles.find { it.id == id }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(ins?.nombre ?: id, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                            var qty by remember { mutableStateOf(ingredientesMap[id]?.toString() ?: "0") }
                            OutlinedTextField(value = qty, onValueChange = { qty = it; it.toDoubleOrNull()?.let { d -> ingredientesMap[id] = d } },
                                label = { Text(ins?.unidadBase ?: "") }, modifier = Modifier.width(90.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
                            IconButton(onClick = { ingredientesMap.remove(id) }) {
                                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.admin_icono_eliminar), tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onGuardar(RecetaV2(productoId = producto.id,
                    ingredientes = ingredientesMap.map { (id, cant) -> IngredienteReceta(insumoId = id, cantidad = cant) }))
            }) { Text(stringResource(R.string.admin_dialog_guardar_receta)) }
        },
        dismissButton = { TextButton(onClick = onCancelar) { Text(stringResource(R.string.admin_cancelar)) } },
        shape = RoundedCornerShape(20.dp)
    )
}

// ── TAB COSTOS INSUMOS ────────────────────────────────────────────────────────

@Composable
fun TabCostosInsumos(vm: AdminViewModel) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            BocattaSectionTitle(stringResource(R.string.admin_costos_titulo))
            Text(stringResource(R.string.admin_costos_desc),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            Spacer(Modifier.height(8.dp))
        }
        items(vm.insumosMaestros.filterNot { it.categoria.contains("Produ", ignoreCase = true) }, key = { it.id }) { insumo ->
            ElevatedCard(shape = RoundedCornerShape(14.dp)) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(insumo.nombre, fontWeight = FontWeight.Bold)
                    var precioStr by remember(insumo.costoUnitarioBase) { mutableStateOf(insumo.costoUnitarioBase.toString()) }
                    Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(stringResource(R.string.admin_costos_por_unidad, insumo.unidadBase), style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = precioStr, onValueChange = { precioStr = it },
                            label = { Text("$") }, modifier = Modifier.width(100.dp), shape = RoundedCornerShape(8.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
                        IconButton(onClick = { vm.actualizarCostoInsumo(insumo.id, precioStr.toDoubleOrNull() ?: 0.0) }) {
                            Icon(Icons.Default.Check, contentDescription = stringResource(R.string.admin_costos_guardar), tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}

// ── TAB PRODUCCIÓN ────────────────────────────────────────────────────────────

@Composable
fun TabProduccion(vm: InventoryViewModel) {
    var selectedId by remember { mutableStateOf<String?>(null) }
    var materiaUsada by remember { mutableStateOf("") }
    var porcionesObtenidas by remember { mutableStateOf("") }
    var guardando by remember { mutableStateOf(false) }
    val itemsProduccion = vm.maestroInsumos.values.filter {
        it.categoria.contains("Produ", ignoreCase = true) ||
            it.id in setOf("masa_crepa", "carlota_unidad", "tiramisu_unidad", "fresas_crema_unidad", "duraznos_crema_unidad")
    }
    var expandProd by remember { mutableStateOf(false) }

    val yieldHistory = remember(selectedId) {
        listOf(58.0, 62.0, 60.0, 55.0, 61.0)
    }
    val avgYield = remember(yieldHistory) {
        if (yieldHistory.isEmpty()) 0.0 else yieldHistory.average()
    }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            BocattaSectionTitle(stringResource(R.string.admin_produccion_titulo))
            Text(stringResource(R.string.admin_produccion_desc),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
        }
        item {
            ElevatedCard(shape = RoundedCornerShape(20.dp)) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Box {
                        OutlinedButton(onClick = { expandProd = true }, modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)) { Text(stringResource(R.string.admin_produccion_seleccionar)) }
                        DropdownMenu(expanded = expandProd, onDismissRequest = { expandProd = false }) {
                            itemsProduccion.forEach { insumo ->
                                DropdownMenuItem(text = { Text(insumo.nombre) }, onClick = {
                                    selectedId = insumo.id; expandProd = false
                                })
                            }
                        }
                    }
                }
            }
        }
    }
}
