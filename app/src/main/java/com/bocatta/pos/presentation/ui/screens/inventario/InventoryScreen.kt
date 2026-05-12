package com.bocatta.pos.presentation.ui.screens.inventario

import androidx.compose.animation.*
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

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent,
        topBar = {
            LargeTopAppBar(
                title = { 
                    Column {
                        Text("INVENTARIO", fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                        Text(session.sucursalActual.uppercase(), style = MaterialTheme.typography.labelSmall, color = BocattaNeonCyan)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) }
                },
                actions = {
                    IconButton(onClick = onAperturaInventario) { Icon(Icons.Default.Login, null) }
                    IconButton(onClick = onCierreInventario) { Icon(Icons.Default.Logout, null) }
                    IconButton(onClick = { vm.configurarSucursal(session.sucursalActual) }) { Icon(Icons.Default.Refresh, null) }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(containerColor = Color.Transparent)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { mostrarRegistroProduccion = true },
                containerColor = BocattaNeonCyan,
                contentColor = BocattaBgDark,
                shape = RoundedCornerShape(16.dp),
                icon = { Icon(Icons.Default.Add, null) },
                text = { Text("REGISTRAR PRODUCCIÓN", fontWeight = FontWeight.Black) }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(BocattaBgDark, BocattaSurfaceDark))
        )) {
            if (vm.cargando) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = BocattaNeonCyan)
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(280.dp),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(padding).fillMaxSize()
                ) {
                    items(vm.stockInsumos.keys.toList()) { id ->
                        val cant = vm.stockInsumos[id] ?: 0.0
                        InventoryCardPremium(
                            nombre = id.replace("_", " ").uppercase(),
                            cantidad = cant,
                            unidad = if(id.contains("masa")) "porciones" else "unidades",
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

    Surface(
        color = Color.White.copy(0.05f),
        shape = RoundedCornerShape(28.dp), // Meridian Spec: 28dp
        border = androidx.compose.foundation.BorderStroke(1.dp, if(bajoStock) BocattaDanger.copy(0.5f) else Color.White.copy(0.1f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically) { // 8dp grid: 24dp padding
            Surface(
                color = if(bajoStock) BocattaDanger.copy(0.1f) else BocattaNeonCyan.copy(0.1f),
                shape = CircleShape,
                modifier = Modifier.size(56.dp) // Industrial scale
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        if(bajoStock) Icons.Default.Warning else Icons.Default.Inventory, 
                        null, 
                        tint = if(bajoStock) BocattaDanger else BocattaNeonCyan,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            Spacer(Modifier.width(20.dp))
            Column(Modifier.weight(1f)) {
                Text(nombre.uppercase(), fontWeight = FontWeight.Black, fontSize = 16.sp, color = Color.White, letterSpacing = 1.sp)
                Text("$cantidad $unidad".uppercase(), style = MaterialTheme.typography.labelSmall, color = if(bajoStock) BocattaDanger else Color.White.copy(0.5f), fontWeight = FontWeight.Bold)
            }
            if (bajoStock) {
                StatusBadgePremium("CRÍTICO", BocattaDanger)
            }
        }
    }
}

