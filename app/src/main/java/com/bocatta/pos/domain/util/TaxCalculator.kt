package com.bocatta.pos.domain.util

import com.bocatta.pos.domain.model.TransactionItemV2
import java.math.BigDecimal

object TaxCalculator {

    fun calculatePerItem(
        subtotal: Double,
        discountTotal: Double,
        items: List<TransactionItemV2>
    ): TaxResult {
        if (subtotal <= 0.0) return TaxResult(taxTotal = 0.0, totalVerified = true)

        val discountRatio = if (subtotal > 0.0) {
            (subtotal - discountTotal) / subtotal
        } else {
            1.0
        }

        var runningTotal = 0.0
        val itemTaxes = mutableListOf<Double>()

        for (item in items) {
            val netItem = item.subtotal * discountRatio
            val itemTax = (netItem * item.taxRate).fiscalRound()
            itemTaxes.add(itemTax)
            runningTotal += itemTax
        }

        val taxTotal = runningTotal.fiscalRound()
        val sumItemTaxes = itemTaxes.sum().fiscalRound()
        val verified = BigDecimal.valueOf(taxTotal)
            .compareTo(BigDecimal.valueOf(sumItemTaxes)) == 0

        return TaxResult(taxTotal = taxTotal, totalVerified = verified)
    }
}

data class TaxResult(
    val taxTotal: Double,
    val totalVerified: Boolean
)

