package com.bocatta.pos.presentation.ui.screens.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    onFinished: () -> Unit
) {
    var step by remember { mutableStateOf(1) }
    var nombreNegocio by remember { mutableStateOf("") }
    var giroNegocio by remember { mutableStateOf("RESTAURANT") }

    Scaffold(contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(title = { Text("Bienvenido a Bocatta POS") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            when (step) {
                1 -> {
                    Text("Paso 1: Nombre de tu Negocio", style = MaterialTheme.typography.headlineSmall)
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = nombreNegocio,
                        onValueChange = { nombreNegocio = it },
                        label = { Text("Nombre del Negocio") }
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { step = 2 },
                        enabled = nombreNegocio.isNotBlank()
                    ) {
                        Text("Siguiente")
                    }
                }
                2 -> {
                    Text("Paso 2: ¿A qué te dedicas?", style = MaterialTheme.typography.headlineSmall)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    val giros = listOf(
                        "RESTAURANT" to "Restaurante / Comida Rápida",
                        "RETAIL" to "Tienda / Abarrotes",
                        "SERVICES" to "Servicios / Barbería"
                    )
                    
                    giros.forEach { (codigo, descripcion) ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = giroNegocio == codigo,
                                onClick = { giroNegocio = codigo }
                            )
                            Text(descripcion)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = { step = 3 }) {
                        Text("Siguiente")
                    }
                }
                3 -> {
                    Text("Paso 3: Creando tu entorno...", style = MaterialTheme.typography.headlineSmall)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Estamos configurando tu negocio ($nombreNegocio) con el giro $giroNegocio.")
                    Spacer(modifier = Modifier.height(24.dp))
                    CircularProgressIndicator()
                    
                    // TODO: Call Repository here
                    LaunchedEffect(Unit) {
                        // Simulate delay for creation
                        kotlinx.coroutines.delay(2000)
                        onFinished()
                    }
                }
            }
        }
    }
}
