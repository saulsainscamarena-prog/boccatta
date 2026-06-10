package com.bocatta.pos.presentation.ui.screens.ventas

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bocatta.pos.R
import java.util.Locale
import kotlinx.coroutines.delay
import com.bocatta.pos.domain.model.*
import com.bocatta.pos.domain.usecase.HeldOrderCheckoutCompletionPolicy
import com.bocatta.pos.presentation.ui.components.*
import com.bocatta.pos.presentation.viewmodel.*
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SalesScreen(
    vmV2: SalesViewModelV2,
    cajaVm: CajaViewModel,
    heldOrderVm: HeldOrderViewModel,
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
    var mostrarMermaDialog by remember { mutableStateOf(false) }
    var pantallaBloqueada by remember { mutableStateOf(false) }
    var ultimaInteraccion by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val scannerFocus = remember { FocusRequester() }
    var scannerInput by remember { mutableStateOf("") }
    val heldOrderCheckoutPolicy = remember { HeldOrderCheckoutCompletionPolicy() }

    fun registrarInteraccion() {
        ultimaInteraccion = System.currentTimeMillis()
    }

    fun seleccionarProducto(prod: SalesInventoryProductV2) {
        registrarInteraccion()
        val categoria = prod.categoria.uppercase(Locale.ROOT)
        val requiereConstructor = categoria.contains("CREPA") || categoria.contains("COMBO")
        when {
            prod.porPeso -> productoPeso = prod
            requiereConstructor || prod.configSchema.isNotEmpty() -> productoConfigurando = prod
            else -> {
                vmV2.agregarAlCarrito(prod, session.sucursalActual, null, emptyList(), emptyList(), false)
            }
        }
    }

    LaunchedEffect(session.sucursalActual) {
        cajaVm.configurarSucursal(session.sucursalActual)
    }

    LaunchedEffect(scannerInput) {
        if (scannerInput.length >= 3) {
            val prod = vmV2.productos.find { it.id == scannerInput || it.nombre.equals(scannerInput, ignoreCase = true) }
            if (prod != null) {
                val stock = vmV2.alertasStock[prod.id] ?: 99.0
                if (stock <= 0) {
                    scope.launch {
                        snackbarHostState.showSnackbar(context.getString(R.string.sales_snackbar_agotado, prod.nombre))
                    }
                } else {
                    seleccionarProducto(prod)
                }
                scannerInput = ""
            }
        }
    }

    fun solicitarCobro() {
        registrarInteraccion()
        when {
            vmV2.carrito.isEmpty() -> scope.launch {
                snackbarHostState.showSnackbar(context.getString(R.string.sales_snackbar_carrito_vacio))
            }
            cajaVm.cargandoTurno -> scope.launch {
                snackbarHostState.showSnackbar(context.getString(R.string.sales_snackbar_validando_turno))
            }
            cajaVm.turnoActivo == null -> scope.launch {
                snackbarHostState.showSnackbar(context.getString(R.string.sales_snackbar_sin_turno))
            }
            else -> mostrarPago = true
        }
    }

    val onKeyEvent: (KeyEvent) -> Boolean = remember {
        { event: KeyEvent ->
            if (event.type == KeyEventType.KeyUp) {
                registrarInteraccion()
                when (event.key) {
                    Key.F1 -> { scannerFocus.requestFocus(); true }
                    Key.F2 -> { solicitarCobro(); true }
                    Key.F3 -> { mostrarBuscarCliente = true; true }
                    Key.Escape -> { mostrarCancelarVentaPin = true; true }
                    else -> false
                }
            } else false
        }
    }

    val uiState by vmV2.uiState.collectAsStateWithLifecycle()
    val isOnline = vmV2.isOnline
    val operacionActiva = mostrarPago || vmV2.carrito.isNotEmpty() || vmV2.cargando
    val bloqueoPermitido = isOnline && !cajaVm.modoContingenciaLocal && !operacionActiva

    val frecuentes = context.getString(R.string.sales_categoria_frecuentes)
    var categoriaSeleccionada by remember { mutableStateOf(frecuentes) }
    val alertasStockEstable by remember {
        derivedStateOf { vmV2.alertasStock.toMap() }
    }

    val filteredProducts = remember(vmV2.catalogoProcesado, categoriaSeleccionada, searchQuery) {
        val catalogo = vmV2.catalogoProcesado ?: return@remember emptyList<SalesInventoryProductV2>()
        val baseProducts = if (categoriaSeleccionada == frecuentes) {
            catalogo.frecuentes
        } else {
            catalogo.vendibles.filter { it.categoria.trim().uppercase(Locale.ROOT) == categoriaSeleccionada }
        }
        baseProducts.filter { prod ->
            searchQuery.isEmpty() || prod.nombre.contains(searchQuery, ignoreCase = true)
        }
    }

    LaunchedEffect(vmV2.mensajeFeedback) {
        vmV2.mensajeFeedback?.let {
            val result = snackbarHostState.showSnackbar(
                message = it,
                actionLabel = if (vmV2.hayUndo) context.getString(R.string.sales_snackbar_deshacer) else null,
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                vmV2.undoLastAction()
            }
            vmV2.mensajeFeedback = null
        }
    }
    LaunchedEffect(vmV2.mensajeError, mostrarPago) {
        vmV2.mensajeError?.let { msg ->
            if (mostrarPago) return@let
            snackbarHostState.showSnackbar(
                message = context.getString(R.string.sales_snackbar_aviso, msg),
                actionLabel = context.getString(R.string.sales_snackbar_ok),
                duration = SnackbarDuration.Long
            )
            vmV2.limpiarError()
        }
    }

    LaunchedEffect(uiState.error, mostrarPago) {
        uiState.error?.let { msg ->
            if (mostrarPago) return@let
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
                    message = context.getString(R.string.sales_snackbar_venta_registrada),
                    duration = SnackbarDuration.Short
                )
                val orderIdToDelete = heldOrderCheckoutPolicy
                    .heldOrderIdToDeleteOnSuccessfulCheckout(vmV2.lastCompletedHeldOrderId)
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
                itemInicial = itemEditando,
                onDismiss = {
                    productoConfigurando = null
                    itemEditando = null
                },
                onConfirm = { config ->
                    val original = itemEditando
                    if (original != null) {
                        vmV2.reemplazarItemCarritoConConfig(original, prod, session.sucursalActual, config)
                    } else {
                        vmV2.agregarAlCarritoConConfig(prod, session.sucursalActual, config)
                    }
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
                titulo = context.getString(R.string.sales_dialog_cancelar_titulo),
                mensaje = context.getString(R.string.sales_dialog_cancelar_mensaje),
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
                error = if (pinCancelError) context.getString(R.string.sales_pin_error) else null
            )
        } else {
            AlertDialog(
                onDismissRequest = { mostrarCancelarVentaPin = false },
                title = { Text(context.getString(R.string.sales_dialog_cancelar_titulo), fontWeight = FontWeight.Bold) },
                text = { Text(if (tieneItems) context.getString(R.string.sales_dialog_confirmar_cancelar) else context.getString(R.string.sales_dialog_sin_items)) },
                confirmButton = {
                    if (tieneItems) {
                        Button(onClick = {
                            vmV2.limpiarCarrito()
                            mostrarCancelarVentaPin = false
                        }) { Text(context.getString(R.string.sales_dialog_boton_cancelar)) }
                    }
                },
                dismissButton = {
                    TextButton(onClick = { mostrarCancelarVentaPin = false }) { Text(context.getString(R.string.sales_dialog_volver)) }
                }
            )
        }
    }
    if (mostrarMermaDialog) {
        DialogMerma(
            productos = vmV2.productos,
            cargando = vmV2.cargando,
            onConfirm = { prod, cant, motivo ->
                vmV2.registrarMermaProducto(prod, cant, motivo, session.sucursalActual, session.nombreUsuario) { exito ->
                    if (exito) mostrarMermaDialog = false
                }
            },
            onDismiss = { mostrarMermaDialog = false }
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

    val navigateFromDrawer: (() -> Unit) -> Unit = { action ->
        scope.launch { drawerState.close() }
        action()
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = drawerState.isOpen,
        drawerContent = {
            BocattaSalesDrawer(
                sucursalActual = session.sucursalActual,
                esAdmin = session.esAdmin,
                isOnline = isOnline,
                onNavigate = navigateFromDrawer,
                onVerCaja = onVerCaja,
                onVerInventario = onVerInventario,
                onVerGastos = onVerGastos,
                onVerDevoluciones = onVerDevoluciones,
                onVerAdmin = onVerAdmin,
                onVerReportes = onVerReportes,
                onVerActividad = onVerActividad,
                onRegistrarMerma = { mostrarMermaDialog = true },
                onLogout = onLogout
            )
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .pointerInput(bloqueoPermitido) {
                    awaitPointerEventScope {
                        while (true) {
                            awaitPointerEvent()
                            if (bloqueoPermitido) {
                                registrarInteraccion()
                            }
                        }
                    }
                }
                .onKeyEvent(onKeyEvent)
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val isTablet = maxWidth >= 720.dp // Meridian Spec: 720dp for tablet layout

                fun apartarOrden() {
                    val oldOrderId = vmV2.activeHeldOrderId
                    heldOrderVm.saveOrder(
                        carrito = vmV2.carrito,
                        cliente = vmV2.clienteSeleccionado,
                        nota = "",
                        sucursal = session.sucursalActual.lowercase(Locale.ROOT),
                        total = (vmV2.totalCarrito.toDouble() - vmV2.descuentoLealtad - vmV2.descuentoPromociones - vmV2.descuentoManual).coerceAtLeast(0.0),
                        modalidad = vmV2.modalidadOrden.name,
                        mesaId = vmV2.mesaIdSeleccionada
                    )
                    if (oldOrderId != null) {
                        heldOrderVm.deleteOrder(oldOrderId, liberarMesa = false)
                    }
                    vmV2.limpiarCarrito()
                }

                Scaffold(
                    contentWindowInsets = WindowInsets.safeDrawing,
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    containerColor = Color.Transparent,
                    topBar = {
                        SalesTopBar(
                            isOnline = isOnline,
                            sucursalActual = session.sucursalActual,
                            isTablet = isTablet,
                            onMenuClick = { scope.launch { drawerState.open() } },
                            onRetiroAlimento = { mostrarRetiroAlimento = true },
                            onCompraRapida = { mostrarCompraRapida = true },
                            onCancelarVenta = { mostrarCancelarVentaPin = true },
                            onVerActividad = onVerActividad,
                            onLogout = onLogout
                        )
                    },
                    bottomBar = {
                        if (!isTablet) {
                            SalesMobileCartBar(
                                cartItemCount = vmV2.carrito.sumOf { it.cantidad },
                                totalCarrito = vmV2.totalCarrito,
                                descuentoLealtad = vmV2.descuentoLealtad,
                                descuentoPromociones = vmV2.descuentoPromociones,
                                descuentoManual = vmV2.descuentoManual,
                                onOpenCart = { mostrarCarritoMobile = true },
                                onCobrar = { solicitarCobro() }
                            )
                        }
                    }
                ) { padding ->
                    Row(modifier = Modifier.padding(padding).fillMaxSize()) {
                        SalesCatalogSection(
                            catalogoProcesado = vmV2.catalogoProcesado,
                            filteredProducts = filteredProducts,
                            categoriaSeleccionada = categoriaSeleccionada,
                            searchQuery = searchQuery,
                            menuCargando = vmV2.menuCargando,
                            menuError = vmV2.menuError,
                            productosCargados = vmV2.productos.isNotEmpty(),
                            alertasStock = alertasStockEstable,
                            sucursalActual = session.sucursalActual,
                            cargandoTurno = cajaVm.cargandoTurno,
                            tieneTurnoActivo = cajaVm.turnoActivo != null,
                            modifier = Modifier.weight(if (isTablet) 0.62f else 1f),
                            onCategoriaSelected = { categoriaSeleccionada = it },
                            onSearchQueryChange = { searchQuery = it },
                            onProductoClick = { seleccionarProducto(it) },
                            onRetryMenu = { vmV2.refrescarMenu() },
                            onVerInventario = onVerInventario,
                            onVerCaja = onVerCaja
                        )

                        if (isTablet) {
                            SalesCartSection(
                                isTablet = isTablet,
                                showMobileCart = false,
                                carrito = vmV2.carrito,
                                totalCarrito = vmV2.totalCarrito,
                                descuentoLealtad = vmV2.descuentoLealtad,
                                descuentoPromociones = vmV2.descuentoPromociones,
                                descuentoManual = vmV2.descuentoManual,
                                clienteSeleccionado = vmV2.clienteSeleccionado,
                                modalidad = vmV2.modalidadOrden,
                                esAdmin = session.esAdmin,
                                mesaId = vmV2.mesaIdSeleccionada,
                                modifier = Modifier.weight(0.38f).fillMaxHeight(),
                                onModalidadChanged = { vmV2.cambiarModalidadOrden(it) },
                                onToggleParaLlevarItem = { vmV2.toggleParaLlevarItem(it) },
                                onEliminarItem = { itemPorEliminar = it },
                                onEditarItem = { item -> itemEditando = item; productoConfigurando = item.producto },
                                onCobrar = { solicitarCobro() },
                                onApplyDiscount = { vmV2.aplicarDescuentoManual(it) },
                                onApartar = { apartarOrden() },
                                onBuscarCliente = { mostrarBuscarCliente = true },
                                onEliminarCliente = { vmV2.eliminarCliente() },
                                onValidarPin = { pin, cb -> session.validarPinAdmin(pin, cb) },
                                onDismissMobileCart = {},
                                lealtadMensaje = vmV2.lealtadMensaje
                            )
                        }
                    }
                }

                if (uiState.isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.4f)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }

                if (mostrarCarritoMobile && !isTablet) {
                    SalesCartSection(
                        isTablet = false,
                        showMobileCart = true,
                        carrito = vmV2.carrito,
                        totalCarrito = vmV2.totalCarrito,
                        descuentoLealtad = vmV2.descuentoLealtad,
                        descuentoPromociones = vmV2.descuentoPromociones,
                        descuentoManual = vmV2.descuentoManual,
                        clienteSeleccionado = vmV2.clienteSeleccionado,
                        modalidad = vmV2.modalidadOrden,
                        esAdmin = session.esAdmin,
                        mesaId = vmV2.mesaIdSeleccionada,
                        modifier = Modifier.fillMaxWidth(),
                        onModalidadChanged = { vmV2.cambiarModalidadOrden(it) },
                        onToggleParaLlevarItem = { vmV2.toggleParaLlevarItem(it) },
                        onEliminarItem = { itemPorEliminar = it; mostrarCarritoMobile = false },
                        onEditarItem = { item -> itemEditando = item; productoConfigurando = item.producto; mostrarCarritoMobile = false },
                        onCobrar = { mostrarCarritoMobile = false; solicitarCobro() },
                        onApplyDiscount = { vmV2.aplicarDescuentoManual(it) },
                        onApartar = {
                            apartarOrden()
                            mostrarCarritoMobile = false
                        },
                        onBuscarCliente = { mostrarBuscarCliente = true },
                        onEliminarCliente = { vmV2.eliminarCliente() },
                        onValidarPin = { pin, cb -> session.validarPinAdmin(pin, cb) },
                        onDismissMobileCart = { mostrarCarritoMobile = false },
                        lealtadMensaje = vmV2.lealtadMensaje
                    )
                }
            }
        }
    }

    LaunchedEffect(bloqueoPermitido, pantallaBloqueada, operacionActiva) {
        if (!bloqueoPermitido && pantallaBloqueada) {
            pantallaBloqueada = false
            registrarInteraccion()
            if (operacionActiva) {
                snackbarHostState.showSnackbar(context.getString(R.string.sales_bloqueo_venta))
            } else {
                snackbarHostState.showSnackbar(context.getString(R.string.sales_bloqueo_contingencia))
            }
        }
    }

    // Temporizador de inactividad (5 minutos)
    LaunchedEffect(ultimaInteraccion, bloqueoPermitido, operacionActiva) {
        if (!bloqueoPermitido) return@LaunchedEffect
        delay(5 * 60 * 1000L)
        if (!pantallaBloqueada && bloqueoPermitido) {
            pantallaBloqueada = true
        }
    }

    // Reset defensivo cuando cambia el carrito o entra scanner.
    LaunchedEffect(vmV2.carrito.size, scannerInput) {
        registrarInteraccion()
    }

    if (pantallaBloqueada && bloqueoPermitido) {
        PantallaLockInactividad(
            usuarioNombre = session.nombreUsuario,
            onDesbloquear = { pin, resultCallback ->
                session.validarPinDesbloqueo(pin) { valido ->
                    if (valido) {
                        pantallaBloqueada = false
                        ultimaInteraccion = System.currentTimeMillis()
                    }
                    resultCallback(valido)
                }
            },
            onLogout = onLogout
        )
    }
}
