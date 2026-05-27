package com.bocatta.pos.presentation.ui.screens.ventas

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
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
    onVerActividad: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    var productoConfigurando by remember { mutableStateOf<SalesInventoryProductV2?>(null) }
    var itemEditando by remember { mutableStateOf<ItemCarritoV2?>(null) }
    var productoPeso by remember { mutableStateOf<SalesInventoryProductV2?>(null) }
    var mostrarPago by remember { mutableStateOf(false) }
    var itemPorEliminar by remember { mutableStateOf<ItemCarritoV2?>(null) }
    var mostrarBuscarCliente by remember { mutableStateOf(false) }
    var mostrarRegistrarCliente by remember { mutableStateOf(false) }
    var mostrarRetiroAlimento by remember { mutableStateOf(false) }
    var mostrarCompraRapida by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var mostrarCarritoMobile by remember { mutableStateOf(false) }
    var mostrarCancelarVentaPin by remember { mutableStateOf(false) }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val cajaVm: CajaViewModel = koinViewModel()
    val heldOrderVm: HeldOrderViewModel = viewModel()
    val scannerFocus = remember { FocusRequester() }
    var scannerInput by remember { mutableStateOf("") }

    LaunchedEffect(session.sucursalActual) {
        cajaVm.configurarSucursal(session.sucursalActual)
    }

    LaunchedEffect(scannerInput) {
        if (scannerInput.length >= 3) {
            val prod = vmV2.productos.find { it.id == scannerInput || it.nombre.equals(scannerInput, ignoreCase = true) }
            if (prod != null) { productoConfigurando = prod; scannerInput = "" }
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    fun solicitarCobro() {
        when {
            vmV2.carrito.isEmpty() -> Unit
            cajaVm.cargandoTurno -> scope.launch {
                snackbarHostState.showSnackbar("Validando turno. Intenta de nuevo en un momento.")
            }
            cajaVm.turnoActivo == null -> scope.launch {
                snackbarHostState.showSnackbar("No hay turno activo. Inicia o unete al turno antes de cobrar.")
            }
            else -> mostrarPago = true
        }
    }

    val onKeyEvent: (KeyEvent) -> Boolean = { event ->
        if (event.type == KeyEventType.KeyUp) {
            when (event.key) {
                Key.F1 -> { scannerFocus.requestFocus(); true }
                Key.F2 -> { solicitarCobro(); true }
                Key.F3 -> { mostrarBuscarCliente = true; true }
                Key.Escape -> { mostrarCancelarVentaPin = true; true }
                else -> false
            }
        } else false
    }

    val uiState by vmV2.uiState.collectAsStateWithLifecycle()
    val isOnline = vmV2.isOnline

    var categoriaSeleccionada by remember { mutableStateOf("FRECUENTES") }

    val filteredProducts = remember(vmV2.catalogoProcesado, categoriaSeleccionada, searchQuery) {
        val catalogo = vmV2.catalogoProcesado ?: return@remember emptyList<SalesInventoryProductV2>()
        val baseProducts = if (categoriaSeleccionada == "FRECUENTES") {
            catalogo.frecuentes
        } else {
            catalogo.vendibles.filter { it.categoria.trim().uppercase() == categoriaSeleccionada }
        }
        baseProducts.filter { prod ->
            searchQuery.isEmpty() || prod.nombre.contains(searchQuery, ignoreCase = true)
        }
    }

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
                message = "Aviso: $msg",
                actionLabel = "OK",
                duration = SnackbarDuration.Long
            )
            vmV2.limpiarError()
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { msg ->
            snackbarHostState.showSnackbar(
                message = msg,
                duration = SnackbarDuration.Long
            )
            vmV2.onErrorShown()
        }
    }

    LaunchedEffect(uiState.showSuccess) {
        if (uiState.showSuccess) {
            mostrarPago = false
            try {
                snackbarHostState.showSnackbar(
                    message = "Venta registrada",
                    duration = SnackbarDuration.Short
                )
                val orderIdToDelete = vmV2.lastCompletedHeldOrderId
                if (orderIdToDelete != null) {
                    heldOrderVm.deleteOrder(orderIdToDelete)
                }
            } finally {
                vmV2.onSuccessShown()
            }
        }
    }

    // -- Dialogos --
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
        val adminVmCompra: AdminViewModel = koinViewModel()
        DialogCompraUnificado(
            insumos = adminVmCompra.insumosMaestros,
            nombreUsuario = session.nombreUsuario,
            usuarioId = session.uid,
            sucursal = session.sucursalActual,
            esAdmin = session.esAdmin,
            onConfirmar = { insumoId, insumoNombre, presentacion, cant, cont, precio ->
                adminVmCompra.registrarCompraRapida(insumoId, insumoNombre, presentacion, cant, cont, precio, session.uid, session.nombreUsuario, session.sucursalActual, session.esAdmin)
                mostrarCompraRapida = false
            },
            onDismiss = { mostrarCompraRapida = false }
        )
    }
    // Dialog de gramaje para productos vendidos por peso
    productoPeso?.let { prod ->
        DialogPesoProducto(
            producto = prod,
            sucursal = session.sucursalActual,
            onDismiss = { productoPeso = null },
            onConfirmar = { gramos ->
                vmV2.agregarAlCarrito(
                    producto = prod,
                    sucursal = session.sucursalActual,
                    cantidadGramos = gramos
                )
                productoPeso = null
            }
        )
    }
    productoConfigurando?.let { prod ->
        val categoria = prod.categoria.uppercase(Locale.ROOT)
        val usaConstructorCrepa = categoria.contains("CREPA") || categoria.contains("COMBO")
        if (usaConstructorCrepa) {
            CrepeBuilderDialog(
                producto = prod,
                vmV2 = vmV2,
                sucursal = session.sucursalActual,
                itemInicial = itemEditando,
                onDismiss = {
                    productoConfigurando = null
                    itemEditando = null
                },
                onAddToCart = { producto, base, aderezos, toppings, esSeparado, componentes ->
                    val original = itemEditando
                    if (original != null) {
                        vmV2.reemplazarItemCarrito(
                            itemOriginal = original,
                            producto = producto,
                            sucursal = session.sucursalActual,
                            base = base,
                            aderezos = aderezos,
                            toppings = toppings,
                            esSeparado = esSeparado,
                            componentes = componentes
                        )
                    } else {
                        vmV2.agregarAlCarrito(
                            producto = producto,
                            sucursal = session.sucursalActual,
                            base = base,
                            aderezos = aderezos,
                            toppings = toppings,
                            esSeparado = esSeparado,
                            componentes = componentes
                        )
                    }
                    productoConfigurando = null
                    itemEditando = null
                }
            )
        } else if (prod.configSchema.isNotEmpty()) {
            DynamicConfigSheet(
                configGroups = prod.configSchema,
                accentColor = MaterialTheme.colorScheme.primary,
                onDismiss = {
                    productoConfigurando = null
                    itemEditando = null
                },
                onConfirm = { config ->
                    vmV2.agregarAlCarritoConConfig(prod, session.sucursalActual, config)
                    productoConfigurando = null
                    itemEditando = null
                }
            )
        } else {
            val original = itemEditando
            if (original != null) {
                vmV2.reemplazarItemCarrito(original, prod, session.sucursalActual, null, emptyList(), emptyList(), false)
            } else {
                vmV2.agregarAlCarrito(prod, session.sucursalActual, null, emptyList(), emptyList(), false)
            }
            productoConfigurando = null
            itemEditando = null
        }
    }
    if (mostrarPago) {
        PagoSheetV2(
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
        val tieneItems = vmV2.carrito.isNotEmpty()
        val requierePin = tieneItems && !session.esAdmin
        if (requierePin) {
            var pinCancelError by remember { mutableStateOf(false) }
            AdminPinDialog(
                titulo = "Cancelar orden",
                mensaje = "Para cancelar una orden con productos se requiere autorizacion de administrador.",
                onDismiss = { mostrarCancelarVentaPin = false; pinCancelError = false },
                onConfirm = { pin ->
                    session.validarPinAdmin(pin) { esValido ->
                        if (esValido) {
                            vmV2.limpiarCarrito()
                            mostrarCancelarVentaPin = false
                            pinCancelError = false
                        } else {
                            pinCancelError = true
                        }
                    }
                },
                error = if (pinCancelError) "PIN incorrecto" else null
            )
        } else {
            AlertDialog(
                onDismissRequest = { mostrarCancelarVentaPin = false },
                title = { Text("Cancelar orden", fontWeight = FontWeight.Bold) },
                text = { Text(if (tieneItems) "Cancelar toda la orden actual?" else "No hay productos en la orden.") },
                confirmButton = {
                    if (tieneItems) {
                        Button(onClick = {
                            vmV2.limpiarCarrito()
                            mostrarCancelarVentaPin = false
                        }) { Text("Cancelar orden") }
                    }
                },
                dismissButton = {
                    TextButton(onClick = { mostrarCancelarVentaPin = false }) { Text("Volver") }
                }
            )
        }
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

    val navigateFromDrawer: (() -> Unit) -> Unit = { action ->
        scope.launch { drawerState.close() }
        action()
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = drawerState.isOpen,
        drawerContent = {
            BocattaSalesDrawer(
                session = session,
                isOnline = isOnline,
                onNavigate = navigateFromDrawer,
                onVerCaja = onVerCaja,
                onVerInventario = onVerInventario,
                onVerGastos = onVerGastos,
                onVerDevoluciones = onVerDevoluciones,
                onVerAdmin = onVerAdmin,
                onVerReportes = onVerReportes,
                onVerActividad = onVerActividad,
                onLogout = onLogout
            )
        }
    ) {
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
                                Text("BOCATTA POS", fontWeight = FontWeight.Black, fontSize = 21.sp, letterSpacing = 1.sp, color = MaterialTheme.colorScheme.onBackground)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(Modifier.size(8.dp).background(if (isOnline) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error, CircleShape))
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        "${if (isOnline) "SISTEMA ONLINE" else "SISTEMA OFFLINE"} - ${session.sucursalActual.uppercase()}",
                                        style = MaterialTheme.typography.labelSmall, 
                                        color = (if (isOnline) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error).copy(0.8f),
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                }
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh, shape = CircleShape, modifier = Modifier.size(42.dp)) {
                                    Icon(Icons.Default.Menu, "Menu", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(10.dp))
                                }
                            }
                        },
                        actions = {
                            if (isTablet) {
                                IconButton(onClick = { mostrarRetiroAlimento = true }) {
                                    Surface(color = MaterialTheme.colorScheme.onBackground.copy(0.05f), shape = CircleShape, modifier = Modifier.size(40.dp)) {
                                        Icon(Icons.Default.Restaurant, "Retiro alimento", tint = MaterialTheme.colorScheme.onBackground, modifier = Modifier.padding(10.dp))
                                    }
                                }
                                Spacer(Modifier.width(8.dp))
                                IconButton(onClick = { mostrarCompraRapida = true }) {
                                    Surface(color = MaterialTheme.colorScheme.tertiary.copy(0.1f), shape = CircleShape, modifier = Modifier.size(40.dp)) {
                                        Icon(Icons.Default.AddShoppingCart, "Compra rapida", tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.padding(10.dp))
                                    }
                                }
                                Spacer(Modifier.width(8.dp))
                                IconButton(onClick = { mostrarCancelarVentaPin = true }) {
                                    Surface(color = MaterialTheme.colorScheme.error.copy(0.1f), shape = CircleShape, modifier = Modifier.size(40.dp)) {
                                        Icon(Icons.Default.Block, "Cancelar venta", tint = MaterialTheme.colorScheme.error, modifier = Modifier.padding(10.dp))
                                    }
                                }
                                Spacer(Modifier.width(8.dp))
                                IconButton(onClick = onVerActividad) {
                                    Surface(color = MaterialTheme.colorScheme.tertiary.copy(0.1f), shape = CircleShape, modifier = Modifier.size(40.dp)) {
                                        Icon(Icons.Default.Bookmark, "Actividad y mesas", tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.padding(10.dp))
                                    }
                                }
                                Spacer(Modifier.width(8.dp))
                                IconButton(onClick = onLogout) {
                                    Surface(color = MaterialTheme.colorScheme.error.copy(0.1f), shape = CircleShape, modifier = Modifier.size(40.dp)) {
                                        Icon(Icons.AutoMirrored.Filled.Logout, "Cerrar sesion", tint = MaterialTheme.colorScheme.error, modifier = Modifier.padding(10.dp))
                                    }
                                }
                                Spacer(Modifier.width(16.dp))
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent, titleContentColor = MaterialTheme.colorScheme.onBackground)
                    )
                },
                bottomBar = {
                    if (!isTablet) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            modifier = Modifier.fillMaxWidth().height(76.dp).clickable { mostrarCarritoMobile = true },
                            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(0.2f))
                        ) {
                            Row(Modifier.padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text("ORDEN - ${vmV2.carrito.sumOf { it.cantidad }} items".uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                                    val total = (vmV2.totalCarrito.toDouble() - vmV2.descuentoLealtad - vmV2.descuentoPromociones).coerceAtLeast(0.0)
                                    Text("$${"%.2f".format(total)}", fontWeight = FontWeight.Black, fontSize = 24.sp, color = MaterialTheme.colorScheme.primary)
                                }
                                NeonButton(texto = "COBRAR", onClick = { solicitarCobro() }, modifier = Modifier.width(148.dp), color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            ) { padding ->
                Row(modifier = Modifier.padding(padding).fillMaxSize()) {
                    Column(modifier = Modifier.weight(if (isTablet) 0.62f else 1f).fillMaxHeight()) {
                        // Barra de busqueda
                        Box(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                            BocattaSearchBar(query = searchQuery, onQueryChange = { searchQuery = it }, placeholder = "Buscar en menu...")
                        }
                        
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            when {
                                cajaVm.cargandoTurno -> SalesStatusBanner(
                                    icon = Icons.Default.Schedule,
                                    title = "Validando turno",
                                    message = "Revisando la apertura activa de ${session.sucursalActual.uppercase()}.",
                                    actionText = null,
                                    onAction = null
                                )
                                cajaVm.turnoActivo == null -> SalesStatusBanner(
                                    icon = Icons.Default.LockOpen,
                                    title = "Turno inactivo",
                                    message = "Para cobrar necesitas iniciar o unirte a un turno.",
                                    actionText = "IR A CAJA",
                                    onAction = onVerCaja
                                )
                            }
                            if (vmV2.menuError != null) {
                                SalesStatusBanner(
                                    icon = Icons.Default.CloudOff,
                                    title = "Menu no disponible",
                                    message = vmV2.menuError ?: "",
                                    actionText = "REINTENTAR",
                                    onAction = { vmV2.refrescarMenu() },
                                    isError = true
                                )
                            } else if (!vmV2.menuCargando && vmV2.productos.isNotEmpty() && vmV2.alertasStock.isEmpty()) {
                                SalesStatusBanner(
                                    icon = Icons.Default.Inventory,
                                    title = "Stock de sucursal pendiente",
                                    message = "El menu cargo, pero aun no hay asignacion visible para esta sucursal.",
                                    actionText = "ASIGNAR",
                                    onAction = onVerInventario
                                )
                            }
                        }

                        // Categorias con frecuentes por defecto
                        val catalogo = vmV2.catalogoProcesado
                        val categoriasVisibles = catalogo?.categoriasVisibles ?: emptyList()
                        PrimaryScrollableTabRow(
                            selectedTabIndex = if (categoriaSeleccionada == "FRECUENTES") 0 else (categoriasVisibles.indexOf(categoriaSeleccionada) + 1).coerceAtLeast(0),
                            containerColor = Color.Transparent,
                            edgePadding = 16.dp,
                            divider = {}
                        ) {
                            Tab(selected = categoriaSeleccionada == "FRECUENTES", onClick = { categoriaSeleccionada = "FRECUENTES" }) {
                                Text("FRECUENTES", modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp), fontWeight = if (categoriaSeleccionada == "FRECUENTES") FontWeight.Black else FontWeight.Normal, fontSize = 12.sp, color = if(categoriaSeleccionada == "FRECUENTES") MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onBackground.copy(0.5f))
                            }
                            categoriasVisibles.forEach { cat ->
                                Tab(selected = categoriaSeleccionada == cat, onClick = { categoriaSeleccionada = cat }) {
                                    Text(cat, modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp), fontWeight = if (categoriaSeleccionada == cat) FontWeight.Black else FontWeight.Normal, fontSize = 12.sp, color = if(categoriaSeleccionada == cat) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onBackground.copy(0.5f))
                                }
                            }
                        }

                        Box(Modifier.weight(1f).padding(horizontal = 16.dp)) {
                            when {
                                vmV2.menuCargando && vmV2.productos.isEmpty() -> {
                                    SalesEmptyState(
                                        icon = Icons.Default.Sync,
                                        title = "Cargando menu",
                                        message = "Estamos trayendo productos, precios y configuraciones.",
                                        actionText = null,
                                        onAction = null
                                    )
                                }
                                filteredProducts.isEmpty() -> {
                                    SalesEmptyState(
                                        icon = Icons.Default.SearchOff,
                                        title = if (vmV2.productos.isEmpty()) "No hay productos cargados" else "Sin resultados",
                                        message = if (vmV2.productos.isEmpty())
                                            "Carga V2 o toca reintentar para volver a leer el menu."
                                        else "No encontramos productos con ese filtro.",
                                        actionText = if (vmV2.productos.isEmpty()) "REINTENTAR" else "LIMPIAR BUSQUEDA",
                                        onAction = {
                                            if (vmV2.productos.isEmpty()) vmV2.refrescarMenu() else searchQuery = ""
                                        }
                                    )
                                }
                                else -> {
                                    LazyVerticalGrid(
                                        columns = GridCells.Adaptive(120.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp),
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
                                                personalizable = prod.configSchema.isNotEmpty(),
                                                esPorPeso = prod.porPeso,
                                                onClick = {
                                                    if (prod.porPeso) productoPeso = prod
                                                    else productoConfigurando = prod
                                                }
                                            )
                                        }
                                    }
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
                              modalidad = vmV2.modalidadOrden,
                              onModalidadChanged = { vmV2.cambiarModalidadOrden(it) },
                              onToggleParaLlevarItem = { vmV2.toggleParaLlevarItem(it) },
                              modifier = Modifier.weight(0.38f).fillMaxHeight(),
                              onEliminarItem = { itemPorEliminar = it },
                              onEditarItem = { item -> itemEditando = item; productoConfigurando = item.producto },
                              onCobrar = { solicitarCobro() },
                              onApplyDiscount = { vmV2.aplicarDescuentoManual(it) },
                              onApartar = {
                                   val oldOrderId = vmV2.activeHeldOrderId
                                   heldOrderVm.saveOrder(
                                       carrito = vmV2.carrito,
                                       cliente = vmV2.clienteSeleccionado,
                                       nota = "",
                                       sucursal = session.sucursalActual.lowercase(),
                                       total = (vmV2.totalCarrito.toDouble() - vmV2.descuentoLealtad - vmV2.descuentoPromociones - vmV2.descuentoManual).coerceAtLeast(0.0),
                                       modalidad = vmV2.modalidadOrden.name,
                                       mesaId = vmV2.mesaIdSeleccionada
                                   )
                                   if (oldOrderId != null) {
                                       heldOrderVm.deleteOrder(oldOrderId, liberarMesa = false)
                                   }
                                   vmV2.limpiarCarrito()
                               },
                               onBuscarCliente = { mostrarBuscarCliente = true },
                               onEliminarCliente = { vmV2.eliminarCliente() },
                               esAdmin = session.esAdmin,
                               onValidarPin = { pin, cb -> session.validarPinAdmin(pin, cb) },
                               mesaId = vmV2.mesaIdSeleccionada
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
                        modalidad = vmV2.modalidadOrden,
                        onModalidadChanged = { vmV2.cambiarModalidadOrden(it) },
                        onToggleParaLlevarItem = { vmV2.toggleParaLlevarItem(it) },
                        modifier = Modifier.fillMaxWidth(),
                        onEliminarItem = { itemPorEliminar = it; mostrarCarritoMobile = false },
                        onEditarItem = { item -> itemEditando = item; productoConfigurando = item.producto; mostrarCarritoMobile = false },
                        onCobrar = { mostrarCarritoMobile = false; solicitarCobro() },
                        onApplyDiscount = { vmV2.aplicarDescuentoManual(it) },
                        onApartar = {
                             val oldOrderId = vmV2.activeHeldOrderId
                             heldOrderVm.saveOrder(
                                 carrito = vmV2.carrito,
                                 cliente = vmV2.clienteSeleccionado,
                                 nota = "",
                                 sucursal = session.sucursalActual.lowercase(),
                                 total = (vmV2.totalCarrito.toDouble() - vmV2.descuentoLealtad - vmV2.descuentoPromociones - vmV2.descuentoManual).coerceAtLeast(0.0),
                                 modalidad = vmV2.modalidadOrden.name,
                                 mesaId = vmV2.mesaIdSeleccionada
                             )
                             if (oldOrderId != null) {
                                 heldOrderVm.deleteOrder(oldOrderId, liberarMesa = false)
                             }
                             vmV2.limpiarCarrito()
                             mostrarCarritoMobile = false
                        },
                        onBuscarCliente = { mostrarBuscarCliente = true },
                        onEliminarCliente = { vmV2.eliminarCliente() },
                        esAdmin = session.esAdmin,
                        onValidarPin = { pin, cb -> session.validarPinAdmin(pin, cb) },
                        mesaId = vmV2.mesaIdSeleccionada
                    )
                    Spacer(Modifier.height(32.dp))
                }
            }
        }
    }
    }
}

