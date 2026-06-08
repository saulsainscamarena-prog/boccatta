package com.bocatta.pos.domain.usecase

import com.bocatta.pos.domain.model.*
import com.bocatta.pos.domain.repository.IInventoryRepository
import com.bocatta.pos.domain.repository.IProductRepository
import com.bocatta.pos.domain.repository.StockDeduction
import com.bocatta.pos.domain.unit.UnitConverter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class SalesFlowUseCaseTest {

    private val fakeProductRepo = FakeProductRepository()
    private val fakeInventoryRepo = FakeInventoryRepository()
    private lateinit var useCase: SalesFlowUseCase

    @Before
    fun setup() {
        useCase = SalesFlowUseCase(fakeProductRepo, fakeInventoryRepo, PromotionsEngineV2)
    }

    @Test
    fun processSale_sinReceta_creaTransaccionYNoDeduceStock() = runTest {
        fakeProductRepo.addProduct(InventoryProductV2(id = "prod-1", name = "Coca", basePrice = 25.0, baseUnit = "pza"))
        val items = listOf(
            SaleItemInput(productId = "prod-1", name = "Coca", quantity = 2.0, unitPrice = 25.0)
        )

        val result = useCase.processSale(
            branchId = "suc-1", userId = "user-1",
            items = items, discounts = emptyList(), giro = "FOOD"
        )

        assertTrue(result.success)
        assertNotNull(result.transaction)
        with(result.transaction!!) {
            assertEquals(50.0, subtotal, 0.01)
            assertEquals(0.0, discountTotal, 0.01)
            assertEquals(0.0, taxTotal, 0.01)
            assertEquals(50.0, grandTotal, 0.01)
            assertEquals(1, items.size)
        }
        assertTrue(fakeInventoryRepo.movements.isEmpty())
    }

    @Test
    fun processSale_conRecetaYStock_llamaAdjustStock() = runTest {
        fakeProductRepo.addProduct(InventoryProductV2(id = "prod-2", name = "Hot Dog", basePrice = 45.0, baseUnit = "pza"))
        val recipe = RecipeV2(
            id = "rec-1", productId = "prod-2", yield = 1.0, yieldUnit = "pza",
            inputs = listOf(RecipeInput(insumoId = "salchicha", qty = 1.0, unit = "pza"))
        )
        val items = listOf(
            SaleItemInput(productId = "prod-2", name = "Hot Dog", quantity = 3.0, unitPrice = 45.0, recipe = recipe)
        )

        val result = useCase.processSale(
            branchId = "suc-1", userId = "user-1",
            items = items, discounts = emptyList(), giro = "FOOD"
        )

        assertTrue(result.success)
        assertFalse(fakeInventoryRepo.movements.isEmpty())
        val deduction = fakeInventoryRepo.movements.find { it.first.endsWith("salchicha") }
        assertNotNull(deduction)
        assertEquals(-3.0, deduction!!.second, 0.01)
    }

    @Test
    fun processSale_conDescuentoPorcentaje_aplicaDescuento() = runTest {
        fakeProductRepo.addProduct(InventoryProductV2(id = "prod-3", name = "Combo", basePrice = 100.0, baseUnit = "pza"))
        val discount = DiscountV2(
            id = "desc-1", name = "10% OFF", type = "PERCENTAGE",
            value = 10.0, isActive = true, startDate = 0L, endDate = null
        )
        val items = listOf(
            SaleItemInput(productId = "prod-3", name = "Combo", quantity = 1.0, unitPrice = 100.0)
        )

        val result = useCase.processSale(
            branchId = "suc-1", userId = "user-1",
            items = items, discounts = listOf(discount), giro = "FOOD"
        )

        assertTrue(result.success)
        assertEquals(10.0, result.discounts.sumOf { it.amount }, 0.01)
        assertEquals(90.0, result.transaction!!.grandTotal, 0.01)
    }

    @Test
    fun processSale_conDescuentoFijo_excedeSubtotal_seLimita() = runTest {
        fakeProductRepo.addProduct(InventoryProductV2(id = "prod-4", name = "Item barato", basePrice = 20.0))
        val discount = DiscountV2(
            id = "desc-2", name = "$50 OFF", type = "FIXED",
            value = 50.0, isActive = true, startDate = 0L, endDate = null
        )
        val items = listOf(
            SaleItemInput(productId = "prod-4", name = "Item", quantity = 1.0, unitPrice = 20.0)
        )

        val result = useCase.processSale(
            branchId = "suc-1", userId = "user-1",
            items = items, discounts = listOf(discount), giro = "FOOD"
        )

        assertTrue(result.success)
        assertEquals(20.0, result.discounts.sumOf { it.amount }, 0.01)
        assertEquals(0.0, result.transaction!!.grandTotal, 0.01)
    }

    @Test
    fun processSale_conRecetaUnidadNoSoportada_retornaError() = runTest {
        fakeProductRepo.addProduct(InventoryProductV2(id = "prod-5", name = "Unidad invalida", basePrice = 30.0))
        val recipe = RecipeV2(
            id = "rec-2", productId = "prod-5", yield = 1.0, yieldUnit = "pza",
            inputs = listOf(RecipeInput(insumoId = "insumo-x", qty = 5.0, unit = "galones"))
        )
        val items = listOf(
            SaleItemInput(productId = "prod-5", name = "Unidad invalida", quantity = 1.0, unitPrice = 30.0, recipe = recipe)
        )

        val result = useCase.processSale(
            branchId = "suc-1", userId = "user-1",
            items = items, discounts = emptyList(), giro = "FOOD"
        )

        assertFalse(result.success)
        assertNotNull(result.error)
    }

    @Test
    fun processSale_conTaxRate_calculaTaxTotal() = runTest {
        fakeProductRepo.addProduct(InventoryProductV2(id = "prod-6", name = "Gravado", basePrice = 100.0, baseUnit = "pza"))
        val items = listOf(
            SaleItemInput(productId = "prod-6", name = "Gravado", quantity = 2.0, unitPrice = 100.0, taxRate = 0.16)
        )

        val result = useCase.processSale(
            branchId = "suc-1", userId = "user-1",
            items = items, discounts = emptyList(), giro = "FOOD"
        )

        assertTrue(result.success)
        assertEquals(200.0, result.transaction!!.subtotal, 0.01)
        assertEquals(32.0, result.transaction!!.taxTotal, 0.01)
        assertEquals(232.0, result.transaction!!.grandTotal, 0.01)
    }

    @Test
    fun processSale_transactionId_seUsaComoReferenceId() = runTest {
        fakeProductRepo.addProduct(InventoryProductV2(id = "prod-ref", name = "Ref test", basePrice = 50.0))
        val recipe = RecipeV2(
            id = "rec-ref", productId = "prod-ref", yield = 1.0, yieldUnit = "pza",
            inputs = listOf(RecipeInput(insumoId = "insumo-ref", qty = 1.0, unit = "pza"))
        )
        val items = listOf(
            SaleItemInput(productId = "prod-ref", name = "Ref test", quantity = 1.0, unitPrice = 50.0, recipe = recipe)
        )

        val result = useCase.processSale(
            branchId = "suc-1", userId = "user-1",
            items = items, discounts = emptyList(), giro = "FOOD"
        )

        assertTrue(result.success)
        val transactionId = result.transaction!!.id
        assertTrue(transactionId.isNotBlank())
        assertTrue(fakeInventoryRepo.lastReferenceIds.any { it == transactionId })
    }

    @Test
    fun processSale_multiItemConTaxHeterogeneo_calculaTaxCorrectamente() = runTest {
        fakeProductRepo.addProduct(InventoryProductV2(id = "p1", name = "Alimento", basePrice = 100.0, baseUnit = "pza"))
        fakeProductRepo.addProduct(InventoryProductV2(id = "p2", name = "Bebida", basePrice = 50.0, baseUnit = "pza"))
        val discount = DiscountV2(
            id = "d10", name = "10% OFF", type = "PERCENTAGE",
            value = 10.0, isActive = true, startDate = 0L, endDate = null
        )
        val items = listOf(
            SaleItemInput(productId = "p1", name = "Alimento", quantity = 1.0, unitPrice = 100.0, taxRate = 0.0),
            SaleItemInput(productId = "p2", name = "Bebida", quantity = 2.0, unitPrice = 50.0, taxRate = 0.16)
        )

        val result = useCase.processSale(
            branchId = "suc-1", userId = "user-1",
            items = items, discounts = listOf(discount), giro = "FOOD"
        )

        assertTrue(result.success)
        assertEquals(200.0, result.transaction!!.subtotal, 0.01)
        assertEquals(20.0, result.transaction!!.discountTotal, 0.01)
        // netSubtotal = 180, discountRatio = 180/200 = 0.9
        // tax = (100*0.0*0.9) + (100*0.16*0.9) = 0 + 14.4
        assertEquals(14.4, result.transaction!!.taxTotal, 0.01)
        assertEquals(194.4, result.transaction!!.grandTotal, 0.01)
    }

    @Test
    fun processSale_fakeAdjustStockFallido_retornaError() = runTest {
        fakeProductRepo.addProduct(InventoryProductV2(id = "prod-fail", name = "Fail", basePrice = 50.0, baseUnit = "pza"))
        fakeInventoryRepo.failBatch = true
        val recipe = RecipeV2(
            id = "rec-fail", productId = "prod-fail", yield = 1.0, yieldUnit = "pza",
            inputs = listOf(RecipeInput(insumoId = "insumo-fail", qty = 1.0, unit = "pza"))
        )
        val items = listOf(
            SaleItemInput(productId = "prod-fail", name = "Fail", quantity = 1.0, unitPrice = 50.0, recipe = recipe)
        )

        val result = useCase.processSale(
            branchId = "suc-1", userId = "user-1",
            items = items, discounts = emptyList(), giro = "FOOD"
        )

        assertFalse(result.success)
        assertNotNull(result.error)
    }
}

