package com.bocatta.pos.data.repository

import com.bocatta.pos.domain.model.IngredienteReceta
import com.bocatta.pos.domain.model.ItemCarritoV2
import com.bocatta.pos.domain.model.SalesInventoryProductV2
import org.junit.Test
import org.junit.Assert.*
import java.math.BigDecimal

class InventoryDeductionsTest {

    @Test
    fun calcularPrecioCrepa_sinBase_sinToppings_esPrecioBase() {
        val precio = InventoryDeductions.calcularPrecioCrepa(100.0, null, emptyList())
        assertEquals(100.0, precio, 0.01)
    }

    @Test
    fun calcularPrecioCrepa_conBase_sinToppings_esPrecioBase() {
        val precio = InventoryDeductions.calcularPrecioCrepa(100.0, "nutella", emptyList())
        assertEquals(100.0, precio, 0.01)
    }

    @Test
    fun calcularPrecioCrepa_unToppingNormal_sinCargo() {
        val precio = InventoryDeductions.calcularPrecioCrepa(100.0, "nutella", listOf("fresa"))
        assertEquals(100.0, precio, 0.01)
    }

    @Test
    fun calcularPrecioCrepa_dosToppingsNormales_sinCargo() {
        val precio = InventoryDeductions.calcularPrecioCrepa(100.0, null, listOf("fresa", "durazno"))
        assertEquals(100.0, precio, 0.01)
    }

    @Test
    fun calcularPrecioCrepa_tresToppingsNormales_cargoExtra() {
        val precio = InventoryDeductions.calcularPrecioCrepa(100.0, null, listOf("fresa", "durazno", "coco"))
        assertEquals(110.0, precio, 0.01)
    }

    @Test
    fun calcularPrecioCrepa_toppingPremium_cargoPremium() {
        val precio = InventoryDeductions.calcularPrecioCrepa(100.0, null, listOf("oreo"))
        assertEquals(110.0, precio, 0.01)
    }

    @Test
    fun calcularPrecioCrepa_baseMasDosNormales_sinCargo() {
        val precio = InventoryDeductions.calcularPrecioCrepa(100.0, "nutella", listOf("fresa"))
        assertEquals(100.0, precio, 0.01)
    }

    @Test
    fun calcularPrecioCrepa_baseMasTresIngredientes_cargoNormal() {
        val precio = InventoryDeductions.calcularPrecioCrepa(100.0, "nutella", listOf("fresa", "durazno", "coco"))
        assertEquals(110.0, precio, 0.01)
    }

    @Test
    fun calcularPrecioCrepa_baseMasPremium_y_dosNormales_soloCargoPremium() {
        val precio = InventoryDeductions.calcularPrecioCrepa(100.0, "nutella", listOf("oreo", "fresa"))
        assertEquals(110.0, precio, 0.01)
    }

    @Test
    fun esPremium_oreo_returnsTrue() {
        assertTrue(InventoryDeductions.esPremium("oreo"))
        assertTrue(InventoryDeductions.esPremium("Oreo"))
        assertTrue(InventoryDeductions.esPremium("OREO"))
    }

    @Test
    fun esPremium_nuez_returnsTrue() {
        assertTrue(InventoryDeductions.esPremium("nuez"))
        assertTrue(InventoryDeductions.esPremium("Nuez"))
    }

    @Test
    fun esPremium_bombon_returnsTrue() {
        assertTrue(InventoryDeductions.esPremium("bombón"))
        assertTrue(InventoryDeductions.esPremium("Bombon"))
    }

    @Test
    fun esPremium_fresa_returnsFalse() {
        assertFalse(InventoryDeductions.esPremium("fresa"))
    }

    @Test
    fun mapearBaseAInsumo_nutella_returnsNutella() {
        val result = InventoryDeductions.mapearBaseAInsumo("nutella")
        assertNotNull(result)
        assertEquals("nutella_kg", result!!.first)
        assertEquals(20.0, result.second, 0.01)
    }

    @Test
    fun mapearBaseAInsumo_quesoCrema_returnsQuesoCrema() {
        val result = InventoryDeductions.mapearBaseAInsumo("queso crema")
        assertNotNull(result)
        assertEquals("queso_crema_kg", result!!.first)
    }

    @Test
    fun mapearBaseAInsumo_zarzamora_returnsZarzamora() {
        val result = InventoryDeductions.mapearBaseAInsumo("zarzamora")
        assertNotNull(result)
        assertEquals("zarzamora_kg", result!!.first)
    }

