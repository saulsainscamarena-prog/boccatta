package com.bocatta.pos.domain.usecase

import com.bocatta.pos.core.constants.FirestoreCollections
import com.bocatta.pos.domain.model.EmpleadoV2
import com.bocatta.pos.domain.model.PermisoEmpleado
import com.bocatta.pos.network.firebase.FirebaseFirestoreProvider
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

/**
 * Caso de uso para gestionar el CRUD y los permisos de los empleados de Bocatta POS.
 * Extrae la lógica de negocio pesada de AdminViewModel.
 */
class GestionEmpleadosUseCase {
    private val db = FirebaseFirestoreProvider.db

    /**
     * Guarda un empleado completo y sus permisos de forma atómica usando Firestore Batch.
     * Si se proporciona un nuevo PIN, genera su hash SHA-256 y lo guarda en la colección de autorizaciones.
     */
    suspend fun guardarEmpleadoCompleto(
        empleado: EmpleadoV2,
        permisos: PermisoEmpleado,
        pinNuevo: String?
    ) {
        val batch = db.batch()
        val empRef = db.collection(FirestoreCollections.EMPLEADOS)
            .document(empleado.id.ifBlank { db.collection(FirestoreCollections.EMPLEADOS).document().id })
        val empId = empRef.id

        val dataMap = mutableMapOf<String, Any?>(
            "id" to empId,
            "nombre" to empleado.nombre,
            "rol" to empleado.rol,
            "sucursalAsignada" to empleado.sucursalAsignada,
            "authUid" to empleado.authUid,
            "puesto" to permisos.puesto,
            "puedeTomarOrden" to permisos.puedeTomarOrden,
            "puedeCobrarEfectivo" to permisos.puedeCobrarEfectivo,
            "puedeCobrarTarjeta" to permisos.puedeCobrarTarjeta,
            "puedePreparar" to permisos.puedePreparar,
            "puedeGestionarMesas" to permisos.puedeGestionarMesas,
            "puedeTransferirMesas" to permisos.puedeTransferirMesas,
            "puedeAprobarCambiosZona" to permisos.puedeAprobarCambiosZona,
            "puedeCerrarTurnoAjeno" to permisos.puedeCerrarTurnoAjeno,
            "puedeVerSueldos" to permisos.puedeVerSueldos,
            "puedeGestionarEmpleados" to permisos.puedeGestionarEmpleados
        )

        batch.set(empRef, dataMap, SetOptions.merge())

        if (!pinNuevo.isNullOrBlank()) {
            val pinHash = AuthorizationManager.hashPinForStorage(pinNuevo)
            val pinRef = db.collection(FirestoreCollections.PIN_AUTHORIZATIONS).document(pinHash)
            batch.set(pinRef, mapOf(
                "pinHash" to pinHash,
                "userId" to empId,
                "displayName" to empleado.nombre,
                "role" to empleado.rol,
                "branchId" to empleado.sucursalAsignada.lowercase(),
                "active" to true,
                "updatedAt" to System.currentTimeMillis()
            ))
        }

        batch.commit().await()
    }
}
