package com.bocatta.pos.presentation.viewmodel

import androidx.compose.runtime.*
import androidx.lifecycle.viewModelScope
import com.bocatta.pos.data.repository.AuthRepository
import com.bocatta.pos.domain.model.EmpleadoV2
import com.bocatta.pos.network.firebase.FirebaseFirestoreProvider
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.bocatta.pos.core.constants.FirestoreCollections
import com.bocatta.pos.domain.usecase.AuthorizationManager
import timber.log.Timber
import kotlin.random.Random

class EmployeeViewModelV2(
    private val authRepo: AuthRepository,
    private val authManager: AuthorizationManager
) : BaseViewModel() {
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
                    doc.toObject(EmpleadoV2::class.java)?.let { empleado ->
                        val normalized = empleado.copy(id = doc.id)
                        listaEmpleados.add(normalized)
                        migrarPinLegacySiExiste(normalized)
                    }
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
                mensajeExito = "Rol actualizado con exito"
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
                mensajeExito = "Empleado despedido con exito"
            } catch (e: Exception) {
                mensajeError = "Error: ${e.message}"
            }
        }
    }

    fun verificarPin(pin: String, onSuccess: (EmpleadoV2) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            cargando = true
            try {
                val emp = authManager.verificarPinEmpleado(pin)
                if (emp != null) {
                    onSuccess(emp)
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
     * Logica de registro con llaves maestras
     */
    fun registrarNuevoUsuario(nombre: String, codigoInput: String, sucursal: String) {
        viewModelScope.launch {
            cargando = true
            try {
                val rol = authRepo.validarCodigoYObtenerRol(codigoInput)
                if (rol == null) {
                    mensajeError = "Codigo de autorizacion invalido"
                    return@launch
                }

                val id = db.collection(FirestoreCollections.EMPLEADOS).document().id
                val pinTemporal = generarPinTemporal()
                val pinHash = AuthorizationManager.hashPinForStorage(pinTemporal)
                val nuevo = EmpleadoV2(id = id, nombre = nombre, rol = rol.name, pinAcceso = "", sucursalAsignada = sucursal, authUid = "")
                val batch = db.batch()
                batch.set(db.collection(FirestoreCollections.EMPLEADOS).document(id), nuevo)
                batch.set(
                    db.collection(FirestoreCollections.PIN_AUTHORIZATIONS).document(pinHash),
                    mapOf(
                        "pinHash" to pinHash,
                        "userId" to id,
                        "displayName" to nombre,
                        "role" to rol.name,
                        "branchId" to sucursal.lowercase(),
                        "active" to true,
                        "updatedAt" to System.currentTimeMillis()
                    )
                )
                batch.commit().await()
                mensajeExito = "Empleado registrado con exito. PIN temporal: $pinTemporal"
            } catch (e: Exception) {
                mensajeError = "Error: ${e.message}"
            } finally {
                cargando = false
            }
        }
    }

    private fun generarPinTemporal(): String =
        Random.nextInt(100000, 1000000).toString()

    private fun migrarPinLegacySiExiste(empleado: EmpleadoV2) {
        val pinLegacy = empleado.pinAcceso.trim()
        if (pinLegacy.isBlank() || pinLegacy == "0000") return

        viewModelScope.launch {
            val result = runCatching {
                val pinHash = AuthorizationManager.hashPinForStorage(pinLegacy)
                val authRef = db.collection(FirestoreCollections.PIN_AUTHORIZATIONS).document(pinHash)
                val employeeRef = db.collection(FirestoreCollections.EMPLEADOS).document(empleado.id)
                db.runTransaction { transaction ->
                    val existing = transaction.get(authRef)
                    val existingUserId = existing.getString("userId")
                    if (existing.exists() && existingUserId != empleado.id) {
                        false
                    } else {
                        transaction.set(
                            authRef,
                            mapOf(
                                "pinHash" to pinHash,
                                "userId" to empleado.id,
                                "displayName" to empleado.nombre,
                                "role" to empleado.rol,
                                "branchId" to empleado.sucursalAsignada.lowercase(),
                                "active" to true,
                                "updatedAt" to System.currentTimeMillis()
                            )
                        )
                        transaction.update(employeeRef, "pinAcceso", "")
                        true
                    }
                }.await()
            }
            result
                .onSuccess { migrated ->
                    if (!migrated) {
                        Timber.tag("AUTH").w(
                            "No se migro PIN legacy de empleado %s porque el hash ya pertenece a otro usuario",
                            empleado.id
                        )
                    }
                }
                .onFailure { error ->
                    Timber.tag("AUTH").e(
                        error,
                        "Error al migrar PIN legacy de empleado %s",
                        empleado.id
                    )
                }
        }
    }
}
