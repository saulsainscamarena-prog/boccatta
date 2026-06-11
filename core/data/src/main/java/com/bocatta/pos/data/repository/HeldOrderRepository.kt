package com.bocatta.pos.data.repository

import android.content.ContentValues
import android.database.Cursor
import com.bocatta.pos.data.local.OfflineDatabase
import com.bocatta.pos.domain.model.HeldOrder
import com.bocatta.pos.domain.repository.IHeldOrderRepository

class HeldOrderRepository(private val dbHelper: OfflineDatabase) : IHeldOrderRepository {

    override fun save(order: HeldOrder) {
        val db = dbHelper.writableDatabase
        db.insertWithOnConflict(TABLE, null, order.toContentValues(), android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE)
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

    override fun assignToMesa(id: String, mesaId: String): HeldOrder? {
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            val current = db.query(TABLE, null, "id = ?", arrayOf(id), null, null, null).use { cursor ->
                if (cursor.moveToFirst()) fromCursor(cursor) else null
            } ?: return null

            val updated = current.copy(
                modalidad = "LOCAL",
                mesaId = mesaId
            )
            db.update(TABLE, updated.toContentValues(), "id = ?", arrayOf(id))
            db.setTransactionSuccessful()
            return updated
        } finally {
            db.endTransaction()
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

    private fun fromCursor(c: Cursor): HeldOrder {
        val id = c.getString(c.getColumnIndexOrThrow("id"))
        val carritoJson = c.getString(c.getColumnIndexOrThrow("carritoJson"))
        val clienteJson = c.getString(c.getColumnIndexOrThrow("clienteJson"))
        val nota = c.getString(c.getColumnIndexOrThrow("nota"))
        val fecha = c.getLong(c.getColumnIndexOrThrow("fecha"))
        val sucursal = c.getString(c.getColumnIndexOrThrow("sucursal"))
        val total = c.getDouble(c.getColumnIndexOrThrow("total"))

        val modIndex = c.getColumnIndex("modalidad")
        val modalidad = if (modIndex >= 0) c.getString(modIndex) ?: "LOCAL" else "LOCAL"

        val mesaIndex = c.getColumnIndex("mesaId")
        val mesaId = if (mesaIndex >= 0) c.getString(mesaIndex) else null

        return HeldOrder(
            id = id,
            carritoJson = carritoJson,
            clienteJson = clienteJson,
            nota = nota,
            fecha = fecha,
            sucursal = sucursal,
            total = total,
            modalidad = modalidad,
            mesaId = mesaId
        )
    }

    private fun HeldOrder.toContentValues(): ContentValues = ContentValues().apply {
        put("id", id)
        put("carritoJson", carritoJson)
        put("clienteJson", clienteJson)
        put("nota", nota)
        put("fecha", fecha)
        put("sucursal", sucursal)
        put("total", total)
        put("modalidad", modalidad)
        put("mesaId", mesaId)
    }

    companion object {
        private const val TABLE = OfflineDatabase.TABLE_HELD_ORDERS
    }
}
