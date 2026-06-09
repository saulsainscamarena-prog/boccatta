package com.bocatta.pos.domain.usecase

import com.bocatta.pos.core.constants.FirestoreCollections
import com.bocatta.pos.domain.model.Rol
import com.bocatta.pos.domain.model.Usuario
import com.bocatta.pos.domain.model.PermisoEmpleado
import com.bocatta.pos.domain.model.EmpleadoV2
import com.bocatta.pos.domain.model.PinAuthorization
import com.bocatta.pos.network.firebase.FirebaseFirestoreProvider
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import java.security.MessageDigest

enum class AccionSensible {
    ABRIR_TURNO,
    APLICAR_DESCUENTO,
    CANCELAR_ITEM,
    TRANSFERIR_MESA,
    CAMBIAR_MESA,
    CERRAR_CAJA,
    AJUSTAR_INVENTARIO,
    GASTAR_CAJA
}

interface PinRateLimitStore {
    var intentosFallidos: Int
    var cooldownHasta: Long

    fun reset() {
        intentosFallidos = 0
        cooldownHasta = 0L
    }
}

class InMemoryPinRateLimitStore : PinRateLimitStore {
    override var intentosFallidos: Int = 0
    override var cooldownHasta: Long = 0L
}

class AuthorizationManager(
    private val rateLimitStore: PinRateLimitStore = InMemoryPinRateLimitStore(),
    private val now: () -> Long = { System.currentTimeMillis() }
) {
    private val db by lazy { FirebaseFirestoreProvider.db }

    companion object {
        private const val PIN_PEPPER = "BocattaPOS2026@PepperSalt"
        private const val MAX_INTENTOS_ANTES_COOLDOWN = 3
        private const val COOLDOWN_INCREMENTAL_MS = 10_000L
        private const val COOLDOWN_MAX_MS = 300_000L

        fun hashPinForStorage(pin: String): String {
            val normalized = pin.trim()
            val digest = MessageDigest.getInstance("SHA-256").digest((normalized + PIN_PEPPER).toByteArray(Charsets.UTF_8))
            return digest.joinToString("") { "%02x".format(it) }
        }
    }

    /**
     * Verifica si el rate-limiter permite un intento de PIN.
     * @return null si permitido, o un mensaje de bloqueo si el cooldown está activo.
     */
    fun verificarRateLimit(): String? {
        val ahora = now()
        if (ahora < rateLimitStore.cooldownHasta) {
            val segundosRestantes = ((rateLimitStore.cooldownHasta - ahora) / 1000) + 1
            return "Demasiados intentos. Espera ${segundosRestantes}s"
        }
        return null
    }

    private fun registrarIntentoFallido() {
        rateLimitStore.intentosFallidos += 1
        if (rateLimitStore.intentosFallidos >= MAX_INTENTOS_ANTES_COOLDOWN) {
            val incrementalTime = rateLimitStore.intentosFallidos * COOLDOWN_INCREMENTAL_MS
            val cappedTime = incrementalTime.coerceAtMost(COOLDOWN_MAX_MS)
            rateLimitStore.cooldownHasta = now() + cappedTime
            Timber.tag("AUTH").w(
                "Rate limit activado: ${rateLimitStore.intentosFallidos} intentos fallidos, cooldown hasta ${rateLimitStore.cooldownHasta}"
            )
        }
    }

    private fun resetearIntentos() {
        rateLimitStore.reset()
    }

    /**
     * Verifica si el usuario actual tiene el permiso necesario de manera directa (por su rol de superusuario)
     * o granular (por su registro de PermisoEmpleado).
     */

    /**
     * Verifica si el usuario actual tiene el permiso necesario de manera directa (por su rol de superusuario)
     * o granular (por su registro de PermisoEmpleado).
     */
    suspend fun verificarPermiso(usuario: Usuario, accion: AccionSensible): Boolean {
        // ADMIN y DUENO son superusuarios autorizados nativamente
        if (usuario.rol == Rol.ADMIN || usuario.rol == Rol.DUEÑO) {
            return true
        }

        if (usuario.rol == Rol.VENDEDOR && accion == AccionSensible.ABRIR_TURNO) {
            return true
        }

        return try {
            val permiso = obtenerPermisoEmpleadoActual(usuario.uid)
            permiso?.let { evaluarPermisoFino(it, accion) } ?: false
        } catch (e: Exception) {
            Timber.tag("AUTH").e(e, "Error al verificar permisos para la accion %s", accion.name)
            false
        }
    }

    private suspend fun obtenerPermisoEmpleadoActual(uidActual: String): PermisoEmpleado? {
        if (uidActual.isBlank()) return null
        val doc = db.collection(FirestoreCollections.EMPLEADOS)
            .document(uidActual)
            .get()
            .await()
        if (!doc.exists() || doc.getString("authUid") != uidActual) return null
        return doc.toObject(PermisoEmpleado::class.java)
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
        verificarRateLimit()?.let {
            Timber.tag("AUTH").w("PIN bloqueado por rate limit: $it")
            onResult(false)
            return
        }
        try {
            val authorization = obtenerAutorizacionPorPin(pin)
                .await()

            if (authorization != null && authorization.active && authorization.role in listOf("ADMIN", "DUEÑO")) {
                resetearIntentos()
                registrarAuditoria(
                    empleadoId = authorization.userId,
                    empleadoNombre = authorization.displayName,
                    accion = accion.name,
                    responsable = usuarioResponsable,
                    sucursal = sucursal,
                    detalles = detalles,
                    autorizoConPin = authorization.displayName
                )
                onResult(true)
                return
            }
            registrarIntentoFallido()
            onResult(false)
        } catch (e: Exception) {
            Timber.tag("AUTH").e(e, "Error al validar pin administrador")
            registrarIntentoFallido()
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
                "sucursal" to sucursal.lowercase(java.util.Locale.getDefault()),
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
            AccionSensible.GASTAR_CAJA -> permiso.puedeCerrarTurnoAjeno
        }
    }

    /**
     * Valida el pin presencial de un administrador o dueno en la coleccion de usuarios
     * y retorna true si es valido y tiene el rol correspondiente en memoria.
     * Esta version ligera no genera bitacora de auditoria.
     */
    suspend fun validarPinAdmin(pin: String): Boolean {
        verificarRateLimit()?.let { return false }
        return try {
            val authorization = obtenerAutorizacionPorPin(pin).await()
            val valido = authorization != null && authorization.active && authorization.role in listOf("ADMIN", "DUEÑO")
            if (valido) resetearIntentos() else registrarIntentoFallido()
            valido
        } catch (e: Exception) {
            Timber.tag("AUTH").e(e, "Error al validar pin admin de forma ligera")
            registrarIntentoFallido()
            false
        }
    }

    /**
     * Verifica un pin de acceso de un empleado en la coleccion de empleados.
     * Retorna el objeto EmpleadoV2 si es valido, de lo contrario null.
     */
    suspend fun verificarPinEmpleado(pin: String): EmpleadoV2? {
        verificarRateLimit()?.let { return null }
        return try {
            val authorization = obtenerAutorizacionPorPin(pin).await()
            if (authorization != null && authorization.active) {
                resetearIntentos()
                db.collection(FirestoreCollections.EMPLEADOS)
                    .document(authorization.userId)
                    .get()
                    .await()
                    .toObject(EmpleadoV2::class.java)
            } else {
                registrarIntentoFallido()
                null
            }
        } catch (e: Exception) {
            Timber.tag("AUTH").e(e, "Error al verificar pin de empleado")
            registrarIntentoFallido()
            null
        }
    }

    suspend fun validarPinDesbloqueo(pin: String, usuario: Usuario?, sucursal: String): Boolean {
        verificarRateLimit()?.let { return false }
        val uidActual = usuario?.uid.orEmpty()
        if (uidActual.isBlank()) {
            registrarIntentoFallido()
            return false
        }

        return try {
            val authorization = obtenerAutorizacionPorPin(pin).await()
            val autorizado = authorization != null &&
                authorization.active &&
                (authorization.branchId.isBlank() || authorization.branchId.equals(sucursal, ignoreCase = true)) &&
                when {
                    authorization.role == "ADMIN" || authorization.role == "DUEÑO" -> true
                    authorization.userId == uidActual -> true
                    else -> empleadoPerteneceAUsuarioActual(authorization.userId, uidActual)
                }

            if (autorizado) resetearIntentos() else registrarIntentoFallido()
            autorizado
        } catch (e: Exception) {
            Timber.tag("AUTH").e(e, "Error al validar pin de desbloqueo")
            registrarIntentoFallido()
            false
        }
    }

    private suspend fun empleadoPerteneceAUsuarioActual(empleadoId: String, uidActual: String): Boolean {
        if (empleadoId.isBlank() || uidActual.isBlank()) return false
        val empleado = db.collection(FirestoreCollections.EMPLEADOS)
            .document(empleadoId)
            .get()
            .await()
            .toObject(EmpleadoV2::class.java)
        return empleado?.authUid == uidActual
    }

    private fun obtenerAutorizacionPorPin(pin: String) =
        db.collection(FirestoreCollections.PIN_AUTHORIZATIONS)
            .document(hashPinForStorage(pin))
            .get()
            .continueWith { task ->
                val doc = task.result
                if (doc.exists()) doc.toObject(PinAuthorization::class.java) else null
            }
}
