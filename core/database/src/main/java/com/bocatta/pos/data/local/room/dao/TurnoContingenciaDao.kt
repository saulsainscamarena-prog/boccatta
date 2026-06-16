package com.bocatta.pos.data.local.room.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.bocatta.pos.data.local.room.entity.TurnoContingenciaEntity

@Dao
interface TurnoContingenciaDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(turno: TurnoContingenciaEntity)

    @Update
    suspend fun update(turno: TurnoContingenciaEntity)

    @Delete
    suspend fun delete(turno: TurnoContingenciaEntity)

    @Query("SELECT * FROM turnos_contingencia WHERE id = :id")
    suspend fun getById(id: String): TurnoContingenciaEntity?

    @Query("SELECT * FROM turnos_contingencia WHERE syncPendiente = 1")
    suspend fun getPendingSync(): List<TurnoContingenciaEntity>

    @Query("UPDATE turnos_contingencia SET estado = :estado, fechaCierre = :fechaCierre WHERE id = :id")
    suspend fun cerrarTurno(id: String, estado: String, fechaCierre: Long)

    @Query("SELECT * FROM turnos_contingencia WHERE estado = 'abierto' AND sucursal = :sucursal")
    suspend fun getTurnoAbierto(sucursal: String): TurnoContingenciaEntity?

    @Query("SELECT * FROM turnos_contingencia")
    suspend fun getAll(): List<TurnoContingenciaEntity>
}
