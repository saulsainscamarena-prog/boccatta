package com.bocatta.pos.presentation.ui.screens.inventario

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bocatta.pos.presentation.ui.theme.*
import com.bocatta.pos.presentation.viewmodel.InventoryViewModel
import com.bocatta.pos.presentation.viewmodel.SessionViewModel
import java.util.Calendar

@Composable
fun InicioDiaScreen(
    sessionVm: SessionViewModel,
    vm: InventoryViewModel,
    onFinalizar: () -> Unit,
    onGestion: (() -> Unit)? = null
) {
    // Masa restante
    val opcionesMasa = listOf("1 (entera)", "1/2 (mitad)", "1/4 (cuarto)", "Baja (<250ml)", "Sin masa")
    var masaSeleccionada by remember { mutableStateOf("") }

    // Postres en vitrina
    var hayPostres by remember { mutableStateOf(false) }
    val postresDisponibles = listOf("Tiramisú", "Carlota")
    var postresSeleccionados = remember { mutableStateListOf<String>() }

    val hora = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    val saludo = when {
        hora < 12 -> "BUENOS DÍAS"
        hora < 18 -> "BUENAS TARDES"
        else -> "BUENAS NOCHES"
    }

    val datosCompletos = masaSeleccionada.isNotBlank() && (!hayPostres || postresSeleccionados.isNotEmpty())

    Box(
        modifier = Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(MaterialTheme.colorScheme.background, Color(0xFF10121A)))
        ), contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.padding(24.dp).widthIn(max = 500.dp),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            border = BorderStroke(1.dp, Color.White.copy(0.1f))
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(saludo, fontWeight = FontWeight.Black, fontSize = 36.sp, color = Color.White, letterSpacing = 2.sp)
                Text(
                    sessionVm.usuario?.nombre?.uppercase() ?: "OPERADOR",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )

                Spacer(Modifier.height(32.dp))

                Text("PROTOCOLOS DE APERTURA", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White.copy(0.4f), letterSpacing = 2.sp)
                Spacer(Modifier.height(20.dp))

                // ── MASA RESTANTE ──
                Text("MASA RESTANTE", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White.copy(0.6f), letterSpacing = 1.sp)
                Spacer(Modifier.height(8.dp))
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

                Spacer(Modifier.height(24.dp))

                // ── POSTRES EN VITRINA ──
                Text("¿HAY POSTRES EN VITRINA?", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White.copy(0.6f), letterSpacing = 1.sp)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = !hayPostres,
                        onClick = { hayPostres = false; postresSeleccionados.clear() },
                        label = { Text("NO", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            selectedLabelColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    FilterChip(
                        selected = hayPostres,
                        onClick = { hayPostres = true },
                        label = { Text("SÍ", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
                if (hayPostres) {
                    Spacer(Modifier.height(12.dp))
                    Text("¿Cuáles hay?", fontSize = 11.sp, color = Color.White.copy(0.5f))
                    Spacer(Modifier.height(8.dp))
                    postresDisponibles.forEach { postre ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = postre in postresSeleccionados,
                                onCheckedChange = {
                                    if (it) postresSeleccionados.add(postre)
                                    else postresSeleccionados.remove(postre)
                                },
                                colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                            )
                            Text(postre, fontSize = 14.sp, color = Color.White)
                        }
                    }
                    Text("* Fresas/Duraznos se preparan al pedido", fontSize = 10.sp, color = Color.White.copy(0.4f))
                }

                Spacer(Modifier.height(32.dp))

                // ── INICIAR JORNADA ──
                Button(
                    onClick = {
                        val masaVal = when (masaSeleccionada) {
                            "1 (entera)" -> 100.0
                            "1/2 (mitad)" -> 50.0
                            "1/4 (cuarto)" -> 25.0
                            "Baja (<250ml)" -> 10.0
                            else -> 0.0
                        }
                        val conteos = mutableMapOf(
                            "masa_crepa" to masaVal,
                            "postres_vitrina" to (if (hayPostres) postresSeleccionados.size.toDouble() else 0.0)
                        )
                        postresSeleccionados.forEachIndexed { i, p ->
                            conteos["postre_${p.lowercase()}"] = 1.0
                        }
                        vm.registrarAperturaDiaria(conteos, sessionVm.uid ?: "", onResult = { exito ->
                            if (exito) onFinalizar()
                        })
                    },
                    enabled = datosCompletos,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("INICIAR JORNADA", fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                }

                if (sessionVm.esAdmin && onGestion != null) {
                    Spacer(Modifier.height(16.dp))
                    OutlinedButton(
                        onClick = onGestion,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.AdminPanelSettings, "Administrar", modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("ADMINISTRAR TIENDA", fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                    }
                }
            }
        }
    }
}


