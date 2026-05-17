package com.bocatta.pos.presentation.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.viewModelScope
import com.bocatta.pos.domain.model.EstadoJornada
import com.bocatta.pos.domain.model.HorarioEmpleado
import com.bocatta.pos.domain.model.JornadaLaboral
import com.bocatta.pos.domain.model.PausaJornada
import com.bocatta.pos.data.repository.HorarioRepository
import com.bocatta.pos.network.firebase.FirebaseFirestoreProvider
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class HorarioViewModel(
    private val repository: HorarioRepository = HorarioRepository()
) : BaseViewModel() {

    var horarios = mutableStateListOf<HorarioEmpleado>()
        private set
    var jornadaActiva by mutableStateOf<JornadaLaboral?>(null)
        private set

    fun cargarHorarios(empleadoId: String) {
        viewModelScope.launch {
            cargando = true
            try {
                horarios.clear()
                horarios.addAll(repository.getHorarios(empleadoId))
            } catch (e: Exception) { mensajeError = "Error: ${e.message}" }
            cargando = false
        }
    }

    fun guardarHorario(horario: HorarioEmpleado) {
        viewModelScope.launch {
            cargando = true
            try {
                if (repository.guardarHorario(horario)) {
                    cargarHorarios(horario.empleadoId)
                    mensajeExito = "Horario guardado"
                }
            } catch (e: Exception) { mensajeError = "Error: ${e.message}" }
            cargando = false
        }
    }

    fun iniciarJornada(empleadoId: String, empleadoNombre: String, horarioId: String, horasProgramadas: Double, dispositivo: String) {
        viewModelScope.launch {
            cargando = true
            try {
                val jornada = JornadaLaboral(
                    empleadoId = empleadoId,
                    empleadoNombre = empleadoNombre,
                    dia = System.currentTimeMillis(),
                    horarioId = horarioId,
                    inicioReal = System.currentTimeMillis(),
                    estado = EstadoJornada.ACTIVA,
                    horasProgramadas = horasProgramadas,
                    dispositivo = dispositivo
                )
                if (repository.iniciarJornada(jornada)) {
                    cargarJornadaActiva(empleadoId)
                    mensajeExito = "Jornada iniciada"
                }
            } catch (e: Exception) { mensajeError = "Error: ${e.message}" }
            cargando = false
        }
    }

    fun cargarJornadaActiva(empleadoId: String) {
        viewModelScope.launch {
            try {
                jornadaActiva = repository.getJornadaActiva(empleadoId)
            } catch (e: Exception) { mensajeError = "Error: ${e.message}" }
        }
    }

    fun pausarJornada(motivo: String) {
        val activa = jornadaActiva ?: return
        viewModelScope.launch {
            try {
                val pausa = PausaJornada(inicio = System.currentTimeMillis(), motivo = motivo)
                val nuevasPausas = activa.pausas + pausa
                val db = com.bocatta.pos.network.firebase.FirebaseFirestoreProvider.db
                db.collection("v2_jornadas").document(activa.id)
                    .update("pausas", nuevasPausas, "estado", "EN_PAUSA").await()
                cargarJornadaActiva(activa.empleadoId)
            } catch (e: Exception) { mensajeError = "Error: ${e.message}" }
        }
    }

    fun reanudarJornada() {
        val activa = jornadaActiva ?: return
        if (activa.pausas.isEmpty()) return
        viewModelScope.launch {
            try {
                val ultimaPausa = activa.pausas.last()
                val pausasActualizadas = activa.pausas.dropLast(1) + ultimaPausa.copy(fin = System.currentTimeMillis())
                val db = com.bocatta.pos.network.firebase.FirebaseFirestoreProvider.db
                db.collection("v2_jornadas").document(activa.id)
                    .update("pausas", pausasActualizadas, "estado", "ACTIVA").await()
                cargarJornadaActiva(activa.empleadoId)
            } catch (e: Exception) { mensajeError = "Error: ${e.message}" }
        }
    }

    fun cerrarJornada() {
        val activa = jornadaActiva ?: return
        viewModelScope.launch {
            cargando = true
            try {
                val ahora = System.currentTimeMillis()
                val totalPausa = activa.pausas.sumOf { p ->
                    val fin = p.fin ?: ahora
                    fin - p.inicio
                }
                val horasEfectivas = ((ahora - activa.inicioReal - totalPausa) / 3600000.0)
                if (repository.cerrarJornada(activa.id, ahora, horasEfectivas)) {
                    jornadaActiva = null
                    mensajeExito = "Jornada cerrada"
                }
            } catch (e: Exception) { mensajeError = "Error: ${e.message}" }
            cargando = false
        }
    }
}

