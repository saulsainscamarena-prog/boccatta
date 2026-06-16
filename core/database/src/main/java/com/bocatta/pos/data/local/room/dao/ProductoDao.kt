package com.bocatta.pos.data.local.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.bocatta.pos.data.local.room.entity.ConsumibleV2Entity
import com.bocatta.pos.data.local.room.entity.FolioEntity
import com.bocatta.pos.data.local.room.entity.HeldOrderEntity
import com.bocatta.pos.data.local.room.entity.IngredienteRecetaEntity
import com.bocatta.pos.data.local.room.entity.InsumoV2Entity
import com.bocatta.pos.data.local.room.entity.PresentacionInsV2Entity
import com.bocatta.pos.data.local.room.entity.ProductoV2Entity
import com.bocatta.pos.data.local.room.entity.RecetaV2Entity
import com.bocatta.pos.data.local.room.entity.RegistroJornadaEntity

@Dao
interface ProductoDao {

    // Insumos
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInsumo(insumo: InsumoV2Entity)

    @Query("SELECT * FROM insumos_v2 WHERE id = :id")
    suspend fun getInsumoById(id: String): InsumoV2Entity?

    @Query("SELECT * FROM insumos_v2")
    suspend fun getAllInsumos(): List<InsumoV2Entity>

    // Consumibles
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConsumible(consumible: ConsumibleV2Entity)

    @Query("SELECT * FROM consumibles_v2 WHERE id = :id")
    suspend fun getConsumibleById(id: String): ConsumibleV2Entity?

    @Query("SELECT * FROM consumibles_v2")
    suspend fun getAllConsumibles(): List<ConsumibleV2Entity>

    // Productos
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducto(producto: ProductoV2Entity)

    @Query("SELECT * FROM productos_v2 WHERE id = :id")
    suspend fun getProductoById(id: String): ProductoV2Entity?

    @Query("SELECT * FROM productos_v2")
    suspend fun getAllProductos(): List<ProductoV2Entity>

    // Recetas
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReceta(receta: RecetaV2Entity)

    @Query("SELECT * FROM recetas_v2 WHERE id = :id")
    suspend fun getRecetaById(id: String): RecetaV2Entity?

    @Query("SELECT * FROM recetas_v2")
    suspend fun getAllRecetas(): List<RecetaV2Entity>

    // Ingredientes de receta
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIngredienteReceta(ingrediente: IngredienteRecetaEntity)

    @Query("SELECT * FROM ingredientes_receta WHERE id = :id")
    suspend fun getIngredienteRecetaById(id: String): IngredienteRecetaEntity?

    @Query("SELECT * FROM ingredientes_receta WHERE recetaId = :recetaId")
    suspend fun getIngredientesByRecetaId(recetaId: String): List<IngredienteRecetaEntity>

    // Presentaciones
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPresentacionIns(presentacion: PresentacionInsV2Entity)

    @Query("SELECT * FROM presentaciones_insumo WHERE id = :id")
    suspend fun getPresentacionInsById(id: String): PresentacionInsV2Entity?

    @Query("SELECT * FROM presentaciones_insumo WHERE insumoId = :insumoId")
    suspend fun getPresentacionesByInsumoId(insumoId: String): List<PresentacionInsV2Entity>

    // Held Orders
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHeldOrder(heldOrder: HeldOrderEntity)

    @Query("SELECT * FROM held_orders WHERE id = :id")
    suspend fun getHeldOrderById(id: String): HeldOrderEntity?

    @Query("SELECT * FROM held_orders")
    suspend fun getAllHeldOrders(): List<HeldOrderEntity>

    // Folios
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolio(folio: FolioEntity)

    @Query("SELECT * FROM folios_offline WHERE sucursal = :sucursal")
    suspend fun getFolioBySucursal(sucursal: String): FolioEntity?

    @Query("SELECT * FROM folios_offline")
    suspend fun getAllFolios(): List<FolioEntity>

    // Registro Jornadas
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRegistroJornada(jornada: RegistroJornadaEntity)

    @Query("SELECT * FROM registro_jornadas WHERE id = :id")
    suspend fun getRegistroJornadaById(id: String): RegistroJornadaEntity?

    @Query("SELECT * FROM registro_jornadas ORDER BY timestamp DESC")
    suspend fun getAllJornadas(): List<RegistroJornadaEntity>
}
