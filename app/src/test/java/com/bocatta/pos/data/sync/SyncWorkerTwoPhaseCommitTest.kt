package com.bocatta.pos.data.sync

import com.bocatta.pos.data.queue.InMemoryStockAdjustmentQueue
import com.bocatta.pos.domain.model.StockAdjustmentEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class SyncWorkerTwoPhaseCommitTest {

    private lateinit var queue: InMemoryStockAdjustmentQueue

    @Before
    fun setup() {
        queue = InMemoryStockAdjustmentQueue()
    }

    @After
    fun teardown() = runBlocking { queue.clearAll() }

    @Test
    fun markAsSyncing_pendingDecreases() = runBlocking {
        repeat(5) { enqueue() }
        val allPending = queue.getAllPending()
        val ids = allPending.mapNotNull { it.id }
        assertEquals(5, allPending.size)

        queue.markAsSyncing(ids)
        assertEquals(0, queue.getAllPending().size)
    }

    @Test
    fun deleteProcessed_removesOnlyCompleted() = runBlocking {
        val ids = (1..5).map { enqueue() }
        queue.markAsSyncing(ids)
        val succeeded = ids.take(3)
        val failed = ids.drop(3)

        queue.deleteProcessed(succeeded)
        queue.resetSyncingState(failed)

        val remaining = queue.getAllPending()
        assertEquals(2, remaining.size)
        assertEquals(failed.sorted(), remaining.mapNotNull { it.id }.sorted())
    }

    @Test
    fun crashAfterMarkAsSyncing_retryResetsToPending() = runBlocking {
        val ids = (1..3).map { enqueue() }
        assertEquals(3, queue.getAllPending().size)

        queue.markAsSyncing(ids)

        val recovered = queue.resetStaleSyncing(0)

        assertEquals(3, recovered)
        assertEquals(3, queue.getAllPending().size)
    }

    @Test
    fun partialFailure_keepsFailedItemsInQueue() = runBlocking {
        val ids = (1..10).map { enqueue() }

        queue.markAsSyncing(ids)
        val processed = mutableListOf<Long>()

        for (id in ids) {
            if (id % 2 == 0L) {
                processed.add(id)
            }
        }

        queue.deleteProcessed(processed)
        val failed = ids - processed.toSet()
        queue.resetSyncingState(failed.toList())

        val remaining = queue.getAllPending()
        assertEquals(failed.size, remaining.size)
    }

    @Test
    fun emptyQueue_noOperationsFail() = runBlocking {
        queue.markAsSyncing(emptyList())
        queue.deleteProcessed(emptyList())
        queue.resetSyncingState(emptyList())
        assertTrue(queue.getAllPending().isEmpty())
    }

    @Test
    fun multipleSyncCycles_maintainConsistency() = runBlocking {
        repeat(3) { cycle ->
            repeat(5) { enqueue() }
            val pending = queue.getAllPending()
            val ids = pending.mapNotNull { it.id }
            queue.markAsSyncing(ids)

            val succeeded = ids.filter { it % 2 == 0L }
            val failedIds = ids - succeeded.toSet()

            queue.deleteProcessed(succeeded)
            queue.resetSyncingState(failedIds.toList())
        }

        assertTrue(queue.getAllPending().isNotEmpty())
    }

    private suspend fun enqueue(): Long {
        val entity = StockAdjustmentEntity(
            branchId = "test-branch",
            productId = "prod-${System.nanoTime()}",
            quantity = 1.0,
            unit = "pza",
            reason = "TEST",
            timestamp = System.currentTimeMillis()
        )
        return queue.enqueue(entity)
    }
}
