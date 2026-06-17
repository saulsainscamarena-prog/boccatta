package com.bocatta.pos.data.sync

import org.junit.Assert.assertEquals
import org.junit.Test

class OfflineQueueSummaryTest {

    @Test
    fun `total outstanding includes every pending source and failed sales`() {
        val summary = OfflineQueueSummary(
            pendingSales = 2,
            failedSales = 1,
            pendingOperations = 3,
            pendingStockAdjustments = 4,
            pendingContingencyShifts = 1
        )

        assertEquals(10, summary.totalPending)
        assertEquals(11, summary.totalOutstanding)
    }
}
