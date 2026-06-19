package com.bocatta.pos.domain.usecase

import android.content.Context
import com.bocatta.pos.data.local.OfflineDatabase
import com.bocatta.pos.data.sync.OfflineManager
import com.bocatta.pos.domain.model.ItemCarritoV2
import com.bocatta.pos.domain.model.SalesInventoryProductV2
import com.bocatta.pos.domain.repository.IInventoryRepository
import com.bocatta.pos.domain.repository.ResultadoVenta
import com.bocatta.pos.domain.repository.SalesRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import java.io.IOException
import java.math.BigDecimal
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CheckoutUseCaseDurabilityTest {

    private val context = mockk<Context>(relaxed = true)
    private val inventoryRepository = mockk<IInventoryRepository>()
    private val offlineDatabase = mockk<OfflineDatabase>()
    private lateinit var salesRepository: FakeSalesRepository

    @Before
    fun setUp() {
        mockkObject(OfflineManager)
        salesRepository = FakeSalesRepository()
        every { OfflineManager.isNetworkAvailable(any()) } returns true
        coEvery {
            OfflineManager.guardarVentaOffline(
                context = any(),
                carrito = any(),
                sucursal = any(),
                usuarioNombre = any(),
                total = any(),
                descuentoLealtad = any(),
                descuentoPromociones = any(),
                descuentoManual = any(),
                clienteSeleccionado = any(),
                metodoPago = any(),
                esConsumoEmpleado = any(),
                propina = any(),
                notaOrden = any(),
                forcedVentaId = any()
            )
        } returns ResultadoVenta(7L, "ATL-LOCAL-7")
        every { offlineDatabase.obtenerInsumos() } returns emptyList()
        every { offlineDatabase.obtenerProductoPorId(any()) } returns null
        coEvery {
            inventoryRepository.hasSufficientStock(any(), any(), any(), any())
        } returns true
    }

    @After
    fun tearDown() {
        unmockkObject(OfflineManager)
    }

    @Test
    fun `successful online sale returns success with ticket`() = runTest {
        val useCase = createUseCase()
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
            forcedVentaId = "sale-test"
        )

        assertTrue(result.success)
        assertEquals(42L, result.numeroTicket)
        assertEquals("ATL-REMOTE-42", result.codigoTicket)
        assertNotNull(result.ticketText)
    }

    @Test
    fun `business rejection does not create sale`() = runTest {
        salesRepository.failure = IllegalStateException("Stock insuficiente local")
        val useCase = createUseCase()

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
        assertTrue(result.error.orEmpty().contains("Stock insuficiente"))
        coVerify(exactly = 0) {
            OfflineManager.guardarVentaOffline(
                context = any(),
                carrito = any(),
                sucursal = any(),
                usuarioNombre = any(),
                total = any(),
                descuentoLealtad = any(),
                descuentoPromociones = any(),
                descuentoManual = any(),
                clienteSeleccionado = any(),
                metodoPago = any(),
                esConsumoEmpleado = any(),
                propina = any(),
                notaOrden = any(),
                forcedVentaId = any()
            )
        }
    }

    @Test
    fun `transient online repository error is saved offline with same sale id`() = runTest {
        salesRepository.failure = IOException("connection reset")
        val useCase = createUseCase()

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
            forcedVentaId = "sale-err"
        )

        assertTrue(result.success)
        assertTrue(result.queuedOffline)
        assertEquals("ATL-LOCAL-7", result.codigoTicket)
        coVerify(exactly = 1) {
            OfflineManager.guardarVentaOffline(
                context = context,
                carrito = any(),
                sucursal = "atlixco",
                usuarioNombre = "cashier",
                total = 40.0,
                descuentoLealtad = 0.0,
                descuentoPromociones = 0.0,
                descuentoManual = 0.0,
                clienteSeleccionado = null,
                metodoPago = "Efectivo",
                esConsumoEmpleado = false,
                propina = 0.0,
                notaOrden = "",
                forcedVentaId = "sale-err"
            )
        }
    }

    private fun createUseCase(): CheckoutUseCase =
        CheckoutUseCase(
            context = context,
            repository = salesRepository,
            inventoryRepo = inventoryRepository,
            offlineDb = offlineDatabase,
            generarTicketWhatsAppUseCase = GenerarTicketWhatsAppUseCase()
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
            monto: Double, motivo: String, sucursal: String, usuarioId: String
        ): Boolean = true

        override fun getActiveKdsOrders(
            sucursal: String
        ): Flow<List<com.bocatta.pos.domain.model.VentaV2>> = emptyFlow()

        override suspend fun updateKdsOrderStatus(
            ventaId: String, estado: String, sucursal: String
        ): Boolean = true
    }
}
