package com.bocatta.pos.data.queue

import com.bocatta.pos.domain.model.StockAdjustmentEntity
import com.bocatta.pos.domain.repository.IStockAdjustmentQueue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.Collections
import java.util.LinkedList
import java.util.Queue

class StockAdjustmentQueueStressTest {

    private lateinit var queue: InMemoryStockAdjustmentQueue

    @Before
    fun setup() {
        queue = InMemoryStockAdjustmentQueue()
    }

    @After
    fun teardown() {
        runBlocking { queue.clearAll() }
    }

    @Test
    fun enqueue_singleItem_returnsPositiveId() = runBlocking {
        val id = enqueueItem("branch-1", "prod-1", 10.0, "pza", "SALE")
        assertTrue("ID should be positive", id > 0)
    }

    @Test
    fun enqueueAndGetPending_returnsItemsInFifoOrder() = runBlocking {
        val id1 = enqueueItem("branch-1", "prod-1", 10.0, "pza", "SALE")
        val id2 = enqueueItem("branch-1", "prod-2", 5.0, "kg", "MERMA")
        val id3 = enqueueItem("branch-1", "prod-3", 2.0, "L", "ADJUSTMENT")

        val pending = queue.getAllPending()
        assertEquals(3, pending.size)
        assertEquals(id1, pending[0].id)
        assertEquals(id2, pending[1].id)
        assertEquals(id3, pending[2].id)
    }

    @Test
    fun markAsSyncing_changesStatusToInProgress() = runBlocking {
        val id1 = enqueueItem("branch-1", "prod-1", 10.0, "pza", "SALE")
        val id2 = enqueueItem("branch-1", "prod-2", 5.0, "kg", "MERMA")

        queue.markAsSyncing(listOf(id1))

        val pending = queue.getAllPending()
        assertEquals(1, pending.size)
        assertEquals(id2, pending[0].id)
    }

    @Test
    fun resetSyncingState_restoresPendingStatus() = runBlocking {
        val id1 = enqueueItem("branch-1", "prod-1", 10.0, "pza", "SALE")

        queue.markAsSyncing(listOf(id1))
        queue.resetSyncingState(listOf(id1))

        val pending = queue.getAllPending()
        assertEquals(1, pending.size)
        assertEquals(id1, pending[0].id)
    }

    @Test
    fun deleteProcessed_removesItemsFromQueue() = runBlocking {
        val id1 = enqueueItem("branch-1", "prod-1", 10.0, "pza", "SALE")

        queue.markAsSyncing(listOf(id1))
        queue.deleteProcessed(listOf(id1))

        val pending = queue.getAllPending()
        assertTrue("Queue should be empty after delete", pending.isEmpty())
    }

    @Test
    fun fullSyncCycle_simulatesTwoPhaseCommit() = runBlocking {
        val ids = (1..10).map { enqueueItem("branch-1", "prod-$it", it * 1.0, "pza", "SALE") }

        val pending1 = queue.getAllPending()
        assertEquals(10, pending1.size)

        queue.markAsSyncing(ids)
        assertEquals(0, queue.getAllPending().size)

        val failedIds = ids.filter { it % 2 == 0L }
        val succeededIds = ids.filter { it % 2 != 0L }

        queue.deleteProcessed(succeededIds)
        queue.resetSyncingState(failedIds)

        val pending2 = queue.getAllPending()
        assertEquals(failedIds.size, pending2.size)
        assertEquals(failedIds.sorted(), pending2.mapNotNull { it.id }.sorted())
    }

    @Test
    fun concurrentEnqueue_maintainsAllItems() = runBlocking {
        val concurrency = 20
        val deferreds = (1..concurrency).map { i ->
            async {
                enqueueItem("branch-1", "prod-$i", i * 1.0, "pza", "CONCURRENT")
            }
        }
        val ids = deferreds.awaitAll()
        assertEquals(concurrency, ids.size)
        assertEquals(concurrency, ids.distinct().size)

        val pending = queue.getAllPending()
        assertEquals(concurrency, pending.size)
    }

    @Test
    fun stress_concurrentEnqueueAndMark() = runBlocking {
        val totalItems = 50
        val ids = (1..totalItems).map { i ->
            async { enqueueItem("branch-1", "prod-$i", i * 1.0, "pza", "STRESS") }
        }.awaitAll()

        val markTasks = ids.chunked(5).map { batch ->
            async {
                queue.markAsSyncing(batch)
                queue.deleteProcessed(batch)
            }
        }
        markTasks.awaitAll()

        val remaining = queue.getAllPending()
        assertTrue("All items should be processed", remaining.isEmpty())
    }

    @Test
    fun emptyQueue_returnsEmptyList() = runBlocking {
        val pending = queue.getAllPending()
        assertTrue(pending.isEmpty())
    }

