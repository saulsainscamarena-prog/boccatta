package com.bocatta.pos.data.repository

import com.bocatta.pos.domain.model.ComboProducto
import com.bocatta.pos.domain.model.ItemCombo
import com.bocatta.pos.domain.model.TipoCombo
import com.bocatta.pos.network.firebase.FirebaseFirestoreProvider
import com.google.firebase.firestore.ListenerRegistration
import timber.log.Timber
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class ComboRepository {

    private val db = FirebaseFirestoreProvider.db
    private val collection = db.collection("v2_combos")

    private val _cache = mutableListOf<ComboProducto>()
    private var listener: ListenerRegistration? = null

    suspend fun getAll(): List<ComboProducto> = suspendCoroutine { cont ->
        listener?.remove()
        listener = collection.addSnapshotListener { snap, error ->
            if (error != null) {
                Timber.e(error, "Error loading combos")
                cont.resume(emptyList())
                return@addSnapshotListener
            }
            _cache.clear()
            snap?.documents?.forEach { doc ->
                doc.toObject(ComboProducto::class.java)?.let {
                    _cache.add(it.copy(id = doc.id))
                }
            }
            cont.resume(_cache.toList())
        }
    }

    fun getCached(): List<ComboProducto> = _cache.toList()

    suspend fun guardar(combo: ComboProducto): Boolean = suspendCoroutine { cont ->
        val doc = if (combo.id.isBlank()) collection.document() else collection.document(combo.id)
        val comboToSave = combo.copy(id = doc.id)
        doc.set(comboToSave)
            .addOnSuccessListener { cont.resume(true) }
            .addOnFailureListener { Timber.e(it, "Error guardar combo"); cont.resume(false) }
    }

    suspend fun eliminar(id: String): Boolean = suspendCoroutine { cont ->
        collection.document(id).delete()
            .addOnSuccessListener { cont.resume(true) }
            .addOnFailureListener { Timber.e(it, "Error eliminar combo"); cont.resume(false) }
    }

    fun limpiarListener() {
        listener?.remove()
        listener = null
    }
}

