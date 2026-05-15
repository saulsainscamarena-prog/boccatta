package com.bocatta.pos.domain.repository

import com.bocatta.pos.domain.model.SolicitudTurnoExtra

interface ISolicitudTurnoRepository {
    suspend fun getAll(): List<SolicitudTurnoExtra>
    suspend fun guardar(solicitud: SolicitudTurnoExtra): Boolean
    suspend fun responder(id: String, estado: String, respondidoPor: String): Boolean
}
