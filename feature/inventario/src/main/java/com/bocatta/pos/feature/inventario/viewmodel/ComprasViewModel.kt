package com.bocatta.pos.feature.inventario.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bocatta.pos.core.constants.FirestoreCollections
import com.bocatta.pos.domain.model.RegistroCompraV2
import com.bocatta.pos.network.firebase.FirebaseFirestoreProvider
import com.google.firebase.firestore.Query
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import timber.log.Timber

class ComprasViewModel : ViewModel() {
    private val db = FirebaseFirestoreProvider.db
    var compras = mutableStateListOf<RegistroCompraV2>()
        private set
    var cargando: Boolean by mutableStateOf(true)
        private set

    init {
        cargarHistorial()
    }

    fun cargarHistorial() {
        viewModelScope.launch {
            try {
                cargando = true
                val snap = db.collection(FirestoreCollections.COMPRAS)
                    .orderBy("fecha", Query.Direction.DESCENDING)
                    .limit(50)
                    .get()
                    .await()
                compras.clear()
                snap.documents.forEach { doc ->
                    compras.add(
                        RegistroCompraV2(
                            id = doc.id,
                            insumoId = doc.getString("insumoId") ?: "",
                            cantidadComprada = doc.getDouble("cantidadComprada") ?: 0.0,
                            precioUnitarioCompra = doc.getDouble("precioUnitarioCompra") ?: 0.0,
                            precioTotal = doc.getDouble("precioTotal") ?: 0.0,
                            proveedorId = doc.getString("proveedorId") ?: "",
                            fecha = doc.getLong("fecha") ?: 0L,
                            sucursalRecibe = doc.getString("sucursalRecibe") ?: ""
                        )
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Error cargando historial de compras")
            } finally {
                cargando = false
            }
        }
    }
}

