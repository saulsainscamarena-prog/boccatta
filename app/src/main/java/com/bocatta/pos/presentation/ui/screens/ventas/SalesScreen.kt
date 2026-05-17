package com.bocatta.pos.presentation.ui.screens.ventas

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.ui.unit.sp
import com.bocatta.pos.domain.model.*
import com.bocatta.pos.presentation.ui.components.*
import com.bocatta.pos.presentation.ui.theme.*
import com.bocatta.pos.presentation.viewmodel.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.*
import com.bocatta.pos.presentation.ui.screens.ventas.HeldOrdersScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SalesScreen(
    vmV2: SalesViewModelV2,
    session: SessionViewModel,
    onVerInventario: () -> Unit,
    onVerReportes: () -> Unit,
    onVerGastos: () -> Unit,
    onVerAdmin: () -> Unit,
    onVerCaja: () -> Unit,
    onVerDevoluciones: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    var productoConfigurando by remember { mutableStateOf<SalesInventoryProductV2?>(null) }
    var mostrarPago by remember { mutableStateOf(false) }
    var itemPorEliminar by remember { mutableStateOf<ItemCarritoV2?>(null) }
    var mostrarBuscarCliente by remember { mutableStateOf(false) }
    var mostrarRegistrarCliente by remember { mutableStateOf(false) }
    var mostrarRetiroAlimento by remember { mutableStateOf(false) }
    var mostrarCompraRapida by remember { mutableStateOf(false) }
    val adminVmCompra: AdminViewModel = koinViewModel()
    var searchQuery by remember { mutableStateOf("") }
    var mostrarCarritoMobile by remember { mutableStateOf(false) }
    var mostrarCancelarVentaPin by remember { mutableStateOf(false) }
    var mostrarHeldOrders by remember { mutableStateOf(false) }
    val heldOrderVm: HeldOrderViewModel = viewModel()
    val scannerFocus = remember { FocusRequester() }
    var scannerInput by remember { mutableStateOf("") }

    LaunchedEffect(scannerInput) {
        if (scannerInput.length >= 3) {
            val prod = vmV2.productos.find { it.id == scannerInput || it.nombre.equals(scannerInput, ignoreCase = true) }
            if (prod != null) { productoConfigurando = prod; scannerInput = "" }
        }
    }

    val onKeyEvent: (KeyEvent) -> Boolean = { event ->
        if (event.type == KeyEventType.KeyUp) {
            when (event.key) {
                Key.F1 -> { scannerFocus.requestFocus(); true }
                Key.F2 -> { if (vmV2.carrito.isNotEmpty()) mostrarPago = true; true }
                Key.F3 -> { mostrarBuscarCliente = true; true }
                Key.Escape -> { mostrarCancelarVentaPin = true; true }
                else -> false
            }
        } else false
    }

    val uiState by vmV2.uiState.collectAsState()
    val isOnline = vmV2.isOnline

    val categorias = remember(vmV2.productos) {
        vmV2.productos
            .map { it.categoria.uppercase() }
            .distinct()
            .sorted()
    }
    val categoriasDisplay = remember(categorias) {
        categorias.map { if (it.startsWith("CREPAS")) "CREPAS" else it }.distinct()
    }
    var categoriaSeleccionada by remember { mutableStateOf("TODOS") }

    val filteredProducts = remember(vmV2.productos, categoriaSeleccionada, searchQuery) {
        vmV2.productos.filter { prod ->
            val catNorm = prod.categoria.uppercase()
            val matchesCategoria = when {
                categoriaSeleccionada == "TODOS" -> true
                categoriaSeleccionada == "CREPAS" -> catNorm.startsWith("CREPAS")
                else -> catNorm == categoriaSeleccionada
            }
            matchesCategoria &&
            (searchQuery.isEmpty() || prod.nombre.contains(searchQuery, ignoreCase = true))
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(vmV2.mensajeFeedback) {
        vmV2.mensajeFeedback?.let {
            val result = snackbarHostState.showSnackbar(
                message = it,
                actionLabel = if (vmV2.hayUndo) "DESHACER" else null,
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                vmV2.undoLastAction()
            }
            vmV2.mensajeFeedback = null
        }
    }
    LaunchedEffect(vmV2.mensajeError) {
        vmV2.mensajeError?.let { msg ->
            snackbarHostState.showSnackbar(
                message = "⚠️ $msg",
                actionLabel = "OK",
                duration = SnackbarDuration.Long
            )
            vmV2.limpiarError()
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { msg ->
            snackbarHostState.showSnackbar(
                message = "❌ $msg",
                duration = SnackbarDuration.Short
            )
            vmV2.onErrorShown()
        }
    }

    LaunchedEffect(uiState.showSuccess) {
        if (uiState.showSuccess) {
            snackbarHostState.showSnackbar(
                message = "✅ Venta registrada",
                duration = SnackbarDuration.Short
            )
        }
    }

    // -- Diálogos --
    if (vmV2.mostrarConfirmacionVenta) {
        ConfirmacionVentaDialog(
            vmV2 = vmV2,
            sucursalActual = session.sucursalActual,
            context = context,
            onDismiss = { vmV2.mostrarConfirmacionVenta = false }
        )
    }

    if (mostrarBuscarCliente) {
        BuscarClienteDialog(
            vmV2 = vmV2,
            onDismiss = { mostrarBuscarCliente = false },
            onNavigateToRegistrar = { mostrarBuscarCliente = false; mostrarRegistrarCliente = true }
        )
    }

    if (mostrarRegistrarCliente) {
        RegistrarClienteDialog(vmV2 = vmV2, onDismiss = { mostrarRegistrarCliente = false })
    }

    if (mostrarRetiroAlimento) {
        RetiroAlimentoDialog(
            vmV2 = vmV2,
            sucursalActual = session.sucursalActual,
            nombreUsuario = session.nombreUsuario,
            onDismiss = { mostrarRetiroAlimento = false }
        )
    }

    if (mostrarCompraRapida) {
        DialogCompraUnificado(
            insumos = adminVmCompra.insumosMaestros,
            nombreUsuario = session.nombreUsuario,
            usuarioId = session.uid ?: "",
            sucursal = session.sucursalActual,
            esAdmin = session.esAdmin,
            onConfirmar = { insumoId, insumoNombre, presentacion, cant, cont, precio ->
                adminVmCompra.registrarCompraRapida(insumoId, insumoNombre, presentacion, cant, cont, precio, session.uid ?: "", session.nombreUsuario, session.sucursalActual, session.esAdmin)
                mostrarCompraRapida = false
            },
            onDismiss = { mostrarCompraRapida = false }
        )
    }

    productoConfigurando?.let { prod ->
        if (prod.configSchema.isNotEmpty()) {
            DynamicConfigSheet(
                configGroups = prod.configSchema,
                accentColor = MaterialTheme.colorScheme.primary,
                onDismiss = { productoConfigurando = null },
                onConfirm = { config ->
                    vmV2.agregarAlCarritoConConfig(prod, session.sucursalActual, config)
                    productoConfigurando = null
                }
            )
        } else {
            vmV2.agregarAlCarrito(prod, session.sucursalActual, null, emptyList(), emptyList(), false)
            productoConfigurando = null
        }
    }

    if (mostrarPago) {
        PagoDialog(
            vmV2 = vmV2,
            sucursalActual = session.sucursalActual,
            nombreUsuario = session.nombreUsuario,
            onDismiss = { mostrarPago = false }
        )
    }

    itemPorEliminar?.let { item ->
        EliminarItemDialog(
            item = item,
            onDismiss = { itemPorEliminar = null },
            onConfirm = { motivo ->
                vmV2.eliminarDelCarrito(item, motivo, session.nombreUsuario, session.sucursalActual)
                itemPorEliminar = null
            }
        )
    }

    if (mostrarCancelarVentaPin) {
        AdminPinDialog(
            onDismiss = { mostrarCancelarVentaPin = false },
            onConfirm = { _ ->
                vmV2.limpiarCarrito()
                mostrarCancelarVentaPin = false
            }
        )
    }

    if (mostrarHeldOrders) {
        AlertDialog(
            onDismissRequest = { mostrarHeldOrders = false },
            modifier = Modifier.fillMaxWidth().heightIn(max = 600.dp),
            title = { Text("ÓRDENES APARTADAS", fontWeight = FontWeight.Black) },
            text = {
                var ordersList by remember { mutableStateOf(heldOrderVm.orders.toList()) }
                LaunchedEffect(Unit) { heldOrderVm.loadOrders() }
                LaunchedEffect(heldOrderVm.orders.size) { ordersList = heldOrderVm.orders.toList() }
                if (ordersList.isEmpty()) {
                    Box(Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                        Text("No hay órdenes apartadas", color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(ordersList, key = { it.id }) { order ->
                            Surface(
                                onClick = {
                                    val items = heldOrderVm.parseCarrito(order.carritoJson)
                                    items.forEach { vmV2.agregarAlCarrito(it.producto, order.sucursal, it.base, it.aderezos, it.toppings, it.esSeparado, it.componentesCombo) }
                                    heldOrderVm.deleteOrder(order.id)
                                    mostrarHeldOrders = false
                                },
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(0.5f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text(SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date(order.fecha)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(0.6f))
                                        Text("$${"%.2f".format(order.total)}", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                                    }
                                    Icon(Icons.Default.Restore, "Restaurar", tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { mostrarHeldOrders = false }) { Text("CERRAR") }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }

    // Scanner oculto
    Box(modifier = Modifier.size(1.dp).alpha(0f)) {
        OutlinedTextField(
            value = scannerInput,
            onValueChange = { scannerInput = it },
            modifier = Modifier.focusRequester(scannerFocus).size(1.dp),
            singleLine = true
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).onKeyEvent(onKeyEvent)) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isTablet = maxWidth >= 720.dp // Meridian Spec: 720dp for tablet layout
            
            Scaffold(contentWindowInsets = WindowInsets.safeDrawing, 
                snackbarHost = { SnackbarHost(snackbarHostState) },
                containerColor = Color.Transparent,
                topBar = {
                    LargeTopAppBar(
                        title = {
                            Column {
                                Text("BOCATTA POS", fontWeight = FontWeight.Black, fontSize = 24.sp, letterSpacing = 2.sp, color = MaterialTheme.colorScheme.onBackground)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(Modifier.size(8.dp).background(if (isOnline) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error, CircleShape))
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        "${if (isOnline) "SISTEMA ONLINE" else "SISTEMA OFFLINE"} · ${session.sucursalActual.uppercase()}", 
                                        style = MaterialTheme.typography.labelSmall, 
                                        color = (if (isOnline) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error).copy(0.8f),
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                }
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = onVerCaja) { 
                                Surface(color = MaterialTheme.colorScheme.onBackground.copy(0.05f), shape = CircleShape, modifier = Modifier.size(40.dp)) {
                                    Icon(Icons.Default.Menu, "Men�", tint = MaterialTheme.colorScheme.onBackground, modifier = Modifier.padding(10.dp)) 
                                }
                            }
                        },
                        actions = {
                            IconButton(onClick = { mostrarBuscarCliente = true }) {
                                Surface(color = (if (vmV2.clienteSeleccionado != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground).copy(0.1f), shape = CircleShape, modifier = Modifier.size(40.dp)) {
                                    Icon(Icons.Default.PersonAdd, "Cliente", tint = if (vmV2.clienteSeleccionado != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground, modifier = Modifier.padding(10.dp)) 
                                }
                            }
                            Spacer(Modifier.width(8.dp))
                            IconButton(onClick = { mostrarRetiroAlimento = true }) {
                                Surface(color = MaterialTheme.colorScheme.onBackground.copy(0.05f), shape = CircleShape, modifier = Modifier.size(40.dp)) {
                                    Icon(Icons.Default.Restaurant, "Retiro alimento", tint = MaterialTheme.colorScheme.onBackground, modifier = Modifier.padding(10.dp)) 
                                }
                            }
                            Spacer(Modifier.width(8.dp))
                            IconButton(onClick = { mostrarCompraRapida = true }) {
                                Surface(color = MaterialTheme.colorScheme.tertiary.copy(0.1f), shape = CircleShape, modifier = Modifier.size(40.dp)) {
                                    Icon(Icons.Default.AddShoppingCart, "Compra r�pida", tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.padding(10.dp))
                                }
                            }
                            Spacer(Modifier.width(8.dp))
                            IconButton(onClick = { mostrarCancelarVentaPin = true }) {
                                Surface(color = MaterialTheme.colorScheme.error.copy(0.1f), shape = CircleShape, modifier = Modifier.size(40.dp)) {
                                    Icon(Icons.Default.Block, "Cancelar venta", tint = MaterialTheme.colorScheme.error, modifier = Modifier.padding(10.dp))
                                }
                            }
                            Spacer(Modifier.width(8.dp))
                            IconButton(onClick = { heldOrderVm.loadOrders(); mostrarHeldOrders = true }) {
                                Surface(color = MaterialTheme.colorScheme.tertiary.copy(0.1f), shape = CircleShape, modifier = Modifier.size(40.dp)) {
                                    Icon(Icons.Default.Bookmark, "Ordenes apartadas", tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.padding(10.dp))
                                }
                            }
                            Spacer(Modifier.width(8.dp))
                            IconButton(onClick = onLogout) { 
                                Surface(color = MaterialTheme.colorScheme.error.copy(0.1f), shape = CircleShape, modifier = Modifier.size(40.dp)) {
                                    Icon(Icons.AutoMirrored.Filled.Logout, "Cerrar sesi�n", tint = MaterialTheme.colorScheme.error, modifier = Modifier.padding(10.dp)) 
                                }
                            }
                            Spacer(Modifier.width(16.dp))
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent, titleContentColor = MaterialTheme.colorScheme.onBackground)
                    )
                },
                bottomBar = {
                    if (!isTablet) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            modifier = Modifier.fillMaxWidth().height(84.dp).clickable { mostrarCarritoMobile = true },
                            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(0.2f))
                        ) {
                            Row(Modifier.padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text("${vmV2.carrito.sumOf { it.cantidad }} items".uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                                    val total = (vmV2.totalCarrito.toDouble() - vmV2.descuentoLealtad - vmV2.descuentoPromociones).coerceAtLeast(0.0)
                                    Text("$${"%.2f".format(total)}", fontWeight = FontWeight.Black, fontSize = 26.sp, color = MaterialTheme.colorScheme.primary)
                                }
                                NeonButton(texto = "COBRAR", onClick = { mostrarPago = true }, modifier = Modifier.width(160.dp), color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            ) { padding ->
                Row(modifier = Modifier.padding(padding).fillMaxSize()) {
                    Column(modifier = Modifier.weight(if (isTablet) 0.62f else 1f).fillMaxHeight()) {
                        // Barra de Búsqueda Premium
                        Box(Modifier.padding(16.dp)) {
                            BocattaSearchBar(query = searchQuery, onQueryChange = { searchQuery = it }, placeholder = "Buscar en menú...")
                        }
                        
                        // Categorías con Estilo Premium
                        PrimaryScrollableTabRow(
                            selectedTabIndex = if (categoriaSeleccionada == "TODOS") 0 else (categoriasDisplay.indexOf(categoriaSeleccionada) + 1).coerceAtLeast(0),
                            containerColor = Color.Transparent,
                            edgePadding = 16.dp,
                            divider = {}
                        ) {
                            Tab(selected = categoriaSeleccionada == "TODOS", onClick = { categoriaSeleccionada = "TODOS" }) {
                                Text("TODOS", modifier = Modifier.padding(16.dp), fontWeight = if (categoriaSeleccionada == "TODOS") FontWeight.Black else FontWeight.Normal, fontSize = 12.sp, color = if(categoriaSeleccionada == "TODOS") MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onBackground.copy(0.5f))
                            }
                            categoriasDisplay.forEach { cat ->
                                Tab(selected = categoriaSeleccionada == cat, onClick = { categoriaSeleccionada = cat }) {
                                    Text(cat, modifier = Modifier.padding(16.dp), fontWeight = if (categoriaSeleccionada == cat) FontWeight.Black else FontWeight.Normal, fontSize = 12.sp, color = if(categoriaSeleccionada == cat) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onBackground.copy(0.5f))
                                }
                            }
                        }

                        Box(Modifier.weight(1f).padding(horizontal = 16.dp)) {
                            LazyVerticalGrid(
                                columns = GridCells.Adaptive(160.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(filteredProducts, key = { it.id }) { prod ->
                                    val precio = prod.precioVenta[session.sucursalActual.lowercase()] ?: 0.0
                                    ProductCardPremium(
                                        nombre = prod.nombre,
                                        precio = precio,
                                        emoji = prod.emoji,
                                        categoria = prod.categoria,
                                        agotado = (vmV2.alertasStock[prod.id] ?: 99.0) <= 0,
                                        pocoStock = (vmV2.alertasStock[prod.id] ?: 99.0) in 0.1..<5.0,
                                        onClick = { productoConfigurando = prod }
                                    )
                                }
                            }
                        }
                    }

                     if (isTablet) {
                         VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(0.3f))
                         CarritoPanelV2(
                              carrito = vmV2.carrito,
                              totalCarrito = vmV2.totalCarrito,
                              descuentoLealtad = vmV2.descuentoLealtad,
                              descuentoPromociones = vmV2.descuentoPromociones,
                              descuentoManual = vmV2.descuentoManual,
                              clienteSeleccionado = vmV2.clienteSeleccionado,
                              modifier = Modifier.weight(0.38f).fillMaxHeight(),
                              onEliminarItem = { itemPorEliminar = it },
                              onEditarItem = { item -> productoConfigurando = item.producto },
                              onCobrar = { mostrarPago = true },
                              onApplyDiscount = { vmV2.aplicarDescuentoManual(it) },
                              onApartar = {
                                  heldOrderVm.saveOrder(
                                      carrito = vmV2.carrito,
                                      cliente = vmV2.clienteSeleccionado,
                                      nota = "",
                                      sucursal = session.sucursalActual.lowercase(),
                                      total = (vmV2.totalCarrito.toDouble() - vmV2.descuentoLealtad - vmV2.descuentoPromociones - vmV2.descuentoManual).coerceAtLeast(0.0)
                                  )
                                  vmV2.limpiarCarrito()
                              }
                          )
                      }
                  }
              }
              if (uiState.isLoading) {
                 Box(
                     modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)),
                     contentAlignment = Alignment.Center
                 ) {
                     CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                 }
             }
             if (mostrarCarritoMobile && !isTablet) {

                ModalBottomSheet(onDismissRequest = { mostrarCarritoMobile = false }, shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)) {
                    CarritoPanelV2(
                        carrito = vmV2.carrito,
                        totalCarrito = vmV2.totalCarrito,
                        descuentoLealtad = vmV2.descuentoLealtad,
                        descuentoPromociones = vmV2.descuentoPromociones,
                        descuentoManual = vmV2.descuentoManual,
                        clienteSeleccionado = vmV2.clienteSeleccionado,
                        modifier = Modifier.fillMaxWidth(),
                        onEliminarItem = { itemPorEliminar = it; mostrarCarritoMobile = false },
                        onEditarItem = { item -> productoConfigurando = item.producto; mostrarCarritoMobile = false },
                        onCobrar = { mostrarCarritoMobile = false; mostrarPago = true },
                        onApplyDiscount = { vmV2.aplicarDescuentoManual(it) }
                    )
                    Spacer(Modifier.height(32.dp))
                }
            }
        }
    }
}


