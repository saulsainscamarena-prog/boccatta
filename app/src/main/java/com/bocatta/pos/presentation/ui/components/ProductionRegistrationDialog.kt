package com.bocatta.pos.presentation.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.bocatta.pos.presentation.ui.theme.*
import com.bocatta.pos.presentation.viewmodel.InventoryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductionRegistrationDialog(
    vm: InventoryViewModel,
    sucursal: String,
    onDismiss: () -> Unit
) {
    var insumoId by remember { mutableStateOf("masa_crepa") }
    var numeroDeTandas by remember { mutableStateOf("1") }
    var porcionesObtenidas by remember { mutableStateOf("60") }
    
    val insumos = listOf("masa_crepa", "helado_vainilla", "helado_chocolate", "fresas_lavadas")

    LaunchedEffect(insumoId, numeroDeTandas) {
        val tandas = numeroDeTandas.toDoubleOrNull() ?: 0.0
        porcionesObtenidas = when(insumoId) {
            "masa_crepa" -> (tandas * 60).toInt().toString()
            "helado_vainilla", "helado_chocolate" -> (tandas * 50).toInt().toString()
            "fresas_lavadas" -> (tandas * 20).toInt().toString()
            else -> porcionesObtenidas
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = BocattaBgDark,
            border = BorderStroke(1.dp, Color.White.copy(0.1f)),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Header
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = BocattaNeonCyan.copy(0.1f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.PrecisionManufacturing, null, tint = BocattaNeonCyan)
                        }
                    }
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text("MÓDULO INDUSTRIAL V2", fontWeight = FontWeight.Black, fontSize = 10.sp, color = BocattaNeonCyan, letterSpacing = 1.sp)
                        Text("CONTROL DE PRODUCCIÓN", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Color.White)
                    }
                }

                Divider(color = Color.White.copy(0.05f))

                Text("SELECCIONAR PRODUCTO BASE", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White.copy(0.6f))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(insumos) { id ->
                        FilterChip(
                            selected = insumoId == id,
                            onClick = { insumoId = id },
                            label = { Text(id.replace("_", " ").uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Black) },
                            shape = RoundedCornerShape(12.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BocattaNeonCyan,
                                selectedLabelColor = BocattaBgDark,
                                labelColor = Color.White.copy(0.5f)
                            )
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                        value = numeroDeTandas,
                        onValueChange = { numeroDeTandas = it },
                        label = { Text("TANDAS", fontWeight = FontWeight.Bold) },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BocattaNeonCyan,
                            unfocusedBorderColor = Color.White.copy(0.1f),
                            focusedLabelColor = BocattaNeonCyan,
                            unfocusedLabelColor = Color.White.copy(0.4f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    OutlinedTextField(
                        value = porcionesObtenidas,
                        onValueChange = { porcionesObtenidas = it },
                        label = { Text("RESULTADO", fontWeight = FontWeight.Bold) },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BocattaNeonCyan,
                            unfocusedBorderColor = Color.White.copy(0.1f),
                            focusedLabelColor = BocattaNeonCyan,
                            unfocusedLabelColor = Color.White.copy(0.4f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }

                Surface(
                    color = Color.White.copy(0.03f),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color.White.copy(0.05f))
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Analytics, null, tint = BocattaNeonCyan, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "Se descontarán ingredientes de Bodega Central y se cargarán $porcionesObtenidas unidades a ${sucursal.uppercase()}.",
                            fontSize = 11.sp,
                            color = Color.White.copy(0.7f),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    NeonButton(
                        texto = "EJECUTAR PRODUCCIÓN",
                        onClick = {
                            val mp = numeroDeTandas.toDoubleOrNull() ?: 0.0
                            val po = porcionesObtenidas.toDoubleOrNull() ?: 0.0
                            vm.registrarProduccion(
                                insumoId = insumoId,
                                porcionesObtenidas = po,
                                tandasPreparadas = mp, 
                                sobranteAnterior = 0.0,
                                onResult = { if (it) onDismiss() }
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        color = BocattaNeonCyan
                    )
                    
                    TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                        Text("CANCELAR", color = Color.White.copy(0.4f), fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    }
                }
            }
        }
    }
}