@Composable
private fun BocattaSalesDrawer(
    session: SessionViewModel,
    isOnline: Boolean,
    onNavigate: (() -> Unit) -> Unit,
    onVerCaja: () -> Unit,
    onVerInventario: () -> Unit,
    onVerGastos: () -> Unit,
    onVerDevoluciones: () -> Unit,
    onVerAdmin: () -> Unit,
    onVerReportes: () -> Unit,
    onVerActividad: () -> Unit,
    onLogout: () -> Unit
) {
    ModalDrawerSheet(
        drawerContainerColor = MaterialTheme.colorScheme.surface,
        drawerContentColor = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.width(320.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Bocatta POS", fontWeight = FontWeight.Black, fontSize = 22.sp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(8.dp)
                        .background(
                            if (isOnline) MaterialTheme.colorScheme.tertiary
                            else MaterialTheme.colorScheme.error,
                            CircleShape
                        )
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "${session.sucursalActual.uppercase()} - ${if (isOnline) "online" else "offline"}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            DrawerGroup("Venta")
            DrawerItem("POS", Icons.Default.PointOfSale, selected = true) { }
            DrawerItem("Mesas / Ordenes", Icons.Default.Restaurant) { onNavigate(onVerActividad) }

            DrawerGroup("Operacion")
            DrawerItem("Caja", Icons.Default.Payments) { onNavigate(onVerCaja) }
            DrawerItem("Cerrar turno", Icons.Default.LockClock) { onNavigate(onVerCaja) }
            DrawerItem("Gastos", Icons.AutoMirrored.Filled.ReceiptLong) { onNavigate(onVerGastos) }
            DrawerItem("Devoluciones", Icons.AutoMirrored.Filled.Undo) { onNavigate(onVerDevoluciones) }

            DrawerGroup("Inventario")
            DrawerItem("Inventario", Icons.Default.Inventory) { onNavigate(onVerInventario) }

            if (session.esAdmin) {
                DrawerGroup("Admin")
                DrawerItem("Control central", Icons.Default.AdminPanelSettings) { onNavigate(onVerAdmin) }
                DrawerItem("Reportes", Icons.Default.Assessment) { onNavigate(onVerReportes) }
            }

            Spacer(Modifier.weight(1f))
            HorizontalDivider()
            DrawerItem("Cerrar sesion", Icons.AutoMirrored.Filled.Logout, danger = true) { onNavigate(onLogout) }
        }
    }
}

