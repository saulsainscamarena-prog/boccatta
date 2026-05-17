package com.bocatta.pos.domain.model

/**
 * Represents a stock adjustment that is queued while the device is offline.
 * This class is deliberately free of any persistence annotations – the queue
 * implementation is handled by a native SQLiteOpenHelper.
 */
data class StockAdjustmentEntity(
    val id: Long? = null,
    val branchId: String = "",
    val productId: String = "",
    val quantity: Double = 0.0,
    val unit: String = "pz",
    val reason: String = "",
    val timestamp: Long = 0L,
    val status: Int = 0
)

