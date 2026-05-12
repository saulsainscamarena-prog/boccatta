package com.bocatta.pos.domain

import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

class CuadreCajaManagerTest {
    @Test
    fun `cuadre correcto sin diferencia`() {
        val res = CuadreCajaManager.calcularCuadre(
            ventasTotal = BigDecimal("1000.00"),
            gastosTotal = BigDecimal("100.00"),
            fondoInicial = BigDecimal("200.00"),
            efectivoEnCaja = BigDecimal("1100.00")
        )
        assertTrue(res.esCorrecto)
        assertEquals(BigDecimal("0.00"), res.diferencia)
    }

    @Test
    fun `cuadre con faltante`() {
        val res = CuadreCajaManager.calcularCuadre(
            ventasTotal = BigDecimal("1000.00"),
            gastosTotal = BigDecimal("100.00"),
            fondoInicial = BigDecimal("200.00"),
            efectivoEnCaja = BigDecimal("1050.00")
        )
        assertFalse(res.esCorrecto)
        assertEquals(BigDecimal("-50.00"), res.diferencia)
    }

    @Test
    fun `cuadre con sobrante`() {
        val res = CuadreCajaManager.calcularCuadre(
            ventasTotal = BigDecimal("1000.00"),
            gastosTotal = BigDecimal("100.00"),
            fondoInicial = BigDecimal("200.00"),
            efectivoEnCaja = BigDecimal("1150.00")
        )
        assertFalse(res.esCorrecto)
        assertEquals(BigDecimal("50.00"), res.diferencia)
    }

    @Test
    fun `cuadre correcto dentro de tolerancia`() {
        val res = CuadreCajaManager.calcularCuadre(
            ventasTotal = BigDecimal("1000.00"),
            gastosTotal = BigDecimal("100.00"),
            fondoInicial = BigDecimal("200.00"),
            efectivoEnCaja = BigDecimal("1105.00"), // +5 diferencia
            toleranciaEfectivo = BigDecimal("10.00")
        )
        assertTrue("Debe ser correcto si está dentro de la tolerancia", res.esCorrecto)
        assertEquals(BigDecimal("5.00"), res.diferencia)
    }

    @Test
    fun `cuadre incorrecto fuera de tolerancia`() {
        val res = CuadreCajaManager.calcularCuadre(
            ventasTotal = BigDecimal("1000.00"),
            gastosTotal = BigDecimal("100.00"),
            fondoInicial = BigDecimal("200.00"),
            efectivoEnCaja = BigDecimal("1115.00"), // +15 diferencia
            toleranciaEfectivo = BigDecimal("10.00")
        )
        assertFalse("Debe ser incorrecto si excede la tolerancia", res.esCorrecto)
        assertEquals(BigDecimal("15.00"), res.diferencia)
    }
}
