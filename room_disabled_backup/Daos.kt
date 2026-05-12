package com.bocatta.pos.data.local.room

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface VentaOfflineDao {
    @Query("SELECT * FROM ventas_pendientes WHERE estado = 'pendiente' ORDER BY fecha ASC")
    suspend fun obtenerPendientes(): List<VentaOfflineEntity>

    @Query("SELECT COUNT(*) FROM ventas_pendientes WHERE estado = 'pendiente'")
    suspend fun contarPendientes(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardar(venta: VentaOfflineEntity)

    @Query("UPDATE ventas_pendientes SET estado = :estado, intentos = intentos + 1, ultimoIntento = :timestamp WHERE id = :id")
    suspend fun actualizarEstado(id: String, estado: String, timestamp: Long? = null)

    @Query("DELETE FROM ventas_pendientes WHERE estado = 'sincronizada'")
    suspend fun limpiarSincronizadas()

    @Query("SELECT * FROM ventas_pendientes WHERE id = :id")
    suspend fun obtenerPorId(id: String): VentaOfflineEntity?
}

@Dao
interface OperacionOfflineDao {
    @Query("SELECT * FROM operaciones_pendientes WHERE estado = 'pendiente' ORDER BY fecha ASC")
    suspend fun obtenerPendientes(): List<OperacionOfflineEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardar(operacion: OperacionOfflineEntity)

    @Query("UPDATE operaciones_pendientes SET estado = :estado, intentos = intentos + 1 WHERE id = :id")
    suspend fun actualizarEstado(id: String, estado: String)

    @Query("DELETE FROM operaciones_pendientes WHERE estado = 'sincronizada'")
    suspend fun limpiarSincronizadas()
}

@Dao
interface InsumoDao {
    @Query("SELECT * FROM insumos_v2 ORDER BY nombre ASC")
    suspend fun obtenerTodos(): List<InsumoEntity>

    @Query("SELECT cantidadEnBase FROM insumos_v2 WHERE id = :id")
    suspend fun obtenerStock(id: String): Double?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardar(insumo: InsumoEntity)

    @Query("UPDATE insumos_v2 SET cantidadEnBase = :cantidad WHERE id = :id")
    suspend fun actualizarStock(id: String, cantidad: Double)
}

@Dao
interface ProductoDao {
    @Query("SELECT * FROM productos_v2 WHERE id = :id")
    suspend fun obtenerPorId(id: String): ProductoEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardar(producto: ProductoEntity)
}

@Dao
interface RecetaDao {
    @Query("SELECT * FROM recetas_v2 WHERE id = :id")
    suspend fun obtenerPorId(id: String): RecetaEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardar(receta: RecetaEntity)

    @Query("SELECT * FROM ingredientes_receta WHERE recetaId = :recetaId")
    suspend fun obtenerIngredientes(recetaId: String): List<IngredienteRecetaEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardarIngrediente(ingrediente: IngredienteRecetaEntity)

    @Query("DELETE FROM ingredientes_receta WHERE recetaId = :recetaId")
    suspend fun eliminarIngredientes(recetaId: String)
}
