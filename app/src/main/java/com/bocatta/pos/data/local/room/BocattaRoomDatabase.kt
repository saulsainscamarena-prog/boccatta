package com.bocatta.pos.data.local.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.bocatta.pos.data.local.room.dao.FolioDao
import com.bocatta.pos.data.local.room.dao.OperacionPendienteDao
import com.bocatta.pos.data.local.room.dao.VentaPendienteDao
import com.bocatta.pos.data.local.room.entity.FolioEntity
import com.bocatta.pos.data.local.room.entity.OperacionPendienteEntity
import com.bocatta.pos.data.local.room.entity.VentaPendienteEntity

/**
 * Base de datos Room para Bocatta POS.
 *
 * Fase 1 de migración: solo incluye entidades de ventas, operaciones y folios.
 * Las tablas de catálogo (insumos, recetas, productos) permanecen en OfflineDatabase (SQLite nativo)
 * y se migrarán en fases posteriores.
 *
 * NOTA: Esta base de datos coexiste con OfflineDatabase (bocatta_offline.db).
 * Usa un archivo separado (bocatta_room.db) para evitar conflictos de migración.
 */
@Database(
    entities = [
        VentaPendienteEntity::class,
        OperacionPendienteEntity::class,
        FolioEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class BocattaRoomDatabase : RoomDatabase() {

    abstract fun ventaPendienteDao(): VentaPendienteDao
    abstract fun operacionPendienteDao(): OperacionPendienteDao
    abstract fun folioDao(): FolioDao

    companion object {
        @Volatile
        private var INSTANCE: BocattaRoomDatabase? = null

        fun getInstance(context: Context): BocattaRoomDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BocattaRoomDatabase::class.java,
                    "bocatta_room.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
