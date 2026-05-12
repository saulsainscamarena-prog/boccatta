package com.bocatta.pos.presentation.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bocatta.pos.core.constants.FirestoreCollections
import com.bocatta.pos.domain.model.*
import com.bocatta.pos.network.firebase.FirebaseFirestoreProvider
import com.bocatta.pos.presentation.ui.components.*
import com.bocatta.pos.presentation.ui.theme.*
import com.bocatta.pos.domain.model.ConsumibleRequerido
import com.bocatta.pos.presentation.ui.components.DynamicFormEngine
import com.bocatta.pos.presentation.ui.components.DynamicProductForm
import com.bocatta.pos.presentation.ui.components.GiroSelector
import com.bocatta.pos.presentation.viewmodel.AdminViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
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
    val snackbarHost = remember { SnackbarHostState() }
    var tabPrincipal by remember { mutableIntStateOf(0) }
    var subTabSeleccionado by remember { mutableIntStateOf(0) }

    LaunchedEffect(tabPrincipal) { subTabSeleccionado = 0 }
    LaunchedEffect(vm.mensajeExito, vm.mensajeError) {
        val msg = vm.mensajeExito ?: vm.mensajeError ?: return@LaunchedEffect
        snackbarHost.showSnackbar(msg)
        vm.limpiarMensajes()
    }

    if (vm.seederEnProgreso) {
        BocattaLoadingDialog("Inicializando catálogo y configuración inicial...")
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
                            "Control Central",
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Black,
                            fontSize = 17.sp
                        )
                        Text(
                            "Panel de administración",
                            color = MaterialTheme.colorScheme.onPrimary.copy(0.7f),
                            fontSize = 10.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BocattaOnSurface
                )
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {

            // Tabs principales
            Surface(
                color = BocattaOnSurface,
                shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                PrimaryTabRow(
                    selectedTabIndex = tabPrincipal,
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    divider = {},
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    listOf("Dashboard", "Logística", "Inventario").forEachIndexed { i, label ->
                        Tab(
                            selected = tabPrincipal == i,
                            onClick = { tabPrincipal = i; subTabSeleccionado = 0 },
                            text = {
                                Text(
                                    label.uppercase(),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        )
                    }
                }
            }

            // Sub-tabs — tokens semánticos, sin colores hardcodeados
            val subTabLabels = when (tabPrincipal) {
                0 -> listOf("Resumen", "Auditoría")
                1 -> listOf("Catálogo", "Recetas", "Costos")
                2 -> listOf("Stock", "Producción", "Config")
                else -> emptyList()
            }
            SecondaryTabRow(
                selectedTabIndex = subTabSeleccionado,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = BocattaPrimary
            ) {
                subTabLabels.forEachIndexed { i, label ->
                    Tab(
                        selected = subTabSeleccionado == i,
                        onClick = { subTabSeleccionado = i },
                        text = {
                            Text(
                                label,
                                fontWeight = if (subTabSeleccionado == i) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            // Contenido
            when (tabPrincipal) {
                0 -> when (subTabSeleccionado) {
                    0 -> TabDashboard(vm = vm)
                    1 -> TabAuditoria(vm = vm)
                }
                1 -> when (subTabSeleccionado) {
                    0 -> TabMenu(vm = vm)
                    1 -> TabRecetas(vm = vm)
                    2 -> TabCostosInsumos(vm = vm)
                }
                2 -> when (subTabSeleccionado) {
                    0 -> TabBodegaGeneral(
                        vm = vm,
                        onVerDashboardBodega = onVerDashboardBodega,
                        onVerGestionarSucursales = onVerGestionarSucursales,
                        onVerReportesInventario = onVerReportesInventario,
                        onVerSyncInventario = onVerSyncInventario
                    )
                    1 -> TabProduccion(inventoryVm)
                    2 -> TabConfigNegocio()
                }
            }
        }
    }
}

@Composable
private fun TabDashboard(vm: AdminViewModel) {
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
                        Text("🚀 ¡Bienvenido a Bocatta POS!", fontWeight = FontWeight.Black,
                            style = MaterialTheme.typography.titleMedium)
                        Text("Sigue estos pasos para comenzar:", style = MaterialTheme.typography.bodySmall)
                        Text("1. 🍽️ Menú → Agrega tus productos")
                        Text("2. 📦 Inventario → Registra tus insumos")
                        Text("3. 🧪 Recetas → Vincula productos con insumos")
                        Spacer(Modifier.height(4.dp))
                        BocattaButton(
                            texto = "Comenzar configuración",
                            onClick = {},
                            modifier = Modifier.fillMaxWidth(),
                            icono = Icons.Default.Settings
                        )
                    }
                }
            }
        }

        // Métricas
        item {
            BocattaSectionTitle("Resumen del día")
            Spacer(Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    BocattaMetricCard("Ventas", "$${"%.2f".format(totalVentas)}", BocattaSuccess, Modifier.weight(1f),
                        subtitulo = "${ventasHoy.size} transacciones")
                    BocattaMetricCard("Gastos", "$${"%.2f".format(totalGastos)}", MaterialTheme.colorScheme.error, Modifier.weight(1f))
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    val numAlertas = vm.insumosMaestros.count { it.cantidadEnBase < it.stockMinimo }
                    BocattaMetricCard(
                        "Alertas stock",
                        numAlertas.toString(),
                        if (numAlertas > 0) BocattaDanger else BocattaSuccess,
                        Modifier.weight(1f)
                    )
                    BocattaMetricCard("Utilidad", "$${"%.2f".format(totalVentas - totalGastos)}", BocattaPrimary, Modifier.weight(1f))
                }
            }
        }

        // Panel mantenimiento
        item {
            var showDeleteConfirm by remember { mutableStateOf(false) }
            if (showDeleteConfirm) {
                AlertDialog(
                    onDismissRequest = { showDeleteConfirm = false },
                    title = { Text("⚠️ ¡Acción irreversible!") },
                    text = { Text("¿Estás seguro de borrar TODOS los datos del sistema? Esta acción no se puede deshacer.") },
                    confirmButton = {
                        Button(onClick = { vm.realizarLimpiezaTotal(); showDeleteConfirm = false },
                            colors = ButtonDefaults.buttonColors(containerColor = BocattaDanger)) {
                            Text("Sí, borrar todo")
                        }
                    },
                    dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancelar") } }
                )
            }
            ElevatedCard(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, null, tint = BocattaDanger, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Mantenimiento del sistema", fontWeight = FontWeight.Bold, color = BocattaDanger)
                    }
                    Text("Solo usar en casos de reinicio total.", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer.copy(0.7f))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { showDeleteConfirm = true },
                            modifier = Modifier.weight(1f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BocattaDanger)
                        ) { Text("Borrar todo", fontSize = 12.sp, color = BocattaDanger) }
                        Button(
                            onClick = { vm.inicializarV2() },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = BocattaSuccess)
                        ) { Text("Cargar V2", fontSize = 12.sp) }
                    }
                }
            }
        }

        // Últimas ventas
        if (ventasHoy.isNotEmpty()) {
            item { BocattaSectionTitle("Últimas ventas") }
            items(ventasHoy.takeLast(5).reversed()) { v ->
                ElevatedCard(shape = RoundedCornerShape(14.dp)) {
                    Row(
                        modifier = Modifier.padding(14.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("$${"%.2f".format(v.total)}", fontWeight = FontWeight.Black, color = BocattaPrimary)
                            Text("Atendió: ${v.atendio}", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline)
                        }
                        BocattaBadge(v.sucursal.uppercase(), BocattaPrimary)
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
                texto = "Agregar nuevo producto",
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
                                    "Atl: $${"%.0f".format(producto.precioVenta["atlixco"] ?: 0.0)} · Met: $${"%.0f".format(producto.precioVenta["metepec"] ?: 0.0)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                        Row {
                            IconButton(onClick = { productoEditar = producto }) {
                                Icon(Icons.Default.Edit, contentDescription = "Editar", tint = BocattaPrimary)
                            }
                            IconButton(onClick = { vm.eliminarProducto(producto) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DialogProducto(
    productoInicial: SalesInventoryProductV2?,
    vm: AdminViewModel,
    onGuardar: (SalesInventoryProductV2, RecetaV2) -> Unit,
    onCancelar: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var paso by remember { mutableIntStateOf(0) }
    var nombre by remember { mutableStateOf(productoInicial?.nombre ?: "") }
    var emoji by remember { mutableStateOf(productoInicial?.emoji ?: "🍩") }
    var categoria by remember { mutableStateOf(productoInicial?.categoria ?: "") }
    var expandCat by remember { mutableStateOf(false) }
    var nuevaCat by remember { mutableStateOf("") }
    var giro by remember { mutableStateOf("FOOD") }
    val formValues = remember { mutableStateMapOf<String, Any?>() }
    var esCombo by remember { mutableStateOf(productoInicial?.esCombo ?: false) }
    val preciosMap = remember { mutableStateMapOf("atlixco" to "25.0", "metepec" to "25.0") }
    var toppingsInc by remember { mutableStateOf(productoInicial?.toppingsIncluidos?.toString() ?: "2") }
    var costoTop by remember { mutableStateOf(productoInicial?.costoToppingExtra?.toString() ?: "10.0") }
    val ingredientes = remember { mutableStateListOf<IngredienteReceta>() }
    var requiereReceta by remember { mutableStateOf(false) }
    var expandInsumo by remember { mutableStateOf(false) }
    val dynamicAttrs = remember(giro) { DynamicFormEngine.getSchema(giro) }

    AlertDialog(
        onDismissRequest = onCancelar,
        title = {
            Column {
                Text("${if (productoInicial != null) "Editar" else "Nuevo"} producto — paso ${paso + 1}/3",
                    fontWeight = FontWeight.Bold)
                LinearProgressIndicator(
                    progress = { (paso + 1) / 3f },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    color = BocattaPrimary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (paso) {
                    0 -> {
                        GiroSelector(currentGiro = giro, onGiroSelected = { giro = it })
                        OutlinedTextField(value = nombre, onValueChange = { nombre = it },
                            label = { Text("Nombre*") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                        ExposedDropdownMenuBox(expanded = expandCat, onExpandedChange = { expandCat = it }) {
                            OutlinedTextField(
                                value = categoria, onValueChange = { categoria = it },
                                label = { Text("Categoría*") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandCat) },
                                modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )
                            ExposedDropdownMenu(expanded = expandCat, onDismissRequest = { expandCat = false }) {
                                vm.categorias.forEach { cat ->
                                    DropdownMenuItem(text = { Text(cat.nombre) },
                                        onClick = { categoria = cat.nombre; expandCat = false })
                                }
                                HorizontalDivider()
                                Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    OutlinedTextField(value = nuevaCat, onValueChange = { nuevaCat = it },
                                        label = { Text("Nueva") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp))
                                    IconButton(onClick = {
                                        if (nuevaCat.isNotBlank()) { vm.agregarCategoria(nuevaCat); nuevaCat = "" }
                                    }) { Icon(Icons.Default.Add, contentDescription = "Agregar categoría") }
                                }
                            }
                        }
                    }
                    1 -> {
                        Text("Precios por sucursal", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        preciosMap.keys.toList().forEach { suc ->
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(value = suc, onValueChange = {}, readOnly = true,
                                    label = { Text("Sucursal") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp))
                                OutlinedTextField(
                                    value = preciosMap[suc] ?: "0",
                                    onValueChange = { preciosMap[suc] = it },
                                    label = { Text("$") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.width(110.dp),
                                    shape = RoundedCornerShape(10.dp)
                                )
                            }
                        }
                        TextButton(onClick = { preciosMap["sucursal_${preciosMap.size}"] = "0.0" }) {
                            Icon(Icons.Default.Add, contentDescription = "Agregar sucursal")
                            Text("Agregar sucursal")
                        }
                        HorizontalDivider()
                        DynamicProductForm(
                            giro = giro,
                            initialValues = formValues,
                            onValuesChanged = { formValues.putAll(it) }
                        )
                    }
                    2 -> {
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("Tiene receta (descuenta inventario)")
                            Switch(checked = requiereReceta, onCheckedChange = { requiereReceta = it })
                        }
                        if (requiereReceta) {
                            Text("Ingredientes:", style = MaterialTheme.typography.labelMedium)
                            ingredientes.forEachIndexed { i, ing ->
                                Row(verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(vm.insumosMaestros.find { it.id == ing.insumoId }?.nombre ?: ing.insumoId,
                                        modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                                    OutlinedTextField(value = ing.cantidad.toString(),
                                        onValueChange = { v -> ingredientes[i] = ing.copy(cantidad = v.toDoubleOrNull() ?: 0.0) },
                                        modifier = Modifier.width(70.dp), shape = RoundedCornerShape(8.dp),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
                                    IconButton(onClick = { ingredientes.removeAt(i) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                            Box {
                                TextButton(onClick = { expandInsumo = true }) {
                                    Icon(Icons.Default.Add, contentDescription = "Agregar ingrediente")
                                    Text("Agregar ingrediente")
                                }
                                DropdownMenu(expanded = expandInsumo, onDismissRequest = { expandInsumo = false }) {
                                    vm.insumosMaestros.take(10).forEach { ins ->
                                        DropdownMenuItem(text = { Text(ins.nombre) }, onClick = {
                                            ingredientes.add(IngredienteReceta(insumoId = ins.id, nombreInsumo = ins.nombre, unidad = ins.unidadBase))
                                            expandInsumo = false
                                        })
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            when (paso) {
                2 -> Button(
                    onClick = {
                        val id = productoInicial?.id ?: FirebaseFirestoreProvider.db.collection(FirestoreCollections.PRODUCTOS).document().id
                        val attributes = DynamicFormEngine.buildAttributes(dynamicAttrs, formValues)
                        val prod = SalesInventoryProductV2(
                            id = id, nombre = nombre, emoji = (attributes["emoji"] as? String) ?: emoji, categoria = categoria,
                            precioVenta = preciosMap.mapValues { it.value.toDoubleOrNull() ?: 0.0 },
                            esCombo = (attributes["esCombo"] as? Boolean) ?: esCombo,
                            toppingsIncluidos = ((attributes["toppingsIncluidos"] as? Number)?.toInt()) ?: toppingsInc.toIntOrNull() ?: 2,
                            costoToppingExtra = ((attributes["costoToppingExtra"] as? Number)?.toDouble()) ?: costoTop.toDoubleOrNull() ?: 10.0
                        )
                        val receta = if (requiereReceta && ingredientes.isNotEmpty())
                            RecetaV2(id = "receta_$id", nombre = "$nombre Receta", productoId = id, ingredientes = ingredientes)
                        else RecetaV2()
                        onGuardar(prod, receta)
                    },
                    enabled = nombre.length >= 2 && categoria.isNotBlank()
                ) { Text("Guardar") }
                else -> Button(
                    onClick = { paso++ },
                    enabled = nombre.length >= 2 && categoria.isNotBlank()
                ) { Text("Siguiente") }
            }
        },
        dismissButton = {
            when (paso) {
                0 -> TextButton(onClick = onCancelar) { Text("Cancelar") }
                else -> TextButton(onClick = { paso-- }) { Text("← Anterior") }
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
private fun TabBodegaGeneral(
    vm: AdminViewModel,
    onVerDashboardBodega: () -> Unit,
    onVerGestionarSucursales: () -> Unit,
    onVerReportesInventario: () -> Unit,
    onVerSyncInventario: () -> Unit
) {
    var insumoAjustar by remember { mutableStateOf<InsumoV2?>(null) }
    var nuevoStock by remember { mutableStateOf("") }
    var showPurchaseDialog by remember { mutableStateOf(false) }

    if (insumoAjustar != null) {
        AlertDialog(
            onDismissRequest = { insumoAjustar = null },
            title = { Text("Ajustar stock: ${insumoAjustar!!.nombre}", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Ingresa el conteo físico real para sincronizar el sistema.", style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(value = nuevoStock, onValueChange = { nuevoStock = it },
                        label = { Text("Stock (${insumoAjustar!!.unidadBase})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                }
            },
            confirmButton = {
                Button(
                    onClick = { vm.ajustarStock(insumoAjustar!!.id, nuevoStock.toDoubleOrNull() ?: 0.0); insumoAjustar = null; nuevoStock = "" },
                    colors = ButtonDefaults.buttonColors(containerColor = BocattaSuccess)
                ) { Text("Guardar") }
            },
            dismissButton = { TextButton(onClick = { insumoAjustar = null }) { Text("Cancelar") } },
            shape = RoundedCornerShape(20.dp)
        )
    }

    if (showPurchaseDialog) {
        DialogRegistroCompra(
            insumos = vm.insumosMaestros,
            onGuardar = { gasto, insumoId, cant ->
                if (insumoId != null) vm.registrarCompraInsumo(gasto, insumoId, cant)
                else vm.registrarGastoNegocio(gasto)
                showPurchaseDialog = false
            },
            onCancelar = { showPurchaseDialog = false }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            BocattaButton(texto = "Registrar compra / gasto", onClick = { showPurchaseDialog = true },
                modifier = Modifier.fillMaxWidth(), icono = Icons.Default.AddShoppingCart)
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onVerDashboardBodega, modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)) { Text("📦 Bodega", fontSize = 12.sp) }
                OutlinedButton(onClick = onVerGestionarSucursales, modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)) { Text("🏪 Sucursales", fontSize = 12.sp) }
            }
            Spacer(Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onVerReportesInventario, modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)) { Text("📊 Reportes", fontSize = 12.sp) }
                OutlinedButton(onClick = onVerSyncInventario, modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)) { Text("🔄 Sync", fontSize = 12.sp) }
            }
            Spacer(Modifier.height(8.dp))
            BocattaSectionTitle("Insumos — toca para ajustar stock")
        }
        items(vm.insumosMaestros, key = { it.id }) { insumo ->
            val esProduccion = insumo.categoria == "Producción"
            val stockBajo = insumo.cantidadEnBase <= insumo.stockMinimo
            ElevatedCard(
                onClick = { insumoAjustar = insumo; nuevoStock = insumo.cantidadEnBase.toString() },
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
                                BocattaBadge("PROD", BocattaPrimary)
                            }
                            if (stockBajo) {
                                Spacer(Modifier.width(6.dp))
                                BocattaBadge("BAJO", BocattaDanger)
                            }
                        }
                        Text("Mínimo: ${insumo.stockMinimo} ${insumo.unidadBase}",
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
    if (vm.cancelacionesPendientes.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            BocattaEmptyState(
                icono = Icons.Default.VerifiedUser,
                titulo = "Sin cancelaciones",
                descripcion = "No hay productos eliminados del carrito hoy",
                modifier = Modifier.fillMaxWidth()
            )
        }
        return
    }
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            BocattaSectionTitle("Auditoría anti-fraude")
            Text("Productos eliminados del carrito durante ventas.", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline)
            Spacer(Modifier.height(8.dp))
        }
        items(vm.cancelacionesPendientes.reversed()) { log ->
            ElevatedCard(shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                Row(modifier = Modifier.padding(14.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(log["productoNombre"]?.toString() ?: "Producto", fontWeight = FontWeight.Black)
                        Text("Motivo: ${log["motivo"]}", style = MaterialTheme.typography.bodySmall, color = BocattaDanger)
                        Text("Vendedor: ${log["vendedor"]}", style = MaterialTheme.typography.labelSmall)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(sdf.format(Date(log["fecha"] as? Long ?: 0L)),
                            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        IconButton(onClick = { vm.revisarCancelacion(log["id"].toString()) }) {
                            Icon(Icons.Default.CheckCircle, contentDescription = "Marcar revisado", tint = BocattaSuccess)
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
            BocattaSectionTitle("Recetas automáticas")
            Text("Define qué insumos se descuentan por cada venta.", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline)
            Spacer(Modifier.height(8.dp))
        }
        vm.productos.groupBy { it.categoria }.forEach { (cat, prods) ->
            item { Text(cat, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = BocattaPrimary) }
            items(prods, key = { it.id }) { prod ->
                ElevatedCard(shape = RoundedCornerShape(12.dp), onClick = { productoParaReceta = prod }) {
                    Row(modifier = Modifier.padding(14.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text(prod.nombre, fontWeight = FontWeight.Bold)
                            val receta = vm.recetas[prod.id]
                            if (receta != null && receta.ingredientes.isNotEmpty())
                                Text("${receta.ingredientes.size} ingredientes", style = MaterialTheme.typography.bodySmall, color = BocattaSuccess)
                            else
                                Text("Sin receta — no descuenta inventario", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = "Configurar", tint = MaterialTheme.colorScheme.outline)
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
        title = { Text("Receta: ${producto.nombre}", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.heightIn(max = 380.dp)) {
                Text("Define los insumos que se consumen por unidad vendida:", style = MaterialTheme.typography.bodySmall)
                Box {
                    BocattaButton(texto = "Agregar ingrediente", onClick = { expandInsumos = true },
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
                                Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error)
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
            }) { Text("Guardar receta") }
        },
        dismissButton = { TextButton(onClick = onCancelar) { Text("Cancelar") } },
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
private fun TabCostosInsumos(vm: AdminViewModel) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            BocattaSectionTitle("Costos de insumos")
            Text("Actualiza el precio de compra de cada insumo. Se usa para calcular la utilidad real.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            Spacer(Modifier.height(8.dp))
        }
        items(vm.insumosMaestros.filter { it.categoria != "Producción" }, key = { it.id }) { insumo ->
            ElevatedCard(shape = RoundedCornerShape(14.dp)) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(insumo.nombre, fontWeight = FontWeight.Bold)
                    var precioStr by remember(insumo.costoUnitarioBase) { mutableStateOf(insumo.costoUnitarioBase.toString()) }
                    Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Por ${insumo.unidadBase}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = precioStr, onValueChange = { precioStr = it },
                            label = { Text("$") }, modifier = Modifier.width(100.dp), shape = RoundedCornerShape(8.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
                        IconButton(onClick = { vm.actualizarCostoInsumo(insumo.id, precioStr.toDoubleOrNull() ?: 0.0) }) {
                            Icon(Icons.Default.Check, contentDescription = "Guardar costo", tint = BocattaSuccess)
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
    val itemsProduccion = vm.maestroInsumos.values.filter { it.categoria == "Producción" }
    var expandProd by remember { mutableStateOf(false) }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            BocattaSectionTitle("Registro de tandas de producción")
            Text("Transforma materia prima en porciones listas para la venta.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
        }
        item {
            ElevatedCard(shape = RoundedCornerShape(20.dp)) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Box {
                        OutlinedButton(onClick = { expandProd = true }, modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)) {
                            Text(itemsProduccion.find { it.id == selectedId }?.nombre ?: "Seleccionar producto")
                        }
                        DropdownMenu(expanded = expandProd, onDismissRequest = { expandProd = false }) {
                            if (itemsProduccion.isEmpty()) {
                                DropdownMenuItem(text = { Text("Sin insumos de producción configurados") }, onClick = { expandProd = false })
                            }
                            itemsProduccion.forEach { ins ->
                                DropdownMenuItem(text = { Text(ins.nombre) }, onClick = { selectedId = ins.id; expandProd = false })
                            }
                        }
                    }
                    if (selectedId != null) {
                        OutlinedTextField(value = materiaUsada, onValueChange = { materiaUsada = it },
                            label = { Text("Materia prima usada (g/ml/kg)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                        OutlinedTextField(value = porcionesObtenidas, onValueChange = { porcionesObtenidas = it },
                            label = { Text("Porciones obtenidas (unidades)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                        BocattaButton(
                            texto = "Registrar tanda",
                            onClick = {
                                guardando = true
                                vm.registrarProduccion(
                                    insumoId = selectedId!!,
                                    porcionesObtenidas = porcionesObtenidas.toDoubleOrNull() ?: 0.0,
                                    materiaPrimaUsadaG = materiaUsada.toDoubleOrNull() ?: 0.0,
                                    sobranteAnterior = 0.0
                                ) { guardando = false; selectedId = null; materiaUsada = ""; porcionesObtenidas = "" }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = materiaUsada.isNotBlank() && porcionesObtenidas.isNotBlank(),
                            cargando = guardando,
                            icono = Icons.Default.Add
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DialogRegistroCompra(
    insumos: List<InsumoV2>,
    onGuardar: (GastoV2, String?, Double) -> Unit,
    onCancelar: () -> Unit
) {
    var concepto by remember { mutableStateOf("") }
    var monto by remember { mutableStateOf("") }
    var selectedInsumo by remember { mutableStateOf<InsumoV2?>(null) }
    var cantidadSurtida by remember { mutableStateOf("1") }
    var expandInsumo by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text("Registrar compra / gasto", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = monto, onValueChange = { monto = it }, label = { Text("Monto total ($)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                    leadingIcon = { Icon(Icons.Default.AttachMoney, contentDescription = "Monto") })
                Box {
                    OutlinedTextField(value = selectedInsumo?.nombre ?: "Seleccionar insumo (opcional)",
                        onValueChange = {}, readOnly = true, label = { Text("Insumo a reabastecer") },
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                    Surface(onClick = { expandInsumo = true }, color = Color.Transparent, modifier = Modifier.matchParentSize()) {}
                    DropdownMenu(expanded = expandInsumo, onDismissRequest = { expandInsumo = false }) {
                        insumos.forEach { ins ->
                            DropdownMenuItem(text = { Text(ins.nombre) }, onClick = {
                                selectedInsumo = ins; concepto = "Compra de ${ins.nombre}"; expandInsumo = false
                            })
                        }
                    }
                }
                if (selectedInsumo != null) {
                    OutlinedTextField(value = cantidadSurtida, onValueChange = { cantidadSurtida = it },
                        label = { Text("Cantidad (${selectedInsumo!!.unidadBase})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                }
                OutlinedTextField(value = concepto, onValueChange = { concepto = it }, label = { Text("Concepto") },
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
            }
        },
        confirmButton = {
            Button(
                enabled = monto.isNotBlank() && concepto.isNotBlank(),
                onClick = {
                    onGuardar(
                        GastoV2(id = UUID.randomUUID().toString(), descripcion = concepto,
                            monto = monto.toDoubleOrNull() ?: 0.0,
                            categoria = if (selectedInsumo != null) "Insumos" else "General",
                            fecha = System.currentTimeMillis(), sucursal = "global"),
                        selectedInsumo?.id,
                        cantidadSurtida.toDoubleOrNull() ?: 0.0
                    )
                }
            ) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onCancelar) { Text("Cancelar") } },
        shape = RoundedCornerShape(20.dp)
    )
}
