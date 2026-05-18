package com.bocatta.pos.presentation.ui.screens.operacion

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bocatta.pos.data.sync.OfflineManager
import com.bocatta.pos.presentation.viewmodel.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun AperturaDiaScreen(
    sessionVm: SessionViewModel,
    aperturaVmV2: AperturaViewModelV2,
    cajaVm: CajaViewModel,
    vm: InventoryViewModel,
    onAperturaCompleta: () -> Unit
) {
    var pasoActual by remember { mutableIntStateOf(0) }
    val snackbarHost = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Cargar stock global al arrancar para que el paso 2 tenga datos
    LaunchedEffect(sessionVm.sucursalActual) {
        aperturaVmV2.cargarGlobalStock()
    }

    fun verificarYAvanzar(siguiente: () -> Unit) {
        if (!OfflineManager.isNetworkAvailable(context)) {
            scope.launch { snackbarHost.showSnackbar("Sin conexión. Verifica internet para iniciar.") }
            return
        }
        siguiente()
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHost) },
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text("BOCATTA POS", fontWeight = FontWeight.Black,
                            fontSize = 24.sp, letterSpacing = 2.sp, color = Color.White)
                        Text("INICIO DE JORNADA", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent, titleContentColor = Color.White)
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(MaterialTheme.colorScheme.background, Color(0xFF10121A))))) {
            Column(modifier = Modifier.padding(padding).fillMaxSize()) {

                // Stepper
                Surface(
                    color = Color.White.copy(0.05f),
                    shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(0.08f))
                ) {
                    Row(modifier = Modifier.padding(horizontal = 24.dp, vertical = 32.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically) {
                        PasoItemPremium("1", "SUCURSAL", pasoActual >= 1, pasoActual == 0)
                        Box(modifier = Modifier.weight(1f).height(1.dp).padding(horizontal = 12.dp)
                            .background(if (pasoActual >= 1) MaterialTheme.colorScheme.tertiary else Color.White.copy(0.1f)))
                        PasoItemPremium("2", "SURTIDO", pasoActual >= 2, pasoActual == 1)
                        Box(modifier = Modifier.weight(1f).height(1.dp).padding(horizontal = 12.dp)
                            .background(if (pasoActual >= 2) MaterialTheme.colorScheme.tertiary else Color.White.copy(0.1f)))
                        PasoItemPremium("3", "CAJA", pasoActual >= 3, pasoActual == 2)
                    }
                }

                Column(modifier = Modifier.padding(horizontal = 24.dp).weight(1f)) {
                    AnimatedContent(
                        targetState = pasoActual,
                        transitionSpec = {
                            slideInHorizontally { it } + fadeIn() togetherWith
                            slideOutHorizontally { -it } + fadeOut()
                        },
                        label = "AperturaSteps"
                    ) { step ->
                        when (step) {
                            0 -> SeleccionSucursalPremium(sessionVm) {
                                verificarYAvanzar { pasoActual = 1 }
                            }
                            // FIX CRÍTICO: usa ValidacionStockPremium (ya existía y funciona)
                            // NO más MasaPostresStep que no existe
                            1 -> ValidacionStockPremium(
                                vm = aperturaVmV2,
                                sucursal = sessionVm.sucursalActual
                            ) {
                                pasoActual = 2
                            }
                            2 -> FondoCajaPremium(cajaVm, sessionVm) { onAperturaCompleta() }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PasoItemPremium(num: String, label: String, completado: Boolean, activo: Boolean) {
    val color = when {
        activo -> MaterialTheme.colorScheme.primary
        completado -> MaterialTheme.colorScheme.tertiary
        else -> Color.White.copy(0.3f)
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.size(36.dp).background(color.copy(0.15f), CircleShape)
            .border(1.dp, color, CircleShape), contentAlignment = Alignment.Center) {
            if (completado) Icon(Icons.Default.Check, "Listo", tint = color, modifier = Modifier.size(20.dp))
            else Text(num, fontWeight = FontWeight.Black, color = color, fontSize = 16.sp)
        }
        Spacer(Modifier.height(8.dp))
        Text(label, fontWeight = FontWeight.ExtraBold, fontSize = 10.sp, color = color, letterSpacing = 1.sp)
    }
}

@Composable
fun SeleccionSucursalPremium(vm: SessionViewModel, onConfirm: () -> Unit) {
    val sucursales = listOf("Metepec", "Atlixco")
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Column {
            Text("¿EN QUÉ SUCURSAL ESTÁS?", fontWeight = FontWeight.Black,
                style = MaterialTheme.typography.headlineSmall, color = Color.White)
            Text("Selecciona la sucursal donde trabajarás hoy.",
                color = Color.White.copy(0.5f), fontSize = 14.sp)
        }
        sucursales.forEach { suc ->
            SucursalCardPremium(nombre = suc, seleccionada = vm.sucursalActual == suc,
                onClick = { vm.sucursalActual = suc })
        }
        Spacer(Modifier.weight(1f))
        Button(onClick = onConfirm,
            modifier = Modifier.fillMaxWidth().height(64.dp).padding(bottom = 8.dp),
            shape = RoundedCornerShape(20.dp),
            enabled = vm.sucursalActual.isNotBlank(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
        ) {
            Text("CONTINUAR", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp,
                color = MaterialTheme.colorScheme.background)
            Spacer(Modifier.width(12.dp))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, "Siguiente",
                tint = MaterialTheme.colorScheme.background)
        }
    }
}

@Composable
fun SucursalCardPremium(nombre: String, seleccionada: Boolean, onClick: () -> Unit) {
    Surface(onClick = onClick,
        color = if (seleccionada) MaterialTheme.colorScheme.tertiary.copy(0.08f) else Color.White.copy(0.03f),
        shape = RoundedCornerShape(28.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp,
            if (seleccionada) MaterialTheme.colorScheme.tertiary else Color.White.copy(0.08f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(color = if (seleccionada) MaterialTheme.colorScheme.tertiary.copy(0.15f) else Color.White.copy(0.05f),
                shape = RoundedCornerShape(16.dp), modifier = Modifier.size(56.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Storefront, "Sucursal",
                        tint = if (seleccionada) MaterialTheme.colorScheme.tertiary else Color.White.copy(0.3f),
                        modifier = Modifier.size(32.dp))
                }
            }
            Spacer(Modifier.width(20.dp))
            Text(nombre.uppercase(), fontWeight = FontWeight.Black, fontSize = 24.sp,
                color = if (seleccionada) Color.White else Color.White.copy(0.4f), letterSpacing = 1.sp)
            Spacer(Modifier.weight(1f))
            if (seleccionada) Icon(Icons.Default.RadioButtonChecked, "Seleccionado",
                tint = MaterialTheme.colorScheme.tertiary)
            else Icon(Icons.Default.RadioButtonUnchecked, "Sin seleccionar",
                tint = Color.White.copy(0.1f))
        }
    }
}

@Composable
fun ValidacionStockPremium(vm: AperturaViewModelV2, sucursal: String, onNext: () -> Unit) {
    val transferencias = remember { mutableStateMapOf<String, Int>() }

    val nombresAmigables = mapOf(
        "masa_crepa" to "Masa de Crepa",
        "carlota_unidad" to "Carlota de Limón",
        "tiramisu_unidad" to "Tiramisú",
        "fresas_crema_unidad" to "Fresas con Crema",
        "duraznos_crema_unidad" to "Duraznos con Crema"
    )

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Column {
            Text("¿CUÁNTAS TANDAS LLEVAS HOY?", fontWeight = FontWeight.Black,
                style = MaterialTheme.typography.headlineSmall, color = Color.White)
            Text("Indica cuántas tandas de cada producto traes de bodega central.",
                color = Color.White.copy(0.6f), fontSize = 14.sp)
        }

        when {
            vm.cargando && vm.globalStock.isEmpty() -> {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp),
                    contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
            vm.globalStock.isEmpty() -> {
                Surface(color = MaterialTheme.colorScheme.errorContainer.copy(0.3f),
                    shape = RoundedCornerShape(16.dp)) {
                    Column(modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Warning, "Advertencia",
                            tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(32.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("Bodega central sin datos. Pide al admin que inicialice el sistema.",
                            color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                    }
                }
            }
            else -> {
                LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(vm.globalStock.keys.toList()) { id ->
                        val disponible = vm.globalStock[id] ?: 0
                        val cantidad = transferencias[id] ?: 0
                        val nombre = nombresAmigables[id]
                            ?: id.replace("_", " ").replaceFirstChar { it.uppercase() }

                        Card(shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (cantidad > 0)
                                    MaterialTheme.colorScheme.primary.copy(0.08f)
                                else Color.White.copy(0.05f)),
                            border = if (cantidad > 0)
                                androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(0.3f))
                            else null
                        ) {
                            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Surface(color = MaterialTheme.colorScheme.primary.copy(0.1f),
                                    shape = CircleShape, modifier = Modifier.size(40.dp)) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(nombre.first().uppercase(),
                                            fontWeight = FontWeight.Black,
                                            color = MaterialTheme.colorScheme.primary, fontSize = 16.sp)
                                    }
                                }
                                Spacer(Modifier.width(16.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(nombre, fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp, color = Color.White)
                                    Text(
                                        if (disponible > 0) "En bodega: $disponible tandas"
                                        else "Sin stock en bodega",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (disponible > 0) Color.White.copy(0.4f)
                                        else MaterialTheme.colorScheme.error.copy(0.7f)
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.background(Color.White.copy(0.1f),
                                        RoundedCornerShape(12.dp)).padding(2.dp)) {
                                    IconButton(onClick = { if (cantidad > 0) transferencias[id] = cantidad - 1 },
                                        modifier = Modifier.size(36.dp)) {
                                        Icon(Icons.Default.Remove, "Quitar",
                                            modifier = Modifier.size(18.dp), tint = Color.White)
                                    }
                                    Text(cantidad.toString(), fontWeight = FontWeight.Black,
                                        fontSize = 18.sp, modifier = Modifier.padding(horizontal = 8.dp),
                                        color = if (cantidad > 0) MaterialTheme.colorScheme.primary else Color.White)
                                    IconButton(onClick = { if (cantidad < disponible) transferencias[id] = cantidad + 1 },
                                        modifier = Modifier.size(36.dp), enabled = disponible > 0) {
                                        Icon(Icons.Default.Add, "Agregar",
                                            tint = if (disponible > 0) MaterialTheme.colorScheme.primary
                                            else Color.White.copy(0.3f),
                                            modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Button(
            onClick = {
                if (transferencias.values.any { it > 0 }) {
                    vm.confirmarTransferencia(sucursal, transferencias.toMap()) { onNext() }
                } else {
                    onNext()
                }
            },
            enabled = !vm.cargando,
            modifier = Modifier.fillMaxWidth().height(64.dp).padding(bottom = 8.dp),
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
        ) {
            if (vm.cargando) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.background,
                    modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            } else {
                Icon(Icons.Default.CheckCircle, "Confirmar", tint = MaterialTheme.colorScheme.background)
                Spacer(Modifier.width(12.dp))
                Text(
                    if (transferencias.values.any { it > 0 }) "CONFIRMAR Y CARGAR INVENTARIO"
                    else "CONTINUAR SIN SURTIR",
                    fontWeight = FontWeight.ExtraBold, fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.background)
            }
        }
    }
}

@Composable
fun FondoCajaPremium(vm: CajaViewModel, session: SessionViewModel, onFinish: () -> Unit) {
    var fondo by remember { mutableStateOf("") }
    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Column {
            Text("FONDO DE CAJA", fontWeight = FontWeight.ExtraBold,
                style = MaterialTheme.typography.titleMedium, color = Color.White, letterSpacing = 1.sp)
            Text("¿Cuánto efectivo tienes para dar cambio?",
                color = Color.White.copy(0.5f), fontSize = 13.sp)
        }
        Surface(color = Color.White.copy(0.05f), shape = RoundedCornerShape(28.dp),
            modifier = Modifier.fillMaxWidth(),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(0.08f))) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.AccountBalanceWallet, "Efectivo",
                    tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(52.dp))
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = fondo,
                    onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) fondo = it },
                    label = { Text("Fondo inicial") },
                    prefix = { Text("$ ", color = Color.White.copy(0.3f)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color.White.copy(0.2f),
                        focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
                Spacer(Modifier.height(12.dp))
                Text("Mínimo requerido: \$100.00",
                    style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.4f))
            }
        }
        vm.mensajeError?.let {
            Text(it, color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(horizontal = 4.dp))
        }
        Button(
            onClick = { vm.abrirTurno(fondo.toDoubleOrNull() ?: 0.0,
                session.sucursalActual, session.nombreUsuario) { if (it) onFinish() } },
            enabled = !vm.cargando && fondo.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(64.dp),
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
        ) {
            if (vm.cargando) {
                CircularProgressIndicator(color = Color.White,
                    modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            } else {
                Icon(Icons.Default.PlayArrow, "Iniciar", tint = Color.White)
                Spacer(Modifier.width(12.dp))
                Text("COMENZAR JORNADA", fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp, color = Color.White)
            }
        }
    }
}
