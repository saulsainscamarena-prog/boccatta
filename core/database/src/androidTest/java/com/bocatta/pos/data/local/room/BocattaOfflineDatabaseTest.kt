package com.bocatta.pos.data.local.room

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bocatta.pos.data.local.room.entity.ConsumibleV2Entity
import com.bocatta.pos.data.local.room.entity.FolioEntity
import com.bocatta.pos.data.local.room.entity.HeldOrderEntity
import com.bocatta.pos.data.local.room.entity.IngredienteRecetaEntity
import com.bocatta.pos.data.local.room.entity.InsumoV2Entity
import com.bocatta.pos.data.local.room.entity.OperacionPendienteEntity
import com.bocatta.pos.data.local.room.entity.PresentacionInsV2Entity
import com.bocatta.pos.data.local.room.entity.ProductoV2Entity
import com.bocatta.pos.data.local.room.entity.RecetaV2Entity
import com.bocatta.pos.data.local.room.entity.RegistroJornadaEntity
import com.bocatta.pos.data.local.room.entity.TurnoContingenciaEntity
import com.bocatta.pos.data.local.room.entity.VentaPendienteEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BocattaOfflineDatabaseTest {
    private lateinit var db: BocattaOfflineDatabase

    @Before
    fun createDb() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            BocattaOfflineDatabase::class.java
        ).allowMainThreadQueries().build()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun testDaosAreAccessible() {
        assertNotNull(db.ventaPendienteDao())
        assertNotNull(db.operacionPendienteDao())
        assertNotNull(db.folioDao())
        assertNotNull(db.turnoContingenciaDao())
        assertNotNull(db.productoDao())
    }

    @Test
    fun testInsertAndReadVenta() = runBlocking {
        val entity = VentaPendienteEntity(
            id = "v1", tenantId = "t1", ticket = 1L, codigoTicket = "T-001",
            total = 100.0, descuentoLealtad = 0.0, fecha = 1000L, sucursal = "s1",
            atendio = "usr1", metodoPago = "efectivo", carritoJson = "{}"
        )
        db.ventaPendienteDao().insertar(entity)
        val result = db.ventaPendienteDao().obtenerPendientes()
        assertEquals(1, result.size)
        assertEquals(entity, result.first())
    }

    @Test
    fun testInsertAndReadTurno() = runBlocking {
        val entity = TurnoContingenciaEntity(
            id = "t1", sucursal = "s1", usuarioId = "usr1", usuarioNombre = "User",
            rol = "admin", fondoInicial = 500.0, fechaApertura = 1000L, fechaCierre = null
        )
        db.turnoContingenciaDao().insert(entity)
        val result = db.turnoContingenciaDao().getById("t1")
        assertEquals(entity, result)
    }

    @Test
    fun testInsertAndReadInsumo() = runBlocking {
        val entity = InsumoV2Entity(id = "i1", nombre = "Harina", categoria = "Granos")
        db.productoDao().insertInsumo(entity)
        val result = db.productoDao().getInsumoById("i1")
        assertEquals(entity, result)
    }

    @Test
    fun testInsertAndReadConsumible() = runBlocking {
        val entity = ConsumibleV2Entity(id = "c1", nombre = "Vaso")
        db.productoDao().insertConsumible(entity)
        val result = db.productoDao().getConsumibleById("c1")
        assertEquals(entity, result)
    }

    @Test
    fun testInsertAndReadProducto() = runBlocking {
        val entity = ProductoV2Entity(
            id = "p1", nombre = "Café", categoria = "Bebidas"
        )
        db.productoDao().insertProducto(entity)
        val result = db.productoDao().getProductoById("p1")
        assertEquals(entity, result)
    }

    @Test
    fun testInsertAndReadReceta() = runBlocking {
        val entity = RecetaV2Entity(id = "r1", nombre = "Receta café", productoId = "p1")
        db.productoDao().insertReceta(entity)
        val result = db.productoDao().getRecetaById("r1")
        assertEquals(entity, result)
    }

    @Test
    fun testInsertAndReadIngrediente() = runBlocking {
        val entity = IngredienteRecetaEntity(
            id = "ir1", recetaId = "r1", insumoId = "i1", cantidad = 10.0
        )
        db.productoDao().insertIngredienteReceta(entity)
        val result = db.productoDao().getIngredienteRecetaById("ir1")
        assertEquals(entity, result)
    }

    @Test
    fun testInsertAndReadPresentacion() = runBlocking {
        val entity = PresentacionInsV2Entity(
            id = "pr1", insumoId = "i1", nombre = "Bolsa 1kg"
        )
        db.productoDao().insertPresentacionIns(entity)
        val result = db.productoDao().getPresentacionInsById("pr1")
        assertEquals(entity, result)
    }

    @Test
    fun testInsertAndReadHeldOrder() = runBlocking {
        val entity = HeldOrderEntity(
            id = "h1", carritoJson = "{}", fecha = 1000L, sucursal = "s1"
        )
        db.productoDao().insertHeldOrder(entity)
        val result = db.productoDao().getHeldOrderById("h1")
        assertEquals(entity, result)
    }

    @Test
    fun testInsertAndReadFolio() = runBlocking {
        val entity = FolioEntity(tenantId = "t1", sucursal = "s1")
        db.folioDao().guardarFolio(entity)
        val result = db.folioDao().obtenerFolio("s1")
        assertEquals(entity, result)
    }

    @Test
    fun testInsertAndReadJornada() = runBlocking {
        val entity = RegistroJornadaEntity(
            id = "j1", usuario = "usr1", sucursal = "s1", accion = "entrada",
            timestamp = 1000L
        )
        db.productoDao().insertRegistroJornada(entity)
        val result = db.productoDao().getRegistroJornadaById("j1")
        assertEquals(entity, result)
    }

    @Test
    fun testInsertAndReadOperacion() = runBlocking {
        val entity = OperacionPendienteEntity(
            id = "o1", tipo = "devolucion", motivo = "cliente",
            usuarioId = "usr1", sucursal = "s1", fecha = 1000L, dataJson = "{}"
        )
        db.operacionPendienteDao().insertar(entity)
        val result = db.operacionPendienteDao().obtenerPorTipo("devolucion")
        assertEquals(1, result.size)
        assertEquals(entity, result.first())
    }
}
