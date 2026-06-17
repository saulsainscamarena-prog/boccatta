package com.bocatta.pos.domain.usecase

import android.content.Context
import com.bocatta.pos.domain.model.ItemCarritoV2
import com.bocatta.pos.domain.model.SalesInventoryProductV2
import io.mockk.mockk
import java.math.BigDecimal
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RegistrarCancelacionUseCaseTest {

    private val context = mockk<Context>(relaxed = true)

    @Test
    fun `cart cancellation persists complete audit payload`() {
        var savedId = ""
        var savedPayload = ""
        val useCase = RegistrarCancelacionUseCase(context) { _, operationId, dataJson ->
            savedId = operationId
            savedPayload = dataJson
        }

        val result = useCase(
            CancellationRequest(
                items = listOf(testItem()),
                scope = CancellationScope.CART,
                reason = "Cliente cambio de opinion",
                userId = "cashier-1",
                branch = "Atlixco"
            )
        )

        assertTrue(result is CancellationResult.Saved)
        val payload = Json.parseToJsonElement(savedPayload).jsonObject
        assertEquals(savedId, payload.getValue("id").jsonPrimitive.content)
        assertEquals("cart", payload.getValue("alcance").jsonPrimitive.content)
        assertEquals("cashier-1", payload.getValue("usuario").jsonPrimitive.content)
        assertEquals("atlixco", payload.getValue("sucursal").jsonPrimitive.content)
        assertEquals(90.0, payload.getValue("total").jsonPrimitive.double, 0.0)
        assertEquals(1, payload.getValue("items").jsonArray.size)
    }

    @Test
    fun `storage failure returns recoverable operator error`() {
        val useCase = RegistrarCancelacionUseCase(context) { _, _, _ ->
            error("database locked")
        }

        val result = useCase(
            CancellationRequest(
                items = listOf(testItem()),
                scope = CancellationScope.ITEM,
                reason = "Captura incorrecta",
                userId = "cashier-1",
                branch = "Atlixco"
            )
        )

        assertTrue(result is CancellationResult.Failed)
        assertTrue(
            (result as CancellationResult.Failed).operatorMessage.contains("se conserva")
        )
    }

    private fun testItem() = ItemCarritoV2(
        cartId = "cart-1",
        producto = SalesInventoryProductV2(id = "product-1", nombre = "Frappe"),
        precioFinal = BigDecimal("45.00"),
        cantidad = 2,
        nombre = "Frappe"
    )
}
