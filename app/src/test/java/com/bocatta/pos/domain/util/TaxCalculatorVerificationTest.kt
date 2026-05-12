package com.bocatta.pos.domain.util

import com.bocatta.pos.domain.model.TransactionItemV2
import org.junit.Assert.assertTrue
import org.junit.Test

class TaxCalculatorVerificationTest {

    @Test
    fun `tax verification must be exact despite rounding edge cases`() {
        val items = listOf(
            TransactionItemV2(productId = "1", subtotal = 9.99, taxRate = 0.16),
            TransactionItemV2(productId = "2", subtotal = 0.01, taxRate = 0.16)
        )
        val result = TaxCalculator.calculatePerItem(
            subtotal = 10.0,
            discountTotal = 0.0,
            items = items
        )
        assertTrue(result.totalVerified)
    }
}
