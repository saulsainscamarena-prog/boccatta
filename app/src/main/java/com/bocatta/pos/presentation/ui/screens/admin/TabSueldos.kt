package com.bocatta.pos.presentation.ui.screens.admin

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bocatta.pos.domain.model.ConfiguracionSalarial
import com.bocatta.pos.domain.model.FormaPago
import com.bocatta.pos.domain.model.RegistroPago
import com.bocatta.pos.domain.model.TipoPago
import timber.log.Timber
import com.bocatta.pos.logging.LogHelper
import com.bocatta.pos.presentation.viewmodel.AdminViewModel
import com.bocatta.pos.presentation.viewmodel.SalarioViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TabSueldos(
    salarioVm: SalarioViewModel,
    adminVm: AdminViewModel,
    sucursal: String = "global"
) {
    val context = LocalContext.current
    var selectedEmpleadoId by remember { mutableStateOf("") }
    val pagos = salarioVm.pagos
    val configActual = salarioVm.configuracion
    val cargando = salarioVm.cargando
    val empleados = adminVm.usuarios.filter { it.uid.isNotBlank() }
    val (periodoInicio, periodoFin) = remember { periodoNominaActual() }
    val periodoLabel = remember(periodoInicio, periodoFin) { "${fechaCorta(periodoInicio)} - ${fechaCorta(periodoFin)}" }

    LaunchedEffect(periodoInicio, periodoFin) {
        salarioVm.cargarPagosPeriodo(periodoInicio, periodoFin)
    }

    val pagosPorEmpleado = empleados.associateWith { user ->
        pagos.filter { it.empleadoId == user.uid }
            .maxByOrNull { it.periodoFin.takeIf { fin -> fin > 0L } ?: it.periodoInicio }
    }
    val pagosRegistrados = pagosPorEmpleado.values.filterNotNull()
    val pagados = pagosRegistrados.filter { it.pagado }
    val pendientes = pagosRegistrados.filter { !it.pagado }
    val totalNeto = pagosRegistrados.sumOf { it.salarioNeto }
    val totalPagado = pagados.sumOf { it.salarioNeto }
    val totalPendiente = pendientes.sumOf { it.salarioNeto }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("SUELDOS Y NÓMINA", fontWeight = FontWeight.Black, fontSize = 20.sp)
                Text(periodoLabel, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }
            FilledTonalButton(
                onClick = {
                    LogHelper.recordBreadcrumb("share_payroll_whatsapp", "sucursal=$sucursal")
                    val texto = salarioVm.generarTextoNominaWhatsApp(sucursal, empleados, periodoInicio, periodoFin)
                    compartirNomina(context, texto)
                },
                enabled = empleados.isNotEmpty() && !cargando,
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Compartir")
            }
        }

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            NominaMetricCard("Neto", totalNeto, Icons.Default.Payments)
            NominaMetricCard("Pagado", totalPagado, Icons.Default.CheckCircle)
            NominaMetricCard("Pendiente", totalPendiente, Icons.Default.PendingActions)
            NominaCountCard("Sin pago", empleados.size - pagosRegistrados.size, Icons.Default.PersonOff)
        }

        NominaLimitationsNotice()

        if (empleados.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No hay empleados registrados", color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(empleados, key = { it.uid }) { user ->
                    val pago = pagosPorEmpleado[user]
                    SueldoEmpleadoCard(
                        nombre = user.nombre,
                        rol = user.rol.name,
                        pago = pago,
                        enabled = !cargando,
                        onEditar = {
                            salarioVm.cargarConfiguracion(user.uid)
                            selectedEmpleadoId = user.uid
                        },
                        onRegistrarPago = {
                            salarioVm.registrarPagoPeriodo(user.uid, user.nombre, periodoInicio, periodoFin)
                        },
                        onMarcarPagado = {
                            pago?.let { salarioVm.marcarPagadoPeriodo(it.id, System.currentTimeMillis(), periodoInicio, periodoFin) }
                        }
                    )
                }
            }
        }
    }

    if (selectedEmpleadoId.isNotBlank()) {
        val user = adminVm.usuarios.find { it.uid == selectedEmpleadoId }
        if (user != null) {
            DialogConfigSalario(
                empleadoId = selectedEmpleadoId,
                empleadoNombre = user.nombre,
                configActual = configActual,
                onSave = { salarioVm.guardarConfiguracion(it); selectedEmpleadoId = "" },
                onDismiss = { selectedEmpleadoId = "" }
            )
        }
    }
}

@Composable
private fun NominaMetricCard(label: String, monto: Double, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    ElevatedCard(shape = RoundedCornerShape(14.dp), modifier = Modifier.widthIn(min = 150.dp, max = 220.dp)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(8.dp))
            Column {
                Text(label.uppercase(java.util.Locale.getDefault()), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("$${"%.2f".format(monto)}", fontWeight = FontWeight.Black, fontSize = 16.sp)
            }
        }
    }
}

@Composable
private fun NominaCountCard(label: String, count: Int, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    ElevatedCard(shape = RoundedCornerShape(14.dp), modifier = Modifier.widthIn(min = 130.dp, max = 180.dp)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
            Spacer(Modifier.width(8.dp))
            Column {
                Text(label.uppercase(java.util.Locale.getDefault()), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(count.toString(), fontWeight = FontWeight.Black, fontSize = 16.sp)
            }
        }
    }
}

