package com.bocatta.pos.domain.repository

import com.bocatta.pos.domain.model.RegistroJornada

interface IRegistroJornadaRepository {
    suspend fun guardar(registro: RegistroJornada): Boolean
    suspend fun getParticipantes(sucursal: String): List<RegistroJornada>
    suspend fun getHistorial(usuario: String): List<RegistroJornada>
}

