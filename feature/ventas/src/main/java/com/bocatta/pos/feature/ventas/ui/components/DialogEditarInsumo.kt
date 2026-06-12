package com.bocatta.pos.feature.ventas.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bocatta.pos.domain.model.InsumoV2

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialogEditarInsumo(
    insumo: InsumoV2,
    onGuardar: (nuevoStockMinimo: Double, conteoFisico: Double?, motivo: String) -> Unit,
    onDismiss: () -> Unit
) {
    var stockMinimo by remember { mutableStateOf(insumo.stockMinimo.toString()) }
    var conteoFisico by remember { mutableStateOf("") }
    var usarConteo by remember { mutableStateOf(false) }
    var motivo by remember { mutableStateOf("") }

    val stockActual = insumo.cantidadEnBase
    val conteoValor = conteoFisico.toDoubleOrNull()
    val diferencia = if (usarConteo && conteoValor != null) conteoValor - stockActual else null

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        title = {
            Text("📦 ${insumo.nombre}", fontWeight = FontWeight.Black, fontSize = 18.sp)
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Info básica
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.primaryContainer.copy(0.3f), modifier = Modifier.weight(1f)) {
                        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Stock actual", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
                            Text("${"%.1f".format(stockActual)} ${insumo.unidadBase}", fontWeight = FontWeight.Black, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.tertiaryContainer.copy(0.3f), modifier = Modifier.weight(1f)) {
                        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Categoría", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
                            Text(insumo.categoria, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                    }
                }

                // Stock mínimo
                OutlinedTextField(
                    value = stockMinimo,
                    onValueChange = { stockMinimo = it },
                    label = { Text("Stock mínimo (${insumo.unidadBase})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Warning, "Advertencia", modifier = Modifier.size(20.dp)) }
                )

                // Conteo físico
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(0.3f))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Conteo físico", fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.weight(1f))
                    Switch(checked = usarConteo, onCheckedChange = { usarConteo = it })
                }

                if (usarConteo) {
                    Text("Ingresa el conteo real. El sistema ajustará automáticamente la diferencia.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
                    OutlinedTextField(
                        value = conteoFisico,
                        onValueChange = { conteoFisico = it },
                        label = { Text("Conteo real (${insumo.unidadBase})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Pin, "PIN", modifier = Modifier.size(20.dp)) }
                    )
                    if (diferencia != null) {
                        val diffColor = when {
                            kotlin.math.abs(diferencia) < 0.001 -> MaterialTheme.colorScheme.primary
                            diferencia > 0 -> MaterialTheme.colorScheme.tertiary
                            else -> MaterialTheme.colorScheme.error
                        }
                        Surface(shape = RoundedCornerShape(12.dp), color = diffColor.copy(0.1f)) {
                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    if (diferencia >= 0) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                                    null, tint = diffColor, modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text(if (diferencia >= 0) "Sobrante: +${"%.1f".format(diferencia)} ${insumo.unidadBase}"
                                    else "Faltante: ${"%.1f".format(diferencia)} ${insumo.unidadBase}",
                                        fontWeight = FontWeight.Bold, color = diffColor, fontSize = 13.sp)
                                    Text("Stock después del ajuste: ${"%.1f".format(conteoValor)} ${insumo.unidadBase}",
                                        fontSize = 11.sp, color = diffColor.copy(0.7f))
                                }
                            }
                        }
                    }
                    OutlinedTextField(
                        value = motivo,
                        onValueChange = { motivo = it },
                        label = { Text("Motivo del ajuste") },
                        placeholder = { Text("Opcional: merma, error de conteo, etc.") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        minLines = 2
                    )
                }

                // Presentaciones de compra
                if (insumo.presentacionesCompra.isNotEmpty()) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(0.3f))
                    Text("Presentaciones de compra", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    insumo.presentacionesCompra.forEach { p ->
                        Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(0.3f), modifier = Modifier.fillMaxWidth()) {
                            Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Inventory, "Inventario", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(8.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(p.nombre, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                    if (p.descripcion.isNotBlank()) Text(p.descripcion, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                    Text("Cancelar")
                }
                Button(
                    onClick = {
                        onGuardar(
                            stockMinimo.toDoubleOrNull() ?: insumo.stockMinimo,
                            if (usarConteo) conteoValor else null,
                            motivo
                        )
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Guardar", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {}
    )
}



