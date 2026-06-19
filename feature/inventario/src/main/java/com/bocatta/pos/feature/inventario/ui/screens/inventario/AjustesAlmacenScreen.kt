package com.bocatta.pos.feature.inventario.ui.screens.inventario

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
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
import com.bocatta.pos.domain.model.InsumoV2
import com.bocatta.pos.presentation.ui.components.NeonButton
import com.bocatta.pos.presentation.ui.components.StatusBadgePremium
import com.bocatta.pos.presentation.ui.theme.BocattaDesign
import com.bocatta.pos.feature.inventario.viewmodel.InventoryViewModel
import com.bocatta.pos.feature.auth.viewmodel.SessionViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AjustesAlmacenScreen(
    vm: InventoryViewModel,
    session: SessionViewModel,
    onBack: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var insumoAjustarMerma by remember { mutableStateOf<InsumoV2?>(null) }
    var insumoRegistrarEntrada by remember { mutableStateOf<InsumoV2?>(null) }

    LaunchedEffect(Unit) {
        vm.configurarSucursal(session.sucursalActual)
    }

    LaunchedEffect(vm.mensajeExito) {
        vm.mensajeExito?.let {
            snackbarHostState.showSnackbar(it)
            vm.mensajeExito = null
        }
    }
    LaunchedEffect(vm.mensajeError) {
        vm.mensajeError?.let {
            snackbarHostState.showSnackbar(it)
            vm.mensajeError = null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(MaterialTheme.colorScheme.background, MaterialTheme.colorScheme.surfaceContainerLow)))
    ) {
        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = Color.Transparent,
            topBar = {
                LargeTopAppBar(
                    title = {
                        Column {
                            Text(
                                "AJUSTES RÁPIDOS DE ALMACÉN",
                                fontWeight = FontWeight.Black,
                                fontSize = 24.sp,
                                letterSpacing = 1.5.sp,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                "COMPRAS RÁPIDAS Y CONTROL DE MERMAS · ${session.sucursalActual.uppercase(java.util.Locale.getDefault())}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                shape = CircleShape,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    "Volver",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = { vm.configurarSucursal(session.sucursalActual) }) {
                            Icon(Icons.Default.Refresh, "Actualizar", tint = MaterialTheme.colorScheme.primary)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.onBackground
                    )
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
            ) {
                // Barra de Búsqueda
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Buscar insumo...") },
                    leadingIcon = { Icon(Icons.Default.Search, "Buscar") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )

                if (vm.cargando) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                } else {
                    val insumosFiltrados = vm.maestroInsumos.values.filter {
                        searchQuery.isBlank() || it.nombre.contains(searchQuery, ignoreCase = true)
                    }.sortedBy { it.nombre }

                    if (insumosFiltrados.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No se encontraron insumos", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(320.dp),
                            contentPadding = PaddingValues(24.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(insumosFiltrados, key = { it.id }) { insumo ->
                                val stockActual = vm.stockInsumos[insumo.id] ?: 0.0
                                val stockMinimo = insumo.stockMinimo
                                val agotado = stockActual <= 0.0
                                val bajoStock = stockActual <= stockMinimo
                                val colorEstado = BocattaDesign.getStockColor(stockActual, stockMinimo)

                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceContainer,
                                    shape = RoundedCornerShape(24.dp),
                                    border = BorderStroke(
                                        1.dp,
                                        if (bajoStock) colorEstado.copy(0.3f) else MaterialTheme.colorScheme.outlineVariant
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(Modifier.padding(16.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Surface(
                                                color = if (bajoStock) colorEstado.copy(0.1f) else MaterialTheme.colorScheme.primary.copy(0.1f),
                                                shape = CircleShape,
                                                modifier = Modifier.size(48.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        if (bajoStock) Icons.Default.Warning else Icons.Default.Inventory,
                                                        null,
                                                        tint = if (bajoStock) colorEstado else MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(24.dp)
                                                    )
                                                }
                                            }
                                            Spacer(Modifier.width(16.dp))
                                            Column(Modifier.weight(1f)) {
                                                Text(
                                                    insumo.nombre,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 15.sp,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    "${"%.2f".format(stockActual)} ${insumo.unidadBase}".uppercase(java.util.Locale.getDefault()),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = if (bajoStock) colorEstado else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            if (bajoStock) {
                                                StatusBadgePremium(if (agotado) "AGOTADO" else "BAJO", colorEstado)
                                            }
                                        }
                                        Spacer(Modifier.height(16.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            // Botón Merma / Agotado
                                            Button(
                                                onClick = { insumoAjustarMerma = insumo },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(0.6f),
                                                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                                                ),
                                                shape = RoundedCornerShape(12.dp),
                                                modifier = Modifier.weight(1f),
                                                contentPadding = PaddingValues(vertical = 8.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.DeleteSweep,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(Modifier.width(4.dp))
                                                Text(
                                                    if (agotado) "REGISTRAR MERMA" else "AGOTAR STOCK",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }

                                            // Botón Entrada rápida
                                            Button(
                                                onClick = { insumoRegistrarEntrada = insumo },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = MaterialTheme.colorScheme.primary,
                                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                                ),
                                                shape = RoundedCornerShape(12.dp),
                                                modifier = Modifier.weight(1f),
                                                contentPadding = PaddingValues(vertical = 8.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Add,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(Modifier.width(4.dp))
                                                Text(
                                                    "+ ENTRADA RÁPIDA",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
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

        // Diálogo para Registrar Merma
        insumoAjustarMerma?.let { insumo ->
            val stockActual = vm.stockInsumos[insumo.id] ?: 0.0
            var cantidadMerma by remember { mutableStateOf(stockActual.toString()) }
            var motivoMerma by remember { mutableStateOf("Agotamiento de mostrador") }
            var guardando by remember { mutableStateOf(false) }

            AlertDialog(
                onDismissRequest = { insumoAjustarMerma = null },
                title = { Text("REGISTRAR AJUSTE / MERMA", fontWeight = FontWeight.Bold) },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Insumo: ${insumo.nombre}",
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "Stock actual reportado: ${"%.2f".format(stockActual)} ${insumo.unidadBase}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedTextField(
                            value = cantidadMerma,
                            onValueChange = { cantidadMerma = it },
                            label = { Text("Cantidad a descontar (${insumo.unidadBase})") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = motivoMerma,
                            onValueChange = { motivoMerma = it },
                            label = { Text("Motivo / Comentario") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val cant = cantidadMerma.toDoubleOrNull()
                            if (cant != null && cant > 0.0) {
                                guardando = true
                                vm.registrarMermaManual(
                                    insumoId = insumo.id,
                                    cantidad = cant,
                                    motivo = motivoMerma.trim(),
                                    usuarioId = session.uid
                                ) { exito ->
                                    guardando = false
                                    if (exito) {
                                        insumoAjustarMerma = null
                                    }
                                }
                            }
                        },
                        enabled = !guardando && cantidadMerma.toDoubleOrNull() != null && motivoMerma.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("REGISTRAR")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { insumoAjustarMerma = null }, enabled = !guardando) {
                        Text("Cancelar")
                    }
                },
                shape = RoundedCornerShape(20.dp)
            )
        }

        // Diálogo para Entrada Rápida (Compra)
        insumoRegistrarEntrada?.let { insumo ->
            var presentacionSeleccionada by remember { mutableStateOf("Pieza") }
            var cantidadComprada by remember { mutableStateOf("1.0") }
            var contenidoBase by remember { mutableStateOf(if (insumo.unidadBase == "g") "1000.0" else "1.0") }
            var costoTotal by remember { mutableStateOf("") }
            var guardando by remember { mutableStateOf(false) }

            AlertDialog(
                onDismissRequest = { insumoRegistrarEntrada = null },
                title = { Text("REGISTRAR COMPRA RÁPIDA", fontWeight = FontWeight.Bold) },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Insumo: ${insumo.nombre}",
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        OutlinedTextField(
                            value = presentacionSeleccionada,
                            onValueChange = { presentacionSeleccionada = it },
                            label = { Text("Presentación (e.g. Caja, Kilo, Bote)") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = cantidadComprada,
                            onValueChange = { cantidadComprada = it },
                            label = { Text("Cantidad de paquetes/piezas compradas") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = contenidoBase,
                            onValueChange = { contenidoBase = it },
                            label = { Text("Contenido por paquete (${insumo.unidadBase})") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = costoTotal,
                            onValueChange = { costoTotal = it },
                            label = { Text("Costo Total de la Compra ($)") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                },
                confirmButton = {
                    val cantComp = cantidadComprada.toDoubleOrNull()
                    val contBase = contenidoBase.toDoubleOrNull()
                    val costo = costoTotal.toDoubleOrNull()

                    Button(
                        onClick = {
                            if (cantComp != null && contBase != null && costo != null && cantComp > 0.0 && contBase > 0.0 && costo >= 0.0) {
                                guardando = true
                                vm.registrarCompraConPresentacion(
                                    insumoId = insumo.id,
                                    presentacionNombre = presentacionSeleccionada.trim(),
                                    cantidad = cantComp,
                                    contenidoEquivalente = contBase,
                                    costoTotal = costo,
                                    usuarioId = session.uid
                                ) { exito ->
                                    guardando = false
                                    if (exito) {
                                        insumoRegistrarEntrada = null
                                    }
                                }
                            }
                        },
                        enabled = !guardando && validerRegistroCompra(cantidadComprada, contenidoBase, costoTotal),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("REGISTRAR COMPRA")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { insumoRegistrarEntrada = null }, enabled = !guardando) {
                        Text("Cancelar")
                    }
                },
                shape = RoundedCornerShape(20.dp)
            )
        }
    }
}

private fun validerRegistroCompra(cant: String, cont: String, costo: String): Boolean {
    val c = cant.toDoubleOrNull() ?: return false
    val ct = cont.toDoubleOrNull() ?: return false
    val cos = costo.toDoubleOrNull() ?: return false
    return c > 0.0 && ct > 0.0 && cos >= 0.0
}
