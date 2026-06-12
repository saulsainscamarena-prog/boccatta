package com.bocatta.pos.feature.admin.ui.screens.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bocatta.pos.domain.model.AlcancePromo
import com.bocatta.pos.domain.model.PromocionUniversal
import com.bocatta.pos.domain.model.SalesInventoryProductV2
import com.bocatta.pos.domain.model.TipoDescuentoPromo
import com.bocatta.pos.feature.admin.viewmodel.PromocionViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TabPromociones(
    vm: PromocionViewModel,
    allProducts: List<SalesInventoryProductV2>
) {
    var showDialog by remember { mutableStateOf(false) }
    var editingPromo by remember { mutableStateOf<PromocionUniversal?>(null) }
    val promos by vm.promociones.collectAsStateWithLifecycle()

    Scaffold(contentWindowInsets = WindowInsets.safeDrawing, 
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { editingPromo = null; showDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                icon = { Icon(Icons.Default.Add, "Nueva") },
                text = { Text("NUEVA PROMO", fontWeight = FontWeight.Bold) }
            )
        }
    ) { padding ->
        if (promos.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.CardGiftcard, "Tarjeta", Modifier.size(80.dp), tint = MaterialTheme.colorScheme.surfaceVariant)
                    Spacer(Modifier.height(16.dp))
                    Text("No hay promociones activas", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Toca + para crear tu primera promoción", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.7f))
                }
            }
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(promos, key = { it.id }) { promo ->
                    PromoCard(
                        promo = promo,
                        onEdit = { editingPromo = promo; showDialog = true },
                        onDelete = { vm.eliminar(promo.id) },
                        onToggleActive = { vm.guardar(promo.copy(activa = !promo.activa)) }
                    )
                }
            }
        }
    }

    if (showDialog) {
        DialogoCrearPromocion(
            promoInicial = editingPromo,
            allProducts = allProducts,
            onSave = { vm.guardar(it); showDialog = false },
            onDismiss = { showDialog = false; editingPromo = null }
        )
    }
}

