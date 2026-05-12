package com.bocatta.pos.domain

import org.junit.Test
import org.junit.Assert.*

class StockValidationTest {

    private fun validarStock(deducciones: Map<String, Double>, stockActual: Map<String, Double>): Boolean {
        return deducciones.all { (insumoId, requerido) ->
            (stockActual[insumoId] ?: 0.0) >= requerido
        }
    }

    @Test
    fun stockSuficiente_permiteVenta() {
        val deducciones = mapOf("harina" to 200.0, "huevo" to 2.0, "leche" to 100.0)
        val stock = mapOf("harina" to 1000.0, "huevo" to 12.0, "leche" to 500.0)
        assertTrue(validarStock(deducciones, stock))
    }

    @Test
    fun stockInsuficiente_bloqueaVenta() {
        val deducciones = mapOf("harina" to 200.0, "huevo" to 2.0)
        val stock = mapOf("harina" to 100.0, "huevo" to 12.0)
        assertFalse(validarStock(deducciones, stock))
    }

    @Test
    fun stockExactoAlLimite_permiteVenta() {
        val deducciones = mapOf("harina" to 200.0, "huevo" to 2.0)
        val stock = mapOf("harina" to 200.0, "huevo" to 2.0)
        assertTrue(validarStock(deducciones, stock))
    }

    @Test
    fun insumoFaltanteEnStock_bloqueaVenta() {
        val deducciones = mapOf("harina" to 200.0, "huevo" to 2.0, "inexistente" to 1.0)
        val stock = mapOf("harina" to 1000.0, "huevo" to 12.0)
        assertFalse(validarStock(deducciones, stock))
    }

    @Test
    fun stockCero_bloqueaVenta() {
        val deducciones = mapOf("harina" to 1.0)
        val stock = mapOf("harina" to 0.0)
        assertFalse(validarStock(deducciones, stock))
    }

    @Test
    fun multiplesInsumos_unoInsuficiente_bloqueaTodo() {
        val deducciones = mapOf("harina" to 200.0, "huevo" to 2.0, "leche" to 100.0)
        val stock = mapOf("harina" to 1000.0, "huevo" to 1.0, "leche" to 500.0)
        assertFalse(validarStock(deducciones, stock))
    }

    @Test
    fun stockVacio_bloqueaVenta() {
        val deducciones = mapOf("harina" to 1.0)
        val stock = emptyMap<String, Double>()
        assertFalse(validarStock(deducciones, stock))
    }

    @Test
    fun membresia_contador5_otorgaDescuento() {
        val historial = listOf(1L, 2L, 3L, 4L)
        val esVisitaDescuento = (historial.size + 1) % 5 == 0
        assertTrue(esVisitaDescuento)
    }

    @Test
    fun membresia_postDescuento_reseteaContador() {
        val historial = listOf(1L, 2L, 3L, 4L, 5L)
        val contador = historial.size % 5
        assertEquals(0, contador)
    }

    @Test
    fun membresia_visita6_sinDescuento() {
        val historial = listOf(1L, 2L, 3L, 4L, 5L)
        val esVisitaDescuento = (historial.size + 1) % 5 == 0
        assertFalse(esVisitaDescuento)
    }
}
