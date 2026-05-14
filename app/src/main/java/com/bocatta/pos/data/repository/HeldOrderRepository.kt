package com.bocatta.pos.data.repository

import android.content.ContentValues
import android.database.Cursor
import com.bocatta.pos.data.local.OfflineDatabase
import com.bocatta.pos.domain.model.HeldOrder
import com.bocatta.pos.domain.repository.IHeldOrderRepository

class HeldOrderRepository(private val dbHelper: OfflineDatabase) : IHeldOrderRepository {

    override fun save(order: HeldOrder) {
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put("id", order.id)
            put("carritoJson", order.carritoJson)
            put("clienteJson", order.clienteJson)
            put("nota", order.nota)
            put("fecha", order.fecha)
            put("sucursal", order.sucursal)
            put("total", order.total)
        }
        db.insertWithOnConflict(TABLE, null, cv, android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE)
    }

    override fun getAll(): List<HeldOrder> {
        val db = dbHelper.readableDatabase
        val cursor: Cursor = db.query(TABLE, null, null, null, null, null, "fecha DESC")
        val orders = mutableListOf<HeldOrder>()
        cursor.use { c ->
            while (c.moveToNext()) {
                orders.add(fromCursor(c))
            }
        }
        return orders
    }

    override fun getById(id: String): HeldOrder? {
        val db = dbHelper.readableDatabase
        val cursor = db.query(TABLE, null, "id = ?", arrayOf(id), null, null, null)
        return cursor.use { c ->
            if (c.moveToFirst()) fromCursor(c) else null
        }
    }

    override fun delete(id: String) {
        val db = dbHelper.writableDatabase
        db.delete(TABLE, "id = ?", arrayOf(id))
    }

    override fun clear() {
        val db = dbHelper.writableDatabase
        db.delete(TABLE, null, null)
    }

    private fun fromCursor(c: Cursor): HeldOrder = HeldOrder(
        id = c.getString(c.getColumnIndexOrThrow("id")),
        carritoJson = c.getString(c.getColumnIndexOrThrow("carritoJson")),
        clienteJson = c.getString(c.getColumnIndexOrThrow("clienteJson")),
        nota = c.getString(c.getColumnIndexOrThrow("nota")),
        fecha = c.getLong(c.getColumnIndexOrThrow("fecha")),
        sucursal = c.getString(c.getColumnIndexOrThrow("sucursal")),
        total = c.getDouble(c.getColumnIndexOrThrow("total"))
    )

    companion object {
        private const val TABLE = OfflineDatabase.TABLE_HELD_ORDERS
    }
}
