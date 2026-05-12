package com.bocatta.pos.presentation.ui.screens.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bocatta.pos.BuildConfig
import com.bocatta.pos.R
import com.bocatta.pos.presentation.ui.theme.*
import com.bocatta.pos.presentation.viewmodel.AuthViewModelV2

@Composable
fun LoginScreen(vm: AuthViewModelV2, onLoginExitoso: (uid: String) -> Unit) {
    LoginScreen(
        onLoginClick = { email, pass -> vm.intentarLogin(email, pass, onLoginExitoso) },
        onRegisterClick = { e, p, n, c, r -> vm.intentarRegistro(e, p, n, c, r, onLoginExitoso) },
        onType = { vm.limpiarError() },
        isLoading = vm.cargando,
        errorMensaje = vm.mensajeError
    )
}

@Composable
fun LoginScreen(
    onLoginClick: (String, String) -> Unit,
    onRegisterClick: (String, String, String, String, String) -> Unit,
    onType: () -> Unit,
    isLoading: Boolean,
    errorMensaje: String? = null
) {
    var user by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var isRegisterMode by remember { mutableStateOf(false) }
    var masterCode by remember { mutableStateOf("") }
    var nombreCompleto by remember { mutableStateOf("") }

    val backgroundBrush = Brush.verticalGradient(listOf(BocattaSecondary, BocattaBgDark))
    
    Box(
        modifier = Modifier.fillMaxSize().background(backgroundBrush).safeDrawingPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(0.9f).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                modifier = Modifier.size(120.dp), 
                shape = CircleShape, 
                color = MaterialTheme.colorScheme.onPrimary,
                shadowElevation = 16.dp
            ) {
                Image(
                    painter = painterResource(id = R.drawable.logo_bocatta), 
                    contentDescription = "Logo Bocatta", 
                    modifier = Modifier.padding(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(horizontal = 32.dp, vertical = 40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        if (isRegisterMode) "NUEVO ACCESO" else "SISTEMA INDUSTRIAL", 
                        style = MaterialTheme.typography.headlineSmall, 
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "Bocatta POS V2 \u00B7 Operaci\u00F3n Cr\u00EDtica", 
                        color = MaterialTheme.colorScheme.outline, 
                        style = MaterialTheme.typography.labelMedium
                    )
                    
                    Spacer(Modifier.height(32.dp))

                    if (errorMensaje != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer, 
                            shape = RoundedCornerShape(16.dp), 
                            modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Error, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(errorMensaje, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (isRegisterMode) {
                        OutlinedTextField(
                            value = masterCode, 
                            onValueChange = { masterCode = it; onType() }, 
                            label = { Text("C\u00F3digo Maestro") }, 
                            modifier = Modifier.fillMaxWidth(), 
                            shape = RoundedCornerShape(16.dp), 
                            visualTransformation = PasswordVisualTransformation(),
                            leadingIcon = { Icon(Icons.Default.VpnKey, null, tint = BocattaPrimary) }
                        )
                        Spacer(Modifier.height(16.dp))
                        OutlinedTextField(
                            value = nombreCompleto, 
                            onValueChange = { nombreCompleto = it; onType() }, 
                            label = { Text("Nombre de Operador") }, 
                            modifier = Modifier.fillMaxWidth(), 
                            shape = RoundedCornerShape(16.dp),
                            leadingIcon = { Icon(Icons.Default.Person, null, tint = BocattaPrimary) }
                        )
                        Spacer(Modifier.height(16.dp))
                    }

                    OutlinedTextField(
                        value = user, 
                        onValueChange = { user = it; onType() }, 
                        label = { Text("ID de Usuario / Email") }, 
                        modifier = Modifier.fillMaxWidth(), 
                        shape = RoundedCornerShape(16.dp),
                        leadingIcon = { Icon(Icons.Default.Email, null, tint = BocattaPrimary) }
                    )
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = pass, 
                        onValueChange = { pass = it; onType() }, 
                        label = { Text("Clave de Acceso / PIN") }, 
                        modifier = Modifier.fillMaxWidth(), 
                        shape = RoundedCornerShape(16.dp), 
                        visualTransformation = PasswordVisualTransformation(),
                        leadingIcon = { Icon(Icons.Default.Lock, null, tint = BocattaPrimary) }
                    )

                    Spacer(Modifier.height(32.dp))

                    Button(
                        onClick = { if (isRegisterMode) onRegisterClick(user, pass, nombreCompleto, masterCode, "") else onLoginClick(user, pass) },
                        modifier = Modifier.fillMaxWidth().height(64.dp),
                        shape = RoundedCornerShape(20.dp),
                        enabled = !isLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = BocattaPrimary)
                    ) {
                        if (isLoading) CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
                        else Text(
                            if (isRegisterMode) "ACTIVAR ACCESO" else "ENTRAR AL SISTEMA", 
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }

            TextButton(
                onClick = { isRegisterMode = !isRegisterMode }, 
                modifier = Modifier.padding(top = 24.dp)
            ) {
                Text(
                    if (isRegisterMode) "\u00BFYa tienes cuenta? Inicia sesi\u00F3n" else "\u00BFSolicitar nuevo acceso al administrador?", 
                    color = Color.White.copy(0.7f),
                    style = MaterialTheme.typography.labelLarge
                )
            }

            // Bot�n para modo demo/testing
            if (BuildConfig.DEMO_MODE_ENABLED) {
                TextButton(
                    onClick = { onLoginClick(BuildConfig.DEMO_EMAIL, BuildConfig.DEMO_PASSWORD) }, 
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text(
                        "MODO DEMO (Admin)", 
                        color = Color.White.copy(0.5f),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}

