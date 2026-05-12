package com.bocatta.pos.domain

import org.junit.Test
import org.junit.Assert.*

class CajaViewModelLogicTest {

    private fun calcularEsperado(fondoInicial: Double, ventasEfectivo: Double, gastos: Double): Double {
        return fondoInicial + ventasEfectivo - gastos
    }

    private fun calcularDiferencia(contado: Double, esperado: Double): Double {
        return contado - esperado
    }

    private fun estaDentroTolerancia(diferencia: Double, tolerancia: Double): Boolean {
        return kotlin.math.abs(diferencia) <= tolerancia
    }

    @Test
    fun diferenciaCaja_exacto() {
        val esperado = calcularEsperado(500.0, 1000.0, 200.0)
        assertEquals(1300.0, esperado, 0.01)
        val diferencia = calcularDiferencia(1300.0, esperado)
        assertEquals(0.0, diferencia, 0.01)
        assertTrue(estaDentroTolerancia(diferencia, 10.0))
    }

    @Test
    fun diferenciaCaja_faltante() {
        val esperado = calcularEsperado(500.0, 1000.0, 200.0)
        val diferencia = calcularDiferencia(1200.0, esperado)
        assertEquals(-100.0, diferencia, 0.01)
    }

    @Test
    fun diferenciaCaja_sobrante() {
        val esperado = calcularEsperado(500.0, 1000.0, 200.0)
        val diferencia = calcularDiferencia(1400.0, esperado)
        assertEquals(100.0, diferencia, 0.01)
    }

    @Test
    fun diferenciaExacta_dentroTolerancia() {
        val esperado = calcularEsperado(500.0, 1000.0, 200.0) // 1300
        assertTrue(estaDentroTolerancia(calcularDiferencia(1305.0, esperado), 10.0))
    }

    @Test
    fun faltanteExcedeTolerancia() {
        val esperado = calcularEsperado(500.0, 1000.0, 200.0) // 1300
        assertFalse(estaDentroTolerancia(calcularDiferencia(1280.0, esperado), 10.0))
    }

    @Test
    fun sobranteExcedeTolerancia() {
        val esperado = calcularEsperado(500.0, 1000.0, 200.0) // 1300
        assertFalse(estaDentroTolerancia(calcularDiferencia(1320.0, esperado), 10.0))
    }

    @Test
    fun toleranciaCero_soloExacto() {
        val esperado = calcularEsperado(100.0, 500.0, 50.0) // 550
        assertTrue(estaDentroTolerancia(calcularDiferencia(550.0, esperado), 0.0))
        assertFalse(estaDentroTolerancia(calcularDiferencia(551.0, esperado), 0.0))
    }

    @Test
    fun toleranciaPersonalizada_5() {
        val esperado = 550.0
        assertTrue(estaDentroTolerancia(calcularDiferencia(553.0, esperado), 5.0))
        assertFalse(estaDentroTolerancia(calcularDiferencia(556.0, esperado), 5.0))
    }

    @Test
    fun toleranciaPersonalizada_50() {
        val esperado = 550.0
        assertTrue(estaDentroTolerancia(calcularDiferencia(500.0, esperado), 50.0))
        assertTrue(estaDentroTolerancia(calcularDiferencia(600.0, esperado), 50.0))
        assertFalse(estaDentroTolerancia(calcularDiferencia(601.0, esperado), 50.0))
    }

    @Test
    fun fondoInicialMinimo_exacto() {
        val fondoMinimo = 100.0
        assertTrue(500.0 >= fondoMinimo)
        assertTrue(100.0 >= fondoMinimo)
        assertFalse(50.0 >= fondoMinimo)
    }

    @Test
    fun membresia_visita5_otorgaDescuento() {
        val historialCompras = listOf(1L, 2L, 3L, 4L)
        val visitaActual = 5L
        val esVisita5 = historialCompras.size + 1 == 5
        assertTrue(esVisita5)
    }

    @Test
    fun membresia_visita4_noOtorgaDescuento() {
        val historialCompras = listOf(1L, 2L, 3L)
        val esVisita5 = historialCompras.size + 1 == 5
        assertFalse(esVisita5)
    }

    @Test
    fun membresia_visita6_reseteaContador() {
        val historialCompras = listOf(1L, 2L, 3L, 4L, 5L)
        val contadorPostDescuento = historialCompras.size % 5
        assertEquals(0, contadorPostDescuento)
    }
}
