package com.bocatta.pos.data.local.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.bocatta.pos.data.local.room.entity.FolioEntity

/**
 * DAO tipado para control de folios de tickets por sucursal.
 */
@Dao
interface FolioDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardarFolio(folio: FolioEntity)

    @Query("SELECT ultimoTicket FROM folios_offline WHERE sucursal = :sucursalId")
    suspend fun obtenerUltimoTicket(sucursalId: String): Long?

    @Query("SELECT * FROM folios_offline WHERE sucursal = :sucursalId")
    suspend fun obtenerFolio(sucursalId: String): FolioEntity?
}
