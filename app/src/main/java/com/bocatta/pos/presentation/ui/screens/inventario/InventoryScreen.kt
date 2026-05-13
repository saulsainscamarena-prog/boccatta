package com.bocatta.pos.presentation.ui.screens.inventario

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bocatta.pos.presentation.ui.components.*
import com.bocatta.pos.presentation.ui.theme.*
import com.bocatta.pos.presentation.viewmodel.InventoryViewModel
import com.bocatta.pos.presentation.viewmodel.SessionViewModel

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

    Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(BocattaBgDark, Color(0xFF10121A))))) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = Color.Transparent,
            topBar = {
                LargeTopAppBar(
                    title = { 
                        Column {
                            Text("GESTIÓN DE INVENTARIO", fontWeight = FontWeight.Black, fontSize = 26.sp, letterSpacing = 2.sp, color = Color.White)
                            Text("MONITOREO DE MATERIA PRIMA · ${session.sucursalActual.uppercase()}", style = MaterialTheme.typography.labelSmall, color = BocattaNeonCyan, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) { 
                            Surface(color = Color.White.copy(0.05f), shape = CircleShape, modifier = Modifier.size(40.dp)) {
                                Icon(Icons.Default.ArrowBack, null, tint = Color.White, modifier = Modifier.padding(10.dp))
                            }
                        }
                    },
                    actions = {
                        if (session.esAdmin) {
                            var mostrarPin by remember { mutableStateOf(false) }
                            IconButton(onClick = { mostrarPin = true }) { 
                                Icon(Icons.Default.FlashOn, null, tint = BocattaNeonMagenta) 
                            }
                            if (mostrarPin) {
                                com.bocatta.pos.presentation.ui.components.AdminPinDialog(
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
                        IconButton(onClick = onAperturaInventario) { Icon(Icons.Default.Login, null, tint = Color.White.copy(0.7f)) }
                        IconButton(onClick = onCierreInventario) { Icon(Icons.Default.Logout, null, tint = Color.White.copy(0.7f)) }
                        IconButton(onClick = { vm.configurarSucursal(session.sucursalActual) }) { Icon(Icons.Default.Refresh, null, tint = BocattaNeonCyan) }
                    },
                    colors = TopAppBarDefaults.largeTopAppBarColors(containerColor = Color.Transparent, titleContentColor = Color.White)
                )
            },
            floatingActionButton = {
                NeonButton(
                    texto = "REGISTRAR PRODUCCIÓN",
                    onClick = { mostrarRegistroProduccion = true },
                    modifier = Modifier.padding(16.dp),
                    color = BocattaNeonCyan
                )
            }
        ) { padding ->
            Column(modifier = Modifier.padding(padding).fillMaxSize()) {
                if (vm.cargando) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = BocattaNeonCyan)
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(300.dp),
                        contentPadding = PaddingValues(24.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(vm.stockInsumos.keys.toList()) { id ->
                            val cant = vm.stockInsumos[id] ?: 0.0
                            InventoryCardPremium(
                                nombre = id.replace("_", " ").uppercase(),
                                cantidad = cant,
                                unidad = if(id.contains("masa") || id.contains("helado")) "unidades/lt" else "unidades",
                                bajoStock = cant < 10.0
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
                onDismiss = { mostrarRegistroProduccion = false }
            )
        }
    }
}

@Composable
fun InventoryCardPremium(nombre: String, cantidad: Double, unidad: String, bajoStock: Boolean) {
    Surface(
        color = BocattaSurfaceDark,
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(1.dp, if(bajoStock) BocattaDanger.copy(0.4f) else Color.White.copy(0.08f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                color = if(bajoStock) BocattaDanger.copy(0.1f) else BocattaNeonCyan.copy(0.1f),
                shape = CircleShape,
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        if(bajoStock) Icons.Default.Warning else Icons.Default.Inventory2, 
                        null, 
                        tint = if(bajoStock) BocattaDanger else BocattaNeonCyan,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            Spacer(Modifier.width(20.dp))
            Column(Modifier.weight(1f)) {
                Text(nombre, fontWeight = FontWeight.Black, fontSize = 14.sp, color = Color.White, letterSpacing = 1.sp)
                Text("${"%.1f".format(cantidad)} $unidad".uppercase(), style = MaterialTheme.typography.labelSmall, color = if(bajoStock) BocattaDanger else Color.White.copy(0.5f), fontWeight = FontWeight.Bold)
            }
            if (bajoStock) {
                StatusBadgePremium("ALERTA", BocattaDanger)
            }
        }
    }
}

