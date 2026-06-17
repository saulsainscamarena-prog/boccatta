package com.bocatta.pos.feature.ventas.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QuickCashAmountsTest {

    @Test
    fun `exact amount preserves decimal total`() {
        val options = quickCashAmounts(47.50)

        assertEquals(47.50, options.first(), 0.0)
        assertTrue(options.contains(50.0))
    }

    @Test
    fun `options are sorted unique and limited`() {
        val options = quickCashAmounts(20.0)

        assertEquals(options.distinct(), options)
        assertEquals(options.sorted(), options)
        assertTrue(options.size <= 7)
    }

    @Test
    fun `invalid total has no options`() {
        assertTrue(quickCashAmounts(0.0).isEmpty())
        assertTrue(quickCashAmounts(Double.NaN).isEmpty())
    }
}
