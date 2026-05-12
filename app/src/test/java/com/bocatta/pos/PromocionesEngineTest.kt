package com.bocatta.pos

import com.bocatta.pos.domain.model.*
import com.bocatta.pos.domain.usecase.PromocionesEngine
import org.junit.Test
import org.junit.Assert.*
import java.math.BigDecimal

/**
 * Tests unitarios para el motor de cálculo de promociones.
 *
 * Valida la lógica de aplicación de descuentos implementada en
 * PromocionesEngine.
 */
class PromocionesEngineTest {

    private val engine = PromocionesEngine()

    // ──────────────────────────────────────────────────────────
    // Helpers para construcción de datos de prueba
    // ──────────────────────────────────────────────────────────

    private fun makeItem(precio: Double, categoria: String = "TEST", cantidad: Int = 1): ItemCarritoV2 =
        ItemCarritoV2(
            producto = SalesInventoryProductV2(id = "p-${precio.toLong()}", nombre = "Test $precio", categoria = categoria, precioVenta = emptyMap()),
            precioFinal = BigDecimal.valueOf(precio),
            cantidad = cantidad
        )

    // ──────────────────────────────────────────────────────────
    // Casos de prueba
    // ──────────────────────────────────────────────────────────

    @Test
    fun descuentoPorcentaje_aplicaCorrectamenteEnTicketCompleto() {
        val carrito = listOf(makeItem(100.0), makeItem(200.0)) // subtotal = 300
        val promo = PromocionUniversal(
            id = "p1", nombre = "10% OFF",
            tipoDescuento = TipoDescuentoPromo.PORCENTAJE,
            valorDescuento = 10.0,
            alcance = AlcancePromo.TICKET_COMPLETO,
            activa = true
        )

        val desc = engine.calcular(carrito, listOf(promo))

        assertEquals(30.0, desc, 0.01)
    }

    @Test
    fun descuentoMontoFijo_aplicaCorrectamenteEnTicketCompleto() {
        val carrito = listOf(makeItem(150.0), makeItem(150.0)) // subtotal = 300
        val promo = PromocionUniversal(
            id = "p2", nombre = "$50 OFF",
            tipoDescuento = TipoDescuentoPromo.MONTO_FIJO_TICKET,
            valorDescuento = 50.0,
            alcance = AlcancePromo.TICKET_COMPLETO,
            activa = true
        )

        val desc = engine.calcular(carrito, listOf(promo))

        assertEquals(50.0, desc, 0.01)
    }

    @Test
    fun descuento_noSuperaMaximoConfigurado() {
        val carrito = listOf(makeItem(1000.0)) // subtotal = 1000
        val promo = PromocionUniversal(
            id = "p3", nombre = "50% MAX 100",
            tipoDescuento = TipoDescuentoPromo.PORCENTAJE,
            valorDescuento = 50.0, // 50% de 1000 = 500
            descuentoMaximoTicket = 100.0, // pero máximo 100
            alcance = AlcancePromo.TICKET_COMPLETO,
            activa = true
        )

        val desc = engine.calcular(carrito, listOf(promo))

        assertEquals(100.0, desc, 0.01) // Debe estar limitado a 100
    }

    @Test
    fun promocion_noAplicaSiMontoMinimoNoAlcanzado() {
        val carrito = listOf(makeItem(50.0)) // subtotal = 50
        val promo = PromocionUniversal(
            id = "p4", nombre = "Promo $200+",
            tipoDescuento = TipoDescuentoPromo.MONTO_FIJO_TICKET,
            valorDescuento = 30.0,
            montoMinimoTicket = 200.0, // requiere $200 mínimo
            alcance = AlcancePromo.TICKET_COMPLETO,
            activa = true
        )

        val desc = engine.calcular(carrito, listOf(promo))

        assertEquals(0.0, desc, 0.01)
    }

    @Test
    fun promocionNoCombinableDetieneElCiclo() {
        val carrito = listOf(makeItem(200.0)) // subtotal = 200
        val promo1 = PromocionUniversal(
            id = "p5a", nombre = "10% (no combinable)",
            tipoDescuento = TipoDescuentoPromo.PORCENTAJE,
            valorDescuento = 10.0,
            alcance = AlcancePromo.TICKET_COMPLETO,
            combinable = false, // Esta bloquea el resto
            activa = true
        )
        val promo2 = PromocionUniversal(
            id = "p5b", nombre = "20% adicional",
            tipoDescuento = TipoDescuentoPromo.PORCENTAJE,
            valorDescuento = 20.0,
            alcance = AlcancePromo.TICKET_COMPLETO,
            combinable = true,
            activa = true
        )

        val desc = engine.calcular(carrito, listOf(promo1, promo2))

        // Solo aplica el 10%, la segunda no debe ejecutarse
        assertEquals(20.0, desc, 0.01)
    }

    @Test
    fun carritoVacio_devuelveCero() {
        assertEquals(0.0, engine.calcular(emptyList(), listOf(
            PromocionUniversal(id="p", nombre="x", tipoDescuento=TipoDescuentoPromo.PORCENTAJE, valorDescuento=10.0, alcance=AlcancePromo.TICKET_COMPLETO, activa=true)
        )), 0.01)
    }

    @Test
    fun promosVacias_devuelveCero() {
        assertEquals(0.0, engine.calcular(listOf(makeItem(100.0)), emptyList()), 0.01)
    }

    @Test
    fun promocionInactiva_noAplica() {
        val carrito = listOf(makeItem(100.0))
        val promo = PromocionUniversal(id="p", nombre="10%", tipoDescuento=TipoDescuentoPromo.PORCENTAJE, valorDescuento=10.0, alcance=AlcancePromo.TICKET_COMPLETO, activa=false)
        assertEquals(0.0, engine.calcular(carrito, listOf(promo)), 0.01)
    }

    @Test
    fun descuento_aplicaSoloAProductosEspecificos() {
        val carritoMixto = listOf(
            makeItem(100.0).copy(producto = SalesInventoryProductV2(id = "prod-A", nombre = "A", precioVenta = emptyMap())),
            makeItem(200.0).copy(producto = SalesInventoryProductV2(id = "prod-B", nombre = "B", precioVenta = emptyMap()))
        )
        val promo = PromocionUniversal(
            id = "p6", nombre = "50% en A",
            tipoDescuento = TipoDescuentoPromo.PORCENTAJE,
            valorDescuento = 50.0,
            alcance = AlcancePromo.PRODUCTOS_ESPECIFICOS,
            itemsIncluidosIds = listOf("prod-A"), // Solo aplica a prod-A
            activa = true
        )

        val desc = engine.calcular(carritoMixto, listOf(promo))

        assertEquals(50.0, desc, 0.01) // 50% de 100 = 50
    }
}
