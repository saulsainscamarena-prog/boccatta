package com.bocatta.pos.domain.usecase

import com.bocatta.pos.domain.model.InventoryItem
import com.bocatta.pos.domain.model.RecipeInput
import com.bocatta.pos.domain.model.RecipeV2
import com.bocatta.pos.domain.model.StockAdjustmentEntity
import com.bocatta.pos.domain.repository.IInventoryRepository
import com.bocatta.pos.domain.repository.InventoryMovement
import com.bocatta.pos.domain.repository.StockDeduction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductionBatchUseCaseTest {

    @Test
    fun execute_usesRecipeYieldAsOutputAndRecipeProductAsOutputStockItem() = runTest {
        val repo = FakeInventoryRepository()
        val useCase = ProductionBatchUseCase(repo)
        val recipe = RecipeV2(
            id = "receta_masa",
            productId = "masa_crepa",
            yield = 12.0,
            yieldUnit = "pza",
            inputs = listOf(RecipeInput(insumoId = "harina", qty = 1000.0, unit = "g"))
        )

        val result = useCase.execute(
            branchId = "atlixco",
            userId = "admin",
            recipe = recipe,
            batchQty = 2.0,
            giro = "food"
        )

        assertTrue(result.success)
        assertEquals("masa_crepa", result.outputProductId)
        assertEquals(24.0, result.outputQty, 0.001)
        assertEquals(listOf("harina" to 2000.0), repo.stockChecks)
        assertEquals(2, repo.adjustments.size)
        assertEquals(Adjustment("harina", -2000.0, "PRODUCTION_INPUT"), repo.adjustments[0])
        assertEquals(Adjustment("masa_crepa", 24.0, "PRODUCTION_OUTPUT"), repo.adjustments[1])
    }

    @Test
    fun execute_failsIfOutputStockAdjustmentFails() = runTest {
        val repo = FakeInventoryRepository(failProductId = "masa_crepa")
        val useCase = ProductionBatchUseCase(repo)
        val recipe = RecipeV2(
            id = "receta_masa",
            productId = "masa_crepa",
            yield = 12.0,
            yieldUnit = "pza",
            inputs = listOf(RecipeInput(insumoId = "harina", qty = 1000.0, unit = "g"))
        )

        val result = useCase.execute("atlixco", "admin", recipe, batchQty = 1.0, giro = "food")

        assertFalse(result.success)
        assertTrue(result.error.orEmpty().contains("masa_crepa"))
    }

    private data class Adjustment(
        val productId: String,
        val quantity: Double,
        val reason: String
    )

    private class FakeInventoryRepository(
        private val failProductId: String? = null
    ) : IInventoryRepository {
        val stockChecks = mutableListOf<Pair<String, Double>>()
        val adjustments = mutableListOf<Adjustment>()

        override fun getStockForBranch(branchId: String): Flow<List<InventoryItem>> = flowOf(emptyList())

        override fun getStockItem(branchId: String, productId: String): Flow<InventoryItem?> = flowOf(null)

        override suspend fun getCurrentStock(branchId: String, productId: String): Double = 9999.0

        override suspend fun adjustStock(
            branchId: String,
            productId: String,
            quantity: Double,
            unit: String,
            reason: String,
            referenceId: String,
            userId: String
        ): Boolean {
            adjustments += Adjustment(productId, quantity, reason)
            return productId != failProductId
        }

        override suspend fun hasSufficientStock(
            branchId: String,
            productId: String,
            requiredQty: Double,
            unit: String
        ): Boolean {
            stockChecks += productId to requiredQty
            return true
        }

        override suspend fun adjustStockBatch(
            branchId: String,
            userId: String,
            referenceId: String,
            deductions: List<StockDeduction>
        ): Boolean = true

        override fun getMovementHistory(
            branchId: String,
            productId: String,
            startDate: Long,
            endDate: Long
        ): Flow<List<InventoryMovement>> = flowOf(emptyList())

        override suspend fun syncOfflineAdjustment(adjustment: StockAdjustmentEntity): Boolean = true

        override suspend fun registrarCompraConPresentacion(
            branchId: String,
            insumoId: String,
            presentacionNombre: String,
            cantidadComprada: Double,
            contenidoEquivalente: Double,
            costoTotal: Double,
            userId: String
        ): Boolean = true
    }
}
