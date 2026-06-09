package com.bocatta.pos.presentation.ui.screens.admin

import android.content.Intent

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bocatta.pos.core.constants.FirestoreCollections
import com.bocatta.pos.domain.model.*
import com.bocatta.pos.logging.LogHelper
import com.bocatta.pos.network.firebase.FirebaseFirestoreProvider
import com.bocatta.pos.presentation.ui.components.*
import com.bocatta.pos.presentation.ui.theme.*
import com.bocatta.pos.domain.model.ConsumibleRequerido
import com.bocatta.pos.presentation.ui.components.DynamicFormEngine
import com.bocatta.pos.presentation.ui.components.DynamicProductForm
import com.bocatta.pos.presentation.ui.components.GiroSelector
import com.bocatta.pos.presentation.viewmodel.AdminViewModel
import com.bocatta.pos.presentation.viewmodel.CatalogoViewModel
import com.bocatta.pos.presentation.viewmodel.ConfigGlobalViewModel
import com.bocatta.pos.presentation.viewmodel.ComboViewModel
import com.bocatta.pos.presentation.viewmodel.MesaViewModel
import com.bocatta.pos.presentation.viewmodel.PromocionViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import androidx.compose.ui.res.stringResource
import com.bocatta.pos.R
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AdminScreen(
    vm: AdminViewModel,
    inventoryVm: com.bocatta.pos.presentation.viewmodel.InventoryViewModel,
    session: com.bocatta.pos.presentation.viewmodel.SessionViewModel,
    onBack: () -> Unit,
    onVerClientes: () -> Unit,
    onVerDashboardBodega: () -> Unit = {},
    onVerReportesInventario: () -> Unit = {},
    onVerSyncInventario: () -> Unit = {},
    onVerGestionarSucursales: () -> Unit = {}
) {
    val context = LocalContext.current
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
        BocattaLoadingDialog(context.getString(R.string.admin_inicializando))
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
                    "recetas" -> listOf(stringResource(R.string.admin_subtab_recetas), stringResource(R.string.admin_subtab_produccion), stringResource(R.string.admin_subtab_costos))
                    "empleados" -> listOf(stringResource(R.string.admin_subtab_empleados), stringResource(R.string.admin_subtab_sueldos))
                    "reportes" -> listOf(stringResource(R.string.admin_subtab_auditoria), stringResource(R.string.admin_subtab_reportes))
                    "config" -> listOf(stringResource(R.string.admin_subtab_config_global), stringResource(R.string.admin_subtab_config_negocio), stringResource(R.string.admin_subtab_apariencia), stringResource(R.string.admin_subtab_notificaciones))
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardCardPremium(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(1.dp, Color.White.copy(0.08f)),
        modifier = Modifier.fillMaxWidth().height(90.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = color.copy(0.15f),
                    shape = CircleShape,
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
                    }
                }
                Icon(
                    Icons.Default.ChevronRight,
                    null,
                    tint = Color.White.copy(0.3f),
                    modifier = Modifier.size(16.dp)
                )
            }
            Column {
                Text(
                    title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 9.sp,
                    color = Color.White.copy(0.5f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private fun String.limpiarEtiquetaAdmin(): String {
    return this
}

@Composable
private fun TabDashboard(
    vm: AdminViewModel,
    onComenzarConfiguracion: () -> Unit,
    onValidarPin: (String, (Boolean) -> Unit) -> Unit
) {
    val context = LocalContext.current
    val hoy = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0)
        }.timeInMillis
    }
    val ventasHoy = vm.historialVentasV2.filter { it.fecha >= hoy }
    val totalVentas = ventasHoy.sumOf { it.total }
    val totalGastos = vm.totalGastosHoy

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Banner primera vez
        if (ventasHoy.isEmpty() && vm.productos.isEmpty()) {
            item {
                ElevatedCard(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.admin_bienvenido), fontWeight = FontWeight.Black,
                            style = MaterialTheme.typography.titleMedium)
                        Text(stringResource(R.string.admin_bienvenido_desc), style = MaterialTheme.typography.bodySmall)
                        Text(stringResource(R.string.admin_bienvenido_paso1))
                        Text(stringResource(R.string.admin_bienvenido_paso2))
                        Text(stringResource(R.string.admin_bienvenido_paso3))
                        Spacer(Modifier.height(4.dp))
                        BocattaButton(
                            texto = stringResource(R.string.admin_btn_configurar),
                            onClick = onComenzarConfiguracion,
                            modifier = Modifier.fillMaxWidth(),
                            icono = Icons.Default.Settings
                        )
                    }
                }
            }
        }

        // Metricas
        item {
            BocattaSectionTitle(stringResource(R.string.admin_resumen_dia))
            Spacer(Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    BocattaMetricCard(stringResource(R.string.admin_metric_ventas), "$${"%.2f".format(totalVentas)}", MaterialTheme.colorScheme.primary, Modifier.weight(1f),
                        subtitulo = stringResource(R.string.admin_metric_transacciones, ventasHoy.size))
                    BocattaMetricCard(stringResource(R.string.admin_metric_gastos), "$${"%.2f".format(totalGastos)}", MaterialTheme.colorScheme.error, Modifier.weight(1f))
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    val numAlertas = vm.insumosMaestros.count { it.cantidadEnBase < it.stockMinimo }
                    BocattaMetricCard(
                        stringResource(R.string.admin_metric_alertas),
                        numAlertas.toString(),
                        if (numAlertas > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        Modifier.weight(1f)
                    )
                    BocattaMetricCard(stringResource(R.string.admin_metric_utilidad), "$${"%.2f".format(totalVentas - totalGastos)}", MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                }
            }
        }

        item {
            ElevatedCard(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.BugReport, stringResource(R.string.admin_icono_diag), tint = MaterialTheme.colorScheme.onSecondaryContainer)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.admin_diagnostico_titulo), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                    Text(
                        stringResource(R.string.admin_diagnostico_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(0.75f)
                    )
                    Button(
                        onClick = {
                            LogHelper.recordBreadcrumb("share_diagnostics", "admin_dashboard")
                            val texto = LogHelper.buildDiagnosticReport(context)
                            com.bocatta.pos.core.ShareUtils.shareTextWhatsAppFallback(
                                context = context,
                                text = texto,
                                subject = context.getString(R.string.admin_diagnostico_subject),
                                logContext = "share_diagnostics"
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.admin_btn_compartir_diag))
                    }
                }
            }
        }

        // Panel mantenimiento
        item {
            // Estados de flujo: null=nada abierto, "borrar"=pin para borrar, "v2"=pin para cargar V2
            var accionPendiente by remember { mutableStateOf<String?>(null) }
            var showPinDialog by remember { mutableStateOf(false) }
            var showBorrarConfirm by remember { mutableStateOf(false) }

            // Dialogo PIN
            if (showPinDialog) {
                AdminPinDialog(
                    onDismiss = { showPinDialog = false; accionPendiente = null },
                    onConfirm = { pin ->
                        onValidarPin(pin) { valido ->
                            showPinDialog = false
                            if (valido) {
                                when (accionPendiente) {
                                    "borrar" -> showBorrarConfirm = true
                                    "v2" -> { vm.inicializarV2(); accionPendiente = null }
                                }
                            } else {
                                accionPendiente = null
                            }
                        }
                    }
                )
            }

            // Dialogo confirmacion borrado (solo alcanzable tras PIN valido)
            if (showBorrarConfirm) {
                AlertDialog(
                    onDismissRequest = { showBorrarConfirm = false; accionPendiente = null },
                    title = { Text(stringResource(R.string.admin_irreversible_titulo)) },
                    text = { Text(stringResource(R.string.admin_irreversible_desc)) },
                    confirmButton = {
                        Button(
                            onClick = { vm.realizarLimpiezaTotal(); showBorrarConfirm = false; accionPendiente = null },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text(stringResource(R.string.admin_btn_si_borrar))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showBorrarConfirm = false; accionPendiente = null }) {
                            Text(stringResource(R.string.admin_cancelar))
                        }
                    }
                )
            }

            ElevatedCard(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, stringResource(R.string.admin_icono_advertencia), tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.admin_mantenimiento_titulo), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                    }
                    Text(stringResource(R.string.admin_mantenimiento_desc), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer.copy(0.7f))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { accionPendiente = "borrar"; showPinDialog = true },
                            modifier = Modifier.weight(1f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                        ) { Text(stringResource(R.string.admin_btn_borrar_todo), fontSize = 12.sp, color = MaterialTheme.colorScheme.error) }
                        Button(
                            onClick = { accionPendiente = "v2"; showPinDialog = true },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) { Text(stringResource(R.string.admin_btn_cargar_v2), fontSize = 12.sp) }
                    }
                }
            }
        }

        // Ultimas ventas
        if (ventasHoy.isNotEmpty()) {
            item { BocattaSectionTitle(stringResource(R.string.admin_ultimas_ventas)) }
            items(ventasHoy.takeLast(5).reversed()) { v ->
                ElevatedCard(shape = RoundedCornerShape(14.dp)) {
                    Row(
                        modifier = Modifier.padding(14.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("$${"%.2f".format(v.total)}", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                            Text(stringResource(R.string.admin_atendio, v.atendio), style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline)
                        }
                        BocattaBadge(v.sucursal.uppercase(java.util.Locale.getDefault()), MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

@Composable
private fun TabMenu(vm: AdminViewModel) {
    var mostrarDialogo by remember { mutableStateOf(false) }
    var productoEditar by remember { mutableStateOf<SalesInventoryProductV2?>(null) }

    if (mostrarDialogo || productoEditar != null) {
        DialogProducto(
            productoInicial = productoEditar,
            vm = vm,
            onGuardar = { prod, receta ->
                if (productoEditar != null) vm.editarProducto(prod) else vm.agregarProducto(prod)
                if (receta.ingredientes.isNotEmpty()) vm.guardarReceta(receta)
                mostrarDialogo = false; productoEditar = null
            },
            onCancelar = { mostrarDialogo = false; productoEditar = null }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            BocattaButton(
                texto = stringResource(R.string.admin_btn_agregar_producto),
                onClick = { mostrarDialogo = true },
                modifier = Modifier.fillMaxWidth(),
                icono = Icons.Default.AddCircle
            )
            Spacer(Modifier.height(8.dp))
        }
        vm.productos.groupBy { it.categoria }.forEach { (cat, prods) ->
            item {
                BocattaSectionTitle(cat)
            }
            items(prods, key = { it.id }) { producto ->
                ElevatedCard(shape = RoundedCornerShape(14.dp)) {
                    Row(
                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Text(producto.emoji, fontSize = 24.sp)
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(producto.nombre, fontWeight = FontWeight.SemiBold)
                                Text(
                                    stringResource(R.string.admin_precios_sucursal, "%.0f".format(producto.precioVenta["atlixco"] ?: 0.0), "%.0f".format(producto.precioVenta["metepec"] ?: 0.0)),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                        Row {
                            IconButton(onClick = { productoEditar = producto }) {
                                Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.admin_icono_editar), tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = { vm.eliminarProducto(producto) }) {
                                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.admin_icono_eliminar), tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TabBodegaGeneral(
    vm: AdminViewModel,
    nombreUsuario: String = "",
    usuarioId: String = "",
    sucursal: String = "",
    onVerDashboardBodega: () -> Unit,
    onVerGestionarSucursales: () -> Unit,
    onVerReportesInventario: () -> Unit,
    onVerSyncInventario: () -> Unit
) {
    var insumoAjustar by remember { mutableStateOf<InsumoV2?>(null) }
    var showPurchaseDialog by remember { mutableStateOf(false) }
    var showNuevoInsumoDialog by remember { mutableStateOf(false) }

    insumoAjustar?.let { insumo ->
        DialogEditarInsumo(
            insumo = insumo,
            onGuardar = { nuevoStockMinimo, conteoFisico, motivo ->
                vm.ajustarConConteoFisico(insumo.id, nuevoStockMinimo, conteoFisico, motivo)
                insumoAjustar = null
            },
            onDismiss = { insumoAjustar = null }
        )
    }

    if (showPurchaseDialog) {
        DialogCompraUnificado(
            insumos = vm.insumosMaestros,
            nombreUsuario = nombreUsuario,
            usuarioId = usuarioId,
            sucursal = sucursal,
            esAdmin = true,
            onConfirmar = { insumoId, insumoNombre, presentacion, cant, cont, precio ->
                vm.registrarCompraRapida(insumoId, insumoNombre, presentacion, cant, cont, precio, usuarioId, nombreUsuario, sucursal, true)
                showPurchaseDialog = false
            },
            onDismiss = { showPurchaseDialog = false }
        )
    }

    if (showNuevoInsumoDialog) {
        var nuevoNombre by remember { mutableStateOf("") }
        var nuevaUnidad by remember { mutableStateOf("kg") }
        var nuevoCosto by remember { mutableStateOf("0") }
        var nuevaCategoria by remember { mutableStateOf("") }
        val scope = rememberCoroutineScope()

        AlertDialog(
            onDismissRequest = { showNuevoInsumoDialog = false },
            title = { Text(stringResource(R.string.admin_nuevo_insumo_titulo), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value = nuevoNombre, onValueChange = { nuevoNombre = it },
                        label = { Text(stringResource(R.string.admin_nuevo_insumo_nombre)) }, placeholder = { Text(stringResource(R.string.admin_nuevo_insumo_ej_nombre)) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                    OutlinedTextField(value = nuevaUnidad, onValueChange = { nuevaUnidad = it },
                        label = { Text(stringResource(R.string.admin_nuevo_insumo_unidad)) }, placeholder = { Text(stringResource(R.string.admin_nuevo_insumo_ej_unidad)) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                    OutlinedTextField(value = nuevoCosto, onValueChange = { nuevoCosto = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text(stringResource(R.string.admin_nuevo_insumo_costo)) }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), shape = RoundedCornerShape(12.dp))
                    OutlinedTextField(value = nuevaCategoria, onValueChange = { nuevaCategoria = it },
                        label = { Text(stringResource(R.string.admin_nuevo_insumo_categoria)) }, placeholder = { Text(stringResource(R.string.admin_nuevo_insumo_ej_categoria)) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                }
            },
            confirmButton = {
                Button(onClick = {
                    scope.launch {
                        val insumo = InsumoV2(
                            nombre = nuevoNombre.trim(),
                            unidadBase = nuevaUnidad.ifBlank { "kg" },
                            costoUnitarioBase = nuevoCosto.toDoubleOrNull() ?: 0.0,
                            categoria = nuevaCategoria.ifBlank { "General" }
                        )
                        vm.agregarInsumo(insumo)
                        showNuevoInsumoDialog = false
                    }
                }, enabled = nuevoNombre.isNotBlank()) { Text(stringResource(R.string.admin_nuevo_insumo_btn_crear), fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { showNuevoInsumoDialog = false }) { Text(stringResource(R.string.admin_cancelar)) } },
            shape = RoundedCornerShape(20.dp)
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            BocattaButton(texto = stringResource(R.string.admin_bodega_titulo), onClick = { showPurchaseDialog = true },
                modifier = Modifier.fillMaxWidth(), icono = Icons.Default.AddShoppingCart)
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onVerDashboardBodega, modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)) { Text(stringResource(R.string.admin_bodega_btn_bodega), fontSize = 12.sp) }
                OutlinedButton(onClick = onVerGestionarSucursales, modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)) { Text(stringResource(R.string.admin_bodega_btn_sucursales), fontSize = 12.sp) }
            }
            Spacer(Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onVerReportesInventario, modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)) { Text(stringResource(R.string.admin_bodega_btn_reportes), fontSize = 12.sp) }
                OutlinedButton(onClick = onVerSyncInventario, modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)) { Text(stringResource(R.string.admin_bodega_btn_sync), fontSize = 12.sp) }
            }
            Spacer(Modifier.height(8.dp))
            BocattaSectionTitle(stringResource(R.string.admin_insumos_titulo))
            OutlinedButton(
                onClick = { showNuevoInsumoDialog = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Add, stringResource(R.string.admin_icono_agregar), modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.admin_btn_nuevo_insumo), fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp))
        }
        items(vm.insumosMaestros, key = { it.id }) { insumo ->
            val esProduccion = insumo.categoria.contains("Produ", ignoreCase = true)
            val stockBajo = insumo.cantidadEnBase <= insumo.stockMinimo
            ElevatedCard(
                onClick = { insumoAjustar = insumo },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = when {
                        stockBajo -> MaterialTheme.colorScheme.errorContainer.copy(0.3f)
                        esProduccion -> MaterialTheme.colorScheme.tertiaryContainer.copy(0.3f)
                        else -> MaterialTheme.colorScheme.surface
                    }
                )
            ) {
                Row(modifier = Modifier.padding(14.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(insumo.nombre, fontWeight = FontWeight.SemiBold)
                            if (esProduccion) {
                                Spacer(Modifier.width(6.dp))
                                BocattaBadge(stringResource(R.string.admin_badge_prod), MaterialTheme.colorScheme.primary)
                            }
                            if (stockBajo) {
                                Spacer(Modifier.width(6.dp))
                                BocattaBadge(stringResource(R.string.admin_badge_bajo), MaterialTheme.colorScheme.error)
                            }
                        }
                        Text(stringResource(R.string.admin_insumo_minimo, insumo.stockMinimo.toString(), insumo.unidadBase),
                            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    }
                    Text("${"%.1f".format(insumo.cantidadEnBase)} ${insumo.unidadBase}",
                        fontWeight = FontWeight.Black, fontSize = 17.sp,
                        color = if (stockBajo) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}

@Composable
private fun TabAuditoria(vm: AdminViewModel) {
    val sdf = remember { SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()) }
    if (vm.cancelacionesPendientes.isEmpty() && vm.diferenciasInventario.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            BocattaEmptyState(
                icono = Icons.Default.VerifiedUser,
                titulo = stringResource(R.string.admin_sin_alertas),
                descripcion = stringResource(R.string.admin_sin_alertas_desc),
                modifier = Modifier.fillMaxWidth()
            )
        }
        return
    }
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            BocattaSectionTitle(stringResource(R.string.admin_auditoria_titulo))
            Text(stringResource(R.string.admin_auditoria_desc), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline)
            Spacer(Modifier.height(8.dp))
        }
        if (vm.diferenciasInventario.isNotEmpty()) {
            item {
                Text(stringResource(R.string.admin_auditoria_diferencias), fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
            }
            items(vm.diferenciasInventario, key = { it["id"]?.toString() ?: it.hashCode().toString() }) { log ->
                ElevatedCard(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(0.55f))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(log["insumoId"]?.toString()?.replace("_", " ") ?: stringResource(R.string.admin_producto_label), fontWeight = FontWeight.Black)
                            Text(
                                log["motivo"]?.let { stringResource(R.string.admin_motivo, it) } ?: stringResource(R.string.admin_sin_motivo),
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                "${stringResource(R.string.admin_sugerido, log["sugerido"] ?: 0)}  ${stringResource(R.string.admin_confirmado, log["confirmado"] ?: 0)}  ${stringResource(R.string.admin_sucursal_label, log["sucursal"] ?: "-")}",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                        Text(
                            sdf.format(Date(log["fecha"] as? Long ?: 0L)),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }
        if (vm.cancelacionesPendientes.isNotEmpty()) {
            item {
                Text(stringResource(R.string.admin_auditoria_cancelaciones), fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
            }
            items(vm.cancelacionesPendientes.reversed()) { log ->
                ElevatedCard(shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Row(modifier = Modifier.padding(14.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(log["productoNombre"]?.toString() ?: stringResource(R.string.admin_producto_label), fontWeight = FontWeight.Black)
                            Text(stringResource(R.string.admin_motivo, log["motivo"]?.toString() ?: ""), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                            Text(stringResource(R.string.admin_vendedor_label, log["vendedor"]?.toString() ?: ""), style = MaterialTheme.typography.labelSmall)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(sdf.format(Date(log["fecha"] as? Long ?: 0L)),
                                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            IconButton(onClick = { vm.revisarCancelacion(log["id"].toString()) }) {
                                Icon(Icons.Default.CheckCircle, contentDescription = stringResource(R.string.admin_marcar_revisado), tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TabRecetas(vm: AdminViewModel) {
    var productoParaReceta by remember { mutableStateOf<SalesInventoryProductV2?>(null) }

    productoParaReceta?.let { prod ->
        DialogReceta(
            producto = prod,
            insumosDisponibles = vm.insumosMaestros,
            recetaActual = vm.recetas[prod.id],
            onGuardar = { receta -> vm.guardarReceta(receta); productoParaReceta = null },
            onCancelar = { productoParaReceta = null }
        )
    }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            BocattaSectionTitle(stringResource(R.string.admin_recetas_titulo))
            Text(stringResource(R.string.admin_recetas_desc), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline)
            Spacer(Modifier.height(8.dp))
        }
        vm.productos.groupBy { it.categoria }.forEach { (cat, prods) ->
            item { Text(cat, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) }
            items(prods, key = { it.id }) { prod ->
                ElevatedCard(shape = RoundedCornerShape(12.dp), onClick = { productoParaReceta = prod }) {
                    Row(modifier = Modifier.padding(14.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text(prod.nombre, fontWeight = FontWeight.Bold)
                            val receta = vm.recetas[prod.id]
                            if (receta != null && receta.ingredientes.isNotEmpty())
                                Text(stringResource(R.string.admin_receta_ingredientes, receta.ingredientes.size), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                            else
                                Text(stringResource(R.string.admin_receta_sin_receta), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = stringResource(R.string.admin_receta_configurar), tint = MaterialTheme.colorScheme.outline)
                    }
                }
            }
        }
    }
}

@Composable
private fun DialogReceta(
    producto: SalesInventoryProductV2,
    insumosDisponibles: List<InsumoV2>,
    recetaActual: RecetaV2?,
    onGuardar: (RecetaV2) -> Unit,
    onCancelar: () -> Unit
) {
    val ingredientesMap = remember { mutableStateMapOf<String, Double>().apply {
        recetaActual?.ingredientes?.forEach { put(it.insumoId, it.cantidad) }
    } }
    var expandInsumos by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text(stringResource(R.string.admin_dialog_receta_titulo, producto.nombre), fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.heightIn(max = 380.dp)) {
                Text(stringResource(R.string.admin_dialog_receta_desc), style = MaterialTheme.typography.bodySmall)
                Box {
                    BocattaButton(texto = stringResource(R.string.admin_dialog_agregar_ingrediente), onClick = { expandInsumos = true },
                        modifier = Modifier.fillMaxWidth(), icono = Icons.Default.Add)
                    DropdownMenu(expanded = expandInsumos, onDismissRequest = { expandInsumos = false }) {
                        insumosDisponibles.forEach { ins ->
                            DropdownMenuItem(text = { Text(ins.nombre) }, onClick = {
                                ingredientesMap[ins.id] = 0.0; expandInsumos = false
                            })
                        }
                    }
                }
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                    items(ingredientesMap.keys.toList()) { id ->
                        val ins = insumosDisponibles.find { it.id == id }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(ins?.nombre ?: id, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                            var qty by remember { mutableStateOf(ingredientesMap[id]?.toString() ?: "0") }
                            OutlinedTextField(value = qty, onValueChange = { qty = it; it.toDoubleOrNull()?.let { d -> ingredientesMap[id] = d } },
                                label = { Text(ins?.unidadBase ?: "") }, modifier = Modifier.width(90.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
                            IconButton(onClick = { ingredientesMap.remove(id) }) {
                                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.admin_icono_eliminar), tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onGuardar(RecetaV2(productoId = producto.id,
                    ingredientes = ingredientesMap.map { (id, cant) -> IngredienteReceta(insumoId = id, cantidad = cant) }))
            }) { Text(stringResource(R.string.admin_dialog_guardar_receta)) }
        },
        dismissButton = { TextButton(onClick = onCancelar) { Text(stringResource(R.string.admin_cancelar)) } },
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
private fun TabCostosInsumos(vm: AdminViewModel) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            BocattaSectionTitle(stringResource(R.string.admin_costos_titulo))
            Text(stringResource(R.string.admin_costos_desc),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            Spacer(Modifier.height(8.dp))
        }
        items(vm.insumosMaestros.filterNot { it.categoria.contains("Produ", ignoreCase = true) }, key = { it.id }) { insumo ->
            ElevatedCard(shape = RoundedCornerShape(14.dp)) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(insumo.nombre, fontWeight = FontWeight.Bold)
                    var precioStr by remember(insumo.costoUnitarioBase) { mutableStateOf(insumo.costoUnitarioBase.toString()) }
                    Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(stringResource(R.string.admin_costos_por_unidad, insumo.unidadBase), style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = precioStr, onValueChange = { precioStr = it },
                            label = { Text("$") }, modifier = Modifier.width(100.dp), shape = RoundedCornerShape(8.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
                        IconButton(onClick = { vm.actualizarCostoInsumo(insumo.id, precioStr.toDoubleOrNull() ?: 0.0) }) {
                            Icon(Icons.Default.Check, contentDescription = stringResource(R.string.admin_costos_guardar), tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TabProduccion(vm: com.bocatta.pos.presentation.viewmodel.InventoryViewModel) {
    var selectedId by remember { mutableStateOf<String?>(null) }
    var materiaUsada by remember { mutableStateOf("") }
    var porcionesObtenidas by remember { mutableStateOf("") }
    var guardando by remember { mutableStateOf(false) }
    val itemsProduccion = vm.maestroInsumos.values.filter {
        it.categoria.contains("Produ", ignoreCase = true) ||
            it.id in setOf("masa_crepa", "carlota_unidad", "tiramisu_unidad", "fresas_crema_unidad", "duraznos_crema_unidad")
    }
    var expandProd by remember { mutableStateOf(false) }

    val yieldHistory = remember(selectedId) {
        listOf(58.0, 62.0, 60.0, 55.0, 61.0)
    }
    val avgYield = remember(yieldHistory) {
        if (yieldHistory.isEmpty()) 0.0 else yieldHistory.average()
    }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            BocattaSectionTitle(stringResource(R.string.admin_produccion_titulo))
            Text(stringResource(R.string.admin_produccion_desc),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
        }
        item {
            ElevatedCard(shape = RoundedCornerShape(20.dp)) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Box {
                        OutlinedButton(onClick = { expandProd = true }, modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)) {
                            Text(itemsProduccion.find { it.id == selectedId }?.nombre ?: stringResource(R.string.admin_produccion_seleccionar))
                        }
                        DropdownMenu(expanded = expandProd, onDismissRequest = { expandProd = false }) {
                            if (itemsProduccion.isEmpty()) {
                                DropdownMenuItem(text = { Text(stringResource(R.string.admin_produccion_sin_insumos)) }, onClick = { expandProd = false })
                            }
                            itemsProduccion.forEach { ins ->
                                DropdownMenuItem(text = { Text(ins.nombre) }, onClick = { selectedId = ins.id; yieldHistory; expandProd = false })
                            }
                        }
                    }
                    val insumoSeleccionado = selectedId
                    if (insumoSeleccionado != null) {
                        if (avgYield > 0) {
                            Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.tertiaryContainer.copy(0.3f)) {
                                Column(Modifier.padding(12.dp)) {
                                    Text(stringResource(R.string.admin_produccion_rendimiento), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Spacer(Modifier.height(4.dp))
                                    Text(stringResource(R.string.admin_produccion_ultimas_tandas, yieldHistory.joinToString(", ") { "%.0f".format(it) }), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.6f))
                                    Text(stringResource(R.string.admin_produccion_promedio, "%.0f".format(avgYield)), fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = MaterialTheme.colorScheme.tertiary)
                                }
                            }
                        }
                        OutlinedTextField(value = materiaUsada, onValueChange = { materiaUsada = it },
                            label = { Text(stringResource(R.string.admin_produccion_materia)) },
                            supportingText = { Text(stringResource(R.string.admin_produccion_materia_support)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                        OutlinedTextField(value = porcionesObtenidas, onValueChange = { porcionesObtenidas = it },
                            label = { Text(stringResource(R.string.admin_produccion_porciones)) },
                            supportingText = {
                                Text(if (porcionesObtenidas.isBlank() && avgYield > 0) stringResource(R.string.admin_produccion_promedio_support, "%.0f".format(avgYield)) else "")
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                        BocattaButton(
                            texto = stringResource(R.string.admin_produccion_registrar),
                            onClick = {
                                guardando = true
                                val porciones = porcionesObtenidas.toDoubleOrNull() ?: if (avgYield > 0) avgYield else 0.0
                                vm.registrarProduccion(
                                    insumoId = insumoSeleccionado,
                                    porcionesObtenidas = porciones,
                                    tandasPreparadas = materiaUsada.toDoubleOrNull() ?: 0.0,
                                    sobranteAnterior = 0.0
                                ) { guardando = false; selectedId = null; materiaUsada = ""; porcionesObtenidas = "" }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = materiaUsada.isNotBlank(),
                            cargando = guardando,
                            icono = Icons.Default.Add
                        )
                    }
                }
            }
        }
    }
}
