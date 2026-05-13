package com.bocatta.pos.presentation.ui.screens.admin

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.koin.androidx.compose.koinViewModel
import com.bocatta.pos.presentation.viewmodel.PurchasesViewModel
import com.bocatta.pos.domain.model.InsumoV2
import com.bocatta.pos.presentation.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchasesScreen(
    vm: PurchasesViewModel = koinViewModel(),
    onBack: () -> Unit
) {
    var showAddSupplier by remember { mutableStateOf(false) }
    var selectedInsumo by remember { mutableStateOf<InsumoV2?>(null) }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("COMPRAS INDUSTRIALES", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Black, fontSize = 16.sp)
                        Text("Abastecimiento de Bodega Central", color = Color.White.copy(0.7f), fontSize = 10.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "", tint = MaterialTheme.colorScheme.onPrimary) }
                },
                actions = {
                    IconButton(onClick = { showAddSupplier = true }) { Icon(Icons.Default.PersonAdd, "Nuevo Proveedor", tint = MaterialTheme.colorScheme.onPrimary) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.secondary)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            Text("Insumos en Bodega Central", modifier = Modifier.padding(16.dp), fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium)

            LazyVerticalGrid(
                columns = GridCells.Adaptive(170.dp),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(vm.insumos) { insumo ->
                    InsumoPurchaseCard(insumo) { selectedInsumo = insumo }
                }
            }

            selectedInsumo?.let { insumo ->
                RegistroCompraDialog(
                    insumo = insumo,
                    proveedores = vm.proveedores,
                    onDismiss = { selectedInsumo = null },
                    onConfirm = { provId, cant, precio, presentacion ->
                        vm.registrarCompra(insumo.id, provId, cant, precio)
                        selectedInsumo = null
                    }
                )
            }
        }
    }
}

@Composable
fun InsumoPurchaseCard(insumo: InsumoV2, onClick: () -> Unit) {
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(Modifier.size(40.dp).background(MaterialTheme.colorScheme.primary.copy(0.1f), CircleShape), contentAlignment = Alignment.Center) {
                Text(insumo.nombre.take(1).uppercase(), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.height(12.dp))
            Text(insumo.nombre, fontWeight = FontWeight.Bold, maxLines = 1)
            Text("${insumo.unidadBase}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { 0.6f },
                modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.outlineVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistroCompraDialog(
    insumo: InsumoV2,
    proveedores: List<com.bocatta.pos.domain.model.ProveedorV2>,
    onDismiss: () -> Unit,
    onConfirm: (String, Double, Double, String) -> Unit
) {
    var cantidad by remember { mutableStateOf("") }
    var precio by remember { mutableStateOf("") }
    var provSeleccionado by remember { mutableStateOf(if (proveedores.isNotEmpty()) proveedores[0].id else "") }
    var presentacionExpandida by remember { mutableStateOf(false) }
    var presentacionSeleccionada by remember { mutableStateOf<com.bocatta.pos.domain.model.PresentacionInsumo?>(insumo.presentaciones.firstOrNull()) }

    val tienePresentaciones = insumo.presentaciones.isNotEmpty()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Abastecer: ${insumo.nombre}", fontWeight = FontWeight.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (tienePresentaciones) {
                    Text("Presentaci�n:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { presentacionExpandida = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("${presentacionSeleccionada?.nombre ?: ""} (${presentacionSeleccionada?.cantidadDisponible ?: 0.0} ${presentacionSeleccionada?.unidadEquivalente ?: ""})")
                        }
                        DropdownMenu(
                            expanded = presentacionExpandida,
                            onDismissRequest = { presentacionExpandida = false }
                        ) {
                            insumo.presentaciones.forEach { pres ->
                                DropdownMenuItem(
                                    text = { Text("${pres.nombre} (x${pres.factorConversionABase} ${insumo.unidadBase}) - $${"%.2f".format(pres.ultimoPrecioPagado)}") },
                                    onClick = {
                                        presentacionSeleccionada = pres
                                        presentacionExpandida = false
                                    }
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = cantidad,
                    onValueChange = { cantidad = it },
                    label = { Text("Cantidad de ${if (tienePresentaciones) (presentacionSeleccionada?.nombre ?: "") else insumo.unidadBase}") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                
                OutlinedTextField(
                    value = precio,
                    onValueChange = { precio = it },
                    label = { Text("Costo Total Facturado") },
                    prefix = { Text("$") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                
                Text("Proveedor:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(proveedores) { p ->
                        FilterChip(selected = provSeleccionado == p.id, onClick = { provSeleccionado = p.id }, label = { Text(p.nombre) })
                    }
                }

                if (precio.toDoubleOrNull() != null && cantidad.toDoubleOrNull() != null) {
                    val totalUnidades = cantidad.toDouble() * (presentacionSeleccionada?.factorConversionABase ?: 1.0)
                    val unitario = precio.toDouble() / totalUnidades
                    val diff = unitario - insumo.costoUnitarioBase
                    Surface(color = if (diff > 0) MaterialTheme.colorScheme.error.copy(0.1f) else MaterialTheme.colorScheme.primary.copy(0.1f), shape = RoundedCornerShape(8.dp)) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(
                                "Unitario: $${"%.2f".format(unitario)}/${insumo.unidadBase} (Ref: $${insumo.costoUnitarioBase})",
                                color = if (diff > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                fontSize = 11.sp, fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Total: ${"%.1f".format(totalUnidades)} ${insumo.unidadBase}",
                                fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = { 
            Button(
                onClick = { 
                    val cant = cantidad.toDoubleOrNull() ?: 0.0
                    val precioTotal = precio.toDoubleOrNull() ?: 0.0
                    val cantidadBase = cant * (presentacionSeleccionada?.factorConversionABase ?: 1.0)
                    onConfirm(provSeleccionado, cantidadBase, precioTotal, presentacionSeleccionada?.nombre ?: "")
                },
                shape = RoundedCornerShape(12.dp)
            ) { Text("GUARDAR REGISTRO") } 
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCELAR") } }
    )
}

