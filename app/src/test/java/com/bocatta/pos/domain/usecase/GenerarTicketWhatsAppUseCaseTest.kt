package com.bocatta.pos.domain.usecase

import com.bocatta.pos.domain.model.ItemCarritoV2
import com.bocatta.pos.domain.model.SalesInventoryProductV2
import java.math.BigDecimal
import org.junit.Assert.assertTrue
import org.junit.Test

class GenerarTicketWhatsAppUseCaseTest {
    private val useCase = GenerarTicketWhatsAppUseCase()

    @Test
    fun ticketIncludesTipAndOrderNoteWhenPresent() {
        val ticket = useCase(
            sucursal = "atlixco",
            items = listOf(
                ItemCarritoV2(
                    cartId = "cart_1",
                    producto = SalesInventoryProductV2(id = "prod_1", nombre = "Frappe"),
                    precioFinal = BigDecimal("50.00"),
                    cantidad = 1,
                    nombre = "Frappe"
                )
            ),
            codigoTicket = "ATL-1",
            total = 60.0,
            descuentoLealtad = 0.0,
            descuentoPromociones = 0.0,
            metodoPago = "Efectivo",
            propina = 10.0,
            notaOrden = "Sin popote"
        )

        assertTrue(ticket.contains("Propina: $10.00"))
        assertTrue(ticket.contains("Nota orden: Sin popote"))
        assertTrue(ticket.contains("*TOTAL: $60.00*"))
    }

    @Test
    fun ticketIncludesOperationModeWhenOffline() {
        val ticket = useCase(
            sucursal = "metepec",
            items = listOf(
                ItemCarritoV2(
                    cartId = "cart_1",
                    producto = SalesInventoryProductV2(id = "prod_1", nombre = "Crepa"),
                    precioFinal = BigDecimal("80.00"),
                    cantidad = 1,
                    nombre = "Crepa"
                )
            ),
            codigoTicket = "MET-1",
            total = 80.0,
            descuentoLealtad = 0.0,
            descuentoPromociones = 0.0,
            metodoPago = "Efectivo",
            modoOperacion = "Offline local - pendiente de sincronizar"
        )

        assertTrue(ticket.contains("Modo: Offline local - pendiente de sincronizar"))
    }

    @Test
    fun ticketIncludesManualDiscountWhenApplied() {
        val ticket = useCase(
            sucursal = "atlixco",
            items = listOf(
                ItemCarritoV2(
                    cartId = "cart_1",
                    producto = SalesInventoryProductV2(id = "prod_1", nombre = "Frappe"),
                    precioFinal = BigDecimal("100.00"),
                    cantidad = 1,
                    nombre = "Frappe"
                )
            ),
            codigoTicket = "ATL-2",
            total = 90.0,
            descuentoLealtad = 0.0,
            descuentoPromociones = 0.0,
            metodoPago = "Efectivo",
            descuentoManual = 10.0
        )

        assertTrue(ticket.contains("Desc. manual: -$10.00"))
        assertTrue(ticket.contains("*TOTAL: $90.00*"))
    }
}