    @Test
    fun deleteProcessed_emptyList_doesNotThrow() = runBlocking {
        queue.deleteProcessed(emptyList())
        queue.markAsSyncing(emptyList())
        queue.resetSyncingState(emptyList())
    }

    @Test
    fun enqueue_largeQuantity_roundTripsCorrectly() = runBlocking {
        val id = enqueueItem("branch-1", "prod-1", 999999.999, "kg", "BULK")
        val pending = queue.getAllPending()
        assertEquals(999999.999, pending[0].quantity, 0.001)
        assertEquals("kg", pending[0].unit)
    }

    @Test
    fun clearAll_removesAllItems() = runBlocking {
        repeat(10) { enqueueItem("branch-1", "prod-$it", 1.0, "pza", "SALE") }
        assertEquals(10, queue.getAllPending().size)
        queue.clearAll()
        assertEquals(0, queue.getAllPending().size)
    }

    @Test
    fun multipleBranches_keepsBranchSeparationInEntity() = runBlocking {
        val id1 = enqueueItem("branch-a", "prod-1", 10.0, "pza", "SALE")
        val id2 = enqueueItem("branch-b", "prod-1", 5.0, "pza", "SALE")

        val pending = queue.getAllPending()
        assertEquals(2, pending.size)
        assertEquals("branch-a", pending.find { it.id == id1 }?.branchId)
        assertEquals("branch-b", pending.find { it.id == id2 }?.branchId)
    }

    @Test
    fun recoveryAfterCrash_marksInProgressAsPending() = runBlocking {
        val ids = (1..5).map { enqueueItem("branch-1", "prod-$it", 1.0, "pza", "SALE") }

        queue.markAsSyncing(ids)

        queue.resetSyncingState(ids)

        val pending = queue.getAllPending()
        assertEquals(5, pending.size)
        assertEquals(ids.sorted(), pending.mapNotNull { it.id }.sorted())
    }

    private suspend fun enqueueItem(
        branchId: String,
        productId: String,
        quantity: Double,
        unit: String,
        reason: String
    ): Long {
        val entity = StockAdjustmentEntity(
            branchId = branchId,
            productId = productId,
            quantity = quantity,
            unit = unit,
            reason = reason,
            timestamp = System.currentTimeMillis()
        )
        return queue.enqueue(entity)
    }
}

class InMemoryStockAdjustmentQueue : IStockAdjustmentQueue {

    private data class Entry(
        val id: Long,
        val branchId: String,
        val productId: String,
        val quantity: Double,
        val unit: String,
        val reason: String,
        val timestamp: Long,
        val status: Int = 0
    )

    private val store = mutableListOf<Entry>()
    private var nextId = 1L
    private val lock = Any()

    override suspend fun enqueue(adjustment: StockAdjustmentEntity): Long = withContext(Dispatchers.Default) {
        synchronized(lock) {
            val id = nextId++
            store.add(
                Entry(
                    id = id,
                    branchId = adjustment.branchId,
                    productId = adjustment.productId,
                    quantity = adjustment.quantity,
                    unit = adjustment.unit,
                    reason = adjustment.reason,
                    timestamp = adjustment.timestamp,
                    status = 0
                )
            )
            id
        }
    }

    override suspend fun getAllPending(): List<StockAdjustmentEntity> = withContext(Dispatchers.Default) {
        synchronized(lock) {
            store
                .filter { it.status == 0 }
                .sortedBy { it.timestamp }
                .map { it.toEntity() }
        }
    }

    override suspend fun deleteProcessed(ids: List<Long>) {
        if (ids.isEmpty()) return
        withContext(Dispatchers.Default) {
            synchronized(lock) {
                store.removeAll { it.id in ids }
            }
        }
    }

    override suspend fun markAsSyncing(ids: List<Long>) {
        if (ids.isEmpty()) return
        withContext(Dispatchers.Default) {
            synchronized(lock) {
                val idSet = ids.toSet()
                store.replaceAll { entry ->
                    if (entry.id in idSet) entry.copy(status = 1) else entry
                }
            }
        }
    }

    override suspend fun resetSyncingState(ids: List<Long>) {
        if (ids.isEmpty()) return
        withContext(Dispatchers.Default) {
            synchronized(lock) {
                val idSet = ids.toSet()
                store.replaceAll { entry ->
                    if (entry.id in idSet) entry.copy(status = 0) else entry
                }
            }
        }
    }

    override suspend fun clearAll() = withContext(Dispatchers.Default) {
        synchronized(lock) {
            store.clear()
        }
    }

    private fun Entry.toEntity() = StockAdjustmentEntity(
        id = id,
        branchId = branchId,
        productId = productId,
        quantity = quantity,
        unit = unit,
        reason = reason,
        timestamp = timestamp,
        status = status
    )
}
