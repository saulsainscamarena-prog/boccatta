package com.bocatta.pos.presentation.ui.components

import androidx.compose.foundation.background
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

    // Actualización automática de porciones según el insumo y tandas
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
            shape = RoundedCornerShape(24.dp), // Meridian Spec: 24dp
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("REGISTRO DE PRODUCCIÓN", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                Text("MÓDULO DE TRANSFORMACIÓN DE MATERIA PRIMA", fontSize = 10.sp, color = BocattaPrimary, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)

                Text("INSUMO A PRODUCIR", fontWeight = FontWeight.Black, fontSize = 12.sp, letterSpacing = 1.sp)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(insumos) { id ->
                        FilterChip(
                            selected = insumoId == id,
                            onClick = { insumoId = id },
                            label = { Text(id.replace("_", " ").uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }

                OutlinedTextField(
                    value = numeroDeTandas,
                    onValueChange = { numeroDeTandas = it },
                    label = { Text("NÚMERO DE TANDAS (LOTES)", fontWeight = FontWeight.Bold) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BocattaPrimary, cursorColor = BocattaPrimary)
                )

                OutlinedTextField(
                    value = porcionesObtenidas,
                    onValueChange = { porcionesObtenidas = it },
                    label = { Text("PORCIONES RESULTANTES", fontWeight = FontWeight.Bold) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp),
                    supportingText = { Text("Rendimiento operativo estimado", fontSize = 10.sp) },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BocattaPrimary)
                )

                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(0.2f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, BocattaPrimary.copy(0.2f))
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, null, tint = BocattaPrimary, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "Esta acción descontará automáticamente los ingredientes de la Bodega Central (Global) y cargará $porcionesObtenidas unidades a tu inventario en ${sucursal.uppercase()}.",
                            fontSize = 11.sp,
                            lineHeight = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Button(
                    onClick = {
                        val mp = numeroDeTandas.toDoubleOrNull() ?: 0.0
                        val po = porcionesObtenidas.toDoubleOrNull() ?: 0.0
                        vm.registrarProduccion(
                            insumoId = insumoId,
                            porcionesObtenidas = po,
                            materiaPrimaUsadaG = mp, 
                            sobranteAnterior = 0.0,
                            onResult = { if (it) onDismiss() }
                        )
                    },
                    modifier = Modifier.fillMaxWidth().height(60.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Black) // Industrial aesthetic
                ) {
                    Icon(Icons.Default.PrecisionManufacturing, null, tint = Color.White)
                    Spacer(Modifier.width(12.dp))
                    Text("EJECUTAR PRODUCCIÓN", fontWeight = FontWeight.Black, fontSize = 16.sp, letterSpacing = 1.sp)
                }
                
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("CANCELAR OPERACIÓN", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
