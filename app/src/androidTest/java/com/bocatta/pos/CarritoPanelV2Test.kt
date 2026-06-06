package com.bocatta.pos

import androidx.compose.foundation.layout.height
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bocatta.pos.domain.model.*
import com.bocatta.pos.presentation.ui.components.CarritoPanelV2
import org.junit.Rule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigDecimal

@RunWith(AndroidJUnit4::class)
class CarritoPanelV2Test {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val testItem = ItemCarritoV2(
        producto = SalesInventoryProductV2(
            id = "test_1",
            nombre = "Crepa Dulce",
            categoria = "CREPAS DULCES",
            precioVenta = mapOf("atlixco" to 25.0)
        ),
        precioFinal = BigDecimal.valueOf(25.0),
        cantidad = 1,
        nombre = "Crepa Dulce"
    )

    @Test
    fun carritoVacio_muestraMensaje() {
        composeTestRule.setContent {
            CarritoPanelV2(
                carrito = emptyList(),
                totalCarrito = BigDecimal.ZERO,
                descuentoLealtad = 0.0,
                descuentoPromociones = 0.0,
                clienteSeleccionado = null,
                modalidad = ModalidadOrden.LOCAL,
                onModalidadChanged = {},
                onToggleParaLlevarItem = {},
                modifier = Modifier.height(700.dp),
                onEliminarItem = {},
                onCobrar = {}
            )
        }
        composeTestRule.onNodeWithText("ORDEN VACIA", useUnmergedTree = true).assertExists()
    }

    @Test
    fun carritoConItem_muestraNombre() {
        composeTestRule.setContent {
            CarritoPanelV2(
                carrito = listOf(testItem),
                totalCarrito = BigDecimal.valueOf(25.0),
                descuentoLealtad = 0.0,
                descuentoPromociones = 0.0,
                clienteSeleccionado = null,
                modalidad = ModalidadOrden.LOCAL,
                onModalidadChanged = {},
                onToggleParaLlevarItem = {},
                onEliminarItem = {},
                onCobrar = {}
            )
        }
        // Verificar que se muestra el nombre del producto
        composeTestRule.onNodeWithText("CREPA DULCE").assertExists()
        // Verificar que el boton de finalizar esta habilitado
        composeTestRule.onNodeWithText("COBRAR").assertIsEnabled()
    }

    @Test
    fun carritoConItem_eliminar_muestraDialogo() {
        var itemEliminado: ItemCarritoV2? = null

        composeTestRule.setContent {
            CarritoPanelV2(
                carrito = listOf(testItem),
                totalCarrito = BigDecimal.valueOf(25.0),
                descuentoLealtad = 0.0,
                descuentoPromociones = 0.0,
                clienteSeleccionado = null,
                modalidad = ModalidadOrden.LOCAL,
                onModalidadChanged = {},
                onToggleParaLlevarItem = {},
                onEliminarItem = { itemEliminado = it },
                onCobrar = {}
            )
        }
        // Tocar el boton de eliminar (icono de cerrar)
        composeTestRule.onNodeWithContentDescription("Eliminar Crepa Dulce").performClick()
        assertNotNull(itemEliminado)
    }

    @Test
    fun carritoConItem_aplicaDescuento() {
        var descuentoAplicado = 0

        composeTestRule.setContent {
            CarritoPanelV2(
                carrito = listOf(testItem),
                totalCarrito = BigDecimal.valueOf(25.0),
                descuentoLealtad = 0.0,
                descuentoPromociones = 0.0,
                clienteSeleccionado = null,
                modalidad = ModalidadOrden.LOCAL,
                onModalidadChanged = {},
                onToggleParaLlevarItem = {},
                onEliminarItem = {},
                onCobrar = {},
                onApplyDiscount = { descuentoAplicado = it }
            )
        }
        // Tocar descuento 10%
        composeTestRule.onNodeWithText("10%").performClick()
        assertEquals(10, descuentoAplicado)
    }

    @Test
    fun carritoVacio_cobrarDeshabilitado() {
        composeTestRule.setContent {
            CarritoPanelV2(
                carrito = emptyList(),
                totalCarrito = BigDecimal.ZERO,
                descuentoLealtad = 0.0,
                descuentoPromociones = 0.0,
                clienteSeleccionado = null,
                modalidad = ModalidadOrden.LOCAL,
                onModalidadChanged = {},
                onToggleParaLlevarItem = {},
                onEliminarItem = {},
                onCobrar = {}
            )
        }
        composeTestRule.onNodeWithText("COBRAR").assertIsNotEnabled()
    }
}
