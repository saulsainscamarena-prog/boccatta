package com.bocatta.pos.data.local

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.bocatta.pos.domain.model.InsumoV2
import org.junit.Assert.assertEquals
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
