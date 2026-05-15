package com.bocatta.pos.domain.repository

import com.bocatta.pos.domain.model.HorarioEmpleado
import com.bocatta.pos.domain.model.JornadaLaboral

interface IHorarioRepository {
    suspend fun getHorarios(empleadoId: String): List<HorarioEmpleado>
    suspend fun guardarHorario(horario: HorarioEmpleado): Boolean
    suspend fun iniciarJornada(jornada: JornadaLaboral): Boolean
    suspend fun cerrarJornada(jornadaId: String, fin: Long, horas: Double): Boolean
    suspend fun getJornadaActiva(empleadoId: String): JornadaLaboral?
}
