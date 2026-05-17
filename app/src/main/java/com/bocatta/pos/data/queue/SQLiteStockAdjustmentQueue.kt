package com.bocatta.pos.data.queue

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.bocatta.pos.domain.model.StockAdjustmentEntity
import com.bocatta.pos.domain.repository.IStockAdjustmentQueue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * SQLite‑based implementation of the offline stock‑adjustment queue.
 * All DB operations run on a single‑thread dispatcher to guarantee FIFO order.
 */
class SQLiteStockAdjustmentQueue(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION),
    IStockAdjustmentQueue {

    // Serialises all SQLite calls – prevents interleaved reads/writes.
    private val queueDispatcher = Dispatchers.IO.limitedParallelism(1)

    companion object {
        private const val DATABASE_NAME = "offline_queue.db"
        private const val DATABASE_VERSION = 2
        private const val TABLE_NAME = "stock_adjustments"

        private const val COL_ID = "id"
        private const val COL_BRANCH_ID = "branch_id"
        private const val COL_PRODUCT_ID = "product_id"
        private const val COL_QUANTITY = "quantity"
        private const val COL_UNIT = "unit"
        private const val COL_REASON = "reason"
        private const val COL_TIMESTAMP = "timestamp"
        private const val COL_STATUS = "status"      // 0=PENDING, 1=IN_PROGRESS
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE_NAME (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_BRANCH_ID TEXT NOT NULL,
                $COL_PRODUCT_ID TEXT NOT NULL,
                $COL_QUANTITY REAL NOT NULL,
                $COL_UNIT TEXT NOT NULL,
                $COL_REASON TEXT NOT NULL,
                $COL_TIMESTAMP INTEGER NOT NULL,
                $COL_STATUS INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL(
                "ALTER TABLE $TABLE_NAME ADD COLUMN $COL_STATUS INTEGER NOT NULL DEFAULT 0"
            )
        }
    }

    override suspend fun enqueue(adjustment: StockAdjustmentEntity): Long =
        withContext(queueDispatcher) {
            val values = ContentValues().apply {
                put(COL_BRANCH_ID, adjustment.branchId)
                put(COL_PRODUCT_ID, adjustment.productId)
                put(COL_QUANTITY, adjustment.quantity)
                put(COL_UNIT, adjustment.unit)
                put(COL_REASON, adjustment.reason)
                put(COL_TIMESTAMP, adjustment.timestamp)
                put(COL_STATUS, 0)
            }
            writableDatabase.insert(TABLE_NAME, null, values)
        }

    override suspend fun getAllPending(): List<StockAdjustmentEntity> =
        withContext(queueDispatcher) {
            val list = mutableListOf<StockAdjustmentEntity>()
            val cursor: Cursor = readableDatabase.query(
                TABLE_NAME,
                null,
                "$COL_STATUS = ?",
                arrayOf("0"),
                null,
                null,
                "$COL_TIMESTAMP ASC"
            )
            cursor.use {
                if (it.moveToFirst()) {
                    do {
                        list.add(cursorToEntity(it))
                    } while (it.moveToNext())
                }
            }
            list
        }

    override suspend fun deleteProcessed(ids: List<Long>) {
        if (ids.isEmpty()) return
        withContext(queueDispatcher) {
            val placeholders = ids.joinToString(",") { "?" }
            val args = ids.map { it.toString() }.toTypedArray()
            writableDatabase.execSQL(
                "DELETE FROM $TABLE_NAME WHERE $COL_ID IN ($placeholders)",
                args
            )
        }
    }

    override suspend fun clearAll() = withContext(queueDispatcher) {
        writableDatabase.delete(TABLE_NAME, null, null)
        Unit
    }

    override suspend fun markAsSyncing(ids: List<Long>) {
        if (ids.isEmpty()) return
        withContext(queueDispatcher) {
            val placeholders = ids.joinToString(",") { "?" }
            val args = ids.map { it.toString() }.toTypedArray()
            writableDatabase.execSQL(
                "UPDATE $TABLE_NAME SET $COL_STATUS = 1 WHERE $COL_ID IN ($placeholders)",
                args
            )
        }
    }

    override suspend fun resetSyncingState(ids: List<Long>) {
        if (ids.isEmpty()) return
        withContext(queueDispatcher) {
            val placeholders = ids.joinToString(",") { "?" }
            val args = ids.map { it.toString() }.toTypedArray()
            writableDatabase.execSQL(
                "UPDATE $TABLE_NAME SET $COL_STATUS = 0 WHERE $COL_ID IN ($placeholders)",
                args
            )
        }
    }

    private fun cursorToEntity(cursor: Cursor): StockAdjustmentEntity {
        return StockAdjustmentEntity(
            id = cursor.getLong(cursor.getColumnIndexOrThrow(COL_ID)),
            branchId = cursor.getString(cursor.getColumnIndexOrThrow(COL_BRANCH_ID)),
            productId = cursor.getString(cursor.getColumnIndexOrThrow(COL_PRODUCT_ID)),
            quantity = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_QUANTITY)),
            unit = cursor.getString(cursor.getColumnIndexOrThrow(COL_UNIT)),
            reason = cursor.getString(cursor.getColumnIndexOrThrow(COL_REASON)),
            timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(COL_TIMESTAMP)),
            status = cursor.getInt(cursor.getColumnIndexOrThrow(COL_STATUS))
        )
    }
}

