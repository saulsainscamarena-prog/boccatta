package com.bocatta.pos.feature.admin.ui.screens.admin

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bocatta.pos.core.ui.R
import timber.log.Timber
import com.bocatta.pos.core.ui.components.*
import com.bocatta.pos.presentation.ui.components.*
import com.bocatta.pos.feature.admin.viewmodel.AdminViewModel
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TabDashboard(
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
                    BocattaMetricCard(
                        stringResource(R.string.admin_metric_ventas),
                        "$${"%.2f".format(totalVentas)}",
                        MaterialTheme.colorScheme.primary,
                        Modifier.weight(1f),
                        subtitulo = stringResource(R.string.admin_metric_transacciones, ventasHoy.size)
                    )
                    BocattaMetricCard(
                        stringResource(R.string.admin_metric_gastos),
                        "$${"%.2f".format(totalGastos)}",
                        MaterialTheme.colorScheme.error,
                        Modifier.weight(1f)
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    val numAlertas = vm.insumosMaestros.count { it.cantidadEnBase < it.stockMinimo }
                    BocattaMetricCard(
                        stringResource(R.string.admin_metric_alertas),
                        numAlertas.toString(),
                        if (numAlertas > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        Modifier.weight(1f)
                    )
                    BocattaMetricCard(
                        stringResource(R.string.admin_metric_utilidad),
                        "$${"%.2f".format(totalVentas - totalGastos)}",
                        MaterialTheme.colorScheme.primary,
                        Modifier.weight(1f)
                    )
                }
            }
        }

        // Diagnostico section
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
                            // LogHelper.recordBreadcrumb("share_diagnostics", "admin_dashboard")
                            val texto = "Diagnostic not available"
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
            var accionPendiente by remember { mutableStateOf<String?>(null) }
            var showPinDialog by remember { mutableStateOf(false) }
            var showBorrarConfirm by remember { mutableStateOf(false) }

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
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
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
fun DashboardCardPremium(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.heightIn(min = 96.dp)
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
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
            Column {
                Text(
                    title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

