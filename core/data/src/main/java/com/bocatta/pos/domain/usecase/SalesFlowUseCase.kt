package com.bocatta.pos.domain.usecase

import com.bocatta.pos.domain.model.DiscountV2
import com.bocatta.pos.domain.model.TransactionItemV2
import com.bocatta.pos.domain.model.TransactionModifier
import com.bocatta.pos.domain.model.TransactionV2
import com.bocatta.pos.domain.model.RecipeV2
import com.bocatta.pos.domain.repository.IInventoryRepository
import com.bocatta.pos.domain.repository.IProductRepository
import com.bocatta.pos.domain.repository.StockDeduction
import com.bocatta.pos.domain.util.TaxCalculator
import java.math.BigDecimal
import java.math.RoundingMode

class SalesFlowUseCase(
    private val productRepo: IProductRepository,
    private val inventoryRepo: IInventoryRepository,
    private val promotionsEngine: PromotionsEngineV2 = PromotionsEngineV2
) {

    suspend fun processSale(
        branchId: String,
        userId: String,
        items: List<SaleItemInput>,
        discounts: List<DiscountV2>,
        giro: String
    ): SaleResult {
        val transactionId = java.util.UUID.randomUUID().toString()
        val transactionItems = mutableListOf<TransactionItemV2>()
        var subtotalBD = BigDecimal.ZERO

        for (item in items) {
            val baseUnit = productRepo.getProductBaseUnit(item.productId)
            val unit = baseUnit ?: item.unit

            val unitPriceBD = BigDecimal.valueOf(item.unitPrice)
            val qtyBD = BigDecimal.valueOf(item.quantity)
            val itemSubtotalBD = unitPriceBD * qtyBD
            subtotalBD += itemSubtotalBD

            transactionItems.add(TransactionItemV2(
                productId = item.productId,
                name = item.name,
                quantity = item.quantity,
                unit = unit,
                unitPrice = item.unitPrice,
                subtotal = itemSubtotalBD.setScale(2, RoundingMode.HALF_UP).toDouble(),
                taxRate = item.taxRate,
                modifiers = item.modifiers
            ))
        }

        val subtotal = subtotalBD.setScale(2, RoundingMode.HALF_UP).toDouble()

        val itemCategories = items.map { it.category }.filterNotNull()
        val discountResults = promotionsEngine.calculate(
            subtotal = subtotal,
            itemCount = items.sumOf { it.quantity.toInt() },
            itemCategories = itemCategories,
            giro = giro,
            discounts = discounts
        )

        val discountTotal = discountResults.sumOf { BigDecimal.valueOf(it.amount) }
        val netSubtotal = subtotalBD - discountTotal
        val netSubtotalD = netSubtotal.setScale(2, RoundingMode.HALF_UP).toDouble()
        val discountTotalD = discountTotal.setScale(2, RoundingMode.HALF_UP).toDouble()

        val taxResult = TaxCalculator.calculatePerItem(
            subtotal = netSubtotalD + discountTotalD,
            discountTotal = discountTotalD,
            items = transactionItems
        )
        val taxTotalBD = BigDecimal.valueOf(taxResult.taxTotal)
        val grandTotalBD = netSubtotal + taxTotalBD

        val deductions = mutableListOf<StockDeduction>()
        for (item in items) {
            val recipe = item.recipe
            if (recipe != null) {
                val engine = BusinessLogicFactory.getProductionEngine(giro)
                val productionInputs = recipe.inputs.map { input ->
                    ProductionInput(
                        productId = input.insumoId,
                        qty = input.qty * item.quantity,
                        unit = input.unit
                    )
                }

                if (!engine.validateInputs(productionInputs)) {
                    return SaleResult(
                        success = false,
                        error = "Invalid inputs for recipe: ${recipe.id}"
                    )
                }

                val result = engine.execute(productionInputs, item.quantity, item.unit)
                if (!result.success) {
                    return SaleResult(
                        success = false,
                        error = result.error ?: "Production engine failed"
                    )
                }

                for (movement in result.movements) {
                    deductions.add(StockDeduction(
                        productId = movement.productId,
                        quantity = movement.quantity,
                        unit = movement.unit,
                        reason = "SALE_DEDUCTION"
                    ))
                }
            }
        }

        if (deductions.isNotEmpty()) {
            val batchSuccess = inventoryRepo.adjustStockBatch(
                branchId = branchId,
                userId = userId,
                referenceId = transactionId,
                deductions = deductions
            )
            if (!batchSuccess) {
                return SaleResult(
                    success = false,
                    error = "Error atómico al deducir inventario para transacción $transactionId"
                )
            }
        }

        val transaction = TransactionV2(
            id = transactionId,
            branchId = branchId,
            userId = userId,
            type = "SALE",
            status = "COMPLETED",
            subtotal = subtotalBD.setScale(2, RoundingMode.HALF_UP).toDouble(),
            discountTotal = discountTotal.setScale(2, RoundingMode.HALF_UP).toDouble(),
            taxTotal = taxTotalBD.setScale(2, RoundingMode.HALF_UP).toDouble(),
            grandTotal = grandTotalBD.setScale(2, RoundingMode.HALF_UP).toDouble(),
            items = transactionItems,
            createdAt = System.currentTimeMillis()
        )

        return SaleResult(
            success = true,
            transaction = transaction,
            discounts = discountResults
        )
    }
}

data class SaleItemInput(
    val productId: String,
    val name: String,
    val quantity: Double,
    val unit: String = "pza",
    val unitPrice: Double = 0.0,
    val taxRate: Double = 0.0,
    val category: String? = null,
    val recipe: RecipeV2? = null,
    val modifiers: List<TransactionModifier> = emptyList()
)

data class SaleResult(
    val success: Boolean,
    val transaction: TransactionV2? = null,
    val discounts: List<PromotionsEngineV2.DiscountResult> = emptyList(),
    val error: String? = null
)

