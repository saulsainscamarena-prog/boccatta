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
import com.bocatta.pos.presentation.viewmodel.CajaViewModel
import com.bocatta.pos.presentation.viewmodel.SessionViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CierreCajaScreen(vm: CajaViewModel, session: SessionViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    LaunchedEffect(session.sucursalActual) { vm.configurarSucursal(session.sucursalActual) }

    val snackbarHost = remember { SnackbarHostState() }
    var mostrarCierre by remember { mutableStateOf(false) }

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
                        Text("CIERRE DE TURNO", fontWeight = FontWeight.Black, fontSize = 24.sp, letterSpacing = 2.sp, color = Color.White)
                        Text("CONSOLIDACIÓN FINANCIERA · ${session.sucursalActual.uppercase()}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    }
                },
                navigationIcon = { 
                    IconButton(onClick = onBack) { 
                        Surface(color = Color.White.copy(0.05f), shape = CircleShape, modifier = Modifier.size(40.dp)) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White, modifier = Modifier.padding(10.dp)) 
                        }
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(containerColor = Color.Transparent, titleContentColor = Color.White)
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(MaterialTheme.colorScheme.background, Color(0xFF10121A))))) {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // ── TARJETAS DE RESUMEN INDUSTRIAL ────────────────────────────────────────
                Text("BALANCE OPERATIVO", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color.White.copy(0.4f), letterSpacing = 1.sp)
                
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
                    Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp), border = BorderStroke(1.dp, Color.White.copy(0.1f)), modifier = Modifier.fillMaxWidth()) {
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
                    Text("AUDITORÍA DE CAJA", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color.White.copy(0.4f), letterSpacing = 1.sp)

                    Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp), border = BorderStroke(1.dp, Color.White.copy(0.1f)), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                            BocattaFilaResumen("FONDO INICIAL", "$${"%.2f".format(turno.fondoInicial)}")
                            BocattaFilaResumen("ESPERADO EN CAJA", "$${"%.2f".format(esperado)}", negrita = true)

                            HorizontalDivider(color = Color.White.copy(0.1f))

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
                                    unfocusedTextColor = Color.White,
                                    focusedTextColor = Color.White,
                                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                                    unfocusedLabelColor = Color.White.copy(0.4f)
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
                                    unfocusedTextColor = Color.White,
                                    focusedTextColor = Color.White,
                                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                                    unfocusedLabelColor = Color.White.copy(0.4f)
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
