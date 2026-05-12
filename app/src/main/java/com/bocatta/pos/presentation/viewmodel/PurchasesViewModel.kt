package com.bocatta.pos.presentation.viewmodel

import androidx.compose.runtime.*
import androidx.lifecycle.viewModelScope
import com.bocatta.pos.domain.model.*
import com.bocatta.pos.network.firebase.FirebaseFirestoreProvider
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.bocatta.pos.data.repository.InventoryRepository
import timber.log.Timber
import com.bocatta.pos.core.constants.FirestoreCollections

class PurchasesViewModel(private val repository: InventoryRepository = InventoryRepository()) : BaseViewModel() {
    private val db = FirebaseFirestoreProvider.db

    var insumos = mutableStateListOf<InsumoV2>()
        private set
    var proveedores = mutableStateListOf<ProveedorV2>()
        private set

    private var listenerInsumos: com.google.firebase.firestore.ListenerRegistration? = null
    private var listenerProveedores: com.google.firebase.firestore.ListenerRegistration? = null

    init {
        escucharInsumos()
        escucharProveedores()
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

    private fun escucharProveedores() {
        listenerProveedores?.remove()
        listenerProveedores = db.collection(FirestoreCollections.SUPPLIERS).addSnapshotListener { snap, _ ->
            if (snap != null) {
                proveedores.clear()
                snap.documents.forEach { doc ->
                    doc.toObject(ProveedorV2::class.java)?.let { proveedores.add(it.copy(id = doc.id)) }
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        listenerInsumos?.remove()
        listenerProveedores?.remove()
    }

    fun registrarCompra(
        insumoId: String,
        proveedorId: String,
        cantidadCompra: Double, // ej: 5 (botes)
        precioTotal: Double,
        sucursal: String = "Bodega Central"
    ) {
        viewModelScope.launch {
            cargando = true
            try {
                val insumo = insumos.find { it.id == insumoId } ?: return@launch
                val precioUnitario = precioTotal / cantidadCompra
                val cantidadUsoTotal = cantidadCompra // ya calculada en UI
                Timber.tag("PURCHASE").i("Registrando compra de ${insumo.nombre} (id=$insumoId) cantidad=$cantidadCompra")
                val exito = repository.registrarCompra(
                    insumoId = insumoId,
                    proveedorId = proveedorId,
                    cantidadCompra = cantidadCompra,
                    precioUnitario = precioUnitario,
                    precioTotal = precioTotal,
                    cantidadUsoTotal = cantidadUsoTotal,
                    sucursal = sucursal
                )
                if (exito) {
                    mensajeExito = "Compra de ${insumo.nombre} registrada correctamente ✓"
                    Timber.tag("PURCHASE").i("Compra registrada con Ñxito")
                }
            } catch (e: Exception) {
                mensajeError = "Error: ${e.message}"
                Timber.tag("PURCHASE").e(e, "Error al registrar compra")
            } finally {
                cargando = false
            }
        }
    }

    fun agregarProveedor(nombre: String, tel: String, cat: String) {
        viewModelScope.launch {
            try {
                val id = db.collection(FirestoreCollections.SUPPLIERS).document().id
                val p = ProveedorV2(id = id, nombre = nombre, telefono = tel, categoria = cat)
                db.collection(FirestoreCollections.SUPPLIERS).document(id).set(p).await()
                mensajeExito = "Proveedor registrado ?"
                Timber.tag("SUPPLIER").i("Proveedor $nombre agregado con id $id")
            } catch (e: Exception) {
                mensajeError = "Error: ${e.message}"
            }
        }
    }
}


