package com.bocatta.pos.domain.util

import com.bocatta.pos.domain.model.Rol
import com.bocatta.pos.domain.model.MetodoPago
import com.bocatta.pos.domain.model.TipoCombo
import org.junit.Assert.assertEquals
import org.junit.Test

class EnumUtilsTest {

    @Test
    fun matchesByName() {
        assertEquals(Rol.ADMIN, fromFirestoreKey("ADMIN", Rol.VENDEDOR))
        assertEquals(Rol.VENDEDOR, fromFirestoreKey("vendedor", Rol.ADMIN))
    }

    @Test
    fun matchesByFirestoreKey() {
        assertEquals(Rol.ADMIN, fromFirestoreKey("admin", Rol.VENDEDOR))
        assertEquals(Rol.VENDEDOR, fromFirestoreKey("vendedor", Rol.ADMIN))
    }

    @Test
    fun null_returnsDefault() {
        assertEquals(Rol.VENDEDOR, fromFirestoreKey(null, Rol.VENDEDOR))
    }

    @Test
    fun unknown_returnsDefault() {
        assertEquals(Rol.ADMIN, fromFirestoreKey("inexistente", Rol.ADMIN))
    }

    @Test
    fun metodoPago() {
        assertEquals(MetodoPago.EFECTIVO, fromFirestoreKey("efectivo", MetodoPago.TARJETA))
        assertEquals(MetodoPago.TARJETA, fromFirestoreKey("tarjeta", MetodoPago.EFECTIVO))
        assertEquals(MetodoPago.EFECTIVO, fromFirestoreKey("EFECTIVO", MetodoPago.TARJETA))
    }

    @Test
    fun tipoCombo() {
        assertEquals(TipoCombo.FIJO, fromFirestoreKey("fijo", TipoCombo.CONFIGURABLE))
        assertEquals(TipoCombo.CONFIGURABLE, fromFirestoreKey("configurable", TipoCombo.FIJO))
    }
}