// ── Fake implementations ──────────────────────────────────────────────

// ── Fake implementations ──────────────────────────────────────────────

class FakeProductRepository : IProductRepository {
    private val products = mutableMapOf<String, InventoryProductV2>()
    private val productsFlow = MutableStateFlow<List<InventoryProductV2>>(emptyList())

    fun addProduct(p: InventoryProductV2) {
        products[p.id] = p
        productsFlow.value = products.values.toList()
    }

    override fun getAllProducts(): Flow<List<InventoryProductV2>> = productsFlow.asStateFlow()

    override fun getProductById(id: String): Flow<InventoryProductV2?> {
        val result = MutableStateFlow(products[id])
        return result.asStateFlow()
    }

    override fun getProductsByFilter(category: String?, giro: String?): Flow<List<InventoryProductV2>> {
        val filtered = products.values.filter {
            (category == null || it.category == category) &&
            (giro == null || it.giro == giro)
        }
        return MutableStateFlow(filtered).asStateFlow()
    }

    override suspend fun saveProduct(product: InventoryProductV2): Boolean {
        products[product.id] = product
        productsFlow.value = products.values.toList()
        return true
    }

    override suspend fun deleteProduct(id: String): Boolean {
        products.remove(id)
        productsFlow.value = products.values.toList()
        return true
    }

