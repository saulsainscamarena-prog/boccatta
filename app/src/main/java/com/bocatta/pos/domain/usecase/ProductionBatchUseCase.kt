package com.bocatta.pos.domain.usecase

import com.bocatta.pos.domain.model.RecipeV2
import com.bocatta.pos.domain.repository.IInventoryRepository
import com.bocatta.pos.domain.unit.UnitConverter

class ProductionBatchUseCase(
    private val inventoryRepo: IInventoryRepository,
    private val promotionsEngine: PromotionsEngineV2 = PromotionsEngineV2
) {

    suspend fun execute(
        branchId: String,
        userId: String,
        recipe: RecipeV2,
        batchQty: Double,
        giro: String
    ): BatchResult {
        val engine = BusinessLogicFactory.getProductionEngine(giro)

        val inputs = recipe.inputs.map { input ->
            ProductionInput(
                productId = input.insumoId,
                qty = input.qty * batchQty,
                unit = input.unit
            )
        }

        if (!engine.validateInputs(inputs)) {
            return BatchResult(
                success = false,
                error = "Insumos inválidos para la receta: ${recipe.id}"
            )
        }

        for (input in inputs) {
            val hasStock = inventoryRepo.hasSufficientStock(
                branchId = branchId,
                productId = input.productId,
                requiredQty = input.qty,
                unit = input.unit
            )
            if (!hasStock) {
                return BatchResult(
                    success = false,
                    error = "Stock insuficiente para insumo: ${input.productId}"
                )
            }
        }

        val result = engine.execute(inputs, batchQty, recipe.yieldUnit)
        if (!result.success) {
            return BatchResult(
                success = false,
                error = result.error ?: "Error en motor de producción"
            )
        }

        for (movement in result.movements) {
            inventoryRepo.adjustStock(
                branchId = branchId,
                productId = movement.productId,
                quantity = movement.quantity,
                unit = movement.unit,
                reason = if (movement.quantity < 0) "PRODUCTION_INPUT" else "PRODUCTION_OUTPUT",
                referenceId = recipe.id,
                userId = userId
            )
        }

        return BatchResult(
            success = true,
            outputQty = result.outputQty,
            outputUnit = result.outputUnit
        )
    }
}

data class BatchResult(
    val success: Boolean,
    val outputQty: Double = 0.0,
    val outputUnit: String = "",
    val error: String? = null
)

