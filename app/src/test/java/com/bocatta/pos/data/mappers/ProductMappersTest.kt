package com.bocatta.pos.data.mappers

import com.bocatta.pos.domain.model.SalesInventoryProductV2
import org.junit.Assert.assertEquals
import org.junit.Test

class ProductMappersTest {

    @Test
    fun `legacy to domain to legacy round-trip preserves all fields`() {
        val original = SalesInventoryProductV2(
            id = "prod-123",
            nombre = "Taco al Pastor",
            emoji = "🌮",
            categoria = "Tacos",
            precioVenta = mapOf("default" to 25.0),
            esCombo = false,
            recetaId = "rec-001",
            toppingsIncluidos = 2,
            costoToppingExtra = 10.0,
            esProductoTopping = false,
            fotoUrl = "https://example.com/taco.jpg"
        )
        val domain = original.toDomain()
        val roundTrip = domain.toLegacy()
        assertEquals(original, roundTrip)
    }

    @Test
    fun `legacy combo to domain to legacy preserves esCombo`() {
        val original = SalesInventoryProductV2(
            id = "combo-1",
            nombre = "Combo Familiar",
            categoria = "Combos",
            precioVenta = mapOf("default" to 0.0),
            esCombo = true
        )
        val domain = original.toDomain()
        val roundTrip = domain.toLegacy()
        assertEquals(original, roundTrip)
        assertEquals(true, roundTrip.esCombo)
    }
}
