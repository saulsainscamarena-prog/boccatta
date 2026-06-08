package com.bocatta.pos.presentation.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bocatta.pos.core.constants.FirestoreCollections
import com.bocatta.pos.network.firebase.FirebaseFirestoreProvider
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import timber.log.Timber

class SyncInventarioViewModel : ViewModel() {
    private val db = FirebaseFirestoreProvider.db
    var insumosGlobal = mutableStateListOf<String>()
        private set
    var sucursales = mutableStateListOf<String>()
        private set
    var stockGlobal = mutableStateMapOf<String, Double>()
        private set
    var cargando by mutableStateOf(true)
        private set

    init {
        cargarGlobal()
    }

    fun cargarGlobal() {
        viewModelScope.launch {
            try {
                cargando = true
                val snap = db.collection(FirestoreCollections.INVENTARIO_GLOBAL).get().await()
                val branchSnap = db.collection(FirestoreCollections.SUCURSALES).get().await()

                insumosGlobal.clear()
                stockGlobal.clear()
                sucursales.clear()

                snap.documents.forEach { doc ->
                    insumosGlobal.add(doc.id)
                    stockGlobal[doc.id] = doc.getDouble("cantidadEnBase")
                        ?: doc.getDouble("cantidadDisponible")
                        ?: 0.0
                }

                branchSnap.documents.forEach { doc ->
                    sucursales.add(doc.getString("nombre") ?: doc.id)
                }
            } catch (e: Exception) {
                Timber.e(e, "Error cargando inventario global")
            } finally {
                cargando = false
            }
        }
    }

    fun syncToBranch(insumoId: String, sucursalId: String, cantidad: Double) {
        viewModelScope.launch {
            try {
                val sucursalNormalizada = sucursalId.lowercase(java.util.Locale.getDefault()).trim()
                val batch = db.batch()
                val branchRef = db.collection(FirestoreCollections.INVENTARIO_SUCURSAL)
                    .document("${sucursalNormalizada}_$insumoId")
                batch.set(
                    branchRef,
                    mapOf("cantidadEnBase" to FieldValue.increment(cantidad)),
                    SetOptions.merge()
                )

                val globalRef = db.collection(FirestoreCollections.INVENTARIO_GLOBAL).document(insumoId)
                batch.set(
                    globalRef,
                    mapOf("cantidadEnBase" to FieldValue.increment(-cantidad)),
                    SetOptions.merge()
                )

                batch.commit().await()
                stockGlobal[insumoId] = (stockGlobal[insumoId] ?: 0.0) - cantidad
            } catch (e: Exception) {
                Timber.e(e, "Error sincronizando inventario a sucursal")
            }
        }
    }
}
