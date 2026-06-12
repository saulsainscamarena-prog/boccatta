package com.bocatta.pos.feature.ventas.ui.components

import com.bocatta.pos.presentation.ui.components.*
import timber.log.Timber

import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bocatta.pos.domain.model.MetodoPago
import com.bocatta.pos.domain.model.ItemCarritoV2
import com.bocatta.pos.domain.model.SalesInventoryProductV2
import com.bocatta.pos.presentation.ui.theme.*
import com.bocatta.pos.feature.ventas.viewmodel.SalesViewModelV2
import kotlin.math.roundToLong
import java.util.Locale

@Composable
fun ConfirmacionVentaDialog(
    vmV2: SalesViewModelV2,
    sucursalActual: String,
    context: Context,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { onDismiss() },
        icon = { Icon(Icons.Default.CheckCircle, "Verificado", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp)) },
        title = { Text("VENTA EXITOSA", fontWeight = FontWeight.Black, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text("Codigo de ticket:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(vmV2.ultimoCodigoTicket, fontWeight = FontWeight.Black, fontSize = 28.sp, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(8.dp))
                Text("en ${sucursalActual.uppercase(java.util.Locale.getDefault())}", textAlign = TextAlign.Center)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val ticket = vmV2.ultimoTicketTexto ?: ""
                    Timber.i(
                        "share_ticket_whatsapp",
                        "codigo=${vmV2.ultimoCodigoTicket}, sucursal=$sucursalActual"
                    )
                    com.bocatta.pos.core.ShareUtils.shareTextWhatsAppFallback(
                        context = context,
                        text = ticket,
                        subject = "Enviar ticket",
                        logContext = "share_ticket"
                    )
                    onDismiss()
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Share, "Compartir")
                Spacer(Modifier.width(8.dp))
                Text("WhatsApp")
            }
        },
        dismissButton = {
            TextButton(onClick = { onDismiss() }) {
                Text("Cerrar", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
fun BuscarClienteDialog(
    vmV2: SalesViewModelV2,
    onDismiss: () -> Unit,
    onNavigateToRegistrar: () -> Unit
) {
    var filtroInput by remember { mutableStateOf("") }
    LaunchedEffect(filtroInput) { if (filtroInput.length >= 3) vmV2.buscarCliente(filtroInput) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.People, contentDescription = "Cliente frecuente", tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Text("CLIENTE FRECUENTE", fontWeight = FontWeight.Black)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = filtroInput,
                    onValueChange = { filtroInput = it },
                    label = { Text("Nombre o telefono") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.Search, "Buscar") },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                LazyColumn(modifier = Modifier.heightIn(max = 280.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(vmV2.clientesSugeridos) { c ->
                        ElevatedCard(
                            onClick = { vmV2.seleccionarCliente(c); onDismiss() },
                            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Surface(color = MaterialTheme.colorScheme.primary.copy(0.1f), shape = CircleShape, modifier = Modifier.size(40.dp)) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(c.nombre.firstOrNull()?.uppercase(java.util.Locale.getDefault()) ?: "?", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(c.nombre, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Text(c.telefono, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Spacer(Modifier.height(12.dp))
                                Icon(Icons.Default.ChevronRight, "Siguiente", tint = MaterialTheme.colorScheme.outline)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onDismiss(); onNavigateToRegistrar() }, shape = RoundedCornerShape(12.dp)) {
                Icon(Icons.Default.Add, "Agregar")
                Spacer(Modifier.width(8.dp))
                Text("Nuevo Registro")
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
fun RegistrarClienteDialog(
    vmV2: SalesViewModelV2,
    onDismiss: () -> Unit
) {
    var n by remember { mutableStateOf("") }
    var t by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("NUEVO CLIENTE", fontWeight = FontWeight.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(value = n, onValueChange = { n = it }, label = { Text("Nombre") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true)
                OutlinedTextField(value = t, onValueChange = { t = it }, label = { Text("WhatsApp (10 digitos)") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), singleLine = true)
            }
        },
        confirmButton = {
            Button(onClick = { vmV2.registrarClienteNuevo(n, t); onDismiss() }, enabled = n.length > 3 && t.length == 10, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Text("Registrar Cliente")
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
fun RetiroAlimentoDialog(
    vmV2: SalesViewModelV2,
    sucursalActual: String,
    nombreUsuario: String,
    onDismiss: () -> Unit
) {
    var montoRetiro by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("RETIRO ALIMENTO ($)", fontWeight = FontWeight.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Maximo permitido: $150 pesos por turno.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(
                    value = montoRetiro,
                    onValueChange = { montoRetiro = it },
                    label = { Text("Monto a retirar") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    prefix = { Text("$ ") },
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    vmV2.registrarRetiroAlimento(sucursalActual, nombreUsuario, montoRetiro.toDoubleOrNull() ?: 0.0)
                    onDismiss()
                },
                enabled = (montoRetiro.toDoubleOrNull() ?: 0.0) in 1.0..150.0,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Registrar Retiro") }
        },
        shape = RoundedCornerShape(24.dp)
    )
}


@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PagoSheetV2(
    vmV2: SalesViewModelV2,
    sucursalActual: String,
    nombreUsuario: String,
    onDismiss: () -> Unit
) {
    var pagaCon by remember { mutableStateOf("") }
    var propinaCustom by remember { mutableStateOf("") }
    var propinaPorcentaje by remember { mutableIntStateOf(0) }
    var notaOrden by remember { mutableStateOf("") }
    var mostrarSplitDialog by remember { mutableStateOf(false) }
    var mostrarOpcionesOrden by remember { mutableStateOf(false) }
    val baseTotal = (vmV2.totalCarrito.toDouble() - vmV2.descuentoLealtad - vmV2.descuentoPromociones - vmV2.descuentoManual).coerceAtLeast(0.0)
    val propina = if (propinaPorcentaje > 0) baseTotal * propinaPorcentaje / 100.0 else propinaCustom.toDoubleOrNull() ?: 0.0
    val total = baseTotal + propina
    val cambio = (pagaCon.toDoubleOrNull() ?: 0.0) - total
    val sumaPagoMixto = vmV2.montosMixtos.values.sum()
    val pagoMixtoCuadra = vmV2.montosMixtos.isNotEmpty() &&
        vmV2.montosMixtos.values.all { it > 0.0 } &&
        (sumaPagoMixto * 100).roundToLong() == (total * 100).roundToLong()
    val pagoSimpleValido = vmV2.metodoPagoSeleccionado != MetodoPago.EFECTIVO || cambio >= 0
    val pagoValido = if (vmV2.pagoMixtoActivo) pagoMixtoCuadra else pagoSimpleValido
    val pagoEnProceso = vmV2.cargando
    val esAdmin = vmV2.rolUsuario == com.bocatta.pos.domain.model.Rol.ADMIN
    val opcionesOrdenActivas = vmV2.splitActivo || vmV2.esConsumoEmpleado
    val sinStock = vmV2.carrito.any { item -> (vmV2.alertasStock[item.producto.id] ?: 99.0) <= 0 }
    val errorPago = vmV2.mensajeError
    LaunchedEffect(total, vmV2.splitActivo) {
        if (vmV2.splitActivo && vmV2.splitPartes.isNotEmpty()) {
            vmV2.activarSplit(total, vmV2.splitPartes.size)
        }
    }
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { it != SheetValue.Hidden }
    )

    ModalBottomSheet(
        onDismissRequest = {},
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 20.dp)
                .heightIn(max = 680.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Payment, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("Pago final", fontWeight = FontWeight.Black, fontSize = 22.sp)
                    Text("Confirma metodo y total antes de cobrar", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton(onClick = onDismiss, enabled = !pagoEnProceso) { Text("Cerrar") }
            }

            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Total a pagar", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        if (propina > 0) {
                            Text("Incluye $${"%.2f".format(propina)} de propina", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    Text("$${"%.2f".format(total)}", fontSize = 34.sp, fontWeight = FontWeight.Black)
                }
            }

            AnimatedVisibility(visible = errorPago != null) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Error, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(
                            errorPago.orEmpty(),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                        TextButton(onClick = { vmV2.limpiarError() }) {
                            Text("OK")
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("Metodo de pago", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    MetodoPagoButton("Efectivo", vmV2.metodoPagoSeleccionado == MetodoPago.EFECTIVO, Modifier.weight(1f), !pagoEnProceso) {
                        vmV2.metodoPagoSeleccionado = MetodoPago.EFECTIVO
                    }
                    MetodoPagoButton("Tarjeta", vmV2.metodoPagoSeleccionado == MetodoPago.TARJETA, Modifier.weight(1f), !pagoEnProceso) {
                        vmV2.metodoPagoSeleccionado = MetodoPago.TARJETA
                    }
                    MetodoPagoButton("Transfer.", vmV2.metodoPagoSeleccionado == MetodoPago.TRANSFERENCIA, Modifier.weight(1f), !pagoEnProceso) {
                        vmV2.metodoPagoSeleccionado = MetodoPago.TRANSFERENCIA
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Pago mixto", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                    Switch(
                        checked = vmV2.pagoMixtoActivo,
                        onCheckedChange = { vmV2.pagoMixtoActivo = it },
                        enabled = !pagoEnProceso,
                        colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary)
                    )
                }

                if (vmV2.pagoMixtoActivo) {
                    listOf(MetodoPago.EFECTIVO, MetodoPago.TARJETA, MetodoPago.TRANSFERENCIA).forEach { metodo ->
                        val seleccionado = vmV2.montosMixtos.containsKey(metodo)
                        val montoActual = vmV2.montosMixtos[metodo] ?: 0.0
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Checkbox(checked = seleccionado, onCheckedChange = { vmV2.toggleMetodoMixto(metodo, 0.0) }, enabled = !pagoEnProceso)
                            Text(metodo.valor, modifier = Modifier.weight(1f))
                            if (seleccionado) {
                                OutlinedTextField(
                                    value = if (montoActual > 0) "%.2f".format(montoActual) else "",
                                    onValueChange = { vmV2.actualizarMontoMixto(metodo, it.toDoubleOrNull() ?: 0.0) },
                                    modifier = Modifier.width(112.dp),
                                    prefix = { Text("$ ") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    enabled = !pagoEnProceso,
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp)
                                )
                            }
                        }
                    }
                    val diferencia = total - sumaPagoMixto
                    Surface(
                        color = if (pagoMixtoCuadra) {
                            MaterialTheme.colorScheme.tertiaryContainer
                        } else {
                            MaterialTheme.colorScheme.errorContainer
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                if (pagoMixtoCuadra) "Pago mixto completo" else "Falta cuadrar pago mixto",
                                fontWeight = FontWeight.Bold,
                                color = if (pagoMixtoCuadra) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                "$${"%.2f".format(kotlin.math.abs(diferencia))}",
                                fontWeight = FontWeight.Black,
                                color = if (pagoMixtoCuadra) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }

                if (!vmV2.pagoMixtoActivo && vmV2.metodoPagoSeleccionado == MetodoPago.EFECTIVO) {
                    FilledTonalButton(
                        onClick = {
                            pagaCon = String.format(Locale.US, "%.2f", total)
                            vmV2.finalizarVenta(sucursalActual, nombreUsuario, propina, notaOrden)
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        enabled = total > 0 && !pagoEnProceso && !sinStock
                    ) {
                        Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Cobrar efectivo exacto", fontWeight = FontWeight.Black)
                    }
                    OutlinedTextField(
                        value = pagaCon,
                        onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) pagaCon = it },
                        label = { Text("Recibido con") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        prefix = { Text("$ ") },
                        enabled = !pagoEnProceso,
                        shape = RoundedCornerShape(14.dp)
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(20, 50, 100, 200, 500, 1000).forEach { denom ->
                            FilterChip(
                                selected = pagaCon == denom.toString(),
                                onClick = { pagaCon = denom.toString() },
                                enabled = !pagoEnProceso,
                                label = { Text("$$denom") }
                            )
                        }
                    }
                    if (pagaCon.isNotEmpty() && cambio >= 0) {
                        Surface(color = MaterialTheme.colorScheme.tertiaryContainer, shape = RoundedCornerShape(14.dp)) {
                            Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Cambio", fontWeight = FontWeight.Bold)
                                Text("$${"%.2f".format(cambio)}", fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }

                Text("Propina", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(10, 15, 20).forEach { pct ->
                        FilterChip(
                            selected = propinaPorcentaje == pct,
                            onClick = {
                                propinaPorcentaje = if (propinaPorcentaje == pct) 0 else pct
                                propinaCustom = ""
                            },
                            enabled = !pagoEnProceso,
                            label = { Text("$pct%") }
                        )
                    }
                    OutlinedTextField(
                        value = propinaCustom,
                        onValueChange = {
                            propinaCustom = it.filter { c -> c.isDigit() || c == '.' }
                            propinaPorcentaje = 0
                        },
                        modifier = Modifier.width(112.dp),
                        placeholder = { Text("Monto") },
                        prefix = { Text("$ ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        enabled = !pagoEnProceso,
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp)
                    )
                }

                OutlinedButton(
                    onClick = { mostrarOpcionesOrden = !mostrarOpcionesOrden },
                    enabled = !pagoEnProceso || opcionesOrdenActivas,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        if (mostrarOpcionesOrden || opcionesOrdenActivas) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(if (opcionesOrdenActivas) "Opciones de orden activas" else "Mas opciones")
                }

                AnimatedVisibility(visible = mostrarOpcionesOrden || opcionesOrdenActivas) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = notaOrden,
                            onValueChange = { notaOrden = it },
                            label = { Text("Nota para la orden") },
                            placeholder = { Text("Ej: Sin cebolla, para llevar...") },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !pagoEnProceso,
                            shape = RoundedCornerShape(14.dp),
                            singleLine = true
                        )

                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text("Dividir cuenta", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                            Switch(
                                checked = vmV2.splitActivo,
                                onCheckedChange = { active ->
                                    if (active) {
                                        vmV2.activarSplit(total, 2)
                                        mostrarSplitDialog = true
                                    } else {
                                        vmV2.desactivarSplit()
                                    }
                                },
                                enabled = !pagoEnProceso,
                                colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary)
                            )
                        }

                        if (vmV2.splitActivo) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Desglose de cuenta:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                TextButton(onClick = { mostrarSplitDialog = true }, enabled = !pagoEnProceso) {
                                    Text("Ajustar division", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                            vmV2.splitPartes.forEachIndexed { i, parte ->
                                Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Persona ${i + 1}", fontSize = 12.sp)
                                    Text("$${"%.2f".format(parte.monto)}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                                    Text(parte.metodoPago.valor, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(0.3f))
                        }

                        if (esAdmin) {
                            Surface(
                                onClick = { if (!pagoEnProceso) vmV2.esConsumoEmpleado = !vmV2.esConsumoEmpleado },
                                color = if (vmV2.esConsumoEmpleado) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceContainer,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(checked = vmV2.esConsumoEmpleado, onCheckedChange = { vmV2.esConsumoEmpleado = it }, enabled = !pagoEnProceso)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Consumo de empleado", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                if (mostrarSplitDialog) {
                    SplitPaymentDialog(
                        total = total,
                        initialParts = vmV2.splitPartes,
                        onConfirm = { partes ->
                            vmV2.actualizarSplitPartes(partes)
                            mostrarSplitDialog = false
                        },
                        onDismiss = { mostrarSplitDialog = false }
                    )
                }

                if (sinStock) {
                    Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(14.dp)) {
                        Text(
                            "Stock insuficiente en uno o mas productos",
                            modifier = Modifier.padding(12.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Button(
                enabled = pagoValido && !vmV2.cargando && total > 0 && !sinStock,
                onClick = { vmV2.finalizarVenta(sucursalActual, nombreUsuario, propina, notaOrden) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(18.dp)
            ) {
                if (vmV2.cargando) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Confirmar pago", fontWeight = FontWeight.Black)
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun MetodoPagoButton(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
            contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
        ),
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        Text(label, maxLines = 1, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun EliminarItemDialog(
    item: ItemCarritoV2,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var motivo by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ELIMINAR PRODUCTO", fontWeight = FontWeight.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Estas seguro de eliminar ${item.nombre}?")
                OutlinedTextField(
                    value = motivo,
                    onValueChange = { motivo = it },
                    label = { Text("Motivo de cancelacion") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(motivo) }, enabled = motivo.isNotBlank(), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                Text("Eliminar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialogMerma(
    productos: List<SalesInventoryProductV2>,
    cargando: Boolean,
    onConfirm: (SalesInventoryProductV2, Int, String) -> Unit,
    onDismiss: () -> Unit
) {
    var productoSeleccionado by remember { mutableStateOf<SalesInventoryProductV2?>(null) }
    var cantidadText by remember { mutableStateOf("1") }
    var motivoText by remember { mutableStateOf("") }
    var expandidoMenu by remember { mutableStateOf(false) }

    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(0.3f)),
            modifier = Modifier.padding(16.dp).fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    "REGISTRAR MERMA / DESPERDICIO",
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp
                )
                Text(
                    "El sistema deducirá automáticamente los ingredientes asociados de la receta y lo registrará como egreso con costo $0.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(0.6f)
                )

                // Selector de Producto
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { expandidoMenu = true },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            productoSeleccionado?.nombre?.uppercase(java.util.Locale.getDefault()) ?: "SELECCIONAR PRODUCTO",
                            fontWeight = FontWeight.Bold
                        )
                    }
                    DropdownMenu(
                        expanded = expandidoMenu,
                        onDismissRequest = { expandidoMenu = false },
                        modifier = Modifier.fillMaxWidth(0.8f).heightIn(max = 300.dp)
                    ) {
                        productos.forEach { prod ->
                            DropdownMenuItem(
                                text = { Text(prod.nombre.uppercase(java.util.Locale.getDefault()), fontWeight = FontWeight.Bold) },
                                onClick = {
                                    productoSeleccionado = prod
                                    expandidoMenu = false
                                }
                            )
                        }
                    }
                }

                // Cantidad
                OutlinedTextField(
                    value = cantidadText,
                    onValueChange = { cantidadText = it },
                    label = { Text("Cantidad") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                // Motivo
                OutlinedTextField(
                    value = motivoText,
                    onValueChange = { motivoText = it },
                    label = { Text("Motivo (Ej. Quemado, Caído, Error)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("CANCELAR")
                    }
                    Button(
                        onClick = {
                            val prod = productoSeleccionado
                            val cant = cantidadText.toIntOrNull() ?: 1
                            if (prod != null && cant >= 1 && motivoText.isNotBlank()) {
                                onConfirm(prod, cant, motivoText)
                            }
                        },
                        enabled = productoSeleccionado != null && motivoText.isNotBlank() && !cargando,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        if (cargando) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                        } else {
                            Text("CONFIRMAR")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PantallaLockInactividad(
    usuarioNombre: String,
    onDesbloquear: (String, (Boolean) -> Unit) -> Unit,
    onLogout: () -> Unit
) {
    var pinText by remember { mutableStateOf("") }
    var mensajeError by remember { mutableStateOf<String?>(null) }
    var cargando by remember { mutableStateOf(false) }

    BackHandler(enabled = true) {}

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.76f)),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(0.35f)),
            tonalElevation = 6.dp,
            shadowElevation = 12.dp,
            modifier = Modifier
                .padding(24.dp)
                .widthIn(max = 420.dp)
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = "Pantalla bloqueada",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(64.dp)
                )

                Text(
                    "PANTALLA BLOQUEADA",
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    letterSpacing = 1.sp
                )

                Text(
                    "Sesion inactiva de $usuarioNombre. Ingresa tu PIN de 4 digitos para continuar trabajando.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(0.72f),
                    textAlign = TextAlign.Center
                )

                OutlinedTextField(
                    value = pinText,
                    onValueChange = { newValue ->
                        if (newValue.length <= 4 && newValue.all { c -> c.isDigit() }) {
                            pinText = newValue
                            mensajeError = null
                        }
                    },
                    label = { Text("PIN de acceso") },
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true
                )

                mensajeError?.let {
                    Text(
                        it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = {
                        if (pinText.length == 4) {
                            cargando = true
                            onDesbloquear(pinText) { esValido ->
                                cargando = false
                                if (esValido) {
                                    pinText = ""
                                } else {
                                    mensajeError = "PIN incorrecto o no autorizado."
                                }
                            }
                        }
                    },
                    enabled = pinText.length == 4 && !cargando,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    if (cargando) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Text("DESBLOQUEAR", fontWeight = FontWeight.Black)
                    }
                }

                TextButton(onClick = onLogout) {
                    Text("Cerrar sesion actual", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}




