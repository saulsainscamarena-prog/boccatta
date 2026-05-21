package com.bocatta.pos.data.local

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.bocatta.pos.core.TicketUtils
import com.bocatta.pos.domain.model.*
import timber.log.Timber

data class VentaOffline(
    val id: String,
    val ticket: Long,
    val codigoTicket: String,
    val total: Double,
    val descuentoLealtad: Double,
    val fecha: Long,
    val sucursal: String,
    val atendio: String,
    val metodoPago: String,
    val esConsumoEmpleado: Boolean,
    val clienteId: String?,
    val carritoJson: String,
    val estado: String = VentaOffline.ESTADO_PENDIENTE,
    val intentos: Int = 0,
    val ultimoIntento: Long? = null
) {
    companion object {
        const val ESTADO_PENDIENTE = "pendiente"
        const val ESTADO_SINCRONIZADA = "sincronizada"
        const val ESTADO_FALLIDA = "fallida"
        const val ESTADO_FALLIDA_CRITICA = "fallida_critica"
        const val MAX_INTENTOS = 3
    }
}

data class OperacionOffline(
    val id: String,
    val tipo: String,
    val ventaId: String?,
    val motivo: String,
    val usuarioId: String,
    val sucursal: String,
    val fecha: Long,
    val requiereAprobacion: Boolean,
    val dataJson: String,
    val estado: String = OperacionOffline.ESTADO_PENDIENTE,
    val intentos: Int = 0
) {
    companion object {
        const val TIPO_DEVOLUCION = "devolucion"
        const val TIPO_CANCELACION = "cancelacion"
        const val TIPO_MERMA = "merma"
        const val ESTADO_PENDIENTE = "pendiente"
        const val ESTADO_SINCRONIZADA = "sincronizada"
        const val ESTADO_FALLIDA = "fallida"
        const val MAX_INTENTOS = 3
    }
}

