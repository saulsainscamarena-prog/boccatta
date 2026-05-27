package com.bocatta.pos.domain.model

import org.junit.Test
import org.junit.Assert.*

class ActividadTest {

    @Test
    fun mesa_estadoInicialEsLibre() {
        val mesa = Mesa(
            id = "mesa_1",
            numero = 1,
            capacidad = 4,
            zonaId = "zona_interior",
            estado = EstadoMesa.LIBRE,
            ordenActual = null
        )

        assertEquals("mesa_1", mesa.id)
        assertEquals(1, mesa.numero)
        assertEquals(4, mesa.capacidad)
        assertEquals("zona_interior", mesa.zonaId)
        assertEquals(EstadoMesa.LIBRE, mesa.estado)
        assertNull(mesa.ordenActual)
    }

    @Test
    fun vincularOrdenAMesa_cambiaEstadoA_Ocupada() {
        val mesaLibre = Mesa(
            id = "mesa_2",
            numero = 2,
            capacidad = 2,
            zonaId = "terraza",
            estado = EstadoMesa.LIBRE,
            ordenActual = null
        )

        // Simular vinculación
        val ordenId = "orden_101"
        val mesaOcupada = mesaLibre.copy(
            estado = EstadoMesa.OCUPADA,
            ordenActual = ordenId
        )

        assertEquals(EstadoMesa.OCUPADA, mesaOcupada.estado)
        assertEquals(ordenId, mesaOcupada.ordenActual)
        assertEquals(mesaLibre.id, mesaOcupada.id)
        assertEquals(mesaLibre.numero, mesaOcupada.numero)
    }

    @Test
    fun moverVentaRapidaAMesa_actualizaOrdenYMesa() {
        // 1. Crear una orden rápida (sin mesaId, modalidad PARA_LLEVAR)
        val ventaRapida = HeldOrder(
            id = "held_fast_99",
            carritoJson = "[{\"cartId\":\"item_1\",\"productoId\":\"prod_1\",\"cantidad\":1}]",
            clienteJson = null,
            nota = "Para llevar rápido",
            fecha = System.currentTimeMillis(),
            sucursal = "Sucursal Principal",
            total = 120.0,
            modalidad = "PARA_LLEVAR",
            mesaId = null
        )

        // 2. Definir mesa de destino
        val mesaDestino = Mesa(
            id = "mesa_5",
            numero = 5,
            capacidad = 4,
            zonaId = "zona_interior",
            estado = EstadoMesa.LIBRE,
            ordenActual = null
        )

        // 3. Simular la acción "Mover a Mesa"
        // La orden rápida se convierte a modalidad LOCAL y se le asigna la mesa
        val ordenMovidaAMesa = ventaRapida.copy(
            mesaId = mesaDestino.id,
            modalidad = "LOCAL"
        )

        // La mesa se actualiza a OCUPADA vinculando el ID de la orden
        val mesaDestinoActualizada = mesaDestino.copy(
            estado = EstadoMesa.OCUPADA,
            ordenActual = ordenMovidaAMesa.id
        )

        // 4. Verificaciones
        assertNull(ventaRapida.mesaId)
        assertEquals("PARA_LLEVAR", ventaRapida.modalidad)

        assertEquals("mesa_5", ordenMovidaAMesa.mesaId)
        assertEquals("LOCAL", ordenMovidaAMesa.modalidad)
        assertEquals(ventaRapida.id, ordenMovidaAMesa.id)
        assertEquals(ventaRapida.total, ordenMovidaAMesa.total, 0.0)

        assertEquals(EstadoMesa.OCUPADA, mesaDestinoActualizada.estado)
        assertEquals(ordenMovidaAMesa.id, mesaDestinoActualizada.ordenActual)
    }

    @Test
    fun liberarMesa_restableceEstadoALibre() {
        val mesaOcupada = Mesa(
            id = "mesa_3",
            numero = 3,
            capacidad = 6,
            zonaId = "terraza",
            estado = EstadoMesa.OCUPADA,
            ordenActual = "orden_456"
        )

        // Simular liberación (por cobro exitoso o cancelación)
        val mesaLiberada = mesaOcupada.copy(
            estado = EstadoMesa.LIBRE,
            ordenActual = null
        )

        assertEquals(EstadoMesa.LIBRE, mesaLiberada.estado)
        assertNull(mesaLiberada.ordenActual)
    }
}
