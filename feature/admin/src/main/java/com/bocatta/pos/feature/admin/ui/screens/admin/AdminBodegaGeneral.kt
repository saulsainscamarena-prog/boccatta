package com.bocatta.pos.feature.admin.ui.screens.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bocatta.pos.core.ui.R
import com.bocatta.pos.domain.model.InsumoV2
import com.bocatta.pos.presentation.ui.components.BocattaBadge
import com.bocatta.pos.presentation.ui.components.BocattaButton
import com.bocatta.pos.presentation.ui.components.BocattaSectionTitle
import com.bocatta.pos.feature.ventas.ui.components.DialogCompraUnificado
import com.bocatta.pos.feature.ventas.ui.components.DialogEditarInsumo
import com.bocatta.pos.feature.admin.viewmodel.AdminViewModel
import kotlinx.coroutines.launch

@Composable
fun TabBodegaGeneral(
    vm: AdminViewModel,
    nombreUsuario: String = "",
    usuarioId: String = "",
    sucursal: String = "",
    onVerDashboardBodega: () -> Unit,
    onVerGestionarSucursales: () -> Unit,
    onVerReportesInventario: () -> Unit,
    onVerSyncInventario: () -> Unit
) {
    var insumoAjustar by remember { mutableStateOf<InsumoV2?>(null) }
    var showPurchaseDialog by remember { mutableStateOf(false) }
    var showNuevoInsumoDialog by remember { mutableStateOf(false) }

    insumoAjustar?.let { insumo ->
        DialogEditarInsumo(
            insumo = insumo,
            onGuardar = { nuevoStockMinimo, conteoFisico, motivo ->
                vm.ajustarConConteoFisico(insumo.id, nuevoStockMinimo, conteoFisico, motivo)
                insumoAjustar = null
            },
            onDismiss = { insumoAjustar = null }
        )
    }

    if (showPurchaseDialog) {
        DialogCompraUnificado(
            insumos = vm.insumosMaestros,
            nombreUsuario = nombreUsuario,
            usuarioId = usuarioId,
            sucursal = sucursal,
            esAdmin = true,
            onConfirmar = { insumoId, insumoNombre, presentacion, cant, cont, precio ->
                vm.registrarCompraRapida(insumoId, insumoNombre, presentacion, cant, cont, precio, usuarioId, nombreUsuario, sucursal, true)
                showPurchaseDialog = false
            },
            onDismiss = { showPurchaseDialog = false }
        )
    }

    if (showNuevoInsumoDialog) {
        var nuevoNombre by remember { mutableStateOf("") }
        var nuevaUnidad by remember { mutableStateOf("kg") }
        var nuevoCosto by remember { mutableStateOf("0") }
        var nuevaCategoria by remember { mutableStateOf("") }
        val scope = rememberCoroutineScope()

        AlertDialog(
            onDismissRequest = { showNuevoInsumoDialog = false },
            title = { Text(stringResource(R.string.admin_nuevo_insumo_titulo), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value = nuevoNombre, onValueChange = { nuevoNombre = it },
                        label = { Text(stringResource(R.string.admin_nuevo_insumo_nombre)) }, placeholder = { Text(stringResource(R.string.admin_nuevo_insumo_ej_nombre)) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                    OutlinedTextField(value = nuevaUnidad, onValueChange = { nuevaUnidad = it },
                        label = { Text(stringResource(R.string.admin_nuevo_insumo_unidad)) }, placeholder = { Text(stringResource(R.string.admin_nuevo_insumo_ej_unidad)) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                    OutlinedTextField(value = nuevoCosto, onValueChange = { nuevoCosto = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text(stringResource(R.string.admin_nuevo_insumo_costo)) }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), shape = RoundedCornerShape(12.dp))
                    OutlinedTextField(value = nuevaCategoria, onValueChange = { nuevaCategoria = it },
                        label = { Text(stringResource(R.string.admin_nuevo_insumo_categoria)) }, placeholder = { Text(stringResource(R.string.admin_nuevo_insumo_ej_categoria)) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                }
            },
            confirmButton = {
                Button(onClick = {
                    scope.launch {
                        val insumo = InsumoV2(
                            nombre = nuevoNombre.trim(),
                            unidadBase = nuevaUnidad.ifBlank { "kg" },
                            costoUnitarioBase = nuevoCosto.toDoubleOrNull() ?: 0.0,
                            categoria = nuevaCategoria.ifBlank { "General" }
                        )
                        vm.agregarInsumo(insumo)
                        showNuevoInsumoDialog = false
                    }
                }, enabled = nuevoNombre.isNotBlank()) { Text(stringResource(R.string.admin_nuevo_insumo_btn_crear), fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { showNuevoInsumoDialog = false }) { Text(stringResource(R.string.admin_cancelar)) } },
            shape = RoundedCornerShape(20.dp)
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            BocattaButton(texto = stringResource(R.string.admin_bodega_titulo), onClick = { showPurchaseDialog = true },
                modifier = Modifier.fillMaxWidth(), icono = Icons.Default.AddShoppingCart)
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onVerDashboardBodega, modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)) { Text(stringResource(R.string.admin_bodega_btn_bodega), fontSize = 12.sp) }
                OutlinedButton(onClick = onVerGestionarSucursales, modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)) { Text(stringResource(R.string.admin_bodega_btn_sucursales), fontSize = 12.sp) }
            }
            Spacer(Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onVerReportesInventario, modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)) { Text(stringResource(R.string.admin_bodega_btn_reportes), fontSize = 12.sp) }
                OutlinedButton(onClick = onVerSyncInventario, modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)) { Text(stringResource(R.string.admin_bodega_btn_sync), fontSize = 12.sp) }
            }
            Spacer(Modifier.height(8.dp))
            BocattaSectionTitle(stringResource(R.string.admin_insumos_titulo))
            OutlinedButton(
                onClick = { showNuevoInsumoDialog = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Add, stringResource(R.string.admin_icono_agregar), modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.admin_btn_nuevo_insumo), fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp))
        }
        items(vm.insumosMaestros, key = { it.id }) { insumo ->
            val esProduccion = insumo.categoria.contains("Produ", ignoreCase = true)
            val stockBajo = insumo.cantidadEnBase <= insumo.stockMinimo
            ElevatedCard(
                onClick = { insumoAjustar = insumo },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = when {
                        stockBajo -> MaterialTheme.colorScheme.errorContainer.copy(0.3f)
                        esProduccion -> MaterialTheme.colorScheme.tertiaryContainer.copy(0.3f)
                        else -> MaterialTheme.colorScheme.surface
                    }
                )
            ) {
                Row(modifier = Modifier.padding(14.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(insumo.nombre, fontWeight = FontWeight.SemiBold)
                            if (esProduccion) {
                                Spacer(Modifier.width(6.dp))
                                BocattaBadge(stringResource(R.string.admin_badge_prod), MaterialTheme.colorScheme.primary)
                            }
                            if (stockBajo) {
                                Spacer(Modifier.width(6.dp))
                                BocattaBadge(stringResource(R.string.admin_badge_bajo), MaterialTheme.colorScheme.error)
                            }
                        }
                        Text(stringResource(R.string.admin_insumo_minimo, insumo.stockMinimo.toString(), insumo.unidadBase),
                            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    }
                    Text("${"%.1f".format(insumo.cantidadEnBase)} ${insumo.unidadBase}",
                        fontWeight = FontWeight.Black, fontSize = 17.sp,
                        color = if (stockBajo) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}


