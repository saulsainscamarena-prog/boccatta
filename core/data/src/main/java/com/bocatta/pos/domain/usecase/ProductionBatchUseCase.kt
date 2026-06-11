package com.bocatta.pos.domain.usecase

import com.bocatta.pos.domain.model.RecipeV2
import com.bocatta.pos.domain.repository.IInventoryRepository

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
        if (batchQty <= 0.0) {
            return BatchResult(
                success = false,
                error = "La cantidad de tandas debe ser mayor a cero"
            )
        }
        if (recipe.productId.isBlank()) {
            return BatchResult(
                success = false,
                error = "La receta de produccion no tiene producto de salida"
            )
        }

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
                error = "Insumos invalidos para la receta: ${recipe.id}"
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

        val result = engine.execute(inputs, recipe.yield * batchQty, recipe.yieldUnit)
        if (!result.success) {
            return BatchResult(
                success = false,
                error = result.error ?: "Error en motor de produccion"
            )
        }

        for (movement in result.movements) {
            val productId = if (movement.type == PRODUCTION_OUTPUT) recipe.productId else movement.productId
            val reason = when (movement.type) {
                PRODUCTION_OUTPUT -> PRODUCTION_OUTPUT
                PRODUCTION_INPUT -> PRODUCTION_INPUT
                else -> if (movement.quantity < 0) PRODUCTION_INPUT else PRODUCTION_OUTPUT
            }
            val adjusted = inventoryRepo.adjustStock(
                branchId = branchId,
                productId = productId,
                quantity = movement.quantity,
                unit = movement.unit,
                reason = reason,
                referenceId = recipe.id,
                userId = userId
            )
            if (!adjusted) {
                return BatchResult(
                    success = false,
                    error = "No se pudo registrar movimiento de produccion para $productId"
                )
            }
        }

        return BatchResult(
            success = true,
            outputProductId = recipe.productId,
            outputQty = result.outputQty,
            outputUnit = result.outputUnit
        )
    }

    private companion object {
        const val PRODUCTION_INPUT = "PRODUCTION_INPUT"
        const val PRODUCTION_OUTPUT = "PRODUCTION_OUTPUT"
    }
}

data class BatchResult(
    val success: Boolean,
    val outputProductId: String = "",
    val outputQty: Double = 0.0,
    val outputUnit: String = "",
    val error: String? = null
)
