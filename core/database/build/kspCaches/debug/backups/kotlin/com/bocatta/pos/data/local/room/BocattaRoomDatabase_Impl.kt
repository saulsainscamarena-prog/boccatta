package com.bocatta.pos.`data`.local.room

import androidx.room.InvalidationTracker
import androidx.room.RoomOpenDelegate
import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.room.util.TableInfo
import androidx.room.util.TableInfo.Companion.read
import androidx.room.util.dropFtsSyncTriggers
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import com.bocatta.pos.`data`.local.room.dao.FolioDao
import com.bocatta.pos.`data`.local.room.dao.FolioDao_Impl
import com.bocatta.pos.`data`.local.room.dao.OperacionPendienteDao
import com.bocatta.pos.`data`.local.room.dao.OperacionPendienteDao_Impl
import com.bocatta.pos.`data`.local.room.dao.VentaPendienteDao
import com.bocatta.pos.`data`.local.room.dao.VentaPendienteDao_Impl
import javax.`annotation`.processing.Generated
import kotlin.Lazy
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.Map
import kotlin.collections.MutableList
import kotlin.collections.MutableMap
import kotlin.collections.MutableSet
import kotlin.collections.Set
import kotlin.collections.mutableListOf
import kotlin.collections.mutableMapOf
import kotlin.collections.mutableSetOf
import kotlin.reflect.KClass

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class BocattaRoomDatabase_Impl : BocattaRoomDatabase() {
  private val _ventaPendienteDao: Lazy<VentaPendienteDao> = lazy {
    VentaPendienteDao_Impl(this)
  }

  private val _operacionPendienteDao: Lazy<OperacionPendienteDao> = lazy {
    OperacionPendienteDao_Impl(this)
  }

  private val _folioDao: Lazy<FolioDao> = lazy {
    FolioDao_Impl(this)
  }

  protected override fun createOpenDelegate(): RoomOpenDelegate {
    val _openDelegate: RoomOpenDelegate = object : RoomOpenDelegate(2, "f83a7d058f72533905b7c255851fd8f2", "6387a25f51f8a4c354a120f32eef7775") {
      public override fun createAllTables(connection: SQLiteConnection) {
        connection.execSQL("CREATE TABLE IF NOT EXISTS `ventas_pendientes` (`id` TEXT NOT NULL, `tenantId` TEXT NOT NULL, `ticket` INTEGER NOT NULL, `codigoTicket` TEXT NOT NULL, `total` REAL NOT NULL, `descuentoLealtad` REAL NOT NULL, `descuentoPromociones` REAL NOT NULL DEFAULT 0.0, `descuentoManual` REAL NOT NULL DEFAULT 0.0, `propina` REAL NOT NULL DEFAULT 0.0, `notaOrden` TEXT NOT NULL DEFAULT '', `fecha` INTEGER NOT NULL, `sucursal` TEXT NOT NULL, `atendio` TEXT NOT NULL, `metodoPago` TEXT NOT NULL, `esConsumoEmpleado` INTEGER NOT NULL DEFAULT 0, `clienteId` TEXT, `carritoJson` TEXT NOT NULL, `estado` TEXT NOT NULL DEFAULT 'pendiente', `intentos` INTEGER NOT NULL DEFAULT 0, `ultimoIntento` INTEGER, PRIMARY KEY(`id`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `operaciones_pendientes` (`id` TEXT NOT NULL, `tenantId` TEXT NOT NULL, `tipo` TEXT NOT NULL, `ventaId` TEXT, `motivo` TEXT NOT NULL, `usuarioId` TEXT NOT NULL, `sucursal` TEXT NOT NULL, `fecha` INTEGER NOT NULL, `requiereAprobacion` INTEGER NOT NULL DEFAULT 1, `dataJson` TEXT NOT NULL, `estado` TEXT NOT NULL DEFAULT 'pendiente', `intentos` INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(`id`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `folios_offline` (`tenantId` TEXT NOT NULL, `sucursal` TEXT NOT NULL, `ultimoTicket` INTEGER NOT NULL DEFAULT 0, `updatedAt` INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(`tenantId`, `sucursal`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
        connection.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'f83a7d058f72533905b7c255851fd8f2')")
      }

      public override fun dropAllTables(connection: SQLiteConnection) {
        connection.execSQL("DROP TABLE IF EXISTS `ventas_pendientes`")
        connection.execSQL("DROP TABLE IF EXISTS `operaciones_pendientes`")
        connection.execSQL("DROP TABLE IF EXISTS `folios_offline`")
      }

      public override fun onCreate(connection: SQLiteConnection) {
      }

      public override fun onOpen(connection: SQLiteConnection) {
        internalInitInvalidationTracker(connection)
      }

      public override fun onPreMigrate(connection: SQLiteConnection) {
        dropFtsSyncTriggers(connection)
      }

      public override fun onPostMigrate(connection: SQLiteConnection) {
      }

      public override fun onValidateSchema(connection: SQLiteConnection): RoomOpenDelegate.ValidationResult {
        val _columnsVentasPendientes: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsVentasPendientes.put("id", TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsVentasPendientes.put("tenantId", TableInfo.Column("tenantId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsVentasPendientes.put("ticket", TableInfo.Column("ticket", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsVentasPendientes.put("codigoTicket", TableInfo.Column("codigoTicket", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsVentasPendientes.put("total", TableInfo.Column("total", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsVentasPendientes.put("descuentoLealtad", TableInfo.Column("descuentoLealtad", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsVentasPendientes.put("descuentoPromociones", TableInfo.Column("descuentoPromociones", "REAL", true, 0, "0.0", TableInfo.CREATED_FROM_ENTITY))
        _columnsVentasPendientes.put("descuentoManual", TableInfo.Column("descuentoManual", "REAL", true, 0, "0.0", TableInfo.CREATED_FROM_ENTITY))
        _columnsVentasPendientes.put("propina", TableInfo.Column("propina", "REAL", true, 0, "0.0", TableInfo.CREATED_FROM_ENTITY))
        _columnsVentasPendientes.put("notaOrden", TableInfo.Column("notaOrden", "TEXT", true, 0, "''", TableInfo.CREATED_FROM_ENTITY))
        _columnsVentasPendientes.put("fecha", TableInfo.Column("fecha", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsVentasPendientes.put("sucursal", TableInfo.Column("sucursal", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsVentasPendientes.put("atendio", TableInfo.Column("atendio", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsVentasPendientes.put("metodoPago", TableInfo.Column("metodoPago", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsVentasPendientes.put("esConsumoEmpleado", TableInfo.Column("esConsumoEmpleado", "INTEGER", true, 0, "0", TableInfo.CREATED_FROM_ENTITY))
        _columnsVentasPendientes.put("clienteId", TableInfo.Column("clienteId", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsVentasPendientes.put("carritoJson", TableInfo.Column("carritoJson", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsVentasPendientes.put("estado", TableInfo.Column("estado", "TEXT", true, 0, "'pendiente'", TableInfo.CREATED_FROM_ENTITY))
        _columnsVentasPendientes.put("intentos", TableInfo.Column("intentos", "INTEGER", true, 0, "0", TableInfo.CREATED_FROM_ENTITY))
        _columnsVentasPendientes.put("ultimoIntento", TableInfo.Column("ultimoIntento", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysVentasPendientes: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesVentasPendientes: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoVentasPendientes: TableInfo = TableInfo("ventas_pendientes", _columnsVentasPendientes, _foreignKeysVentasPendientes, _indicesVentasPendientes)
        val _existingVentasPendientes: TableInfo = read(connection, "ventas_pendientes")
        if (!_infoVentasPendientes.equals(_existingVentasPendientes)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |ventas_pendientes(com.bocatta.pos.data.local.room.entity.VentaPendienteEntity).
              | Expected:
              |""".trimMargin() + _infoVentasPendientes + """
              |
              | Found:
              |""".trimMargin() + _existingVentasPendientes)
        }
        val _columnsOperacionesPendientes: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsOperacionesPendientes.put("id", TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsOperacionesPendientes.put("tenantId", TableInfo.Column("tenantId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsOperacionesPendientes.put("tipo", TableInfo.Column("tipo", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsOperacionesPendientes.put("ventaId", TableInfo.Column("ventaId", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsOperacionesPendientes.put("motivo", TableInfo.Column("motivo", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsOperacionesPendientes.put("usuarioId", TableInfo.Column("usuarioId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsOperacionesPendientes.put("sucursal", TableInfo.Column("sucursal", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsOperacionesPendientes.put("fecha", TableInfo.Column("fecha", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsOperacionesPendientes.put("requiereAprobacion", TableInfo.Column("requiereAprobacion", "INTEGER", true, 0, "1", TableInfo.CREATED_FROM_ENTITY))
        _columnsOperacionesPendientes.put("dataJson", TableInfo.Column("dataJson", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsOperacionesPendientes.put("estado", TableInfo.Column("estado", "TEXT", true, 0, "'pendiente'", TableInfo.CREATED_FROM_ENTITY))
        _columnsOperacionesPendientes.put("intentos", TableInfo.Column("intentos", "INTEGER", true, 0, "0", TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysOperacionesPendientes: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesOperacionesPendientes: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoOperacionesPendientes: TableInfo = TableInfo("operaciones_pendientes", _columnsOperacionesPendientes, _foreignKeysOperacionesPendientes, _indicesOperacionesPendientes)
        val _existingOperacionesPendientes: TableInfo = read(connection, "operaciones_pendientes")
        if (!_infoOperacionesPendientes.equals(_existingOperacionesPendientes)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |operaciones_pendientes(com.bocatta.pos.data.local.room.entity.OperacionPendienteEntity).
              | Expected:
              |""".trimMargin() + _infoOperacionesPendientes + """
              |
              | Found:
              |""".trimMargin() + _existingOperacionesPendientes)
        }
        val _columnsFoliosOffline: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsFoliosOffline.put("tenantId", TableInfo.Column("tenantId", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsFoliosOffline.put("sucursal", TableInfo.Column("sucursal", "TEXT", true, 2, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsFoliosOffline.put("ultimoTicket", TableInfo.Column("ultimoTicket", "INTEGER", true, 0, "0", TableInfo.CREATED_FROM_ENTITY))
        _columnsFoliosOffline.put("updatedAt", TableInfo.Column("updatedAt", "INTEGER", true, 0, "0", TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysFoliosOffline: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesFoliosOffline: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoFoliosOffline: TableInfo = TableInfo("folios_offline", _columnsFoliosOffline, _foreignKeysFoliosOffline, _indicesFoliosOffline)
        val _existingFoliosOffline: TableInfo = read(connection, "folios_offline")
        if (!_infoFoliosOffline.equals(_existingFoliosOffline)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |folios_offline(com.bocatta.pos.data.local.room.entity.FolioEntity).
              | Expected:
              |""".trimMargin() + _infoFoliosOffline + """
              |
              | Found:
              |""".trimMargin() + _existingFoliosOffline)
        }
        return RoomOpenDelegate.ValidationResult(true, null)
      }
    }
    return _openDelegate
  }

  protected override fun createInvalidationTracker(): InvalidationTracker {
    val _shadowTablesMap: MutableMap<String, String> = mutableMapOf()
    val _viewTables: MutableMap<String, Set<String>> = mutableMapOf()
    return InvalidationTracker(this, _shadowTablesMap, _viewTables, "ventas_pendientes", "operaciones_pendientes", "folios_offline")
  }

  public override fun clearAllTables() {
    super.performClear(false, "ventas_pendientes", "operaciones_pendientes", "folios_offline")
  }

  protected override fun getRequiredTypeConverterClasses(): Map<KClass<*>, List<KClass<*>>> {
    val _typeConvertersMap: MutableMap<KClass<*>, List<KClass<*>>> = mutableMapOf()
    _typeConvertersMap.put(VentaPendienteDao::class, VentaPendienteDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(OperacionPendienteDao::class, OperacionPendienteDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(FolioDao::class, FolioDao_Impl.getRequiredConverters())
    return _typeConvertersMap
  }

  public override fun getRequiredAutoMigrationSpecClasses(): Set<KClass<out AutoMigrationSpec>> {
    val _autoMigrationSpecsSet: MutableSet<KClass<out AutoMigrationSpec>> = mutableSetOf()
    return _autoMigrationSpecsSet
  }

  public override fun createAutoMigrations(autoMigrationSpecs: Map<KClass<out AutoMigrationSpec>, AutoMigrationSpec>): List<Migration> {
    val _autoMigrations: MutableList<Migration> = mutableListOf()
    return _autoMigrations
  }

  public override fun ventaPendienteDao(): VentaPendienteDao = _ventaPendienteDao.value

  public override fun operacionPendienteDao(): OperacionPendienteDao = _operacionPendienteDao.value

  public override fun folioDao(): FolioDao = _folioDao.value
}
