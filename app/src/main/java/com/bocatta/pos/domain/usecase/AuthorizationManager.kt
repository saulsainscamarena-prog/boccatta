package com.bocatta.pos.domain.usecase

import com.bocatta.pos.core.constants.FirestoreCollections
import com.bocatta.pos.domain.model.Rol
import com.bocatta.pos.domain.model.Usuario
import com.bocatta.pos.domain.model.PermisoEmpleado
import com.bocatta.pos.domain.model.EmpleadoV2
import com.bocatta.pos.network.firebase.FirebaseFirestoreProvider
import kotlinx.coroutines.tasks.await
import timber.log.Timber

enum class AccionSensible {
    ABRIR_TURNO,
    APLICAR_DESCUENTO,
    CANCELAR_ITEM,
    TRANSFERIR_MESA,
    CAMBIAR_MESA,
    CERRAR_CAJA,
    AJUSTAR_INVENTARIO
}

class AuthorizationManager {
    private val db by lazy { FirebaseFirestoreProvider.db }

    /**
     * Verifica si el usuario actual tiene el permiso necesario de manera directa (por su rol de superusuario)
     * o granular (por su registro de PermisoEmpleado).
     */
    suspend fun verificarPermiso(usuario: Usuario, accion: AccionSensible): Boolean {
        // ADMIN y DUENO son superusuarios autorizados nativamente
        if (usuario.rol == Rol.ADMIN || usuario.rol == Rol.DUEÑO) {
            return true
        }

        return try {
            // Buscamos el permiso granular del empleado en v2_employees
            val snap = db.collection(FirestoreCollections.EMPLEADOS)
                .document(usuario.uid)
                .get()
                .await()

            if (snap.exists()) {
                val permiso = snap.toObject(PermisoEmpleado::class.java)
                if (permiso != null) {
                    return evaluarPermisoFino(permiso, accion)
                }
            }
            false
        } catch (e: Exception) {
            Timber.tag("AUTH").e(e, "Error al verificar permisos para la accion %s", accion.name)
            false
        }
    }

    /**
     * Valida el pin presencial de un administrador o dueno y, si es correcto,
     * registra la accion en la bitacora de auditoria firmada por el administrador autorizante.
     */
    suspend fun validarConPinYAuditar(
        pin: String,
        accion: AccionSensible,
        usuarioResponsable: String,
        sucursal: String,
        detalles: String = "",
        onResult: (Boolean) -> Unit
    ) {
        try {
            // Auditoria de seguridad: consulta por PIN en servidor y valida el rol del unico documento retornado.
            // Esto evita la necesidad de configurar indices compuestos y previene brechas de seguridad.
            val snap = db.collection(FirestoreCollections.USUARIOS)
                .whereEqualTo("pinAcceso", pin)
                .limit(1)
                .get()
                .await()

            if (!snap.isEmpty) {
                val adminDoc = snap.documents.first()
                val rolDoc = adminDoc.getString("rol") ?: ""

                if (rolDoc == "ADMIN" || rolDoc == "DUEÑO") {
                    val adminNombre = adminDoc.getString("nombre") ?: "ADMIN/DUENO"
                    val adminUid = adminDoc.id

                    // Registramos en la bitacora
                    registrarAuditoria(
                        empleadoId = adminUid,
                        empleadoNombre = adminNombre,
                        accion = accion.name,
                        responsable = usuarioResponsable,
                        sucursal = sucursal,
                        detalles = detalles,
                        autorizoConPin = adminNombre
                    )
                    onResult(true)
                    return
                }
            }
            onResult(false)
        } catch (e: Exception) {
            Timber.tag("AUTH").e(e, "Error al validar pin administrador")
            onResult(false)
        }
    }

    /**
     * Registra un evento en la coleccion v2_auditoria_empleados de forma atomica.
     */
    suspend fun registrarAuditoria(
        empleadoId: String,
        empleadoNombre: String,
        accion: String,
        responsable: String,
        sucursal: String,
        detalles: String = "",
        autorizoConPin: String? = null
    ) {
        try {
            val auditoriaId = db.collection(FirestoreCollections.AUDITORIA_EMPLEADOS).document().id
            val registro = mapOf(
                "id" to auditoriaId,
                "empleadoId" to empleadoId,
                "nombreEmpleado" to empleadoNombre,
                "accion" to accion,
                "responsable" to responsable,
                "sucursal" to sucursal.lowercase(),
                "detalles" to detalles,
                "fecha" to System.currentTimeMillis(),
                "autorizoConPin" to (autorizoConPin ?: "")
            )
            db.collection(FirestoreCollections.AUDITORIA_EMPLEADOS).document(auditoriaId).set(registro).await()
            Timber.tag("AUTH").i("Auditoria registrada: $auditoriaId para accion $accion")
        } catch (e: Exception) {
            Timber.tag("AUTH").e(e, "Error al escribir en bitacora de auditoria")
        }
    }

    internal fun evaluarPermisoFino(permiso: PermisoEmpleado, accion: AccionSensible): Boolean {
        return when (accion) {
            AccionSensible.ABRIR_TURNO -> permiso.puedeTomarOrden
            AccionSensible.APLICAR_DESCUENTO -> permiso.puedeCobrarTarjeta || permiso.puedeCobrarEfectivo
            AccionSensible.CANCELAR_ITEM -> permiso.puedeTomarOrden
            AccionSensible.TRANSFERIR_MESA -> permiso.puedeTransferirMesas
            AccionSensible.CAMBIAR_MESA -> permiso.puedeGestionarMesas
            AccionSensible.CERRAR_CAJA -> permiso.puedeCerrarTurnoAjeno
            AccionSensible.AJUSTAR_INVENTARIO -> permiso.puedeGestionarEmpleados
        }
    }

    /**
     * Valida el pin presencial de un administrador o dueno en la coleccion de usuarios
     * y retorna true si es valido y tiene el rol correspondiente en memoria.
     * Esta version ligera no genera bitacora de auditoria.
     */
    suspend fun validarPinAdmin(pin: String): Boolean {
        return try {
            val snap = db.collection(FirestoreCollections.USUARIOS)
                .whereEqualTo("pinAcceso", pin)
                .limit(1)
                .get()
                .await()

            if (!snap.isEmpty) {
                val adminDoc = snap.documents.first()
                val rolDoc = adminDoc.getString("rol") ?: ""
                rolDoc == "ADMIN" || rolDoc == "DUEÑO"
            } else {
                false
            }
        } catch (e: Exception) {
            Timber.tag("AUTH").e(e, "Error al validar pin admin de forma ligera")
            false
        }
    }

    /**
     * Verifica un pin de acceso de un empleado en la coleccion de empleados.
     * Retorna el objeto EmpleadoV2 si es valido, de lo contrario null.
     */
    suspend fun verificarPinEmpleado(pin: String): EmpleadoV2? {
        return try {
            val snap = db.collection(FirestoreCollections.EMPLEADOS)
                .whereEqualTo("pinAcceso", pin)
                .limit(1)
                .get()
                .await()

            if (!snap.isEmpty) {
                snap.documents.first().toObject(EmpleadoV2::class.java)
            } else {
                null
            }
        } catch (e: Exception) {
            Timber.tag("AUTH").e(e, "Error al verificar pin de empleado")
            null
        }
    }
}
