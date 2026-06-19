package com.bocatta.pos.data.local.room

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

private fun addColumnIfMissing(db: SupportSQLiteDatabase, table: String, column: String, definition: String) {
    db.query("PRAGMA table_info($table)").use { cursor ->
        while (cursor.moveToNext()) {
            if (cursor.getString(cursor.getColumnIndexOrThrow("name")) == column) return
        }
    }
    db.execSQL("ALTER TABLE $table ADD COLUMN $column $definition")
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS registro_jornadas (
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
            CREATE TABLE IF NOT EXISTS turnos_contingencia (
                id TEXT PRIMARY KEY,
                tenantId TEXT NOT NULL DEFAULT 'tenant_pionero',
                sucursal TEXT NOT NULL,
                usuarioId TEXT NOT NULL,
                usuarioNombre TEXT NOT NULL,
                rol TEXT NOT NULL,
                fondoInicial REAL NOT NULL,
                fechaApertura INTEGER NOT NULL,
                fechaCierre INTEGER,
                estado TEXT NOT NULL DEFAULT 'abierto',
                efectivoContado REAL NOT NULL DEFAULT 0.0,
                tarjetaContada REAL NOT NULL DEFAULT 0.0,
                syncPendiente INTEGER NOT NULL DEFAULT 1
            )
        """)
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS held_orders (
                id TEXT PRIMARY KEY,
                carritoJson TEXT NOT NULL,
                clienteJson TEXT,
                nota TEXT NOT NULL DEFAULT '',
                fecha INTEGER NOT NULL,
                sucursal TEXT NOT NULL,
                total REAL NOT NULL DEFAULT 0.0,
                modalidad TEXT NOT NULL DEFAULT 'LOCAL',
                mesaId TEXT
            )
        """)
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS insumos_v2 (
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
            CREATE TABLE IF NOT EXISTS consumibles_v2 (
                id TEXT PRIMARY KEY,
                nombre TEXT NOT NULL,
                unidadBase TEXT NOT NULL DEFAULT 'pz',
                stockActual REAL NOT NULL DEFAULT 0.0,
                stockMinimo REAL NOT NULL DEFAULT 50.0
            )
        """)
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS productos_v2 (
                id TEXT PRIMARY KEY,
                tenantId TEXT NOT NULL DEFAULT '',
                businessType TEXT NOT NULL DEFAULT 'RESTAURANT',
                nombre TEXT NOT NULL,
                emoji TEXT DEFAULT '🍽',
                categoria TEXT,
                precioVenta TEXT,
                esCombo INTEGER NOT NULL DEFAULT 0,
                recetaId TEXT,
                toppingsIncluidos INTEGER NOT NULL DEFAULT 2,
                costoToppingExtra REAL NOT NULL DEFAULT 10.0,
                esProductoTopping INTEGER NOT NULL DEFAULT 0,
                consumiblesJson TEXT,
                requiresStock INTEGER NOT NULL DEFAULT 0,
                hasVariants INTEGER NOT NULL DEFAULT 0,
                barcode TEXT,
                activo INTEGER NOT NULL DEFAULT 1
            )
        """)
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS recetas_v2 (
                id TEXT PRIMARY KEY,
                nombre TEXT NOT NULL,
                productoId TEXT,
                rendimientoPorcion REAL NOT NULL DEFAULT 1.0
            )
        """)
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS ingredientes_receta (
                id TEXT PRIMARY KEY,
                recetaId TEXT NOT NULL,
                insumoId TEXT NOT NULL,
                nombreInsumo TEXT,
                cantidad REAL NOT NULL,
                unidad TEXT NOT NULL DEFAULT 'g',
                FOREIGN KEY (recetaId) REFERENCES recetas_v2(id) ON DELETE CASCADE
            )
        """)
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS presentaciones_insumo (
                id TEXT PRIMARY KEY,
                insumoId TEXT NOT NULL,
                nombre TEXT NOT NULL,
                unidadEquivalente TEXT NOT NULL DEFAULT 'pz',
                factorConversionABase REAL NOT NULL DEFAULT 1.0,
                cantidadDisponible REAL NOT NULL DEFAULT 0.0,
                ultimoPrecioPagado REAL NOT NULL DEFAULT 0.0,
                FOREIGN KEY (insumoId) REFERENCES insumos_v2(id) ON DELETE CASCADE
            )
        """)
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS held_orders (
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
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS registro_jornadas (
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
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS folios_offline (
                sucursal TEXT PRIMARY KEY,
                ultimoTicket INTEGER NOT NULL DEFAULT 0,
                updatedAt INTEGER NOT NULL DEFAULT 0
            )
        """)
    }
}

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        addColumnIfMissing(db, "held_orders", "modalidad", "TEXT NOT NULL DEFAULT 'LOCAL'")
        addColumnIfMissing(db, "held_orders", "mesaId", "TEXT")
    }
}

