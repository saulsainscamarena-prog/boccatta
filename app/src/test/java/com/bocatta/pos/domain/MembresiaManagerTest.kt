package com.bocatta.pos.domain

import org.junit.Assert.*
import org.junit.Test

class MembresiaManagerTest {
    @Test
    fun `visita 5 es premio y nivel plata`() {
        val res = MembresiaManager.verificarEstadoMembresia(5)
        assertTrue("La visita 5 debe ser premio en ciclo de 5", res.esElegiblePremio)
        assertEquals("Plata", "PLATA", res.nivel)
    }

    @Test
    fun `visita 10 es premio y nivel plata`() {
        val res = MembresiaManager.verificarEstadoMembresia(10)
        assertTrue("La visita 10 debe ser premio en ciclo de 5", res.esElegiblePremio)
        assertEquals("Plata", "PLATA", res.nivel)
    }

    @Test
    fun `visita 3 no es premio`() {
        val res = MembresiaManager.verificarEstadoMembresia(3)
        assertFalse("La visita 3 no debe ser premio", res.esElegiblePremio)
        assertEquals(2, res.visitasRestantesParaPremio)
    }

    @Test
    fun `visita 25 es nivel oro`() {
        val res = MembresiaManager.verificarEstadoMembresia(25)
        assertEquals("ORO", res.nivel)
    }
}
