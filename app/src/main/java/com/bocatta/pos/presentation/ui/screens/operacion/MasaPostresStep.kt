package com.bocatta.pos.presentation.ui.screens.operacion

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MasaPostresStep(
    sessionVm: com.bocatta.pos.presentation.viewmodel.SessionViewModel,
    vm: com.bocatta.pos.presentation.viewmodel.InventoryViewModel,
    onNext: () -> Unit
) {
    val opcionesMasa = listOf("1 (entera)", "1/2 (mitad)", "1/4 (cuarto)", "Baja (<250ml)", "Sin masa")
    var masaSeleccionada by remember { mutableStateOf("") }
    var hayPostres by remember { mutableStateOf(false) }
    val postresDisponibles = listOf("Tiramisú", "Carlota")
    var postresSeleccionados = remember { mutableStateListOf<String>() }
    val datosCompletos = masaSeleccionada.isNotBlank() && (!hayPostres || postresSeleccionados.isNotEmpty())

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("INSUMOS DEL DÍA", fontWeight = FontWeight.Black, style = MaterialTheme.typography.headlineSmall, color = Color.White)
        Text("Registra el estado actual de insumos.", color = Color.White.copy(0.6f), fontSize = 14.sp)

        Text("MASA RESTANTE", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White.copy(0.6f), letterSpacing = 1.sp)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            opcionesMasa.forEach { op ->
                FilterChip(
                    selected = masaSeleccionada == op,
                    onClick = { masaSeleccionada = op },
                    label = { Text(op, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
        }

        Text("¿HAY POSTRES EN VITRINA?", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White.copy(0.6f), letterSpacing = 1.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = !hayPostres,
                onClick = { hayPostres = false; postresSeleccionados.clear() },
                label = { Text("NO") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    selectedLabelColor = MaterialTheme.colorScheme.onSurface
                )
            )
            FilterChip(
                selected = hayPostres,
                onClick = { hayPostres = true },
                label = { Text("SÍ") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
        if (hayPostres) {
            postresDisponibles.forEach { postre ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = postre in postresSeleccionados,
                        onCheckedChange = { if (it) postresSeleccionados.add(postre) else postresSeleccionados.remove(postre) },
                        colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                    )
                    Text(postre, fontSize = 14.sp, color = Color.White)
                }
            }
            Text("* Fresas/Duraznos se preparan al pedido", fontSize = 10.sp, color = Color.White.copy(0.4f))
        }

        Spacer(Modifier.weight(1f))
        Button(
            onClick = {
                val masaVal = when (masaSeleccionada) {
                    "1 (entera)" -> 100.0
                    "1/2 (mitad)" -> 50.0
                    "1/4 (cuarto)" -> 25.0
                    "Baja (<250ml)" -> 10.0
                    else -> 0.0
                }
                val conteos = mutableMapOf("masa_crepa" to masaVal, "postres_vitrina" to (if (hayPostres) postresSeleccionados.size.toDouble() else 0.0))
                postresSeleccionados.forEachIndexed { i, p -> conteos["postre_${p.lowercase()}"] = 1.0 }
                vm.registrarAperturaDiaria(conteos, sessionVm.uid ?: "", onResult = { if (it) onNext() })
            },
            enabled = datosCompletos,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text("CONTINUAR", fontWeight = FontWeight.Black, letterSpacing = 1.sp)
        }
    }
}
