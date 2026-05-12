package com.bocatta.pos.presentation.ui.screens.caja

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bocatta.pos.presentation.ui.components.*
import com.bocatta.pos.presentation.ui.theme.*
import com.bocatta.pos.presentation.viewmodel.CajaViewModel
import com.bocatta.pos.presentation.viewmodel.SessionViewModel

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
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHost) },
        topBar = {
            BocattaTopBar(
                title = "Cierre de turno",
                subtitle = session.sucursalActual,
                onBack = onBack
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── TARJETAS DE RESUMEN ────────────────────────────────────────
            BocattaSectionTitle("Balance del turno")
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BocattaMetricCard(
                    titulo = "Ventas",
                    valor = "$${"%.2f".format(vm.totalEfectivoSistema + vm.totalTarjetaSistema)}",
                    color = BocattaSuccess,
                    modifier = Modifier.weight(1f)
                )
                BocattaMetricCard(
                    titulo = "Gastos",
                    valor = "$${"%.2f".format(vm.totalGastosSistema)}",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.weight(1f)
                )
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BocattaMetricCard(
                    titulo = "Efectivo",
                    valor = "$${"%.2f".format(vm.totalEfectivoSistema)}",
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                BocattaMetricCard(
                    titulo = "Tarjeta",
                    valor = "$${"%.2f".format(vm.totalTarjetaSistema)}",
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
            }

            HorizontalDivider()

            if (vm.turnoActivo == null) {
                ElevatedCard(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                    BocattaEmptyState(
                        icono = Icons.Default.LockOpen,
                        titulo = "No hay turno activo",
                        descripcion = "El turno se activa al registrar la apertura del día",
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else {
                val turno = vm.turnoActivo!!
                val esperado = turno.fondoInicial + vm.totalEfectivoSistema - vm.totalGastosSistema

                // ── CONTEO ────────────────────────────────────────────────
                BocattaSectionTitle("Conteo físico")

                ElevatedCard(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        BocattaFilaResumen("Fondo inicial", "$${"%.2f".format(turno.fondoInicial)}")
                        BocattaFilaResumen("Esperado en caja", "$${"%.2f".format(esperado)}", negrita = true)

                        HorizontalDivider()

                        OutlinedTextField(
                            value = vm.efectivoContado,
                            onValueChange = { vm.efectivoContado = it },
                            label = { Text("Efectivo contado físicamente") },
                            prefix = { Text("$  ") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            singleLine = true,
                            supportingText = { Text("Esperado: $${"%.2f".format(esperado)}") }
                        )

                        OutlinedTextField(
                            value = vm.tarjetaContada,
                            onValueChange = { vm.tarjetaContada = it },
                            label = { Text("Cobros por tarjeta / transferencia") },
                            prefix = { Text("$  ") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            singleLine = true,
                            supportingText = { Text("Sistema: $${"%.2f".format(vm.totalTarjetaSistema)}") }
                        )
                    }
                }

                // ── RESULTADO DEL CUADRE ───────────────────────────────────
                if (vm.efectivoContado.isNotBlank()) {
                    val diff = vm.diferenciaCaja
                    val cuadra = kotlin.math.abs(diff) <= vm.toleranciaEfectivo
                    val statusColor = when {
                        cuadra -> BocattaSuccess
                        kotlin.math.abs(diff) <= vm.toleranciaEfectivo * 2 -> BocattaWarning
                        else -> MaterialTheme.colorScheme.error
                    }

                    ElevatedCard(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = statusColor.copy(0.08f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp).fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                if (cuadra) "✓ Caja cuadrada" else "⚠ Diferencia detectada",
                                fontWeight = FontWeight.Black,
                                color = statusColor,
                                fontSize = 16.sp
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "$${String.format(java.util.Locale.getDefault(), "%+.2f", diff)}",
                                fontWeight = FontWeight.Black,
                                fontSize = 36.sp,
                                color = statusColor
                            )
                            Text(
                                if (cuadra) "Dentro del margen de ±$${"%.0f".format(vm.toleranciaEfectivo)}"
                                else "Tolancia: ±$${"%.0f".format(vm.toleranciaEfectivo)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = statusColor.copy(0.7f)
                            )
                        }
                    }
                }

                // ── ALERTAS ────────────────────────────────────────────────
                if (vm.tieneCancelacionesPendientes) {
                    ElevatedCard(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = BocattaWarning.copy(0.08f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Warning, null, tint = BocattaWarning)
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    "${vm.numCancelacionesPendientes} cancelaciones pendientes",
                                    fontWeight = FontWeight.Bold,
                                    color = BocattaWarning
                                )
                                Text(
                                    "Requieren aprobación de admin antes del cierre",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = BocattaWarning.copy(0.8f)
                                )
                            }
                        }
                    }
                }

                // ── BOTÓN CIERRE ───────────────────────────────────────────
                val puedesCerrar = (session.esAdmin || vm.cadraCaja) &&
                    !(vm.tieneCancelacionesPendientes && !session.esAdmin)

                BocattaButton(
                    texto = when {
                        !puedesCerrar -> "Resuelve las diferencias para cerrar"
                        !vm.cadraCaja && session.esAdmin -> "Forzar cierre (Admin)"
                        else -> "Confirmar cierre de turno"
                    },
                    onClick = { mostrarCierre = true },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = puedesCerrar && vm.efectivoContado.isNotBlank(),
                    color = if (vm.cadraCaja) BocattaSuccess else BocattaPrimary,
                    icono = Icons.Default.CheckCircle
                )

                if (!puedesCerrar) {
                    Text(
                        "La caja debe cuadrar dentro de la tolerancia para cerrar el turno",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
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
                            colorValor = if (vm.cadraCaja) BocattaSuccess else MaterialTheme.colorScheme.error,
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
                    colors = ButtonDefaults.buttonColors(containerColor = BocattaSuccess),
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
