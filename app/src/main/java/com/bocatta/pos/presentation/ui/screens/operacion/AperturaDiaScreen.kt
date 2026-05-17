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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.bocatta.pos.presentation.viewmodel.*
import com.bocatta.pos.presentation.ui.theme.*
import com.bocatta.pos.data.sync.OfflineManager
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun AperturaDiaScreen(
    sessionVm: SessionViewModel,
    aperturaVmV2: AperturaViewModelV2,
    cajaVm: CajaViewModel,
    vm: com.bocatta.pos.presentation.viewmodel.InventoryViewModel,
    onAperturaCompleta: () -> Unit
) {
    var pasoActual by remember { mutableIntStateOf(0) }
    val snackbarHost = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current

    fun verificarYAvanzar(siguiente: () -> Unit) {
        if (!OfflineManager.isNetworkAvailable(context)) {
            scope.launch { snackbarHost.showSnackbar("Verifica la conexión para iniciar la jornada.") }
            return
        }
        siguiente()
    }

    Scaffold(contentWindowInsets = WindowInsets.safeDrawing, 
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHost) },
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text("PROTOCOLOS", fontWeight = FontWeight.Black, fontSize = 24.sp, letterSpacing = 2.sp, color = Color.White)
                        Text("PROTOCOLO DE APERTURA", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent, titleContentColor = Color.White)
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(MaterialTheme.colorScheme.background, Color(0xFF10121A))))) {
            Column(modifier = Modifier.padding(padding).fillMaxSize()) {
                // STEPPER PREMIUM
                Surface(
                    color = Color.White.copy(0.05f),
                    shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(0.08f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 32.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PasoItemPremium("1", "SUCURSAL", pasoActual >= 1, pasoActual == 0)
                        Box(modifier = Modifier.weight(1f).height(1.dp).padding(horizontal = 12.dp).background(if (pasoActual >= 1) MaterialTheme.colorScheme.tertiary else Color.White.copy(0.1f)))
                        PasoItemPremium("2", "INSUMO", pasoActual >= 2, pasoActual == 1)
                        Box(modifier = Modifier.weight(1f).height(1.dp).padding(horizontal = 12.dp).background(if (pasoActual >= 2) MaterialTheme.colorScheme.tertiary else Color.White.copy(0.1f)))
                        PasoItemPremium("3", "CAJA", pasoActual >= 3, pasoActual == 2)
                    }
                }
                
                Column(modifier = Modifier.padding(horizontal = 24.dp).weight(1f)) {
                    AnimatedContent(
                        targetState = pasoActual,
                        transitionSpec = {
                            slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it } + fadeOut()
                        },
                        label = "AperturaSteps"
                    ) { step ->
                        when(step) {
                            0 -> SeleccionSucursalPremium(sessionVm) { verificarYAvanzar { pasoActual = 1 } }
                            1 -> MasaPostresStep(sessionVm, vm) { pasoActual = 2 }
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
    val color = if (activo) MaterialTheme.colorScheme.primary else if (completado) MaterialTheme.colorScheme.tertiary else Color.White.copy(0.3f)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier.size(36.dp).background(color.copy(0.15f), CircleShape).border(1.dp, color, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (completado) Icon(Icons.Default.Check, null, tint = color, modifier = Modifier.size(20.dp))
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
            Text("IDENTIFICACIÓN", fontWeight = FontWeight.Black, style = MaterialTheme.typography.headlineSmall, color = Color.White)
            Text("Selecciona la estación de trabajo actual.", color = Color.White.copy(0.5f), fontSize = 14.sp)
        }
        
        sucursales.forEach { suc ->
            SucursalCardPremium(
                nombre = suc, 
                seleccionada = vm.sucursalActual == suc,
                sugerida = false, // Removiendo sugerencia automática por falta de exactitud GPS
                onClick = { vm.sucursalActual = suc }
            )
        }
        
        Spacer(Modifier.weight(1f))
        
        Button(
            onClick = onConfirm, 
            modifier = Modifier.fillMaxWidth().height(64.dp).padding(bottom = 8.dp), 
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
        ) {
            Text("INGRESAR A ESTACIÓN", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = MaterialTheme.colorScheme.background)
            Spacer(Modifier.width(12.dp))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = MaterialTheme.colorScheme.background)
        }
    }
}

@Composable
fun SucursalCardPremium(nombre: String, seleccionada: Boolean, sugerida: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = if (seleccionada) MaterialTheme.colorScheme.tertiary.copy(0.08f) else Color.White.copy(0.03f),
        shape = RoundedCornerShape(28.dp), // Meridian Spec: 28dp
        border = androidx.compose.foundation.BorderStroke(1.dp, if (seleccionada) MaterialTheme.colorScheme.tertiary else Color.White.copy(0.08f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                color = if (seleccionada) MaterialTheme.colorScheme.tertiary.copy(0.15f) else Color.White.copy(0.05f), 
                shape = RoundedCornerShape(16.dp), 
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Storefront, null, tint = if (seleccionada) MaterialTheme.colorScheme.tertiary else Color.White.copy(0.3f), modifier = Modifier.size(32.dp))
                }
            }
            Spacer(Modifier.width(20.dp))
            Column {
                Text(nombre.uppercase(), fontWeight = FontWeight.Black, fontSize = 24.sp, color = if(seleccionada) Color.White else Color.White.copy(0.4f), letterSpacing = 1.sp)
                if (sugerida) Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Verified, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("SUGERIDA POR SISTEMA", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.ExtraBold, fontSize = 10.sp, letterSpacing = 0.5.sp)
                }
            }
            Spacer(Modifier.weight(1f))
            if (seleccionada) Icon(Icons.Default.RadioButtonChecked, null, tint = MaterialTheme.colorScheme.tertiary)
            else Icon(Icons.Default.RadioButtonUnchecked, null, tint = Color.White.copy(0.1f))
        }
    }
}

