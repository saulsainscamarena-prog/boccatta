package com.bocatta.pos.feature.ventas.ui.screens.ventas

import com.bocatta.pos.feature.ventas.ui.components.*

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.LockClock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bocatta.pos.domain.model.ClienteV2
import com.bocatta.pos.domain.model.ItemCarritoV2
import com.bocatta.pos.domain.model.ModalidadOrden
import com.bocatta.pos.domain.model.SalesInventoryProductV2
import com.bocatta.pos.domain.usecase.CatalogoProcesado
import com.bocatta.pos.presentation.ui.components.BocattaSearchBar
import com.bocatta.pos.presentation.ui.components.NeonButton
import com.bocatta.pos.presentation.ui.components.ProductCardPremium
import com.bocatta.pos.presentation.ui.theme.bocattaSemanticColors
import com.bocatta.pos.core.ui.R
import java.math.BigDecimal
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SalesTopBar(
    isOnline: Boolean,
    sucursalActual: String,
    isTablet: Boolean,
    usaRecetas: Boolean,
    onMenuClick: () -> Unit,
    onRetiroAlimento: () -> Unit,
    onCompraRapida: () -> Unit,
    onCancelarVenta: () -> Unit,
    onVerActividad: () -> Unit,
    onLogout: () -> Unit
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    "BOCATTA POS",
                    fontWeight = FontWeight.Black,
                    fontSize = 21.sp,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(8.dp)
                            .background(
                                if (isOnline) MaterialTheme.bocattaSemanticColors.success
                                else MaterialTheme.colorScheme.error,
                                CircleShape
                            )
                    )
                    Spacer(Modifier.width(6.dp))
                    val statusColor = if (isOnline) {
                        MaterialTheme.bocattaSemanticColors.success
                    } else {
                        MaterialTheme.colorScheme.error
                    }
                    Text(
                        "${stringResource(if (isOnline) R.string.sales_system_online else R.string.sales_system_offline)} - ${sucursalActual.uppercase(Locale.ROOT)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor.copy(0.8f),
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }
        },
        navigationIcon = {
            if (!isTablet) {
                IconButton(onClick = onMenuClick) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = CircleShape,
                        modifier = Modifier.size(42.dp)
                    ) {
                        Icon(
                            Icons.Default.Menu,
                            stringResource(R.string.sales_open_menu),
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }
        },
        actions = {
            if (isTablet) {
                if (usaRecetas) {
                    TopBarAction(
                        icon = Icons.Default.Restaurant,
                        description = stringResource(R.string.sales_action_food_withdrawal),
                        onClick = onRetiroAlimento
                    )
                }
                TopBarAction(
                    icon = Icons.Default.AddShoppingCart,
                    description = stringResource(R.string.sales_action_quick_purchase),
                    tint = MaterialTheme.bocattaSemanticColors.success,
                    containerColor = MaterialTheme.bocattaSemanticColors.successContainer,
                    onClick = onCompraRapida
                )
                TopBarAction(
                    icon = Icons.Default.Block,
                    description = stringResource(R.string.sales_action_cancel_sale),
                    tint = MaterialTheme.colorScheme.error,
                    containerColor = MaterialTheme.colorScheme.error.copy(0.1f),
                    onClick = onCancelarVenta
                )
                TopBarAction(
                    icon = Icons.Default.Bookmark,
                    description = stringResource(R.string.sales_action_activity),
                    tint = MaterialTheme.bocattaSemanticColors.success,
                    containerColor = MaterialTheme.bocattaSemanticColors.successContainer,
                    onClick = onVerActividad
                )
                TopBarAction(
                    icon = Icons.AutoMirrored.Filled.Logout,
                    description = stringResource(R.string.sales_nav_logout),
                    tint = MaterialTheme.colorScheme.error,
                    containerColor = MaterialTheme.colorScheme.error.copy(0.1f),
                    onClick = onLogout
                )
                Spacer(Modifier.width(16.dp))
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            titleContentColor = MaterialTheme.colorScheme.onBackground
        )
    )
}