    @Test
    fun mapearBaseAInsumo_desconocido_returnsNull() {
        assertNull(InventoryDeductions.mapearBaseAInsumo("chocolate"))
    }

    @Test
    fun mapearTopping_fresa_returnsFresas() {
        val result = InventoryDeductions.mapearToppingOAderezoAInsumo("fresa")
        assertNotNull(result)
        assertEquals("fresas", result!!.first)
        assertEquals(50.0, result.second, 0.01)
    }

    @Test
    fun mapearTopping_oreo_returnsOreo() {
        val result = InventoryDeductions.mapearToppingOAderezoAInsumo("oreo")
        assertNotNull(result)
        assertEquals("oreo", result!!.first)
        assertEquals(6.0, result.second, 0.01)
    }

    @Test
    fun mapearTopping_jamon_returnsJamon() {
        val result = InventoryDeductions.mapearToppingOAderezoAInsumo("jamón")
        assertNotNull(result)
        assertEquals("jamon_kg", result!!.first)
    }

    @Test
    fun mapearTopping_pepperoni_variantes_returnsPeperoni() {
        assertEquals("peperoni_kg", InventoryDeductions.mapearToppingOAderezoAInsumo("pepperoni")!!.first)
        assertEquals("peperoni_kg", InventoryDeductions.mapearToppingOAderezoAInsumo("peperoni")!!.first)
    }

    @Test
    fun mapearTopping_blueCheese_returnBlueCheese() {
        assertEquals("blue_cheese", InventoryDeductions.mapearToppingOAderezoAInsumo("blue cheese")!!.first)
        assertEquals("blue_cheese", InventoryDeductions.mapearToppingOAderezoAInsumo("blue chesse")!!.first)
    }

    @Test
    fun mapearTopping_desconocido_returnsNull() {
        assertNull(InventoryDeductions.mapearToppingOAderezoAInsumo("trufa"))
    }

    @Test
    fun convertirAUnidadBase_kg_aGramos() {
        assertEquals(2000.0, InventoryDeductions.convertirAUnidadBase(2.0, "kg"), 0.01)
    }

    @Test
    fun convertirAUnidadBase_litros_aMililitros() {
        assertEquals(1500.0, InventoryDeductions.convertirAUnidadBase(1.5, "l"), 0.01)
        assertEquals(1500.0, InventoryDeductions.convertirAUnidadBase(1.5, "lt"), 0.01)
    }

    @Test
    fun convertirAUnidadBase_taza_aMililitros() {
        assertEquals(480.0, InventoryDeductions.convertirAUnidadBase(2.0, "tz"), 0.01)
    }

    @Test
    fun convertirAUnidadBase_cucharada_aMililitros() {
        assertEquals(30.0, InventoryDeductions.convertirAUnidadBase(2.0, "cd"), 0.01)
    }

    @Test
    fun convertirAUnidadBase_unidadSinConversion_returnsMisma() {
        assertEquals(5.0, InventoryDeductions.convertirAUnidadBase(5.0, "pz"), 0.01)
        assertEquals(3.0, InventoryDeductions.convertirAUnidadBase(3.0, "unidad"), 0.01)
    }

    @Test
    fun calcularParaItem_conRecetaYToppings_agrupaDeducciones() {
        val item = ItemCarritoV2(
            producto = SalesInventoryProductV2(id = "p1", nombre = "Crepa", esCombo = false),
            precioFinal = BigDecimal.valueOf(100),
            cantidad = 2,
            toppings = listOf("fresa", "oreo")
        )
        val ingredientes = listOf(
            IngredienteReceta(insumoId = "harina", nombreInsumo = "Harina", cantidad = 100.0, unidad = "g"),
            IngredienteReceta(insumoId = "huevo", nombreInsumo = "Huevo", cantidad = 1.0, unidad = "pz")
        )
        val ded = InventoryDeductions.calcularParaItem(item, ingredientes)
        assertEquals(200.0, ded["harina"]!!, 0.01)
        assertEquals(2.0, ded["huevo"]!!, 0.01)
        assertEquals(100.0, ded["fresas"]!!, 0.01)
        assertEquals(12.0, ded["oreo"]!!, 0.01)
    }

