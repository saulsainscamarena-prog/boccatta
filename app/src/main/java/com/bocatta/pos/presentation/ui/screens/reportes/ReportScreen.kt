package com.bocatta.pos.presentation.ui.screens.reportes

import android.content.Intent
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bocatta.pos.domain.model.VentaPorDia
import com.bocatta.pos.presentation.ui.components.BocattaEmptyState
import com.bocatta.pos.presentation.ui.components.BocattaMetricCard
import com.bocatta.pos.presentation.ui.components.BocattaSectionTitle
import com.bocatta.pos.presentation.ui.components.BocattaTopBar
import com.bocatta.pos.presentation.ui.theme.*
import com.bocatta.pos.presentation.viewmodel.ReportViewModelV2
import com.bocatta.pos.logging.LogHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(vmV2: ReportViewModelV2, sucursal: String, onBack: () -> Unit) {
    val context = LocalContext.current
    LaunchedEffect(sucursal) { vmV2.escucharReporteHoy(sucursal) }

    var tabSeleccionado by remember { mutableIntStateOf(0) }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            BocattaTopBar(
                title = "Métricas",
                subtitle = sucursal.uppercase(java.util.Locale.getDefault()),
                onBack = onBack,
                actions = {
                    IconButton(onClick = {
                        LogHelper.recordBreadcrumb("share_report_whatsapp", "sucursal=$sucursal")
                        val texto = vmV2.generarTextoCierreWhatsApp(sucursal)
                        com.bocatta.pos.core.ShareUtils.shareTextWhatsAppFallback(
                            context = context,
                            text = texto,
                            subject = "Compartir reporte",
                            logContext = "share_report"
                        )
                    }) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = "Compartir reporte",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Tabs
            SecondaryTabRow(
                selectedTabIndex = tabSeleccionado,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(selected = tabSeleccionado == 0, onClick = { tabSeleccionado = 0 },
                    text = { Text("Hoy", fontWeight = if (tabSeleccionado == 0) FontWeight.Bold else FontWeight.Normal) })
                Tab(selected = tabSeleccionado == 1, onClick = { tabSeleccionado = 1 },
                    text = { Text("Semana", fontWeight = if (tabSeleccionado == 1) FontWeight.Bold else FontWeight.Normal) })
                Tab(selected = tabSeleccionado == 2, onClick = { tabSeleccionado = 2 },
                    text = { Text("Productos", fontWeight = if (tabSeleccionado == 2) FontWeight.Bold else FontWeight.Normal) })
            }

            when (tabSeleccionado) {
                0 -> TabResumenHoy(vmV2)
                1 -> TabSemanal(vmV2)
                2 -> TabProductos(vmV2)
            }
        }
    }
}

