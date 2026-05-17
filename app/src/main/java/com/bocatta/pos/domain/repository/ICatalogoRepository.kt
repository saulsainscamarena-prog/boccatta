package com.bocatta.pos.domain.repository

import com.bocatta.pos.domain.model.OpcionCatalogo

interface ICatalogoRepository {
    fun getOpciones(tipo: String): List<OpcionCatalogo>
    suspend fun getAll(): List<OpcionCatalogo>
    suspend fun guardar(opcion: OpcionCatalogo): Boolean
    suspend fun eliminar(id: String): Boolean
}

