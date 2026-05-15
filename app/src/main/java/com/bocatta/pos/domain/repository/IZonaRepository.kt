package com.bocatta.pos.domain.repository

import com.bocatta.pos.domain.model.Zona

interface IZonaRepository {
    suspend fun getAll(): List<Zona>
    suspend fun guardar(zona: Zona): Boolean
    suspend fun eliminar(id: String): Boolean
}
