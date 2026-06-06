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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import com.bocatta.pos.R
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CierreCajaScreen(vm: CajaViewModel, session: SessionViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val locale = LocalLocale.current.platformLocale
    LaunchedEffect(session.sucursalActual) { vm.configurarSucursal(session.sucursalActual) }

    val countFallidas by vm.conteoVentasFallidas.collectAsStateWithLifecycle(0)

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

    if (vm.cargando) BocattaLoadingDialog(context.getString(R.string.cierre_loading))

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHost) },
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.cierre_titulo), fontWeight = FontWeight.Black, fontSize = 24.sp, letterSpacing = 2.sp, color = MaterialTheme.colorScheme.onSurface)
                        Text(stringResource(R.string.cierre_subtitulo, session.sucursalActual.uppercase()), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Surface(color = MaterialTheme.colorScheme.onSurface.copy(0.05f), shape = CircleShape, modifier = Modifier.size(40.dp)) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.cierre_volver), tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(10.dp))
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
                Text(stringResource(R.string.cierre_balance_label), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface.copy(0.4f), letterSpacing = 1.sp)

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    BocattaMetricCardPremium(
                        titulo = stringResource(R.string.cierre_ventas_totales),
                        valor = "$${"%.2f".format(vm.totalEfectivoSistema + vm.totalTarjetaSistema)}",
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    BocattaMetricCardPremium(
                        titulo = stringResource(R.string.cierre_gastos_reg),
                        valor = "$${"%.2f".format(vm.totalGastosSistema)}",
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.weight(1f)
                    )
                }

                if (vm.cargandoTurno) {
                    Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surfaceContainer, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(0.3f)), modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            Text(stringResource(R.string.cierre_cargando_turno), fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                            Text(
                                stringResource(R.string.cierre_validando, session.sucursalActual.uppercase()),
                                color = MaterialTheme.colorScheme.onSurface.copy(0.5f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else if (vm.turnoActivo == null) {
                    Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surfaceContainer, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(0.3f)), modifier = Modifier.fillMaxWidth()) {
                        BocattaEmptyState(
                            icono = Icons.Default.LockOpen,
                            titulo = stringResource(R.string.cierre_turno_inactivo),
                            descripcion = stringResource(R.string.cierre_turno_inactivo_desc),
                            modifier = Modifier.fillMaxWidth().padding(32.dp)
                        )
                    }
                } else {
                    val turno = vm.turnoActivo ?: return@Column
                    val esperado = turno.fondoInicial + vm.totalEfectivoSistema - vm.totalGastosSistema

                    // ── CONTEO INDUSTRIAL ────────────────────────────────────────────────
                    Text(stringResource(R.string.cierre_auditoria), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface.copy(0.4f), letterSpacing = 1.sp)

                    Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surfaceContainer, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(0.3f)), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                            BocattaFilaResumen(stringResource(R.string.cierre_fondo_inicial), "$${"%.2f".format(turno.fondoInicial)}")
                            BocattaFilaResumen(stringResource(R.string.cierre_esperado_caja), "$${"%.2f".format(esperado)}", negrita = true)

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(0.3f))

                            OutlinedTextField(
                                value = vm.efectivoContado,
                                onValueChange = { vm.efectivoContado = it },
                                label = { Text(stringResource(R.string.cierre_efectivo_contado), fontWeight = FontWeight.Bold) },
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
                                label = { Text(stringResource(R.string.cierre_tarjeta), fontWeight = FontWeight.Bold) },
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
                                    stringResource(if (cuadra) R.string.cierre_balanceada else R.string.cierre_discrepancia),
                                    fontWeight = FontWeight.Black,
                                    color = statusColor,
                                    fontSize = 14.sp,
                                    letterSpacing = 1.sp
                                )
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    "$${String.format(locale, "%+.2f", diff)}",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 48.sp,
                                    color = statusColor
                                )
                                Text(
                                    if (cuadra) stringResource(R.string.cierre_margen, "%.0f".format(vm.toleranciaEfectivo))
                                    else stringResource(R.string.cierre_tolerancia, "%.0f".format(vm.toleranciaEfectivo)),
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
                                Icon(Icons.Default.Warning, stringResource(R.string.cierre_alerta_advertencia), tint = MaterialTheme.colorScheme.error)
                                Spacer(Modifier.width(16.dp))
                                Column {
                                    Text(
                                        stringResource(R.string.cierre_cancelaciones, vm.numCancelacionesPendientes),
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.error,
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        stringResource(R.string.cierre_cancelaciones_req),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.error.copy(0.8f),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                    if (countFallidas > 0) {
                        if (session.esAdmin) {
                            TextButton(
                                onClick = { vm.abrirDialogoVentasFallidas() },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = stringResource(R.string.cierre_alerta_advertencia),
                                    tint = Color(0xFFFFB300),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "${stringResource(R.string.cierre_fallidas, countFallidas)} · ${stringResource(R.string.cierre_gestionar)}",
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFFB300),
                                    fontSize = 12.sp
                                )
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = stringResource(R.string.cierre_alerta_advertencia),
                                    tint = Color(0xFFFFB300).copy(0.7f),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = stringResource(R.string.cierre_fallidas, countFallidas),
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFFB300).copy(0.7f),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    // ── COMPRAS PENDIENTES ─────────────────────────────────────────────
                    if (session.esAdmin && adminVm.comprasPendientes.isNotEmpty()) {
                        Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.tertiary.copy(0.1f), border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(0.3f))) {
                            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(stringResource(R.string.cierre_compras_pendientes), fontWeight = FontWeight.Black, fontSize = 12.sp, letterSpacing = 1.sp, color = MaterialTheme.colorScheme.tertiary)
                                adminVm.comprasPendientes.forEach { compra ->
                                    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
                                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                                Column(Modifier.weight(1f)) {
                                                    Text(compra.insumoNombre, fontWeight = FontWeight.Bold)
                                                    Text("${compra.cantidadComprada} ${compra.presentacion} (${"%.0f".format(compra.contenidoUnidades)} uds) · $$${"%.2f".format(compra.precioPagado)}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.6f))
                                                    Text("${compra.compradoPorNombre} · ${java.text.SimpleDateFormat("dd/MM HH:mm", locale).format(java.util.Date(compra.fecha))}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.4f))
                                                }
                                            }
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Button(onClick = { adminVm.aprobarCompra(compra, session.uid, session.nombreUsuario, "") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp)) { Text(stringResource(R.string.cierre_aprobar), fontSize = 11.sp) }
                                                OutlinedButton(onClick = {
                                                    compraAccion = compra; accionTipo = "reajustar"
                                                    nuevoPrecioAjuste = compra.precioPagado.toString()
                                                    nuevaCantidadAjuste = compra.cantidadComprada.toString()
                                                }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp)) { Text(stringResource(R.string.cierre_reajustar), fontSize = 11.sp) }
                                                OutlinedButton(onClick = {
                                                    compraAccion = compra; accionTipo = "perdida"
                                                }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp)) { Text(stringResource(R.string.cierre_perdida), fontSize = 11.sp) }
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
                                    title = { Text(stringResource(R.string.cierre_reajustar_titulo, compra.insumoNombre), fontWeight = FontWeight.Bold) },
                                    text = {
                                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                            OutlinedTextField(value = nuevaCantidadAjuste, onValueChange = { nuevaCantidadAjuste = it }, label = { Text(stringResource(R.string.cierre_reajustar_cantidad, compra.presentacion)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                                            OutlinedTextField(value = nuevoPrecioAjuste, onValueChange = { nuevoPrecioAjuste = it }, label = { Text(stringResource(R.string.cierre_reajustar_precio)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                                            OutlinedTextField(value = motivoTexto, onValueChange = { motivoTexto = it }, label = { Text(stringResource(R.string.cierre_reajustar_motivo)) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), minLines = 2)
                                        }
                                    },
                                    confirmButton = {
                                        Button(onClick = {
                                            adminVm.reajustarCompra(compra, nuevaCantidadAjuste.toDoubleOrNull() ?: compra.cantidadComprada, nuevoPrecioAjuste.toDoubleOrNull() ?: compra.precioPagado, motivoTexto, session.uid, session.nombreUsuario)
                                            compraAccion = null; motivoTexto = ""
                                        }, enabled = motivoTexto.isNotBlank()) { Text(stringResource(R.string.cierre_guardar_aprobar)) }
                                    },
                                    dismissButton = { TextButton(onClick = { compraAccion = null; motivoTexto = "" }) { Text(stringResource(R.string.cierre_cancelar)) } },
                                    shape = RoundedCornerShape(20.dp)
                                )
                            }
                            "perdida" -> {
                                AlertDialog(
                                    onDismissRequest = { compraAccion = null; motivoTexto = ""; confirmacionEscrita = "" },
                                    title = { Text(stringResource(R.string.cierre_perdida_titulo), fontWeight = FontWeight.Bold) },
                                    text = {
                                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                            Text(stringResource(R.string.cierre_perdida_desc, "%.0f".format(compra.cantidadComprada * compra.contenidoUnidades), "$${"%.2f".format(compra.precioPagado)}"))
                                            OutlinedTextField(value = motivoTexto, onValueChange = { motivoTexto = it }, label = { Text(stringResource(R.string.cierre_perdida_motivo)) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), minLines = 2)
                                            OutlinedTextField(value = confirmacionEscrita, onValueChange = { confirmacionEscrita = it }, label = { Text(stringResource(R.string.cierre_perdida_confirmar)) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true)
                                        }
                                    },
                                    confirmButton = {
                                        Button(onClick = {
                                            adminVm.registrarPerdida(compra, motivoTexto, session.uid, session.nombreUsuario)
                                            compraAccion = null; motivoTexto = ""; confirmacionEscrita = ""
                                        }, enabled = motivoTexto.isNotBlank() && confirmacionEscrita == "CONFIRMAR", colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text(stringResource(R.string.cierre_registrar_perdida)) }
                                    },
                    dismissButton = { TextButton(onClick = { compraAccion = null; motivoTexto = ""; confirmacionEscrita = "" }) { Text(stringResource(R.string.cierre_cancelar)) } },
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
                            !puedesCerrar -> stringResource(R.string.cierre_btn_resolver)
                            !vm.cadraCaja && session.esAdmin -> stringResource(R.string.cierre_btn_forzar)
                            else -> stringResource(R.string.cierre_btn_consolidar)
                        },
                        onClick = { mostrarCierre = true },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = puedesCerrar && vm.efectivoContado.isNotBlank(),
                        color = if (vm.cadraCaja) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
                    )

                    if (!puedesCerrar) {
                        Text(
                            stringResource(R.string.cierre_cuadrar_msg),
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
            title = { Text(stringResource(R.string.cierre_confirmar_titulo), fontWeight = FontWeight.Bold) },
            text = {
                ElevatedCard(shape = RoundedCornerShape(16.dp)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        BocattaFilaResumen(stringResource(R.string.cierre_detalle_fondo), "$${"%.2f".format(vm.turnoActivo?.fondoInicial ?: 0.0)}")
                        BocattaFilaResumen(stringResource(R.string.cierre_detalle_ventas_efectivo), "$${"%.2f".format(vm.totalEfectivoSistema)}")
                        BocattaFilaResumen(stringResource(R.string.cierre_detalle_ventas_tarjeta), "$${"%.2f".format(vm.totalTarjetaSistema)}")
                        BocattaFilaResumen(stringResource(R.string.cierre_detalle_gastos), "-$${"%.2f".format(vm.totalGastosSistema)}")
                        BocattaFilaResumen(
                            stringResource(R.string.cierre_detalle_contado),
                            "$${"%.2f".format(vm.efectivoContado.toDoubleOrNull() ?: 0.0)}",
                            negrita = true,
                            separador = true
                        )
                        BocattaFilaResumen(
                            stringResource(R.string.cierre_detalle_diferencia),
                            "$${String.format(locale, "%+.2f", vm.diferenciaCaja)}",
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
                            context.startActivity(Intent.createChooser(intent, context.getString(R.string.cierre_enviar_chooser)))
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.cierre_confirmar_enviar), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarCierre = false }) { Text(stringResource(R.string.cierre_revisar)) }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    if (vm.mostrarDialogoFallidas) {
        AlertDialog(
            onDismissRequest = { vm.cerrarDialogoFallidas() },
            title = { Text(stringResource(R.string.cierre_fallidas_titulo), fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        stringResource(R.string.cierre_fallidas_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(0.7f)
                    )

                    if (vm.ventasFallidas.isEmpty()) {
                        Text(
                            stringResource(R.string.cierre_fallidas_vacio),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    } else {
                        vm.ventasFallidas.forEach { venta ->
                            ElevatedCard(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(stringResource(R.string.cierre_fallidas_folio, venta.codigoTicket), fontWeight = FontWeight.Bold)
                                        Text(
                                            "$$${"%.2f".format(venta.total)}",
                                            fontWeight = FontWeight.Black,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Text(stringResource(R.string.cierre_fallidas_atendio, venta.atendio), fontSize = 12.sp)
                                    Text(stringResource(R.string.cierre_fallidas_metodo, venta.metodoPago), fontSize = 12.sp)
                                    Text(stringResource(R.string.cierre_fallidas_intentos, venta.intentos), fontSize = 12.sp)

                                    val errorMsg = stringResource(R.string.cierre_fallidas_error)
                                    if (errorMsg.isNotBlank()) {
                                        Spacer(Modifier.height(8.dp))
                                        Surface(
                                            color = MaterialTheme.colorScheme.errorContainer.copy(0.4f),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = errorMsg,
                                                color = MaterialTheme.colorScheme.error,
                                                fontSize = 11.sp,
                                                modifier = Modifier.padding(8.dp)
                                            )
                                        }
                                    }

                                    Spacer(Modifier.height(8.dp))
                                    Button(
                                        onClick = {
                                            vm.reintentarVentaFallida(venta.id, session.esAdmin)
                                        },
                                        modifier = Modifier.align(Alignment.End),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(stringResource(R.string.cierre_btn_reintentar), fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { vm.cerrarDialogoFallidas() }) {
                    Text(stringResource(R.string.cierre_btn_cerrar))
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }
}
