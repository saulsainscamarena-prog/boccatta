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
import com.bocatta.pos.domain.model.RegistroJornada
import com.bocatta.pos.presentation.viewmodel.SessionViewModel
import com.bocatta.pos.presentation.viewmodel.CajaViewModel
import com.bocatta.pos.presentation.viewmodel.InventoryViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TurnosScreen(
    sessionVm: SessionViewModel,
    cajaVm: CajaViewModel,
    inventarioVm: com.bocatta.pos.presentation.viewmodel.InventoryViewModel,
    participantes: List<RegistroJornada>,
    jornadaActiva: Boolean = false,
    onIniciarTurno: () -> Unit,
    onUnirseTurno: () -> Unit,
    onAdministrarTienda: () -> Unit,
    onLogout: () -> Unit
) {
    val turno = cajaVm.turnoActivo
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    val hayTurno = turno != null

    // Filtrar participantes según rol
    val participantesVisibles = if (sessionVm.esAdmin) participantes
    else participantes.filter { it.rol != "ADMIN" }

    Box(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.padding(24.dp).widthIn(max = 500.dp),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(0.2f))
        ) {
            Column(Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                // Header
                Icon(
                    if (hayTurno) Icons.Default.AccountCircle else Icons.Default.Schedule,
                    null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(12.dp))
                Text("TURNOS", fontWeight = FontWeight.Black, fontSize = 24.sp, color = MaterialTheme.colorScheme.onSurface, letterSpacing = 2.sp)
                Text(
                    "${sessionVm.sucursalActual.uppercase()} · ${dateFormat.format(Date())}",
                    fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.5f)
                )

                Spacer(Modifier.height(24.dp))

                if (sessionVm.esAdmin) {
                    Text(sessionVm.usuario?.nombre?.uppercase() ?: "ADMIN", fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(8.dp))
                }

                // Status card
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(if (hayTurno) 0.15f else 0.05f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        if (hayTurno) {
                            Text("TURNO ACTIVO", fontWeight = FontWeight.Black, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.height(6.dp))
                            Text("Abierto por: ${turno!!.usuarioResponsable}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.7f))
                            Text("Fondo: $${"%.2f".format(turno.fondoInicial)}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.height(8.dp))
                            Text("Participantes:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
                            participantesVisibles.forEach { p ->
                                Text("• ${p.usuario} (${p.rol})", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.7f))
                            }
                            if (participantesVisibles.none { it.usuario == sessionVm.usuario?.nombre }) {
                                Text("• Tú (${sessionVm.rol.name})", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                            }
                        } else {
                            Icon(Icons.Default.Schedule, null, modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.onSurface.copy(0.3f))
                            Spacer(Modifier.height(8.dp))
                            Text("NO HAY TURNO ACTIVO", fontWeight = FontWeight.Black, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
                            Text("Inicia la jornada para comenzar a operar.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.4f))
                        }
                    }
                }

                Spacer(Modifier.height(32.dp))

                if (jornadaActiva) {
                    Surface(color = MaterialTheme.colorScheme.tertiary.copy(0.1f), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.tertiary)
                            Spacer(Modifier.width(8.dp))
                            Text("Jornada en curso", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.tertiary)
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }

                // Botón principal
                Button(
                    onClick = if (hayTurno) onUnirseTurno else onIniciarTurno,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(if (hayTurno) Icons.Default.GroupAdd else Icons.Default.PlayArrow, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (hayTurno) "UNIRSE AL TURNO" else "INICIAR TURNO",
                        fontWeight = FontWeight.Black, letterSpacing = 1.sp
                    )
                }
                if (hayTurno) {
                    Text("Inicia tu jornada laboral", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.4f))
                }

                // Admin button
                if (sessionVm.esAdmin) {
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = onAdministrarTienda,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.AdminPanelSettings, null, modifier = Modifier.size(20.dp))
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
