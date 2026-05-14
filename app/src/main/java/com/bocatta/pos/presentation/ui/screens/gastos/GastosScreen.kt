package com.bocatta.pos.presentation.ui.screens.gastos

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
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
    var insumoSeleccionado by remember { mutableStateOf<InsumoV2?>(null) }
    var expandedCat by remember { mutableStateOf(false) }

    val categoriasGasto = listOf("Insumo", "Servicios", "Sueldos", "Renta", "Mantenimiento", "Otros")
    val snackbarHost = remember { SnackbarHostState() }

    LaunchedEffect(vm.mensajeExito, vm.mensajeError) {
        val msg = vm.mensajeExito ?: vm.mensajeError ?: return@LaunchedEffect
        snackbarHost.showSnackbar(msg)
        vm.limpiarMensajes()
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHost) },
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text("GASTOS Y OPERACIÓN", fontWeight = FontWeight.Black, fontSize = 24.sp, letterSpacing = 2.sp, color = Color.White)
                        Text("REGISTRO DE SALIDAS · ${session.sucursalActual.uppercase()}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    }
                },
                navigationIcon = { 
                    IconButton(onClick = onBack) { 
                        Surface(color = Color.White.copy(0.05f), shape = CircleShape, modifier = Modifier.size(40.dp)) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White, modifier = Modifier.padding(10.dp)) 
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent, titleContentColor = Color.White)
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(MaterialTheme.colorScheme.background, Color(0xFF10121A))))) {
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize().padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                contentPadding = PaddingValues(bottom = 32.dp, top = 16.dp)
            ) {
                if (vm.insumosDisponibles.isNotEmpty()) {
                    item {
                        Text("INSUMOS FRECUENTES", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color.White.copy(0.4f), letterSpacing = 1.sp)
                        Spacer(Modifier.height(12.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(vm.insumosDisponibles) { insumo ->
                                val selected = insumoSeleccionado?.id == insumo.id
                                FilterChip(
                                    selected = selected,
                                    onClick = {
                                        if (selected) {
                                            insumoSeleccionado = null
                                            concepto = ""
                                            categoria = "Insumo"
                                        } else {
                                            insumoSeleccionado = insumo
                                            concepto = "Compra de ${insumo.nombre}"
                                            categoria = "Insumo"
                                        }
                                    },
                                    label = { Text(insumo.nombre.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Black) },
                                    shape = RoundedCornerShape(20.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.background,
                                        containerColor = Color.White.copy(0.05f),
                                        labelColor = Color.White.copy(0.6f)
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = selected,
                                        borderColor = Color.White.copy(0.1f),
                                        selectedBorderColor = MaterialTheme.colorScheme.primary
                                    )
                                )
                            }
                        }
                    }
                }

                item {
                    Text("NUEVA ENTRADA DE GASTO", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color.White.copy(0.4f), letterSpacing = 1.sp)
                    Spacer(Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(28.dp),
                        color = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
                        border = BorderStroke(1.dp, Color.White.copy(0.1f))
                    ) {
                        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            if (insumoSeleccionado != null) {
                                OutlinedTextField(
                                    value = cantidadSurtida,
                                    onValueChange = { cantidadSurtida = it },
                                    label = { Text("Cantidad surtida (${insumoSeleccionado!!.unidadBase})") },
                                    modifier = Modifier.fillMaxWidth(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    shape = RoundedCornerShape(14.dp),
                                    leadingIcon = { Icon(Icons.Default.Inventory2, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp)) }
                                )
                            }

                            ExposedDropdownMenuBox(expanded = expandedCat, onExpandedChange = { expandedCat = it }) {
                                OutlinedTextField(
                                    value = categoria,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Categoría del gasto") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCat) },
                                    modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp)
                                )
                                ExposedDropdownMenu(expanded = expandedCat, onDismissRequest = { expandedCat = false }) {
                                    categoriasGasto.forEach { cat ->
                                        DropdownMenuItem(
                                            text = { Text(cat) },
                                            onClick = {
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

                            OutlinedTextField(
                                value = concepto,
                                onValueChange = { concepto = it },
                                label = { Text("Concepto / Descripción") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp)
                            )

                            OutlinedTextField(
                                value = monto,
                                onValueChange = { monto = it },
                                label = { Text("Monto total pagado") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                prefix = { Text("$  ") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                            )

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
                                    concepto = ""
                                    monto = ""
                                    insumoSeleccionado = null
                                    cantidadSurtida = "1"
                                    categoria = "Insumo"
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !vm.cargando && monto.isNotBlank() && concepto.isNotBlank(),
                                cargando = vm.cargando,
                                icono = if (insumoSeleccionado != null) Icons.Default.AddShoppingCart else Icons.Default.AddCircle
                            )
                        }
                    }
                }

                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        BocattaMetricCardPremium(
                            titulo = "SALIDAS HOY",
                            valor = "$${"%.2f".format(vm.gastos.sumOf { it.monto })}",
                            color = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.weight(1f)
                        )
                        BocattaMetricCardPremium(
                            titulo = "REGISTROS",
                            valor = vm.gastos.size.toString(),
                            color = Color.White,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item { BocattaSectionTitle("Actividad de hoy") }

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
                        GastoItemRow(gasto = gasto, onEliminar = { vm.eliminarGasto(gasto.id) })
                    }
                }
            }
        }
    }
}

@Composable
fun GastoItemRow(gasto: GastoV2, onEliminar: () -> Unit) {
    val (icono, color) = when (gasto.categoria) {
        "Insumo" -> Icons.Default.Inventory2 to MaterialTheme.colorScheme.primary
        "Servicios" -> Icons.Default.ElectricalServices to Color(0xFF4FC3F7)
        "Sueldos" -> Icons.Default.Person to Color(0xFF81C784)
        "Renta" -> Icons.Default.Home to Color(0xFFBA68C8)
        "Mantenimiento" -> Icons.Default.Build to Color(0xFFFFB74D)
        else -> Icons.Default.ShoppingBag to Color.White.copy(0.6f)
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
        border = BorderStroke(1.dp, color.copy(0.3f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(44.dp).background(color.copy(0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icono, null, tint = color, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(gasto.descripcion.uppercase(), fontWeight = FontWeight.Black, fontSize = 13.sp, maxLines = 1, color = Color.White)
                StatusBadgePremium(gasto.categoria.uppercase(), color)
            }
            Text("-$${"%.2f".format(gasto.monto)}", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.tertiary, fontSize = 16.sp)
            IconButton(onClick = onEliminar) {
                Icon(Icons.Default.Delete, null, tint = Color.White.copy(0.2f))
            }
        }
    }
}
