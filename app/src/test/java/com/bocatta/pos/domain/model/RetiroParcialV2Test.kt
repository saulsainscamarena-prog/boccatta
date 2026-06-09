package com.bocatta.pos.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class RetiroParcialV2Test {

    @Test
    fun `default values are set correctly`() {
        val retiro = RetiroParcialV2()
        assertEquals("", retiro.id)
        assertEquals("", retiro.turnoId)
        assertEquals(0.0, retiro.monto, 0.0)
        assertEquals("", retiro.motivo)
        assertEquals("", retiro.usuarioId)
        assertEquals("", retiro.usuarioNombre)
        assertEquals(0L, retiro.fecha)
    }

    @Test
    fun `construction with parameters works`() {
        val retiro = RetiroParcialV2(
            id = "r1",
            turnoId = "t1",
            monto = 123.45,
            motivo = "Test",
            usuarioId = "u1",
            usuarioNombre = "John Doe",
            fecha = 1620000000000L
        )
        assertEquals("r1", retiro.id)
        assertEquals("t1", retiro.turnoId)
        assertEquals(123.45, retiro.monto, 0.0)
        assertEquals("Test", retiro.motivo)
        assertEquals("u1", retiro.usuarioId)
        assertEquals("John Doe", retiro.usuarioNombre)
        assertEquals(1620000000000L, retiro.fecha)
    }
}