@Composable
fun ValidacionStockPremium(vm: AperturaViewModelV2, sucursal: String, onNext: () -> Unit) {
    val transferencias = remember { mutableStateMapOf<String, Int>() }
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Column {
            Text("Surtido desde Bodega Central", fontWeight = FontWeight.Black, style = MaterialTheme.typography.headlineSmall, color = Color.White)
            Text("Surtido de insumos críticos para la jornada.", color = Color.White.copy(0.6f), fontSize = 14.sp)
        }
        
        LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(vm.globalStock.keys.toList()) { id ->
                val disp = vm.globalStock[id] ?: 0
                val cant = transferencias[id] ?: 0
                Card(
                    shape = RoundedCornerShape(16.dp), 
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(0.05f)),
                    border = if (cant > 0) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(0.3f)) else null
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(color = MaterialTheme.colorScheme.primary.copy(0.1f), shape = CircleShape, modifier = Modifier.size(40.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Inventory2, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            }
                        }
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text(id.replace("_", " ").uppercase(), fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                            Text("Disponibilidad: $disp", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.4f))
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.background(Color.White.copy(0.1f), RoundedCornerShape(12.dp)).padding(2.dp)) {
                            IconButton(onClick = { if (cant > 0) transferencias[id] = cant - 1 }, modifier = Modifier.size(36.dp)) { Icon(Icons.Default.Remove, null, modifier = Modifier.size(18.dp), tint = Color.White) }
                            Text(cant.toString(), fontWeight = FontWeight.Black, fontSize = 18.sp, modifier = Modifier.padding(horizontal = 8.dp), color = Color.White)
                            IconButton(onClick = { if (cant < disp) transferencias[id] = cant + 1 }, modifier = Modifier.size(36.dp)) { Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp)) }
                        }
                    }
                }
            }
        }
        
        Button(
            onClick = { vm.confirmarTransferencia(sucursal, transferencias.toMap()) { onNext() } }, 
            enabled = !vm.cargando,
            modifier = Modifier.fillMaxWidth().height(64.dp).padding(bottom = 8.dp), 
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
        ) {
            Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.background)
            Spacer(Modifier.width(12.dp))
            Text("CONFIRMAR Y CARGAR", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = MaterialTheme.colorScheme.background)
        }
    }
}

@Composable
fun FondoCajaPremium(vm: CajaViewModel, session: SessionViewModel, onFinish: () -> Unit) {
    var fondo by remember { mutableStateOf("") }
    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Column {
            Text("ARRANQUE DE CAJA", fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium, color = Color.White, letterSpacing = 1.sp)
            Text("Ingresa el fondo inicial asignado para cambio.", color = Color.White.copy(0.5f), fontSize = 13.sp)
        }
        
        Surface(color = Color.White.copy(0.05f), shape = RoundedCornerShape(28.dp), modifier = Modifier.fillMaxWidth(), border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(0.08f))) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.AccountBalanceWallet, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(52.dp))
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = fondo, 
                    onValueChange = { if(it.all { c -> c.isDigit() || c == '.' }) fondo = it }, 
                    label = { Text("Fondo en Efectivo") }, 
                    prefix = { Text("$ ", color = Color.White.copy(0.3f)) }, 
                    modifier = Modifier.fillMaxWidth(), 
                    shape = RoundedCornerShape(16.dp), 
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = Color.White.copy(0.2f), focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
                Spacer(Modifier.height(12.dp))
                Text("Se requiere un fondo mínimo de $100.00 pesos.", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.4f))
            }
        }

        Button(
            onClick = { vm.abrirTurno(fondo.toDoubleOrNull() ?: 0.0, session.sucursalActual, session.nombreUsuario) { if (it) onFinish() } }, 
            enabled = !vm.cargando,
            modifier = Modifier.fillMaxWidth().height(64.dp), 
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
        ) {
            Icon(Icons.Default.PowerSettingsNew, null, tint = Color.White)
            Spacer(Modifier.width(12.dp))
            Text("INICIAR OPERACIÓN", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Color.White)
        }
    }
}


