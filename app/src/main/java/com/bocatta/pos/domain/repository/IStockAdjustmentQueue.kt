package com.bocatta.pos.domain.repository

import com.bocatta.pos.domain.model.StockAdjustmentEntity

/**
 * Abstract contract for a persistent queue of stock adjustments.
 * The implementation will be a thin SQLiteOpenHelper that stores the
 * adjustments locally when the device is offline.
 */
interface IStockAdjustmentQueue {
    /**
     * Inserts a new adjustment into the queue. Returns the generated row‑id.
     */
    suspend fun enqueue(adjustment: StockAdjustmentEntity): Long

    /**
     * Retrieves **all** pending adjustments ordered by timestamp (oldest first).
     */
    suspend fun getAllPending(): List<StockAdjustmentEntity>

    /**
     * Removes the rows whose primary‑key ids are supplied. Used after successful sync.
     */
    suspend fun deleteProcessed(ids: List<Long>)

    /**
     * Marks the given rows as IN_PROGRESS (status = 1).
     */
    suspend fun markAsSyncing(ids: List<Long>)

    /**
     * Resets the status of the given rows back to PENDING (status = 0).
     * Used when a sync attempt fails for some rows.
     */
    suspend fun resetSyncingState(ids: List<Long>)

    /**
     * Clears the entire queue – useful for debugging or a hard reset.
     */
    suspend fun clearAll()

}
