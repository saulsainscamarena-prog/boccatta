package com.bocatta.pos.feature.ventas.ui.screens.ventas

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bocatta.pos.domain.model.ItemCarritoV2
import com.bocatta.pos.domain.model.ModalidadOrden
import com.bocatta.pos.domain.model.SalesInventoryProductV2
import com.bocatta.pos.domain.usecase.CatalogoProcesado
import java.math.BigDecimal
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SalesScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun catalogSection_clickingSimpleProduct_reachesCartCallback() {
        val product = productoMostrador()
        val catalog = CatalogoProcesado(
            vendibles = listOf(product),
            frecuentes = listOf(product),
            categoriasVisibles = listOf("BEBIDAS"),
            excluidos = emptyList()
        )

        composeTestRule.setContent {
            var cartCount by remember { mutableIntStateOf(0) }
            MaterialTheme {
                SalesCatalogSection(
                    catalogoProcesado = catalog,
                    filteredProducts = listOf(product),
                    categoriaSeleccionada = "FRECUENTES",
                    searchQuery = "",
                    menuCargando = false,
                    menuError = null,
                    productosCargados = true,
                    alertasStock = mapOf(product.id to 20.0),
                    sucursalActual = "Metepec",
                    cargandoTurno = false,
                    tieneTurnoActivo = true,
                    modifier = Modifier.fillMaxSize(),
                    onCategoriaSelected = {},
                    onSearchQueryChange = {},
                    onProductoClick = { cartCount += 1 },
                    onRetryMenu = {},
                    onVerInventario = {},
                    onVerCaja = {}
                )
                Text("Harness cart: $cartCount")
            }
        }

        composeTestRule.onNodeWithText("COCA COLA").performClick()
        composeTestRule.onNodeWithText("Harness cart: 1").assertIsDisplayed()
    }

    @Test
    fun cartSection_checkoutAndDeleteActions_areReachableOnTablet() {
        val product = productoMostrador()
        val item = ItemCarritoV2(
            cartId = "cart-1",
            producto = product,
            precioFinal = BigDecimal("25.00"),
            cantidad = 2,
            nombre = "Coca Cola"
        )

        composeTestRule.setContent {
            var carrito by remember { mutableStateOf(listOf(item)) }
            var cobroSolicitado by remember { mutableStateOf(false) }
            MaterialTheme {
                Row(Modifier.fillMaxSize()) {
                    SalesCartSection(
                        isTablet = true,
                        showMobileCart = false,
                        carrito = carrito,
                        totalCarrito = BigDecimal("50.00"),
                        descuentoLealtad = 0.0,
                        descuentoPromociones = 0.0,
                        descuentoManual = 0.0,
                        clienteSeleccionado = null,
                        modalidad = ModalidadOrden.LOCAL,
                        esAdmin = true,
                        mesaId = null,
                        modifier = Modifier.width(420.dp).height(900.dp),
                        onModalidadChanged = {},
                        onToggleParaLlevarItem = {},
                        onEliminarItem = { carrito = emptyList() },
                        onEditarItem = {},
                        onCobrar = { cobroSolicitado = true },
                        onApplyDiscount = {},
                        onApartar = {},
                        onBuscarCliente = {},
                        onEliminarCliente = {},
                        onValidarPin = { _, result -> result(true) },
                        onDismissMobileCart = {}
                    )
                    Text("Harness checkout: $cobroSolicitado")
                    Text("Harness items: ${carrito.size}")
                }
            }
        }

        composeTestRule.onNodeWithText("COCA COLA").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("$50.00")[0].assertIsDisplayed()
        composeTestRule.onNode(hasText("COBRAR") and hasClickAction()).performClick()
        composeTestRule.onNodeWithText("Harness checkout: true").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Eliminar Coca Cola").performClick()
        composeTestRule.onNodeWithText("Harness items: 0").assertIsDisplayed()
    }

    @Test
    fun counterFlow_tablet_addProductCheckoutExactCash_clearsOrder() {
        val product = productoMostrador()
        val catalog = CatalogoProcesado(
            vendibles = listOf(product),
            frecuentes = listOf(product),
            categoriasVisibles = listOf("BEBIDAS"),
            excluidos = emptyList()
        )

        composeTestRule.setContent {
            MostradorFlowHarness(
                catalog = catalog,
                product = product
            )
        }

        composeTestRule.onNodeWithText("COCA COLA").performClick()
        composeTestRule.onNodeWithText("ORDEN - 1").assertIsDisplayed()
        composeTestRule.onNode(hasText("COBRAR") and hasClickAction()).performClick()
        composeTestRule.waitUntil {
            composeTestRule.onAllNodesWithText("Cobrar efectivo exacto").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("Pago final").assertExists()
        composeTestRule.onAllNodesWithText("$25.00")[0].assertExists()
        composeTestRule.onNodeWithText("Cobrar efectivo exacto").performClick()
        composeTestRule.onNodeWithText("Venta finalizada: $25.00").assertIsDisplayed()
        composeTestRule.onNodeWithText("ORDEN VACIA", useUnmergedTree = true).assertIsDisplayed()
    }

    @Composable
    private fun MostradorFlowHarness(
        catalog: CatalogoProcesado,
        product: SalesInventoryProductV2
    ) {
        val carrito = remember { mutableStateListOf<ItemCarritoV2>() }
        var mostrarPago by remember { mutableStateOf(false) }
        var ventaFinalizada by remember { mutableStateOf("") }
        val total = carrito.fold(BigDecimal.ZERO) { acc, item ->
            acc + item.precioFinal.multiply(BigDecimal(item.cantidad))
        }

        MaterialTheme {
            Column(Modifier.fillMaxSize()) {
                Text(
                    text = "ORDEN - ${carrito.size}",
                    modifier = Modifier.padding(12.dp)
                )
                if (ventaFinalizada.isNotBlank()) {
                    Text(
                        text = ventaFinalizada,
                        modifier = Modifier.padding(12.dp)
                    )
                }
                Row(Modifier.weight(1f)) {
                    Column(Modifier.weight(1f)) {
                    SalesCatalogSection(
                        catalogoProcesado = catalog,
                        filteredProducts = catalog.frecuentes,
                        categoriaSeleccionada = "FRECUENTES",
                        searchQuery = "",
                        menuCargando = false,
                        menuError = null,
                        productosCargados = true,
                        alertasStock = mapOf(product.id to 20.0),
                        sucursalActual = "Metepec",
                        cargandoTurno = false,
                        tieneTurnoActivo = true,
                        modifier = Modifier.fillMaxSize(),
                        onCategoriaSelected = {},
                        onSearchQueryChange = {},
                        onProductoClick = {
                            carrito.add(
                                ItemCarritoV2(
                                    cartId = "cart-${carrito.size + 1}",
                                    producto = it,
                                    precioFinal = BigDecimal("25.00"),
                                    cantidad = 1,
                                    nombre = it.nombre
                                )
                            )
                        },
                        onRetryMenu = {},
                        onVerInventario = {},
                        onVerCaja = {}
                    )
                }
                    SalesCartSection(
                        isTablet = true,
                        showMobileCart = false,
                        carrito = carrito,
                        totalCarrito = total,
                        descuentoLealtad = 0.0,
                        descuentoPromociones = 0.0,
                        descuentoManual = 0.0,
                        clienteSeleccionado = null,
                        modalidad = ModalidadOrden.LOCAL,
                        esAdmin = true,
                        mesaId = null,
                        modifier = Modifier.width(420.dp).fillMaxSize(),
                        onModalidadChanged = {},
                        onToggleParaLlevarItem = {},
                        onEliminarItem = { carrito.remove(it) },
                        onEditarItem = {},
                        onCobrar = { mostrarPago = true },
                        onApplyDiscount = {},
                        onApartar = {},
                        onBuscarCliente = {},
                        onEliminarCliente = {},
                        onValidarPin = { _, result -> result(true) },
                        onDismissMobileCart = {}
                    )
                }
            }

            if (mostrarPago) {
                AlertDialog(
                    onDismissRequest = { mostrarPago = false },
                    title = { Text("Pago final") },
                    text = {
                        Column {
                            Text("Total a pagar")
                            Text("$${"%.2f".format(total)}")
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                ventaFinalizada = "Venta finalizada: $${"%.2f".format(total)}"
                                carrito.clear()
                                mostrarPago = false
                            }
                        ) {
                            Text("Cobrar efectivo exacto")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { mostrarPago = false }) {
                            Text("Cerrar")
                        }
                    }
                )
            }
        }
    }

    private fun productoMostrador() = SalesInventoryProductV2(
        id = "p-coca",
        nombre = "Coca Cola",
        emoji = "🥤",
        categoria = "BEBIDAS",
        precioVenta = mapOf("metepec" to 25.0),
        activo = true
    )
}
