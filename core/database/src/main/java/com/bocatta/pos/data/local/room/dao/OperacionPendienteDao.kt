package com.bocatta.pos.data.local.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.bocatta.pos.data.local.room.entity.OperacionPendienteEntity

/**
 * DAO tipado para operaciones offline pendientes (devoluciones, cancelaciones, mermas).
 */
@Dao
interface OperacionPendienteDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(operacion: OperacionPendienteEntity)

    @Query("SELECT * FROM operaciones_pendientes WHERE estado = 'pendiente' ORDER BY fecha ASC")
    suspend fun obtenerPendientes(): List<OperacionPendienteEntity>

    @Query("UPDATE operaciones_pendientes SET estado = 'sincronizada', intentos = intentos + 1 WHERE id = :id")
    suspend fun marcarSincronizada(id: String)

    @Query("UPDATE operaciones_pendientes SET estado = 'pendiente', intentos = intentos + 1 WHERE id = :id")
    suspend fun registrarIntentoFallido(id: String)

    @Query("UPDATE operaciones_pendientes SET estado = 'fallida', intentos = intentos + 1 WHERE id = :id")
    suspend fun marcarFallida(id: String)

    @Query("DELETE FROM operaciones_pendientes WHERE estado = 'sincronizada'")
    suspend fun limpiarSincronizadas()
}
