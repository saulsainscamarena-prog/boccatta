package com.bocatta.pos.domain.usecase

import com.bocatta.pos.domain.model.PermisoEmpleado
import com.bocatta.pos.domain.model.Rol
import com.bocatta.pos.domain.model.Usuario
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class AuthorizationManagerTest {

    private val authManager = AuthorizationManager()

    @Test
    fun verificarPermiso_superUsuarioAdmin_siempreAutorizado() = runTest {
        val admin = Usuario(
            uid = "admin123",
            nombre = "Carlos Administrador",
            rol = Rol.ADMIN
        )

        // Verificamos que todas las acciones sensibles estén autorizadas para un ADMIN sin tocar Firestore
        for (accion in AccionSensible.values()) {
            val autorizado = authManager.verificarPermiso(admin, accion)
            assertTrue("ADMIN debería estar autorizado para ${accion.name}", autorizado)
        }
    }

    @Test
    fun verificarPermiso_superUsuarioDueno_siempreAutorizado() = runTest {
        val dueno = Usuario(
            uid = "dueno123",
            nombre = "Sofía Dueña",
            rol = Rol.DUEÑO
        )

        // Verificamos que todas las acciones sensibles estén autorizadas para DUEÑO sin tocar Firestore
        for (accion in AccionSensible.values()) {
            val autorizado = authManager.verificarPermiso(dueno, accion)
            assertTrue("DUEÑO debería estar autorizado para ${accion.name}", autorizado)
        }
    }

    @Test
    fun evaluarPermisoFino_abrirTurno_validaCorrectamente() {
        val permisoConAcceso = PermisoEmpleado(puedeTomarOrden = true)
        val permisoSinAcceso = PermisoEmpleado(puedeTomarOrden = false)

        assertTrue(authManager.evaluarPermisoFino(permisoConAcceso, AccionSensible.ABRIR_TURNO))
        assertFalse(authManager.evaluarPermisoFino(permisoSinAcceso, AccionSensible.ABRIR_TURNO))
    }

    @Test
    fun verificarPermiso_vendedorPuedeAbrirTurnoSinConsultarFirestore() = runTest {
        val vendedor = Usuario(uid = "vendedor123", nombre = "Mostrador", rol = Rol.VENDEDOR)

        assertTrue(authManager.verificarPermiso(vendedor, AccionSensible.ABRIR_TURNO))
    }

    @Test
    fun evaluarPermisoFino_aplicarDescuento_validaCorrectamente() {
        // Requiere puedeCobrarTarjeta || puedeCobrarEfectivo
        val permisoTarjeta = PermisoEmpleado(puedeCobrarTarjeta = true, puedeCobrarEfectivo = false)
        val permisoEfectivo = PermisoEmpleado(puedeCobrarTarjeta = false, puedeCobrarEfectivo = true)
        val permisoAmbos = PermisoEmpleado(puedeCobrarTarjeta = true, puedeCobrarEfectivo = true)
        val permisoNinguno = PermisoEmpleado(puedeCobrarTarjeta = false, puedeCobrarEfectivo = false)

        assertTrue(authManager.evaluarPermisoFino(permisoTarjeta, AccionSensible.APLICAR_DESCUENTO))
        assertTrue(authManager.evaluarPermisoFino(permisoEfectivo, AccionSensible.APLICAR_DESCUENTO))
        assertTrue(authManager.evaluarPermisoFino(permisoAmbos, AccionSensible.APLICAR_DESCUENTO))
        assertFalse(authManager.evaluarPermisoFino(permisoNinguno, AccionSensible.APLICAR_DESCUENTO))
    }

    @Test
    fun evaluarPermisoFino_cancelarItem_validaCorrectamente() {
        // Requiere puedeTomarOrden
        val permisoConAcceso = PermisoEmpleado(puedeTomarOrden = true)
        val permisoSinAcceso = PermisoEmpleado(puedeTomarOrden = false)

        assertTrue(authManager.evaluarPermisoFino(permisoConAcceso, AccionSensible.CANCELAR_ITEM))
        assertFalse(authManager.evaluarPermisoFino(permisoSinAcceso, AccionSensible.CANCELAR_ITEM))
    }

    @Test
    fun evaluarPermisoFino_transferirMesa_validaCorrectamente() {
        // Requiere puedeTransferirMesas
        val permisoConAcceso = PermisoEmpleado(puedeTransferirMesas = true)
        val permisoSinAcceso = PermisoEmpleado(puedeTransferirMesas = false)

        assertTrue(authManager.evaluarPermisoFino(permisoConAcceso, AccionSensible.TRANSFERIR_MESA))
        assertFalse(authManager.evaluarPermisoFino(permisoSinAcceso, AccionSensible.TRANSFERIR_MESA))
    }

    @Test
    fun evaluarPermisoFino_cambiarMesa_validaCorrectamente() {
        // Requiere puedeGestionarMesas
        val permisoConAcceso = PermisoEmpleado(puedeGestionarMesas = true)
        val permisoSinAcceso = PermisoEmpleado(puedeGestionarMesas = false)

        assertTrue(authManager.evaluarPermisoFino(permisoConAcceso, AccionSensible.CAMBIAR_MESA))
        assertFalse(authManager.evaluarPermisoFino(permisoSinAcceso, AccionSensible.CAMBIAR_MESA))
    }

    @Test
    fun evaluarPermisoFino_cerrarCaja_validaCorrectamente() {
        // Requiere puedeCerrarTurnoAjeno
        val permisoConAcceso = PermisoEmpleado(puedeCerrarTurnoAjeno = true)
        val permisoSinAcceso = PermisoEmpleado(puedeCerrarTurnoAjeno = false)

        assertTrue(authManager.evaluarPermisoFino(permisoConAcceso, AccionSensible.CERRAR_CAJA))
        assertFalse(authManager.evaluarPermisoFino(permisoSinAcceso, AccionSensible.CERRAR_CAJA))
    }

    @Test
    fun evaluarPermisoFino_ajustarInventario_validaCorrectamente() {
        // Requiere puedeGestionarEmpleados
        val permisoConAcceso = PermisoEmpleado(puedeGestionarEmpleados = true)
        val permisoSinAcceso = PermisoEmpleado(puedeGestionarEmpleados = false)

        assertTrue(authManager.evaluarPermisoFino(permisoConAcceso, AccionSensible.AJUSTAR_INVENTARIO))
        assertFalse(authManager.evaluarPermisoFino(permisoSinAcceso, AccionSensible.AJUSTAR_INVENTARIO))
    }

    @Test
    fun hashPinForStorage_isDeterministicAndDoesNotExposePin() {
        val hash1 = AuthorizationManager.hashPinForStorage("123456")
        val hash2 = AuthorizationManager.hashPinForStorage(" 123456 ")

        assertEquals(hash1, hash2)
        assertFalse(hash1.contains("123456"))
        assertEquals(64, hash1.length)
    }

    @Test
    fun verificarRateLimit_usesInjectedStoreAndClock() {
        val store = InMemoryPinRateLimitStore().apply {
            cooldownHasta = 20_000L
        }
        val manager = AuthorizationManager(store) { 10_000L }

        val message = manager.verificarRateLimit()

        assertTrue(message.orEmpty().contains("Espera 11s"))
    }
}
