package com.bocatta.pos.feature.inventario.viewmodel

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bocatta.pos.core.ui.viewmodel.BaseViewModel
import com.bocatta.pos.domain.model.*
import com.bocatta.pos.network.firebase.FirebaseFirestoreProvider
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.bocatta.pos.core.constants.FirestoreCollections

class InventoryAdjustmentViewModelV2 : BaseViewModel() {
    private val db = FirebaseFirestoreProvider.db

    var insumos = mutableStateListOf<InsumoV2>()
        private set
    var mermasRecientes = mutableStateListOf<MermaV2>()
        private set

    private var listenerInsumos: com.google.firebase.firestore.ListenerRegistration? = null
    private var listenerMermas: com.google.firebase.firestore.ListenerRegistration? = null

    init {
        load()
    }

    private fun load() {
        launchIO {
            escucharInsumos()
            escucharMermas()
        }
    }

    private fun escucharInsumos() {
        listenerInsumos?.remove()
        listenerInsumos = db.collection(FirestoreCollections.INSUMOS).addSnapshotListener { snap, _ ->
            if (snap != null) {
                insumos.clear()
                snap.documents.forEach { doc ->
                    doc.toObject(InsumoV2::class.java)?.let { insumos.add(it.copy(id = doc.id)) }
                }
            }
        }
    }

    private fun escucharMermas() {
        listenerMermas?.remove()
        listenerMermas = db.collection(FirestoreCollections.MERMA_LOGS).orderBy("fecha", com.google.firebase.firestore.Query.Direction.DESCENDING).limit(20)
            .addSnapshotListener { snap, _ ->
                if (snap != null) {
                    mermasRecientes.clear()
                    snap.documents.forEach { doc ->
                        doc.toObject(MermaV2::class.java)?.let { mermasRecientes.add(it) }
                    }
                }
            }
    }

    override fun onCleared() {
        super.onCleared()
        listenerInsumos?.remove()
        listenerMermas?.remove()
    }

    fun registrarMerma(insumoId: String, cantidad: Double, motivo: String, comentario: String, usuarioId: String, sucursal: String) {
        viewModelScope.launch {
            cargando = true
            try {
                val batch = db.batch()
                val id = db.collection(FirestoreCollections.MERMA_LOGS).document().id
                
                val merma = MermaV2(
                    id = id,
                    insumoId = insumoId,
                    cantidad = cantidad,
                    motivo = motivo,
                    comentario = comentario,
                    fecha = System.currentTimeMillis(),
                    usuarioId = usuarioId,
                    sucursal = sucursal.lowercase(java.util.Locale.getDefault())
                )

                // 1. Guardar log de merma
                batch.set(db.collection(FirestoreCollections.MERMA_LOGS).document(id), merma)

                // 2. Descontar de Stock de Sucursal V2
                val stockRef = db.collection(FirestoreCollections.INVENTARIO_SUCURSAL)
                    .document("${sucursal.lowercase(java.util.Locale.getDefault())}_$insumoId")
                batch.set(
                    stockRef,
                    mapOf("cantidadEnBase" to FieldValue.increment(-cantidad), "ultimaActualizacion" to System.currentTimeMillis()),
                    com.google.firebase.firestore.SetOptions.merge()
                )

                batch.commit().await()
            } catch (e: Exception) {
                mensajeError = "Error: ${e.message}"
            } finally {
                cargando = false
            }
        }
    }
}





