package com.bocatta.pos.data.repository

import com.bocatta.pos.domain.model.OpcionCatalogo
import com.bocatta.pos.domain.repository.ICatalogoRepository
import com.bocatta.pos.core.constants.FirestoreCollections
import com.bocatta.pos.network.firebase.FirebaseFirestoreProvider
import kotlinx.coroutines.tasks.await
import timber.log.Timber

class CatalogoRepository : ICatalogoRepository {

    private val db = FirebaseFirestoreProvider.db
    private val collection = db.collection(FirestoreCollections.CATALOGO_OPCIONES)

    private val _cache = mutableListOf<OpcionCatalogo>()

    override fun getOpciones(tipo: String): List<OpcionCatalogo> {
        return _cache.filter { it.tipo.name == tipo && it.activo }
    }

    override suspend fun getAll(): List<OpcionCatalogo> {
        return try {
            val snap = collection.get().await()
            _cache.clear()
            snap.documents.forEach { doc ->
                doc.toObject(OpcionCatalogo::class.java)?.let {
                    _cache.add(it.copy(id = doc.id))
                }
            }
            _cache.toList()
        } catch (e: Exception) {
            Timber.e(e, "Error getting catálogo")
            emptyList()
        }
    }

    override suspend fun guardar(opcion: OpcionCatalogo): Boolean {
        return try {
            val doc = if (opcion.id.isBlank()) collection.document() else collection.document(opcion.id)
            doc.set(opcion.copy(id = doc.id)).await()
            true
        } catch (e: Exception) {
            Timber.e(e, "Error guardar catálogo")
            false
        }
    }

    override suspend fun eliminar(id: String): Boolean {
        return try {
            collection.document(id).delete().await()
            true
        } catch (e: Exception) {
            Timber.e(e, "Error eliminar catálogo")
            false
        }
    }
}

