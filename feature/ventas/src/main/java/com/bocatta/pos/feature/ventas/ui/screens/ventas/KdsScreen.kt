package com.bocatta.pos.feature.ventas.ui.screens.ventas

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.koin.androidx.compose.koinViewModel
import com.bocatta.pos.feature.ventas.viewmodel.KdsViewModel
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import com.bocatta.pos.domain.model.VentaV2

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KdsScreen(
    sucursal: String,
    viewModel: KdsViewModel = koinViewModel()
) {
    androidx.compose.runtime.LaunchedEffect(sucursal) {
        viewModel.loadOrders(sucursal)
    }

    val uiState by viewModel.uiState.collectAsState()

    var currentTime by androidx.compose.runtime.remember { androidx.compose.runtime.mutableLongStateOf(System.currentTimeMillis()) }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        while(true) {
            kotlinx.coroutines.delay(60_000L)
            currentTime = System.currentTimeMillis()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Kitchen Display System (KDS)") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (uiState.error != null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "Error: ${uiState.error}", color = MaterialTheme.colorScheme.error)
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Columna Pendientes
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Pendientes (${uiState.pendingOrders.size})",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 250.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(uiState.pendingOrders) { order ->
                            KdsOrderCard(
                                order = order,
                                currentTime = currentTime,
                                primaryActionText = "Preparar",
                                primaryActionIcon = Icons.Default.PlayArrow
                            ) {
                                viewModel.markAsPreparing(order.id)
                            }
                        }
                    }
                }

                Divider(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(1.dp)
                )

                // Columna En Preparación
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "En Preparación (${uiState.preparingOrders.size})",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 250.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(uiState.preparingOrders) { order ->
                            KdsOrderCard(
                                order = order,
                                currentTime = currentTime,
                                primaryActionText = "Listo",
                                primaryActionIcon = Icons.Default.Check
                            ) {
                                viewModel.markAsReady(order.id)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun KdsOrderCard(
    order: VentaV2,
    currentTime: Long,
    primaryActionText: String,
    primaryActionIcon: androidx.compose.ui.graphics.vector.ImageVector,
    onAction: () -> Unit
) {
    val tiempoTranscurridoMinutos = (currentTime - order.fecha) / 60000

    val isRed = tiempoTranscurridoMinutos >= 10
    val isYellow = tiempoTranscurridoMinutos >= 5

    // Semáforo de tiempos usando tokens M3
    val alertColor = when {
        isRed -> MaterialTheme.colorScheme.errorContainer
        isYellow -> MaterialTheme.colorScheme.tertiaryContainer
        else -> MaterialTheme.colorScheme.surface
    }
    
    val borderColor = when {
        isRed -> MaterialTheme.colorScheme.error
        isYellow -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.outlineVariant
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = alertColor),
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Orden #${order.numeroTicket}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${tiempoTranscurridoMinutos}m",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isRed) MaterialTheme.colorScheme.error else if (isYellow) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            val cliente = if (!order.clienteId.isNullOrEmpty()) "Cliente: ${order.clienteId}" else "Cliente: General"
            Text(
                text = cliente,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
            )
            
            Divider()

            // Lista de items de la orden
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                order.productos.forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${item.cantidad}x ${item.nombre}",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                    val extras = mutableListOf<String>()
                    item.base?.let { extras.add("Base: $it") }
                    if (item.aderezos.isNotEmpty()) extras.add("Aderezos: ${item.aderezos.joinToString(", ")}")
                    if (item.toppings.isNotEmpty()) extras.add("Toppings: ${item.toppings.joinToString(", ")}")
                    
                    if (extras.isNotEmpty()) {
                        Text(
                            text = extras.joinToString(" | "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }
                }
            }
            
            if (order.notaOrden.isNotEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    Text(
                        text = "Nota: ${order.notaOrden}",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onAction,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(imageVector = primaryActionIcon, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(primaryActionText)
            }
        }
    }
}