val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        addColumnIfMissing(db, "ventas_pendientes", "propina", "REAL NOT NULL DEFAULT 0.0")
        addColumnIfMissing(db, "ventas_pendientes", "notaOrden", "TEXT NOT NULL DEFAULT ''")
    }
}

val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS turnos_contingencia (
                id TEXT PRIMARY KEY,
                tenantId TEXT NOT NULL DEFAULT 'tenant_pionero',
                sucursal TEXT NOT NULL,
                usuarioId TEXT NOT NULL,
                usuarioNombre TEXT NOT NULL,
                rol TEXT NOT NULL,
                fondoInicial REAL NOT NULL,
                fechaApertura INTEGER NOT NULL,
                fechaCierre INTEGER,
                estado TEXT NOT NULL DEFAULT 'abierto',
                efectivoContado REAL NOT NULL DEFAULT 0.0,
                tarjetaContada REAL NOT NULL DEFAULT 0.0,
                syncPendiente INTEGER NOT NULL DEFAULT 1
            )
        """)
    }
}

val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        addColumnIfMissing(db, "ventas_pendientes", "descuentoPromociones", "REAL NOT NULL DEFAULT 0.0")
        addColumnIfMissing(db, "ventas_pendientes", "descuentoManual", "REAL NOT NULL DEFAULT 0.0")
    }
}

val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Usar addColumnIfMissing para rutas de migración alternativas
        addColumnIfMissing(db, "turnos_contingencia", "tenantId", "TEXT NOT NULL DEFAULT 'tenant_pionero'")
        addColumnIfMissing(db, "operaciones_pendientes", "tenantId", "TEXT NOT NULL DEFAULT 'tenant_pionero'")
        addColumnIfMissing(db, "ventas_pendientes", "tenantId", "TEXT NOT NULL DEFAULT 'tenant_pionero'")
        addColumnIfMissing(db, "folios_offline", "tenantId", "TEXT NOT NULL DEFAULT 'tenant_pionero'")
    }
}

val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // tenantId ya existe en dispositivos migrados desde v1→v2 (MIGRATION_1_2 lo incluye).
        // Usar addColumnIfMissing para evitar SQLiteException: duplicate column
        addColumnIfMissing(db, "productos_v2", "tenantId", "TEXT NOT NULL DEFAULT ''")
        addColumnIfMissing(db, "productos_v2", "businessType", "TEXT NOT NULL DEFAULT 'RESTAURANT'")
        addColumnIfMissing(db, "productos_v2", "requiresStock", "INTEGER NOT NULL DEFAULT 0")
        addColumnIfMissing(db, "productos_v2", "hasVariants", "INTEGER NOT NULL DEFAULT 0")
        addColumnIfMissing(db, "productos_v2", "barcode", "TEXT")
        addColumnIfMissing(db, "productos_v2", "activo", "INTEGER NOT NULL DEFAULT 1")
    }
}

val MIGRATION_11_12 = object : Migration(11, 12) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Version bump sin cambio de esquema: alinea el número de versión
        // después de consolidar rutas de migración duplicadas (1_2 vs 3_4).
    }
}

val ALL_MIGRATIONS = arrayOf(
    MIGRATION_1_2,
    MIGRATION_2_3,
    MIGRATION_3_4,
    MIGRATION_4_5,
    MIGRATION_5_6,
    MIGRATION_6_7,
    MIGRATION_7_8,
    MIGRATION_8_9,
    MIGRATION_9_10,
    MIGRATION_10_11,
    MIGRATION_11_12
)
