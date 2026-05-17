package com.bocatta.pos.domain.util

import com.bocatta.pos.domain.model.SalesInventoryProductV2
import com.bocatta.pos.domain.model.ConsumibleRequerido
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductoValidatorTest {

    @Test
    fun valido_conCamposCorrectos() {
        val prod = SalesInventoryProductV2(
            id = "test", nombre = "Crepa Dulce",
            precioVenta = mapOf("atlixco" to 25.0),
            categoria = "CREPAS_DULCES",
            consumiblesAsociados = listOf(ConsumibleRequerido("charola", 1.0))
        )
        assertTrue(ProductoValidator.esValido(prod))
    }

    @Test
    fun invalido_siNombreVacio() {
        val prod = SalesInventoryProductV2(
            id = "test", nombre = "",
            precioVenta = mapOf("atlixco" to 25.0),
            categoria = "CREPAS_DULCES"
        )
        assertFalse(ProductoValidator.esValido(prod))
    }

    @Test
    fun invalido_sinPrecio() {
        val prod = SalesInventoryProductV2(
            id = "test", nombre = "Crepa",
            precioVenta = emptyMap(),
            categoria = "CREPAS_DULCES"
        )
        assertFalse(ProductoValidator.esValido(prod))
    }

    @Test
    fun invalido_siCategoriaVacia() {
        val prod = SalesInventoryProductV2(
            id = "test", nombre = "Crepa",
            precioVenta = mapOf("atlixco" to 25.0),
            categoria = ""
        )
        assertFalse(ProductoValidator.esValido(prod))
    }

    @Test
    fun invalido_comboSinItems() {
        val prod = SalesInventoryProductV2(
            id = "test", nombre = "Combo",
            precioVenta = mapOf("atlixco" to 50.0),
            categoria = "Combos", esCombo = true,
            productosCombo = emptyList()
        )
        assertFalse(ProductoValidator.esValido(prod))
    }

    @Test
    fun invalido_consumibleCantidadCero() {
        val prod = SalesInventoryProductV2(
            id = "test", nombre = "Crepa",
            precioVenta = mapOf("atlixco" to 25.0),
            categoria = "CREPAS_DULCES",
            consumiblesAsociados = listOf(ConsumibleRequerido("charola", 0.0))
        )
        assertFalse(ProductoValidator.esValido(prod))
    }
}
