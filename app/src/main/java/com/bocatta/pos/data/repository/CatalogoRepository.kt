package com.bocatta.pos.data.repository

import com.bocatta.pos.domain.model.OpcionCatalogo
import com.bocatta.pos.domain.repository.ICatalogoRepository
import com.bocatta.pos.network.firebase.FirebaseFirestoreProvider
import com.google.firebase.firestore.ListenerRegistration
import timber.log.Timber
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class CatalogoRepository : ICatalogoRepository {

    private val db = FirebaseFirestoreProvider.db
    private val collection = db.collection("v2_catalogo_opciones")

    private val _cache = mutableListOf<OpcionCatalogo>()
    private var listener: ListenerRegistration? = null

    override fun getOpciones(tipo: String): List<OpcionCatalogo> {
        return _cache.filter { it.tipo.name == tipo && it.activo }
    }

    override suspend fun getAll(): List<OpcionCatalogo> = suspendCoroutine { cont ->
        listener?.remove()
        listener = collection.addSnapshotListener { snap, error ->
            if (error != null) { Timber.e(error, "Error catálogo"); cont.resume(emptyList()); return@addSnapshotListener }
            _cache.clear()
            snap?.documents?.forEach { doc ->
                doc.toObject(OpcionCatalogo::class.java)?.let {
                    _cache.add(it.copy(id = doc.id))
                }
            }
            cont.resume(_cache.toList())
        }
    }

    override suspend fun guardar(opcion: OpcionCatalogo): Boolean = suspendCoroutine { cont ->
        val doc = if (opcion.id.isBlank()) collection.document() else collection.document(opcion.id)
        doc.set(opcion.copy(id = doc.id))
            .addOnSuccessListener { cont.resume(true) }
            .addOnFailureListener { Timber.e(it, "Error guardar catálogo"); cont.resume(false) }
    }

    override suspend fun eliminar(id: String): Boolean = suspendCoroutine { cont ->
        collection.document(id).delete()
            .addOnSuccessListener { cont.resume(true) }
            .addOnFailureListener { cont.resume(false) }
    }
}

