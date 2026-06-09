package com.bocatta.pos.data.sync

import android.content.Context
import com.bocatta.pos.data.local.OfflineDatabase
import com.bocatta.pos.data.local.VentaOffline
import com.bocatta.pos.domain.model.ClienteV2
import com.bocatta.pos.domain.model.ItemCarritoV2
import com.bocatta.pos.domain.model.SalesInventoryProductV2
import com.bocatta.pos.domain.repository.ResultadoVenta
import com.bocatta.pos.data.repository.InventoryRepository
import com.bocatta.pos.core.TicketUtils
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import timber.log.Timber

@ExperimentalCoroutinesApi
class OfflineManagerTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var testScope: TestScope
    private lateinit var mockContext: Context
    private lateinit var mockDb: OfflineDatabase
    private lateinit var mockInventoryRepo: InventoryRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        testScope = TestScope(testDispatcher)
        mockContext = mockk(relaxed = true)
        mockDb = mockk(relaxed = true)
        mockInventoryRepo = mockk(relaxed = true)
        // Stub static calls
        mockkStatic(OfflineDatabase::class)
        every { OfflineDatabase.getInstance(mockContext) } returns mockDb
        mockkConstructor(InventoryRepository::class)
        every { anyConstructed<InventoryRepository>().calcularDeduccionesItemOffline(any()) } returns emptyMap()
        // Stub SyncScheduler
        mockkObject(SyncScheduler)
        every { SyncScheduler.scheduleImmediateSync(any()) } just Runs
    }

    @After
    fun tearDown() {
        unmockkAll()
        Dispatchers.resetMain()
    }

    private fun createDummyItem(): ItemCarritoV2 {
        // Minimal dummy implementation – only fields used in OfflineManager
        val producto = SalesInventoryProductV2(
            id = "prod-1",
            recetaId = null,
            categoria = "cat"
        )
        return ItemCarritoV2(
            nombre = "Item",
            cantidad = 1,
            precioFinal = java.math.BigDecimal("10.0"),
            producto = producto,
            esSeparado = false,
            base = null,
            aderezos = emptyList(),
            toppings = emptyList(),
            paraLlevar = false,
            cantidadGramos = null,
            componentesCombo = emptyList()
        )
    }

    @Test
    fun `guardarVentaOffline guarda con estado PENDING y agenda sync`() = testScope.runTest {
        val carrito = listOf(createDummyItem())
        val resultado = OfflineManager.guardarVentaOffline(
            context = mockContext,
            carrito = carrito,
            sucursal = "S1",
            usuarioNombre = "user",
            total = 10.0,
            descuentoLealtad = 0.0,
            descuentoPromociones = 0.0,
            clienteSeleccionado = null,
            metodoPago = "efectivo",
            esConsumoEmpleado = false,
            notaOrden = "",
            forcedVentaId = "testVentaId"
        )
        // Verify that the DB was called to store the sale
        verify { mockDb.guardarVentaYDescontarStockReservandoFolio(any(), any()) }
        // Verify that the sale stored has ESTADO_PENDIENTE
        val ventaSlot = slot<VentaOffline>()
        verify { mockDb.guardarVentaYDescontarStockReservandoFolio(capture(ventaSlot), any()) }
        assertEquals(VentaOffline.ESTADO_PENDIENTE, ventaSlot.captured.estado)
        // Verify sync scheduled
        verify { SyncScheduler.scheduleImmediateSync(mockContext) }
        // Result should contain the ticket info from the returned VentaOffline (mocked)
        assertNotNull(resultado)
    }

    @Test
    fun `guardarOperacionOffline guarda operación y agenda sync`() = testScope.runTest {
        OfflineManager.guardarOperacionOffline(
            context = mockContext,
            tipo = "tipo",
            ventaId = "ventaId",
            motivo = "motivo",
            usuarioId = "userId",
            sucursal = "S1",
            dataJson = "{}",
            requiereAprobacion = true
        )
        verify { mockDb.guardarOperacion(any()) }
        verify { SyncScheduler.scheduleImmediateSync(mockContext) }
    }
}
