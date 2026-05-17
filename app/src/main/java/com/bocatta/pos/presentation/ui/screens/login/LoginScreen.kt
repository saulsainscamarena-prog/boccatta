package com.bocatta.pos.presentation.ui.screens.login

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bocatta.pos.R
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
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
    val keyboardController = LocalSoftwareKeyboardController.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val versionName = remember { try { context.packageManager.getPackageInfo(context.packageName, 0).versionName } catch (_: Exception) { "2.6.0" } }

    Box(
        modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(MaterialTheme.colorScheme.background, Color(0xFF10121A)))).safeDrawingPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(0.9f).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                modifier = Modifier.size(120.dp), 
                shape = CircleShape, 
                color = Color.White.copy(0.05f),
                border = BorderStroke(1.dp, Color.White.copy(0.1f))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.logo_bocatta), 
                    contentDescription = "Logo Bocatta", 
                    modifier = Modifier.padding(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(32.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                border = BorderStroke(1.dp, Color.White.copy(0.1f))
            ) {
                Column(modifier = Modifier.padding(horizontal = 32.dp, vertical = 40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        if (isRegisterMode) "NUEVO ACCESO" else "BOCATTA", 
                        style = MaterialTheme.typography.headlineSmall, 
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        letterSpacing = 2.sp
                    )
                    Text(
                        "BOCATTA POS \u00B7 MERIDIAN DESIGN", 
                        color = MaterialTheme.colorScheme.primary, 
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    
                    Spacer(Modifier.height(32.dp))

                    if (errorMensaje != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.error.copy(0.1f), 
                            shape = RoundedCornerShape(16.dp), 
                            modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(0.3f))
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
                            label = { Text("CÓDIGO DE ACCESO", fontWeight = FontWeight.Bold) }, 
                            modifier = Modifier.fillMaxWidth(), 
                            shape = RoundedCornerShape(16.dp), 
                            visualTransformation = PasswordVisualTransformation(),
                            leadingIcon = { Icon(Icons.Default.VpnKey, null, tint = MaterialTheme.colorScheme.primary) },
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
                            value = nombreCompleto, 
                            onValueChange = { nombreCompleto = it; onType() }, 
                            label = { Text("NOMBRE DE OPERADOR", fontWeight = FontWeight.Bold) }, 
                            modifier = Modifier.fillMaxWidth(), 
                            shape = RoundedCornerShape(16.dp),
                            leadingIcon = { Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.primary) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedTextColor = Color.White,
                                focusedTextColor = Color.White,
                                focusedLabelColor = MaterialTheme.colorScheme.primary,
                                unfocusedLabelColor = Color.White.copy(0.4f)
                            )
                        )
                        Spacer(Modifier.height(16.dp))
                    }

                    OutlinedTextField(
                        value = user, 
                        onValueChange = { user = it; onType() }, 
                        label = { Text("ID DE USUARIO / EMAIL", fontWeight = FontWeight.Bold) }, 
                        modifier = Modifier.fillMaxWidth(), 
                        shape = RoundedCornerShape(16.dp),
                        leadingIcon = { Icon(Icons.Default.Email, null, tint = MaterialTheme.colorScheme.primary) },
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
                        value = pass, 
                        onValueChange = { pass = it; onType() }, 
                        label = { Text("CLAVE DE ACCESO / PIN", fontWeight = FontWeight.Bold) }, 
                        modifier = Modifier.fillMaxWidth(), 
                        shape = RoundedCornerShape(16.dp), 
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            keyboardController?.hide()
                            if (!isLoading) {
                                if (isRegisterMode) onRegisterClick(user, pass, nombreCompleto, masterCode, "")
                                else onLoginClick(user, pass)
                            }
                        }),
                        leadingIcon = { Icon(Icons.Default.Lock, null, tint = MaterialTheme.colorScheme.primary) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedTextColor = Color.White,
                            focusedTextColor = Color.White,
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            unfocusedLabelColor = Color.White.copy(0.4f)
                        )
                    )

                    Spacer(Modifier.height(40.dp))

                    Button(
                        onClick = { if (isRegisterMode) onRegisterClick(user, pass, nombreCompleto, masterCode, "") else onLoginClick(user, pass) },
                        modifier = Modifier.fillMaxWidth().height(64.dp).shadow(16.dp, RoundedCornerShape(20.dp), spotColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(20.dp),
                        enabled = !isLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.background)
                    ) {
                        if (isLoading) CircularProgressIndicator(color = MaterialTheme.colorScheme.background, modifier = Modifier.size(24.dp))
                        else Text(
                            if (isRegisterMode) "ACTIVAR ACCESO" else "ENTRAR AL SISTEMA", 
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (isRegisterMode) "Administrador · Empleado" else "Administrador o Empleado",
                        fontSize = 10.sp,
                        color = Color.White.copy(0.3f),
                        letterSpacing = 0.5.sp
                    )
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

            Spacer(Modifier.height(12.dp))
            Text("v$versionName", fontSize = 9.sp, color = Color.White.copy(0.15f), letterSpacing = 0.5.sp)
        }
    }
}

