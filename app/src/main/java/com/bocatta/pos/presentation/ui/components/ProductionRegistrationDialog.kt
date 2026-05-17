package com.bocatta.pos.presentation.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.bocatta.pos.presentation.ui.theme.*
import com.bocatta.pos.presentation.viewmodel.InventoryViewModel
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProductionRegistrationDialog(
    vm: InventoryViewModel,
    sucursal: String,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var insumoSeleccionado by remember { mutableStateOf("") }
    var porcionesText by remember { mutableStateOf("") }
    var tandasText by remember { mutableStateOf("1") }
    var sobranteText by remember { mutableStateOf("") }
    var compraPesoText by remember { mutableStateOf("") }
    var porcionPesoText by remember { mutableStateOf("") }
    var modo by remember { mutableStateOf("producir") } // "producir" | "peso" | "presentacion"
    var expandedMenu by remember { mutableStateOf(false) }
    var expandedPresMenu by remember { mutableStateOf(false) }
    var presentaciones by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var presentacionSeleccionada by remember { mutableStateOf<Map<String, Any>?>(null) }
    var cantidadPresentacionText by remember { mutableStateOf("") }

    // Cargar presentaciones cuando cambia el insumo
    LaunchedEffect(insumoSeleccionado) {
        if (insumoSeleccionado.isNotBlank()) {
            try {
                val db = com.bocatta.pos.network.firebase.FirebaseFirestoreProvider.db
                val snap = db.collection(com.bocatta.pos.core.constants.FirestoreCollections.PRESENTACIONES)
                    .whereEqualTo("insumoId", insumoSeleccionado)
                    .get().await()
                presentaciones = snap.documents.mapNotNull { it.data?.plus("id" to it.id) }
                presentacionSeleccionada = null
                cantidadPresentacionText = ""
            } catch (_: Exception) { presentaciones = emptyList() }
        } else {
            presentaciones = emptyList()
        }
    }
    val pesoTotal = compraPesoText.toDoubleOrNull() ?: 0.0
    val pesoPorcion = porcionPesoText.toDoubleOrNull()
        ?: (if (insumoSeleccionado.contains("boneless", true)) 250.0
            else if (insumoSeleccionado.contains("nuggets", true)) 210.0
            else if (insumoSeleccionado.contains("papas", true)) 200.0
            else 0.0)

    val insumosDisponibles = vm.maestroInsumos.values.toList().filter { it.id.isNotBlank() }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(0.3f)),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(0.1f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.PrecisionManufacturing, "Produccion", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text("CONTROL DE INVENTARIO", fontWeight = FontWeight.Black, fontSize = 10.sp, color = MaterialTheme.colorScheme.primary, letterSpacing = 1.sp)
                        Text("PRODUCCIÓN Y COMPRAS", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                }

                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(0.3f))

                // Toggle modo
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = modo == "producir",
                        onClick = { modo = "producir" },
                        label = { Text("PRODUCIR") },
                        leadingIcon = { Icon(Icons.Default.Factory, "Fabrica", modifier = Modifier.size(16.dp)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                    FilterChip(
                        selected = modo == "peso",
                        onClick = { modo = "peso" },
                        label = { Text("COMPRAR POR PESO") },
                        leadingIcon = { Icon(Icons.Default.MonitorWeight, "Peso", modifier = Modifier.size(16.dp)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.tertiary,
                            selectedLabelColor = MaterialTheme.colorScheme.onTertiary
                        )
                    )
                    FilterChip(
                        selected = modo == "presentacion",
                        onClick = { modo = "presentacion" },
                        label = { Text("POR PRESENTACIÓN") },
                        leadingIcon = { Icon(Icons.Default.Inventory2, "Inventario", modifier = Modifier.size(16.dp)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.secondary,
                            selectedLabelColor = MaterialTheme.colorScheme.onSecondary
                        )
                    )
                }

                // Selección de insumo
                Text("INSUMO", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.6f))
                ExposedDropdownMenuBox(
                    expanded = expandedMenu,
                    onExpandedChange = { expandedMenu = it }
                ) {
                    OutlinedTextField(
                        value = if (insumoSeleccionado.isNotBlank())
                            insumosDisponibles.find { it.id == insumoSeleccionado }?.nombre ?: insumoSeleccionado
                        else "",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedMenu) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = expandedMenu,
                        onDismissRequest = { expandedMenu = false }
                    ) {
                        insumosDisponibles.forEach { insumo ->
                            DropdownMenuItem(
                                text = { Text(insumo.nombre) },
                                onClick = { insumoSeleccionado = insumo.id; expandedMenu = false }
                            )
                        }
                    }
                }

                if (modo == "producir") {
                    // Campos de producción por tanda
                    OutlinedTextField(
                        value = porcionesText,
                        onValueChange = { porcionesText = it.filter { c -> c.isDigit() } },
                        label = { Text("Porciones obtenidas") },
                        placeholder = { Text("Ej: 60 para masa de crepas") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                    OutlinedTextField(
                        value = tandasText,
                        onValueChange = { tandasText = it.filter { c -> c.isDigit() } },
                        label = { Text("Tandas preparadas") },
                        placeholder = { Text("Ej: 1") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                    OutlinedTextField(
                        value = sobranteText,
                        onValueChange = { sobranteText = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("Sobrante anterior (opcional)") },
                        placeholder = { Text("0") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                }

                if (modo == "peso") {
                    // Modo compra por peso
                    OutlinedTextField(
                        value = compraPesoText,
                        onValueChange = { compraPesoText = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("Peso total comprado (kg)") },
                        placeholder = { Text("Ej: 2.6") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.tertiary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                    OutlinedTextField(
                        value = porcionPesoText,
                        onValueChange = { porcionPesoText = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("Peso por porción (g)") },
                        placeholder = { Text(if (pesoPorcion > 0) "${pesoPorcion.toInt()}" else "Ej: 250") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.tertiary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )

                    // Vista previa de cálculo
                    if (pesoTotal > 0 && pesoPorcion > 0) {
                        val porciones = (pesoTotal * 1000 / pesoPorcion).toInt()
                        Surface(
                            color = MaterialTheme.colorScheme.tertiary.copy(0.1f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Calculate, "Calcular", tint = MaterialTheme.colorScheme.tertiary)
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text("Rendimiento estimado:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Text("$porciones porciones de ${pesoPorcion.toInt()}g", fontWeight = FontWeight.Black, fontSize = 18.sp, color = MaterialTheme.colorScheme.tertiary)
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Botón de acción
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            if (insumoSeleccionado.isBlank()) return@Button
                            val porciones = porcionesText.toIntOrNull() ?: 0
                            val tandas = tandasText.toIntOrNull() ?: 1
                            val sobrante = sobranteText.toDoubleOrNull() ?: 0.0

                            if (modo == "producir" && porciones > 0) {
                                vm.registrarProduccion(
                                    insumoId = insumoSeleccionado,
                                    porcionesObtenidas = porciones.toDouble(),
                                    tandasPreparadas = tandas.toDouble(),
                                    sobranteAnterior = sobrante,
                                    onResult = { if (it) onDismiss() }
                                )
                            } else if (modo == "peso" && pesoTotal > 0 && pesoPorcion > 0) {
                                val totalKilos = pesoTotal
                                val porcionesCalc = (totalKilos * 1000 / pesoPorcion).toInt()
                                vm.registrarProduccion(
                                    insumoId = insumoSeleccionado,
                                    porcionesObtenidas = porcionesCalc.toDouble(),
                                    tandasPreparadas = 1.0,
                                    sobranteAnterior = 0.0,
                                    onResult = { if (it) onDismiss() }
                                )
                            } else if (modo == "presentacion") {
                                val pres = presentacionSeleccionada
                                val cant = cantidadPresentacionText.toIntOrNull() ?: 0
                                if (pres != null && cant > 0) {
                                    val totalUnidades = cant * (pres["contenido"] as? Number)?.toInt()!! * (pres["subunidades"] as? Number)?.toInt()!!
                                    vm.registrarProduccion(
                                        insumoId = insumoSeleccionado,
                                        porcionesObtenidas = totalUnidades.toDouble(),
                                        tandasPreparadas = 1.0,
                                        sobranteAnterior = 0.0,
                                        onResult = { if (it) onDismiss() }
                                    )
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = when (modo) {
                                "peso" -> MaterialTheme.colorScheme.tertiary
                                "presentacion" -> MaterialTheme.colorScheme.secondary
                                else -> MaterialTheme.colorScheme.primary
                            }
                        ),
                        enabled = insumoSeleccionado.isNotBlank() &&
                            ((modo == "producir" && (porcionesText.toIntOrNull() ?: 0) > 0) ||
                             (modo == "peso" && (compraPesoText.toDoubleOrNull() ?: 0.0) > 0 && (porcionPesoText.toDoubleOrNull() ?: 0.0) > 0) ||
                             (modo == "presentacion" && presentacionSeleccionada != null && (cantidadPresentacionText.toIntOrNull() ?: 0) > 0))
                    ) {
                        Icon(
                            when (modo) {
                                "producir" -> Icons.Default.AddCircle
                                "peso" -> Icons.Default.MonitorWeight
                                "presentacion" -> Icons.Default.Inventory2
                                else -> Icons.Default.ShoppingCartCheckout
                            },
                            null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            when (modo) {
                                "producir" -> "REGISTRAR PRODUCCIÓN"
                                "presentacion" -> "REGISTRAR COMPRA POR PRESENTACIÓN"
                                else -> "REGISTRAR COMPRA"
                            },
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }

                    TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                        Text("CANCELAR", color = MaterialTheme.colorScheme.onSurface.copy(0.4f), fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    }
                }
            }
        }
    }
}


