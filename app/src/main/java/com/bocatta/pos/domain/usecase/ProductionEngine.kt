package com.bocatta.pos.domain.usecase

import com.bocatta.pos.domain.unit.UnitConverter

interface ProductionEngine {
    fun validateInputs(inputs: List<ProductionInput>): Boolean
    fun execute(inputs: List<ProductionInput>, yieldQty: Double, yieldUnit: String): ProductionResult
}

data class ProductionInput(
    val productId: String,
    val qty: Double,
    val unit: String
)

data class ProductionResult(
    val success: Boolean,
    val outputProductId: String,
    val outputQty: Double,
    val outputUnit: String,
    val movements: List<MovementRecord>,
    val error: String? = null
)

data class MovementRecord(
    val productId: String,
    val quantity: Double,
    val unit: String,
    val type: String
)

class FoodProductionEngine : ProductionEngine {
    override fun validateInputs(inputs: List<ProductionInput>): Boolean {
        return inputs.all { it.qty > 0 && UnitConverter.isUnitSupported(it.unit) }
    }

    override fun execute(inputs: List<ProductionInput>, yieldQty: Double, yieldUnit: String): ProductionResult {
        if (!validateInputs(inputs)) {
            return ProductionResult(
                success = false, outputProductId = "", outputQty = 0.0, outputUnit = "",
                movements = emptyList(), error = "Invalid inputs"
            )
        }
        val movements = mutableListOf<MovementRecord>()
        for (input in inputs) {
            val baseQty = UnitConverter.toBase(input.qty, input.unit)
            movements.add(MovementRecord(input.productId, -baseQty, input.unit, "PRODUCTION_INPUT"))
        }
        val outputBase = UnitConverter.toBase(yieldQty, yieldUnit)
        movements.add(MovementRecord("output", outputBase, yieldUnit, "PRODUCTION_OUTPUT"))
        return ProductionResult(
            success = true,
            outputProductId = "",
            outputQty = outputBase,
            outputUnit = yieldUnit,
            movements = movements
        )
    }
}

class RetailKitEngine : ProductionEngine {
    override fun validateInputs(inputs: List<ProductionInput>): Boolean {
        return inputs.isNotEmpty() && inputs.all { it.qty > 0 }
    }

    override fun execute(inputs: List<ProductionInput>, yieldQty: Double, yieldUnit: String): ProductionResult {
        val movements = mutableListOf<MovementRecord>()
        for (input in inputs) {
            movements.add(MovementRecord(input.productId, -input.qty, input.unit, "PRODUCTION_INPUT"))
        }
        movements.add(MovementRecord("kit_output", yieldQty, yieldUnit, "PRODUCTION_OUTPUT"))
        return ProductionResult(
            success = true,
            outputProductId = "",
            outputQty = yieldQty,
            outputUnit = yieldUnit,
            movements = movements
        )
    }
}

class ServiceTaskEngine : ProductionEngine {
    override fun validateInputs(inputs: List<ProductionInput>): Boolean = true

    override fun execute(inputs: List<ProductionInput>, yieldQty: Double, yieldUnit: String): ProductionResult {
        val movements = inputs.map {
            MovementRecord(it.productId, -it.qty, it.unit, "PRODUCTION_INPUT")
        } + MovementRecord("service_output", yieldQty, yieldUnit, "PRODUCTION_OUTPUT")
        return ProductionResult(
            success = true,
            outputProductId = "",
            outputQty = yieldQty,
            outputUnit = yieldUnit,
            movements = movements
        )
    }
}

