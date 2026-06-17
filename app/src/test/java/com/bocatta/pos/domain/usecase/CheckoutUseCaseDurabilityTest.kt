package com.bocatta.pos.domain.usecase

import android.content.Context
import com.bocatta.pos.data.local.OfflineDatabase
import com.bocatta.pos.domain.model.ItemCarritoV2
import com.bocatta.pos.domain.model.SalesInventoryProductV2
import com.bocatta.pos.domain.repository.IInventoryRepository
import com.bocatta.pos.domain.repository.ResultadoVenta
import com.bocatta.pos.domain.repository.SalesRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import java.io.IOException
import java.math.BigDecimal
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CheckoutUseCaseDurabilityTest {

    private val context = mockk<Context>(relaxed = true)
    private val inventoryRepository = mockk<IInventoryRepository>()
    private val offlineDatabase = mockk<OfflineDatabase>()
    private lateinit var salesRepository: FakeSalesRepository
    private lateinit var savedOfflineRequests: MutableList<OfflineSaleRequest>

    @Before
    fun setUp() {
        salesRepository = FakeSalesRepository()
        savedOfflineRequests = mutableListOf()
        every { offlineDatabase.obtenerInsumos() } returns emptyList()
        every { offlineDatabase.obtenerProductoPorId(any()) } returns null
        coEvery {
            inventoryRepository.hasSufficientStock(any(), any(), any(), any())
        } returns true
    }

    @Test
    fun `transient remote failure stores sale locally with same id`() = runTest {
        salesRepository.failure = IOException("connection reset")
        val useCase = createUseCase(online = true)

        val result = useCase.finalizarVenta(
            carrito = listOf(testItem()),
            sucursal = "atlixco",
            usuarioNombre = "cashier",
            clienteSeleccionado = null,
            descuentoLealtad = 0.0,
            descuentoPromociones = 0.0,
            descuentoManual = 0.0,
            propina = 5.0,
            notaOrden = "sin cuchara",
            esConsumoEmpleado = false,
            splitActivo = false,
            splitPartes = emptyList(),
            metodoPagoSeleccionado = "Efectivo",
            forcedVentaId = "sale-fixed"
        )

        assertTrue(result.success)
        assertFalse(result.online)
        assertEquals("sale-fixed", savedOfflineRequests.single().forcedVentaId)
        assertEquals(5.0, savedOfflineRequests.single().propina, 0.0)
        assertEquals("sin cuchara", savedOfflineRequests.single().notaOrden)
    }

    @Test
    fun `business rejection does not create offline sale`() = runTest {
        salesRepository.failure = IllegalStateException("Stock insuficiente local")
        val useCase = createUseCase(online = true)

        val result = useCase.finalizarVenta(
            carrito = listOf(testItem()),
            sucursal = "atlixco",
            usuarioNombre = "cashier",
            clienteSeleccionado = null,
            descuentoLealtad = 0.0,
            descuentoPromociones = 0.0,
            descuentoManual = 0.0,
            propina = 0.0,
            notaOrden = "",
            esConsumoEmpleado = false,
            splitActivo = false,
            splitPartes = emptyList(),
            metodoPagoSeleccionado = "Efectivo",
            forcedVentaId = "sale-stock"
        )

        assertFalse(result.success)
        assertTrue(savedOfflineRequests.isEmpty())
        assertTrue(result.error.orEmpty().contains("Stock insuficiente"))
    }

    @Test
    fun `offline signal stores without calling remote repository`() = runTest {
        val useCase = createUseCase(online = false)

        val result = useCase.finalizarVenta(
            carrito = listOf(testItem()),
            sucursal = "atlixco",
            usuarioNombre = "cashier",
            clienteSeleccionado = null,
            descuentoLealtad = 0.0,
            descuentoPromociones = 0.0,
            descuentoManual = 0.0,
            propina = 0.0,
            notaOrden = "",
            esConsumoEmpleado = false,
            splitActivo = false,
            splitPartes = emptyList(),
            metodoPagoSeleccionado = "Efectivo",
            forcedVentaId = "sale-offline"
        )

        assertTrue(result.success)
        assertFalse(result.online)
        assertEquals(0, salesRepository.calls)
        assertEquals("sale-offline", savedOfflineRequests.single().forcedVentaId)
    }

    private fun createUseCase(online: Boolean): CheckoutUseCase =
        CheckoutUseCase(
            context = context,
            repository = salesRepository,
            inventoryRepo = inventoryRepository,
            offlineDb = offlineDatabase,
            generarTicketWhatsAppUseCase = GenerarTicketWhatsAppUseCase(),
            networkStatus = { online },
            offlineSaleStore = { request ->
                savedOfflineRequests += request
                ResultadoVenta(
                    numeroTicket = 41,
                    codigoTicket = "ATL-LOCAL-41"
                )
            }
        )

    private fun testItem(): ItemCarritoV2 =
        ItemCarritoV2(
            cartId = "cart-1",
            producto = SalesInventoryProductV2(
                id = "product-1",
                nombre = "Producto",
                categoria = "BEBIDAS"
            ),
            precioFinal = BigDecimal("40.00"),
            cantidad = 1,
            nombre = "Producto"
        )

    private class FakeSalesRepository : SalesRepository {
        var failure: Exception? = null
        var calls: Int = 0

        override suspend fun finalizarVentaConInventario(
            carrito: List<ItemCarritoV2>,
            sucursal: String,
            usuarioNombre: String,
            clienteSeleccionado: com.bocatta.pos.domain.model.ClienteV2?,
            descuentoLealtad: Double,
            metodoPagoSeleccionado: String,
            esConsumoEmpleado: Boolean,
            descuentoPromociones: Double,
            descuentoManual: Double,
            propina: Double,
            notaOrden: String,
            splitPartes: List<com.bocatta.pos.domain.model.SplitParte>,
            forcedVentaId: String?
        ): ResultadoVenta {
            calls++
            failure?.let { throw it }
            return ResultadoVenta(42, "ATL-REMOTE-42")
        }

        override suspend fun registrarGastoValidado(
            monto: Double,
            motivo: String,
            sucursal: String,
            usuarioId: String
        ): Boolean = true

        override fun getActiveKdsOrders(
            sucursal: String
        ): Flow<List<com.bocatta.pos.domain.model.VentaV2>> = emptyFlow()

        override suspend fun updateKdsOrderStatus(
            ventaId: String,
            newStatus: String,
            sucursal: String
        ): Boolean = true
    }
}
