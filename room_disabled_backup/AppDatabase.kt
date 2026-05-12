package com.bocatta.pos.data.local.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        VentaOfflineEntity::class,
        OperacionOfflineEntity::class,
        InsumoEntity::class,
        ConsumibleEntity::class,
        ProductoEntity::class,
        RecetaEntity::class,
        IngredienteRecetaEntity::class,
        PresentacionEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun ventaOfflineDao(): VentaOfflineDao
    abstract fun operacionOfflineDao(): OperacionOfflineDao
    abstract fun insumoDao(): InsumoDao
    abstract fun productoDao(): ProductoDao
    abstract fun recetaDao(): RecetaDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "bocatta_room.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
