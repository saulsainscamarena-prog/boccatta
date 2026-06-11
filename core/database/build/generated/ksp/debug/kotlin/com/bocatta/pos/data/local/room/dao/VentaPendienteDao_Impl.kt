package com.bocatta.pos.`data`.local.room.dao

import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import com.bocatta.pos.`data`.local.room.entity.VentaPendienteEntity
import javax.`annotation`.processing.Generated
import kotlin.Boolean
import kotlin.Double
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.Suppress
import kotlin.Unit
import kotlin.collections.List
import kotlin.collections.MutableList
import kotlin.collections.mutableListOf
import kotlin.reflect.KClass
import kotlinx.coroutines.flow.Flow

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class VentaPendienteDao_Impl(
  __db: RoomDatabase,
) : VentaPendienteDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfVentaPendienteEntity: EntityInsertAdapter<VentaPendienteEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfVentaPendienteEntity = object : EntityInsertAdapter<VentaPendienteEntity>() {
      protected override fun createQuery(): String = "INSERT OR REPLACE INTO `ventas_pendientes` (`id`,`tenantId`,`ticket`,`codigoTicket`,`total`,`descuentoLealtad`,`descuentoPromociones`,`descuentoManual`,`propina`,`notaOrden`,`fecha`,`sucursal`,`atendio`,`metodoPago`,`esConsumoEmpleado`,`clienteId`,`carritoJson`,`estado`,`intentos`,`ultimoIntento`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: VentaPendienteEntity) {
        statement.bindText(1, entity.id)
        statement.bindText(2, entity.tenantId)
        statement.bindLong(3, entity.ticket)
        statement.bindText(4, entity.codigoTicket)
        statement.bindDouble(5, entity.total)
        statement.bindDouble(6, entity.descuentoLealtad)
        statement.bindDouble(7, entity.descuentoPromociones)
        statement.bindDouble(8, entity.descuentoManual)
        statement.bindDouble(9, entity.propina)
        statement.bindText(10, entity.notaOrden)
        statement.bindLong(11, entity.fecha)
        statement.bindText(12, entity.sucursal)
        statement.bindText(13, entity.atendio)
        statement.bindText(14, entity.metodoPago)
        val _tmp: Int = if (entity.esConsumoEmpleado) 1 else 0
        statement.bindLong(15, _tmp.toLong())
        val _tmpClienteId: String? = entity.clienteId
        if (_tmpClienteId == null) {
          statement.bindNull(16)
        } else {
          statement.bindText(16, _tmpClienteId)
        }
        statement.bindText(17, entity.carritoJson)
        statement.bindText(18, entity.estado)
        statement.bindLong(19, entity.intentos.toLong())
        val _tmpUltimoIntento: Long? = entity.ultimoIntento
        if (_tmpUltimoIntento == null) {
          statement.bindNull(20)
        } else {
          statement.bindLong(20, _tmpUltimoIntento)
        }
      }
    }
  }

  public override suspend fun insertar(venta: VentaPendienteEntity): Unit = performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfVentaPendienteEntity.insert(_connection, venta)
  }

  public override suspend fun obtenerPendientes(): List<VentaPendienteEntity> {
    val _sql: String = "SELECT * FROM ventas_pendientes WHERE estado = 'pendiente' ORDER BY fecha ASC"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfTenantId: Int = getColumnIndexOrThrow(_stmt, "tenantId")
        val _columnIndexOfTicket: Int = getColumnIndexOrThrow(_stmt, "ticket")
        val _columnIndexOfCodigoTicket: Int = getColumnIndexOrThrow(_stmt, "codigoTicket")
        val _columnIndexOfTotal: Int = getColumnIndexOrThrow(_stmt, "total")
        val _columnIndexOfDescuentoLealtad: Int = getColumnIndexOrThrow(_stmt, "descuentoLealtad")
        val _columnIndexOfDescuentoPromociones: Int = getColumnIndexOrThrow(_stmt, "descuentoPromociones")
        val _columnIndexOfDescuentoManual: Int = getColumnIndexOrThrow(_stmt, "descuentoManual")
        val _columnIndexOfPropina: Int = getColumnIndexOrThrow(_stmt, "propina")
        val _columnIndexOfNotaOrden: Int = getColumnIndexOrThrow(_stmt, "notaOrden")
        val _columnIndexOfFecha: Int = getColumnIndexOrThrow(_stmt, "fecha")
        val _columnIndexOfSucursal: Int = getColumnIndexOrThrow(_stmt, "sucursal")
        val _columnIndexOfAtendio: Int = getColumnIndexOrThrow(_stmt, "atendio")
        val _columnIndexOfMetodoPago: Int = getColumnIndexOrThrow(_stmt, "metodoPago")
        val _columnIndexOfEsConsumoEmpleado: Int = getColumnIndexOrThrow(_stmt, "esConsumoEmpleado")
        val _columnIndexOfClienteId: Int = getColumnIndexOrThrow(_stmt, "clienteId")
        val _columnIndexOfCarritoJson: Int = getColumnIndexOrThrow(_stmt, "carritoJson")
        val _columnIndexOfEstado: Int = getColumnIndexOrThrow(_stmt, "estado")
        val _columnIndexOfIntentos: Int = getColumnIndexOrThrow(_stmt, "intentos")
        val _columnIndexOfUltimoIntento: Int = getColumnIndexOrThrow(_stmt, "ultimoIntento")
        val _result: MutableList<VentaPendienteEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: VentaPendienteEntity
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpTenantId: String
          _tmpTenantId = _stmt.getText(_columnIndexOfTenantId)
          val _tmpTicket: Long
          _tmpTicket = _stmt.getLong(_columnIndexOfTicket)
          val _tmpCodigoTicket: String
          _tmpCodigoTicket = _stmt.getText(_columnIndexOfCodigoTicket)
          val _tmpTotal: Double
          _tmpTotal = _stmt.getDouble(_columnIndexOfTotal)
          val _tmpDescuentoLealtad: Double
          _tmpDescuentoLealtad = _stmt.getDouble(_columnIndexOfDescuentoLealtad)
          val _tmpDescuentoPromociones: Double
          _tmpDescuentoPromociones = _stmt.getDouble(_columnIndexOfDescuentoPromociones)
          val _tmpDescuentoManual: Double
          _tmpDescuentoManual = _stmt.getDouble(_columnIndexOfDescuentoManual)
          val _tmpPropina: Double
          _tmpPropina = _stmt.getDouble(_columnIndexOfPropina)
          val _tmpNotaOrden: String
          _tmpNotaOrden = _stmt.getText(_columnIndexOfNotaOrden)
          val _tmpFecha: Long
          _tmpFecha = _stmt.getLong(_columnIndexOfFecha)
          val _tmpSucursal: String
          _tmpSucursal = _stmt.getText(_columnIndexOfSucursal)
          val _tmpAtendio: String
          _tmpAtendio = _stmt.getText(_columnIndexOfAtendio)
          val _tmpMetodoPago: String
          _tmpMetodoPago = _stmt.getText(_columnIndexOfMetodoPago)
          val _tmpEsConsumoEmpleado: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfEsConsumoEmpleado).toInt()
          _tmpEsConsumoEmpleado = _tmp != 0
          val _tmpClienteId: String?
          if (_stmt.isNull(_columnIndexOfClienteId)) {
            _tmpClienteId = null
          } else {
            _tmpClienteId = _stmt.getText(_columnIndexOfClienteId)
          }
          val _tmpCarritoJson: String
          _tmpCarritoJson = _stmt.getText(_columnIndexOfCarritoJson)
          val _tmpEstado: String
          _tmpEstado = _stmt.getText(_columnIndexOfEstado)
          val _tmpIntentos: Int
          _tmpIntentos = _stmt.getLong(_columnIndexOfIntentos).toInt()
          val _tmpUltimoIntento: Long?
          if (_stmt.isNull(_columnIndexOfUltimoIntento)) {
            _tmpUltimoIntento = null
          } else {
            _tmpUltimoIntento = _stmt.getLong(_columnIndexOfUltimoIntento)
          }
          _item = VentaPendienteEntity(_tmpId,_tmpTenantId,_tmpTicket,_tmpCodigoTicket,_tmpTotal,_tmpDescuentoLealtad,_tmpDescuentoPromociones,_tmpDescuentoManual,_tmpPropina,_tmpNotaOrden,_tmpFecha,_tmpSucursal,_tmpAtendio,_tmpMetodoPago,_tmpEsConsumoEmpleado,_tmpClienteId,_tmpCarritoJson,_tmpEstado,_tmpIntentos,_tmpUltimoIntento)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun obtenerFallidas(): List<VentaPendienteEntity> {
    val _sql: String = "SELECT * FROM ventas_pendientes WHERE estado IN ('fallida', 'fallida_critica') ORDER BY fecha ASC"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfTenantId: Int = getColumnIndexOrThrow(_stmt, "tenantId")
        val _columnIndexOfTicket: Int = getColumnIndexOrThrow(_stmt, "ticket")
        val _columnIndexOfCodigoTicket: Int = getColumnIndexOrThrow(_stmt, "codigoTicket")
        val _columnIndexOfTotal: Int = getColumnIndexOrThrow(_stmt, "total")
        val _columnIndexOfDescuentoLealtad: Int = getColumnIndexOrThrow(_stmt, "descuentoLealtad")
        val _columnIndexOfDescuentoPromociones: Int = getColumnIndexOrThrow(_stmt, "descuentoPromociones")
        val _columnIndexOfDescuentoManual: Int = getColumnIndexOrThrow(_stmt, "descuentoManual")
        val _columnIndexOfPropina: Int = getColumnIndexOrThrow(_stmt, "propina")
        val _columnIndexOfNotaOrden: Int = getColumnIndexOrThrow(_stmt, "notaOrden")
        val _columnIndexOfFecha: Int = getColumnIndexOrThrow(_stmt, "fecha")
        val _columnIndexOfSucursal: Int = getColumnIndexOrThrow(_stmt, "sucursal")
        val _columnIndexOfAtendio: Int = getColumnIndexOrThrow(_stmt, "atendio")
        val _columnIndexOfMetodoPago: Int = getColumnIndexOrThrow(_stmt, "metodoPago")
        val _columnIndexOfEsConsumoEmpleado: Int = getColumnIndexOrThrow(_stmt, "esConsumoEmpleado")
        val _columnIndexOfClienteId: Int = getColumnIndexOrThrow(_stmt, "clienteId")
        val _columnIndexOfCarritoJson: Int = getColumnIndexOrThrow(_stmt, "carritoJson")
        val _columnIndexOfEstado: Int = getColumnIndexOrThrow(_stmt, "estado")
        val _columnIndexOfIntentos: Int = getColumnIndexOrThrow(_stmt, "intentos")
        val _columnIndexOfUltimoIntento: Int = getColumnIndexOrThrow(_stmt, "ultimoIntento")
        val _result: MutableList<VentaPendienteEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: VentaPendienteEntity
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpTenantId: String
          _tmpTenantId = _stmt.getText(_columnIndexOfTenantId)
          val _tmpTicket: Long
          _tmpTicket = _stmt.getLong(_columnIndexOfTicket)
          val _tmpCodigoTicket: String
          _tmpCodigoTicket = _stmt.getText(_columnIndexOfCodigoTicket)
          val _tmpTotal: Double
          _tmpTotal = _stmt.getDouble(_columnIndexOfTotal)
          val _tmpDescuentoLealtad: Double
          _tmpDescuentoLealtad = _stmt.getDouble(_columnIndexOfDescuentoLealtad)
          val _tmpDescuentoPromociones: Double
          _tmpDescuentoPromociones = _stmt.getDouble(_columnIndexOfDescuentoPromociones)
          val _tmpDescuentoManual: Double
          _tmpDescuentoManual = _stmt.getDouble(_columnIndexOfDescuentoManual)
          val _tmpPropina: Double
          _tmpPropina = _stmt.getDouble(_columnIndexOfPropina)
          val _tmpNotaOrden: String
          _tmpNotaOrden = _stmt.getText(_columnIndexOfNotaOrden)
          val _tmpFecha: Long
          _tmpFecha = _stmt.getLong(_columnIndexOfFecha)
          val _tmpSucursal: String
          _tmpSucursal = _stmt.getText(_columnIndexOfSucursal)
          val _tmpAtendio: String
          _tmpAtendio = _stmt.getText(_columnIndexOfAtendio)
          val _tmpMetodoPago: String
          _tmpMetodoPago = _stmt.getText(_columnIndexOfMetodoPago)
          val _tmpEsConsumoEmpleado: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfEsConsumoEmpleado).toInt()
          _tmpEsConsumoEmpleado = _tmp != 0
          val _tmpClienteId: String?
          if (_stmt.isNull(_columnIndexOfClienteId)) {
            _tmpClienteId = null
          } else {
            _tmpClienteId = _stmt.getText(_columnIndexOfClienteId)
          }
          val _tmpCarritoJson: String
          _tmpCarritoJson = _stmt.getText(_columnIndexOfCarritoJson)
          val _tmpEstado: String
          _tmpEstado = _stmt.getText(_columnIndexOfEstado)
          val _tmpIntentos: Int
          _tmpIntentos = _stmt.getLong(_columnIndexOfIntentos).toInt()
          val _tmpUltimoIntento: Long?
          if (_stmt.isNull(_columnIndexOfUltimoIntento)) {
            _tmpUltimoIntento = null
          } else {
            _tmpUltimoIntento = _stmt.getLong(_columnIndexOfUltimoIntento)
          }
          _item = VentaPendienteEntity(_tmpId,_tmpTenantId,_tmpTicket,_tmpCodigoTicket,_tmpTotal,_tmpDescuentoLealtad,_tmpDescuentoPromociones,_tmpDescuentoManual,_tmpPropina,_tmpNotaOrden,_tmpFecha,_tmpSucursal,_tmpAtendio,_tmpMetodoPago,_tmpEsConsumoEmpleado,_tmpClienteId,_tmpCarritoJson,_tmpEstado,_tmpIntentos,_tmpUltimoIntento)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun observarConteoFallidas(): Flow<Int> {
    val _sql: String = "SELECT COUNT(*) FROM ventas_pendientes WHERE estado IN ('fallida', 'fallida_critica')"
    return createFlow(__db, false, arrayOf("ventas_pendientes")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _result: Int
        if (_stmt.step()) {
          val _tmp: Int
          _tmp = _stmt.getLong(0).toInt()
          _result = _tmp
        } else {
          _result = 0
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun contarPendientes(): Int {
    val _sql: String = "SELECT COUNT(*) FROM ventas_pendientes WHERE estado = 'pendiente'"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _result: Int
        if (_stmt.step()) {
          val _tmp: Int
          _tmp = _stmt.getLong(0).toInt()
          _result = _tmp
        } else {
          _result = 0
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun obtenerDesde(sucursal: String, desde: Long): List<VentaPendienteEntity> {
    val _sql: String = "SELECT * FROM ventas_pendientes WHERE sucursal = ? AND fecha >= ? ORDER BY fecha ASC"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, sucursal)
        _argIndex = 2
        _stmt.bindLong(_argIndex, desde)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfTenantId: Int = getColumnIndexOrThrow(_stmt, "tenantId")
        val _columnIndexOfTicket: Int = getColumnIndexOrThrow(_stmt, "ticket")
        val _columnIndexOfCodigoTicket: Int = getColumnIndexOrThrow(_stmt, "codigoTicket")
        val _columnIndexOfTotal: Int = getColumnIndexOrThrow(_stmt, "total")
        val _columnIndexOfDescuentoLealtad: Int = getColumnIndexOrThrow(_stmt, "descuentoLealtad")
        val _columnIndexOfDescuentoPromociones: Int = getColumnIndexOrThrow(_stmt, "descuentoPromociones")
        val _columnIndexOfDescuentoManual: Int = getColumnIndexOrThrow(_stmt, "descuentoManual")
        val _columnIndexOfPropina: Int = getColumnIndexOrThrow(_stmt, "propina")
        val _columnIndexOfNotaOrden: Int = getColumnIndexOrThrow(_stmt, "notaOrden")
        val _columnIndexOfFecha: Int = getColumnIndexOrThrow(_stmt, "fecha")
        val _columnIndexOfSucursal: Int = getColumnIndexOrThrow(_stmt, "sucursal")
        val _columnIndexOfAtendio: Int = getColumnIndexOrThrow(_stmt, "atendio")
        val _columnIndexOfMetodoPago: Int = getColumnIndexOrThrow(_stmt, "metodoPago")
        val _columnIndexOfEsConsumoEmpleado: Int = getColumnIndexOrThrow(_stmt, "esConsumoEmpleado")
        val _columnIndexOfClienteId: Int = getColumnIndexOrThrow(_stmt, "clienteId")
        val _columnIndexOfCarritoJson: Int = getColumnIndexOrThrow(_stmt, "carritoJson")
        val _columnIndexOfEstado: Int = getColumnIndexOrThrow(_stmt, "estado")
        val _columnIndexOfIntentos: Int = getColumnIndexOrThrow(_stmt, "intentos")
        val _columnIndexOfUltimoIntento: Int = getColumnIndexOrThrow(_stmt, "ultimoIntento")
        val _result: MutableList<VentaPendienteEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: VentaPendienteEntity
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpTenantId: String
          _tmpTenantId = _stmt.getText(_columnIndexOfTenantId)
          val _tmpTicket: Long
          _tmpTicket = _stmt.getLong(_columnIndexOfTicket)
          val _tmpCodigoTicket: String
          _tmpCodigoTicket = _stmt.getText(_columnIndexOfCodigoTicket)
          val _tmpTotal: Double
          _tmpTotal = _stmt.getDouble(_columnIndexOfTotal)
          val _tmpDescuentoLealtad: Double
          _tmpDescuentoLealtad = _stmt.getDouble(_columnIndexOfDescuentoLealtad)
          val _tmpDescuentoPromociones: Double
          _tmpDescuentoPromociones = _stmt.getDouble(_columnIndexOfDescuentoPromociones)
          val _tmpDescuentoManual: Double
          _tmpDescuentoManual = _stmt.getDouble(_columnIndexOfDescuentoManual)
          val _tmpPropina: Double
          _tmpPropina = _stmt.getDouble(_columnIndexOfPropina)
          val _tmpNotaOrden: String
          _tmpNotaOrden = _stmt.getText(_columnIndexOfNotaOrden)
          val _tmpFecha: Long
          _tmpFecha = _stmt.getLong(_columnIndexOfFecha)
          val _tmpSucursal: String
          _tmpSucursal = _stmt.getText(_columnIndexOfSucursal)
          val _tmpAtendio: String
          _tmpAtendio = _stmt.getText(_columnIndexOfAtendio)
          val _tmpMetodoPago: String
          _tmpMetodoPago = _stmt.getText(_columnIndexOfMetodoPago)
          val _tmpEsConsumoEmpleado: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfEsConsumoEmpleado).toInt()
          _tmpEsConsumoEmpleado = _tmp != 0
          val _tmpClienteId: String?
          if (_stmt.isNull(_columnIndexOfClienteId)) {
            _tmpClienteId = null
          } else {
            _tmpClienteId = _stmt.getText(_columnIndexOfClienteId)
          }
          val _tmpCarritoJson: String
          _tmpCarritoJson = _stmt.getText(_columnIndexOfCarritoJson)
          val _tmpEstado: String
          _tmpEstado = _stmt.getText(_columnIndexOfEstado)
          val _tmpIntentos: Int
          _tmpIntentos = _stmt.getLong(_columnIndexOfIntentos).toInt()
          val _tmpUltimoIntento: Long?
          if (_stmt.isNull(_columnIndexOfUltimoIntento)) {
            _tmpUltimoIntento = null
          } else {
            _tmpUltimoIntento = _stmt.getLong(_columnIndexOfUltimoIntento)
          }
          _item = VentaPendienteEntity(_tmpId,_tmpTenantId,_tmpTicket,_tmpCodigoTicket,_tmpTotal,_tmpDescuentoLealtad,_tmpDescuentoPromociones,_tmpDescuentoManual,_tmpPropina,_tmpNotaOrden,_tmpFecha,_tmpSucursal,_tmpAtendio,_tmpMetodoPago,_tmpEsConsumoEmpleado,_tmpClienteId,_tmpCarritoJson,_tmpEstado,_tmpIntentos,_tmpUltimoIntento)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun marcarSincronizada(id: String, timestamp: Long) {
    val _sql: String = "UPDATE ventas_pendientes SET estado = 'sincronizada', intentos = intentos + 1, ultimoIntento = ? WHERE id = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, timestamp)
        _argIndex = 2
        _stmt.bindText(_argIndex, id)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun marcarFallida(id: String, timestamp: Long) {
    val _sql: String = "UPDATE ventas_pendientes SET estado = 'fallida', intentos = intentos + 1, ultimoIntento = ? WHERE id = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, timestamp)
        _argIndex = 2
        _stmt.bindText(_argIndex, id)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun marcarFallidaCritica(id: String, timestamp: Long) {
    val _sql: String = "UPDATE ventas_pendientes SET estado = 'fallida_critica', intentos = intentos + 1, ultimoIntento = ? WHERE id = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, timestamp)
        _argIndex = 2
        _stmt.bindText(_argIndex, id)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun registrarIntentoFallido(id: String, timestamp: Long) {
    val _sql: String = "UPDATE ventas_pendientes SET estado = 'pendiente', intentos = intentos + 1, ultimoIntento = ? WHERE id = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, timestamp)
        _argIndex = 2
        _stmt.bindText(_argIndex, id)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun reintentarVenta(id: String) {
    val _sql: String = "UPDATE ventas_pendientes SET estado = 'pendiente', intentos = 0 WHERE id = ?"
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
    val _sql: String = "DELETE FROM ventas_pendientes WHERE estado = 'sincronizada'"
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
