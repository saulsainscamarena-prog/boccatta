package com.bocatta.pos.feature.ventas.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bocatta.pos.domain.model.MetodoPago
import com.bocatta.pos.domain.model.SplitParte
import com.bocatta.pos.domain.model.calcularSplit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SplitPaymentDialog(
    total: Double,
    initialParts: List<SplitParte>,
    onConfirm: (List<SplitParte>) -> Unit,
    onDismiss: () -> Unit
) {
    var personasText by remember { mutableStateOf(initialParts.size.coerceAtLeast(2).toString()) }
    var partes by remember(initialParts) {
        mutableStateOf(
            if (initialParts.isNotEmpty()) initialParts
            else calcularSplit(total, 2, MetodoPago.EFECTIVO)
        )
    }

    val personas = personasText.toIntOrNull()?.coerceIn(2, 20) ?: 2

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("DIVIDIR CUENTA", fontWeight = FontWeight.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = personasText,
                    onValueChange = {
                        personasText = it.filter { c -> c.isDigit() }
                        val n = it.toIntOrNull()
                        if (n != null && n in 2..20) {
                            partes = calcularSplit(total, n, MetodoPago.EFECTIVO)
                        }
                    },
                    label = { Text("Número de personas") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Text("$personas personas — Total: $${"%.2f".format(total)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)

                if (personas > 1 && personas <= 20) {
                    partes.forEachIndexed { index, parte ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Persona ${index + 1}:", modifier = Modifier.weight(1f), fontSize = 14.sp)
                            Text("$${"%.2f".format(parte.monto)}", fontWeight = FontWeight.Black, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            var expanded by remember { mutableStateOf(false) }
                            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                                OutlinedTextField(
                                    value = parte.metodoPago.valor,
                                    onValueChange = {},
                                    readOnly = true,
                                    modifier = Modifier.width(110.dp).menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                                    textStyle = MaterialTheme.typography.labelSmall,
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                                    shape = RoundedCornerShape(8.dp)
                                )
                                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                    MetodoPago.entries.forEach { mp ->
                                        DropdownMenuItem(
                                            text = { Text(mp.valor) },
                                            onClick = {
                                                partes = partes.toMutableList().also { it[index] = parte.copy(metodoPago = mp) }
                                                expanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(partes) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) { Text("CONFIRMAR DIVISIÓN", fontWeight = FontWeight.Black) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        },
        shape = RoundedCornerShape(24.dp)
    )
}


