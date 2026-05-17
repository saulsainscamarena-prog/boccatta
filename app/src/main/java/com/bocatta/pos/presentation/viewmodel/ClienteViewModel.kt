package com.bocatta.pos.presentation.viewmodel

import androidx.compose.runtime.*
import androidx.lifecycle.viewModelScope
import com.bocatta.pos.domain.model.ClienteV2
import com.bocatta.pos.network.firebase.FirebaseFirestoreProvider
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.bocatta.pos.core.constants.FirestoreCollections

class ClienteViewModel : BaseViewModel() {
    private val db = FirebaseFirestoreProvider.db

    var clientes = mutableStateListOf<ClienteV2>()
        private set

    fun buscarPorTelefono(input: String) {
        val queryStr = input.trim()
        if (queryStr.length < 3) {
            clientes.clear()
            return
        }

        val esNumerico = queryStr.all { it.isDigit() }
        
        viewModelScope.launch {
            try {
                val collection = db.collection(FirestoreCollections.CLIENTES)
                val query = if (esNumerico) {
                    collection.whereEqualTo("telefono", queryStr)
                } else {
                    val capitalizado = queryStr.replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() }
                    collection.whereGreaterThanOrEqualTo("nombre", capitalizado)
                        .whereLessThanOrEqualTo("nombre", capitalizado + "\uf8ff")
                }
                val snap = query.limit(10).get().await()
                clientes.clear()
                snap.documents.forEach { doc ->
                    doc.toObject(ClienteV2::class.java)?.let { clientes.add(it.copy(idDocumento = doc.id)) }
                }
            } catch (e: Exception) {
                mensajeError = "Error: ${e.message}"
            }
        }
    }

    fun registrarCliente(nombre: String, telefono: String, onExito: (ClienteV2) -> Unit) {
        viewModelScope.launch {
            cargando = true
            try {
                val snap = db.collection(FirestoreCollections.CLIENTES).whereEqualTo("telefono", telefono.trim()).get().await()
                if (!snap.isEmpty) { 
                    mensajeError = "Ya existe un cliente con ese telÑfono"
                    cargando = false
                    return@launch 
                }
                
                val docRef = db.collection(FirestoreCollections.CLIENTES).document()
                val clienteV2 = ClienteV2(
                    idDocumento = docRef.id,
                    nombre = nombre.trim(),
                    telefono = telefono.trim(),
                    visitasCicloActual = 0,
                    comprasCicloActual = emptyList(),
                    fechaUltimaVisita = System.currentTimeMillis()
                )
                docRef.set(clienteV2).await()
                
                mensajeExito = "Cliente registrado ?"
                onExito(clienteV2)
            } catch (e: Exception) {
                mensajeError = "Error: ${e.message}"
            }
            cargando = false
        }
    }

    fun editarCliente(cliente: ClienteV2) {
        viewModelScope.launch {
            try {
                val docId = cliente.idDocumento.ifBlank { cliente.telefono }
                db.collection(FirestoreCollections.CLIENTES).document(docId).update("nombre", cliente.nombre).await()
                mensajeExito = "Cliente actualizado ?"
            } catch (e: Exception) {
                mensajeError = "Error: ${e.message}"
            }
        }
    }

    fun eliminarCliente(cliente: ClienteV2) {
        viewModelScope.launch {
            try {
                val docId = cliente.idDocumento.ifBlank { cliente.telefono }
                db.collection(FirestoreCollections.CLIENTES).document(docId).delete().await()
                mensajeExito = "Cliente eliminado ?"
            } catch (e: Exception) {
                mensajeError = "Error: ${e.message}"
            }
        }
    }
}



