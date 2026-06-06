package com.bocatta.pos.data.queue

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.bocatta.pos.domain.model.StockAdjustmentEntity
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlinx.coroutines.runBlocking

@RunWith(AndroidJUnit4::class)
class SQLiteStockAdjustmentQueueInstrumentedTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        context.deleteDatabase(DATABASE_NAME)
    }

    @After
    fun teardown() {
        context.deleteDatabase(DATABASE_NAME)
    }

    @Test
    fun resetStaleSyncing_recoversRowsMarkedInProgress() = runBlocking {
        val queue = SQLiteStockAdjustmentQueue(context)
        val id = queue.enqueue(adjustment("branch", "product"))

        queue.markAsSyncing(listOf(id))
        assertEquals(0, queue.getAllPending().size)

        val recovered = queue.resetStaleSyncing(-1)

        assertEquals(1, recovered)
        assertEquals(listOf(id), queue.getAllPending().mapNotNull { it.id })
    }

    @Test
    fun databaseUpgradeFromVersionTwoAddsRecoveryColumn() = runBlocking {
        createVersionTwoDatabase()

        val queue = SQLiteStockAdjustmentQueue(context)
        val id = queue.enqueue(adjustment("branch", "product"))
        queue.markAsSyncing(listOf(id))

        val recovered = queue.resetStaleSyncing(-1)

        assertEquals(1, recovered)
        assertEquals(1, queue.getAllPending().size)
    }

    private fun createVersionTwoDatabase() {
        context.openOrCreateDatabase(DATABASE_NAME, Context.MODE_PRIVATE, null).use { db ->
            db.execSQL(
                """
                CREATE TABLE stock_adjustments (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    branch_id TEXT NOT NULL,
                    product_id TEXT NOT NULL,
                    quantity REAL NOT NULL,
                    unit TEXT NOT NULL,
                    reason TEXT NOT NULL,
                    timestamp INTEGER NOT NULL,
                    status INTEGER NOT NULL DEFAULT 0
                )
                """.trimIndent()
            )
            db.version = 2
        }
    }

    private fun adjustment(branchId: String, productId: String) = StockAdjustmentEntity(
        branchId = branchId,
        productId = productId,
        quantity = 1.0,
        unit = "pz",
        reason = "TEST",
        timestamp = System.currentTimeMillis()
    )

    private companion object {
        const val DATABASE_NAME = "offline_queue.db"
    }
}
