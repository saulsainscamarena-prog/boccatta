package com.bocatta.pos.presentation.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bocatta.pos.data.repository.InventoryDeductions
import com.bocatta.pos.domain.model.SalesInventoryProductV2
import com.bocatta.pos.presentation.viewmodel.SalesViewModelV2
import com.bocatta.pos.presentation.ui.theme.BocattaWarning

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun CrepeBuilderDialog(
    producto: SalesInventoryProductV2,
    vmV2: SalesViewModelV2,
    sucursal: String,
    onDismiss: () -> Unit,
    onAddToCart: (SalesInventoryProductV2, String?, String?, List<String>, Boolean) -> Unit
) {
    var baseSeleccionada by remember { mutableStateOf<String?>(null) }
    var aderezosSeleccionados by remember { mutableStateOf<List<String>>(emptyList()) }
    var toppingsSeleccionados by remember { mutableStateOf<List<String>>(emptyList()) }
    var presetsSeleccionados by remember { mutableStateOf<List<String>>(emptyList()) }
    var esSeparado by remember { mutableStateOf(false) }

    val categoria = producto.categoria.uppercase()
    val esSalada = categoria.contains("SALADA") || producto.id.contains("2s") || producto.nombre.contains("Salada", true)
    val basesDulces = listOf("Nutella", "Lechera", "Zarzamora", "Mermelada Fresa", "Philadelphia")
    val toppingsDulces = listOf("Durazno", "Fresa Natural", "Coco Rayado", "Granillo Chocolate", "Granillo Colores", "Oreo", "Bombon", "Nuez")
    val presetsSalados = mapOf(
        "Pepperoni" to listOf("Pepperoni"),
        "Hawaiana" to listOf("Jamon", "Pina"),
        "Jamon con Philadelphia" to listOf("Jamon")
    )
    val basesSaladas = listOf("Salsa Tomate", "Philadelphia")
    val aderezosSalados = listOf("BBQ", "Buffalo", "Blue Cheese", "Valentina", "Queso Amarillo", "Catsup", "Mayonesa")

    val precioBase = producto.precioVenta[sucursal.lowercase()] ?: 0.0
    var precioCalculado by remember { mutableStateOf(precioBase) }

    val maxPresets = when {
        producto.id.contains("combo", true) -> 2
        producto.id.contains("2s") -> 2
        producto.nombre.contains("2 ", true) -> 2
        else -> 1
    }

    LaunchedEffect(toppingsSeleccionados, baseSeleccionada, presetsSeleccionados) {
        precioCalculado = if (esSalada) {
            precioBase
        } else {
            InventoryDeductions.calcularPrecioCrepa(precioBase, baseSeleccionada, toppingsSeleccionados, producto.costoToppingExtra)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Arma tu crepa", fontWeight = FontWeight.Black, fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurface)
                Text(producto.nombre.uppercase(), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (esSalada) {
                    Text("ESPECIFICACIONES (${presetsSeleccionados.size}/$maxPresets)", fontWeight = FontWeight.Black, fontSize = 15.sp, letterSpacing = 1.sp)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(horizontal = 4.dp)) {
                        items(presetsSalados.keys.toList()) { preset ->
                            FilterChip(
                                selected = presetsSeleccionados.contains(preset),
                                onClick = {
                                    if (presetsSeleccionados.contains(preset)) {
                                        presetsSeleccionados = presetsSeleccionados - preset
                                    } else if (presetsSeleccionados.size < maxPresets) {
                                        presetsSeleccionados = presetsSeleccionados + preset
                                        // Auto-seleccionar base si es el primero
                                        if (baseSeleccionada == null) {
                                            baseSeleccionada = if (preset == "Pepperoni" || preset == "Hawaiana") "Salsa Tomate" else "Philadelphia"
                                        }
                                        // Combinar toppings
                                        val nuevosToppings = (toppingsSeleccionados + (presetsSalados[preset] ?: emptyList())).distinct()
                                        toppingsSeleccionados = nuevosToppings
                                    }
                                },
                                label = { Text(preset, fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                    Text("BASE", fontWeight = FontWeight.Black, fontSize = 15.sp, letterSpacing = 1.sp)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(horizontal = 4.dp)) {
                        items(basesSaladas) { base ->
                            FilterChip(
                                selected = baseSeleccionada == base,
                                onClick = { baseSeleccionada = if (baseSeleccionada == base) null else base },
                                label = { Text(base, fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                    Text("ADEREZOS (${aderezosSeleccionados.size}/3)", fontWeight = FontWeight.Black, fontSize = 15.sp, letterSpacing = 1.sp)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(horizontal = 4.dp)) {
                        items(aderezosSalados) { aderezo ->
                            FilterChip(
                                selected = aderezosSeleccionados.contains(aderezo),
                                onClick = {
                                    if (aderezosSeleccionados.contains(aderezo)) {
                                        aderezosSeleccionados = aderezosSeleccionados - aderezo
                                    } else if (aderezosSeleccionados.size < 3) {
                                        aderezosSeleccionados = aderezosSeleccionados + aderezo
                                    }
                                },
                                label = { Text(aderezo, fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                } else {
                    Text("BASE", fontWeight = FontWeight.Black, fontSize = 15.sp, letterSpacing = 1.sp)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(horizontal = 4.dp)) {
                        items(basesDulces) { base ->
                            FilterChip(
                                selected = baseSeleccionada == base,
                                onClick = { baseSeleccionada = if (baseSeleccionada == base) null else base },
                                label = { Text(base, fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                                shape = RoundedCornerShape(8.dp),
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primaryContainer)
                            )
                        }
                    }
                    Text("TOPPINGS", fontWeight = FontWeight.Black, fontSize = 15.sp, letterSpacing = 1.sp)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        toppingsDulces.forEach { topping ->
                            val isSelected = toppingsSeleccionados.contains(topping)
                            val isPremium = InventoryDeductions.esPremium(topping)
                            Surface(
                                onClick = {
                                    toppingsSeleccionados = if (isSelected) toppingsSeleccionados - topping else toppingsSeleccionados + topping
                                },
                                modifier = Modifier.fillMaxWidth(),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(0.3f) else MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(12.dp),
                                border = if (isSelected) BorderStroke(2.dp, BocattaPrimary) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = { checked ->
                                            toppingsSeleccionados = if (checked) toppingsSeleccionados + topping else toppingsSeleccionados - topping
                                        },
                                        colors = CheckboxDefaults.colors(checkedColor = BocattaPrimary)
                                    )
                                    Text(topping, fontSize = 14.sp, fontWeight = if(isSelected) FontWeight.Bold else FontWeight.Normal)
                                    if (isPremium) {
                                        Spacer(Modifier.width(8.dp))
                                        StatusBadgePremium("PREMIUM", BocattaWarning)
                                    }
                                }
                            }
                        }
                    }
                }

                if (producto.esCombo) {
                    Row(
                        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant.copy(0.3f), RoundedCornerShape(12.dp)).padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("SERVIR SEPARADAS", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Switch(checked = esSeparado, onCheckedChange = { esSeparado = it }, colors = SwitchDefaults.colors(checkedThumbColor = BocattaPrimary))
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.Black,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("TOTAL A PAGAR", fontSize = 10.sp, color = Color.White.copy(0.6f), fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                        Text("$${"%.2f".format(precioCalculado)}", fontSize = 32.sp, fontWeight = FontWeight.Black, color = Color.White)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onAddToCart(producto, baseSeleccionada, aderezosSeleccionados.joinToString(", ").ifBlank { null }, toppingsSeleccionados, esSeparado)
                    onDismiss()
                },
                enabled = baseSeleccionada != null && (!esSalada || presetsSeleccionados.size == maxPresets),
                shape = RoundedCornerShape(14.dp), // Meridian Spec
                colors = ButtonDefaults.buttonColors(containerColor = BocattaPrimary),
                modifier = Modifier.height(52.dp).fillMaxWidth(0.6f)
            ) { Text("AÑADIR AL PEDIDO", fontWeight = FontWeight.Black) }
        },
        dismissButton = { 
            TextButton(onClick = onDismiss) { 
                Text("CANCELAR", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold) 
            } 
        },
        shape = RoundedCornerShape(24.dp) // Meridian Spec: 24dp for dialogs
    )
}

