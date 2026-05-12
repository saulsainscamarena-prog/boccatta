package com.bocatta.pos.presentation.viewmodel

import androidx.compose.runtime.*
import androidx.lifecycle.viewModelScope
import com.bocatta.pos.domain.model.*
import com.bocatta.pos.network.firebase.FirebaseFirestoreProvider
import com.bocatta.pos.core.constants.FirestoreCollections
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MenuViewModel : BaseViewModel() {

    private val db = FirebaseFirestoreProvider.db
    private var listenerProductos: ListenerRegistration? = null
    private var listenerRecetas: ListenerRegistration? = null

    var productos = mutableStateListOf<SalesInventoryProductV2>()
        private set
    var recetas = mutableStateMapOf<String, RecetaV2>()
        private set

    init {
        escucharProductos()
        escucharRecetas()
    }

    private fun escucharProductos() {
        listenerProductos?.remove()
        listenerProductos = db.collection(FirestoreCollections.PRODUCTOS)
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    mensajeError = "Error: ${error.message}"
                    return@addSnapshotListener
                }
                productos.clear()
                snap?.documents?.forEach { doc ->
                    doc.toObject(SalesInventoryProductV2::class.java)?.let { productos.add(it.copy(id = doc.id)) }
                }
            }
    }

    private fun escucharRecetas() {
        listenerRecetas?.remove()
        listenerRecetas = db.collection(FirestoreCollections.RECETAS)
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    mensajeError = "Error: ${error.message}"
                    return@addSnapshotListener
                }
                recetas.clear()
                snap?.documents?.forEach { doc ->
                    doc.toObject(RecetaV2::class.java)?.let { recetas[it.productoId ?: doc.id] = it }
                }
            }
    }

    fun agregarProducto(producto: SalesInventoryProductV2) {
        viewModelScope.launch {
            try {
                db.collection(FirestoreCollections.PRODUCTOS)
                    .document(producto.id)
                    .set(producto)
                    .await()
                mensajeExito = "Producto agregado ✓"
            } catch (e: Exception) {
                mensajeError = "Error: ${e.message}"
            }
        }
    }

    fun editarProducto(producto: SalesInventoryProductV2) {
        viewModelScope.launch {
            try {
                db.collection(FirestoreCollections.PRODUCTOS)
                    .document(producto.id)
                    .set(producto)
                    .await()
                mensajeExito = "Producto actualizado ✓"
            } catch (e: Exception) {
                mensajeError = "Error: ${e.message}"
            }
        }
    }

    fun eliminarProducto(producto: SalesInventoryProductV2) {
        viewModelScope.launch {
            try {
                db.collection(FirestoreCollections.PRODUCTOS)
                    .document(producto.id)
                    .delete()
                    .await()
                mensajeExito = "Producto eliminado ✓"
            } catch (e: Exception) {
                mensajeError = "Error: ${e.message}"
            }
        }
    }

    fun guardarReceta(receta: RecetaV2) {
        viewModelScope.launch {
            try {
                db.collection(FirestoreCollections.RECETAS)
                    .document(receta.id)
                    .set(receta)
                    .await()
                mensajeExito = "Receta guardada ✓"
            } catch (e: Exception) {
                mensajeError = "Error: ${e.message}"
            }
        }
    }

    fun eliminarReceta(recetaId: String) {
        viewModelScope.launch {
            try {
                db.collection(FirestoreCollections.RECETAS)
                    .document(recetaId)
                    .delete()
                    .await()
                mensajeExito = "Receta eliminada ✓"
            } catch (e: Exception) {
                mensajeError = "Error: ${e.message}"
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        listenerProductos?.remove()
        listenerRecetas?.remove()
    }
}