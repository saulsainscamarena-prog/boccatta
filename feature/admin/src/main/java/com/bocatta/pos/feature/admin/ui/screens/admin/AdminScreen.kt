package com.bocatta.pos.feature.admin.ui.screens.admin

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bocatta.pos.core.ui.R
import com.bocatta.pos.domain.model.CategoriaProducto
import com.bocatta.pos.presentation.ui.components.BocattaLoadingDialog
import com.bocatta.pos.feature.admin.viewmodel.AdminViewModel
import com.bocatta.pos.feature.admin.viewmodel.CatalogoViewModel
import com.bocatta.pos.feature.admin.viewmodel.ConfigGlobalViewModel
import com.bocatta.pos.feature.admin.viewmodel.ComboViewModel
import com.bocatta.pos.feature.ventas.viewmodel.MesaViewModel
import com.bocatta.pos.feature.admin.viewmodel.PromocionViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AdminScreen(
    vm: AdminViewModel,
    inventoryVm: com.bocatta.pos.feature.inventario.viewmodel.InventoryViewModel,
    session: com.bocatta.pos.feature.auth.viewmodel.SessionViewModel,
    onBack: () -> Unit,
    onVerClientes: () -> Unit,
    onVerDashboardBodega: () -> Unit = {},
    onVerReportesInventario: () -> Unit = {},
    onVerSyncInventario: () -> Unit = {},
    onVerGestionarSucursales: () -> Unit = {}
) {
    val snackbarHost = remember { SnackbarHostState() }
    var seccionActiva by remember { mutableStateOf<String?>("dashboard") }
    var subTabSeleccionado by remember { mutableIntStateOf(0) }

    LaunchedEffect(seccionActiva) {
        subTabSeleccionado = 0
        if (seccionActiva == "empleados") {
            vm.escucharEmpleadosOperativos()
        }
    }

    LaunchedEffect(vm.mensajeExito, vm.mensajeError) {
        val msg = vm.mensajeExito ?: vm.mensajeError ?: return@LaunchedEffect
        snackbarHost.showSnackbar(msg)
        vm.limpiarMensajes()
    }

    if (vm.seederEnProgreso) {
        BocattaLoadingDialog()
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(snackbarHost) },
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            when (seccionActiva) {
                                "menu" -> stringResource(R.string.admin_menu_precios)
                                "recetas" -> stringResource(R.string.admin_recetas_prod)
                                "inventario" -> stringResource(R.string.admin_almacen_inventario)
                                "empleados" -> stringResource(R.string.admin_empleados_permisos)
                                "reportes" -> stringResource(R.string.admin_reportes_auditoria)
                                "config" -> stringResource(R.string.admin_ajustes_config)
                                else -> stringResource(R.string.admin_control_central)
                            },
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Black,
                            fontSize = 17.sp
                        )
                        Text(
                            if (seccionActiva == "dashboard") stringResource(R.string.admin_panel_general)
                            else stringResource(R.string.admin_panel_seccion, seccionActiva!!.uppercase(java.util.Locale.getDefault())),
                            color = MaterialTheme.colorScheme.onPrimary.copy(0.7f),
                            fontSize = 10.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (seccionActiva == "dashboard") {
                            onBack()
                        } else {
                            seccionActiva = "dashboard"
                        }
                    }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.admin_volver),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (seccionActiva != "dashboard") {
                val subTabLabels = when (seccionActiva) {
                    "recetas" -> listOf(
                        stringResource(R.string.admin_subtab_recetas),
                        stringResource(R.string.admin_subtab_produccion),
                        stringResource(R.string.admin_subtab_costos)
                    )
                    "empleados" -> listOf(
                        stringResource(R.string.admin_subtab_empleados),
                        stringResource(R.string.admin_subtab_sueldos)
                    )
                    "reportes" -> listOf(
                        stringResource(R.string.admin_subtab_auditoria),
                        stringResource(R.string.admin_subtab_reportes)
                    )
                    "config" -> listOf(
                        stringResource(R.string.admin_subtab_config_global),
                        stringResource(R.string.admin_subtab_config_negocio),
                        stringResource(R.string.admin_subtab_apariencia),
                        stringResource(R.string.admin_subtab_notificaciones)
                    )
                    else -> emptyList()
                }

                if (subTabLabels.isNotEmpty()) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 1.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            subTabLabels.forEachIndexed { i, label ->
                                FilterChip(
                                    selected = subTabSeleccionado == i,
                                    onClick = { subTabSeleccionado = i },
                                    label = {
                                        Text(
                                            label,
                                            fontWeight = if (subTabSeleccionado == i) FontWeight.Bold else FontWeight.Normal,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    shape = RoundedCornerShape(14.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                )
                            }
                        }
                    }
                }
            }

            when (seccionActiva) {
                "dashboard" -> {
                    Column(Modifier.fillMaxSize()) {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            modifier = Modifier.fillMaxWidth().height(330.dp),
                            contentPadding = PaddingValues(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            item {
                                DashboardCardPremium(
                                    title = vm.businessFeatures.labelCatalogo,
                                    subtitle = stringResource(R.string.admin_card_sub_productos, vm.productos.size),
                                    icon = Icons.AutoMirrored.Filled.MenuBook,
                                    color = MaterialTheme.colorScheme.primary,
                                    onClick = { seccionActiva = "menu" }
                                )
                            }
                            if (vm.businessFeatures.usaRecetas) {
                                item {
                                    DashboardCardPremium(
                                        title = stringResource(R.string.admin_card_recetas),
                                        subtitle = stringResource(R.string.admin_card_sub_config),
                                        icon = Icons.AutoMirrored.Filled.ReceiptLong,
                                        color = MaterialTheme.colorScheme.secondary,
                                        onClick = { seccionActiva = "recetas" }
                                    )
                                }
                            }
                            if (vm.businessFeatures.usaRecetas || vm.businessFeatures.usaVariantesRetail) {
                                item {
                                    DashboardCardPremium(
                                        title = stringResource(R.string.admin_card_inventario),
                                        subtitle = if (vm.businessFeatures.usaRecetas) stringResource(R.string.admin_card_sub_insumos, vm.insumosMaestros.size) else "Stock disponible",
                                        icon = Icons.Default.Warehouse,
                                        color = MaterialTheme.colorScheme.tertiary,
                                        onClick = { seccionActiva = "inventario" }
                                    )
                                }
                            }
                            item {
                                DashboardCardPremium(
                                    title = stringResource(R.string.admin_card_personal),
                                    subtitle = stringResource(R.string.admin_card_sub_permisos),
                                    icon = Icons.Default.People,
                                    color = MaterialTheme.colorScheme.tertiary,
                                    onClick = { seccionActiva = "empleados" }
                                )
                            }
                            item {
                                DashboardCardPremium(
                                    title = stringResource(R.string.admin_card_reportes),
                                    subtitle = stringResource(R.string.admin_card_sub_estadisticas),
                                    icon = Icons.Default.Analytics,
                                    color = MaterialTheme.colorScheme.secondary,
                                    onClick = { seccionActiva = "reportes" }
                                )
                            }
                            item {
                                DashboardCardPremium(
                                    title = stringResource(R.string.admin_card_ajustes),
                                    subtitle = stringResource(R.string.admin_card_sub_parametros),
                                    icon = Icons.Default.Settings,
                                    color = MaterialTheme.colorScheme.primary,
                                    onClick = { seccionActiva = "config" }
                                )
                            }
                        }

                        Box(modifier = Modifier.weight(1f)) {
                            TabDashboard(
                                vm = vm,
                                onComenzarConfiguracion = { seccionActiva = "config" },
                                onValidarPin = { pin, resultado -> session.validarPinAdmin(pin, resultado) }
                            )
                        }
                    }
                }
                "menu" -> TabMenu(vm = vm)
                "recetas" -> when (subTabSeleccionado) {
                    0 -> TabRecetas(vm = vm)
                    1 -> TabProduccion(inventoryVm)
                    2 -> TabCostosInsumos(vm = vm)
                }
                "inventario" -> TabBodegaGeneral(
                    vm = vm,
                    nombreUsuario = session.nombreUsuario,
                    usuarioId = session.uid,
                    sucursal = session.sucursalActual,
                    onVerDashboardBodega = onVerDashboardBodega,
                    onVerGestionarSucursales = onVerGestionarSucursales,
                    onVerReportesInventario = onVerReportesInventario,
                    onVerSyncInventario = onVerSyncInventario
                )
                "empleados" -> when (subTabSeleccionado) {
                    0 -> TabEmpleados(vm = vm)
                    1 -> TabSueldos(salarioVm = koinViewModel(), adminVm = vm, sucursal = session.sucursalActual)
                }
                "reportes" -> when (subTabSeleccionado) {
                    0 -> TabAuditoria(vm = vm)
                    1 -> {
                        Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                            ElevatedCard(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                    Icon(Icons.Default.Assessment, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
                                    Text(stringResource(R.string.admin_reportes_inv_titulo), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                    Text(stringResource(R.string.admin_reportes_inv_desc), color = MaterialTheme.colorScheme.outline)
                                    Button(onClick = onVerReportesInventario) {
                                        Text(stringResource(R.string.admin_btn_abrir_reportes))
                                    }
                                }
                            }
                        }
                    }
                }
                "config" -> when (subTabSeleccionado) {
                    0 -> TabConfigGlobal(vm = koinViewModel(), allProducts = vm.productos, allCategories = vm.categorias.map { CategoriaProducto(id = it.id, nombre = it.nombre) })
                    1 -> TabConfigNegocio()
                    2 -> TabApariencia(session = session)
                    3 -> TabNotificaciones(vm = koinViewModel())
                }
            }
        }
    }
}

