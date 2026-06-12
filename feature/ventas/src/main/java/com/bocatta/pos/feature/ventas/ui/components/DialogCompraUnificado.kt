package com.bocatta.pos.feature.ventas.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bocatta.pos.domain.model.InsumoV2
import com.bocatta.pos.domain.model.PresentacionCompraPreview
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialogCompraUnificado(
    insumos: List<InsumoV2>,
    nombreUsuario: String,
    usuarioId: String,
    sucursal: String,
    esAdmin: Boolean,
    onConfirmar: (
        insumoId: String,
        insumoNombre: String,
        presentacion: String,
        cantidadComprada: Double,
        contenidoUnidades: Double,
        precioPagado: Double
    ) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var paso by remember { mutableIntStateOf(0) }
    var selectedInsumo by remember { mutableStateOf<InsumoV2?>(null) }
    var selectedPresentacion by remember { mutableStateOf<PresentacionCompraPreview?>(null) }
    var presentacionManual by remember { mutableStateOf(false) }
    var cantidadComprada by remember { mutableStateOf("1") }
    var contenidoUnidades by remember { mutableStateOf("") }
    var precioPagado by remember { mutableStateOf("") }

    val insumosFiltrados = remember(insumos, searchQuery) {
        insumos.filter { searchQuery.isBlank() || it.nombre.contains(searchQuery, ignoreCase = true) }
            .sortedBy { it.nombre }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth().heightIn(max = 700.dp),
        title = {
            Text(
                if (esAdmin) "Compra rapida" else "Registrar compra",
                fontWeight = FontWeight.Black,
                fontSize = 18.sp
            )
        },
        text = {
            when (paso) {
                0 -> SelectorInsumoCompra(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    insumos = insumosFiltrados,
                    onSelect = { insumo ->
                        selectedInsumo = insumo
                        selectedPresentacion = null
                        presentacionManual = false
                        cantidadComprada = "1"
                        contenidoUnidades = ""
                        precioPagado = ""
                        paso = 1
                    }
                )

                1 -> {
                    val insumo = selectedInsumo ?: return@AlertDialog
                    val presentaciones = insumo.presentacionesCompra
                    var expandPres by remember { mutableStateOf(false) }
                    val cant = cantidadComprada.toDoubleOrNull() ?: 0.0
                    val cont = contenidoUnidades.toDoubleOrNull() ?: 0.0
                    val precio = precioPagado.toDoubleOrNull() ?: 0.0
                    val totalUnidades = cant * cont
                    val fueraDeRango = selectedPresentacion?.let { p ->
                        totalUnidades > 0 &&
                            (totalUnidades < p.contenidoMin ||
                                totalUnidades > p.contenidoMax ||
                                (precio > 0 && (precio < p.precioMin || precio > p.precioMax)))
                    } ?: false

                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(insumo.nombre, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(
                            "Stock actual: ${"%.1f".format(insumo.cantidadEnBase)} ${insumo.unidadBase}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(0.55f)
                        )

                        if (presentaciones.isNotEmpty()) {
                            Box {
                                OutlinedButton(
                                    onClick = { expandPres = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    val presActual = selectedPresentacion
                                    Text(
                                        when {
                                            presentacionManual -> "Otra / peso real"
                                            presActual != null -> presActual.nombre
                                            else -> "Seleccionar presentacion"
                                        },
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(Modifier.weight(1f))
                                    Icon(Icons.Default.ExpandMore, "Expandir")
                                }
                                DropdownMenu(expanded = expandPres, onDismissRequest = { expandPres = false }) {
                                    presentaciones.forEach { pres ->
                                        DropdownMenuItem(
                                            text = {
                                                Column {
                                                    Text(pres.nombre, fontWeight = FontWeight.SemiBold)
                                                    if (pres.descripcion.isNotBlank()) {
                                                        Text(
                                                            pres.descripcion,
                                                            fontSize = 11.sp,
                                                            color = MaterialTheme.colorScheme.onSurface.copy(0.5f)
                                                        )
                                                    }
                                                }
                                            },
                                            onClick = {
                                                selectedPresentacion = pres
                                                presentacionManual = false
                                                contenidoUnidades =
                                                    if (pres.contenidoSugerido == pres.contenidoSugerido.toLong().toDouble()) {
                                                        pres.contenidoSugerido.toLong().toString()
                                                    } else {
                                                        pres.contenidoSugerido.toString()
                                                    }
                                                precioPagado = ""
                                                expandPres = false
                                            }
                                        )
                                    }
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text("Otra / peso real", fontWeight = FontWeight.SemiBold)
                                                Text(
                                                    "Captura el contenido comprado manualmente",
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurface.copy(0.5f)
                                                )
                                            }
                                        },
                                        onClick = {
                                            selectedPresentacion = null
                                            presentacionManual = true
                                            contenidoUnidades = ""
                                            precioPagado = ""
                                            expandPres = false
                                        }
                                    )
                                }
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = cantidadComprada,
                                onValueChange = { cantidadComprada = it },
                                label = { Text("Cantidad") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.width(112.dp),
                                shape = RoundedCornerShape(10.dp),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = contenidoUnidades,
                                onValueChange = { contenidoUnidades = it },
                                label = { Text("Contenido (${insumo.unidadBase})") },
                                supportingText = { Text("Ej. peso comprado o unidades por caja") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                singleLine = true,
                                enabled = presentaciones.isEmpty() || selectedPresentacion != null || presentacionManual
                            )
                        }

                        OutlinedTextField(
                            value = precioPagado,
                            onValueChange = { precioPagado = it },
                            label = { Text("Precio pagado ($)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.AttachMoney, "Monto") }
                        )

                        if (totalUnidades > 0) {
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (fueraDeRango) {
                                        MaterialTheme.colorScheme.errorContainer.copy(0.4f)
                                    } else {
                                        MaterialTheme.colorScheme.primaryContainer.copy(0.3f)
                                    }
                                )
                            ) {
                                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        "Se agregan ${"%.2f".format(totalUnidades)} ${insumo.unidadBase} al stock",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        "Stock total: ${"%.2f".format(insumo.cantidadEnBase + totalUnidades)} ${insumo.unidadBase}",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(0.6f)
                                    )
                                    if (precio > 0) {
                                        Text(
                                            "Costo unitario: $${"%.4f".format(precio / totalUnidades)} por ${insumo.unidadBase}",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(0.6f)
                                        )
                                    }
                                    if (fueraDeRango) {
                                        Text(
                                            "Valores fuera del rango esperado. Revisa antes de confirmar.",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.error,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    if (insumo.unidadBase.lowercase(Locale.ROOT) in setOf("g", "ml")) {
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            "\uD83D\uDCA1 Nota: Estas registrando en ${insumo.unidadBase.uppercase(Locale.ROOT)}. Asegurate de que el contenido multiplique las unidades reales (ej. 1 caja de 5kg = 5000g).",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.SemiBold,
                                            lineHeight = 14.sp
                                        )
                                    }
                                }
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = {
                                    paso = 0
                                    selectedInsumo = null
                                    selectedPresentacion = null
                                    presentacionManual = false
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Atras")
                            }
                            Button(
                                onClick = {
                                    if (totalUnidades > 0 && precio > 0) {
                                        onConfirmar(
                                            insumo.id,
                                            insumo.nombre,
                                            if (presentacionManual) "Otra / peso real" else selectedPresentacion?.nombre ?: "Directo",
                                            cant,
                                            cont,
                                            precio
                                        )
                                        onDismiss()
                                    }
                                },
                                enabled = totalUnidades > 0 && precio > 0,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (fueraDeRango && precio > 0) {
                                        MaterialTheme.colorScheme.error
                                    } else {
                                        MaterialTheme.colorScheme.primary
                                    }
                                )
                            ) {
                                Text(if (fueraDeRango && precio > 0) "Confirmar" else "Confirmar", fontWeight = FontWeight.Bold)
                            }
                        }

                        if (!esAdmin) {
                            Text("Pendiente de auditoria del administrador", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            if (paso == 0) {
                TextButton(onClick = onDismiss) { Text("Cancelar") }
            }
        }
    )
}

@Composable
private fun SelectorInsumoCompra(
    query: String,
    onQueryChange: (String) -> Unit,
    insumos: List<InsumoV2>,
    onSelect: (InsumoV2) -> Unit
) {
    Column(modifier = Modifier.heightIn(max = 500.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text("Buscar insumo...") },
            leadingIcon = { Icon(Icons.Default.Search, "Buscar") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            singleLine = true
        )
        Spacer(Modifier.height(12.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
            items(insumos, key = { it.id }) { insumo ->
                val enStock = insumo.cantidadEnBase > 0
                Surface(
                    onClick = { onSelect(insumo) },
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(if (enStock) 0.3f else 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(insumo.nombre, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text(
                                if (enStock) {
                                    "Stock: ${"%.1f".format(insumo.cantidadEnBase)} ${insumo.unidadBase}"
                                } else {
                                    "Sin stock"
                                },
                                fontSize = 11.sp,
                                color = if (enStock) {
                                    MaterialTheme.colorScheme.onSurface.copy(0.5f)
                                } else {
                                    MaterialTheme.colorScheme.error
                                }
                            )
                        }
                        Icon(Icons.Default.ChevronRight, "Siguiente", tint = MaterialTheme.colorScheme.onSurface.copy(0.3f))
                    }
                }
            }
        }
    }
}


