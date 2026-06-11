package com.bocatta.pos.`data`.local.room.dao

import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import com.bocatta.pos.`data`.local.room.entity.FolioEntity
import javax.`annotation`.processing.Generated
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.Suppress
import kotlin.Unit
import kotlin.collections.List
import kotlin.reflect.KClass

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class FolioDao_Impl(
  __db: RoomDatabase,
) : FolioDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfFolioEntity: EntityInsertAdapter<FolioEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfFolioEntity = object : EntityInsertAdapter<FolioEntity>() {
      protected override fun createQuery(): String = "INSERT OR REPLACE INTO `folios_offline` (`tenantId`,`sucursal`,`ultimoTicket`,`updatedAt`) VALUES (?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: FolioEntity) {
        statement.bindText(1, entity.tenantId)
        statement.bindText(2, entity.sucursal)
        statement.bindLong(3, entity.ultimoTicket)
        statement.bindLong(4, entity.updatedAt)
      }
    }
  }

  public override suspend fun guardarFolio(folio: FolioEntity): Unit = performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfFolioEntity.insert(_connection, folio)
  }

  public override suspend fun obtenerUltimoTicket(sucursalId: String): Long? {
    val _sql: String = "SELECT ultimoTicket FROM folios_offline WHERE sucursal = ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, sucursalId)
        val _result: Long?
        if (_stmt.step()) {
          if (_stmt.isNull(0)) {
            _result = null
          } else {
            _result = _stmt.getLong(0)
          }
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun obtenerFolio(sucursalId: String): FolioEntity? {
    val _sql: String = "SELECT * FROM folios_offline WHERE sucursal = ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, sucursalId)
        val _columnIndexOfTenantId: Int = getColumnIndexOrThrow(_stmt, "tenantId")
        val _columnIndexOfSucursal: Int = getColumnIndexOrThrow(_stmt, "sucursal")
        val _columnIndexOfUltimoTicket: Int = getColumnIndexOrThrow(_stmt, "ultimoTicket")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updatedAt")
        val _result: FolioEntity?
        if (_stmt.step()) {
          val _tmpTenantId: String
          _tmpTenantId = _stmt.getText(_columnIndexOfTenantId)
          val _tmpSucursal: String
          _tmpSucursal = _stmt.getText(_columnIndexOfSucursal)
          val _tmpUltimoTicket: Long
          _tmpUltimoTicket = _stmt.getLong(_columnIndexOfUltimoTicket)
          val _tmpUpdatedAt: Long
          _tmpUpdatedAt = _stmt.getLong(_columnIndexOfUpdatedAt)
          _result = FolioEntity(_tmpTenantId,_tmpSucursal,_tmpUltimoTicket,_tmpUpdatedAt)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public companion object {
    public fun getRequiredConverters(): List<KClass<*>> = emptyList()
  }
}
