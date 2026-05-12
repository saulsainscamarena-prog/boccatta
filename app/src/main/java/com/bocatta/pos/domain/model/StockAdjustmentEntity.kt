package com.bocatta.pos.domain.model

/**
 * Represents a stock adjustment that is queued while the device is offline.
 * This class is deliberately free of any persistence annotations – the queue
 * implementation is handled by a native SQLiteOpenHelper.
 */
data class StockAdjustmentEntity(
    val id: Long? = null,
    val branchId: String,
    val productId: String,
    val quantity: Double,
    val unit: String,
    val reason: String,
    val timestamp: Long,
    val status: Int = 0 // 0=PENDING, 1=IN_PROGRESS
)
