package com.bocatta.pos.domain.usecase

import com.bocatta.pos.domain.model.HeldOrder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HeldOrderMesaLinkPolicyTest {
    private val policy = HeldOrderMesaLinkPolicy()

    @Test
    fun guardarOrdenConMesa_ocupaMesaYVinculaOrden() {
        val order = HeldOrder(
            id = "held_1",
            sucursal = "atlixco",
            mesaId = "mesa_1",
            modalidad = "LOCAL"
        )

        val action = policy.alGuardar(order)

        assertEquals("mesa_1", action?.mesaId)
        assertEquals("OCUPADA", action?.estado)
        assertEquals("held_1", action?.ordenId)
    }

    @Test
    fun guardarOrdenSinMesa_noGeneraAccionDeMesa() {
        val order = HeldOrder(
            id = "held_fast",
            sucursal = "atlixco",
            mesaId = null,
            modalidad = "PARA_LLEVAR"
        )

        assertNull(policy.alGuardar(order))
    }

    @Test
    fun borrarOrdenCobrada_liberaMesa() {
        val order = HeldOrder(
            id = "held_2",
            sucursal = "atlixco",
            mesaId = "mesa_2",
            modalidad = "LOCAL"
        )

        val action = policy.alBorrar(order, liberarMesa = true)

        assertEquals("mesa_2", action?.mesaId)
        assertEquals("LIBRE", action?.estado)
        assertNull(action?.ordenId)
    }

    @Test
    fun borrarOrdenAnteriorDuranteReApartado_noLiberaMesa() {
        val order = HeldOrder(
            id = "held_old",
            sucursal = "atlixco",
            mesaId = "mesa_3",
            modalidad = "LOCAL"
        )

        assertNull(policy.alBorrar(order, liberarMesa = false))
    }
}
