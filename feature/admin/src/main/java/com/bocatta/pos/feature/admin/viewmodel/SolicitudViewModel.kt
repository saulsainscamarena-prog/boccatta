package com.bocatta.pos.feature.admin.viewmodel

import com.bocatta.pos.core.ui.viewmodel.BaseViewModel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.viewModelScope
import com.bocatta.pos.domain.model.EstadoSolicitudTurno
import com.bocatta.pos.domain.model.SolicitudTurnoExtra
import com.bocatta.pos.data.repository.SolicitudTurnoRepository
import com.bocatta.pos.network.firebase.FirebaseFirestoreProvider
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.launch

class SolicitudViewModel(
    private val repository: SolicitudTurnoRepository = SolicitudTurnoRepository()
) : BaseViewModel() {

    var solicitudes = mutableStateListOf<SolicitudTurnoExtra>()
        private set
    private var listener: ListenerRegistration? = null

    fun enviarSolicitud(solicitud: SolicitudTurnoExtra) {
        viewModelScope.launch {
            cargando = true
            try {
                if (repository.guardar(solicitud)) {
                    mensajeExito = "Solicitud enviada"
                }
            } catch (e: Exception) { mensajeError = "Error: ${e.message}" }
            cargando = false
        }
    }

    fun escucharSolicitudes() {
        listener?.remove()
        listener = FirebaseFirestoreProvider.db.collection("v2_solicitudes_turno")
            .orderBy("horaSolicitada", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snap, error ->
                if (error != null) { mensajeError = "Error: ${error.message}"; return@addSnapshotListener }
                if (snap != null) {
                    solicitudes.clear()
                    snap.documents.forEach { doc ->
                        doc.toObject(SolicitudTurnoExtra::class.java)?.let {
                            solicitudes.add(it.copy(id = doc.id))
                        }
                    }
                }
            }
    }

    fun responderSolicitud(id: String, estado: EstadoSolicitudTurno, respondidoPor: String) {
        viewModelScope.launch {
            try {
                if (repository.responder(id, estado.name, respondidoPor)) {
                    mensajeExito = "Solicitud ${estado.name.lowercase(java.util.Locale.getDefault())}"
                }
            } catch (e: Exception) { mensajeError = "Error: ${e.message}" }
        }
    }

    fun detenerEscucha() {
        listener?.remove()
        listener = null
    }

    override fun onCleared() {
        super.onCleared()
        detenerEscucha()
    }
}



