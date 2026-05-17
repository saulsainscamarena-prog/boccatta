package com.bocatta.pos.presentation.viewmodel

import androidx.compose.runtime.*
import androidx.lifecycle.viewModelScope
import com.bocatta.pos.network.firebase.FirebaseFirestoreProvider
import com.bocatta.pos.core.constants.FirestoreCollections
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ConfigViewModel : BaseViewModel() {

    private val db = FirebaseFirestoreProvider.db
    private var listenerConfigCaja: ListenerRegistration? = null
    private var listenerConfigSeguridad: ListenerRegistration? = null

    var configCaja by mutableStateOf<Map<String, Any>>(emptyMap())
    var configSeguridad by mutableStateOf<Map<String, Any>>(emptyMap())

    init {
        escucharConfigGlobal()
    }

    private fun escucharConfigGlobal() {
        listenerConfigCaja?.remove()
        listenerConfigSeguridad?.remove()

        listenerConfigCaja = db.collection(FirestoreCollections.CONFIGURACION)
            .document("parametros_caja")
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    mensajeError = "Error: ${error.message}"
                    return@addSnapshotListener
                }
                configCaja = snap?.data ?: emptyMap()
            }

        listenerConfigSeguridad = db.collection(FirestoreCollections.CONFIGURACION)
            .document("seguridad")
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    mensajeError = "Error: ${error.message}"
                    return@addSnapshotListener
                }
                configSeguridad = snap?.data ?: emptyMap()
            }
    }

    fun guardarParametrosCaja(nuevosParametros: Map<String, Any>, callback: () -> Unit) {
        viewModelScope.launch {
            try {
                db.collection(FirestoreCollections.CONFIGURACION)
                    .document("parametros_caja")
                    .set(nuevosParametros)
                    .await()
                callback()
            } catch (e: Exception) {
                mensajeError = "Error: ${e.message}"
            }
        }
    }

    fun guardarConfigSeguridad(nuevaConfig: Map<String, Any>, callback: () -> Unit) {
        viewModelScope.launch {
            try {
                db.collection(FirestoreCollections.CONFIGURACION)
                    .document("seguridad")
                    .set(nuevaConfig)
                    .await()
                callback()
            } catch (e: Exception) {
                mensajeError = "Error: ${e.message}"
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        listenerConfigCaja?.remove()
        listenerConfigSeguridad?.remove()
    }
}
