package com.bocatta.pos.data.local

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.bocatta.pos.domain.model.InsumoV2
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OfflineDatabaseFolioInstrumentedTest {

    private val db: OfflineDatabase
        get() = OfflineDatabase.getInstance(
            InstrumentationRegistry.getInstrumentation().targetContext
        )

    @Test
    fun primeraVentaOfflineReservaFolioUnoYDescuentaStock() {
        val suffix = System.nanoTime()
        val sucursal = "folio_test_$suffix"
        val insumoId = "insumo_$suffix"
        db.guardarInsumo(insumo(insumoId, 10.0))

        val venta = db.guardarVentaYDescontarStockReservandoFolio(
            ventaBase = ventaBase(sucursal),
            deducciones = mapOf(insumoId to 2.0)
        )

        assertEquals(1L, venta.ticket)
        assertTrue(venta.codigoTicket.isNotBlank())
        assertEquals(8.0, db.obtenerStockInsumo(insumoId), 0.001)
    }

    @Test
    fun ventaSinStockNoAvanzaFolioNiDescuentaStock() {
        val suffix = System.nanoTime()
        val sucursal = "rollback_test_$suffix"
        val insumoId = "insumo_$suffix"
        db.guardarInsumo(insumo(insumoId, 1.0))

        try {
            db.guardarVentaYDescontarStockReservandoFolio(
                ventaBase = ventaBase(sucursal),
                deducciones = mapOf(insumoId to 2.0)
            )
            throw AssertionError("La venta debio fallar por stock insuficiente")
        } catch (e: IllegalStateException) {
            assertTrue(e.message.orEmpty().contains("Stock local insuficiente"))
        }

        assertEquals(1.0, db.obtenerStockInsumo(insumoId), 0.001)

        val ventaConfirmada = db.guardarVentaYDescontarStockReservandoFolio(
            ventaBase = ventaBase(sucursal),
            deducciones = mapOf(insumoId to 1.0)
        )

        assertEquals(1L, ventaConfirmada.ticket)
        assertEquals(0.0, db.obtenerStockInsumo(insumoId), 0.001)
    }

    @Test
    fun segundaVentaOfflineUsaFolioDos() {
        val suffix = System.nanoTime()
        val sucursal = "secuencia_test_$suffix"
        val insumoId = "insumo_$suffix"
        db.guardarInsumo(insumo(insumoId, 5.0))

        val primera = db.guardarVentaYDescontarStockReservandoFolio(
            ventaBase = ventaBase(sucursal),
            deducciones = mapOf(insumoId to 1.0)
        )
        val segunda = db.guardarVentaYDescontarStockReservandoFolio(
            ventaBase = ventaBase(sucursal),
            deducciones = mapOf(insumoId to 1.0)
        )

        assertEquals(1L, primera.ticket)
        assertEquals(2L, segunda.ticket)
        assertEquals(3.0, db.obtenerStockInsumo(insumoId), 0.001)
    }

    @Test
    fun ventaOfflinePreservaPropinaYNotaOrden() {
        val suffix = System.nanoTime()
        val sucursal = "propina_test_$suffix"
        val insumoId = "insumo_$suffix"
        db.guardarInsumo(insumo(insumoId, 5.0))

        val venta = db.guardarVentaYDescontarStockReservandoFolio(
            ventaBase = ventaBase(sucursal).copy(
                propina = 12.5,
                notaOrden = "Cliente espera en barra"
            ),
            deducciones = mapOf(insumoId to 1.0)
        )

        val persisted = db.obtenerVentasPendientes().first { it.id == venta.id }
        assertEquals(12.5, persisted.propina, 0.001)
        assertEquals("Cliente espera en barra", persisted.notaOrden)
    }

    @Test
    fun ventaOfflinePreservaDescuentosOperativos() {
        val suffix = System.nanoTime()
        val sucursal = "descuentos_test_$suffix"
        val insumoId = "insumo_$suffix"
        db.guardarInsumo(insumo(insumoId, 5.0))

        val venta = db.guardarVentaYDescontarStockReservandoFolio(
            ventaBase = ventaBase(sucursal).copy(
                descuentoPromociones = 7.5,
                descuentoManual = 3.0
            ),
            deducciones = mapOf(insumoId to 1.0)
        )

        val persisted = db.obtenerVentasPendientes().first { it.id == venta.id }
        assertEquals(7.5, persisted.descuentoPromociones, 0.001)
        assertEquals(3.0, persisted.descuentoManual, 0.001)
    }

    @Test
    fun turnoContingenciaLocalSePersisteCierraYQuedaPendienteSync() {
        val suffix = System.nanoTime()
        val sucursal = "turno_contingencia_$suffix"
        val turno = TurnoContingenciaLocal(
            id = "contingencia_${sucursal}_20260530",
            sucursal = sucursal,
            usuarioId = "admin_$suffix",
            usuarioNombre = "Admin Test",
            rol = "ADMIN",
            fondoInicial = 250.0,
            fechaApertura = System.currentTimeMillis()
        )

        db.guardarTurnoContingencia(turno)

        val abierto = db.obtenerTurnoContingenciaAbierto(sucursal)
        assertNotNull(abierto)
        assertEquals(250.0, abierto!!.fondoInicial, 0.001)
        assertTrue(db.obtenerTurnosContingenciaPendientesSync().any { it.id == turno.id })

        db.cerrarTurnoContingencia(turno.id, efectivoContado = 500.0, tarjetaContada = 120.0)

        assertNull(db.obtenerTurnoContingenciaAbierto(sucursal))
        val pendiente = db.obtenerTurnosContingenciaPendientesSync().first { it.id == turno.id }
        assertEquals(TurnoContingenciaLocal.ESTADO_CERRADO, pendiente.estado)
        assertEquals(500.0, pendiente.efectivoContado, 0.001)
        assertEquals(120.0, pendiente.tarjetaContada, 0.001)

        db.marcarTurnoContingenciaSincronizado(turno.id)
        assertTrue(db.obtenerTurnosContingenciaPendientesSync().none { it.id == turno.id })
    }

    @Test
    fun ventasLocalesDesdeFiltraPorSucursalYFecha() {
        val suffix = System.nanoTime()
        val sucursal = "turno_ventas_$suffix"
        val insumoId = "insumo_$suffix"
        val desde = System.currentTimeMillis() - 1_000
        db.guardarInsumo(insumo(insumoId, 5.0))

        val venta = db.guardarVentaYDescontarStockReservandoFolio(
            ventaBase = ventaBase(sucursal),
            deducciones = mapOf(insumoId to 1.0)
        )

        val ventas = db.obtenerVentasLocalesDesde(sucursal, desde)
        assertTrue(ventas.any { it.id == venta.id })
        assertTrue(db.obtenerVentasLocalesDesde("otra_$suffix", desde).none { it.id == venta.id })
    }

    private fun insumo(id: String, stock: Double): InsumoV2 {
        return InsumoV2(
            id = id,
            nombre = id,
            categoria = "test",
            unidadBase = "pz",
            costoUnitarioBase = 1.0,
            cantidadEnBase = stock,
            stockMinimo = 0.0
        )
    }

    private fun ventaBase(sucursal: String): VentaOffline {
        return VentaOffline(
            id = "offline_${System.nanoTime()}",
            ticket = 0L,
            codigoTicket = "",
            total = 10.0,
            descuentoLealtad = 0.0,
            fecha = System.currentTimeMillis(),
            sucursal = sucursal,
            atendio = "test",
            metodoPago = "Efectivo",
            esConsumoEmpleado = false,
            clienteId = null,
            carritoJson = "[]"
        )
    }
}