@Composable
private fun RowScope.TopBarAction(
    icon: ImageVector,
    description: String,
    tint: Color = MaterialTheme.colorScheme.onBackground,
    containerColor: Color = MaterialTheme.colorScheme.onBackground.copy(0.05f),
    onClick: () -> Unit
) {
    IconButton(onClick = onClick) {
        Surface(color = containerColor, shape = CircleShape, modifier = Modifier.size(40.dp)) {
            Icon(icon, description, tint = tint, modifier = Modifier.padding(10.dp))
        }
    }
    Spacer(Modifier.width(8.dp))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SalesCatalogSection(
    catalogoProcesado: CatalogoProcesado?,
    filteredProducts: List<SalesInventoryProductV2>,
    categoriaSeleccionada: String,
    searchQuery: String,
    menuCargando: Boolean,
    menuError: String?,
    productosCargados: Boolean,
    alertasStock: Map<String, Double>,
    sucursalActual: String,
    cargandoTurno: Boolean,
    tieneTurnoActivo: Boolean,
    modifier: Modifier = Modifier,
    onCategoriaSelected: (String) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onProductoClick: (SalesInventoryProductV2) -> Unit,
    onRetryMenu: () -> Unit,
    onVerInventario: () -> Unit,
    onVerCaja: () -> Unit
) {
    Column(modifier = modifier.fillMaxHeight()) {
        Box(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            BocattaSearchBar(
                query = searchQuery,
                onQueryChange = onSearchQueryChange,
                placeholder = "Buscar en menu..."
            )
        }

        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            when {
                cargandoTurno -> SalesStatusBanner(
                    icon = Icons.Default.Schedule,
                    title = "Validando turno",
                    message = "Revisando la apertura activa de ${sucursalActual.uppercase(Locale.ROOT)}.",
                    actionText = null,
                    onAction = null
                )
                !tieneTurnoActivo -> SalesStatusBanner(
                    icon = Icons.Default.LockOpen,
                    title = "Turno inactivo",
                    message = "Para cobrar necesitas iniciar o unirte a un turno.",
                    actionText = "IR A CAJA",
                    onAction = onVerCaja
                )
            }
            if (menuError != null) {
                SalesStatusBanner(
                    icon = Icons.Default.CloudOff,
                    title = "Menu no disponible",
                    message = menuError,
                    actionText = "REINTENTAR",
                    onAction = onRetryMenu,
                    isError = true
                )
            } else if (!menuCargando && productosCargados && alertasStock.isEmpty()) {
                SalesStatusBanner(
                    icon = Icons.Default.Inventory,
                    title = "Stock de sucursal pendiente",
                    message = "El menu cargo, pero aun no hay asignacion visible para esta sucursal.",
                    actionText = "ASIGNAR",
                    onAction = onVerInventario
                )
            }
        }

        val categoriasVisibles = catalogoProcesado?.categoriasVisibles ?: emptyList()
        PrimaryScrollableTabRow(
            selectedTabIndex = if (categoriaSeleccionada == "FRECUENTES") {
                0
            } else {
                (categoriasVisibles.indexOf(categoriaSeleccionada) + 1).coerceAtLeast(0)
            },
            containerColor = Color.Transparent,
            edgePadding = 16.dp,
            divider = {}
        ) {
            SalesCategoryTab(
                label = "FRECUENTES",
                selected = categoriaSeleccionada == "FRECUENTES",
                onClick = { onCategoriaSelected("FRECUENTES") }
            )
            categoriasVisibles.forEach { cat ->
                SalesCategoryTab(
                    label = cat,
                    selected = categoriaSeleccionada == cat,
                    onClick = { onCategoriaSelected(cat) }
                )
            }
        }

        BoxWithConstraints(Modifier.weight(1f).padding(horizontal = 16.dp)) {
            val productCardMinWidth = if (maxWidth >= 600.dp) 144.dp else 112.dp
            when {
                menuCargando && !productosCargados -> SalesEmptyState(
                    icon = Icons.Default.Sync,
                    title = "Cargando menu",
                    message = "Estamos trayendo productos, precios y configuraciones.",
                    actionText = null,
                    onAction = null
                )
                filteredProducts.isEmpty() -> SalesEmptyState(
                    icon = Icons.Default.SearchOff,
                    title = if (!productosCargados) "No hay productos cargados" else "Sin resultados",
                    message = if (!productosCargados) {
                        "Carga V2 o toca reintentar para volver a leer el menu."
                    } else {
                        "No encontramos productos con ese filtro."
                    },
                    actionText = if (!productosCargados) "REINTENTAR" else "LIMPIAR BUSQUEDA",
                    onAction = {
                        if (!productosCargados) onRetryMenu() else onSearchQueryChange("")
                    }
                )
                else -> LazyVerticalGrid(
                    columns = GridCells.Adaptive(productCardMinWidth),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredProducts, key = { it.id }) { prod ->
                        val precio = prod.precioVenta[sucursalActual.lowercase(Locale.ROOT)] ?: 0.0
                        ProductCardPremium(
                            nombre = prod.nombre,
                            precio = precio,
                            emoji = prod.emoji,
                            categoria = prod.categoria,
                            agotado = (alertasStock[prod.id] ?: 99.0) <= 0,
                            pocoStock = (alertasStock[prod.id] ?: 99.0) in 0.1..<5.0,
                            personalizable = prod.configSchema.isNotEmpty(),
                            esPorPeso = prod.porPeso,
                            onClick = { onProductoClick(prod) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SalesCategoryTab(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Tab(selected = selected, onClick = onClick) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            fontWeight = if (selected) FontWeight.Black else FontWeight.Normal,
            fontSize = 12.sp,
            color = if (selected) {
                MaterialTheme.colorScheme.onBackground
            } else {
                MaterialTheme.colorScheme.onBackground.copy(0.5f)
            }
        )
    }
}

@Composable
internal fun SalesMobileCartBar(
    cartItemCount: Int,
    totalCarrito: BigDecimal,
    descuentoLealtad: Double,
    descuentoPromociones: Double,
    descuentoManual: Double,
    onOpenCart: () -> Unit,
    onCobrar: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier
            .fillMaxWidth()
            .height(76.dp)
            .clickable { onOpenCart() },
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(0.2f))
    ) {
        Row(
            Modifier.padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    "ORDEN - $cartItemCount ITEMS",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
                val total = (
                    totalCarrito.toDouble() -
                        descuentoLealtad -
                        descuentoPromociones -
                        descuentoManual
                    ).coerceAtLeast(0.0)
                Text(
                    "$${"%.2f".format(total)}",
                    fontWeight = FontWeight.Black,
                    fontSize = 24.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            NeonButton(
                texto = "COBRAR",
                onClick = onCobrar,
                modifier = Modifier.width(148.dp),
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SalesCartSection(
    isTablet: Boolean,
    showMobileCart: Boolean,
    carrito: List<ItemCarritoV2>,
    totalCarrito: BigDecimal,
    descuentoLealtad: Double,
    descuentoPromociones: Double,
    descuentoManual: Double,
    clienteSeleccionado: ClienteV2?,
    modalidad: ModalidadOrden,
    esAdmin: Boolean,
    mesaId: String?,
    modifier: Modifier = Modifier,
    onModalidadChanged: (ModalidadOrden) -> Unit,
    onToggleParaLlevarItem: (String) -> Unit,
    onEliminarItem: (ItemCarritoV2) -> Unit,
    onEditarItem: (ItemCarritoV2) -> Unit,
    onCobrar: () -> Unit,
    onApplyDiscount: (Int) -> Unit,
    onApartar: () -> Unit,
    onBuscarCliente: () -> Unit,
    onEliminarCliente: () -> Unit,
    onValidarPin: (String, (Boolean) -> Unit) -> Unit,
    onDismissMobileCart: () -> Unit,
    lealtadMensaje: String? = null
) {
    if (isTablet) {
        VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(0.3f))
        SalesCartPanel(
            carrito = carrito,
            totalCarrito = totalCarrito,
            descuentoLealtad = descuentoLealtad,
            descuentoPromociones = descuentoPromociones,
            descuentoManual = descuentoManual,
            clienteSeleccionado = clienteSeleccionado,
            modalidad = modalidad,
            esAdmin = esAdmin,
            mesaId = mesaId,
            modifier = modifier,
            onModalidadChanged = onModalidadChanged,
            onToggleParaLlevarItem = onToggleParaLlevarItem,
            onEliminarItem = onEliminarItem,
            onEditarItem = onEditarItem,
            onCobrar = onCobrar,
            onApplyDiscount = onApplyDiscount,
            onApartar = onApartar,
            onBuscarCliente = onBuscarCliente,
            onEliminarCliente = onEliminarCliente,
            onValidarPin = onValidarPin,
            lealtadMensaje = lealtadMensaje
        )
    } else if (showMobileCart) {
        ModalBottomSheet(
            onDismissRequest = onDismissMobileCart,
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
        ) {
            SalesCartPanel(
                carrito = carrito,
                totalCarrito = totalCarrito,
                descuentoLealtad = descuentoLealtad,
                descuentoPromociones = descuentoPromociones,
                descuentoManual = descuentoManual,
                clienteSeleccionado = clienteSeleccionado,
                modalidad = modalidad,
                esAdmin = esAdmin,
                mesaId = mesaId,
                modifier = Modifier.fillMaxWidth(),
                onModalidadChanged = onModalidadChanged,
                onToggleParaLlevarItem = onToggleParaLlevarItem,
                onEliminarItem = onEliminarItem,
                onEditarItem = onEditarItem,
                onCobrar = onCobrar,
                onApplyDiscount = onApplyDiscount,
                onApartar = onApartar,
                onBuscarCliente = onBuscarCliente,
                onEliminarCliente = onEliminarCliente,
                onValidarPin = onValidarPin,
                lealtadMensaje = lealtadMensaje
            )
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SalesCartPanel(
    carrito: List<ItemCarritoV2>,
    totalCarrito: BigDecimal,
    descuentoLealtad: Double,
    descuentoPromociones: Double,
    descuentoManual: Double,
    clienteSeleccionado: ClienteV2?,
    modalidad: ModalidadOrden,
    esAdmin: Boolean,
    mesaId: String?,
    modifier: Modifier,
    onModalidadChanged: (ModalidadOrden) -> Unit,
    onToggleParaLlevarItem: (String) -> Unit,
    onEliminarItem: (ItemCarritoV2) -> Unit,
    onEditarItem: (ItemCarritoV2) -> Unit,
    onCobrar: () -> Unit,
    onApplyDiscount: (Int) -> Unit,
    onApartar: () -> Unit,
    onBuscarCliente: () -> Unit,
    onEliminarCliente: () -> Unit,
    onValidarPin: (String, (Boolean) -> Unit) -> Unit,
    lealtadMensaje: String? = null
) {
    CarritoPanelV2(
        carrito = carrito,
        totalCarrito = totalCarrito,
        descuentoLealtad = descuentoLealtad,
        descuentoPromociones = descuentoPromociones,
        descuentoManual = descuentoManual,
        clienteSeleccionado = clienteSeleccionado,
        modalidad = modalidad,
        onModalidadChanged = onModalidadChanged,
        onToggleParaLlevarItem = onToggleParaLlevarItem,
        modifier = modifier,
        onEliminarItem = onEliminarItem,
        onEditarItem = onEditarItem,
        onCobrar = onCobrar,
        onApplyDiscount = onApplyDiscount,
        onApartar = onApartar,
        onBuscarCliente = onBuscarCliente,
        onEliminarCliente = onEliminarCliente,
        esAdmin = esAdmin,
        onValidarPin = onValidarPin,
        mesaId = mesaId,
        lealtadMensaje = lealtadMensaje
    )
}

@Composable
internal fun BocattaSalesDrawer(
    sucursalActual: String,
    esAdmin: Boolean,
    isOnline: Boolean,
    onNavigate: (() -> Unit) -> Unit,
    onVerCaja: () -> Unit,
    onVerInventario: () -> Unit,
    onVerGastos: () -> Unit,
    onVerDevoluciones: () -> Unit,
    onVerAdmin: () -> Unit,
    onVerReportes: () -> Unit,
    onVerActividad: () -> Unit,
    onRegistrarMerma: () -> Unit,
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
                    "${sucursalActual.uppercase(Locale.ROOT)} - ${if (isOnline) "online" else "offline"}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            DrawerGroup(stringResource(R.string.sales_nav_sale))
            DrawerItem(stringResource(R.string.sales_nav_pos), Icons.Default.PointOfSale, selected = true) { }
            DrawerItem(stringResource(R.string.sales_nav_activity), Icons.Default.Restaurant) {
                onNavigate(onVerActividad)
            }

            DrawerGroup(stringResource(R.string.sales_nav_operation))
            DrawerItem(stringResource(R.string.sales_nav_cash), Icons.Default.Payments) { onNavigate(onVerCaja) }
            DrawerItem(stringResource(R.string.sales_nav_expenses), Icons.AutoMirrored.Filled.ReceiptLong) {
                onNavigate(onVerGastos)
            }
            DrawerItem(stringResource(R.string.sales_nav_returns), Icons.AutoMirrored.Filled.Undo) {
                onNavigate(onVerDevoluciones)
            }
            DrawerItem(stringResource(R.string.sales_nav_waste), Icons.Default.DeleteForever) {
                onNavigate(onRegistrarMerma)
            }

            DrawerGroup(stringResource(R.string.sales_nav_inventory))
            DrawerItem(stringResource(R.string.sales_nav_inventory), Icons.Default.Inventory) {
                onNavigate(onVerInventario)
            }

            if (esAdmin) {
                DrawerGroup(stringResource(R.string.sales_nav_admin))
                DrawerItem(stringResource(R.string.sales_nav_control_center), Icons.Default.AdminPanelSettings) {
                    onNavigate(onVerAdmin)
                }
                DrawerItem(stringResource(R.string.sales_nav_reports), Icons.Default.Assessment) {
                    onNavigate(onVerReportes)
                }
            }

            Spacer(Modifier.weight(1f))
            HorizontalDivider()
            DrawerItem(
                stringResource(R.string.sales_nav_logout),
                Icons.AutoMirrored.Filled.Logout,
                danger = true
            ) { onNavigate(onLogout) }
        }
    }
}

@Composable
fun BocattaSalesNavigationRail(
    esAdmin: Boolean,
    onVerCaja: () -> Unit,
    onVerInventario: () -> Unit,
    onVerGastos: () -> Unit,
    onVerDevoluciones: () -> Unit,
    onVerAdmin: () -> Unit,
    onVerReportes: () -> Unit,
    onVerActividad: () -> Unit
) {
    NavigationRail(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.fillMaxHeight()
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            SalesRailItem(
                label = stringResource(R.string.sales_nav_pos),
                icon = Icons.Default.PointOfSale,
                selected = true,
                onClick = {}
            )
            SalesRailItem(
                label = stringResource(R.string.sales_nav_activity),
                icon = Icons.Default.Restaurant,
                onClick = onVerActividad
            )
            SalesRailItem(
                label = stringResource(R.string.sales_nav_cash),
                icon = Icons.Default.Payments,
                onClick = onVerCaja
            )
            SalesRailItem(
                label = stringResource(R.string.sales_nav_expenses),
                icon = Icons.AutoMirrored.Filled.ReceiptLong,
                onClick = onVerGastos
            )
            SalesRailItem(
                label = stringResource(R.string.sales_nav_returns),
                icon = Icons.AutoMirrored.Filled.Undo,
                onClick = onVerDevoluciones
            )
            SalesRailItem(
                label = stringResource(R.string.sales_nav_inventory),
                icon = Icons.Default.Inventory,
                onClick = onVerInventario
            )
            if (esAdmin) {
                SalesRailItem(
                    label = stringResource(R.string.sales_nav_control_center),
                    icon = Icons.Default.AdminPanelSettings,
                    onClick = onVerAdmin
                )
                SalesRailItem(
                    label = stringResource(R.string.sales_nav_reports),
                    icon = Icons.Default.Assessment,
                    onClick = onVerReportes
                )
            }
        }
    }
}

@Composable
private fun SalesRailItem(
    label: String,
    icon: ImageVector,
    selected: Boolean = false,
    onClick: () -> Unit
) {
    NavigationRailItem(
        selected = selected,
        onClick = onClick,
        icon = { Icon(icon, contentDescription = label) },
        label = { Text(label, maxLines = 1) },
        alwaysShowLabel = false
    )
}

@Composable
private fun DrawerGroup(label: String) {
    Text(
        label.uppercase(Locale.ROOT),
        modifier = Modifier.padding(top = 10.dp, start = 12.dp, bottom = 2.dp),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Black,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun DrawerItem(
    label: String,
    icon: ImageVector,
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
    icon: ImageVector,
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
    icon: ImageVector,
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
                Text(
                    message,
                    textAlign = TextAlign.Center,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(0.62f)
                )
                if (actionText != null && onAction != null) {
                    Button(onClick = onAction, shape = RoundedCornerShape(14.dp)) {
                        Text(actionText, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}