@Composable
private fun TabResumenHoy(vmV2: ReportViewModelV2) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        item {
            BocattaSectionTitle("Balance del día")
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    BocattaMetricCard(
                        titulo = "Ventas",
                        valor = "$${"%.0f".format(vmV2.ventasBrutas)}",
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f),
                        subtitulo = "${vmV2.ventasDelDia.size} transacciones"
                    )
                    BocattaMetricCard(
                        titulo = "Gastos",
                        valor = "$${"%.0f".format(vmV2.totalGastos)}",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    BocattaMetricCard(
                        titulo = "Utilidad",
                        valor = "$${"%.0f".format(vmV2.utilidadNeta)}",
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    BocattaMetricCard(
                        titulo = "Promedio/venta",
                        valor = if (vmV2.ventasDelDia.isNotEmpty())
                            "$${"%.0f".format(vmV2.ventasBrutas / vmV2.ventasDelDia.size)}"
                        else "$0",
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Método de pago
        item {
            BocattaSectionTitle("Por método de pago")
        }
        item {
            ElevatedCard(shape = RoundedCornerShape(20.dp)) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    val efectivo = vmV2.ventasDelDia.filter { it.metodoPago == "Efectivo" }.sumOf { it.total }
                    val tarjeta = vmV2.ventasDelDia.filter { it.metodoPago != "Efectivo" }.sumOf { it.total }
                    val total = (efectivo + tarjeta).coerceAtLeast(0.01)

                    FilaMetodoPago("💵 Efectivo", efectivo, total, MaterialTheme.colorScheme.primary)
                    FilaMetodoPago("💳 Tarjeta/Transfer", tarjeta, total, MaterialTheme.colorScheme.primary)
                }
            }
        }

        // Historial del día
        if (vmV2.ventasDelDia.isNotEmpty()) {
            item { BocattaSectionTitle("Últimas transacciones") }
            items(vmV2.ventasDelDia.takeLast(8).reversed(), key = { it.id }) { venta ->
                ElevatedCard(shape = RoundedCornerShape(14.dp)) {
                    Row(
                        modifier = Modifier.padding(14.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "#${venta.codigoTicket.ifBlank { venta.numeroTicket.toString() }}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                venta.metodoPago,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                        Text(
                            "$${"%.2f".format(venta.total)}",
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FilaMetodoPago(label: String, monto: Double, total: Double, color: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(
                "$${"%.2f".format(monto)} · ${"%.0f".format(monto / total * 100)}%",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
        LinearProgressIndicator(
            progress = { (monto / total).toFloat().coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(6.dp),
            color = color,
            trackColor = color.copy(0.12f)
        )
    }
}

@Composable
private fun TabSemanal(vmV2: ReportViewModelV2) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        item { BocattaSectionTitle("Ventas últimos 7 días") }
        item {
            ElevatedCard(shape = RoundedCornerShape(20.dp)) {
                Column(modifier = Modifier.padding(20.dp)) {
                    if (vmV2.ventasPorDia.isEmpty()) {
                        BocattaEmptyState(
                            icono = Icons.Default.BarChart,
                            titulo = "Sin datos semanales",
                            descripcion = "Los datos aparecerán después de varios días de operación",
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        GraficaBarras(datos = vmV2.ventasPorDia, color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(12.dp))
                        // Tabla de valores
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            vmV2.ventasPorDia.forEach { dia ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        dia.etiqueta,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                    Text(
                                        "$${"%.2f".format(dia.total)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TabProductos(vmV2: ReportViewModelV2) {
    if (vmV2.topProductos.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            BocattaEmptyState(
                icono = Icons.Default.Category,
                titulo = "Sin datos de productos",
                descripcion = "Registra ventas para ver el análisis de productos"
            )
        }
        return
    }

    val maxCantidad = vmV2.topProductos.maxOf { it.cantidad }.coerceAtLeast(1)

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        item { BocattaSectionTitle("Top productos del día") }

        items(vmV2.topProductos, key = { it.nombre }) { prod ->
            ElevatedCard(shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(MaterialTheme.colorScheme.primary.copy(0.12f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    prod.nombre.first().uppercase(java.util.Locale.getDefault()),
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 14.sp
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(prod.nombre, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(
                                    "${prod.cantidad} vendidos",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                        Text(
                            "$${"%.2f".format(prod.ingresos)}",
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 15.sp
                        )
                    }
                    LinearProgressIndicator(
                        progress = { prod.cantidad.toFloat() / maxCantidad.toFloat() },
                        modifier = Modifier.fillMaxWidth().height(5.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primary.copy(0.1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun GraficaBarras(datos: List<VentaPorDia>, color: Color) {
    val maxVal = datos.maxOf { it.total }.coerceAtLeast(1.0)
    Canvas(modifier = Modifier.fillMaxWidth().height(140.dp)) {
        val barWidth = (size.width / datos.size) * 0.5f
        val gap = size.width / datos.size
        datos.forEachIndexed { i, item ->
            val h = (item.total / maxVal * (size.height - 16.dp.toPx())).toFloat().coerceAtLeast(4.dp.toPx())
            drawRoundRect(
                color = color,
                topLeft = Offset(i * gap + gap * 0.25f, size.height - h),
                size = Size(barWidth, h),
                cornerRadius = CornerRadius(6.dp.toPx())
            )
        }
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        datos.forEach { Text(it.etiqueta, fontSize = 9.sp, color = MaterialTheme.colorScheme.outline) }
    }
}
