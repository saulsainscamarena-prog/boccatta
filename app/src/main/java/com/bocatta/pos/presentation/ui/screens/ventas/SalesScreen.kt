package com.bocatta.pos.presentation.ui.screens.ventas

import androidx.compose.animation.*
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bocatta.pos.domain.model.*
import com.bocatta.pos.presentation.ui.components.*
import com.bocatta.pos.presentation.ui.theme.*
import com.bocatta.pos.presentation.viewmodel.*

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
    var searchQuery by remember { mutableStateOf("") }
    var mostrarCarritoMobile by remember { mutableStateOf(false) }
    val isOnline = vmV2.isOnline

    val categorias = remember(vmV2.productos) {
        vmV2.productos
            .map { it.categoria.uppercase() }
            .distinct()
            .sorted()
    }
    var categoriaSeleccionada by remember { mutableStateOf("TODOS") }

    val filteredProducts = remember(vmV2.productos, categoriaSeleccionada, searchQuery) {
        vmV2.productos.filter { prod ->
            val catNorm = prod.categoria.uppercase()
            (categoriaSeleccionada == "TODOS" || catNorm == categoriaSeleccionada) &&
            (searchQuery.isEmpty() || prod.nombre.contains(searchQuery, ignoreCase = true))
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(vmV2.mensajeFeedback) {
        vmV2.mensajeFeedback?.let {
            snackbarHostState.showSnackbar(it)
            vmV2.mensajeFeedback = null
        }
    }
    LaunchedEffect(vmV2.mensajeError) {
        vmV2.mensajeError?.let {
            snackbarHostState.showSnackbar(it)
            vmV2.limpiarError()
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

    productoConfigurando?.let { prod ->
        val cat = prod.categoria.uppercase()
        val requiereBuilder = cat.contains("CREPA") || cat == "COMBOS"

        if (requiereBuilder) {
            CrepeBuilderDialog(
                producto = prod,
                vmV2 = vmV2,
                sucursal = session.sucursalActual,
                onDismiss = { productoConfigurando = null },
                onAddToCart = { p, base, aderezo, toppings, esSeparado ->
                    vmV2.agregarAlCarrito(p, session.sucursalActual, base, aderezo, toppings, esSeparado)
                    productoConfigurando = null
                }
            )
        } else {
            vmV2.agregarAlCarrito(prod, session.sucursalActual, null, null, emptyList(), false)
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

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isTablet = maxWidth >= 720.dp // Meridian Spec: 720dp for tablet layout
        
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = Color.Transparent,
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("BOCATTA POS", fontWeight = FontWeight.Black, fontSize = 20.sp, letterSpacing = 2.sp)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(8.dp).background(if (isOnline) BocattaSuccess else BocattaDanger, CircleShape))
                                Spacer(Modifier.width(6.dp))
                                Text(if (isOnline) "SISTEMA ONLINE" else "SISTEMA OFFLINE", style = MaterialTheme.typography.labelSmall, color = if (isOnline) BocattaSuccess else BocattaDanger)
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onVerCaja) { Icon(Icons.Default.Menu, contentDescription = "Menú") }
                    },
                    actions = {
                        IconButton(onClick = { mostrarBuscarCliente = true }) {
                            Icon(Icons.Default.PersonAdd, contentDescription = "Cliente", tint = if (vmV2.clienteSeleccionado != null) BocattaPrimary else MaterialTheme.colorScheme.onSurface)
                        }
                        IconButton(onClick = { mostrarRetiroAlimento = true }) {
                            Icon(Icons.Default.Restaurant, contentDescription = "Retiro Alimento")
                        }
                        IconButton(onClick = onLogout) { Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Salir") }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
                )
            },
            bottomBar = {
                if (!isTablet) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(0.9f),
                        modifier = Modifier.fillMaxWidth().height(80.dp).clickable { mostrarCarritoMobile = true },
                        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
                    ) {
                        Row(Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("${vmV2.carrito.sumOf { it.cantidad }} items", style = MaterialTheme.typography.labelSmall)
                                val total = (vmV2.totalCarrito.toDouble() - vmV2.descuentoLealtad - vmV2.descuentoPromociones).coerceAtLeast(0.0)
                                Text("$${"%.2f".format(total)}", fontWeight = FontWeight.Black, fontSize = 24.sp, color = BocattaPrimary)
                            }
                            NeonButton(texto = "COBRAR", onClick = { mostrarPago = true }, modifier = Modifier.width(140.dp))
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
                    ScrollableTabRow(
                        selectedTabIndex = if (categoriaSeleccionada == "TODOS") 0 else (categorias.indexOf(categoriaSeleccionada) + 1).coerceAtLeast(0),
                        containerColor = Color.Transparent,
                        edgePadding = 16.dp,
                        divider = {},
                        indicator = { tabPositions ->
                             val index = if (categoriaSeleccionada == "TODOS") 0 else (categorias.indexOf(categoriaSeleccionada) + 1).coerceAtLeast(0)
                             TabRowDefaults.SecondaryIndicator(
                                 modifier = Modifier.tabIndicatorOffset(tabPositions[index]),
                                 color = BocattaPrimary
                             )
                        }
                    ) {
                        Tab(selected = categoriaSeleccionada == "TODOS", onClick = { categoriaSeleccionada = "TODOS" }) {
                            Text("TODOS", modifier = Modifier.padding(16.dp), fontWeight = if (categoriaSeleccionada == "TODOS") FontWeight.Black else FontWeight.Normal, fontSize = 12.sp)
                        }
                        categorias.forEach { cat ->
                            Tab(selected = categoriaSeleccionada == cat, onClick = { categoriaSeleccionada = cat }) {
                                Text(cat, modifier = Modifier.padding(16.dp), fontWeight = if (categoriaSeleccionada == cat) FontWeight.Black else FontWeight.Normal, fontSize = 12.sp)
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
                        clienteSeleccionado = vmV2.clienteSeleccionado,
                        modifier = Modifier.weight(0.38f).fillMaxHeight(),
                        onEliminarItem = { itemPorEliminar = it },
                        onCobrar = { mostrarPago = true }
                    )
                }
            }
        }

        if (mostrarCarritoMobile && !isTablet) {
            ModalBottomSheet(onDismissRequest = { mostrarCarritoMobile = false }, shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)) {
                CarritoPanelV2(vmV2.carrito, vmV2.totalCarrito, vmV2.descuentoLealtad, vmV2.descuentoPromociones, vmV2.clienteSeleccionado, Modifier.fillMaxWidth(), { itemPorEliminar = it; mostrarCarritoMobile = false }, { mostrarCarritoMobile = false; mostrarPago = true })
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}