    override suspend fun getProductType(id: String): String? = products[id]?.type

    override suspend fun getProductBaseUnit(productId: String): String? = products[productId]?.baseUnit

    override fun getSalesProducts(): Flow<List<SalesInventoryProductV2>> =
        MutableStateFlow<List<SalesInventoryProductV2>>(emptyList()).asStateFlow()
}

class FakeInventoryRepository : IInventoryRepository {
    val movements = mutableListOf<Pair<String, Double>>()
    val lastReferenceIds = mutableListOf<String>()
    var failBatch = false
    private val stock = mutableMapOf<String, Double>()

    fun setStock(productId: String, qty: Double) { stock[productId] = qty }

    override fun getStockForBranch(branchId: String): Flow<List<InventoryItem>> =
        MutableStateFlow<List<InventoryItem>>(emptyList()).asStateFlow()

    override fun getStockItem(branchId: String, productId: String): Flow<InventoryItem?> =
        MutableStateFlow<InventoryItem?>(null).asStateFlow()

    override suspend fun getCurrentStock(branchId: String, productId: String): Double =
        stock[productId] ?: 0.0

    override suspend fun adjustStock(
        branchId: String, productId: String, quantity: Double, unit: String,
        reason: String, referenceId: String, userId: String
    ): Boolean {
        movements.add(productId to quantity)
        lastReferenceIds.add(referenceId)
        val base = stock[productId] ?: 0.0
        stock[productId] = base + UnitConverter.toBase(quantity, unit)
        return true
    }

    override suspend fun adjustStockBatch(
        branchId: String, userId: String, referenceId: String,
        deductions: List<StockDeduction>
    ): Boolean {
        if (failBatch) return false
        for (d in deductions) {
            movements.add(d.productId to d.quantity)
        }
        lastReferenceIds.add(referenceId)
        return true
    }

    override suspend fun hasSufficientStock(
        branchId: String, productId: String, requiredQty: Double, unit: String
    ): Boolean {
        val base = UnitConverter.toBase(requiredQty, unit)
        return (stock[productId] ?: 0.0) >= base
    }

    override suspend fun syncOfflineAdjustment(
        adjustment: com.bocatta.pos.domain.model.StockAdjustmentEntity
    ): Boolean = true

    override fun getMovementHistory(
        branchId: String, productId: String, startDate: Long, endDate: Long
    ): Flow<List<com.bocatta.pos.domain.repository.InventoryMovement>> =
        MutableStateFlow<List<com.bocatta.pos.domain.repository.InventoryMovement>>(emptyList()).asStateFlow()

    override suspend fun registrarCompraConPresentacion(
        branchId: String,
        insumoId: String,
        presentacionNombre: String,
        cantidadComprada: Double,
        contenidoEquivalente: Double,
        costoTotal: Double,
        userId: String
    ): Boolean {
        movements.add(insumoId to (cantidadComprada * contenidoEquivalente))
        return true
    }

    override fun getStockAlertsFlow(branchId: String): Flow<Map<String, Double>> = kotlinx.coroutines.flow.flowOf(emptyMap())
}
