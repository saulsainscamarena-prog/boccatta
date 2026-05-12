package com.bocatta.pos.domain.usecase

import com.bocatta.pos.domain.model.DiscountV2

object PromotionsEngineV2 {

    data class DiscountResult(
        val discountId: String,
        val name: String,
        val amount: Double,
        val type: String
    )

    fun calculate(
        subtotal: Double,
        itemCount: Int,
        itemCategories: List<String>,
        giro: String,
        discounts: List<DiscountV2>,
        customerMembershipId: String? = null
    ): List<DiscountResult> {
        val now = System.currentTimeMillis()
        val results = mutableListOf<DiscountResult>()
        var runningTotal = 0.0

        for (discount in discounts) {
            if (!discount.isActive) continue
            if (discount.endDate != null && now > discount.endDate) continue
            if (now < discount.startDate) continue

            val conditions = discount.conditions
            val minAmount = (conditions["monto"] as? Double) ?: 0.0
            val category = conditions["categoria"] as? String
            val discountGiro = conditions["giro"] as? String

            if (subtotal < minAmount) continue
            if (discountGiro != null && discountGiro != giro) continue
            if (category != null && category !in itemCategories) continue

            val remainingBudget = subtotal - runningTotal
            if (remainingBudget <= 0) break

            val rawAmount = when (discount.type) {
                "PERCENTAGE" -> subtotal * (discount.value / 100.0)
                "FIXED" -> discount.value
                "BUY_X_GET_Y" -> calculateBuyXGetY(discount, itemCount, subtotal)
                else -> 0.0
            }

            val amount = rawAmount.coerceIn(0.0, remainingBudget)

            if (amount > 0) {
                results.add(DiscountResult(
                    discountId = discount.id,
                    name = discount.name,
                    amount = amount,
                    type = discount.type
                ))
                runningTotal += amount
            }
        }

        return results
    }

    private fun calculateBuyXGetY(discount: DiscountV2, itemCount: Int, subtotal: Double): Double {
        val buyQty = (discount.conditions["buyQty"] as? Double)?.toInt() ?: 2
        val freeQty = (discount.conditions["freeQty"] as? Double)?.toInt() ?: 1
        if (itemCount < buyQty) return 0.0
        val sets = itemCount / (buyQty + freeQty)
        if (sets == 0) return 0.0
        val avgPrice = subtotal / itemCount
        return avgPrice * freeQty * sets
    }
}
