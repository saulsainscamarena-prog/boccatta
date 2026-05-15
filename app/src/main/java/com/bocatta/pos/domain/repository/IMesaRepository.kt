package com.bocatta.pos.domain.repository

import com.bocatta.pos.domain.model.Mesa

interface IMesaRepository {
    suspend fun getAll(): List<Mesa>
    suspend fun getByZona(zonaId: String): List<Mesa>
    suspend fun guardar(mesa: Mesa): Boolean
    suspend fun eliminar(id: String): Boolean
    suspend fun actualizarEstado(id: String, estado: String): Boolean
}