class OfflineDatabase(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "bocatta_offline.db"
        private const val DATABASE_VERSION = 5

        const val TABLE_VENTAS = "ventas_pendientes"
        const val TABLE_FOLIOS = "folios_offline"
        const val TABLE_OPS = "operaciones_pendientes"
        const val TABLE_HELD_ORDERS = "held_orders"
        const val TABLE_JORNADAS = "registro_jornadas"

        const val TABLE_INSUMOS = "insumos_v2"
        const val TABLE_CONSUMIBLES = "consumibles_v2"
        const val TABLE_PRODUCTOS = "productos_v2"
        const val TABLE_RECETAS = "recetas_v2"
        const val TABLE_INGREDIENTES_RECETA = "ingredientes_receta"
        const val TABLE_PRESENTACIONES = "presentaciones_insumo"

        @Volatile
        private var INSTANCE: OfflineDatabase? = null

        fun getInstance(context: Context): OfflineDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = OfflineDatabase(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE $TABLE_VENTAS (
                id TEXT PRIMARY KEY,
                ticket INTEGER NOT NULL,
                codigoTicket TEXT NOT NULL,
                total REAL NOT NULL,
                descuentoLealtad REAL NOT NULL,
                fecha INTEGER NOT NULL,
                sucursal TEXT NOT NULL,
                atendio TEXT NOT NULL,
                metodoPago TEXT NOT NULL,
                esConsumoEmpleado INTEGER NOT NULL DEFAULT 0,
                clienteId TEXT,
                carritoJson TEXT NOT NULL,
                estado TEXT NOT NULL DEFAULT '${VentaOffline.ESTADO_PENDIENTE}',
                intentos INTEGER NOT NULL DEFAULT 0,
                ultimoIntento INTEGER
            )
        """)

        createFolioTable(db)
        db.execSQL("""
            CREATE TABLE $TABLE_OPS (
                id TEXT PRIMARY KEY,
                tipo TEXT NOT NULL,
                ventaId TEXT,
                motivo TEXT NOT NULL,
                usuarioId TEXT NOT NULL,
                sucursal TEXT NOT NULL,
                fecha INTEGER NOT NULL,
                requiereAprobacion INTEGER NOT NULL DEFAULT 1,
                dataJson TEXT NOT NULL,
                estado TEXT NOT NULL DEFAULT '${OperacionOffline.ESTADO_PENDIENTE}',
                intentos INTEGER NOT NULL DEFAULT 0
            )
        """)

        createNewTables(db)
    }

    private fun createNewTables(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS $TABLE_JORNADAS (
                id TEXT PRIMARY KEY,
                usuario TEXT NOT NULL,
                sucursal TEXT NOT NULL,
                accion TEXT NOT NULL,
                rol TEXT NOT NULL DEFAULT '',
                sesionId TEXT NOT NULL DEFAULT '',
                timestamp INTEGER NOT NULL
            )
        """)

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS $TABLE_HELD_ORDERS (
                id TEXT PRIMARY KEY,
                carritoJson TEXT NOT NULL,
                clienteJson TEXT,
                nota TEXT NOT NULL DEFAULT '',
                fecha INTEGER NOT NULL,
                sucursal TEXT NOT NULL,
                total REAL NOT NULL DEFAULT 0.0
            )
        """)

        db.execSQL("""
            CREATE TABLE $TABLE_INSUMOS (
                id TEXT PRIMARY KEY,
                nombre TEXT NOT NULL,
                categoria TEXT,
                unidadBase TEXT NOT NULL DEFAULT 'g',
                costoUnitarioBase REAL NOT NULL DEFAULT 0.0,
                cantidadEnBase REAL NOT NULL DEFAULT 0.0,
                stockMinimo REAL NOT NULL DEFAULT 10.0
            )
        """)

        db.execSQL("""
            CREATE TABLE $TABLE_CONSUMIBLES (
                id TEXT PRIMARY KEY,
                nombre TEXT NOT NULL,
                unidadBase TEXT NOT NULL DEFAULT 'pz',
                stockActual REAL NOT NULL DEFAULT 0.0,
                stockMinimo REAL NOT NULL DEFAULT 50.0
            )
        """)

        db.execSQL("""
            CREATE TABLE $TABLE_PRODUCTOS (
                id TEXT PRIMARY KEY,
                nombre TEXT NOT NULL,
                emoji TEXT DEFAULT '??',
                categoria TEXT,
                precioVenta TEXT,
                esCombo INTEGER NOT NULL DEFAULT 0,
                recetaId TEXT,
                toppingsIncluidos INTEGER NOT NULL DEFAULT 2,
                costoToppingExtra REAL NOT NULL DEFAULT 10.0,
                esProductoTopping INTEGER NOT NULL DEFAULT 0,
                consumiblesJson TEXT
            )
        """)

        db.execSQL("""
            CREATE TABLE $TABLE_RECETAS (
                id TEXT PRIMARY KEY,
                nombre TEXT NOT NULL,
                productoId TEXT,
                rendimientoPorcion REAL NOT NULL DEFAULT 1.0
            )
        """)

        db.execSQL("""
            CREATE TABLE $TABLE_INGREDIENTES_RECETA (
                id TEXT PRIMARY KEY,
                recetaId TEXT NOT NULL,
                insumoId TEXT NOT NULL,
                nombreInsumo TEXT,
                cantidad REAL NOT NULL,
                unidad TEXT NOT NULL DEFAULT 'g',
                FOREIGN KEY (recetaId) REFERENCES $TABLE_RECETAS(id) ON DELETE CASCADE
            )
        """)

        db.execSQL("""
            CREATE TABLE $TABLE_PRESENTACIONES (
                id TEXT PRIMARY KEY,
                insumoId TEXT NOT NULL,
                nombre TEXT NOT NULL,
                unidadEquivalente TEXT NOT NULL DEFAULT 'pz',
                factorConversionABase REAL NOT NULL DEFAULT 1.0,
                cantidadDisponible REAL NOT NULL DEFAULT 0.0,
                ultimoPrecioPagado REAL NOT NULL DEFAULT 0.0,
                FOREIGN KEY (insumoId) REFERENCES $TABLE_INSUMOS(id) ON DELETE CASCADE
            )
        """)
    }

    private fun createFolioTable(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS $TABLE_FOLIOS (
                sucursal TEXT PRIMARY KEY,
                ultimoTicket INTEGER NOT NULL DEFAULT 0,
                updatedAt INTEGER NOT NULL DEFAULT 0
            )
        """)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            createNewTables(db)
        }
        if (oldVersion < 3) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS $TABLE_HELD_ORDERS (
                    id TEXT PRIMARY KEY,
                    carritoJson TEXT NOT NULL,
                    clienteJson TEXT,
                    nota TEXT NOT NULL DEFAULT '',
                    fecha INTEGER NOT NULL,
                    sucursal TEXT NOT NULL,
                    total REAL NOT NULL DEFAULT 0.0
                )
            """)
        }
        if (oldVersion < 4) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS $TABLE_JORNADAS (
                    id TEXT PRIMARY KEY,
                    usuario TEXT NOT NULL,
                    sucursal TEXT NOT NULL,
                    accion TEXT NOT NULL,
                    rol TEXT NOT NULL DEFAULT '',
                    sesionId TEXT NOT NULL DEFAULT '',
                    timestamp INTEGER NOT NULL
                )
            """)
        }
        if (oldVersion < 5) {
            createFolioTable(db)
        }
    }

    fun guardarVenta(venta: VentaOffline) {
        val db = writableDatabase
        db.insertWithOnConflict(TABLE_VENTAS, null, venta.toContentValues(), SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun guardarVentaYDescontarStock(venta: VentaOffline, deducciones: Map<String, Double>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            deducciones.forEach { (insumoId, requerido) ->
                val actual = obtenerStockInsumo(db, insumoId)
                if (actual < requerido) {
                    throw IllegalStateException("Stock local insuficiente para $insumoId. Disponible: $actual, requerido: $requerido")
                }
            }

            val insertResult = db.insertWithOnConflict(
                TABLE_VENTAS,
                null,
                venta.toContentValues(),
                SQLiteDatabase.CONFLICT_REPLACE
            )
            if (insertResult == -1L) {
                throw IllegalStateException("No se pudo guardar la venta offline")
            }

            deducciones.forEach { (insumoId, cantidad) ->
                db.execSQL(
                    "UPDATE $TABLE_INSUMOS SET cantidadEnBase = cantidadEnBase - ? WHERE id = ?",
                    arrayOf<Any>(cantidad, insumoId)
                )
            }

            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun guardarVentaYDescontarStockReservandoFolio(
        ventaBase: VentaOffline,
        deducciones: Map<String, Double>,
        legacyUltimoTicket: Long = 0L
    ): VentaOffline {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val sucursalId = ventaBase.sucursal.lowercase()
            val ultimoPersistido = obtenerUltimoTicket(db, sucursalId)
            val ticket = maxOf(ultimoPersistido, legacyUltimoTicket) + 1L
            val ventaConfirmada = ventaBase.copy(
                id = if (ventaBase.id.isBlank()) {
                    "offline_${System.currentTimeMillis()}_$ticket"
                } else {
                    "${ventaBase.id}_$ticket"
                },
                ticket = ticket,
                codigoTicket = TicketUtils.generarCodigoTicket(sucursalId, ticket)
            )

            deducciones.forEach { (insumoId, requerido) ->
                val actual = obtenerStockInsumo(db, insumoId)
                if (actual < requerido) {
                    throw IllegalStateException("Stock local insuficiente para $insumoId. Disponible: $actual, requerido: $requerido")
                }
            }

            val insertResult = db.insertWithOnConflict(
                TABLE_VENTAS,
                null,
                ventaConfirmada.toContentValues(),
                SQLiteDatabase.CONFLICT_REPLACE
            )
            if (insertResult == -1L) {
                throw IllegalStateException("No se pudo guardar la venta offline")
            }

            deducciones.forEach { (insumoId, cantidad) ->
                db.execSQL(
                    "UPDATE $TABLE_INSUMOS SET cantidadEnBase = cantidadEnBase - ? WHERE id = ?",
                    arrayOf<Any>(cantidad, insumoId)
                )
            }

            guardarUltimoTicket(db, sucursalId, ticket)
            db.setTransactionSuccessful()
            return ventaConfirmada
        } finally {
            db.endTransaction()
        }
    }

    private fun obtenerUltimoTicket(db: SQLiteDatabase, sucursalId: String): Long {
        return db.query(
            TABLE_FOLIOS,
            arrayOf("ultimoTicket"),
            "sucursal = ?",
            arrayOf(sucursalId),
            null,
            null,
            null
        ).use { cursor ->
            if (cursor.moveToFirst()) cursor.getLong(0) else 0L
        }
    }

    private fun guardarUltimoTicket(db: SQLiteDatabase, sucursalId: String, ticket: Long) {
        val values = ContentValues().apply {
            put("sucursal", sucursalId)
            put("ultimoTicket", ticket)
            put("updatedAt", System.currentTimeMillis())
        }
        db.insertWithOnConflict(TABLE_FOLIOS, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    private fun VentaOffline.toContentValues(): ContentValues {
        return ContentValues().apply {
            put("id", id)
            put("ticket", ticket)
            put("codigoTicket", codigoTicket)
            put("total", total)
            put("descuentoLealtad", descuentoLealtad)
            put("fecha", fecha)
            put("sucursal", sucursal)
            put("atendio", atendio)
            put("metodoPago", metodoPago)
            put("esConsumoEmpleado", if (esConsumoEmpleado) 1 else 0)
            put("clienteId", clienteId)
            put("carritoJson", carritoJson)
            put("estado", estado)
            put("intentos", intentos)
            ultimoIntento?.let { put("ultimoIntento", it) }
        }
    }

    fun obtenerVentasPendientes(): List<VentaOffline> {
        val list = mutableListOf<VentaOffline>()
        val db = readableDatabase
        db.query(TABLE_VENTAS, null, "estado = ?", arrayOf(VentaOffline.ESTADO_PENDIENTE), null, null, "fecha ASC")
            .use { cursor -> list.addAll(cursor.toVentasList()) }
        return list
    }

    private fun Cursor.toVentasList(): List<VentaOffline> {
        val list = mutableListOf<VentaOffline>()
        while (moveToNext()) {
            list.add(
                VentaOffline(
                    id = getString(getColumnIndexOrThrow("id")),
                    ticket = getLong(getColumnIndexOrThrow("ticket")),
                    codigoTicket = getString(getColumnIndexOrThrow("codigoTicket")),
                    total = getDouble(getColumnIndexOrThrow("total")),
                    descuentoLealtad = getDouble(getColumnIndexOrThrow("descuentoLealtad")),
                    fecha = getLong(getColumnIndexOrThrow("fecha")),
                    sucursal = getString(getColumnIndexOrThrow("sucursal")),
                    atendio = getString(getColumnIndexOrThrow("atendio")),
                    metodoPago = getString(getColumnIndexOrThrow("metodoPago")),
                    esConsumoEmpleado = getInt(getColumnIndexOrThrow("esConsumoEmpleado")) == 1,
                    clienteId = getString(getColumnIndexOrThrow("clienteId")),
                    carritoJson = getString(getColumnIndexOrThrow("carritoJson")),
                    estado = getString(getColumnIndexOrThrow("estado")),
                    intentos = getInt(getColumnIndexOrThrow("intentos")),
                    ultimoIntento = getColumnIndex("ultimoIntento").takeIf { it >= 0 }?.let { getLong(it) }
                )
            )
        }
        return list
    }

    fun marcarVentaSincronizada(id: String, timestamp: Long) {
        val db = writableDatabase
        db.execSQL(
            "UPDATE $TABLE_VENTAS SET estado = '${VentaOffline.ESTADO_SINCRONIZADA}', intentos = intentos + 1, ultimoIntento = ? WHERE id = ?",
            arrayOf<Any>(timestamp, id)
        )
    }

    fun marcarVentaFallida(id: String, timestamp: Long) {
        val db = writableDatabase
        db.execSQL(
            "UPDATE $TABLE_VENTAS SET estado = '${VentaOffline.ESTADO_FALLIDA}', intentos = intentos + 1, ultimoIntento = ? WHERE id = ?",
            arrayOf<Any>(timestamp, id)
        )
    }

    fun registrarIntentoVentaFallido(id: String, timestamp: Long) {
        val db = writableDatabase
        db.execSQL(
            "UPDATE $TABLE_VENTAS SET estado = '${VentaOffline.ESTADO_PENDIENTE}', intentos = intentos + 1, ultimoIntento = ? WHERE id = ?",
            arrayOf<Any>(timestamp, id)
        )
    }

    fun marcarVentaFallidaCritica(id: String, timestamp: Long) {
        val db = writableDatabase
        db.execSQL(
            "UPDATE $TABLE_VENTAS SET estado = '${VentaOffline.ESTADO_FALLIDA_CRITICA}', intentos = intentos + 1, ultimoIntento = ? WHERE id = ?",
            arrayOf<Any>(timestamp, id)
        )
    }

    fun limpiarSincronizadas() {
        writableDatabase.use { db ->
            db.delete(TABLE_VENTAS, "estado = ?", arrayOf(VentaOffline.ESTADO_SINCRONIZADA))
        }
    }

    fun contarPendientes(): Int {
        val db = readableDatabase
        return db.rawQuery("SELECT COUNT(*) FROM $TABLE_VENTAS WHERE estado = ?", arrayOf(VentaOffline.ESTADO_PENDIENTE)).use { cursor ->
            if (cursor.moveToFirst()) cursor.getInt(0) else 0
        }
    }

    fun guardarOperacion(op: OperacionOffline) {
        writableDatabase.use { db ->
            val values = ContentValues().apply {
                put("id", op.id)
                put("tipo", op.tipo)
                put("ventaId", op.ventaId)
                put("motivo", op.motivo)
                put("usuarioId", op.usuarioId)
                put("sucursal", op.sucursal)
                put("fecha", op.fecha)
                put("requiereAprobacion", if (op.requiereAprobacion) 1 else 0)
                put("dataJson", op.dataJson)
                put("estado", op.estado)
                put("intentos", op.intentos)
            }
            db.insertWithOnConflict(TABLE_OPS, null, values, SQLiteDatabase.CONFLICT_REPLACE)
        }
    }

    fun obtenerOperacionesPendientes(): List<OperacionOffline> {
        val list = mutableListOf<OperacionOffline>()
        readableDatabase.use { db ->
            db.query(TABLE_OPS, null, "estado = ?", arrayOf(OperacionOffline.ESTADO_PENDIENTE), null, null, "fecha ASC")
                .use { cursor -> list.addAll(cursor.toOpsList()) }
        }
        return list
    }

    private fun Cursor.toOpsList(): List<OperacionOffline> {
        val list = mutableListOf<OperacionOffline>()
        while (moveToNext()) {
            list.add(
                OperacionOffline(
                    id = getString(getColumnIndexOrThrow("id")),
                    tipo = getString(getColumnIndexOrThrow("tipo")),
                    ventaId = getString(getColumnIndexOrThrow("ventaId")),
                    motivo = getString(getColumnIndexOrThrow("motivo")),
                    usuarioId = getString(getColumnIndexOrThrow("usuarioId")),
                    sucursal = getString(getColumnIndexOrThrow("sucursal")),
                    fecha = getLong(getColumnIndexOrThrow("fecha")),
                    requiereAprobacion = getInt(getColumnIndexOrThrow("requiereAprobacion")) == 1,
                    dataJson = getString(getColumnIndexOrThrow("dataJson")),
                    estado = getString(getColumnIndexOrThrow("estado")),
                    intentos = getInt(getColumnIndexOrThrow("intentos"))
                )
            )
        }
        return list
    }

    fun marcarOperacionSincronizada(id: String) {
        writableDatabase.use { db ->
            db.execSQL(
                "UPDATE $TABLE_OPS SET estado = '${OperacionOffline.ESTADO_SINCRONIZADA}', intentos = intentos + 1 WHERE id = ?",
                arrayOf(id)
            )
        }
    }

    fun marcarOperacionFallida(id: String) {
        writableDatabase.use { db ->
            db.execSQL(
                "UPDATE $TABLE_OPS SET estado = '${OperacionOffline.ESTADO_FALLIDA}', intentos = intentos + 1 WHERE id = ?",
                arrayOf(id)
            )
        }
    }

    fun limpiarOperacionesSincronizadas() {
        writableDatabase.use { db ->
            db.delete(TABLE_OPS, "estado = ?", arrayOf(OperacionOffline.ESTADO_SINCRONIZADA))
        }
    }

    fun guardarInsumo(insumo: InsumoV2) {
        writableDatabase.use { db ->
            val values = ContentValues().apply {
                put("id", insumo.id)
                put("nombre", insumo.nombre)
                put("categoria", insumo.categoria)
                put("unidadBase", insumo.unidadBase)
                put("costoUnitarioBase", insumo.costoUnitarioBase)
                put("cantidadEnBase", insumo.cantidadEnBase)
                put("stockMinimo", insumo.stockMinimo)
            }
            db.insertWithOnConflict(TABLE_INSUMOS, null, values, SQLiteDatabase.CONFLICT_REPLACE)
        }
    }

    fun obtenerInsumos(): List<InsumoV2> {
        val list = mutableListOf<InsumoV2>()
        readableDatabase.use { db ->
            db.query(TABLE_INSUMOS, null, null, null, null, null, "nombre ASC").use { cursor ->
                while (cursor.moveToNext()) {
                    list.add(
                        InsumoV2(
                            id = cursor.getString(cursor.getColumnIndexOrThrow("id")),
                            nombre = cursor.getString(cursor.getColumnIndexOrThrow("nombre")),
                            categoria = cursor.getString(cursor.getColumnIndexOrThrow("categoria")) ?: "",
                            unidadBase = cursor.getString(cursor.getColumnIndexOrThrow("unidadBase")),
                            costoUnitarioBase = cursor.getDouble(cursor.getColumnIndexOrThrow("costoUnitarioBase")),
                            cantidadEnBase = cursor.getDouble(cursor.getColumnIndexOrThrow("cantidadEnBase")),
                            stockMinimo = cursor.getDouble(cursor.getColumnIndexOrThrow("stockMinimo"))
                        )
                    )
                }
            }
        }
        return list
    }

    fun actualizarStockInsumo(insumoId: String, nuevaCantidadBase: Double) {
        val db = writableDatabase
        db.execSQL("UPDATE $TABLE_INSUMOS SET cantidadEnBase = ? WHERE id = ?", arrayOf<Any>(nuevaCantidadBase, insumoId))
    }

    fun obtenerStockInsumo(insumoId: String): Double {
        return obtenerStockInsumo(readableDatabase, insumoId)
    }

    private fun obtenerStockInsumo(db: SQLiteDatabase, insumoId: String): Double {
        db.query(TABLE_INSUMOS, arrayOf("cantidadEnBase"), "id = ?", arrayOf(insumoId), null, null, null).use { cursor ->
            if (cursor.moveToFirst()) {
                return cursor.getDouble(0)
            }
        }
        return 0.0
    }

    fun guardarReceta(receta: RecetaV2) {
        writableDatabase.use { db ->
            val values = ContentValues().apply {
                put("id", receta.id)
                put("nombre", receta.nombre)
                put("productoId", receta.productoId)
                put("rendimientoPorcion", receta.rendimientoPorcion)
            }
            db.insertWithOnConflict(TABLE_RECETAS, null, values, SQLiteDatabase.CONFLICT_REPLACE)
        }
    }

    fun guardarIngredienteReceta(ingrediente: IngredienteReceta, recetaId: String) {
        writableDatabase.use { db ->
            val values = ContentValues().apply {
                put("id", java.util.UUID.randomUUID().toString())
                put("recetaId", recetaId)
                put("insumoId", ingrediente.insumoId)
                put("nombreInsumo", ingrediente.nombreInsumo)
                put("cantidad", ingrediente.cantidad)
                put("unidad", ingrediente.unidad)
            }
            db.insertWithOnConflict(TABLE_INGREDIENTES_RECETA, null, values, SQLiteDatabase.CONFLICT_REPLACE)
        }
    }

    fun obtenerRecetaPorId(recetaId: String): RecetaV2? {
        readableDatabase.use { db ->
            db.query(TABLE_RECETAS, null, "id = ?", arrayOf(recetaId), null, null, null).use { cursor ->
                if (cursor.moveToFirst()) {
                    val ingredientes = obtenerIngredientesDeReceta(db, recetaId)
                    return RecetaV2(
                        id = cursor.getString(cursor.getColumnIndexOrThrow("id")),
                        nombre = cursor.getString(cursor.getColumnIndexOrThrow("nombre")),
                        productoId = cursor.getString(cursor.getColumnIndexOrThrow("productoId")),
                        rendimientoPorcion = cursor.getDouble(cursor.getColumnIndexOrThrow("rendimientoPorcion")),
                        ingredientes = ingredientes
                    )
                }
            }
        }
        return null
    }

    private fun obtenerIngredientesDeReceta(db: SQLiteDatabase, recetaId: String): List<IngredienteReceta> {
        val list = mutableListOf<IngredienteReceta>()
        db.query(TABLE_INGREDIENTES_RECETA, null, "recetaId = ?", arrayOf(recetaId), null, null, null).use { cursor ->
            while (cursor.moveToNext()) {
                list.add(
                    IngredienteReceta(
                        insumoId = cursor.getString(cursor.getColumnIndexOrThrow("insumoId")),
                        nombreInsumo = cursor.getString(cursor.getColumnIndexOrThrow("nombreInsumo")),
                        cantidad = cursor.getDouble(cursor.getColumnIndexOrThrow("cantidad")),
                        unidad = cursor.getString(cursor.getColumnIndexOrThrow("unidad"))
                    )
                )
            }
        }
        return list
    }

    fun guardarProducto(producto: SalesInventoryProductV2) {
        writableDatabase.use { db ->
            val values = ContentValues().apply {
                put("id", producto.id)
                put("nombre", producto.nombre)
                put("emoji", producto.emoji)
                put("categoria", producto.categoria)
                val precioJson = org.json.JSONObject(producto.precioVenta).toString()
                put("precioVenta", precioJson)
                put("esCombo", if (producto.esCombo) 1 else 0)
                put("recetaId", producto.recetaId)
                put("toppingsIncluidos", producto.toppingsIncluidos)
                put("costoToppingExtra", producto.costoToppingExtra)
                put("esProductoTopping", if (producto.esProductoTopping) 1 else 0)
                val consumiblesStr = producto.consumiblesAsociados.joinToString(";") { "${it.consumibleId},${it.cantidad},${it.unidad}" }
                put("consumiblesJson", consumiblesStr)
            }
            db.insertWithOnConflict(TABLE_PRODUCTOS, null, values, SQLiteDatabase.CONFLICT_REPLACE)
        }
    }

    fun obtenerProductoPorId(productoId: String): SalesInventoryProductV2? {
        readableDatabase.use { db ->
            db.query(TABLE_PRODUCTOS, null, "id = ?", arrayOf(productoId), null, null, null).use { cursor ->
                if (cursor.moveToFirst()) {
                    val precioJson = cursor.getString(cursor.getColumnIndexOrThrow("precioVenta"))
                    val precioMap = mutableMapOf<String, Double>()
                    try {
                        val json = org.json.JSONObject(precioJson)
                        json.keys().forEach { key ->
                            precioMap[key] = json.getDouble(key)
                        }
                    } catch (e: Exception) {
                        Timber.tag("DB").w(e, "Error al parsear precio JSON para producto")
                    }
                    return@use SalesInventoryProductV2(
                        id = cursor.getString(cursor.getColumnIndexOrThrow("id")),
                        nombre = cursor.getString(cursor.getColumnIndexOrThrow("nombre")),
                        emoji = cursor.getString(cursor.getColumnIndexOrThrow("emoji")),
                        categoria = cursor.getString(cursor.getColumnIndexOrThrow("categoria")),
                        precioVenta = precioMap,
                        esCombo = cursor.getInt(cursor.getColumnIndexOrThrow("esCombo")) == 1,
                        recetaId = cursor.getString(cursor.getColumnIndexOrThrow("recetaId")),
                        toppingsIncluidos = cursor.getInt(cursor.getColumnIndexOrThrow("toppingsIncluidos")),
                        costoToppingExtra = cursor.getDouble(cursor.getColumnIndexOrThrow("costoToppingExtra")),
                        esProductoTopping = cursor.getInt(cursor.getColumnIndexOrThrow("esProductoTopping")) == 1
                    )
                }
            }
        }
        return null
    }
}

