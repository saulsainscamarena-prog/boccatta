package com.bocatta.pos.presentation.ui.screens.gastos

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bocatta.pos.domain.model.GastoV2
import com.bocatta.pos.domain.model.InsumoV2
import com.bocatta.pos.presentation.ui.components.*
import com.bocatta.pos.presentation.ui.theme.*
import com.bocatta.pos.presentation.viewmodel.ExpensesViewModelV2
import com.bocatta.pos.presentation.viewmodel.SessionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GastosScreen(
    vm: ExpensesViewModelV2,
    session: SessionViewModel,
    onBack: () -> Unit
) {
    var concepto by remember { mutableStateOf("") }
    var monto by remember { mutableStateOf("") }
    var categoria by remember { mutableStateOf("Insumo") }
    var cantidadSurtida by remember { mutableStateOf("1") }

    // FIX: insumoSeleccionado se limpia correctamente al cambiar categoría manualmente
    var insumoSeleccionado by remember { mutableStateOf<InsumoV2?>(null) }

    val categoriasGasto = listOf("Insumo", "Servicios", "Sueldos", "Renta", "Mantenimiento", "Otros")
    var expandedCat by remember { mutableStateOf(false) }

    val snackbarHost = remember { SnackbarHostState() }
    LaunchedEffect(vm.mensajeExito, vm.mensajeError) {
        val msg = vm.mensajeExito ?: vm.mensajeError ?: return@LaunchedEffect
        snackbarHost.showSnackbar(msg)
        vm.limpiarMensajes()
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHost) },
        topBar = {
            BocattaTopBar(
                title = "Gastos",
                subtitle = session.sucursalActual.uppercase(),
                onBack = onBack
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {

            // ── SELECTOR RÁPIDO DE INSUMOS ─────────────────────────────────
            if (vm.insumosDisponibles.isNotEmpty()) {
                item {
                    BocattaSectionTitle("Insumos rápidos")
                    Spacer(Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(vm.insumosDisponibles) { insumo ->
                            FilterChip(
                                selected = insumoSeleccionado?.id == insumo.id,
                                onClick = {
                                    if (insumoSeleccionado?.id == insumo.id) {
                                        // Deseleccionar
                                        insumoSeleccionado = null
                                        concepto = ""
                                        categoria = "Insumo"
                                    } else {
                                        insumoSeleccionado = insumo
                                        concepto = "Compra de ${insumo.nombre}"
                                        categoria = "Insumo"
                                    }
                                },
                                label = { Text(insumo.nombre, fontSize = 13.sp) },
                                shape = RoundedCornerShape(20.dp)
                            )
                        }
                    }
                }
            }

            // ── FORMULARIO ─────────────────────────────────────────────────
            item {
                BocattaSectionTitle("Registrar operación")
                Spacer(Modifier.height(8.dp))
                ElevatedCard(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {

                        // Cantidad surtida — solo si hay insumo seleccionado
                        if (insumoSeleccionado != null) {
                            OutlinedTextField(
                                value = cantidadSurtida,
                                onValueChange = { cantidadSurtida = it },
                                label = { Text("Cantidad surtida (${insumoSeleccionado!!.unidadBase})") },
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                shape = RoundedCornerShape(14.dp),
                                leadingIcon = {
                                    Icon(Icons.Default.Inventory2, contentDescription = null,
                                        tint = BocattaPrimary, modifier = Modifier.size(20.dp))
                                },
                                supportingText = {
                                    Text("Se sumarán al inventario de ${insumoSeleccionado!!.nombre}")
                                }
                            )
                        }

                        // Categoría — FIX: al cambiar manualmente limpia el insumo
                        ExposedDropdownMenuBox(
                            expanded = expandedCat,
                            onExpandedChange = { expandedCat = it }
                        ) {
                            OutlinedTextField(
                                value = categoria,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Categoría del gasto") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCat) },
                                modifier = Modifier
                                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                                    .fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp)
                            )
                            ExposedDropdownMenu(
                                expanded = expandedCat,
                                onDismissRequest = { expandedCat = false }
                            ) {
                                categoriasGasto.forEach { cat ->
                                    DropdownMenuItem(
                                        text = { Text(cat) },
                                        onClick = {
                                            // FIX CRÍTICO: limpiar insumo si el usuario cambia a categoría no-insumo
                                            if (cat != "Insumo") {
                                                insumoSeleccionado = null
                                                cantidadSurtida = "1"
                                                if (concepto.startsWith("Compra de ")) concepto = ""
                                            }
                                            categoria = cat
                                            expandedCat = false
                                        }
                                    )
                                }
                            }
                        }

                        // Concepto
                        OutlinedTextField(
                            value = concepto,
                            onValueChange = { concepto = it },
                            label = { Text("Concepto / Descripción") },
                            placeholder = { Text("Ej: Reparación freidora, sueldo, gas...") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            leadingIcon = {
                                Icon(Icons.Default.Description, contentDescription = null,
                                    tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(20.dp))
                            }
                        )

                        // Monto
                        OutlinedTextField(
                            value = monto,
                            onValueChange = { monto = it },
                            label = { Text("Monto total pagado") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            prefix = { Text("$  ") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            leadingIcon = {
                                Icon(Icons.Default.AttachMoney, contentDescription = null,
                                    tint = BocattaPrimary, modifier = Modifier.size(20.dp))
                            }
                        )

                        // Botón
                        BocattaButton(
                            texto = if (insumoSeleccionado != null) "Surtir y registrar" else "Registrar gasto",
                            onClick = {
                                vm.registrarGastoIndustrial(
                                    descripcion = concepto,
                                    monto = monto.toDoubleOrNull() ?: 0.0,
                                    categoria = categoria,
                                    sucursal = session.sucursalActual,
                                    usuarioId = session.nombreUsuario,
                                    insumoId = insumoSeleccionado?.id,
                                    cantidadSurtida = cantidadSurtida.toDoubleOrNull() ?: 0.0
                                )
                                // Limpiar formulario completo
                                concepto = ""
                                monto = ""
                                insumoSeleccionado = null
                                cantidadSurtida = "1"
                                categoria = "Insumo"
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !vm.cargando && monto.isNotBlank() && concepto.isNotBlank(),
                            cargando = vm.cargando,
                            icono = if (insumoSeleccionado != null) Icons.Default.AddShoppingCart
                            else Icons.Default.AddCircle
                        )
                    }
                }
            }

            // ── RESUMEN DEL DÍA ────────────────────────────────────────────
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    BocattaMetricCard(
                        titulo = "Total gastos hoy",
                        valor = "$${"%.2f".format(vm.gastos.sumOf { it.monto })}",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.weight(1f)
                    )
                    BocattaMetricCard(
                        titulo = "Registros",
                        valor = vm.gastos.size.toString(),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // ── HISTORIAL ─────────────────────────────────────────────────
            item {
                BocattaSectionTitle("Actividad de hoy")
            }

            if (vm.gastos.isEmpty()) {
                item {
                    BocattaEmptyState(
                        icono = Icons.Default.ReceiptLong,
                        titulo = "Sin registros hoy",
                        descripcion = "Los gastos del día aparecerán aquí",
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else {
                items(vm.gastos.reversed(), key = { it.id }) { gasto ->
                    GastoCard(gasto = gasto, onEliminar = { vm.eliminarGasto(gasto.id) })
                }
            }
        }
    }
}

@Composable
private fun GastoCard(gasto: GastoV2, onEliminar: () -> Unit) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Ícono por categoría
            val (icono, color) = when (gasto.categoria) {
                "Insumo" -> Icons.Default.Inventory2 to BocattaPrimary
                "Servicios" -> Icons.Default.ElectricalServices to Color(0xFF1565C0)
                "Sueldos" -> Icons.Default.Person to Color(0xFF2E7D32)
                "Renta" -> Icons.Default.Home to Color(0xFF6A1B9A)
                "Mantenimiento" -> Icons.Default.Build to Color(0xFFE65100)
                else -> Icons.Default.ShoppingBag to MaterialTheme.colorScheme.outline
            }
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(color.copy(0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icono, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(gasto.descripcion, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                BocattaBadge(gasto.categoria, color)
            }
            Spacer(Modifier.width(8.dp))
            Text(
                "-$${"%.2f".format(gasto.monto)}",
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.error,
                fontSize = 15.sp
            )
            IconButton(onClick = onEliminar, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Delete, contentDescription = "Eliminar gasto",
                    tint = MaterialTheme.colorScheme.outline.copy(0.5f),
                    modifier = Modifier.size(18.dp))
            }
        }
    }
}
