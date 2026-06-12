package com.bocatta.pos.feature.inventario.ui.screens.bodega

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bocatta.pos.feature.inventario.viewmodel.DashboardBodegaViewModel
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardBodegaScreen(
    onBack: () -> Unit,
    onSync: () -> Unit,
    onReportes: () -> Unit
) {
    val vm: DashboardBodegaViewModel = koinViewModel()
    var insumoExpandido by remember { mutableStateOf<String?>(null) }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("DASHBOARD BODEGA", fontWeight = FontWeight.Black, fontSize = 16.sp) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        if (vm.cargando) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text("Ultima actualizacion: ${vm.ultimaActualizacion}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                            Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("${vm.totalInsumos}", fontWeight = FontWeight.Black, fontSize = 24.sp, color = MaterialTheme.colorScheme.primary)
                                Text("Insumos", fontSize = 11.sp)
                            }
                        }
                        Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                            Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    "${vm.promedioStock.toInt()}%",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 24.sp,
                                    color = if (vm.promedioStock >= 50) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                )
                                Text("Stock Prom", fontSize = 11.sp)
                            }
                        }
                        Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                            Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    "${vm.cantidadCriticos}",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 24.sp,
                                    color = if (vm.cantidadCriticos > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                )
                                Text("Criticos", fontSize = 11.sp)
                            }
                        }
                    }
                }

                if (vm.cantidadCriticos > 0) {
                    item { Text("CRITICO", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error) }

                    items(vm.insumos.filter { it.esCritico }) { insumo ->
                        ElevatedCard(
                            onClick = { insumoExpandido = if (insumoExpandido == insumo.id) null else insumo.id },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(insumo.id.replace("_", " ").replaceFirstChar { it.uppercase(java.util.Locale.getDefault()) }, fontWeight = FontWeight.Bold)
                                        Text("${insumo.diasRestantes} dias restantes", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Text("${insumo.cantidadGlobal.toInt()}u", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.error)
                                }
                                if (insumoExpandido == insumo.id) {
                                    Spacer(Modifier.height(8.dp))
                                    vm.sucursales.forEach { suc ->
                                        val stock = insumo.stockPorSucursal[suc] ?: 0.0
                                        Row(modifier = Modifier.fillMaxWidth().padding(start = 8.dp)) {
                                            Text("  $suc: ", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text("${stock.toInt()}u", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = if (stock > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item { Text("NORMAL", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) }

                items(vm.insumos.filter { !it.esCritico }) { insumo ->
                    ElevatedCard(
                        onClick = { insumoExpandido = if (insumoExpandido == insumo.id) null else insumo.id },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(insumo.id.replace("_", " ").replaceFirstChar { it.uppercase(java.util.Locale.getDefault()) }, fontWeight = FontWeight.Medium)
                                Text("${insumo.cantidadGlobal.toInt()}u", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                            if (insumoExpandido == insumo.id) {
                                Spacer(Modifier.height(8.dp))
                                vm.sucursales.forEach { suc ->
                                    val stock = insumo.stockPorSucursal[suc] ?: 0.0
                                    Row(modifier = Modifier.fillMaxWidth().padding(start = 8.dp)) {
                                        Text("  $suc: ", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("${stock.toInt()}u", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = if (stock > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(onClick = onSync, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Sync, "Sincronizar", modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Sync")
                        }
                        OutlinedButton(onClick = onReportes, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.BarChart, "Grafico", modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Reportes")
                        }
                    }
                }
            }
        }
    }
}
