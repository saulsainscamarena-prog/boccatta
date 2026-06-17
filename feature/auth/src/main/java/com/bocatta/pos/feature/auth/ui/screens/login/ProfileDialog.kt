package com.bocatta.pos.feature.auth.ui.screens.login

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.bocatta.pos.feature.auth.viewmodel.AuthViewModelV2
import org.koin.androidx.compose.koinViewModel

@Composable
fun ProfileDialog(
    onDismiss: () -> Unit,
    vm: AuthViewModelV2 = koinViewModel()
) {
    var oldPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var confirmNewPin by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cambiar NIP", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Ingresa tu NIP actual y el nuevo para cambiarlo.", style = MaterialTheme.typography.bodyMedium)

                OutlinedTextField(
                    value = oldPin,
                    onValueChange = { 
                        localError = null
                        if (it.length <= 4 && it.all { char -> char.isDigit() }) oldPin = it 
                    },
                    label = { Text("NIP actual (4 dígitos)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = newPin,
                    onValueChange = { 
                        localError = null
                        if (it.length <= 4 && it.all { char -> char.isDigit() }) newPin = it 
                    },
                    label = { Text("Nuevo NIP (4 dígitos)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = confirmNewPin,
                    onValueChange = { 
                        localError = null
                        if (it.length <= 4 && it.all { char -> char.isDigit() }) confirmNewPin = it 
                    },
                    label = { Text("Confirmar nuevo NIP") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )

                if (localError != null || vm.mensajeError != null) {
                    Text(
                        text = localError ?: vm.mensajeError ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    when {
                        oldPin.length != 4 -> localError = "El NIP actual debe ser de 4 dígitos"
                        newPin.length != 4 -> localError = "El nuevo NIP debe ser de 4 dígitos"
                        newPin != confirmNewPin -> localError = "Los nuevos NIP no coinciden"
                        else -> {
                            vm.changeNip(oldPin, newPin) {
                                onDismiss()
                            }
                        }
                    }
                },
                enabled = !vm.cargando
            ) {
                if (vm.cargando) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Guardar")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !vm.cargando) {
                Text("Cancelar")
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}