@Composable
private fun PromoCard(
    promo: PromocionUniversal,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleActive: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd/MM/yy", Locale.getDefault()) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (promo.activa) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(0.5f)
        )
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(promo.nombre, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(promo.descripcion, fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                }
                Switch(checked = promo.activa, onCheckedChange = { onToggleActive() })
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("${if (promo.tipoDescuento == TipoDescuentoPromo.PORCENTAJE) "${promo.valorDescuento.toInt()}%" else "$${"%.0f".format(promo.valorDescuento)}"}",
                    fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
                Text("·", color = MaterialTheme.colorScheme.outline)
                Text(promo.alcance.name.replace("_", " "), fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                if (promo.montoMinimoTicket > 0) {
                    Text("· Min. $${"%.0f".format(promo.montoMinimoTicket)}", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                }
            }
            val fechaFin = promo.fechaFin
            if (fechaFin != null) {
                Text("Válida hasta ${dateFormat.format(Date(fechaFin))}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
            }
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Edit, "Editar", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp)) }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Delete, "Eliminar", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp)) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DialogoCrearPromocion(
    promoInicial: PromocionUniversal?,
    allProducts: List<SalesInventoryProductV2>,
    onSave: (PromocionUniversal) -> Unit,
    onDismiss: () -> Unit
) {
    var nombre by remember { mutableStateOf(promoInicial?.nombre ?: "") }
    var descripcion by remember { mutableStateOf(promoInicial?.descripcion ?: "") }
    var tipoDesc by remember { mutableStateOf(promoInicial?.tipoDescuento ?: TipoDescuentoPromo.PORCENTAJE) }
    var valorDesc by remember { mutableStateOf(promoInicial?.valorDescuento?.toString() ?: "10") }
    var alcance by remember { mutableStateOf(promoInicial?.alcance ?: AlcancePromo.TICKET_COMPLETO) }
    var montoMin by remember { mutableStateOf(promoInicial?.montoMinimoTicket?.toString() ?: "0") }
    var productosSeleccionados by remember { mutableStateOf(promoInicial?.itemsIncluidosIds ?: emptyList<String>()) }
    var mostrarRentabilidad by remember { mutableStateOf(false) }
    var precioPromoInput by remember { mutableStateOf("") }

    val mostrarAvisoCostoReal = mostrarRentabilidad &&
        productosSeleccionados.isNotEmpty() &&
        precioPromoInput.toDoubleOrNull() != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${if (promoInicial != null) "Editar" else "Nueva"} promoción", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(value = nombre, onValueChange = { nombre = it }, label = { Text("Nombre*") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                OutlinedTextField(value = descripcion, onValueChange = { descripcion = it }, label = { Text("Descripción") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(TipoDescuentoPromo.PORCENTAJE to "%", TipoDescuentoPromo.MONTO_FIJO_TICKET to "\$").forEach { (t, l) ->
                        FilterChip(selected = tipoDesc == t, onClick = { tipoDesc = t }, label = { Text(l) })
                    }
                }
                OutlinedTextField(value = valorDesc, onValueChange = { valorDesc = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text(if (tipoDesc == TipoDescuentoPromo.PORCENTAJE) "Porcentaje (%)" else "Monto (\$)") },
                    modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), shape = RoundedCornerShape(12.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(AlcancePromo.TICKET_COMPLETO to "Ticket", AlcancePromo.PRODUCTOS_ESPECIFICOS to "Productos").forEach { (a, l) ->
                        FilterChip(selected = alcance == a, onClick = { alcance = a }, label = { Text(l) })
                    }
                }
                if (alcance == AlcancePromo.PRODUCTOS_ESPECIFICOS) {
                    Text("Productos incluidos:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    var expandProd by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(expanded = expandProd, onExpandedChange = { expandProd = it }) {
                        OutlinedTextField(value = "", onValueChange = {}, readOnly = true, placeholder = { Text("Seleccionar producto...") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expandProd) }, modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                        ExposedDropdownMenu(expanded = expandProd, onDismissRequest = { expandProd = false }) {
                            allProducts.filter { it.id !in productosSeleccionados }.forEach { prod ->
                                DropdownMenuItem(text = { Text(prod.nombre) }, onClick = { productosSeleccionados = productosSeleccionados + prod.id; expandProd = false })
                            }
                        }
                    }
                    productosSeleccionados.forEach { id ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(allProducts.find { it.id == id }?.nombre ?: id, modifier = Modifier.weight(1f), fontSize = 13.sp)
                            IconButton(onClick = { productosSeleccionados = productosSeleccionados - id }) { Icon(Icons.Default.Close, "Cerrar", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp)) }
                        }
                    }
                    if (productosSeleccionados.isNotEmpty()) {
                        Spacer(Modifier.height(4.dp))
                        Text("Analisis de rentabilidad", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                        OutlinedTextField(value = precioPromoInput, onValueChange = { precioPromoInput = it; mostrarRentabilidad = it.isNotBlank() },
                            label = { Text("Precio promocional") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), shape = RoundedCornerShape(12.dp))
                        if (mostrarAvisoCostoReal) {
                            Surface(
                                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(Modifier.padding(12.dp)) {
                                    Text(
                                        "Costo real no disponible aqui",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                    Text(
                                        "Se debe calcular desde recetas y movimientos de inventario. No se usa precio de venta como costo.",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                }
                            }
                        }
                    }
                }
                OutlinedTextField(value = montoMin, onValueChange = { montoMin = it.filter { c -> c.isDigit() || c == '.' } }, label = { Text("Monto mínimo de ticket") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), shape = RoundedCornerShape(12.dp))
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(PromocionUniversal(
                    id = promoInicial?.id ?: "",
                    nombre = nombre, descripcion = descripcion,
                    tipoDescuento = tipoDesc, valorDescuento = valorDesc.toDoubleOrNull() ?: 0.0,
                    alcance = alcance, itemsIncluidosIds = if (alcance == AlcancePromo.PRODUCTOS_ESPECIFICOS) productosSeleccionados else emptyList(),
                    montoMinimoTicket = montoMin.toDoubleOrNull() ?: 0.0,
                    activa = true
                ))
                onDismiss()
            }, enabled = nombre.isNotBlank()) { Text("Guardar", fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
        shape = RoundedCornerShape(20.dp)
    )
}



