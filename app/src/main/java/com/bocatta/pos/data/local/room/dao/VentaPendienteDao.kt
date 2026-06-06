package com.bocatta.pos.data.local.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.bocatta.pos.data.local.room.entity.VentaPendienteEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO tipado para operaciones sobre ventas pendientes de sincronización.
 * Reemplaza las queries raw SQL de OfflineDatabase para esta tabla.
 */
@Dao
interface VentaPendienteDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(venta: VentaPendienteEntity)

    @Query("SELECT * FROM ventas_pendientes WHERE estado = 'pendiente' ORDER BY fecha ASC")
    suspend fun obtenerPendientes(): List<VentaPendienteEntity>

    @Query("SELECT * FROM ventas_pendientes WHERE estado IN ('fallida', 'fallida_critica') ORDER BY fecha ASC")
    suspend fun obtenerFallidas(): List<VentaPendienteEntity>

    /** Flow reactivo para observar conteo de ventas fallidas (badge en Caja). */
    @Query("SELECT COUNT(*) FROM ventas_pendientes WHERE estado IN ('fallida', 'fallida_critica')")
    fun observarConteoFallidas(): Flow<Int>

    @Query("SELECT COUNT(*) FROM ventas_pendientes WHERE estado = 'pendiente'")
    suspend fun contarPendientes(): Int

    @Query("SELECT * FROM ventas_pendientes WHERE sucursal = :sucursal AND fecha >= :desde ORDER BY fecha ASC")
    suspend fun obtenerDesde(sucursal: String, desde: Long): List<VentaPendienteEntity>

    @Query("UPDATE ventas_pendientes SET estado = 'sincronizada', intentos = intentos + 1, ultimoIntento = :timestamp WHERE id = :id")
    suspend fun marcarSincronizada(id: String, timestamp: Long)

    @Query("UPDATE ventas_pendientes SET estado = 'fallida', intentos = intentos + 1, ultimoIntento = :timestamp WHERE id = :id")
    suspend fun marcarFallida(id: String, timestamp: Long)

    @Query("UPDATE ventas_pendientes SET estado = 'fallida_critica', intentos = intentos + 1, ultimoIntento = :timestamp WHERE id = :id")
    suspend fun marcarFallidaCritica(id: String, timestamp: Long)

    @Query("UPDATE ventas_pendientes SET estado = 'pendiente', intentos = intentos + 1, ultimoIntento = :timestamp WHERE id = :id")
    suspend fun registrarIntentoFallido(id: String, timestamp: Long)

    /** Reintentar una venta fallida: la marca como pendiente de nuevo. */
    @Query("UPDATE ventas_pendientes SET estado = 'pendiente', intentos = 0 WHERE id = :id")
    suspend fun reintentarVenta(id: String)

    @Query("DELETE FROM ventas_pendientes WHERE estado = 'sincronizada'")
    suspend fun limpiarSincronizadas()
}