@Composable
private fun DrawerGroup(label: String) {
    Text(
        label.uppercase(),
        modifier = Modifier.padding(top = 10.dp, start = 12.dp, bottom = 2.dp),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Black,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun DrawerItem(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean = false,
    danger: Boolean = false,
    onClick: () -> Unit
) {
    NavigationDrawerItem(
        selected = selected,
        onClick = onClick,
        icon = {
            Icon(
                icon,
                contentDescription = null,
                tint = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        label = {
            Text(
                label,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
            )
        },
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
private fun SalesStatusBanner(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    message: String,
    actionText: String?,
    onAction: (() -> Unit)?,
    isError: Boolean = false
) {
    val color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = color.copy(0.10f),
        border = BorderStroke(1.dp, color.copy(0.25f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Black, fontSize = 12.sp, color = color)
                Text(message, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.72f))
            }
            if (actionText != null && onAction != null) {
                TextButton(onClick = onAction) {
                    Text(actionText, fontWeight = FontWeight.Black, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun SalesEmptyState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    message: String,
    actionText: String?,
    onAction: (() -> Unit)?
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(0.25f)),
            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp))
                Text(title, fontWeight = FontWeight.Black, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                Text(message, textAlign = TextAlign.Center, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.62f))
                if (actionText != null && onAction != null) {
                    Button(onClick = onAction, shape = RoundedCornerShape(14.dp)) {
                        Text(actionText, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}
