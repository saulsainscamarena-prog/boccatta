package com.bocatta.pos.domain.repository

import com.bocatta.pos.domain.model.GrupoConfiguracionGlobal

interface IConfigGlobalRepository {
    suspend fun getAll(): List<GrupoConfiguracionGlobal>
    suspend fun guardar(grupo: GrupoConfiguracionGlobal): Boolean
    suspend fun eliminar(id: String): Boolean
}

