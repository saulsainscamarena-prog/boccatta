package com.bocatta.pos.feature.ventas.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bocatta.pos.domain.model.SalesInventoryProductV2
import kotlin.math.roundToInt

/**
 * Dialog para capturar el gramaje de un producto que se vende por peso.
 *
 * - Muestra el nombre del producto y precio por kg.
 * - Actualiza el precio estimado en tiempo real mientras el operador escribe.
 * - onConfirm devuelve los gramos ingresados para que el VM construya el ItemCarritoV2.
 */
@Composable
fun DialogPesoProducto(
    producto: SalesInventoryProductV2,
    sucursal: String,
    onDismiss: () -> Unit,
    onConfirmar: (gramos: Double) -> Unit
) {
    val precioKg = producto.precioVenta[sucursal.lowercase(java.util.Locale.getDefault())] ?: 0.0
    var gramosTexto by remember { mutableStateOf("") }
    val gramos = gramosTexto.toDoubleOrNull() ?: 0.0
    val precioEstimado = if (gramos > 0) precioKg * (gramos / 1000.0) else 0.0
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    producto.emoji + " " + producto.nombre,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Text(
                    "Precio: $${String.format(java.util.Locale.getDefault(), "%.2f", precioKg)} / kg",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = gramosTexto,
                    onValueChange = { nuevo ->
                        // Solo acepta números y un punto decimal
                        if (nuevo.all { it.isDigit() || it == '.' } && nuevo.count { it == '.' } <= 1) {
                            gramosTexto = nuevo
                        }
                    },
                    label = { Text("Gramos a vender") },
                    suffix = { Text("g") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            keyboardController?.hide()
                            if (gramos > 0) {
                                onConfirmar(gramos)
                                onDismiss()
                            }
                        }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                // Precio estimado en tiempo real
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(10.dp)
                        )
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(color = MaterialTheme.colorScheme.onPrimaryContainer, fontSize = 13.sp)) {
                                append(if (gramos > 0) "${gramos.roundToInt()}g = " else "Ingresa los gramos")
                            }
                            if (gramos > 0) {
                                withStyle(
                                    SpanStyle(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 20.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    append("$${String.format(java.util.Locale.getDefault(), "%.2f", precioEstimado)}")
                                }
                            }
                        }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    keyboardController?.hide()
                    onConfirmar(gramos)
                    onDismiss()
                },
                enabled = gramos > 0,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("AGREGAR AL CARRITO", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}


