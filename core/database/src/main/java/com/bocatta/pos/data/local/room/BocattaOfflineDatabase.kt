package com.bocatta.pos.data.local.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.bocatta.pos.data.local.room.dao.FolioDao
import com.bocatta.pos.data.local.room.dao.OperacionPendienteDao
import com.bocatta.pos.data.local.room.dao.ProductoDao
import com.bocatta.pos.data.local.room.dao.TurnoContingenciaDao
import com.bocatta.pos.data.local.room.dao.VentaPendienteDao
import com.bocatta.pos.data.local.room.entity.ConsumibleV2Entity
import com.bocatta.pos.data.local.room.entity.FolioEntity
import com.bocatta.pos.data.local.room.entity.HeldOrderEntity
import com.bocatta.pos.data.local.room.entity.IngredienteRecetaEntity
import com.bocatta.pos.data.local.room.entity.InsumoV2Entity
import com.bocatta.pos.data.local.room.entity.OperacionPendienteEntity
import com.bocatta.pos.data.local.room.entity.PresentacionInsV2Entity
import com.bocatta.pos.data.local.room.entity.ProductoV2Entity
import com.bocatta.pos.data.local.room.entity.RecetaV2Entity
import com.bocatta.pos.data.local.room.entity.RegistroJornadaEntity
import com.bocatta.pos.data.local.room.entity.TurnoContingenciaEntity
import com.bocatta.pos.data.local.room.entity.VentaPendienteEntity

@Database(
    entities = [
        VentaPendienteEntity::class,
        OperacionPendienteEntity::class,
        FolioEntity::class,
        HeldOrderEntity::class,
        RegistroJornadaEntity::class,
        TurnoContingenciaEntity::class,
        InsumoV2Entity::class,
        ConsumibleV2Entity::class,
        ProductoV2Entity::class,
        RecetaV2Entity::class,
        IngredienteRecetaEntity::class,
        PresentacionInsV2Entity::class
    ],
    version = 12,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class BocattaOfflineDatabase : RoomDatabase() {

    abstract fun ventaPendienteDao(): VentaPendienteDao
    abstract fun operacionPendienteDao(): OperacionPendienteDao
    abstract fun folioDao(): FolioDao
    abstract fun turnoContingenciaDao(): TurnoContingenciaDao
    abstract fun productoDao(): ProductoDao

    companion object {
        @Volatile
        private var INSTANCE: BocattaOfflineDatabase? = null

        fun getInstance(context: Context): BocattaOfflineDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    BocattaOfflineDatabase::class.java,
                    "bocatta_offline.db"
                )
                    .fallbackToDestructiveMigration(false)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
