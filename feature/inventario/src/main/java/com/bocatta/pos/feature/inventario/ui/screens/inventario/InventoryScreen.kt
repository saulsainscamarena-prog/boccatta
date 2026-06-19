package com.bocatta.pos.feature.inventario.ui.screens.inventario

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bocatta.pos.presentation.ui.components.*
import com.bocatta.pos.feature.inventario.ui.components.ProductionRegistrationDialog
import com.bocatta.pos.presentation.ui.theme.*
import com.bocatta.pos.feature.inventario.viewmodel.InventoryViewModel
import com.bocatta.pos.feature.auth.viewmodel.SessionViewModel
import com.bocatta.pos.core.ui.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    vm: InventoryViewModel,
    session: SessionViewModel,
    onBack: () -> Unit,
    onCierreInventario: () -> Unit,
    onAperturaInventario: () -> Unit
) {
    var mostrarRegistroProduccion by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

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
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surfaceContainerLow
                    )
                )
            )
    ) {
        Scaffold(contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = Color.Transparent,
            topBar = {
                LargeTopAppBar(
                    title = {
                        Column {
                            Text(
                                stringResource(R.string.inventory_title),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                stringResource(
                                    R.string.inventory_subtitle,
                                    session.sucursalActual.uppercase(java.util.Locale.getDefault())
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
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
                                    stringResource(R.string.inventory_back),
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                    },
                    actions = {
                        if (session.esAdmin) {
                            var mostrarPin by remember { mutableStateOf(false) }
                            IconButton(onClick = { mostrarPin = true }) {
                                Icon(
                                    Icons.Default.FlashOn,
                                    stringResource(R.string.inventory_emergency_stock),
                                    tint = MaterialTheme.bocattaSemanticColors.warning
                                )
                            }
                            if (mostrarPin) {
                                com.bocatta.pos.core.ui.components.AdminPinDialog(
                                    onDismiss = { mostrarPin = false },
                                    onConfirm = { pin ->
                                        session.validarPinAdmin(pin) { valido ->
                                            if (valido) {
                                                vm.cargarStockEmergencia(session.sucursalActual)
                                                mostrarPin = false
                                            }
                                        }
                                    }
                                )
                            }
                        }
                        IconButton(onClick = onAperturaInventario) {
                            Icon(
                                Icons.AutoMirrored.Filled.Login,
                                stringResource(R.string.inventory_opening),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = onCierreInventario) {
                            Icon(
                                Icons.AutoMirrored.Filled.Logout,
                                stringResource(R.string.inventory_closing),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { vm.configurarSucursal(session.sucursalActual) }) {
                            Icon(
                                Icons.Default.Refresh,
                                stringResource(R.string.inventory_refresh),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.onBackground
                    )
                )
            },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = { mostrarRegistroProduccion = true },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text(stringResource(R.string.inventory_register_production)) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            }
        ) { padding ->
            Column(modifier = Modifier.padding(padding).fillMaxSize()) {
                if (vm.cargando) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(300.dp),
                        contentPadding = PaddingValues(24.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(vm.stockInsumos.keys.toList(), key = { it }) { id ->
                            val cant = vm.stockInsumos[id] ?: 0.0
                            val insumo = vm.maestroInsumos[id]
                            val stockMinimo = insumo?.stockMinimo ?: 10.0
                            InventoryCardPremium(
                                nombre = insumo?.nombre ?: id.replace("_", " ").uppercase(java.util.Locale.getDefault()),
                                cantidad = cant,
                                unidad = insumo?.unidadBase ?: if(id.contains("masa") || id.contains("helado")) "unidades/lt" else "unidades",
                                stockMinimo = stockMinimo
                            )
                        }
                    }
                }
            }
        }

        if (mostrarRegistroProduccion) {
            ProductionRegistrationDialog(
                vm = vm,
                sucursal = session.sucursalActual,
                usuarioId = session.uid,
                onDismiss = { mostrarRegistroProduccion = false }
            )
        }
    }
}

@Composable
fun InventoryCardPremium(nombre: String, cantidad: Double, unidad: String, stockMinimo: Double) {
    val agotado = cantidad <= 0.0
    val bajoStock = cantidad <= stockMinimo
    val semanticColors = MaterialTheme.bocattaSemanticColors
    val colorEstado = when {
        agotado -> MaterialTheme.colorScheme.error
        bajoStock -> semanticColors.warning
        else -> semanticColors.success
    }
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(
            1.dp,
            if (bajoStock) colorEstado.copy(0.5f) else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                color = if(bajoStock) colorEstado.copy(0.1f) else MaterialTheme.colorScheme.primary.copy(0.1f),
                shape = CircleShape,
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        if(bajoStock) Icons.Default.Warning else Icons.Default.Inventory2,
                        null,
                        tint = if(bajoStock) colorEstado else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            Spacer(Modifier.width(20.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    nombre,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "${"%.1f".format(cantidad)} $unidad".uppercase(java.util.Locale.getDefault()),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (bajoStock) colorEstado else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    stringResource(R.string.inventory_minimum, "%.1f".format(stockMinimo), unidad),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (bajoStock) {
                StatusBadgePremium(
                    if (agotado) stringResource(R.string.inventory_out_of_stock)
                    else stringResource(R.string.inventory_low_stock),
                    colorEstado
                )
            }
        }
    }
}
