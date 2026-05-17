package com.bocatta.pos.domain.util

import com.bocatta.pos.domain.model.ItemCarritoV2
import com.bocatta.pos.domain.model.SalesInventoryProductV2
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

class CarritoCalculatorTest {

    private val productoBase = SalesInventoryProductV2(
        id = "test", nombre = "Producto",
        precioVenta = mapOf("atlixco" to 25.0),
        categoria = "CAT",
        consumiblesAsociados = listOf()
    )

    @Test
    fun calcularSubtotal_vacio() {
        assertEquals(BigDecimal.ZERO, CarritoCalculator.calcularSubtotal(emptyList()))
    }

    @Test
    fun calcularSubtotal_unItem() {
        val items = listOf(
            ItemCarritoV2(producto = productoBase, precioFinal = BigDecimal("25.00"), cantidad = 2)
        )
        assertEquals(BigDecimal("50.00"), CarritoCalculator.calcularSubtotal(items))
    }

    @Test
    fun calcularSubtotal_multiplesItems() {
        val items = listOf(
            ItemCarritoV2(producto = productoBase, precioFinal = BigDecimal("25.00"), cantidad = 2),
            ItemCarritoV2(producto = productoBase, precioFinal = BigDecimal("35.00"), cantidad = 1)
        )
        assertEquals(BigDecimal("85.00"), CarritoCalculator.calcularSubtotal(items))
    }

    @Test
    fun calcularTotalVenta_sinDescuentos() {
        val total = CarritoCalculator.calcularTotalVenta(
            subtotal = BigDecimal("100.00"),
            descuentoLealtad = 0.0, descuentoPromociones = 0.0, descuentoManual = 0.0
        )
        assertEquals(100.0, total, 0.001)
    }

    @Test
    fun calcularTotalVenta_conDescuentoLealtad() {
        val total = CarritoCalculator.calcularTotalVenta(
            subtotal = BigDecimal("100.00"),
            descuentoLealtad = 10.0, descuentoPromociones = 0.0, descuentoManual = 0.0
        )
        assertEquals(90.0, total, 0.001)
    }

    @Test
    fun calcularTotalVenta_conMultiplesDescuentos() {
        val total = CarritoCalculator.calcularTotalVenta(
            subtotal = BigDecimal("100.00"),
            descuentoLealtad = 10.0, descuentoPromociones = 5.0, descuentoManual = 3.0
        )
        assertEquals(82.0, total, 0.001)
    }

    @Test
    fun calcularTotalVenta_totalNegativo_devuelveCero() {
        val total = CarritoCalculator.calcularTotalVenta(
            subtotal = BigDecimal("10.00"),
            descuentoLealtad = 20.0, descuentoPromociones = 0.0, descuentoManual = 0.0
        )
        assertEquals(0.0, total, 0.001)
    }

    @Test
    fun calcularItemsCount() {
        val items = listOf(
            ItemCarritoV2(producto = productoBase, precioFinal = BigDecimal("10"), cantidad = 3),
            ItemCarritoV2(producto = productoBase, precioFinal = BigDecimal("15"), cantidad = 1)
        )
        assertEquals(4, CarritoCalculator.calcularItemsCount(items))
    }
}
