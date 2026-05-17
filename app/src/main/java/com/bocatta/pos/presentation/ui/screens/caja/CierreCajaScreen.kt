package com.bocatta.pos.presentation.ui.screens.caja

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bocatta.pos.presentation.ui.components.*
import com.bocatta.pos.presentation.ui.theme.*
import com.bocatta.pos.domain.model.CompraRegistro
import com.bocatta.pos.presentation.viewmodel.CajaViewModel
import com.bocatta.pos.presentation.viewmodel.SessionViewModel
import com.bocatta.pos.presentation.viewmodel.AdminViewModel
import org.koin.androidx.compose.koinViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CierreCajaScreen(vm: CajaViewModel, session: SessionViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    LaunchedEffect(session.sucursalActual) { vm.configurarSucursal(session.sucursalActual) }

    val snackbarHost = remember { SnackbarHostState() }
    var mostrarCierre by remember { mutableStateOf(false) }
    val adminVm: AdminViewModel = koinViewModel()
    var compraAccion by remember { mutableStateOf<CompraRegistro?>(null) }
    var accionTipo by remember { mutableStateOf("") }
    var motivoTexto by remember { mutableStateOf("") }
    var confirmacionEscrita by remember { mutableStateOf("") }
    var nuevoPrecioAjuste by remember { mutableStateOf("") }
    var nuevaCantidadAjuste by remember { mutableStateOf("") }

    LaunchedEffect(vm.mensajeExito, vm.mensajeError) {
        val msg = vm.mensajeExito ?: vm.mensajeError ?: return@LaunchedEffect
        snackbarHost.showSnackbar(msg)
        vm.limpiarMensajes()
    }

    if (vm.cargando) BocattaLoadingDialog("Procesando cierre...")

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHost) },
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text("CIERRE DE TURNO", fontWeight = FontWeight.Black, fontSize = 24.sp, letterSpacing = 2.sp, color = MaterialTheme.colorScheme.onSurface)
                        Text("CONSOLIDACIÓN FINANCIERA · ${session.sucursalActual.uppercase()}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    }
                },
                navigationIcon = { 
                    IconButton(onClick = onBack) { 
                        Surface(color = MaterialTheme.colorScheme.onSurface.copy(0.05f), shape = CircleShape, modifier = Modifier.size(40.dp)) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(10.dp)) 
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent, titleContentColor = MaterialTheme.colorScheme.onSurface)
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(MaterialTheme.colorScheme.background, MaterialTheme.colorScheme.surface)))) {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // ── TARJETAS DE RESUMEN INDUSTRIAL ────────────────────────────────────────
                Text("BALANCE OPERATIVO", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface.copy(0.4f), letterSpacing = 1.sp)
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    BocattaMetricCardPremium(
                        titulo = "VENTAS TOTALES",
                        valor = "$${"%.2f".format(vm.totalEfectivoSistema + vm.totalTarjetaSistema)}",
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    BocattaMetricCardPremium(
                        titulo = "GASTOS REG.",
                        valor = "$${"%.2f".format(vm.totalGastosSistema)}",
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.weight(1f)
                    )
                }

                if (vm.turnoActivo == null) {
                    Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(0.3f)), modifier = Modifier.fillMaxWidth()) {
                        BocattaEmptyState(
                            icono = Icons.Default.LockOpen,
                            titulo = "TURNO INACTIVO",
                            descripcion = "EL TURNO SE ACTIVA AL REGISTRAR LA APERTURA OPERATIVA",
                            modifier = Modifier.fillMaxWidth().padding(32.dp)
                        )
                    }
                } else {
                    val turno = vm.turnoActivo!!
                    val esperado = turno.fondoInicial + vm.totalEfectivoSistema - vm.totalGastosSistema

                    // ── CONTEO INDUSTRIAL ────────────────────────────────────────────────
                    Text("AUDITORÍA DE CAJA", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface.copy(0.4f), letterSpacing = 1.sp)

                    Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(0.3f)), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                            BocattaFilaResumen("FONDO INICIAL", "$${"%.2f".format(turno.fondoInicial)}")
                            BocattaFilaResumen("ESPERADO EN CAJA", "$${"%.2f".format(esperado)}", negrita = true)

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(0.3f))

                            OutlinedTextField(
                                value = vm.efectivoContado,
                                onValueChange = { vm.efectivoContado = it },
                                label = { Text("EFECTIVO CONTADO", fontWeight = FontWeight.Bold) },
                                prefix = { Text("$ ", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurface.copy(0.4f)
                                )
                            )

                            OutlinedTextField(
                                value = vm.tarjetaContada,
                                onValueChange = { vm.tarjetaContada = it },
                                label = { Text("TARJETA / TRANSFERENCIA", fontWeight = FontWeight.Bold) },
                                prefix = { Text("$ ", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurface.copy(0.4f)
                                )
                            )
                        }
                    }

                    // ── RESULTADO DEL CUADRE INDUSTRIAL ───────────────────────────────────
                    if (vm.efectivoContado.isNotBlank()) {
                        val diff = vm.diferenciaCaja
                        val cuadra = kotlin.math.abs(diff) <= vm.toleranciaEfectivo
                        val statusColor = when {
                            cuadra -> MaterialTheme.colorScheme.tertiary
                            kotlin.math.abs(diff) <= vm.toleranciaEfectivo * 2 -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.error
                        }

                        Surface(
                            shape = RoundedCornerShape(28.dp),
                            color = statusColor.copy(0.1f),
                            border = BorderStroke(1.dp, statusColor.copy(0.3f))
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp).fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    if (cuadra) "✓ CAJA BALANCEADA" else "⚠ DISCREPANCIA DETECTADA",
                                    fontWeight = FontWeight.Black,
                                    color = statusColor,
                                    fontSize = 14.sp,
                                    letterSpacing = 1.sp
                                )
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    "$${String.format(java.util.Locale.getDefault(), "%+.2f", diff)}",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 48.sp,
                                    color = statusColor
                                )
                                Text(
                                    if (cuadra) "DENTRO DEL MARGEN OPERATIVO ±$${"%.0f".format(vm.toleranciaEfectivo)}"
                                    else "TOLERANCIA MÁXIMA: ±$${"%.0f".format(vm.toleranciaEfectivo)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = statusColor.copy(0.7f),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // ── ALERTAS INDUSTRIALES ────────────────────────────────────────────────
                    if (vm.tieneCancelacionesPendientes) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.error.copy(0.1f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(0.3f))
                        ) {
                            Row(
                                modifier = Modifier.padding(20.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error)
                                Spacer(Modifier.width(16.dp))
                                Column {
                                    Text(
                                        "${vm.numCancelacionesPendientes} CANCELACIONES PENDIENTES",
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.error,
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        "REQUIEREN AUTORIZACIÓN ANTES DEL CIERRE",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.error.copy(0.8f),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // ── COMPRAS PENDIENTES ─────────────────────────────────────────────
                    if (session.esAdmin && adminVm.comprasPendientes.isNotEmpty()) {
                        Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.tertiary.copy(0.1f), border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(0.3f))) {
                            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("COMPRAS PENDIENTES DE AUDITORÍA", fontWeight = FontWeight.Black, fontSize = 12.sp, letterSpacing = 1.sp, color = MaterialTheme.colorScheme.tertiary)
                                adminVm.comprasPendientes.forEach { compra ->
                                    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)) {
                                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                                Column(Modifier.weight(1f)) {
                                                    Text(compra.insumoNombre, fontWeight = FontWeight.Bold)
                                                    Text("${compra.cantidadComprada} ${compra.presentacion} (${"%.0f".format(compra.contenidoUnidades)} uds) · $$${"%.2f".format(compra.precioPagado)}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.6f))
                                                    Text("${compra.compradoPorNombre} · ${java.text.SimpleDateFormat("dd/MM HH:mm", java.util.Locale.getDefault()).format(java.util.Date(compra.fecha))}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.4f))
                                                }
                                            }
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Button(onClick = { adminVm.aprobarCompra(compra, session.uid ?: "", session.nombreUsuario, "") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp)) { Text("✓ Aprobar", fontSize = 11.sp) }
                                                OutlinedButton(onClick = {
                                                    compraAccion = compra; accionTipo = "reajustar"
                                                    nuevoPrecioAjuste = compra.precioPagado.toString()
                                                    nuevaCantidadAjuste = compra.cantidadComprada.toString()
                                                }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp)) { Text("✏️ Reajustar", fontSize = 11.sp) }
                                                OutlinedButton(onClick = {
                                                    compraAccion = compra; accionTipo = "perdida"
                                                }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp)) { Text("🗑 Pérdida", fontSize = 11.sp) }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Diálogos de auditoría
                    compraAccion?.let { compra ->
                        when (accionTipo) {
                            "reajustar" -> {
                                AlertDialog(
                                    onDismissRequest = { compraAccion = null; motivoTexto = "" },
                                    title = { Text("Reajustar compra: ${compra.insumoNombre}", fontWeight = FontWeight.Bold) },
                                    text = {
                                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                            OutlinedTextField(value = nuevaCantidadAjuste, onValueChange = { nuevaCantidadAjuste = it }, label = { Text("Cantidad (${compra.presentacion})") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                                            OutlinedTextField(value = nuevoPrecioAjuste, onValueChange = { nuevoPrecioAjuste = it }, label = { Text("Nuevo precio ($)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                                            OutlinedTextField(value = motivoTexto, onValueChange = { motivoTexto = it }, label = { Text("Motivo del ajuste *") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), minLines = 2)
                                        }
                                    },
                                    confirmButton = {
                                        Button(onClick = {
                                            adminVm.reajustarCompra(compra, nuevaCantidadAjuste.toDoubleOrNull() ?: compra.cantidadComprada, nuevoPrecioAjuste.toDoubleOrNull() ?: compra.precioPagado, motivoTexto, session.uid ?: "", session.nombreUsuario)
                                            compraAccion = null; motivoTexto = ""
                                        }, enabled = motivoTexto.isNotBlank()) { Text("Guardar y aprobar") }
                                    },
                                    dismissButton = { TextButton(onClick = { compraAccion = null; motivoTexto = "" }) { Text("Cancelar") } },
                                    shape = RoundedCornerShape(20.dp)
                                )
                            }
                            "perdida" -> {
                                AlertDialog(
                                    onDismissRequest = { compraAccion = null; motivoTexto = ""; confirmacionEscrita = "" },
                                    title = { Text("¿Registrar como pérdida?", fontWeight = FontWeight.Bold) },
                                    text = {
                                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                            Text("⚠️ Esta acción descuenta ${"%.0f".format(compra.cantidadComprada * compra.contenidoUnidades)} uds del stock y registra $$${"%.2f".format(compra.precioPagado)} como pérdida del día.")
                                            OutlinedTextField(value = motivoTexto, onValueChange = { motivoTexto = it }, label = { Text("Motivo obligatorio *") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), minLines = 2)
                                            OutlinedTextField(value = confirmacionEscrita, onValueChange = { confirmacionEscrita = it }, label = { Text("Escribe CONFIRMAR") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true)
                                        }
                                    },
                                    confirmButton = {
                                        Button(onClick = {
                                            adminVm.registrarPerdida(compra, motivoTexto, session.uid ?: "", session.nombreUsuario)
                                            compraAccion = null; motivoTexto = ""; confirmacionEscrita = ""
                                        }, enabled = motivoTexto.isNotBlank() && confirmacionEscrita == "CONFIRMAR", colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Registrar pérdida") }
                                    },
                                    dismissButton = { TextButton(onClick = { compraAccion = null; motivoTexto = ""; confirmacionEscrita = "" }) { Text("Cancelar") } },
                                    shape = RoundedCornerShape(20.dp)
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // ── BOTÓN CIERRE INDUSTRIAL ───────────────────────────────────────────
                    val puedesCerrar = (session.esAdmin || vm.cadraCaja) &&
                        !(vm.tieneCancelacionesPendientes && !session.esAdmin)

                    NeonButton(
                        texto = when {
                            !puedesCerrar -> "RESUELVA DIFERENCIAS PARA CERRAR"
                            !vm.cadraCaja && session.esAdmin -> "FORZAR CIERRE (ADMIN)"
                            else -> "CONSOLIDAR CIERRE DE TURNO"
                        },
                        onClick = { mostrarCierre = true },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = puedesCerrar && vm.efectivoContado.isNotBlank(),
                        color = if (vm.cadraCaja) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
                    )

                    if (!puedesCerrar) {
                        Text(
                            "LA CAJA DEBE CUADRAR DENTRO DE LA TOLERANCIA PARA CONSOLIDAR",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(horizontal = 8.dp),
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }

    // ── DIÁLOGO DE CONFIRMACIÓN ───────────────────────────────────────────────
    if (mostrarCierre) {
        AlertDialog(
            onDismissRequest = { mostrarCierre = false },
            title = { Text("Confirmar cierre de turno", fontWeight = FontWeight.Bold) },
            text = {
                ElevatedCard(shape = RoundedCornerShape(16.dp)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        BocattaFilaResumen("Fondo inicial", "$${"%.2f".format(vm.turnoActivo?.fondoInicial ?: 0.0)}")
                        BocattaFilaResumen("Ventas efectivo", "$${"%.2f".format(vm.totalEfectivoSistema)}")
                        BocattaFilaResumen("Ventas tarjeta", "$${"%.2f".format(vm.totalTarjetaSistema)}")
                        BocattaFilaResumen("Gastos del turno", "-$${"%.2f".format(vm.totalGastosSistema)}")
                        BocattaFilaResumen(
                            "Efectivo contado",
                            "$${"%.2f".format(vm.efectivoContado.toDoubleOrNull() ?: 0.0)}",
                            negrita = true,
                            separador = true
                        )
                        BocattaFilaResumen(
                            "Diferencia",
                            "$${String.format(java.util.Locale.getDefault(), "%+.2f", vm.diferenciaCaja)}",
                            colorValor = if (vm.cadraCaja) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            negrita = true
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        vm.cerrarTurno(session.nombreUsuario, session.esAdmin) { texto ->
                            mostrarCierre = false
                            val intent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, texto)
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(intent, "Enviar cierre"))
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Confirmar y enviar", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarCierre = false }) { Text("Revisar") }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }
}
