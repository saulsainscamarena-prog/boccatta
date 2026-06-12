package com.bocatta.pos.feature.admin.ui.screens.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.bocatta.pos.domain.model.EstadoSolicitudTurno
import com.bocatta.pos.feature.admin.viewmodel.SolicitudViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TabNotificaciones(vm: SolicitudViewModel) {
    val solicitudes = vm.solicitudes
    val dateFormat = remember { SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("NOTIFICACIONES", fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Spacer(Modifier.height(8.dp))

        if (solicitudes.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.NotificationsNone, "Notificaciones", Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurface.copy(0.2f))
                    Spacer(Modifier.height(8.dp))
                    Text("Sin notificaciones pendientes", color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(solicitudes.toList(), key = { it.id }) { sol ->
                    ElevatedCard(shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth(), colors = CardDefaults.elevatedCardColors(
                        containerColor = if (sol.estado == EstadoSolicitudTurno.PENDIENTE) MaterialTheme.colorScheme.errorContainer.copy(0.3f) else MaterialTheme.colorScheme.surface
                    )) {
                        Column(Modifier.padding(14.dp)) {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Warning, "Advertencia", modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.error)
                                Spacer(Modifier.width(8.dp))
                                Text(sol.empleadoNombre, fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.weight(1f))
                                Text(sol.estado.name, fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                            }
                            Spacer(Modifier.height(4.dp))
                            Text("Motivo: ${sol.motivo}", fontSize = 13.sp)
                            Text("Horario: ${sol.horarioProgramado}", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                            Text(dateFormat.format(Date(sol.horaSolicitada)), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.5f))

                            if (sol.estado == EstadoSolicitudTurno.PENDIENTE) {
                                Spacer(Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(onClick = { vm.responderSolicitud(sol.id, EstadoSolicitudTurno.APROBADA, "Admin") }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) { Text("APROBAR", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                                    OutlinedButton(onClick = { vm.responderSolicitud(sol.id, EstadoSolicitudTurno.RECHAZADA, "Admin") }) { Text("RECHAZAR", fontSize = 12.sp) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


