package com.bocatta.pos.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class MembresiaDiscountCalculatorTest {
    @Test
    fun `promedio menor a 100 da 5 porciento`() {
        assertEquals(5.0, MembresiaDiscountCalculator.calcularPorcentajeDescuento(50.0), 0.001)
        assertEquals(5.0, MembresiaDiscountCalculator.calcularPorcentajeDescuento(99.99), 0.001)
        assertEquals(5.0, MembresiaDiscountCalculator.calcularPorcentajeDescuento(0.0), 0.001)
    }

    @Test
    fun `promedio entre 100 y 299 da 10 porciento`() {
        assertEquals(10.0, MembresiaDiscountCalculator.calcularPorcentajeDescuento(100.0), 0.001)
        assertEquals(10.0, MembresiaDiscountCalculator.calcularPorcentajeDescuento(200.0), 0.001)
        assertEquals(10.0, MembresiaDiscountCalculator.calcularPorcentajeDescuento(299.99), 0.001)
    }

    @Test
    fun `promedio entre 300 y 499 da 15 porciento`() {
        assertEquals(15.0, MembresiaDiscountCalculator.calcularPorcentajeDescuento(300.0), 0.001)
        assertEquals(15.0, MembresiaDiscountCalculator.calcularPorcentajeDescuento(400.0), 0.001)
        assertEquals(15.0, MembresiaDiscountCalculator.calcularPorcentajeDescuento(499.99), 0.001)
    }

    @Test
    fun `promedio 500 o mas da 20 porciento`() {
        assertEquals(20.0, MembresiaDiscountCalculator.calcularPorcentajeDescuento(500.0), 0.001)
        assertEquals(20.0, MembresiaDiscountCalculator.calcularPorcentajeDescuento(1000.0), 0.001)
        assertEquals(20.0, MembresiaDiscountCalculator.calcularPorcentajeDescuento(9999.99), 0.001)
    }

    @Test
    fun `calcularPromedio con lista vacia devuelve cero`() {
        assertEquals(0.0, MembresiaDiscountCalculator.calcularPromedio(emptyList()), 0.001)
    }

    @Test
    fun `calcularPromedio con valores`() {
        assertEquals(100.0, MembresiaDiscountCalculator.calcularPromedio(listOf(80.0, 120.0)), 0.001)
        assertEquals(250.0, MembresiaDiscountCalculator.calcularPromedio(listOf(200.0, 300.0)), 0.001)
    }

    @Test
    fun `calcularPromedio con un solo valor`() {
        assertEquals(150.0, MembresiaDiscountCalculator.calcularPromedio(listOf(150.0)), 0.001)
    }
}
