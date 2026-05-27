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
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bocatta.pos.domain.model.RegistroJornada
import com.bocatta.pos.presentation.viewmodel.CajaViewModel
import com.bocatta.pos.presentation.viewmodel.SessionViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    val cargandoTurno = cajaVm.cargandoTurno
    val locale = LocalLocale.current.platformLocale
    val dateFormat = remember(locale) { SimpleDateFormat("dd/MM/yyyy", locale) }
    val hayTurno = turno != null
    val participantesVisibles = if (sessionVm.esAdmin) participantes else participantes.filter { it.rol != "ADMIN" }
    val errorTurno = cajaVm.errorTurno?.let { error ->
        when {
            error.contains("FAILED_PRECONDITION", ignoreCase = true) ||
                error.contains("index", ignoreCase = true) ->
                "No se pudo leer el turno en tiempo real. Toca iniciar o unirte; la app intentara recuperar el turno."
            else -> error
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).safeDrawingPadding(),
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
                    if (hayTurno) Icons.Default.AccountCircle else Icons.Default.Schedule,
                    null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "TURNOS",
                    fontWeight = FontWeight.Black,
                    fontSize = 24.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    letterSpacing = 2.sp
                )
                Text(
                    "${sessionVm.sucursalActual.uppercase(Locale.ROOT)} - ${dateFormat.format(Date())}",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(0.5f)
                )

                Spacer(Modifier.height(24.dp))

                if (sessionVm.esAdmin) {
                    Text(
                        sessionVm.usuario?.nombre?.uppercase(Locale.ROOT) ?: "ADMIN",
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(8.dp))
                }

                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(if (hayTurno) 0.15f else 0.05f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        when {
                            cargandoTurno -> {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(32.dp),
                                    strokeWidth = 3.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.height(8.dp))
                                Text("CARGANDO TURNO", fontWeight = FontWeight.Black, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                                Text("Validando la jornada abierta.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.4f))
                            }
                            hayTurno -> {
                                val turnoActivo = turno
                                Text("TURNO ACTIVO", fontWeight = FontWeight.Black, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.height(6.dp))
                                Text("Abierto por: ${turnoActivo.usuarioResponsable}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.7f))
                                Text("Fondo: $${"%.2f".format(turnoActivo.fondoInicial)}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.height(8.dp))
                                Text("Participantes:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
                                participantesVisibles.forEach { p ->
                                    Text("- ${p.usuario} (${p.rol})", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.7f))
                                }
                                if (participantesVisibles.none { it.usuario == sessionVm.usuario?.nombre }) {
                                    Text("- Tu (${sessionVm.rol.name})", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                            else -> {
                                Icon(Icons.Default.Schedule, "Horario", modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.onSurface.copy(0.3f))
                                Spacer(Modifier.height(8.dp))
                                Text("NO HAY TURNO ACTIVO", fontWeight = FontWeight.Black, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
                                Text("Inicia la jornada para comenzar a operar.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.4f))
                            }
                        }
                    }
                }

                Spacer(Modifier.height(32.dp))

                if (jornadaActiva) {
                    Surface(color = MaterialTheme.colorScheme.tertiary.copy(0.1f), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, "Verificado", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.tertiary)
                            Spacer(Modifier.width(8.dp))
                            Text("Jornada en curso", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.tertiary)
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }

                errorTurno?.let { error ->
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer.copy(0.35f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(error, fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }

                Button(
                    onClick = if (hayTurno) onUnirseTurno else onIniciarTurno,
                    enabled = !cargandoTurno,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    if (cargandoTurno) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("VALIDANDO", fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                    } else {
                        Icon(if (hayTurno) Icons.Default.GroupAdd else Icons.Default.PlayArrow, if (hayTurno) "Unirse a turno" else "Iniciar turno", modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(if (hayTurno) "UNIRSE AL TURNO" else "INICIAR TURNO", fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                    }
                }

                if (hayTurno) {
                    Text("Inicia tu jornada laboral", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.4f))
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
                    Text("CERRAR SESION", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}
