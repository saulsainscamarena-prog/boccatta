package com.bocatta.pos.presentation.viewmodel

import androidx.compose.runtime.*
import androidx.lifecycle.viewModelScope
import com.bocatta.pos.domain.model.*
import com.bocatta.pos.network.firebase.FirebaseFirestoreProvider
import com.bocatta.pos.core.constants.FirestoreCollections
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AuditoriaViewModel : BaseViewModel() {

    private val db = FirebaseFirestoreProvider.db
    private var listenerCancelaciones: ListenerRegistration? = null
    private var listenerGastos: ListenerRegistration? = null
    private var listenerUsuarios: ListenerRegistration? = null

    var cancelacionesPendientes = mutableStateListOf<Map<String, Any>>()
        private set
    var usuarios = mutableStateListOf<Usuario>()
        private set
    var totalGastosHoy by mutableStateOf(0.0)
        private set
    var historialVentasV2 = mutableStateListOf<VentaV2>()
        private set

    init {
        escucharCancelaciones()
        escucharGastosHoy()
        escucharUsuarios()
    }

    private fun escucharCancelaciones() {
        listenerCancelaciones?.remove()
        listenerCancelaciones = db.collection(FirestoreCollections.CANCELACIONES)
            .whereEqualTo("aprobada", false)
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    mensajeError = "Error: ${error.message}"
                    return@addSnapshotListener
                }
                cancelacionesPendientes.clear()
                snap?.documents?.forEach { doc ->
                    doc.data?.let { cancelacionesPendientes.add(it) }
                }
            }
    }

    private fun escucharGastosHoy() {
        val hoy = java.util.Calendar.getInstance().apply { 
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0) 
        }.timeInMillis
        
        listenerGastos?.remove()
        listenerGastos = db.collection(FirestoreCollections.GASTOS)
            .whereGreaterThanOrEqualTo("fecha", hoy)
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    mensajeError = "Error: ${error.message}"
                    return@addSnapshotListener
                }
                totalGastosHoy = snap?.documents?.sumOf { it.getDouble("monto") ?: 0.0 } ?: 0.0
            }
    }

    private fun escucharUsuarios() {
        listenerUsuarios?.remove()
        listenerUsuarios = db.collection(FirestoreCollections.USUARIOS)
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    mensajeError = "Error: ${error.message}"
                    return@addSnapshotListener
                }
                usuarios.clear()
                snap?.documents?.forEach { doc ->
                    doc.toObject(Usuario::class.java)?.let { usuarios.add(it.copy(uid = doc.id)) }
                }
            }
    }

    fun aprobarCancelacion(cancelacion: Map<String, Any>, callback: () -> Unit) {
        viewModelScope.launch {
            try {
                val docId = cancelacion["id"] as? String
                    ?: cancelacion["documentoId"] as? String
                if (docId != null) {
                    db.collection(FirestoreCollections.CANCELACIONES)
                        .document(docId)
                        .update("aprobada", true)
                        .await()
                    mensajeExito = "Cancelación aprobada ✓"
                    callback()
                }
            } catch (e: Exception) {
                mensajeError = "Error: ${e.message}"
            }
        }
    }

    fun rechazarCancelacion(cancelacion: Map<String, Any>, callback: () -> Unit) {
        viewModelScope.launch {
            try {
                val docId = cancelacion["id"] as? String
                    ?: cancelacion["documentoId"] as? String
                if (docId != null) {
                    db.collection(FirestoreCollections.CANCELACIONES)
                        .document(docId)
                        .delete()
                        .await()
                    callback()
                }
            } catch (e: Exception) {
                mensajeError = "Error: ${e.message}"
            }
        }
    }

    fun cargarHistorial() {
        viewModelScope.launch {
            try {
                val snap = db.collection(FirestoreCollections.VENTAS)
                    .orderBy("fecha", Query.Direction.DESCENDING)
                    .limit(50)
                    .get()
                    .await()
                historialVentasV2.clear()
                snap.documents.forEach { doc ->
                    doc.toObject(VentaV2::class.java)?.let { historialVentasV2.add(it.copy(id = doc.id)) }
                }
            } catch (e: Exception) {
                mensajeError = "Error: ${e.message}"
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        listenerCancelaciones?.remove()
        listenerGastos?.remove()
        listenerUsuarios?.remove()
    }
}
