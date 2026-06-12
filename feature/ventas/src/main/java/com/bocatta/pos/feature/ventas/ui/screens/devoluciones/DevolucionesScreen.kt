package com.bocatta.pos.feature.ventas.ui.screens.devoluciones

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bocatta.pos.domain.model.SolicitudDevolucion
import com.bocatta.pos.domain.model.VentaV2
import com.bocatta.pos.feature.ventas.viewmodel.DevolucionViewModel
import com.bocatta.pos.feature.auth.viewmodel.SessionViewModel
import com.bocatta.pos.presentation.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevolucionesScreen(
    vm: DevolucionViewModel,
    session: SessionViewModel,
    onBack: () -> Unit
) {
    val snackbarHost = remember { SnackbarHostState() }

    LaunchedEffect(vm.mensajeExito, vm.mensajeError) {
        val msg = vm.mensajeExito ?: vm.mensajeError ?: return@LaunchedEffect
        snackbarHost.showSnackbar(msg)
        vm.limpiarMensajes()
    }

    val esAdmin = session.esAdmin

    LaunchedEffect(Unit) {
        if (!esAdmin) vm.cargarVentasRecientes(session.sucursalActual)
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHost) },
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("DEVOLUCIONES Y CANCELACIONES", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Black, fontSize = 16.sp)
                        Text(if (esAdmin) "GESTION DE AUDITORIA" else "SOLICITAR REEMBOLSO", color = Color.White.copy(0.7f), fontSize = 10.sp)
                    }
                },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.secondary)
            )
        }
    ) { padding ->
        if (esAdmin) {
            AdminDevolucionesContent(vm = vm, padding = padding)
        } else {
            VendedorDevolucionContent(vm = vm, session = session, padding = padding)
        }
    }
}

@Composable
private fun AdminDevolucionesContent(vm: DevolucionViewModel, padding: PaddingValues) {
    if (vm.solicitudes.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.DoneAll, "Completado", modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.3f))
                Text("Sin solicitudes pendientes", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        return
    }
    LazyColumn(modifier = Modifier.padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        items(vm.solicitudes, key = { it.id }) { solicitud ->
            SolicitudCard(solicitud = solicitud, onAprobar = { vm.aprobarDevolucion(solicitud) }, onRechazar = { vm.rechazarDevolucion(solicitud) })
        }
    }
}

@Composable
private fun SolicitudCard(solicitud: SolicitudDevolucion, onAprobar: () -> Unit, onRechazar: () -> Unit) {
    val locale = LocalLocale.current.platformLocale
    val sdf = remember(locale) { SimpleDateFormat("dd/MM HH:mm", locale) }
    ElevatedCard(shape = RoundedCornerShape(20.dp), colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Total: $${"%.2f".format(solicitud.totalVenta)}", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary, fontSize = 20.sp)
                Text(sdf.format(Date(solicitud.fecha)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Surface(color = MaterialTheme.colorScheme.primary.copy(0.05f), shape = RoundedCornerShape(12.dp)) {
                Column(Modifier.padding(12.dp)) {
                    Text("MOTIVO:", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                    Text(solicitud.motivo, style = MaterialTheme.typography.bodyMedium)
                }
            }
            Text("Solicitante: ${solicitud.solicitadoPor}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onRechazar, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) { Text("RECHAZAR") }
                Button(onClick = onAprobar, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) { Text("APROBAR") }
            }
        }
    }
}

@Composable
private fun VendedorDevolucionContent(vm: DevolucionViewModel, session: SessionViewModel, padding: PaddingValues) {
    val locale = LocalLocale.current.platformLocale
    var ventaSeleccionada by remember { mutableStateOf<VentaV2?>(null) }
    var motivoInput by remember { mutableStateOf("") }
    var mostrarDialog by remember { mutableStateOf(false) }
    var filtroMonto by remember { mutableStateOf("") }

    val ventasFiltradas = if (filtroMonto.isBlank()) vm.ventasBuscadas else vm.ventasBuscadas.filter { it.total.toString().contains(filtroMonto) }

    val venta = ventaSeleccionada ?: return
    if (mostrarDialog) {
        AlertDialog(
            onDismissRequest = { mostrarDialog = false },
            title = { Text("Solicitar Devolucion", fontWeight = FontWeight.Black) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Monto: $${"%.2f".format(venta.total)}", fontWeight = FontWeight.Bold)
                    OutlinedTextField(value = motivoInput, onValueChange = { motivoInput = it }, label = { Text("Justificacion obligatoria") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                }
            },
            confirmButton = { Button(onClick = { vm.solicitarDevolucion(venta, motivoInput, session.nombreUsuario); mostrarDialog = false; motivoInput = ""; ventaSeleccionada = null }, enabled = motivoInput.isNotBlank()) { Text("Enviar para Revision") } },
            dismissButton = { TextButton(onClick = { mostrarDialog = false }) { Text("Cancelar") } }
        )
    }

    Column(modifier = Modifier.padding(padding).padding(16.dp)) {
        Text("VENTAS RECIENTES", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(value = filtroMonto, onValueChange = { filtroMonto = it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("Filtrar por monto...") }, leadingIcon = { Icon(Icons.Default.Search, "Buscar") }, shape = RoundedCornerShape(26.dp))
        Spacer(Modifier.height(16.dp))
        if (ventasFiltradas.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No hay ventas para mostrar", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(ventasFiltradas, key = { it.id }) { venta ->
                    val sdf = remember(locale) { SimpleDateFormat("HH:mm", locale) }
                    ElevatedCard(shape = RoundedCornerShape(16.dp), onClick = { ventaSeleccionada = venta; mostrarDialog = true }, colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text("$${"%.2f".format(venta.total)}", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary, fontSize = 18.sp)
                                Text(venta.metodoPago.uppercase(java.util.Locale.getDefault()), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(sdf.format(Date(venta.fecha)), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}