    @Test
    fun calcularParaItem_conBase_agregaBaseDeduccion() {
        val item = ItemCarritoV2(
            producto = SalesInventoryProductV2(id = "p1", nombre = "Crepa", esCombo = false),
            precioFinal = BigDecimal.valueOf(100),
            base = "nutella"
        )
        val ded = InventoryDeductions.calcularParaItem(item, emptyList())
        assertEquals(20.0, ded["nutella_kg"]!!, 0.01)
    }

    @Test
    fun calcularParaItem_conAderezo_agregaAderezoDeduccion() {
        val item = ItemCarritoV2(
            producto = SalesInventoryProductV2(id = "p1", nombre = "Crepa", esCombo = false),
            precioFinal = BigDecimal.valueOf(100),
            aderezo = "chocolate"
        )
        val ded = InventoryDeductions.calcularParaItem(item, emptyList())
        assertEquals(20.0, ded["granillo_chocolate"]!!, 0.01)
    }

    @Test
    fun calcularParaItem_separadoYCombo_agregaCharolaYPapel() {
        val item = ItemCarritoV2(
            producto = SalesInventoryProductV2(id = "p1", nombre = "Combo", esCombo = true),
            precioFinal = BigDecimal.valueOf(100),
            esSeparado = true
        )
        val ded = InventoryDeductions.calcularParaItem(item, emptyList())
        assertEquals(1.0, ded["charola"]!!, 0.01)
        assertEquals(1.0, ded["papel_hamburguesero"]!!, 0.01)
    }

    @Test
    fun calcularParaItem_separadoPeroNoCombo_noAgregaCharola() {
        val item = ItemCarritoV2(
            producto = SalesInventoryProductV2(id = "p1", nombre = "Simple", esCombo = false),
            precioFinal = BigDecimal.valueOf(100),
            esSeparado = true
        )
        val ded = InventoryDeductions.calcularParaItem(item, emptyList())
        assertNull(ded["charola"])
        assertNull(ded["papel_hamburguesero"])
    }

    @Test
    fun calcularParaItem_conMultiplesToppingsMismos_agrupaCantidad() {
        val item = ItemCarritoV2(
            producto = SalesInventoryProductV2(id = "p1", nombre = "Frutal", esCombo = false),
            precioFinal = BigDecimal.valueOf(100),
            toppings = listOf("fresa", "fresa", "durazno")
        )
        val ded = InventoryDeductions.calcularParaItem(item, emptyList())
        assertEquals(100.0, ded["fresas"]!!, 0.01)
        assertEquals(50.0, ded["durazno_kg"]!!, 0.01)
    }

    @Test
    fun calcularParaItem_toppingSinMapeo_noGeneraDeduccion() {
        val item = ItemCarritoV2(
            producto = SalesInventoryProductV2(id = "p1", nombre = "Test", esCombo = false),
            precioFinal = BigDecimal.valueOf(100),
            toppings = listOf("desconocido_xyz")
        )
        val ded = InventoryDeductions.calcularParaItem(item, emptyList())
        assertTrue(ded.isEmpty())
    }

    @Test
    fun normalizado_manejaTildes() {
        val result = InventoryDeductions.mapearBaseAInsumo("Nutella")
        assertNotNull(result)
    }

    @Test
    fun normalizado_manejaMayusculas() {
        val result = InventoryDeductions.mapearToppingOAderezoAInsumo("JAMÓN")
        assertNotNull(result)
        assertEquals("jamon_kg", result!!.first)
    }

    @Test
    fun calcularPrecioCrepa_conExtraPersonalizado() {
        val precio = InventoryDeductions.calcularPrecioCrepa(80.0, null, listOf("fresa", "durazno", "coco"), extra = 15.0)
        assertEquals(95.0, precio, 0.01)
    }

    @Test
    fun mapaInsumo_conNombreVacio_devuelveNull() {
        assertNull(InventoryDeductions.mapearBaseAInsumo(""))
        assertNull(InventoryDeductions.mapearToppingOAderezoAInsumo(""))
    }

    @Test
    fun calcularPrecioCrepa_sinToppings_usaPrecioBase() {
        assertEquals(80.0, InventoryDeductions.calcularPrecioCrepa(80.0, "base_nutella", listOf()), 0.01)
    }

    @Test
    fun convertirAUnidadBase_valorNegativo_devuelveNegativo() {
        assertEquals(-5.0, InventoryDeductions.convertirAUnidadBase(-5.0, "porciones"), 0.01)
    }
}
