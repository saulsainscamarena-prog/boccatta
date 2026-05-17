package com.bocatta.pos.data.repository

import com.bocatta.pos.domain.model.GrupoConfiguracionGlobal
import com.bocatta.pos.domain.repository.IConfigGlobalRepository
import com.bocatta.pos.network.firebase.FirebaseFirestoreProvider
import com.google.firebase.firestore.ListenerRegistration
import timber.log.Timber
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class ConfigGlobalRepository : IConfigGlobalRepository {

    private val db = FirebaseFirestoreProvider.db
    private val collection = db.collection("v2_grupos_configuracion")

    private val _cache = mutableListOf<GrupoConfiguracionGlobal>()
    private var listener: ListenerRegistration? = null

    override suspend fun getAll(): List<GrupoConfiguracionGlobal> = suspendCoroutine { cont ->
        listener?.remove()
        listener = collection.addSnapshotListener { snap, error ->
            if (error != null) {
                Timber.e(error, "Error grupos configuración")
                return@addSnapshotListener
            }
            _cache.clear()
            snap?.documents?.forEach { doc ->
                doc.toObject(GrupoConfiguracionGlobal::class.java)?.let {
                    _cache.add(it.copy(id = doc.id))
                }
            }
            cont.resume(_cache.toList())
        }
    }

    override suspend fun guardar(grupo: GrupoConfiguracionGlobal): Boolean = suspendCoroutine { cont ->
        val doc = if (grupo.id.isBlank()) collection.document() else collection.document(grupo.id)
        doc.set(grupo.copy(id = doc.id))
            .addOnSuccessListener { cont.resume(true) }
            .addOnFailureListener { Timber.e(it, "Error guardar grupo config"); cont.resume(false) }
    }

    override suspend fun eliminar(id: String): Boolean = suspendCoroutine { cont ->
        collection.document(id).delete()
            .addOnSuccessListener { cont.resume(true) }
            .addOnFailureListener { cont.resume(false) }
    }
}

