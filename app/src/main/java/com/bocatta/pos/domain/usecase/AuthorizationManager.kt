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
        // ADMIN y DUEÃ‘O son superusuarios autorizados nativamente
        if (usuario.rol == Rol.ADMIN || usuario.rol == Rol.DUEÃ‘O) {
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
            Timber.tag("AUTH").e(e, "Error al verificar permisos para la acciÃ³n %s", accion.name)
            false
        }
    }

    /**
     * Valida el pin presencial de un administrador o dueÃ±o y, si es correcto,
     * registra la acciÃ³n en la bitÃ¡cora de auditorÃ­a firmada por el administrador autorizante.
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
            // AuditorÃ­a de Seguridad: Consulta por PIN en servidor, validaciÃ³n de Rol en memoria del Ãºnico documento retornado.
            // Esto evita la necesidad de configurar Ã­ndices compuestos y previene brechas de seguridad.
            val snap = db.collection(FirestoreCollections.USUARIOS)
                .whereEqualTo("pinAcceso", pin)
                .limit(1)
                .get()
                .await()

            if (!snap.isEmpty) {
                val adminDoc = snap.documents.first()
                val rolDoc = adminDoc.getString("rol") ?: ""

                if (rolDoc == "ADMIN" || rolDoc == "DUEÃ‘O") {
                    val adminNombre = adminDoc.getString("nombre") ?: "ADMIN/DUEÃ‘O"
                    val adminUid = adminDoc.id

                    // Registramos en la bitÃ¡cora
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
     * Registra un evento en la colecciÃ³n v2_auditoria_empleados de forma atÃ³mica.
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
            Timber.tag("AUTH").i("AuditorÃ­a registrada: $auditoriaId para acciÃ³n $accion")
        } catch (e: Exception) {
            Timber.tag("AUTH").e(e, "Error al escribir en bitÃ¡cora de auditorÃ­a")
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
     * Valida el pin presencial de un administrador o dueÃ±o en la colecciÃ³n de usuarios
     * y retorna true si es vÃ¡lido y tiene el rol correspondiente en memoria.
     * Esta versiÃ³n ligera no genera bitÃ¡cora de auditorÃ­a.
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
                rolDoc == "ADMIN" || rolDoc == "DUEÃ‘O"
            } else {
                false
            }
        } catch (e: Exception) {
            Timber.tag("AUTH").e(e, "Error al validar pin admin de forma ligera")
            false
        }
    }

    /**
     * Verifica un pin de acceso de un empleado en la colecciÃ³n de empleados.
     * Retorna el objeto EmpleadoV2 si es vÃ¡lido, de lo contrario null.
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
