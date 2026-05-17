package com.bocatta.pos.presentation.ui.screens.operacion

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bocatta.pos.domain.model.TurnoCajaV2
import com.bocatta.pos.presentation.viewmodel.SessionViewModel

@Composable
fun TurnoActivoScreen(
    turno: TurnoCajaV2,
    sessionVm: SessionViewModel,
    onEntrarAVentas: () -> Unit,
    onAdministrarTienda: () -> Unit,
    onLogout: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.padding(24.dp).widthIn(max = 500.dp),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(0.2f))
        ) {
            Column(Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    if (sessionVm.esAdmin) Icons.Default.AdminPanelSettings else Icons.Default.Person,
                    null, modifier = Modifier.size(72.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(16.dp))
                Text("TURNO ACTIVO", fontWeight = FontWeight.Black, fontSize = 24.sp, color = MaterialTheme.colorScheme.onSurface, letterSpacing = 2.sp)

                Spacer(Modifier.height(24.dp))

                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(0.3f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(turno.sucursal.uppercase(), fontWeight = FontWeight.Black, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Person, "Usuario", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurface.copy(0.6f))
                            Spacer(Modifier.width(6.dp))
                            Text("Abierto por: ${turno.usuarioResponsable}", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.7f))
                        }
                        Spacer(Modifier.height(4.dp))
                        Text("Rol: ${sessionVm.rol.name}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
                        Spacer(Modifier.height(8.dp))
                        Text("Fondo inicial: $${"%.2f".format(turno.fondoInicial)}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }

                // Participantes actuales (mockup)
                Spacer(Modifier.height(16.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(0.3f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text("Participantes:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
                        Text("• ${turno.usuarioResponsable} (${if (sessionVm.esAdmin) "Admin" else "Operador"})", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.7f))
                        Text("• Tú (${sessionVm.usuario?.nombre?.uppercase() ?: sessionVm.rol.name})", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                    }
                }

                Spacer(Modifier.height(24.dp))

                Text("¿Qué deseas hacer?", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.5f), letterSpacing = 1.sp)
                Spacer(Modifier.height(16.dp))

                Button(
                    onClick = onEntrarAVentas,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(
                        if (sessionVm.esAdmin) Icons.Default.ShoppingCart else Icons.Default.GroupAdd,
                        null, modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (sessionVm.esAdmin) "ENTRAR A VENTAS" else "UNIRME AL TURNO",
                        fontWeight = FontWeight.Black, letterSpacing = 1.sp
                    )
                }

                if (sessionVm.esAdmin) {
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = onAdministrarTienda,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.AdminPanelSettings, "Administrar", modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("ADMINISTRAR TIENDA", fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    }
                }

                Spacer(Modifier.height(24.dp))
                TextButton(onClick = onLogout) {
                    Text("CERRAR SESIÓN", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}


