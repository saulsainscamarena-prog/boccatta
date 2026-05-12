package com.bocatta.pos.domain.unit

import org.junit.Test
import org.junit.Assert.*

class UnitConverterTest {

    @Test
    fun toBase_kgToGramos() {
        val result = UnitConverter.toBase(1.5, "kg")
        assertEquals(1500.0, result, 0.001)
    }

    @Test
    fun toBase_gramosSeMantienen() {
        val result = UnitConverter.toBase(500.0, "g")
        assertEquals(500.0, result, 0.001)
    }

    @Test
    fun toBase_litrosAMililitros() {
        val result = UnitConverter.toBase(2.0, "L")
        assertEquals(2000.0, result, 0.001)
    }

    @Test
    fun toBase_mililitrosSeMantienen() {
        val result = UnitConverter.toBase(250.0, "ml")
        assertEquals(250.0, result, 0.001)
    }

    @Test
    fun toBase_docenaAPiezas() {
        val result = UnitConverter.toBase(2.0, "docena")
        assertEquals(24.0, result, 0.001)
    }

    @Test
    fun toBase_piezasSeMantienen() {
        val result = UnitConverter.toBase(5.0, "pza")
        assertEquals(5.0, result, 0.001)
    }

    @Test(expected = IllegalArgumentException::class)
    fun toBase_unidadNoSoportada_lanzaExcepcion() {
        UnitConverter.toBase(1.0, "tonelada")
    }

    @Test
    fun fromBase_gramosAKg() {
        val result = UnitConverter.fromBase(1500.0, "kg")
        assertEquals(1.5, result, 0.001)
    }

    @Test
    fun fromBase_mililitrosALitros() {
        val result = UnitConverter.fromBase(2000.0, "L")
        assertEquals(2.0, result, 0.001)
    }

    @Test
    fun fromBase_piezasADocenas() {
        val result = UnitConverter.fromBase(24.0, "docena")
        assertEquals(2.0, result, 0.001)
    }

    @Test
    fun convert_kgALb_redondeo() {
        val result = UnitConverter.convert(1.0, "kg", "lb")
        assertEquals(2.20462, result, 0.01)
    }

    @Test
    fun convert_kgAGramos() {
        val result = UnitConverter.convert(2.0, "kg", "g")
        assertEquals(2000.0, result, 0.001)
    }

    @Test(expected = IllegalArgumentException::class)
    fun convert_dimensionesDistintas_lanzaExcepcion() {
        UnitConverter.convert(1.0, "kg", "L")
    }

    @Test
    fun isUnitSupported_devuelveTrueParaUnidadesRegistradas() {
        assertTrue(UnitConverter.isUnitSupported("g"))
        assertTrue(UnitConverter.isUnitSupported("kg"))
        assertTrue(UnitConverter.isUnitSupported("ml"))
        assertTrue(UnitConverter.isUnitSupported("L"))
        assertTrue(UnitConverter.isUnitSupported("pza"))
        assertTrue(UnitConverter.isUnitSupported("docena"))
        assertTrue(UnitConverter.isUnitSupported("lb"))
    }

    @Test
    fun isUnitSupported_devuelveFalseParaUnidadesNoRegistradas() {
        assertFalse(UnitConverter.isUnitSupported("oz"))
        assertFalse(UnitConverter.isUnitSupported("ton"))
    }

    @Test
    fun getDimension_masa() {
        assertEquals(UnitConverter.Dimension.MASA, UnitConverter.getDimension("g"))
        assertEquals(UnitConverter.Dimension.MASA, UnitConverter.getDimension("kg"))
        assertEquals(UnitConverter.Dimension.MASA, UnitConverter.getDimension("lb"))
    }

    @Test
    fun getDimension_volumen() {
        assertEquals(UnitConverter.Dimension.VOLUMEN, UnitConverter.getDimension("ml"))
        assertEquals(UnitConverter.Dimension.VOLUMEN, UnitConverter.getDimension("L"))
    }

    @Test
    fun getDimension_contable() {
        assertEquals(UnitConverter.Dimension.CONTABLE, UnitConverter.getDimension("pza"))
        assertEquals(UnitConverter.Dimension.CONTABLE, UnitConverter.getDimension("docena"))
    }

    @Test
    fun getDimension_unidadNoSoportada_devuelveNull() {
        assertNull(UnitConverter.getDimension("galon"))
    }

    @Test
    fun getBaseUnit_masa_returnsG() {
        assertEquals("g", UnitConverter.getBaseUnit("kg"))
        assertEquals("g", UnitConverter.getBaseUnit("g"))
    }

    @Test
    fun getBaseUnit_volumen_returnsMl() {
        assertEquals("ml", UnitConverter.getBaseUnit("L"))
        assertEquals("ml", UnitConverter.getBaseUnit("ml"))
    }

    @Test
    fun getBaseUnit_contable_returnsPza() {
        assertEquals("pza", UnitConverter.getBaseUnit("docena"))
        assertEquals("pza", UnitConverter.getBaseUnit("pza"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun getBaseUnit_unidadNoSoportada_lanzaExcepcion() {
        UnitConverter.getBaseUnit("galon")
    }

    @Test
    fun convert_lbAKg_precision() {
        val result = UnitConverter.convert(1.0, "lb", "kg")
        assertEquals(0.45359, result, 0.001)
    }

    @Test
    fun toBase_lbAGramos() {
        val result = UnitConverter.toBase(1.0, "lb")
        assertEquals(453.592, result, 0.001)
    }
}
