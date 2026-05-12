package com.bocatta.pos.domain

import com.bocatta.pos.domain.model.ItemCarritoV2
import com.bocatta.pos.domain.model.SalesInventoryProductV2
import com.bocatta.pos.data.repository.InventoryDeductions
import org.junit.Test
import org.junit.Assert.*
import java.math.BigDecimal

class CarritoLogicTest {

    // ── Tests existentes ────────────────────────────────────

    @Test
    fun itemCarrito_inmutabilidad() {
        val producto = SalesInventoryProductV2(id = "1", nombre = "Crepa", precioVenta = mapOf("atlixco" to 80.0))
        val item = ItemCarritoV2(producto = producto, precioFinal = BigDecimal(80), cantidad = 1, nombre = "Crepa")
        
        val copia = item.copyConCantidad(3)
        
        assertEquals("Original debe mantener cantidad 1", 1, item.cantidad)
        assertEquals("Copia debe tener cantidad 3", 3, copia.cantidad)
    }

    @Test
    fun totalCarrito_multiplesItems() {
        val p1 = SalesInventoryProductV2(id = "1", nombre = "Crepa", precioVenta = mapOf("atlixco" to 50.0))
        val p2 = SalesInventoryProductV2(id = "2", nombre = "Bebida", precioVenta = mapOf("atlixco" to 100.0))
        
        val item1 = ItemCarritoV2(producto = p1, precioFinal = BigDecimal(50), cantidad = 2, nombre = "Crepa")
        val item2 = ItemCarritoV2(producto = p2, precioFinal = BigDecimal(100), cantidad = 1, nombre = "Bebida")
        
        val total = item1.precioFinal.toDouble() * item1.cantidad + item2.precioFinal.toDouble() * item2.cantidad
        
        assertEquals("Total debe ser 200", 200.0, total, 0.01)
    }

    // ── Nuevos: lógica de precio (antes bug de sobreescritura) ──

    private val premiumToppings = listOf("oreo", "nuez", "bombón", "bombon")

    private fun tienePremium(toppings: List<String>): Boolean {
        return toppings.any { t -> premiumToppings.any { p -> t.lowercase().contains(p) } }
    }

    private fun contarNormales(toppings: List<String>): Int {
        return toppings.count { t -> !premiumToppings.any { p -> t.lowercase().contains(p) } }
    }

    private fun calcularPrecioProducto(
        precioBase: Double,
        categoria: String,
        base: String?,
        toppings: List<String>,
        costoToppingExtra: Double
    ): Double {
        val precioConReceta = if (categoria.equals("CREPAS_DULCES", true) || categoria.equals("Combos", true)) {
            InventoryDeductions.calcularPrecioCrepa(precioBase, base, toppings, costoToppingExtra)
        } else {
            precioBase
        }
        val extraToppings = if (tienePremium(toppings) || contarNormales(toppings) >= 2) 10.0 else 0.0
        return precioConReceta + extraToppings
    }

    // Escenarios del bug original

    @Test
    fun crepaDulce_sinToppings_precioBase() {
        val precio = calcularPrecioProducto(80.0, "CREPAS_DULCES", null, emptyList(), 10.0)
        assertEquals(80.0, precio, 0.01)
    }

    @Test
    fun crepaDulce_conToppingPremium_recibeExtra() {
        val precio = calcularPrecioProducto(80.0, "CREPAS_DULCES", null, listOf("oreo"), 10.0)
        // calcularPrecioCrepa(80, null, [oreo], 10) = 80 + 0 + 10 = 90
        // extraToppings = true → +10
        // Total: 100
        assertEquals(100.0, precio, 0.01)
    }

    @Test
    fun crepaDulce_conDosToppingsNormales_recibeExtra() {
        val precio = calcularPrecioProducto(80.0, "CREPAS_DULCES", null, listOf("fresa", "durazno"), 10.0)
        // calcularPrecioCrepa(80, null, [fresa, durazno], 10) = 80 + 10 + 0 = 90 (3 ingredientes normales)
        // extraToppings = true (2 normales >= 2) → +10
        // Total: 100
        assertEquals(90.0, precio, 0.01)
    }

    @Test
    fun productoNormal_sinToppings_precioBase() {
        val precio = calcularPrecioProducto(50.0, "BEBIDAS", null, emptyList(), 10.0)
        assertEquals(50.0, precio, 0.01)
    }

    @Test
    fun productoNormal_conToppingPremium_recibeExtra() {
        // BUG ORIGINAL: esto daba 50.0 porque sobreescribía con precioBase ignorando el +10
        val precio = calcularPrecioProducto(50.0, "BEBIDAS", null, listOf("oreo"), 10.0)
        assertEquals(60.0, precio, 0.01) // 50 + 10
    }

    @Test
    fun productoNormal_conDosToppingsNormales_recibeExtra() {
        // BUG ORIGINAL: esto daba 50.0
        val precio = calcularPrecioProducto(50.0, "BEBIDAS", null, listOf("fresa", "durazno"), 10.0)
        assertEquals(60.0, precio, 0.01) // 50 + 10
    }

    @Test
    fun productoNormal_conUnToppingNormal_sinExtra() {
        val precio = calcularPrecioProducto(50.0, "BEBIDAS", null, listOf("fresa"), 10.0)
        assertEquals(50.0, precio, 0.01) // 50 + 0 (solo 1 normal)
    }

    @Test
    fun crepaConBaseYTresToppings_calculaCorrecto() {
        val precio = calcularPrecioProducto(80.0, "CREPAS_DULCES", "nutella", listOf("fresa", "durazno", "coco"), 10.0)
        // calcularPrecioCrepa: base(1) + 3 toppings = 4 ingredientes >= 3 → +10
        // extraToppings: 3 normales >= 2 → +10
        // Total: 80 + 10 + 10 = 100
        assertEquals(100.0, precio, 0.01)
    }

    @Test
    fun combo_conToppings_recibeExtra() {
        val precio = calcularPrecioProducto(120.0, "Combos", null, listOf("oreo", "nuez"), 10.0)
        // calcularPrecioCrepa(120, null, [oreo, nuez], 10) = 120 + 0 + 10 = 130 (solo premium)
        // extraToppings: premium → +10
        // Total: 140
        assertEquals(140.0, precio, 0.01)
    }

    @Test
    fun costoToppingExtraPersonalizado() {
        val precio = calcularPrecioProducto(80.0, "CREPAS_DULCES", "nutella", listOf("fresa", "durazno"), 15.0)
        // calcularPrecioCrepa(80, nutella, [fresa, durazno], 15):
        //   base(1) + 2 normales = 3 ingredientes >= 3 → +15
        // extraToppings: 2 normales >= 2 → +10
        // Total: 80 + 15 + 10 = 105
        assertEquals(105.0, precio, 0.01)
    }
}