@Composable
private fun SueldoEmpleadoCard(
    nombre: String,
    rol: String,
    pago: RegistroPago?,
    enabled: Boolean,
    onEditar: () -> Unit,
    onRegistrarPago: () -> Unit,
    onMarcarPagado: () -> Unit
) {
    ElevatedCard(shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(nombre.uppercase(java.util.Locale.getDefault()), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(rol, color = MaterialTheme.colorScheme.outline, fontSize = 12.sp)
                }
                AssistChip(
                    onClick = {},
                    label = {
                        Text(
                            when {
                                pago == null -> "Sin pago"
                                pago.pagado -> "Pagado"
                                else -> "Pendiente"
                            },
                            fontSize = 11.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            when {
                                pago == null -> Icons.Default.RadioButtonUnchecked
                                pago.pagado -> Icons.Default.CheckCircle
                                else -> Icons.Default.PendingActions
                            },
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
            }

            if (pago == null) {
                Text("No hay pago registrado para este periodo.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NominaMiniValue("Bruto", pago.salarioBruto, Modifier.weight(1f))
                    NominaMiniValue("Deducciones", pago.deducciones, Modifier.weight(1f))
                    NominaMiniValue("Neto", pago.salarioNeto, Modifier.weight(1f))
                }
                Text(
                    "${pago.formaPago.name} · ${fechaCorta(pago.periodoInicio)} - ${fechaCorta(pago.periodoFin)}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = onEditar, enabled = enabled, shape = RoundedCornerShape(10.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Configurar")
                }
                if (pago == null) {
                    Button(onClick = onRegistrarPago, enabled = enabled, shape = RoundedCornerShape(10.dp)) {
                        Icon(Icons.Default.AddCard, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Registrar pago")
                    }
                } else if (!pago.pagado) {
                    Button(onClick = onMarcarPagado, enabled = enabled, shape = RoundedCornerShape(10.dp)) {
                        Icon(Icons.Default.Done, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Marcar pagado")
                    }
                }
            }
        }
    }
}

@Composable
private fun NominaLimitationsNotice() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                "Nómina administrativa: el periodo es mensual, el préstamo es una deducción fija y los pagos registrados no se recalculan ni cancelan en esta fase.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
        }
    }
}

@Composable
private fun NominaMiniValue(label: String, value: Double, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(Modifier.padding(10.dp)) {
            Text(label.uppercase(java.util.Locale.getDefault()), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("$${"%.2f".format(value)}", fontSize = 13.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun DialogConfigSalario(
    empleadoId: String,
    empleadoNombre: String,
    configActual: ConfiguracionSalarial?,
    onSave: (ConfiguracionSalarial) -> Unit,
    onDismiss: () -> Unit
) {
    var tipoPago by remember { mutableStateOf(configActual?.tipoPago ?: TipoPago.DIARIO) }
    var salarioBase by remember { mutableStateOf(configActual?.salarioBase?.toString() ?: "250") }
    var formaPago by remember { mutableStateOf(configActual?.formaPago ?: FormaPago.EFECTIVO) }
    var banco by remember { mutableStateOf(configActual?.banco ?: "") }
    var clabe by remember { mutableStateOf(configActual?.clabe ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(empleadoNombre.uppercase(java.util.Locale.getDefault()), fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Tipo de pago:", style = MaterialTheme.typography.labelSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    TipoPago.entries.forEach { t ->
                        FilterChip(
                            selected = tipoPago == t,
                            onClick = { tipoPago = t },
                            label = { Text(t.name.replace("_", " "), fontSize = 10.sp) }
                        )
                    }
                }
                OutlinedTextField(
                    value = salarioBase,
                    onValueChange = { salarioBase = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Salario base ($)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(12.dp)
                )
                Text("Forma de pago:", style = MaterialTheme.typography.labelSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    FormaPago.entries.forEach { f ->
                        FilterChip(
                            selected = formaPago == f,
                            onClick = { formaPago = f },
                            label = { Text(f.name, fontSize = 10.sp) }
                        )
                    }
                }
                if (formaPago == FormaPago.TRANSFERENCIA) {
                    OutlinedTextField(value = banco, onValueChange = { banco = it }, label = { Text("Banco") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                    OutlinedTextField(value = clabe, onValueChange = { clabe = it }, label = { Text("CLABE") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        ConfiguracionSalarial(
                            empleadoId = empleadoId,
                            tipoPago = tipoPago,
                            salarioBase = salarioBase.toDoubleOrNull() ?: 0.0,
                            formaPago = formaPago,
                            banco = banco,
                            clabe = clabe
                        )
                    )
                    onDismiss()
                },
                enabled = (salarioBase.toDoubleOrNull() ?: 0.0) > 0,
                shape = RoundedCornerShape(12.dp)
            ) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
        shape = RoundedCornerShape(20.dp)
    )
}

private fun periodoNominaActual(): Pair<Long, Long> {
    val cal = Calendar.getInstance()
    cal.set(Calendar.DAY_OF_MONTH, 1)
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    val inicio = cal.timeInMillis
    cal.add(Calendar.MONTH, 1)
    cal.add(Calendar.MILLISECOND, -1)
    return inicio to cal.timeInMillis
}

private fun fechaCorta(timestamp: Long): String {
    if (timestamp <= 0L) return "--/--/----"
    return SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("es-MX")).format(Date(timestamp))
}

private fun compartirNomina(context: android.content.Context, texto: String) {
    com.bocatta.pos.core.ShareUtils.shareTextWhatsAppFallback(
        context = context,
        text = texto,
        subject = "Compartir nómina",
        logContext = "share_payroll"
    )
}
