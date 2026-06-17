package com.bocatta.pos.feature.auth.ui.screens.login

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.bocatta.pos.feature.auth.viewmodel.AuthViewModelV2

@Composable
fun SetNipScreen(vm: AuthViewModelV2, onNipSet: () -> Unit) {
    var step by remember { mutableStateOf(1) } // 1 = Create, 2 = Confirm
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (step == 1) "Configurar NIP" else "Confirmar NIP",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (step == 1) "Crea un NIP de 4 dígitos para autorizar acciones." else "Vuelve a ingresar el NIP para confirmar.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            val currentPin = if (step == 1) pin else confirmPin

            OutlinedTextField(
                value = currentPin,
                onValueChange = { 
                    localError = null
                    if (it.length <= 4 && it.all { char -> char.isDigit() }) {
                        if (step == 1) pin = it else confirmPin = it 
                    }
                },
                label = { Text("NIP (4 dígitos)", fontWeight = FontWeight.Bold) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                visualTransformation = PasswordVisualTransformation(),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )

            if (localError != null || vm.mensajeError != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = localError ?: vm.mensajeError ?: "",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    if (step == 1) {
                        if (pin.length == 4) {
                            step = 2
                        } else {
                            localError = "El NIP debe ser de 4 dígitos"
                        }
                    } else {
                        if (confirmPin.length == 4) {
                            if (pin == confirmPin) {
                                vm.setupInitialNip(pin, onNipSet)
                            } else {
                                localError = "Los NIP no coinciden"
                                confirmPin = ""
                            }
                        } else {
                            localError = "El NIP debe ser de 4 dígitos"
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = !vm.cargando && ((step == 1 && pin.length == 4) || (step == 2 && confirmPin.length == 4))
            ) {
                if (vm.cargando) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text(if (step == 1) "Siguiente" else "Guardar NIP", fontWeight = FontWeight.Bold)
                }
            }

            if (step == 2 && !vm.cargando) {
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(onClick = { 
                    step = 1
                    confirmPin = ""
                    localError = null
                }) {
                    Text("Volver", color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}
