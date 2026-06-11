package com.bocatta.pos.presentation.ui.screens.ventas

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bocatta.pos.domain.model.*
import com.bocatta.pos.presentation.viewmodel.HeldOrderViewModel
import com.bocatta.pos.presentation.viewmodel.MesaViewModel
import com.bocatta.pos.presentation.viewmodel.SalesViewModelV2
import com.bocatta.pos.presentation.viewmodel.SessionViewModel
import kotlinx.coroutines.launch
import com.bocatta.pos.presentation.ui.components.AdminPinDialog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActividadScreen(
    mesaVm: MesaViewModel,
    heldOrderVm: HeldOrderViewModel,
    salesVm: SalesViewModelV2,
    sessionVm: SessionViewModel,
    onBack: () -> Unit,
    onNavigateToSales: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 720

    var selectedZonaId by remember { mutableStateOf<String?>(null) }
    var orderToMoveToMesa by remember { mutableStateOf<HeldOrder?>(null) }
    var showMesaOcupadaExternoDialog by remember { mutableStateOf<com.bocatta.pos.domain.model.Mesa?>(null) }

    var pendingActionOnPinSuccess by remember { mutableStateOf<(() -> Unit)?>(null) }
    var pinDialogMessage by remember { mutableStateOf("") }

    // Cargar datos
    LaunchedEffect(Unit) {
        mesaVm.cargarZonas()
        mesaVm.cargarMesas()
        heldOrderVm.loadOrders()
    }

    // Seleccionar primera zona por defecto
    if (selectedZonaId == null && mesaVm.zonas.isNotEmpty()) {
        selectedZonaId = mesaVm.zonas.first().id
    }

    val mesasFiltradas = mesaVm.mesas.filter { it.zonaId == selectedZonaId }
    val ventasRapidas = heldOrderVm.orders.filter { it.mesaId.isNullOrBlank() }

        Scaffold(contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text(
                            "Mesas / Actividad de Ventas",
                            fontWeight = FontWeight.Black,
                            fontSize = 24.sp
                        )
                        Text(
                            "Gestiona el salon, pedidos rapidos y apartados fisicamente",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        mesaVm.cargarZonas()
                        mesaVm.cargarMesas()
                        heldOrderVm.loadOrders()
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Actualizar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Seccion principal (Zonas y Mesas)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(16.dp)
            ) {
                // Selector de Zonas (Tabs)
                if (mesaVm.zonas.isNotEmpty()) {
                    PrimaryScrollableTabRow(
                        selectedTabIndex = mesaVm.zonas.indexOfFirst { it.id == selectedZonaId }.coerceAtLeast(0),
                        edgePadding = 0.dp,
                        containerColor = Color.Transparent,
                        divider = {}
                    ) {
                        mesaVm.zonas.forEach { zona ->
                            val isSelected = zona.id == selectedZonaId
                            Tab(
                                selected = isSelected,
                                onClick = { selectedZonaId = zona.id },
                                text = {
                                    Text(
                                        zona.nombre.uppercase(Locale.ROOT),
                                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold
                                    )
                                }
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text("Cargando zonas...", style = MaterialTheme.typography.bodyMedium)
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Grid de Mesas
                if (mesasFiltradas.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.LayersClear,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "No hay mesas registradas en esta zona",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 160.dp),
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(mesasFiltradas, key = { it.id }) { mesa ->
                            val ordenAsociada = heldOrderVm.orders.find { it.mesaId == mesa.id || it.id == mesa.ordenActual }
                            MesaCard(
                                mesa = mesa,
                                orden = ordenAsociada,
                                onClick = {
                                    scope.launch {
                                        if (mesa.estado == EstadoMesa.LIBRE) {
                                            // Iniciar orden limpia
                                            salesVm.limpiarCarrito()
                                            salesVm.cambiarModalidadOrden(ModalidadOrden.LOCAL)
                                            salesVm.seleccionarMesa(mesa.id)
                                            onNavigateToSales()
                                        } else {
                                            // Cargar orden ocupada
                                            if (ordenAsociada != null) {
                                                val items = heldOrderVm.parseCarrito(ordenAsociada.carritoJson)
                                                val cliente = heldOrderVm.parseCliente(ordenAsociada.clienteJson)
                                                salesVm.cargarOrdenEnCarrito(
                                                    items = items,
                                                    cliente = cliente,
                                                    modalidad = ModalidadOrden.LOCAL,
                                                    idMesa = mesa.id,
                                                    heldOrderId = ordenAsociada.id
                                                )
                                                // La orden se mantiene activa en SQLite para protegerla contra perdidas por apagado.
                                                // Se elimina unicamente cuando se finaliza el cobro o se guarda con un nuevo apartado.
                                            } else {
                                                // Mesa ocupada sin orden local en SQLite; mostramos el dialogo informativo y de seguridad.
                                                showMesaOcupadaExternoDialog = mesa
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // Panel lateral de ventas rapidas
            if (isTablet) {
                VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Column(
                    modifier = Modifier
                        .width(340.dp)
                        .fillMaxHeight()
                        .padding(16.dp)
                ) {
                    Text(
                        "Ventas Rapidas Abiertas",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "Ordenes apartadas listas para cobrar o asignar a mesa",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(16.dp))

                    if (ventasRapidas.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No hay pedidos rapidos abiertos",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(ventasRapidas, key = { it.id }) { orden ->
                                VentaRapidaCard(
                                    orden = orden,
                                    onCargar = {
                                        val items = heldOrderVm.parseCarrito(orden.carritoJson)
                                        val cliente = heldOrderVm.parseCliente(orden.clienteJson)
                                        salesVm.cargarOrdenEnCarrito(
                                            items = items,
                                            cliente = cliente,
                                            modalidad = ModalidadOrden.PARA_LLEVAR,
                                            idMesa = null,
                                            heldOrderId = orden.id
                                        )
                                        onNavigateToSales()
                                    },
                                    onEliminar = {
                                        val deleteAction = { heldOrderVm.deleteOrder(orden.id) }
                                        if (sessionVm.esAdmin) {
                                            deleteAction()
                                        } else {
                                            pinDialogMessage = "Se requiere PIN de administrador para eliminar un pedido rapido."
                                            pendingActionOnPinSuccess = deleteAction
                                        }
                                    },
                                    onAsignarMesa = {
                                        val assignAction = { orderToMoveToMesa = orden }
                                        if (sessionVm.esAdmin) {
                                            assignAction()
                                        } else {
                                            pinDialogMessage = "Se requiere PIN de administrador para mover un pedido rapido a una mesa."
                                            pendingActionOnPinSuccess = assignAction
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal de asignar venta rapida a mesa
    orderToMoveToMesa?.let { orden ->
        val mesasLibres = mesaVm.mesas.filter { it.estado == EstadoMesa.LIBRE }

        AlertDialog(
            onDismissRequest = { orderToMoveToMesa = null },
            title = {
                Text(
                    "Asignar Venta a Mesa",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        "Selecciona una mesa libre para mover el pedido de $${orden.total}.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(16.dp))

                    if (mesasLibres.isEmpty()) {
                        Text(
                            "No hay mesas libres en ninguna zona actualmente.",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 240.dp)
                        ) {
                            items(mesasLibres) { mesa ->
                                val zona = mesaVm.zonas.find { it.id == mesa.zonaId }?.nombre ?: "Sin zona"
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clickable {
                                            scope.launch {
                                                if (heldOrderVm.assignOrderToMesa(orden.id, mesa.id)) {
                                                    orderToMoveToMesa = null
                                                    mesaVm.cargarMesas()
                                                    heldOrderVm.loadOrders()
                                                }
                                            }
                                        },
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                "Mesa #${mesa.numero}",
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.bodyLarge
                                            )
                                            Text(
                                                "Zona: ${zona.uppercase(Locale.ROOT)}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.People, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("${mesa.capacidad} pers.")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { orderToMoveToMesa = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (pendingActionOnPinSuccess != null) {
        var pinError by remember { mutableStateOf<String?>(null) }
        AdminPinDialog(
            titulo = "Autorizacion de Administrador",
            mensaje = pinDialogMessage,
            error = pinError,
            onDismiss = {
                pendingActionOnPinSuccess = null
                pinError = null
            },
            onConfirm = { pin ->
                sessionVm.validarPinAdmin(pin) { esValido ->
                    if (esValido) {
                        pendingActionOnPinSuccess?.invoke()
                        pendingActionOnPinSuccess = null
                        pinError = null
                    } else {
                        pinError = "PIN incorrecto"
                    }
                }
            }
        )
    }

    // Modal de mesa ocupada en otro dispositivo
    showMesaOcupadaExternoDialog?.let { mesa ->
        val esAdmin = sessionVm.esAdmin

        AlertDialog(
            onDismissRequest = { showMesaOcupadaExternoDialog = null },
            title = {
                Text(
                    "Mesa Ocupada en Otro Dispositivo",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        "Esta mesa esta registrada como ocupada en Firestore, pero no cuenta con un ticket activo en la base de datos local de este dispositivo.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Lo mas probable es que este siendo gestionada desde otra tablet de la sucursal. No la liberes a menos que confirmes que la mesa ya pago.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (esAdmin) {
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Como administrador o dueno, tienes autorizacion para liberar la mesa de forma remota en Firestore si es necesario.",
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            },
            confirmButton = {
                if (esAdmin) {
                    Button(
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        onClick = {
                            scope.launch {
                                mesaVm.actualizarEstadoMesa(mesa.id, "LIBRE")
                                mesaVm.vincularOrdenAMesa(mesa.id, "LIBRE", null)
                                showMesaOcupadaExternoDialog = null
                            }
                        }
                    ) {
                        Text("Forzar liberacion")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showMesaOcupadaExternoDialog = null }) {
                    Text("Regresar")
                }
            }
        )
    }
}

@Composable
fun MesaCard(
    mesa: Mesa,
    orden: HeldOrder?,
    onClick: () -> Unit
) {
    val statusColor = when (mesa.estado) {
        EstadoMesa.LIBRE -> MaterialTheme.colorScheme.primary
        EstadoMesa.OCUPADA -> MaterialTheme.colorScheme.tertiary
        EstadoMesa.RESERVADA -> MaterialTheme.colorScheme.secondary
        EstadoMesa.INACTIVA -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val statusBg = when (mesa.estado) {
        EstadoMesa.LIBRE -> MaterialTheme.colorScheme.primaryContainer
        EstadoMesa.OCUPADA -> MaterialTheme.colorScheme.tertiaryContainer
        EstadoMesa.RESERVADA -> MaterialTheme.colorScheme.secondaryContainer
        EstadoMesa.INACTIVA -> MaterialTheme.colorScheme.surfaceVariant
    }

    val statusBadgeContentColor = when (mesa.estado) {
        EstadoMesa.LIBRE -> MaterialTheme.colorScheme.onPrimary
        EstadoMesa.OCUPADA -> MaterialTheme.colorScheme.onTertiary
        EstadoMesa.RESERVADA -> MaterialTheme.colorScheme.onSecondary
        EstadoMesa.INACTIVA -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = statusBg),
        border = BorderStroke(1.5.dp, statusColor.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Indicador de estado
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = statusColor,
                    modifier = Modifier.height(20.dp)
                ) {
                    Text(
                        mesa.estado.name,
                        color = statusBadgeContentColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }

                // Capacidad
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.People,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "${mesa.capacidad}",
                        color = statusColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Numero grande
            Text(
                "${mesa.numero}",
                fontWeight = FontWeight.Black,
                fontSize = 44.sp,
                color = statusColor
            )

            Spacer(Modifier.height(8.dp))

            // Informacion de la cuenta / orden activa
            if (orden != null) {
                val locale = LocalLocale.current.platformLocale
                val formattedTime = remember(orden.fecha, locale) {
                    SimpleDateFormat("hh:mm a", locale).format(Date(orden.fecha))
                }
                Text(
                    "Total: $${String.format(Locale.US, "%.2f", orden.total)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = statusColor
                )
                Text(
                    "Apartada: $formattedTime",
                    fontSize = 11.sp,
                    color = statusColor.copy(alpha = 0.8f)
                )
            } else {
                Text(
                    "Mesa Disponible",
                    fontSize = 12.sp,
                    color = statusColor.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Medium
                )
                Text(
                    "-",
                    fontSize = 11.sp,
                    color = Color.Transparent
                )
            }
        }
    }
}

@Composable
fun VentaRapidaCard(
    orden: HeldOrder,
    onCargar: () -> Unit,
    onEliminar: () -> Unit,
    onAsignarMesa: () -> Unit
) {
    val locale = LocalLocale.current.platformLocale
    val formattedTime = remember(orden.fecha, locale) {
        SimpleDateFormat("hh:mm a", locale).format(Date(orden.fecha))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Pedido Rapido",
                    fontWeight = FontWeight.Black,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    formattedTime,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(8.dp))

            if (orden.nota.isNotBlank()) {
                Text(
                    "Nota: ${orden.nota}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(8.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Total: $${String.format(Locale.US, "%.2f", orden.total)}",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Eliminar
                IconButton(
                    onClick = onEliminar,
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Eliminar",
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Asignar a mesa
                Button(
                    onClick = onAsignarMesa,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f).height(36.dp)
                ) {
                    Icon(
                        Icons.Default.TableBar,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("A Mesa", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                // Cargar al POS
                Button(
                    onClick = onCargar,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f).height(36.dp)
                ) {
                    Icon(
                        Icons.Default.ShoppingCart,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Cargar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
