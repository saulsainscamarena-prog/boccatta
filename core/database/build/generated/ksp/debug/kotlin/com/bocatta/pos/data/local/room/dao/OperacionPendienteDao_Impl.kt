package com.bocatta.pos.`data`.local.room.dao

import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import com.bocatta.pos.`data`.local.room.entity.OperacionPendienteEntity
import javax.`annotation`.processing.Generated
import kotlin.Boolean
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.Suppress
import kotlin.Unit
import kotlin.collections.List
import kotlin.collections.MutableList
import kotlin.collections.mutableListOf
import kotlin.reflect.KClass

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class OperacionPendienteDao_Impl(
  __db: RoomDatabase,
) : OperacionPendienteDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfOperacionPendienteEntity:
      EntityInsertAdapter<OperacionPendienteEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfOperacionPendienteEntity = object : EntityInsertAdapter<OperacionPendienteEntity>() {
      protected override fun createQuery(): String = "INSERT OR REPLACE INTO `operaciones_pendientes` (`id`,`tenantId`,`tipo`,`ventaId`,`motivo`,`usuarioId`,`sucursal`,`fecha`,`requiereAprobacion`,`dataJson`,`estado`,`intentos`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: OperacionPendienteEntity) {
        statement.bindText(1, entity.id)
        statement.bindText(2, entity.tenantId)
        statement.bindText(3, entity.tipo)
        val _tmpVentaId: String? = entity.ventaId
        if (_tmpVentaId == null) {
          statement.bindNull(4)
        } else {
          statement.bindText(4, _tmpVentaId)
        }
        statement.bindText(5, entity.motivo)
        statement.bindText(6, entity.usuarioId)
        statement.bindText(7, entity.sucursal)
        statement.bindLong(8, entity.fecha)
        val _tmp: Int = if (entity.requiereAprobacion) 1 else 0
        statement.bindLong(9, _tmp.toLong())
        statement.bindText(10, entity.dataJson)
        statement.bindText(11, entity.estado)
        statement.bindLong(12, entity.intentos.toLong())
      }
    }
  }

  public override suspend fun insertar(operacion: OperacionPendienteEntity): Unit = performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfOperacionPendienteEntity.insert(_connection, operacion)
  }

  public override suspend fun obtenerPendientes(): List<OperacionPendienteEntity> {
    val _sql: String = "SELECT * FROM operaciones_pendientes WHERE estado = 'pendiente' ORDER BY fecha ASC"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfTenantId: Int = getColumnIndexOrThrow(_stmt, "tenantId")
        val _columnIndexOfTipo: Int = getColumnIndexOrThrow(_stmt, "tipo")
        val _columnIndexOfVentaId: Int = getColumnIndexOrThrow(_stmt, "ventaId")
        val _columnIndexOfMotivo: Int = getColumnIndexOrThrow(_stmt, "motivo")
        val _columnIndexOfUsuarioId: Int = getColumnIndexOrThrow(_stmt, "usuarioId")
        val _columnIndexOfSucursal: Int = getColumnIndexOrThrow(_stmt, "sucursal")
        val _columnIndexOfFecha: Int = getColumnIndexOrThrow(_stmt, "fecha")
        val _columnIndexOfRequiereAprobacion: Int = getColumnIndexOrThrow(_stmt, "requiereAprobacion")
        val _columnIndexOfDataJson: Int = getColumnIndexOrThrow(_stmt, "dataJson")
        val _columnIndexOfEstado: Int = getColumnIndexOrThrow(_stmt, "estado")
        val _columnIndexOfIntentos: Int = getColumnIndexOrThrow(_stmt, "intentos")
        val _result: MutableList<OperacionPendienteEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: OperacionPendienteEntity
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpTenantId: String
          _tmpTenantId = _stmt.getText(_columnIndexOfTenantId)
          val _tmpTipo: String
          _tmpTipo = _stmt.getText(_columnIndexOfTipo)
          val _tmpVentaId: String?
          if (_stmt.isNull(_columnIndexOfVentaId)) {
            _tmpVentaId = null
          } else {
            _tmpVentaId = _stmt.getText(_columnIndexOfVentaId)
          }
          val _tmpMotivo: String
          _tmpMotivo = _stmt.getText(_columnIndexOfMotivo)
          val _tmpUsuarioId: String
          _tmpUsuarioId = _stmt.getText(_columnIndexOfUsuarioId)
          val _tmpSucursal: String
          _tmpSucursal = _stmt.getText(_columnIndexOfSucursal)
          val _tmpFecha: Long
          _tmpFecha = _stmt.getLong(_columnIndexOfFecha)
          val _tmpRequiereAprobacion: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfRequiereAprobacion).toInt()
          _tmpRequiereAprobacion = _tmp != 0
          val _tmpDataJson: String
          _tmpDataJson = _stmt.getText(_columnIndexOfDataJson)
          val _tmpEstado: String
          _tmpEstado = _stmt.getText(_columnIndexOfEstado)
          val _tmpIntentos: Int
          _tmpIntentos = _stmt.getLong(_columnIndexOfIntentos).toInt()
          _item = OperacionPendienteEntity(_tmpId,_tmpTenantId,_tmpTipo,_tmpVentaId,_tmpMotivo,_tmpUsuarioId,_tmpSucursal,_tmpFecha,_tmpRequiereAprobacion,_tmpDataJson,_tmpEstado,_tmpIntentos)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun marcarSincronizada(id: String) {
    val _sql: String = "UPDATE operaciones_pendientes SET estado = 'sincronizada', intentos = intentos + 1 WHERE id = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, id)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun registrarIntentoFallido(id: String) {
    val _sql: String = "UPDATE operaciones_pendientes SET estado = 'pendiente', intentos = intentos + 1 WHERE id = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, id)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun marcarFallida(id: String) {
    val _sql: String = "UPDATE operaciones_pendientes SET estado = 'fallida', intentos = intentos + 1 WHERE id = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, id)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun limpiarSincronizadas() {
    val _sql: String = "DELETE FROM operaciones_pendientes WHERE estado = 'sincronizada'"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public companion object {
    public fun getRequiredConverters(): List<KClass<*>> = emptyList()
  }
}
