package com.bocatta.pos.presentation.viewmodel

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.bocatta.pos.data.local.OfflineDatabase
import com.bocatta.pos.domain.model.Rol
import com.bocatta.pos.domain.model.Usuario
import com.bocatta.pos.domain.usecase.AuthorizationManager
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import com.bocatta.pos.data.local.room.BocattaRoomDatabase

@RunWith(AndroidJUnit4::class)
class CajaViewModelContingencyInstrumentedTest {

    private val db: OfflineDatabase
        get() = OfflineDatabase.getInstance(
            InstrumentationRegistry.getInstrumentation().targetContext
        )
        
    private val roomDb: BocattaRoomDatabase
        get() = BocattaRoomDatabase.getInstance(
            InstrumentationRegistry.getInstrumentation().targetContext
        )

    @Test
    fun abrirTurnoContingenciaLocal_adminPersisteTurnoYDesbloqueaCaja() = runBlocking {
        val suffix = System.nanoTime()
        val sucursal = "contingencia_vm_$suffix"
        val vm = CajaViewModel(AuthorizationManager(), db, roomDb.ventaPendienteDao())
        val deferred = CompletableDeferred<Boolean>()

        vm.abrirTurnoContingenciaLocal(
            fondoInicial = 300.0,
            sucursal = sucursal,
            usuario = Usuario(
                uid = "admin_$suffix",
                nombre = "Admin Test",
                rol = Rol.ADMIN
            )
        ) { deferred.complete(it) }

        val result = deferred.await()
        assertTrue(result)
        assertTrue(vm.modoContingenciaLocal)
        assertNotNull(vm.turnoActivo)
        assertEquals(300.0, vm.turnoActivo!!.fondoInicial, 0.001)

        val persisted = db.obtenerTurnoContingenciaAbierto(sucursal)
        assertNotNull(persisted)
        assertEquals("Admin Test", persisted!!.usuarioNombre)
        assertTrue(persisted.syncPendiente)
    }

    @Test
    fun abrirTurnoContingenciaLocal_vendedorQuedaBloqueado() {
        val suffix = System.nanoTime()
        val sucursal = "contingencia_vendedor_$suffix"
        val vm = CajaViewModel(AuthorizationManager(), db, roomDb.ventaPendienteDao())
        var callbackCalled = false
        var result = true

        vm.abrirTurnoContingenciaLocal(
            fondoInicial = 300.0,
            sucursal = sucursal,
            usuario = Usuario(
                uid = "vend_$suffix",
                nombre = "Vendedor Test",
                rol = Rol.VENDEDOR
            )
        ) {
            callbackCalled = true
            result = it
        }

        assertTrue(callbackCalled)
        assertFalse(result)
        assertFalse(vm.modoContingenciaLocal)
        assertEquals(null, vm.turnoActivo)
        assertEquals(null, db.obtenerTurnoContingenciaAbierto(sucursal))
    }
}
