package com.bocatta.pos.presentation.viewmodel

import androidx.compose.runtime.*
import androidx.lifecycle.viewModelScope
import com.bocatta.pos.data.repository.DataSeederV2
import com.bocatta.pos.domain.model.*
import com.bocatta.pos.network.firebase.FirebaseFirestoreProvider
import com.bocatta.pos.core.constants.FirestoreCollections
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.launch

class InventarioAdminViewModel(private val dataSeeder: DataSeederV2 = DataSeederV2()) : BaseViewModel() {

    private val db = FirebaseFirestoreProvider.db
    private var listenerInsumos: ListenerRegistration? = null

    var insumosMaestros = mutableStateListOf<InsumoV2>()
    var seederEnProgreso by mutableStateOf(false)
        private set
    private var seederEjecutado = false

    init {
        escucharInsumosMaestros()
    }

    private fun escucharInsumosMaestros() {
        listenerInsumos?.remove()
        listenerInsumos = db.collection(FirestoreCollections.INVENTARIO_GLOBAL).addSnapshotListener { snap, error ->
            if (error != null) {
                mensajeError = "Error al carregar: ${error.message}"
                return@addSnapshotListener
            }
            if (snap != null) {
                insumosMaestros.clear()
                snap.documents.forEach { doc ->
                    val data = doc.data
                    if (data != null) {
                        insumosMaestros.add(InsumoV2(
                            id = doc.id,
                            nombre = data["nombre"]?.toString() ?: doc.id,
                            cantidadEnBase = (data["cantidadEnBase"] as? Number)?.toDouble()
                                ?: (data["cantidadDisponible"] as? Number)?.toDouble()
                                ?: 0.0,
                            categoria = data["categoria"]?.toString() ?: "Bodega",
                            unidadBase = data["unidadBase"]?.toString()
                                ?: data["unidadMedida"]?.toString()
                                ?: data["unidadMedidaMinima"]?.toString()
                                ?: "g"
                        ))
                    }
                }
            }
        }
    }

    fun cargarInsumos(rol: Rol) {
        if (rol != Rol.DUEÑO && rol != Rol.ADMIN) {
            mensajeError = "Acceso Denegado: Se requiere rol Administrativo."
            return
        }
        listenerInsumos?.remove()
        listenerInsumos = db.collection(FirestoreCollections.INSUMOS).addSnapshotListener { snap, error ->
            if (error != null) {
                mensajeError = "Error: ${error.message}"
                return@addSnapshotListener
            }
            if (snap != null) {
                if (snap.isEmpty && !seederEjecutado) {
                    seederEjecutado = true
                    seederEnProgreso = true
                    viewModelScope.launch(safeHandler) {
                        try {
                            dataSeeder.inicializarTodoV2()
                            seederEnProgreso = false
                        } catch (e: Exception) {
                            mensajeError = "Error en seed: ${e.message}"
                            seederEnProgreso = false
                        }
                    }
                } else if (!snap.isEmpty) {
                    insumosMaestros.clear()
                    snap.documents.forEach { doc ->
                        doc.toObject(InsumoV2::class.java)?.let {
                            insumosMaestros.add(it.copy(id = doc.id))
                        }
                    }
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        listenerInsumos?.remove()
    }
}
