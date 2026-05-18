package com.bocatta.pos.data.repository

import com.bocatta.pos.domain.model.GrupoConfiguracionGlobal
import com.bocatta.pos.domain.repository.IConfigGlobalRepository
import com.bocatta.pos.network.firebase.FirebaseFirestoreProvider
import kotlinx.coroutines.tasks.await
import timber.log.Timber

class ConfigGlobalRepository : IConfigGlobalRepository {

    private val db = FirebaseFirestoreProvider.db
    private val collection = db.collection("v2_grupos_configuracion")

    override suspend fun getAll(): List<GrupoConfiguracionGlobal> {
        return try {
            val snap = collection.get().await()
            snap.documents.mapNotNull { doc ->
                doc.toObject(GrupoConfiguracionGlobal::class.java)?.copy(id = doc.id)
            }
        } catch (e: Exception) {
            Timber.e(e, "Error getting grupos configuración")
            emptyList()
        }
    }

    override suspend fun guardar(grupo: GrupoConfiguracionGlobal): Boolean {
        return try {
            val doc = if (grupo.id.isBlank()) collection.document() else collection.document(grupo.id)
            doc.set(grupo.copy(id = doc.id)).await()
            true
        } catch (e: Exception) {
            Timber.e(e, "Error guardar grupo config")
            false
        }
    }

    override suspend fun eliminar(id: String): Boolean {
        return try {
            collection.document(id).delete().await()
            true
        } catch (e: Exception) {
            Timber.e(e, "Error eliminar grupo config")
            false
        }
    }
}

