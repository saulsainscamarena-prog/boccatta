package com.bocatta.pos.presentation.viewmodel

import androidx.compose.runtime.*
import androidx.lifecycle.viewModelScope
import com.bocatta.pos.data.repository.AuthRepository
import com.bocatta.pos.domain.model.EmpleadoV2
import com.bocatta.pos.network.firebase.FirebaseFirestoreProvider
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.bocatta.pos.core.constants.FirestoreCollections

class EmployeeViewModelV2(private val authRepo: AuthRepository = AuthRepository()) : BaseViewModel() {
    private val db = FirebaseFirestoreProvider.db

    var listaEmpleados = mutableStateListOf<EmpleadoV2>()
        private set

    private var listenerEmpleados: com.google.firebase.firestore.ListenerRegistration? = null

    init {
        escucharEmpleados()
    }

    private fun escucharEmpleados() {
        listenerEmpleados?.remove()
        listenerEmpleados = db.collection(FirestoreCollections.EMPLEADOS).addSnapshotListener { snap, _ ->
            if (snap != null) {
                listaEmpleados.clear()
                snap.documents.forEach { doc ->
                    doc.toObject(EmpleadoV2::class.java)?.let { listaEmpleados.add(it.copy(id = doc.id)) }
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        listenerEmpleados?.remove()
    }

    fun cambiarRol(empleadoId: String, nuevoRol: String) {
        viewModelScope.launch {
            try {
                db.collection(FirestoreCollections.EMPLEADOS).document(empleadoId)
                    .update("rol", nuevoRol).await()
                mensajeExito = "Rol actualizado ?"
            } catch (e: Exception) {
                mensajeError = "Error: ${e.message}"
            }
        }
    }

    fun despedirEmpleado(empleadoId: String, empleadoNombre: String, usuarioResponsable: String) {
        viewModelScope.launch {
            try {
                val ahora = System.currentTimeMillis()
                val historialId = db.collection(FirestoreCollections.AUDITORIA_EMPLEADOS).document().id
                val historial = mapOf(
                    "id" to historialId,
                    "empleadoId" to empleadoId,
                    "nombreEmpleado" to empleadoNombre,
                    "accion" to "DESPIDO",
                    "responsable" to usuarioResponsable,
                    "fecha" to ahora
                )
                db.collection(FirestoreCollections.AUDITORIA_EMPLEADOS).document(historialId).set(historial).await()
                db.collection(FirestoreCollections.EMPLEADOS).document(empleadoId).delete().await()
                mensajeExito = "Empleado despedido ?"
            } catch (e: Exception) {
                mensajeError = "Error: ${e.message}"
            }
        }
    }

    fun verificarPin(pin: String, onSuccess: (EmpleadoV2) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            cargando = true
            try {
                val snap = db.collection(FirestoreCollections.EMPLEADOS)
                    .whereEqualTo("pinAcceso", pin)
                    .get().await()
                
                if (!snap.isEmpty) {
                    val emp = snap.documents.first().toObject(EmpleadoV2::class.java)
                    if (emp != null) onSuccess(emp) else onError("Error al leer datos")
                } else {
                    onError("PIN Incorrecto")
                }
            } catch (e: Exception) {
                onError("Error: ${e.message}")
            } finally {
                cargando = false
            }
        }
    }

    /**
     * LÑgica de registro con llaves maestras
     */
    fun registrarNuevoUsuario(nombre: String, codigoInput: String, sucursal: String) {
        viewModelScope.launch {
            cargando = true
            try {
                val rol = authRepo.validarCodigoYObtenerRol(codigoInput)
                if (rol == null) {
                    mensajeError = "CÑdigo de autorizaciÑn invÑlido"
                    return@launch
                }
                
                val id = db.collection(FirestoreCollections.EMPLEADOS).document().id
                val nuevo = EmpleadoV2(id = id, nombre = nombre, rol = rol.name, pinAcceso = "0000", sucursalAsignada = sucursal)
                db.collection(FirestoreCollections.EMPLEADOS).document(id).set(nuevo).await()
                mensajeExito = "Empleado registrado ?"
            } catch (e: Exception) {
                mensajeError = "Error: ${e.message}"
            } finally {
                cargando = false
            }
        }
    }
}

