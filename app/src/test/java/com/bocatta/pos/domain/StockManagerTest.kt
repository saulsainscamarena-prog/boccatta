package com.bocatta.pos.domain

import com.bocatta.pos.domain.model.IngredienteReceta
import com.bocatta.pos.domain.model.SalesInventoryProductV2
import com.bocatta.pos.domain.model.RecetaV2
import org.junit.Assert.*
import org.junit.Test

class StockManagerTest {

    private val mockProducto = SalesInventoryProductV2(nombre = "Crepa Nutella")

    private fun recetaCon(vararg pares: Pair<String, Double>): RecetaV2 {
        return RecetaV2(
            id = "receta_test",
            nombre = "Receta Test",
            ingredientes = pares.map { (id, cant) ->
                IngredienteReceta(insumoId = id, nombreInsumo = id, cantidad = cant, unidad = "g")
            }
        )
    }

    @Test
    fun `sin receta siempre permite venta`() {
        val res = StockManager.validarStockParaVenta(mockProducto, 1, null, emptyMap())
        assertTrue(res is StockManager.StockResult.Suficiente)
    }

    @Test
    fun `stock suficiente para todos los ingredientes permite venta`() {
        val receta = recetaCon("ins_nutella" to 20.0, "ins_masa" to 50.0)
        val stock = mapOf("ins_nutella" to 100.0, "ins_masa" to 200.0)
        val res = StockManager.validarStockParaVenta(mockProducto, 1, receta, stock)
        assertTrue(res is StockManager.StockResult.Suficiente)
    }

    @Test
    fun `stock insuficiente en un ingrediente bloquea la venta`() {
        val receta = recetaCon("ins_nutella" to 20.0, "ins_masa" to 50.0)
        val stock = mapOf("ins_nutella" to 5.0, "ins_masa" to 200.0) // nutella insuficiente
        val res = StockManager.validarStockParaVenta(mockProducto, 1, receta, stock)
        assertTrue(res is StockManager.StockResult.Insuficiente)
        val insuf = res as StockManager.StockResult.Insuficiente
        assertEquals("ins_nutella", insuf.insumo)
        assertEquals(15.0, insuf.faltante, 0.01)
    }

    @Test
    fun `cantidad mayor multiplica el requerimiento correctamente`() {
        val receta = recetaCon("ins_masa" to 50.0)
        val stock = mapOf("ins_masa" to 80.0) // suficiente para 1, insuficiente para 2
        val res = StockManager.validarStockParaVenta(mockProducto, 2, receta, stock)
        assertTrue(res is StockManager.StockResult.Insuficiente)
        val insuf = res as StockManager.StockResult.Insuficiente
        assertEquals(20.0, insuf.faltante, 0.01)
    }

    @Test
    fun `insumo ausente en stock se trata como cero`() {
        val receta = recetaCon("ins_cajeta" to 15.0)
        val stock = emptyMap<String, Double>() // insumo no registrado
        val res = StockManager.validarStockParaVenta(mockProducto, 1, receta, stock)
        assertTrue(res is StockManager.StockResult.Insuficiente)
        val insuf = res as StockManager.StockResult.Insuficiente
        assertEquals(15.0, insuf.faltante, 0.01)
    }
}
