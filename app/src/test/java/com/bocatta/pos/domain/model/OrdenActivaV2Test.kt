package com.bocatta.pos.domain.model

import org.junit.Test
import org.junit.Assert.*
import java.math.BigDecimal

class OrdenActivaV2Test {

    @Test
    fun ordenActiva_construccionConValoresPorDefecto() {
        val orden = OrdenActivaV2()
        assertEquals("", orden.id)
        assertEquals(ModalidadOrden.LOCAL, orden.modalidad)
        assertNull(orden.mesaId)
        assertNull(orden.cliente)
        assertTrue(orden.items.isEmpty())
        assertEquals("", orden.nota)
        assertEquals(BigDecimal.ZERO, orden.descuentoManual)
        assertEquals(BigDecimal.ZERO, orden.descuentoLealtad)
        assertFalse(orden.esConsumoEmpleado)
    }

    @Test
    fun ordenActiva_construccionConValoresPersonalizados() {
        val cliente = ClienteV2(idDocumento = "cli_123", nombre = "Juan Perez")
        val producto = SalesInventoryProductV2(id = "prod_1", nombre = "Crepa Chocolate", precioVenta = mapOf("general" to 75.0))
        val item = ItemCarritoV2(cartId = "cart_1", producto = producto, precioFinal = BigDecimal.valueOf(75.0), cantidad = 2, paraLlevar = true)

        val orden = OrdenActivaV2(
            id = "orden_999",
            modalidad = ModalidadOrden.PARA_LLEVAR,
            mesaId = "mesa_5",
            cliente = cliente,
            items = listOf(item),
            nota = "Sin cebolla",
            descuentoManual = BigDecimal.valueOf(10.0),
            descuentoLealtad = BigDecimal.valueOf(5.0),
            esConsumoEmpleado = true
        )

        assertEquals("orden_999", orden.id)
        assertEquals(ModalidadOrden.PARA_LLEVAR, orden.modalidad)
        assertEquals("mesa_5", orden.mesaId)
        assertEquals(cliente, orden.cliente)
        assertEquals(1, orden.items.size)
        assertTrue(orden.items.first().paraLlevar)
        assertEquals("Sin cebolla", orden.nota)
        assertEquals(BigDecimal.valueOf(10.0), orden.descuentoManual)
        assertEquals(BigDecimal.valueOf(5.0), orden.descuentoLealtad)
        assertTrue(orden.esConsumoEmpleado)
    }

    @Test
    fun itemCarrito_defaultParaLlevar() {
        val producto = SalesInventoryProductV2(id = "prod_1", nombre = "Crepa Chocolate", precioVenta = mapOf("general" to 75.0))
        val item = ItemCarritoV2(cartId = "cart_1", producto = producto, precioFinal = BigDecimal.valueOf(75.0))
        assertFalse(item.paraLlevar)

        val itemLlevar = item.copy(paraLlevar = true)
        assertTrue(itemLlevar.paraLlevar)
    }

    @Test
    fun heldOrder_serializationAndDeserialization() {
        val order = HeldOrder(
            id = "held_123",
            carritoJson = "[]",
            clienteJson = null,
            nota = "Prueba",
            fecha = 123456L,
            sucursal = "Sucursal Central",
            total = 150.0,
            modalidad = "DELIVERY",
            mesaId = "mesa_10"
        )

        val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
        val serialized = json.encodeToString(HeldOrder.serializer(), order)
        val deserialized = json.decodeFromString(HeldOrder.serializer(), serialized)

        assertEquals("held_123", deserialized.id)
        assertEquals("DELIVERY", deserialized.modalidad)
        assertEquals("mesa_10", deserialized.mesaId)
        assertEquals("Prueba", deserialized.nota)
        assertEquals(150.0, deserialized.total, 0.0)
    }
}
