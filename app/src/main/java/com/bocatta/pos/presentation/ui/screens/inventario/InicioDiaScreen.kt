package com.bocatta.pos.presentation.ui.screens.inventario

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.sp
import com.bocatta.pos.presentation.viewmodel.InventoryViewModel
import java.util.Calendar

import com.bocatta.pos.presentation.ui.theme.*
import com.bocatta.pos.presentation.ui.components.NeonButton

@Composable
fun InicioDiaScreen(sessionVm: com.bocatta.pos.presentation.viewmodel.SessionViewModel, vm: InventoryViewModel, onFinalizar: () -> Unit, onGestion: (() -> Unit)? = null) {
    var masaAyer by remember { mutableStateOf("0") }
    var postresAyer by remember { mutableStateOf("0") }

    val hora = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    val saludo = when {
        hora < 12 -> "BUENOS DÍAS"
        hora < 18 -> "BUENAS TARDES"
        else -> "BUENAS NOCHES"
    }

    Box(modifier = Modifier.fillMaxSize().background(
        Brush.verticalGradient(listOf(MaterialTheme.colorScheme.background, Color(0xFF10121A)))
    ), contentAlignment = Alignment.Center) {
        Surface(
            modifier = Modifier.padding(24.dp).widthIn(max = 500.dp),
            shape = RoundedCornerShape(28.dp), // Meridian Spec: 28dp
            color = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
            border = BorderStroke(1.dp, Color.White.copy(0.1f))
        ) {
            Column(modifier = Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(saludo, fontWeight = FontWeight.Black, fontSize = 36.sp, color = Color.White, letterSpacing = 2.sp)
                Text(sessionVm.usuario?.nombre?.uppercase() ?: "OPERADOR", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                
                Spacer(Modifier.height(40.dp))
                
                Text("PROTOCOLOS DE APERTURA INDUSTRIAL", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White.copy(0.4f), letterSpacing = 2.sp)
                Spacer(Modifier.height(24.dp))

                OutlinedTextField(
                    value = masaAyer,
                    onValueChange = { masaAyer = it },
                    label = { Text("MASA RESTANTE (KG/MEDIDAS)", fontWeight = FontWeight.Bold) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedTextColor = Color.White,
                        focusedTextColor = Color.White,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = Color.White.copy(0.4f)
                    )
                )
                
                Spacer(Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = postresAyer,
                    onValueChange = { postresAyer = it },
                    label = { Text("POSTRES EN VITRINA (UNIDADES)", fontWeight = FontWeight.Bold) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedTextColor = Color.White,
                        focusedTextColor = Color.White,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = Color.White.copy(0.4f)
                    )
                )
                
                Spacer(Modifier.height(40.dp))
                
                NeonButton(
                    texto = "INICIAR JORNADA OPERATIVA",
                    onClick = { 
                        val conteos = mapOf(
                            "masa_crepa" to (masaAyer.toDoubleOrNull() ?: 0.0),
                            "postres_vitrina" to (postresAyer.toDoubleOrNull() ?: 0.0)
                        )
                        vm.registrarAperturaDiaria(conteos, sessionVm.uid ?: "", onResult = { exito ->
                            if (exito) onFinalizar()
                        })
                    },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary
                )

                if (sessionVm.esAdmin && onGestion != null) {
                    Spacer(Modifier.height(16.dp))
                    TextButton(
                        onClick = onGestion,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("IGNORAR PROTOCOLO (ACCESO ADMIN)", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 1.sp)
                    }
                }
            }
        }
    }
}

